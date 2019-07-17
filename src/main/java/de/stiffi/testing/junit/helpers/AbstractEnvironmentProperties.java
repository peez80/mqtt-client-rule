package de.stiffi.testing.junit.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractEnvironmentProperties {

    private static final String SYSTEM_PROPERTY_OVERRIDE_PROPERTIES = "PROPERTIES_OVERRIDE";

    private boolean alreadyPrinted = false;

    public abstract String getPropertiesFilePath();

    private Properties props = new Properties();


    public AbstractEnvironmentProperties() {
        try {
            loadProperties();
            if (!alreadyPrinted) {
                printProperties();
                alreadyPrinted = true;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void printProperties() {
        String s = "----- Properties: ---------------";

        for (Map.Entry<Object, Object> entry: props.entrySet()) {
            System.out.println(entry.getKey() + "\t:" + entry.getValue());
        }

        s       += "---------------------------------";
    }


    private void loadProperties() throws IOException {
        props = new Properties();
        Path p = Paths.get(getPropertyPath());
        if (Files.isRegularFile(p)) {
            //Load from external file
            try (InputStream in = Files.newInputStream(p)) {
                props.load(in);
            }
        } else {
            //Load from resources
            try (InputStream in = getClass().getResourceAsStream(getPropertyPath())) {
                props.load(in);
            }
        }
    }

    private String getPropertyPath() {
        String propertyPath = getEnvironmentVariable(SYSTEM_PROPERTY_OVERRIDE_PROPERTIES);
        return propertyPath != null ? propertyPath : getPropertiesFilePath();
    }

    private String getEnvironmentVariable(String envName) {
        return System.getProperty(envName) != null ? System.getProperty(envName) : System.getenv(envName);
    }

    public Properties props() {
        return props;
    }
}
