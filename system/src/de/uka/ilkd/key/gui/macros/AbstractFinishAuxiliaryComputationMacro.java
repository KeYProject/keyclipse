/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uka.ilkd.key.gui.macros;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.visitor.ProgVarReplaceVisitor;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermFactory;
import de.uka.ilkd.key.logic.op.ElementaryUpdate;
import de.uka.ilkd.key.logic.op.Function;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.ParsableVariable;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.op.QuantifiableVariable;
import de.uka.ilkd.key.logic.op.UpdateableOperator;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.VariableNameProposer;
import de.uka.ilkd.key.proof.init.IFProofObligationVars;
import de.uka.ilkd.key.proof.init.ProofObligationVars;
import de.uka.ilkd.key.rule.NoPosTacletApp;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author christoph
 */
abstract class AbstractFinishAuxiliaryComputationMacro implements ProofMacro {

    static final TermBuilder TB = TermBuilder.DF;


    @Override
    public String getName() {
        return "Finish auxiliary computation";
    }


    @Override
    public String getDescription() {
        return "Finish auxiliary computation.";
    }


    protected Term calculateResultingTerm(Proof proof,
                                          IFProofObligationVars ifVars,
                                          Goal initGoal) {
        final Term[] goalFormulas1 =
                buildExecution(ifVars.c1, ifVars.getMapFor(ifVars.c1),
                               proof.openGoals(), initGoal);
        final Term[] goalFormulas2 =
                buildExecution(ifVars.c2, ifVars.getMapFor(ifVars.c2),
                               proof.openGoals(), initGoal);

        Term composedStates = TB.ff();
        for (int i = 0; i < goalFormulas1.length; i++) {
            for (int j = i; j < goalFormulas2.length; j++) {
                final Term composedState = TB.and(goalFormulas1[i], goalFormulas2[j]);
                composedStates = TB.or(composedStates, composedState);
            }
        }
        initGoal.proof().addIFSymbol(composedStates);
        return composedStates;
    }


    private Term[] buildExecution(ProofObligationVars c,
                                  Map<Term, Term> vsMap,
                                  ImmutableList<de.uka.ilkd.key.proof.Goal> symbExecGoals,
                                  Goal initGoal) {
        Services services = initGoal.proof().getServices();
        final Term[] goalFormulas = buildFormulasFromGoals(symbExecGoals);
        // the build in heap symbol has to be handled with care
        final HashMap<Operator, Boolean> doNotReplace =
                new HashMap<Operator, Boolean>();
        doNotReplace.put(TB.getBaseHeap(services).op(), Boolean.TRUE);
        final Term[] renamedGoalFormulas =
                renameVariablesAndSkolemConstants(goalFormulas, vsMap, doNotReplace,
                                                  c.postfix, initGoal);
        Term[] result = new Term[renamedGoalFormulas.length];
        for (int i = 0; i < renamedGoalFormulas.length; i++) {
            result[i] =
                    TB.applyElementary(services, c.pre.heap, renamedGoalFormulas[i]);
        }
        return result;
    }


    private Term[] applyProgramRenamingsToSubs(Term term,
                                               Map<ProgramVariable, ProgramVariable> progVarReplaceMap,
                                               Map<Operator, Boolean> notReplaceMap,
                                               String postfix,
                                               Services services) {
        Term[] appliedSubs = null;
        if (term.subs() != null) {
            appliedSubs = new Term[term.subs().size()];
            for (int i = 0; i < appliedSubs.length; i++) {
                appliedSubs[i] = applyRenamingsToPrograms(term.sub(i),
                                                          progVarReplaceMap,
                                                          notReplaceMap,
                                                          postfix,
                                                          services);
            }
        }
        return appliedSubs;
    }


    private Map<ProgramVariable, ProgramVariable>
                        extractProgamVarReplaceMap(Map<Term, Term> replaceMap) {
        Map<ProgramVariable, ProgramVariable> progVarReplaceMap =
                new HashMap<ProgramVariable, ProgramVariable>();
        for (final Term t : replaceMap.keySet()) {
            if (t.op() instanceof ProgramVariable) {
                progVarReplaceMap.put((ProgramVariable) t.op(),
                                      (ProgramVariable) replaceMap.get(t).op());
            }
        }
        return progVarReplaceMap;
    }


    private void insertBoundVarsIntoNotReplaceMap(Term term,
                                                  Map<Operator, Boolean> notReplaceMap) {
        if (term.boundVars() != null) {
            for (final QuantifiableVariable bv : term.boundVars()) {
                notReplaceMap.put(bv, Boolean.TRUE);
            }
        }
    }


    private Term renameFormulasWithoutPrograms(Term term,
                                               Map<Term, Term> replaceMap,
                                               Map<Operator, Boolean> notReplaceMap,
                                               String postfix,
                                               Goal initGoal) {
        Services services = initGoal.proof().getServices();
        if (term == null) {
            return null;
        }

        if (replaceMap == null) {
            replaceMap = new HashMap<Term, Term>();
        }
        if (notReplaceMap == null) {
            notReplaceMap = new HashMap<Operator, Boolean>();
        }

        if (notReplaceMap.containsKey(term.op())) {
            return term;
        } else if (replaceMap.containsKey(term)) {
            return replaceMap.get(term);
        } else if (term.op() instanceof ParsableVariable) {
            assert term.subs().isEmpty();
            final ParsableVariable pv = (ParsableVariable) term.op();
            final Name newName =
                    VariableNameProposer.DEFAULT.getNewName(services,
                                                            new Name(pv.name() +
                                                                     postfix));
            final Operator renamedPv = pv.rename(newName);
            if (renamedPv instanceof ProgramVariable) {
                // for the taclet application dialog (which gets the declared
                // program variables in a strange way and not directly from the
                // namespace); adds it also to the namespace
                initGoal.addProgramVariable((ProgramVariable)renamedPv);
            } else {
                services.getNamespaces().programVariables().addSafely(renamedPv);
            }
            final Term pvTerm = TermFactory.DEFAULT.createTerm(renamedPv);
            replaceMap.put(term, pvTerm);
            return pvTerm;

        } else if (term.op() instanceof Function &&
                   ((Function) term.op()).isSkolemConstant()) {
            final Function f = (Function) term.op();
            final Name newName =
                    VariableNameProposer.DEFAULT.getNewName(services,
                                                            new Name(f.name() +
                                                                     postfix));
            final Function renamedF = f.rename(newName);
            services.getNamespaces().functions().addSafely(renamedF);
            final Term fTerm =
                    TermFactory.DEFAULT.createTerm(renamedF);
            replaceMap.put(term, fTerm);
            return fTerm;
        } else if (term.op() instanceof ElementaryUpdate) {
            final ElementaryUpdate u = (ElementaryUpdate) term.op();
            final Term lhsTerm = TB.var(u.lhs());
            final Term renamedLhs = renameFormulasWithoutPrograms(lhsTerm,
                                                                  replaceMap,
                                                                  notReplaceMap,
                                                                  postfix,
                                                                  initGoal);
            final Term[] renamedSubs =
                    renameSubs(term, replaceMap, notReplaceMap, postfix, initGoal);
            final ElementaryUpdate renamedU =
                    ElementaryUpdate.getInstance((UpdateableOperator) renamedLhs.op());
            final Term uTerm = TermFactory.DEFAULT.createTerm(renamedU, renamedSubs);
            replaceMap.put(term, uTerm);
            return uTerm;
        } else {
            insertBoundVarsIntoNotReplaceMap(term, notReplaceMap);
            final Term[] renamedSubs =
                    renameSubs(term, replaceMap, notReplaceMap, postfix, initGoal);
            final Term renamedTerm =
                    TermFactory.DEFAULT.createTerm(term.op(), renamedSubs,
                                                   term.boundVars(),
                                                   term.javaBlock());
            replaceMap.put(term, renamedTerm);
            return renamedTerm;
        }
    }


    private JavaBlock renameJavaBlock(Map<ProgramVariable, ProgramVariable> progVarReplaceMap,
                                      Term term, Services services) {
        final ProgVarReplaceVisitor paramRepl =
                new ProgVarReplaceVisitor(term.javaBlock().program(), progVarReplaceMap, services);
        paramRepl.start();
        final JavaBlock renamedJavaBlock =
                JavaBlock.createJavaBlock((StatementBlock) paramRepl.result());
        return renamedJavaBlock;
    }


    private Term[] buildFormulasFromGoals(ImmutableList<Goal> symbExecGoals) {
        Term[] result = new Term[symbExecGoals.size()];
        int i = 0;
        for (final Goal symbExecGoal : symbExecGoals) {
            result[i] = buildFormulaFromGoal(symbExecGoal);
            i++;
        }
        return result;
    }


    private Term buildFormulaFromGoal(Goal symbExecGoal) {
        Term result = TB.tt();
        for (final SequentFormula f : symbExecGoal.sequent().antecedent()) {
            result = TB.and(result, f.formula());
        }
        for (final SequentFormula f : symbExecGoal.sequent().succedent()) {
            result = TB.and(result, TB.not(f.formula()));
        }
        return result;
    }


    private Term[] renameVariablesAndSkolemConstants(Term[] terms,
                                                     Map<Term, Term> replaceMap,
                                                     Map<Operator, Boolean> notReplaceMap,
                                                     String postfix,
                                                     Goal initGoal) {
        Term[] result = new Term[terms.length];
        for (int i = 0; i < terms.length; i++) {
            result[i] =
                    renameVariablesAndSkolemConstants(terms[i], replaceMap,
                                                      notReplaceMap, postfix,
                                                      initGoal);
        }
        return result;
    }


    private Term renameVariablesAndSkolemConstants(Term term,
                                                   Map<Term, Term> replaceMap,
                                                   Map<Operator, Boolean> notReplaceMap,
                                                   String postfix,
                                                   Goal initGoal) {
        final Term temp = renameFormulasWithoutPrograms(term, replaceMap,
                                                        notReplaceMap,
                                                        postfix,
                                                        initGoal);
        Services services = initGoal.proof().getServices();
        final Map<ProgramVariable, ProgramVariable> progVarReplaceMap =
                extractProgamVarReplaceMap(replaceMap);
        return applyRenamingsToPrograms(temp, progVarReplaceMap, notReplaceMap,
                                        postfix, services);
    }


    protected Term applyRenamingsToPrograms(Term term,
                                            Map<ProgramVariable, ProgramVariable> progVarReplaceMap,
                                            Map<Operator, Boolean> notReplaceMap,
                                            String postfix,
                                            Services services) {
        if (term != null) {
            final JavaBlock renamedJavaBlock =
                    renameJavaBlock(progVarReplaceMap, term, services);
            final Term[] appliedSubs =
                    applyProgramRenamingsToSubs(term, progVarReplaceMap,
                                                notReplaceMap, postfix,
                                                services);

            final Term renamedTerm =
                    TermFactory.DEFAULT.createTerm(term.op(), appliedSubs,
                                                   term.boundVars(),
                                                   renamedJavaBlock);
            return renamedTerm;
        } else {
            return null;
        }
    }


    private Term[] renameSubs(Term term,
                              Map<Term, Term> replaceMap,
                              Map<Operator, Boolean> notReplaceMap,
                              String postfix,
                              Goal initGoal) {
        Term[] renamedSubs = null;
        if (term.subs() != null) {
            renamedSubs = new Term[term.subs().size()];
            for (int i = 0; i < renamedSubs.length; i++) {
                renamedSubs[i] = renameFormulasWithoutPrograms(term.sub(i),
                                                               replaceMap,
                                                               notReplaceMap,
                                                               postfix,
                                                               initGoal);
            }
        }
        return renamedSubs;
    }


    void addContractApplicationTaclets(Goal initiatingGoal,
                                       Proof symbExecProof) {
        final ImmutableList<Goal> openGoals = symbExecProof.openGoals();
        for (final Goal openGoal : openGoals) {
            final ImmutableSet<NoPosTacletApp> ruleApps =
                    openGoal.indexOfTaclets().allNoPosTacletApps();
            for (final NoPosTacletApp ruleApp : ruleApps) {
                final Taclet t = ruleApp.taclet();
                if (t.getSurviveSymbExec()) {
                    initiatingGoal.addTaclet(t, SVInstantiations.EMPTY_SVINSTANTIATIONS, true);
                }
            }
        }
    }
}