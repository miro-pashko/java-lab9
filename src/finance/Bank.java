package finance;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Holds all accounts itself: balances are stored in an array and every account
 * is protected by its own lock.
 */
public class Bank {

    private final long[] balances;
    private final ReentrantLock[] locks;

    public Bank(long[] openingBalances) {
        balances = openingBalances.clone();
        locks = new ReentrantLock[balances.length];
        for (int i = 0; i < balances.length; i++) {
            if (balances[i] < 0) {
                throw new IllegalArgumentException("Opening balance must not be negative: account " + i);
            }
            locks[i] = new ReentrantLock();
        }
    }

    public int accountCount() {
        return balances.length;
    }

    public long balanceOf(int account) {
        locks[account].lock();
        try {
            return balances[account];
        } finally {
            locks[account].unlock();
        }
    }

    /** Sum of all balances; call it when no transfers are running. */
    public long totalFunds() {
        long sum = 0;
        for (int i = 0; i < balances.length; i++) {
            sum += balanceOf(i);
        }
        return sum;
    }

    /**
     * Atomically moves {@code sum} from one account to another.
     *
     * @throws NotEnoughMoneyException if the source account cannot cover the sum
     */
    public void send(int from, int to, int sum) throws NotEnoughMoneyException {
        if (from == to) {
            throw new IllegalArgumentException("Source and target must differ");
        }
        if (sum <= 0) {
            throw new IllegalArgumentException("Sum must be positive");
        }

        // Deadlock is impossible because a thread never waits while holding a lock:
        // if the second account is busy, the first one is released and the attempt
        // is repeated after a short random pause (the pause prevents livelock).
        while (true) {
            if (locks[from].tryLock()) {
                try {
                    if (locks[to].tryLock()) {
                        try {
                            if (balances[from] < sum) {
                                throw new NotEnoughMoneyException(from, balances[from], sum);
                            }
                            balances[from] -= sum;
                            balances[to] += sum;
                            return;
                        } finally {
                            locks[to].unlock();
                        }
                    }
                } finally {
                    locks[from].unlock();
                }
            }
            LockSupport.parkNanos(ThreadLocalRandom.current().nextLong(1_000, 50_000));
        }
    }
}