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

package org.eclipse.edc.heleade.commons.content.based.catalog;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;

/**
 * This class contains a set of constants used in the Content Based Metadata (CBM) schema and the
 * Dataset Catalog Vocabulary (DCAT) schema. These constants serve as identifiers for various
 * components, properties, and relationships in the CBM and DCAT schemas. They provide a structured
 * way to reference elements in metadata processing and JSON-LD structures.
 */
public class CbmConstants {
    /**
     * Tag used to identify datasets in a JSON-LD structure, formed by appending "dataset" to DCAT schema.
     * Used to extract datasets from JSON objects in {@code getDatasetJsonArray(JsonObject)}.
     */
    public static final String DATASETS_TAG = DCAT_SCHEMA + "dataset";

    /**
     * A constant representing the JSON-LD field name for distribution in the DCAT schema.
     */
    public static final String DISTRIBUTION_TAG = DCAT_SCHEMA + "distribution";
    /**
     * Key to identify dataset/entity type, used for marking sample datasets.
     * Value derived from {@code CREDENTIAL_STATUS_TYPE_PROPERTY}.
     */
    public static final String TYPE_TAG = CREDENTIAL_STATUS_TYPE_PROPERTY;
    /**
     * Namespace URI for Content Based Metadata (CBM) schema.
     */
    public static final String CBM_SCHEMA = "https://w3id.org/cbm/v0.0.1/ns/";
    /**
     * Identifier for "isSampleOf" relationship in CBM schema, links dataset to original sample.
     */
    public static final String CBM_IS_SAMPLE_OF = CBM_SCHEMA + "isSampleOf";
    /**
     * Type identifier for CBM Sample, formed by appending "Sample" to CBM_SCHEMA.
     */
    public static final String CBM_SAMPLE_TYPE = CBM_SCHEMA + "Sample";
    /**
     * A constant representing the prefix "cbm" used for Content Based Metadata (CBM).
     */
    public static final String CBM_PREFIX = "cbm";
    /**
     * Indicates the presence of a data dictionary in the content-based metadata (CBM) schema.
     */
    public static final String CBM_HAS_DATA_DICTIONARY = CBM_SCHEMA + "hasDataDictionary";

    /**
     * A constant representing the prefix for schema.org.
     */
    public static final String SCHEMA_PREFIX = "schema";
    /**
     * A constant representing the namespace URI for the RDF (Resource Description Framework) schema.
     */
    public static final String RDF_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
    /**
     * A constant representing the prefix "rdf" used for the RDF (Resource Description Framework) namespace.
     */
    public static final String RDF_PREFIX = "rdf";

}
