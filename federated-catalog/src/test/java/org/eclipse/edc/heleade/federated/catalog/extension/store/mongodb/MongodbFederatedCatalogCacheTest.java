/*
 *  Copyright (c) 2025 University of Alicante
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       University of Alicante - Initial implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.cache.MongodbFederatedCatalogCache;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MongodbFederatedCatalogCacheTest {

    @Test
    void catalogShouldProvideEmptyPipelineWhenEmptyQuerySpecProvided() {
        QuerySpec querySpecEmpty = QuerySpec.Builder.newInstance().build();
        List<Bson> aggregation = MongodbFederatedCatalogCache.createCatalogAggregationPipeline(querySpecEmpty);
        assert aggregation.size() <= 3;
        for (Bson stage : aggregation) {
            BsonDocument stageDocument = stage.toBsonDocument();
            assert !stageDocument.containsKey("$match");
            assert stageDocument.containsKey("$addFields") || stageDocument.containsKey("$limit") ||
                   stageDocument.containsKey("$skip") || stageDocument.containsKey("$project");
        }
    }

    @Test
    void catalogShouldProvidePipelineWhenQuerySpecProvided() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", "assetId")).build();

        List<Bson> aggregation = MongodbFederatedCatalogCache.createCatalogAggregationPipeline(querySpec);

        // Just for dev purposes
        String aggregationsJsonString = MongodbFederatedCatalogCache.getAggregationPipelineAsJson(aggregation);

        assert aggregation.size() > 3;

        Bson addCatalogFieldsStage = aggregation.get(0);
        assert addCatalogFieldsStage.toBsonDocument().containsKey("$addFields");

        Bson matchStage = aggregation.get(1);
        assert matchStage.toBsonDocument().containsKey("$match");

        Bson addFieldsStage = aggregation.get(2);
        assert addFieldsStage.toBsonDocument().containsKey("$addFields");

        Bson addFieldsSizeStage = aggregation.get(3);
        assert addFieldsSizeStage.toBsonDocument().containsKey("$addFields");

        Bson matchStageSize = aggregation.get(4);
        assert matchStageSize.toBsonDocument().containsKey("$match");
    }

    @Test
    void datasetShouldProvideEmptyPipelineWhenEmptyQuerySpecProvided() {
        QuerySpec querySpecEmpty = QuerySpec.Builder.newInstance().build();
        List<Bson> aggregation = MongodbFederatedCatalogCache.createDatasetAggregationPipeline(querySpecEmpty);

        // Just for dev purposes
        String aggregationsJsonString = MongodbFederatedCatalogCache.getAggregationPipelineAsJson(aggregation);

        assert aggregation.size() <= 6;
        for (Bson stage : aggregation) {
            BsonDocument stageDocument = stage.toBsonDocument();
            assert !stageDocument.containsKey("$match");
        }
    }

    @Test
    void datasetShouldProvidePipelineWhenQuerySpecProvided() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", "assetId")).build();

        List<Bson> aggregation = MongodbFederatedCatalogCache.createDatasetAggregationPipeline(querySpec);

        // Just for dev purposes
        String aggregationsJsonString = MongodbFederatedCatalogCache.getAggregationPipelineAsJson(aggregation);

        assert aggregation.size() > 3;

        Bson addCatalogFieldsStage = aggregation.get(0);
        assert addCatalogFieldsStage.toBsonDocument().containsKey("$addFields");

        Bson matchStage = aggregation.get(1);
        assert matchStage.toBsonDocument().containsKey("$match");

        Bson addFieldsStage = aggregation.get(2);
        assert addFieldsStage.toBsonDocument().containsKey("$addFields");

        Bson addFieldsSizeStage = aggregation.get(3);
        assert addFieldsSizeStage.toBsonDocument().containsKey("$addFields");

        Bson matchStageSize = aggregation.get(4);
        assert matchStageSize.toBsonDocument().containsKey("$match");
    }
}
