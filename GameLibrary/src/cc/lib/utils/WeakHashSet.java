package cc.lib.utils;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cc.lib.game.Utils;

/**
 * Created by Chris Caron on 7/23/21.
 */
public class WeakHashSet<E> implements Set<E> {

    private final static class MyWeakReference<T> extends WeakReference<T> {

        final int hash;
        public MyWeakReference(T referent) {
            super(referent);
            hash = referent.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (obj instanceof MyWeakReference) {
                return Utils.isEquals(get(), ((MyWeakReference)obj).get());
            }

            return Utils.isEquals(get(), obj);
        }
    }

    private HashSet<MyWeakReference<E>> backingSet = new HashSet<>();

    @Override
    public int size() {
        purge();
        return backingSet.size();
    }

    @Override
    public boolean isEmpty() {
        purge();
        return backingSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backingSet.contains(new MyWeakReference<>(o));
    }

    @Override
    public Iterator<E> iterator() {

        purge();
        return new MyIterator(backingSet.iterator(), this);
    }

    private static class MyIterator<E> implements Iterator<E> {

        final Iterator<WeakReference<E>> backingIt;
        WeakReference<E> last = null;
        final WeakHashSet whs;

        MyIterator(Iterator<WeakReference<E>> backingIt, WeakHashSet whs) {
            this.backingIt = backingIt;
            this.whs = whs;
        }

        @Override
        public boolean hasNext() {
            return backingIt.hasNext();
        }

        @Override
        public E next() {
            last=backingIt.next();
            return last.get();
        }

        @Override
        public void remove() {
            backingIt.remove();
            if (last != null) {
                whs.remove(last);
                last = null;
            }
        }
    }


    @Override
    public Object[] toArray() {
        int size = size();
        Object [] a = backingSet.toArray();
        E [] r = (E[])java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), size);
        int idx = 0;
        for (Object o : a) {
            MyWeakReference<E> wr = (MyWeakReference)o;
            E item = wr.get();
            r[idx++] = item;
        }
        return r;
    }

    @Override
    public <E> E[] toArray(E[] a) {
        int size = size();
        MyWeakReference<E> [] weakArr = backingSet.toArray(new MyWeakReference[a.length]);
        E [] r = a.length >= size ? a :
                (E[])java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), size);
        int idx = 0;
        for (MyWeakReference<E> i : weakArr) {
            r[idx++] = i.get();
        }

        return r;
    }

    @Override
    public boolean add(E t) {
        return backingSet.add(new MyWeakReference<>(t));
    }

    @Override
    public boolean remove(Object o) {
        return backingSet.remove(new MyWeakReference<>(o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingSet.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E i : c) {
            if (backingSet.add(new MyWeakReference<>(i)))
                changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return backingSet.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return backingSet.removeAll(c);
    }

    @Override
    public void clear() {
        backingSet.clear();
    }

    private void purge() {
        for (Iterator<MyWeakReference<E>> it = backingSet.iterator(); it.hasNext(); ) {
            if (it.next().get() ==  null)
                it.remove();
        }
    }

}
