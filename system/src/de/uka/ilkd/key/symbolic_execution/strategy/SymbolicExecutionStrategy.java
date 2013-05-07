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

package de.uka.ilkd.key.symbolic_execution.strategy;

import de.uka.ilkd.key.logic.LoopBodyTermLabel;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.strategy.JavaCardDLStrategy;
import de.uka.ilkd.key.strategy.Strategy;
import de.uka.ilkd.key.strategy.StrategyFactory;
import de.uka.ilkd.key.strategy.StrategyProperties;
import de.uka.ilkd.key.strategy.feature.BinaryFeature;
import de.uka.ilkd.key.strategy.feature.CountBranchFeature;
import de.uka.ilkd.key.strategy.feature.Feature;
import de.uka.ilkd.key.strategy.feature.RuleSetDispatchFeature;
import de.uka.ilkd.key.strategy.feature.ScaleFeature;
import de.uka.ilkd.key.strategy.termfeature.ContainsLabelFeature;
import de.uka.ilkd.key.symbolic_execution.util.SymbolicExecutionUtil;

/**
 * {@link Strategy} to use for symbolic execution.
 */
public class SymbolicExecutionStrategy extends JavaCardDLStrategy {
   /**
    * The {@link Name} of the symbolic execution {@link Strategy}.
    */
   public static final Name name = new Name("Symbolic Execution Strategy");

   /**
    * Constructor.
    * @param proof The proof.
    * @param sp The {@link StrategyProperties} to use.
    */
   private SymbolicExecutionStrategy(Proof proof, StrategyProperties sp) {
      super(proof, sp);
      RuleSetDispatchFeature rsd = getCostComputationDispatcher();

      clearRuleSetBindings (rsd, "simplify_prog" );
      bindRuleSet (rsd, "simplify_prog",10000);
      
      clearRuleSetBindings (rsd, "simplify_prog_subset" );
      bindRuleSet (rsd, "simplify_prog_subset",10000);
      
      Feature splitF = ScaleFeature.createScaled(CountBranchFeature.INSTANCE, -4000);
      bindRuleSet(rsd, "split_if", splitF); // The costs of rules in heuristic "split_if" is reduced at runtime by numberOfBranches * -400. The result is that rules of "split_if" preferred to "split_cond" and run and step into has the same behavior
      bindRuleSet(rsd, "instanceof_to_exists", inftyConst());
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected Feature setupGlobalF(Feature dispatcher, Proof p_proof) {
       Feature globalF = super.setupGlobalF(dispatcher, p_proof);
       // Make sure that modalities without symbolic execution label are executed first because they might forbid rule application on modalities with symbolic execution label (see loop body branches)
       globalF = add(globalF, ifZero(not(new BinaryFeature() {
          @Override
          protected boolean filter(RuleApp app, PosInOccurrence pos, Goal goal) {
             return pos != null && SymbolicExecutionUtil.hasSymbolicExecutionLabel(pos.subTerm());
          }
       }), longConst(-3000)));
       // Make sure that the modality which executes a loop body is preferred against the modalities which executes special loop terminations like return, exceptions or break. 
       globalF = add(globalF, ifZero(new ContainsLabelFeature(LoopBodyTermLabel.INSTANCE), longConst(-2000)));
       return globalF;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Name name() {
      return name;
   }
   
   /**
    * Returns the default {@link StrategyProperties} of symbolic execution. 
    * @param splittingRulesAllowed Allow splitting rules?
    * @param quantifierInstantiationWithSplitting Instantiate quantifiers?
    * @param methodTreatmentContract Use method contracts or inline method bodies otherwise?
    * @param loopTreatmentInvariant Use loop invariants or unrole loops otherwise?
    * @return The default {@link StrategyProperties} for symbolic execution.
    */
   public static StrategyProperties getSymbolicExecutionStrategyProperties(boolean splittingRulesAllowed,
                                                                           boolean quantifierInstantiationWithSplitting,
                                                                           boolean methodTreatmentContract, 
                                                                           boolean loopTreatmentInvariant) {
      StrategyProperties sp = new StrategyProperties();
      sp.setProperty(StrategyProperties.LOOP_OPTIONS_KEY, loopTreatmentInvariant ? StrategyProperties.LOOP_INVARIANT : StrategyProperties.LOOP_EXPAND);
      sp.setProperty(StrategyProperties.BLOCK_OPTIONS_KEY, StrategyProperties.BLOCK_EXPAND);
      sp.setProperty(StrategyProperties.METHOD_OPTIONS_KEY, methodTreatmentContract ? StrategyProperties.METHOD_CONTRACT : StrategyProperties.METHOD_EXPAND);
      sp.setProperty(StrategyProperties.QUERY_OPTIONS_KEY, StrategyProperties.QUERY_ON);
      sp.setProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY, StrategyProperties.NON_LIN_ARITH_DEF_OPS);
      sp.setProperty(StrategyProperties.AUTO_INDUCTION_OPTIONS_KEY, StrategyProperties.AUTO_INDUCTION_OFF);
      sp.setProperty(StrategyProperties.DEP_OPTIONS_KEY, StrategyProperties.DEP_OFF);
      sp.setProperty(StrategyProperties.QUERYAXIOM_OPTIONS_KEY, StrategyProperties.QUERYAXIOM_OFF);
      sp.setProperty(StrategyProperties.SPLITTING_OPTIONS_KEY, StrategyProperties.SPLITTING_DELAYED);
      sp.setProperty(StrategyProperties.RETREAT_MODE_OPTIONS_KEY, StrategyProperties.RETREAT_MODE_NONE);
      sp.setProperty(StrategyProperties.STOPMODE_OPTIONS_KEY, StrategyProperties.STOPMODE_DEFAULT);
      sp.setProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY, quantifierInstantiationWithSplitting ? StrategyProperties.QUANTIFIERS_INSTANTIATE : StrategyProperties.QUANTIFIERS_NON_SPLITTING_WITH_PROGS);
      return sp;
   }

   /**
    * The {@link StrategyFactory} to create instances of {@link SymbolicExecutionStrategy}.
    * @author Martin Hentschel
    */
   public static class Factory extends StrategyFactory {
      /**
       * {@inheritDoc}
       */
      @Override
      public Strategy create(Proof proof, StrategyProperties sp) {
         return new SymbolicExecutionStrategy(proof, sp);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public Name name() {
         return name;
      }
   }
}