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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.thinghsboard.sparkplug.util.JacksonUtil;
import org.thinghsboard.sparkplug.util.SparkplugTopic;

import java.util.Map;

import static org.thinghsboard.sparkplug.util.MetricDataType.Bytes;
import static org.thinghsboard.sparkplug.util.MetricDataType.Int32;
import static org.thinghsboard.sparkplug.util.SparkplugConstatns.CONFIG_JSON;
import static org.thinghsboard.sparkplug.util.SparkplugConstatns.KEYS_BD_SEQ;
import static org.thinghsboard.sparkplug.util.SparkplugConstatns.LIST_METRICS_JSON;
import static org.thinghsboard.sparkplug.util.SparkplugConstatns.SPARK_CONFIG_PATH_KEY;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.DDATA;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NBIRTH;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NDATA;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NDEATH;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.createMetric;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.nextValueChange;
import static org.thinghsboard.sparkplug.util.SparkplugTopicUtil.NAMESPACE;
import static org.thinghsboard.sparkplug.util.SparkplugTopicUtil.validateNameSpace;

/**
 * An example Sparkplug B application.
 */
/**
 * Created by nickAS21 on 10.01.23
 */
@Slf4j
public class SparkplugEmulation {

    // Configuration
    //	private String serverUrl = "tcp://192.168.1.100:1883";
    private String serverUrl;
    private String namespace;
    protected String groupId;
    protected String edgeNode;
    private String clientId;
    private String username;
    private ExecutorService executor;
    private MqttAsyncClient client;


    private SparkplugNodeConfig sparkplugNodeConfig;
    private List<NodeDevice> nodeDevices;

    private long publishTimeout;                    // Publish period in milliseconds
    private int index;
    private Calendar calendar;

    private int bdSeq;
    private int seq;

    private Object seqLock;
    private SparkplugMqttCallback mqttCallback;

    /**
     * PARK_CONFIG_PATH='src/main/resources'
     * java -jar sparkplug-1.0-SNAPSHOT-jar-with-dependencies.jar SPARK_CONFIG_PATH='src/main/resources'
     *
     * @param args
     */
    public static void main(String[] args) throws MqttException {
        SparkplugEmulation demoEmulation = new SparkplugEmulation();
        demoEmulation.init(args);
        demoEmulation.run();
    }

    /**
     * @param args
     * @throws IOException
     */
    public void init(String[] args) throws MqttException {
        try {
            Map<String, String> env = System.getenv();
            InputStream isConfig;
            InputStream isListMetrics;
            String appPath = System.getProperty("user.dir");
            String pathConfigJson = env.get(SPARK_CONFIG_PATH_KEY);
            if (pathConfigJson != null &&
                    new File(pathConfigJson + File.separator + CONFIG_JSON).isFile() &&
                    new File(pathConfigJson + File.separator + LIST_METRICS_JSON).isFile()) {
//                System.out.println("Path from any: [" + pathConfigJson +"]");
                isConfig = new FileInputStream(pathConfigJson + File.separator + CONFIG_JSON);
                isListMetrics = new FileInputStream(pathConfigJson + File.separator + LIST_METRICS_JSON);
            } else if (new File(appPath + File.separator + CONFIG_JSON).isFile() &&
                    new File(appPath + File.separator + LIST_METRICS_JSON).isFile()) {
//               System.out.println("Path from appPath: [" + appPath +"]");
                isConfig = new FileInputStream(appPath + File.separator + CONFIG_JSON);
                isListMetrics = new FileInputStream(appPath + File.separator + LIST_METRICS_JSON);
            } else {
//                System.out.println("Path resources");
                isConfig = new ClassPathResource(CONFIG_JSON).getInputStream();
                isListMetrics = new ClassPathResource(LIST_METRICS_JSON).getInputStream();
            }
            this.sparkplugNodeConfig = JacksonUtil.fromInputToObject(isConfig, SparkplugNodeConfig.class);
            this.nodeDevices = JacksonUtil.fromInputToCollection(isListMetrics, new TypeReference<>() {
            });
            Optional<NodeDevice> nodeOpt = this.nodeDevices.stream().filter(o -> (o.getNodeDeviceId().equals(this.sparkplugNodeConfig.getEdgeNode()))).findAny();
            if (nodeOpt.isPresent()) {
                nodeOpt.get().setNode(true);
            }
            this.serverUrl = this.sparkplugNodeConfig.getServerUrl();
            this.namespace = validateNameSpace(this.sparkplugNodeConfig.getNamespace());
            this.groupId = this.sparkplugNodeConfig.getGroupId();
            this.edgeNode = this.sparkplugNodeConfig.getEdgeNode();
            this.clientId = edgeNode;
            this.username = this.sparkplugNodeConfig.getEdgeNodeToken();
            this.publishTimeout = this.sparkplugNodeConfig.getPublishTimeout();
            this.index = 0;
            this.calendar = Calendar.getInstance();
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
                    .setTimestamp(calendar.getTimeInMillis());
            deathPayload.addMetrics(createMetric(this.getBdSeqNum(), calendar.getTimeInMillis(), KEYS_BD_SEQ, Int32));
            byte[] deathBytes = deathPayload.build().toByteArray();
            this.client = createClient();
            this.mqttCallback = new SparkplugMqttCallback(this);
            this.client.setCallback(this.mqttCallback);
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setUserName(this.username);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(30);
            String topic = this.namespace + "/" + groupId + "/" + NDEATH.name() + "/" + edgeNode;
            MqttMessage msg = new MqttMessage();
            msg.setId(0);
            msg.setPayload(deathBytes);
            options.setWill(topic, msg);
            client.connect(options);
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
            if (client.isConnected()) {
                publishBirth();
                // Subscribe to control/command messages for both the edge of network node and the attached devices
                client.subscribe(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", 0);
                for (NodeDevice device : this.nodeDevices) {
                    if (!device.isNode()) {
                        client.subscribe(NAMESPACE + "/" + groupId + "/DCMD/" + edgeNode + "/" + device.getNodeDeviceId() + "/#", 0);
                    }
                }
                publishData();
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
                long ts = calendar.getTimeInMillis();
                if (nodeDeviceNames.length == 0) {
                    // Create the Node/Devices BIRTH payload with metrics
                    SparkplugBProto.Payload.Builder payloadBirthDevice;
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
                    .setTimestamp(calendar.getTimeInMillis())
                    .setSeq(getBdSeqNum());
            payloadBirthNode.addMetrics(createMetric(getBdSeqNum(), ts, KEYS_BD_SEQ, Int32));
            creatBirthMetrics(payloadBirthNode, nodeDeviceId, ts);
            String topic;
            if (edgeNode.equals(nodeDeviceId)) {
                topic = NAMESPACE + "/" + groupId + "/" + NBIRTH + "/" + edgeNode;
            } else {
                topic = NAMESPACE + "/" + groupId + "/" + NBIRTH + "/" + edgeNode + "/" + nodeDeviceId;
            }
            executor.execute(new Publisher(topic, payloadBirthNode.build()));
            log.info("Publishing [" + nodeDeviceId + "] Birth");
        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeviceId + "] publishBirth, " + e.getMessage());
        }
    }

    private void creatBirthMetrics(SparkplugBProto.Payload.Builder payload, String nodeDeiceName, long ts) throws AdaptorException {
        try {
            List<NodeDeviceMetric> nodeListMetrics = this.nodeDevices.stream().filter(
                    nodeDevice -> nodeDevice.getNodeDeviceId().equals(nodeDeiceName)).findAny().get().getNodeDeviceListMetrics();
            for (NodeDeviceMetric nodeMetric : nodeListMetrics) {
                if (Bytes.equals(nodeMetric.getTypeMetric())) {
                    byte[] valueBytes = new byte[((ArrayList) nodeMetric.getDefaultValue()).size()];
                    for (int i = 0; i < ((ArrayList) nodeMetric.getDefaultValue()).size(); i++) {
                        valueBytes[i] = ((Integer) ((ArrayList) nodeMetric.getDefaultValue()).get(i)).byteValue();
                    }
                    nodeMetric.setDefaultValue(valueBytes);
                }
                payload.addMetrics(createMetric(nodeMetric.getDefaultValue(), ts, nodeMetric.getNameMetric(), nodeMetric.getTypeMetric()));
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
                    Object value = nextValueChange(nodeMetric.getTypeMetric());
                    if (value != null) {
                        payload.addMetrics(createMetric(value, ts, nodeMetric.getNameMetric(), nodeMetric.getTypeMetric()));
                    } else {
                        throw new AdaptorException("Invalid next value for device [" + nodeDeiceName + "] publishDataMetrics, MetricDataType " + nodeMetric.getTypeMetric());
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
                            .setTimestamp(calendar.getTimeInMillis())
                            .setSeq(getBdSeqNum());
                } else {
                    payloadData = SparkplugBProto.Payload.newBuilder()
                            .setTimestamp(calendar.getTimeInMillis())
                            .setSeq(getSeqNum());
                }
                payloadDatas.put(device, payloadData);
            }

            // Loop forever publishing data every PUBLISH_PERIOD
            while (true) {
                Thread.sleep(this.publishTimeout);
                synchronized (seqLock) {
                    if (client.isConnected()) {
                        for (NodeDevice device : this.nodeDevices) {
                            // node/devices
                            ts = calendar.getTimeInMillis();
                            creatChangeMetrics(payloadDatas.get(device), device.getNodeDeviceId(), ts);
                        }
                        // Publish, increment the calendar and index and reset
                        calendar.add(Calendar.MILLISECOND, 1);
                        if (this.index == this.sparkplugNodeConfig.getIndexMax()) {
                            this.index = 0;
                            for (NodeDevice device : this.nodeDevices) {
                                if (device.isNode()) {
                                    executor.execute(new Publisher(NAMESPACE + "/" + groupId + "/" + NDATA + "/" + edgeNode, payloadDatas.get(device).build()));
                                } else {
                                    executor.execute(new Publisher(NAMESPACE + "/" + groupId + "/" + DDATA + "/" + edgeNode + "/" + device.getNodeDeviceId(), payloadDatas.get(device).build()));
                                }
                                log.info("Publishing [{}] Data", device.getNodeDeviceId());
                            }

                            payloadDatas = new ConcurrentHashMap<>();
                            for (NodeDevice device : this.nodeDevices) {
                                SparkplugBProto.Payload.Builder payloadData;
                                if (device.isNode()) {
                                    payloadData = SparkplugBProto.Payload.newBuilder()
                                            .setTimestamp(calendar.getTimeInMillis())
                                            .setSeq(getBdSeqNum());
                                } else {
                                    payloadData = SparkplugBProto.Payload.newBuilder()
                                            .setTimestamp(calendar.getTimeInMillis())
                                            .setSeq(getSeqNum());
                                }
                                payloadDatas.put(device, payloadData);
                            }
                        } else {
                            this.index++;
                            log.info("Publishing index [{}] of Data", index);
                        }
                    } else {
                        log.error("Not connected. Publishing data is bad");
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
        if (!NAMESPACE.equals(sparkplugTopic.getNamespace())) {
            throw new AdaptorException("The namespace [" + sparkplugTopic.getNamespace() + "] is not valid and must be [" + NAMESPACE + "] for the Sparkplug™ B version.");
        }
       if (!this.groupId.equals(sparkplugTopic.getGroupId())) {
            throw new AdaptorException("The groupId [" + sparkplugTopic.getGroupId() + "] is not valid and must be [" + this.groupId + "].");
        }
       if (!this.edgeNode.equals(sparkplugTopic.getEdgeNodeId())) {
            throw new AdaptorException("The edgeNode [" + sparkplugTopic.getEdgeNodeId() + "] is not valid and must be [" + this.edgeNode + "] for the Sparkplug™ B version.");
        }
        return  sparkplugTopic;
    }

    private class Publisher implements Runnable {

        private String topic;
        private SparkplugBProto.Payload outboundPayload;

        public Publisher(String topic, SparkplugBProto.Payload outboundPayload) {
            this.topic = topic;
            this.outboundPayload = outboundPayload;
        }

        public void run() {
            try {
                client.publish(topic, outboundPayload.toByteArray(), 0, false);
            } catch (MqttException mqttException) {
                mqttException.printStackTrace();
            }
        }
    }

}
