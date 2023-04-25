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
package org.thinghsboard.sparkplug.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.thinghsboard.sparkplug.config.NodeDevice;
import org.thinghsboard.sparkplug.config.SparkplugNodeConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class SparkplugUtil {

    private static final String SPARKPLUG_CLIENT_CONFIG_FILE_PATH_KEY = "SPARKPLUG_CLIENT_CONFIG_FILE_PATH";
    private static final String SPARKPLUG_CLIENT_METRICS_FILE_PATH_KEY = "SPARKPLUG_CLIENT_METRICS_FILE_PATH";
    private static final String SPARKPLUG_SERVER_URL_KEY = "SPARKPLUG_SERVER_URL";
    private static final String SPARKPLUG_CLIENT_GROUP_ID_KEY = "SPARKPLUG_CLIENT_GROUP_ID";
    private static final String SPARKPLUG_CLIENT_NODE_ID_KEY = "SPARKPLUG_CLIENT_NODE_ID";
    private static final String SPARKPLUG_CLIENT_MQTT_CLIENT_ID_KEY = "SPARKPLUG_CLIENT_MQTT_CLIENT_ID";
    private static final String SPARKPLUG_CLIENT_MQTT_USERNAME_KEY = "SPARKPLUG_CLIENT_MQTT_USERNAME";
    private static final String SPARKPLUG_CLIENT_MQTT_PASSWORD_KEY = "SPARKPLUG_CLIENT_MQTT_PASSWORD";
    private static final String SPARKPLUG_PUBLISH_INTERVAL_KEY = "SPARKPLUG_PUBLISH_INTERVAL";
    private static final String CONFIG_JSON = "Configuration.json";
    private static final String METRICS_JSON = "Metrics.json";
    private static Map<String, String> env = System.getenv();
    private static SparkplugNodeConfiguration sparkplugNodeConfiguration;

    public static SparkplugNodeConfiguration getSparkplugNodeConfig() throws IOException {
        InputStream isConfig;
        String fileConfigJson = env.get(SPARKPLUG_CLIENT_CONFIG_FILE_PATH_KEY);
        if (fileConfigJson != null && new File(fileConfigJson).isFile()) {
            isConfig = new FileInputStream(fileConfigJson);
        } else {
            isConfig = new ClassPathResource(CONFIG_JSON).getInputStream();
        }
        sparkplugNodeConfiguration = JacksonUtil.fromInputToObject(isConfig, SparkplugNodeConfiguration.class);
        if (!StringUtils.hasText(sparkplugNodeConfiguration.getClientId())) {
            sparkplugNodeConfiguration.setClientId(sparkplugNodeConfiguration.getNodeId());
        }
        updateSparkplugNodeConfig();
        return sparkplugNodeConfiguration;
    }

    public static List<NodeDevice> getNodeDevices() throws IOException {
        InputStream isListMetrics;
        String fileMetricsJson = env.get(SPARKPLUG_CLIENT_METRICS_FILE_PATH_KEY);
        if (fileMetricsJson != null && new File(fileMetricsJson).isFile()) {
            isListMetrics = new FileInputStream(fileMetricsJson);
        } else {
            isListMetrics = new ClassPathResource(METRICS_JSON).getInputStream();
        }
        List<NodeDevice> nodeDevices = JacksonUtil.fromInputToCollection(isListMetrics, new TypeReference<>() {
        });
        if (env.get(SPARKPLUG_CLIENT_NODE_ID_KEY) != null) {
            nodeDevices.get(0).setNodeDeviceId(env.get(SPARKPLUG_CLIENT_NODE_ID_KEY));
        }

        return nodeDevices;
    }

    private static void updateSparkplugNodeConfig() {
        //	private String serverUrl = "tcp://192.168.1.100:1883";
        if (env.get(SPARKPLUG_SERVER_URL_KEY) != null) {
            sparkplugNodeConfiguration.setServerUrl(env.get(SPARKPLUG_SERVER_URL_KEY));
        }
        if (env.get(SPARKPLUG_CLIENT_GROUP_ID_KEY) != null) {
            sparkplugNodeConfiguration.setGroupId(env.get(SPARKPLUG_CLIENT_GROUP_ID_KEY));
        }
        if (env.get(SPARKPLUG_CLIENT_NODE_ID_KEY) != null) {
            sparkplugNodeConfiguration.setNodeId(env.get(SPARKPLUG_CLIENT_NODE_ID_KEY));
        }
        if (env.get(SPARKPLUG_CLIENT_MQTT_CLIENT_ID_KEY) != null) {
            sparkplugNodeConfiguration.setClientId(env.get(SPARKPLUG_CLIENT_MQTT_CLIENT_ID_KEY));
        }
        if (env.get(SPARKPLUG_CLIENT_MQTT_USERNAME_KEY) != null) {
            sparkplugNodeConfiguration.setUsername(env.get(SPARKPLUG_CLIENT_MQTT_USERNAME_KEY));
        }
        if (env.get(SPARKPLUG_CLIENT_MQTT_PASSWORD_KEY) != null) {
            sparkplugNodeConfiguration.setPassword(env.get(SPARKPLUG_CLIENT_MQTT_PASSWORD_KEY));
        }
        if (env.get(SPARKPLUG_PUBLISH_INTERVAL_KEY) != null) {
            sparkplugNodeConfiguration.setPublishInterval(Integer.parseInt(env.get(SPARKPLUG_PUBLISH_INTERVAL_KEY)));
        }
    }
}

