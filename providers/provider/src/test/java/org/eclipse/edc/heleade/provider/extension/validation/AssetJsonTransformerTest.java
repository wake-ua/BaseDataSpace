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
import org.eclipse.edc.jsonld.spi.JsonLd;
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
import static org.eclipse.edc.heleade.provider.extension.validation.AssetJsonSchemaValidatorTest.initializeJsonLd;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public class AssetJsonTransformerTest {
    private static final JsonLd JSON_LD = initializeJsonLd();
    private static final String ASSET_SCHEMA_PATH = "providers/provider/src/main/resources/asset-schema-cbm.json";
    private static final String DATASET_SCHEMA_PATH = "providers/provider/src/main/resources/dataset-schema-cbm.json";
    private static final String RESOURCES_PATH = "providers/provider/";
    private static final String MINIMAL_ASSET_PATH = RESOURCES_PATH + "src/test/resources/minimal-edc-asset.json";
    private static final String MINIMAL_CBM_PATH = RESOURCES_PATH + "src/test/resources/minimal-cbm-dataset.json";
    private static final String MINIMAL_ID = "datasetMandatory";
    private static String minimalAssetString;
    private static String minimalCbmString;
    private static Validator<JsonObject> assetValidator;
    private static Validator<JsonObject> datasetValidator;

    @BeforeAll
    static void beforeAll() {
        minimalAssetString = getFileContentFromRelativePath(MINIMAL_ASSET_PATH);
        minimalCbmString = getFileContentFromRelativePath(MINIMAL_CBM_PATH);

        JsonSchema schemaAsset = getJsonSchema(getFileContentFromRelativePath(ASSET_SCHEMA_PATH));
        assetValidator = new AssetJsonSchemaValidator().getValidator(schemaAsset, JSON_LD);

        JsonSchema schemaDataset = getJsonSchema(getFileContentFromRelativePath(DATASET_SCHEMA_PATH));
        datasetValidator = new DatasetJsonSchemaValidator().getValidator(schemaDataset, JSON_LD);
    }

    @Test
    void transformCbmToAsset() {
        JsonObject cbmJsonObject = Json.createReader(new StringReader(minimalCbmString)).readObject();

        JsonObject cbmExpandedJsonObject = JSON_LD.expand(cbmJsonObject).getContent();
        JsonObject assetTransformedJsonObject = CbmJsonObjectToAssetJsonObjectTransformer.transformCbmToAssetJsonObject(MINIMAL_ID, cbmExpandedJsonObject);
        Assertions.assertNotNull(assetTransformedJsonObject);

        JsonObject assetTransformedCompactedJsonObject = JSON_LD.compact(assetTransformedJsonObject).getContent();
        String assetJsonTransformedString = assetTransformedCompactedJsonObject.toString();
        Assertions.assertNotNull(assetJsonTransformedString);

        var result = assetValidator.validate(assetTransformedJsonObject);
        assertThat(result).isSucceeded();
    }

    @Test
    void transformAssetToCbm() {
        JsonObject assetJsonObject = Json.createReader(new StringReader(minimalAssetString)).readObject();

        JsonObject assetExpandedJsonObject = JSON_LD.expand(assetJsonObject).getContent();
        JsonObject cbmTransformedJsonObject = AssetJsonObjectToCbmJsonObjectTransformer.transformAssetToCbmJsonObject(MINIMAL_ID, assetExpandedJsonObject);
        Assertions.assertNotNull(cbmTransformedJsonObject);

        JsonObject cbmTransformedCompactedJsonObject = JSON_LD.compact(cbmTransformedJsonObject).getContent();
        String cbmJsonTransformedString = cbmTransformedCompactedJsonObject.toString();
        Assertions.assertNotNull(cbmJsonTransformedString);

        var result = datasetValidator.validate(cbmTransformedJsonObject);
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
