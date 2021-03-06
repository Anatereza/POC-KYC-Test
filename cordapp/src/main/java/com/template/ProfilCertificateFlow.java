package com.template;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.*;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.lang.reflect.Field;
import java.security.PublicKey;
import java.security.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.template.TemplateContract.TEMPLATE_CONTRACT_ID;
/**
 * Define your flow here.
 */
@InitiatingFlow
@StartableByRPC
public class ProfilCertificateFlow extends FlowLogic<SignedTransaction> {
    private final String cert;
    private final String profil;



    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();


    public ProfilCertificateFlow(String cert, String profil) {

        this.cert = cert;
        this.profil = profil;

    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // We retrieve the notary and nodes identity from the network map.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        CordaX500Name OtherX1 = CordaX500Name.parse("O=Caisse Epargne,L=Paris,C=FR");
        CordaX500Name OtherX2 = CordaX500Name.parse("O=Natixis Assurance,L=Paris,C=FR");
        CordaX500Name OtherX3 = CordaX500Name.parse("O=BPCE Assurance,L=Paris,C=FR");

        Party other1 = getServiceHub().getNetworkMapCache().getPeerByLegalName(OtherX1);
        Party other2 = getServiceHub().getNetworkMapCache().getPeerByLegalName(OtherX2);
        Party other3 = getServiceHub().getNetworkMapCache().getPeerByLegalName(OtherX3);


        // We create the transaction components.
        //final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        //String now = sdf.format(new Date());


        //récupérer le state à mettre à jour
        QueryCriteria.VaultQueryCriteria generalcriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        Field cert1 = null;
        try {
            cert1 = CertificateSchemaV1.PersistentCertificate.class.getDeclaredField("Cert");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }


        CriteriaExpression certIndex = Builder.equal(cert1, cert);
        QueryCriteria certCriteria = new QueryCriteria.VaultCustomQueryCriteria(certIndex);

        QueryCriteria criteria = generalcriteria.and(certCriteria);


        // *****
        Vault.Page<CertificateState> result = getServiceHub().getVaultService().queryBy(CertificateState.class, criteria);
        StateAndRef<CertificateState> inputState = result.getStates().get(0);

        StateRef ourStateRef = new StateRef(inputState.getRef().getTxhash(),0);
        StateAndRef ourStateAndRef = getServiceHub().toStateAndRef(ourStateRef);

        // test  inputs
        String client = inputState.getState().getData().getClient();
        String docKYC = inputState.getState().getData().getDocKYC();
        Integer maintien = inputState.getState().getData().getMaintien();
        Integer status = inputState.getState().getData().getStatus();
        List<String> documents = inputState.getState().getData().getDocuments();
        String description = inputState.getState().getData().getDescription();
        String dateC = inputState.getState().getData().getDateCreation();
        String dateProchaineCert = inputState.getState().getData().getDateProchaineCert();

        CertificateState certificateOutputState = new CertificateState(cert, client, docKYC, status, maintien, getOurIdentity(), profil, documents, description, dateC, dateProchaineCert, other2, other3);


        if(getOurIdentity().equals(other2)){
            certificateOutputState.setOther1(other1);
            certificateOutputState.setOther2(other3);
        }

        else if(getOurIdentity().equals(other3)){
            certificateOutputState.setOther1(other1);
            certificateOutputState.setOther2(other2);
        }

        // We create a transaction builder and add the components.
        final TransactionBuilder txBuilder = new TransactionBuilder(notary);

        txBuilder.addInputState(ourStateAndRef);
        txBuilder.addOutputState(certificateOutputState, CertificateContract.CERTIFICATE_CONTRACT_ID);

        // We add the InitiateSell command to the transaction.
        // Note that we also specific who is required to sign the transaction.
        CertificateContract.Commands.Update commandData = new CertificateContract.Commands.Update();
        List<PublicKey> requiredSigners = ImmutableList.of(certificateOutputState.getInitiator().getOwningKey());
        txBuilder.addCommand(commandData, requiredSigners);

        // STEP.4.5. We check that the transaction builder we've created meets the
        // contracts of the input and output states.
        txBuilder.verify(getServiceHub());


        // Signing the transaction.
        final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Finalising the transaction.
        subFlow(new FinalityFlow(signedTx));


        return null;
    }
}