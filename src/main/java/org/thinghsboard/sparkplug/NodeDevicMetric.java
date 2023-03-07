package org.thinghsboard.sparkplug;

import lombok.Data;
import org.thinghsboard.sparkplug.Util.MetricDataType;

@Data
public class NodeDevicMetric {
    String nameMetric;
    MetricDataType typeMetric;
}
