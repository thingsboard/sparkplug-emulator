package org.thinghsboard.sparkplug;

import lombok.Data;

@Data
public class SparkplugNodeConfig {
    String serverUrl;
    String namespace;
    String groupId;
    String edgeNode;
    String edgeNodeToken;
}
