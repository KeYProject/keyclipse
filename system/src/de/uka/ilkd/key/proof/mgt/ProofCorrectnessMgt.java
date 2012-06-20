// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.proof.mgt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.uka.ilkd.key.collection.DefaultImmutableSet;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSLList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.gui.RuleAppListener;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.op.ObserverFunction;
import de.uka.ilkd.key.proof.*;
import de.uka.ilkd.key.proof.init.*;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.speclang.Contract;


public final class ProofCorrectnessMgt {

    private final Proof proof;
    private final SpecificationRepository specRepos;
    private final DefaultMgtProofListener proofListener 
	= new DefaultMgtProofListener();
    private final DefaultMgtProofTreeListener proofTreeListener
	= new DefaultMgtProofTreeListener();

    private KeYMediator mediator;
    private Set<RuleApp> cachedRuleApps = new HashSet<RuleApp>();
    private ProofStatus proofStatus = ProofStatus.OPEN;
    
    
    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------
    
    public ProofCorrectnessMgt(Proof p) {
	this.proof = p;
        this.specRepos = p.getServices().getSpecificationRepository();
	proof.addProofTreeListener(proofTreeListener);
    }
    

    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------
    
    
    private boolean allHaveMeasuredBy(ImmutableList<Contract> contracts) {
	for(Contract contract : contracts) {
	    if(!contract.hasMby()) {
		return false;
	    }
	}
	return true;
    }
    
    
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------
    
    public RuleJustification getJustification(RuleApp r) {
	return proof.env()
	            .getJustifInfo()
	            .getJustification(r, proof.getServices());
    }
    
    
    /**
     * Tells whether a contract for the passed target may be applied 
     * in the passed goal without creating circular dependencies.
     */
    public boolean isContractApplicable(Contract contract) {
        //get the contract which is being verified in our current proof
	final ContractPO po = specRepos.getPOForProof(proof);
        if(po == null) {
            return true;
        }
        final Contract originalContract = po.getContract();
        
        //get initial contracts
	ImmutableSet<Contract> contractsToBeApplied
		= specRepos.splitContract(contract);
	assert contractsToBeApplied != null;
	contractsToBeApplied 
		= specRepos.getInheritedContracts(contractsToBeApplied);
	
	//initial paths
	ImmutableSet<ImmutableList<Contract>> newPaths 
		= DefaultImmutableSet.nil();
	for(Contract c : contractsToBeApplied) {
	    newPaths = newPaths.add(ImmutableSLList.<Contract>nil().prepend(c));
	}
	
	//look for cycles
        while(!newPaths.isEmpty()) {
            final Iterator<ImmutableList<Contract>> it = newPaths.iterator();
            newPaths = DefaultImmutableSet.<ImmutableList<Contract>>nil();

            while(it.hasNext()) {
        	final ImmutableList<Contract> path = it.next();
        	final Contract end = path.head();
        	if(end.equals(originalContract)) {
        	    if(!allHaveMeasuredBy(path.prepend(originalContract))) {
        		return false;
        	    }
        	} else {        	
        	    final ImmutableSet<Proof> proofsForEnd 
        	    	= specRepos.getProofs(end);
        	    for(Proof proofForEnd : proofsForEnd) {
        		final ImmutableSet<Contract> contractsUsedForEnd 
        			= proofForEnd.mgt().getUsedContracts();
        		for(Contract contractUsedForEnd : contractsUsedForEnd) {
        		    if(!path.contains(contractUsedForEnd)) {
        			final ImmutableList<Contract> extendedPath 
        				= path.prepend(contractUsedForEnd);
        			newPaths = newPaths.add(extendedPath);
        		    }
        		}
        	    }
        	}
            }
	}
	
        return true;
    }
    
    
    @Deprecated
    public boolean contractApplicableFor(KeYJavaType kjt,
	                                 ObserverFunction target) {
	target = specRepos.unlimitObs(target);
	
        //get the target which is being verified in the passed goal
        final ObserverFunction targetUnderVerification 
            = specRepos.getTargetOfProof(proof);
        if(targetUnderVerification == null) {
            return true;
        }
        
        //collect all proofs on which the specification of the 
        //passed target may depend
        ImmutableSet<Proof> proofs = specRepos.getProofs(kjt, target);    
        ImmutableSet<Proof> newProofs = proofs;
        while(newProofs.size() > 0) {
            final Iterator<Proof> it = newProofs.iterator();
            newProofs = DefaultImmutableSet.<Proof>nil();
            
            while(it.hasNext()) {
                final Proof p = it.next();
                for(Contract contract : p.mgt().getUsedContracts()) {
                    ImmutableSet<Proof> proofsForContractTarget 
                    	= specRepos.getProofs(contract.getKJT(),
                    		              contract.getTarget());
                    newProofs = newProofs.union(proofsForContractTarget);
                    proofs = proofs.union(proofsForContractTarget);                    
                }
            }
        }
        
        //is one of those proofs about the target under verification? 
        for(Proof p : proofs) {
            final ObserverFunction target2 = specRepos.getTargetOfProof(p);
            if(target2.equals(targetUnderVerification)) {
                return false;
            }
        }
        
        return true;
    }


    public void updateProofStatus() {
	final ImmutableSet<Proof> all = specRepos.getAllProofs();
	
	//mark open proofs as open, all others as presumably closed 
	ImmutableSet<Proof> presumablyClosed = DefaultImmutableSet.nil();
	for(Proof p : all) {
	    if(p.openGoals().size() > 0) {
		p.mgt().proofStatus = ProofStatus.OPEN;
	    } else {
		p.mgt().proofStatus = ProofStatus.CLOSED;
		presumablyClosed = presumablyClosed.add(p);
	    }
	}
	
	//revert status of all "presumably closed" proofs for which at least one
	//used contract is definitely not proven to "lemmas left"
	boolean changed = true;
	while(changed) {
	    changed = false;
	    for(Proof p : presumablyClosed) {
		for(Contract usedContract : p.mgt().getUsedContracts()) {
	            final ImmutableSet<Proof> usedProofs 
	            	= specRepos.getProofs(usedContract);
	            if(usedProofs.isEmpty()) {
	        	 p.mgt().proofStatus 
	        	 	= ProofStatus.CLOSED_BUT_LEMMAS_LEFT;
	        	 presumablyClosed = presumablyClosed.remove(p);
	        	 changed = true;
	            } else {
	        	for(Proof usedProof : usedProofs) {
	        	    if(usedProof.mgt().proofStatus 
	        		    != ProofStatus.CLOSED) {
	        		p.mgt().proofStatus 
	        			= ProofStatus.CLOSED_BUT_LEMMAS_LEFT;
	        		presumablyClosed = presumablyClosed.remove(p);
	        		changed = true;
	        		break;
	        	    }
	        	}
	            }
	        }
	    }
	}
    }

    
    public void ruleApplied(RuleApp r) {
	RuleJustification rj = getJustification(r);
	if(rj==null) {
	    System.err.println("No justification found for rule " 
		               + r.rule().name());
	    return;
	}
	if(!rj.isAxiomJustification()) {
            cachedRuleApps.add(r);
	}
    }

    
    public void ruleUnApplied(RuleApp r) {
        cachedRuleApps.remove(r);
    }

    
    public ImmutableSet<Contract> getUsedContracts() {
	ImmutableSet<Contract> result 
		= DefaultImmutableSet.<Contract>nil();
	for(RuleApp ruleApp : cachedRuleApps) {
            RuleJustification ruleJusti = getJustification(ruleApp);
            if(ruleJusti instanceof RuleJustificationBySpec) {
        	Contract contract
                    = ((RuleJustificationBySpec) ruleJusti).getSpec();
        	ImmutableSet<Contract> atomicContracts
        		= specRepos.splitContract(contract);
        	assert atomicContracts != null;
        	atomicContracts 
        		= specRepos.getInheritedContracts(atomicContracts); 
        	result = result.union(atomicContracts);
            }
        }
	return result;
    }

    
    public void setMediator(KeYMediator p_mediator) {
	if(mediator != null) {
	    mediator.removeRuleAppListener(proofListener);
	}

	mediator = p_mediator;

	if(mediator != null) {
	    mediator.addRuleAppListener(proofListener);
	}
    }
    
    
    public void removeProofListener(){
        if(mediator != null) {
          mediator.removeRuleAppListener(proofListener);
        }
    }

        
    public ProofStatus getStatus() {
	return proofStatus;
    }
    
    
    public void finalize() {
        if(mediator != null) {
            mediator.removeRuleAppListener(proofListener);
        }
    }

    
    
    //-------------------------------------------------------------------------
    //inner classes
    //-------------------------------------------------------------------------
    
    private class DefaultMgtProofListener implements RuleAppListener {
	public void ruleApplied(ProofEvent e) {
	    if(proof == e.getSource()) {
                //%% actually I only want to listen to events of one proof
		ProofCorrectnessMgt.this.ruleApplied
		    (e.getRuleAppInfo().getRuleApp());
	    }
	}
    }

    
    private class DefaultMgtProofTreeListener extends ProofTreeAdapter {
	public void proofClosed(ProofTreeEvent e) {	    
	    updateProofStatus();
	}
    }
}
