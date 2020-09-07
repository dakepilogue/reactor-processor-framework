package com.emotibot.framework.processor.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import java.lang.reflect.Type;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;

/**
 * Http Client 工具类
 */
public class GsonUtil {

    private static Gson gson;

    static {
        final GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        builder.addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                Expose expose = f.getAnnotation(Expose.class);
                return expose != null && !expose.serialize(); //按注解排除
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        });
        builder.registerTypeAdapter(Duration.class,
            (JsonDeserializer<Duration>) (element, type, jsonSerializationContext) -> {
                if (element.isJsonPrimitive()) {
                    String value = element.getAsString().toUpperCase();
                    if (value.endsWith("MS")) {
                        double num = Integer.parseInt(value.substring(0, value.length() - 2));
                        value = (num / 1000) + "S";
                    }
                    return Duration.parse("PT" + value);
                } else {
                    return jsonSerializationContext.deserialize(element, type);
                }
            }
        );
        builder.registerTypeAdapter(Duration.class,
            (JsonSerializer<Duration>) (obj, type, jsonSerializationContext) ->
                new JsonPrimitive(obj.toString())
        );
        gson = builder.create();
    }

    public static Gson gson() {
        return gson;
    }

    public static String toJsonString(Object object) {
        return object == null ? null : gson().toJson(object);
    }

    /**
     * 将json字符串转成对象
     */
    public static <T> T jsonToObject(String str, Class<T> clazz) {
        T object = null;

        if (StringUtils.isNotBlank(str)) {
            object = gson().fromJson(str, clazz);
        }
        return object;
    }

    /**
     * 字符串转对象
     *
     * @param str  json字符串
     * @param type 对象类型
     */
    public static <T> T jsonToObject(String str, Type type) {
        T object = null;

        if (StringUtils.isNotBlank(str)) {
            object = gson().fromJson(str, type);
        }
        return object;
    }
}
