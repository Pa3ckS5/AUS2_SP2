package file.hashfile;

import file.IRecord;
import file.overflowfile.LinkedBlock;

import java.io.*;
import java.util.ArrayList;

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

    public void incrementOverflowBlockCount() {
        this.overflowBlockCount++;
    }

    public void decrementOverflowBlockCount() {
        if (this.overflowBlockCount > 0) {
            this.overflowBlockCount--;
        }
    }

    public void resetOverflowBlockCount() {
        this.overflowBlockCount = 0;
    }

    public void setOverflowBlockCount(int overflowBlockCount) {
        this.overflowBlockCount = overflowBlockCount;
    }

    public void resetRecordCount() {
        this.recordCount = 0;
    }

    @Override
    public int getSize() {
        try {
            // LinkedBlock + recordCount + overflowBlockCount
            return super.getSize() + 2 * Integer.BYTES;
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

            // overflowBlockCount
            hlpOutStream.writeInt(overflowBlockCount);

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

            // overflowBlockCount
            this.overflowBlockCount = hlpInStream.readInt();

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
        sb.append(String.format("Overflow blocks in chain: %d\n", overflowBlockCount));
        return sb.toString();
    }

    public int getOverflowBlockCount() {
        return overflowBlockCount;
    }

    @Override
    public ArrayList<T> clear() {
        ArrayList<T> r =  getRecords();
        validCount = 0;
        recordCount = 0;
        overflowBlockCount = 0;
        clearNextBlock();

        return r;
    }
}