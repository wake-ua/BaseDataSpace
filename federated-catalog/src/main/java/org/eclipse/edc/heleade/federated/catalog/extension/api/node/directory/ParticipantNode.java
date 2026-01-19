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
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import org.eclipse.edc.crawler.spi.TargetNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a participant in a system, including details such as name, identifier, URL,
 * supported protocols, claims, and credentials.
 * This class is immutable and can function as a data transfer object for serialization and deserialization.
 */
public record ParticipantNode(@JsonProperty("name") String name,
                              @JsonProperty("id") String id,
                              @JsonProperty("url") String targetUrl,
                              @JsonProperty("supportedProtocols") List<String> supportedProtocols,
                              @JsonProperty("claims") Map<String, Object> claims,
                              @JsonProperty("attributes") Map<String, String> attributes,
                              @JsonProperty("security")  Map<String, String> security) {

    /**
     * Constructs a new instance of ParticipantNode.
     *
     * @param name the name of the participant
     * @param id the unique identifier of the participant
     * @param targetUrl the URL associated with the participant
     * @param supportedProtocols the list of supported protocols for the participant
     * @param claims the list of claims associated with the participant
     * @param attributes the list of credentials associated with the participant
     * @param security the list of private security info associated with the participant
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
        this(targetNode.name(), targetNode.id(), targetNode.targetUrl(), targetNode.supportedProtocols(), Map.of(), Map.of(), Map.of());
    }

    /**
     * Converts this ParticipantNode instance into a TargetNode instance.
     *
     * @return a new TargetNode containing the name, id, targetUrl, and supportedProtocols of this ParticipantNode
     */
    public TargetNode asTargetNode() {
        return new TargetNode(name, id, targetUrl, supportedProtocols);
    }

    /**
     * Converts a JsonObject into a ParticipantNode instance by extracting relevant fields.
     *
     * @param jsonObject the input JsonObject containing the participant data
     * @return a ParticipantNode instance populated with data from the JsonObject
     * @throws RuntimeException if an error occurs during the conversion process
     */
    public static ParticipantNode fromJsonObject(JsonObject jsonObject) {
        try {
            if (jsonObject.get(EDC_NAMESPACE + "name").getValueType().toString().equals("STRING")) {
                return fromSimpleJsonObject(jsonObject);
            } else {
                return fromArrayJsonObject(jsonObject);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error converting JsonObject to ParticipantNode", e);
        }
    }

    private static ParticipantNode fromSimpleJsonObject(JsonObject jsonObject) {
        // Extract values from the expanded JSON
        String name = jsonObject.getString(EDC_NAMESPACE + "name");
        String id = jsonObject.getString(EDC_NAMESPACE + "id");
        String url = jsonObject.getString(EDC_NAMESPACE + "url");
        List<String> protocols = getStringArrayValues(jsonObject, EDC_NAMESPACE + "supportedProtocols");
        Map<String, Object> claims = getMapFromJsonObject(jsonObject, EDC_NAMESPACE + "claims");
        Map<String, String> attributes = getStringMapFromJsonObject(jsonObject, EDC_NAMESPACE + "attributes");
        Map<String, String> security = getStringMapFromJsonObject(jsonObject, EDC_NAMESPACE + "security");

        // Create and return a new ParticipantNode
        return new ParticipantNode(name, id, url, protocols, claims, attributes, security);
    }

    private static ParticipantNode fromArrayJsonObject(JsonObject jsonObject) {
        // printJsonObject(jsonObject);

        // Extract values from the expanded JSON
        String name = getStringValue(jsonObject, EDC_NAMESPACE + "name");
        String id = getStringValue(jsonObject, EDC_NAMESPACE + "id");
        String url = getStringValue(jsonObject, EDC_NAMESPACE + "url");
        List<String> protocols = getStringArrayValues(jsonObject, EDC_NAMESPACE + "supportedProtocols");
        Map<String, Object> claims = getMapFromArrayJsonObject(jsonObject, EDC_NAMESPACE + "claims");
        Map<String, String> attributes = getStringMapFromArrayJsonObject(jsonObject, EDC_NAMESPACE + "attributes");
        Map<String, String> security = getStringMapFromArrayJsonObject(jsonObject, EDC_NAMESPACE + "security");

        // Create and return a new ParticipantNode
        return new ParticipantNode(name, id, url, protocols, claims, attributes, security);
    }

    /**
     * Converts the current instance into a JsonObject representation containing its properties.
     *
     * @return a JsonObject containing the name, id, targetUrl, supportedProtocols, claims, and attributes of the instance
     */
    public JsonObject asJsonObject() {
        return asJsonObject(false);
    }

    /**
     * Converts the current instance into a JsonObject representation containing its properties.
     *
     * @return a JsonObject containing the name, id, targetUrl, supportedProtocols, claims, and attributes of the instance
     */
    public JsonObject asPublicJsonObject() {
        return asJsonObject(true);
    }

    private JsonObject asJsonObject(boolean asPublic) {
        try {
            // Create a JSON object with the TargetNode properties
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add(EDC_NAMESPACE + "name", this.name())
                    .add(EDC_NAMESPACE + "id", this.id());

            // Add the url if not null
            if (this.targetUrl() != null) {
                builder.add(EDC_NAMESPACE + "url", this.targetUrl());
            }

            // Add the supported protocols as an array
            if (this.supportedProtocols() != null) {
                JsonArrayBuilder protocolsArray = Json.createArrayBuilder();
                for (String protocol : this.supportedProtocols()) {
                    protocolsArray.add(protocol);
                }
                builder.add(EDC_NAMESPACE + "supportedProtocols", protocolsArray);
            }
            // Add the claims as an array
            if (this.claims() != null) {
                JsonObjectBuilder claimsObject = fromHashMap(claims);
                builder.add(EDC_NAMESPACE + "claims", claimsObject);
            }

            // Add the attributes as an array
            if (this.attributes() != null) {
                JsonObjectBuilder attributesObject = Json.createObjectBuilder();
                for (Map.Entry<String, String> attribute : this.attributes().entrySet()) {
                    attributesObject.add(attribute.getKey(), Json.createValue(attribute.getValue()));
                }
                builder.add(EDC_NAMESPACE + "attributes", attributesObject);
            }

            if (!asPublic) {
                // Add the security as an array
                if (this.security() != null) {
                    JsonObjectBuilder securityObject = Json.createObjectBuilder();
                    for (Map.Entry<String, String> item : this.security().entrySet()) {
                        securityObject.add(item.getKey(), Json.createValue(item.getValue()));
                    }
                    builder.add(EDC_NAMESPACE + "security", securityObject);
                }
            }

            // Build and return the JSON object
            JsonObject jsonObject = builder.build();

            return jsonObject;

        } catch (Exception e) {
            throw new RuntimeException("Error converting TargetNode to JsonObject", e);
        }
    }

    /**
     * Converts a given map into a JsonObjectBuilder by traversing its entries and handling nested structures.
     *
     * @param map the map to be converted; keys must be strings, and values can be strings, maps, or lists
     * @return a JsonObjectBuilder representing the equivalent JSON structure of the input map
     * @throws RuntimeException if an unsupported data type is encountered within a list
     */
    public static JsonObjectBuilder fromHashMap(Map<String, Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                builder.add(entry.getKey(), Json.createValue(entry.getValue().toString()));
            } else if (entry.getValue() instanceof Map) {
                builder.add(entry.getKey(), fromHashMap((Map<String, Object>) entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Object item : (List) entry.getValue()) {
                    if (item instanceof String) {
                        arrayBuilder.add(Json.createValue(item.toString()));
                    } else if (item instanceof Map) {
                        arrayBuilder.add(fromHashMap((Map<String, Object>) item));
                    } else {
                        throw new RuntimeException("Unsupported type for list item: " + item.getClass().getName());
                    }
                }
            }
        }
        return builder;
    }

    /**
     * Extracts a list of string values from a JSON array associated with a specific key in the given JSON object.
     * The method handles arrays of simple strings as well as objects containing "@value" or "@list" fields.
     *
     * @param jsonObject the JSON object containing the target array
     * @param key the key whose associated array of values is to be extracted
     * @return a list of string values extracted from the JSON array; an empty list if the key does not exist,
     *         the array is empty, or no valid string values are found
     */
    public static  List<String> getStringArrayValues(JsonObject jsonObject, String key) {
        List<String> results = new ArrayList<>();

        if (jsonObject.containsKey(key)) {
            var valueArray = jsonObject.getJsonArray(key);
            if (valueArray != null) {
                for (var i = 0; i < valueArray.size(); i++) {
                    var item = valueArray.get(i);
                    if (item instanceof JsonObject itemObj) {
                        // Handle case where array items are objects with @value
                        if (itemObj.containsKey("@value")) {
                            results.add(itemObj.getString("@value"));
                        } else if (itemObj.containsKey("@list")) {
                            // Handle case where the array is represented as @list
                            var listArray = itemObj.getJsonArray("@list");
                            for (var j = 0; j < listArray.size(); j++) {
                                var listItem = listArray.get(j);
                                if (listItem instanceof JsonObject listItemObj && listItemObj.containsKey("@value")) {
                                    results.add(listItemObj.getString("@value"));
                                }
                            }
                        }
                    } else if (item.getValueType().toString().equals("STRING")) {
                        // Simple string value
                        results.add(valueArray.getString(i));
                    }
                }
            }
        }
        return results;
    }

    /**
     * Extracts a map of string keys and associated values from a nested JSON object
     * based on a specified key. If the key exists and its value is a JSON object,
     * the method recursively processes the nested structure.
     *
     * @param jsonObject the JSON object containing the target key and value
     * @param key the key whose associated JSON object needs to be processed
     * @return a map where the top-level key is included with its nested structure or
     *         an empty map if the key does not exist
     */
    public static  Map<String, Object> getMapFromJsonObject(JsonObject jsonObject, String key) {
        if (jsonObject.containsKey(key)) {
            return getMapFromJsonObject(jsonObject.getJsonObject(key));
        } else {
            return Map.of();
        }
    }

    /**
     * Extracts a map from the first JSON object in an array associated with a specified key
     * in the given JSON object. If the key exists and its value is a non-empty JSON array,
     * the method processes the first element of that array recursively.
     *
     * @param jsonObject the JSON object containing the key and associated array
     * @param key the key whose value is expected to be a JSON array
     * @return a map where the keys and values are extracted from the first JSON object
     *         in the array, or an empty map if the key does not exist or the array is empty
     */
    public static  Map<String, Object> getMapFromArrayJsonObject(JsonObject jsonObject, String key) {
        if (jsonObject.containsKey(key)) {
            var valueArray = jsonObject.getJsonArray(key);
            if (valueArray != null && !valueArray.isEmpty()) {
                JsonObject firstValue = valueArray.getJsonObject(0);
                return getMapFromArrayJsonObject(firstValue);
            }
        }
        return Map.of();
    }

    /**
     * Extracts a map of string keys and associated JSON object values from a specified
     * JSON object's array values. Each array element is expected to be a JSON object
     * containing an "@id" key.
     *
     * @param jsonObject the JSON object containing the target array
     * @return a map where the keys are the "@id" values and the values are the
     *         JSON objects from the array's elements; an empty map if the key
     *         does not exist or the array is empty
     */
    public static  Map<String, Object> getMapFromJsonObject(JsonObject jsonObject) {
        Map<String, Object> results = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            JsonValue value = jsonObject.get(key);
            if (value != null) {
                if (value.getValueType().toString().equals("STRING")) {
                    results.put(key, jsonObject.getString(key));
                } else {
                    if (value.getValueType().toString().equals("OBJECT")) {
                        JsonObject object = (JsonObject) value;
                        results.put(key, getMapFromJsonObject(object));
                    }
                }
            }
        }
        return results;
    }

    /**
     * Extracts a map of string keys and their corresponding values from a JSON object.
     * If a value is a JSON array containing objects, it recursively processes the first object.
     *
     * @param jsonObject the JSON object containing the key-value pairs to be processed
     * @return a map where keys are the string keys from the JSON object and values are either
     *         simple strings or nested structures processed recursively
     */
    public static  Map<String, Object> getMapFromArrayJsonObject(JsonObject jsonObject) {
        Map<String, Object> results = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            JsonValue value = jsonObject.get(key);
            String valueType = value.getValueType().toString();
            if (value.getValueType().toString().equals("ARRAY")) {
                var valueArray = jsonObject.getJsonArray(key);
                if (!valueArray.isEmpty()) {
                    var firstObject = valueArray.getJsonObject(0);
                    if (valueArray.size() == 1 && firstObject.containsKey("@value")) {
                        String firstValue = firstObject.getString("@value");
                        results.put(key, firstValue);
                    } else {
                        for (int i = 0; i < valueArray.size(); i++) {
                            JsonObject object = valueArray.getJsonObject(i);
                            results.put(key, getMapFromArrayJsonObject(object));
                        }
                    }
                }
            } else if (valueType.equals("OBJECT")) {
                var valueObject = jsonObject.getJsonObject(key);
                results.put(key, getMapFromArrayJsonObject(valueObject));
            } else if (valueType.equals("STRING")) {
                results.put(key, jsonObject.getString(key));
            } else {
                throw new RuntimeException("Unsupported data type for key: " + key);
            }
        }
        return results;
    }

    /**
     * Extracts a map of string key-value pairs from a specified JSON object's array values.
     * Each array element is expected to be a JSON object containing "@id" and "@value" keys.
     *
     * @param jsonObject the JSON object containing the target array
     * @param key the key in the JSON object whose value is an array of JSON objects
     * @return a map where keys are the "@id" values and values are the "@value" values from the array's JSON objects
     */
    public static Map<String, String> getStringMapFromJsonObject(JsonObject jsonObject, String key) {
        Map<String, String> results = new HashMap<>();
        if (jsonObject.containsKey(key)) {
            JsonObject object = jsonObject.getJsonObject(key);
            if (object != null) {
                for (String id : object.keySet()) {
                    results.put(id, object.getString(id));
                }
            }
        }
        return results;
    }

    /**
     * Extracts a map of string key-value pairs from a JSON object's array values.
     * Each array element is expected to be a JSON object with nested key-value pairs.
     *
     * @param jsonObject the JSON object containing the target array
     * @param key the key in the JSON object whose value is an array of JSON objects
     * @return a map where keys are the nested keys in the array's JSON objects, and values are their corresponding extracted string values
     */
    public static  Map<String, String> getStringMapFromArrayJsonObject(JsonObject jsonObject, String key) {
        Map<String, String> results = new HashMap<>();
        if (jsonObject.containsKey(key)) {
            JsonArray objectArray = jsonObject.getJsonArray(key);
            if (objectArray != null && !objectArray.isEmpty()) {
                for (int i = 0; i < objectArray.size(); i++) {
                    JsonObject object = objectArray.getJsonObject(i);
                    if (object != null) {
                        for (String id : object.keySet()) {
                            results.put(id, getStringValue(object, id));
                        }
                    }
                }
            }

        }
        return results;
    }

    /**
     * Helper method to extract a string value from a JSON object.
     *
     * @param jsonObject the JSON object containing the target array
     * @param key the key in the JSON object whose value is an array of JSON objects
     * @return the extracted string values
     */
    public static String getStringValue(JsonObject jsonObject, String key) {
        if (jsonObject.containsKey(key)) {
            var valueArray = jsonObject.getJsonArray(key);
            if (valueArray != null && !valueArray.isEmpty()) {
                var firstValue = valueArray.get(0);
                if (firstValue instanceof JsonObject valueObj && valueObj.containsKey("@value")) {
                    return valueObj.getString("@value");
                }
            }
        }
        return null;
    }

    private static void printJsonObject(JsonObject object) {
        JsonWriter jsonWriter = Json.createWriter(System.out);
        jsonWriter.writeObject(object);
        jsonWriter.close();
        System.out.println(jsonWriter.toString());
    }
}