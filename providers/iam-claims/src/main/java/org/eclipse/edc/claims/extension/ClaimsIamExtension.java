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



@Provides(IdentityService.class)
@Extension(value = ClaimsIamExtension.NAME)
public class ClaimsIamExtension implements ServiceExtension {

    public static final String NAME = "Credentials IAM";

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        monitor.info("Initializing claims IAM Extension");

        var claimsPath = context.getConfig().getString("edc.participant.claims", "deployment/assets/consumer/creds.json");
        var participantId = context.getParticipantId();

        ParticipantClaimsLoader loader = new FileParticipantClaimsLoader(monitor);
        var claims = loader.loadClaims(claimsPath);

        context.registerService(
                IdentityService.class,
                new ClaimsIdentityService(typeManager, monitor, claims, participantId));
    }

    @Provider
    public AudienceResolver audienceResolver() {
        return (msg) -> Result.success(msg.getCounterPartyAddress());
    }
}
