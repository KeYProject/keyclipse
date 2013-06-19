/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uka.ilkd.key.gui.macros;

import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.gui.ProverTaskListener;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.ContractPO;
import de.uka.ilkd.key.proof.init.IFProofObligationVars;
import de.uka.ilkd.key.proof.init.InfFlowContractPO;
import de.uka.ilkd.key.proof.init.SymbolicExecutionPO;
import de.uka.ilkd.key.proof.init.po.snippet.InfFlowPOSnippetFactory;
import de.uka.ilkd.key.proof.init.po.snippet.POSnippetFactory;
import de.uka.ilkd.key.rule.RewriteTaclet;
import de.uka.ilkd.key.rule.RuleSet;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletGoalTemplate;
import de.uka.ilkd.key.util.GuiUtilities;
import de.uka.ilkd.key.util.MiscTools;


/**
 *
 * @author christoph
 */
public class FinishAuxiliaryComputationMacro
        extends AbstractFinishAuxiliaryComputationMacro {

    private static int i = 0;

    @Override
    public boolean canApplyTo(KeYMediator mediator,
                              PosInOccurrence posInOcc) {
        final Proof proof = mediator.getSelectedProof();
        final Services services = proof.getServices();
        final ContractPO poForProof =
                services.getSpecificationRepository().getPOForProof(proof);
        return poForProof instanceof SymbolicExecutionPO;
    }


    @Override
    public void applyTo(final KeYMediator mediator,
                        PosInOccurrence posInOcc,
                        ProverTaskListener listener) {
        final Proof proof = mediator.getSelectedProof();
        final ContractPO poForProof =
                proof.getServices().getSpecificationRepository().getPOForProof(proof);
        if (!(poForProof instanceof SymbolicExecutionPO)) {
            return;
        }
        final Goal initiatingGoal = ((SymbolicExecutionPO) poForProof).getInitiatingGoal();
        final Proof initiatingProof = initiatingGoal.proof();
        final Services services = initiatingProof.getServices();
        final InfFlowContractPO ifPO =
                (InfFlowContractPO) services.getSpecificationRepository().getPOForProof(initiatingProof);

        // create and register resulting taclets
        final Term result = calculateResultingTerm(proof, ifPO.getIFVars(), initiatingGoal);
        final Taclet rwTaclet = generateRewriteTaclet(result, ifPO, services);
        initiatingGoal.proof().addIFSymbol(rwTaclet);
        initiatingGoal.addTaclet(rwTaclet, SVInstantiations.EMPTY_SVINSTANTIATIONS, true);
        addContractApplicationTaclets(initiatingGoal, proof);
        initiatingGoal.proof().unionIFSymbols(proof.getIFSymbols());

        saveAuxiliaryProof();

        // close auxiliary computation proof
        GuiUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // make everyone listen to the proof remove
                mediator.startInterface(true);
                mediator.getUI().removeProof(proof);
                // go into automode again
                mediator.stopInterface(true);
            }
        });        
    }


    private Taclet generateRewriteTaclet(Term replacewith,
                                         InfFlowContractPO infPO,
                                         Services services) {
        final Name tacletName =
                MiscTools.toValidTacletName("unfold computed formula " + i + " of " +
                                            infPO.getContract().getTarget().getFullName());
        i++;

        // create find term
        final IFProofObligationVars ifVars = infPO.getIFVars();
        InfFlowPOSnippetFactory f =
                POSnippetFactory.getInfFlowFactory(infPO.getContract(),
                                                   ifVars.c1, ifVars.c2,
                                                   services);
        final Term find =
                f.create(InfFlowPOSnippetFactory.Snippet.SELFCOMPOSED_EXECUTION_WITH_PRE_RELATION);
        services.getProof().addIFSymbol(find);

        //create taclet
        final RewriteTacletBuilder tacletBuilder = new RewriteTacletBuilder();
        tacletBuilder.setName(tacletName);
        tacletBuilder.setFind(find);
        tacletBuilder.setApplicationRestriction(RewriteTaclet.ANTECEDENT_POLARITY);
        final RewriteTacletGoalTemplate goal =
                new RewriteTacletGoalTemplate(replacewith);
        tacletBuilder.addTacletGoalTemplate(goal);
        tacletBuilder.addRuleSet(new RuleSet(new Name("concrete")));
        tacletBuilder.setSurviveSmbExec(true);

        return tacletBuilder.getTaclet();
    }
}