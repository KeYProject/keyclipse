// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.rule;

import java.util.Iterator;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableMap;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.op.QuantifiableVariable;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.proof.Goal;


/** 
 * An abstract class to represent Taclets with a find part. This means, they
 * have to be attached to a formula or term of the sequent. This class is
 * extended by several subclasses to distinct between taclets that have to
 * attached to a top level formula of the antecedent ({@link AntecTaclet}),
 * to the succedent ({@link SuccTaclet}) or to an arbitrary term that matches
 * the find part somewhere in the sequent ({@link RewriteTaclet}).  
 */ 
public abstract class FindTaclet extends Taclet {

    /** contains the find term */
    protected Term find;

    /** Set of schemavariables of the if and the (optional) find part */
    private ImmutableSet<SchemaVariable> ifFindVariables = null;

    /** this method is used to determine if top level updates are
     * allowed to be ignored. This is the case if we have an Antec or
     * SuccTaclet but not for a RewriteTaclet
     * @return true if top level updates shall be ignored 
     */
    protected abstract boolean ignoreTopLevelUpdates();

    
    /** creates a FindTaclet 
     * @param name the Name of the taclet
     * @param applPart the TacletApplPart that contains the if-sequent, the
     * not-free and new-vars conditions 
     * @param goalTemplates a IList<TacletGoalTemplate> that contains all goaltemplates of
     * the taclet (these are the instructions used to create new goals when
     * applying the Taclet)
     * @param ruleSets a IList<RuleSet> that contains all rule sets the Taclet
     *      is attached to
     * @param attrs the TacletAttributes encoding if the Taclet is non-interactive,
     * recursive or something like that
     * @param find the Term that is the pattern that has to be found in a
     * sequent and the places where it matches the Taclet can be applied
     * @param prefixMap a ImmMap<SchemaVariable,TacletPrefix> that contains the
     * prefix for each SchemaVariable in the Taclet
     */
    public FindTaclet(Name name, TacletApplPart applPart,  
		      ImmutableList<TacletGoalTemplate> goalTemplates, 
		      ImmutableList<RuleSet> ruleSets,
		      TacletAttributes attrs, Term find,
		      ImmutableMap<SchemaVariable,TacletPrefix> prefixMap,
		      ImmutableSet<Choice> choices){
	super(name, applPart, goalTemplates, ruleSets, attrs, prefixMap,
	      choices);
	this.find = find;
    }
    
    /** returns the find term of the taclet to be matched */
    public Term find() {
	return find;
    }

    /** 
     * matches the given term against the taclet's find term and
     * @param term the Term to be matched against the find expression 
     * of the taclet
     * @param matchCond the MatchConditions with side conditions to be 
     * satisfied, eg. partial instantiations of schema variables; before
     * calling this method the constraint contained in the match conditions
     * must be ensured to be satisfiable, i.e.
     *       <tt> matchCond.getConstraint ().isSatisfiable () </tt>
     * must return true
     * @param services the Services 
     * @return the found schema variable mapping or <tt>null</tt> if 
     * the matching failed
     */
    public MatchConditions matchFind(Term term,
            MatchConditions matchCond,
            Services services) {
        return match(term, find(), 
                ignoreTopLevelUpdates(),
                matchCond, services);
    }

    /** CONSTRAINT NOT USED 
     * applies the replacewith part of Taclets
     * @param gt TacletGoalTemplate used to get the replaceexpression 
     * in the Taclet
     * @param goal the Goal where the rule is applied
     * @param posOfFind the PosInOccurrence belonging to the find expression
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions with all required instantiations 
     */
    protected abstract void applyReplacewith(TacletGoalTemplate gt, Goal goal,
					     PosInOccurrence posOfFind,
					     Services services,
					     MatchConditions matchCond);


    /**
     * adds the sequent of the add part of the Taclet to the goal sequent
     * @param add the Sequent to be added
     * @param goal the Goal to be updated
     * @param posOfFind the PosInOccurrence describes the place where to add
     * the semisequent 
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions with all required instantiations 
     */
    protected abstract void applyAdd(Sequent add, Goal goal,
				     PosInOccurrence posOfFind,
				     Services services,
				     MatchConditions matchCond);


    /**  
     * the rule is applied on the given goal using the
     * information of rule application. 
     * @param goal the goal that the rule application should refer to.
     * @param services the Services encapsulating all java information
     * @param ruleApp the taclet application that is executed.
     */
    public ImmutableList<Goal> apply(Goal     goal,
			    Services services,
			    RuleApp  ruleApp) {

	// Number without the if-goal eventually needed
	int                          numberOfNewGoals = goalTemplates().size();

	TacletApp                    tacletApp        = (TacletApp) ruleApp;
	MatchConditions              mc               = tacletApp.matchConditions ();

	ImmutableList<Goal>                   newGoals         =
	    checkIfGoals ( goal,
			   tacletApp.ifFormulaInstantiations (),
			   mc,
			   numberOfNewGoals );
	
	Iterator<TacletGoalTemplate> it               = goalTemplates().iterator();
	Iterator<Goal>               goalIt           = newGoals.iterator();

	while (it.hasNext()) {
	    TacletGoalTemplate gt          = it    .next();
	    Goal               currentGoal = goalIt.next();
	    // add first because we want to use pos information that
	    // is lost applying replacewith
	    applyAdd(         gt.sequent(),
			      currentGoal,
			      tacletApp.posInOccurrence(),
			      services,
			      mc );

	    applyReplacewith( gt,
			      currentGoal,
			      tacletApp.posInOccurrence(),
			      services,
			      mc );

	    applyAddrule(     gt.rules(),
			      currentGoal,
			      services,
			      mc );

	    
	    applyAddProgVars( gt.addedProgVars(),
			      currentGoal,
                              tacletApp.posInOccurrence(),
                              services,
			      mc);
                               
            currentGoal.setBranchLabel(gt.name());
	}

	return newGoals;
    }

    StringBuffer toStringFind(StringBuffer sb) {
	return sb.append("\\find(").
	    append(find().toString()).append(")\n");
    }


    /**
     * returns a representation of the Taclet with find part as String
     * @return string representation
     */
    public String toString() {
	if (tacletAsString == null) {
	    StringBuffer sb = new StringBuffer();
	    sb = sb.append(name()).append(" {\n");
	    sb = toStringIf(sb);
	    sb = toStringFind(sb);
	    sb = toStringVarCond(sb);
	    sb = toStringGoalTemplates(sb);
	    sb = toStringRuleSets(sb);
	    sb = toStringAttribs(sb); 
	    tacletAsString = sb.append("}").toString();
	}
	return tacletAsString;
    }


    /**
     * @return Set of schemavariables of the if and the (optional)
     * find part
     */
    public ImmutableSet<SchemaVariable> getIfFindVariables () {
	if ( ifFindVariables == null ) {
	    TacletSchemaVariableCollector svc = new TacletSchemaVariableCollector ();
	    find ().execPostOrder ( svc );
	    
	    ifFindVariables             = getIfVariables ();
	    Iterator<SchemaVariable> it = svc.varIterator ();
	    while ( it.hasNext () )
		ifFindVariables = ifFindVariables.add ( it.next () );
	}

	return ifFindVariables;
    }


    protected Taclet setName(String s, FindTacletBuilder b) {
	return super.setName(s, b); 
    }


    /**
     * returns the variables that occur bound in the find part
     */
    protected ImmutableSet<QuantifiableVariable> getBoundVariablesHelper() {
        final BoundVarsVisitor bvv = new BoundVarsVisitor();
        bvv.visit(find());
        return bvv.getBoundVariables();
    }
    
} 