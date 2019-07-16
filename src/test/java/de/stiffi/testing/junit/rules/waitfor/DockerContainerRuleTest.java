package de.stiffi.testing.junit.rules.waitfor;

import de.stiffi.testing.junit.rules.docker.DockerContainerRule;

public class DockerContainerRuleTest {

    public void dockerContainerRuleTest() {
        DockerContainerRule dockerContainerRule = DockerContainerRule.newDockerContainerRule("peez/playground");

    }
}
