// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
//

package de.uka.ilkd.key.proof;

import de.uka.ilkd.key.proof.mgt.ProofStatus;

public class SingleProof extends ProofAggregate {

    private final Proof proof;
    
    public SingleProof(Proof p, String name) {
        super(name);
        this.proof = p;
    }
    
    @Override
    public ProofStatus getStatus() {
        return proof.mgt().getStatus();
    }

    @Override
    public Proof[] getProofs() {
        return new Proof[]{proof};
    }

    @Override    
    public int size() {
        return 1;
    }
    
    @Override    
    public ProofAggregate[] getChildren() {
        return new ProofAggregate[0];
    }
}
