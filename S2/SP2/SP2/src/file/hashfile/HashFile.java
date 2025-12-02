package file.hashfile;

import file.heapfile.HeapFile;
import file.IRecord;
import file.overflowfile.OverflowFile;

import java.io.*;
import java.util.ArrayList;

public class HashFile<T extends IRecord<T>> extends HeapFile<T> {
    private OverflowFile<T> overflowFile;
    private int hashPower;
    private int splitPointer;
    private int recordCount;
    private static final int INITIAL_BLOCK_COUNT = 2;
    private String hashFileName;


    public HashFile(String fileName, int blockSize, int overflowBlockSize, Class<T> recordClass) throws IOException {
        super(fileName, blockSize, recordClass);
        this.recordsPerBlock = (blockSize - 4 * Integer.BYTES) / recordSize; // validCount, nextBlock, recordCount, overflowBlockCount

        this.hashPower = 0;
        this.splitPointer = 0;
        this.recordCount = 0;
        this.overflowFile = new OverflowFile<>(fileName + "_overflow", overflowBlockSize,  recordClass);
        this.hashFileName = fileName + "_hash.dat";


        // starting 2 empty blocks (M=2)
        for (int i = 0; i < INITIAL_BLOCK_COUNT; i++) {
            saveBlockToFile(blockCount, createNewBlock());
            blockCount++;
        }

        loadHashFile();
    }

    @Override
    public int insert(T record) {
        int index = getHashIndex(record.hashCode());

        insert(index, record);
        checkFilling();
        return index;
    }

    private int insert(int index, T record) {
        HashBlock<T> block = loadBlock(index);
        if (block.isPartiallyEmpty()) {
            if (block.addRecord(record)) {
                this.recordCount++;
            }
        } else {
            if (block.hasNextBlock()) {
                if (overflowFile.insertToChain(block.getNextBlock(), record)) {
                    block.incrementOverflowBlockCount();
                }
            } else {
                int overflowBlockIndex = overflowFile.insertToFirst(record);
                block.setNextBlock(overflowBlockIndex);
                block.incrementOverflowBlockCount();
            }
            this.recordCount++;
        }
        block.incrementRecordCount();
        saveBlockToFile(index, block);
        return index;
    }

    public boolean delete(T record) {
        int index = getHashIndex(record.hashCode());
        HashBlock<T> block = loadBlock(index);
        boolean removed = false;

        if (block.removeRecord(record)) {
            block.decrementRecordCount();
            this.recordCount--;
            removed = true;
        } else if (block.hasNextBlock()) {
            removed = overflowFile.delete(block.getNextBlock(), record);
            if (removed) {
                block.decrementRecordCount();
                this.recordCount--;
            }
        }

        saveBlockToFile(index, block);
        shakeChain(index);
        overflowFile.truncateEmptyBlocksAtEnd();
        truncateEmptyBlocksAtEnd();
        checkFilling();
        return removed;
    }

    public T get(T record) {
        int index = getHashIndex(record.hashCode());

        HashBlock<T> block = loadBlock(index);
        T found = block.getRecord(record);
        if (found == null && block.hasNextBlock()) {
            found = overflowFile.get(block.getNextBlock(), record);
        }

        return found;
    }

    @Override
    public void close() {
        try {
            saveHashFile(); // hash
            super.close();  // heap
        } catch (IOException e) {
            throw new RuntimeException("Error closing hash file", e);
        } finally {
            overflowFile.close();
        }
    }

    private int getHashIndex (int code) {
        int index = hash(code, hashPower);
        if (index < splitPointer) {
            index = hash(code, hashPower + 1);
        }
        return index;
    }

    private int hash(int code, int power) {
        return code % ((int) Math.pow(2, power) * INITIAL_BLOCK_COUNT);
    }

    private void increaseHash() {
        int oldSplitPointer = splitPointer;
        int oldHashEdge = getHashEdge();

        int newBlockIndex = blockCount;
        HashBlock<T> newBlock = createNewBlock();
        blockCount++;

        HashBlock<T> oldBlock = loadBlock(oldSplitPointer);
        if (oldBlock == null) {
            saveBlockToFile(newBlockIndex, newBlock);
            return;
        }

        ArrayList<T> allRecords = clearLinkedBlocks(oldBlock);
        saveBlockToFile(oldSplitPointer, oldBlock);

        splitPointer++;
        if (splitPointer >= oldHashEdge) {
            hashPower++;
            splitPointer = 0;
        }

        for (T record : allRecords) {
            int index = getHashIndex(record.hashCode());

            HashBlock<T> targetBlock;
            int targetIndex;

            if (index == oldSplitPointer) {
                targetBlock = oldBlock;
                targetIndex = oldSplitPointer;
            } else if (index == newBlockIndex) {
                targetBlock = newBlock;
                targetIndex = newBlockIndex;
            } else {
                targetBlock = loadBlock(index);
                if (targetBlock == null) {
                    targetBlock = createNewBlock();
                }
                targetIndex = index;
            }

            if (targetBlock.isPartiallyEmpty()) {
                if (targetBlock.addRecord(record)) {
                    recordCount++;
                    targetBlock.incrementRecordCount();
                }
            } else {
                if (targetBlock.hasNextBlock()) {
                    if (overflowFile.insertToChain(targetBlock.getNextBlock(), record)) {
                        targetBlock.incrementOverflowBlockCount();
                    }
                } else {
                    int overflowBlockIndex = overflowFile.insertToFirst(record);
                    targetBlock.setNextBlock(overflowBlockIndex);
                    targetBlock.incrementOverflowBlockCount();
                }
                recordCount++;
                targetBlock.incrementRecordCount();
            }

            if (targetIndex != oldSplitPointer && targetIndex != newBlockIndex) {
                saveBlockToFile(targetIndex, targetBlock);
            }
        }

        saveBlockToFile(oldSplitPointer, oldBlock);
        saveBlockToFile(newBlockIndex, newBlock);
    }

    public void shakeChain(int startBlockIndex) {
        HashBlock<T> block = loadBlock(startBlockIndex);

        boolean canShake = false;
        int recordsToOverflow = block.getRecordCount() - block.getCapacity();
        if (recordsToOverflow > 0) {
            canShake = (recordsToOverflow / overflowFile.getRecordsPerBlock()) < block.getOverflowBlockCount();
        }

        if (canShake) {
            ArrayList<T> allRecords = clearLinkedBlocks(block);

            //pridaj všetky zaznamy znova
            for (T record : allRecords) {
                if (block.isPartiallyEmpty()) {
                    if (block.addRecord(record)) {
                        recordCount++;
                        block.incrementRecordCount();
                    }
                } else {
                    if (block.hasNextBlock()) {
                        if (overflowFile.insertToChain(block.getNextBlock(), record)) {
                            block.incrementOverflowBlockCount();
                        }
                    } else {
                        int overflowBlockIndex = overflowFile.insertToFirst(record);
                        block.setNextBlock(overflowBlockIndex);
                        block.incrementOverflowBlockCount();
                    }
                    recordCount++;
                    block.incrementRecordCount();
                }

            }
            saveBlockToFile(startBlockIndex, block);

            overflowFile.truncateEmptyBlocksAtEnd();
            truncateEmptyBlocksAtEnd();
        }
    }

    private boolean decreaseHash() {
        if (blockCount > 2) {
            splitPointer--;
            if (splitPointer < 0) {
                hashPower--;
                splitPointer = getHashEdge() - 1;
            }
            HashBlock<T> biggerBlock = loadBlock(blockCount - 1);
            HashBlock<T> smallerBlock = loadBlock(splitPointer);
            ArrayList<T> allRecords = new ArrayList<>();
            allRecords.addAll(clearLinkedBlocks(biggerBlock));
            allRecords.addAll(clearLinkedBlocks(smallerBlock));
            saveBlockToFile(blockCount -1, biggerBlock);
            saveBlockToFile(splitPointer, smallerBlock);

            for (T record : allRecords) {
                insert(getHashIndex(record.hashCode()), record);
            }

            blockCount--;

            return true;
        }
        return false;
    }

    private ArrayList<T> clearLinkedBlocks(HashBlock<T> block) {
        if (block != null) {
            ArrayList<T> r = new ArrayList<>(block.getRecordCount());
            int nextIndex = block.getNextBlock();
            r.addAll(block.clear());
            if (nextIndex >= 0) {
                r.addAll(overflowFile.removeLinkedRecords(nextIndex));
            }
            recordCount = recordCount - r.size();
            return r;
        }
        return new ArrayList<>();
    }

    private void checkFilling() {
        double fill = calculateFilling();

        if (fill > 0.8) {
            increaseHash();
        } else if (fill < 0.4 && blockCount > INITIAL_BLOCK_COUNT) {
            decreaseHash();
        }
    }

    @Override
    protected HashBlock<T> createNewBlock() {
        return new HashBlock<>(recordsPerBlock, recordClass);
    }

    @Override
    protected HashBlock<T> loadBlock(int blockIndex) {
        return (HashBlock<T>) super.loadBlock(blockIndex);
    }

    private int getHashEdge() {
        return (int) Math.pow(2, hashPower) * INITIAL_BLOCK_COUNT;
    }

    private void loadHashFile() throws IOException {
        File metadata = new File(hashFileName);
        if (!metadata.exists()) {
            return;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(metadata))) {
            this.hashPower = dis.readInt();
            this.splitPointer = dis.readInt();
            this.recordCount = dis.readInt();
        }
    }

    /**
     * Saves current metadata to the metadata file.
     */
    private void saveHashFile() throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(hashFileName))) {
            dos.writeInt(this.hashPower);
            dos.writeInt(this.splitPointer);
            dos.writeInt(this.recordCount);
        }
    }

    private double calculateFilling() {
        int totalCapacity = blockCount * recordsPerBlock + overflowFile.getCapacity();
        if (totalCapacity == 0) return 0.0;
        return (double) recordCount / totalCapacity;
    }

    protected int getLastNonEmptyBlock() {
        int lastNonEmptyBlock = -1;

        // Prejdeme všetky bloky od konca a nájdeme posledný neprázdny
        for (int i = blockCount - 1; i >= 0; i--) {
            HashBlock<T> block = loadBlock(i);
            if (block != null && !block.isEmpty() && !block.hasNextBlock()) {
                lastNonEmptyBlock = i;
                break;
            }
        }
        return lastNonEmptyBlock;
    }

    @Override
    public void printAllBlocks() {
        System.out.println("=== HASH FILE ===");
        System.out.println("File: " + fileName);
        System.out.println("Block size: " + blockSize);
        System.out.println("Records per block: " + recordsPerBlock);
        System.out.println("Hash power: " + hashPower);
        System.out.println("Split pointer: " + splitPointer);
        System.out.println("Total records: " + recordCount);
        System.out.println("Total blocks: " + blockCount);
        System.out.println("Empty blocks: " + emptyBlocks);
        System.out.println("Partially empty blocks: " + partiallyEmptyBlocks);
        System.out.println();

        // Vypíš všetky hlavné bloky hash tabuľky
        for (int i = 0; i < blockCount; i++) {
            HashBlock<T> block = loadBlock(i);
            if (block != null) {
                System.out.println("Hash Block #" + i + ":");
                System.out.println(block.toString());
                System.out.println();
            }
        }

        System.out.println("=== END OF HASH FILE ===");

        // Vypíš overflow file
        System.out.println();
        overflowFile.printAllBlocks();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== HASH FILE ===\n");
        sb.append("File: ").append(fileName).append("\n");
        sb.append("Block size: ").append(blockSize).append("\n");
        sb.append("Records per block: ").append(recordsPerBlock).append("\n");
        sb.append("Hash power: ").append(hashPower).append("\n");
        sb.append("Split pointer: ").append(splitPointer).append("\n");
        sb.append("Total records: ").append(recordCount).append("\n");
        sb.append("Total blocks: ").append(blockCount).append("\n");
        sb.append("Empty blocks: ").append(emptyBlocks).append("\n");
        sb.append("Partially empty blocks: ").append(partiallyEmptyBlocks).append("\n\n");

        // Vypíš všetky hlavné bloky hash tabuľky
        for (int i = 0; i < blockCount; i++) {
            HashBlock<T> block = loadBlock(i);
            if (block != null) {
                sb.append("Hash Block #").append(i).append(":\n");
                sb.append(block.toString()).append("\n");
            }
        }

        sb.append("=== END OF HASH FILE ===\n\n");

        // Pridaj overflow file
        sb.append(overflowFile.toString());

        return sb.toString();
    }

    // Pomocná metóda na výpis štatistík
    public void printStatistics() {
        System.out.println("=== HASH FILE STATISTICS ===");
        System.out.println("Total records: " + recordCount);
        System.out.println("Main blocks: " + blockCount);
        System.out.println("Overflow blocks: " + overflowFile.getBlockCount());
        System.out.println("Hash power: " + hashPower);
        System.out.println("Split pointer: " + splitPointer);
        System.out.println("Load factor: " + String.format("%.2f", calculateFilling()));
        System.out.println("=============================");
    }

    // V triede HashFile pridaj túto metódu:
    public void printStats() {
        System.out.println("=== DETAILNÉ ŠTATISTIKY HASH FILE ===");
        System.out.println("Celkový počet záznamov: " + recordCount);
        System.out.println("Počet hlavných blokov: " + blockCount);
        System.out.println("Počet overflow blokov: " + overflowFile.getBlockCount());
        System.out.println("Hash power: " + hashPower);
        System.out.println("Split pointer: " + splitPointer);
        System.out.println("Faktor zaplnenia: " + String.format("%.2f%%", calculateFilling() * 100));

        // Štatistiky o reťazcoch
        int chainsWithOverflow = 0;
        int maxChainLength = 0;

        for (int i = 0; i < blockCount; i++) {
            HashBlock<T> block = loadBlock(i);
            if (block != null && block.hasNextBlock()) {
                chainsWithOverflow++;
                int chainLength = block.getOverflowBlockCount();
                if (chainLength > maxChainLength) {
                    maxChainLength = chainLength;
                }
            }
        }

        System.out.println("Počet reťazcov s overflow: " + chainsWithOverflow);
        System.out.println("Maximálna dĺžka reťazca: " + maxChainLength);
        System.out.println("======================================");
    }

    public int getRecordCount() {
        return recordCount;
    }
}