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

    private DockerComposeRunInformation runInfo;

    public DockerComposeRule(String initialComposeFilePath) {
        this.runInfo = new DockerComposeRunInformation(initialComposeFilePath);
    }

    public DockerComposeRule(String initialComposeFilePath, String... servicesToStart) {
        this.runInfo = new DockerComposeRunInformation(initialComposeFilePath, servicesToStart);
    }


    public void start() throws IOException {
        String cmd = "docker-compose -f " + runInfo.composeFilePath + " up -d " + runInfo.getServicesAsString();
        System.out.println(cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        System.out.println(getProcessOutput(p));
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


    public void stop() {
        try {
            String cmd = "docker-compose -f " + runInfo.composeFilePath + " rm -sf " + runInfo.getServicesAsString();

            System.out.println(cmd);
            Process p = Runtime.getRuntime().exec(cmd);
            System.out.println(getProcessOutput(p));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        stop();
    }
}
