package whoApp.data;

public interface IBinarySerializable<T> {
    int getSize();
    byte[] getBytes();
    T fromBytes(byte[] bytes);
}