package io.sev.queue;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class IntrusiveQueue<T extends IntrusiveQueue.Element<T>> extends AbstractQueue<T> {

    private T head = null;

    private T tail = null;

    private int size;

    public IntrusiveQueue() {}

    public static abstract class Element<E> {
        E next = null;
        E prev = null;
        protected Element() {}
    }

    @Override
    public boolean offer(T el) {
        Objects.requireNonNull(el);
        if(tail == null) {
            head = el;
            tail = el;
        } else {
            tail.next = el;
            el.prev = tail;
            tail = el;
        }
        size++;
        return true;
    }

    public boolean offerFirst(T el) {
        Objects.requireNonNull(el);
        if(head == null) {
            head = el;
            tail = el;
        } else {
            head.prev = el;
            el.next = head;
            head = el;
        }
        size++;
        return true;
    }

    @Override
    public T poll() {
        if(head == null) {
            return null;
        }
        T el = head;
        if(head == tail) {
            tail = null;
        } else {
            head.next.prev = null;
        }
        head = head.next;
        el.next = null;
        size--;
        return el;
    }

    public T pollLast() {
        if(tail == null) {
            return null;
        }
        T el = tail;
        if(head == tail) {
            head = null;
        } else {
            tail.prev.next = null;
        }
        tail = tail.prev;
        el.prev = null;
        size--;
        return el;
    }

    @Override
    public boolean isEmpty() {
        return head == null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public T peek() {
        return head;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            
            T curr = head;

            @Override
            public T next() {
                if(curr == null) {
                    throw new NoSuchElementException();
                }
                T el = curr;
                curr = curr.next;
                return el;
            }

            @Override
            public boolean hasNext() {
                return curr != null;
            }
        };
    }
    
}
