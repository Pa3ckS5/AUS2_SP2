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
        // Hľadanie prvého prázdneho bloku (najmenší index v zozname emptyBlocks)
        if (!emptyBlocks.isEmpty()) {
            // Získame a zoradíme prázdne bloky podľa indexu (od najmenšieho)
            emptyBlocks.sort(Integer::compareTo);
            int firstEmptyBlockIndex = emptyBlocks.getFirst();

            // Načítame prázdny blok
            LinkedBlock<T> emptyBlock = loadBlock(firstEmptyBlockIndex);

            if (emptyBlock != null && emptyBlock.addRecord(record)) {
                // Uložíme blok s novým záznamom
                saveBlockToFile(firstEmptyBlockIndex, emptyBlock);

                // Aktualizujeme zoznamy blokov
                emptyBlocks.remove(Integer.valueOf(firstEmptyBlockIndex));

                if (emptyBlock.isPartiallyEmpty()) {
                    partiallyEmptyBlocks.add(firstEmptyBlockIndex);
                }

                return firstEmptyBlockIndex;
            }
        }

        // Ak nie je žiadny voľný prázdny blok, vytvoríme nový na konci súboru
        int newBlockIndex = blockCount;
        LinkedBlock<T> newBlock = (LinkedBlock<T>) createNewBlock();

        if (newBlock.addRecord(record)) {
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;

            // Aktualizujeme zoznamy
            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }

            return newBlockIndex;
        }

        return -1; // Chyba pri vkladaní
    }

    public int insertToChain(int firstBlockIndex, T record) {
        if (firstBlockIndex < blockCount) {
            int currentBlockIndex = firstBlockIndex;

            while (currentBlockIndex != -1) {
                LinkedBlock<T> block = loadBlock(currentBlockIndex);

                if (block == null) {
                    return -1;
                }

                // Skús vložiť záznam do aktuálneho bloku
                if (block.addRecord(record)) {
                    saveBlockToFile(currentBlockIndex, block);

                    // Aktualizujte zoznamy prázdnych a čiastočne prázdnych blokov
                    updateBlockLists(currentBlockIndex, block);

                    return currentBlockIndex;
                }

                // Ak je blok plný, presuňte sa na ďalší blok v reťazci
                currentBlockIndex = block.getNextBlock();
            }

            // Ak sme prešli celý reťazec a nenašli sme miesto, vytvorte nový blok
            return createNewBlockInChain(record, firstBlockIndex);
        } else  {
            // vytvorenie - novy blok na danom indexe a zapisanie do suboru
            return createNewBlockAtSpecificIndex(record, firstBlockIndex);
        }



    }

    private int createNewBlockAtSpecificIndex(T record, int targetBlockIndex) {
        // Ak je targetBlockIndex väčší ako aktuálny počet blokov, musíme vytvoriť medzibloky
        while (blockCount <= targetBlockIndex) {
            if (blockCount == targetBlockIndex) {
                // Vytvoríme cieľový blok
                LinkedBlock<T> newBlock = (LinkedBlock<T>) createNewBlock();

                if (newBlock.addRecord(record)) {
                    saveBlockToFile(targetBlockIndex, newBlock);
                    blockCount++;

                    // Aktualizujte zoznamy
                    if (!newBlock.isFull()) {
                        partiallyEmptyBlocks.add(targetBlockIndex);
                    }

                    return targetBlockIndex;
                } else {
                    return -1; // Chyba pri vkladaní
                }
            } else {
                // Vytvoríme prázdny medziblok
                LinkedBlock<T> emptyBlock = (LinkedBlock<T>) createNewBlock();
                saveBlockToFile(blockCount, emptyBlock);
                blockCount++;

                // Pridáme do zoznamu prázdnych blokov
                emptyBlocks.add(blockCount - 1);
            }
        }

        return -1; // Nemalo by nastať
    }

    private int createNewBlockInChain(T record, int chainStartIndex) {
        // Nájdite koniec reťazca
        int lastBlockIndex = findLastBlockInChain(chainStartIndex);

        // Vytvorte nový blok
        int newBlockIndex = blockCount;
        LinkedBlock<T> newBlock = (LinkedBlock<T>) createNewBlock();

        if (newBlock.addRecord(record)) {
            // Uložte nový blok
            saveBlockToFile(newBlockIndex, newBlock);
            blockCount++;

            // Aktualizujte zoznamy
            if (!newBlock.isFull()) {
                partiallyEmptyBlocks.add(newBlockIndex);
            }

            // Prepojte predchádzajúci blok na nový blok
            if (lastBlockIndex != -1) {
                LinkedBlock<T> lastBlock = loadBlock(lastBlockIndex);
                lastBlock.setNextBlock(newBlockIndex);
                saveBlockToFile(lastBlockIndex, lastBlock);
            }

            return newBlockIndex;
        }

        return -1;
    }

    private int findLastBlockInChain(int startBlockIndex) {
        int currentBlockIndex = startBlockIndex;
        int lastBlockIndex = -1;

        while (currentBlockIndex != -1) {
            LinkedBlock<T> block = loadBlock(currentBlockIndex);
            if (block == null) {
                break;
            }

            lastBlockIndex = currentBlockIndex;
            currentBlockIndex = block.getNextBlock();
        }

        return lastBlockIndex;
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

                // Aktualizujte zoznamy blokov
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
            currentBlock.clearNextBlock();

            saveBlockToFile(currentBlockIndex, currentBlock);
            emptyBlocks.add(currentBlockIndex);
            partiallyEmptyBlocks.remove(Integer.valueOf(currentBlockIndex));

            currentBlockIndex = nextBlockIndex;
        }

        return linkedRecords;
    }

    private void updateBlockLists(int blockIndex, LinkedBlock<T> block) {
        // Odstráňte blok zo zoznamu prázdnych blokov, ak tam bol
        emptyBlocks.remove(Integer.valueOf(blockIndex));

        // Aktualizujte zoznam čiastočne prázdnych blokov
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

    // Pomocná metóda na získanie celého reťazca blokov
    public LinkedList<Integer> getBlockChain(int startBlockIndex) {
        LinkedList<Integer> chain = new LinkedList<>();
        int currentBlockIndex = startBlockIndex;

        while (currentBlockIndex != -1) {
            chain.add(currentBlockIndex);
            LinkedBlock<T> block = loadBlock(currentBlockIndex);
            if (block == null) {
                break;
            }
            currentBlockIndex = block.getNextBlock();
        }

        return chain;
    }

    // Pomocná metóda na zistenie dĺžky reťazca
    public int getChainLength(int startBlockIndex) {
        return getBlockChain(startBlockIndex).size();
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