package hashfile;

import heapfile.HeapFile;
import heapfile.IRecord;

import java.io.*;
import java.util.LinkedList;

public class HashFile<T extends IRecord<T>> extends HeapFile<T> {
    private String overflowFileName;
    private RandomAccessFile overflowFile;

    private int splitPointer;
    private int hashEdge;


    public HashFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        super(fileName, blockSize, recordClass);
        // Re-inicializácia zoznamov blokov pre HashFile
        this.emptyBlocks = new LinkedList<>();
        this.partiallyEmptyBlocks = new LinkedList<>();
        loadHeader(); // Znova načíta header, aby sa použili správne zoznamy
    }

    public int hash(int code, int power) {
        return code % ((int) Math.pow(2, power));
    }

    public boolean addHash() {
        return false;
    }

    @Override
    protected HashBlock<T> createNewBlock(int recordsPerBlock, Class<T> recordClass) {
        return new HashBlock<>(recordsPerBlock, recordClass);
    }

    @Override
    protected HashBlock<T> get(int blockIndex) {
        if (currentBlockIndex != blockIndex) {
            saveCurrentAndLoadBlock(blockIndex);
        }
        return (HashBlock<T>) currentBlock;
    }

    public int getAllRecordCount(int startBlockIndex) {
        HashBlock<T> block = get(startBlockIndex);
        if (block != null) {
            return block.getRecordCount();
        }
        return 0;
    }

    @Override
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

            HashBlock<T> block = createNewBlock(recordsPerBlock, recordClass);
            block.fromBytes(blockBytes);
            currentBlock = block;
            currentBlockIndex = blockIndex;
            return true;

        } catch (IOException e) {
            throw new IllegalStateException("Error loading block from file.", e);
        }
    }
}