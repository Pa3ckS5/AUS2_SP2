package file.heapfile;

import whoApp.data.IRecord;
import tree.bs.avl.AVLTree;

import java.io.*;

public class HeapFile<T extends IRecord<T>> {
    protected String fileName;
    protected RandomAccessFile file;
    protected String heapFileName;

    protected Class<T> recordClass;
    protected int recordSize;
    protected int recordsPerBlock;

    protected int blockSize;
    protected int blockCount;

    protected AVLTree<Integer> emptyBlocks; //no duplicates
    protected AVLTree<Integer> partiallyEmptyBlocks; //no duplicates


    public HeapFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        this.fileName = fileName + ".dat";
        this.heapFileName = fileName + "_heap.dat";
        this.blockSize = blockSize;
        this.blockCount = 0;
        this.recordClass = recordClass;
        this.emptyBlocks = new AVLTree<>();
        this.partiallyEmptyBlocks = new AVLTree<>();

        try {
            this.recordSize = recordClass.newInstance().getSize();
            this.recordsPerBlock = (blockSize - Integer.BYTES) / recordSize; //validCount size
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating record instance.", e);
        }

        if (recordSize > blockSize) {
            throw new RuntimeException("Record size is larger than block size.");
        }

        try {
            this.file = new RandomAccessFile(fileName + ".dat", "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error creating file at " + fileName, e);
        }

        loadHeapFile();
    }

    public int insert(T record) {
        // partially empty block
        if (!partiallyEmptyBlocks.isEmpty()) {
            int blockIndex = partiallyEmptyBlocks.findMin();
            Block<T> block = loadBlock(blockIndex);
            if (block.addRecord(record)) {
                saveBlockToFile(blockIndex, block);
                if (block.isFull()) {
                    partiallyEmptyBlocks.remove(blockIndex);
                }
                return blockIndex;
            }
        }

        // empty block
        if (!emptyBlocks.isEmpty()) {
            int blockIndex = emptyBlocks.findMin();

            Block<T> block = createNewBlock();
            if (block.addRecord(record)) {
                saveBlockToFile(blockIndex, block);
                if (block.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.insert(blockIndex);
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
                partiallyEmptyBlocks.insert(newBlockIndex);
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
            if (!block.deleteRecord(record)) {
                return false;
            }

            if (wasFull) {
                if (block.isEmpty()) {
                    emptyBlocks.insert(blockIndex);
                } else if (block.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.insert(blockIndex);
                }
            } else if (block.isEmpty()) {
                partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
                emptyBlocks.insert(blockIndex);

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

            byte[] data = block.getBytes();

            if (data.length > blockSize) {
                throw new IllegalStateException(
                        "Block bytes larger than blockSize! data=" + data.length + ", blockSize=" + blockSize
                );
            }

            if (data.length < blockSize) {
                byte[] padded = new byte[blockSize];
                System.arraycopy(data, 0, padded, 0, data.length);
                file.write(padded);
            } else {
                file.write(data);
            }
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

            int eSize = emptyBlocks.getNodeCount();
            dos.writeInt(eSize);
            if (eSize > 0) {
                for (Integer blockIndex : emptyBlocks) {
                    dos.writeInt(blockIndex);
                }
            }

            int peSize = partiallyEmptyBlocks.getNodeCount();
            dos.writeInt(peSize);
            if (peSize > 0) {
                for (Integer blockIndex : partiallyEmptyBlocks) {
                    dos.writeInt(blockIndex);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error saving header file.", e);
        }
    }

    private void loadHeapFile() {
        File headerFile = new File(heapFileName);
        if (!headerFile.exists()) {
            // initialize
            this.blockCount = 0;
            return;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(headerFile))) {
            this.blockCount = dis.readInt();

            int emptyCount = dis.readInt();
            emptyBlocks.clear();
            for (int i = 0; i < emptyCount; i++) {
                emptyBlocks.insert(dis.readInt());
            }

            int partialCount = dis.readInt();
            partiallyEmptyBlocks.clear();
            for (int i = 0; i < partialCount; i++) {
                partiallyEmptyBlocks.insert(dis.readInt());
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error loading header file.", e);
        }
    }

    protected int getLastNonEmptyBlock() {
        int lastNonEmptyBlock = -1;

        // finding last non empty block
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

        // all empty
        if (lastNonEmptyBlock == -1) {
            if (blockCount > 0) {
                truncateToBlock(0);
                blockCount = 0;
                emptyBlocks.clear();
                partiallyEmptyBlocks.clear();
            }
            return;
        }

        int newBlockCount = lastNonEmptyBlock + 1;

        if (newBlockCount < blockCount) {
            for (int i = blockCount - 1; i >= newBlockCount; i--) {
                emptyBlocks.remove(Integer.valueOf(i));
                partiallyEmptyBlocks.remove(Integer.valueOf(i));
            }

            truncateToBlock(newBlockCount);
            this.blockCount = newBlockCount;
        }
    }

    protected void truncateLastBlock() {
        truncateToBlock(blockCount - 1);
        blockCount--;
    }

    private void truncateToBlock(int newBlockCount) {
            long newLength = (long) newBlockCount * blockSize;
        try {
            file.setLength(newLength);
        } catch (IOException e) {
            System.out.println("Error truncating block " + newBlockCount + "!");
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
        System.out.println("Empty blocks: " + emptyBlocks.nodesToString());
        System.out.println("Partially empty blocks: " + partiallyEmptyBlocks.nodesToString());
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
        sb.append("Empty blocks: ").append(emptyBlocks.nodesToString()).append("\n");
        sb.append("Partially empty blocks: ").append(partiallyEmptyBlocks.nodesToString()).append("\n");
        sb.append("Total blocks in file: ").append(getBlockCount()).append("\n\n");

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

    public boolean isLastBlockEmpty() {
        return loadBlock(blockCount - 1).isEmpty();
    }
}