/*
 *  Copyright (c) 2024 Fraunhofer-Gesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.directory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;

import java.io.IOException;
import java.util.List;

/**
 * This class reads the participants from a file defined in the configuration
 */
public class CatalogNodeDirectory implements TargetNodeDirectory {

    private final ObjectMapper objectMapper;
    private final String participantFileContent;

    /**
     * Instantiates a new object
     *
     * @param objectMapper reads JSON content
     * @param participantFileContent list of DS participants
     */
    public CatalogNodeDirectory(ObjectMapper objectMapper, String participantFileContent) {
        this.objectMapper = objectMapper;
        this.participantFileContent = participantFileContent;
    }

    @Override
    public List<TargetNode> getAll() {
        try {
            return objectMapper.readValue(participantFileContent, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insert(TargetNode targetNode) {

    }
}
