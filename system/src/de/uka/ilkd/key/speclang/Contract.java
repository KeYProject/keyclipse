// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.speclang;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.op.ObserverFunction;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.ProofOblInput;


/**
 * A contractual agreement about an ObserverFunction.
 */
public interface Contract extends SpecificationElement {
    
    public static final int INVALID_ID = -1;    
        
    
    /**
     * Returns the id number of the contract. If a contract has instances for
     * several methods (inheritance!), all instances have the same id.
     * The id is either non-negative or equal to INVALID_ID.
     */
    public int id();
    
    /**
     * Returns the contracted function symbol.
     */
    public ObserverFunction getTarget();
        
    /**
     * Tells whether the contract contains a measured_by clause.
     */
    public boolean hasMby();
    
    /**
     * Returns the precondition of the contract.
     */
    public Term getPre(ProgramVariable selfVar, 
	    	       ImmutableList<ProgramVariable> paramVars,
	    	       Services services);
    
    /**
     * Returns the precondition of the contract.
     */
    public Term getPre(Term heapTerm,
	               Term selfTerm, 
	    	       ImmutableList<Term> paramTerms,
	    	       Services services);    
    
    
    /**
     * Returns the measured_by clause of the contract.
     */
    public Term getMby(ProgramVariable selfVar,
	               ImmutableList<ProgramVariable> paramVars,
	               Services services);    
        
    /**
     * Returns the measured_by clause of the contract.
     */
    public Term getMby(Term heapTerm,
	               Term selfTerm,
	               ImmutableList<Term> paramTerms,
	               Services services);
    
    /**
     * Returns another contract like this one but with the passed id.
     */
    public Contract setID(int id);
    
    /**
     * Returns another contract like this one, except that it refers to the 
     * passed target. 
     */
    public Contract setTarget(KeYJavaType newKJT,
	    	              ObserverFunction newTarget,
	    		      Services services);        
            
    /**
     * Returns the contract in pretty HTML format.
     */
    public String getHTMLText(Services services);
    
    /**
     * Tells whether, on saving a proof where this contract is available, the
     * contract should be saved too. (this is currently true for contracts
     * specified directly in DL, but not for JML contracts)
     */
    public boolean toBeSaved();
    
    /**
     * Returns a parseable String representation of the contract. 
     * Precondition: toBeSaved() must be true.
     */
    public String proofToString(Services services);
    
    
    /**
     * Returns a proof obligation to the passed contract and initConfig.
     */
    public ProofOblInput createProofObl(InitConfig initConfig,
	    Contract contract);
}