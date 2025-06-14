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

package org.eclipse.edc.heleade.control.plane.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;

import java.util.ArrayList;
import java.util.List;

public class DataServiceRegistryImpl implements DataServiceRegistry {

    private final List<DataService> dataServices = new ArrayList<>();

    @Override
    public void register(DataService dataService) {
        dataServices.add(dataService);
    }

    @Override
    public List<DataService> getDataServices() {
        return dataServices;
    }

}
