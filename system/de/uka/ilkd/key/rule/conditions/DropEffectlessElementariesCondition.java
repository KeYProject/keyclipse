// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.rule.conditions;

import java.util.Set;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.proof.TermProgramVariableCollector;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.rule.VariableCondition;
import de.uka.ilkd.key.rule.inst.SVInstantiations;


public final class DropEffectlessElementariesCondition 
						implements VariableCondition {
    
    private static final TermBuilder TB = TermBuilder.DF;
    
    private final UpdateSV u;
    private final SchemaVariable x;
    private final SchemaVariable result;
    
    public DropEffectlessElementariesCondition(UpdateSV u,
	                               	       SchemaVariable x,
	                               	       SchemaVariable x2) {
	this.u = u;
	this.x = x;
	this.result = x2;
    }
    
    
    private static Term dropEffectlessElementariesHelper(
	    				Term update, 
	    				Set<LocationVariable> relevantVars) {
	if(update.op() instanceof ElementaryUpdate) {
	    ElementaryUpdate eu = (ElementaryUpdate) update.op();
	    LocationVariable lhs = (LocationVariable) eu.lhs();
	    if(relevantVars.contains(lhs)) {
		relevantVars.remove(lhs);
		return null;
	    } else {
		return TB.skip();
	    }
	} else if(update.op() == UpdateJunctor.PARALLEL_UPDATE) {
	    Term sub0 = update.sub(0);
	    Term sub1 = update.sub(1);
	    Term newSub1 = dropEffectlessElementariesHelper(sub1, relevantVars);
	    Term newSub0 = dropEffectlessElementariesHelper(sub0, relevantVars);
	    if(newSub0 == null && newSub1 == null) {
		return null;
	    } else {
		newSub0 = newSub0 == null ? sub0 : newSub0;
		newSub1 = newSub1 == null ? sub1 : newSub1;
		return TB.parallel(newSub0, newSub1);
	    }
	} else if(update.op() == UpdateApplication.UPDATE_APPLICATION) {
	    Term sub0 = update.sub(0);
	    Term sub1 = update.sub(1);
	    Term newSub1 = dropEffectlessElementariesHelper(sub1, relevantVars);
	    return newSub1 == null ? null : TB.apply(sub0, newSub1);
	} else {
	    return null;
	}
    }    
    
    
    private static Term dropEffectlessElementaries(Term update, 
	    					   Term target,
	    					   Services services) {
	TermProgramVariableCollector collector 
		= new TermProgramVariableCollector(services);
	target.execPostOrder(collector);
	Set<LocationVariable> varsInTarget = collector.result();
	Term simplifiedUpdate = dropEffectlessElementariesHelper(update, 
							         varsInTarget); 
	return simplifiedUpdate == null 
	       ? null 
	       : TB.apply(simplifiedUpdate, target); 
    }
    
    
    @Override
    public MatchConditions check(SchemaVariable var, 
	    		  	 SVSubstitute instCandidate, 
	    		  	 MatchConditions mc, 
	    		  	 Services services) {
	SVInstantiations svInst = mc.getInstantiations();
	Term uInst      = (Term) svInst.getInstantiation(u);
	Term xInst      = (Term) svInst.getInstantiation(x);
	Term resultInst = (Term) svInst.getInstantiation(result);
	if(uInst == null || xInst == null) {
	    return mc;
	}
	
	Term properResultInst = dropEffectlessElementaries(uInst, 
						           xInst, 
						           services);
	if(properResultInst == null) {
	    return null;
	} else if(resultInst == null) {
	    svInst = svInst.add(result, properResultInst, services);
	    return mc.setInstantiations(svInst);
	} else if(resultInst.equals(properResultInst)) {
	    return mc;
	} else {
	    return null;
	}
    }
    
    
    @Override
    public String toString () {
        return "\\dropEffectlessElementaries(" 
               + u + ", " + x + ", " + result + ")";
    }
}
