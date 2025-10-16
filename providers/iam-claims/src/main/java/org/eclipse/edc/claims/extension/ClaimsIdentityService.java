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

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Map;


public class ClaimsIdentityService implements IdentityService {

    private final TypeManager typeManager;
    private final String clientId;
    private final Monitor monitor;
    Map<String, Object> claims;

    public ClaimsIdentityService(TypeManager typeManager, Monitor monitor,
                                 Map<String, Object> claims,
                                 String clientId) {

        this.typeManager = typeManager;
        this.clientId = clientId;
        this.monitor = monitor;
        this.claims = claims;
    }


    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {

        var token = new Token();
        token.setAudience(parameters.getStringClaim("aud"));
        token.setClientId(clientId);
        token.setClaims(claims);

        TokenRepresentation tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();

        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation,
                                             VerificationContext context) {
        var token = typeManager.readValue(tokenRepresentation.getToken(), Token.class);

        return Result.success(
                ClaimToken.Builder.newInstance()
                        .claim("client_id", token.clientId)
                        .claim("claims", token.getClaims())
                        .build()
        );
    }

    private static class Token {
        private String audience;
        private Map<String, Object> claims;
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public Map<String, Object> getClaims() {
            return claims;
        }

        public void setClaims(Map<String, Object> claims) {
            this.claims = claims;
        }
    }
}
