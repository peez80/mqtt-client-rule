package de.stiffi.testing.junit.helpers.docker;



import de.stiffi.testing.junit.helpers.ProcessHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DockerCastlemockHelper {

    private final String containername;
    private boolean debug = false;

    public DockerCastlemockHelper(String containername) {
        this.containername = containername;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }


    public List<String> getEvents() throws IOException {
        List<String> eventContents = new ArrayList<>();
        print("getEvents() -------------------------------------");
        for (String filename : getEventFiles()) {
            String s = getEventFileContent(filename);
            eventContents.add(s);
            print(s + "\n\n");
        }
        return eventContents;
    }

    private List<String> getEventFiles() throws IOException {
        String cmd = "docker exec " +containername+ " find /root/.castlemock/rest/event/v1 -printf %f\\n";
        String files = ProcessHelper.execute(cmd);
        String[] filesArray = files.split("\n");

        print("getEventFiles() --------------------------------------------");
        List<String> filesList = new ArrayList<>();
        for (String file : filesArray) {
            if (file.endsWith(".event")) {
                filesList.add(file);
                print(file);
            }
        }
        return filesList;
    }

    private void print(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

    public String getEventFileContent(String eventFileName) throws IOException {
        String cmd = "docker exec " + containername + " cat /root/.castlemock/rest/event/v1/" + eventFileName;
        String eventFileContent = ProcessHelper.execute(cmd);
        return eventFileContent;
    }

    public void clearEvents() throws IOException {
        String cmd = "docker exec " + containername + " rm -r /root/.castlemock/rest/event/v1";
        String result = ProcessHelper.execute(cmd);

        cmd = "docker exec " + containername + " mkdir -p /root/.castlemock/rest/event/v1";
        result = ProcessHelper.execute(cmd);
    }

    public boolean doEventsContainString(String search) throws IOException {
        List<String> events = getEvents();
        for (String event : events) {
            if (event.contains(search)) {
                return true;
            }
        }
        return false;
    }

    public void dumpEvents() throws IOException {
        System.out.println("Castlemock Events:");
        getEvents().forEach(e -> {
            System.out.println(e);
            System.out.println("---------------------");
        });
    }
}
