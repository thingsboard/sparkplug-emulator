# This is a sample that demonstrates how a SparkplugEmulation application receive information about configuration, device names and a list of metrics for each of the devices.

You can find more information about this **SparkplugEmulation application** with **Thingsboard** [here](https://thingsboard.io/docs/reference/mqtt-sparkplug-api/)


For the successful operation of the SparkplugEmulation application, two files in json format are required:
- configuration (`Config.json`);
- device names and a list of metrics for each of the devices (`ListMetrics.json`);
  
Where might these files be located?
1. The path is specified using a environment variable: <span style="color:blue">SPARK_CONFIG_PATH</span>;
2. In the Path of the current project directory.
3. In the Path of the current project resources:  <span style="color:green">src/main/resources</span> (<i>Default</i>);
## Running application JAR:
Run the jar as always:
```
    java -jar configuration-0.0.1-SNAPSHOT.jar
```
## Default values are provided in the application.

- filePath: `src/main/resources`
- configuration (`Config.json`)
```json
{
  "serverUrl": "tcp://localhost:1883",
  "groupId": "GroupIdSparkplug",
  "edgeNode": "NodeSparkplugId",
  "edgeNodeToken": "admin",
  "publishTimeout": 10000,
  "indexMax": 50
}
```
*Note:

-**"publishTimeout"** is the time interval between the creation of new metrics.

-**"indexMax"** is the count of metric updates between a publish message sent from **Node** with SparkplugMessageType **NDATA/DDATA**.

- device names and a list of metrics for each of the devices (`Metrics.json`)

*Note:
- the value of the first **"nodeDeviceId"** in `Metrics.json` must be equal to the value of **"edgeNode"** in `Config.json`. Otherwise the value of the first **"nodeDeviceId"** in `Metrics.json` will be used as **"DeviceId"**.
```json
[
  {
    "nodeDeviceId": "NodeSparkplugId",
    "nodeDeviceListMetrics": [
      {
        "nameMetric": "Node Control/Reboot",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Node Control/Rebirth",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Node Control/Next Server",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Node Control/Scan Rate",
        "typeMetric": "Int64",
        "defaultValue": 92233720368547758,
        "autoChange": false
      },
      {
        "nameMetric": "Properties/Hardware Make",
        "typeMetric": "String",
        "defaultValue": "Properties Hardware Make: install",
        "autoChange": false
      },
      {
        "nameMetric": "Last Update FW",
        "typeMetric": "DateTime",
        "defaultValue": 1486144502122,
        "autoChange": false
      },
      {
        "nameMetric": "Current Grid Voltage",
        "typeMetric": "Float",
        "defaultValue": 220,
        "autoChange": true
      }
    ]
  },
  {
    "nodeDeviceId": "DeviceSparkplugId1",
    "nodeDeviceListMetrics": [
      {
        "nameMetric": "Device Control/Reboot",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Device Control/Rebirth",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Device Control/Next Server",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Device Control/Scan Rate",
        "typeMetric": "Int64",
        "defaultValue": 922337203685477580,
        "autoChange": false
      },
      {
        "nameMetric": "Properties/Hardware Make",
        "typeMetric": "String",
        "defaultValue": "Properties/Hardware Make - uninstall",
        "autoChange": false
      },
      {
        "nameMetric": "Last Update FW",
        "typeMetric": "DateTime",
        "defaultValue": 1486144509122,
        "autoChange": false
      },
      {
        "nameMetric": "Current Grid Voltage",
        "typeMetric": "Float",
        "defaultValue": 5.12,
        "autoChange": true
      },
      {
        "nameMetric": "Outputs/LEDs/Green",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": true
      },
      {
        "nameMetric": "Outputs/LEDs/Yellow",
        "typeMetric": "Boolean",
        "defaultValue": true,
        "autoChange": true
      }
    ]
  },
  {
    "nodeDeviceId": "DeviceSparkplugId2",
    "nodeDeviceListMetrics": [
      {
        "nameMetric": "Device Control/Reboot",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "Device Control/Rebirth",
        "typeMetric": "Boolean",
        "defaultValue": false,
        "autoChange": false
      },
      {
        "nameMetric": "MyNodeMetric01_Int32",
        "typeMetric": "Int32",
        "defaultValue": 2147483647,
        "autoChange": true
      },
      {
        "nameMetric": "MyNodeMetric02_LongInt64",
        "typeMetric": "Int64",
        "defaultValue": 9223372036854775,
        "autoChange": true
      },
      {
        "nameMetric": "MyNodeMetric03_Double",
        "typeMetric": "Double",
        "defaultValue": 9223372036854775807,
        "autoChange": true
      },
      {
        "nameMetric": "MyNodeMetric04_Float",
        "typeMetric": "Float",
        "defaultValue": 32.432,
        "autoChange": true
      },
      {
        "nameMetric": "MyNodeMetric05_String",
        "typeMetric": "String",
        "defaultValue": "MyNodeMetric05 String: changed",
        "autoChange": true
      },
      {
        "nameMetric": "MyNodeMetric06_Json_Bytes",
        "typeMetric": "Bytes",
        "defaultValue": [12, 4, -120],
        "autoChange": true
      }
    ]
  }
]
```
## Start project

### Without using SparkplugEmulation environment variables
- FilePath: <span style="color:green">/home/SparkplugB_Thingsboard/sparkplug</span> ;
- File with configuration: <span style="color:brown">Config.json</span>;
- File with device names and a list of metrics for each of the devices: <span style="color:brown">Metrics.json</span>;


### Using environment variables with SparkplugEmulation and set Environment variable option from terminal OS
- On linux, execute:
```
    export SPARKPLUG_CLIENT_CONFIG_FILE_PATH="/home/SparkplugB_Thingsboard/sparkplug/Config.json"
    export SPARKPLUG_CLIENT_METRICS_FILE_PATH="/home/SparkplugB_Thingsboard/sparkplug/Metrics.json"
    export SPARKPLUG_SERVER_URL="tcp://192.168.1.100:1883"
    export SPARKPLUG_CLIENT_GROUP_ID="GroupIdSparkplug"
    export SPARKPLUG_CLIENT_NODE_ID="NodeSparkplug"
    export SPARKPLUG_CLIENT_NODE_TOKEN="admin"
    export SPARKPLUG_PUBLISH_TIME="10000"
    export SPARKPLUG_INDEX_MAX="50"
```
- On windows, execute:
```
    C:\>SomeDir>set SPARKPLUG_CLIENT_CONFIG_FILE_PATH="/home/SparkplugB_Thingsboard/sparkplug/Config.json"
    C:\>SomeDir>set SPARKPLUG_CLIENT_METRICS_FILE_PATH="/home/SparkplugB_Thingsboard/sparkplug/Metrics.json"
    C:\>SomeDir>set SPARKPLUG_SERVER_URL="tcp://192.168.1.100:1883"
    C:\>SomeDir>set SPARKPLUG_CLIENT_GROUP_ID="GroupIdSparkplug"
    C:\>SomeDir>set SPARKPLUG_CLIENT_NODE_ID="NodeSparkplug"
    C:\>SomeDir>set SPARKPLUG_CLIENT_NODE_TOKEN="admin"
    C:\>SomeDir>set SPARKPLUG_PUBLISH_TIME="10000"
    C:\>SomeDir>set SPARKPLUG_INDEX_MAX="50"
```

### Start project from terminal OS with Docker
#### Copy Docker image and start (without git clone project)
- copy Docker image <span style="color:blue">thingsboard/tb-sparkplug-emulator</span> TAG <span style="color:blue">latest</span> from repository docker hub
```shell
docker pull nickas21/tb-sparkplug-emulation:latest
```

#### Created Docker image and start (after git clone project)

- to create a docker named <span style="color:blue">tb-sparkplug</span> run the following command in terminal from main dir`s project: <i>'./sparkplug$'</i>
```shell
./create-tb-sparkplug-docker.sh
```

#### Run the docker image
- run command from terminal: to check docker image with name <span style="color:blue">tb-sparkplug</span> installation from repository docker hub
```shell
$ docker images nickas21/tb-sparkplug-emulation
REPOSITORY                        TAG            IMAGE ID       CREATED          SIZE
nickas21/tb-sparkplug-emulation   latest   370e893e9e92   41 minutes ago   496MB
```

- run the docker image **nickas21/tb-sparkplug-emulation** with **TAG** <span style="color:blue">tb-sparkplug</span> for to connect to server with API <span style="color:green">**192.168.1.100**</span>: 
```shell
docker run -e SPARKPLUG_SERVER_URL='tcp://192.168.1.100:1883' nickas21/tb-sparkplug-emulation:latest
```

or

- run the docker image **nickas21/tb-sparkplug-emulation** with **TAG** <span style="color:blue">tb-sparkplug</span> by I**MAGE ID** = ***370e893e9e92*** for to connect to server with API <span style="color:green">**192.168.1.100**</span>: 
```shell
docker run -e SPARKPLUG_SERVER_URL='tcp://192.168.1.100:1883' 370e893e9e92
```


### Start project from terminal OS with jar
```shaell
java -jar sparkplug-1.0-SNAPSHOT-jar-with-dependencies.jar
```


