package heapfile;

public interface IRecord<T> extends IBinarySerializable<T> {
    boolean isEqualTo(T other);
}
