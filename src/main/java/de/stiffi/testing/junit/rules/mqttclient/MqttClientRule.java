package de.stiffi.testing.junit.rules.mqttclient;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

import java.util.*;

public class MqttClientRule extends ExternalResource implements MqttCallback {

    private final String brokerhost;
    private final boolean ssl;
    private final int brokerPort;
    private final String username;
    private final String password;
    private final String clientId;
    private String truststorePath;
    private String truststorePass;

    private MqttClient mqttClient;

    /**
     * topic - list(messages)
     */
    private Set<ReceivedMessage> receivedMessages = Collections.synchronizedSet(new HashSet<>());


    public MqttClientRule(String brokerhost, boolean ssl, int brokerPort, String username, String password, String truststorePath, String truststorePass) {
        this.brokerhost = brokerhost;
        this.ssl = ssl;
        this.brokerPort = brokerPort;
        this.username = username;
        this.password = password;
        this.clientId = "MqttClientRuleTesting_" + System.currentTimeMillis() + "_" + new Random(System.currentTimeMillis()).nextInt();
        this.truststorePath = truststorePath;
        this.truststorePass = truststorePass;
    }

    @Override
    protected void before() throws Throwable {

        String serverUri = (ssl ? "ssl://" : "tcp://") + brokerhost + ":" + brokerPort;

        mqttClient = new MqttClient(serverUri, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMaxInflight(1000);
        connOpts.setCleanSession(true);
        if (username != null) {
            connOpts.setUserName(username);
        }
        if (password != null) {
            connOpts.setPassword(password.toCharArray());
        }
        connOpts.setAutomaticReconnect(false);

        if (ssl && !StringUtils.isBlank(truststorePath)) {
            Properties sslClientProperties = new Properties();
            sslClientProperties.setProperty(SSLSocketFactoryFactory.SSLPROTOCOL, "TLSv1.2");
            sslClientProperties.setProperty(SSLSocketFactoryFactory.JSSEPROVIDER, "SunJSSE");
            sslClientProperties.setProperty(SSLSocketFactoryFactory.TRUSTSTORETYPE, "JKS");
            sslClientProperties.setProperty(SSLSocketFactoryFactory.TRUSTSTORE, truststorePath);
            sslClientProperties.setProperty(SSLSocketFactoryFactory.TRUSTSTOREPWD, truststorePass);

            connOpts.setSSLProperties(sslClientProperties);
        }


        System.out.println("MQTT Connect...");
        mqttClient.connect(connOpts);
    }

    @Override
    protected void after() {
        try {
            System.out.println("MQTT Disconnect...");
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) throws MqttException {
        mqttClient.subscribe(topic, 1);
    }


    public void publish(String topic, byte[] payload, int qos) throws MqttException {
        System.out.println("Publishing message to " + topic);
        mqttClient.publish(topic, payload, qos, false);
    }

    @Override
    public void connectionLost(Throwable cause) {
        throw new IllegalStateException(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        receivedMessages.add(new ReceivedMessage(topic, message.getPayload()));
        System.out.println("Received MQTT message on topic " + topic + ", Count: " + getMessages(topic).size());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public List<byte[]> getMessages(String topic) {
        List<byte[]> messages = new ArrayList<>(receivedMessages.size());
        for (ReceivedMessage msg : receivedMessages) {
            if (msg.topic.equals(topic)) {
                messages.add(msg.payload);
            }
        }
        return messages;
    }

    public void waitForMessage(String topic, long timeoutMs){
        waitForMessage(topic, timeoutMs, 1);
    }

    public void waitForMessage(String topic, long timeoutMs, int minimalNumberOfMessages) {
        if (timeoutMs <= 0) {
            return;
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + timeoutMs) {
            try {
                Thread.sleep(50);
                List<byte[]> messages = getMessages(topic);
                if (messages.size() >= minimalNumberOfMessages) {
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForMessage(String topic) {
        waitForMessage(topic, 10000l);
    }

    public void assertMessagesReceived(String failedMessage, String topic, int expectedMessageCount) {
        assertMessagesReceived(failedMessage, topic, expectedMessageCount, 10000l);
    }

    public void assertMessagesReceived(String failedMessage, String topic, int expectedMessageCount, long waitForMessageTimeout) {
        waitForMessage(topic, waitForMessageTimeout, expectedMessageCount);
        List<byte[]> receivedMessages = getMessages(topic);

        String msg = failedMessage + ", \nExpected : " + expectedMessageCount + " messages on " + topic + "\nActual   : " + receivedMessages.size() + " messages";
        if (receivedMessages.size() != expectedMessageCount) {
            Assert.fail(msg);
        }
    }
}
