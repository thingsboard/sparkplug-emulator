package org.thinghsboard.sparkplug.config;

import lombok.Data;

@Data
public class SparkplugNodeConfig {
    String serverUrl;
    long publishTimeout;
    int indexMax;
    String namespace;
    String groupId;
    String edgeNode;
    String edgeNodeToken;
}
