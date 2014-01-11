package de.uka.ilkd.key.proof.init.po.snippet;

import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.proof.init.ProofObligationVars;


public class SelfcomposedLoopSnippet extends ReplaceAndRegisterMethod implements
        InfFlowFactoryMethod {

    @Override
    public Term produce(BasicSnippetData d,
                        ProofObligationVars poVars1,
                        ProofObligationVars poVars2) throws UnsupportedOperationException {
        BasicPOSnippetFactory f1 =
                POSnippetFactory.getBasicFactory(d, poVars1);
        BasicPOSnippetFactory f2 =
                POSnippetFactory.getBasicFactory(d, poVars2);
        final Term exec1 =
                f1.create(BasicPOSnippetFactory.Snippet.LOOP_EXEC_WITH_INV);
        final Term exec2 =
                f2.create(BasicPOSnippetFactory.Snippet.LOOP_EXEC_WITH_INV);

        return d.tb.and(exec1, exec2);
    }
}