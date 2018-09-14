package de.stiffi.testing.junit.rules.waitfor;

import de.stiffi.testing.junit.rules.helpers.ProcessHelper;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.regex.Pattern;

public class DockerLogPullerRule extends ExternalResource {


    private String containerName;
    private Pattern regex;
    private long timeoutMs;
    private long pollInterval;

    public DockerLogPullerRule(String containerName, String regex, long timeoutMs, long pollInterval) {
        this.containerName = containerName;
        this.regex = Pattern.compile(regex);
        this.timeoutMs = timeoutMs;
        this.pollInterval = pollInterval;
    }

    @Override
    protected void before() throws Throwable {
        System.out.println("Polling Logs of container " + containerName + " for regex " + regex.pattern());
        poll();
        System.out.println("Pattern " + regex.pattern() + " found in Container " + containerName + " logs");
    }

    private void poll() throws IOException, InterruptedException {
        long stopTime = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < stopTime) {
            String cmd = "docker logs " + containerName;
            String logs = ProcessHelper.execute(cmd);
            if (regex.matcher(logs).find()) {
                return;
            }

            Thread.sleep(pollInterval);
        }

        //If we reach here, the regex was never found
        throw new TimedoutException("Regex " + regex.pattern() + " " + " not found in logs for container " + containerName);
    }
}
