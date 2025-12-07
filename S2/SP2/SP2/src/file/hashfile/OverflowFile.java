package file.hashfile;

import whoApp.data.IRecord;
import file.heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class OverflowFile<T extends IRecord<T>> extends HeapFile<T> {

    public OverflowFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        super(fileName, blockSize, recordClass);
        this.recordsPerBlock = (blockSize - 2 * Integer.BYTES) / recordSize; // validCount, nextBlock
    }

    public int insertToStart(T record) {

        if (!emptyBlocks.isEmpty()) {

            int firstEmptyBlockIndex = emptyBlocks.findMin();
            LinkedBlock<T> emptyBlock = createNewBlock();

            if (emptyBlock != null && emptyBlock.addRecord(record)) {
                saveBlockToFile(firstEmptyBlockIndex, emptyBlock);

                emptyBlocks.remove(firstEmptyBlockIndex);

                return firstEmptyBlockIndex;
            }
        }

        // new block at end
        int newBlockIndex = blockCount;
        LinkedBlock<T> newBlock = createNewBlock();

        if (newBlock.addRecord(record)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;

            return newBlockIndex;
        }

        return -1; // error
    }

    public boolean insertToChain(int startBlockIndex, T record) {
        if (startBlockIndex < blockCount) {
            int currentBlockIndex = startBlockIndex;
            int previousBlockIndex = -1;

            while (currentBlockIndex != -1) {
                LinkedBlock<T> block = loadBlock(currentBlockIndex);

                if (block == null) {
                    return false;
                }

                if (block.addRecord(record)) {
                    saveBlockToFile(currentBlockIndex, block);
                    emptyBlocks.remove(Integer.valueOf(currentBlockIndex));

                    return false;
                }

                previousBlockIndex = currentBlockIndex;
                currentBlockIndex = block.getNextBlock();
            }

            createNewBlockInChain(record, previousBlockIndex);
            return true;
        } else {

            System.out.println("Error in insertToChain - index out of bounds");
            return false;
        }
    }

    public void addToEmptyBlocks(ArrayList<Integer> emptyBlocksIndexes) {
        for (Integer index : emptyBlocksIndexes) {
            if (index >= 0 && index < blockCount) {
                saveBlockToFile(index, createNewBlock());
                emptyBlocks.insert(index);
            }
        }
    }

    public void editBlockChain(int startBlockIndex, ArrayList<LinkedBlock<T>> linkedBlocks) {
        if (startBlockIndex < 0 || startBlockIndex >= blockCount) {
            return;
        }

        int currentBlockIndex = startBlockIndex;

        for (LinkedBlock<T> block : linkedBlocks) {
            saveBlockToFile(currentBlockIndex, block);
            currentBlockIndex = block.getNextBlock();
        }
    }

    @Override
    public T get(int startBlockIndex, T record) {
        int currentBlockIndex = startBlockIndex;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> block = loadBlock(currentBlockIndex);
            if (block == null) {
                return null;
            }

            T foundRecord = block.getRecord(record);
            if (foundRecord != null) {
                return foundRecord;
            }

            currentBlockIndex = block.getNextBlock();
        }

        return null;
    }

    @Override
    public boolean delete(int startBlockIndex, T record) {
        int currentBlockIndex = startBlockIndex;
        boolean removed = false;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> block = loadBlock(currentBlockIndex);
            if (block == null) {
                break;
            }

            if (block.deleteRecord(record)) {
                removed = true;
                saveBlockToFile(currentBlockIndex, block);
            }

            currentBlockIndex = block.getNextBlock();
        }

        return removed;
    }

    public boolean edit(int startBlockIndex, T editedRecord) {
        int currentBlockIndex = startBlockIndex;
        boolean edited = false;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> block = loadBlock(currentBlockIndex);
            if (block == null) {
                break;
            }

            if (block.editRecord(editedRecord)) {
                edited = true;
                saveBlockToFile(currentBlockIndex, block);
            }

            currentBlockIndex = block.getNextBlock();
        }

        return edited;
    }

    public ArrayList<LinkedBlock<T>> getBlockChain(int startBlockIndex) {
        ArrayList<LinkedBlock<T>> linkedBlocks = new ArrayList<>();

        if (startBlockIndex < 0 || startBlockIndex >= blockCount) {
            return linkedBlocks;
        }

        int currentBlockIndex = startBlockIndex;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> currentBlock = loadBlock(currentBlockIndex);
            if (currentBlock == null) {
                break;
            }

            linkedBlocks.add(currentBlock);
            currentBlockIndex = currentBlock.getNextBlock();;
        }

        return linkedBlocks;
    }

    public int getCapacity() {
        return (this.blockCount - emptyBlocks.getNodeCount()) * this.recordsPerBlock;
    }

    @Override
    protected LinkedBlock<T> createNewBlock() {
        return new LinkedBlock<>(recordsPerBlock, recordClass);
    }

    private int createNewBlockInChain(T record, int lastBlockIndex) {

        if (!emptyBlocks.isEmpty()) {
            int firstEmptyBlockIndex = emptyBlocks.findMin();

            LinkedBlock<T> emptyBlock = createNewBlock();

            if (emptyBlock != null && emptyBlock.addRecord(record)) {

                emptyBlock.setNextBlock(-1);

                // new link
                if (lastBlockIndex != -1) {
                    LinkedBlock<T> lastBlock = loadBlock(lastBlockIndex);
                    lastBlock.setNextBlock(firstEmptyBlockIndex);
                    saveBlockToFile(lastBlockIndex, lastBlock);
                }


                saveBlockToFile(firstEmptyBlockIndex, emptyBlock);
                emptyBlocks.remove(firstEmptyBlockIndex);

                return firstEmptyBlockIndex;
            }
        }

        int newBlockIndex = blockCount;
        LinkedBlock<T> newBlock = createNewBlock();

        if (newBlock.addRecord(record)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;

            if (lastBlockIndex != -1) {
                LinkedBlock<T> lastBlock = loadBlock(lastBlockIndex);
                lastBlock.setNextBlock(newBlockIndex);
                saveBlockToFile(lastBlockIndex, lastBlock);
            }

            return newBlockIndex;
        }

        return -1;
    }

    @Override
    protected LinkedBlock<T> loadBlock(int blockIndex) {
        return (LinkedBlock<T>) super.loadBlock(blockIndex);
    }

    @Override
    public void printAllBlocks() {
        System.out.println("=== OVERFLOW FILE ===");
        System.out.println("File: " + fileName);
        System.out.println("Block size: " + blockSize);
        System.out.println("Records per block: " + recordsPerBlock);
        System.out.println("Empty blocks: " + emptyBlocks);
        //System.out.println("Partially empty blocks: " + partiallyEmptyBlocks);
        System.out.println("Total blocks in file: " + getBlockCount());
        System.out.println();

        for (int i = 0; i < getBlockCount(); i++) {
            LinkedBlock<T> block = loadBlock(i);
            if (block != null) {
                System.out.println("Block #" + i + ":");
                System.out.println(block.toString());
                System.out.println();
            }
        }

        System.out.println("=== END OF OVERFLOW FILE ===");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== OVERFLOW FILE ===\n");
        sb.append("File: ").append(fileName).append("\n");
        sb.append("Block size: ").append(blockSize).append("\n");
        sb.append("Records per block: ").append(recordsPerBlock).append("\n");
        sb.append("Empty blocks: ").append(emptyBlocks).append("\n");
        //sb.append("Partially empty blocks: ").append(partiallyEmptyBlocks).append("\n");
        sb.append("Total blocks in file: ").append(getBlockCount()).append("\n\n");

        for (int i = 0; i < getBlockCount(); i++) {
            LinkedBlock<T> block = loadBlock(i);
            if (block != null) {
                sb.append("Block #").append(i).append(":\n");
                sb.append(block.toString()).append("\n");
            }
        }

        sb.append("=== END OF OVERFLOW FILE ===");

        return sb.toString();
    }
}