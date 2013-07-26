package de.uka.ilkd.key.proof.init.po.snippet;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSLList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.java.Label;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.init.StateVars;
import de.uka.ilkd.key.speclang.BlockContract;
import de.uka.ilkd.key.speclang.BlockContract.Variables;
import de.uka.ilkd.key.speclang.FunctionalOperationContract;
import de.uka.ilkd.key.speclang.InformationFlowContract;
import de.uka.ilkd.key.speclang.LoopInvariant;
import de.uka.ilkd.key.util.MiscTools;
import java.util.EnumMap;
import java.util.List;


/**
 * Immutable class.
 * <p/>
 * @author christoph
 */
class BasicSnippetData {

    /**
     * Tells whether the contract contains a measured_by clause.
     */
    final boolean hasMby;

    /**
     * Variables originally used during parsing.
     */
    final StateVars origVars;

    /**
     * TermBuilder used by the FactoryMethods.
     */
    final TermBuilder.Serviced tb;

    /**
     * Unified contract content.
     */
    private final EnumMap<Key, Object> contractContents =
            new EnumMap<Key, Object>(Key.class) {

                private static final long serialVersionUID = -8548805965130100236L;

                @Override
                public Object put(Key key,
                                  Object value) {
                    assert value == null || key.getType().isInstance(value);
                    return super.put(key, value);
                }
            };


    /**
     * Keys to access the unified contract content.
     */
    static enum Key {

        /**
         * Returns the KeYJavaType representing the class/interface to which the
         * specification element belongs.
         */
        FOR_CLASS(KeYJavaType.class),
        /**
         * Returns the contracted function symbol.
         */
        TARGET_METHOD(IObserverFunction.class),
        /**
         * Returns the contracted block.
         */
        TARGET_BLOCK(StatementBlock.class),
        PRECONDITION(Term.class),
        POSTCONDITION(Term.class),
        LOOP_INVARIANT(LoopInvariant.class),
        LOOP_INVARIANT_TERM(Term.class),
        MODIFIES(Term.class),
        DEPENDENS(Term.class),
        MEASURED_BY(Term.class),
        MODALITY(Modality.class),
        INF_FLOW_SPECS(ImmutableList.class),
        /**
         * Variables originally used during parsing.
         */
        BLOCK_VARS(Variables.class),
        LABELS(Label[].class),
        CONTEXT(ExecutionContext.class); // this does not fit well here

        private final Class<?> type;


        Key(Class<?> type) {
            this.type = type;
        }


        public Class<?> getType() {
            return type;
        }
    };


    BasicSnippetData(FunctionalOperationContract contract,
                     Services services) {
        this.hasMby = contract.hasMby();
        this.tb = new TermBuilder.Serviced(services);

        contractContents.put(Key.TARGET_METHOD, contract.getTarget());
        contractContents.put(Key.FOR_CLASS, contract.getKJT());
        contractContents.put(Key.PRECONDITION, contract.getPre());
        contractContents.put(Key.POSTCONDITION, contract.getPost());
        contractContents.put(Key.MODIFIES, contract.getMod());
        contractContents.put(Key.MEASURED_BY, contract.getMby());
        contractContents.put(Key.MODALITY, contract.getModality());

        final Term heap = TermBuilder.DF.getBaseHeap(services);
        origVars =
                new StateVars(contract.getSelf(), contract.getParams(),
                              contract.getResult(), contract.getExc(), heap);
    }
    
    BasicSnippetData(LoopInvariant invariant,
                     Services services) {
        this.hasMby = false;
        this.tb = new TermBuilder.Serviced(services);

        contractContents.put(Key.TARGET_METHOD, invariant.getTarget());
        contractContents.put(Key.CONTEXT, invariant.getExecutionContext());
        contractContents.put(Key.LOOP_INVARIANT, invariant);
        contractContents.put(Key.LOOP_INVARIANT_TERM, invariant.getInvariant(services));
        contractContents.put(Key.MODIFIES, invariant.getModifies());
        contractContents.put(Key.MODALITY, Modality.BOX);
        contractContents.put(Key.INF_FLOW_SPECS, invariant.getInfFlowSpecs(services));

        final Term heap = TermBuilder.DF.getBaseHeap(services);
        final ImmutableSet<ProgramVariable> localInVariables =
                MiscTools.getLocalIns(invariant.getLoop(), services);
        final ImmutableSet<ProgramVariable> localOutVariables =
                MiscTools.getLocalOuts(invariant.getLoop(), services);
        final ImmutableList<Term> localInTerms = toTermList(localInVariables);
        final ImmutableList<Term> localOutTerms = toTermList(localOutVariables);
        final ImmutableList<Term> localInsWithoutOutDuplicates =
                    MiscTools.filterOutDuplicates(localInTerms, localOutTerms);
        final ImmutableList<Term> localVarsTerms = localInsWithoutOutDuplicates.append(localOutTerms);

        origVars = new StateVars(invariant.getInternalSelfTerm(),
                                 invariant.getGuard(),
                                 localVarsTerms, heap);
    }
    
    
    BasicSnippetData(LoopInvariant invariant,
                     ExecutionContext context,
                     Services services) {
        this(invariant, services);
        contractContents.put(Key.CONTEXT, context);
    }


    BasicSnippetData(InformationFlowContract contract,
                     Services services) {
        this.hasMby = contract.hasMby();
        this.tb = new TermBuilder.Serviced(services);

        contractContents.put(Key.TARGET_METHOD, contract.getTarget());
        contractContents.put(Key.FOR_CLASS, contract.getKJT());
        contractContents.put(Key.PRECONDITION, contract.getPre());        
        contractContents.put(Key.MODIFIES, contract.getMod());
        contractContents.put(Key.DEPENDENS, contract.getDep());
        contractContents.put(Key.MEASURED_BY, contract.getMby());
        contractContents.put(Key.MODALITY, contract.getModality());
        contractContents.put(Key.INF_FLOW_SPECS, contract.getInfFlowSpecs());

        final Term heap = TermBuilder.DF.getBaseHeap(services);
        origVars =
                new StateVars(contract.getSelf(), contract.getParams(),
                              contract.getResult(), contract.getExc(), heap);
    }


    BasicSnippetData(BlockContract contract,
                     Services services) {
        this.hasMby = contract.hasMby();
        this.tb = new TermBuilder.Serviced(services);

        contractContents.put(Key.TARGET_METHOD, contract.getTarget());
        contractContents.put(Key.FOR_CLASS, contract.getKJT());
        contractContents.put(Key.TARGET_BLOCK, contract.getBlock());
        contractContents.put(Key.BLOCK_VARS, contract.getVariables());
        contractContents.put(Key.PRECONDITION, contract.getPre(services));
        contractContents.put(Key.POSTCONDITION, contract.getPost(services));
        contractContents.put(Key.MODIFIES, contract.getMod(services));
        contractContents.put(Key.MODALITY, contract.getModality());
        contractContents.put(Key.INF_FLOW_SPECS, contract.getInfFlowSpecs());
        List<Label> labels = contract.getLabels();
        contractContents.put(Key.LABELS,
                             labels.toArray(new Label[labels.size()]));

        final Term heap = TermBuilder.DF.getBaseHeap(services);
        BlockContract.Terms vars = contract.getVariablesAsTerms();
        final ImmutableSet<ProgramVariable> localInVariables =
                MiscTools.getLocalIns(contract.getBlock(), services);
        final ImmutableSet<ProgramVariable> localOutVariables =
                MiscTools.getLocalOuts(contract.getBlock(), services);
        final ImmutableList<Term> localInTerms = toTermList(localInVariables);
        final ImmutableList<Term> localOutTerms = toTermList(localOutVariables);
        final ImmutableList<Term> localInsWithoutOutDuplicates =
                    MiscTools.filterOutDuplicates(localInTerms, localOutTerms);
        final ImmutableList<Term> localVarsTerms = localInsWithoutOutDuplicates.append(localOutTerms);

        origVars = new StateVars(vars.self, localVarsTerms,
                                 vars.result, vars.exception, heap);
    }


    BasicSnippetData(BlockContract contract,
                     ExecutionContext context,
                     Services services) {
        this(contract, services);
        contractContents.put(Key.CONTEXT, context);
    }

    private ImmutableList<Term> toTermList(ImmutableSet<ProgramVariable> vars) {
        ImmutableList<Term> result = ImmutableSLList.<Term>nil();
        for (ProgramVariable v : vars) {
            result = result.append(tb.var(v));
        }
        return result;
    }


    Object get(Key contentKey) {
        return contractContents.get(contentKey);
    }
}
