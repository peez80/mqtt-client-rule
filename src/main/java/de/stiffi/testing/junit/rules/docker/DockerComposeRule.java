package de.stiffi.testing.junit.rules.docker;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class DockerComposeRule extends ExternalResource {

    private List<String> runningFiles = new ArrayList<>();

    private String initialComposeFilePath;


    public DockerComposeRule(String initialComposeFilePath) {
        this.initialComposeFilePath = initialComposeFilePath;
    }


    public void start(String composeFilePath) throws IOException {
        String cmd = "docker-compose -f " + composeFilePath + " up -d";
        System.out.println(cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        System.out.println(getProcessOutput(p));
        runningFiles.add(composeFilePath);
    }

    private String getProcessOutput(Process p) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(p.getInputStream()))) {

            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        }
        return sb.toString();
    }

    public void stop(String composeFilePath) {
        try {
            String cmd = "docker-compose -f " + composeFilePath + " rm -sf";
            System.out.println(cmd);
            Process p = Runtime.getRuntime().exec(cmd);
            System.out.println(getProcessOutput(p));
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
