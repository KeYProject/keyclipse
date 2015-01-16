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

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableMap;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Choice;
import de.uka.ilkd.key.logic.IntIterator;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PIOPathIterator;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentChangeInfo;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.label.TermLabelManager;
import de.uka.ilkd.key.logic.label.TermLabelState;
import de.uka.ilkd.key.logic.op.IfThenElse;
import de.uka.ilkd.key.logic.op.Junctor;
import de.uka.ilkd.key.logic.op.ModalOperatorSV;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.logic.util.TermHelper;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.Taclet.TacletLabelHint.TacletOperation;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletGoalTemplate;
import de.uka.ilkd.key.rule.tacletbuilder.TacletGoalTemplate;

/** 
 * A RewriteTaclet represents a taclet, whose find can be matched against any
 * term in the sequent no matter where it occurs. The only constraint to be
 * fulfilled is that the term matches the structure described by the term of the
 * find-part.
 */
public class RewriteTaclet extends FindTaclet {

    /** does not pose state restrictions on valid matchings */
    public static final int NONE = 0;

    /** all taclet constituents must appear in the same state
     * (and not below a modality (for efficiency reasons)) */
    public static final int SAME_UPDATE_LEVEL = 1;

    /** all taclet constituents must be in the same state
     * as the sequent */
    public static final int IN_SEQUENT_STATE = 2;
    
    /**
     * If the surrounding formula has been decomposed completely, the find-term
     * will NOT appear on the SUCcedent. The formula "wellformed(h)" in
     * "wellformed(h) ==>" or in "==> wellformed(h) -> (inv(h) = inv(h2))"
     * or in "==> \if(b) \then(!wellformed(h)) \else(!wellformed(h2))"
     * has antecedent polarity. The formula "wellformed(h)" in
     * "wellformed(h) <-> wellformed(h2) ==>" has NO antecedent polarity.
     */
    public static final int ANTECEDENT_POLARITY = 4;
    
    /**
     * If the surrounding formula has been decomposed completely, the find-term
     * will NOT appear on the ANTEcedent. The formula "wellformed(h)" in
     * "==> wellformed(h)" or in "wellformed(h) -> (inv(h) = inv(h2)) ==>"
     * or in "\if(b) \then(!wellformed(h)) \else(!wellformed(h2)) ==>"
     * has succedent polarity. The formula "wellformed(h)" in
     * "wellformed(h) <-> wellformed(h2) ==>" has NO succedent polarity.
     */
    public static final int SUCCEDENT_POLARITY = 8;
    
    /**
     * encodes restrictions on the state where a rewrite taclet is applicable
     * If the value is equal to
     * <ul>
     * <li> {@link RewriteTaclet#NONE} no state restrictions are posed</li>
     * <li> {@link RewriteTaclet#SAME_UPDATE_LEVEL} then <code>\assumes</code>
     * must match on a formula within the same state as <code>\find</code>
     * rsp. <code>\add</code>. For efficiency no modalities are allowed above
     * the <code>\find</code> position  </li>
     * <li> {@link RewriteTaclet#IN_SEQUENT_STATE} the <code>\find</code> part is
     * only allowed to match on formulas which are evaluated in the same state as
     * the sequent</li>
     *</ul>
     */
    private int applicationRestriction;


    /**
     * creates a Schematic Theory Specific Rule (Taclet) with the given
     * parameters that represents a rewrite rule.  
     * @param name the Name of the Taclet 
     * @param applPart the TacletApplPart that contains the application part of an Taclet that is
     * the if-sequent, the variable conditions
     * @param goalTemplates a list of goal descriptions.
     * @param ruleSets a list of rule sets for the Taclet
     * @param attrs the TacletAttributes; these are boolean values
     * indicating a noninteractive or recursive use of the Taclet. 
     * @param find the find term of the Taclet
     * @param prefixMap a ImmMap<SchemaVariable,TacletPrefix> that contains the
     * prefix for each SchemaVariable in the Taclet
     * @param p_applicationRestriction an int defining state restrictions of the taclet
     * (required for location check)
     * @param choices the SetOf<Choices> to which this taclet belongs to
     */
    public RewriteTaclet(Name name, TacletApplPart applPart,  
			 ImmutableList<TacletGoalTemplate>  goalTemplates, 
			 ImmutableList<RuleSet>             ruleSets,
			 TacletAttributes          attrs,
			 Term                      find,
			 ImmutableMap<SchemaVariable,TacletPrefix> prefixMap, 
			 int                       p_applicationRestriction,
			 ImmutableSet<Choice> choices){
        this(name, applPart, goalTemplates, ruleSets, attrs, find, prefixMap,
             p_applicationRestriction, choices, false);
    }	

    public RewriteTaclet(Name name, TacletApplPart applPart,  
			 ImmutableList<TacletGoalTemplate>  goalTemplates, 
			 ImmutableList<RuleSet>             ruleSets,
			 TacletAttributes          attrs,
			 Term                      find,
			 ImmutableMap<SchemaVariable,TacletPrefix> prefixMap, 
			 int                       p_applicationRestriction,
			 ImmutableSet<Choice> choices,
             boolean surviveSymbExec){
	super(name, applPart, goalTemplates, ruleSets, attrs,
	      find, prefixMap, choices, surviveSymbExec);
	applicationRestriction = p_applicationRestriction;
	
	cacheMatchInfo();
    }	

      
    /** 
     * this method is used to determine if top level updates are
     * allowed to be ignored. This is the case if we have an Antec or
     * SuccTaclet but not for a RewriteTaclet
     * @return true if top level updates shall be ignored 
     */
    @Override
    protected boolean ignoreTopLevelUpdates() {
	return false;
    }
    
    /**
     * returns the int encoding the kind of state restriction this rewrite 
     * taclet must obey     
     * @return the int encoding the kind of state restriction this rewrite 
     * taclet must obey      
     */
    public int getApplicationRestriction () {
	return applicationRestriction;
    }


    /**
     * the top level operator has to be a simultaneous update. This method 
     * checks if the assignment pairs of the update contain free logic
     * variables and gives a veto if positive
     * @param t the Term to check
     * @return false if vetoing 
     */
    private boolean veto (Term t) {
        return t.freeVars ().size () > 0;
    }

    /**
     * For taclets with <code>getSameUpdatePrefix ()</code>, collect
     * the updates above <code>p_pos</code> and add them to the update
     * context of the instantiations object <code>p_mc</code>.
     * @return the new instantiations with the additional updates, or
     * <code>null</code>, if program modalities appear above
     * <code>p_pos</code>
     */
    public MatchConditions checkPrefix(PosInOccurrence p_pos,
                                       MatchConditions p_mc) {
	int polarity = p_pos.isInAntec() ? -1 : 1;  // init polarity
	SVInstantiations svi = p_mc.getInstantiations ();
	// this is assumed to hold
	assert p_pos.posInTerm () != null;

	PIOPathIterator it = p_pos.iterator ();
	Operator        op;
	while ( it.next () != -1 ) {
	    final Term t = it.getSubTerm ();
	    op = t.op ();
	    if (op instanceof Transformer) {
	        return null;
	    } else  if (op instanceof UpdateApplication &&
	            it.getChild () == UpdateApplication.targetPos() &&
	            getApplicationRestriction() != NONE) {
	        if ( (getApplicationRestriction() & IN_SEQUENT_STATE) != 0 || veto(t) ) {
	            return null;
	        } else {
	            Term update = UpdateApplication.getUpdate(t);
	            svi = svi.addUpdate(update, t.getLabels());
	        }
	    } else if (getApplicationRestriction() != NONE &&
	            (op instanceof Modality || op instanceof ModalOperatorSV)) {
	        return null;
	    }
	    
	    if (polarity != 0) {
	        polarity = polarity(op, it, polarity);
	    }
	}
	
	if (getApplicationRestriction() == NONE)
            return p_mc;
	if (((getApplicationRestriction() & ANTECEDENT_POLARITY) != 0 && polarity != -1) ||
	        ((getApplicationRestriction() & SUCCEDENT_POLARITY) != 0 && polarity != 1)) {
	    return null;
	}
	return p_mc.setInstantiations ( svi );
    }

    /**
     * Compute polarity
     * @see AntecSuccPrefixChecker seems to reimplement this.
     */
    private int polarity(final Operator op, final PIOPathIterator it, int polarity) {
                                                                // toggle polarity if find term is
                                                                // subterm of
        if ((op == Junctor.NOT) ||                              //   not
                (op == Junctor.IMP && it.getChild() == 0)) {    //   left hand side of implication
            polarity = polarity * -1;
                                                                // do not change polarity if find term
                                                                // is subterm of
        } else if ((op == Junctor.AND) ||                       //   and
                (op == Junctor.OR) ||                           //   or
                (op == Junctor.IMP && it.getChild() != 0) ||    //   right hand side of implication
                (op == IfThenElse.IF_THEN_ELSE &&
                    it.getChild() != 0)) {                      //   then or else part of if-then-else
            // do nothing
        } else {                                                // find term has no polarity in any
                                                                // other case
            polarity = 0;
        }
        return polarity;
    }

    /**
     * does the work for applyReplacewith (wraps recursion) 
     */
    private Term replace(TermLabelState termLabelState,
                         Term term,
                         Term with,
                         PosInOccurrence posOfFind,
                         IntIterator it,
                         Services services,
                         MatchConditions mc,
                         Sort maxSort,
                         TacletLabelHint labelHint,
                         Goal goal,
                         TacletApp tacletApp) {
	if (it.hasNext()) {
	    int sub = it.next();

	    final Term[] subs = new Term[term.arity()];

	    for (int i=0, arity = term.arity(); i<arity; i++) {

                if (i!=sub) {
		    subs[i] = term.sub(i);
		} else {
                    final Sort newMaxSort = TermHelper.getMaxSort(term, i, services);
		    subs[i] = replace(termLabelState, term.sub(i),
				      with,
			    	      posOfFind,
				      it,
				      services,
				      mc,
			    	      newMaxSort,
			    	     labelHint,
			    	     goal, 
			    	     tacletApp);
		}
	    }

	    return services.getTermFactory().createTerm(term.op(),
                                                  subs,
                                                  term.boundVars(),
                                                  term.javaBlock(),
	            				  term.getLabels());
	}

	with = syntacticalReplace(termLabelState, with, services, mc, posOfFind, labelHint, goal, tacletApp);

	if(!with.sort().extendsTrans(maxSort)) {
	    with = services.getTermBuilder().cast(maxSort, with);
	}

	return with;
    }


    private SequentFormula applyReplacewithHelper(Goal goal,
               TermLabelState termLabelState, 
	    			RewriteTacletGoalTemplate gt, 
				 	PosInOccurrence    posOfFind,
				 	Services           services,
				 	MatchConditions    matchCond,
				 	TacletApp tacletApp) {
	Term term = posOfFind.constrainedFormula().formula();
	IntIterator it = posOfFind.posInTerm().iterator();
	Term rwTemplate=gt.replaceWith();

	Term formula = replace(termLabelState,
	                       term,
	                       rwTemplate,
	                       posOfFind,
	                       it,
	                       services,
	                       matchCond,
	                       term.sort(),
	                       new TacletLabelHint(rwTemplate),
	                       goal,
	                       tacletApp);
	formula = TermLabelManager.refactorSequentFormula(termLabelState, services, formula, posOfFind, this, goal, null, rwTemplate);
	if(term == formula) {
	    return posOfFind.constrainedFormula();
	} else {
	    return new SequentFormula(formula);
	}
    }
    
    
    public SequentFormula getRewriteResult(Goal goal, TermLabelState termLabelState, Services services, TacletApp app) {
	assert goalTemplates().size() == 1;
	assert goalTemplates().head().sequent().isEmpty();	
	assert getApplicationRestriction() != IN_SEQUENT_STATE;
	assert app.complete();
	RewriteTacletGoalTemplate gt 
		= (RewriteTacletGoalTemplate) goalTemplates().head();
	return applyReplacewithHelper(goal, termLabelState,
	               gt,
				      app.posInOccurrence(),
				      services,
				      app.matchConditions(),
				      app);
    }


    /** 
     * applies the replacewith part of Taclets
     * @param gt TacletGoalTemplate used to get the replaceexpression in the Taclet
     * @param currentSequent
     *           the Sequent which is the current (intermediate) result of
     *           applying the taclet
     * @param posOfFind the PosInOccurrence belonging to the find expression
     * @param services the Services encapsulating all java information
     * @param matchCond the MatchConditions with all required instantiations 
     */
   @Override
   protected void applyReplacewith(Goal goal, TermLabelState termLabelState, TacletGoalTemplate gt,
         SequentChangeInfo currentSequent, PosInOccurrence posOfFind,
         Services services, MatchConditions matchCond, TacletApp tacletApp) {
      if (gt instanceof RewriteTacletGoalTemplate) {
         SequentFormula cf = applyReplacewithHelper(goal, termLabelState,
               (RewriteTacletGoalTemplate) gt, posOfFind, services, matchCond, tacletApp);

         currentSequent.combine(currentSequent.sequent().changeFormula(cf, posOfFind));
      }
      else {
         // Then there was no replacewith...
         // This is strange in a RewriteTaclet, but who knows...
         // However, term label refactorings have to be performed.
         Term oldFormula = posOfFind.constrainedFormula().formula();
         Term newFormula = TermLabelManager.refactorSequentFormula(termLabelState, services, oldFormula, posOfFind, this, goal, null, null);
         if (oldFormula != newFormula) {
            currentSequent.combine(currentSequent.sequent().changeFormula(new SequentFormula(newFormula), posOfFind));
         }
      }
   }

   /**
    * adds the sequent of the add part of the Taclet to the goal sequent
    * 
    * @param termLabelState The {@link TermLabelState} of the current rule application.
    * @param add
    *           the Sequent to be added
    * @param currentSequent
    *           the Sequent which is the current (intermediate) result of
    *           applying the taclet
    * @param posOfFind
    *           the PosInOccurrence describes the place where to add the
    *           semisequent
    * @param services
    *           the Services encapsulating all java information
    * @param matchCond
    *           the MatchConditions with all required instantiations
    */
   @Override
   protected void applyAdd(TermLabelState termLabelState, Sequent add,
         SequentChangeInfo currentSequent, PosInOccurrence posOfFind,
         Services services, MatchConditions matchCond, Goal goal, TacletApp tacletApp) {
      if (posOfFind.isInAntec()) {
         addToAntec(termLabelState, add.antecedent(), currentSequent, posOfFind, services, matchCond, posOfFind, new TacletLabelHint(TacletOperation.ADD_ANTECEDENT, add), goal, tacletApp);
         addToSucc(termLabelState, add.succedent(), currentSequent, null, services, matchCond, posOfFind, new TacletLabelHint(TacletOperation.ADD_SUCCEDENT, add), goal, tacletApp);
      }
      else {
         addToAntec(termLabelState, add.antecedent(), currentSequent, null, services, matchCond, posOfFind, new TacletLabelHint(TacletOperation.ADD_ANTECEDENT, add), goal, tacletApp);
         addToSucc(termLabelState, add.succedent(), currentSequent, posOfFind, services, matchCond, posOfFind, new TacletLabelHint(TacletOperation.ADD_SUCCEDENT, add), goal, tacletApp);
      }
   }
    
    @Override
    protected Taclet setName(String s) {
	final RewriteTacletBuilder b = new RewriteTacletBuilder();
	b.setFind(find());
	b.setApplicationRestriction ( getApplicationRestriction() );
	return super.setName(s, b);
    }


    @Override
    StringBuffer toStringFind(StringBuffer sb) {
	StringBuffer res = super.toStringFind ( sb );
	if ((getApplicationRestriction() & RewriteTaclet.SAME_UPDATE_LEVEL) != 0) {
            res.append("\\sameUpdateLevel");
        }
        if ((getApplicationRestriction() & RewriteTaclet.IN_SEQUENT_STATE) != 0) {
            res.append("\\inSequentState");
        }
        if ((getApplicationRestriction() & RewriteTaclet.ANTECEDENT_POLARITY) != 0) {
            res.append("\\antecedentPolarity");
        }
        if ((getApplicationRestriction() & RewriteTaclet.SUCCEDENT_POLARITY) != 0) {
            res.append("\\succedentPolarity");
        }
	return res;
    }
}
