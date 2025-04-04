/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    id("java")
}

tasks.withType<Jar> {
    from(sourceSets["main"].output)
    manifest {
        attributes["Main-Class"] = "org.eclipse.edc.heleade.util.HttpRequestLoggerServer"
    }
    archiveFileName.set("http-request-logger.jar")
}
