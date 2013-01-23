// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.logic.op;

import java.util.HashMap;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.ldt.IntegerLDT;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.logic.sort.SortImpl;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.rule.metaconstruct.*;
import de.uka.ilkd.key.rule.metaconstruct.arith.*;
import de.uka.ilkd.key.util.Debug;


/**
 * Abstract class factoring out commonalities of typical term transformer implementations. 
 * The available singletons of term transformers are kept here.
 */
public abstract class AbstractTermTransformer extends AbstractSortedOperator 
                                           implements TermTransformer {

    private static final HashMap<String, AbstractTermTransformer> name2metaop 
    	= new HashMap<String, AbstractTermTransformer>(70);
    
    //must be first
    public static final Sort METASORT = new SortImpl(new Name("Meta"));    
    
    public static final AbstractTermTransformer META_ADD = new MetaAdd();

    public static final AbstractTermTransformer META_SUB = new MetaSub();

    public static final AbstractTermTransformer META_MUL = new MetaMul();

    public static final AbstractTermTransformer META_DIV = new MetaDiv();

    public static final AbstractTermTransformer META_LESS = new MetaLess();

    public static final AbstractTermTransformer META_GREATER = new MetaGreater();

    public static final AbstractTermTransformer META_LEQ = new MetaLeq();

    public static final AbstractTermTransformer META_GEQ = new MetaGeq();

    public static final AbstractTermTransformer META_EQ = new MetaEqual();

    public static final AbstractTermTransformer META_INT_AND = new MetaJavaIntAnd();

    public static final AbstractTermTransformer META_INT_OR = new MetaJavaIntOr();

    public static final AbstractTermTransformer META_INT_XOR = new MetaJavaIntXor();

    public static final AbstractTermTransformer META_INT_SHIFTRIGHT = new MetaJavaIntShiftRight();

    public static final AbstractTermTransformer META_INT_SHIFTLEFT = new MetaJavaIntShiftLeft();

    public static final AbstractTermTransformer META_INT_UNSIGNEDSHIFTRIGHT = new MetaJavaIntUnsignedShiftRight();

    public static final AbstractTermTransformer META_LONG_AND = new MetaJavaLongAnd();

    public static final AbstractTermTransformer META_LONG_OR = new MetaJavaLongOr();

    public static final AbstractTermTransformer META_LONG_XOR = new MetaJavaLongXor();

    public static final AbstractTermTransformer META_LONG_SHIFTRIGHT = new MetaJavaLongShiftRight();

    public static final AbstractTermTransformer META_LONG_SHIFTLEFT = new MetaJavaLongShiftLeft();

    public static final AbstractTermTransformer META_LONG_UNSIGNEDSHIFTRIGHT = new MetaJavaLongUnsignedShiftRight();

    public static final AbstractTermTransformer WHILE_INV_RULE = new WhileInvRule();
    
    public static final AbstractTermTransformer ENHANCEDFOR_INV_RULE = new EnhancedForInvRule();

    public static final AbstractTermTransformer ARRAY_BASE_INSTANCE_OF = new ArrayBaseInstanceOf();

    public static final AbstractTermTransformer CONSTANT_VALUE = new ConstantValue();
    
    public static final AbstractTermTransformer ENUM_CONSTANT_VALUE = new EnumConstantValue();
    
    public static final AbstractTermTransformer DIVIDE_MONOMIALS = new DivideMonomials ();

    public static final AbstractTermTransformer DIVIDE_LCR_MONOMIALS = new DivideLCRMonomials ();

    public static final AbstractTermTransformer INTRODUCE_ATPRE_DEFINITIONS = new IntroAtPreDefsOp();
    
    public static final AbstractTermTransformer MEMBER_PV_TO_FIELD = new MemberPVToField();

    public static final AbstractTermTransformer ADD_CAST = new AddCast();    

    public static final AbstractTermTransformer EXPAND_QUERIES = new ExpandQueriesMetaConstruct();
    
    protected static final TermFactory termFactory = TermFactory.DEFAULT;
    protected static final TermBuilder TB = TermBuilder.DF;
    
    
    private static Sort[] createMetaSortArray(int arity) {
	Sort[] result = new Sort[arity];
	for(int i = 0; i < arity; i++) {
	    result[i] = METASORT;
	}
	return result;
    }
    
    
    protected AbstractTermTransformer(Name name, int arity, Sort sort) {
	super(name, createMetaSortArray(arity), sort, false);
	name2metaop.put(name.toString(), this);	
    }
    
    
   protected AbstractTermTransformer(Name name, int arity) {
	this(name, arity, METASORT);
    }


    
    public static TermTransformer name2metaop(String s) {
	return name2metaop.get(s);
    }


    /** @return String representing a logical integer literal 
     *  in decimal representation
     */
    public static String convertToDecimalString(Term term, Services services) {
      	StringBuilder result = new StringBuilder();
	boolean neg = false;
	
	Operator top = term.op();
	IntegerLDT intModel = services.getTypeConverter().getIntegerLDT();	    
	final Operator numbers = intModel.getNumberSymbol();
	final Operator base    = intModel.getNumberTerminator();
	final Operator minus   = intModel.getNegativeNumberSign();
	// check whether term is really a "literal"
	
	if (top != numbers) {
	    Debug.out("abstractmetaoperator: Cannot convert to number:", term);
	    throw (new NumberFormatException());
	}
	
	term = term.sub(0);
	top = term.op();

	while (top == minus) {
	    neg=!neg;
	    term = term.sub(0);
	    top = term.op();
	}

	while (top != base) {
	    result.insert(0, top.name());
	    term = term.sub(0);
	    top = term.op();
	}
	
	if (neg) {
	    result.insert(0,"-");
	}
	
	return result.toString();
    }
            
    @Override    
    public MatchConditions match(SVSubstitute subst, MatchConditions mc,
            Services services) {
	// by default meta operators do not match anything 	
        return null;
    }    
}
