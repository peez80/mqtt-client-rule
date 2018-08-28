package de.stiffi.testing.junit.rules.docker;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class DockerComposeRunInformation {
    public String composeFilePath;
    List<String> runningServices;

    public DockerComposeRunInformation(String composeFilePath, List<String> runningServices) {
        this.composeFilePath = composeFilePath;
        this.runningServices = runningServices;
    }

    public DockerComposeRunInformation(String composeFilePath, String... runningServices) {
        this.composeFilePath = composeFilePath;
        this.runningServices = Arrays.asList(runningServices);
    }

    public DockerComposeRunInformation(String composeFilePath) {
        this(composeFilePath, (List<String>)null);
    }

    public String getServicesAsString() {
        if (runningServices != null && !runningServices.isEmpty()) {
            return " " + StringUtils.join(runningServices, " ");
        }else{
            return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DockerComposeRunInformation that = (DockerComposeRunInformation) o;
        return Objects.equals(composeFilePath, that.composeFilePath) &&
                Objects.equals(runningServices, that.runningServices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composeFilePath, runningServices);
    }
}
