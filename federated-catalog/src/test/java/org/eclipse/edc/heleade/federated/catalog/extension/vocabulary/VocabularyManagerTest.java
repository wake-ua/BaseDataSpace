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

package org.eclipse.edc.heleade.federated.catalog.extension.vocabulary;

import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;


public class VocabularyManagerTest {

    // public static final String SEGITTUR_ONTOLOGY_IRI = "https://ontologia.segittur.es/turismo/def/core/ontology.rdf";
    private static final String TEST_OWL_PATH = "federated-catalog/src/test/resources/test.owl";

    // @Test
    // void shouldLoadOntologyFromRemoteLocation() {
    //     VocabularyManager.loadOntology(SEGITTUR_ONTOLOGY_IRI);
    // }

    @Test
    void shouldLoadOntologyFromFile() {
        var fileFromRelativePath = new File(TestUtils.findBuildRoot(), TEST_OWL_PATH);
        String filePath = fileFromRelativePath.getAbsolutePath();
        String output = VocabularyManager.loadOntology(filePath);
        assert !output.isEmpty();
        assert output.contains("Rock");
    }
}
