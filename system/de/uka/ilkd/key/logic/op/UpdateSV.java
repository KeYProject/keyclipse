// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.logic.op;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.util.Debug;


public final class UpdateSV extends AbstractSV {

    /** 
     * Creates a new SchemaVariable that is used as placeholder for updates.
     */    
    UpdateSV(Name name) {	
        super(name, Sort.UPDATE, false, true);
    }
    
    
    @Override
    public MatchConditions match(SVSubstitute subst, 
	    			 MatchConditions mc,
	    			 Services services) {
        if (subst instanceof Term) {
            return addInstantiation((Term) subst, mc, services);
        }
        Debug.out("FAILED. Schemavariable of this kind only match terms.");
        return null;
    }
	
    
    @Override
    public String toString() {
        return toString("update");
    }
}