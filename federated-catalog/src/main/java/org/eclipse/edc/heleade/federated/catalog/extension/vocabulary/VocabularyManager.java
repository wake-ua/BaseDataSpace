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
 *       LdE - Universidad de Alicante - initial implementation
 *
 */

package org.eclipse.edc.heleade.federated.catalog.extension.vocabulary;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

import java.io.File;

import static org.semanticweb.owlapi.search.EntitySearcher.getAnnotationObjects;

public class VocabularyManager {

    public static OWLOntologyManager create() {
        OWLOntologyManager m =
                OWLManager.createOWLOntologyManager();
        m.getIRIMappers().add(new AutoIRIMapper(
                new File("materializedOntologies"), true));
        return m;
    }

    public static String loadOntology(String ontologyIri) {
        
        StringBuilder output = new StringBuilder();
        
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLOntologyManager m = create();

        try {
            IRI iri;

            if (isPath(ontologyIri)) {
                File file = new File(ontologyIri);
                iri = IRI.create(file);
            } else {
                iri = IRI.create(ontologyIri);
            }

            OWLOntology o = m.loadOntologyFromOntologyDocument(iri);

            output.append(" ONTOLOGY: ").append(o.getOntologyID().getOntologyIRI().toString()).append("\n");
            if (o.getOntologyID().getVersionIRI().isPresent()) {
                output.append(" VERSION: ").append(o.getOntologyID().getVersionIRI().get()).append("\n");
            }

            output.append("\n getAnnotations");
            for (var a : o.getAnnotations()) {
                output.append(a.getProperty().getIRI().toString()).append(" ").append(a.getValue().toString()).append("\n");
            }

            output.append("\n getClassesInSignature");

            for (OWLClass cls : o.getClassesInSignature()) {
                output.append(cls.getIRI().toString()).append(" ").append(cls.getIRI().getFragment()).append("\n");
                var annotations = getAnnotationObjects(cls, o, df.getRDFSLabel());
                annotations.forEach(a -> output.append("\t - ").append(a.getValue().asLiteral().get().getLang()).append(": ").append(a.getValue().asLiteral().get().getLiteral()).append("\n"));
                var comments = getAnnotationObjects(cls, o, df.getRDFSComment());
                comments.forEach(a -> output.append("\t * ").append(a.getValue().asLiteral().get().getLang()).append(": ").append(a.getValue().asLiteral().get().getLiteral()).append("\n"));
            }
            output.append("\n getDataPropertiesInSignature");
            o.getDataPropertiesInSignature().forEach(p -> output.append(p).append("\n"));
            output.append("\n getObjectPropertiesInSignature");
            o.getObjectPropertiesInSignature().forEach(p -> output.append(p).append("\n"));
            output.append("\n getIndividualsInSignature");
            o.getIndividualsInSignature().forEach(p -> output.append(p).append("\n"));

        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        return output.toString();
    }

    private static boolean isPath(String ontologyIri) {
        return !ontologyIri.startsWith("http://") && !ontologyIri.startsWith("https://");
    }
}
