// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.rule;

import java.util.*;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.op.Function;
import de.uka.ilkd.key.logic.op.IfThenElse;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.OpReplacer;


public final class PullOutConditionalsRule implements BuiltInRule {
    
    public static final PullOutConditionalsRule INSTANCE 
                                            = new PullOutConditionalsRule();
    
    private static final Name NAME = new Name("Pull Out Conditionals");
    private static final TermBuilder TB = TermBuilder.DF;
    
    private final List<List<Term>> equivalenceClasses 
    	= new LinkedList<List<Term>>();
    private Term focus;

        
    

    //-------------------------------------------------------------------------
    //constructors
    //------------------------------------------------------------------------- 
    
    private PullOutConditionalsRule() {
    }
    
    
    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------
    
    private void collectConditionals(Term t) {
	if(t.op() instanceof IfThenElse 
	   && t.sort() != Sort.FORMULA 
           && t.freeVars().isEmpty()) {
	    boolean inExistingClass = false;
	    for(List<Term> equivalenceClass : equivalenceClasses) {
		if(equivalenceClass.get(0).equalsModRenaming(t)) {
		    equivalenceClass.add(t);
		    inExistingClass = true;
		    break;
		}
	    }
	    if(!inExistingClass) {
		final List<Term> newClass = new LinkedList<Term>();
		newClass.add(t);
		equivalenceClasses.add(newClass);
	    }
	}
	for(Term sub : t.subs()) {
	    collectConditionals(sub);
	}
    }
    
    
    
    //-------------------------------------------------------------------------
    //public interface
    //------------------------------------------------------------------------- 
    
    @Override    
    public boolean isApplicable(Goal goal, 
                                PosInOccurrence pio, 
                                Constraint userConstraint) {
	//pio must be top level formula
	if(pio == null || !pio.isTopLevel()) {
	    return false;
	}
	
	//node must have parent
	final Node parent = goal.node().parent();
	if(parent == null) {
	    return false;
	}
	
	//last rule app must be one step simplification
	final RuleApp app = parent.getAppliedRuleApp();
	if(app == null || !(app.rule() instanceof OneStepSimplifier)) {
	    return false;
	}
	
	//semisequent of pio must be same as in last rule app
	final PosInOccurrence parentPio = app.posInOccurrence();
	if(parentPio.isInAntec() != pio.isInAntec()) {
	    return false;
	}
	
	//formula number must be same as in last rule app
	final int parentNum
		= parent.sequent()
	                .formulaNumberInSequent(parentPio.isInAntec(), 
		                                parentPio.constrainedFormula());
	final int num 
		= goal.sequent()
		      .formulaNumberInSequent(pio.isInAntec(), 
			                      pio.constrainedFormula());
	if(parentNum != num) {
	    return false;
	}
	
	//determine and cache equivalence classes
	focus = pio.subTerm();	
	equivalenceClasses.clear();		
	collectConditionals(focus);
	
	//there must be at least one equivalence class with more than one 
	//element
	for(List<Term> equivalenceClass : equivalenceClasses) {
	    if(equivalenceClass.size() > 1) {
		return true;
	    }
	}
	return false;
    }

    
    @Override
    public ImmutableList<Goal> apply(Goal goal, 
	    			     Services services, 
	    			     RuleApp ruleApp) {
	final PosInOccurrence pio = ruleApp.posInOccurrence();
	final ImmutableList<Goal> result = goal.split(1);	
	
	//collect equivalence classes, if necessary
	if(!pio.subTerm().equals(focus)) {
	    focus = pio.subTerm();
	    equivalenceClasses.clear();
	    collectConditionals(focus);
	}
	
	//prepare replace map
	final Map<Term,Term> map = new LinkedHashMap<Term,Term>();
	for(List<Term> equivalenceClass : equivalenceClasses) {	    
	    if(equivalenceClass.size() > 1) {
		final Function f = new Function(new Name(TB.newName(services, 
								    "cond")), 
		                                equivalenceClass.get(0).sort());
		final Term fTerm = TB.func(f);
		services.getNamespaces().functions().addSafely(f);
		for(Term t : equivalenceClass) {
		    map.put(t, fTerm);
		}
	    }
	}
	
	//replace
	final OpReplacer or = new OpReplacer(map);
	final ConstrainedFormula newCF 
		= new ConstrainedFormula(or.replace(focus));
	result.head().changeFormula(newCF, ruleApp.posInOccurrence());
	
	//add definitions
	final Set<Term> alreadyDefined = new HashSet<Term>(); 
	for(Map.Entry<Term, Term> entry : map.entrySet()) {
	    final Term term   = entry.getKey();
	    final Term abbrev = entry.getValue();
	    if(!alreadyDefined.contains(abbrev)) {	    
		alreadyDefined.add(abbrev);
		final Term term2 = TB.ife(or.replace(term.sub(0)), 
		                      	  or.replace(term.sub(1)),
		                      	  or.replace(term.sub(2)));
		final ConstrainedFormula defCF 
	    		= new ConstrainedFormula(TB.equals(term2, abbrev));
		result.head().addFormula(defCF, true, false);
	    }
	}
	
	return result;
    }
    
    
    @Override
    public Name name() {
        return NAME;
    }


    @Override
    public String displayName() { 
        return NAME.toString();
    }
    
    
    @Override
    public String toString() {
        return displayName();
    }    
}
