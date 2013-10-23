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

package de.uka.ilkd.key.rule.label;

import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.label.SymbolicExecutionTermLabel;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.label.TermLabels;

/**
 * The {@link TermLabelInstantiator} used during prove to define how a
 * {@link SymbolicExecutionTermLabel} is maintained.
 * @author Martin Hentschel
 */
public final class LoopBodyTermLabelInstantiator extends AbstractSymbolicExecutionInstantiator {

   /**
    * The only instance of this class.
    */
   public static final LoopBodyTermLabelInstantiator INSTANCE = 
           new LoopBodyTermLabelInstantiator();

   /**
    * Constructor to forbid multiple instances.
    */
   private LoopBodyTermLabelInstantiator() {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected TermLabel getTermLabel(Term applicationTerm) {
      return TermLabels.LOOP_BODY_LABEL;
   }

}