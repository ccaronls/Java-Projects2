package cc.lib.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * A map with fixed capacity. When limit reached the 'oldest' elem is removed
 *
 * @param <K>
 * @param <V>
 */
public final class LRUCache<K, V> implements Map<K, V> {

    private final HashMap<K, V> map = new HashMap<>();
    private LinkedList<K> list = new LinkedList<>();
    private final int max;

    public LRUCache(int max) {
        this.max = max;
    }

    @Override
    public int size() {
        if (map.size() != list.size())
            throw new AssertionError();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V value = map.get(key);
        if (value == null)
            return null;

        if (!list.remove(key))
            throw new AssertionError("");
        list.addFirst((K)key);
        return value;
    }

    @Override
    public V put(K key, V value) {

        if (map.containsKey(key)) {
            map.put(key, value);
            if (!list.remove(key))
                throw new AssertionError();
            list.addFirst(key);
        } else {

            if (list.size() == max && !list.remove(key)) {
                K k = list.removeLast();
                if (k == null)
                    throw new AssertionError();
                if (map.remove(k) == null)
                    throw new AssertionError();
            }
            map.put(key, value);
            list.addFirst(key);
        }
        return value;
    }

    @Override
    public V remove(Object key) {
        V v = map.remove(key);
        if (v != null) {
            list.remove(v);
        }
        return v;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
        list.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public int getMax() {
        return max;
    }

    public K getOldest() {
        return list.getLast();
    }

    public K getNewest() {
        return list.getFirst();
    }

}
