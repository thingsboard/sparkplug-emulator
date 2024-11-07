# Simple Sparkplug EoN node emulator 

This emulator was designed for demonstration purposes to connect sample clients to [Thingsboard](https://thingsboard.io) via [MQTT Sparkplug API](https://thingsboard.io/docs/reference/mqtt-sparkplug-api/).

## Running the emulator as docker container

```bash
docker run -e SPARKPLUG_SERVER_URL='tcp://thingsboard.cloud:1883' -e SPARKPLUG_CLIENT_MQTT_USERNAME='YOUR_THINGSBOARD_DEVICE_TOKEN' thingsboard/tb-sparkplug-emulator:latest
```

See all available environment variables below:

 * <code>SPARKPLUG_SERVER_URL</code> - MQTT broker URL. Default value is <code>tcp://demo.thingsboard.io:1883</code>;
 * <code>SPARKPLUG_CLIENT_GROUP_ID</code> - Sparkplug Group ID. Default value is <code>Sparkplug Group 1</code>;   
 * <code>SPARKPLUG_CLIENT_NODE_ID</code> - Sparkplug Node ID. Default value is <code>Sparkplug Node 1</code>;   
 * <code>SPARKPLUG_CLIENT_MQTT_CLIENT_ID</code> - Sparkplug MQTT Client ID. Default value is <code>Sparkplug Node 1</code>;   
 * <code>SPARKPLUG_CLIENT_MQTT_USERNAME</code> - Sparkplug MQTT Client username. See [authentication options](https://thingsboard.io/docs/user-guide/device-credentials/);    
 * <code>SPARKPLUG_CLIENT_MQTT_PASSWORD</code> - Sparkplug MQTT Client password. See [MQTT basic credentials](https://thingsboard.io/docs/user-guide/basic-mqtt/);
 * <code>SPARKPLUG_PUBLISH_INTERVAL</code> - Interval for publishing of the metrics, in milliseconds. Default value is <code>10000</code>;   
 * <code>SPARKPLUG_CLIENT_CONFIG_FILE_PATH</code> - alternative path for the configuration file. No default value. Ignored if no value set;
 * <code>SPARKPLUG_CLIENT_METRICS_FILE_PATH</code> - alternative path for the metrics descriptor file. No default value. Ignored if no value set;   

You can find more information about this **SparkplugEmulation application** with **Thingsboard** [here](https://thingsboard.io/docs/reference/mqtt-sparkplug-api/)

### Metrics

Default Sparkplug metrics descriptor is located [here](https://github.com/thingsboard/sparkplug-emulator/blob/main/src/main/resources/Metrics.json).
You may notice simple JSON structure that describes device id and a list of metrics. 
Special <code>node</code> flag in the first JSON object defines metrics for the EoN Node itself.  
You may supply your own metrics file using environment variable <code>SPARKPLUG_CLIENT_METRICS_FILE_PATH</code>

### Building from sources

```shell
mvn clean install
```

### Running as plain java application

```shell
java -jar sparkplug-1.17-jar-with-dependencies.jar
```


