// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.logic;

import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.Sort;


final class TermImpl implements Term {

    private static final ArrayOfTerm EMPTY_TERM_LIST 
    	= new ArrayOfTerm();
    private static final ArrayOfQuantifiableVariable EMPTY_VAR_LIST
    	= new ArrayOfQuantifiableVariable();

    //content
    private final Operator op;
    private final ArrayOfTerm subs;
    private final ArrayOfQuantifiableVariable boundVars;
    private final JavaBlock javaBlock;
    
    //caches
    private static enum ThreeValuedTruth { TRUE, FALSE, UNKNOWN }
    private int depth = -1;
    private ThreeValuedTruth rigid = ThreeValuedTruth.UNKNOWN; 
    private SetOfQuantifiableVariable freeVars = null; 
    private int hashcode = -1;
    
    
    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------
    
    public TermImpl(Operator op, 
	    	    ArrayOfTerm subs, 
	    	    JavaBlock javaBlock, 
	    	    ArrayOfQuantifiableVariable boundVars) {
	assert op != null;
	assert subs != null;
	this.op   = op;
	this.subs = subs.size() == 0 ? EMPTY_TERM_LIST : subs;
	this.javaBlock = javaBlock == null ? JavaBlock.EMPTY_JAVABLOCK : javaBlock;
	this.boundVars = boundVars == null ? EMPTY_VAR_LIST : boundVars;
    }
    
    
    public TermImpl(Operator op, 
	    	    ArrayOfTerm subs, 
	    	    JavaBlock javaBlock) {
	this(op, subs, javaBlock, null);
    }    

    
    public TermImpl(Operator op, ArrayOfTerm subs) {
	this(op, subs, null, null);
    }


    public TermImpl(Operator op, Term[] subs) {
	this(op, new ArrayOfTerm(subs), null, null);
    }
    
    
    public TermImpl(Operator op) {
	this(op, EMPTY_TERM_LIST);
    }    
    
        
    //-------------------------------------------------------------------------
    //internal methods
    //------------------------------------------------------------------------- 

    private void determineFreeVars() {
	freeVars = SetAsListOfQuantifiableVariable.EMPTY_SET;
        
        if(op instanceof QuantifiableVariable) {
            freeVars = freeVars.add((QuantifiableVariable) op);
        }
        
        for(int i = 0, ar = arity(); i < ar; i++) {
            Term subTerm = sub(i);
	    SetOfQuantifiableVariable subFreeVars = subTerm.freeVars();
	    for(int j = 0, sz = varsBoundHere(i).size(); j < sz; j++) {
		subFreeVars 
		    = subFreeVars.remove(varsBoundHere(i)
			         .getQuantifiableVariable(j));
	    }
	    freeVars = freeVars.union(subFreeVars);
	}
    }
    
    
    
    //-------------------------------------------------------------------------
    //public interface
    //------------------------------------------------------------------------- 
    
    @Override
    public Operator op() {
        return op;
    }
    
    
    @Override
    public ArrayOfTerm subs() {
	return subs;
    }
    
    
    @Override
    public Term sub(int nr) {
	return subs.getTerm(nr);
    }
    
    
    @Override
    public Term subAt(PosInTerm pos) {
        Term sub = this;
        for (final IntIterator it = pos.iterator(); it.hasNext(); ) {	
            sub = sub.sub(it.next());
        }
        return sub;
    }
    
    
    @Override
    public ArrayOfQuantifiableVariable boundVars() {
	return boundVars;
    }
    
        
    @Override
    public ArrayOfQuantifiableVariable varsBoundHere(int n) {
	return op.bindVarsAt(n) ? boundVars : EMPTY_VAR_LIST;
    }
    
    
    @Override
    public JavaBlock javaBlock() {
	return javaBlock;
    }    
    
    
    @Override
    public Term checked() {
	if(op().validTopLevel(this)) {
	    return this;	    
	} else {	   	    
	    throw new TermCreationException(op(), this);
	}
    }

    
    @Override
    public int arity() {
	return op.arity();
    }
    
    
    @Override
    public Sort sort() {
	return op.sort(subs);
    }
    

    @Override
    public int depth() {
	if(depth == -1) {
            for (int i = 0, n = arity(); i < n; i++) {
                final int subTermDepth = sub(i).depth();
                if(subTermDepth > depth) {
                    depth = subTermDepth;   
                }
            }
            depth++;
	}
        return depth;
    }
    
    
    @Override
    public boolean isRigid() {
	if(rigid == ThreeValuedTruth.UNKNOWN) {
            if(!op.isRigid()) {
        	rigid = ThreeValuedTruth.FALSE;
            } else {
        	rigid = ThreeValuedTruth.TRUE;
        	for(int i = 0, n = arity(); i < n; i++) {
            	    if(!sub(i).isRigid()) {
            		rigid = ThreeValuedTruth.FALSE;
            		break;
            	    }
        	}
            }
        }
            
       return rigid == ThreeValuedTruth.TRUE;
    }
    

    @Override
    public SetOfQuantifiableVariable freeVars() {
        if(freeVars == null) {
            determineFreeVars();
        }
        return freeVars;
    }
    

    @Override
    public SetOfMetavariable metaVars () {
	return SetAsListOfMetavariable.EMPTY_SET;
    }
    
    
    @Override
    public void execPostOrder(Visitor visitor) {
	visitor.subtreeEntered(this);
	for(int i = 0, ar = arity(); i < ar; i++) {
	    sub(i).execPostOrder(visitor);
	}
	visitor.visit(this);
	visitor.subtreeLeft(this);
    }


    @Override
    public void execPreOrder(Visitor visitor) {
	visitor.subtreeEntered(this);
	visitor.visit(this);
	for (int i = 0, ar = arity(); i < ar; i++) {
	    sub(i).execPreOrder(visitor);
	}
	visitor.subtreeLeft(this);	
    }
    

    @Override
    public boolean equalsModRenaming(Object o) {
        if(o == this) {
            return true;
        }       
        if (!(o instanceof Term)) {
	    return false;
	}
	final Constraint constraint = Constraint.BOTTOM.unify(this, 
							      (Term) o, 
							      null);

	return constraint == Constraint.BOTTOM;	
    }
    

    /**
     * true if <code>o</code> is syntactically equal to this term
     */
    @Override
    public boolean equals(Object o) {
	if(o == this) {
	    return true;
	}
	
	if(!(o instanceof Term)) {
	    return false;	
	}
	final Term t = (Term) o;
	
	return op().equals(t.op())
	       && subs().equals(t.subs())
	       && boundVars().equals(t.boundVars())
	       && javaBlock().equals(t.javaBlock());
    }


    @Override
    public final int hashCode(){
        if(hashcode == -1) {
            hashcode = 5;
            hashcode = hashcode*17 + op().hashCode();
            hashcode = hashcode*17 + subs().hashCode();
            hashcode = hashcode*17 + boundVars().hashCode();            
            hashcode = hashcode*17 + javaBlock().hashCode();
            
            if(hashcode == -1) {
        	hashcode = 0;
            }
        }
        return hashcode;
    }


    /**
     * returns a linearized textual representation of this term 
     */
    @Override    
    public String toString() {
	StringBuffer sb = new StringBuffer();
	if(!javaBlock.isEmpty()) {
	    if(op() == Modality.DIA) {
		sb.append("\\<").append(javaBlock).append("\\> ");
	    } else if (op() == Modality.BOX) {
		sb.append("\\[").append(javaBlock).append("\\] ");
	    } else {
		sb.append(op()).append("\\[").append(javaBlock).append("\\] ");
	    }
	    sb.append("(").append(sub(0)).append(")");
	    return sb.toString();
	} else {
            sb.append(op().name().toString());
            if (arity() == 0) return sb.toString();
            sb.append("(");
            for (int i = 0, ar = arity(); i<ar; i++) {
                for (int j=0, vbSize = varsBoundHere(i).size(); j<vbSize; j++) {
                    if (j == 0) {
                        sb.append("{");
                    }
                    sb.append(varsBoundHere(i).getQuantifiableVariable(j));
                    if (j!=varsBoundHere(i).size()-1) {
                        sb.append(", ");
                    } else {
                        sb.append("}");
                    }
                }
                sb.append(sub(i));
                if (i < ar-1) {
                    sb.append(",");
                }
            }
            sb.append(")");
	}
	
        return sb.toString();
    }
}