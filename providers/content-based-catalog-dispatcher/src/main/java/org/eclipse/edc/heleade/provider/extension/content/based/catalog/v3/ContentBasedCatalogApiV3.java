/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.provider.extension.content.based.catalog.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.edc.api.model.ApiCoreSchema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

/**
 * Interface for managing the content-based catalog API version 3.
 * Provides methods for handling requests related to catalog data exchange
 * between connectors using predefined schemas and specifications.
 */
@OpenAPIDefinition(info = @Info(version = "v3"))
@Tag(name = "Content Based Catalog V3")
public interface ContentBasedCatalogApiV3 {

    /**
     * Handles a request to retrieve the catalog (contract offers) from a single connector.
     *
     * @param request The JSON object containing the request parameters, structured according to the `CatalogRequestSchema`.
     * @param response The asynchronous response that will be populated with the catalog data, structured as `CatalogSchema`.
     */
    @Operation(
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = CatalogRequestSchema.class))),
            responses = { @ApiResponse(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CatalogSchema.class)
                    ),
                    description = "Gets contract offers (=catalog) of a single connector") }
    )
    void requestCatalogV3(JsonObject request, @Suspended AsyncResponse response);

    /**
     * Represents the schema for a catalog request. This schema defines the structure and requirements
     * of the request body when interacting with the content-based catalog API.
     * Components:
     * - Includes details such as context, request type, counterparty address, counterparty ID, protocol,
     *   and query specifications.
     * - Provides an example JSON representation for better understanding of the expected payload.
     */
    @Schema(name = "CatalogRequest", example = CatalogRequestSchema.CATALOG_REQUEST_EXAMPLE)
    record CatalogRequestSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, example = CATALOG_REQUEST_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String counterPartyAddress,
            // Switch to required in the next API iteration
            @Schema(requiredMode = NOT_REQUIRED)
            String counterPartyId,
            @Schema(requiredMode = REQUIRED)
            String protocol,
            ApiCoreSchema.QuerySpecSchema querySpec) {

        /**
         * Example JSON representation of a catalog request.
         * This string provides the structure for a typical catalog request, including:
         * - Context metadata defining the namespace vocabulary.
         * - Request type as "CatalogRequest".
         * - Counterparty and protocol information.
         * - Query specifications such as offset, limit, sort order, and filter expressions.
         * Useful for illustrating the expected payload format for catalog interactions.
         */
        public static final String CATALOG_REQUEST_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "CatalogRequest",
                    "counterPartyAddress": "http://provider-address",
                    "counterPartyId": "providerId",
                    "protocol": "dataspace-protocol-http",
                    "querySpec": {
                        "offset": 0,
                        "limit": 50,
                        "sortOrder": "DESC",
                        "sortField": "fieldName",
                        "filterExpression": []
                    }
                }
                """;
    }

    /**
     * Represents a data structure for a DCAT catalog schema.
     * The `CatalogSchema` defines the structure and properties of a catalog
     * following the DCAT and ODRL ontologies, including datasets, policies,
     * distributions, data services, and metadata.
     * The schema example provides a comprehensive JSON-LD representation of
     * a catalog, including relational references, policy rules, and endpoint
     * configurations.
     */
    @Schema(name = "Catalog", description = "DCAT catalog", example = CatalogSchema.CATALOG_EXAMPLE)
    record CatalogSchema(
    ) {
        /**
         * Represents a sample string value of a catalog in JSON-LD format,
         * formatted according to the DCAT and ODRL ontology specifications.
         * This constant provides a predefined data structure containing catalog metadata,
         * datasets, access policies, permissions, constraints, and service details.
         * It serves as an example representation of a catalog's schema with specific
         * attributes like dataset distribution, data service endpoints, and policy conditions.
         */
        public static final String CATALOG_EXAMPLE = """
                {
                    "@id": "7df65569-8c59-4013-b1c0-fa14f6641bf2",
                    "@type": "dcat:Catalog",
                    "dcat:dataset": {
                        "@id": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                        "@type": "dcat:Dataset",
                        "odrl:hasPolicy": {
                            "@id": "OGU0ZTMzMGMtODQ2ZS00ZWMxLThmOGQtNWQxNWM0NmI2NmY4:YmNjYTYxYmUtZTgyZS00ZGE2LWJmZWMtOTcxNmE1NmNlZjM1:NDY2ZTZhMmEtNjQ1Yy00ZGQ0LWFlZDktMjdjNGJkZTU4MDNj",
                            "@type": "odrl:Set",
                            "odrl:permission": {
                                "odrl:target": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                                "odrl:action": {
                                    "odrl:type": "http://www.w3.org/ns/odrl/2/use"
                                },
                                "odrl:constraint": {
                                    "odrl:and": [
                                        {
                                            "odrl:leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
                                            "odrl:operator": {
                                                "@id": "odrl:gteq"
                                            },
                                            "odrl:rightOperand": "2023-07-07T07:19:58.585601395Z"
                                        },
                                        {
                                            "odrl:leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
                                            "odrl:operator": {
                                                "@id": "odrl:lteq"
                                            },
                                            "odrl:rightOperand": "2023-07-12T07:19:58.585601395Z"
                                        }
                                    ]
                                }
                            },
                            "odrl:prohibition": [],
                            "odrl:obligation": []
                        },
                        "dcat:distribution": [
                            {
                                "@type": "dcat:Distribution",
                                "dct:format": {
                                    "@id": "HttpData"
                                },
                                "dcat:accessService": "5e839777-d93e-4785-8972-1005f51cf367"
                            }
                        ],
                        "description": "description",
                        "id": "bcca61be-e82e-4da6-bfec-9716a56cef35"
                    },
                    "dcat:service": {
                        "@id": "5e839777-d93e-4785-8972-1005f51cf367",
                        "@type": "dcat:DataService",
                        "dct:terms": "connector",
                        "dct:endpointUrl": "http://localhost:16806/protocol"
                    },
                    "dspace:participantId": "urn:connector:provider",
                    "@context": {
                        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                        "dct": "http://purl.org/dc/terms/",
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
                        "dcat": "http://www.w3.org/ns/dcat#",
                        "odrl": "http://www.w3.org/ns/odrl/2/",
                        "dspace": "https://w3id.org/dspace/v0.8/"
                    }
                }
                """;
    }

    /**
     * Represents the schema definition for a dataset, following the structure and semantics
     * defined by the DCAT (Data Catalog Vocabulary) and ODRL (Open Digital Rights Language) standards.
     *
     * This schema is used to define metadata for datasets, including attributes related to access policies,
     * constraints, distributions, and context information.
     */
    @Schema(name = "Dataset", description = "DCAT dataset", example = DatasetSchema.DATASET_EXAMPLE)
    record DatasetSchema(
    ) {
        /**
         * Represents an example dataset in JSON-LD format, compliant with DCAT and ODRL vocabularies.
         * Used to demonstrate or validate dataset structures, including distribution, policy constraints,
         * and access configurations.
         * This variable is a constant and serves as a predefined template for referencing dataset schemas.
         */
        public static final String DATASET_EXAMPLE = """
                {
                    "@id": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                    "@type": "dcat:Dataset",
                    "odrl:hasPolicy": {
                        "@id": "OGU0ZTMzMGMtODQ2ZS00ZWMxLThmOGQtNWQxNWM0NmI2NmY4:YmNjYTYxYmUtZTgyZS00ZGE2LWJmZWMtOTcxNmE1NmNlZjM1:NDY2ZTZhMmEtNjQ1Yy00ZGQ0LWFlZDktMjdjNGJkZTU4MDNj",
                        "@type": "odrl:Set",
                        "odrl:permission": {
                            "odrl:target": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                            "odrl:action": {
                                "odrl:type": "http://www.w3.org/ns/odrl/2/use"
                            },
                            "odrl:constraint": {
                                "odrl:and": [
                                    {
                                        "odrl:leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
                                        "odrl:operator": {
                                            "@id": "odrl:gteq"
                                        },
                                        "odrl:rightOperand": "2023-07-07T07:19:58.585601395Z"
                                    },
                                    {
                                        "odrl:leftOperand": "https://w3id.org/edc/v0.0.1/ns/inForceDate",
                                        "odrl:operator": {
                                            "@id": "odrl:lteq"
                                        },
                                        "odrl:rightOperand": "2023-07-12T07:19:58.585601395Z"
                                    }
                                ]
                            }
                        },
                        "odrl:prohibition": [],
                        "odrl:obligation": [],
                        "odrl:target": "bcca61be-e82e-4da6-bfec-9716a56cef35"
                    },
                    "dcat:distribution": [
                        {
                            "@type": "dcat:Distribution",
                            "dct:format": {
                                "@id": "HttpData"
                            },
                            "dcat:accessService": "5e839777-d93e-4785-8972-1005f51cf367"
                        }
                    ],
                    "description": "description",
                    "id": "bcca61be-e82e-4da6-bfec-9716a56cef35",
                    "@context": {
                        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                        "dct": "http://purl.org/dc/terms/",
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
                        "dcat": "http://www.w3.org/ns/dcat#",
                        "odrl": "http://www.w3.org/ns/odrl/2/",
                        "dspace": "https://w3id.org/dspace/v0.8/"
                    }
                }
                """;
    }
}
