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

package org.eclipse.edc.heleade.policy.extension.claims.checker;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


/**
 * Defines an extension that is used to check if the claims provided by the
 * participant in a request match those in the participant registry list.
 */
public class ClaimCheckerExtension implements ServiceExtension {

    /**
     * Initializes the participant claim checker as a service.
     *
     * @param context the service extension context used to access configuration and monitoring
     * @return an initialized {@link FcParticipantClaimChecker} instance
     */
    @Provider
    public FcParticipantClaimChecker initializeService(ServiceExtensionContext context) {
        var participantRegistryUrl = context.getConfig().getString("edc.participant.registry.url");
        var monitor = context.getMonitor();
        return new FcParticipantClaimChecker(monitor, participantRegistryUrl);
    }

}
