/**
 * Copyright © ${project.inceptionYear}-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thinghsboard.sparkplug.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.FileSerializer;
import com.google.protobuf.ByteString;

import org.thinghsboard.gen.sparkplug.SparkplugBProto;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.thinghsboard.sparkplug.util.MetricDataType.fromInteger;


/**
 * Provides utility methods for SparkplugB MQTT Payload Metric.
 */
/**
 * Created by nickAS21 on 10.01.23
 */
public class SparkplugMetricUtil {

    protected static ThreadLocalRandom random = ThreadLocalRandom.current();
    public static final String KEYS_BD_SEQ = "bdSeq";

    public static SparkplugBProto.Payload.Metric createMetric(Object value, long ts, String key, MetricDataType metricDataType) throws AdaptorException {
        try {
            SparkplugBProto.Payload.Metric metric = SparkplugBProto.Payload.Metric.newBuilder()
                    .setTimestamp(ts)
                    .setName(key)
                    .setDatatype(metricDataType.toIntValue())
                    .build();
            switch (metricDataType) {
                case Int8:      //  (byte)
                    return metric.toBuilder().setIntValue(Byte.valueOf(value.toString()).intValue()).build();
                case Int16:     // (short)
                case UInt8:
                    return metric.toBuilder().setIntValue(Short.valueOf(value.toString()).intValue()).build();
                case UInt16:     //  (int)
                case Int32:
                    return metric.toBuilder().setIntValue(Integer.valueOf(value.toString()).intValue()).build();
                case UInt32:     // (long)
                case Int64:
                case UInt64:
                case DateTime:
                    return metric.toBuilder().setLongValue(Long.valueOf(value.toString()).longValue()).build();
                case Float:     // (float)
                    return metric.toBuilder().setFloatValue((Float.valueOf(value.toString())).floatValue()).build();
                case Double:     // (double)
                    return metric.toBuilder().setDoubleValue(Double.valueOf(value.toString()).doubleValue()).build();
                case Boolean:      // (boolean)
                    return metric.toBuilder().setBooleanValue(Boolean.valueOf(value.toString()).booleanValue()).build();
                case String:        // String)
                case Text:
                case UUID:
                    return metric.toBuilder().setStringValue((String) value).build();
                case Bytes:
                    ByteString byteString = ByteString.copyFrom((byte[]) value);
                    return metric.toBuilder().setBytesValue(byteString).build();
                case DataSet:
                    return metric.toBuilder().setDatasetValue((SparkplugBProto.Payload.DataSet) value).build();
                case File:
                    File file = (File) value;
                    ByteString byteFileString = ByteString.copyFrom(file.getBytes());
                    return metric.toBuilder().setBytesValue(byteFileString).build();
                case Template:
                    return metric.toBuilder().setTemplateValue((SparkplugBProto.Payload.Template) value).build();
                case Unknown:
                    throw new AdaptorException("Invalid value for MetricDataType " + metricDataType.name());
            }
            return metric;
        } catch (Exception e) {
            throw new AdaptorException("Invalid metric [" + key + "] value [" + value + "]  MetricDataType [" + metricDataType.name() + "]");
        }
    }

    public static Optional<Object> validatedValueJsonByTypeMetric(String arrayNodeStr, MetricDataType metricDataType) {
        try {
            Optional<Object> valueOpt;
            switch (metricDataType) {
                // byte[]
                case Bytes:
                    List<Byte> listBytes = JacksonUtil.fromString(arrayNodeStr, new TypeReference<>() {});
                    byte[] bytes = new byte[listBytes.size()];
                    for (int i = 0; i < listBytes.size(); i++) {
                        bytes[i] = listBytes.get(i).byteValue();
                    }
                    return Optional.of(bytes);
                case DataSet:
                case File:
                case Template:
                    return Optional.empty();
                case Unknown:
                default:
                    return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Object getValue(SparkplugBProto.Payload.Metric metric) {
        switch (fromInteger(metric.getDatatype())) {
            case Bytes:
                return metric.getBytesValue().toByteArray();
            case Int8:
            case Int16:
            case UInt8:
            case Int32:
                return metric.getIntValue();
            case UInt32:     // (long)
            case Int64:
            case UInt64:
            case DateTime:
                return metric.getLongValue();
            case Double:
                return metric.getDoubleValue();
            case Float:
                return metric.getFloatValue();
            case Boolean:
                return metric.getBooleanValue();
            case String:        // String)
            case Text:
            case UUID:
                return metric.getStringValue();
            case Unknown:
        }
        return null;
    }


    public static Object nextValueChange(MetricDataType metricDataType) {
        switch (metricDataType) {
            case Bytes:
                return new byte[]{nextByte(), nextByte(), nextByte(), nextByte()};
            case Int8:
                return nextInt8();
            case Int16:
            case UInt8:
                return nextInt16();
            case Int32:
                return nextInt32();
            case UInt32:     // (long)
            case Int64:
            case UInt64:
            case DateTime:
                return nextInt64();
            case Double:
                return nextDouble();
            case Float:
                return nextFloat(150, 290);
            case Boolean:
                return nextBoolean();
            case String:        // String)
            case Text:
            case UUID:
                return nexString();
            case Unknown:
        }
        return null;
    }

    private static byte nextByte() {
        return (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    private static Byte nextInt8() {
        return Byte.valueOf(String.valueOf(random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE)));
    }


    private static Short nextInt16() {
        return Short.parseShort(String.valueOf(random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE)));
    }

    private static Integer nextInt32() {
        return random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static Long nextInt64() {
        return random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static Double nextDouble() {
        return random.nextDouble(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static float nextFloat(float min, float max) {
        if (min >= max)
            throw new IllegalArgumentException("max must be greater than min");
        float result = ThreadLocalRandom.current().nextFloat() * (max - min) + min;
        if (result >= max) // correct for rounding
            result = Float.intBitsToFloat(Float.floatToIntBits(max) - 1);
        return result;
    }

    private static boolean nextBoolean() {
        return random.nextBoolean();
    }

    private static String nexString() {
        return java.util.UUID.randomUUID().toString();
    }


    @JsonIgnoreProperties(
            value = {"fileName"})
    @JsonSerialize(
            using = FileSerializer.class)
    public class File {

        private String fileName;
        private byte[] bytes;

        /**
         * Default Constructor
         */
        public File() {
            super();
        }

        /**
         * Constructor
         *
         * @param fileName the full file name path
         * @param bytes    the array of bytes that represent the contents of the file
         */
        public File(String fileName, byte[] bytes) {
            super();
            this.fileName = fileName == null
                    ? null
                    : fileName.replace("/", System.getProperty("file.separator")).replace("\\",
                    System.getProperty("file.separator"));
            this.bytes = Arrays.copyOf(bytes, bytes.length);
        }

        /**
         * Gets the full filename path
         *
         * @return the full filename path
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Sets the full filename path
         *
         * @param fileName the full filename path
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Gets the bytes that represent the contents of the file
         *
         * @return the bytes that represent the contents of the file
         */
        public byte[] getBytes() {
            return bytes;
        }

        /**
         * Sets the bytes that represent the contents of the file
         *
         * @param bytes the bytes that represent the contents of the file
         */
        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("File [fileName=");
            builder.append(fileName);
            builder.append(", bytes=");
            builder.append(Arrays.toString(bytes));
            builder.append("]");
            return builder.toString();
        }
    }

}
