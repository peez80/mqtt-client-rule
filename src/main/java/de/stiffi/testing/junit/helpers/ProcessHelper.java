package de.stiffi.testing.junit.helpers;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class ProcessHelper {
    public static String getProcessOutput(Process p, boolean trace) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(p.getInputStream()))) {

            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
                if (trace) {
                    System.out.println(line);
                }
            }
        }
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(p.getErrorStream()))) {

            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
                if (trace) {
                    System.out.println(line);
                }
            }
        }

        return sb.toString();
    }

    public static String execute(String cmd, boolean trace, byte[] writeToStdIn) throws IOException {
        if (trace) {
            System.out.println("Exec:");
            System.out.println(StringUtils.join(cmd, " "));
            System.out.println("");
        }
        Process p = Runtime.getRuntime().exec(cmd);

        if (writeToStdIn != null) {
            OutputStream stdIn = p.getOutputStream();
            stdIn.write(writeToStdIn);
            stdIn.close();
        }

        String s = getProcessOutput(p, trace);

        try {
            p.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return s;
    }

    public static String execute(String cmd, boolean trace) throws IOException {
        return execute(cmd, trace, null);
    }

    public static String execute(String cmd) throws IOException {
        return execute(cmd, false, null);
    }
}
