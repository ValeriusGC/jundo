package undomodel;

@FunctionalInterface
public interface UndoSetter<V> {
    void set(V value);
}
