package de.stiffi.testing.junit.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class TemplateHelper {

    private Map<String, String> replacements;


    public TemplateHelper() {
        this.replacements = new HashMap<>();
    }

    public static TemplateHelper get() {
        return new TemplateHelper();
    }

    /**
     * Replace {{vin}}
     * @param vin
     * @return
     */
    public TemplateHelper withVin(String vin) {
        return with("{{vin}}", vin);
    }

    public TemplateHelper withPort(int port) {
        return with("{{port}}", "" + port);
    }

    /**
     * Replace {{csmSnr}}
     * @param csmSnr
     * @return
     */
    public TemplateHelper withCsmSnr(String csmSnr) {
        return with("{{csmSnr}}", csmSnr);
    }

    public TemplateHelper with(String tag, String replacement) {
        replacements.put(tag, replacement);
        return this;
    }

    public TemplateHelper with(String tag, Date replacementDate) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String dateString = df.format(replacementDate);
        return with(tag, dateString);
    }

    public TemplateHelper withFleet(String fleet) {
        return with("{{fleet}}", fleet);
    }

    public TemplateHelper withEventId(String eventId) {
        return with("{{eventId}}", eventId).with("{{eventid}}", eventId);
    }

    public String readTemplateFile(String filePath) throws IOException {
        return readTemplateFile(filePath, getClass());
    }


    public String readTemplateFile(String filePath, Class<?> refClazz) throws IOException {
        String content = loadFile(filePath, refClazz);
        content = replaceValues(content);
        return content;
    }

    public String readString(String content) {
        return replaceValues(content);
    }

    private String replaceValues(String template) {
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            String value = replacement.getValue() != null ? replacement.getValue() : "";
            template = template.replace(replacement.getKey(), value);
        }
        return template;
    }



    private String loadFile(String filePath, Class<?> refClazz) throws IOException {
        InputStream in = null;

        try {
            in = refClazz.getResourceAsStream(filePath);
            if (in == null) {
                in = getClass().getClassLoader().getResourceAsStream(filePath);
            }
            if (in == null) {
                in = new FileInputStream(filePath);
            }

            return StreamReader.read(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
