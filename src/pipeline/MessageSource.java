package pipeline;

public class MessageSource extends Thread {

    private final int number;
    private final RingQueue<String> target;

    public MessageSource(int number, RingQueue<String> target) {
        super("source-" + number);
        this.number = number;
        this.target = target;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            int counter = 0;
            while (!isInterrupted()) {
                target.push("Thread No. " + number + " generated message " + (++counter));
            }
        } catch (InterruptedException e) {
            // asked to stop
        }
    }
}
