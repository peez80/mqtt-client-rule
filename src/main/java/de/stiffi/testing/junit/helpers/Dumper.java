package de.stiffi.testing.junit.helpers;


import java.util.ArrayList;
import java.util.List;

public class Dumper {

    private static final List<String> CENSOR = new ArrayList<>();

    public static void censor(String... elementsToCensorInOutput) {
        for (String s : elementsToCensorInOutput) {
            CENSOR.add(s);
        }
    }

    public static void sout(String s) {
        String output = s;
        for (String censor : CENSOR) {
            output = output.replaceAll(censor, "XXXX");
        }
        System.out.println(output);
    }
}
