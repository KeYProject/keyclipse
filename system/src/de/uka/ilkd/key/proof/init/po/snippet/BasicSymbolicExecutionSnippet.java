package de.uka.ilkd.key.proof.init.po.snippet;

import de.uka.ilkd.key.collection.ImmutableArray;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSLList;
import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.JavaInfo;
import de.uka.ilkd.key.java.Statement;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.Modifier;
import de.uka.ilkd.key.java.declaration.ParameterDeclaration;
import de.uka.ilkd.key.java.declaration.VariableSpecification;
import de.uka.ilkd.key.java.expression.literal.NullLiteral;
import de.uka.ilkd.key.java.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.expression.operator.New;
import de.uka.ilkd.key.java.reference.TypeRef;
import de.uka.ilkd.key.java.reference.TypeReference;
import de.uka.ilkd.key.java.statement.Branch;
import de.uka.ilkd.key.java.statement.Catch;
import de.uka.ilkd.key.java.statement.MethodBodyStatement;
import de.uka.ilkd.key.java.statement.Try;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.init.ProofObligationVars;

import java.util.Iterator;

/**
 *
 * @author christoph
 */
class BasicSymbolicExecutionSnippet extends ReplaceAndRegisterMethod
        implements FactoryMethod {

    @Override
    public Term produce(BasicSnippetData d,
                        ProofObligationVars poVars)
            throws UnsupportedOperationException {
        ImmutableList<Term> posts = ImmutableSLList.<Term>nil();
        if (poVars.post.self != null) {
            posts = posts.append(d.tb.equals(poVars.post.self, poVars.pre.self));
        }
        if (poVars.post.result != null) {
            posts = posts.append(d.tb.equals(poVars.post.result,
                                             poVars.pre.result));
        }
        posts = posts.append(d.tb.equals(poVars.post.exception,
                                         poVars.pre.exception));
        posts = posts.append(d.tb.equals(poVars.post.heap, d.tb.getBaseHeap()));
        final Term prog = buildProgramTerm(d, poVars, d.tb.and(posts), d.tb);
        return prog;
    }

    private Term buildProgramTerm(BasicSnippetData d,
                                  ProofObligationVars vs,
                                  Term postTerm,
                                  TermBuilder.Serviced tb) {
        if (d.get(BasicSnippetData.Key.MODALITY) == null) {
            throw new UnsupportedOperationException("Tried to produce a "
                    + "program-term for a contract without modality.");
        }
        assert Modality.class.equals(BasicSnippetData.Key.MODALITY.getType());
        Modality modality = (Modality) d.get(
                BasicSnippetData.Key.MODALITY);


        //create formal parameters
        ImmutableList<LocationVariable> formalParamVars =
                ImmutableSLList.<LocationVariable>nil();
        for (Term param : vs.pre.localVars) {
            ProgramVariable paramVar = param.op(ProgramVariable.class);
            ProgramElementName pen = new ProgramElementName("_"
                    + paramVar.name());
            LocationVariable formalParamVar =
                    new LocationVariable(pen, paramVar.getKeYJavaType());
            formalParamVars = formalParamVars.append(formalParamVar);
            register(formalParamVar, tb.getServices());
            tb.getServices().getProof().addIFSymbol(formalParamVar);
        }

        //create java block
        final JavaBlock jb = buildJavaBlock(d, formalParamVars,
                                            vs.pre.self != null
                                            ? vs.pre.self.op(ProgramVariable.class)
                                            : null,
                                            vs.pre.result != null
                                            ? vs.pre.result.op(ProgramVariable.class)
                                            : null,
                                            vs.pre.exception != null
                                            ? vs.pre.exception.op(ProgramVariable.class)
                                            : null);

        //create program term
        final Modality symbExecMod;
        if (modality == Modality.BOX) {
            symbExecMod = Modality.DIA;
        } else {
            symbExecMod = Modality.BOX;
        }
        final Term programTerm = tb.prog(symbExecMod, jb, postTerm);
        //final Term programTerm = tb.not(tb.prog(modality, jb, tb.not(postTerm)));

        //create update
        Term update = tb.skip();
        Iterator<LocationVariable> formalParamIt = formalParamVars.iterator();
        Iterator<Term> paramIt = vs.pre.localVars.iterator();
        while (formalParamIt.hasNext()) {
            Term paramUpdate = tb.elementary(formalParamIt.next(),
                                             paramIt.next());
            update = tb.parallel(update, paramUpdate);
        }

        return tb.apply(update, programTerm);
    }

    private JavaBlock buildJavaBlock(BasicSnippetData d,
                                     ImmutableList<LocationVariable> formalParVars,
                                     ProgramVariable selfVar,
                                     ProgramVariable resultVar,
                                     ProgramVariable exceptionVar) {
        IObserverFunction targetMethod =
                (IObserverFunction) d.get(BasicSnippetData.Key.TARGET_METHOD);
        if (!(targetMethod instanceof IProgramMethod)) {
            throw new UnsupportedOperationException("Tried to produce a "
                    + "java-block for an observer which is no program method.");
        }
        JavaInfo javaInfo = d.tb.getServices().getJavaInfo();
        IProgramMethod pm = (IProgramMethod) targetMethod;

        //create method call
        final ImmutableArray<Expression> formalArray =
                new ImmutableArray<Expression>(formalParVars.toArray(
                new ProgramVariable[formalParVars.size()]));
        final StatementBlock sb;
        if (pm.isConstructor()) {
            assert selfVar != null;
            assert resultVar == null;
            final Expression[] formalArray2 =
                formalArray.toArray(new Expression[formalArray.size()]);
            KeYJavaType forClass =
                    (KeYJavaType) d.get(BasicSnippetData.Key.FOR_CLASS);
            final New n =
                    new New(formalArray2, new TypeRef(forClass),
                            null);
            final CopyAssignment ca = new CopyAssignment(selfVar, n);
            sb = new StatementBlock(ca);
        } else {
            final MethodBodyStatement call =
                new MethodBodyStatement(pm, selfVar, resultVar, formalArray);
            sb = new StatementBlock(call);
        }

        //create variables for try statement
        final KeYJavaType eType =
            javaInfo.getTypeByClassName("java.lang.Exception");
        final TypeReference excTypeRef = javaInfo.createTypeReference(eType);
        final ProgramElementName ePEN = new ProgramElementName("e");
        final ProgramVariable eVar = new LocationVariable(ePEN, eType);

        //create try statement
        final CopyAssignment nullStat =
            new CopyAssignment(exceptionVar, NullLiteral.NULL);
        final VariableSpecification eSpec = new VariableSpecification(eVar);
        final ParameterDeclaration excDecl =
            new ParameterDeclaration(new Modifier[0], excTypeRef, eSpec,
                    false);
        final CopyAssignment assignStat =
            new CopyAssignment(exceptionVar, eVar);
        final Catch catchStat =
            new Catch(excDecl, new StatementBlock(assignStat));
        final Try tryStat = new Try(sb, new Branch[]{catchStat});
        final StatementBlock sb2 = new StatementBlock(
                new Statement[]{nullStat, tryStat});

        //create java block
        JavaBlock result = JavaBlock.createJavaBlock(sb2);

        return result;
    }
}