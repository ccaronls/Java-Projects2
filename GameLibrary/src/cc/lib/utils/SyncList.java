package cc.lib.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by chriscaron on 3/17/18.
 */

public class SyncList<T> implements List<T> {

    private final List<T> backingList;

    public SyncList() {
        backingList = new ArrayList<>();
    }

    public SyncList(List<T> list) {
        this.backingList = list;
    }

    @Override
    public synchronized int size() {
        return backingList.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return backingList.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return backingList.contains(o);
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return backingList.iterator();
    }

    @Override
    public synchronized Object[] toArray() {
        return backingList.toArray();
    }

    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return backingList.toArray(a);
    }

    @Override
    public synchronized boolean add(T t) {
        return backingList.add(t);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return backingList.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return backingList.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        return backingList.addAll(c);
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends T> c) {
        return backingList.addAll(index, c);
    }

    @Override
    public  synchronized boolean removeAll(Collection<?> c) {
        return backingList.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return backingList.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        backingList.clear();
    }

    @Override
    public synchronized T get(int index) {
        return backingList.get(index);
    }

    @Override
    public synchronized T set(int index, T element) {
        return backingList.set(index, element);
    }

    @Override
    public synchronized void add(int index, T element) {
        backingList.add(index, element);
    }

    @Override
    public synchronized T remove(int index) {
        return backingList.remove(index);
    }

    @Override
    public synchronized int indexOf(Object o) {
        return backingList.indexOf(o);
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return backingList.lastIndexOf(o);
    }

    @Override
    public synchronized ListIterator<T> listIterator() {
        return backingList.listIterator();
    }

    @Override
    public synchronized ListIterator<T> listIterator(int index) {
        return backingList.listIterator();
    }

    @Override
    public synchronized List<T> subList(int fromIndex, int toIndex) {
        return backingList.subList(fromIndex, toIndex);
    }
}
