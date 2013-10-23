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

package de.uka.ilkd.key.proof.init;

import java.io.IOException;
import java.util.Properties;

import de.uka.ilkd.key.collection.DefaultImmutableSet;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.configuration.ProofSettings;
import de.uka.ilkd.key.java.JavaInfo;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.Choice;
import de.uka.ilkd.key.logic.Namespace;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermFactory;
import de.uka.ilkd.key.logic.op.Function;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.proof.InfFlowCheckInfo;
import de.uka.ilkd.key.proof.JavaModel;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.ProofAggregate;
import de.uka.ilkd.key.proof.StrategyInfoUndoMethod;
import de.uka.ilkd.key.proof.mgt.AxiomJustification;
import de.uka.ilkd.key.proof.mgt.SpecificationRepository;
import de.uka.ilkd.key.rule.label.ITermLabelWorker;
import de.uka.ilkd.key.rule.AnonHeapTermLabelInstantiator;
import de.uka.ilkd.key.rule.NoPosTacletApp;
import de.uka.ilkd.key.rule.label.SelectSkolemConstantTermLabelInstantiator;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.rule.label.PostConditionTermLabelInstantiator;
import de.uka.ilkd.key.speclang.ClassAxiom;
import de.uka.ilkd.key.speclang.Contract;
import de.uka.ilkd.key.strategy.StrategyProperties;
import de.uka.ilkd.key.util.Pair;



/**
 * An abstract proof obligation implementing common functionality.
 */
public abstract class AbstractPO implements IPersistablePO {

    protected static final TermFactory TF = TermFactory.DEFAULT;
    protected static final TermBuilder TB = TermBuilder.DF;
    protected final InitConfig initConfig;
    protected final Services services;
    protected final JavaInfo javaInfo;
    protected final HeapLDT heapLDT;
    protected final SpecificationRepository specRepos;
    protected final String name;
    protected ImmutableSet<NoPosTacletApp> taclets;
    private String header;
    private ProofAggregate proofAggregate;
    protected Term[] poTerms;
    protected String[] poNames;


    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------
    public AbstractPO(InitConfig initConfig,
                      String name) {
        this.initConfig = initConfig;
        this.services = initConfig.getServices();
        this.javaInfo = initConfig.getServices().getJavaInfo();
        this.heapLDT = initConfig.getServices().getTypeConverter().getHeapLDT();
        this.specRepos = initConfig.getServices().getSpecificationRepository();
        this.name = name;
        taclets = DefaultImmutableSet.nil();
    }


    //-------------------------------------------------------------------------
    //methods for use in subclasses
    //-------------------------------------------------------------------------
    private ImmutableSet<ClassAxiom> getAxiomsForObserver(
            Pair<Sort, IObserverFunction> usedObs,
            ImmutableSet<ClassAxiom> axioms) {
        for (ClassAxiom axiom : axioms) {
            if (axiom.getTarget()==null || !(axiom.getTarget().equals(usedObs.second)
                  && usedObs.first.extendsTrans(axiom.getKJT().getSort()))) {
                axioms = axioms.remove(axiom);
            }
        }
        return axioms;
    }


    private boolean reach(Pair<Sort, IObserverFunction> from,
                          Pair<Sort, IObserverFunction> to,
                          ImmutableSet<ClassAxiom> axioms) {
        ImmutableSet<Pair<Sort, IObserverFunction>> reached =
                DefaultImmutableSet.nil();
        ImmutableSet<Pair<Sort, IObserverFunction>> newlyReached = DefaultImmutableSet.<Pair<Sort, IObserverFunction>>nil().add(
                from);

        while (!newlyReached.isEmpty()) {
            for (Pair<Sort, IObserverFunction> node : newlyReached) {
                newlyReached = newlyReached.remove(node);
                reached = reached.add(node);
                final ImmutableSet<ClassAxiom> nodeAxioms = getAxiomsForObserver(
                        node, axioms);
                for (ClassAxiom nodeAxiom : nodeAxioms) {
                    final ImmutableSet<Pair<Sort, IObserverFunction>> nextNodes = nodeAxiom.getUsedObservers(
                            services);
                    for (Pair<Sort, IObserverFunction> nextNode : nextNodes) {
                        if (nextNode.equals(to)) {
                            return true;
                        } else if (!reached.contains(nextNode)) {
                            newlyReached = newlyReached.add(nextNode);
                        }
                    }
                }
            }
        }

        return false;
    }


    private ImmutableSet<Pair<Sort, IObserverFunction>> getSCC(ClassAxiom startAxiom,
                                                              ImmutableSet<ClassAxiom> axioms) {
        //TODO: make more efficient
        final Pair<Sort, IObserverFunction> start =
                new Pair<Sort, IObserverFunction>(startAxiom.getKJT().getSort(),
                                                  startAxiom.getTarget());
        ImmutableSet<Pair<Sort, IObserverFunction>> result =
                DefaultImmutableSet.nil();
        for (ClassAxiom nodeAxiom : axioms) {
            final Pair<Sort, IObserverFunction> node =
                    new Pair<Sort, IObserverFunction>(
                    nodeAxiom.getKJT().getSort(),
                                                     nodeAxiom.getTarget());
            if (reach(start, node, axioms) && reach(node, start, axioms)) {
                result = result.add(node);
            }
        }
        return result;
    }


    protected void collectClassAxioms(KeYJavaType selfKJT) {
        final ImmutableSet<ClassAxiom> axioms =
                specRepos.getClassAxioms(selfKJT);

        for (ClassAxiom axiom : axioms) {
            final ImmutableSet<Pair<Sort, IObserverFunction>> scc =
                    getSCC(axiom, axioms);
            for (Taclet axiomTaclet : axiom.getTaclets(scc, services)) {
                assert axiomTaclet != null : "class axiom returned null taclet: "
                        + axiom.getName();

                // only include if choices are appropriate
                if (choicesApply(axiomTaclet, initConfig.getActivatedChoices())) {
                    taclets = taclets.add(NoPosTacletApp.createNoPosTacletApp(
                            axiomTaclet));
                    initConfig.getProofEnv().registerRule(axiomTaclet,
                            AxiomJustification.INSTANCE);
                }
            }
        }
    }
    
    /** Check whether a taclet conforms with the currently active choices.
     * I.e., whether the taclet's given choices is a subset of <code>choices</code>.
     */
    private boolean choicesApply (Taclet taclet, ImmutableSet<Choice> choices) {
        for (Choice tacletChoices: taclet.getChoices())
            if (!choices.contains(tacletChoices)) return false;
        return true;
    }


    protected final void register(ProgramVariable pv) {
         Namespace progVarNames = services.getNamespaces().programVariables();
         if (pv != null && progVarNames.lookup(pv.name()) == null) {
             progVarNames.addSafely(pv);
         }
    }


    protected final void register(ImmutableList<ProgramVariable> pvs) {
        for (ProgramVariable pv : pvs) {
            register(pv);
        }
    }


    protected final void register(Function f) {
         Namespace functionNames = services.getNamespaces().functions();
         if (f != null && functionNames.lookup(f.name()) == null) {
             assert f.sort() != Sort.UPDATE;
             if (f.sort() == Sort.FORMULA) {
                 functionNames.addSafely(f);
             } else {
                 functionNames.addSafely(f);
             }
         }
    }


    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------
    @Override
    public final String name() {
        return name;
    }


    /**
     * Creates declarations necessary to save/load proof in textual form
     * (helper for createProof()).
     */
    private void createProofHeader(String javaPath,
                                   String classPath,
                                   String bootClassPath) {
        if (header != null) {
            return;
        }
        final StringBuffer sb = new StringBuffer();

        //bootclasspath
        if (bootClassPath != null && !bootClassPath.equals("")) {
            sb.append("\\bootclasspath \"").append(bootClassPath).append(
                    "\";\n\n");
        }

        //classpath
        if (classPath != null && !classPath.equals("")) {
            sb.append("\\classpath \"").append(classPath).append("\";\n\n");
        }

        //javaSource
        sb.append("\\javaSource \"").append(javaPath).append("\";\n\n");

        //contracts
        ImmutableSet<Contract> contractsToSave = specRepos.getAllContracts();
        for (Contract c : contractsToSave) {
            if (!c.toBeSaved()) {
                contractsToSave = contractsToSave.remove(c);
            }
        }
        if (!contractsToSave.isEmpty()) {
            sb.append("\\contracts {\n");
            for (Contract c : contractsToSave) {
                sb.append(c.proofToString(services));
            }
            sb.append("}\n\n");
        }

        header = sb.toString();
    }


    /**
     * Creates a Proof (helper for getPO()).
     */
    private Proof createProof(String proofName,
                              Term poTerm) {
        final JavaModel javaModel = initConfig.getProofEnv().getJavaModel();
        createProofHeader(javaModel.getModelDir(),
                          javaModel.getClassPath(),
                          javaModel.getBootClassPath());
        Proof proof = new Proof(proofName,
                                poTerm,
                                header,
                                initConfig.createTacletIndex(),
                                initConfig.createBuiltInRuleIndex(),
                                initConfig.getServices(),
                                initConfig.getSettings() != null
                                ? initConfig.getSettings()
                                : new ProofSettings(ProofSettings.DEFAULT_SETTINGS));
        if (!labelInstantiators.contains(AnonHeapTermLabelInstantiator.INSTANCE)) {
           labelInstantiators = labelInstantiators.append(AnonHeapTermLabelInstantiator.INSTANCE);
        }
        if (!labelInstantiators.contains(PostConditionTermLabelInstantiator.INSTANCE)) {
           labelInstantiators = labelInstantiators.append(PostConditionTermLabelInstantiator.INSTANCE);
        }
        assert proof.openGoals().size() == 1 : "expected one first open goal";
        final boolean isInfFlowProof =
                (this instanceof InfFlowRelatedPO) ||
                // this is a hack and has to be changed by time
                proof.getSettings().getStrategySettings().getActiveStrategyProperties()
                                   .getProperty(StrategyProperties.INF_FLOW_CHECK_PROPERTY)
                                   .equals(StrategyProperties.INF_FLOW_CHECK_TRUE);
        if (isInfFlowProof) {
            StrategyInfoUndoMethod undo =
                    new StrategyInfoUndoMethod() {
                @Override
                public void undo(
                        de.uka.ilkd.key.util.properties.Properties strategyInfos) {
                    strategyInfos.put(InfFlowCheckInfo.INF_FLOW_CHECK_PROPERTY, true);
                }
            };
            proof.openGoals().head().addStrategyInfo(InfFlowCheckInfo.INF_FLOW_CHECK_PROPERTY, true, undo);
        }
        return proof;
    }


    @Override
    public final ProofAggregate getPO() {
        if (proofAggregate != null) {
            return proofAggregate;
        }

        if (poTerms == null) {
            throw new IllegalStateException("No proof obligation terms.");
        }

        Proof[] proofs = new Proof[poTerms.length];
        for (int i = 0; i < proofs.length; i++) {
            proofs[i] = createProof(poNames != null ? poNames[i] : name,
                                    poTerms[i]);
            if (taclets != null) {
                proofs[i].getGoal(proofs[i].root()).indexOfTaclets().addTaclets(
                        taclets);
            }
        }

        proofAggregate = ProofAggregate.createProofAggregate(proofs, name);
        return proofAggregate;
    }


    @Override
    public boolean implies(ProofOblInput po) {
        return equals(po);
    }


    protected void assignPOTerms(Term... poTerms) {
        this.poTerms = poTerms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillSaveProperties(Properties properties) throws IOException {
        properties.setProperty(IPersistablePO.PROPERTY_CLASS, getClass().getCanonicalName());
        properties.setProperty(IPersistablePO.PROPERTY_NAME, name);
    }
    
    /**
     * Returns the name value from the given properties.
     * @param properties The properties to read from.
     * @return The name value.
     */
    public static String getName(Properties properties) {
       return properties.getProperty(IPersistablePO.PROPERTY_NAME);
    }
}
