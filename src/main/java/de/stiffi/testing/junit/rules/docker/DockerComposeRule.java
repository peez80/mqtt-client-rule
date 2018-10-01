package de.stiffi.testing.junit.rules.docker;

import de.stiffi.testing.junit.rules.helpers.ProcessHelper;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public String getLogs(String service) throws IOException {
        String cmd = "docker-compose -f " + runInfo.composeFilePath + " logs " + service;
        System.out.println(cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        String logs = getProcessOutput(p);
        return logs;
    }

    /**
     *
     * @param service Service Name
     * @param privatePort Private/Internal port of the service
     * @return
     * @throws IOException
     */
    public int getPort(String service, int privatePort) throws IOException {
        String cmd = "docker-compose -f " + runInfo.composeFilePath + " port " + service + " " + privatePort;
        String output = ProcessHelper.execute(cmd);
        String sPort = output.split(":")[1];
        return Integer.parseInt(sPort);
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

    public void stop(String serviceName) {
        try {
            String cmd = "docker-compose -f " + runInfo.composeFilePath + " rm -sf " + serviceName;

            System.out.println(cmd);
            Process p = Runtime.getRuntime().exec(cmd);
            System.out.println(getProcessOutput(p));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public String getComposeFilePath() {
        return runInfo.composeFilePath;
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
