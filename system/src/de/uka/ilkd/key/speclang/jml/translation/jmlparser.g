// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//


/* -*-antlr-*- */
header {
    package de.uka.ilkd.key.speclang.jml.translation;

    import java.io.StringReader;

    import de.uka.ilkd.key.collection.*;
    import de.uka.ilkd.key.java.JavaInfo;
    import de.uka.ilkd.key.java.Position;
    import de.uka.ilkd.key.java.Services;
    import de.uka.ilkd.key.java.abstraction.*;
    import de.uka.ilkd.key.java.declaration.ArrayDeclaration;
    import de.uka.ilkd.key.java.declaration.ClassDeclaration;
    import de.uka.ilkd.key.java.expression.literal.StringLiteral;
    import de.uka.ilkd.key.java.recoderext.ImplicitFieldAdder;
    import de.uka.ilkd.key.ldt.*;
    import de.uka.ilkd.key.logic.*;
    import de.uka.ilkd.key.logic.op.*;
    import de.uka.ilkd.key.logic.sort.*;
    import de.uka.ilkd.key.proof.OpReplacer;
    import de.uka.ilkd.key.speclang.PositionedString;
    import de.uka.ilkd.key.speclang.translation.*;
    import de.uka.ilkd.key.util.Pair;
    import de.uka.ilkd.key.util.Triple;    

    import java.lang.RuntimeException;
    import java.math.BigInteger;
    import java.util.Map;
    import java.util.Iterator;
    import java.util.LinkedHashMap;
}

class KeYJMLParser extends Parser;
options {
    importVocab=KeYJMLLexer;
    k = 2;
    defaultErrorHandler=false;
}

{
    private static final TermBuilder TB = TermBuilder.DF;

    private Services services;
    private JavaInfo javaInfo;
    private IntegerLDT intLDT;
    private HeapLDT heapLDT;
    private LocSetLDT locSetLDT;
    private BooleanLDT booleanLDT;
    private SLTranslationExceptionManager excManager;

    private ProgramVariable selfVar;
    private ImmutableList<ProgramVariable> paramVars;
    private ProgramVariable resultVar;
    private ProgramVariable excVar;
    private Term heapAtPre;
    
    // Helper objects
    private JMLResolverManager resolverManager;
    private JavaIntegerSemanticsHelper intHelper;

    // Helper attributes
    private boolean currentlyParsing = false;

    
    public KeYJMLParser(TokenStream lexer,
		String fileName,
		Position offsetPos,
		Services services,
		KeYJavaType specInClass,
		ProgramVariable self,
		ImmutableList<ProgramVariable> paramVars,
		ProgramVariable result,
		ProgramVariable exc,
		Term heapAtPre) {
	this(lexer);

	// save parameters
	this.services       = services;
	this.javaInfo       = services.getJavaInfo();
	this.intLDT         = services.getTypeConverter().getIntegerLDT();
	this.heapLDT        = services.getTypeConverter().getHeapLDT();
	this.locSetLDT      = services.getTypeConverter().getLocSetLDT();
	this.booleanLDT     = services.getTypeConverter().getBooleanLDT();
	this.excManager     = new SLTranslationExceptionManager(this,
				    				fileName, 
				    				offsetPos);
	this.selfVar	    = self;
	this.paramVars      = paramVars;
	this.resultVar      = result;
	this.excVar	    = exc;
	this.heapAtPre      = heapAtPre;

	// initialize helper objects
	this.resolverManager = new JMLResolverManager(this.javaInfo,
						      specInClass,
						      selfVar,
						      this.excManager);

	// initialize namespaces
	resolverManager.pushLocalVariablesNamespace();
	if(paramVars != null) {
	    resolverManager.putIntoTopLocalVariablesNamespace(paramVars);
	}
	if(resultVar != null) {
	    resolverManager.putIntoTopLocalVariablesNamespace(resultVar);
	}

	intHelper = new JavaIntegerSemanticsHelper(services, excManager);
    }
    
    
    public KeYJMLParser(PositionedString ps,
		Services services,
		KeYJavaType specInClass,
		ProgramVariable self,
		ImmutableList<ProgramVariable> paramVars,
		ProgramVariable result,
		ProgramVariable exc,
		Term heapAtPre) {
	this(new KeYJMLLexer(new StringReader(ps.text)), 
	     ps.fileName, 
	     ps.pos,
	     services,
	     specInClass,
	     self,
	     paramVars,
	     result,
	     exc,
	     heapAtPre);
    }


    private void raiseError(String msg) throws SLTranslationException {
	throw excManager.createException(msg);
    }
    
    
    private void raiseError(String msg, Token t) throws SLTranslationException {
	throw excManager.createException(msg, t);
    }
    
    
    private void raiseNotSupported(String feature) 
	    throws SLTranslationException {
	throw excManager.createWarningException(feature + " not supported"); 
    }
	

    public Term parseExpression() throws SLTranslationException {
    
	Term result = null;
	this.currentlyParsing = true;

	try {
	    result = expression().getTerm();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}

	this.currentlyParsing = false;

	return convertToFormula(result);
    }


    public Term parseSignals() throws SLTranslationException {

	Term result = null;
	this.currentlyParsing = true;

	try {
	    result = signalsclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}

	this.currentlyParsing = false;

	return convertToFormula(result);
    }


    public Term parseSignalsOnly() throws SLTranslationException {

	ImmutableList<KeYJavaType> signalsonly = null;
	this.currentlyParsing = true;

	try {
	    signalsonly = signalsonlyclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}

	this.currentlyParsing = false;

	// Build appropriate term out of the parsed list of types
	// i.e. disjunction of "excVar instanceof ExcType"
	// for every ExcType in the list
	Term result = TB.ff();

	Iterator<KeYJavaType> it = signalsonly.iterator();
	while (it.hasNext()) {
	    KeYJavaType kjt = it.next();
		Function instance
			= kjt.getSort().getInstanceofSymbol(services);
	    result = TB.or( result,
		TB.equals(
		    TB.func(instance, TB.var(this.excVar)),
		    TB.TRUE(services)));
	}

	return result;
    }


    public Term parseAssignable() throws SLTranslationException {
    	Term result = null;

	this.currentlyParsing = true;
	try {
	    result = assignableclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	return result;
    }


    public ImmutableList<Term> parseSecureFor() throws SLTranslationException {
    	ImmutableList<Term> result = ImmutableSLList.<Term>nil();

	this.currentlyParsing = true;
	try {
	    result = secureforclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	return result;
    }


    public ImmutableList<Term> parseDeclassify() throws SLTranslationException {
    	ImmutableList<Term> result = ImmutableSLList.<Term>nil();

	this.currentlyParsing = true;
	try {
	    result = declassifyclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	return result;
    }


    public ImmutableList<Term> parseDeclassifyVar() throws SLTranslationException {
    	ImmutableList<Term> result = ImmutableSLList.<Term>nil();

	this.currentlyParsing = true;
	try {
	    result = declassifyvarclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	return result;
    }

    
    public Pair<ObserverFunction,Term> parseRepresents() throws SLTranslationException {
    	Pair<ObserverFunction,Term> result = null;

	this.currentlyParsing = true;
	try {
	    result = representsclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	return result;
    }    
    
    
    public Triple<ObserverFunction,Term,Term> parseDepends() throws SLTranslationException {
    	Triple<ObserverFunction,Term,Term> result = null;

	this.currentlyParsing = true;
	try {
	    result = dependsclause();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	return result;
    }    


    public ImmutableList<ProgramVariable> parseVariableDeclaration() throws SLTranslationException {

	Pair<KeYJavaType,ImmutableList<LogicVariable>> vars;

	this.currentlyParsing = true;
	try {
	    vars = quantifiedvardecls();
	} catch (antlr.ANTLRException e) {
	    throw excManager.convertException(e);
	}
	this.currentlyParsing = false;

	ImmutableList<ProgramVariable> result = ImmutableSLList.<ProgramVariable>nil();
	for(LogicVariable lv : vars.second) {
	    ProgramVariable pv 
	    	= new LocationVariable(new ProgramElementName(
	    	                           lv.name().toString()), 
	                               vars.first);
	    result = result.append(pv);
	}
	return result;
    }



    /**
     * Extracts a term's subterms as an array.
     */
    private Term[] getSubTerms(Term term) {
	Term[] result = new Term[term.arity()];
	for(int i = 0; i < term.arity(); i++) {
	    result[i] = term.sub(i);
	    assert result[i] != null;
	}
	return result;
    }


    /**
     * Extracts the sorts from an array of terms as an array.
     */
    private Sort[] getSorts(Term[] terms) {
	Sort[] result = new Sort[terms.length];
	for(int i = 0; i < terms.length; i++) {
	    result[i] = terms[i].sort();
	}
	return result;
    }

    /**
     * Converts a term so that all of its non-rigid operators refer to the pre-state.
     */
    private Term convertToOld(Term term) {
	assert heapAtPre != null;
	Map map = new LinkedHashMap();
	map.put(TB.heap(services), heapAtPre);
	OpReplacer or = new OpReplacer(map);
	return or.replace(term);
    }

    private boolean isBoundedSum(Term a, LogicVariable lv){
        return lowerBound(a,lv)!=null && upperBound(a,lv)!=null;
    }
    
    private Term lowerBound(Term a, LogicVariable lv){
        if(a.arity()>0 && a.sub(0).op()==Junctor.AND){
            a=a.sub(0);
        }
        if(a.arity()==2 && a.op()==Junctor.AND && a.sub(0).arity()==2 && a.sub(0).sub(1).op()==lv
                && a.sub(0).op().equals(intLDT.getLessOrEquals())){
            return a.sub(0).sub(0);
        }
        return null;
    }
   
    private Term upperBound(Term a, LogicVariable lv){
        if(a.arity()>0 && a.sub(0).op()==Junctor.AND){
            a=a.sub(0);
        }   
        if(a.arity()==2 && a.op()==Junctor.AND && a.sub(1).arity()==2 && a.sub(1).sub(0).op()==lv
                && a.sub(1).op().equals(intLDT.getLessThan())){
            return a.sub(1).sub(1);
        }
        return null;
    }


    private String createSignatureString(ImmutableList<SLExpression> signature) {
	if (signature == null || signature.isEmpty()) {
	    return "";
	}
	String sigString = "";
	
	for(SLExpression expr : signature) {
	    sigString += expr.getType().getFullName() + ", ";
	}
	
	return sigString.substring(0, sigString.length() - 2);
    }
    
    
    private SLExpression lookupIdentifier(String lookupName,
					  SLExpression receiver,
					  SLParameters params,
					  Token t)
				       throws SLTranslationException {

	// Identifier with suffix in parantheses? Probably a method call
	// parse in the parameter list and call again
	try {
	    if (LA(1) == LPAREN) {
	    	return receiver;
	    }
	} catch (TokenStreamException e) {
	    if(currentlyParsing) {
		raiseError("internal Error: no further Token in Stream");
	    }
	}

	SLExpression result = resolverManager.resolve(receiver,
						      lookupName,
						      params);
	if(result != null) {
	    return result;
	}
    
	// no identifier found, maybe it was just a package prefix.
	// but package prefixes don't have a receiver!
	// Let primarysuffix handle faulty method call.
	if (receiver != null && params == null) {
	    raiseError("Identifier " + lookupName + " not found: " + 
	               lookupName);
	}
	
	return null;
    }


    // If a is a boolean literal, the method returns the literal as a Formula.
    private Term convertToFormula(Term a) {
	if(a.sort() == booleanLDT.targetSort()) {
	    return TB.equals(a, TB.TRUE(services));
	}

	return a;
    }

    private SLExpression buildEqualityTerm(SLExpression a, SLExpression b)
	throws SLTranslationException {
    
	if (a.isTerm() && b.isTerm()) {
	    return new SLExpression(buildEqualityTerm(a.getTerm(),  
	                                              b.getTerm()));
	}
	
	if (a.isType() && b.isType()) {
	    SLExpression typeofExpr;
	    SLExpression typeExpr;
	    if(a.getTerm() != null) {
		typeofExpr = a;
		typeExpr = b;
	    } else {
		if (b.getTerm() == null) {
		    raiseError("Type equality only supported for expressions " +
			" of shape \"\\typeof(term) == \\type(Typename)\"");
		}
		typeofExpr = b;
		typeExpr = a;
	    }
	    
	    Sort os = typeExpr.getType().getSort();
	    Function ioFunc = os.getExactInstanceofSymbol(services);
	     
	    return new SLExpression(TB.equals(
		TB.func(ioFunc, typeofExpr.getTerm()),
		TB.TRUE(services)));
	}
	
	return null;
    }
    
    
    private Term buildEqualityTerm(Term a, Term b) throws SLTranslationException {

	Term result = null;
	try {
	    if(a.sort() != Sort.FORMULA && b.sort() != Sort.FORMULA) {
		result = TB.equals(a,b);
	    } else {
		result = TB.equals(convertToFormula(a), convertToFormula(b));
	    }
	} catch (IllegalArgumentException e) {
	    try {
		raiseError("Illegal Arguments in equality expression near " + LT(0));
	    } catch (TokenStreamException tse) {
		System.err.println("No further Token in stream");
		raiseError("Illegal Arguments in equality expression");
	    }
	} catch (TermCreationException e) {
	    raiseError("Error in equality-expression\n" + a.toString() + " == " + b.toString() + ".");
	}

	return result;
    }
    
    
    private Term getFields(Term t) throws SLTranslationException {
        if(t.op().equals(locSetLDT.getUnion())) {
            final Term sub0 = getFields(t.sub(0));
            final Term sub1 = getFields(t.sub(1));
            return TB.union(services, sub0, sub1);
        } else if(t.op().equals(locSetLDT.getSingleton())) {
	    return TB.allObjects(services, t.sub(1));
        } else {
            raiseError("Inacceptable field expression: " + t);
            return null;
        }
    }
}


top throws SLTranslationException
{
    SLExpression expr;
}
:
    expr=expression
    { 
	assert false : "Should be never entered. Only workaround of an antlr bug."; 
    }
    ;
    

assignableclause returns [Term result = null] throws SLTranslationException
:
    result=storereflist
    ;


storereflist returns [Term result = null] throws SLTranslationException
{
    Term mod = null;
}
:
    result=storeref
	(COMMA mod=storeref { result = TB.union(services, result, mod); } )*
    ;


secureforclause returns  [ImmutableList<Term> result = ImmutableSLList.<Term>nil()] throws SLTranslationException
:
    result = secureforarglist
    ;


secureforarglist returns  [ImmutableList<Term> result = ImmutableSLList.<Term>nil()] throws SLTranslationException
{
    Term term = null;
}
:
    NOTHING
    |   term = storeref { result = result.append(term); }
        (COMMA term = storeref { result = result.append(term); })*
    ;


declassifyclause returns  [ImmutableList<Term> result = ImmutableSLList.<Term>nil()] throws SLTranslationException
{
    Term declass = null;
    Term frompart = null;
    Term topart = null;
    Term ifpart = null;
}
:
    declass = predicate { result = result.append(declass); }
    FROM frompart = storereflist { result = result.append(frompart);}
    TO topart = storereflist { result = result.append(topart); }
    (IF ifpart = predicate { result = result.append(ifpart); })*
    ;


declassifyvarclause returns  [ImmutableList<Term> result = ImmutableSLList.<Term>nil()] throws SLTranslationException
{
    SLExpression declass = null;
    Term frompart = null;
    Term topart = null;
    Term ifpart = null;
}
:
    declass = expression { result = result.append(declass.getTerm()); }
    FROM frompart = storereflist { result = result.append(frompart);}
    TO topart = storereflist { result = result.append(topart); }
    (IF ifpart = predicate { result = result.append(ifpart); })*
    ;


storeref returns [Term result = null] throws SLTranslationException
{
    Term s1 = null;
    Term s2 = null;
}
:
	result=storerefexpression
    |   result=storerefkeyword
    ;

storerefexpression returns [Term result = null] throws SLTranslationException
{
    SLExpression expr;
}
:
    expr=expression 
    {
 	    if(expr.isTerm() && expr.getTerm().sort().equals(locSetLDT.targetSort())) {
	    	result = expr.getTerm();
	    } else if(expr.isTerm() && heapLDT.getSortOfSelect(expr.getTerm().op()) != null) {
	        final Term objTerm = expr.getTerm().sub(1);
	        final Term fieldTerm = expr.getTerm().sub(2);
	    	result = TB.singleton(services, objTerm, fieldTerm);
	    } else {
	    	raiseError("Not a valid store-ref expression: " + expr.getTerm());
	    }
    }
    ; exception
        catch [TermCreationException ex] {
	    raiseError(ex.getMessage());
        }


specarrayrefexpr[SLExpression receiver, String fullyQualifiedName, Token lbrack] 
               returns [SLExpression result = null] 
               throws SLTranslationException
{
    SLExpression rangeFrom=null;
    SLExpression rangeTo=null;
}
:
    (
	( rangeFrom=expression (DOTDOT rangeTo=expression)? )
	| MULT
    )
    {
	if(receiver == null) {
	    raiseError("Array \"" + fullyQualifiedName + "\" not found.", lbrack);
	} else if(receiver.isType()) {
	    raiseError("Error in array expression: \"" + fullyQualifiedName +
		    "\" is a type.", lbrack);
	} else if(!(receiver.getType().getJavaType() instanceof ArrayType
	            || receiver.getType().getJavaType().equals(PrimitiveType.JAVA_SEQ))) {
	    raiseError("Cannot access " + receiver.getTerm() + " as an array.");
	}
    
    	//arrays
    	if(receiver.getType().getJavaType() instanceof ArrayType) {
	    if (rangeFrom == null) {
	        // We have a star. A star includes all components of an array even
	        // those out of bounds. This makes proving easier.	    
	        Term t = TB.allFields(services, receiver.getTerm());
    	        result = new SLExpression(t);
	    } else if (rangeTo != null) {
	        // We have "rangeFrom .. rangeTo"
	        Term t = TB.arrayRange(services, 
	    			       receiver.getTerm(), 
	    			       rangeFrom.getTerm(), 
	    			       rangeTo.getTerm());
	        result = new SLExpression(t);
	    } else {
	        // We have a regular array access
	        Term t = TB.dotArr(services, 
	    		           receiver.getTerm(),
	    	    	           rangeFrom.getTerm());
	        ArrayType arrayType = (ArrayType) receiver.getType().getJavaType();
	        KeYJavaType elementType = arrayType.getBaseType().getKeYJavaType();	    		       
                result = new SLExpression(t, elementType);
	    }
	    
	//sequences	
	} else {
	    if(rangeTo != null) {
	        Term t = TB.seqSub(services, 
	                           receiver.getTerm(), 
	                           rangeFrom.getTerm(), 
	                           rangeTo.getTerm());
	        result = new SLExpression(t);
	    } else {
	    	Term t = TB.seqGet(services, 
	    			   Sort.ANY,
	    		           receiver.getTerm(), 
	    		           rangeFrom.getTerm());
	        result = new SLExpression(t);
	    }	
	}
    }
    ;exception
        catch [TermCreationException ex] {
              raiseError(ex.getMessage());
        }


storerefkeyword returns [Term result = null] throws SLTranslationException
{
    KeYJavaType t = null;
}
:
    NOTHING { result = TB.empty(services); }
    | EVERYTHING { result = TB.createdLocs(services); }
    | NOT_SPECIFIED { result = TB.createdLocs(services); }
;


signalsonlyclause returns [ImmutableList<KeYJavaType> result = ImmutableSLList.<KeYJavaType>nil()] throws SLTranslationException
{
    KeYJavaType t=null;
}
:
	NOTHING
    |   t=referencetype { result = result.append(t); } (COMMA t=referencetype { result = result.append(t); })*
    ;
    
signalsclause returns [Term result=null] throws SLTranslationException
{
    KeYJavaType excType = null;
    Term pred = null;
    String vName = null;
    LogicVariable eVar = null;
}
:
	LPAREN excType=referencetype (id:IDENT { vName = id.getText(); })? RPAREN
	{
	    if (vName != null) {
		eVar = new LogicVariable(new Name(vName), excType.getSort());
		resolverManager.pushLocalVariablesNamespace();
		resolverManager.putIntoTopLocalVariablesNamespace(eVar, excType);
	    }
	}
	(result = predornot)?
	{
	    if (vName != null) {
		resolverManager.popLocalVariablesNamespace();
	    }
	    if (result == null) {
		result = TB.tt();
	    } else {
		Map /* Operator -> Operator */ replaceMap = new LinkedHashMap();
		replaceMap.put(eVar, excVar);
		OpReplacer excVarReplacer = new OpReplacer(replaceMap);
		
		Sort os = excType.getSort();
		Function instance = os.getInstanceofSymbol(services);
		
		result = TB.imp(
		    TB.equals(TB.func(instance, TB.var(excVar)), TB.TRUE(services)),
		    convertToFormula(excVarReplacer.replace(result)));
	    }
	}
    ;
    
representsclause returns [Pair<ObserverFunction,Term> result=null] throws SLTranslationException
{
    SLExpression lhs, rhs;
    Term t = null;
}
:
    lhs=expression 
    {
        if(!lhs.isTerm() 
            || !(lhs.getTerm().op() instanceof ObserverFunction)
            || lhs.getTerm().sub(0).op() != heapLDT.getHeap()) {
            raiseError("Represents clause with unexpected lhs: " + lhs);
        } else if(selfVar != null 
                  && ((ObserverFunction)lhs.getTerm().op()).isStatic()) {
            raiseError("Represents clauses for static model fields must be static.");
        }
    } 
    (
        {!lhs.getTerm().sort().equals(locSetLDT.targetSort())}?    
        (
            (LARROW | EQUAL_SINGLE) rhs=expression
            {
                if(!rhs.isTerm()) {
                    raiseError("Represents clause with unexpected rhs: " + rhs);
                }
                Term rhsTerm = rhs.getTerm();
                if(rhsTerm.sort() == Sort.FORMULA) {
                    rhsTerm = TB.ife(rhsTerm, TB.TRUE(services), TB.FALSE(services));
                }
                t = TB.equals(lhs.getTerm(), rhsTerm);
            }
        ) 
        |
        {lhs.getTerm().sort().equals(locSetLDT.targetSort())}?        
        (
            (LARROW | EQUAL_SINGLE) t=storereflist
            {
                t = TB.equals(lhs.getTerm(), t);
            }            
        )
        |
        (
            SUCH_THAT t=predicate
        ) 
    )
    {
        result = new Pair<ObserverFunction,Term>(
                     (ObserverFunction) lhs.getTerm().op(), 
                     t);
    }
    ;
    
    
dependsclause returns [Triple<ObserverFunction,Term,Term> result=null] throws SLTranslationException
{
    SLExpression lhs, mby = null;
    Term rhs;
}
:
    lhs=expression COLON rhs=storereflist (MEASURED_BY mby=expression)? SEMI
    {
        if(!lhs.isTerm() 
            || !(lhs.getTerm().op() instanceof ObserverFunction)
            || lhs.getTerm().sub(0).op() != heapLDT.getHeap()) {
            raiseError("Depends clause with unexpected lhs: " + lhs);
        }
        result = new Triple<ObserverFunction,Term,Term>(
                             (ObserverFunction) lhs.getTerm().op(), 
                             rhs,
                             mby == null ? null : mby.getTerm());
    } 
    ;
    

predornot returns [Term result=null] throws SLTranslationException
:
	result=predicate
    |   NOT_SPECIFIED
    |   SAME
    ;
    
predicate returns [Term result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	expr=expression
	{
	    if(!expr.isTerm() && expr.getTerm().sort() == Sort.FORMULA) {
	        raiseError("Expected a formula: " + expr);
	    } 
	    result = expr.getTerm();
	}
    ;


expression returns [SLExpression result=null] throws SLTranslationException
:
	result=assignmentexpr
	{
	    if(!result.isTerm()) {
	        raiseError("Expected a term: " + result);
	    }
	}
    ;

assignmentexpr returns [SLExpression result=null] throws SLTranslationException
:
	result=conditionalexpr
    ;

	
conditionalexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression a,b;
}
:
	result=equivalenceexpr 
	(
	    QUESTIONMARK a=conditionalexpr COLON b=conditionalexpr
	    {
	    	Term ife = TB.ife(convertToFormula(result.getTerm()), a.getTerm(), b.getTerm());
	    	if(a.getType() != null && a.getType().equals(b.getType())) {
		    result = new SLExpression(ife, a.getType());
		} else {
		    result = new SLExpression(ife);
		}
	    }
	)?
    ;

equivalenceexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=impliesexpr 
	(
	    EQV expr=equivalenceexpr
	    {
		result = buildEqualityTerm(result,expr);
	    } 
	    
	|
	    ANTIV expr=equivalenceexpr
	    {
		result = buildEqualityTerm(result,expr);
		result = new SLExpression(TB.not(result.getTerm()),
		                          result.getType());
	    } 
	    
	)?
    ;
	
impliesexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=logicalorexpr 
	(
	    IMPLIES expr=impliesnonbackwardexpr
	    {
		result = new SLExpression(TB.imp(convertToFormula(result.getTerm()),
		                                 convertToFormula(expr.getTerm())));
	    }
	    
	  |
	    (
		IMPLIESBACKWARD expr=logicalorexpr
		{
		    result = new SLExpression(TB.imp(convertToFormula(expr.getTerm()),
		                                     convertToFormula(result.getTerm())));
		}
	    )+
	)?
;

impliesnonbackwardexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=logicalorexpr
	(
	    IMPLIES expr=impliesnonbackwardexpr
	    {
		result = new SLExpression(TB.imp(convertToFormula(result.getTerm()),
		                                 convertToFormula(expr.getTerm())));
	    }
	)?
;	

logicalorexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=logicalandexpr
	(
	    LOGICALOR expr=logicalorexpr
	    {
		result = new SLExpression(TB.or(convertToFormula(result.getTerm()), 
		                                convertToFormula(expr.getTerm())));
	    }
	)?
;

logicalandexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=inclusiveorexpr
	(
	    LOGICALAND expr=logicalandexpr
	    {
		result = new SLExpression(TB.and(convertToFormula(result.getTerm()), 
		                                 convertToFormula(expr.getTerm())));
	    }
	)?
;


inclusiveorexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=exclusiveorexpr 
	(
	    INCLUSIVEOR expr=inclusiveorexpr
	    {
	       if(intHelper.isIntegerTerm(result)) {
                   result = intHelper.buildPromotedOrExpression(result,expr);
               } else {
                   result = new SLExpression(TB.or(convertToFormula(result.getTerm()), 
                                                   convertToFormula(expr.getTerm())));
               }
	    }
	)?
;


exclusiveorexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=andexpr 
	(
	    XOR expr=exclusiveorexpr
	    {
	       if(intHelper.isIntegerTerm(result)) {
                   result = intHelper.buildPromotedXorExpression(result,expr);
               } else {
                   Term resultFormula = convertToFormula(result.getTerm());
                   Term exprFormula = convertToFormula(expr.getTerm());
                   result = new SLExpression(TB.or(TB.and(resultFormula, TB.not(exprFormula)), 
                                                   TB.and(TB.not(resultFormula), exprFormula)));
               }
	    }
	)?
;


andexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression expr;
}
:
	result=equalityexpr
	{
	    if(!result.isTerm()) {
		raiseError("Found a type where only a term is allowed: " 
			   + result);
	    }
	}
	(
	    AND expr=andexpr
	    { 
	       if(intHelper.isIntegerTerm(result)) {
                   result = intHelper.buildPromotedAndExpression(result, expr);
               } else {
                   result = new SLExpression(TB.and(convertToFormula(result.getTerm()), 
                                                    convertToFormula(expr.getTerm())));
               }
	    }
	)?
;


equalityexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression right;
}
:
	result=relationalexpr 
	(
	    eq: EQUAL right=equalityexpr
	    {
		if (result.isType() != right.isType()) {
		    raiseError("Cannot build equality expression between term " +
			"and type.", eq);
		}
		result = buildEqualityTerm(result, right);
	    }
	|
	    ne: NOTEQUAL right=equalityexpr
	    {
		if (result.isType() != right.isType()) {
		    raiseError("Cannot build equality expression between term " +
			"and type.", ne);
		}
		result = new SLExpression(TB.not(buildEqualityTerm(result, right).getTerm()));
	    }
	)?
;

relationalexpr returns [SLExpression result=null] throws SLTranslationException
{
    Function f = null;
    KeYJavaType type = null;
    SLExpression right = null;
    Token opToken = null;
}
:
	result=shiftexpr
	(
	    lt:LT right=shiftexpr 
	    {
		f = intLDT.getLessThan();
		opToken = lt;
	    }
	|
	    gt:GT right=shiftexpr
	    {
		f = intLDT.getGreaterThan();
		opToken = gt;
	    }
	|
	    leq:LEQ right=shiftexpr
	    {
		f = intLDT.getLessOrEquals();
		opToken = leq;
	    }
	|
	    geq:GEQ right=shiftexpr
	    {
		f = intLDT.getGreaterOrEquals();
		opToken = geq;
	    }
	|
	    io:INSTANCEOF type=typespec 
	    {
		f = type.getSort().getInstanceofSymbol(services);
		opToken = io;
	    }
	|
	    st:ST right=shiftexpr
	    {
		if (result.isTerm() || right.isTerm()) {
		    raiseError("Cannot build subtype expression from terms.", st);
		}
		assert result.isType();
		assert right.isType();
		
		if (result.getTerm() == null) {
		    raiseError("subtype expression <: only supported for" +
			" \\typeof() arguments on the left side.", st);
		}
		
		Sort os = right.getType().getSort();
		Function ioFunc = os.getInstanceofSymbol(services);
		
		result = new SLExpression(
		    TB.equals(
			TB.func(ioFunc, result.getTerm()),
			TB.TRUE(services)));
	    }
	)?
	{
	    if (f != null) {
		assert opToken != null;
		if (result.isType()) {
		    raiseError("Cannot build relational expression from type " +
			result.getType().getName() + ".", opToken);
		}
		assert result.isTerm();
		
		try {
			if (right == null) {
			    // instanceof-expression
			    result = new SLExpression(
				TB.and(TB.not(TB.equals(result.getTerm(), TB.NULL(services))),
				       TB.equals(TB.func(f, result.getTerm()), TB.TRUE(services))));
			} else {
			    if (right.isType()) {
			    raiseError("Cannot build relational expression from type " +
				right.getType().getName() + ".", opToken);
			    }
			    assert right.isTerm();
			    
			    result = new SLExpression(
				TB.func(f,result.getTerm(),right.getTerm()));
			}
		} catch (TermCreationException e) {
		    raiseError("Error in relational expression: " + e.getMessage());
		} catch (IllegalArgumentException e) {
		    raiseError("Internal error.");
		}
	    }
	}
;

shiftexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression e;
}
:
    result=additiveexpr
    (
	SHIFTRIGHT e=additiveexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build shift expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot shift right by type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildRightShiftExpression(result, e);
	}
    |   
	SHIFTLEFT e=additiveexpr 
	{
	    if (result.isType()) {
		raiseError("Cannot build shift expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot shift left by type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildLeftShiftExpression(result, e);
	}
    |   
	UNSIGNEDSHIFTRIGHT e=additiveexpr 
	{
	    if (result.isType()) {
		raiseError("Cannot build shift expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot shift right (unsigned) by type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildUnsignedRightShiftExpression(result, e);
	}
    )*
; 


additiveexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression e;
}
:
    result=multexpr
    (
	PLUS e=multexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build additive expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot add type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildAddExpression(result, e);
	}
    |
	MINUS e=multexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build additive expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot subtract type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildSubExpression(result, e);
	}
    )*
;


multexpr returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression e;
}
:
    result=unaryexpr
    (
	MULT e=unaryexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build multiplicative expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot multiply by type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();
	
	    result = intHelper.buildMulExpression(result, e);
	}
    |
	DIV e=unaryexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build multiplicative expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot divide by type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildDivExpression(result, e);
	}
    |
	MOD e=unaryexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build multiplicative expression from type " +
		    result.getType().getName() + ".");
	    }
	    if (e.isType()) {
		raiseError("Cannot build modulo expression from type " +
		    e.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    assert e.isTerm();

	    result = intHelper.buildModExpression(result, e);
	}
    )*
;


unaryexpr returns [SLExpression result=null] throws SLTranslationException
{
    KeYJavaType type = null;
}
:
(
       PLUS result=unaryexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build  +" + result.getType().getName() + ".");
	    }
	    assert result.isTerm();
	    
	    result = intHelper.buildPromotedUnaryPlusExpression(result);
	}
    |
	MINUS result=unaryexpr
	{
	    if (result.isType()) {
		raiseError("Cannot build  -" + result.getType().getName() + ".");
	    }
	    assert result.isTerm();

	    result = intHelper.buildUnaryMinusExpression(result);
	}
    |
	(LPAREN typespec RPAREN ) => 
	   LPAREN type=typespec RPAREN result=unaryexpr
	
    |
	result=unaryexprnotplusminus
)
	 {
	     if (type != null) {
		 if (result.isType()) {
		     raiseError("Casting of type variables not (yet) supported.");
		 }
		 assert result.isTerm();
		 
		 if(intHelper.isIntegerTerm(result)) {
		     result = intHelper.buildCastExpression(type, result);
		 } else {
		     result = new SLExpression(
		         TB.cast(services, type.getSort(), result.getTerm()), 
		         type);
		 }
	     }
	}
;


unaryexprnotplusminus returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression e;
}
:
	NOT e=unaryexpr
	{
	    if (e.isType()) {
		raiseError("Cannot negate type " + e.getType().getName() + ".");
	    }
	    
	    Term t = e.getTerm();
	    assert t != null;
	    
	    if (t.sort() == Sort.FORMULA) {
		result = new SLExpression(TB.not(t));
	    } else if(t.sort() == booleanLDT.targetSort()) {
		result = new SLExpression(TB.not(TB.equals(t, TB.TRUE(services))));
	    } else {
		raiseError("Wrong type in not-expression: " + t);
	    }
	}
    |   
	BITWISENOT e=unaryexpr
	{
	    if(e.isType()) {
		raiseError("Cannot negate type " + e.getType().getName() + ".");
	    }
		
	    result = intHelper.buildPromotedNegExpression(e);
	}
	
    |
	result=postfixexpr
;


postfixexpr returns [SLExpression result=null] throws SLTranslationException
{
    String fullyQualifiedName = "";
    SLExpression expr = null;
}
:
	expr=primaryexpr
	{
	    fullyQualifiedName = LT(0).getText();
	}
	(
	    {
	        if (expr != null && expr.getType() == null) {
	            raiseError("SLExpression without a type: " + expr);
	        }/* else if (expr != null && expr.getType().getJavaType() instanceof PrimitiveType) {
		    raiseError("Cannot build postfix expression from primitive type.");
		}*/	    		
	    }
	    expr=primarysuffix[expr, fullyQualifiedName]
	    {	    
		fullyQualifiedName += "." + LT(0).getText();
	    }
	)*
	
	{
	    if (expr == null) {
		raiseError("Expression " + fullyQualifiedName + " cannot be resolved.");
	    }
	    result = expr; //.getTerm();
	}
	    
;

primaryexpr returns [SLExpression result=null] throws SLTranslationException
{
    Term s1, s2;
}
:
	result=constant
    |   id:IDENT     { result = lookupIdentifier(id.getText(), null, null, id); }
    |   INV          { result = new SLExpression(TB.inv(services, TB.var(selfVar)));}
    |   TRUE         { result = new SLExpression(TB.tt()); }
    |   FALSE        { result = new SLExpression(TB.ff()); }
    |   NULL         { result = new SLExpression(TB.NULL(services)); }
    |   result=jmlprimary 
    |   THIS       
        { 
            if(selfVar == null) {
            	raiseError("Cannot access \"this\" in a static context!"); 
            }
            result = new SLExpression(TB.var(selfVar), selfVar.getKeYJavaType());
        }
    |   new_expr
;   

primarysuffix[SLExpression receiver, String fullyQualifiedName] 
		returns [SLExpression result=null] 
		throws SLTranslationException
{
    String lookupName = null;   
    ImmutableList<SLExpression> params = ImmutableSLList.<SLExpression>nil();
}
:
{
    lookupName = fullyQualifiedName;
}
(
	DOT id:IDENT
	{
	    if(receiver == null) {
		// Receiver was only a package/classname prefix
		lookupName = fullyQualifiedName + "." + id.getText();
	    } else {
		lookupName = id.getText();
	    }
	    try {
	    	result = lookupIdentifier(lookupName, receiver, null, id);
	    } catch(SLTranslationException e) {
	    	result = lookupIdentifier(fullyQualifiedName + "." + lookupName, null, null, id);
	    }
	}
    |
    DOT THIS
    {
    	result = new SLExpression(
    		services.getTypeConverter().findThisForSort(receiver.getType().getSort(),
    							    TB.var(selfVar), 
    							    javaInfo.getKeYJavaType(selfVar.sort()), 
    							    true),
                receiver.getType());
    }
    |
    DOT INV
    {
        result = new SLExpression(TB.inv(services, receiver.getTerm()));
    }
    |
	l:LPAREN (params=expressionlist)? RPAREN
	{
	    result = lookupIdentifier(lookupName, receiver, new SLParameters(params), l);
	    if (result == null) {
		raiseError("Method " + lookupName + "("
		           + createSignatureString(params) + ") not found!", l);
	    }
	}
    |
	lbrack:LBRACKET result=specarrayrefexpr[receiver, fullyQualifiedName, lbrack] RBRACKET
    |    
         DOT MULT
         {
	     result = new SLExpression(TB.allFields(services, receiver.getTerm()),
	                               javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
         }
	
)	
;

new_expr throws SLTranslationException
{
    KeYJavaType typ = null;
    ImmutableList<SLExpression> params;
}
:
	NEW typ=type LPAREN ( params=expressionlist )? RPAREN 
        {	
        	raiseNotSupported("'new' within specifications"); 
        }
    ;

expressionlist returns [ImmutableList<SLExpression> result=ImmutableSLList.<SLExpression>nil()] 
               throws SLTranslationException
{ 
    SLExpression expr;
}
:
	expr=expression { result = result.append(expr); } (COMMA expr=expression {result = result.append(expr);} )* 
;

constant returns [SLExpression result=null] throws SLTranslationException
:
	result=javaliteral
;

javaliteral returns [SLExpression result=null] throws SLTranslationException
:
	result=integerliteral
    |
	l:STRING_LITERAL 
	{
	    Term charListTerm
	       = services.getTypeConverter()
	                 .convertToLogicElement(
	                 	new StringLiteral("\"" + l.getText() + "\""));
	    Function strPool 
	    	= (Function) services.getNamespaces()
	    	                     .functions()
	    	                     .lookup(CharListLDT.STRINGPOOL_NAME);
	    if(strPool == null) {
	        raiseError("string literals used in specification, "
	                   + "but string pool function not found");
	    }
	    Term stringTerm = TB.func(strPool, charListTerm);
	    return new SLExpression(stringTerm, 
	                            javaInfo.getKeYJavaType("java.lang.String"));
	}
    |
	CHAR_LITERAL 
	{
	    raiseNotSupported("character literals");
	}
    ;

integerliteral returns [SLExpression result=null] throws SLTranslationException
:
	result=decimalintegerliteral
    |
	result=hexintegerliteral
;

hexintegerliteral returns [SLExpression result=null] throws SLTranslationException
:
    n:HEXNUMERAL
    {
	BigInteger decInteger = new BigInteger(n.getText(), 16);
	result = new SLExpression(TB.zTerm(services, decInteger.toString()),
	                          javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_INT));
    }
;

decimalintegerliteral returns [SLExpression result=null] throws SLTranslationException
:
	result=decimalnumeral
;

decimalnumeral returns [SLExpression result=null] throws SLTranslationException
:
    n:DIGITS
    {
	result = new SLExpression(TB.zTerm(services,n.getText()),
	                          javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_INT));
    }
;

jmlprimary returns [SLExpression result=null] throws SLTranslationException
{
    ImmutableList<SLExpression> list;
    SLExpression e1 = null;
    SLExpression e2 = null;
    SLExpression e3 = null;
    KeYJavaType typ;
    Term t, t2;
    Pair<KeYJavaType,ImmutableList<LogicVariable>> declVars = null;    
}
:
	RESULT
	{
	    if(resultVar==null) {
		raiseError("\\result used in wrong context");
	    }
	    result = new SLExpression(TB.var(resultVar), resultVar.getKeYJavaType());
	}
    |
	(LPAREN QUANTIFIER) => t=specquantifiedexpression { result = new SLExpression(t); }
    |
        (LPAREN BSUM) => result=bsumterm
    |
	(OLD | PRE) LPAREN result=expression RPAREN
	{
	    if (heapAtPre == null) {
		raiseError("JML construct " +
			   "\\old not allowed in this context.");
	    }
	    
	    result = new SLExpression(convertToOld(result.getTerm()), 
	                              result.getType());
	}
    |   
	CREATED LPAREN result=expression RPAREN
	{
		raiseNotSupported("\\created is deliberately not supported in this KeY version, you should not need it");
	}
    |
	NONNULLELEMENTS LPAREN result=expression RPAREN
	{
	    t = result.getTerm();
	    Term resTerm = TB.not(TB.equals(t, TB.NULL(services)));

	    if (t.sort() instanceof ArraySort) {
		LogicVariable i = new LogicVariable(new Name("i"), javaInfo
				.getKeYJavaType(PrimitiveType.JAVA_INT)
				.getSort());

		// See JML reference manual
		// http://www.cs.iastate.edu/~leavens/JML/jmlrefman/jmlrefman_11.html#SEC139		
		Term range = TB.and(
		    TB.leq(TB.zero(services), TB.var(i), services),
		    TB.lt(TB.var(i), TB.dotLength(services, t), services));
		Term body = TB.equals(
		    TB.dotArr(services, t, TB.var(i)),
		    TB.NULL(services));
		body = TB.not(body);
		body = TB.imp(range, body);

		result = new SLExpression(TB.and(resTerm, TB.all(i, body)));
	    }
	}
	
    |   INFORMAL_DESCRIPTION 
	{
	    raiseNotSupported("informal predicates");
	}
    |   NOT_MODIFIED LPAREN t=storereflist RPAREN
        {
            raiseNotSupported("\\not_modified");
        } 
	
    |   FRESH LPAREN list=expressionlist RPAREN
	{
	    if(heapAtPre == null) {
	        raiseError("\\fresh not allowed in this context");
	    }
	    t = TB.tt();
	    final Sort objectSort = services.getJavaInfo().objectSort();
	    for(SLExpression expr: list) {
	        if(!expr.isTerm()) {
	            raiseError("Expected a term, but found: " + expr);
	        } else if(expr.getTerm().sort().extendsTrans(objectSort)) {
	            t = TB.and(t, 
	                       TB.equals(TB.select(services,
	                                           booleanLDT.targetSort(),
	                                           heapAtPre,
	                                           expr.getTerm(),
	                                           TB.func(heapLDT.getCreated())),
	                                 TB.FALSE(services)));
	        } else if(expr.getTerm().sort().extendsTrans(locSetLDT.targetSort())) {
	            t = TB.and(t, TB.subset(services, 
	                                    expr.getTerm(), 
	                                    TB.freshLocs(services, heapAtPre)));
	        } else {
	            raiseError("Wrong type: " + expr);
	        }
	    }
	    result = new SLExpression(t);
	} 

    |   REACH LPAREN t=storeref COMMA e1=expression COMMA e2=expression (COMMA e3=expression)? RPAREN
	{
	    final LogicVariable stepsLV 
	    	= e3 == null 
	          ? new LogicVariable(new Name("n"), intLDT.targetSort()) 
	          : null;
	    final Term h = TB.heap(services);
	    final Term s = getFields(t);
	    final Term o = e1.getTerm();
	    final Term o2 = e2.getTerm();
	    final Term n = e3 == null ? TB.var(stepsLV) : e3.getTerm();
	    Term reach = TB.reach(services, h, s, o, o2, n);
	    if(e3 == null) {
	        reach = TB.ex(stepsLV, reach);
	    }
	    result = new SLExpression(reach);
	} 
	
    |   REACHLOCS LPAREN t=storeref COMMA e1=expression (COMMA e3=expression)? RPAREN
	{
	    final LogicVariable objLV
	    	= new LogicVariable(new Name("o"), javaInfo.objectSort());
	    final LogicVariable stepsLV 
	    	= e3 == null 
	          ? new LogicVariable(new Name("n"), intLDT.targetSort()) 
	          : null;
	    final Term h = TB.heap(services);
	    final Term s = getFields(t);
	    final Term o = e1.getTerm();
	    final Term o2 = TB.var(objLV);
	    final Term n = e3 == null ? TB.var(stepsLV) : e3.getTerm();
	    Term reach = TB.reach(services, h, s, o, o2, n);
	    if(e3 == null) {
	        reach = TB.ex(stepsLV, reach);
	    }
	    
	    final LogicVariable fieldLV
	    	= new LogicVariable(new Name("f"), heapLDT.getFieldSort());
	    final Term locSet 
	    	= TB.guardedSetComprehension(services, 
	    	                             new LogicVariable[]{objLV, fieldLV},
	    	                             reach, 
	    	                      	     o2,
	    	                      	     TB.var(fieldLV));
	    
	    result = new SLExpression(locSet, javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
	} 	
	
    |   DURATION LPAREN result=expression RPAREN 
	{
	    raiseNotSupported("\\duration");
	} 
	
    |   SPACE LPAREN result=expression RPAREN
	{
	    raiseNotSupported("\\space");
	} 
	
    |   WORKINGSPACE LPAREN result=expression RPAREN
	{
	    raiseNotSupported("\\working_space");
	} 
	
    |   TYPEOF LPAREN result=expression RPAREN
	{
	    result = new SLExpression(result.getTerm(),
	                              result.getType(),
	                              false);
	} 
	
    |   ELEMTYPE LPAREN result=expression RPAREN 
	{
	    raiseNotSupported("\\elemtype");
	} 
	
    |   TYPE_SMALL LPAREN typ=typespec RPAREN 
	{
	    result = new SLExpression(typ);
	} 
	
    |   LOCKSET
	{
	    raiseNotSupported("\\lockset");
	} 
	
    |   IS_INITIALIZED LPAREN typ=referencetype RPAREN 
	{
	    Term resTerm = TB.equals(
		TB.var(
		    javaInfo.getAttribute(ImplicitFieldAdder.IMPLICIT_CLASS_INITIALIZED, 
					  typ)),
		TB.TRUE(services));
	    result = new SLExpression(resTerm);
	} 
	
    |   INVARIANT_FOR LPAREN result=expression RPAREN 
	{
	    raiseNotSupported("\\invariant_for");
	} 
	
    |   ( LPAREN LBLNEG ) => LPAREN LBLNEG IDENT result=expression RPAREN
	{
	    raiseNotSupported("\\lblneg");
	} 
	
    |   ( LPAREN LBLPOS ) => LPAREN LBLPOS IDENT result=expression RPAREN 
	{
	    raiseNotSupported("\\lblpos");
	} 
	
    |   NOWARN 
	{
	    raiseNotSupported("\\nowarn");
	}
	 
    |   STRING_EQUAL LPAREN e1=expression COMMA e2=expression RPAREN
        {
	    Function strContent
	    	= (Function) services.getNamespaces()
            	                     .functions()
            	                     .lookup(CharListLDT.STRINGCONTENT_NAME);
            if(strContent == null) {
                raiseError("strings used in spec, but string content "
                           + "function not found");
            }
            return new SLExpression(TB.equals(TB.func(strContent, e1.getTerm()), 
                                              TB.func(strContent, e2.getTerm())));
        }

    |   EMPTYSET
        {
            result = new SLExpression(TB.empty(services),
                                      javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
        }
        
    |   SINGLETON LPAREN t=storeref RPAREN
        {
            if(!t.op().equals(locSetLDT.getSingleton())) {
            	if(heapLDT.getSortOfSelect(t.op()) != null) {
	            final Term objTerm = t.sub(1);
	            final Term fieldTerm = t.sub(2);
	    	    t = TB.singleton(services, objTerm, fieldTerm);
            	} else {
                    raiseError("Not a singleton: " + t);
                }
            }
            result = new SLExpression(t, javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
        }
        
    |   UNION LPAREN t=storeref COMMA t2 = storeref RPAREN
        {
            result = new SLExpression(TB.union(services, t, t2),
                                      javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
        }
        
    |   INTERSECT LPAREN t=storeref COMMA t2=storeref RPAREN
        {
            result = new SLExpression(TB.intersect(services, t, t2),
                                      javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
        }         

    |   SETMINUS LPAREN t=storeref COMMA t2=storeref RPAREN
        {
            result = new SLExpression(TB.setMinus(services, t, t2),
                                      javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
        } 
        
    |   ALLFIELDS LPAREN e1=expression RPAREN
        {
            if(!e1.isTerm() || !e1.getTerm().sort().extendsTrans(services.getJavaInfo().objectSort())) {
                raiseError("Invalid argument to \\allFields: " + e1);
            }
            result = new SLExpression(TB.allFields(services, e1.getTerm()),
                                      javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
        }        
        
    |   UNIONINF 
        LPAREN 
        declVars=quantifiedvardecls SEMI
        {
            resolverManager.pushLocalVariablesNamespace();
            resolverManager.putIntoTopLocalVariablesNamespace(declVars.second, declVars.first);
        } 
        t=storeref 
        RPAREN
        {
	    resolverManager.popLocalVariablesNamespace();
	    result = new SLExpression(TB.infiniteUnion(services,
	                                               declVars.second.toArray(new QuantifiableVariable[declVars.second.size()]),
	                                               t),
	                              javaInfo.getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));        
        }        

    |   DISJOINT LPAREN t=storeref COMMA t2=storeref RPAREN
        {
            result = new SLExpression(TB.disjoint(services, t, t2));
        } 

    |   SUBSET LPAREN t=storeref COMMA t2=storeref RPAREN
        {
            result = new SLExpression(TB.subset(services, t, t2));
        } 
        
    |   NEWELEMSFRESH LPAREN t=storeref RPAREN
        {
            result = new SLExpression(TB.subset(services, 
                                                t, 
                                                TB.union(services,
                                                         convertToOld(t),
                                                         TB.freshLocs(services, 
                                                         	      heapAtPre))));
                                                        
        }
        
    |   SEQEMPTY
        {
            result = new SLExpression(TB.seqEmpty(services));
        }
    
    |   SEQSINGLETON LPAREN e1=expression RPAREN
        {
            result = new SLExpression(TB.seqSingleton(services, e1.getTerm()));
        }    
    
    |   SEQCONCAT LPAREN e1=expression COMMA e2=expression RPAREN
        {
            result = new SLExpression(TB.seqConcat(services, e1.getTerm(), e2.getTerm()));
        }    
    
    |   SEQSUB LPAREN e1=expression COMMA e2=expression COMMA e3=expression RPAREN
        {
            result = new SLExpression(TB.seqSub(services, e1.getTerm(), e2.getTerm(), e3.getTerm()));
        }
        
    |   SEQREVERSE LPAREN e1=expression RPAREN
        {
            result = new SLExpression(TB.seqReverse(services, e1.getTerm()));
        }                         

    |   LPAREN result=expression RPAREN
;

specquantifiedexpression returns [Term result = null] throws SLTranslationException
{
    SLExpression expr;
    Term p = TB.tt();
    boolean nullable = false;
    Pair<KeYJavaType,ImmutableList<LogicVariable>> declVars = null;
}
:
	LPAREN
	q:QUANTIFIER 
	(nullable=boundvarmodifiers)? 
	declVars=quantifiedvardecls SEMI
	{
	    resolverManager.pushLocalVariablesNamespace();
	    resolverManager.putIntoTopLocalVariablesNamespace(declVars.second, declVars.first);
	} 
	((predicate SEMI) => p=predicate SEMI | SEMI)? 
	expr=expression
	{
	    resolverManager.popLocalVariablesNamespace();
	    
	    p = convertToFormula(p);
	    Term t = convertToFormula(expr.getTerm());
	    
	    //add implicit "non-null and created" guards for reference types, 
	    //"in-bounds" guards for integer types
	    Term nullTerm = TB.NULL(services);
	    for(LogicVariable lv : declVars.second) {
	        p = TB.and(p, TB.reachableValue(services, TB.var(lv), declVars.first));
	    	if(lv.sort().extendsTrans(services.getJavaInfo().objectSort()) && !nullable) {
		    p = TB.and(p, TB.not(TB.equals(TB.var(lv), nullTerm)));
		}
	    }	    

	    if (q.getText().equals("\\forall")) {
		if (p != null) {
		    t = TB.imp(p, t);
		}
		result = TB.all(declVars.second.toArray(new LogicVariable[declVars.second.size()]), t);
	    }
	    else if (q.getText().equals("\\exists")) {
		if (p != null) {
		    t = TB.and(p, t);
		}
		result = TB.ex(declVars.second.toArray(new LogicVariable[declVars.second.size()]), t);
	    }
	    else if (q.getText().equals("\\min")) {
	    	raiseNotSupported("\\min");
	    }
	    else if (q.getText().equals("\\max")) {
	        raiseNotSupported("\\max");
	    }
	    /*XXX
	    else if (q.getText().equals("\\num_of")) {
            	LogicVariable lv = declVars.head();
            	p=p.sub(0);
            	if(p!=null && isBoundedSum(p, lv) && p.sub(0).op()!=Junctor.AND){
	                result = TermFactory.DEFAULT.createBoundedNumericalQuantifierTerm(BoundedNumericalQuantifier.BSUM, 
        	                lowerBound(p, lv), upperBound(p, lv), TB.ife(
                	                t, TB.zTerm(services, "1"), TB.zTerm(services, "0")),
                        	        new ImmutableArray<QuantifiableVariable>(lv));
                                       
                } else {
                    raiseError("only \\num_of expressions of form (\\sum int i; l<=i && i<u; t) are permitted");
            	}
	    }*/
	    else if (q.getText().equals("\\product")) {
		raiseNotSupported("\\product");
	    }
	    /*XXX	    
	    else if (q.getText().equals("\\sum")) {
                LogicVariable lv = declVars.head();
            	p=p.sub(0);
            
            	if(isBoundedSum(p, lv)) {
	            if(p.arity()>0 && p.sub(0).op()==Junctor.AND) {
                        t = TB.ife(p.sub(1), t, TB.zTerm(services, "0"));
                    }
                    result = TermFactory.DEFAULT.createBoundedNumericalQuantifierTerm(BoundedNumericalQuantifier.BSUM, 
                            lowerBound(p, lv), upperBound(p, lv), t, new ImmutableArray<QuantifiableVariable>(lv));
                } else {
                    raiseError("only \\sum expressions of form (\\sum int i; l<=i && i<u; t) are permitted");
                }
	    }*/ 
	    else {
		raiseError("Unknown quantifier: " + q.getText() + "!");
	    }
	}
	RPAREN
;

bsumterm returns [SLExpression result=null] throws SLTranslationException
{
    SLExpression a = null;
    SLExpression b = null; 
    Pair<KeYJavaType,ImmutableList<LogicVariable>> decls = null;
}:
        LPAREN
        q:BSUM decls=quantifiedvardecls 
        {	    
            resolverManager.pushLocalVariablesNamespace();
            resolverManager.putIntoTopLocalVariablesNamespace(decls.second, decls.first);
        } 
        SEMI
        (
            a=expression SEMI  b=expression SEMI result=expression
        )
        {
            if(!decls.first.getJavaType().equals(PrimitiveType.JAVA_INT)) {
                raiseError("bounded sum variable must be of type int");
            } else if(decls.second.size() != 1) {
                raiseError("bounded sum must declare exactly one variable");
            }
            LogicVariable lv = (LogicVariable) decls.second.head();
            Function bsum = intLDT.getBsum();
            Term t = TB.func(bsum, 
                             new Term[]{a.getTerm(), b.getTerm(), result.getTerm()},
                             new ImmutableArray<QuantifiableVariable>(lv));
	    result = new SLExpression(t);
            resolverManager.popLocalVariablesNamespace();
        }
        RPAREN
; exception
        catch [SLTranslationException ex] {
        resolverManager.popLocalVariablesNamespace();
        throw ex;
        }   

quantifiedvardecls returns [Pair<KeYJavaType,ImmutableList<LogicVariable>> result = null]
                   throws SLTranslationException
{
    KeYJavaType t = null;
    ImmutableList<LogicVariable> vars = ImmutableSLList.<LogicVariable>nil();
    LogicVariable v = null;
}
:
	t=typespec v=quantifiedvariabledeclarator[t] 
	
	{ vars = vars.append(v); }
	
	(
	    COMMA v=quantifiedvariabledeclarator[t]
	    
	    { vars = vars.append(v); }
	)*
	{
	    result = new Pair<KeYJavaType,ImmutableList<LogicVariable>>(t, vars);
	}
;

boundvarmodifiers returns [boolean nullable = false] throws SLTranslationException
:
	NON_NULL | NULLABLE { nullable = true; }
;

typespec returns [KeYJavaType t = null] throws SLTranslationException
{
    int dim = 0;
}
:
	t=type 
	(
	    dim=dims
	    {
		String fullName = t.getFullName();
		for (int i=0; i < dim; i++) {
		    fullName += "[]";
		}
		t = javaInfo.getKeYJavaType(fullName);
	if(t == null && dim > 0) {
	    //try to create missing array type
	      try {
	    javaInfo.readJavaBlock("{" + fullName + " k;}");
	    t = javaInfo.getKeYJavaType(fullName);
	    } catch (Exception e) {
	    t = null;
		}
	    }
	    }
	)?
;

dims returns [int dimension = 0] throws SLTranslationException
:
	(LBRACKET RBRACKET { dimension++; } )+
    ;

type returns [KeYJavaType t = null] throws SLTranslationException
:
	(builtintype) => t=builtintype
    |
	t=referencetype
    |
	TYPE
	{
	    raiseNotSupported("\\TYPE");
	}
	
;

referencetype returns [KeYJavaType type = null] throws SLTranslationException
{
    String typename;
}
:
	typename=name
	{
	    try {
		type = resolverManager.resolve(null, typename, null).getType();
	    } catch (NullPointerException e) {
		raiseError("Type " + typename + " not found.");
	    }
	}
;   

builtintype returns [KeYJavaType type = null] throws SLTranslationException
:
	(
	    BYTE 
	    {
		type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_BYTE);
	    }
	|
	    SHORT 
	    {
		type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_SHORT);
	    }
	|
	    INT 
	    {
		type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_INT);
	    }
	|
	    LONG 
	    {
		type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_LONG);
	    }
	|
	    BOOLEAN 
	    {
		type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_BOOLEAN);
	    }
	|
	    VOID 
	    {
		type = null;
	    }
	|
	    BIGINT
	    {
		raiseNotSupported("\\bigint");
	    } 
	|
	    REAL
	    {
		raiseNotSupported("\\real");
	    } 
        |   LOCSET
            {
                type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_LOCSET);
            }
        |   SEQ
            {
                type = javaInfo.getKeYJavaType(PrimitiveType.JAVA_SEQ);
            }            
	)
	
;

name returns [String result = ""] throws SLTranslationException
:
	id:IDENT
	{ result += id.getText(); }
	(
	    DOT id1:IDENT 
	    { result += "." + id1.getText(); }
	)*
;

quantifiedvariabledeclarator[KeYJavaType t] returns [LogicVariable v = null] throws SLTranslationException
{
    int dim = 0;
    KeYJavaType varType = null;
}
:
   id:IDENT (dim=dims)?
   {
	  if (dim > 0) {
	    String fullName;
	    if (t.getJavaType() instanceof ArrayType) {
		fullName = ((ArrayType) t.getJavaType()).getAlternativeNameRepresentation();
	    } else {
		fullName = t.getFullName();
	    }
	    for (int i=0; i < dim; i++) {
		fullName += "[]";
	    }
	    
	    varType = javaInfo.getKeYJavaType(fullName);
	  } else {
		  varType = t;
	  }
	  
	  v = new LogicVariable(new Name(id.getText()), varType.getSort());
   }
;