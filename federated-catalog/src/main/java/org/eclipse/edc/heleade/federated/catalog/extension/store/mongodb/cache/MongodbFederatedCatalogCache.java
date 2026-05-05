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

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Collection;

import static java.util.Optional.ofNullable;

/**
 * Represents a MongoDB-based cache for federated catalog data, providing functionalities
 * for storing, querying, and managing `ContractOffer` and dataset information.
 */
public class MongodbFederatedCatalogCache extends MongodbFederatedCatalogCacheStore implements FederatedCatalogCache {

    /**
     * Represents the field used to identify datasets in the DCAT context within
     * the MongoDB Federated Catalog Cache. This field is used as a key to reference
     * datasets stored or queried in the catalog under the "dcat:dataset" schema.
     */
    public static final String DATASET_FIELD = "dcat:dataset";

    /**
     * Field name used to represent a participant's identifier in the federated catalog stored in MongoDB.
     */
    public static final String PARTICIPANT_FIELD = "dspace:participantId";

    private final JsonLd jsonLd;
    private final TypeTransformerRegistry transformerRegistry;

    /**
     * Represents a cache for federated catalog data stored in MongoDB.
     *
     * @param dataSourceUri the URI of the MongoDB data source to connect to
     * @param dataSourceDb the name of the MongoDB database to use
     * @param transactionContext the transaction context to manage database transactions
     * @param objectMapper the object mapper for handling JSON serialization and deserialization
     * @param jsonLd the JsonLd instance for processing JSON-LD data
     * @param transformerRegistry the registry for type transformers
     */
    public MongodbFederatedCatalogCache(String dataSourceUri, String dataSourceDb, TransactionContext transactionContext, ObjectMapper objectMapper, JsonLd jsonLd, TypeTransformerRegistry transformerRegistry) {
        super(dataSourceUri, dataSourceDb, transactionContext, objectMapper);
        this.jsonLd = jsonLd;
        this.transformerRegistry = transformerRegistry;
    }

    /**
     * Adds an {@code ContractOffer} to the store
     */
    @Override
    public void save(Catalog catalog) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var id = ofNullable(catalog.getProperties().get(CatalogConstants.PROPERTY_ORIGINATOR))
                        .map(Object::toString)
                        .orElse(catalog.getId());
                upsertInternal(connection, id, catalog);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Deletes all entries from the cache that are marked as "expired"
     */
    @Override
    public void deleteExpired() {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                deleteByMarkedTemplateInternal(connection);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Marks all entries as "expired", i.e. marks them for deletion
     */
    @Override
    public void expireAll() {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                expireAllInternal(connection);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Queries the store for {@code ContractOffer}s
     *
     * @param query A list of criteria the catalog must fulfill
     * @return A collection of catalogs that are already in the store and that satisfy a given list of criteria.
     */
    @Override
    public Collection<Catalog> query(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogCollectionName());
                return MongodbFederatedCatalogCacheQuery.queryInternalCatalog(query, collection, jsonLd, transformerRegistry);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Queries datasets from the federated catalog cache based on the provided query specification.
     *
     * @param query the query specification containing filtering, sorting, and pagination criteria
     * @return a collection of datasets that match the criteria specified in the query
     */
    public Collection<Dataset> queryDatasets(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogDatasetCollectionName());
                return MongodbFederatedCatalogCacheQuery.queryInternalDatasets(query, collection, jsonLd, transformerRegistry);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Counts the datasets in the federated catalog cache based on the provided query specification.
     *
     * @param query   the query specification containing filtering, sorting, and pagination criteria
     * @param noLimit a flag indicating whether the limit stage should be omitted from the query
     * @return a JSON string representing the count of datasets that match the query criteria
     */
    public String countDatasets(QuerySpec query, boolean noLimit) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogDatasetCollectionName());
                return MongodbFederatedCatalogCacheQuery.countInternalDatasets(query, collection, noLimit);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Counts the datasets per keyword in the federated catalog cache based on the provided query specification.
     *
     * @param query   the query specification containing filtering, sorting, and pagination criteria
     * @param noLimit a flag indicating whether the limit stage should be omitted from the query
     * @return a JSON string representing the count of datasets per keyword that match the query criteria
     */
    public String countKeywords(QuerySpec query, boolean noLimit) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var collection = getCollection(connection, getFederatedCatalogDatasetCollectionName());
                return MongodbFederatedCatalogCacheQuery.countInternalKeywords(query, collection, noLimit);
            } catch (Exception e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void upsertInternal(MongoClient connection, String id, Catalog catalog) {
        Bson filter = Filters.eq(getIdField(), id);
        UpdateOptions options = new UpdateOptions().upsert(true);
        JsonObject catalogJson = this.transformerRegistry.transform(catalog, JsonObject.class).getContent();
        JsonObject catalogJsonCompacted = jsonLd.compact(catalogJson).getContent();
        JsonObject catalogJsonCompactedDatasetArray = ensureDatasetAsArray(catalogJsonCompacted);
        Document catalogDoc = Document.parse(catalogJsonCompactedDatasetArray.toString()).append(getIdField(), id);
        // Create a new document with just the fields we want to set
        Document setDoc = new Document();

        // Add each field from the catalog document to our set document
        for (String key : catalogDoc.keySet()) {
            setDoc.append(key, catalogDoc.get(key));
        }
        setDoc.append(getIdField(), id);

        Bson update = new Document("$set", setDoc);
        MongoCollection<Document> collection = getCollection(connection, getFederatedCatalogCollectionName());
        collection.updateOne(filter, update, options);

        // Upsert datasets
        String participantId = catalogDoc.getString("dspace:participantId");
        var context = catalogDoc.get("@context");
        for (JsonValue dataset : catalogJsonCompactedDatasetArray.getJsonArray("dcat:dataset")) {
            var datasetJson = dataset.asJsonObject();
            var datasetId = datasetJson.getString("@id");
            var datasetDoc = Document.parse(datasetJson.toString()).append("dspace:participantId", participantId).append("@context", context);
            collection = getCollection(connection, getFederatedCatalogDatasetCollectionName());
            collection.updateOne(Filters.and(Filters.eq("@id", datasetId),
                    Filters.eq("dspace:participantId", participantId)),
                    new Document("$set", datasetDoc), options);
        }
    }

    private void deleteByMarkedTemplateInternal(MongoClient connection) {
        Bson filter = Filters.eq(getMarkedField(), true);
        MongoCollection<Document> collection =  getCollection(connection, getFederatedCatalogCollectionName());
        collection.deleteMany(filter);
        MongoCollection<Document> collectionDatasets =  getCollection(connection, getFederatedCatalogDatasetCollectionName());
        collectionDatasets.deleteMany(filter);
    }

    private void expireAllInternal(MongoClient connection) {
        UpdateOptions options = new UpdateOptions().upsert(false);
        Document doc = Document.parse("{ $set: { " + getMarkedField() + ": true } }");
        MongoCollection<Document> collection = getCollection(connection, getFederatedCatalogCollectionName());
        collection.updateMany(Filters.empty(), doc, options);
        MongoCollection<Document> collectionDatasets =  getCollection(connection, getFederatedCatalogDatasetCollectionName());
        collectionDatasets.updateMany(Filters.empty(), doc, options);
    }

    private JsonObject ensureDatasetAsArray(JsonObject catalogJson) {
        var dataset = catalogJson.get("dcat:dataset");
        if (dataset != null) {
            if (dataset instanceof JsonObject) {
                var catalogWithArrayBuilder = Json.createObjectBuilder(catalogJson);
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                arrayBuilder.add(dataset);
                catalogWithArrayBuilder.add("dcat:dataset", arrayBuilder.build());
                return catalogWithArrayBuilder.build();
            }
        }
        return catalogJson;
    }

}


