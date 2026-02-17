/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.api.query;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.catalog.api.query.FederatedCatalogApi;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ServiceResultHandler;

import javax.xml.catalog.Catalog;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Handles API endpoints for querying cached federated catalog content.
 * Consumes and produces data in JSON format.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v1alpha/catalog/query-cbm")
public class FederatedCatalogContentBasedApiController implements FederatedCatalogApi {

    private final QueryService queryService;
    private final TypeTransformerRegistry transformerRegistry;

    /**
     * Constructs a controller for handling federated catalog content-based query APIs.
     *
     * @param queryService the service used to query catalog data
     * @param transformerRegistry the registry managing type transformations
     */
    public FederatedCatalogContentBasedApiController(QueryService queryService, TypeTransformerRegistry transformerRegistry) {
        this.queryService = queryService;
        this.transformerRegistry = transformerRegistry;
    }

    /**
     * Queries the cached federated catalog based on the provided query parameters and returns the matching result as a JSON array.
     *
     * @param catalogQuery the query object specifying the catalog search criteria; if null, a default query specification is used
     * @param flatten a boolean flag indicating whether the result catalog should be flattened
     * @return a JSON array containing the resulting catalog data, transformed based on the provided parameters
     */
    @Override
    @POST
    public JsonArray getCachedCatalog(JsonObject catalogQuery, @DefaultValue("false") @QueryParam("flatten") boolean flatten) {
        var querySpec = catalogQuery == null
                ? QuerySpec.Builder.newInstance().build()
                : transformerRegistry.transform(catalogQuery, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);

        var catalogs = queryService.getCatalog(querySpec)
                .orElseThrow(ServiceResultHandler.exceptionMapper(Catalog.class));

        return catalogs.stream()
                .map(c -> flatten ? FederatedCatalogUtil.flatten(c) : c)
                .map(c -> transformerRegistry.transform(c, JsonObject.class))
                .filter(Result::succeeded)
                .map(AbstractResult::getContent)
                .collect(toJsonArray());
    }

    /**
     * Retrieves cached datasets based on the provided query parameters.
     *
     * @param catalogQuery the query parameters as a JsonObject; if null, a default query specification is used
     * @return a JsonArray containing the matching datasets
     * @throws InvalidRequestException if the catalogQuery transformation to QuerySpec fails
     * @throws IllegalStateException if the QueryService or cache is not of the correct type
     */
    @Path("/datasets")
    @POST
    public JsonArray getCachedDatasets(JsonObject catalogQuery) {
        if (queryService instanceof HeleadeQueryServiceImpl) {
            HeleadeQueryServiceImpl heleadeQueryService = (HeleadeQueryServiceImpl) queryService;
            var querySpec = catalogQuery == null
                    ? QuerySpec.Builder.newInstance().build()
                    : transformerRegistry.transform(catalogQuery, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);

            var datasets = heleadeQueryService.getDatasets(querySpec)
                    .orElseThrow(ServiceResultHandler.exceptionMapper(Dataset.class));

            return datasets.stream()
                    .map(c -> transformerRegistry.transform(c, JsonObject.class))
                    .filter(Result::succeeded)
                    .map(AbstractResult::getContent)
                    .collect(toJsonArray());
        } else {
            throw new IllegalStateException("Dataset query unavailable: QueryService is not of type HeleadeQueryServiceImpl");
        }
    }

    /**
     * Counts the number of cached datasets based on the provided query parameters.
     *
     * @param catalogQuery the query parameters as a JsonObject; if null, a default query specification is used
     * @return a String representing the number of datasets matching the query
     * @throws InvalidRequestException if the catalogQuery transformation to QuerySpec fails
     * @throws IllegalStateException if the QueryService is not of type HeleadeQueryServiceImpl
     */
    @Path("/datasets/count")
    @POST
    public String getCachedDatasetsCount(JsonObject catalogQuery) {
        if (queryService instanceof HeleadeQueryServiceImpl) {
            HeleadeQueryServiceImpl heleadeQueryService = (HeleadeQueryServiceImpl) queryService;
            var querySpec = catalogQuery == null
                    ? QuerySpec.Builder.newInstance().build()
                    : transformerRegistry.transform(catalogQuery, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);

            //check if the original query did not have a limit
            boolean noLimit = (catalogQuery != null && !catalogQuery.containsKey(EDC_NAMESPACE + "limit"));
            return heleadeQueryService.countDatasets(querySpec, noLimit);
        } else {
            throw new IllegalStateException("Dataset query unavailable: QueryService is not of type HeleadeQueryServiceImpl");
        }
    }

    /**
     * Counts the number of cached datasets per keyword based on the provided query parameters.
     *
     * @param catalogQuery the query parameters as a JsonObject; if null, a default query specification is used
     * @return a String representing the number of datasets per keyword matching the query
     * @throws InvalidRequestException if the catalogQuery transformation to QuerySpec fails
     * @throws IllegalStateException if the QueryService is not of type HeleadeQueryServiceImpl
     */
    @Path("/keywords/count")
    @POST
    public String getCachedKeywordsCount(JsonObject catalogQuery) {
        if (queryService instanceof HeleadeQueryServiceImpl) {
            HeleadeQueryServiceImpl heleadeQueryService = (HeleadeQueryServiceImpl) queryService;
            var querySpec = catalogQuery == null
                    ? QuerySpec.Builder.newInstance().build()
                    : transformerRegistry.transform(catalogQuery, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);

            //check if the original query did not have a limit
            boolean noLimit = (catalogQuery != null && !catalogQuery.containsKey(EDC_NAMESPACE + "limit"));
            return heleadeQueryService.countKeywords(querySpec, noLimit);
        } else {
            throw new IllegalStateException("Dataset query unavailable: QueryService is not of type HeleadeQueryServiceImpl");
        }
    }
}
