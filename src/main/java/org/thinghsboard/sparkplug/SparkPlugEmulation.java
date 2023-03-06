package org.thinghsboard.sparkplug;

import org.eclipse.paho.mqttv5.client.MqttClient;

import java.io.File;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An example Sparkplug B application.
 */
public class SparkPlugEmulation extends SparkplugMqttCallback {

    private static final String NAMESPACE = "spBv1.0";
    Random r = new Random();
    protected ThreadLocalRandom random = ThreadLocalRandom.current();

    // Configuration
    //	private String serverUrl = "tcp://192.168.1.100:1883";
    private String serverUrl = "tcp://localhost:1883";
    private String groupId = "MyGroupId";
    private String edgeNode = "NodeSparkplug";
    private String deviceId = "DeviceSparkplug";
    private String clientId = edgeNode;
    private String username = "admin";
    private long PUBLISH_PERIOD = 1000;                    // Publish period in milliseconds
    private ExecutorService executor;
    private MqttClient client;
    private String pathNameMetricJson = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "ListMetrics";


    private int index = 0;
    private Calendar calendar = Calendar.getInstance();

    private int bdSeq = 0;
    private int seq = 0;

    private Object seqLock = new Object();


    public static void main(String[] args) {
        SparkPlugEmulation example = new SparkPlugEmulation();
        example.run();
    }

    public void run() {
        System.out.println(deviceId);
    }

    /**
     * The DBIRTH must include every metric the device will ever report on.
     */
}
