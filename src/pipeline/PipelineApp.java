package pipeline;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PipelineApp {

    private static final int QUEUE_CAPACITY = 8;
    private static final int SOURCES = 5;
    private static final int RELAYS = 2;
    private static final int TO_PRINT = 100;

    // Thread numbers are unique: sources are 1..5, relays continue with 6..7.
    private static final Pattern LINE = Pattern.compile(
            "Thread No\\. (\\d+) transferred message \\[Thread No\\. (\\d+) generated message (\\d+)]");

    public static void main(String[] args) throws InterruptedException {
        RingQueue<String> raw = new RingQueue<>(QUEUE_CAPACITY);
        RingQueue<String> relayed = new RingQueue<>(QUEUE_CAPACITY);

        for (int n = 1; n <= SOURCES; n++) {
            new MessageSource(n, raw).start();
        }
        for (int n = SOURCES + 1; n <= SOURCES + RELAYS; n++) {
            new MessageRelay(n, raw, relayed).start();
        }

        Map<Integer, Integer> perRelay = new TreeMap<>();
        Map<Integer, Integer> perSource = new TreeMap<>();

        System.out.printf("%-4s | %-5s | %-6s | %s%n", "#", "relay", "origin", "message");
        System.out.println("-".repeat(104));
        for (int i = 1; i <= TO_PRINT; i++) {
            String line = relayed.pop();
            Matcher m = LINE.matcher(line);
            if (m.matches()) {
                perRelay.merge(Integer.parseInt(m.group(1)), 1, Integer::sum);
                perSource.merge(Integer.parseInt(m.group(2)), 1, Integer::sum);
                System.out.printf("%03d  | T%-4s | T%-5s | %s%n", i, m.group(1), m.group(2), line);
            } else {
                System.out.printf("%03d  | %s%n", i, line);
            }
        }

        System.out.println("-".repeat(104));
        System.out.println("Messages handled per relay thread:  " + perRelay);
        System.out.println("Messages originated per source:     " + perSource);
    }
}
