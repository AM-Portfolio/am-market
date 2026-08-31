package com.am.marketdata.api.config;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clean Architecture OpenAPI ModelConverter for Enums.
 * Dynamically converts domain enums to explicit JSON string arrays (enum: ["VAL1", "VAL2"])
 * inside the OpenAPI schema without requiring @Schema annotations on domain models.
 * Formats enum descriptions explicitly as JSON array vectors ["VAL1", "VAL2", ...] for clear visibility.
 */
@Component
public class EnumModelConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Class<?> cls = null;
        if (type.getType() instanceof Class<?>) {
            cls = (Class<?>) type.getType();
        } else if (type.getType() != null) {
            JavaType _type = Json.mapper().constructType(type.getType());
            if (_type != null) {
                cls = _type.getRawClass();
            }
        }

        if (cls != null && cls.isEnum()) {
            StringSchema schema = new StringSchema();
            List<String> values = Arrays.stream(cls.getEnumConstants())
                    .map(c -> ((Enum<?>) c).name())
                    .collect(Collectors.toList());
            schema.setEnum(values);

            // Format enum values as explicit JSON array string ["VAL1", "VAL2", ...]
            String arrayFormat = "[" + values.stream()
                    .map(v -> "\"" + v + "\"")
                    .collect(Collectors.joining(", ")) + "]";

            schema.setDescription(arrayFormat);
            schema.setExample(arrayFormat);

            return schema;
        }

        if (chain.hasNext()) {
            return chain.next().resolve(type, context, chain);
        }
        return null;
    }
}
