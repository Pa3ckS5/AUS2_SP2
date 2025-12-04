package file.heapfile;

import file.IBinarySerializable;
import file.IRecord;

import java.io.*;
import java.util.ArrayList;

public class Block<T extends IRecord<T>> implements IBinarySerializable<Block<T>> {
    protected int capacity;
    protected ArrayList<T> records;
    protected int validCount;
    protected Class<T> recordClass;

    public Block(int capacity, Class<T> recordClass) {
        this.recordClass = recordClass;
        this.records = new ArrayList<>(capacity);
        this.capacity = capacity;
        this.validCount = 0;

        for (int i = 0; i < capacity; i++) {
            try {
                records.add(recordClass.newInstance().createClass());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error creating record instance", e);
            }
        }
    }

    public boolean addRecord(T record) {
        if (isPartiallyEmpty()) {
            records.set(validCount, record);
            validCount++;
            return true;
        }
        return false;
    }

    public boolean removeRecord(T record) {
        if (!isEmpty()) {
            for (int i = 0; i < validCount; i++) {
                T current = records.get(i);
                if (current != null && current.isEqualTo(record)) {
                    if (i != validCount - 1) {
                        records.set(i, records.get(validCount - 1));
                    }
                    validCount--;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean editRecord(T editedRecord) {
        if (!isEmpty()) {
            for (int i = 0; i < validCount; i++) {
                T current = records.get(i);
                if (current != null && current.isEqualTo(editedRecord)) {
                    records.set(i, editedRecord);
                    return true;
                }
            }
        }
        return false;
    }

    public T getRecord(int index) {
        if (index < 0 || index >= validCount) {
            return null;
        }
        return records.get(index);
    }

    public T getRecord(T  record) {
        for (int i = 0; i < validCount; i++) {
            T current = records.get(i);
            if (current != null && current.isEqualTo(record)) {
                return current;
            }
        }
        return null;
    }

    public ArrayList<T> getRecords() {
        ArrayList<T> r =  new ArrayList<>(validCount);
        for (int i = 0; i < validCount; i++) {
            r.add(records.get(i));
        }
        return r;
    }

    public ArrayList<T> clear() {
        ArrayList<T> r =  getRecords();
        validCount = 0;
        return r;
    }

    public int getValidCount() {
        return validCount;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getSize() {
        try {
            //validCount + records
            return Integer.BYTES + recordClass.newInstance().getSize() * capacity;
        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("Cannot get record size");;
        }
        return -1;
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            // validCount
            hlpOutStream.writeInt(validCount);

            // records
            for (T record : records) {
                hlpOutStream.write(record.getBytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }

        return hlpByteArrayOutputStream.toByteArray();
    }

    @Override
    public Block<T> fromBytes(byte[] bytes) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);

        try {
            T sampleRecord = recordClass.newInstance().createClass();
            int recordSize = sampleRecord.getSize();

            // validCount
            this.validCount = hlpInStream.readInt();

            // records
            for (int i = 0; i < capacity; i++) {
                byte[] recordBytes = new byte[recordSize];
                int bytesRead = hlpInStream.read(recordBytes);
                if (bytesRead == recordSize) {
                    records.get(i).fromBytes(recordBytes);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error creating record instance.", e);
        }

        return this;
    }

    @Override
    public String toString() {
        String status = "unknown";
        if (isEmpty()) {
            status = "empty";
        } else if (isPartiallyEmpty()) {
            status = "partially empty";
        } else if (isFull()) {
            status = "full";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- %s (valid=%d, capacity=%d)\n", status, validCount, capacity));
        for (int i = 0; i < capacity; i++) {
            T record = records.get(i);
            if (i < validCount) {
                sb.append(String.format("  [%d]: %s\n", i, record != null ? record.toString() : "EMPTY"));
            } else {
                sb.append(String.format("  [%d]: EMPTY\n", i));
            }
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return validCount == 0;
    }

    public boolean isPartiallyEmpty() {
        return validCount < capacity;
    }

    public boolean isFull() {
        return validCount == capacity;
    }
}