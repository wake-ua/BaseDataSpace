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

package org.eclipse.edc.heleade.federated.catalog.extension.api.query;

import org.eclipse.edc.catalog.cache.query.QueryServiceImpl;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.cache.MongodbFederatedCatalogCache;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

/**
 * Implementation of QueryService that retrieves dataset information using a FederatedCatalogCache.
 * This class is designed to provide query functionalities specifically for Heleade.
 */
public class HeleadeQueryServiceImpl extends QueryServiceImpl implements QueryService {

    private final FederatedCatalogCache cache;

    /**
     * Constructs a new instance of HeleadeQueryServiceImpl.
     *
     * @param cache the FederatedCatalogCache used for querying datasets
     */
    public HeleadeQueryServiceImpl(FederatedCatalogCache cache) {
        super(cache);
        this.cache = cache;
    }

    /**
     * Retrieves datasets based on the provided query specification.
     *
     * @param query the query specification containing filtering and pagination details
     * @return a ServiceResult containing a collection of datasets or an error result if the operation fails
     */
    public ServiceResult<Collection<Dataset>> getDatasets(QuerySpec query) {
        if (this.cache instanceof MongodbFederatedCatalogCache) {
            MongodbFederatedCatalogCache mongoCache = (MongodbFederatedCatalogCache) this.cache;
            return ServiceResult.from(Result.ofThrowable(() -> mongoCache.queryDatasets(query)));
        } else {
            throw new IllegalStateException("Dataset query unavailable: Cache is not of type MongodbFederatedCatalogCache");
        }
    }

    /**
     * Counts the datasets based on the provided query specification.
     *
     * @param query the query specification containing filtering and pagination details
     * @return a String representing the number of datasets matching the query
     * @throws IllegalStateException if the cache is not of type MongodbFederatedCatalogCache
     */
    public String countDatasets(QuerySpec query) {
        if (this.cache instanceof MongodbFederatedCatalogCache) {
            MongodbFederatedCatalogCache mongoCache = (MongodbFederatedCatalogCache) this.cache;
            return mongoCache.countDatasets(query);
        } else {
            throw new IllegalStateException("Dataset count unavailable: Cache is not of type MongodbFederatedCatalogCache");
        }
    }
}
