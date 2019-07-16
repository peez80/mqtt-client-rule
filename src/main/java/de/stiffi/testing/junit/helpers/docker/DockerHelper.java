package de.stiffi.testing.junit.helpers.docker;

import de.stiffi.testing.junit.helpers.ProcessHelper;

import java.io.IOException;

public class DockerHelper {
    public static void startDocker(String imageName, String containerName, String portmapping) {

        final String myContainerName = containerName + "_" + System.currentTimeMillis();

        String command = "docker run -d --name " + myContainerName + " -p " + portmapping + " " + imageName;

        System.out.println("Start " + myContainerName + ": " + command);

        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stopDocker(myContainerName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public static void stopDocker(String containerName) throws IOException {
        System.out.println("Remove " + containerName);
        String command = "docker rm -f " + containerName;
        ProcessHelper.execute(command, true);

    }

    public static String getLogs(String containerName, Long sinceMs) throws IOException {
        String cmd = (sinceMs != null ?
                "docker logs --since " + sinceMs + "ms " + containerName
                : "docker logs " + containerName);
        System.out.println(cmd);
        String logs = ProcessHelper.execute(cmd);
        return logs;
    }

    public static String getLogsTail(String containerName, int lines) throws IOException {
        String cmd = "docker logs --tail " + lines + " " + containerName;
        System.out.println(cmd);
        String logs = ProcessHelper.execute(cmd);
        return logs;
    }


}
