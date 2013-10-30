package de.uka.ilkd.key.rule.label;

import java.util.List;

import de.uka.ilkd.key.collection.ImmutableArray;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.label.SymbolicExecutionTermLabel;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.QuantifiableVariable;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.Rule;
import de.uka.ilkd.key.rule.WhileInvariantRule;
import de.uka.ilkd.key.symbolic_execution.util.IFilter;
import de.uka.ilkd.key.symbolic_execution.util.JavaUtil;

/**
 * Makes sure that the ID of {@link SymbolicExecutionTermLabel}s is increased
 * when a {@link WhileInvariantRule} is applied.
 * @author Martin Hentschel
 */
public class SymbolicExecutionTermLabelUpdate implements TermLabelUpdate {
   /**
    * {@inheritDoc}
    */
   @Override
   public void updateLabels(Services services, 
                            PosInOccurrence applicationPosInOccurrence, 
                            Term applicationTerm, 
                            Term modalityTerm,
                            Rule rule, 
                            Goal goal, 
                            Object hint, 
                            Term tacletTerm, 
                            Operator newTermOp, 
                            ImmutableArray<Term> newTermSubs, 
                            ImmutableArray<QuantifiableVariable> newTermBoundVars, 
                            JavaBlock newTermJavaBlock, 
                            List<TermLabel> labels) {
      if (rule instanceof WhileInvariantRule && "LoopBodyModality".equals(hint)) {
         TermLabel label = JavaUtil.searchAndRemove(labels, new IFilter<TermLabel>() {
            @Override
            public boolean select(TermLabel element) {
               return element instanceof SymbolicExecutionTermLabel;
            }
         });
         if (label instanceof SymbolicExecutionTermLabel) {
            int labelID = services.getCounter(SymbolicExecutionTermLabel.PROOF_COUNTER_NAME).getCountPlusPlus();
            labels.add(new SymbolicExecutionTermLabel(labelID));
         }
      }
   }
}
