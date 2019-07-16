package de.stiffi.testing.junit.rules.docker;

import de.stiffi.testing.junit.helpers.ProcessHelper;
import org.junit.Assert;
import org.junit.Test;

public class DockerContainerRuleTest {

    @Test
    public void dockerContainerRuleTest() throws Throwable {
        int containerPort = 800;
        DockerContainerRule dockerContainerRule = DockerContainerRule.newDockerContainerRule("peez/playground")
                .withPortForward(containerPort)
                .withEnvironmentParameter("some", "env-value");
        dockerContainerRule.before();
        int hostPort = dockerContainerRule.getMappedHostPort(containerPort);
        String containerName = dockerContainerRule.getContainerName();

        Thread.sleep(1000);

        //Ensure container running
        String result = ProcessHelper.execute("docker inspect " + containerName);
        Assert.assertTrue(result.contains("" + hostPort));
        Assert.assertTrue(result.contains("some=env-value"));

        dockerContainerRule.after();

        //Ensure container not running
    }
}