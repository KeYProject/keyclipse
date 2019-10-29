package de.uka.ilkd.key.nparser;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.NamespaceSet;
import de.uka.ilkd.key.logic.op.Function;
import de.uka.ilkd.key.logic.op.SortDependingFunction;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.sort.GenericSort;
import de.uka.ilkd.key.logic.sort.Sort;
import org.key_project.util.collection.ImmutableArray;

import java.util.List;

public class FunctionPredicateBuilder extends DefaultBuilder {
    public FunctionPredicateBuilder(Services services, NamespaceSet nss) {
        super(services, nss);
    }

    @Override
    public Object visitFile(KeYParser.FileContext ctx) {
        return accept(ctx.decls());
    }

    @Override
    public Object visitDecls(KeYParser.DeclsContext ctx) {
        allOf(ctx.pred_decls(), ctx.func_decls(), ctx.transform_decls());
        return null;
    }

    @Override
    public Object visitPred_decl(KeYParser.Pred_declContext ctx) {
        String pred_name = accept(ctx.funcpred_name());
        List<Boolean> whereToBind = accept(ctx.where_to_bind());
        List<Sort> argSorts = accept(ctx.arg_sorts());
        if (whereToBind != null && whereToBind.size() != argSorts.size()) {
            semanticError(ctx, "Where-to-bind list must have same length as argument list");
        }

        Function p = null;

        int separatorIndex = pred_name.indexOf("::");
        if (separatorIndex > 0) {
            String sortName = pred_name.substring(0, separatorIndex);
            String baseName = pred_name.substring(separatorIndex + 2);
            Sort genSort = lookupSort(sortName);
            if (genSort instanceof GenericSort) {
                p = SortDependingFunction.createFirstInstance(
                        (GenericSort) genSort,
                        new Name(baseName),
                        Sort.FORMULA,
                        (Sort[]) argSorts.toArray(),
                        false);
            }
        }

        if (p == null) {
            p = new Function(new Name(pred_name),
                    Sort.FORMULA,
                    argSorts.toArray(new Sort[0]),
                    whereToBind == null ? null : whereToBind.toArray(new Boolean[0]),
                    false);
        }
        if (lookup(p.name()) != null) {
        } else {
            functions().add(p);
        }
        return null;
    }

    @Override
    public Object visitFunc_decl(KeYParser.Func_declContext ctx) {
        boolean unique = ctx.UNIQUE() != null;
        Sort retSort = accept(ctx.any_sortId_check());
        String func_name = accept(ctx.funcpred_name());
        List<Boolean[]> whereToBind = accept(ctx.where_to_bind());
        List<Sort> argSorts = accept(ctx.arg_sorts());
        assert argSorts != null;

        if (whereToBind != null && whereToBind.size() != argSorts.size()) {
            semanticError(ctx, "Where-to-bind list must have same length as argument list");
        }

        Function f = null;
        int separatorIndex = func_name.indexOf("::");
        if (separatorIndex > 0) {
            String sortName = func_name.substring(0, separatorIndex);
            String baseName = func_name.substring(separatorIndex + 2);
            Sort genSort = lookupSort(sortName);
            if (genSort instanceof GenericSort) {
                f = SortDependingFunction.createFirstInstance(
                        (GenericSort) genSort,
                        new Name(baseName),
                        retSort,
                        argSorts.toArray(new Sort[0]),
                        unique);
            }
        }

        if (f == null) {
            f = new Function(new Name(func_name),
                    retSort,
                    argSorts.toArray(new Sort[0]),
                    whereToBind == null ? null : whereToBind.toArray(new Boolean[0]),
                    unique);
        }


        if (lookup(f.name()) != null) {
        } else {
            functions().add(f);
        }
        return f;
    }

    @Override
    public Object visitFunc_decls(KeYParser.Func_declsContext ctx) {
        return allOf(ctx.func_decl());
    }

    @Override
    public Object visitTransform_decl(KeYParser.Transform_declContext ctx) {
        var retSort = (Sort) (ctx.FORMULA() != null ? Sort.FORMULA : accept(ctx.any_sortId_check()));
        var trans_name = (String) accept(ctx.funcpred_name());
        var argSorts = (List<Sort>) accept(ctx.arg_sorts_or_formula());
        Transformer t = new Transformer(new Name(trans_name),
                retSort,
                new ImmutableArray<>(argSorts));
        if (lookup(t.name()) != null) {
            //weigl: do something?
        }
        return null;
    }


    @Override
    public Object visitTransform_decls(KeYParser.Transform_declsContext ctx) {
        return allOf(ctx.transform_decl());
    }


    @Override
    public Object visitPred_decls(KeYParser.Pred_declsContext ctx) {
        return allOf(ctx.pred_decl());
    }
}
