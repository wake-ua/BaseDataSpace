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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MongodbFederatedCatalogCacheTest {

    @Test
    void shouldProvideEmptyPipelineWhenEmptyQuerySpecProvided() {
        QuerySpec querySpecEmpty = QuerySpec.Builder.newInstance().build();
        List<Bson> aggregation = MongodbFederatedCatalogCache.createAggregationPipeline(querySpecEmpty);
        assert aggregation.size() <= 2;
        for (Bson stage : aggregation) {
            BsonDocument stageDocument = stage.toBsonDocument();
            assert stageDocument.containsKey("$limit") || stageDocument.containsKey("$skip") ||
                    stageDocument.containsKey("$project");
        }
    }

    @Test
    void shouldProvidePipelineWhenQuerySpecProvided() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", "assetId")).build();

        List<Bson> aggregation = MongodbFederatedCatalogCache.createAggregationPipeline(querySpec);
        assert aggregation.size() > 3;

        Bson matchStage = aggregation.get(0);
        assert matchStage.toBsonDocument().containsKey("$match");

        Bson addFieldsStage = aggregation.get(1);
        assert addFieldsStage.toBsonDocument().containsKey("$addFields");

        Bson addFieldsSizeStage = aggregation.get(2);
        assert addFieldsSizeStage.toBsonDocument().containsKey("$addFields");

        Bson matchStageSize = aggregation.get(3);
        assert matchStageSize.toBsonDocument().containsKey("$match");
    }
}
