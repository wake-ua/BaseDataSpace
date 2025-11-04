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

import java.util.Map;

/**
 * Defines the contract for loading participant claims from a given source.
 */
public interface ParticipantClaimsLoader {

    /**
     * Loads participant claims from the specified path.
     *
     * @param path the file system path or resource location from which to load the claims
     * @return a map containing the participant's claims; never {@code null}, but may be empty
     */
    Map<String, Object> loadClaims(String path);

}

