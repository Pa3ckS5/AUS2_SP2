package heapfile;

public class HeapFile<T extends IRecord<T>> {
    private String fileName;
    private int clusterSize;

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
