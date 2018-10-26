package de.stiffi.testing.junit.rules.mqttclient;

public interface MqttMessageHandler {
    void messageReceived(ReceivedMessage receivedMessage);
}
