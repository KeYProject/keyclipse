// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//


package de.uka.ilkd.key.rule.metaconstruct;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.rule.inst.SVInstantiations;


public final class AddCast extends AbstractTermTransformer {
    
    private static final TermBuilder TB = TermBuilder.DF;
    

    public AddCast() {
        super(new Name("#addCast"), 2);
    }
    

    @Override
    public Term transform(Term term, 
	    		  SVInstantiations svInst, 
	    		  Services services ) {
	Term sub = term.sub(0);
	Sort sort = term.sub(1).sort();
	
	return sub.sort().extendsTrans(sort) 
	       ? sub 
	       : TB.cast(services, sort, sub);
    }
}
