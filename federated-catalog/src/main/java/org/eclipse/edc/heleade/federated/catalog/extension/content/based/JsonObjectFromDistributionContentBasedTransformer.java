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

package org.eclipse.edc.heleade.federated.catalog.extension.content.based;

import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;

public class JsonObjectFromDistributionContentBasedTransformer extends JsonObjectFromDistributionTransformer {

    public JsonObjectFromDistributionContentBasedTransformer(jakarta.json.JsonBuilderFactory jsonFactory) {
        super(jsonFactory);
    }
}