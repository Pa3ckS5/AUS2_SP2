package heapfile;

import java.util.ArrayList;

public class Block<T extends IRecord<T>> implements IBinarySerializable<T> {
    private int adress;
    private int validCount;
    private ArrayList<T> records;

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public T fromBytes(byte[] bytes) {
        return null;
    }
}
