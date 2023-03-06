package org.thinghsboard.sparkplug;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.thinghsboard.gen.sparkplug.SparkplugBProto;

import java.util.ArrayList;
import java.util.List;

public class SparkplugMqttCallback  implements MqttCallback {
    private final List<SparkplugBProto.Payload.Metric> messageArrivedMetrics = new ArrayList<>();

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

    }

    @Override
    public void mqttErrorOccurred(MqttException e) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMsg) throws Exception {
        SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(mqttMsg.getPayload());
        messageArrivedMetrics.addAll(sparkplugBProtoNode.getMetricsList());
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {

    }

    @Override
    public void connectComplete(boolean b, String s) {

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