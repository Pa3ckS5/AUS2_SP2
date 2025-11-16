package heapfile;

import java.io.*;
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
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try {
            for (T record : records) {
                hlpOutStream.write(record.getBytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }

        return hlpByteArrayOutputStream.toByteArray();
    }

    @Override
    public T fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);


        return null;
    }
}
