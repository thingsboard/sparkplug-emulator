package org.thinghsboard.sparkplug;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.thinghsboard.gen.sparkplug.SparkplugBProto;
import org.thinghsboard.sparkplug.util.AdaptorException;
import org.thinghsboard.sparkplug.util.MetricDataType;

import java.util.ArrayList;
import java.util.List;

import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.getValue;

@Slf4j
public class SparkplugMqttCallback  implements MqttCallback {

    private SparkPlugEmulation sparkPlugEmulation;

    public SparkplugMqttCallback (SparkPlugEmulation sparkPlugEmulation) {
        this.sparkPlugEmulation = sparkPlugEmulation;
    }

    private final List<SparkplugBProto.Payload.Metric> messageArrivedMetrics = new ArrayList<>();

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

    }

    @Override
    public void mqttErrorOccurred(MqttException e) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMsg) throws Exception {
        log.info("Message Arrived on topic " + topic);
        SparkplugBProto.Payload sparkplugBProtoPayload = SparkplugBProto.Payload.parseFrom(mqttMsg.getPayload());

        // Debug
        for (SparkplugBProto.Payload.Metric metric : sparkplugBProtoPayload.getMetricsList()) {
                log.info("Metric [{}] value [{}]", metric.getName(), getValue(metric) );
        }

        String[] splitTopic = topic.split("/");
        if (splitTopic[0].equals(this.sparkPlugEmulation.NAMESPACE) &&
                splitTopic[1].equals(this.sparkPlugEmulation.groupId) &&
                splitTopic[2].equals("NCMD") &&
                splitTopic[3].equals(this.sparkPlugEmulation.edgeNode)) {
            for (SparkplugBProto.Payload.Metric metric : sparkplugBProtoPayload.getMetricsList()) {
                if ("Node Control/Rebirth".equals(metric.getName()) && (metric.getBooleanValue())) {
                    this.sparkPlugEmulation.publishBirth();
                } else {
                    System.out.println("Node Command NCMD: " + metric.getName());
                }
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {

    }

    @Override
    public void connectComplete(boolean b, String s) {
        log.info("Connected! - publishing birth!!!");
    }

    @Override
    public void authPacketArrived(int i, MqttProperties mqttProperties) {

    }

    public List<SparkplugBProto.Payload.Metric> getMessageArrivedMetrics() {
        return messageArrivedMetrics;
    }

    public void deleteMessageArrivedMetrics(int id) {
        messageArrivedMetrics.remove(id);
    }
}