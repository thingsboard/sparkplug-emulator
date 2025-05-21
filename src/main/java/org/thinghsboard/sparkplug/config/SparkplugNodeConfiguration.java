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
package org.thinghsboard.sparkplug.config;

import lombok.Data;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Created by nickAS21 on 10.01.23
 */
@Data
public class SparkplugNodeConfiguration {

    private String serverUrl;
    private String groupId;
    private String nodeId;
    private String clientId;
    private String username;
    private String password;

    private long publishInterval;

    public MqttConnectionOptions toMqttConnectOptions() {
        var options = new MqttConnectionOptions();
        if (StringUtils.hasText(username)) {
            options.setUserName(username);
        }
        if (StringUtils.hasText(password)) {
            options.setPassword(password.getBytes(StandardCharsets.UTF_8));
        }
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(30);
        if (options.getMqttVersion() == 5) {
            options.setSessionExpiryInterval(0L);
        }
        return options;
    }
}

