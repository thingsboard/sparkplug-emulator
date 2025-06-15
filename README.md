
# Simple Sparkplug EoN Node Emulator

This emulator is designed for demonstration purposes to connect sample clients to [Thingsboard](https://thingsboard.io) via the [MQTT Sparkplug API](https://thingsboard.io/docs/reference/mqtt-sparkplug-api/).

---

## 🐳 Running he Emulator as a Docker Container


```bash
docker run -e SPARKPLUG_SERVER_URL='tcp://thingsboard.cloud:1883' \
  -e SPARKPLUG_CLIENT_MQTT_USERNAME='YOUR_THINGSBOARD_DEVICE_TOKEN' \
  thingsboard/tb-sparkplug-emulator:latest
```

> **Note:** Do not use `localhost` inside a Docker container — it refers to the container itself. Use your actual IP address (e.g., `192.168.x.x`) or `host.docker.internal` (on macOS/Windows). On Linux, consider using `--network host`.

- localhost -> Address = 192.168.28.74; Port = 1883, YOUR_THINGSBOARD_DEVICE_TOKEN = n03suscyeul0o1dn6b09.

```bash
docker run -e SPARKPLUG_SERVER_URL='tcp://t192.168.28.74:1883' \
  -e SPARKPLUG_CLIENT_MQTT_USERNAME='n03suscyeul0o1dn6b09' \
  thingsboard/tb-sparkplug-emulator:latest
```

---

## Available Environment Variables

| Variable                             | Description                                                   | Default Value                    |
|:-------------------------------------|:--------------------------------------------------------------|:---------------------------------|
| `SPARKPLUG_SERVER_URL`               | MQTT broker URL                                               | `tcp://demo.thingsboard.io:1883` |
| `SPARKPLUG_CLIENT_GROUP_ID`          | Sparkplug Group ID                                            | `Sparkplug Group 1`              |
| `SPARKPLUG_CLIENT_NODE_ID`           | Sparkplug Node ID                                             | `Sparkplug Node 1`               |
| `SPARKPLUG_CLIENT_MQTT_CLIENT_ID`    | Sparkplug MQTT Client ID                                      | `Sparkplug Node 1`               |
| `SPARKPLUG_CLIENT_MQTT_USERNAME`     | MQTT client username (for authentication)                     | —                                |
| `SPARKPLUG_CLIENT_MQTT_PASSWORD`     | MQTT client password                                          | —                                |
| `SPARKPLUG_PUBLISH_INTERVAL`         | Interval for publishing metrics in milliseconds               | `10000`                          |
| `SPARKPLUG_CLIENT_CONFIG_FILE_PATH`  | Alternative path for configuration file                       | —                                |
| `SPARKPLUG_CLIENT_METRICS_FILE_PATH` | Alternative path for metrics descriptor file                  | —                                |

---

## Metrics

The default Sparkplug metrics descriptor is located here:

[Metrics.json](https://github.com/thingsboard/sparkplug-emulator/blob/main/src/main/resources/Metrics.json)

This file contains a simple JSON structure describing the device ID and a list of metrics.  
The special `node` flag in the first JSON object defines metrics for the EoN Node itself.  
You can supply your own metrics file using the environment variable `SPARKPLUG_CLIENT_METRICS_FILE_PATH`.

---

## Building from Sources

```bash
mvn clean install
```

or

```bash
mvn clean package -DskipTests
```

---

## Running as a Plain Java Application

### Configuring Sparkplug Emulator with Environment Variables

Set environment variables:

```bash
export SPARKPLUG_SERVER_URL=tcp://demo.thingsboard.io:1883
export SPARKPLUG_CLIENT_MQTT_USERNAME=YOUR_THINGSBOARD_DEVICE_TOKEN
```

### Running the application
- 
- version can vary

```bash
java -jar sparkplug-{version}-jar-with-dependencies.jar
```

or

```bash
java -jar sparkplug-3.0.1-jar-with-dependencies.jar
```

---

### Example running with a local MQTT broker:

```bash
SPARKPLUG_SERVER_URL=tcp://localhost:1883 \
SPARKPLUG_CLIENT_MQTT_USERNAME=n03suscyeul0o1dn6b09 \
java -jar sparkplug-3.0.1-jar-with-dependencies.jar
```

or

```bash
export SPARKPLUG_SERVER_URL=tcp://localhost:1883
export SPARKPLUG_CLIENT_MQTT_USERNAME=n03suscyeul0o1dn6b09
java -jar sparkplug-3.0.1-jar-with-dependencies.jar
```

---

### Running again with Docker

```bash
docker run -e SPARKPLUG_SERVER_URL='tcp://demo.thingsboard.io:1883' \
  -e SPARKPLUG_CLIENT_MQTT_USERNAME='YOUR_THINGSBOARD_DEVICE_TOKEN' \
  thingsboard/tb-sparkplug-emulator:latest
```

---

For more detailed information, see the [Thingsboard Sparkplug API documentation](https://thingsboard.io/docs/reference/mqtt-sparkplug-api/).

---

If you need, I can help add clarifications or improvements.
