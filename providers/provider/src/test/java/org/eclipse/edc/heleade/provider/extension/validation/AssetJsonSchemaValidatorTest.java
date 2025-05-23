/*
 *  Copyright (c) 2025 University of Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       University of Alicante - Initial implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.validation;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.serialization.JsonNodeReader;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.TestServiceExtensionContext;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_DATA_ADDRESS;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.PROPERTY_NAME;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;


public class AssetJsonSchemaValidatorTest {
    private final JsonLd jsonLd = initializeJsonLd();
    private final Validator<JsonObject> validator = new AssetJsonSchemaValidator().getValidator(getTestJsonSchema(), jsonLd);

    @Test
    void shouldSucceed_whenValidInput() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add(PROPERTY_NAME, "test asset")))
                .add(EDC_ASSET_DATA_ADDRESS, validDataAddress())
                .build();
        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenIdIsBlank() {
        var input = createObjectBuilder()
                .add(ID, " ")
                .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder()))
                .add(EDC_ASSET_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v -> {
                assertThat(v.path()).isEqualTo(ID);
            });
        });
    }

    @Test
    void shouldFail_whenPropertiesAreMissing() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v ->
                    assertThat(v.path()).isEqualTo(EDC_ASSET_PROPERTIES));
        });
    }


    @Test
    void shouldFail_whenPropertiesAndPrivatePropertiesHaveDuplicatedKeys() {
        var input = createObjectBuilder()
                .add(EDC_ASSET_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add(PROPERTY_NAME, "test asset")
                                                               .add(EDC_NAMESPACE + "key", createArrayBuilder())))
                .add(EDC_ASSET_PRIVATE_PROPERTIES, createArrayBuilder().add(createObjectBuilder().add(EDC_NAMESPACE + "key", createArrayBuilder())))
                .add(EDC_ASSET_DATA_ADDRESS, validDataAddress())
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getViolations()).hasSize(1).anySatisfy(v ->
                    assertThat(v.path()).isEqualTo(EDC_ASSET_PROPERTIES));
        });
    }

    private JsonArrayBuilder validDataAddress() {
        return createArrayBuilder().add(createObjectBuilder()
                .add(EDC_DATA_ADDRESS_TYPE_PROPERTY, createArrayBuilder().add(createObjectBuilder().add(VALUE, "AddressType")))
        );
    }

    private JsonSchema getTestJsonSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.jsonNodeReader(JsonNodeReader.builder().locationAware().build()));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        String jsonSchemaStr = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"properties\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                 "        \"name\"\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                 "    \"properties\"\n" +
                "  ]\n" +
                "}";

        JsonSchema schema = factory.getSchema(jsonSchemaStr, InputFormat.JSON, config);
        return schema;
    }

    private JsonObject injectVocab(JsonObject json) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder(json);
        if (json.get("@context") instanceof JsonObject) {
            JsonObject contextObject = (JsonObject) Optional.ofNullable(json.getJsonObject("@context")).orElseGet(() -> {
                return Json.createObjectBuilder().build();
            });
            JsonObjectBuilder contextBuilder = Json.createObjectBuilder(contextObject);
            if (!contextObject.containsKey("@vocab")) {
                JsonObject newContextObject = contextBuilder.add("@vocab", EDC_NAMESPACE).build();
                jsonObjectBuilder.add("@context", newContextObject);
            }
        }

        return jsonObjectBuilder.build();
    }

    public JsonLd initializeJsonLd() {
        JsonLd jsonLd = new JsonLdExtension().createJsonLdService(TestServiceExtensionContext.testServiceExtensionContext());
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        return jsonLd;
    }
}
