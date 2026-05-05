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
import org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb.cache.MongodbFederatedCatalogCacheQuery;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MongodbFederatedCatalogCacheQueryTest {

    @Test
    void catalogShouldProvideEmptyPipelineWhenEmptyQuerySpecProvided() {
        QuerySpec querySpecEmpty = QuerySpec.Builder.newInstance().build();
        List<Bson> aggregation = MongodbFederatedCatalogCacheQuery.createAggregationPipeline(querySpecEmpty);
        assert aggregation.size() <= 3;
        for (Bson stage : aggregation) {
            BsonDocument stageDocument = stage.toBsonDocument();
            assert !stageDocument.containsKey("$match");
            assert stageDocument.containsKey("$addFields") || stageDocument.containsKey("$limit") ||
                   stageDocument.containsKey("$skip") || stageDocument.containsKey("$project");
        }
    }

    @Test
    void shouldProvidePipelineWhenQuerySpecProvided() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", "assetId")).build();

        List<Bson> aggregation = MongodbFederatedCatalogCacheQuery.createAggregationPipeline(querySpec);

        // Just for dev purposes
        String aggregationsJsonString = MongodbFederatedCatalogCacheQuery.getAggregationPipelineAsJson(aggregation);

        assert aggregation.size() == 2;

        Bson matchStage = aggregation.get(0);
        assert matchStage.toBsonDocument().containsKey("$match");
        assert matchStage.toBsonDocument().get("$match").toString().contains("assetId");

        Bson limitStage = aggregation.get(1);
        assert limitStage.toBsonDocument().containsKey("$limit");
    }

    @Test
    void shouldProvidePipelineWhenQuerySpecProvidedWithLike() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "like", "assetId")).build();

        List<Bson> aggregation = MongodbFederatedCatalogCacheQuery.createAggregationPipeline(querySpec);

        // Just for dev purposes
        String aggregationsJsonString = MongodbFederatedCatalogCacheQuery.getAggregationPipelineAsJson(aggregation);

        assert aggregation.size() == 2;

        Bson matchStage = aggregation.get(0);
        assert matchStage.toBsonDocument().containsKey("$match");
        assert matchStage.toBsonDocument().get("$match").toString().contains("^assetId$");

        Bson limitStage = aggregation.get(1);
        assert limitStage.toBsonDocument().containsKey("$limit");
    }

    @Test
    void keywordCountShouldProvidePipelineWhenEmptyQuerySpecProvided() {
        QuerySpec querySpecEmpty = QuerySpec.Builder.newInstance().build();

        List<Bson> aggregation = MongodbFederatedCatalogCacheQuery.createKeywordCountAggregationPipeline(querySpecEmpty, true);

        // Just for dev purposes
        String aggregationsJsonString = MongodbFederatedCatalogCacheQuery.getAggregationPipelineAsJson(aggregation);

        assert aggregation.size() == 5;

        Bson addCatalogFieldsStage = aggregation.get(0);
        assert addCatalogFieldsStage.toBsonDocument().containsKey("$project");

        Bson matchStage = aggregation.get(1);
        assert matchStage.toBsonDocument().containsKey("$unwind");

        Bson addFieldsStage = aggregation.get(2);
        assert addFieldsStage.toBsonDocument().containsKey("$group");

        Bson addFieldsSizeStage = aggregation.get(3);
        assert addFieldsSizeStage.toBsonDocument().containsKey("$project");

        Bson matchStageSize = aggregation.get(4);
        assert matchStageSize.toBsonDocument().containsKey("$sort");
    }

}
