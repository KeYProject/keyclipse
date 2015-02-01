// This file is part of KeY - Integrated Deductive Software Design
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2014 Karlsruhe Institute of Technology, Germany
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General
// Public License. See LICENSE.TXT for details.
//

package de.uka.ilkd.key.rule;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import de.uka.ilkd.key.collection.DefaultImmutableSet;
import de.uka.ilkd.key.collection.ImmutableArray;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableMap;
import de.uka.ilkd.key.collection.ImmutableSLList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.java.ContextStatementBlock;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.SourceData;
import de.uka.ilkd.key.logic.BoundVarsVisitor;
import de.uka.ilkd.key.logic.Choice;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Named;
import de.uka.ilkd.key.logic.OpCollector;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.RenameTable;
import de.uka.ilkd.key.logic.RenamingTable;
import de.uka.ilkd.key.logic.Semisequent;
import de.uka.ilkd.key.logic.SemisequentChangeInfo;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentChangeInfo;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.VariableNamer;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.label.TermLabelState;
import de.uka.ilkd.key.logic.op.Junctor;
import de.uka.ilkd.key.logic.op.LogicVariable;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.op.QuantifiableVariable;
import de.uka.ilkd.key.logic.op.SVSubstitute;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.logic.op.TermTransformer;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.ProgVarReplacer;
import de.uka.ilkd.key.rule.inst.GenericSortCondition;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.tacletbuilder.AntecSuccTacletGoalTemplate;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletGoalTemplate;
import de.uka.ilkd.key.rule.tacletbuilder.TacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.TacletGoalTemplate;
import de.uka.ilkd.key.util.Debug;


/** 
 * Taclets are the DL-extension of schematic theory specific rules. They are
 * used to describe rules of a logic (sequent) calculus. A typical taclet
 * definition looks similar to <br></br>
 * <code> 
 *    taclet_name { if ( ... ) find ( ... ) goal_descriptions }
 * </code> <br></br>
 * where the if-part must and the find-part can contain a sequent arrow, that
 * indicates, if a term has to occur at the top level and if so, on which side of
 * the sequent. The goal descriptions consists of lists of add and replacewith
 * constructs. They describe, how to construct a new goal out of the old one by
 * adding or replacing parts of the sequent. Each of these lists describe a new
 * goal, whereas if no such list exists, means that the goal is closed. <p>
 *   The find part of a taclet is used to attached the rule to a term in the
 * sequent of the current goal. Therefore the term of the sequent has to match
 * the schema as found in the taclet's find part. The taclet is then attached to
 * this term, more precise not the taclet itself, but an application object of
 * this taclet (see {@link de.uka.ilkd.key.rule.TacletApp TacletApp}. When
 * this attached taclet application object is applied, the new goals are
 * constructed as described by the goal descriptions. For example <br></br>
 * <code> 
 *    find (A | B ==>) replacewith ( A ==> ); replacewith(B ==>) 
 * </code> <br></br>creates 
 * two new goals, where the first has been built by replacing <code> A | B </code>
 * with <code>A</code> and the second one by replacing <code>A | B</code> with
 * <code>B</code>. For a complete description of the syntax and semantics of
 * taclets consult the KeY-Manual.  The objects of this class serve different
 * purposes: First they represent the syntactical structure of a taclet, but 
 * they also include the taclet interpreter isself. The taclet interpreter
 * knows two modes: the match and the execution mode. The match mode tries to
 * find a a mapping from schemavariables to a given term or formula. In the
 * execution mode, a given goal is manipulated in the manner as described by the
 * goal descriptions. </p>
 * <p>
 *   But an object of this class neither copies or split the goal, nor it
 * iterates through a sequent looking where it can be applied, these tasks have
 * to be done in advance. For example by one of the following classes 
 * {@link de.uka.ilkd.key.proof.RuleAppIndex RuleAppIndex} or 
 * {@link de.uka.ilkd.key.proof.TacletAppIndex TacletAppIndex} or 
 * {@link de.uka.ilkd.key.rule.TacletApp TacletApp} </p>
 */
public abstract class Taclet implements Rule, Named {
    
    private static final String AUTONAME = "_taclet";

    /** name of the taclet */
    private final Name name;
    
    /** name displayed by the pretty printer */
    private final String displayName;
    
    /** contains useful text explaining the taclet */
    private final String helpText = null;
    
    /** the set of taclet options for this taclet */
    protected final ImmutableSet<Choice> choices;

    /**
     * the <tt>if</tt> sequent of the taclet
     */
    private final Sequent ifSequent;
    /** 
     * Variables that have to be created each time the taclet is applied. 
     * Those variables occur in the varcond part of a taclet description.
     */
    private final ImmutableList<NewVarcond> varsNew;
    /** 
     * variables with a "x not free in y" variable condition. This means the
     * instantiation of VariableSV x must not occur free in the instantiation of
     * TermSV y.
     */
    private final ImmutableList<NotFreeIn> varsNotFreeIn;
    /** 
     * variable conditions used to express that a termsv depends on the free
     * variables of a given formula(SV)
     * Used by skolemization rules.
     */
    @Deprecated
    private final ImmutableList<NewDependingOn> varsNewDependingOn;

    /** Additional generic conditions for schema variable instantiations. */
    private final ImmutableList<VariableCondition> variableConditions;

    /**
     * the list of taclet goal descriptions 
     */
    private final ImmutableList<TacletGoalTemplate> goalTemplates;

    /**
     * list of rulesets (formerly known as heuristica) the taclet belongs to
     */
    protected final ImmutableList<RuleSet> ruleSets;

    /**
     * map from a schemavariable to its prefix. The prefix is used to test
     * correct instantiations of the schemavariables by resolving/avoiding
     * collisions. Mainly the prefix consists of a list of all variables that
     * may appear free in the instantiation of the schemavariable (a bit more
     * complicated for rewrite taclets, see paper of M:Giese)
     */
    protected final ImmutableMap<SchemaVariable,TacletPrefix> prefixMap;
    
    /** cache; contains set of all bound variables */
    private ImmutableSet<QuantifiableVariable> boundVariables = null;
    
    /** tracks state of pre-computation */
    private boolean contextInfoComputed = false;
    private boolean contextIsInPrefix   = false;
    
    /** true if one of the goal descriptions is a replacewith */
    private boolean hasReplaceWith      = false;
     
    
    protected String tacletAsString;

    /** Set of schemavariables of the if part */
    private ImmutableSet<SchemaVariable> ifVariables = null;

    /** This map contains (a, b) if there is a substitution {b a}
     * somewhere in this taclet */
    private ImmutableMap<SchemaVariable,SchemaVariable>
	svNameCorrespondences = null;
	
    /** Integer to cache the hashcode */
    private int hashcode = 0;    
    
    private Trigger trigger;
    
    /* TODO: find better solution*/
    private final boolean surviveSymbExec;
    
    

    /**
     * creates a Schematic Theory Specific Rule (Taclet) with the given
     * parameters.  
     * @param name the name of the Taclet 
     * @param applPart contains the application part of an Taclet that is
     * the if-sequence, the variable conditions
     * @param goalTemplates a list of goal descriptions.
     * @param ruleSets a list of rule sets for the Taclet
     * @param attrs attributes for the Taclet; these are boolean values
     * indicating a noninteractive or recursive use of the Taclet.      
     */
    Taclet(Name name,
           TacletApplPart applPart,
           ImmutableList<TacletGoalTemplate> goalTemplates,
           ImmutableList<RuleSet> ruleSets,
           TacletAttributes attrs,
           ImmutableMap<SchemaVariable, TacletPrefix> prefixMap,
           ImmutableSet<Choice> choices,
           boolean surviveSmbExec) {

        this.name          = name;
        ifSequent          = applPart.ifSequent();
        varsNew            = applPart.varsNew();
        varsNotFreeIn      = applPart.varsNotFreeIn();
        varsNewDependingOn = applPart.varsNewDependingOn();
        variableConditions = applPart.getVariableConditions();
        this.goalTemplates = goalTemplates;
        this.ruleSets      = ruleSets;
        this.choices       = choices;
        this.prefixMap     = prefixMap;
        this.displayName = attrs.displayName() == null
                           ? name.toString() : attrs.displayName();
        this.surviveSymbExec = surviveSmbExec;

        this.trigger = attrs.getTrigger();
    }

    public boolean hasTrigger() {
        return trigger != null;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * creates a Schematic Theory Specific Rule (Taclet) with the given
     * parameters.
     *
     * @param name the name of the Taclet
     * @param applPart contains the application part of an Taclet that is the
     * if-sequence, the variable conditions
     * @param goalTemplates a list of goal descriptions.
     * @param ruleSets a list of rule sets for the Taclet
     * @param attrs attributes for the Taclet; these are boolean values
     * indicating a noninteractive or recursive use of the Taclet.
     */
    Taclet(Name name,
           TacletApplPart applPart,
           ImmutableList<TacletGoalTemplate> goalTemplates,
           ImmutableList<RuleSet> ruleSets,
           TacletAttributes attrs,
           ImmutableMap<SchemaVariable, TacletPrefix> prefixMap,
           ImmutableSet<Choice> choices) {
        this(name, applPart, goalTemplates, ruleSets, attrs, prefixMap, choices,
             false);
    }
    
    protected void cacheMatchInfo() {
	boundVariables = getBoundVariables();
        
	final Iterator<TacletGoalTemplate> goalDescriptions = 
	    goalTemplates.iterator();
	
	while (!hasReplaceWith && goalDescriptions.hasNext()) {
	    if (goalDescriptions.next().
		replaceWithExpressionAsObject() != null) {
		hasReplaceWith = true;
	    }
	}	
    }

    
    /** 
     * computes and returns all variables that occur bound in the taclet
     * including the taclets defined in <tt>addrules</tt> sections. The result
     * is cached and therefore only computed once.  
     * @return all variables occuring bound in the taclet
     */
    public ImmutableSet<QuantifiableVariable> getBoundVariables() {        
        if (boundVariables == null) {        
            ImmutableSet<QuantifiableVariable> result = 
                DefaultImmutableSet.<QuantifiableVariable>nil();
                       
            for (final TacletGoalTemplate tgt : goalTemplates()) {
                result = result.union(tgt.getBoundVariables());
            }

            final BoundVarsVisitor bvv = new BoundVarsVisitor();
            bvv.visit(ifSequent());
            result = result.union(bvv.getBoundVariables());

            result = result.union(getBoundVariablesHelper());

            boundVariables = result;
        }
        
        return boundVariables;
    }

    /**
     * collects bound variables in taclet entities others than goal templates
     * @return set of variables that occur bound in taclet entities others 
     * than goal templates
     */
    protected abstract ImmutableSet<QuantifiableVariable> getBoundVariablesHelper(); 

    /**
     * looks if a variable is declared as not free in
     * @param var the SchemaVariable to look for
     * @return true iff declared not free
     */
    private boolean varDeclaredNotFree(SchemaVariable var) {
	for (final NotFreeIn nfi : varsNotFreeIn) {
	    if (nfi.first() == var) {
		return true;
	    }
	}
	return false;
    }

    /**
     * returns true iff the taclet contains a "close goal"-statement
     * @return true iff the taclet contains a "close goal"-statement
     */
    public boolean closeGoal () {
	return goalTemplates.isEmpty();
    }

    /**
     * looks if a variable is declared as new and returns its sort to match
     * with or the schema variable it shares the match-sort with. Returns
     * null if the SV is not declared to as new.
     * @param var the SchemaVariable to look for
     * @return the sort of the SV to match or the SV it shares the same
     * match-sort with
     */
    public NewVarcond varDeclaredNew(SchemaVariable var) {
	for(final NewVarcond nv : varsNew) {
	    if(nv.getSchemaVariable() == var) {
		return nv;
	    }
	}
	return null;
    }

    /**
     * @return the generic variable conditions of this taclet
     */
    public Iterator<VariableCondition> getVariableConditions () {
	return variableConditions.iterator ();
    }

    /**
     * returns true iff the given variable is bound either in the
     * ifSequent or in 
     * any part of the TacletGoalTemplates
     * @param v the bound variable to be searched 
     */
    protected boolean varIsBound(SchemaVariable v) {
        return (v instanceof QuantifiableVariable) && getBoundVariables().contains((QuantifiableVariable) v);
    }

 
    /**
     * returns a SVInstantiations object iff the given Term
     * template can be instantiated to 
     * match the given Term term using the known instantiations stored in
     * svInst.  If a
     * matching cannot be found null is returned.
     * The not free in condition is checked in TacletApp. Collisions are
     * resolved there as well, if necessary.
     * @param term the Term that has to be matched
     * @param template the Term that is checked if it can match term
     * @param matchCond the SVInstantiations/Constraint that are
     * required because of formerly matchings
     * @param services the Services object encapsulating information
     * about the java datastructures like (static)types etc.
     * @return the new MatchConditions needed to match template with
     * term , if possible, null otherwise
     */
    protected MatchConditions match(Term            term,
				    Term            template,
				    MatchConditions matchCond,
				    Services        services) {
	Debug.out("Start Matching rule: ", name);
	matchCond = matchHelp(term, template, matchCond, services);	
	Debug.out(matchCond == null ? "Failed: " : "Succeeded: ", name);
	return matchCond == null ? null : checkConditions(matchCond, services);
    }

    /**
     * checks if the conditions for a correct instantiation are satisfied
     * @param var the SchemaVariable to be instantiated
     * @param instantiationCandidate the SVSubstitute, which is a
     * candidate for a possible instantiation of var
     * @param matchCond the MatchConditions which have to be respected
     * for the new match
     * @param services the Services object encapsulating information
     * about the Java type model
     * @return the match conditions resulting from matching
     * <code>var</code> with <code>instantiationCandidate</code> or
     * <code>null</code> if a match was not possible
     */
    public MatchConditions checkVariableConditions(SchemaVariable var, 
                                                   SVSubstitute instantiationCandidate,
                                                   MatchConditions matchCond,
                                                   Services services) {
	if (instantiationCandidate instanceof Term) {
	    Term term = (Term) instantiationCandidate;
	    if (!(term.op() instanceof QuantifiableVariable)) {
		if (varIsBound(var) || varDeclaredNotFree(var)) {
		    // match(x) is not a variable, but the
		    // corresponding template variable is bound
		    // or declared non free (so it has to be
		    // matched to a variable) 		
		    return null; // FAILED
		}
	    }
	}
	// check generic conditions
	for (final VariableCondition vc : variableConditions) {
	    matchCond = vc.check(var, instantiationCandidate, matchCond, services);	    
	    if (matchCond == null) {	     
		return null; // FAILED
	    }
	}

	return matchCond;	
    }


    public MatchConditions checkConditions(MatchConditions cond, Services services) {
        if (cond == null) {
            return null;
        }
        MatchConditions result = cond;
        final Iterator<SchemaVariable> svIterator = 
            cond.getInstantiations().svIterator();
        
        if(!svIterator.hasNext()) {
            return checkVariableConditions(null, null, cond, services);//XXX
        }
        
        while (svIterator.hasNext()) {
            final SchemaVariable sv = svIterator.next();
            final Object o = result.getInstantiations().getInstantiation(sv);
            if (o instanceof SVSubstitute) {
                result = checkVariableConditions
                   (sv, (SVSubstitute)o , result, services);
            }
            if (result == null) {                
                Debug.out("FAILED. InstantiationEntry failed condition for ", sv, o);
                return null;
            }
        }
        return result;
    }
    

    /**
     * tries to match the bound variables of the given term against the one
     * described by the template
     * @param term the Term whose bound variables are matched against the
     * JavaBlock of the template
     * (marked as final to help the compiler inlining methods)
     * @param template the Term whose bound variables are the template that have
     * to be matched
     * @param matchCond the MatchConditions that has to be paid respect when
     * trying to match
     * @return the new matchconditions if a match is possible, otherwise null
     */
    private final MatchConditions matchBoundVariables(Term term, 
						      Term template, 
						      MatchConditions matchCond,
						      Services services) {
	
        matchCond = matchCond.extendRenameTable();
        
        for (int j=0, arity = term.arity(); j<arity; j++) {		
	    
	    ImmutableArray<QuantifiableVariable> bound    = term.varsBoundHere(j);
	    ImmutableArray<QuantifiableVariable> tplBound = template.varsBoundHere(j); 
	    
	    if (bound.size() != tplBound.size()) {
		return null; //FAILED
	    }
	    
	    for (int i=0, boundSize = bound.size(); i<boundSize; i++) {		
	        final QuantifiableVariable templateQVar = tplBound.get(i);
                final QuantifiableVariable qVar = bound.get(i);
                if (templateQVar instanceof LogicVariable) {
                    final RenameTable rt = matchCond.renameTable();                   
                    if (!rt.containsLocally(templateQVar) && !rt.containsLocally(qVar)) {                           
                        matchCond = matchCond.addRenaming(templateQVar, qVar);
                    }
                }
                matchCond = templateQVar.match(qVar, matchCond, services);					  
                
                if (matchCond == null) {	              
                    return null;        
	       }
	    }
	}
	return matchCond;
    }

    /**
     * returns the matchconditions that are required if the java block of the
     * given term matches the schema given by the template term or null if no
     * match is possible
     * (marked as final to help the compiler inlining methods)
     * @param term the Term whose JavaBlock is matched against the JavaBlock of
     * the template
     * @param template the Term whose JavaBlock is the template that has to
     * be matched
     * @param matchCond the MatchConditions that has to be paid respect when
     * trying to match the JavaBlocks
     * @param services the Services object encapsulating information about the
     * program context
     * @return the new matchconditions if a match is possible, otherwise null
     */
    protected final MatchConditions matchJavaBlock(Term term, 
						   Term template, 
						   MatchConditions matchCond,
						   Services services) {
      
	if (term.javaBlock().isEmpty()) { // this.name().toString().startsWith("unfold_computed_formula")
	    if (!template.javaBlock().isEmpty()){
		Debug.out("Match Failed. No program to match.");
		return null; //FAILED
	    }
            if (template.javaBlock().program()
                    instanceof ContextStatementBlock) {
                // we must match empty context blocks too
                matchCond = template.javaBlock().program().
                    match(new SourceData(term.javaBlock().program(), -1, services), matchCond);
            }
	} else { //both javablocks != null                            
            matchCond = template.javaBlock().program().
            match(new SourceData(term.javaBlock().program(), -1, services), matchCond);
        }        
	return matchCond;
    }
    
    /** returns a SVInstantiations object with the needed SchemaVariable to Term
     * mappings to match the given Term template to the Term term or
     * null if no matching is possible.
     * (marked as final to help the compiler inlining methods)
     * @param term the Term the Template should match
     * @param template the Term tried to be instantiated so that it matches term
     * @param matchCond the MatchConditions to be obeyed by a
     * successful match
     * @return the new MatchConditions needed to match template with
     * term, if possible, null otherwise
     *
     * PRECONDITION: matchCond.getConstraint ().isSatisfiable ()
     */

    private MatchConditions matchHelp(final Term             term,
				      final Term             template, 
				      MatchConditions  	     matchCond,
				      final Services         services) {
	Debug.out("Match: ", template);
	Debug.out("With: ",  term);
        
	final Operator sourceOp   =     term.op ();
	final Operator templateOp = template.op ();
                
	if (template.hasLabels()) {
	    final ImmutableArray<TermLabel> labels = template.getLabels();
	    for (TermLabel l: labels) {
	        // ignore all labels which are not schema variables
	        // if intended to match concrete label, match against schema label
	        // and use an appropriate variable condition
	        if (l instanceof SchemaVariable) {
	            final SchemaVariable schemaLabel = (SchemaVariable) l;
	            final MatchConditions cond =
	                    schemaLabel.match(term, matchCond, services);
	            if (cond == null) {
	                return null;
	            }
	            matchCond = cond;
	        }
	    }
	}

	if (templateOp instanceof SchemaVariable && templateOp.arity() == 0) {
	    return templateOp.match(term, matchCond, services);
	}

	matchCond = templateOp.match (sourceOp, matchCond, services);
	if(matchCond == null) {
	    Debug.out("FAILED 3x.");
	    return null; ///FAILED
	}

	//match java blocks:
	matchCond = matchJavaBlock(term, template, matchCond, services);
	if (matchCond == null) {
	    Debug.out("FAILED. 9: Java Blocks not matching");
	    return null;  //FAILED
	}

	//match bound variables:
	matchCond = matchBoundVariables(term, template, matchCond, services);
	if (matchCond == null) {
	    Debug.out("FAILED. 10: Bound Vars");
	    return null;  //FAILED
	}

	for (int i = 0, arity = term.arity(); i < arity; i++) {
	    matchCond = matchHelp(term.sub(i),
	                          template.sub(i),
	                          matchCond,
	                          services);
	    if (matchCond == null) {
	        return null; //FAILED
	    }
	}

        return matchCond.shrinkRenameTable();
    }


    /**
     * Match the given template (which is probably a formula of the if
     * sequence) against a list of constraint formulas (probably the
     * formulas of the antecedent or the succedent), starting with the
     * given instantiations and constraint p_matchCond.
     * @param p_toMatch list of constraint formulas to match p_template to
     * @param p_template template formula as in "match"
     * @param p_matchCond already performed instantiations
     * @param p_services the Services object encapsulating information
     * about the java datastructures like (static)types etc.
     * @return Two lists (in an IfMatchResult object), containing the
     * the elements of p_toMatch that could successfully be matched
     * against p_template, and the corresponding MatchConditions.
     */
    public IfMatchResult matchIf ( Iterator<IfFormulaInstantiation> p_toMatch,
				   Term                             p_template,
				   MatchConditions                  p_matchCond,
				   Services                         p_services ) {
	ImmutableList<IfFormulaInstantiation> resFormulas = ImmutableSLList
	        .<IfFormulaInstantiation> nil();
	ImmutableList<MatchConditions> resMC = ImmutableSLList
	        .<MatchConditions> nil();

	Term updateFormula;
	if (p_matchCond.getInstantiations().getUpdateContext().isEmpty())
	    updateFormula = p_template;
	else
	    updateFormula = p_services.getTermBuilder().applyUpdatePairsSequential(p_matchCond.getInstantiations()
		    .getUpdateContext(), p_template);

	IfFormulaInstantiation cf;
	MatchConditions newMC;

	while (p_toMatch.hasNext()) {
	    cf = p_toMatch.next();

	    newMC = match(cf.getConstrainedFormula().formula(), updateFormula, p_matchCond, p_services);
	    if (newMC != null) {
		resFormulas = resFormulas.prepend(cf);
		resMC = resMC.prepend(newMC);
	    }
	}

	return new IfMatchResult ( resFormulas, resMC );
    }


    /**
     * Match the whole if sequent using the given list of
     * instantiations of all if sequent formulas, starting with the
     * instantiations given by p_matchCond.
     * PRECONDITION: p_toMatch.size () == ifSequent ().size ()
     * @return resulting MatchConditions or null if the given list
     * p_toMatch does not match
     */
    public MatchConditions matchIf ( Iterator<IfFormulaInstantiation> p_toMatch,
				     MatchConditions                  p_matchCond,
				     Services                         p_services ) {

	Iterator<SequentFormula>     itIfSequent   = ifSequent () .iterator ();

	ImmutableList<MatchConditions>            newMC;	
	
	while ( itIfSequent.hasNext () ) {
	    newMC = matchIf ( ImmutableSLList.<IfFormulaInstantiation>nil()
				.prepend ( p_toMatch.next () ).iterator (),
			      itIfSequent.next ().formula (),
			      p_matchCond,
			      p_services ).getMatchConditions ();

	    if ( newMC.isEmpty() )
		return null;

	    p_matchCond = newMC.head ();
	}

	return p_matchCond;

    }


    /** returns the name of the Taclet
     */
    public Name name() {
	return name;
    } 
    

    /** returns the display name of the taclet, or, if not specified -- 
     *  the canonical name
     */
    public String displayName() {
	return displayName;
    }
    
    
    public String helpText() {
       return helpText;
    }
 
   /** 
    * returns the if-sequence of the application part of the Taclet.
    */
    public Sequent ifSequent() {
	return ifSequent;
    } 
    
    /** returns an iterator over the variables that are new in the Taclet. 
     */
    public ImmutableList<NewVarcond> varsNew() {
	return varsNew;
    } 

    
    /** returns an iterator over the variable pairs that indicate that are 
     * new in the Taclet. 
     */
    public Iterator<NotFreeIn> varsNotFreeIn() { 
	return varsNotFreeIn.iterator();
    } 

    public Iterator<NewDependingOn> varsNewDependingOn() { 
	return varsNewDependingOn.iterator();
    } 
    
    /** returns an iterator over the goal descriptions.
     */
    public ImmutableList<TacletGoalTemplate> goalTemplates() {
	return goalTemplates;
    } 

    public ImmutableSet<Choice> getChoices(){
	return choices;
    }

    /** returns an iterator over the rule sets. */
    public Iterator<RuleSet> ruleSets() {
	return ruleSets.iterator();
    } 

    public ImmutableList<RuleSet> getRuleSets() {
	return ruleSets;
    }

//    /** 
//     * returns true iff the Taclet is to be applied only noninteractive
//     */
//    public boolean noninteractive() {
//	return noninteractive;
//    }


    public ImmutableMap<SchemaVariable,TacletPrefix> prefixMap() {
	return prefixMap;
    }

    /** 
     * returns true if one of the goal templates is a replacewith. Already
     * computed and cached by method cacheMatchInfo
     */
    public boolean hasReplaceWith() {
	return hasReplaceWith;
    }
    
    /**
     * returns the computed prefix for the given schemavariable. The
     * prefix of a schemavariable is used to determine if an
     * instantiation is correct, in more detail: it mainly contains all
     * variables that can appear free in an instantiation of the
     * schemvariable sv (rewrite taclets have some special handling, see
     * paper of M. Giese and comment of method isContextInPrefix).
     * @param sv the Schemavariable 
     * @return prefix of schema variable sv
     */
    public TacletPrefix getPrefix(SchemaVariable sv) {
	return prefixMap.get(sv);
    }

    /**
     * returns true iff a context flag is set in one of the entries in
     * the prefix map. Is cached after having been called
     * once. __OPTIMIZE__ is caching really necessary here?
     *
     * @return true iff a context flag is set in one of the entries in
     * the prefix map.
     */
    public boolean isContextInPrefix() {
	if (contextInfoComputed) {
	    return contextIsInPrefix;
	}
	contextInfoComputed=true;
	Iterator<TacletPrefix> it=prefixMap().valueIterator();
	while (it.hasNext()) {
	    if (it.next().context()) {
		contextIsInPrefix=true;
		return true;
	    }
	}
	contextIsInPrefix=false;
	return false;
    }

    /** 
     * return true if <code>o</code> is a taclet of the same name and 
     * <code>o</code> and <code>this</code> contain no mutually exclusive 
     * taclet options. 
     */
    public boolean equals(Object o) {
        if (o == this) return true;
        
        if (o == null || o.getClass() != this.getClass() ){
           return false;
        }	

        final Taclet t2 = (Taclet)o;
        if (!name.equals(t2.name)) { 
           return false;
        }

        if ((ifSequent == null && t2.ifSequent != null) || 
              (ifSequent != null && t2.ifSequent == null)) {
           return false;
        } else if (ifSequent != null && !ifSequent.equals(t2.ifSequent)) {
           return false;
        }
         
        if (!choices.equals(t2.choices)) {
           return false;
        }

       if (!goalTemplates.equals(t2.goalTemplates)) {
          return false;
       }
        
        return true;
    }

    public int hashCode() {
        if (hashcode == 0) {
           hashcode = 37 * name.hashCode() + 17;
            if (hashcode == 0) {
                hashcode = -1;
            }
        }
        return hashcode;
    }

    /** 
     * a new term is created by replacing variables of term whose replacement is
     * found in the given SVInstantiations 
     * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param term the Term the syntactical replacement is performed on
     * @param services the Services
     * @param mc the {@link MatchConditions} with all instantiations and
     * the constraint 
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     * @return the (partially) instantiated term  
     */
    protected Term syntacticalReplace(TermLabelState termLabelState, Term term,
				      Services services,
				      MatchConditions mc,
				      PosInOccurrence applicationPosInOccurrence,
				      TacletLabelHint labelHint,
				      Goal goal, 
				      TacletApp tacletApp) {
       final SyntacticalReplaceVisitor srVisitor =
             new SyntacticalReplaceVisitor(termLabelState, 
                                                services,
                                                mc.getInstantiations(),
                                                applicationPosInOccurrence,
                                                this,
                                                labelHint,
                                                goal);
         term.execPostOrder(srVisitor);
         return srVisitor.getTerm();
    }

    /**
     * adds SequentFormula to antecedent or succedent depending on
     * position information or the boolean antec 
     * contrary to "addToPos" frm will not be modified
     * @param frm the formula that should be added
     * @param currentSequent the Sequent which is the current (intermediate) result of applying the taclet
     * @param pos the PosInOccurrence describing the place in the sequent
     * @param antec boolean true(false) if elements have to be added to the
     * antecedent(succedent) (only looked at if pos == null)
     */
    private void addToPosWithoutInst(SequentFormula frm,
				     SequentChangeInfo currentSequent,			  
				     PosInOccurrence pos,
				     boolean antec) {
       if (pos != null) {
          currentSequent.combine(currentSequent.sequent().addFormula(frm, pos));
       } else {
          // cf : formula to be added , 1. true/false: antec/succ,
          // 2. true: at head 
          currentSequent.combine(currentSequent.sequent().addFormula(frm, antec, true));
       }	    
    }


    /** 
     * the given constrained formula is instantiated and then
     * the result (usually a complete instantiated formula) is returned.
     * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param schemaFormula the SequentFormula to be instantiated
     * @param services the Services object carrying ja related information
     * @param matchCond the MatchConditions object with the instantiations of
     * the schemavariables, constraints etc.
     * @param applicationPosInOccurrence The {@link PosInOccurrence} of the {@link Term} which is rewritten
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     * @return the as far as possible instantiated SequentFormula
     */
    private SequentFormula 
	instantiateReplacement(TermLabelState termLabelState, SequentFormula schemaFormula,
			       Services           services,
			       MatchConditions    matchCond,
			       PosInOccurrence applicationPosInOccurrence,
			       TacletLabelHint labelHint,
			       Goal goal,
			       TacletApp tacletApp) { 

       final SVInstantiations svInst = matchCond.getInstantiations ();

       Term instantiatedFormula = syntacticalReplace(termLabelState, schemaFormula.formula(), 
             services, matchCond, applicationPosInOccurrence, new TacletLabelHint(labelHint, schemaFormula), goal, tacletApp);

       if (!svInst.getUpdateContext().isEmpty()) {
          instantiatedFormula = services.getTermBuilder().applyUpdatePairsSequential(svInst.getUpdateContext(), 
                instantiatedFormula);         
       }

       return new SequentFormula(instantiatedFormula);
    }
		
    /**
     * instantiates the given semisequent with the instantiations found in 
     * Matchconditions
     * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param semi the Semisequent to be instantiated
     * @param services the Services
     * @param matchCond the MatchConditions including the mapping 
     * Schemavariables to concrete logic elements
     * @param applicationPosInOccurrence The {@link PosInOccurrence} of the {@link Term} which is rewritten
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     * @return the instanted formulas of the semisquent as list
     */
    protected ImmutableList<SequentFormula> instantiateSemisequent(TermLabelState termLabelState, Semisequent semi, Services services,
            MatchConditions matchCond, PosInOccurrence applicationPosInOccurrence, TacletLabelHint labelHint, Goal goal, TacletApp tacletApp) {       
        
       // TODO: use mutable list
        ImmutableList<SequentFormula> replacements = ImmutableSLList.<SequentFormula>nil();

        for (SequentFormula sf : semi) {
            replacements = replacements.append
                (instantiateReplacement(termLabelState, sf, services, matchCond, applicationPosInOccurrence, labelHint, goal, tacletApp));           
        }
        
        return replacements;
    }
    

    
    /**
     * replaces the constrained formula at the given position with the first
     * formula in the given semisequent and adds possible other formulas of the
     * semisequent starting at the position
     * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param semi the Semisequent with the the ConstrainedFormulae to be added
     * @param currentSequent the Sequent which is the current (intermediate) result of applying the taclet
     * @param pos the PosInOccurrence describing the place in the sequent
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions containing in particular
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     * the instantiations of the schemavariables
     */   
    protected void replaceAtPos(TermLabelState termLabelState, 
            Semisequent semi,
            SequentChangeInfo currentSequent,
            PosInOccurrence pos,
            Services services, 
            MatchConditions matchCond,
            TacletLabelHint labelHint,
            Goal goal,
            TacletApp tacletApp) {
       final ImmutableList<SequentFormula> replacements = instantiateSemisequent(termLabelState, semi, services, matchCond, pos, labelHint, goal, tacletApp);
       currentSequent.combine(currentSequent.sequent().changeFormula(replacements, pos));
    }

    /**
     * instantiates the constrained formulas of semisequent
     *  <code>semi</code> and adds the instantiatied formulas at the specified
     *   position to <code>goal</code>   
     * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param semi the Semisequent with the the ConstrainedFormulae to be added
     * @param currentSequent the Sequent which is the current (intermediate) result of applying the taclet
     * @param pos the PosInOccurrence describing the place in the sequent
     * @param antec boolean true(false) if elements have to be added to the
     * antecedent(succedent) (only looked at if pos == null)
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions containing in particular
     * @param applicationPosInOccurrence The {@link PosInOccurrence} of the {@link Term} which is rewritten
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     * the instantiations of the schemavariables
     */
    private void addToPos (TermLabelState termLabelState, Semisequent semi,
             SequentChangeInfo currentSequent,         
             PosInOccurrence pos,
             boolean antec,
             Services services, 
             MatchConditions matchCond,
             PosInOccurrence applicationPosInOccurrence,
             TacletLabelHint labelHint,
             Goal goal,
             TacletApp tacletApp) {
       final ImmutableList<SequentFormula> replacements = 
             instantiateSemisequent(termLabelState, semi, services, matchCond, applicationPosInOccurrence, labelHint, goal, tacletApp);
       
       if (pos != null) {
          currentSequent.combine(currentSequent.sequent().addFormula(replacements, pos));
       } else {
          currentSequent.combine(currentSequent.sequent().addFormula(replacements, antec, true));
       }
    }

    /**
     * adds SequentFormula to antecedent depending on
     * position information (if none is handed over it is added at the
     * head of the antecedent). Of course it has to be ensured that
     * the position information describes one occurrence in the
     * antecedent of the sequent.
     * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param semi the Semisequent with the the ConstrainedFormulae to be added
     * @param currentSequent the Sequent which is the current (intermediate) result of applying the taclet
     * @param pos the PosInOccurrence describing the place in the
     * sequent or null for head of antecedent
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions containing in particular
     * the instantiations of the schemavariables
     * @param applicationPosInOccurrence The {@link PosInOccurrence} of the {@link Term} which is rewritten
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     */
    protected void addToAntec(TermLabelState termLabelState,
               Semisequent semi,
			      SequentChangeInfo currentSequent,
			      PosInOccurrence pos,
			      Services services, 
			      MatchConditions matchCond,
			      PosInOccurrence applicationPosInOccurrence,
			      TacletLabelHint labelHint,
			      Goal goal,
			      TacletApp tacletApp) { 
	    addToPos(termLabelState, semi, currentSequent, pos, true, services, matchCond, applicationPosInOccurrence, labelHint, goal, tacletApp);
    }

    /**
     * adds SequentFormula to succedent depending on
     * position information (if none is handed over it is added at the
     * head of the succedent). Of course it has to be ensured that
     * the position information describes one occurrence in the
     * succedent of the sequent.
      * @param termLabelState The {@link TermLabelState} of the current rule application.
     * @param semi the Semisequent with the the ConstrainedFormulae to be added
     * @param goal the Goal that knows the node the formulae have to be added
     * @param pos the PosInOccurrence describing the place in the
     * sequent or null for head of antecedent
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions containing in particular
     * the instantiations of the schemavariables
     * @param applicationPosInOccurrence The {@link PosInOccurrence} of the {@link Term} which is rewritten
     * @param labelHint The hint used to maintain {@link TermLabel}s.
     */
    protected void addToSucc(TermLabelState termLabelState,
              Semisequent semi,
			     SequentChangeInfo currentSequent,
			     PosInOccurrence pos,
			     Services services, 
			     MatchConditions matchCond,
			     PosInOccurrence applicationPosInOccurrence,
			     TacletLabelHint labelHint,
			     Goal goal,
			     TacletApp tacletApp) {
       addToPos(termLabelState, semi, currentSequent, pos, false, services, matchCond, applicationPosInOccurrence, labelHint, goal, tacletApp);
    }

    protected abstract Taclet setName(String s);
    
    protected Taclet setName(String s, TacletBuilder b) {	
	b.setTacletGoalTemplates(goalTemplates());
	b.setRuleSets(getRuleSets());
	b.setName(new Name(s));
	b.setDisplayName(displayName());
	b.setIfSequent(ifSequent());
	b.addVarsNew(varsNew());
	b.addVarsNotFreeIn(varsNotFreeIn);
	return b.getTaclet();
    }

    /**
     * adds the given rules (i.e. the rules to add according to the Taclet goal
     * template to the node of the given goal
     * @param rules the rules to be added
     * @param goal the goal describing the node where the rules should be added
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions containing in particular
     * the instantiations of the schemavariables
     */
    protected void applyAddrule(ImmutableList<Taclet> rules, Goal goal, 
          Services services,
          MatchConditions matchCond) {

       for (Taclet tacletToAdd : rules) { 
          final Node n = goal.node();
          final StringBuilder uniqueTail = new StringBuilder(tacletToAdd.name().toString());                   
          uniqueTail.append(AUTONAME).append(n.getUniqueTacletId());
          tacletToAdd = tacletToAdd.setName(uniqueTail.toString());


          // the new Taclet may contain variables with a known
          // instantiation. These must be used by the new Taclet and all
          // further rules it contains in the addrules-sections. Therefore all
          // appearing (including the addrules) SchemaVariables have to be
          // collected, then it is looked if an instantiation is known and if
          // positive the instantiation is memorized. At last the Taclet with
          // its required instantiations is handed over to the goal, where a
          // new TacletApp should be built including the necessary instantiation
          // information

          SVInstantiations neededInstances = SVInstantiations.
                EMPTY_SVINSTANTIATIONS.addUpdateList
                (matchCond.getInstantiations ().getUpdateContext());
          final TacletSchemaVariableCollector collector = new
                TacletSchemaVariableCollector(); 
          collector.visit(tacletToAdd, true);// true, because
          // descend into addrules
          for (SchemaVariable sv : collector.vars()) {
             if (matchCond.getInstantiations ().isInstantiated(sv)) {
                neededInstances = neededInstances.add(
                      sv, 
                      matchCond.getInstantiations ().getInstantiationEntry(sv), 
                      services);
             } 
          }

          final ImmutableList<GenericSortCondition>     cs  =
                matchCond.getInstantiations ()
                .getGenericSortInstantiations ().toConditions ();

          for (final GenericSortCondition gsc : cs) {
             neededInstances = neededInstances.add(gsc, services );
          }

          goal.addTaclet(tacletToAdd, neededInstances, true);
       }
    }

    protected void applyAddProgVars(ImmutableSet<SchemaVariable> pvs, 
                                    SequentChangeInfo currentSequent,
                                    Goal goal,
                                    PosInOccurrence posOfFind,
                                    Services services, 
                                    MatchConditions matchCond) {
       ImmutableList<RenamingTable> renamings = ImmutableSLList.<RenamingTable>nil();
       for (final SchemaVariable sv : pvs) {
          final ProgramVariable inst 
          = (ProgramVariable)matchCond.getInstantiations ().getInstantiation(sv);
          //if the goal already contains the variable to be added 
          //(not just a variable with the same name), then there is nothing to do
          if(goal.getGlobalProgVars().contains(inst)) {
             continue;
          }

          final VariableNamer vn = services.getVariableNamer();
          final ProgramVariable renamedInst = vn.rename(inst, goal, posOfFind);
          goal.addProgramVariable(renamedInst);
          goal.proof().getServices().addNameProposal(renamedInst.name());

          HashMap<ProgramVariable, ProgramVariable> renamingMap =
                vn.getRenamingMap();
          if (!renamingMap.isEmpty()) {        
             //execute renaming
             final ProgVarReplacer pvr = new ProgVarReplacer(vn.getRenamingMap(), services);
             
             //globals
             goal.setGlobalProgVars(pvr.replace(goal.getGlobalProgVars()));

             //taclet apps
             pvr.replace(goal.ruleAppIndex().tacletIndex());

             //sequent
             currentSequent.combine(pvr.replace(currentSequent.sequent()));

             final RenamingTable rt = 
                   RenamingTable.getRenamingTable(vn.getRenamingMap());
             
             renamings = renamings.append(rt);
          }
       }
       goal.node().setRenamings(renamings);
    }



    /** the rule is applied to the given goal using the
     * information of rule application.
     * @param goal the goal that the rule application should refer to.
     * @param services the Services encapsulating all java information
     * @param tacletApp the rule application that is executed.
     * @return List of the goals created by the rule which have to be
     * proved. If this is a close-goal-taclet ( this.closeGoal () ),
     * the first goal of the return list is the goal that should be
     * closed (with the constraint this taclet is applied under).
     */
    public abstract ImmutableList<Goal> apply(Goal goal, Services services, 
				     RuleApp tacletApp);


    /**
     * Search for formulas within p_list that have to be proved by an
     * explicit if goal, i.e. elements of type IfFormulaInstDirect.
     * @return an array with two goals if such formulas exist (the
     * second goal is the if goal), otherwise an array consisting of
     * the single element p_goal
     */
    protected ImmutableList<SequentChangeInfo> checkIfGoals ( Goal                         p_goal,
          ImmutableList<IfFormulaInstantiation> p_list,
          MatchConditions              p_matchCond,
          int                          p_numberOfNewGoals ) {
       ImmutableList<SequentChangeInfo>     res    = null;
       Iterator<SequentChangeInfo> itNewGoalSequents;

       // proof obligation for the if formulas
       Term           ifObl  = null;

       // always create at least one new goal
       if ( p_numberOfNewGoals == 0 )
          p_numberOfNewGoals = 1;

       if ( p_list != null ) {
          int i = ifSequent ().antecedent ().size ();
          Term ifPart;

          for (final IfFormulaInstantiation inst : p_list) {
             if ( !( inst instanceof IfFormulaInstSeq ) ) {
                // build the if obligation formula
                ifPart = inst.getConstrainedFormula ().formula ();

                // negate formulas of the if succedent
                final TermServices services = p_goal.proof().getServices();
                if ( i <= 0 )
                   ifPart = services.getTermBuilder().not(ifPart);		    

                if ( res == null ) {
                   res = ImmutableSLList.<SequentChangeInfo>nil();
                   for (int j = 0; j< p_numberOfNewGoals + 1; j++) {
                    res = res.prepend(SequentChangeInfo.createSequentChangeInfo((SemisequentChangeInfo)null, 
                         (SemisequentChangeInfo)null, p_goal.sequent(), p_goal.sequent()));
                   }
                   ifObl = ifPart;
                } else {
                   ifObl = services.getTermFactory().createTerm
                   ( Junctor.AND, ifObl, ifPart );
                }
                
                // UGLY: We create a flat structure of the new
                // goals, thus the if formulas have to be added to
                // every new goal
                itNewGoalSequents = res.iterator ();
                SequentChangeInfo seq = itNewGoalSequents.next ();
                while ( itNewGoalSequents.hasNext () ) {
                   addToPosWithoutInst ( inst.getConstrainedFormula (),
                         seq,
                         null,
                         ( i > 0 ) ); // ( i > 0 ) iff inst is formula
                   // of the antecedent
                   seq = itNewGoalSequents.next ();
                }
             }

             --i;
          }
       }

       if ( res == null ) {
          res = ImmutableSLList.<SequentChangeInfo>nil();
          for (int j = 0; j< p_numberOfNewGoals; j++) {
             res = res.prepend(SequentChangeInfo.createSequentChangeInfo((SemisequentChangeInfo)null, 
                  (SemisequentChangeInfo)null, p_goal.sequent(), p_goal.sequent()));
            }      
       } else {
          // find the sequent the if obligation has to be added to
          itNewGoalSequents = res.iterator ();
          SequentChangeInfo seq = itNewGoalSequents.next ();
          while ( itNewGoalSequents.hasNext () ) {
             seq = itNewGoalSequents.next ();
          }

          addToPosWithoutInst ( new SequentFormula ( ifObl ),
                seq,
                null,
                false );
       }

       return res;
    }

    /**
     * returns the set of schemavariables of the taclet's if-part
     * @return Set of schemavariables of the if part
     */
    protected ImmutableSet<SchemaVariable> getIfVariables () {
	// should be synchronized
	if ( ifVariables == null ) {
	    TacletSchemaVariableCollector svc = new TacletSchemaVariableCollector ();
	    svc.visit( ifSequent () );
	    
	    ifVariables                 = DefaultImmutableSet.<SchemaVariable>nil();
	    for (final SchemaVariable sv : svc.vars()) {
		  ifVariables = ifVariables.add ( sv );
	    }
	}

	return ifVariables;
    }


    /**
     * @return set of schemavariables of the if and the (optional)
     * find part
     */
    public abstract ImmutableSet<SchemaVariable> getIfFindVariables ();


    /**
     * Find a schema variable that could be used to choose a name for
     * an instantiation (a new variable or constant) of "p"
     * @return a schema variable that is substituted by "p" somewhere
     * in this taclet (that is, these schema variables occur as
     * arguments of a substitution operator)
     */
    public SchemaVariable getNameCorrespondent ( SchemaVariable p,
                                                 Services services) {
	// should be synchronized
	if ( svNameCorrespondences == null ) {
	    final SVNameCorrespondenceCollector c =
		new SVNameCorrespondenceCollector (services.getTypeConverter().getHeapLDT());
	    c.visit ( this, true );
	    svNameCorrespondences = c.getCorrespondences ();
	}

	return svNameCorrespondences.get ( p );
    }


    StringBuffer toStringIf(StringBuffer sb) {
	if (!ifSequent.isEmpty()) {
	    sb = sb.append("\\assumes (").append(ifSequent).append(") ");
	    sb = sb.append("\n");
	}
	return sb;
    }

    StringBuffer toStringVarCond(StringBuffer sb) {
	Iterator<NewVarcond> itVarsNew=varsNew().iterator();
	Iterator<NotFreeIn> itVarsNotFreeIn=varsNotFreeIn();
	Iterator<VariableCondition> itVC=getVariableConditions();
	if (itVarsNew.hasNext() ||
	    itVarsNotFreeIn.hasNext() ||
	    itVC.hasNext()) {
	    sb = sb.append("\\varcond(");
	    while (itVarsNew.hasNext()) {
	    sb=sb.append(itVarsNew.next());
		if (itVarsNew.hasNext() || itVarsNotFreeIn.hasNext())
		    sb=sb.append(", "); 
	    }
	    while (itVarsNotFreeIn.hasNext()) {
		NotFreeIn pair=itVarsNotFreeIn.next();
                sb=sb.append("\\notFreeIn(").append(pair.first()).append
		  (", ").append(pair.second()).append(")");	 
		if (itVarsNotFreeIn.hasNext()) sb=sb.append(", ");
	    }
	    while (itVC.hasNext()) {
		sb.append("" + itVC.next());
		if (itVC.hasNext())
		    sb.append(", ");
	    }
	    sb=sb.append(")\n");	    
	}
	return sb;
    }

    StringBuffer toStringGoalTemplates(StringBuffer sb) {
	if (goalTemplates.isEmpty()) {
	    sb.append("\\closegoal");
	} else {
	    Iterator<TacletGoalTemplate> it=goalTemplates().iterator();
	    while (it.hasNext()) {
		sb=sb.append(it.next());
		if (it.hasNext()) sb = sb.append(";");
		sb = sb.append("\n");
	    }
	}
	return sb;
    }

    StringBuffer toStringRuleSets(StringBuffer sb) {
	Iterator<RuleSet> itRS=ruleSets();
	if (itRS.hasNext()) {
	    sb=sb.append("\\heuristics(");
	    while (itRS.hasNext()) {
		sb = sb.append(itRS.next());
		if (itRS.hasNext()) sb=sb.append(", ");
	    }
	    sb=sb.append(")");
	}
	return sb;
    }

    StringBuffer toStringAttribs(StringBuffer sb) {
//	if (noninteractive()) sb = sb.append(" \\noninteractive");
       sb.append("\nChoices: ").append(choices);
	return sb;
    }
    
    /**
     * returns a representation of the Taclet as String
     * @return string representation
     */
    public String toString() {
	if (tacletAsString == null) {
	    StringBuffer sb=new StringBuffer();
	    sb = sb.append(name()).append(" {\n");
	    sb = toStringIf(sb);
	    sb = toStringVarCond(sb);
	    sb = toStringGoalTemplates(sb);
	    sb = toStringRuleSets(sb);
	    sb = toStringAttribs(sb); 
	    tacletAsString = sb.append("}").toString();
	}
	return tacletAsString;
    }

    /**
     * @return true iff <code>this</code> taclet may be applied for the
     * given mode (interactive/non-interactive, activated rule sets)
     */
    public boolean admissible(boolean       interactive,
			      ImmutableList<RuleSet> p_ruleSets) {
	if ( interactive )
	    return admissibleInteractive(p_ruleSets);
	else
	    return admissibleAutomatic(p_ruleSets);
    }

    protected boolean admissibleInteractive(ImmutableList<RuleSet> notAdmissibleRuleSets) {
        return true;
    }

    protected boolean admissibleAutomatic(ImmutableList<RuleSet> admissibleRuleSets) {
        for (final RuleSet tacletRuleSet : getRuleSets() ) {
            if ( admissibleRuleSets.contains ( tacletRuleSet ) ) return true;
        }
        return false;
    }

    public boolean getSurviveSymbExec() {
        return surviveSymbExec;
    }

    public Set<SchemaVariable> collectSchemaVars() {

	Set<SchemaVariable> result = new LinkedHashSet<SchemaVariable>();
	OpCollector oc = new OpCollector();

	//find, assumes
	for(SchemaVariable sv: this.getIfFindVariables()) {
	    result.add(sv);
	}

	//add, replacewith
	for(TacletGoalTemplate tgt : this.goalTemplates()) {
	    collectSchemaVarsHelper(tgt.sequent(), oc);
	    if(tgt instanceof AntecSuccTacletGoalTemplate) {
		collectSchemaVarsHelper(
			((AntecSuccTacletGoalTemplate)tgt).replaceWith(), oc);
	    } else if(tgt instanceof RewriteTacletGoalTemplate) {
		((RewriteTacletGoalTemplate)tgt).replaceWith()
					        .execPostOrder(oc);
	    }
	}

	for(Operator op : oc.ops()) {
	    if(op instanceof SchemaVariable) {
		result.add((SchemaVariable)op);
	    }
	}

	return result;
    }

    private void collectSchemaVarsHelper(Sequent s, OpCollector oc) {
	for(SequentFormula cf : s) {
	    cf.formula().execPostOrder(oc);
	}
    }
    
    /**
     * Instances of this class are used as hints to maintain {@link TermLabel}s.
     * @author Martin Hentschel
     */
    public static class TacletLabelHint {
       /**
        * The currently performed operation.
        */
       private final TacletOperation tacletOperation;
       
       /**
        * The optional {@link Sequent} of the add or replace part of the taclet.
        */
       private final Sequent sequent;
       
       /**
        * The optional {@link SequentFormula} contained in {@link #getSequent()}.
        */
       private final SequentFormula sequentFormula;

       /**
        * The optional replace {@link Term} of the taclet.
        */
       private final Term term;
       
       /**
        * Constructor.
        * @param tacletOperation The currently performed operation.
        * @param sequent The optional {@link Sequent} of the add or replace part of the taclet.
        */
       public TacletLabelHint(TacletOperation tacletOperation, Sequent sequent) {
          assert tacletOperation != null;
          assert !TacletOperation.REPLACE_TERM.equals(tacletOperation);
          assert sequent != null;
          this.tacletOperation = tacletOperation;
          this.sequent = sequent;
          this.sequentFormula = null;
          this.term = null;
       }
      
       /**
        * Constructor.
        * @param tacletOperation The currently performed operation.
        * @param term The optional replace {@link Term} of the taclet.
        */
       public TacletLabelHint(Term term) {
          assert term != null;
          this.tacletOperation = TacletOperation.REPLACE_TERM;
          this.sequent = null;
          this.sequentFormula = null;
          this.term = term;
       }
      
       /**
        * Constructor.
        * @param labelHint The previous {@link TacletLabelHint} which is now specialised.
        * @param sequentFormula The optional {@link SequentFormula} contained in {@link #getSequent()}.
        */
       public TacletLabelHint(TacletLabelHint labelHint, SequentFormula sequentFormula) {
          assert labelHint != null;
          assert !TacletOperation.REPLACE_TERM.equals(labelHint.getTacletOperation());
          assert sequentFormula != null;
          this.tacletOperation = labelHint.getTacletOperation();
          this.sequent = labelHint.getSequent();
          this.sequentFormula = sequentFormula;
          this.term = labelHint.getTerm();
       }
        
       /**
        * Returns the currently performed operation.
        * @return The currently performed operation.
        */
       public TacletOperation getTacletOperation() {
          return tacletOperation;
       }

       /**
        * Returns the optional {@link Sequent} of the add or replace part of the taclet.
        * @return The optional {@link Sequent} of the add or replace part of the taclet.
        */
       public Sequent getSequent() {
          return sequent;
       }

       /**
        * Returns the optional {@link SequentFormula} contained in {@link #getSequent()}.
        * @return The optional {@link SequentFormula} contained in {@link #getSequent()}.
        */
       public SequentFormula getSequentFormula() {
          return sequentFormula;
       }

       /**
        * Returns the optional replace {@link Term} of the taclet.
        * @return The optional replace {@link Term} of the taclet.
        */
       public Term getTerm() {
          return term;
       }

       /**
        * {@inheritDoc}
        */
       @Override
       public String toString() {
          return tacletOperation + ", sequent = " + sequent + ", sequent formula = " + sequentFormula + ", term = " + term;
       }

       /**
        * Defines the possible operations a {@link Taclet} performs.
        * @author Martin Hentschel
        */
       public static enum TacletOperation {
          /**
           * Add clause of a {@link Taclet} applied to the antecedent.
           * Available information are {@link TacletLabelHint#getSequent()} and {@link TacletLabelHint#getSequentFormula()}.
           */
          ADD_ANTECEDENT, 

          /**
           * Add clause of a {@link Taclet} applied to the succedent.
           * Available information are {@link TacletLabelHint#getSequent()} and {@link TacletLabelHint#getSequentFormula()}.
           */
          ADD_SUCCEDENT, 
          
          /**
           * Replace clause of a {@link Taclet} provides a {@link Sequent} and currently additional adds to the antecedent are performed.
           * Available information are {@link TacletLabelHint#getSequent()} and {@link TacletLabelHint#getSequentFormula()}.
           */
          REPLACE_TO_ANTECEDENT, 
          
          /**
           * Replace clause of a {@link Taclet} provides a {@link Sequent} and currently the current {@link PosInOccurrence} on the succedent is modified.
           * Available information are {@link TacletLabelHint#getSequent()} and {@link TacletLabelHint#getSequentFormula()}.
           */
          REPLACE_AT_SUCCEDENT, 
          
          /**
           * Replace clause of a {@link Taclet} provides a {@link Sequent} and currently the current {@link PosInOccurrence} on the antecedent is modified.
           * Available information are {@link TacletLabelHint#getSequent()} and {@link TacletLabelHint#getSequentFormula()}.
           */
          REPLACE_AT_ANTECEDENT, 
          
          /**
           * Replace clause of a {@link Taclet} provides a {@link Sequent} and currently additional adds to the succedent are performed.
           * Available information are {@link TacletLabelHint#getSequent()} and {@link TacletLabelHint#getSequentFormula()}.
           */
          REPLACE_TO_SUCCEDENT, 

          /**
           * Replace clause of a {@link Taclet} provides a {@link Term} which is currently used to modify the {@link PosInOccurrence}.
           * Available information are {@link TacletLabelHint#getTerm()}.
           */
          REPLACE_TERM;
       }
    }
}