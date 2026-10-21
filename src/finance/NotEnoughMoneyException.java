package finance;

public class NotEnoughMoneyException extends Exception {

    public NotEnoughMoneyException(int accountNumber, long available, long requested) {
        super("Account #" + accountNumber + " holds " + available + ", but " + requested + " was requested");
    }
}