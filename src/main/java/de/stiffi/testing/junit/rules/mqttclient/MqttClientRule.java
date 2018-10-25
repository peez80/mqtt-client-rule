package de.stiffi.testing.junit.rules.mqttclient;

import org.apache.commons.codec.digest.DigestUtils;
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
    protected int brokerPort;
    private final String username;
    private final String password;
    private String truststorePath;
    private String truststorePass;

    private List<MqttClient> mqttClients = new ArrayList<>();

    private int maxInflightWindow = 10;
    private int clientInstanceCount = 1;

    private String SHARED_SUBSCRIPTION_PREFIX = "$share:SH" + DigestUtils.md5Hex("" + new Random().nextInt()) + ":";

    /**
     * topic - list(messages)
     */
    private List<ReceivedMessage> receivedMessages = Collections.synchronizedList(new LinkedList<>());


    public MqttClientRule(String brokerhost, boolean ssl, int brokerPort, String username, String password, String truststorePath, String truststorePass) {
        this.brokerhost = brokerhost;
        this.ssl = ssl;
        this.brokerPort = brokerPort;
        this.username = username;
        this.password = password;
        this.truststorePath = truststorePath;
        this.truststorePass = truststorePass;
    }

    public MqttClientRule withMaxInflight(int maxInflight) {
        maxInflightWindow = maxInflight;
        return this;
    }

    public MqttClientRule withMqttClientInstances(int clientInstanceCount) {
        this.clientInstanceCount = clientInstanceCount;
        return this;
    }

    @Override
    protected void before() throws Throwable {
        for (int i = 0; i < clientInstanceCount; i++) {
            MqttClient client = connect();
            mqttClients.add(client);
        }
    }

    private MqttClient connect() throws MqttException {
        String serverUri = (ssl ? "ssl://" : "tcp://") + brokerhost + ":" + brokerPort;
        String clientId = "MqttClientRuleTesting_" + System.currentTimeMillis() + "_" + new Random(System.currentTimeMillis()).nextInt();

        MqttClient mqttClient = new MqttClient(serverUri, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMaxInflight(maxInflightWindow);
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


        System.out.println("MQTT Connect....");
        mqttClient.connect(connOpts);

        return mqttClient;
    }

    @Override
    protected void after() {
        try {
            System.out.println("MQTT Disconnect...");
            for (MqttClient mqttClient : mqttClients) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) throws MqttException {
        if (clientInstanceCount > 1) {
            topic = SHARED_SUBSCRIPTION_PREFIX + topic;
        }
        for (MqttClient mqttClient : mqttClients) {
            System.out.println("Subscribe " + topic);
            mqttClient.subscribe(topic, 1);
        }

    }


    public void publish(String topic, byte[] payload, int qos) throws MqttException {
        System.out.println("Publishing message to " + topic);
        //Currently we publish on the first client - if this causes trouble - make round robin
        mqttClients.get(0).publish(topic, payload, qos, false);
    }

    @Override
    public void connectionLost(Throwable cause) {
        throw new IllegalStateException(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        receivedMessages.add(new ReceivedMessage(topic, message.getPayload()));
        System.out.println("Received MQTT message on topic "
                + topic
                + ", Count: " + getMessages(topic).size()
                + ", Content: " + (message.getPayload() != null ? Base64.getEncoder().encodeToString(message.getPayload()) : "null")
        );
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public List<byte[]> getMessages(String topic) {
        List<byte[]> messages = new ArrayList<>(receivedMessages.size());
        synchronized (receivedMessages) {
            for (ReceivedMessage msg : receivedMessages) {
                if (msg.topic.equals(topic)) {
                    messages.add(msg.payload);
                }
            }
        }

        return messages;
    }

    public void waitForMessage(String topic, long timeoutMs) {
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
