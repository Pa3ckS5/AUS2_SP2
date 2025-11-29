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
    private static final int INITIAL_BLOCK_COUNT = 2;


    public HashFile(String fileName, int blockSize, int overflowBlockSize, Class<T> recordClass) throws FileNotFoundException {
        super(fileName, blockSize, recordClass);
        this.recordsPerBlock = (blockSize - 4 * Integer.BYTES) / recordSize; // validCount, nextBlock, recordCount, overflowBlockCount

        this.hashPower = 0;
        this.splitPointer = 0;

        // starting 2 empty blocks (M=2)
        for (int i = 0; i < INITIAL_BLOCK_COUNT; i++) {
            saveBlockToFile(blockCount, createNewBlock());
            blockCount++;
        }
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
            block.addRecord(record);
        } else {
            if (block.hasNextBlock()) {
                overflowFile.insertToChain(block.getNextBlock(), record);
            } else {
                block.setNextBlock(overflowFile.insertToFirst(record));
            }
        }

        saveBlockToFile(index, block);
        return index;
    }

    public boolean delete(T record) {
        int index = getHashIndex(record.hashCode());

        HashBlock<T> block = loadBlock(index);
        boolean removed = block.removeRecord(record);
        if(!removed && block.hasNextBlock()) {
            removed = overflowFile.delete(block.getNextBlock(), record);
        }
        saveBlockToFile(index, block);
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
        super.close();
        overflowFile.close();
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

    private boolean increaseHash() {
        saveBlockToFile(blockCount, createNewBlock()); //add new block
        blockCount++;

        HashBlock<T> block = loadBlock(splitPointer);
        ArrayList<T> allRecords = new ArrayList<>();
        allRecords.addAll(clearLinkedBlocks(block));

        splitPointer++;
        if (splitPointer >= getHashEdge()) {
            hashPower++;
            splitPointer = 0;
        }

        for (T record : allRecords) {
            insert(hash(record.hashCode(),  hashPower + 1), record);
        }

        return false;
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

            for (T record : allRecords) {
                insert(getHashIndex(record.hashCode()), record);
            }

            blockCount--;

            return true;
        }
        return false;
    }

    private ArrayList<T> clearLinkedBlocks(HashBlock<T> block) {
        ArrayList<T> r =  new ArrayList<>(block.getRecordCount());
        if (block.hasNextBlock()) {
            r.addAll(overflowFile.removeLinkedRecords(block.getNextBlock()));
        }
        r.addAll(block.clear());
        return r;
    }

    private void checkFilling() {
        //todo
        double fill = 0.0;

        if (fill > 0.8) {
            increaseHash();
        } else if (fill < 0.4) {
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
        return (int) Math.pow(2, hashPower);
    }
}