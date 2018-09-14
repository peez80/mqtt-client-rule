package de.stiffi.testing.junit.rules.waitfor;

import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;


public class TcpPollerRuleTest {

    private final static int PORT = 34763;

    @Test
    public void testTcpPollerSuccess() throws Throwable {

        // Given
        startSocket(2000);
        TcpPollerRule rule = new TcpPollerRule("localhost", PORT, 4000, 100);


        //When
        rule.before();

        //Then
        //No exception
    }

    @Test(expected = TimedoutException.class)
    public void testTcpPollerFail() throws Throwable {
// Given
        TcpPollerRule rule = new TcpPollerRule("localhost", PORT, 4000, 100);


        //When
        rule.before();

        //Then
        //No exception
    }


    private void startSocket(long delayBeforeStart) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayBeforeStart);
                    System.out.println("Start Socket");
                    ServerSocket socket = new ServerSocket(PORT);
                    Thread.sleep(3000);
                    socket.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}