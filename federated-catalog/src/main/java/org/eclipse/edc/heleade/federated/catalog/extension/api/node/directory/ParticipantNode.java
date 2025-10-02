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

package org.eclipse.edc.heleade.federated.catalog.extension.api.node.directory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.crawler.spi.TargetNode;

import java.util.List;

/**
 * Represents a participant in a system, including details such as name, identifier, URL,
 * supported protocols, claims, and credentials.
 * This class is immutable and can function as a data transfer object for serialization and deserialization.
 */
public record ParticipantNode(@JsonProperty("name") String name,
                              @JsonProperty("id") String id,
                              @JsonProperty("url") String targetUrl,
                              @JsonProperty("supportedProtocols") List<String> supportedProtocols,
                              @JsonProperty("claims") List<String> claims,
                              @JsonProperty("credentials") List<String> credentials) {

    /**
     * Constructs a new instance of ParticipantNode.
     *
     * @param name the name of the participant
     * @param id the unique identifier of the participant
     * @param targetUrl the URL associated with the participant
     * @param supportedProtocols the list of supported protocols for the participant
     * @param claims the list of claims associated with the participant
     * @param credentials the list of credentials associated with the participant
     */
    @JsonCreator
    public ParticipantNode {
    }

    /**
     * Constructs a ParticipantNode instance using the provided TargetNode.
     *
     * @param targetNode the TargetNode instance containing name, id, target URL, and supported protocols
     */
    public ParticipantNode(TargetNode targetNode) {
        this(targetNode.name(), targetNode.id(), targetNode.targetUrl(), targetNode.supportedProtocols(), List.of(), List.of());
    }
    
    /**
     * Converts this ParticipantNode instance into a TargetNode instance.
     *
     * @return a new TargetNode containing the name, id, targetUrl, and supportedProtocols of this ParticipantNode
     */
    public TargetNode asTargetNode() {
        return new TargetNode(name, id, targetUrl, supportedProtocols);
    }
}