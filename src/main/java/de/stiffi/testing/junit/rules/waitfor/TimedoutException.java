package de.stiffi.testing.junit.rules.waitfor;

public class TimedoutException extends RuntimeException {
    public TimedoutException(String message) {
        super(message);
    }
}
