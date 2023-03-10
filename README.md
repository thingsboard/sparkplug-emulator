# This is a sample that demonstrates how a SparkplugEmulation application receive information about configuration, device names and a list of metrics for each of the devices.

You can find more information about this **SparkplugEmulation application** with **Thingsboard** [here](https://thingsboard.io/docs/reference/mqtt-sparkplug-api/)
You can find more information about this **SparkplugEmulation application** with **Thingsboard** [here](http://0.0.0.0:4000/docs/reference/mqtt-sparkplug-api/)


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
  "publishTimeout": 10000,
  "indexMax": 50,
  "namespace": "spBv1.0",
  "groupId": "MyGroupId",
  "edgeNode": "NodeSparkplug",
  "edgeNodeToken": "admin"
}
```
*Note:

-**"publishTimeout"** is the time interval between the creation of new metrics.

-**"indexMax"** is the count of metric updates between a publish message sent from **Node** with SparkplugMessageType **NDATA/DDATA**.

- device names and a list of metrics for each of the devices (`ListMetrics.json`)
```json
[
  {
    "nodeDeviceId": "NodeSparkplug",
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
```shell
    private static final String config_json = "Config.json";
    
    InputStream isConfig = new ClassPathResource(config_json).getInputStream();    

    private static final String list_metrics_json = "ListMetrics.json";
    
    InputStream isListMetrics = new ClassPathResource(list_metrics_json).getInputStream();    
```
## Without using SparkplugEmulation environment variables
- FilePath: <span style="color:green">/home/SparkplugB_Thingsboard/sparkplug</span> ;
- File with configuration: <span style="color:brown">Config.json</span>;
- File with device names and a list of metrics for each of the devices: <span style="color:brown">ListMetrics.json</span>;


## Using environment variables with SparkplugEmulation

This is a sample application that demonstrates how to set up a SparkplugEmulation application through an environment
variables.
It expects to receive information about configuration, device names and a list of metrics for each of the devices.
- FilePath: <span style="color:green">/home/SparkplugB_Thingsboard/sparkplug</span> ;
- File with configuration: <span style="color:brown">Config.json</span>;
- File with device names and a list of metrics for each of the devices: <span style="color:brown">ListMetrics.json</span>;

### Environment variable ih Run/Debug Configuration project
```
    SPARK_CONFIG_PATH=/home/SparkplugB_Thingsboard/sparkplug
```

### Set Environment variable option while running JAR
You can set environment variables by assigning them on the command line.
For example, to set `SPARK_CONFIG_PATH` to `/home/SparkplugB_Thingsboard/sparkplug`, you could
run this application as follows.
- On linux, execute:
```
    export SPARK_CONFIG_PATH="/home/SparkplugB_Thingsboard/sparkplug"
```
- On windows, execute:
```
    C:\>SomeDir>set SPARK_CONFIG_PATH="/home/SparkplugB_Thingsboard/sparkplug"
```
- then run the jar as always:
```
    java -jar configuration-0.0.1-SNAPSHOT.jar
```

