package file.overflowfile;

import file.IRecord;
import file.heapfile.HeapFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;

public class OverflowFile<T extends IRecord<T>> extends HeapFile<T> {

    public OverflowFile(String fileName, int blockSize, Class<T> recordClass) throws FileNotFoundException {
        super(fileName, blockSize, recordClass);
        this.recordsPerBlock = (blockSize - 2 * Integer.BYTES) / recordSize; // validCount, nextBlock
    }

    @Override
    protected LinkedBlock<T> createNewBlock() {
        return new LinkedBlock<>(recordsPerBlock, recordClass);
    }

    public int insertToFirst(T record) {

        if (!emptyBlocks.isEmpty()) {

            emptyBlocks.sort(Integer::compareTo);
            int firstEmptyBlockIndex = emptyBlocks.getFirst();

            LinkedBlock<T> emptyBlock = loadBlock(firstEmptyBlockIndex);

            if (emptyBlock != null && emptyBlock.addRecord(record)) {
                saveBlockToFile(firstEmptyBlockIndex, emptyBlock);

                emptyBlocks.removeFirst();

                if (emptyBlock.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.add(firstEmptyBlockIndex);
                }

                return firstEmptyBlockIndex;
            }
        }

        // new block at end
        int newBlockIndex = blockCount;
        LinkedBlock<T> newBlock = createNewBlock();

        if (newBlock.addRecord(record)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;

            // Aktualizujeme zoznamy
            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }

            return newBlockIndex;
        }

        return -1; // error
    }

    public boolean insertToChain(int firstBlockIndex, T record) {
        if (firstBlockIndex < blockCount) {
            int currentBlockIndex = firstBlockIndex;
            int previousBlockIndex = -1;

            while (currentBlockIndex != -1) {
                LinkedBlock<T> block = loadBlock(currentBlockIndex);

                if (block == null) {
                    return false;
                }

                if (block.addRecord(record)) {
                    saveBlockToFile(currentBlockIndex, block);
                    updateBlockLists(currentBlockIndex, block);

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

    public int getCapacity() {
        return (this.blockCount - emptyBlocks.size()) * this.recordsPerBlock;
    }


    private int createNewBlockInChain(T record, int lastBlockIndex) {

        if (!emptyBlocks.isEmpty()) {
            emptyBlocks.sort(Integer::compareTo);
            int firstEmptyBlockIndex = emptyBlocks.getFirst();

            LinkedBlock<T> emptyBlock = loadBlock(firstEmptyBlockIndex);

            if (emptyBlock != null && emptyBlock.addRecord(record)) {

                // pre istotu zruš starý nextBlock (ak tam nejaký bol)
                emptyBlock.setNextBlock(-1);

                // prepoj predchádzajúci blok reťazca na tento blok
                if (lastBlockIndex != -1) {
                    LinkedBlock<T> lastBlock = loadBlock(lastBlockIndex);
                    lastBlock.setNextBlock(firstEmptyBlockIndex);
                    saveBlockToFile(lastBlockIndex, lastBlock);
                }

                // ulož samotný nový blok
                saveBlockToFile(firstEmptyBlockIndex, emptyBlock);

                // Aktualizuj zoznamy blokov
                emptyBlocks.removeFirst();

                if (emptyBlock.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.add(firstEmptyBlockIndex);
                }

                return firstEmptyBlockIndex;
            }
        }

        int newBlockIndex = blockCount;
        LinkedBlock<T> newBlock = createNewBlock();

        if (newBlock.addRecord(record)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;

            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }

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
    public T get(int blockIndex, T record) {
        int currentBlockIndex = blockIndex;

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
    public boolean delete(int blockIndex, T record) {
        int currentBlockIndex = blockIndex;
        boolean recordDeleted = false;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> block = loadBlock(currentBlockIndex);
            if (block == null) {
                break;
            }

            boolean wasFull = block.isFull();

            if (block.removeRecord(record)) {
                recordDeleted = true;
                saveBlockToFile(currentBlockIndex, block);

                updateBlockListsAfterDelete(currentBlockIndex, block, wasFull);
            }

            currentBlockIndex = block.getNextBlock();
        }
        //no truncate

        return recordDeleted;
    }

    public ArrayList<T> removeLinkedRecords(int startBlockIndex) {
        ArrayList<T> linkedRecords = new ArrayList<>();

        if (startBlockIndex < 0 || startBlockIndex >= blockCount) {
            return linkedRecords;
        }

        int currentBlockIndex = startBlockIndex;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> currentBlock = loadBlock(currentBlockIndex);
            if (currentBlock == null) {
                break;
            }

            int nextBlockIndex = currentBlock.getNextBlock();
            linkedRecords.addAll(currentBlock.clear());

            saveBlockToFile(currentBlockIndex, currentBlock);
            emptyBlocks.add(currentBlockIndex);
            partiallyEmptyBlocks.remove(Integer.valueOf(currentBlockIndex));

            currentBlockIndex = nextBlockIndex;
        }

        //truncateEmptyBlocksAtEnd();
        return linkedRecords;
    }

    private void updateBlockLists(int blockIndex, LinkedBlock<T> block) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));

        if (block.isFull()) {
            partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        } else if (!partiallyEmptyBlocks.contains(blockIndex)) {
            partiallyEmptyBlocks.add(blockIndex);
        }
    }

    private void updateBlockListsAfterDelete(int blockIndex, LinkedBlock<T> block, boolean wasFull) {
        if (block.isEmpty()) {
            partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
            emptyBlocks.add(blockIndex);
        } else if (wasFull && block.isPartiallyEmpty()) {
            if (!partiallyEmptyBlocks.contains(blockIndex)) {
                partiallyEmptyBlocks.add(blockIndex);
            }
        }
    }

    @Override
    public void printAllBlocks() {
        System.out.println("=== OVERFLOW FILE ===");
        System.out.println("File: " + fileName);
        System.out.println("Block size: " + blockSize);
        System.out.println("Records per block: " + recordsPerBlock);
        System.out.println("Empty blocks: " + emptyBlocks);
        System.out.println("Partially empty blocks: " + partiallyEmptyBlocks);
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
        sb.append("Partially empty blocks: ").append(partiallyEmptyBlocks).append("\n");
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