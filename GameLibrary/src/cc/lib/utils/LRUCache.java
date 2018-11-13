package cc.lib.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A map with fixed capacity. When limit reached the 'oldest' elem is removed
 *
 * @param <K>
 * @param <V>
 */
public final class LRUCache<K, V> implements Map<K, V> {

    public static class LRUEntry<K> {
        LRUEntry<K> prev, next;
        final K key;

        LRUEntry(K key) {
            if (key == null)
                throw new NullPointerException("Key cannot be null");
            this.key = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof LRUEntry) {
                return ((LRUEntry) obj).key.equals(key);
            }
            return key.equals(obj);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return key.toString();
        }
    }

    private final HashMap<LRUEntry<K>, V> map = new HashMap<>();

    private LRUEntry<K> first=null, last=null;

    private final int max;

    public LRUCache(int max) {
        this.max = max;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(new LRUEntry(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V value = map.get(new LRUEntry(key));
        if (value == null)
            return null;

        LRUEntry<K> e = listFindEntry(key);
        // move to front of the list
        listRemove(e);
        first.prev = e;
        e.next = first;
        first = e;

        return value;
    }

    @Override
    public V put(K key, V value) {

        LRUEntry<K> e = new LRUEntry<K>(key);
        listInsertFirst(e);

        map.put(e, value);
        if (map.size() > max) {
            map.remove(last);
            last = last.prev;
            last.next = null;
        }

        return value;
    }

    private void listInsertFirst(LRUEntry<K> e) {
        if (first == null) {
            first = last = e;
        } else {
            e.next = first;
            first.prev = e;
            first = e;
        }
    }

    private void listRemove(LRUEntry<K> e) {
        //System.out.println("listRemove " + e + " first=" + first + " last=" + last + " size=" + size());
        if (first == e) {
            if (first == last) {
                //System.out.println("remove single entry");
                first = last = null;
            } else {
                //System.out.println("remove first entry");
                first = first.next;
                first.prev = null;
            }
        }
        else if (last == e) {
            //System.out.println("remove last entry");
            last = last.prev;
            last.next = null;
        } else {
            //System.out.println("remove middle entry");
            e.prev.next = e.next;
            e.next.prev = e.prev;
        }
        e.next = e.prev = null;

        //if (last.next != null)
        //    throw new AssertionError("");
        //if (first.prev != null)
        //    throw new AssertionError("");
    }

    private LRUEntry<K> listFindEntry(Object key) {
        for (LRUEntry<K> e = first; e !=null; e = e.next) {
            if (e.key.equals(key)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        LRUEntry<K> e = listFindEntry(key);
        if (e != null) {
            listRemove(e);
        }
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        first = last = null;
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> keys = new HashSet<>();
        for (LRUEntry<K> e = first; e!=null; e=e.next) {
            keys.add(e.key);
        }
        return keys;
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Map<K,V> mapCopy=new LinkedHashMap<>();
        for (LRUEntry<K> e = first; e!= null; e=e.next) {
            //System.out.println("e=" + e);
            mapCopy.put(e.key, map.get(e));
        }
        return mapCopy.entrySet();
    }

    public int getMax() {
        return max;
    }

    public LRUEntry<K> getOldest() {
        return last;
    }

    public LRUEntry<K> getNewest() {
        return first;
    }

}
