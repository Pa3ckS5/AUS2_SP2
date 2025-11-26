package heapfile;

import java.io.*;
import java.util.LinkedList;

public class HeapFile<T extends IRecord<T>> {
    protected String fileName;
    protected String headerFileName;
    protected RandomAccessFile file;

    protected Class<T> recordClass;
    protected int recordSize;
    protected int recordsPerBlock;

    protected int blockSize;
    protected int blockCount;
    protected Block<T> currentBlock;
    protected int currentBlockIndex;

    protected LinkedList<Integer> emptyBlocks;
    protected LinkedList<Integer> partiallyEmptyBlocks;


    public HeapFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        this.fileName = fileName + ".dat";
        this.headerFileName = fileName + ".hdr";
        this.blockSize = blockSize;
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
        loadHeader();
    }

    public int insert(T data) {
        // current block
        if (currentBlock != null && currentBlock.isPartiallyEmpty()) {
            if (currentBlock.addRecord(data)) {
                if (currentBlock.isFull()) {
                    partiallyEmptyBlocks.remove(Integer.valueOf(currentBlockIndex));
                }
                saveBlockToFile(currentBlockIndex, currentBlock);
                return currentBlockIndex;
            }
        }

        // partially empty block
        if (!partiallyEmptyBlocks.isEmpty()) {
            int blockIndex = partiallyEmptyBlocks.getFirst();
            saveCurrentAndLoadBlock(blockIndex);
            if (currentBlock.addRecord(data)) {
                saveBlockToFile(blockIndex, currentBlock);
                if (currentBlock.isFull()) {
                    partiallyEmptyBlocks.removeFirst();
                }
                currentBlockIndex = blockIndex;
                return blockIndex;
            }
        }

        // empty block
        if (!emptyBlocks.isEmpty()) {
            int blockIndex = emptyBlocks.getFirst();
            saveCurrentAndLoadBlock(blockIndex);
            if (currentBlock.addRecord(data)) {
                saveBlockToFile(blockIndex, currentBlock);
                emptyBlocks.removeFirst();
                if (!currentBlock.isFull()) {
                    partiallyEmptyBlocks.add(blockIndex);
                }
                return blockIndex;
            }
        }

        // new block
        if (currentBlock != null) {
            saveBlockToFile(currentBlockIndex, currentBlock);
        }

        int newBlockIndex = blockCount;
        Block<T> newBlock = createNewBlock(recordsPerBlock, recordClass);
        if (newBlock.addRecord(data)) {
            blockCount++;
            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }
            currentBlock = newBlock;
            currentBlockIndex = newBlockIndex;
            return newBlockIndex;
        }

        return -1; // error
    }

    public T get(int blockIndex, int recordIndex) {
        get(blockIndex);
        if (currentBlock != null) {
            return currentBlock.getRecord(recordIndex);
        }
        return null;
    }

    public T get(int blockIndex, T record) {
        get(blockIndex);
        if (currentBlock != null) {
            return currentBlock.getRecord(record);
        }
        return null;
    }


    protected Block<T> get(int blockIndex) {
        if (currentBlockIndex != blockIndex) {
            saveCurrentAndLoadBlock(blockIndex);
        }
        return currentBlock;
    }

    public boolean delete(int blockIndex, T record) {
        if (currentBlockIndex != blockIndex) {
            saveCurrentAndLoadBlock(blockIndex);
        }
        if (currentBlock == null) {
            return false;
        }

        boolean wasFull = currentBlock.isFull();

        if (!currentBlock.removeRecord(record)) {
            return false;
        }

        if (wasFull) {
            if (currentBlock.isEmpty()) {
                emptyBlocks.add(blockIndex);
            } else if (currentBlock.isPartiallyEmpty()) {
                partiallyEmptyBlocks.add(blockIndex);
            }
        } else if (currentBlock.isEmpty()) {
            partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        }

        truncateEmptyBlocksAtEnd();

        return true;
    }

    protected Block<T> createNewBlock(int recordsPerBlock, Class<T> recordClass) {
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

    protected boolean loadBlockFromFile(int blockIndex) {
        try {
            long position = (long) blockIndex * blockSize;
            if (position >= file.length()) {
                return false;
            }

            file.seek(position);
            byte[] blockBytes = new byte[blockSize];
            int bytesRead = file.read(blockBytes);
            if (bytesRead == -1) {
                return false;
            }

            Block<T> block = createNewBlock(recordsPerBlock, recordClass);
            block.fromBytes(blockBytes);
            currentBlock = block;
            currentBlockIndex = blockIndex;
            return true;

        } catch (IOException e) {
            throw new IllegalStateException("Error loading block from file.", e);
        }
    }

    protected void saveCurrentAndLoadBlock(int blockIndex) {
        if (currentBlock != null && currentBlockIndex >= 0) {
            saveBlockToFile(currentBlockIndex, currentBlock);
        }
        loadBlockFromFile(blockIndex);
    }

    private void saveHeader() {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(headerFileName))) {
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

    protected void loadHeader() {
        File headerFile = new File(headerFileName);
        if (!headerFile.exists()) {
            // Inicializácia pre nový súbor
            this.blockCount = 0;
            this.currentBlock = null;
            this.currentBlockIndex = -1;
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

            // Načítanie aktuálneho bloku
            if (blockCount > 0) {
                this.currentBlockIndex = blockCount - 1;
                loadBlockFromFile(currentBlockIndex);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error loading header file.", e);
        }
    }

    private void truncateEmptyBlocksAtEnd() {
        int lastNonEmptyBlock = -1;

        // Prejdeme všetky bloky od konca a nájdeme posledný neprázdny
        for (int i = blockCount - 1; i >= 0; i--) {
            saveCurrentAndLoadBlock(i);
            if (currentBlock != null && !currentBlock.isEmpty()) {
                lastNonEmptyBlock = i;
                break;
            }
        }

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

            // Aktualizovať aktuálny blok, ak bol odstránený
            if (currentBlockIndex >= newBlockCount) {
                if (newBlockCount > 0) {
                    loadBlockFromFile(newBlockCount - 1);
                    currentBlockIndex = newBlockCount - 1;
                } else {
                    currentBlock = null;
                    currentBlockIndex = -1;
                }
            }
        }
    }

    private void truncateFileToBlock(int blockIndex) {
        try (RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            long newLength = (long) blockIndex * blockSize;
            raf.setLength(newLength);
        } catch (IOException e) {
            throw new IllegalStateException("Error truncating file.", e);
        }
    }

    protected int getBlockCount() {
        return blockCount;
    }

    public void close() {
        if (currentBlock != null) {
            saveBlockToFile(currentBlockIndex, currentBlock);
        }

        saveHeader();

        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error closing file", e);
        }
    }

    public void printAllBlocks() {
        if (currentBlock != null) {
            saveBlockToFile(currentBlockIndex, currentBlock);
        }

        System.out.println("=== START OF FILE ===");
        System.out.println("File: " + fileName);
        System.out.println("Block size: " + blockSize);
        System.out.println("Records per block: " + recordsPerBlock);
        System.out.println("Empty blocks: " + emptyBlocks);
        System.out.println("Partially empty blocks: " + partiallyEmptyBlocks);
        System.out.println("Total blocks in file: " + getBlockCount());
        System.out.println();

        for (int i = 0; i < getBlockCount(); i++) {
            loadBlockFromFile(i);
            if (currentBlock != null) {
                System.out.println("Block #" + i + ":");
                System.out.println(currentBlock.toString());
            }
        }

        System.out.println("=== END OF FILE ===");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Uloženie aktuálneho bloku pred načítaním všetkých
        if (currentBlock != null) {
            saveBlockToFile(currentBlockIndex, currentBlock);
        }

        sb.append("=== START OF FILE ===\n");
        sb.append("File: ").append(fileName).append("\n");
        sb.append("Block size: ").append(blockSize).append("\n");
        sb.append("Records per block: ").append(recordsPerBlock).append("\n");
        sb.append("Empty blocks: ").append(emptyBlocks).append("\n");
        sb.append("Partially empty blocks: ").append(partiallyEmptyBlocks).append("\n");
        sb.append("Total blocks in file: ").append(getBlockCount()).append("\n\n");

        // Prechod všetkými blokmi
        for (int i = 0; i < getBlockCount(); i++) {
            loadBlockFromFile(i);
            if (currentBlock != null) {
                sb.append("Block #").append(i).append(":\n");
                sb.append(currentBlock.toString()).append("\n");
            }
        }

        sb.append("=== END OF FILE ===");

        return sb.toString();
    }
}