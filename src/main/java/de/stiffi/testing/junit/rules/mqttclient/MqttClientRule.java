package de.stiffi.testing.junit.rules.mqttclient;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
    private Map<String , List<byte[]>> receivedMessages = new HashMap<>();


    public MqttClientRule(String brokerhost, boolean ssl, int brokerPort, String username, String password, String truststorePath, String truststorePass) {
        this.brokerhost = brokerhost;
        this.ssl = ssl;
        this.brokerPort = brokerPort;
        this.username = username;
        this.password = password;
        this.clientId = "MqttClientRuleTesting_" + System.currentTimeMillis();
        this.truststorePath = truststorePath;
        this.truststorePass = truststorePass;
    }

    @Override
    protected void before() throws Throwable {

        String serverUri = (ssl ? "ssl://" : "tcp://") + brokerhost + ":" + brokerPort;

        mqttClient = new MqttClient(serverUri, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);

        MqttConnectOptions connOpts = new MqttConnectOptions();
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
            sslClientProperties.setProperty(SSLSocketFactoryFactory.CIPHERSUITES, "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

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
        mqttClient.subscribe(topic);
    }



    public void publish(String topic, byte[] payload, int qos) throws MqttException {
        mqttClient.publish(topic, payload, qos, false);
    }

    @Override
    public void connectionLost(Throwable cause) {
        throw new IllegalStateException(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (!receivedMessages.containsKey(topic)) {
            receivedMessages.put(topic, new ArrayList<>());
        }

        List<byte[]> messagesOnTopic = receivedMessages.get(topic);
        messagesOnTopic.add(message.getPayload());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public List<byte[]> getMessages(String topic) {
        return receivedMessages.getOrDefault(topic, new ArrayList<>());
    }
}
