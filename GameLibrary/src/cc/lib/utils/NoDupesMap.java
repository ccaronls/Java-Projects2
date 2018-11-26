package cc.lib.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by chriscaron on 3/11/18.
 *
 * Will assert if try to add a duplicate key
 */

public class NoDupesMap<K,V> implements Map<K,V> {

    final Map<K,V> backingMap;

    public NoDupesMap(Map<K,V> map) {
        this.backingMap = map;
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backingMap.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return backingMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        if (backingMap.containsKey(key) && backingMap.get(key) != value)
            throw new IllegalArgumentException("Key '" + key + " is already mapped to a value");
        return backingMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return backingMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        backingMap.putAll(m);
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return backingMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return backingMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return backingMap.entrySet();
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        for (K key : backingMap.keySet()) {
            b.append(key).append("=").append(backingMap.get(key)).append("\n");
        }
        return b.toString();
    }
}
