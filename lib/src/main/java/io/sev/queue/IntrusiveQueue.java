package io.sev.queue;

import java.util.Objects;

public class IntrusiveQueue<T extends IntrusiveQueue.Element<T>> {

    private T head = null;

    private T tail = null;

    public IntrusiveQueue() {}

    public void offer(T el) {
        Objects.requireNonNull(el);
        if(tail == null) {
            head = el;
            tail = el;
        } else {
            tail.next = el;
            tail = el;
        }
    }

    public T poll() {
        if(head == null) {
            return null;
        }
        T el = head;
        if(head == tail) {
            tail = null;
        }
        head = head.next;
        el.next = null;
        return el;
    }

    public boolean empty() {
        return head == null;
    }


    public static abstract class Element<E> {
        E next = null;
        protected Element() {}
    }
    
}
