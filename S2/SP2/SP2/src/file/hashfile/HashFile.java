package file.hashfile;

import file.heapfile.HeapFile;
import whoApp.data.IRecord;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class HashFile<T extends IRecord<T>> extends HeapFile<T> {
    private OverflowFile<T> overflowFile;
    private int hashPower;
    private int splitPointer;
    private int recordCount;
    private String hashFileName;
    private static final int INITIAL_BLOCK_COUNT = 2;
    private static final double MAX_DENSITY = 0.8;
    private static final double MIN_DENSITY = 0.5;


    public HashFile(String fileName, int blockSize, int overflowBlockSize, Class<T> recordClass) throws IOException {
        super(Paths.get("").toAbsolutePath().resolve(fileName + ".dat").toString(), blockSize, recordClass); //convert to absolute path
        //super(fileName, blockSize, recordClass);
        if (blockSize < overflowBlockSize) {
            throw new IOException("Block size must be greater than overflow block size.");
        }
        if (overflowBlockSize < recordSize) {
            throw new IOException("Overflow block size must be greater than record size.");
        }

        this.recordsPerBlock = (blockSize - 4 * Integer.BYTES) / recordSize; // validCount, nextBlock, recordCount, overflowBlockCount

        this.hashPower = 0;
        this.splitPointer = 0;
        this.recordCount = 0;
        this.overflowFile = new OverflowFile<>(fileName + "_overflow", overflowBlockSize, recordClass);
        this.hashFileName = fileName + "_hash.dat";

        loadHashFile();

        if (blockCount < INITIAL_BLOCK_COUNT) {
            // starting 2 empty blocks (M=2)
            for (int i = 0; i < INITIAL_BLOCK_COUNT; i++) {
                saveBlockToFile(blockCount, createNewBlock());
                blockCount++;
            }
        }
    }

    @Override
    public int insert(T record) {
        int index = getHashIndex(record.hashCode());

        HashBlock<T> block = loadBlock(index);
        insertIntoMainBlockOrOverflow(block, record);
        saveBlockToFile(index, block);

        while (calculateDensity() > MAX_DENSITY) {
            split();
        }

        return index;
    }

    public boolean delete(T record) {
        int index = getHashIndex(record.hashCode());
        HashBlock<T> block = loadBlock(index);

        boolean removed = block.deleteRecord(record);

        if (!removed && block.hasNextBlock()) {
            removed = overflowFile.delete(block.getNextBlock(), record);
        }

        if (removed) {
            block.decrementRecordCount();
            this.recordCount--;

            saveBlockToFile(index, block);
            shake(index);

            while (calculateDensity() < MIN_DENSITY && blockCount > INITIAL_BLOCK_COUNT) {
                merge();
            }
        }

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

    public boolean edit(T editedRecord) {
        int index = getHashIndex(editedRecord.hashCode());
        HashBlock<T> block = loadBlock(index);

        boolean edited = block.editRecord(editedRecord);
        if (edited) {
            saveBlockToFile(index, block);
        } else {
            if (block.hasNextBlock()) {
                edited = overflowFile.edit(block.getNextBlock(), editedRecord);
            }
        }

        return edited;
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

    private int getHashIndex(int code) {
        int index = hash(code, hashPower);
        if (index < 0 || index >= blockCount) {
            System.out.println("Index out of range");
        }
        if (index < splitPointer) {
            index = hash(code, hashPower + 1);
        }
        return index;
    }

    private int hash(int code, int power) {
        int hash = Math.abs(code % ((int) Math.pow(2, power) * INITIAL_BLOCK_COUNT));
        if ((hash == 21 || hash == 6) && (code == 166 || code == 198 || code == 213 || code == 230)) {
            System.out.print("");
        }
        return hash;
    }

    private void split() {
        int oldSplitPointer = splitPointer;
        int oldHashEdge = getHashEdge();     // N = INITIAL_BLOCK_COUNT * 2^hashPower
        int oldHashPower = hashPower;        // i

        int newBlockIndex = blockCount;
        HashBlock<T> newBlock = createNewBlock();
        blockCount++;

        HashBlock<T> oldBlock = loadBlock(oldSplitPointer);
        if (oldBlock == null) {
            return;
        }

        ArrayList<LinkedBlock<T>> allOverflowBlocks = overflowFile.getBlockChain(oldBlock.getNextBlock());
        ArrayList<T> allRecords = new ArrayList<>();
        allRecords.addAll(oldBlock.removeRecords());
        for (LinkedBlock<T> ob : allOverflowBlocks) {
            allRecords.addAll(ob.removeRecords());
        }

        splitPointer++;
        if (splitPointer >= oldHashEdge) {
            hashPower++;
            splitPointer = 0;
        }

        ArrayList<T> oldBlockRecords = new ArrayList<>();
        ArrayList<T> newBlockRecords = new ArrayList<>();

        for (T record : allRecords) {
            int code = record.hashCode();

            int hNext = hash(code, oldHashPower + 1);

            if (hNext == oldSplitPointer) {
                // to old
                oldBlockRecords.add(record);
            } else if (hNext == newBlockIndex) {
                // to new
                newBlockRecords.add(record);
            } else {
                System.err.println("Error: incorrect linear hashing!!!");
                oldBlockRecords.add(record);
            }
        }

        // filling old empty chain
        int lastUsedOld = insertRecordsToChain(oldBlockRecords, oldBlock, allOverflowBlocks);

        ArrayList<LinkedBlock<T>> newOverflowBlocks = new ArrayList<>(); //unused blocks in old chain
        int firstNewOverflowBlockIndex = -1;

        if (lastUsedOld == -1) {
            firstNewOverflowBlockIndex = oldBlock.getNextBlock();
            oldBlock.setNextBlock(-1);
            newOverflowBlocks = allOverflowBlocks;
        } else {
            ArrayList<LinkedBlock<T>> oldUsedOverflowBlocks = new ArrayList<>();

            //divide blocks
            for (int i = 0; i < allOverflowBlocks.size(); i++) {
                if (i <= lastUsedOld) {
                    oldUsedOverflowBlocks.add(allOverflowBlocks.get(i));
                } else {
                    newOverflowBlocks.add(allOverflowBlocks.get(i));
                }
            }
            firstNewOverflowBlockIndex = oldUsedOverflowBlocks.getLast().getNextBlock();
            oldUsedOverflowBlocks.getLast().setNextBlock(-1);

            overflowFile.editBlockChain(oldBlock.getNextBlock(), oldUsedOverflowBlocks);
        }
        saveBlockToFile(oldSplitPointer, oldBlock);

        // filling new empty chain
        int lastUsedNew = insertRecordsToChain(newBlockRecords, newBlock, newOverflowBlocks);

        ArrayList<LinkedBlock<T>> emptyOverflowBlocks = new ArrayList<>(); //unused blocks in new chain
        int firstEmptyOverflowBlockIndex = -1;

        if (lastUsedNew == -1) {
            firstEmptyOverflowBlockIndex = firstNewOverflowBlockIndex;
            newBlock.setNextBlock(-1);
            emptyOverflowBlocks = newOverflowBlocks;
        } else {
            newBlock.setNextBlock(firstNewOverflowBlockIndex);
            ArrayList<LinkedBlock<T>> newUsedOverflowBlocks = new ArrayList<>();

            //divide blocks
            for (int i = 0; i < newOverflowBlocks.size(); i++) {
                if (i <= lastUsedNew) {
                    newUsedOverflowBlocks.add(newOverflowBlocks.get(i));
                } else {
                    emptyOverflowBlocks.add(newOverflowBlocks.get(i));
                }
            }
            firstEmptyOverflowBlockIndex = newUsedOverflowBlocks.getLast().getNextBlock();
            newUsedOverflowBlocks.getLast().setNextBlock(-1);

            overflowFile.editBlockChain(newBlock.getNextBlock(), newUsedOverflowBlocks);
        }
        saveBlockToFile(newBlockIndex, newBlock);

        //clear empty blocks
        ArrayList<Integer> emptyBlocksIndexes = new ArrayList<>();
        emptyBlocksIndexes.add(firstEmptyOverflowBlockIndex);
        for (int i = 0; i < emptyOverflowBlocks.size() - 1; i++) {
            emptyBlocksIndexes.add(emptyOverflowBlocks.get(i).getNextBlock());
        }
        overflowFile.addToEmptyBlocks(emptyBlocksIndexes);

        overflowFile.truncateEmptyBlocksAtEnd();
    }

    private boolean merge() {
        if (blockCount > INITIAL_BLOCK_COUNT) {
            splitPointer--;
            if (splitPointer < 0) {
                hashPower--;
                splitPointer = getHashEdge() - 1;
            }

            //main blocks
            HashBlock<T> lowerBlock = loadBlock(splitPointer);
            HashBlock<T> higherBlock = loadBlock(blockCount - 1);

            //overflow blocks
            ArrayList<LinkedBlock<T>> lowerOverflowBlocks = overflowFile.getBlockChain(lowerBlock.getNextBlock());
            ArrayList<LinkedBlock<T>> higherOverflowBlocks = overflowFile.getBlockChain(higherBlock.getNextBlock());

            ArrayList<LinkedBlock<T>> allOverflowBlocks = new ArrayList<>();
            int allOverflowHeadIndex = -1;

            if (!lowerOverflowBlocks.isEmpty()) {
                // first overflow index
                allOverflowHeadIndex = lowerBlock.getNextBlock();
                allOverflowBlocks.addAll(lowerOverflowBlocks);

                //connect 2 chains
                if (!higherOverflowBlocks.isEmpty()) {
                    allOverflowBlocks.get(allOverflowBlocks.size() - 1).setNextBlock(higherBlock.getNextBlock());
                }
            } else {
                if (!higherOverflowBlocks.isEmpty()) {
                    allOverflowHeadIndex = higherBlock.getNextBlock();
                    lowerBlock.setNextBlock(allOverflowHeadIndex);
                }
            }

            allOverflowBlocks.addAll(higherOverflowBlocks);

            //all records
            ArrayList<T> allRecords = new ArrayList<>();
            allRecords.addAll(lowerBlock.removeRecords());
            allRecords.addAll(higherBlock.removeRecords());
            for (LinkedBlock<T> ob : allOverflowBlocks) {
                allRecords.addAll(ob.removeRecords());
            }

            int lastUsed = insertRecordsToChain(allRecords, lowerBlock, allOverflowBlocks);

            reattachAndFreeOverflowBlocks(lowerBlock, allOverflowBlocks, lastUsed, allOverflowHeadIndex);

            saveBlockToFile(splitPointer, lowerBlock);
            truncateLastBlock();

            return true;
        }
        return false;
    }

    private void shake(int index) {
        HashBlock<T> block = loadBlock(index);

        boolean canShake = false;
        int recordsToOverflow = block.getRecordCount() - block.getCapacity();
        if (recordsToOverflow > 0) {
            int minBlocks = (int) Math.ceil((double) recordsToOverflow / overflowFile.getRecordsPerBlock());
            canShake = minBlocks < block.getOverflowBlockCount();
        } else {
            if (block.getOverflowBlockCount() > 0) {
                canShake = true;
            }
        }

        if (canShake) {
            //shake
            ArrayList<LinkedBlock<T>> allOverflowBlocks = overflowFile.getBlockChain(block.getNextBlock());
            ArrayList<T> allRecords = new ArrayList<>();
            allRecords.addAll(block.removeRecords());
            for (LinkedBlock<T> ob : allOverflowBlocks) {
                allRecords.addAll(ob.removeRecords());
            }

            int chainHeadIndex = block.getNextBlock();
            int lastUsed = insertRecordsToChain(allRecords, block, allOverflowBlocks);

            reattachAndFreeOverflowBlocks(block, allOverflowBlocks, lastUsed, chainHeadIndex);

            saveBlockToFile(index, block);
            overflowFile.truncateEmptyBlocksAtEnd();
        }
    }

    private void reattachAndFreeOverflowBlocks(HashBlock<T> block, ArrayList<LinkedBlock<T>> overflowBlocks, int lastUsed, int originalChainHeadIdx) {
        ArrayList<LinkedBlock<T>> emptyOverflowBlocks = new ArrayList<>();
        int firstEmptyOverflowBlockIndex = -1;

        if (lastUsed == -1) {
            // no overflow blocks
            block.setNextBlock(-1);

            if (!overflowBlocks.isEmpty() && originalChainHeadIdx != -1) {
                firstEmptyOverflowBlockIndex = originalChainHeadIdx;
                emptyOverflowBlocks.addAll(overflowBlocks);
            }
        } else {
            // divide blocks
            ArrayList<LinkedBlock<T>> usedOverflowBlocks = new ArrayList<>();

            for (int i = 0; i < overflowBlocks.size(); i++) {
                if (i <= lastUsed) {
                    usedOverflowBlocks.add(overflowBlocks.get(i));
                } else {
                    emptyOverflowBlocks.add(overflowBlocks.get(i));
                }
            }

            if (!usedOverflowBlocks.isEmpty()) {
                if (!emptyOverflowBlocks.isEmpty()) {
                    firstEmptyOverflowBlockIndex = usedOverflowBlocks.get(usedOverflowBlocks.size() - 1).getNextBlock();
                }

                usedOverflowBlocks.get(usedOverflowBlocks.size() - 1).setNextBlock(-1);

                if (originalChainHeadIdx != -1) {
                    overflowFile.editBlockChain(originalChainHeadIdx, usedOverflowBlocks);
                } else {
                    // for safety
                    block.setNextBlock(-1);
                }
            }
        }

        // clear empty blocks
        if (firstEmptyOverflowBlockIndex != -1 && !emptyOverflowBlocks.isEmpty()) {
            ArrayList<Integer> emptyBlocksIndexes = new ArrayList<>();
            emptyBlocksIndexes.add(firstEmptyOverflowBlockIndex);
            for (int i = 0; i < emptyOverflowBlocks.size() - 1; i++) {
                emptyBlocksIndexes.add(emptyOverflowBlocks.get(i).getNextBlock());
            }
            overflowFile.addToEmptyBlocks(emptyBlocksIndexes);
        }
    }

    private int insertRecordsToChain(ArrayList<T> records, HashBlock<T> block, ArrayList<LinkedBlock<T>> overflowBlocks) {
        block.setRecordCount(0);
        block.setOverflowBlockCount(0);
        int r = 0;

        // main block
        while (r < records.size() && block.isPartiallyEmpty()) {
            block.addRecord(records.get(r++));
        }

        int lastUsed = -1;
        int i = 0;

        // overflow blocks
        while (r < records.size() && i < overflowBlocks.size()) {
            LinkedBlock<T> ob = overflowBlocks.get(i);

            while (r < records.size() && ob.isPartiallyEmpty()) {
                ob.addRecord(records.get(r++));
            }

            if (!ob.isEmpty()) {
                lastUsed = i;
            }

            i++;
        }

        if (r < records.size()) {
            throw new IllegalStateException("Not enough overflow blocks capacity during split");
        }

        block.setRecordCount(records.size());
        block.setOverflowBlockCount(lastUsed + 1); // 0 if last used = -1

        return lastUsed; // -1 = no overflow blocks
    }

    private void insertIntoMainBlockOrOverflow(HashBlock<T> block, T record) {
        if (block == null) {
            return;
        }

        if (block.isPartiallyEmpty()) {
            if (block.addRecord(record)) {
                this.recordCount++;
                block.incrementRecordCount();
            }
        } else {
            if (block.hasNextBlock()) {
                if (overflowFile.insertToChain(block.getNextBlock(), record)) {
                    block.incrementOverflowBlockCount();
                }
            } else {
                int overflowBlockIndex = overflowFile.insertToStart(record);

                block.setNextBlock(overflowBlockIndex);
                block.incrementOverflowBlockCount();
            }
            this.recordCount++;
            block.incrementRecordCount();
        }
    }

    private double calculateDensity() {
        int totalCapacity = blockCount * recordsPerBlock + overflowFile.getCapacity();
        if (totalCapacity == 0) return 0.0;
        return (double) recordCount / totalCapacity;
    }

    @Override
    protected HashBlock<T> createNewBlock() {
        return new HashBlock<>(recordsPerBlock, recordClass);
    }

    @Override
    protected HashBlock<T> loadBlock(int blockIndex) {
        return (HashBlock<T>) super.loadBlock(blockIndex);
    }

    //public for tests
    public int getHashEdge() {
        return (int) Math.pow(2, hashPower) * INITIAL_BLOCK_COUNT;
    }

    //public for tests
    public int getSplitPointer() {
        return splitPointer;
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

    private void saveHashFile() throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(hashFileName))) {
            dos.writeInt(this.hashPower);
            dos.writeInt(this.splitPointer);
            dos.writeInt(this.recordCount);
        }
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

        System.out.println();

        // main blocks
        for (int i = 0; i < blockCount; i++) {
            HashBlock<T> block = loadBlock(i);
            if (block != null) {
                System.out.println("Hash Block #" + i + ":");
                System.out.println(block.toString());
                System.out.println();
            }
        }

        System.out.println("=== END OF HASH FILE ===");

        // overflow file
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

        // main blocks
        for (int i = 0; i < blockCount; i++) {
            HashBlock<T> block = loadBlock(i);
            if (block != null) {
                sb.append("Hash Block #").append(i).append(":\n");
                sb.append(block.toString()).append("\n");
            }
        }

        sb.append("=== END OF HASH FILE ===\n\n");

        // overflow file
        sb.append(overflowFile.toString());

        return sb.toString();
    }

    public void printStatistics() {
        System.out.println("=== HASH FILE STATISTICS ===");
        System.out.println("Total records: " + recordCount);
        System.out.println("Main blocks: " + blockCount);
        System.out.println("Overflow blocks: " + overflowFile.getBlockCount());
        System.out.println("Hash power: " + hashPower);
        System.out.println("Split pointer: " + splitPointer);
        System.out.println("Load factor: " + String.format("%.2f", calculateDensity()));

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

        System.out.println("Chains with overflow: " + chainsWithOverflow);
        System.out.println("Max chain length: " + maxChainLength);

        System.out.println("=============================");
    }

    public int getRecordCount() {
        return recordCount;
    }

    @Override
    public void truncateEmptyBlocksAtEnd() {
        //empty for safety
    }
}