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

}
