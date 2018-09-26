package de.stiffi.testing.junit.rules.mqttclient;

public class ReceivedMessage {
    public String topic;
    public byte[] payload;

    public ReceivedMessage(String topic, byte[] payload) {
        this.topic = topic;
        this.payload = payload;
    }
}
