// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//
package de.uka.ilkd.key.informationflow.rule.tacletbuilder;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.Visitor;
import de.uka.ilkd.key.logic.op.QuantifiableVariable;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.logic.op.SchemaVariableFactory;
import de.uka.ilkd.key.logic.op.TermSV;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.rule.RewriteTaclet;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletBuilderSchemaVarCollector;
import de.uka.ilkd.key.util.MiscTools;


/**
 * Builds the rule which inserts information flow contract applications.
 * <p/>
 * @author christoph
 */
abstract class AbstractInfFlowTacletBuilder extends TermBuilder {

    public AbstractInfFlowTacletBuilder(final Services services) {
        super(services.getTermFactory(), services);
    }


    ImmutableList<Term> createTermSV(ImmutableList<Term> ts,
                                     String schemaPrefix,
                                     Services services) {
        ImmutableList<Term> result = ImmutableSLList.<Term>nil();
        for (Term t : ts) {
            result = result.append(createTermSV(t, schemaPrefix, services));
        }
        return result;
    }


    Term createTermSV(Term t,
                      String schemaPrefix,
                      Services services) {
        if (t == null) {
            return null;
        }
        t = unlabel(t);
        String svName = MiscTools.toValidVariableName(schemaPrefix +
                                                      t.toString()).toString();
        Sort sort = t.sort();
        Name name =
                services.getVariableNamer().getTemporaryNameProposal(svName);
        return var(SchemaVariableFactory.createTermSV(name, sort));
    }


    SchemaVariable createVariableSV(QuantifiableVariable v,
                                    String schemaPrefix,
                                    Services services) {
        if (v == null) {
            return null;
        }
        String svName =
                MiscTools.toValidVariableName(schemaPrefix + v.name()).toString();
        Sort sort = v.sort();
        Name name =
                services.getVariableNamer().getTemporaryNameProposal(svName);
        return SchemaVariableFactory.createVariableSV(name, sort);

    }


    void addVarconds(RewriteTacletBuilder<? extends RewriteTaclet> tacletBuilder,
                     Iterable<SchemaVariable> quantifiableSVs)
            throws IllegalArgumentException {
        RewriteTacletBuilderSchemaVarCollector svCollector =
                new RewriteTacletBuilderSchemaVarCollector(tacletBuilder);
        Set<SchemaVariable> schemaVars = svCollector.collectSchemaVariables();
        for (SchemaVariable sv : schemaVars) {
            if (sv instanceof TermSV) {
                for (SchemaVariable qv : quantifiableSVs) {
                    tacletBuilder.addVarsNotFreeIn(qv, sv);
                }
            }
        }
    }


    Map<QuantifiableVariable, SchemaVariable> collectQuantifiableVariables(Term replaceWithTerm,
                                                                           Services services) {
        QuantifiableVariableVisitor qvVisitor =
                new QuantifiableVariableVisitor();
        replaceWithTerm.execPreOrder(qvVisitor);
        LinkedList<QuantifiableVariable> quantifiableVariables =
                qvVisitor.getResult();
        final Map<QuantifiableVariable, SchemaVariable> quantifiableVarsToSchemaVars =
                new LinkedHashMap<QuantifiableVariable, SchemaVariable>();
        for (QuantifiableVariable qv : quantifiableVariables) {
            quantifiableVarsToSchemaVars.put(qv, createVariableSV(qv, "", services));
        }
        return quantifiableVarsToSchemaVars;
    }


    class QuantifiableVariableVisitor implements Visitor {

        private LinkedList<QuantifiableVariable> vars = new LinkedList<QuantifiableVariable>();

        @Override
        public boolean visitSubtree(Term visited) {
            return true;
        }

        @Override
        public void visit(Term visited) {
            final ImmutableArray<QuantifiableVariable> boundVars =
                    visited.boundVars();
            for (QuantifiableVariable var : boundVars) {
                vars.add(var);
            }
        }


        @Override
        public void subtreeEntered(Term subtreeRoot) {
            // nothing to do
        }


        @Override
        public void subtreeLeft(Term subtreeRoot) {
            // nothing to do
        }


        public LinkedList<QuantifiableVariable> getResult() {
            return vars;
        }
    }
}