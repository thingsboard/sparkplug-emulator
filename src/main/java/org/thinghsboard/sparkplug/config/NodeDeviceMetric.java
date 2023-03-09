package org.thinghsboard.sparkplug.config;

import lombok.Data;
import org.thinghsboard.sparkplug.util.MetricDataType;

@Data
public class NodeDeviceMetric {
    String nameMetric;
    MetricDataType typeMetric;
    Object defaultValue;
    boolean autoChange;
}
