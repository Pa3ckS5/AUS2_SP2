package file.hashfile;

import file.heapfile.Block;
import whoApp.data.IRecord;
import java.io.*;
import java.util.ArrayList;

public class LinkedBlock<T extends IRecord<T>> extends Block<T> {
    protected int nextBlock;

    public LinkedBlock(int capacity, Class<T> recordClass) {
        super(capacity, recordClass);
        this.nextBlock = -1;
    }

    public void setNextBlock(int nextBlock) {
        this.nextBlock = nextBlock;
    }

    @Override
    public int getSize() {
        try {
            return super.getSize() + Integer.BYTES; // nextBlock
        } catch (Exception e) {
            throw new RuntimeException("Error calculating block size", e);
        }
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            byte[] superBytes = super.getBytes();
            hlpOutStream.write(superBytes);

            // nextBlock
            hlpOutStream.writeInt(nextBlock);

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }

        return hlpByteArrayOutputStream.toByteArray();
    }

    @Override
    public LinkedBlock<T> fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);

        try {
            byte[] superBytes = new byte[super.getSize()];
            hlpInStream.readFully(superBytes);
            super.fromBytes(superBytes);

            // nextBlock
            this.nextBlock = hlpInStream.readInt();

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }

        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(String.format("Next block: %d\n", nextBlock));
        return sb.toString();
    }

    public int getNextBlock() {
        return nextBlock;
    }

    public boolean hasNextBlock() {
        return nextBlock != -1;
    }

    public void clearNextBlock() {
        this.nextBlock = -1;
    }

    @Override
    public ArrayList<T> clear() {
        ArrayList<T> r =  super.clear();
        clearNextBlock();
        return r;
    }
}