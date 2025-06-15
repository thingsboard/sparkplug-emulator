/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thinghsboard.sparkplug;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.thinghsboard.gen.sparkplug.SparkplugBProto;
import org.thinghsboard.sparkplug.config.NodeDeviceMetric;
import org.thinghsboard.sparkplug.config.NodeDevice;
import org.thinghsboard.sparkplug.config.SparkplugNodeConfiguration;
import org.thinghsboard.sparkplug.util.AdaptorException;
import org.thinghsboard.sparkplug.util.ReturnCode;
import org.thinghsboard.sparkplug.util.SparkplugTopic;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thinghsboard.sparkplug.util.MetricDataType.Bytes;
import static org.thinghsboard.sparkplug.util.MetricDataType.Int32;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.KEYS_BD_SEQ;
import static org.thinghsboard.sparkplug.util.SparkplugTopicUtil.SPARKPLUG_CLIENT_NAME_SPACE;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.DBIRTH;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.DDATA;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NBIRTH;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NDATA;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NDEATH;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.createMetric;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.nextValueChange;
import static org.thinghsboard.sparkplug.util.SparkplugTopicUtil.parseTopicPublish;
import static org.thinghsboard.sparkplug.util.SparkplugUtil.getNodeDevices;
import static org.thinghsboard.sparkplug.util.SparkplugUtil.getSparkplugNodeConfig;

@Slf4j
public class SparkplugCore {

       // Configuration
    private ExecutorService executor;
    private MqttAsyncClient client;
    private SparkplugNodeConfiguration configuration;
    private String groupId;
    private String nodeId;
    private List<NodeDevice> nodeDevices;

    private int bdSeq;
    private int seq;

    private final Lock seqLock = new ReentrantLock();

    public  SparkplugCore() throws MqttException {
        try {
            this.configuration = getSparkplugNodeConfig();
            this.nodeDevices = getNodeDevices();
            this.groupId = configuration.getGroupId();
            this.nodeId = configuration.getNodeId();
            this.bdSeq = 0;
            this.seq = 0;
            log.info("Emulator configuration: {}", this.configuration);
            log.info("Emulator devices:");
            for (NodeDevice device : this.nodeDevices) {
                if(device.isNode()){
                    device.setNodeDeviceId(configuration.getNodeId());
                }
                log.info("{}: {}", device.getNodeDeviceId(), device.getNodeDeviceListMetrics());
            }
        } catch (Exception e) {
            log.error("Initialization failure ", e);
            if (this.client.isConnected()) {
                this.client.disconnect();
            }
            this.client.close();
            System.exit(0);
        }
    }

    public void run() {
        try {
            // Random generator and thread pool for outgoing published messages
            executor = Executors.newFixedThreadPool(1);
            // Build up DEATH payload - note DEATH payloads
            SparkplugBProto.Payload.Builder deathPayload = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(System.currentTimeMillis());
            deathPayload.addMetrics(createMetric(this.getBdSeqNum(), System.currentTimeMillis(), KEYS_BD_SEQ, Int32));
            byte[] deathBytes = deathPayload.build().toByteArray();
            this.client = createClient();
            SparkplugMqttCallback mqttCallback = new SparkplugMqttCallback(this);
            this.client.setCallback(mqttCallback);
            MqttConnectionOptions options = configuration.toMqttConnectOptions();
            String topic = SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + NDEATH.name() + "/" + nodeId;
            MqttMessage msg = new MqttMessage();
            msg.setId(0);
            msg.setPayload(deathBytes);
            options.setWill(topic, msg);
            CountDownLatch connectionLatch = new CountDownLatch(1);
            client.connect(options, null, new MqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    connectionLatch.countDown();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    log.error("Connect failed due to: {}", exception.getMessage());
                    log.error("\nCheck:\n" +
                            "- parameters in \"Config.json\"\n" +
                            "- is the server running at the address [{}]\n" +
                            "- whether the client is created as indicated in the documentation [https://thingsboard.io/docs/reference/mqtt-sparkplug-api/]", configuration.getServerUrl());
                    clientClose();
                    System.exit(0);
                }
            });
            if (connectionLatch.await(options.getConnectionTimeout() + 1, TimeUnit.SECONDS)) {
                publishBirth();
                // Subscribe to control/command messages for both the edge of network node and the attached devices
                client.subscribe(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/NCMD/" + nodeId + "/#", 0);
                for (NodeDevice device : this.nodeDevices) {
                    if (!device.isNode()) {
                        client.subscribe(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/DCMD/" + nodeId + "/" + device.getNodeDeviceId() + "/#", 0);
                    }
                }
                publishData();
            }
        } catch (Exception e) {
            log.error("Error occurred: {}", e.getMessage());
            clientClose();
            System.exit(0);
        }
    }

    private MqttAsyncClient createClient() throws MqttException {
        return new MqttAsyncClient(this.configuration.getServerUrl(), this.configuration.getClientId(), new MemoryPersistence());
    }

    /**
     * The DBIRTH must include every metric the device will ever report on.
     */
    public void publishBirth(String... nodeDeviceNames) throws AdaptorException {
        String nodeDeiceName = null;
        seqLock.lock();
        try {
            long ts = System.currentTimeMillis();
            if (nodeDeviceNames.length == 0) {
                // Create the Node/Devices BIRTH payload with metrics
                for (NodeDevice device : this.nodeDevices) {
                    sendBirthNodeDevice(device.getNodeDeviceId(), ts);
                }
            } else {
                for (String nodeDeviceId : nodeDeviceNames) {
                    sendBirthNodeDevice(nodeDeviceId, ts);
                }
            }
        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeiceName + "] publishBirth, " + e.getMessage());
        } finally {
            seqLock.unlock();
        }
    }

    private void sendBirthNodeDevice(String nodeDeviceId, long ts) throws AdaptorException {
        try {
            // Create the Node/Device BIRTH payload with metrics
            SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
                    .setSeq(getBdSeqNum());
            payloadBirthNode.addMetrics(createMetric(getBdSeqNum(), ts, KEYS_BD_SEQ, Int32));
            creatBirthMetrics(payloadBirthNode, nodeDeviceId, ts);
            String topic;
            if (nodeId.equals(nodeDeviceId)) {
                topic = SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + NBIRTH + "/" + nodeId;
                executor.execute(new Publisher(topic, payloadBirthNode.build(), 1));
            } else {
                topic = SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + DBIRTH + "/" + nodeId + "/" + nodeDeviceId;
                executor.execute(new Publisher(topic, payloadBirthNode.build(), 0));
            }

        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeviceId + "] publishBirth, " + e.getMessage());
        }
    }

    private void creatBirthMetrics(SparkplugBProto.Payload.Builder payload, String nodeDeiceName, long ts) throws AdaptorException {
        try {
            List<NodeDeviceMetric> nodeListMetrics = this.nodeDevices.stream().filter(
                    nodeDevice -> nodeDevice.getNodeDeviceId().equals(nodeDeiceName)).findAny().get().getNodeDeviceListMetrics();
            for (NodeDeviceMetric nodeMetric : nodeListMetrics) {
                if (Bytes.equals(nodeMetric.getDataType())) {
                    byte[] valueBytes = new byte[((ArrayList) nodeMetric.getValue()).size()];
                    for (int i = 0; i < ((ArrayList) nodeMetric.getValue()).size(); i++) {
                        valueBytes[i] = ((Integer) ((ArrayList) nodeMetric.getValue()).get(i)).byteValue();
                    }
                    nodeMetric.setValue(valueBytes);
                }
                payload.addMetrics(createMetric(nodeMetric.getValue(), ts, nodeMetric.getNameMetric(), nodeMetric.getDataType()));
            }
        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeiceName + "] publishBirthMetrics, " + e.getMessage());
        }
    }

    private void createOrChangeMetrics(SparkplugBProto.Payload.Builder payload, String nodeDeiceName, long ts) throws AdaptorException {
        try {
            List<NodeDeviceMetric> nodeListMetrics = this.nodeDevices.stream().filter(
                    nodeDevice -> nodeDevice.getNodeDeviceId().equals(nodeDeiceName)).findAny().get().getNodeDeviceListMetrics();
            for (NodeDeviceMetric nodeMetric : nodeListMetrics) {
                if (nodeMetric.isAutoChange()) {
                    Object value = nextValueChange(nodeMetric.getDataType());
                    if (value != null) {
                        payload.addMetrics(createMetric(value, ts, nodeMetric.getNameMetric(), nodeMetric.getDataType()));
                    } else {
                        throw new AdaptorException("Invalid next value for device [" + nodeDeiceName + "] publishDataMetrics, MetricDataType " + nodeMetric.getDataType());
                    }
                }
            }
        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeiceName + "] publishDataMetrics, " + e.getMessage());
        }
    }

    private void publishData() {
        long ts;
        try {
            Map<NodeDevice, SparkplugBProto.Payload.Builder> payloadDatas = new ConcurrentHashMap<>();
            for (NodeDevice device : this.nodeDevices) {
                SparkplugBProto.Payload.Builder payloadData;
                if (device.isNode()) {
                    payloadData = SparkplugBProto.Payload.newBuilder()
                            .setTimestamp(System.currentTimeMillis())
                            .setSeq(getBdSeqNum());
                } else {
                    payloadData = SparkplugBProto.Payload.newBuilder()
                            .setTimestamp(System.currentTimeMillis())
                            .setSeq(getSeqNum());
                }
                payloadDatas.put(device, payloadData);
            }

            // Loop forever publishing data every PUBLISH_PERIOD
            while (true) {
                if (Thread.interrupted()) {
                    break;
                }
                Thread.sleep(configuration.getPublishInterval());
                seqLock.lock();
                try {
                    ts = System.currentTimeMillis();
                    for (NodeDevice device : this.nodeDevices) {
                        // node/devices
                        payloadDatas.put(device, payloadDatas.get(device).clearMetrics());
                        createOrChangeMetrics(payloadDatas.get(device), device.getNodeDeviceId(), ts);
                    }
                    for (NodeDevice device : this.nodeDevices) {
                        if (device.isNode()) {
                            executor.execute(new Publisher(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + NDATA + "/" + nodeId, payloadDatas.get(device).build(), 0));
                        } else {
                            executor.execute(new Publisher(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + DDATA + "/" + nodeId + "/" + device.getNodeDeviceId(), payloadDatas.get(device).build(), 0));
                        }
                    }
                } finally {
                    seqLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("Publishing data is bad.... ", e);
        }
    }

    protected int getBdSeqNum() throws Exception {
        if (bdSeq == 256) {
            bdSeq = 0;
        }
        return bdSeq++;
    }

    protected int getSeqNum() throws Exception {
        if (seq == 256) {
            seq = 0;
        }
        return seq++;
    }

    public SparkplugTopic validateTopic(SparkplugTopic sparkplugTopic) throws AdaptorException {
        if (!SPARKPLUG_CLIENT_NAME_SPACE.equals(sparkplugTopic.getNamespace())) {
            throw new AdaptorException("The namespace [" + sparkplugTopic.getNamespace() + "] is not valid and must be [" + SPARKPLUG_CLIENT_NAME_SPACE + "] for the Sparkplug™ B version.");
        }
        if (!this.groupId.equals(sparkplugTopic.getGroupId())) {
            throw new AdaptorException("The groupId [" + sparkplugTopic.getGroupId() + "] is not valid and must be [" + this.groupId + "].");
        }
        if (!this.nodeId.equals(sparkplugTopic.getEdgeNodeId())) {
            throw new AdaptorException("The edgeNode [" + sparkplugTopic.getEdgeNodeId() + "] is not valid and must be [" + this.nodeId + "] for the Sparkplug™ B version.");
        }
        return sparkplugTopic;
    }

    private boolean clientReconnect(long maxDelay) throws InterruptedException {
        if (!client.isConnected()) {
            int cntConnectionFailed = 0;
            while (cntConnectionFailed <= 3) {
                if (cntConnectionFailed == 0) {
                    log.info("Start connection.... ");
                } else {
                    log.info("Start connection.... [{}]", cntConnectionFailed);
                }
                long delay = 0;
                while (delay < maxDelay) {
                    Thread.sleep(100);
                    delay += 100;
                    if (client.isConnected()) {
                        log.info("Connection success!!!");
                        return true;
                    }
                }
                log.info("Connect failed.... [{}]", cntConnectionFailed);
                cntConnectionFailed++;
            }
        }
        return client.isConnected();
    }

    public void clientClose() {
        if (this.client != null && this.client.isConnected()) {
            try {
                this.client.disconnect();
                this.client.close();
            } catch (MqttException e) {
                log.error("", e);
            }
        }
    }

    private class Publisher implements Runnable {

        private final String topic;
        private final SparkplugBProto.Payload outboundPayload;
        int qos;
        MqttActionListener mqttActionListener;

        public Publisher(String topic, SparkplugBProto.Payload outboundPayload, int qos) {
            this.topic = topic;
            this.outboundPayload = outboundPayload;
            this.qos = qos;
            if (qos > 0) {
                mqttActionListener = new MqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        if (asyncActionToken.getResponse() != null && asyncActionToken.getResponse().getReasonCodes().length > 0 && asyncActionToken.getResponse().getReasonCodes()[0] > 0) {
                            log.error("Listener topic [{}] Response code: [{}]", topic, ReturnCode.valueOf((byte) asyncActionToken.getResponse().getReasonCodes()[0]).name());
                            System.exit(0);
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        log.error("Listener topic [{}] Response: [{}] error: [{}]", topic, asyncActionToken.getResponse(), exception.getMessage());
                    }
                };
            }
        }

        @SneakyThrows
        public void run() {
            String nodeDeiceName = "";
            try {
                SparkplugTopic sparkplugTopic = parseTopicPublish(topic);
                nodeDeiceName = sparkplugTopic.isNode() ? sparkplugTopic.getEdgeNodeId() : sparkplugTopic.getDeviceId();
                if (clientReconnect(configuration.getPublishInterval())) {
                    if (qos > 0) {
                        client.publish(topic, outboundPayload.toByteArray(), qos, false, null, mqttActionListener);
                    } else {
                        client.publish(topic, outboundPayload.toByteArray(), qos, false);
                    }
                    log.info("Publishing [{}] {}", nodeDeiceName, sparkplugTopic.getType().name());
                } else {
                    log.error("Client is not connected. Publishing [{}] {} bad", nodeDeiceName, sparkplugTopic.getType().name());
                }
            } catch (MqttException | InterruptedException | AdaptorException mqttException) {
                mqttException.printStackTrace();
            }
        }
    }
}
