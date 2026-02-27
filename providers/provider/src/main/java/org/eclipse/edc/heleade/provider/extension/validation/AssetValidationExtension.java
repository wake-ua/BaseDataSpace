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
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.serialization.JsonNodeReader;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_PREFIX;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SAMPLE_TYPE;
import static org.eclipse.edc.heleade.commons.content.based.catalog.CbmConstants.CBM_SCHEMA;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.SCHEMA_ORG_NAMESPACE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DSP_SCOPE_V_2024_1;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


/**
 * Class to validate the asset JSON input against a JSON Schema
 */
@Extension(value = AssetValidationExtension.NAME)
public class AssetValidationExtension implements ServiceExtension {

    public static final String NAME = "Provider AssetValidation Extension";

    /**
     * Prefix used for Schema.org properties and types.
     */
    public static final String SCHEMA_ORG_PREFIX = "schema";

    static final String CONTROL_SCOPE = "CONTROL_API";
    static final String MANAGEMENT_SCOPE = "MANAGEMENT_API";

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLdGlobal;

    private Monitor monitor;
    private JsonSchema assetSchema;
    private JsonSchema datasetSchema;
    private JsonSchema sampleSchema;
    private JsonLd jsonLd;

    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        String assetSchemaFilePath = context.getConfig().getString("edc.heleade.provider.validation.asset.schema.path", "asset.json");
        String datasetSchemaFilePath = context.getConfig().getString("edc.heleade.provider.validation.dataset.schema.path", "dataset.json");
        String sampleSchemaFilePath = context.getConfig().getString("edc.heleade.provider.validation.sample.schema.path", "sample.json");
        assetSchema = getJsonSchemaFromFile(assetSchemaFilePath);
        datasetSchema = getJsonSchemaFromFile(datasetSchemaFilePath);
        sampleSchema = getJsonSchemaFromFile(sampleSchemaFilePath);

        jsonLd = new JsonLdExtension().createJsonLdService(context);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);

        for (String scope : Arrays.asList(DSP_SCOPE_V_08, DSP_SCOPE_V_2024_1, CONTROL_SCOPE, MANAGEMENT_SCOPE)) {
            jsonLdGlobal.registerNamespace(SCHEMA_ORG_PREFIX, SCHEMA_ORG_NAMESPACE, scope);
            jsonLdGlobal.registerNamespace(CBM_PREFIX, CBM_SCHEMA, scope);
            jsonLdGlobal.registerNamespace(DCT_PREFIX, DCT_SCHEMA, scope);
            jsonLdGlobal.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, scope);
        }
    }

    public void prepare() {
        var assetValidator = new AssetJsonSchemaValidator().getValidator(assetSchema, jsonLd);
        validatorRegistry.register(Asset.EDC_ASSET_TYPE, assetValidator);

        var datasetValidator = new DatasetJsonSchemaValidator().getValidator(datasetSchema, jsonLd);
        validatorRegistry.register(DCAT_DATASET_TYPE, datasetValidator);

        var sampleValidator = new SampleJsonSchemaValidator().getValidator(sampleSchema, jsonLd);
        validatorRegistry.register(CBM_SAMPLE_TYPE, sampleValidator);
    }

    private JsonSchema getJsonSchemaFromFile(String filePath) {
        monitor.info("JsonSchema file selected: " + filePath);

        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream jsonSchemaFileInputStream = classLoader.getResourceAsStream(filePath)) {
            if (jsonSchemaFileInputStream == null) {
                throw new RuntimeException("JsonSchema file does not exist: " + filePath);
            }
            String jsonSchemaContent = new String(jsonSchemaFileInputStream.readAllBytes(), StandardCharsets.UTF_8);

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                    builder -> builder.jsonNodeReader(JsonNodeReader.builder().locationAware().build()));
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
            JsonSchema schema = factory.getSchema(jsonSchemaContent, InputFormat.JSON, config);
            return schema;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read and create a JsonSchema definition from file: " + filePath, e);
        }
    }
}
