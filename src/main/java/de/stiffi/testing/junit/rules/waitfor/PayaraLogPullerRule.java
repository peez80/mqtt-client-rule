package de.stiffi.testing.junit.rules.waitfor;

public class PayaraLogPullerRule extends DockerLogPullerRule {

    private static final String PAYARA_REGEX = "Payara Micro.*badassmicrofish.*ready in.*ms";

    public PayaraLogPullerRule(String containerName) {
        super(containerName, PAYARA_REGEX, 120000, 2000);
    }

    public PayaraLogPullerRule(String containerName, long timeoutMs) {
        super(containerName, PAYARA_REGEX, timeoutMs, 2000);
    }

    public PayaraLogPullerRule(String containerName, String regex, long timeoutMs, long pollInterval) {
        super(containerName, regex, timeoutMs, pollInterval);
    }
}
