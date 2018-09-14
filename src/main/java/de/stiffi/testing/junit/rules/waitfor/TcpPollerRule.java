package de.stiffi.testing.junit.rules.waitfor;

import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.Socket;

public class TcpPollerRule extends ExternalResource {

    private String host;
    private int port;
    private long timeoutMs;
    private long pollIntervalMs;

    public TcpPollerRule(String host, int port, long timeoutMs, long pollIntervalMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Override
    protected void before() throws Throwable {
        System.out.println("Waiting for " + host + ":" + port);
        poll();
        System.out.println(host + ":" + port + " is available now");
    }

    private void poll() throws InterruptedException {
        long stopTimestamp = System.currentTimeMillis() + timeoutMs;

        while(System.currentTimeMillis() < stopTimestamp ) {
            System.out.println("Poll..");
            try (Socket socket = new Socket(host, port)){
                //If we get here, Socket succeeded
                return;
            } catch (Exception e) {
                //Nothing...
            }

            Thread.sleep(pollIntervalMs);
        }

        throw new TimedoutException("Socket " + host + ":" + port + " didn't open in time.");
    }
}
