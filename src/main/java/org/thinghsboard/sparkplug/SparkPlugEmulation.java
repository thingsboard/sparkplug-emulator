package org.thinghsboard.sparkplug;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.thinghsboard.gen.sparkplug.SparkplugBProto;
import org.thinghsboard.sparkplug.Util.JacksonUtil;
import org.thinghsboard.sparkplug.Util.MetricDataType;

import java.util.Map;

import static org.thinghsboard.sparkplug.Util.MetricDataType.Int64;
import static org.thinghsboard.sparkplug.Util.SparkplugMessageType.NBIRTH;
import static org.thinghsboard.sparkplug.Util.SparkplugMessageType.NDEATH;
import static org.thinghsboard.sparkplug.Util.SparkplugMetricUtil.createMetric;

/**
 * An example Sparkplug B application.
 */

public class SparkPlugEmulation extends SparkplugMqttCallback {

    private static final String SPARK_CONFIG_KEY = "--spark.config.";
    private static final String SPARK_CONFIG_PATH_KEY = SPARK_CONFIG_KEY + "path";
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

    private long PUBLISH_PERIOD;                    // Publish period in milliseconds
    private int index;
    private Calendar calendar;

    private int bdSeq;
    private int seq;

    private Object seqLock;
    private SparkplugMqttCallback mqttCallback;


    public static void main(String[] args) {
        SparkPlugEmulation demoEmulation = new SparkPlugEmulation();
        demoEmulation.init(args);
        demoEmulation.run();
    }

    /**
     * @param args
     * @throws IOException
     */
    public void init(String[] args) {
        try {
            Map<String, String> env = System.getenv();
            InputStream isConfig;
            InputStream isListMetrics;
            if (env.containsKey(SPARK_CONFIG_PATH_KEY)) {
                String pathConfigJson = env.get(SPARK_CONFIG_PATH_KEY);
                isConfig = new FileInputStream(pathConfigJson + File.separator + config_json);
                isListMetrics= new FileInputStream(pathConfigJson + File.separator + list_metrics_json);
            } else {
                isConfig = new ClassPathResource(config_json).getInputStream();
                isListMetrics = new ClassPathResource(list_metrics_json).getInputStream();
            }
            this.sparkplugNodeConfig = JacksonUtil.fromInputToObject(isConfig, SparkplugNodeConfig.class);
            this.nodeDevices = JacksonUtil.fromInputToCollection(isListMetrics, new TypeReference<>() {});
            Optional<NodeDevice> nodeOpt = this.nodeDevices.stream().filter(o -> (o.nodeDeviceId.equals(this.sparkplugNodeConfig.getEdgeNode()))).findAny();
            if (nodeOpt.isPresent()) {
                nodeOpt.get().setNode(true);
            }
            this.serverUrl = this.sparkplugNodeConfig.getServerUrl();
            this.namespace = this.sparkplugNodeConfig.getNamespace();
            this.groupId = this.sparkplugNodeConfig.getGroupId();
            this.edgeNode = this.sparkplugNodeConfig.getEdgeNode();
            this.clientId = edgeNode;
            this.username = this.sparkplugNodeConfig.getEdgeNodeToken();
            this.PUBLISH_PERIOD = 1000;
            this.index = 0;
            this.calendar = Calendar.getInstance();
            this.bdSeq = 0;
            this.seq = 0;
            this.seqLock = new Object();
            System.out.println(this.sparkplugNodeConfig);
            System.out.println(this.nodeDevices);
        } catch (Exception e) {
            System.out.println("" + e.getMessage());
        }
    }

    public void run() {
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
            String topic = this.namespace + "/" + groupId + "/" + NDEATH.name() + "/" + edgeNode;
            MqttMessage msg = new MqttMessage();
            msg.setId(0);
            msg.setPayload(deathBytes);
            options.setWill(topic, msg);
            client.connect(options);
            if (client.isConnected()) {
                publishBirth();
                // Subscribe to control/command messages for both the edge of network node and the attached devices
                client.subscribe(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", 0);
                for (NodeDevice device : this.nodeDevices) {
                    if (!device.isNode()) {
                        client.subscribe(NAMESPACE + "/" + groupId + "/DCMD/" + edgeNode + "/" + device.nodeDeviceId + "/#", 0);
                    }
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The DBIRTH must include every metric the device will ever report on.
     */
    private void publishBirth() {
        try {
            synchronized(seqLock) {
                // Reset the sequence number
                seq = 0;

                // Reset the index and time
                index = 0;
                long ts = calendar.getTimeInMillis();
                long valueBdSec = getBdSeqNum();

                // Create the Node BIRTH payload and set the position and other metrics
                SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                        .setTimestamp(calendar.getTimeInMillis());
                payloadBirthNode.addMetrics(createMetric(valueBdSec, ts, keysBdSeq, Int64));
                List<NodeDevicMetric> nodeListMetrics = this.nodeDevices.stream().filter(node -> node.isNode()).findAny().get().getNodeDeviceListMetrics();

                for (NodeDevicMetric nodeMetric: nodeListMetrics) {
                    Object value = false;
                    payloadBirthNode.addMetrics(createMetric(value, ts, nodeMetric.getNameMetric(), nodeMetric.getTypeMetric()));
                }
                if (client.isConnected()) {
                    System.out.println("Publishing Edge Node Birth");
                    executor.execute(new Publisher(NAMESPACE + "/" + groupId + "/NBIRTH/" + edgeNode,  payloadBirthNode.build()));
                }

                System.out.println("Publishing Device Birth");
//                executor.execute(new Publisher(NAMESPACE + "/" + groupId + "/DBIRTH/" + edgeNode + "/" + deviceId, payload));

                // Increment the global vars
                calendar.add(Calendar.MILLISECOND, 1);
                index++;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private MqttAsyncClient createClient() throws MqttException {
        return new MqttAsyncClient(this.serverUrl, this.clientId, new MemoryPersistence());
    }


    protected long getBdSeqNum() throws Exception {
        if (bdSeq == 256) {
            bdSeq = 0;
        }
        return bdSeq++;
    }

    protected long getSeqNum() throws Exception {
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
