package com.am.marketdata.api.config;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OpenAPI ModelConverter for Enums adhering to Clean Architecture standards.
 * Dynamically converts domain Enums to explicit OpenAPI string arrays (enum: ["VAL1", "VAL2"])
 * without polluting domain models with io.swagger.v3 annotations.
 * 
 * Inspects methods for Jackson @JsonValue or custom getApiValue() getters to produce exact
 * string representations used in HTTP JSON payloads.
 */
@Component
public class EnumModelConverter implements ModelConverter {

    private static final String[] TARGET_PACKAGES = {
        "com.am.marketdata.",
        "com.marketdata.",
        "com.am.common.investment."
    };

    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (type.isSchemaProperty()) {
            JavaType _type = io.swagger.v3.core.util.Json.mapper().constructType(type.getType());
            if (_type != null) {
                Class<?> cls = _type.getRawClass();
                if (cls != null && cls.isEnum() && isTargetPackage(cls.getName())) {
                    StringSchema schema = new StringSchema();

                    // Check if enum has a method annotated with @JsonValue or custom getApiValue()
                    Method valueMethod = findValueMethod(cls);

                    List<String> values = new ArrayList<>();
                    for (Object constant : cls.getEnumConstants()) {
                        if (valueMethod != null) {
                            try {
                                Object val = valueMethod.invoke(constant);
                                if (val != null) {
                                    values.add(val.toString());
                                } else {
                                    values.add(((Enum<?>) constant).name());
                                }
                            } catch (Exception ignored) {
                                values.add(((Enum<?>) constant).name());
                            }
                        } else {
                            values.add(((Enum<?>) constant).name());
                        }
                    }

                    schema.setEnum(values);
                    return schema;
                }
            }
        }
        if (chain.hasNext()) {
            return chain.next().resolve(type, context, chain);
        }
        return null;
    }

    private boolean isTargetPackage(String className) {
        for (String pkg : TARGET_PACKAGES) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private Method findValueMethod(Class<?> cls) {
        // 1. Check for @JsonValue annotated method
        for (Method m : cls.getMethods()) {
            if (m.isAnnotationPresent(JsonValue.class)) {
                return m;
            }
        }
        // 2. Check for getApiValue() convention method
        try {
            return cls.getMethod("getApiValue");
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }
}
