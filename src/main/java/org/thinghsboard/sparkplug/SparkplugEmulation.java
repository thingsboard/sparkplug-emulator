/**
 * Copyright © ${project.inceptionYear}-2023 The Thingsboard Authors
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
import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.thinghsboard.gen.sparkplug.SparkplugBProto;
import org.thinghsboard.sparkplug.config.NodeDeviceMetric;
import org.thinghsboard.sparkplug.config.NodeDevice;
import org.thinghsboard.sparkplug.config.SparkplugNodeConfig;
import org.thinghsboard.sparkplug.util.AdaptorException;
import org.thinghsboard.sparkplug.util.ReturnCode;
import org.thinghsboard.sparkplug.util.SparkplugTopic;

import java.util.Map;

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

/**
 * An example Sparkplug B application.
 */

/**
 * Created by nickAS21 on 10.01.23
 */
@Slf4j
public class SparkplugEmulation {

    // Configuration
    private String serverUrl;
    private String namespace;
    protected String groupId;
    protected String edgeNode;
    private String clientId;
    private String edgeNodeToken;
    private ExecutorService executor;
    public MqttAsyncClient client;


    private SparkplugNodeConfig sparkplugNodeConfig;
    private List<NodeDevice> nodeDevices;

    private long publishTimeout;                    // Publish period in milliseconds
    private int index;

    private int bdSeq;
    private int seq;

    private Object seqLock;
    private SparkplugMqttCallback mqttCallback;

    /**
     * constant SPARKPLUG_CLIENT_NAME_SPACE="spBv1.0"
     * Env:
     * export SPARKPLUG_SERVER_URL=tcp://localhost:1883
     * export SPARKPLUG_CLIENT_GROUP_ID="GroupIdSparkplug"
     * export SPARKPLUG_CLIENT_NODE_ID="NodeSparkplugId"
     * export SPARKPLUG_CLIENT_NODE_TOKEN="admin"
     * export SPARKPLUG_CLIENT_IDS="DeviceSparkplugId1,DeviceSparkplugId2"
     *
     * export SPARKPLUG_CLIENT_USERNAME="" - Default empty
     * export SPARKPLUG_CLIENT_PASSWORD="" - Default empty
     *
     * export SPARKPLUG_CLIENT_PUBLISH_TIMEOUT=10000
     * export SPARKPLUG_CLIENT_INDES_MAX=50
     *
     * export SPARKPLUG_CONFIG_FILE="" // Default: SPARKPLUG_CONFIG_FILE: in resourses "Config.json"
     * export SPARKPLUG_METRICS_FILE="" // Default: SPARKPLUG_METRICS_FILE: in resources "Metrics.json"
     *
     * @param args
     */
    public static void main(String[] args) throws MqttException {
        SparkplugEmulation demoEmulation = new SparkplugEmulation();
        demoEmulation.init();
        demoEmulation.run();
    }

    /**
     * @throws IOException
     */
    public void init() throws MqttException {
        try {
            this.sparkplugNodeConfig = getSparkplugNodeConfig();
            this.nodeDevices = getNodeDevices();

            Optional<NodeDevice> nodeOpt = this.nodeDevices.stream().filter(o -> (o.getNodeDeviceId().equals(this.sparkplugNodeConfig.getEdgeNode()))).findAny();
            if (nodeOpt.isPresent()) {
                nodeOpt.get().setNode(true);
            }
            this.serverUrl = this.sparkplugNodeConfig.getServerUrl();
            this.namespace = SPARKPLUG_CLIENT_NAME_SPACE;
            this.groupId = this.sparkplugNodeConfig.getGroupId();
            this.edgeNode = this.sparkplugNodeConfig.getEdgeNode();
            this.clientId = edgeNode;
            this.edgeNodeToken = this.sparkplugNodeConfig.getEdgeNodeToken();
            this.publishTimeout = this.sparkplugNodeConfig.getPublishTimeout();
            this.index = 0;
            this.bdSeq = 0;
            this.seq = 0;
            this.seqLock = new Object();
            log.warn("{}", this.sparkplugNodeConfig);
            log.warn("{}", this.nodeDevices);
        } catch (Exception e) {
            log.error("Invalidate init, " + e.getMessage());
            if (this.client.isConnected()) {
                this.client.disconnect();
            }
            this.client.close();
            System.exit(0);
        }
    }

    public void run() throws MqttException {
        try {
            // Random generator and thread pool for outgoing published messages
            executor = Executors.newFixedThreadPool(1);

            // Build up DEATH payload - note DEATH payloads
            SparkplugBProto.Payload.Builder deathPayload = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(System.currentTimeMillis());
            deathPayload.addMetrics(createMetric(this.getBdSeqNum(),System.currentTimeMillis(), KEYS_BD_SEQ, Int32));
            byte[] deathBytes = deathPayload.build().toByteArray();
            this.client = createClient();
            this.mqttCallback = new SparkplugMqttCallback(this);
            this.client.setCallback(this.mqttCallback);
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setUserName(this.edgeNodeToken);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(30);
            String topic = this.namespace + "/" + groupId + "/" + NDEATH.name() + "/" + edgeNode;
            MqttMessage msg = new MqttMessage();
            msg.setId(0);
            msg.setPayload(deathBytes);
            options.setWill(topic, msg);
            client.connect(options);
            clientReconnect();
            if (client.isConnected()) {
                publishBirth();
                // Subscribe to control/command messages for both the edge of network node and the attached devices
                client.subscribe(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", 0);
                for (NodeDevice device : this.nodeDevices) {
                    if (!device.isNode()) {
                        client.subscribe(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/DCMD/" + edgeNode + "/" + device.getNodeDeviceId() + "/#", 0);
                    }
                }
                publishData();
            } else {
                clientFinishWithError();
            }
        } catch (Exception e) {
            log.error("Invalid run, " + e.getMessage());
            if (this.client.isConnected()) {
                this.client.disconnect();
            }
            this.client.close();
            System.exit(0);
        }
    }

    private MqttAsyncClient createClient() throws MqttException {
        return new MqttAsyncClient(this.serverUrl, this.clientId, new MemoryPersistence());
    }

    /**
     * The DBIRTH must include every metric the device will ever report on.
     */
    public void publishBirth(String... nodeDeviceNames) throws AdaptorException {
        String nodeDeiceName = null;
        try {
            synchronized (seqLock) {
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
            }
        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeiceName + "] publishBirth, " + e.getMessage());
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
            if (edgeNode.equals(nodeDeviceId)) {
                topic = SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + NBIRTH + "/" + edgeNode;
                executor.execute(new Publisher(topic, payloadBirthNode.build(), 1));
            } else {
                topic = SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + DBIRTH + "/" + edgeNode + "/" + nodeDeviceId;
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

    private void creatChangeMetrics(SparkplugBProto.Payload.Builder payload, String nodeDeiceName, long ts) throws AdaptorException {
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
                Thread.sleep(this.publishTimeout);
                synchronized (seqLock) {
                    ts = System.currentTimeMillis();
                    for (NodeDevice device : this.nodeDevices) {
                        // node/devices
                        creatChangeMetrics(payloadDatas.get(device), device.getNodeDeviceId(), ts);
                    }
                    if (this.index >= this.sparkplugNodeConfig.getIndexMax()) {
                        this.index = 0;
                        for (NodeDevice device : this.nodeDevices) {
                            if (device.isNode()) {
                                executor.execute(new Publisher(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + NDATA + "/" + edgeNode, payloadDatas.get(device).build(), 0));
                            } else {
                                executor.execute(new Publisher(SPARKPLUG_CLIENT_NAME_SPACE + "/" + groupId + "/" + DDATA + "/" + edgeNode + "/" + device.getNodeDeviceId(), payloadDatas.get(device).build(), 0));
                            }
                        }

                        payloadDatas = new ConcurrentHashMap<>();
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
                    } else {
                        this.index++;
                        log.info("Publishing index [{}] of Data", index);
                    }
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
        if (!this.edgeNode.equals(sparkplugTopic.getEdgeNodeId())) {
            throw new AdaptorException("The edgeNode [" + sparkplugTopic.getEdgeNodeId() + "] is not valid and must be [" + this.edgeNode + "] for the Sparkplug™ B version.");
        }
        return sparkplugTopic;
    }

    private boolean clientReconnect() throws InterruptedException {
        try {
            if (!client.isConnected()) {
                client.reconnect();
            }
        } catch (MqttException e) {
            if (e.getReasonCode() != MqttClientException.REASON_CODE_CONNECT_IN_PROGRESS) {
                clientFinishWithError();
            }
        }
        if (!client.isConnected()) {
            int cntConnectionFailed = 0;
            while (cntConnectionFailed <= 3) {
                if (cntConnectionFailed == 0) {
                    log.info("Start connection.... ");
                } else {
                    log.info("Start connection.... [{}]", cntConnectionFailed);
                }
                Thread.sleep(this.publishTimeout);
                if (!client.isConnected()) {
                    log.info("Connect failed.... [{}]", cntConnectionFailed);
                    cntConnectionFailed++;
                } else {
                    log.info("Connection success!!!");
                    break;
                }
            }
        }
        return client.isConnected();
    }

    private void clientFinishWithError() {
        log.error("Connect failed.... ");
        log.error("\nCheck:\n" +
                "- parameters in \"Config.json\"\n" +
                "- is the server running at the address [{}]\n" +
                "- whether the client is created as indicated in the documentation [https://thingsboard.io/docs/reference/mqtt-sparkplug-api/]", this.serverUrl);
        clientClose ();
    }

    public void clientClose () {
        if (this.client.isConnected()) {
            try {
                this.client.disconnect();
                this.client.close();
            } catch (MqttException e) {
                log.error("", e);
            }
        }
    }

    private class Publisher implements Runnable  {

        private String topic;
        private SparkplugBProto.Payload outboundPayload;
        int qos;
        MqttActionListener mqttActionListener;

        public Publisher(String topic, SparkplugBProto.Payload outboundPayload, int qos) {
            this.topic = topic;
            this.outboundPayload = outboundPayload;
            this.qos = qos;
            if (qos > 0) {
                mqttActionListener = new MqttActionListener() {
                    private Object topicListener = topic;

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        if (asyncActionToken.getResponse() != null && asyncActionToken.getResponse().getReasonCodes().length > 0 && asyncActionToken.getResponse().getReasonCodes()[0] > 0) {
                            log.error("Listener topic [{}] Response code: [{}]", topicListener, ReturnCode.valueOf((byte) asyncActionToken.getResponse().getReasonCodes()[0]).name());
                            System.exit(0);
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        log.error("Listener topic [{}] Response: [{}] error: [{}]", topicListener, asyncActionToken.getResponse(), exception.getMessage());
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
                if (clientReconnect()) {
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


