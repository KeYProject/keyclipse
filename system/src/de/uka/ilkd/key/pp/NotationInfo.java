// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.pp;

import java.util.HashMap;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.ldt.CharListLDT;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.ldt.IntegerLDT;
import de.uka.ilkd.key.ldt.LocSetLDT;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.Sort;


/** 
 * <p> 
 * Stores the mapping from operators to {@link Notation}s.  Each 
 * {@link Notation} represents the concrete syntax for some 
 * {@link de.uka.ilkd.key.logic.op.Operator}.  The {@link LogicPrinter}
 * asks the NotationInfo to find out which Notation to use for a given term.
 * <p>
 * The Notation associated with an operator might change.  New Notations can
 * be added.
 * 
 * <p>
 * The next lines describe a general rule how to determine priorities and 
 * associativities:
 * 
 *  One thing we need to know from the pretty printer:
 *  Given a term <tt>t</tt> containg <tt>s</tt> as proper subterm. 
 *  Then <tt>s</tt> is printed in parentheses when the priority of the
 *  top level symbol of <tt>s</tt> is strict less than the associativity of the 
 *  position where <tt>s</tt> occurs. For example:
 *  <p>
 *   Let the priority of <tt>AND</tt> be <tt>30</tt> and the associativities for each 
 * of its subterms be 40; <tt>OR</tt>s priority is <tt>20</tt> and the associativites are 
 * both <tt>30</tt> then 
 *     <ul> <li> formula <tt>(p & q) | r</tt> is pretty printed as <tt>p & q | r</tt>
 *         as the priority of & is 30 which is (greater or) equal than the 
 *         associativity of <tt>OR</tt>s left subterm which is 30.</li>
 *         <li> In contrast the formula <tt>p & (q | r)</tt> is pretty printed as 
 *         <tt>p & (q | r)</tt> as the priority of <tt>OR</tt> is 20 which is less than 
 *         the associativity of <tt>AND</tt>s left subterm, which is 40.</li>
 *     </ul> 
 *         
 * A general rule to determine the correct priority and associativity is to use: 
 *  
 *  Grammar rules whose derivation delivers a syntactical correct logic term should follow 
 *  a standard numbering scheme, which is used as indicator for priorities and associativites, 
 *  e.g. 
 *   by simply reading the grammar rule 
 *          <blockquote><tt>term60 ::= term70 (IMP term70)?</tt></blockquote> 
 *   we get the priority of <tt>IMP</tt>, which is <tt>60</tt>. The associativities 
 *   of <tt>IMP</tt>s subterms are not much more difficult to determine, namely    
 *   the left subterm has associativity <tt>70</tt> and in this case its the same 
 *   for the right subterm (<tt>70</tt>).
 *  <p>
 *  There are exceptional cases for
 *  <ul>
 *  <li> <em>infix function</em> symbols that are left associative e.g. 
 *  <code>-, +</code>
 *     <blockquote> 
 *         <tt> term90 ::= term100 (PLUS term100)* </tt>
 *     </blockquote>           
 * then the associative for the right subterm is increased by <tt>1</tt>, 
 * i.e. here we have a priority of <tt>90</tt> for <tt>PLUS</tt> as infix operator, 
 * a left associativity of <tt>100</tt> <em>and</em> a right associativity of <tt>101</tt>
 * </li>
 * <li> update and substituition terms: for them their associativity is 
 * determined dynamically by the pretty printer depending if it is applied on a 
 * formula or term. In principal there should be two different
 * rules in the parser as then we could reuse the general rule from above, but 
 * there are technical reasons which causes this exception.
 * </li>
 * <li> some very few rules do not follow the usual parser design 
 * e.g. like
 *     <blockquote><tt>R_PRIO ::= SubRule_ASS1 | SubRule_ASS2 </tt></blockquote>
 *   where
 *      <blockquote><tt>SubRule_ASS2 ::= OP SubRule_ASS1</tt></blockquote> 
 * Most of these few rules could in general be rewritten to fit the usual scheme
 * e.g. as
 * <blockquote><tt> R_PRIO ::= (OP)? SubRule_ASS1</tt></blockquote> 
 * using the priorities and associativities of the so rewritten rules 
 * (instead of rewriting them actually) is a way to cope with them.   
 * </li>
 * </ul>
 */
public final class NotationInfo {
    
    public static boolean PRETTY_SYNTAX = true;
        
    
    /** This maps operators and classes of operators to {@link
     * Notation}s.  The idea is that we first look wether the operator has
     * a Notation registered.  Otherwise, we see if there is one for the
     * <em>class</em> of the operator.
     */
    private HashMap<Object, Notation> tbl;

    /**
     * Maps terms to abbreviations and reverse.
     */
    private AbbrevMap scm = new AbbrevMap();
    
    

    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------    

    public NotationInfo() {
    	createDefaultNotationTable();
    }
    
    
    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------     
        
    /** Register the standard set of notations (that can be defined without
     * a services object).
     */
    private void createDefaultNotationTable() {
	tbl = new HashMap<Object,Notation>();
	
	tbl.put(Junctor.TRUE ,new Notation.Constant("\u22A4", 130));
	tbl.put(Junctor.FALSE,new Notation.Constant("\u22A5", 130));
	tbl.put(Junctor.NOT,new Notation.Prefix("¬" ,60,60));
	tbl.put(Junctor.AND,new Notation.Infix("\u2227"  ,50,50,60));
	tbl.put(Junctor.OR, new Notation.Infix("\u2228"  ,40,40,50));
	tbl.put(Junctor.IMP,new Notation.Infix("\u279C" ,30,40,30));
	tbl.put(Equality.EQV,new Notation.Infix("\u2194",20,20,30));
	tbl.put(Quantifier.ALL,new Notation.Quantifier("\u2200", 60, 60));
	tbl.put(Quantifier.EX, new Notation.Quantifier("\u2203", 60, 60));
	tbl.put(Modality.DIA,new Notation.ModalityNotation("\\<","\\>", 60, 60));
	tbl.put(Modality.BOX,new Notation.ModalityNotation("\\[","\\]", 60, 60));
	tbl.put(IfThenElse.IF_THEN_ELSE, new Notation.IfThenElse(130, "\\if"));
	tbl.put(WarySubstOp.SUBST,new Notation.Subst());
	tbl.put(UpdateApplication.UPDATE_APPLICATION, new Notation.UpdateApplicationNotation());
	tbl.put(UpdateJunctor.PARALLEL_UPDATE, new Notation.ParallelUpdateNotation());	
	
	tbl.put(Function.class, new Notation.FunctionNotation());               
	tbl.put(LogicVariable.class, new Notation.VariableNotation());
	tbl.put(LocationVariable.class, new Notation.VariableNotation());
        tbl.put(ProgramConstant.class, new Notation.VariableNotation());
	tbl.put(Equality.class, new Notation.Infix("=", 70, 80, 80)); 
	tbl.put(ElementaryUpdate.class, new Notation.ElementaryUpdateNotation());
	tbl.put(ModalOperatorSV.class, new Notation.ModalSVNotation(60, 60));
	tbl.put(SchemaVariable.class, new Notation.SchemaVariableNotation());
	
	tbl.put(Sort.CAST_NAME, new Notation.CastFunction("(",")",120, 140));
    }
        
    
    /**
     * Adds notations that can only be defined when a services object is 
     * available.
     */
    private void addFancyNotations(Services services) {
	//arithmetic operators
	final IntegerLDT integerLDT 
		= services.getTypeConverter().getIntegerLDT();	
	tbl.put(integerLDT.getNumberSymbol(), new Notation.NumLiteral());
	tbl.put(integerLDT.getCharSymbol(), new Notation.CharLiteral());
	tbl.put(integerLDT.getLessThan(), new Notation.Infix("<", 80, 90, 90));
	tbl.put(integerLDT.getGreaterThan(), new Notation.Infix("> ", 80, 90, 90));
	tbl.put(integerLDT.getLessOrEquals(), new Notation.Infix("\u2264", 80, 90, 90));
	tbl.put(integerLDT.getGreaterOrEquals(), new Notation.Infix("\u2265", 80, 90, 90));
	tbl.put(integerLDT.getSub(), new Notation.Infix("-", 90, 90, 91));
	tbl.put(integerLDT.getAdd(), new Notation.Infix("+", 90, 90, 91));
	tbl.put(integerLDT.getMul(), new Notation.Infix("*", 100, 100, 101));
	tbl.put(integerLDT.getDiv(), new Notation.Infix("/", 100, 100, 101));
	tbl.put(integerLDT.getMod(), new Notation.Infix("%", 100, 100, 101));
	tbl.put(integerLDT.getNeg(),new Notation.Prefix("-", 140, 130));
	tbl.put(integerLDT.getNegativeNumberSign(), new Notation.Prefix("-", 140, 130));
        	
	//heap operators
	final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
	tbl.put(HeapLDT.SELECT_NAME, new Notation.SelectNotation());
	tbl.put(ObserverFunction.class, new Notation.ObserverNotation());
	tbl.put(ProgramMethod.class, new Notation.ObserverNotation());
	tbl.put(heapLDT.getLength(), new Notation.LengthNotation());
	
	//set operators
	final LocSetLDT setLDT = services.getTypeConverter().getLocSetLDT();
	tbl.put(setLDT.getEmpty(), new Notation.Constant("{}", 130));
	tbl.put(setLDT.getSingleton(), new Notation.SingletonNotation());
	tbl.put(setLDT.getUnion(), new Notation.Infix("\u222a", 130, 0, 0));
	tbl.put(setLDT.getIntersect(), new Notation.Infix("\u2229", 130, 0, 0));
	tbl.put(setLDT.getSetMinus(), new Notation.Infix("\u2216", 130, 0, 0));
	tbl.put(setLDT.getElementOf(), new Notation.ElementOfNotation());
	tbl.put(setLDT.getSubset(), new Notation.Infix("\u2286", 130, 0, 0));
	//$mem = \ \u220a\ 
	//$dom_restrict = \ \u22B2\ 
	//$rng_restrict = \ \u22B3\ 
	//$complement = \u2201
	
	//string operators
	final CharListLDT charListLDT 
		= services.getTypeConverter().getCharListLDT();
	tbl.put(charListLDT.getClConcat(), new Notation.Infix("+",120,130,130));
	tbl.put(charListLDT.getClCons(), new CharListNotation());
	tbl.put(charListLDT.getClEmpty(), new Notation.Constant("\"\"",140));
    }
    


    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------
    
    public void refresh(Services services) {
	createDefaultNotationTable();
	if(PRETTY_SYNTAX && services != null) {
	    addFancyNotations(services);
	}
    }    
        
    
    public AbbrevMap getAbbrevMap(){
	return scm;
    }
    

    public void setAbbrevMap(AbbrevMap am){
	scm = am;
    }

    
    /** Get the Notation for a given Operator.  
     * If no notation is registered, a Function notation is returned.
     */
    public Notation getNotation(Operator op, Services services) {
	Notation result = tbl.get(op);
	if(result != null) {
	    return result;
	}
	
	result = tbl.get(op.getClass());
	if(result != null) {
	    return result;
	}
	
	if(op instanceof SchemaVariable) {
	    result = tbl.get(SchemaVariable.class);
	    if(result != null) {
		return result;
	    }
	}
	
	if(op instanceof SortDependingFunction) {
	    result = tbl.get(((SortDependingFunction)op).getKind());
	    if(result != null) {
		return result;
	    }
	}
	
	return new Notation.FunctionNotation();
    }
}