package cc.lib.utils;

public class DoubleLinkedList {

    public static interface DNode {

    }

    int num;
    DNode first, last;

    public void insertFront(DNode node) {
        if (first == null) {
            first = node;
        } else {

        }
    }

    public void insertLast(DNode node) {

    }

    public void remove(DNode node) {

    }

    public int size() {
        return num;
    }
}
