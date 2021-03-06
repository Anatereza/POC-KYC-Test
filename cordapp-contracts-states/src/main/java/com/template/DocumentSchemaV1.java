package com.template;

import com.google.common.collect.ImmutableList;
import com.sun.org.apache.xpath.internal.operations.Bool;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * A DocumentState schema.
 */
public class DocumentSchemaV1 extends MappedSchema {
    public DocumentSchemaV1() {
        super(DocumentSchemaV1.class, 1, ImmutableList.of(PersistentDocument.class));
    }

    @Entity
    @Table(name = "document_states")
    public static class PersistentDocument extends PersistentState {
        @Column(name = "Document")private final String Document;
        @Column(name = "Client") private final String Client;
        @Column(name = "NomDoc") private final String NomDoc;
        @Column(name = "Status") private final int Status;
        @Column(name = "DateA") private final String DateA;


        public PersistentDocument(String document, String client, String nomdoc, int status, String dateA) {
            this.Document = document;
            this.Client = client;
            this.NomDoc = nomdoc;
            this.Status = status;
            this.DateA = dateA;

        }

        // Default constructor required by hibernate.
        public PersistentDocument() {
            this.Document = null;
            this.Client = null;
            this.NomDoc = null;
            this.Status = 0;
            this.DateA = null;

        }

        public String getDocument() {
            return Document;
        }

        public String getClient() {
            return Client;
        }

        public String getNomDoc() {
            return NomDoc;
        }

        public int getStatus() {
            return Status;
        }

        public String getDateA() {
            return DateA;
        }


    }
}
