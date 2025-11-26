package hashfile;

import heapfile.IRecord;

import java.io.*;

public class HashBlock<T extends IRecord<T>> extends LinkedBlock<T> {
    private int recordCount;
    private int overflowBlockCount;

    public HashBlock(int capacity, Class<T> recordClass) {
        super(capacity, recordClass);
        this.recordCount = 0;
        this.overflowBlockCount = 0;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public void incrementRecordCount() {
        this.recordCount++;
    }

    public void decrementRecordCount() {
        if (this.recordCount > 0) {
            this.recordCount--;
        }
    }

    public void resetRecordCount() {
        this.recordCount = 0;
    }

    @Override
    public int getSize() {
        try {
            // LinkedBlock + recordCount
            return super.getSize() + Integer.BYTES;
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

            // recordCount
            hlpOutStream.writeInt(recordCount);

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }

        return hlpByteArrayOutputStream.toByteArray();
    }

    @Override
    public HashBlock<T> fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);

        try {
            byte[] superBytes = new byte[super.getSize()];
            hlpInStream.readFully(superBytes);
            super.fromBytes(superBytes);

            // recordCount
            this.recordCount = hlpInStream.readInt();

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }

        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(String.format("Total records in chain: %d\n", recordCount));
        return sb.toString();
    }

    public int getOverflowBlockCount() {
        return overflowBlockCount;
    }
}