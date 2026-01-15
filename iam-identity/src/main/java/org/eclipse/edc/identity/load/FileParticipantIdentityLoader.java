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

package org.eclipse.edc.identity.load;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Implementation of the {@link ParticipantIdentityLoader} interface that provides
 * mechanisms for loading claims and private keys from files, as well as signing
 * claims using a private key.
 * This implementation primarily works with JSON-encoded claims files and PEM-encoded
 * private keys in PKCS #8 format. It also provides logging for diagnostics and error handling.
 */
public class FileParticipantIdentityLoader implements ParticipantIdentityLoader {
    /**
     * An instance of {@link ObjectMapper} used for serializing and deserializing JSON data.
     * It is a core dependency for converting objects to JSON and vice versa during
     * various operations like loading claims, signing claims, or processing file contents
     * related to participant identity.
     */
    private final ObjectMapper mapper;
    /**
     * Provides a logging mechanism for the {@link FileParticipantIdentityLoader} class.
     * This monitor is used to log warnings, errors, and other relevant information
     * during operations such as file loading, private key parsing, and claims signing.
     * It ensures proper observability and aids in debugging any issues that arise
     * in the identity loader process.
     */
    private final Monitor monitor;


    /**
     * Constructs a new instance of FileParticipantIdentityLoader.
     *
     * @param monitor the monitor instance used for logging and diagnostics
     * @param mapper  the object mapper used for JSON serialization and deserialization
     */
    public FileParticipantIdentityLoader(Monitor monitor, ObjectMapper mapper) {
        this.monitor = monitor;
        this.mapper = mapper;
    }

    /**
     * Loads a set of claims from a file located at the specified path.
     * The file is expected to contain a JSON representation of the claims.
     * If an error occurs while reading or parsing the file, a warning is logged,
     * and an empty map is returned.
     *
     * @param path the file system path to the claims file
     * @return a map representing the loaded claims, or an empty map if an error occurs
     */
    @Override
    public Map<String, Object> loadClaims(String path) {
        var file = new File(path);
        try {
            monitor.debug("Loading claims from: " + file.getAbsolutePath());
            return mapper.readValue(file, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            monitor.severe("Error reading claims from file: " + file.getAbsolutePath(), e);
            return Map.of();
        }
    }

    /**
     * Loads a private key from a PEM-encoded file located at the specified path.
     * The key is expected to be in PKCS #8 format and encoded for the Ed25519 algorithm.
     * If an error occurs (e.g., the file is not found, the content is invalid, or an unsupported
     * algorithm is specified), the method logs the error and returns {@code null}.
     *
     * @param path the file system path to the PEM-encoded private key
     * @return the {@link PrivateKey} instance if the key is successfully loaded, or {@code null} if an error occurs
     */
    @Override
    public PrivateKey loadPrivateKey(String path) {
        Path filePath = Paths.get(path);
        try {

            String content = new String(Files.readAllBytes(filePath));
            String pem = content
                    .replaceAll("-+[A-Z ]+-+", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            monitor.severe(String.format("Error loading private key from %s", filePath));
            throw new RuntimeException(String.format("Error loading private key from %s", filePath));
        }
    }

    /**
     * Signs the provided claims using the provided private key and returns the signed claims as a Base64-encoded string.
     * If an error occurs during the signing process, a warning is logged, and the method returns {@code null}.
     *
     * @param claims     the claims to be signed, represented as a map of key-value pairs
     * @param privateKey the private key used to sign the claims
     * @param monitor    the monitor used for logging warnings or errors
     * @return the signed claims as a Base64-encoded string, or {@code null} if an error occurs during signing
     */
    public String signClaims(Map<String, Object> claims, PrivateKey privateKey, Monitor monitor) {
        try {
            String claimsString = mapper.writeValueAsString(claims);

            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey);
            signature.update(claimsString.getBytes(StandardCharsets.UTF_8));

            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            monitor.severe("Claims could not be signed: " + e.getMessage(), e);
            throw new RuntimeException("Claims could not be signed: " + e.getMessage(), e);
        }
    }


    @Override
    public PublicKey loadPublicKey(String path) {
        Path filePath = Paths.get(path);
        try {
            String content = new String(Files.readAllBytes(filePath));
            String pem = content
                    .replaceAll("-+[A-Z ]+-+", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            return keyFactory.generatePublic(spec);

        } catch (Exception e) {
            monitor.severe(String.format("Error loading public key from %s", filePath));
            throw new RuntimeException(String.format("Error loading public key from %s", filePath), e);
        }
    }

    public boolean publicKeyMatchesPrivateKey(PublicKey publicKey, PrivateKey privateKey) {
        try {
            byte[] message = "key-validation".getBytes(StandardCharsets.UTF_8);

            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(privateKey);
            signer.update(message);
            byte[] signature = signer.sign();

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(message);

            if (!verifier.verify(signature)) {
                throw new RuntimeException("Public key does not match the provided private key");
            }

            return true;

        } catch (Exception e) {
            monitor.severe("Error validating public/private key pair");
            throw new RuntimeException("Error validating public/private key pair", e);
        }
    }


    public boolean checkConfigurations(String claimsPath, String participantRegistryUrl, String privateKeyPath, String publicKeyPath) {

        boolean configurationOk = true;

        if (claimsPath == null) {
            monitor.warning("edc.participant.claims is null");
            configurationOk = false;
        }

        if (privateKeyPath == null) {
            monitor.warning("edc.participant.private.key is null");
            configurationOk = false;
        }

        if (publicKeyPath == null) {
            monitor.warning("edc.participant.public.key is null");
            configurationOk = false;
        }

        if (participantRegistryUrl == null) {
            monitor.warning("edc.participant.registry.url is null");
            configurationOk = false;
        }

        return configurationOk;
    }


}
