package file.heapfile;

import file.IRecord;

import java.io.*;
import java.util.LinkedList;

public class HeapFile<T extends IRecord<T>> {
    protected String fileName;
    protected RandomAccessFile file;
    protected String heapFileName;

    protected Class<T> recordClass;
    protected int recordSize;
    protected int recordsPerBlock;

    protected int blockSize;
    protected int blockCount;

    protected LinkedList<Integer> emptyBlocks;
    protected LinkedList<Integer> partiallyEmptyBlocks;


    public HeapFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        this.fileName = fileName + ".dat";
        this.heapFileName = fileName + "_heap.dat";
        this.blockSize = blockSize;
        this.blockCount = 0;
        this.recordClass = recordClass;
        this.emptyBlocks = new LinkedList<>();
        this.partiallyEmptyBlocks = new LinkedList<>();

        try {
            this.recordSize = recordClass.newInstance().getSize();
            this.recordsPerBlock = (blockSize - Integer.BYTES) / recordSize; //validCount size
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating record instance.", e);
        }

        this.file = new RandomAccessFile(fileName + ".dat", "rw");
        loadHeapFile();
    }

    public int insert(T record) {
        // partially empty block
        if (!partiallyEmptyBlocks.isEmpty()) {
            partiallyEmptyBlocks.sort(Integer::compareTo);
            int blockIndex = partiallyEmptyBlocks.getFirst();
            Block<T> block = loadBlock(blockIndex);
            if (block.addRecord(record)) {
                saveBlockToFile(blockIndex, block);
                if (block.isFull()) {
                    partiallyEmptyBlocks.removeFirst();
                }
                return blockIndex;
            }
        }

        // empty block
        if (!emptyBlocks.isEmpty()) {
            emptyBlocks.sort(Integer::compareTo);
            int blockIndex = emptyBlocks.getFirst();

            Block<T> block = loadBlock(blockIndex);
            if (block.addRecord(record)) {
                saveBlockToFile(blockIndex, block);
                if (block.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.add(blockIndex);
                }
                return blockIndex;
            }
        }

        // new block
        int newBlockIndex = blockCount;
        Block<T> newBlock = createNewBlock();
        if (newBlock.addRecord(record)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;
            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }
            return newBlockIndex;
        }

        return -1; // error
    }

    public T get(int blockIndex, int recordIndex) {
        Block<T> block = loadBlock(blockIndex);
        if (block != null) {
            return block.getRecord(recordIndex);
        }
        return null;
    }

    public T get(int blockIndex, T record) {
        Block<T> block = loadBlock(blockIndex);
        if (block != null) {
            return block.getRecord(record);
        }
        return null;
    }

    public boolean delete(int blockIndex, T record) {
        Block<T> block = loadBlock(blockIndex);

        if (block == null) {
            return false;
        }

        boolean wasFull = block.isFull();

        if (!block.isEmpty()) {
            if (!block.removeRecord(record)) {
                return false;
            }

            if (wasFull) {
                if (block.isEmpty()) {
                    emptyBlocks.add(blockIndex);
                } else if (block.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.add(blockIndex);
                }
            } else if (block.isEmpty()) {
                partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
                emptyBlocks.add(blockIndex);

            }
        }
        saveBlockToFile(blockIndex, block);
        truncateEmptyBlocksAtEnd();

        return true;
    }

    protected Block<T> createNewBlock() {
        return new Block<>(recordsPerBlock, recordClass);
    }

    protected void saveBlockToFile(int blockIndex, Block<T> block) {
        try {
            long position = (long) blockIndex * blockSize;
            file.seek(position);
            file.write(block.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Error saving block in file.", e);
        }
    }

    protected Block<T> loadBlock(int blockIndex) {
        try {
            long position = (long) blockIndex * blockSize;
            if (position >= file.length()) {
                return null;
            }

            file.seek(position);
            byte[] blockBytes = new byte[blockSize];
            int bytesRead = file.read(blockBytes);
            if (bytesRead == -1) {
                return null;
            }

            Block<T> block = createNewBlock();
            block.fromBytes(blockBytes);
            return block;

        } catch (IOException e) {
            System.out.println("Error loading block " + blockIndex);
            return null;
        }
    }

    private void saveHeapFile() {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(heapFileName))) {
            dos.writeInt(blockCount);

            dos.writeInt(emptyBlocks.size());
            for (Integer blockIndex : emptyBlocks) {
                dos.writeInt(blockIndex);
            }

            dos.writeInt(partiallyEmptyBlocks.size());
            for (Integer blockIndex : partiallyEmptyBlocks) {
                dos.writeInt(blockIndex);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error saving header file.", e);
        }
    }

    private void loadHeapFile() {
        File headerFile = new File(heapFileName);
        if (!headerFile.exists()) {
            // Inicializácia pre nový súbor
            this.blockCount = 0;
            return;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(headerFile))) {
            this.blockCount = dis.readInt();

            int emptyCount = dis.readInt();
            emptyBlocks.clear();
            for (int i = 0; i < emptyCount; i++) {
                emptyBlocks.add(dis.readInt());
            }

            int partialCount = dis.readInt();
            partiallyEmptyBlocks.clear();
            for (int i = 0; i < partialCount; i++) {
                partiallyEmptyBlocks.add(dis.readInt());
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error loading header file.", e);
        }
    }

    protected int getLastNonEmptyBlock() {
        int lastNonEmptyBlock = -1;

        // Prejdeme všetky bloky od konca a nájdeme posledný neprázdny
        for (int i = blockCount - 1; i >= 0; i--) {
            Block<T> block = loadBlock(i);
            if (block != null && !block.isEmpty()) {
                lastNonEmptyBlock = i;
                break;
            }
        }
        return lastNonEmptyBlock;
    }

    public void truncateEmptyBlocksAtEnd() {
        int lastNonEmptyBlock = getLastNonEmptyBlock();
        if (lastNonEmptyBlock >= 0) {
            // Nový počet blokov je o 1 viac ako index posledného neprázdneho bloku
            int newBlockCount = lastNonEmptyBlock + 1;

            if (newBlockCount < blockCount) {
                // Odstrániť prázdne bloky z zoznamov
                for (int i = blockCount - 1; i >= newBlockCount; i--) {
                    emptyBlocks.remove(Integer.valueOf(i));
                    partiallyEmptyBlocks.remove(Integer.valueOf(i));
                }

                // Skrátiť súbor a aktualizovať počet
                truncateFileToBlock(newBlockCount);
                this.blockCount = newBlockCount;
            }
        }
    }

    private void truncateFileToBlock(int blockIndex) {
            long newLength = (long) blockIndex * blockSize;
        try {
            file.setLength(newLength);
        } catch (IOException e) {
            System.out.println("Error truncating block " + blockIndex + "!");
        }
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void close() {
        saveHeapFile();

        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing file!");
        }
    }

    public void printAllBlocks() {
        System.out.println("=== START OF FILE ===");
        System.out.println("File: " + fileName);
        System.out.println("Block size: " + blockSize);
        System.out.println("Records per block: " + recordsPerBlock);
        System.out.println("Empty blocks: " + emptyBlocks);
        System.out.println("Partially empty blocks: " + partiallyEmptyBlocks);
        System.out.println("Total blocks in file: " + getBlockCount());
        System.out.println();

        for (int i = 0; i < getBlockCount(); i++) {
            Block<T> block = loadBlock(i);
            if (block != null) {
                System.out.println("Block #" + i + ":");
                System.out.println(block.toString());
            }
        }

        System.out.println("=== END OF FILE ===");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== START OF FILE ===\n");
        sb.append("File: ").append(fileName).append("\n");
        sb.append("Block size: ").append(blockSize).append("\n");
        sb.append("Records per block: ").append(recordsPerBlock).append("\n");
        sb.append("Empty blocks: ").append(emptyBlocks).append("\n");
        sb.append("Partially empty blocks: ").append(partiallyEmptyBlocks).append("\n");
        sb.append("Total blocks in file: ").append(getBlockCount()).append("\n\n");

        // Prechod všetkými blokmi
        for (int i = 0; i < getBlockCount(); i++) {
            Block<T> block = loadBlock(i);
            if (block != null) {
                sb.append("Block #").append(i).append(":\n");
                sb.append(block.toString()).append("\n");
            }
        }

        sb.append("=== END OF FILE ===");

        return sb.toString();
    }

    public int getRecordsPerBlock() {
        return recordsPerBlock;
    }
}