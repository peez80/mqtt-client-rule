package de.stiffi.testing.junit.rules.docker;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.*;

public class DockerComposeRule extends ExternalResource {

    private List<String> runningFiles;

    private String initialComposeFilePath;


    public DockerComposeRule(String initialComposeFilePath) {
        this.initialComposeFilePath = initialComposeFilePath;
    }


    public void start(String composeFilePath) throws IOException {
        String cmd = "docker-compose -f " + composeFilePath + " up -d";
        System.out.println(cmd);
        Runtime.getRuntime().exec(cmd);
        runningFiles.add(composeFilePath);
    }

    public void stop(String composeFilePath) {
        try {
            String cmd = "docker-compose -f " + composeFilePath + " rm -sf";
            System.out.println(cmd);
            Runtime.getRuntime().exec(cmd);
            runningFiles.remove(composeFilePath);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void stopAll() {
        List<String> composeFiles = Collections.unmodifiableList(runningFiles);
        for (String composeFile : composeFiles) {
            stop(composeFile);
        }
    }

    @Override
    protected void before() throws Throwable {
        if (StringUtils.isNotBlank(initialComposeFilePath)) {
            start(initialComposeFilePath);
        }
    }

    @Override
    protected void after() {
        stopAll();
    }
}
