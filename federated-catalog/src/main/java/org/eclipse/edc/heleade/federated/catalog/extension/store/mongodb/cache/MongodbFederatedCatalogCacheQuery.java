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

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UnwindOptions;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a MongoDB-based cache for federated catalog data, providing functionalities
 * for storing, querying, and managing `ContractOffer` and dataset information.
 */
public class MongodbFederatedCatalogCacheQuery {

    /**
     * Queries the internal catalog from a MongoDB collection using the specified query specification.
     *
     * @param querySpec the query specification containing filtering, sorting, and pagination criteria
     * @param collection the MongoDB collection to query
     * @param jsonLd the JSON-LD processor used for data expansion
     * @param transformerRegistry the registry for transforming JSON-LD expanded objects into Catalog instances
     * @return a collection of catalogs matching the query criteria
     */
    public static Collection<Catalog> queryInternalCatalog(QuerySpec querySpec, MongoCollection<Document> collection, JsonLd jsonLd, TypeTransformerRegistry transformerRegistry) {
        List<Bson> aggregations = createAggregationPipeline(querySpec);
        var resultsStr = new ArrayList<String>();
        var results = new ArrayList<Catalog>();

        collection.aggregate(aggregations).forEach(doc -> resultsStr.add(doc.toJson()));

        for (String s : resultsStr) {
            JsonReader jsonReader = Json.createReader(new StringReader(s));
            JsonObject result = jsonReader.readObject();
            JsonObject resultExpanded = jsonLd.expand(result).getContent();
            jsonReader.close();
            Catalog catalog = transformerRegistry.transform(resultExpanded, Catalog.class).getContent();
            results.add(catalog);
        }

        return results;
    }

    /**
     * Queries internal datasets from a MongoDB collection based on the provided query specification.
     *
     * @param querySpec the query specification containing filtering, sorting, and pagination criteria
     * @param collection the MongoDB collection to query for datasets
     * @param jsonLd the JSON-LD processor used for data expansion
     * @param transformerRegistry the registry used to transform JSON-LD expanded objects to Dataset instances
     * @return a collection of datasets matching the query criteria
     */
    public static Collection<Dataset> queryInternalDatasets(QuerySpec querySpec, MongoCollection<Document> collection, JsonLd jsonLd, TypeTransformerRegistry transformerRegistry) {
        List<Bson> aggregations = createAggregationPipeline(querySpec);
        var resultsStr = new java.util.ArrayList<String>();
        var results = new java.util.ArrayList<Dataset>();

        collection.aggregate(aggregations).forEach(doc -> resultsStr.add(doc.toJson()));

        for (String s : resultsStr) {
            JsonReader jsonReader = Json.createReader(new StringReader(s));
            JsonObject result = jsonReader.readObject();
            JsonObject resultExpanded = jsonLd.expand(result).getContent();
            jsonReader.close();
            Dataset dataset = transformerRegistry.transform(resultExpanded, Dataset.class).getContent();
            results.add(dataset);
        }

        return results;
    }

    /**
     * Counts the datasets that match the query specification, applying an optional limit removal.
     *
     * @param querySpec the query specification containing filtering, sorting, and pagination criteria
     * @param collection the MongoDB collection to query
     * @param noLimit whether to remove limit and skip stages from the aggregation pipeline
     * @return a JSON string representing the count of datasets, with a default of {"count": 0} if no matches are found
     */
    public static String countInternalDatasets(QuerySpec querySpec, MongoCollection<Document> collection, boolean noLimit) {
        List<Bson> aggregations = createAggregationPipeline(querySpec);
        // remove limit stage
        if (noLimit) {
            aggregations.removeIf(stage -> (stage.getClass().getSimpleName().equals("BsonDocument") && stage.toBsonDocument().containsKey("$limit")));
            aggregations.removeIf(stage -> (stage.getClass().getSimpleName().equals("BsonDocument") && stage.toBsonDocument().containsKey("$skip")));
        }
        // add count
        aggregations.add(Aggregates.count());

        // run the aggregation
        var documents = collection.aggregate(aggregations);

        // check result is not empty
        if (documents.iterator().hasNext()) {
            var document = documents.first();
            if (document != null && !document.isEmpty()) {
                return document.toJson();
            }
        }

        return "{\"count\": 0}";
    }

    /**
     * Counts the keywords that match the query specification, applying an optional limit removal.
     *
     * @param querySpec the query specification containing filtering, sorting, and pagination criteria
     * @param collection the MongoDB collection to query
     * @param noLimit whether to remove limit and skip stages from the aggregation pipeline
     * @return a JSON string representing the count of keywords, formatted as a JSON array;
     *         returns an empty array "[]" if no matches are found
     */
    public static String countInternalKeywords(QuerySpec querySpec, MongoCollection<Document> collection, boolean noLimit) {

        List<Bson> aggregations = createKeywordCountAggregationPipeline(querySpec, noLimit);

        // run the aggregation
        var documents = collection.aggregate(aggregations);

        String result = "[";
        var resultsStr = new java.util.ArrayList<String>();

        // check result is not empty
        if (documents.iterator().hasNext()) {
            documents.forEach(doc -> resultsStr.add(doc.toJson()));
            result += String.join(", ", resultsStr);
        }

        result += "]";
        return result;
    }


    /**
     * Creates a MongoDB aggregation pipeline based on the provided query specification and optional dataset unwinding.
     *
     * @param querySpec the query specification containing filtering, sorting, and pagination criteria
     * @return a list of BSON objects representing the aggregation pipeline
     */
    public static List<Bson> createAggregationPipeline(QuerySpec querySpec) {
        // aggregation creation
        var aggregations = new ArrayList<Bson>();

        // Add basic filter to get catalogs with at least one dataset matching the filter criteria
        Bson filter = createFilter(querySpec, "");

        if (!(Objects.equals(filter, Filters.empty()))) {
            aggregations.add(Aggregates.match(filter));
        }

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
     * Creates a MongoDB aggregation pipeline based on the provided query specification for keyword totals.
     *
     * @param querySpec the query specification containing filtering, sorting, and pagination criteria
     * @param noLimit a flag indicating whether to limit dataset entries in the pipeline
     * @return a list of BSON objects representing the aggregation pipeline
     */
    public static List<Bson> createKeywordCountAggregationPipeline(QuerySpec querySpec, boolean noLimit) {

        List<Bson> aggregations = createAggregationPipeline(querySpec);

        // remove limit stage
        if (noLimit) {
            aggregations.removeIf(stage -> (stage.getClass().getSimpleName().equals("BsonDocument") && stage.toBsonDocument().containsKey("$limit")));
            aggregations.removeIf(stage -> (stage.getClass().getSimpleName().equals("BsonDocument") && stage.toBsonDocument().containsKey("$skip")));
        }

        // remove sort
        aggregations.removeIf(stage -> (stage.getClass().getSimpleName().equals("BsonDocument") && stage.toBsonDocument().containsKey("$sort")));

        // project to leave only the keyword field
        aggregations.add(Aggregates.project(Document.parse("{_id: 0, \"dcat:keyword\": 1}")));

        // unwind the keywords
        UnwindOptions unwindOptions = new UnwindOptions().preserveNullAndEmptyArrays(false);
        aggregations.add(Aggregates.unwind("$dcat:keyword", unwindOptions));

        // group the keywords
        aggregations.add(Aggregates.group("$dcat:keyword", List.of(new BsonField("count", Document.parse("{ $sum: 1}")))));

        // project to replace _id with keyword
        aggregations.add(Aggregates.project(Document.parse("{_id: 0, \"dcat:keyword\": \"$_id\", count: 1}")));

        // sort decreasingly by count
        aggregations.add(Aggregates.sort(Document.parse("{\"count\": -1, \"dcat:keyword\": 1}")));

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
                case "like", "contains":
                    // For 'like' and 'contains' queries, convert SQL-like patterns to regex
                    filters.add(Filters.regex(fieldPath, getRegExp(rightOperand.toString()), "i"));
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

    private static String getRegExp(String pattern) {
        // For 'like' and 'contains' queries, convert SQL-like patterns to regex
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
        return pattern; // "i" for case-insensitive

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

    /**
     * Converts a list of MongoDB aggregation pipeline stages represented as BSON objects into a JSON string.
     * The resulting JSON string includes formatted indentation for improved readability.
     *
     * @param aggregations the list of BSON aggregation pipeline stages to convert into a JSON representation
     * @return a JSON string representing the aggregation pipeline
     */
    public static String getAggregationPipelineAsJson(List<Bson> aggregations) {
        var codecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        List<BsonDocument> bsonDocuments = aggregations.stream()
                .map(agg -> agg.toBsonDocument(BsonDocument.class, codecRegistry))
                .collect(Collectors.toList());

        // Use JsonWriterSettings to control the JSON formatting
        JsonWriterSettings settings = JsonWriterSettings.builder()
                .indent(true)  // Optional: for pretty printing
                .build();

        // Convert to JSON string
        return bsonDocuments.stream()
                .map(doc -> doc.toJson(settings))
                .collect(Collectors.joining(",\n", "[\n", "\n]"));
    }
}


