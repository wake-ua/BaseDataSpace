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

package org.eclipse.edc.identity.extension;

import org.eclipse.edc.spi.monitor.Monitor;

import java.security.PrivateKey;
import java.util.Map;

/**
 * The ParticipantIdentityLoader interface defines operations for loading and processing
 * participant identity data, such as claims and private keys, and signing claims using
 * a private key. Implementations of this interface enable the management and usage
 * of identity-related artifacts for participants in a system.
 */
public interface ParticipantIdentityLoader {

    /**
     * Loads claims from a specified file path and returns them as a key-value map.
     *
     * @param path the file path from which the claims are to be loaded
     * @return a map containing the claims as key-value pairs
     */
    Map<String, Object> loadClaims(String path);

    /**
     * Loads a private key from the specified file path.
     *
     * @param path the file path to the private key file
     * @return the loaded private key
     */
    PrivateKey loadPrivateKey(String path);

    /**
     * Signs the provided claims using the given private key and generates a signed representation.
     *
     * @param claims the claims to be signed, provided as a key-value map
     * @param privateKey the private key used to sign the claims, can be null
     * @param monitor the monitor used for logging and diagnostics
     * @return a string representation of the signed claims
     */
    String signClaims(Map<String, Object> claims, PrivateKey privateKey, Monitor monitor);

}
