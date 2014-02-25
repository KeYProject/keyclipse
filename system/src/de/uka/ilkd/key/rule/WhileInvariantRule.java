// This file is part of KeY - Integrated Deductive Software Design 
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany 
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2013 Karlsruhe Institute of Technology, Germany 
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General 
// Public License. See LICENSE.TXT for details.
// 


package de.uka.ilkd.key.rule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.uka.ilkd.key.collection.ImmutableArray;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.macros.WellDefinednessMacro;
import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.SourceElement;
import de.uka.ilkd.key.java.Statement;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.LocalVariableDeclaration;
import de.uka.ilkd.key.java.declaration.VariableSpecification;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.java.reference.TypeRef;
import de.uka.ilkd.key.java.statement.MethodFrame;
import de.uka.ilkd.key.java.statement.While;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.label.ParameterlessTermLabel;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.label.TermLabelManager;
import de.uka.ilkd.key.logic.op.Function;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.WhileInvariantTransformer;
import de.uka.ilkd.key.speclang.LoopInvariant;
import de.uka.ilkd.key.speclang.LoopWellDefinedness;
import de.uka.ilkd.key.speclang.WellDefinednessCheck;
import de.uka.ilkd.key.util.MiscTools;
import de.uka.ilkd.key.util.Pair;
import de.uka.ilkd.key.util.Triple;

public final class WhileInvariantRule implements BuiltInRule {


    public static final WhileInvariantRule INSTANCE = new WhileInvariantRule();

    private static final Name NAME = new Name("Loop Invariant");
    private Term lastFocusTerm;
    private Instantiation lastInstantiation;


    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------

    private WhileInvariantRule() {
    }


    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------

    private Instantiation instantiate(LoopInvariantBuiltInRuleApp app, Services services)
            throws RuleAbortException {

        Term focusTerm = app.posInOccurrence().subTerm();

        if(focusTerm == lastFocusTerm
                && lastInstantiation.inv 
                == services.getSpecificationRepository()
                .getLoopInvariant(lastInstantiation.loop)) {
            return lastInstantiation;
        }

        //leading update?
        Pair<Term, Term> update = applyUpdates(focusTerm, services);
        final Term u        = update.first;
        final Term progPost = update.second;

        //focus (below update) must be modality term
        if (!checkFocus(progPost)) {
            return null;
        }

        //active statement must be while loop
        final While loop = app.getLoopStatement();

        // try to get invariant from JML specification
        LoopInvariant inv = app.getInvariant(); 
        if (inv == null) // may happen after reloading proof 
            throw new RuleAbortException("no invariant found");

        //collect self, execution context
        final MethodFrame innermostMethodFrame 
            = JavaTools.getInnermostMethodFrame(progPost.javaBlock(), services);
        final Term selfTerm = innermostMethodFrame == null ?
                null
                : MiscTools.getSelfTerm(innermostMethodFrame, services);
        final ExecutionContext innermostExecutionContext 
            = innermostMethodFrame == null ?
                null
                : (ExecutionContext) innermostMethodFrame.getExecutionContext();

        //cache and return result
        Instantiation result = new Instantiation(u, 
                progPost, 
                loop, 
                inv,
                selfTerm, 
                innermostExecutionContext);
        lastFocusTerm = focusTerm;
        lastInstantiation = result;
        return result;
    }

    private Term createLocalAnonUpdate(ImmutableSet<ProgramVariable> localOuts, Services services) {
        Term anonUpdate = null;
        for(ProgramVariable pv : localOuts) {
            final String anonFuncName 
                = services.getTermBuilder().newName(services, pv.name().toString());
            final Function anonFunc 
                = new Function(new Name(anonFuncName), pv.sort());
            services.getNamespaces().functions().addSafely(anonFunc);
            final Term elemUpd = services.getTermBuilder().elementary(services, (LocationVariable)pv, services.getTermBuilder().func(anonFunc));
            if(anonUpdate == null) {
                anonUpdate = elemUpd;
            }else{
                anonUpdate = services.getTermBuilder().parallel(anonUpdate, elemUpd);
            }
        }
        return anonUpdate;
    }

    /**
     * @return (anon update, anon heap)
     */
    private Pair<Term,Term> createAnonUpdate(LocationVariable heap,
                                             While loop,
                                             Term mod,
                                             Services services) {
        final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
        final Name anonHeapName = new Name(services.getTermBuilder().newName(services, "anon_"+heap.name()+"_loop"));
        final Function anonHeapFunc = new Function(anonHeapName,
                heapLDT.targetSort());
        services.getNamespaces().functions().addSafely(anonHeapFunc);
        final Term anonHeapTerm = services.getTermBuilder().label(services.getTermBuilder().func(anonHeapFunc), ParameterlessTermLabel.ANON_HEAP_LABEL);

        // check for strictly pure loops
        final Term anonUpdate;
        if(services.getTermBuilder().strictlyNothing().equals(mod)) {
            anonUpdate = services.getTermBuilder().skip();
        } else {
            anonUpdate = services.getTermBuilder().anonUpd(heap, services, mod, anonHeapTerm);
        }

        return new Pair<Term,Term>(anonUpdate, anonHeapTerm);
    }

    private static boolean checkFocus(final Term progPost) {
        // focus (below update) must be modality term
        return progPost.op() instanceof Modality;
    }

    
    //-------------------------------------------------------------------------
    // helper methods for apply()
    //-------------------------------------------------------------------------

    private Term conjunctInv(Services services, Instantiation inst,
            final Map<LocationVariable, Term> atPres,
            final List<LocationVariable> heapContext) {
        Term invTerm = null;
        for(LocationVariable heap : heapContext) {
            final Term i = inst.inv.getInvariant(heap, inst.selfTerm, atPres, services);
            if(i == null) continue;
            if(invTerm == null) {
                invTerm = i;
            }else{
                invTerm = services.getTermBuilder().and(invTerm, i);
            }
        }
        return invTerm;
    }

    private Pair<Term,Term> prepareVariant (Instantiation inst, Term variant, Services services) {
        final ProgramElementName variantName 
            = new ProgramElementName(services.getTermBuilder().newName(services, "variant"));
        final LocationVariable variantPV = new LocationVariable(variantName, Sort.ANY);
        services.getNamespaces().programVariables().addSafely(variantPV);

        final boolean dia = ((Modality)inst.progPost.op()).terminationSensitive();
        final Term variantUpdate 
            = dia ? services.getTermBuilder().elementary(services, variantPV, variant) : services.getTermBuilder().skip();
        final Term variantPO = dia ? services.getTermBuilder().prec(variant, services.getTermBuilder().var(variantPV), services) : services.getTermBuilder().tt();
        return new Pair<Term,Term> (variantUpdate,variantPO);
    }


    private Term bodyTerm(Services services, RuleApp ruleApp,
            final Sequent applicationSequent, Instantiation inst,
            final Term invTerm, Term frameCondition, final Term variantPO,
            Goal bodyGoal, final JavaBlock guardJb, final Term guardTrueTerm) {
        final WhileInvariantTransformer wir = new WhileInvariantTransformer();
        SVInstantiations svInst 
        = SVInstantiations.EMPTY_SVINSTANTIATIONS.replace(
                null, 
                null, 
                inst.innermostExecutionContext, 
                null, 
                services);
        for(SchemaVariable sv : wir.neededInstantiations(inst.loop, svInst)) {
            assert sv instanceof ProgramSV;
            svInst = svInst.addInteresting(sv, 
                    (Name) new ProgramElementName(sv.name().toString()), 
                    services);
        }

        Term bodyTerm = wir.transform(this, 
                bodyGoal, 
                applicationSequent, 
                ruleApp.posInOccurrence(), 
                inst.progPost, 
                services.getTermBuilder().and(invTerm, frameCondition, variantPO),
                svInst, 
                services);
        final Term guardTrueBody = services.getTermBuilder().imp(services.getTermBuilder().box(guardJb,guardTrueTerm), 
                bodyTerm);
        return guardTrueBody;
    }


    private SequentFormula initFormula(Instantiation inst, final Term invTerm,
            Term reachableState, Services services) {
        return new SequentFormula(
                services.getTermBuilder().apply(inst.u, services.getTermBuilder().and(invTerm, reachableState), null));
    }

    private Term useCaseFormula(Services services, RuleApp ruleApp,
                                Instantiation inst, Goal useGoal, final JavaBlock guardJb,
                                final Term guardFalseTerm) {
        JavaBlock useJavaBlock = JavaTools.removeActiveStatement(inst.progPost.javaBlock(), services);
        final ImmutableArray<TermLabel> instantiateLabels =
                TermLabelManager.instantiateLabels(services, ruleApp.posInOccurrence(), this, useGoal,
                                                   "UseModality", null, inst.progPost.op(),
                                                   new ImmutableArray<Term>(inst.progPost.sub(0)),
                                                   null, useJavaBlock);
        Term restPsi = services.getTermBuilder().prog((Modality)inst.progPost.op(), useJavaBlock, inst.progPost.sub(0),
                               instantiateLabels);
        Term guardFalseRestPsi = services.getTermBuilder().box(guardJb, 
                services.getTermBuilder().imp(guardFalseTerm, restPsi));
        return guardFalseRestPsi;
    }

    private Triple<JavaBlock, Term, Term> prepareGuard(final Instantiation inst,
                                                       final KeYJavaType booleanKJT,
                                                       final Services services) {
        final ProgramElementName guardVarName 
        = new ProgramElementName(services.getTermBuilder().newName(services, "b"));
        final LocationVariable guardVar = new LocationVariable(guardVarName, 
                booleanKJT);
        services.getNamespaces().programVariables().addSafely(guardVar);        
        final VariableSpecification guardVarSpec 
        = new VariableSpecification(guardVar, 
                inst.loop.getGuardExpression(), 
                booleanKJT);
        final LocalVariableDeclaration guardVarDecl 
        = new LocalVariableDeclaration(new TypeRef(booleanKJT), 
                guardVarSpec);
        final Statement guardVarMethodFrame 
        = inst.innermostExecutionContext == null ?
                guardVarDecl
                : new MethodFrame(null, inst.innermostExecutionContext,
                                  new StatementBlock(guardVarDecl));
        final JavaBlock guardJb 
        = JavaBlock.createJavaBlock(new StatementBlock(
                guardVarMethodFrame));
        final Term guardTrueTerm = services.getTermBuilder().equals(services.getTermBuilder().var(guardVar), 
                services.getTermBuilder().TRUE(services));
        final Term guardFalseTerm = services.getTermBuilder().equals(services.getTermBuilder().var(guardVar), 
                services.getTermBuilder().FALSE(services));
        return new Triple<JavaBlock, Term, Term>(guardJb,guardTrueTerm,guardFalseTerm);
    }

    private void prepareInvInitiallyValidBranch(Services services,
            RuleApp ruleApp, Instantiation inst, final Term invTerm,
            Term reachableState, Goal initGoal) {
        initGoal.setBranchLabel("Invariant Initially Valid");
        initGoal.changeFormula(initFormula(inst, invTerm, reachableState, services),
                        ruleApp.posInOccurrence());
        TermLabelManager.refactorLabels(services, ruleApp.posInOccurrence(), this, initGoal, null);
    }


    private void prepareBodyPreservesBranch(Services services, RuleApp ruleApp,
            final Sequent applicationSequent, Instantiation inst,
            final Term invTerm, Term wellFormedAnon, Term frameCondition,
            final Term variantPO, Goal bodyGoal, final JavaBlock guardJb,
            final Term guardTrueTerm, final Term[] uBeforeLoopDefAnonVariant,
            final Term uAnonInv) {
        bodyGoal.setBranchLabel("Body Preserves Invariant");
        bodyGoal.addFormula(new SequentFormula(wellFormedAnon), 
                true, 
                false);         

        bodyGoal.addFormula(new SequentFormula(uAnonInv), 
                true, 
                false);

        final Term guardTrueBody = bodyTerm(services, ruleApp,
                applicationSequent, inst, invTerm, frameCondition, variantPO,
                bodyGoal, guardJb, guardTrueTerm); 

        bodyGoal.changeFormula(new SequentFormula(services.getTermBuilder().applySequential(
                uBeforeLoopDefAnonVariant, 
                guardTrueBody)), 
                ruleApp.posInOccurrence());
    }


    private void prepareUseCaseBranch(Services services, RuleApp ruleApp,
            Instantiation inst, Term wellFormedAnon, Goal useGoal,
            final JavaBlock guardJb, final Term guardFalseTerm,
            final Term[] uAnon, final Term uAnonInv) {
        useGoal.setBranchLabel("Use Case");
        useGoal.addFormula(new SequentFormula(wellFormedAnon), 
                true, 
                false);         
        useGoal.addFormula(new SequentFormula(uAnonInv), true, false);

        Term guardFalseRestPsi = useCaseFormula(services, ruleApp, inst,
                useGoal, guardJb, guardFalseTerm);
        useGoal.changeFormula(new SequentFormula(services.getTermBuilder().applySequential(
                uAnon,
                guardFalseRestPsi)), 
                ruleApp.posInOccurrence());
    }
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------

    @Override
    public boolean isApplicable(Goal goal, 
            PosInOccurrence pio) {
        return checkApplicability(goal,pio);
    }


    //focus must be top level succedent
    static boolean checkApplicability (Goal g, PosInOccurrence pio){
        if (pio == null || !pio.isTopLevel() || pio.isInAntec()) {
            return false;
        }
        // abort if inside of transformer
        if (Transformer.inTransformer(pio)) {
            return false;
        }
        Pair<Term, Term> up = applyUpdates(pio.subTerm(), g.proof().getServices());
        final Term progPost = up.second;

        if (!checkFocus(progPost)) {
            return false;
        }

        // active statement must be while loop
        SourceElement activeStatement = JavaTools.getActiveStatement(progPost.javaBlock());
        if (!(activeStatement instanceof While)) {
            return false;
        }
        return true;
    }


    static Pair<Term, Term> applyUpdates(Term focusTerm, Services services) {
        if (focusTerm.op() instanceof UpdateApplication) {
            return new Pair<Term, Term>(UpdateApplication.getUpdate(focusTerm),
                    UpdateApplication.getTarget(focusTerm));
        } else {
            return new Pair<Term, Term>(services.getTermBuilder().skip(), focusTerm);
        }
    }

    private void setupWdGoal(final Goal goal, final LoopInvariant inv,
                             final Term update, final Term selfTerm,
                             final LocationVariable heap, final Term anonHeap,
                             final ImmutableSet<ProgramVariable> localIns,
                             PosInOccurrence pio, Services services) {
        if (goal == null) {
            return;
        }
        final LoopWellDefinedness lwd =  new LoopWellDefinedness(inv, localIns, services);
        final ProgramVariable self;
        if(selfTerm != null && selfTerm.op() instanceof ProgramVariable) {
            self = (ProgramVariable)selfTerm.op();
        } else {
            self = null;
        }
        services.getSpecificationRepository().addWdStatement(lwd);
        final SequentFormula wdInv = lwd.generateSequent(self, heap, anonHeap, localIns,
                                                         update, services);
        goal.changeFormula(wdInv, pio);
    }

    @Override
    public ImmutableList<Goal> apply(Goal goal, Services services, RuleApp ruleApp)
            throws RuleAbortException {
        final Sequent applicationSequent = goal.sequent();
        final KeYJavaType booleanKJT = services.getTypeConverter().getBooleanType();

        //get instantiation
        Instantiation inst = instantiate((LoopInvariantBuiltInRuleApp) ruleApp, services);	

        final Map<LocationVariable,Term> atPres = inst.inv.getInternalAtPres();
        final List<LocationVariable> heapContext = ((IBuiltInRuleApp)ruleApp).getHeapContext();
        final Term invTerm = conjunctInv(services, inst, atPres, heapContext);

        final Map<LocationVariable,Term> mods = new LinkedHashMap<LocationVariable,Term>();
        for(LocationVariable heap : heapContext) {
            final Term m = inst.inv.getModifies(heap, inst.selfTerm, atPres, services);
            mods.put(heap, m);
        }

        final Term variant = inst.inv.getVariant(inst.selfTerm, 
                atPres, 
                services);
        
        //collect input and output local variables, 
        //prepare reachableIn and reachableOut
        final ImmutableSet<ProgramVariable> localIns 
            = MiscTools.getLocalIns(inst.loop, services);
        Term reachableIn = services.getTermBuilder().tt();
        for(ProgramVariable pv : localIns) {
            reachableIn = services.getTermBuilder().and(reachableIn, 
                    services.getTermBuilder().reachableValue(services, pv));
        }	
        final ImmutableSet<ProgramVariable> localOuts 
            = MiscTools.getLocalOuts(inst.loop, services);
        Term reachableOut = services.getTermBuilder().tt();
        for(ProgramVariable pv : localOuts) {
            reachableOut = services.getTermBuilder().and(reachableOut, 
                    services.getTermBuilder().reachableValue(services, pv));
        }

        Term beforeLoopUpdate = null;
        final Map<LocationVariable,Map<Term,Term>> heapToBeforeLoop =
                new LinkedHashMap<LocationVariable,Map<Term,Term>>();

        for(LocationVariable heap : heapContext) {
            heapToBeforeLoop.put(heap, new LinkedHashMap<Term,Term>());
            final LocationVariable lv =
                    services.getTermBuilder().heapAtPreVar(services, heap.name()+"BeforeLoop", heap.sort(), true);
            services.getNamespaces().programVariables().addSafely(lv);
            final Term u = services.getTermBuilder().elementary(services, lv, services.getTermBuilder().var(heap));
            if(beforeLoopUpdate == null) {
                beforeLoopUpdate = u;
            }else{
                beforeLoopUpdate = services.getTermBuilder().parallel(beforeLoopUpdate, u);
            }
            heapToBeforeLoop.get(heap).put(services.getTermBuilder().var(heap), services.getTermBuilder().var(lv));
        }

        for(ProgramVariable pv : localOuts) {
            final String pvBeforeLoopName 
            = services.getTermBuilder().newName(services, pv.name().toString() + "BeforeLoop");
            final LocationVariable pvBeforeLoop 
            = new LocationVariable(new ProgramElementName(pvBeforeLoopName), 
                    pv.getKeYJavaType());
            services.getNamespaces().programVariables().addSafely(pvBeforeLoop);
            beforeLoopUpdate = services.getTermBuilder().parallel(beforeLoopUpdate, 
                    services.getTermBuilder().elementary(services, 
                            pvBeforeLoop, 
                            services.getTermBuilder().var(pv)));
            heapToBeforeLoop.get(services.getTypeConverter().getHeapLDT().getHeap()).put(
                    services.getTermBuilder().var(pv), services.getTermBuilder().var(pvBeforeLoop));
        }

        //prepare anon update, frame condition, etc.
        Term anonUpdate = createLocalAnonUpdate(localOuts, services); // can still be null
        Term wellFormedAnon = null;
        Term frameCondition = null;
        Term reachableState = reachableIn;
        Term anonHeap = null;
        for(LocationVariable heap : heapContext) {
            final Pair<Term,Term> tAnon 
            = createAnonUpdate(heap, inst.loop, mods.get(heap), services);
            if(anonUpdate == null) {
                anonUpdate = tAnon.first;
            }else{
                anonUpdate = services.getTermBuilder().parallel(anonUpdate, tAnon.first);
            }            
            if(wellFormedAnon == null) {
                wellFormedAnon = services.getTermBuilder().wellFormed(tAnon.second, services);
            } else {
                wellFormedAnon = services.getTermBuilder().and(wellFormedAnon, services.getTermBuilder().wellFormed(tAnon.second, services));
            }
            if (anonHeap == null) {
                anonHeap = tAnon.second;
            }
            final Term m = mods.get(heap);
            final Term fc;
            if(services.getTermBuilder().strictlyNothing().equals(m)) {
                fc = services.getTermBuilder().frameStrictlyEmpty(services, services.getTermBuilder().var(heap), heapToBeforeLoop.get(heap)); 
            }else{
                fc = services.getTermBuilder().frame(services, services.getTermBuilder().var(heap), heapToBeforeLoop.get(heap), m);
            }
            if(frameCondition == null){
                frameCondition = fc;
            }else{
                frameCondition = services.getTermBuilder().and(frameCondition, fc);
            }
            reachableState = services.getTermBuilder().and(reachableState, services.getTermBuilder().wellFormed(heap, services));
        }
        //prepare variant
        final Pair<Term,Term> variantPair = prepareVariant(inst, variant, services);
        final Term variantUpdate = variantPair.first;
        final Term variantPO = variantPair.second;

	final ImmutableList<Goal> result;
	Goal wdGoal;
	if (WellDefinednessCheck.isOn()) {
	    //split goal into four branches
	    result = goal.split(4);
	    wdGoal = result.tail().tail().tail().head();
	    wdGoal.setBranchLabel(WellDefinednessMacro.WD_BRANCH);
	} else {
	    //split goal into three branches
	    result = goal.split(3);
	    wdGoal = null;
	}
	Goal initGoal = result.tail().tail().head();
        Goal bodyGoal = result.tail().head();
        Goal useGoal = result.head();

        //prepare guard
        final Triple<JavaBlock,Term,Term> guardStuff = prepareGuard(inst, booleanKJT, services);
        final JavaBlock guardJb = guardStuff.first;
        final Term guardTrueTerm = guardStuff.second;
        final Term guardFalseTerm = guardStuff.third;

	//prepare common assumption
	final Term[] uAnon 
		= new Term[]{inst.u, anonUpdate};
	final Term[] uBeforeLoopDefAnonVariant 
		= new Term[]{inst.u, 
		             beforeLoopUpdate, 
		             anonUpdate, 
		             variantUpdate};
	final Term uAnonInv 
		= services.getTermBuilder().applySequential(uAnon, services.getTermBuilder().and(invTerm, reachableOut));

	//"Invariant Initially Valid":
	// \replacewith (==> inv );
	prepareInvInitiallyValidBranch(services, ruleApp, inst, invTerm, reachableState, initGoal);

	setupWdGoal(wdGoal, inst.inv, inst.u, inst.selfTerm, heapContext.get(0),
	            anonHeap, localIns, ruleApp.posInOccurrence(), services);

	//"Body Preserves Invariant":
        // \replacewith (==>  #atPreEqs(anon1) 
        //                       -> #introNewAnonUpdate(#modifies, #locDepFunc(anon1, \[{.. while (#e) #s ...}\]post) & inv -> 
        //                         (\[{ method-frame(#ex):{#typeof(#e) #v1 = #e;} }\]#v1=TRUE ->
        //                          #whileInvRule(\[{.. while (#e) #s ...}\]post, 
        //                               #locDepFunc(anon1, \[{.. while (#e) #s ...}\]post) & inv)),anon1));
        prepareBodyPreservesBranch(services, ruleApp, applicationSequent, inst,
                invTerm, wellFormedAnon, frameCondition, variantPO, bodyGoal,
                guardJb, guardTrueTerm, uBeforeLoopDefAnonVariant, uAnonInv);

        // "Use Case":
        // \replacewith (==> #introNewAnonUpdate(#modifies, inv ->
        // (\[{ method-frame(#ex):{#typeof(#e) #v1 = #e;} }\]
        // (#v1=FALSE -> \[{.. ...}\]post)),anon2))
        prepareUseCaseBranch(services, ruleApp, inst, wellFormedAnon, useGoal,
                guardJb, guardFalseTerm, uAnon, uAnonInv);
        return result;
    }



    @Override
    public Name name() {
        return NAME;
    }


    @Override
    public String displayName() {
        return toString();
    }


    @Override
    public String toString() {
        return NAME.toString();
    }


    @Override
    public LoopInvariantBuiltInRuleApp createApp(PosInOccurrence pos, Services services) {
        return new LoopInvariantBuiltInRuleApp(this, pos, services);
    }

    //-------------------------------------------------------------------------
    //inner classes
    //-------------------------------------------------------------------------

    private static final class Instantiation {
        public final Term u;
        public final Term progPost;
        public final While loop;
        public final LoopInvariant inv;
        public final Term selfTerm;
        public final ExecutionContext innermostExecutionContext;

        public Instantiation(Term u, 
                Term progPost, 
                While loop,
                LoopInvariant inv, 
                Term selfTerm,
                ExecutionContext innermostExecutionContext) {
            assert u != null;
            assert u.sort() == Sort.UPDATE;
            assert progPost != null;
            assert progPost.sort() == Sort.FORMULA;
            assert loop != null;
            assert inv != null;
            this.u = u;
            this.progPost = progPost;
            this.loop = loop;
            this.inv = inv;
            this.selfTerm = selfTerm;
            this.innermostExecutionContext = innermostExecutionContext;
        }
    }


}
