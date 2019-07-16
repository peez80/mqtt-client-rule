package de.stiffi.testing.junit.rules.docker;

import de.stiffi.testing.junit.helpers.ProcessHelper;
import de.stiffi.testing.junit.helpers.SocketHelper;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DockerContainerRule extends ExternalResource {

    private String dockerImage;
    private String containerName;
    private Set<Integer> mappedContainerPorts = new HashSet<>();
    private Map<String, String> environmentVariable = new HashMap<>();

    /**
     * container Port - Host Port
     */
    private Map<Integer, Integer> mappedPorts = new HashMap<>();

    private DockerContainerRule() {
        containerName = buildDefaultContainerName();
    }

    public static DockerContainerRule newDockerContainerRule(String image) {
        DockerContainerRule rule = new DockerContainerRule();
        rule.dockerImage = image;
        return rule;
    }

    public DockerContainerRule withContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    public DockerContainerRule withPortForward(int containerPort) {
        mappedContainerPorts.add(containerPort);
        return this;
    }

    public DockerContainerRule withEnvironmentParameter(String key, String value) {
        environmentVariable.put(key, value);
        return this;
    }

    private String buildDefaultContainerName() {
        return "docker-container-rule-" + System.currentTimeMillis();
    }

    private void findMappedPorts() {
        for (Integer containerPort  : mappedContainerPorts) {
            int hostPort = SocketHelper.findFreePort();
            mappedPorts.put(containerPort, hostPort);
        }
    }

    public int getMappedHostPort(int mappedContainerPort) {
        return mappedPorts.get(mappedContainerPort);
    }


    @Override
    protected void before() throws Throwable {
        findMappedPorts();
        String cmd = "docker run -itd --name " + containerName + " ";
        for (Integer containerPort : mappedPorts.keySet()) {
            int hostPort = getMappedHostPort(containerPort);
            cmd += "-p " + hostPort + ":" + containerPort + " ";
        }

        for (String envKey : environmentVariable.keySet()) {
            String value = environmentVariable.get(envKey);
            cmd += "-e \"" + envKey + "=" + value + "\" ";
        }


        cmd += " peez/playground";

        String result = ProcessHelper.execute(cmd);

    }

    public String getContainerName() {
        return containerName;
    }

    @Override
    protected void after() {
        try {
            ProcessHelper.execute("docker stop " + containerName, true);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        try {
            ProcessHelper.execute("docker rm -f " + containerName);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    private class PortMapping {
        public int hostPort;
        public int containerPort;
    }
}
