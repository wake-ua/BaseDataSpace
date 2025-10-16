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
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.UpdateOptions;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The MongodbFederatedCatalogCache is a MongoDB-based implementation of the FederatedCatalogCache interface.
 * It provides functionality to manage and query a catalog cache using MongoDB as the backend storage.
 * This class handles operations such as saving catalog entries, querying them based on filters,
 * expiring all entries, and cleaning up expired entries from the cache.
 */
public class MongodbFederatedCatalogCache extends MongodbFederatedCatalogCacheStore implements FederatedCatalogCache {

    /**
     * Represents the field used to identify datasets in the DCAT context within
     * the MongoDB Federated Catalog Cache. This field is used as a key to reference
     * datasets stored or queried in the catalog under the "dcat:dataset" schema.
     */
    public static final String DATASET_FIELD = "dcat:dataset";

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
     * Queries the store for {@code ContractOffer}s
     *
     * @param query A list of criteria the asset must fulfill
     * @return A collection of assets that are already in the store and that satisfy a given list of criteria.
     */
    @Override
    public Collection<Catalog> query(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return queryInternal(connection, query);
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
                return queryInternalDatasets(connection, query);
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


    private Collection<Catalog> queryInternal(MongoClient connection, QuerySpec querySpec) {
        List<Bson> aggregations = createAggregationPipeline(querySpec);
        var resultsStr = new java.util.ArrayList<String>();
        var results = new java.util.ArrayList<Catalog>();

        getCollection(connection, getFederatedCatalogCollectionName()).aggregate(
                aggregations
        ).forEach(doc -> resultsStr.add(doc.toJson()));

        for (String s : resultsStr) {
            JsonReader jsonReader = Json.createReader(new StringReader(s));
            JsonObject result = jsonReader.readObject();
            JsonObject resultExpanded = jsonLd.expand(result).getContent();
            jsonReader.close();
            Catalog catalog = this.transformerRegistry.transform(resultExpanded, Catalog.class).getContent();
            results.add(catalog);
        }

        return results;
    }

    private Collection<Dataset> queryInternalDatasets(MongoClient connection, QuerySpec querySpec) {
        List<Bson> aggregations = createDatasetAggregationPipeline(querySpec);
        var resultsStr = new java.util.ArrayList<String>();
        var results = new java.util.ArrayList<Dataset>();

        getCollection(connection, getFederatedCatalogCollectionName()).aggregate(
                aggregations
        ).forEach(doc -> resultsStr.add(doc.toJson()));

        for (String s : resultsStr) {
            JsonReader jsonReader = Json.createReader(new StringReader(s));
            JsonObject result = jsonReader.readObject();
            JsonObject resultExpanded = jsonLd.expand(result).getContent();
            jsonReader.close();
            Dataset dataset = this.transformerRegistry.transform(resultExpanded, Dataset.class).getContent();
            results.add(dataset);
        }

        return results;
    }

    private void upsertInternal(MongoClient connection, String id, Catalog catalog) {
        Bson filter = Filters.eq(getIdField(), id);
        UpdateOptions options = new UpdateOptions().upsert(true);
        JsonObject catalogJson = this.transformerRegistry.transform(catalog, JsonObject.class).getContent();
        JsonObject catalogJsonCompacted = jsonLd.compact(catalogJson).getContent();
        Document catalogDoc = Document.parse(catalogJsonCompacted.toString()).append(getIdField(), id);
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
    }

    private void deleteByMarkedTemplateInternal(MongoClient connection) {
        Bson filter = Filters.eq(getMarkedField(), true);
        MongoCollection<Document> collection =  getCollection(connection, getFederatedCatalogCollectionName());
        collection.deleteMany(filter);
    }

    private void expireAllInternal(MongoClient connection) {
        UpdateOptions options = new UpdateOptions().upsert(false);
        Document doc = Document.parse("{ $set: { " + getMarkedField() + ": true } }");
        MongoCollection<Document> collection = getCollection(connection, getFederatedCatalogCollectionName());
        collection.updateMany(Filters.empty(), doc, options);
    }


    /**
     * Creates an aggregation pipeline for querying documents in a MongoDB collection based on the provided {@code QuerySpec}.
     * The pipeline is built with stages to filter, sort, and paginate the results as per the query specification.
     *
     * @param querySpec The query specification containing filter, pagination, and sorting criteria.
     * @return A list of BSON objects representing the aggregation pipeline for querying the MongoDB collection.
     */
    public static List<Bson> createAggregationPipeline(QuerySpec querySpec) {
        // aggregation creation
        var aggregations = new java.util.ArrayList<Bson>();

        // Add basic filter to get catalogs with at least one dataset matching the filter criteria
        Bson filter = createFilter(querySpec, DATASET_FIELD + ".");

        if (!(Objects.equals(filter, Filters.empty()))) {
            aggregations.add(Aggregates.match(filter));

            // Create the filter expression for datasets using the MongoDB driver's filter API
            Bson filterInner = createFilterExpression(querySpec, "$$this.");
            Bson datasetsFilter = new Document("$filter",
                    new Document("input", "$" + DATASET_FIELD)
                            .append("cond", filterInner));

            // Create and add the "add fields" stage with the datasets filter
            Bson addFieldsStage = Aggregates.addFields(new Field<>(DATASET_FIELD, datasetsFilter));
            aggregations.add(addFieldsStage);

            // Filter out the catalogsw with no datasets matching the filter criteria
            aggregations.add(Aggregates.addFields(new Field<>("datasets_size", new Document("$size", "$" + DATASET_FIELD))));
            aggregations.add(Aggregates.match(Filters.gt("datasets_size", 0L)));
        }

        // Remove auxiliary fields
        aggregations.add(Aggregates.project(Document.parse("{_id: 0, datasets_size: 0}")));

        // Add pagination and sorting stages if necessary
        if (querySpec.getOffset() > 0) {
            aggregations.add(Aggregates.skip(querySpec.getOffset()));
        }
        if (querySpec.getLimit() > 0) {
            aggregations.add(Aggregates.limit(querySpec.getLimit()));
        }
        Bson sort = createSort(querySpec);
        if (sort != null) {
            aggregations.add(Aggregates.sort(sort));
        }

        return aggregations;
    }

    /**
     * Creates an aggregation pipeline for querying datasets in a MongoDB collection based on the provided {@code QuerySpec}.
     * The pipeline includes filtering, sorting, pagination, and various transformations to process datasets as per the query specification.
     *
     * @param querySpec The query specification containing filter criteria, pagination, and sorting options.
     * @return A list of BSON objects representing the aggregation pipeline for querying datasets in the MongoDB collection.
     */
    public static List<Bson> createDatasetAggregationPipeline(QuerySpec querySpec) {
        // aggregation creation
        var aggregations = new java.util.ArrayList<Bson>();

        // Add basic filter to get catalogs with at least one dataset matching the filter criteria
        Bson filter = createFilter(querySpec, DATASET_FIELD + ".");

        if (!(Objects.equals(filter, Filters.empty()))) {
            aggregations.add(Aggregates.match(filter));

            // Create the filter expression for datasets using the MongoDB driver's filter API
            Bson filterInner = createFilterExpression(querySpec, "$$this.");
            Bson datasetsFilter = new Document("$filter",
                    new Document("input", "$" + DATASET_FIELD)
                            .append("cond", filterInner));

            // Create and add the "add fields" stage with the datasets filter
            Bson addFieldsStage = Aggregates.addFields(new Field<>(DATASET_FIELD, datasetsFilter));
            aggregations.add(addFieldsStage);

            // Filter out the catalogs with no datasets matching the filter criteria
            aggregations.add(Aggregates.addFields(new Field<>("datasets_size", new Document("$size", "$" + DATASET_FIELD))));
            aggregations.add(Aggregates.match(Filters.gt("datasets_size", 0L)));
        }

        // Unwind
        UnwindOptions unwindOptions = new UnwindOptions().preserveNullAndEmptyArrays(false);
        aggregations.add(Aggregates.unwind("$dcat:dataset", unwindOptions));

        // Keep context from Catalog object
        aggregations.add(Aggregates.addFields(new Field<>("dcat:dataset.@context", "$@context")));

        // Replace root
        aggregations.add(Aggregates.replaceRoot("$dcat:dataset"));


        // Remove auxiliary fields
        aggregations.add(Aggregates.project(Document.parse("{_id: 0, datasets_size: 0}")));

        // Add pagination and sorting stages if necessary
        if (querySpec.getOffset() > 0) {
            aggregations.add(Aggregates.skip(querySpec.getOffset()));
        }
        if (querySpec.getLimit() > 0) {
            aggregations.add(Aggregates.limit(querySpec.getLimit()));
        }
        Bson sort = createSort(querySpec);
        if (sort != null) {
            aggregations.add(Aggregates.sort(sort));
        }

        return aggregations;
    }


    /**
     * Creates a MongoDB BSON filter from the given QuerySpec
     *
     * @param querySpec The query specification containing filter criteria
     * @param prefix Prefix to use for the fields
     * @return A BSON filter that can be used with MongoDB queries
     */
    public static Bson createFilter(QuerySpec querySpec, String prefix) {
        if (querySpec == null || querySpec.getFilterExpression().isEmpty()) {
            // Return a filter that matches all documents if no criteria are specified
            return Filters.empty();
        }

        List<Bson> filters = new ArrayList<>();

        // Process each criterion in the filter expression
        for (Criterion criterion : querySpec.getFilterExpression()) {
            String operator = criterion.getOperator();
            Object leftOperand = criterion.getOperandLeft();
            Object rightOperand = criterion.getOperandRight();

            // Convert the left operand to a field path string
            String fieldPath = prefix + leftOperand.toString();

            // Convert criterion to appropriate MongoDB filter
            switch (operator) {
                case "=":
                    filters.add(Filters.eq(fieldPath, rightOperand));
                    break;
                case "!=":
                    filters.add(Filters.ne(fieldPath, rightOperand));
                    break;
                case ">":
                    filters.add(Filters.gt(fieldPath, rightOperand));
                    break;
                case ">=":
                    filters.add(Filters.gte(fieldPath, rightOperand));
                    break;
                case "<":
                    filters.add(Filters.lt(fieldPath, rightOperand));
                    break;
                case "<=":
                    filters.add(Filters.lte(fieldPath, rightOperand));
                    break;
                case "in":
                    // Handle 'in' operator - convert right operand to a collection if it's not already
                    if (rightOperand instanceof Collection) {
                        filters.add(Filters.in(fieldPath, (Collection<?>) rightOperand));
                    } else if (rightOperand instanceof Object[]) {
                        filters.add(Filters.in(fieldPath, Arrays.asList((Object[]) rightOperand)));
                    } else {
                        // If it's a single value, create a singleton list
                        filters.add(Filters.in(fieldPath, Collections.singletonList(rightOperand)));
                    }
                    break;
                case "like":
                case "contains":
                    // For 'like' and 'contains' queries, convert SQL-like patterns to regex
                    String pattern = rightOperand.toString();
                    if (!pattern.startsWith("%")) {
                        pattern = "^" + pattern; // Anchor to start if no leading wildcard
                    } else {
                        pattern = pattern.substring(1); // Remove leading %
                    }
                    if (!pattern.endsWith("%")) {
                        pattern = pattern + "$"; // Anchor to end if no trailing wildcard
                    } else {
                        pattern = pattern.substring(0, pattern.length() - 1); // Remove trailing %
                    }
                    // Replace SQL wildcards with regex patterns
                    pattern = pattern.replace("%", ".*").replace("_", ".");
                    filters.add(Filters.regex(fieldPath, pattern, "i")); // "i" for case-insensitive
                    break;
                case "exists":
                    boolean exists = Boolean.parseBoolean(rightOperand.toString());
                    filters.add(exists ? Filters.exists(fieldPath) : Filters.exists(fieldPath, false));
                    break;
                case "between":
                    // Assuming the right operand is an array or collection with exactly two elements
                    if (rightOperand instanceof Object[] array && array.length == 2) {
                        filters.add(Filters.and(
                                Filters.gte(fieldPath, array[0]),
                                Filters.lte(fieldPath, array[1])
                        ));
                    } else if (rightOperand instanceof Collection coll && coll.size() == 2) {
                        Iterator<?> iterator = coll.iterator();
                        Object lower = iterator.next();
                        Object upper = iterator.next();
                        filters.add(Filters.and(
                                Filters.gte(fieldPath, lower),
                                Filters.lte(fieldPath, upper)
                        ));
                    } else {
                        throw new IllegalArgumentException("Between operator requires exactly two values for comparison");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        }

        // Combine all filters with AND operator
        return filters.isEmpty() ? Filters.empty() : Filters.and(filters);
    }

    /**
     * Creates a MongoDB BSON filter as an expression from the given QuerySpec
     *
     * @param querySpec The query specification containing filter criteria
     * @param prefix Prefix to use for the fields
     * @return A BSON filter expression that can be used with MongoDB queries
     */
    public static Bson createFilterExpression(QuerySpec querySpec, String prefix) {
        if (querySpec == null || querySpec.getFilterExpression().isEmpty()) {
            return Document.parse("{}"); // Return an empty MongoDB expression
        }

        List<Document> filters = new ArrayList<>();

        for (Criterion criterion : querySpec.getFilterExpression()) {
            String operator = criterion.getOperator();
            Object leftOperand = criterion.getOperandLeft();
            Object rightOperand = criterion.getOperandRight();

            String fieldPath = prefix + leftOperand.toString();

            // Generate MongoDB expressions for each of the operators
            switch (operator) {
                case "=":
                    filters.add(new Document("$eq", Arrays.asList(fieldPath, rightOperand)));
                    break;
                case "!=":
                    filters.add(new Document("$ne", Arrays.asList(fieldPath, rightOperand)));
                    break;
                case ">":
                    filters.add(new Document("$gt", Arrays.asList(fieldPath, rightOperand)));
                    break;
                case ">=":
                    filters.add(new Document("$gte", Arrays.asList(fieldPath, rightOperand)));
                    break;
                case "<":
                    filters.add(new Document("$lt", Arrays.asList(fieldPath, rightOperand)));
                    break;
                case "<=":
                    filters.add(new Document("$lte", Arrays.asList(fieldPath, rightOperand)));
                    break;
                default:
                    // ignore unsupported operators
            }
        }

        // Combine all filters with $and operator
        if (!filters.isEmpty()) {
            StringBuilder combined = new StringBuilder("{$and: [");
            for (Document filter : filters) {
                combined.append(filter.toJson()).append(",");
            }
            combined.setLength(combined.length() - 1); // remove trailing comma
            combined.append("]}");
            return Document.parse(combined.toString());
        }

        return Document.parse("{}");
    }

    /**
     * Creates a MongoDB BSON sort parameter from the given QuerySpec
     *
     * @param querySpec The query specification containing sort field and order
     * @return A BSON sort object that can be used with MongoDB queries, or null if no sorting is specified
     */
    public static Bson createSort(QuerySpec querySpec) {
        if (querySpec == null || querySpec.getSortField() == null || querySpec.getSortField().isEmpty()) {
            // Return null if no sort field is specified
            return null;
        }

        String sortField = querySpec.getSortField();

        // Special handling for nested fields - MongoDB uses dot notation for nested fields
        if (sortField.contains(":")) {
            // Assuming Eclipse EDC uses ":" for nested properties, convert to MongoDB dot notation
            sortField = sortField.replace(":", ".");
        }

        // Create sort based on the specified sort order
        SortOrder sortOrder = querySpec.getSortOrder();
        if (sortOrder == null) {
            sortOrder = SortOrder.ASC; // Default to ascending if not specified
        }

        // Convert SortOrder to MongoDB sort direction
        return switch (sortOrder) {
            case ASC -> Sorts.ascending(sortField);
            case DESC -> Sorts.descending(sortField);
            default -> throw new IllegalArgumentException("Unsupported sort order: " + sortOrder);
        };
    }

}


