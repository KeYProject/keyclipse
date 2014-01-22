/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uka.ilkd.key.gui.macros;

import de.uka.ilkd.key.gui.ExceptionDialog;
import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.ProverTaskListener;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.ContractPO;
import de.uka.ilkd.key.proof.init.InfFlowContractPO;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.ProblemInitializer;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.init.ProofOblInput;
import de.uka.ilkd.key.proof.init.SymbolicExecutionPO;
import de.uka.ilkd.key.proof.init.po.snippet.InfFlowPOSnippetFactory;
import de.uka.ilkd.key.proof.init.po.snippet.POSnippetFactory;
import javax.swing.KeyStroke;


/**
 *
 * @author christoph
 */
public class StartAuxiliaryMethodComputationMacro implements ProofMacro {

    @Override
    public String getName() {
        return "Start auxiliary computation for self-composition proofs";
    }


    @Override
    public String getDescription() {
        return "In order to increase the efficiency of self-composition " +
               "proofs, this macro starts a side calculation which does " +
               "the symbolic execution only once. The result is " +
               "instantiated twice with the variable to be used in the " +
               "two executions of the self-composition.";
    }


    @Override
    public boolean canApplyTo(KeYMediator mediator,
                              PosInOccurrence posInOcc) {
        if (posInOcc == null
                || posInOcc.subTerm() == null) {
            return false;
        }
        Proof proof = mediator.getSelectedProof();
        Services services = proof.getServices();
        ProofOblInput poForProof =
                services.getSpecificationRepository().getPOForProof(proof);
        if (!(poForProof instanceof InfFlowContractPO)) {
            return false;
        }
        InfFlowContractPO po = (InfFlowContractPO) poForProof;

        InfFlowPOSnippetFactory f =
                POSnippetFactory.getInfFlowFactory(po.getContract(),
                                                   po.getIFVars().c1,
                                                   po.getIFVars().c2, services);
        Term selfComposedExec =
                f.create(InfFlowPOSnippetFactory.Snippet.SELFCOMPOSED_EXECUTION_WITH_PRE_RELATION);

        return posInOcc.subTerm().equalsModRenaming(selfComposedExec);
    }


    @Override
    public void applyTo(KeYMediator mediator,
                        PosInOccurrence posInOcc,
                        ProverTaskListener listener) {
        Proof proof = mediator.getSelectedProof();
        Goal goal = mediator.getSelectedGoal();
        Services services = proof.getServices();
        InitConfig initConfig = proof.env().getInitConfig();

        ProofOblInput poForProof = services.getSpecificationRepository().getPOForProof(proof);
        if (!(poForProof instanceof InfFlowContractPO)) {
            return;
        }
        InfFlowContractPO po = (InfFlowContractPO) poForProof;

        SymbolicExecutionPO symbExecPO =
                new SymbolicExecutionPO(initConfig, po.getContract(),
                                        po.getIFVars().symbExecVars.labelHeapAtPreAsAnonHeapFunc(),
                                        goal, proof.getServices());
        ProblemInitializer pi =
                new ProblemInitializer(mediator.getUI(),
                                       mediator.getServices(), true,
                                       mediator.getUI());
        try {
            Proof p = pi.startProver(initConfig, symbExecPO, 0);
            p.unionIFSymbols(proof.getIFSymbols());
            // stop interface again, because it is activated by the proof
            // change through startProver; the ProofMacroWorker will activate
            // it again at the right time
            mediator.stopInterface(true);
            mediator.setInteractive(false);
        } catch (ProofInputException exc) {
            ExceptionDialog.showDialog(MainWindow.getInstance(), exc);
        }
    }


    @Override
    public KeyStroke getKeyStroke() {
        return null; // default implementation
    }
}