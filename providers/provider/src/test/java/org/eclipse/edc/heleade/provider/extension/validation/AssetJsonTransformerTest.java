/*
 *  Copyright (c) 2025 Universidad de Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       LdE - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.validation;

import com.networknt.schema.JsonSchema;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.heleade.provider.extension.content.based.api.asset.AssetJsonObjectToCbmJsonObjectTransformer;
import org.eclipse.edc.heleade.provider.extension.content.based.api.asset.CbmJsonObjectToAssetJsonObjectTransformer;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.TestServiceExtensionContext;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.eclipse.edc.heleade.provider.extension.validation.AssetJsonSchemaValidatorTest.getJsonSchema;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class AssetJsonTransformerTest {
    private static final JsonLd JSON_LD_EMPTY = new JsonLdExtension().createJsonLdService(TestServiceExtensionContext.testServiceExtensionContext());
    private static final JsonLd JSON_LD_EDC = new JsonLdExtension().createJsonLdService(TestServiceExtensionContext.testServiceExtensionContext());
    private static final String ASSET_SCHEMA_PATH = "providers/provider/src/main/resources/asset-schema-cbm.json";
    private static final String DATASET_SCHEMA_PATH = "providers/provider/src/main/resources/dataset-schema-cbm.json";
    private static final String RESOURCES_PATH = "providers/provider/";
    private static final String MINIMAL_ASSET_PATH = RESOURCES_PATH + "src/test/resources/minimal-edc-asset.json";
    private static final String MINIMAL_CBM_PATH = RESOURCES_PATH + "src/test/resources/minimal-cbm-dataset.json";
    private static final String MINIMAL_ID = "datasetMandatory";
    private static final String FULL_ASSET_PATH = RESOURCES_PATH + "src/test/resources/full-edc-asset.json";
    private static final String FULL_CBM_PATH = RESOURCES_PATH + "src/test/resources/full-cbm-dataset.json";
    private static final String FULL_ID = "datasetFull";
    private static Validator<JsonObject> assetValidator;
    private static Validator<JsonObject> datasetValidator;

    @BeforeAll
    static void beforeAll() {

        JSON_LD_EDC.registerNamespace(VOCAB, EDC_NAMESPACE);

        JsonSchema schemaAsset = getJsonSchema(getFileContentFromRelativePath(ASSET_SCHEMA_PATH));
        assetValidator = new AssetJsonSchemaValidator().getValidator(schemaAsset, JSON_LD_EDC);

        JsonSchema schemaDataset = getJsonSchema(getFileContentFromRelativePath(DATASET_SCHEMA_PATH));
        datasetValidator = new DatasetJsonSchemaValidator().getValidator(schemaDataset, JSON_LD_EDC);
    }

    @Test
    void transformCbmToAsset() {
        String minimalCbmString = getFileContentFromRelativePath(MINIMAL_CBM_PATH);
        JsonObject cbmJsonObject = Json.createReader(new StringReader(minimalCbmString)).readObject();

        JsonObject cbmExpandedJsonObject = JSON_LD_EMPTY.expand(cbmJsonObject).getContent();
        JsonObject assetTransformedJsonObject = CbmJsonObjectToAssetJsonObjectTransformer.transformCbmToAssetJsonObject(MINIMAL_ID, cbmExpandedJsonObject);
        Assertions.assertNotNull(assetTransformedJsonObject);

        JsonObject assetTransformedCompactedJsonObject = JSON_LD_EMPTY.compact(assetTransformedJsonObject).getContent();
        String assetJsonTransformedString = assetTransformedCompactedJsonObject.toString();
        Assertions.assertNotNull(assetJsonTransformedString);

        var result = assetValidator.validate(assetTransformedJsonObject);
        assertThat(result).isSucceeded();
    }

    @Test
    void transformFullCbmToAsset() {
        String fullCbmString = getFileContentFromRelativePath(FULL_CBM_PATH);
        JsonObject cbmJsonObject = Json.createReader(new StringReader(fullCbmString)).readObject();

        JsonObject cbmExpandedJsonObject = JSON_LD_EMPTY.expand(cbmJsonObject).getContent();
        JsonObject assetTransformedJsonObject = CbmJsonObjectToAssetJsonObjectTransformer.transformCbmToAssetJsonObject(MINIMAL_ID, cbmExpandedJsonObject);
        Assertions.assertNotNull(assetTransformedJsonObject);

        JsonObject assetTransformedCompactedJsonObject = JSON_LD_EMPTY.compact(assetTransformedJsonObject).getContent();
        String assetJsonTransformedString = assetTransformedCompactedJsonObject.toString();
        Assertions.assertNotNull(assetJsonTransformedString);

        var result = assetValidator.validate(assetTransformedJsonObject);
        assertThat(result).isSucceeded();
    }

    @Test
    void transformAssetToCbm() {
        String  minimalAssetString = getFileContentFromRelativePath(MINIMAL_ASSET_PATH);

        JsonObject assetJsonObject = Json.createReader(new StringReader(minimalAssetString)).readObject();
        JsonObject assetExpandedJsonObject = JSON_LD_EMPTY.expand(assetJsonObject).getContent();

        JsonObject assetCompactedJsonObject = JSON_LD_EMPTY.compact(assetExpandedJsonObject).getContent();
        JsonObject cbmTransformedJsonObject = AssetJsonObjectToCbmJsonObjectTransformer.transformAssetToCbmJsonObject(MINIMAL_ID, assetCompactedJsonObject);
        Assertions.assertNotNull(cbmTransformedJsonObject);

        String cbmJsonTransformedString = cbmTransformedJsonObject.toString();
        Assertions.assertNotNull(cbmJsonTransformedString);

        JsonObject cbmTransformedCompactedJsonObject = JSON_LD_EDC.compact(cbmTransformedJsonObject).getContent();
        var result = datasetValidator.validate(cbmTransformedCompactedJsonObject);
        assertThat(result).isSucceeded();
    }

    @Test
    void transformFullAssetToCbm() {
        String fullAssetString = getFileContentFromRelativePath(FULL_ASSET_PATH);

        JsonObject assetJsonObject = Json.createReader(new StringReader(fullAssetString)).readObject();
        JsonObject assetExpandedJsonObject = JSON_LD_EMPTY.expand(assetJsonObject).getContent();

        JsonObject assetCompactedJsonObject = JSON_LD_EMPTY.compact(assetExpandedJsonObject).getContent();
        JsonObject cbmTransformedJsonObject = AssetJsonObjectToCbmJsonObjectTransformer.transformAssetToCbmJsonObject(MINIMAL_ID, assetCompactedJsonObject);
        Assertions.assertNotNull(cbmTransformedJsonObject);

        String cbmJsonTransformedString = cbmTransformedJsonObject.toString();
        Assertions.assertNotNull(cbmJsonTransformedString);

        JsonObject cbmTransformedCompactedJsonObject = JSON_LD_EDC.compact(cbmTransformedJsonObject).getContent();
        var result = datasetValidator.validate(cbmTransformedCompactedJsonObject);
        assertThat(result).isSucceeded();
    }

    private static String getFileContentFromRelativePath(String relativePath) {
        var fileFromRelativePath = new File(TestUtils.findBuildRoot(), relativePath);
        try {
            return Files.readString(Paths.get(fileFromRelativePath.toURI()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
