// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.explicitheap;


import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;


public class UniqueCondition extends VariableConditionAdapter {
    
    private final SchemaVariable var;
    
    public UniqueCondition(SchemaVariable var) {
        this.var = var;
    }

    /**
     * checks if the condition for a correct instantiation is fulfilled
     * @param var the template Variable to be instantiated
     * @param candidate the SVSubstitute which is a candidate for an
     * instantiation of var
     * @param svInst the SVInstantiations that are already known to be needed 
     * @return true iff condition is fulfilled
     */
    public boolean check(SchemaVariable var, 
                         SVSubstitute candidate, 
                         SVInstantiations svInst,
                         Services services) {
        if (var != this.var) { 
            return true; 
        } else if(!(candidate instanceof Term)) {
            return false;
        } else {
            Term candTerm = (Term)candidate;
            return candTerm.op() instanceof RigidFunction
                   && ((RigidFunction)candTerm.op()).isUnique();
        }
    }
    

    public String toString () {
        return "\\isUnique (" + var+ ")";
    }
}