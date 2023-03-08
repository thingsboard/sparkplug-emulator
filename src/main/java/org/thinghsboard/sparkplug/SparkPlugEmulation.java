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
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.thinghsboard.gen.sparkplug.SparkplugBProto;
import org.thinghsboard.sparkplug.config.NodeDevicMetric;
import org.thinghsboard.sparkplug.config.NodeDevice;
import org.thinghsboard.sparkplug.config.SparkplugNodeConfig;
import org.thinghsboard.sparkplug.util.AdaptorException;
import org.thinghsboard.sparkplug.util.JacksonUtil;
import org.thinghsboard.sparkplug.util.MetricDataType;

import java.util.Map;

import static org.thinghsboard.sparkplug.util.MetricDataType.Int32;
import static org.thinghsboard.sparkplug.util.MetricDataType.Int64;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NBIRTH;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NDEATH;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.createMetric;

/**
 * An example Sparkplug B application.
 */
@Slf4j
public class SparkPlugEmulation extends SparkplugMqttCallback {

    private static final String SPARK_CONFIG_PATH_KEY = "SPARK_CONFIG_PATH";
    private static final String config_json = "Config.json";
    private static final String list_metrics_json = "ListMetrics.json";
    private static final String keysBdSeq = "bdSeq";

    private static final String NAMESPACE = "spBv1.0";
    Random r = new Random();
    protected ThreadLocalRandom random = ThreadLocalRandom.current();
    ObjectMapper mapper = new ObjectMapper();

    // Configuration
    //	private String serverUrl = "tcp://192.168.1.100:1883";
    private String serverUrl;
    private String namespace;
    private String groupId;
    private String edgeNode;
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
        SparkPlugEmulation demoEmulation = new SparkPlugEmulation();
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
                    new File(pathConfigJson + File.separator + config_json).isFile() &&
                    new File(pathConfigJson + File.separator + list_metrics_json).isFile()) {
//                System.out.println("Path from any: [" + pathConfigJson +"]");
                isConfig = new FileInputStream(pathConfigJson + File.separator + config_json);
                isListMetrics = new FileInputStream(pathConfigJson + File.separator + list_metrics_json);
            } else if (new File(appPath + File.separator + config_json).isFile() &&
                    new File(appPath + File.separator + list_metrics_json).isFile()) {
//               System.out.println("Path from appPath: [" + appPath +"]");
                isConfig = new FileInputStream(appPath + File.separator + config_json);
                isListMetrics = new FileInputStream(appPath + File.separator + list_metrics_json);
            } else {
//                System.out.println("Path resources");
                isConfig = new ClassPathResource(config_json).getInputStream();
                isListMetrics = new ClassPathResource(list_metrics_json).getInputStream();
            }
            this.sparkplugNodeConfig = JacksonUtil.fromInputToObject(isConfig, SparkplugNodeConfig.class);
            this.nodeDevices = JacksonUtil.fromInputToCollection(isListMetrics, new TypeReference<>() {
            });
            Optional<NodeDevice> nodeOpt = this.nodeDevices.stream().filter(o -> (o.getNodeDeviceId().equals(this.sparkplugNodeConfig.getEdgeNode()))).findAny();
            if (nodeOpt.isPresent()) {
                nodeOpt.get().setNode(true);
            }
            this.serverUrl = this.sparkplugNodeConfig.getServerUrl();
            this.namespace = this.sparkplugNodeConfig.getNamespace();
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
            MetricDataType metricDataType = Int64;
            Long valueLong = Long.valueOf(this.bdSeq);
            deathPayload.addMetrics(createMetric(valueLong, calendar.getTimeInMillis(), keysBdSeq, metricDataType));
            byte[] deathBytes = deathPayload.build().toByteArray();
            this.client = createClient();
            this.mqttCallback = new SparkplugMqttCallback();
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

    /**
     * The DBIRTH must include every metric the device will ever report on.
     */
    private void publishBirth() throws AdaptorException {
        String nodeDeiceName = null;
        try {
            synchronized (seqLock) {

                // Reset the sequence number
                seq = 0;

                // Reset the index and time
                index = 0;
                long ts = calendar.getTimeInMillis();

                // Create the Node BIRTH payload and set the position and other metrics
                SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                        .setTimestamp(calendar.getTimeInMillis());
                payloadBirthNode.addMetrics(createMetric(getBdSeqNum(), ts, keysBdSeq, Int32));
                List<NodeDevicMetric> nodeListMetrics = this.nodeDevices.stream().filter(node -> node.isNode()).findAny().get().getNodeDeviceListMetrics();
                nodeDeiceName = this.edgeNode;
                for (NodeDevicMetric nodeMetric : nodeListMetrics) {
                    payloadBirthNode.addMetrics(createMetric(nodeMetric.getDefaultValue(), ts, nodeMetric.getNameMetric(), nodeMetric.getTypeMetric()));
                }
                if (client.isConnected()) {
                    log.info("Publishing Edge Node Birth");
                    executor.execute(new Publisher(NAMESPACE + "/" + groupId + "/NBIRTH/" + edgeNode, payloadBirthNode.build()));
                }

                log.info("Publishing Device Birth");
//                nodeDeiceName =  deviceId;
//                executor.execute(new Publisher(NAMESPACE + "/" + groupId + "/DBIRTH/" + edgeNode + "/" + deviceId, payload));

                // Increment the global vars
                calendar.add(Calendar.MILLISECOND, 1);
                index++;
            }
        } catch (Exception e) {
            throw new AdaptorException("Invalid device [" + nodeDeiceName +"] publishBirth, " + e.getMessage());
        }
    }

    private MqttAsyncClient createClient() throws MqttException {
        return new MqttAsyncClient(this.serverUrl, this.clientId, new MemoryPersistence());
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

    private class Publisher implements Runnable {

        private String topic;
        private SparkplugBProto.Payload outboundPayload;

        public Publisher(String topic, SparkplugBProto.Payload outboundPayload) {
            this.topic = topic;
            this.outboundPayload = outboundPayload;
        }

        public void run() {
            try {
                client.publish(NAMESPACE + "/" + groupId + "/" + NBIRTH.name() + "/" + edgeNode,
                        outboundPayload.toByteArray(), 0, false);
            } catch (MqttException mqttException) {
                mqttException.printStackTrace();
            }
        }
    }

}
