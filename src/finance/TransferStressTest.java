package finance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class TransferStressTest {

    private static final int ACCOUNTS = 100;
    private static final int WORKERS = 64;
    private static final int JOBS = 5_000;
    private static final int TRANSFERS_PER_JOB = 20;
    private static final int MAX_OPENING_FUNDS = 10_000;
    private static final int MAX_SUM = 5_000;
    private static final int TIMEOUT_SECONDS = 60;

    public static void main(String[] args) throws Exception {
        long[] opening = new long[ACCOUNTS];
        for (int i = 0; i < ACCOUNTS; i++) {
            opening[i] = ThreadLocalRandom.current().nextLong(1, MAX_OPENING_FUNDS + 1);
        }
        Bank bank = new Bank(opening);
        long before = bank.totalFunds();

        LongAdder succeeded = new LongAdder();
        LongAdder declined = new LongAdder();

        List<Callable<Void>> jobs = new ArrayList<>();
        for (int j = 0; j < JOBS; j++) {
            jobs.add(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int k = 0; k < TRANSFERS_PER_JOB; k++) {
                    int a = random.nextInt(ACCOUNTS);
                    int b = (a + 1 + random.nextInt(ACCOUNTS - 1)) % ACCOUNTS; // always a different account
                    try {
                        bank.send(a, b, random.nextInt(1, MAX_SUM + 1));
                        succeeded.increment();
                    } catch (NotEnoughMoneyException e) {
                        declined.increment();
                    }
                }
                return null;
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        long started = System.nanoTime();
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> job : jobs) {
            futures.add(pool.submit(job));
        }
        pool.shutdown();
        boolean finished = pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        if (!finished) {
            System.out.println("FAILED: no result in " + TIMEOUT_SECONDS + " s (possible deadlock)");
            pool.shutdownNow();
            System.exit(2);
        }
        for (Future<Void> future : futures) {
            future.get(); // rethrows anything a job failed with
        }

        long after = bank.totalFunds();
        long overdrawn = 0;
        long richest = 0;
        for (int i = 0; i < ACCOUNTS; i++) {
            long balance = bank.balanceOf(i);
            overdrawn += balance < 0 ? 1 : 0;
            richest = Math.max(richest, balance);
        }
        long attempts = succeeded.sum() + declined.sum();
        boolean ok = before == after && overdrawn == 0 && attempts == (long) JOBS * TRANSFERS_PER_JOB;

        System.out.println("+---------------------------------------------+");
        System.out.println("|            TRANSFER STRESS TEST             |");
        System.out.println("+---------------------------------------------+");
        System.out.printf("| %-22s %20s |%n", "accounts", ACCOUNTS);
        System.out.printf("| %-22s %20s |%n", "worker threads", WORKERS);
        System.out.printf("| %-22s %20d |%n", "attempted transfers", attempts);
        System.out.printf("| %-22s %20s |%n", "completed", succeeded.sum() + " (" + percent(succeeded.sum(), attempts) + ")");
        System.out.printf("| %-22s %20s |%n", "declined (no funds)", declined.sum() + " (" + percent(declined.sum(), attempts) + ")");
        System.out.printf("| %-22s %20d |%n", "money before", before);
        System.out.printf("| %-22s %20d |%n", "money after", after);
        System.out.printf("| %-22s %20d |%n", "overdrawn accounts", overdrawn);
        System.out.printf("| %-22s %20d |%n", "largest balance", richest);
        System.out.printf("| %-22s %17d ms |%n", "elapsed", millis);
        System.out.println("+---------------------------------------------+");
        System.out.println(ok ? "VERDICT: PASSED - total amount preserved" : "VERDICT: FAILED - invariant broken");
        if (!ok) {
            System.exit(1);
        }
    }

    private static String percent(long part, long whole) {
        return String.format("%.1f%%", 100.0 * part / whole);
    }
}