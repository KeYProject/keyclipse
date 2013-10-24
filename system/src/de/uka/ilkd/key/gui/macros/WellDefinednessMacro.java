// This file is part of KeY - Integrated Deductive Software Design
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2013 Karlsruhe Institute of Technology, Germany
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General
// Public License. See LICENSE.TXT for details.
//

package de.uka.ilkd.key.gui.macros;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.init.ContractPO;
import de.uka.ilkd.key.proof.init.FunctionalOperationContractPO;
import de.uka.ilkd.key.proof.init.WellDefinednessPO;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.strategy.LongRuleAppCost;
import de.uka.ilkd.key.strategy.RuleAppCost;
import de.uka.ilkd.key.strategy.RuleAppCostCollector;
import de.uka.ilkd.key.strategy.Strategy;
import de.uka.ilkd.key.strategy.TopRuleAppCost;

/**
 * Resolve well-definedness predicate
 *
 * @author Michael Kirsten
 */
public class WellDefinednessMacro extends StrategyProofMacro {

    public static final String WD_PREFIX = "wd_";
    public static final String WD_BRANCH = "Well-Definedness";

    @Override
    public String getName() {
        return "Well-Definedness Rules";
    }

    @Override
    public String getDescription() {
        return "Apply only rules to resolve the Well-Definedness transformer.";
    }

    @Override
    protected Strategy createStrategy(KeYMediator mediator,
                                      PosInOccurrence posInOcc) {
        return new WellDefinednessStrategy();
    }

    /** FIXME: Somehow currently doesn't work (but keytroke still gets shown) as
     *         {@link #canApplyTo(KeYMediator, PosInOccurrence)} returns false on
     *         instantiation.
     */
    @Override
    public KeyStroke getKeyStroke () {
        return KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.SHIFT_DOWN_MASK);
    }

    @Override
    public boolean canApplyTo(KeYMediator mediator, PosInOccurrence posInOcc) {
        if (mediator.getSelectedProof() == null
                || mediator.getServices() == null
                || mediator.getServices().getSpecificationRepository() == null) {
            return false;
        }
        final ContractPO po =
                mediator.getServices().getSpecificationRepository()
                        .getPOForProof(mediator.getSelectedProof());
        if (po instanceof WellDefinednessPO) {
            return true;
        } else if (!(po instanceof FunctionalOperationContractPO)) {
            return false;
        }
        Node n = mediator.getSelectedNode();
        while (n != null) {
            // Check if we are in a well-definedness branch (e.g. of a loop statement)
            if (n.getNodeInfo().getBranchLabel() != null
                    && n.getNodeInfo().getBranchLabel().equals(WD_BRANCH)) {
                return true;
            }
            n = n.parent();
        }
        return false;
    }

    /**
     * This strategy accepts all rule apps for which the rule name is a
     * Well-Definedness rule and rejects everything else.
     */
    private static class WellDefinednessStrategy implements Strategy {

        private static final Name NAME = new Name(WellDefinednessStrategy.class.getSimpleName());

        public WellDefinednessStrategy() {
        }

        @Override
        public Name name() {
            return NAME;
        }

        @Override
        public RuleAppCost computeCost(RuleApp ruleApp, PosInOccurrence pio, Goal goal) {
            String name = ruleApp.rule().name().toString();
            if(name.startsWith(WD_PREFIX)) {
                return LongRuleAppCost.ZERO_COST;
            } else {
                return TopRuleAppCost.INSTANCE;
            }
        }

        @Override
        public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
            return true;
        }

        @Override
        public void instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
                RuleAppCostCollector collector) {
        }
    }
}