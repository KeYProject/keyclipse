package de.uka.ilkd.key.nparser.builder;

import de.uka.ilkd.key.nparser.BuildingException;
import de.uka.ilkd.key.nparser.KeYParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class brings some nice features to the visitors of key's ast.
 *
 * <ul>
 * <li>It makes casting implicit by using {{@link #accept(RuleContext)}}
 * <li>It allows to pass arguments by an explicit stack.
 * <li>It brings handling of errors and warnings.
 * </ul>
 *
 * @param <T> return type
 * @author Alexander Weigl
 */
@SuppressWarnings("unchecked")
abstract class AbstractBuilder<T> extends KeYParserBaseVisitor<T> {
    //region handling of warnings
    @Nullable
    protected List<BuildingException> warnings = null;
    //region stack handling
    private Stack<Object> parameters = new Stack<>();

    /**
     * Helper function for avoiding cast.
     *
     * @param ctx
     * @param <T>
     * @return
     */
    public <T> @Nullable T accept(@Nullable RuleContext ctx) {
        if (ctx == null) {
            return null;
        }
        return (T) ctx.accept(this);
    }

    @Override
    protected T aggregateResult(T aggregate, T nextResult) {
        if(nextResult!=null) return nextResult;
        return aggregate;
    }

    /**
     * @param <T>
     * @return
     */
    protected <T> T peek() {
        return (T) (parameters.size() == 0 ? null : parameters.peek());
    }

    protected <T> T acceptFirst(Collection<? extends RuleContext> seq) {
        if (seq.isEmpty()) return null;
        return accept(seq.iterator().next());
    }

    protected <T> T pop() {
        return (T) parameters.pop();
    }

    protected void push(Object... obj) {
        for (Object a : obj) parameters.push(a);
    }

    protected <T> @Nullable T accept(@Nullable RuleContext ctx, Object... args) {
        int stackSize = parameters.size();
        push(args);
        T t = accept(ctx);
        //Stack hygiene
        while (parameters.size() > stackSize) {
            parameters.pop();
        }
        return t;
    }

    protected <T> T oneOf(ParserRuleContext... ctxs) {
        for (ParserRuleContext ctx : ctxs) {
            if (ctx != null) {
                return (T) ctx.accept(this);
            }
        }
        return null;
    }

    protected <T> List<T> mapOf(Collection<? extends ParserRuleContext> argument) {
        return argument.stream().map(it -> (T) it.accept(this)).collect(Collectors.toList());
    }

    protected void each(RuleContext... ctx) {
        for (RuleContext c : ctx) accept(c);
    }

    protected void each(Collection<? extends ParserRuleContext> argument) {
        for (RuleContext c : argument) accept(c);
    }
    //endregion

    protected <T2> List<T2> mapMapOf(List<? extends RuleContext>... ctxss) {
        return Arrays.stream(ctxss)
                .flatMap(it -> it.stream().map(a -> (T2) accept(a)))
                .collect(Collectors.toList());
    }

    public @NotNull List<BuildingException> getWarnings() {
        if (warnings == null) warnings = new LinkedList<>();
        return warnings;
    }

    protected BuildingException addWarning(ParserRuleContext node, String description) {
        var be = new BuildingException(node, description);
        getWarnings().add(be);
        return be;
    }

    protected BuildingException addWarning(String description) {
        var be = new BuildingException(description);
        getWarnings().add(be);
        return be;
    }
    //endregion

    //region error handling

    /**
     * Throws a semanticError for the given ast node and message.
     *
     * @param ctx
     * @param format
     * @param args
     */
    protected void semanticError(ParserRuleContext ctx, String format, Object... args) {
        throw new BuildingException(ctx, String.format(format, args));
    }

    /**
     * Wraps an exception into a {@link BuildingException}
     *
     * @param e
     */
    protected void throwEx(Throwable e) {
        throw new BuildingException(e);
    }
    //endregion
}
