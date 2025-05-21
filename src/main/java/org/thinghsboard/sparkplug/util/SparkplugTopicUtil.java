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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nickAS21 on 10.01.23
 */
public class SparkplugTopicUtil {

    private static final Map<String, String[]> SPLIT_TOPIC_CACHE = new HashMap<String, String[]>();
    private static final String TOPIC_INVALID_NUMBER = "Invalid number of topic elements: ";
    /**
     * https://www.eclipse.org/tahu/spec/sparkplug_spec.pdf
     * [tck-id-topic-structure-namespace-a] For the Sparkplug B version of the payload definition, the
     * UTF-8 string constant for the namespace element MUST be:
     * spBv1.0
     * https://forum.cirrus-link.com/t/sparkplug-b-namespace-when-above-version-1-0/310/3
     * The current topic token for use with Sparkplug v3.0.0 (per the spec) is still spBv1.0.
     * Nothing fundamentally changed between v2.2 and v3.0.0 that required a change in this token.
     */
    public static final String SPARKPLUG_CLIENT_NAME_SPACE = "spBv1.0";

    public static String[] getSplitTopic(String topic) {
        String[] splitTopic = SPLIT_TOPIC_CACHE.get(topic);
        if (splitTopic == null) {
            splitTopic = topic.split("/");
            SPLIT_TOPIC_CACHE.put(topic, splitTopic);
        }

        return splitTopic;
    }

    public static SparkplugTopic parseTopicPublish(String topic) throws AdaptorException {
        if (topic.contains("#") || topic.contains("$") || topic.contains("+")) {
            throw new AdaptorException("Invalid of topic elements for Publish");
        } else {
            String[] splitTopic = SparkplugTopicUtil.getSplitTopic(topic);
            if (splitTopic.length < 4 || splitTopic.length > 5) {
                throw new AdaptorException(TOPIC_INVALID_NUMBER + splitTopic.length);
            }
            return parseTopic(splitTopic);
        }
    }

    /**
     * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
     *
     * @param splitTopic a topic split into tokens
     * @return a {@link SparkplugTopic} instance
     * @throws Exception if an error occurs while parsing
     */
    @SuppressWarnings("incomplete-switch")
    public static SparkplugTopic parseTopic(String[] splitTopic) throws AdaptorException {
        int length = splitTopic.length;
        if (length == 0) {
            throw new AdaptorException(TOPIC_INVALID_NUMBER + length);
        } else {
            SparkplugMessageType type;
            String namespace, edgeNodeId, groupId, deviceId;
            namespace = validateNameSpace(splitTopic[0]);
            groupId = length > 1 ? splitTopic[1] : null;
            type = length > 2 ? SparkplugMessageType.parseMessageType(splitTopic[2]) : null;
            edgeNodeId = length > 3 ? splitTopic[3] : null;
            deviceId = length > 4 ? splitTopic[4] : null;
            return new SparkplugTopic(namespace, groupId, edgeNodeId, deviceId, type);
        }
    }

    /**
     * For the Sparkplug™ B version of the specification, the UTF-8 string constant for the namespace element will be: "spBv1.0"
     * @param nameSpace
     * @return
     */
    public static String validateNameSpace(String nameSpace)  throws AdaptorException {
        if (SPARKPLUG_CLIENT_NAME_SPACE.equals(nameSpace)) return nameSpace;
        throw new AdaptorException("The namespace [" + nameSpace + "] is not valid and must be [" + SPARKPLUG_CLIENT_NAME_SPACE + "] for the Sparkplug™ B version.");
    }
}

