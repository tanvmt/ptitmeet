package com.ptithcm.ptitmeet.config;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${app.backend-url}")
    private String backendUrl;

    @Bean
    public OpenAPI ptitMeetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PTITMeet API")
                        .description("API documentation for the PTITMeet backend services.")
                        .version("v1")
                        .contact(new Contact()
                                .name("PTITMeet Team")))
                .addServersItem(new Server()
                        .url(backendUrl)
                        .description("PTITMeet backend server"));
    }

    @Bean
    public OpenApiCustomizer stableOpenApiOrderingCustomizer() {
        return openApi -> {
            openApi.setPaths(sortPaths(openApi.getPaths()));

            Components components = openApi.getComponents();
            if (components != null && components.getSchemas() != null) {
                Map<String, Schema> sortedSchemas = sortMapByKey(components.getSchemas());
                sortedSchemas.values().forEach(this::sortSchemaRecursively);
                components.setSchemas(sortedSchemas);
            }
        };
    }

    private Paths sortPaths(Paths paths) {
        if (paths == null || paths.isEmpty()) {
            return paths;
        }

        Paths sortedPaths = new Paths();
        sortMapByKey(paths).forEach(sortedPaths::addPathItem);
        return sortedPaths;
    }

    private void sortSchemaRecursively(Schema schema) {
        if (schema == null) {
            return;
        }

        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Schema> sortedProperties = sortMapByKey(schema.getProperties());
            sortedProperties.values().forEach(this::sortSchemaRecursively);
            schema.setProperties(sortedProperties);
        }

        if (schema instanceof ArraySchema arraySchema) {
            sortSchemaRecursively(arraySchema.getItems());
        }

        if (schema instanceof ComposedSchema composedSchema) {
            sortSchemaList(composedSchema.getAllOf());
            sortSchemaList(composedSchema.getAnyOf());
            sortSchemaList(composedSchema.getOneOf());
        }

        Object additionalProperties = schema.getAdditionalProperties();
        if (additionalProperties instanceof Schema additionalSchema) {
            sortSchemaRecursively(additionalSchema);
        }
    }

    private void sortSchemaList(List<Schema> schemas) {
        if (schemas == null) {
            return;
        }

        schemas.forEach(this::sortSchemaRecursively);
    }

    private <T> LinkedHashMap<String, T> sortMapByKey(Map<String, T> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(LinkedHashMap::new,
                        (sorted, entry) -> sorted.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }
}
