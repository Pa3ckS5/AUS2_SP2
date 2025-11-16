package heapfile;

import java.util.ArrayList;
import java.util.LinkedList;

public class HeapFile<T extends IRecord<T>> {
    private String fileName;
    private int clusterSize;
    private ArrayList<Block<T>> blocks;
    private LinkedList<Integer> emptyBlocks;
    private LinkedList<Integer> partiallyEmptyBlocks;


    public HeapFile(String fileName, int clusterSize) {
        this.fileName = fileName;
        this.clusterSize = clusterSize;
    }

    public int insert(T data) {
        return 0;
    }

    public T get(int index, T data) {
        return null;
    }

    protected Block<T> get(int blockIndex) {
        return null;
    }

    public boolean delete(int blockIndex, T data) {
        return false;
    }
}
