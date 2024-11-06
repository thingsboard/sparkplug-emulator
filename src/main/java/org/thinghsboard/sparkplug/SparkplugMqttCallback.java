/**
 * Copyright © ${project.inceptionYear}-2024 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.thinghsboard.gen.sparkplug.SparkplugBProto;
import org.thinghsboard.sparkplug.util.SparkplugTopic;

import java.util.ArrayList;
import java.util.List;

import static org.thinghsboard.sparkplug.util.SparkplugMessageType.DCMD;
import static org.thinghsboard.sparkplug.util.SparkplugMessageType.NCMD;
import static org.thinghsboard.sparkplug.util.SparkplugMetricUtil.getValue;
import static org.thinghsboard.sparkplug.util.SparkplugTopicUtil.parseTopicPublish;

/**
 * Created by nickAS21 on 10.01.23
 */
@Slf4j
public class SparkplugMqttCallback implements MqttCallback {

    private SparkplugEmulation sparkPlugEmulation;

    public SparkplugMqttCallback(SparkplugEmulation sparkPlugEmulation) {
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
        SparkplugTopic sparkplugTopic = this.sparkPlugEmulation.validateTopic(parseTopicPublish(topic));
        String nodeDeviceId = sparkplugTopic.isNode() ? sparkplugTopic.getEdgeNodeId() : sparkplugTopic.getDeviceId();
        // Debug
        log.info("Command: [{}]  nodeDeviceId: [{}]", sparkplugTopic.getType().name(), sparkplugTopic.isNode() ? sparkplugTopic.getEdgeNodeId() : sparkplugTopic.getDeviceId());
        for (SparkplugBProto.Payload.Metric metric : sparkplugBProtoPayload.getMetricsList()) {
            log.info("Metric [{}] value [{}]", metric.getName(), getValue(metric));
        }
        if (NCMD.equals(sparkplugTopic.getType()) || DCMD.equals(sparkplugTopic.getType())) {
            for (SparkplugBProto.Payload.Metric metric : sparkplugBProtoPayload.getMetricsList()) {
                if (("Node Control/Rebirth".equals(metric.getName()) || "Node Control/Reboot".equals(metric.getName()) ||
                        "Device Control/Rebirth".equals(metric.getName()) || "Device Control/Reboot".equals(metric.getName()))
                        && (metric.getBooleanValue())) {
                    this.sparkPlugEmulation.publishBirth(nodeDeviceId);
                }
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {

    }

    @Override
    public void connectComplete(boolean b, String s) {
        log.debug("Connected! - publishing birth!!!");
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