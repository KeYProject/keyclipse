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

package de.uka.ilkd.key.strategy;

import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Named;
import de.uka.ilkd.key.logic.Namespace;
import de.uka.ilkd.key.logic.NamespaceSet;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.PosInTerm;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.rulefilter.IHTacletFilter;
import de.uka.ilkd.key.proof.rulefilter.TacletFilter;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.rule.RuleSet;
import de.uka.ilkd.key.strategy.feature.ApplyTFFeature;
import de.uka.ilkd.key.strategy.feature.CompareCostsFeature;
import de.uka.ilkd.key.strategy.feature.ComprehendedSumFeature;
import de.uka.ilkd.key.strategy.feature.ConditionalFeature;
import de.uka.ilkd.key.strategy.feature.ConstFeature;
import de.uka.ilkd.key.strategy.feature.Feature;
import de.uka.ilkd.key.strategy.feature.ImplicitCastNecessary;
import de.uka.ilkd.key.strategy.feature.InstantiatedSVFeature;
import de.uka.ilkd.key.strategy.feature.LetFeature;
import de.uka.ilkd.key.strategy.feature.RuleSetDispatchFeature;
import de.uka.ilkd.key.strategy.feature.ShannonFeature;
import de.uka.ilkd.key.strategy.feature.SortComparisonFeature;
import de.uka.ilkd.key.strategy.feature.SumFeature;
import de.uka.ilkd.key.strategy.feature.TriggerVarInstantiatedFeature;
import de.uka.ilkd.key.strategy.feature.instantiator.BackTrackingManager;
import de.uka.ilkd.key.strategy.feature.instantiator.ForEachCP;
import de.uka.ilkd.key.strategy.feature.instantiator.OneOfCP;
import de.uka.ilkd.key.strategy.feature.instantiator.SVInstantiationCP;
import de.uka.ilkd.key.strategy.termProjection.ProjectionToTerm;
import de.uka.ilkd.key.strategy.termProjection.SVInstantiationProjection;
import de.uka.ilkd.key.strategy.termProjection.SubtermProjection;
import de.uka.ilkd.key.strategy.termProjection.TermBuffer;
import de.uka.ilkd.key.strategy.termProjection.TermConstructionProjection;
import de.uka.ilkd.key.strategy.termProjection.TriggerVariableInstantiationProjection;
import de.uka.ilkd.key.strategy.termfeature.BinarySumTermFeature;
import de.uka.ilkd.key.strategy.termfeature.ConstTermFeature;
import de.uka.ilkd.key.strategy.termfeature.EqTermFeature;
import de.uka.ilkd.key.strategy.termfeature.OperatorTF;
import de.uka.ilkd.key.strategy.termfeature.PrintTermFeature;
import de.uka.ilkd.key.strategy.termfeature.RecSubTermFeature;
import de.uka.ilkd.key.strategy.termfeature.ShannonTermFeature;
import de.uka.ilkd.key.strategy.termfeature.SortExtendsTransTermFeature;
import de.uka.ilkd.key.strategy.termfeature.SubTermFeature;
import de.uka.ilkd.key.strategy.termfeature.TermFeature;
import de.uka.ilkd.key.strategy.termgenerator.TermGenerator;

public abstract class AbstractFeatureStrategy implements Strategy {

    private final Proof proof;

    protected AbstractFeatureStrategy (Proof proof) {
        this.proof = proof;
    }

    /**
     * @return Returns the proof.
     */
    protected Proof getProof () {
        return proof;
    }

    /**
     * @param heuristics
     * @param thenFeature
     * @return the conditional feature return true when the rule belongs to one of the given heuristics
     */
    protected Feature ifHeuristics (String[] heuristics, Feature thenFeature) {
        return ConditionalFeature.createConditional (
                getFilterFor ( heuristics ),
                thenFeature );
    }

    /**
     * @param heuristics
     * @param thenFeature
     * @param elseFeature
     */
    protected Feature ifHeuristics (String[] heuristics,
                                    Feature thenFeature,
                                    Feature elseFeature) {
        return ConditionalFeature.createConditional ( getFilterFor ( heuristics ),
                                                      thenFeature,
                                                      elseFeature );
    }

    protected static Feature longConst (long a) {
        return ConstFeature.createConst ( c ( a ) );
    }

    protected static Feature inftyConst () {
        return ConstFeature.createConst ( infty () );
    }
    
    protected static TermFeature any() {
        return longTermConst ( 0 );
    }
    
    protected static TermFeature longTermConst(long a) {
        return ConstTermFeature.createConst ( c ( a ) );
    }

    protected static TermFeature inftyTermConst() {
        return ConstTermFeature.createConst ( infty () );
    }

    protected static Feature add (Feature a, Feature b) {
        return SumFeature.createSum ( a, b );
    }

    protected static Feature add (Feature a, Feature b, Feature c) {
        return SumFeature.createSum ( a, b, c );
    }

    protected static Feature add (Feature... features) {
        return SumFeature.createSum ( features );
    }

    protected static TermFeature add (TermFeature a, TermFeature b) {
        return BinarySumTermFeature.createSum ( a, b );
    }

    protected static TermFeature add (TermFeature a, TermFeature b, TermFeature c) {
        // could be done more efficiently
        return add ( a, add ( b, c ) );
    }

    protected static TermFeature or(TermFeature a, TermFeature b) {
        return ifZero ( a, longTermConst ( 0 ), b );
    }

    protected static TermFeature or(TermFeature a, TermFeature b, TermFeature c) {
        return or ( a, or ( b, c ) );
    }

    protected static Feature or(Feature a, Feature b) {
        return ifZero ( a, longConst ( 0 ), b );
    }
    
    protected static Feature or(Feature a, Feature b, Feature c) {
        return or ( a, or ( b, c ) );
    }
    
    protected static Feature or(Feature... features) {
        Feature orFeature = inftyConst();
        for (Feature f : features) {
            orFeature = or( orFeature, f);
        }
        return orFeature;
    }

    protected static Feature ifZero (Feature cond, Feature thenFeature) {
        return ShannonFeature.createConditionalBinary ( cond, thenFeature );
    }

    protected static Feature ifZero (Feature cond, Feature thenFeature, Feature elseFeature) {
        return ShannonFeature.createConditionalBinary ( cond,
                                                        thenFeature,
                                                        elseFeature );
    }

    protected static TermFeature ifZero (TermFeature cond, TermFeature thenFeature) {
        return ShannonTermFeature.createConditionalBinary ( cond, thenFeature );
    }

    protected static TermFeature ifZero (TermFeature cond,
                                  TermFeature thenFeature,
                                  TermFeature elseFeature) {
        return ShannonTermFeature.createConditionalBinary ( cond,
                                                            thenFeature,
                                                            elseFeature );
    }

    protected static Feature not(Feature f) {
        return ifZero ( f, inftyConst (), longConst ( 0 ) );
    }
    
    protected static TermFeature not(TermFeature f) {
        return ifZero ( f,
                        ConstTermFeature.createConst(TopRuleAppCost.INSTANCE),
                        longTermConst(0) );
    }
    
    protected static Feature eq(Feature a, Feature b) {
        return CompareCostsFeature.eq ( a, b );
    }
    
    protected static Feature less(Feature a, Feature b) {
        return CompareCostsFeature.less ( a, b );
    }

    protected static Feature leq(Feature a, Feature b) {
        return CompareCostsFeature.leq ( a, b );
    }

    protected Feature ifHeuristics (String[] names, int priority) {
        return ConditionalFeature.createConditional ( getFilterFor ( names ),
                c ( priority ), c ( 0 ) );
    }

    private static RuleAppCost c (long p) {
        return NumberRuleAppCost.create ( p );
    }

    private static RuleAppCost infty () {
        return TopRuleAppCost.INSTANCE;
    }

    protected TacletFilter getFilterFor (String[] p_names) {
        ImmutableList<RuleSet> heur = ImmutableSLList.<RuleSet>nil();
        for ( int i = 0; i != p_names.length; ++i )
            heur = heur.prepend ( getHeuristic ( p_names[i] ) );
        return new IHTacletFilter ( false, heur );
    }

    protected RuleSet getHeuristic (String p_name) {
        final NamespaceSet nss = getProof ().getNamespaces ();
        
        assert nss != null : "Rule set namespace not available."; 
        
        final Namespace ns = nss.ruleSets ();
        final Named h = ns.lookup ( new Name ( p_name ) );
    
        assert h != null : "Did not find the rule set " + p_name;
    
        return (RuleSet)h;
    }

    protected void bindRuleSet(RuleSetDispatchFeature d,
                               RuleSet ruleSet, Feature f) {
        d.add ( ruleSet, f );
    }

    protected void bindRuleSet(RuleSetDispatchFeature d,
                               RuleSet ruleSet, long cost) {
        bindRuleSet ( d, ruleSet, longConst ( cost ) );
    }

    protected void bindRuleSet(RuleSetDispatchFeature d,
                               String ruleSet, Feature f) {
        bindRuleSet ( d, getHeuristic ( ruleSet ), f );
    }

    protected void bindRuleSet(RuleSetDispatchFeature d,
                               String ruleSet, long cost) {
        bindRuleSet ( d, getHeuristic ( ruleSet ), longConst ( cost ) );
    }

    protected void clearRuleSetBindings(RuleSetDispatchFeature d,
                                        RuleSet ruleSet) {
        d.clear ( ruleSet );
    }

    protected void clearRuleSetBindings(RuleSetDispatchFeature d,
                                        String ruleSet) {
        d.clear ( getHeuristic ( ruleSet ) );
    }

    /**
     * Create a projection of taclet applications to the instantiation of the
     * trigger variable of a taclet. If the trigger variable
     * is not instantiated for a particular taclet app, an error will be raised
     */
    protected static ProjectionToTerm instOfTriggerVariable() {
        return new TriggerVariableInstantiationProjection();
    }

    
    /**
     * Create a projection of taclet applications to the instantiation of the
     * schema variables <code>schemaVar</code>. If <code>schemaVar</code>
     * is not instantiated for a particular taclet app, an error will be raised
     */
    protected static ProjectionToTerm instOf(String schemaVar) {
        return SVInstantiationProjection.create ( new Name ( schemaVar ), true );
    }

    /**
     * Create a projection of taclet applications to the instantiation of the
     * schema variables <code>schemaVar</code>. The projection will be
     * partial and undefined for those taclet applications that do not
     * instantiate <code>schemaVar</code>
     */
    protected static ProjectionToTerm instOfNonStrict(String schemaVar) {
        return SVInstantiationProjection.create ( new Name ( schemaVar ), false );
    }
    
    protected static ProjectionToTerm subAt(ProjectionToTerm t, PosInTerm pit) {
        return SubtermProjection.create ( t, pit );
    }
    
    protected static ProjectionToTerm sub(ProjectionToTerm t, int index) {
        return SubtermProjection.create ( t, PosInTerm.getTopLevel().down ( index ) );
    }
    
    protected static ProjectionToTerm opTerm(Operator op, ProjectionToTerm[] subTerms) {
        return TermConstructionProjection.create ( op, subTerms );
    }

    protected static ProjectionToTerm opTerm(Operator op, ProjectionToTerm subTerm) {
        return opTerm ( op, new ProjectionToTerm[] { subTerm } );
    }

    protected static ProjectionToTerm opTerm(Operator op,
                                      ProjectionToTerm subTerm0,
                                      ProjectionToTerm subTerm1) {
        return opTerm ( op, new ProjectionToTerm[] { subTerm0, subTerm1 } );
    }

    protected static Feature isInstantiated(String schemaVar) {
        return InstantiatedSVFeature.create ( new Name ( schemaVar ) );
    }

    protected static Feature isTriggerVariableInstantiated() {
        return TriggerVarInstantiatedFeature.INSTANCE;
    }
    
    protected static TermFeature op(Operator op) {
        return OperatorTF.create ( op );
    }

    protected static TermFeature rec(TermFeature cond, TermFeature summand) {
        return RecSubTermFeature.create ( cond, summand );
    }
    
    protected static TermFeature sub(TermFeature sub0) {
        return SubTermFeature.create ( new TermFeature[] { sub0 } );
    }
    
    protected static TermFeature sub(TermFeature sub0, TermFeature sub1) {
        return SubTermFeature.create ( new TermFeature[] { sub0, sub1 } );
    }
    
    protected static TermFeature opSub(Operator op, TermFeature sub0) {
        return add ( op ( op ), sub ( sub0 ) );
    }    
    
    protected static TermFeature opSub(Operator op,
                                TermFeature sub0, TermFeature sub1) {
        return add ( op ( op ), sub ( sub0, sub1 ) );
    }    

    protected static TermFeature eq(TermBuffer t) {
        return EqTermFeature.create ( t );
    }
    
    protected static Feature eq(ProjectionToTerm t1, ProjectionToTerm t2) {
        final TermBuffer buf = new TermBuffer ();
        return let ( buf, t1, applyTF ( t2, eq ( buf ) ) );
    }

    protected static Feature contains(ProjectionToTerm bigTerm,
                               ProjectionToTerm searchedTerm) {
        final TermBuffer buf = new TermBuffer ();
        return let ( buf, searchedTerm,
                     applyTF ( bigTerm,
                               not ( rec ( any (), not ( eq ( buf ) ) ) ) ) );
    }
    
    protected static Feature println(ProjectionToTerm t) {
        return applyTF ( t, PrintTermFeature.INSTANCE );
    }
    
    protected static TermFeature extendsTrans(Sort s) {
        return SortExtendsTransTermFeature.create ( s );
    }
    
    /**
     * Invoke the term feature <code>tf</code> on the term that instantiation
     * of <code>schemaVar</code>. This is the strict/safe version that raises an
     * error of <code>schemaVar</code> is not instantiated for a particular
     * taclet app
     */
    protected static Feature applyTF(String schemaVar, TermFeature tf) {
        return applyTF ( instOf ( schemaVar ), tf );
    }

    /**
     * Invoke the term feature <code>tf</code> on the term that instantiation
     * of <code>schemaVar</code>. This is the non-strict/unsafe version that
     * simply returns zero if <code>schemaVar</code> is not instantiated for a
     * particular taclet app
     */
    protected static Feature applyTFNonStrict(String schemaVar, TermFeature tf) {
        return applyTFNonStrict ( instOfNonStrict ( schemaVar ), tf );
    }

    /**
     * Evaluate the term feature <code>tf</code> for the term described by the
     * projection <code>term</code>. If <code>term</code> is undefined for
     * a particular rule app, an exception is raised
     */
    protected static Feature applyTF(ProjectionToTerm term, TermFeature tf) {
        return ApplyTFFeature.create ( term, tf );
    }

    /**
     * Evaluate the term feature <code>tf</code> for the term described by the
     * projection <code>term</code>. If <code>term</code> is undefined for
     * a particular rule app, zero is returned
     */
    protected static Feature applyTFNonStrict(ProjectionToTerm term, TermFeature tf) {
        return ApplyTFFeature.createNonStrict ( term, tf, NumberRuleAppCost.getZeroCost() );
    }

    
    private final BackTrackingManager btManager = new BackTrackingManager ();
    
    protected BackTrackingManager getBtManager() {
        return btManager;
    }

    public final void instantiateApp ( RuleApp              app,
                                       PosInOccurrence      pio,
                                       Goal                 goal,
                                       RuleAppCostCollector collector ) {
        getBtManager ().setup ( app );
        do {
            final RuleAppCost cost = instantiateApp ( app, pio, goal );
            if ( cost instanceof TopRuleAppCost ) continue;
            final RuleApp res = getBtManager ().getResultingapp ();
            if ( res == app || res == null ) continue;
            collector.collect ( res, cost );
        } while ( getBtManager ().backtrack () );
    }
 
    protected abstract RuleAppCost instantiateApp (RuleApp              app,
                                                   PosInOccurrence      pio,
                                                   Goal                 goal);
    
    protected Feature forEach(TermBuffer x, TermGenerator gen, Feature body) {
        return ForEachCP.create ( x, gen, body, getBtManager () );
    }

    protected Feature oneOf(Feature[] features) {
        return OneOfCP.create ( features, getBtManager () );
    }
    
    protected Feature oneOf(Feature feature0, Feature feature1) {
        return oneOf ( new Feature[] { feature0, feature1 } );
    }
    
    protected static Feature sum(TermBuffer x, TermGenerator gen, Feature body) {
        return ComprehendedSumFeature.create ( x, gen, body );
    }
    
    // it is possible to turn off the method <code>instantiate</code>,
    // which can be useful in order to use the same feature definitions both for
    // cost computation and instantiation
    
    private boolean instantiateActive = false;
    protected void enableInstantiate() {
        instantiateActive = true;
    }
    protected void disableInstantiate() {
        instantiateActive = false;
    }
    
    protected Feature instantiate(Name sv, ProjectionToTerm value) {
        if ( instantiateActive )
            return SVInstantiationCP.create ( sv, value, getBtManager () );
        else
            return longConst ( 0 );
    }

    protected Feature instantiateTriggeredVariable(ProjectionToTerm value) {
        if ( instantiateActive )
            return SVInstantiationCP.createTriggeredVarCP( value, getBtManager () );
        else
            return longConst ( 0 );
    }

    protected static Feature let(TermBuffer x, ProjectionToTerm value, Feature body) {
        return LetFeature.create ( x, value, body );
    }    
    
    protected Feature instantiate(String sv, ProjectionToTerm value) {
        return instantiate ( new Name ( sv ), value );
    }

    
    protected static Feature isSubSortFeature(ProjectionToTerm s1, ProjectionToTerm s2) {        
        return SortComparisonFeature.create(s1, s2, SortComparisonFeature.SUBSORT);
    }
    
    protected static Feature implicitCastNecessary(ProjectionToTerm s1) {
        return ImplicitCastNecessary.create(s1);
    }
    
}