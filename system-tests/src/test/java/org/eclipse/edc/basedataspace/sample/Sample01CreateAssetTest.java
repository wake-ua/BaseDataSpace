package org.eclipse.edc.basedataspace.sample;

import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.basedataspace.common.NegotiationCommon.*;
import static org.eclipse.edc.basedataspace.common.PrerequisitesCommon.getConsumer;
import static org.eclipse.edc.basedataspace.common.PrerequisitesCommon.getProvider;

public class Sample01CreateAssetTest {
    private static final String CREATE_ASSET_FILE_PATH = "system-tests/src/test/resources/sample/create-asset-original.json";
    private static final String CATALOG_REQUEST_FILE_PATH = "system-tests/src/test/resources/sample/fetch-catalog-with-samples.json";
    private static final String CREATE_RESTRICTED_POLICY_FILE_PATH = "system-tests/src/test/resources/sample/create-restricted-policy.json";
    private static final String CREATE_RESTRICTED_CONTRACT_DEFINITION_FILE_PATH = "system-tests/src/test/resources/sample/create-restricted-contract-definition.json";


    @RegisterExtension
    static RuntimeExtension provider = getProvider();

    @RegisterExtension
    static RuntimeExtension consumer = getConsumer();

    @Test
    void testCreateAssetWithSample() {
        createAsset(CREATE_ASSET_FILE_PATH);
        createPolicy(CREATE_RESTRICTED_POLICY_FILE_PATH);
        createContractDefinition(CREATE_RESTRICTED_CONTRACT_DEFINITION_FILE_PATH);
        var catalogDatasets = fetchCatalogDatasets(CATALOG_REQUEST_FILE_PATH);
        assertThat(catalogDatasets).isNotNull();
    }
}
