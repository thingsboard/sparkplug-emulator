package org.thinghsboard.sparkplug.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class JacksonUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T fromString(String string, TypeReference<T> valueTypeRef) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + string + " cannot be transformed to Json object", e);
        }

    }

    public static <T> T fromInputToCollection(InputStream is, TypeReference<T> valueTypeRef) {
        try {
            return is != null ? OBJECT_MAPPER.readValue(is, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given InputStream: "
                    + is + " cannot be transformed to Json object", e);
        }

    }

    public static <T> T fromInputToObject(InputStream is, Class<T> valueType) {
        try {
            return is != null ? OBJECT_MAPPER.readValue(is, valueType) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given InputStream value: "
                    + is + " cannot be transformed to Json object", e);
        }

    }
}
