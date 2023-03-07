package org.thinghsboard.sparkplug;

import lombok.Data;

import java.util.List;

@Data
public class NodeDevice {
    String nodeDeviceId;
    List<NodeDevicMetric> nodeDeviceListMetrics;
    boolean node;
}

