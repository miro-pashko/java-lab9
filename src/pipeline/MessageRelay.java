package pipeline;

public class MessageRelay extends Thread {

    private final int number;
    private final RingQueue<String> from;
    private final RingQueue<String> to;

    public MessageRelay(int number, RingQueue<String> from, RingQueue<String> to) {
        super("relay-" + number);
        this.number = number;
        this.from = from;
        this.to = to;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                String original = from.pop();
                to.push("Thread No. " + number + " transferred message [" + original + "]");
            }
        } catch (InterruptedException e) {
            // asked to stop
        }
    }
}
