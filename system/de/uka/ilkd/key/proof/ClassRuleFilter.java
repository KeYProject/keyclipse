// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//


package de.uka.ilkd.key.proof;


import de.uka.ilkd.key.rule.Rule;

/**
 * Rule filter that selects taclets which are of a specific class 
 */
public class ClassRuleFilter implements RuleFilter {

    private final Class c;
    
    public ClassRuleFilter(Class c) {
	this.c = c;
    }
    

    public boolean filter( Rule rule ) {
        return c.isAssignableFrom(rule.getClass());
    }
}