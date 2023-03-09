package org.thinghsboard.sparkplug.config;

import lombok.Data;

import java.util.List;

@Data
public class NodeDevice {
    String nodeDeviceId;
    List<NodeDeviceMetric> nodeDeviceListMetrics;
    boolean node;
}

