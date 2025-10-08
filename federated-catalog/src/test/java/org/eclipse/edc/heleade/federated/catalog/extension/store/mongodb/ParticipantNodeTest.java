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

package org.eclipse.edc.heleade.federated.catalog.extension.store.mongodb;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;
import org.eclipse.edc.heleade.federated.catalog.extension.api.node.directory.ParticipantNode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class ParticipantNodeTest {

    @Test
    void shouldGenerateParticipantNodeFromSimpleJsonObject() {
        List<String> supportedProtocols = List.of("dataspace-protocol-http", "dataspace-protocol-https");
        Map<String, Object> claims = Map.of("membership", Map.of("level", "gold"));
        Map<String, String> attributes = Map.of("description", "testing purposes");
        String url = "http://localhost:19194/protocol";
        ParticipantNode node = new ParticipantNode("test provider", "test-provider", url, supportedProtocols, claims, attributes);
        JsonObject object = node.asJsonObject();

        ParticipantNode nodeFromJsonObject = ParticipantNode.fromJsonObject(object);

        assert node.name().equals(nodeFromJsonObject.name());
        assert node.id().equals(nodeFromJsonObject.id());
        assert node.targetUrl().equals(nodeFromJsonObject.targetUrl());
        assert node.supportedProtocols().equals(nodeFromJsonObject.supportedProtocols());
        assert node.claims().get("membership").equals(nodeFromJsonObject.claims().get("membership"));
        assert node.attributes().get("description").equals(nodeFromJsonObject.attributes().get("description"));
    }

    @Test
    void shouldGenerateParticipantNodeFromApiJsonObject() {
        InputStream inputStream = this.getClass().getResourceAsStream("/participant.json");
        JsonParser parser = Json.createParser(inputStream);
        parser.next();
        JsonObject object = parser.getObject();
        ParticipantNode node = ParticipantNode.fromJsonObject(object);
        assert node.name().equals("Test Provider");
        assert node.id().equals("provider-test");
        assert node.targetUrl().equals("http://localhost:19194/protocol");
        assert node.supportedProtocols().equals(List.of("dataspace-protocol-http", "dataspace-protocol-https"));
    }

    @Test
    void shouldGenerateParticipantNodeWithClaimsFromApiJsonObject() {
        InputStream inputStream = this.getClass().getResourceAsStream("/participant-with-claims.json");
        JsonParser parser = Json.createParser(inputStream);
        parser.next();
        JsonObject object = parser.getObject();

        ParticipantNode node = ParticipantNode.fromJsonObject(object);

        assert node.name().equals("Test Consumer");
        assert node.id().equals("consumer-test");
        assert (node.targetUrl() == null);
        assert node.supportedProtocols().isEmpty();

        Map<String, Object> claims = Map.of("membership", Map.of(EDC_NAMESPACE + "level", "gold", EDC_NAMESPACE + "branch", "operator"),
                                            "location", "eu");
        Map<String, String> attributes = Map.of("description", "testing purposes", "role", "testing");

        assert node.claims().get(EDC_NAMESPACE + "location").equals(claims.get("location"));
        assert node.claims().get(EDC_NAMESPACE + "membership").equals(claims.get("membership"));

        assert node.attributes().get(EDC_NAMESPACE + "description").equals(attributes.get("description"));
        assert node.attributes().get(EDC_NAMESPACE + "role").equals(attributes.get("role"));
    }

    @Test
    void shouldGenerateJsonObjectFromParticipantNode() {
        List<String> supportedProtocols = List.of("http", "https");
        Map<String, Object> claims = Map.of("membership", Map.of("level", "gold"));
        Map<String, String> attributes = Map.of("description", "testing purposes");
        String url = "http://localhost:19194/protocol";
        ParticipantNode node = new ParticipantNode("test participant", "test-provider", url, supportedProtocols, claims, attributes);

        JsonObject object = node.asJsonObject();

        String jsonStringTitle = object.get(EDC_NAMESPACE + "name").toString();
        assert jsonStringTitle.equals("\"" + node.name() + "\"");
        String jsonStringProviderId = object.get(EDC_NAMESPACE + "id").toString();
        assert jsonStringProviderId.equals("\"" + node.id() + "\"");
        String jsonStringUrl = object.get(EDC_NAMESPACE + "url").toString();
        assert jsonStringUrl.equals("\"" + node.targetUrl() + "\"");
        String jsonStringSupportedProtocols = object.get(EDC_NAMESPACE + "supportedProtocols").toString();
        assert jsonStringSupportedProtocols.equals("[\"http\",\"https\"]");
        String jsonStringClaims = object.get(EDC_NAMESPACE + "claims").toString();
        assert jsonStringClaims.equals("{\"membership\":{\"level\":\"gold\"}}");
        String jsonStringAttributes = object.get(EDC_NAMESPACE + "attributes").toString();
        assert jsonStringAttributes.equals("{\"description\":\"testing purposes\"}");
    }
}
