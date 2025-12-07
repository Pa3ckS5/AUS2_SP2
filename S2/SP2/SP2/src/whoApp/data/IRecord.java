package whoApp.data;

public interface IRecord<T> extends IBinarySerializable<T> {
    boolean isEqualTo(T other);
    T createClass();
}
