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
 *       MO - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.claims.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.File;
import java.util.Map;


/**
 * Implementation of {@link ParticipantClaimsLoader} that loads participant claims
 * from a local JSON file.
 * <p>
 * The file is expected to contain a JSON object representing participant claims.
 * If the file cannot be read or parsed, an empty map is returned and a warning
 * is logged using the provided {@link Monitor}.
 * </p>
 */
public class FileParticipantClaimsLoader implements ParticipantClaimsLoader {


    private final ObjectMapper mapper = new ObjectMapper();
    private final  Monitor  monitor;

    public FileParticipantClaimsLoader(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Loads participant claims from the specified JSON file path.
     *
     * @param path the file system path to the JSON claims file
     * @return a {@link Map} containing the participant claims, or an empty map if
     *         the file could not be read or parsed
     */
    @Override
    public Map<String, Object> loadClaims(String path) {
        try {
            return mapper.readValue(new File(path), Map.class);
        } catch (Exception e) {
            monitor.warning("Error reading claims file: ");
            return Map.of();
        }
    }



}
