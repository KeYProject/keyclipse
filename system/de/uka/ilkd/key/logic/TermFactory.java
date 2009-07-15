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

import java.util.*;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.*;
import de.uka.ilkd.key.util.Debug;
import de.uka.ilkd.key.util.LRUCache;

/** 
 * The TermFactory is the <em>only</em> way to create terms using constructos of
 * class Term or any of its subclasses. It is the
 * only class that implements and may exploit knowledge about sub
 * classes of {@link Term} all other classes of the system only know
 * about terms what the {@link Term} class offers them. 
 * 
 * This class is used to encapsulate knowledge about the internal term structures.
 * See {@link de.uka.ilkd.key.logic.TermBuilder} for more convienient methods to create
 * terms. 
 */
public class TermFactory {

    /**
     * class used a key for term cache if more than one composite needs 
     * to be compared
     * Attention: for complex terms comparing may be too expensive in these
     * cases do not cache them
     */
    static class CacheKey {
        
        private final static Object DUMMY_KEY_COMPOSITE = new Object();
        
        private final Object o1, o2, o3;
        
        /**
         * the first key composite is compared by identity
         * the second key composite is compared via equals.
         * It must not be null.
         * @param o1 the first key composite
         * @param o2 the second key composite (non null)
         */
        public CacheKey(Object o1, Object o2) {
            assert o2 != null :
                "CacheKey composites must not be null";
            
            this.o1 = o1;
            this.o2 = o2;
            this.o3 = DUMMY_KEY_COMPOSITE;
        }
        
        /**
         * the first key composite is compared by identity
         * the second and third key composite is compared via equals.
         * They must not be null.
         * @param o1 the first key composite
         * @param o2 the second key composite (non null)
         * @param o3 the third key composite (non null)
         */
        public CacheKey(Object o1, Object o2, Object o3) {
            assert o2 != null && o3 != null :
                "CacheKey composites must not be null";
            
            this.o1 = o1;
            this.o2 = o2;
            this.o3 = o3;
        }
        

        public int hashCode() {
            // fixed: o1.hashCode may only be called if o1 is not null.
            int o1Hash = 0;
            if(o1 != null) { 
                o1Hash = o1.hashCode();
            }
            return o1Hash + 17*o2.hashCode() + 7*o3.hashCode(); 
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof CacheKey)) {
                return false;
            }
            final CacheKey ck = (CacheKey) o;
            return o1 == ck.o1 && o2.equals(ck.o2) && o3.equals(ck.o3);
        }
        
    }

    private  static Map<Object, Term> cache = 
        Collections.synchronizedMap(new LRUCache<Object, Term>(5000));

    
    /** An instance of TermFactory */
    public static final TermFactory DEFAULT=new TermFactory();

    private static final Term[] NO_SUBTERMS = new Term[0];


    public Term createBoxTerm(JavaBlock javaBlock, Term subTerm) {
	return createProgramTerm(Modality.BOX, javaBlock, subTerm);
    }


    public Term createDiamondTerm(JavaBlock javaBlock, Term subTerm) {        
	return createProgramTerm(Modality.DIA, javaBlock, subTerm);
    }


    /**
     * creates a EqualityTerm with a given equality operator. USE THIS WITH
     * CARE! THERE IS NO CHECK THAT THE EQUALITY OPERATOR MATCHES THE TERMS.
     */
    public Term createEqualityTerm(Equality op, Term[] subTerms) {	
        return new TermImpl(op, subTerms).checked();
    }

    /**
     * creates an EqualityTerm with a given equality operator. USE THIS WITH
     * CARE! THERE IS NO CHECK THAT THE EQUALITY OPERATOR MATCHES THE TERMS.
     */
    public Term createEqualityTerm(Equality op, Term t1, Term t2) {
	return createEqualityTerm(op, new Term[]{t1, t2});
    }
     
    /** create an EqualityTerm with the correct equality symbol for
     * the sorts involved, according to {@link Sort#getEqualitySymbol}
     */
    public Term createEqualityTerm(Term t1, Term t2) {
	return createEqualityTerm(new Term[]{t1, t2});

    }


    /** create an EqualityTerm with the correct equality symbol for
     * the sorts involved, according to {@link Sort#getEqualitySymbol}
     */
    public Term createEqualityTerm(Term[] terms) {
	Equality eq;
	if(terms[0].sort() == Sort.FORMULA) {
	    eq = Equality.EQV;
	} else {
	    eq = Equality.EQUALS;
	}        
        return createEqualityTerm(eq, terms);
    }
    
    public Term createFunctionTerm(Operator op) {
        Term result = cache.get(op);
        if (result == null) {
            result = createFunctionTerm(op, NO_SUBTERMS);
            cache.put(op, result);
        } 
        return result;
    }

    public Term createFunctionTerm(Operator op, Term s1) {
        final CacheKey key = new CacheKey(op, s1);
        Term result = cache.get(key);
        if (result == null) {
            result = createFunctionTerm(op, new Term[]{s1});
            cache.put(key, result);
        } 
        return result;
    }

    public Term createFunctionTerm(Operator op, Term s1, Term s2) {	
	if(op == null || s1 == null || s2 == null) {
	    throw new TermCreationException("null not allowed");
	}
        final CacheKey key = new CacheKey(op, s1, s2);
        Term result = cache.get(key);
        if (result == null) {
            result = createFunctionTerm(op, new Term[]{s1,s2});
            cache.put(key, result);
        } 
        return result;
    }

     /** 
      * creates a term representing a function or predicate 
      * @param op a TermSymbol which is the top level operator of the
      * function term to be created
      * @param subTerms array of Term containing the sub terms,
      * usually the function's or predicate's arguments
      * @return a term with <code>op</code> as top level symbol and
      * the terms in <code>subTerms</code> as arguments (direct
      * subterms)
      */
    public Term createFunctionTerm(Operator op, Term[] subTerms) {
	if (op==null) throw new IllegalArgumentException("null-Operator at"+
							 "TermFactory");
	
	return new TermImpl(op, subTerms).checked();
    }


    //For OCL simplification

    /** creates a term representing an OCL expression with a
      * collection operation as top operator that 
      * takes an OclExpression as argument (not "iterate")
      * @param op the OCL collection operation
      * @param subs subs[0] is the collection and subs[1] is the 
      *        expression in which the iterator variable is bound
      */

    public Term createFunctionWithBoundVarsTerm(Operator op,
						PairOfTermArrayAndBoundVarsArray subs) {
	return createFunctionWithBoundVarsTerm(op, subs.getTerms(), subs.getBoundVars());
    }

    public Term createFunctionWithBoundVarsTerm(Operator op,
						Term[] subTerms,
						ArrayOfQuantifiableVariable[] boundVars) {
	if (boundVars != null && boundVars.length > 0) {
	   return new TermImpl(op, 
		   	       new ArrayOfTerm(subTerms), 
		   	       JavaBlock.EMPTY_JAVABLOCK, 
		   	       boundVars[0]).checked(); 
	} else {
	    return createFunctionTerm(op, subTerms);
	}
     }
    
    
    /**
     * Create an 'if-then-else' term (or formula)
     */
    public Term createIfThenElseTerm(Term condF, Term thenT, Term elseT) {
        return new TermImpl(IfThenElse.IF_THEN_ELSE, new Term [] { condF, thenT, elseT });
    }
    

    /**
     * Create an 'ifEx-then-else' term (or formula)
     */
    public Term createIfExThenElseTerm(ArrayOfQuantifiableVariable exVars,
                                       Term condF, Term thenT, Term elseT) {
        return new TermImpl ( IfExThenElse.IF_EX_THEN_ELSE,
                               new ArrayOfTerm( new Term [] { condF, thenT, elseT }),
                               JavaBlock.EMPTY_JAVABLOCK,
                               new ArrayOfQuantifiableVariable[]{exVars,
                                                                 exVars,
                                                                 new ArrayOfQuantifiableVariable()} ).checked();
    }
    


    /** some methods to handle the equality for formulas (equiv - operator) ... */
    public Term createJunctorTerm(Equality op, Term t1, Term t2) {
	return createEqualityTerm(op, new Term[]{t1, t2});
    }
    
    public Term createJunctorTerm(Equality op, Term[] subTerms) {
	return createEqualityTerm(op, subTerms);
    }

    public Term createJunctorTermAndSimplify(Equality op, Term t1, Term t2) {
        if ( op == Equality.EQV ) {
            if ( t1.op () == Junctor.TRUE ) return t2;
            if ( t2.op () == Junctor.TRUE ) return t1;
            if ( t1.op () == Junctor.FALSE )
                return createJunctorTermAndSimplify ( Junctor.NOT, t2 );
            if ( t2.op () == Junctor.FALSE )
                return createJunctorTermAndSimplify ( Junctor.NOT, t1 );
            if ( t1.equals ( t2 ) ) return createJunctorTerm ( Junctor.TRUE );
        }
        return createEqualityTerm ( op, new Term [] { t1, t2 } );
    }

    public Term createJunctorTerm(Junctor op) {     
	return createJunctorTerm(op, NO_SUBTERMS);
    }

    public Term createJunctorTerm(Junctor op, Term t1) {
	return createJunctorTerm(op, new Term[]{t1});
    } 

    public Term createJunctorTerm(Junctor op, Term t1, Term t2) {
	return createJunctorTerm(op, new Term[]{t1, t2});
    }
 
    

    /** creates a JunctorTerm with top operator op, some subterms
     * @param op Operator at the top of the termstructure that starts
     * here
     * @param subTerms an array containing subTerms (an array with length 0 if
     * there are no more subterms */
    public Term createJunctorTerm(Junctor op, Term[] subTerms) {
	if (op==null) throw new IllegalArgumentException("null-Operator at"+
							 "TermFactory");
	return new TermImpl(op, subTerms).checked();
    }
    
    /** some methods for the creation of junctor terms with automatically performed simplification
     * like  ( b /\ true ) == (b) ...
     * Currently only the AND, OR, IMP Operators will be simplified (if possible)
     */
    public Term createJunctorTermAndSimplify(Junctor op, Term t1) {
        if (op == Junctor.NOT) {
            if (t1.op() == Junctor.TRUE) {
                return createJunctorTerm(Junctor.FALSE);
            } else if (t1.op() == Junctor.FALSE) {
                return createJunctorTerm(Junctor.TRUE);
            } else if (t1.op() == Junctor.NOT) {
		return t1.sub(0);
	    }
        }
        return createJunctorTerm(op, t1);
    }

    /** some methods for the creation of junctor terms with automatically performed simplification
     * like  ( b /\ true ) == (b) ...
     * Currently only the AND, OR, IMP Operators will be simplified (if possible)
     */
    public Term createJunctorTermAndSimplify(Junctor op, Term t1, Term t2) {
	if (op == Junctor.AND) {
	    // if one of the terms is false the expression is false as a whole
	    if (t1.op() == Junctor.FALSE || t2.op() == Junctor.FALSE)
	        return createJunctorTerm(Junctor.FALSE);
	    // if one of the terms is true skip the subterm.
	    if (t1.op() == Junctor.TRUE) {
		return  t2;
	    } else if(t2.op() == Junctor.TRUE) {
		return t1;
	    } else { // nothing to simplifiy ...
		return createJunctorTerm(op, t1, t2);
	    }
	} else if (op == Junctor.OR) {
	    // if one of the terms is true the expression is true as a whole
	    if (t1.op() == Junctor.TRUE || t2.op() == Junctor.TRUE)
		return createJunctorTerm(Junctor.TRUE);
	    // if one of the terms is false skip the subterm.
	    if (t1.op() == Junctor.FALSE) {
		return t2;
	    } else if(t2.op() == Junctor.FALSE) {
		return t1;
	    } else { // nothing to simplifiy ...
		return createJunctorTerm(op, t1, t2);
	    } 
	} else if (op == Junctor.IMP) {
	    if (t1.op() == Junctor.FALSE || t2.op() == Junctor.TRUE)
		// then the expression is true as a whole
		return createJunctorTerm(Junctor.TRUE);
	    // if t1 is true or t2 is false skip that subterm.
	    if (t1.op() == Junctor.TRUE) {
		return t2;
	    } else if(t2.op() == Junctor.FALSE) {
		return createJunctorTermAndSimplify(Junctor.NOT, t1);
	    } else { // nothing to simplifiy ...
		return createJunctorTerm(op, t1, t2);
	    }
        } else { // all other Junctors ...
	    return createJunctorTerm(op, t1, t2);
	}
    }

     /** creates a OpTerm with a meta operator as top operator. These
     * terms are only used in the replacewith areas of taclets. And are
     * replaced by the SyntacticalReplaceVisitor
     * @param op Operator at the top of the termstructure that starts
     * here
     * @param subTerms an array containing subTerms (an array with length 0 if
     * there are no more subterms
     */
    public Term createMetaTerm(MetaOperator op, Term[] subTerms) {
	if (op==null) throw new IllegalArgumentException("null-Operator at"+
							 "TermFactory");    	
	return new TermImpl(op, subTerms).checked();
    }

    public Term createProgramTerm(Operator op, 
            JavaBlock javaBlock, 
            Term subTerm) {
	return new TermImpl(op, 
			    new ArrayOfTerm(subTerm), 
			    javaBlock, 
			    new ArrayOfQuantifiableVariable()).checked();
    }


    public Term createProgramTerm(Operator op, 
            JavaBlock javaBlock, 
            Term[] subTerms) {
	return new TermImpl(op, 
		            new ArrayOfTerm(subTerms), 
		            javaBlock, 
		            new ArrayOfQuantifiableVariable()).checked();
    }



    /**
     * creates a quantifier term 
     * @param quant the Quantifier (all, exist) which binds the
     * variables in <code>varsBoundHere</code> 
     * @param varsBoundHere an array of QuantifiableVariable
     * containing all variables bound by the quantifier
     * @param subTerm the Term where the variables are bound
     * @return the quantified term
     */
    public Term createQuantifierTerm(Quantifier quant,
				     ArrayOfQuantifiableVariable varsBoundHere, 
				     Term subTerm) {
	if (varsBoundHere.size()<=1) {
	    return new TermImpl(quant, 
		    		new ArrayOfTerm(subTerm),
		    		JavaBlock.EMPTY_JAVABLOCK,
		    		varsBoundHere).checked();
	} else {
	    Term qt = subTerm;
	    for (int i=varsBoundHere.size()-1; i>=0; i--) {
		QuantifiableVariable qv 
		    = varsBoundHere.getQuantifiableVariable(i);
		qt = createQuantifierTerm(quant, qv, qt);
	    }
	    return qt;
	}	
    }



    /**
     * creates a quantifier term 
     * @param quant Operator representing the
     * Quantifier (all, exist) of this term 
     * @param var a QuantifiableVariable representing the only bound
     * variable of this quantifier.
     */
    public Term createQuantifierTerm(Quantifier quant,
				     QuantifiableVariable var, 
				     Term subTerm) {
       return createQuantifierTerm
	   (quant, new QuantifiableVariable[]{var}, subTerm);
    }


    /**
     * creates a quantifier term 
     * @param quant Operator representing the
     * Quantifier (all, exist) of this term 
     * @param varsBoundHere an
     * array of QuantifiableVariable containing all variables bound by the
     * quantifier
     */
    public Term createQuantifierTerm(Quantifier quant, 
				     QuantifiableVariable[] varsBoundHere, 
				     Term subTerm) {
	return createQuantifierTerm(quant, new ArrayOfQuantifiableVariable
	    (varsBoundHere), subTerm);
    }


     /** creates a substitution term
     * @param substVar the QuantifiableVariable to be substituted
     * @param substTerm the Term that replaces substVar
     * @param origTerm the Term that is substituted
     */
    public Term createSubstitutionTerm
	(SubstOp op, QuantifiableVariable substVar, 
	 Term substTerm, Term origTerm) {
	return new TermImpl
	    (op, 
	     new ArrayOfTerm(new Term[]{substTerm, origTerm}), 
	     JavaBlock.EMPTY_JAVABLOCK, 
	     new ArrayOfQuantifiableVariable[]{new ArrayOfQuantifiableVariable(), 
		                               new ArrayOfQuantifiableVariable(substVar)}).checked();
    }
    

     /** creates a substitution term
      * @param op the replacement variable
      * @param substVar the QuantifiableVariable to be substituted
      * @param subs an array of Term where subs[0] is the term that
      * replaces the variable substVar in subs[1] 
      */
    public Term createSubstitutionTerm(SubstOp op,
            QuantifiableVariable substVar, Term[] subs) {
	return new TermImpl
	    (op, 
             new ArrayOfTerm(subs), 
             JavaBlock.EMPTY_JAVABLOCK, 
             new ArrayOfQuantifiableVariable[]{new ArrayOfQuantifiableVariable(), 
		                               new ArrayOfQuantifiableVariable(substVar)}).checked();
    }


    /**
     * helper method for term creation (reduces duplicate code)
     */
    private Term createTermWithNoBoundVariables(Operator op, 
						Term[] subTerms, 
						JavaBlock javaBlock) {
	if (op instanceof Equality) {
	    return createEqualityTerm(subTerms);
	} else if (op instanceof SortedOperator) {
	    return createFunctionTerm(op, subTerms);
	} else if (op instanceof Junctor) {
	    return createJunctorTerm((Junctor)op,subTerms);
	} else if (op instanceof Modality) {
	    return createProgramTerm((Modality)op, javaBlock, subTerms[0]); 
	} else if (op instanceof IfThenElse) {
	    return createIfThenElseTerm ( subTerms[0], subTerms[1], subTerms[2] );
	} else if (op instanceof MetaOperator) {
	    return createMetaTerm((MetaOperator)op, subTerms);
	} else if (op instanceof UpdateApplication) {
	    return createFunctionTerm(op, subTerms);
	} else {
	    de.uka.ilkd.key.util.Debug.fail("Should never be"+
					    " reached. Missing case for class", 
					    op.getClass());
	    return null;
	}       	
    }

  


   public Term createTerm(Operator op, Term[] subTerms, 
			  ArrayOfQuantifiableVariable[] bv,
			  JavaBlock javaBlock) {
	if (op==null) {
	    throw new IllegalArgumentException("null-Operator at TermFactory");
	} else if (op instanceof Quantifier) {
	    return createQuantifierTerm((Quantifier)op, bv[0], subTerms[0]);
        } else if(op instanceof NumericalQuantifier){
	    if(bv[0].size()!=1 || bv[1].size() != 1) {
                throw new RuntimeException();
	    }
	    final Term[] resTerms = new Term [2];
	    System.arraycopy ( subTerms, 0, resTerms, 0, 2 );
	    final ArrayOfQuantifiableVariable exVars =
		BoundVariableTools.DEFAULT.unifyBoundVariables (bv, resTerms, 
								0, 1);
	    return createNumericalQuantifierTerm((NumericalQuantifier) op, 
						 resTerms[0],
						 resTerms[1], 
						 exVars); 
	} else if(op instanceof BoundedNumericalQuantifier){
            if(bv[2].size()!=1) throw new RuntimeException();
            return createBoundedNumericalQuantifierTerm(
		    (BoundedNumericalQuantifier) op, subTerms[0], 
                    subTerms[1], subTerms[2], bv[2]);
        } else if (op instanceof IfExThenElse) {
	    final Term[] resTerms = new Term [3];
            System.arraycopy ( subTerms, 0, resTerms, 0, 3 );
	    final ArrayOfQuantifiableVariable exVars =
	        BoundVariableTools.DEFAULT.unifyBoundVariables ( bv, resTerms,
	                                                         0, 2 );
	    return createIfExThenElseTerm ( exVars,
                                            resTerms[0],
                                            resTerms[1],
                                            resTerms[2] );
	} else if (op instanceof SubstOp) {	    
	    return createSubstitutionTerm((SubstOp)op, 
					  bv[1].getQuantifiableVariable(0),
					  subTerms);
	} else if (op instanceof SortedOperator) { 
	    // special treatment for OCL operators binding variables	    
	    return createFunctionWithBoundVarsTerm(op, subTerms, bv);	 
	} else {
	    return createTermWithNoBoundVariables(op, subTerms, javaBlock);
	}       
   }

    public Term createNumericalQuantifierTerm(NumericalQuantifier op, 
            Term cond, Term t, ArrayOfQuantifiableVariable va){
        return new TermImpl(op, new ArrayOfTerm(new Term[]{cond, t}), JavaBlock.EMPTY_JAVABLOCK, va).checked();
    }
    
    public Term createBoundedNumericalQuantifierTerm(BoundedNumericalQuantifier op, 
            Term a, Term b, Term t, ArrayOfQuantifiableVariable va){
        return new TermImpl(op, 
        		    new ArrayOfTerm(new Term[]{a, b, t}), 
        		    JavaBlock.EMPTY_JAVABLOCK, 
        		    new ArrayOfQuantifiableVariable[]{new ArrayOfQuantifiableVariable(),
            						      new ArrayOfQuantifiableVariable(),
            						      va}).checked();
    } 

    /** 
     * creates a term consisting of the given variable.
     * @param v the variable
     */
    public Term createVariableTerm(LogicVariable v) {
        Term varTerm = cache.get(v);
        if (varTerm == null) {
            varTerm = new TermImpl(v).checked();
            cache.put(v, varTerm);
        }
        return varTerm;
    }

    /**
     * creates a variable term representing the given programvariable
     * @param v the ProgramVariable to be represented
     * @return variable <code>v</code> as term 
     */
    public Term createVariableTerm(ProgramVariable v) {
        Term varTerm = cache.get(v);
        if (varTerm == null) {
            varTerm = new TermImpl(v).checked();
            cache.put(v, varTerm);
        }
        return varTerm;
    }

    /**
     * creates a term with schemavariable <code>v</code> as top level operator     
     * @param v the SchemaVariable to be represented
     * @return the term <code>v</code>
     */
    public Term createVariableTerm(SchemaVariable v) {
        Term varTerm = cache.get(v);
        if (varTerm == null) {
            varTerm = new TermImpl(v).checked();
            cache.put(v, varTerm);
        } 
        return varTerm;
    }


    /** creates a cast of term with to the given sort */    
    public Term createCastTerm(AbstractSort sort, Term with) {        
        return createFunctionTerm(sort.getCastSymbol(), with);
    }
    
    
    public static void clearCache(){
        cache.clear();
    }
}
