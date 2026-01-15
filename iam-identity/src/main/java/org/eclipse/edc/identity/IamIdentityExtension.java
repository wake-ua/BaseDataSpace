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

package org.eclipse.edc.identity;

import org.eclipse.edc.identity.api.IamIdentityApiController;
import org.eclipse.edc.identity.load.FileParticipantIdentityLoader;
import org.eclipse.edc.identity.load.ParticipantIdentityLoader;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

/**
 * The {@code IamIdentityExtension} class is a service extension that integrates an IAM-based
 * identity mechanism into the application. It provides functionality for initializing participant
 * identity data, signing claims, and setting up the {@link IdentityService}.
 *
 * This extension reads configuration properties for loading claims and private keys, processes them
 * using a {@link ParticipantIdentityLoader}, and registers a configured {@link IdentityService}
 * instance into the service context.
 *
 * The extension also defines and provides the default {@link AudienceResolver} implementation
 * for determining audiences for message exchanges.
 *
 * Configuration properties:
 * - {@code edc.participant.claims}: The path to the file containing the participant claims (default: "creds.json").
 * - {@code edc.participant.private.key}: The path to the private key file used for signing claims (default: "ed25519_private.pem").
 */
@Provides(IdentityService.class)
@Extension(value = IamIdentityExtension.NAME)
public class IamIdentityExtension implements ServiceExtension {

    /**
     * Represents the name of the IAM Identity Extension.
     * This constant is used as an identifier for the extension, which integrates an IAM-based
     * identity mechanism into the application.
     */
    public static final String NAME = "Iam Identity";

    @Inject
    private TypeManager typeManager;


    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        monitor.info("Initializing iam-identity extension");


        var claimsPath = context.getConfig().getString("edc.participant.claims", null);
        var participantPrivateKeyPath = context.getConfig().getString("edc.participant.private.key", null);
        var participantPublicKeyPath = context.getConfig().getString("edc.participant.public.key", null);
        var participantRegistryUrl = context.getConfig().getString("edc.participant.registry.url", null);
        var participantId = context.getParticipantId();


        ParticipantIdentityLoader loader = new FileParticipantIdentityLoader(monitor, typeManager.getMapper());
        boolean checkConfiguration = loader.checkConfigurations(claimsPath, participantRegistryUrl, participantPrivateKeyPath, participantPublicKeyPath);

        Map<String, Object> claims = Map.of();
        String signedClaims = null;
        if (checkConfiguration) {
            claims = loader.loadClaims(claimsPath);
            PrivateKey participantPrivateKey = loader.loadPrivateKey(participantPrivateKeyPath);
            PublicKey participantPublicKey = loader.loadPublicKey(participantPublicKeyPath);
            String base64PublicKey = Base64.getEncoder().encodeToString(participantPublicKey.getEncoded());
            signedClaims = loader.signClaims(claims, participantPrivateKey, monitor);
            boolean publicKeyMatchesPrivateKey = loader.publicKeyMatchesPrivateKey(participantPublicKey, participantPrivateKey);

            if (publicKeyMatchesPrivateKey) {
                monitor.info("Private && Public keys has been successfully validated!");
                monitor.info("Claims has been successfully signed:  " + signedClaims);
                monitor.info("Claims: " + claims);
                monitor.info("Public key: " + base64PublicKey);
                monitor.info("Participant registry url: " + participantRegistryUrl);
            }
        }


        webService.registerResource(new IamIdentityApiController(context.getMonitor()));

        context.registerService(
                IdentityService.class,
                new IamIdentityService(typeManager, claims, participantId, signedClaims));
    }

    /**
     * Provides the default {@link AudienceResolver} implementation.
     *
     * @return a resolver that uses the counterparty address as the audience
     */
    @Provider
    public AudienceResolver audienceResolver() {
        return (msg) -> Result.success(msg.getCounterPartyAddress());
    }

    @Inject
    WebService webService;


}
