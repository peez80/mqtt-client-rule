package de.stiffi.testing.junit.rules.mqttclient;

public class ReceivedMessage {
    private String topic;
    private byte[] payload;
    private  boolean retained;

    public ReceivedMessage(String topic, byte[] payload, boolean retained) {
        this.topic = topic;
        this.payload = payload;
        this.retained = retained;
    }

    public String getTopic() {
        return topic;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isRetained() {
        return retained;
    }
}
