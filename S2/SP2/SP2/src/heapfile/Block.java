package heapfile;

import java.io.*;
import java.util.ArrayList;
import java.util.BitSet;

public class Block<T extends IRecord<T>> implements IBinarySerializable<Block<T>> {
    private int capacity;
    private ArrayList<T> records;
    private final BitSet validRecords;
    private int validCount;
    private Class<T> recordClass;

    public Block(int capacity, Class<T> recordClass) {
        this.recordClass = recordClass;
        this.records = new ArrayList<>(capacity);
        this.capacity = capacity;
        validRecords = new BitSet(capacity);
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
            for (int i = 0; i < capacity; i++) {
                if (!validRecords.get(i)) {
                    records.set(i, record);
                    validRecords.set(i);
                    validCount++;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeRecord(T record) {
        if (!isEmpty()) {
            for (int i = 0; i < validCount; i++) {
                T current = records.get(i);
                if (current != null && current.isEqualTo(record)) {
                    validRecords.clear(i);
                    validCount--;
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
        for (int i = 0; i < capacity; i++) {
            T current = records.get(i);
            if (validRecords.get(i) && current != null && current.equals(record)) {
                return current;
            }
        }
        return null;
    }

    public ArrayList<T> getRecords() {
        return records;
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
            int bitmapSize = (int) Math.ceil(capacity / 8.0);
            //validCount + capacity + bitmap + records
            return Integer.BYTES + Integer.BYTES + bitmapSize + recordClass.newInstance().getSize() * capacity;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);

        try {
            // Zápis validCount
            hlpOutStream.writeInt(validCount);

            // Zápis bitmapy validRecords
            byte[] bitmapBytes = validRecords.toByteArray();
            // Zápis dĺžky bitmapy
            hlpOutStream.writeInt(bitmapBytes.length);
            // Zápis bitmapy
            hlpOutStream.write(bitmapBytes);

            // Zápis záznamov
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

            // Načítanie validCount
            this.validCount = hlpInStream.readInt();

            // Načítanie bitmapy
            int bitmapLength = hlpInStream.readInt();
            byte[] bitmapBytes = new byte[bitmapLength];
            hlpInStream.readFully(bitmapBytes);
            validRecords.clear();
            validRecords.or(BitSet.valueOf(bitmapBytes));

            // Načítanie všetkých záznamov
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
        sb.append(String.format("- %s (valid=%d, capacity=%d) %s\n", status, validCount, capacity, bitmapToString()));
        for (int i = 0; i < capacity; i++) {
            T record = records.get(i);
            if (validRecords.get(i)) {
                sb.append(String.format("  [%d]: %s\n", i, record != null ? record.toString() : "EMPTY"));
            } else {
                sb.append(String.format("  [%d]: EMPTY\n", i));
            }
        }
        return sb.toString();
    }

    private String bitmapToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < capacity; i++) {
            sb.append(validRecords.get(i) ? "1" : "0");
            if (i < capacity - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
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