package heapfile;

import java.io.*;
import java.util.LinkedList;

public class HeapFile<T extends IRecord<T>> {
    private String fileName;
    private String headerFileName;
    private RandomAccessFile file;

    private Class<T> recordClass;
    private int recordSize;
    private int recordsPerBlock;

    private int blockSize;
    private int blockCount;
    private Block<T> currentBlock;
    private int currentBlockIndex;

    private LinkedList<Integer> emptyBlocks;
    private LinkedList<Integer> partiallyEmptyBlocks;


    public HeapFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        this.fileName = fileName + ".dat";
        this.headerFileName = fileName + ".hdr";
        this.blockSize = blockSize;
        this.recordClass = recordClass;
        this.emptyBlocks = new LinkedList<>();
        this.partiallyEmptyBlocks = new LinkedList<>();

        try {
            this.recordSize = recordClass.newInstance().getSize();
            this.recordsPerBlock = blockSize / recordSize;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating record instance.", e);
        }

        this.file = new RandomAccessFile(fileName + ".dat", "rw");
        loadHeader();
    }

    public int insert(T data) {
        // 1. Skús vložiť do aktuálneho bloku
        if (currentBlock != null && currentBlock.isPartiallyEmpty()) {
            if (currentBlock.addRecord(data)) {
                if (currentBlock.isFull()) {
                    partiallyEmptyBlocks.remove(Integer.valueOf(currentBlockIndex));
                }
                saveBlockToFile(currentBlockIndex, currentBlock);
                return currentBlockIndex;
            }
        }

        // 2. Skús vložiť do čiastočne prázdneho bloku
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

        // 3. Skús vložiť do prázdneho bloku
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

        // 4. Vytvor nový blok
        int newBlockIndex = blockCount;
        Block<T> newBlock = new Block<>(recordsPerBlock, recordClass);
        if (newBlock.addRecord(data)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;
            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }
            currentBlock = newBlock;
            currentBlockIndex = newBlockIndex;
            return newBlockIndex;
        }

        return -1; // Chyba
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

    private void saveBlockToFile(int blockIndex, Block<T> block) {
        try {
            long position = (long) blockIndex * blockSize;
            file.seek(position);
            file.write(block.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Error saving block in file.", e);
        }
    }

    private boolean loadBlockFromFile(int blockIndex) {
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

            Block<T> block = new Block<>(recordsPerBlock, recordClass);
            block.fromBytes(blockBytes);
            currentBlock = block;
            currentBlockIndex = blockIndex;
            return true;

        } catch (IOException e) {
            throw new IllegalStateException("Error loading block from file.", e);
        }
    }

    private void saveCurrentAndLoadBlock(int blockIndex) {
        saveBlockToFile(currentBlockIndex, currentBlock);
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

    private void loadHeader() {
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
        for (int i = blockCount - 1; i >= 0; i--) {
            saveCurrentAndLoadBlock(i);
            if (currentBlock != null && currentBlock.isEmpty()) {
                emptyBlocks.remove(Integer.valueOf(i));
                truncateFileToBlock(i);
                this.blockCount--;
            } else {
                break; // narazili sme na neprázdny blok
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

    private int getBlockCount() {
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
        saveBlockToFile(currentBlockIndex, currentBlock);

        System.out.println("=== HEAP FILE BLOCKS ===");
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

        System.out.println("=== END OF HEAP FILE ===");
    }
}