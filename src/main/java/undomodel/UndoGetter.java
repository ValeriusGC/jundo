package undomodel;

@FunctionalInterface
public interface UndoGetter<V> {
    V get();
}
