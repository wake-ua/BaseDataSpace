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
import org.eclipse.edc.catalog.api.query.BaseCatalogsApiController;
import org.eclipse.edc.catalog.api.query.v4.CatalogsApiV4;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ServiceResultHandler;
import org.eclipse.edc.web.spi.validation.SchemaType;

import javax.xml.catalog.Catalog;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

/**
 * Handles API endpoints for querying cached federated catalog content.
 * Consumes and produces data in JSON format.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v4/catalogs-cbm")
public class CatalogsContentBasedApiV4Controller extends BaseCatalogsApiController implements CatalogsApiV4 {

    private final QueryService queryService;
    private final TypeTransformerRegistry transformerRegistry;

    /**
     * Constructs a controller for handling federated catalog content-based query APIs.
     *
     * @param queryService the service used to query catalog data
     * @param transformerRegistry the registry managing type transformations
     */
    public CatalogsContentBasedApiV4Controller(QueryService queryService, TypeTransformerRegistry transformerRegistry) {
        super(queryService, transformerRegistry);

        this.queryService = queryService;
        this.transformerRegistry = transformerRegistry;
    }

    /**
     * Processes a catalog query and returns a list of matching catalogs as a JSON array.
     *
     * @param querySpecJson the query specification as a JSON object. If null, a default query specification is used.
     * @param flatten a boolean flag indicating whether to flatten the catalog entries into one level.
     * @return a JsonArray containing the matched catalogs, potentially flattened based on the input flag.
     * @throws InvalidRequestException if the querySpecJson cannot be transformed into a QuerySpec object.
     */
    @Override
    @POST
    public JsonArray requestCatalogsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson, @DefaultValue("false") @QueryParam("flatten") boolean flatten) {
        var querySpec = querySpecJson == null
                ? QuerySpec.Builder.newInstance().build()
                : transformerRegistry.transform(querySpecJson, QuerySpec.class)
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
     * @param querySpecJson the query parameters as a JsonObject; if null, a default query specification is used
     * @return a JsonArray containing the matching datasets
     * @throws InvalidRequestException if the querySpecJson transformation to QuerySpec fails
     * @throws IllegalStateException if the QueryService or cache is not of the correct type
     */
    @Path("/datasets")
    @POST
    public JsonArray getCachedDatasets(JsonObject querySpecJson) {
        if (queryService instanceof HeleadeQueryServiceImpl) {
            HeleadeQueryServiceImpl heleadeQueryService = (HeleadeQueryServiceImpl) queryService;
            var querySpec = querySpecJson == null
                    ? QuerySpec.Builder.newInstance().build()
                    : transformerRegistry.transform(querySpecJson, QuerySpec.class)
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
     * @param querySpecJson the query parameters as a JsonObject; if null, a default query specification is used
     * @return a String representing the number of datasets matching the query
     * @throws InvalidRequestException if the querySpecJson transformation to QuerySpec fails
     * @throws IllegalStateException if the QueryService is not of type HeleadeQueryServiceImpl
     */
    @Path("/datasets/count")
    @POST
    public String getCachedDatasetsCount(JsonObject querySpecJson) {
        if (queryService instanceof HeleadeQueryServiceImpl) {
            HeleadeQueryServiceImpl heleadeQueryService = (HeleadeQueryServiceImpl) queryService;
            var querySpec = querySpecJson == null
                    ? QuerySpec.Builder.newInstance().build()
                    : transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);

            //check if the original query did not have a limit
            boolean noLimit = (querySpecJson != null && !querySpecJson.containsKey(EDC_NAMESPACE + "limit"));
            return heleadeQueryService.countDatasets(querySpec, noLimit);
        } else {
            throw new IllegalStateException("Dataset query unavailable: QueryService is not of type HeleadeQueryServiceImpl");
        }
    }

    /**
     * Counts the number of cached datasets per keyword based on the provided query parameters.
     *
     * @param querySpecJson the query parameters as a JsonObject; if null, a default query specification is used
     * @return a String representing the number of datasets per keyword matching the query
     * @throws InvalidRequestException if the querySpecJson transformation to QuerySpec fails
     * @throws IllegalStateException if the QueryService is not of type HeleadeQueryServiceImpl
     */
    @Path("/keywords/count")
    @POST
    public String getCachedKeywordsCount(JsonObject querySpecJson) {
        if (queryService instanceof HeleadeQueryServiceImpl) {
            HeleadeQueryServiceImpl heleadeQueryService = (HeleadeQueryServiceImpl) queryService;
            var querySpec = querySpecJson == null
                    ? QuerySpec.Builder.newInstance().build()
                    : transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);

            //check if the original query did not have a limit
            boolean noLimit = (querySpecJson != null && !querySpecJson.containsKey(EDC_NAMESPACE + "limit"));
            return heleadeQueryService.countKeywords(querySpec, noLimit);
        } else {
            throw new IllegalStateException("Dataset query unavailable: QueryService is not of type HeleadeQueryServiceImpl");
        }
    }
}
