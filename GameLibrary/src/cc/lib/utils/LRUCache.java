package cc.lib.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A map with fixed capacity. When limit reached the 'oldest' elem is removed.
 * This version optimized for fastest access at expense of memory.
 *
 * @param <K>
 * @param <V>
 */
public final class LRUCache<K, V> implements Map<K, V> {

    /**
     * Double linked list node
     * @param <K>
     * @param <V>
     */
    static class DList<K,V> {
        private final K key;
        private V val;
        private DList<K,V> prev, next;

        public DList(K key, V val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            return key.equals(((DList)obj).key);
        }
    }

    private final HashMap<K, DList<K,V>> map = new HashMap<>();
    private DList<K,V> first, last;
    private final int max;

    public LRUCache(int max) {
        this.max = max;
    }

    private int listCount() {
        int num=0;
        for (DList<K,V> e = first; e!=null; e=e.next) {
            num++;
        }
        return num;
    }

    private void listRemove(DList<K,V> e) {
        if (e==first) {
            if (first == last) {
                first = last = null;
            } else {
                first = first.next;
                first.prev = null;
            }
        } else if (e==last) {
            last = last.prev;
            last.next = null;
        } else {
            e.next.prev = e.prev;
            e.prev.next = e.next;
        }

        e.next=e.prev=null;
    }

    private void listAddFirst(DList<K,V> e) {
        if (first == null) {
            first = last = e;
            e.prev = e.next = null;
        } else {
            e.next = first;
            first.prev = e;
            first = e;
            e.prev = null;
        }
    }

    private void listClear() {
        DList<K,V> e = first;
        while (e != null) {
            DList t = e;
            e = e.next;
            t.prev = t.next = null;
        }
        first = last = null;
    }

    @Override
    public int size() {
//        if (map.size() != listCount())
//            throw new cc.lib.utils.GException();
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
        for (DList<K,V> e = first; e!=null; e=e.next) {
            if (e.val == null) {
                if (value == null)
                    return true;
            } else if (e.val.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        DList<K,V> e = map.get(key);
        if (e == null)
            return null;

        if (e != first) {
            listRemove(e);
            listAddFirst(e);
        }
        return e.val;
    }

    @Override
    public V put(K key, V value) {

        DList<K,V> e = map.get(key);
        if (e != null) {
            e.val = value;
            if (e != first) {
                listRemove(e);
                listAddFirst(e);
            }
        } else {
            e = new DList<>(key, value);
            if (map.size() == max) {
                if (map.remove(last.key)==null)
                    throw new cc.lib.utils.GException();
                listRemove(last);
            }
            map.put(key, e);
            listAddFirst(e);
        }
        return value;
    }

    @Override
    public V remove(Object key) {
        DList<K,V> e = map.remove(key);
        if (e == null)
            return null;

        listRemove(e);
        return e.val;
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
        listClear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>(map.size());
        for (DList<K,V> e = first; e != null; e=e.next) {
            values.add(e.val);
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Map<K,V> tmp = new HashMap<>();
        for (Map.Entry<K, DList<K,V>> e : map.entrySet()) {
            tmp.put(e.getKey(), e.getValue().val);
        }
        return tmp.entrySet();
    }

    /**
     * Return the max number of elements this cache can hold
     *
     * @return
     */
    public int getMax() {
        return max;
    }

    /**
     * Return the oldest key associated with this cache
     * @return
     */
    public K getOldest() {
        return last == null ? null : last.key;
    }

    /**
     * Return the newest key associated with this cache
     * @return
     */
    public K getNewest() {
        return first == null ? null : first.key;
    }

}
