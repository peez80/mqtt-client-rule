package de.stiffi.testing.junit.rules.waitfor;

import de.stiffi.testing.junit.helpers.ProcessHelper;
import org.junit.Test;

public class DockerLogPullerRuleTest {

    String CONTAINER_NAME = "testContainer" + System.currentTimeMillis();

    @Test
    public void testLogSuccess() throws Throwable {
        //Given
        DockerLogPullerRule rule = new DockerLogPullerRule(CONTAINER_NAME, "thisisthepattern", 6000, 100);
        String cmd = "docker run -itd --rm --name " + CONTAINER_NAME + " peez/playground /bin/sh -c \"echo first line && sleep 1 && echo second line && sleep 2 && echo thisisthepattern && sleep 20\"";
        ProcessHelper.execute(cmd);

        //When
        rule.before();


        //Then
    }

    @Test(expected = TimedoutException.class)
    public void testFail() throws Throwable {
        //Given
        DockerLogPullerRule rule = new DockerLogPullerRule(CONTAINER_NAME, "patternneverfound", 6000, 100);
        String cmd = "docker run -itd --name " + CONTAINER_NAME + " peez/playground /bin/sh -c \"echo first line && sleep 1 && echo second line && sleep 2 && echo thisisthepattern && sleep 20\"";
        ProcessHelper.execute(cmd);

        //When
        rule.before();

    }
}
