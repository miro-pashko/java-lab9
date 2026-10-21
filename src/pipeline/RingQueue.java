package pipeline;

public class RingQueue<E> {

    private final Object[] slots;
    private int head; // next slot to read
    private int tail; // next slot to write

    public RingQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        // One slot is sacrificed so that head == tail always means "empty".
        slots = new Object[capacity + 1];
    }

    public synchronized void push(E element) throws InterruptedException {
        while (advance(tail) == head) {
            wait(); // full
        }
        slots[tail] = element;
        tail = advance(tail);
        notifyAll(); // producers and consumers share one monitor, so wake everybody
    }

    @SuppressWarnings("unchecked")
    public synchronized E pop() throws InterruptedException {
        while (head == tail) {
            wait(); // empty
        }
        E element = (E) slots[head];
        slots[head] = null;
        head = advance(head);
        notifyAll();
        return element;
    }

    public synchronized int count() {
        return (tail - head + slots.length) % slots.length;
    }

    private int advance(int index) {
        int moved = index + 1;
        return moved == slots.length ? 0 : moved;
    }
}
