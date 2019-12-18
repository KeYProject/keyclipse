package de.uka.ilkd.key.nparser.builder;

import com.google.common.base.CharMatcher;
import de.uka.ilkd.key.java.*;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.expression.literal.StringLiteral;
import de.uka.ilkd.key.ldt.IntegerLDT;
import de.uka.ilkd.key.ldt.SeqLDT;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.nparser.BuildingException;
import de.uka.ilkd.key.nparser.KeYParser;
import de.uka.ilkd.key.parser.NotDeclException;
import de.uka.ilkd.key.pp.AbbrevMap;
import de.uka.ilkd.key.util.Debug;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This visitor creates expression from {@link de.uka.ilkd.key.nparser.KeyAst.Term}.
 * You should use the facade {@link de.uka.ilkd.key.nparser.KeyIO#parseExpression(String)}
 * for term parsing.
 *
 * @author weigl
 */
public class ExpressionBuilder extends DefaultBuilder {
    public static final String NO_HEAP_EXPRESSION_BEFORE_AT_EXCEPTION_MESSAGE = "Expecting select term before '@', not: ";

    /**
     * TODO Support for abbreviation maps.
     */
    private AbbrevMap scm;

    /**
     * Altlast.
     */
    private Term quantifiedArrayGuard;

    /**
     * A list of terms, that are marked for having an already set heap.
     */
    private List<Term> explicitHeap = new LinkedList<>();

    public ExpressionBuilder(Services services, NamespaceSet nss) {
        this(services, nss, new Namespace<>());
    }


    public ExpressionBuilder(Services services, NamespaceSet nss, Namespace<SchemaVariable> schemaNamespace) {
        super(services, nss);
        setSchemaVariables(schemaNamespace);
    }

    /**
     * Given a raw modality string, this function trims the modality information.
     *
     * @param raw non-null string
     * @return non-null string
     */
    public static String trimJavaBlock(String raw) {
        if (raw.startsWith("\\<")) {
            return CharMatcher.anyOf("\\<>").trimFrom(raw);
        }
        if (raw.startsWith("\\[")) {
            return CharMatcher.anyOf("\\[]").trimFrom(raw);
        }
        int end = raw.length() - (raw.endsWith("\\endmodality") ? "\\endmodality".length() : 0);
        int start = 0;
        if (raw.startsWith("\\throughout_transaction")) start = "\\throughout_transaction".length();
        else if (raw.startsWith("\\diamond_transaction")) start = "\\diamond_transaction".length();
        else if (raw.startsWith("\\diamond")) start = "\\diamond".length();
        else if (raw.startsWith("\\box_transaction")) start = "\\box_transaction".length();
        else if (raw.startsWith("\\box")) start = "\\box".length();
        else if (raw.startsWith("\\throughout")) start = "\\throughout".length();
        else if (raw.startsWith("\\modality")) start = raw.indexOf("}") + 1;
        return raw.substring(start, end);
    }

    /**
     * Given a raw modality string, this method determines the operator name.
     *
     * @param raw
     * @return
     */
    public static String operatorOfJavaBlock(String raw) {
        if (raw.startsWith("\\<")) {
            return "diamond";
        }
        if (raw.startsWith("\\[")) {
            return "box";
        }
        if (raw.startsWith("\\diamond_transaction")) return "diamond_transaction";
        if (raw.startsWith("\\box_transaction")) return "box_transaction";
        if (raw.startsWith("\\diamond")) return "diamond";
        if (raw.startsWith("\\box")) return "box";
        if (raw.startsWith("\\throughout_transaction")) return "throughout_transaction";
        if (raw.startsWith("\\throughout")) return "throughout";
        if (raw.startsWith("\\modality")) {
            int start = raw.indexOf('{') + 1;
            int end = raw.indexOf('}');
            return raw.substring(start, end);
        }
        return "n/a";
    }

    private static boolean isSelectTerm(Term term) {
        return term.op().name().toString().endsWith("::select") && term.arity() == 3;
    }

    @Override
    public Term visitParallel(KeYParser.ParallelContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return getTermFactory().createTerm(UpdateJunctor.PARALLEL_UPDATE, termL, termR);
    }

    @Override
    public Term visitTermEOF(KeYParser.TermEOFContext ctx) {
        return accept(ctx.term());
    }

    @Override
    public Term visitElementary_update_term(KeYParser.Elementary_update_termContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return getServices().getTermBuilder().elementary(termL, termR);
    }

    @Override
    public Term visitEquivalence_term(KeYParser.Equivalence_termContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return getTermFactory().createTerm(Equality.EQV, termL, termR);
    }

    @Override
    public Term visitImplication_term(KeYParser.Implication_termContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return getTermFactory().createTerm(Junctor.IMP, termL, termR);
    }

    @Override
    public Term visitDisjunction_term(KeYParser.Disjunction_termContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return getTermFactory().createTerm(Junctor.OR, termL, termR);
    }

    @Override
    public Term visitConjunction_term(KeYParser.Conjunction_termContext ctx) {
        if (accept(ctx.term(0)) == null) {
            System.out.println("sss");
        }
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));

        return getTermFactory().createTerm(Junctor.AND, termL, termR);
    }

    @Override
    public Term visitUnaryMinus(KeYParser.UnaryMinusContext ctx) {
        Term result = accept(ctx.term());
        if (result.sort() != Sort.FORMULA) {
            return getTermFactory().createTerm(
                    functions().lookup(new Name("neg")), result);
        } else {
            semanticError(ctx, "Formula cannot be prefixed with '-'");
        }
        return result;
    }

    @Override
    public Term visitNegation(KeYParser.NegationContext ctx) {
        Term termL = accept(ctx.term());
        return getTermFactory().createTerm(Junctor.NOT, termL);
    }

    @Override
    public Term visitTermEquals(KeYParser.TermEqualsContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return capsulateTf(ctx, () -> getTermFactory().createTerm(Equality.EQUALS, termL, termR));
    }


    @Override
    public Term visitTermNotEquals(KeYParser.TermNotEqualsContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.NOT,
                getTermFactory().createTerm(Equality.EQUALS, termL, termR)));
        /*
                Term b = accept(ctx.a1); if (a.sort() == Sort.FORMULA || b.sort() == Sort.FORMULA) {
            String errorMessage =
                    "The term equality \'=\'/\'!=\' is not " +
                            "allowed between formulas.\n Please use \'" + Equality.EQV +
                            "\' in combination with \'" + Junctor.NOT + "\' instead.";
            if (a.op() == Junctor.TRUE || a.op() == Junctor.FALSE ||
                    b.op() == Junctor.TRUE || b.op() == Junctor.FALSE) {
                errorMessage +=
                        " It seems as if you have mixed up the boolean " +
                                "constants \'TRUE\'/\'FALSE\' " +
                                "with the formulas \'true\'/\'false\'.";
            }
            semanticError(ctx, errorMessage);
        }*/
    }

    @Override
    public Term visitTermCompare(KeYParser.TermCompareContext ctx) {
        String op_name = "";
        if (ctx.LESS() != null)
            op_name = "lt";
        if (ctx.LESSEQUAL() != null)
            op_name = "leq";
        if (ctx.GREATER() != null)
            op_name = "gt";
        if (ctx.GREATEREQUAL() != null)
            op_name = "geq";
        var op = (Function) functions().lookup(new Name(op_name));
        if (op == null) {
            semanticError(ctx, "Function symbol '" + op_name + "' not found.");
        }
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return capsulateTf(ctx, () -> getTermFactory().createTerm(op, termL, termR));
    }

    @Override
    public Term visitTermWeakArith(KeYParser.TermWeakArithContext ctx) {
        String op_name = "";
        if (ctx.PLUS() != null) {
            op_name = "add";
        }
        if (ctx.MINUS() != null) {
            op_name = "sub";
        }
        var op = (Function) functions().lookup(new Name(op_name));
        if (op == null) {
            semanticError(ctx, "Function symbol '" + op_name + "' not found.");
        }
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        return capsulateTf(ctx, () -> getTermFactory().createTerm(op, termL, termR));
    }

    @Override
    public Term visitTermMult(KeYParser.TermMultContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        var op = (Function) functions().lookup(new Name("mul"));
        if (op == null) {
            semanticError(ctx, "Function symbol 'mul' not found.");
        }
        return capsulateTf(ctx, () -> getTermFactory().createTerm(op, termL, termR));
    }

    @Override
    public Term visitTermDivisionModulo(KeYParser.TermDivisionModuloContext ctx) {
        Term termL = accept(ctx.term(0));
        Term termR = accept(ctx.term(1));
        String op_name = "";
        if (ctx.SLASH() != null) {
            op_name = "div";
        }
        if (ctx.PERCENT() != null) {
            op_name = "mod";
        }
        var op = (Function) functions().lookup(new Name(op_name));
        if (op == null) {
            semanticError(ctx, "Function symbol '" + op_name + "' not found.");
        }
        return capsulateTf(ctx, () -> getTermFactory().createTerm(op, termL, termR));
    }

    protected Term capsulateTf(ParserRuleContext ctx, Supplier<Term> termSupplier) {
        try {
            return termSupplier.get();
        } catch (TermCreationException e) {
            throw new BuildingException(ctx, String.format("Could not build term on: %s", ctx.getText()), e);
        }
    }

    @Override
    public String visitStaticAttributeOrQueryReference(KeYParser.StaticAttributeOrQueryReferenceContext ctx) {
        //TODO weigl: this rule is a total grammar blower.
        String attrReference = ctx.id.getText();
        for (int i = 0; i < ctx.EMPTYBRACKETS().size(); i++) {
            attrReference += "[]";
        }

        /*KeYJavaType kjt = null;
        kjt = getTypeByClassName(attrReference);
        if (kjt == null) {
            throwEx(new NotDeclException(input, "Class", attrReference));
        }
        attrReference = kjt.getSort().name().toString();
        match(input, DOT, null);
            attrReference += "::" + input.LT(1).getText();
            match(input, IDENT, null);
            if(savedGuessing > -1) {
                state.backtracking = savedGuessing;
                savedGuessing = -1;
            }*/
        return attrReference;
    }

    /*@Override
    public Term visitStatic_attribute_suffix(KeYParser.Static_attribute_suffixContext ctx) {
        Operator v = null;
        String attributeName = accept(ctx.staticAttributeOrQueryReference());
        String className;
        if (attributeName.indexOf(':') != -1) {
            className =
                    attributeName.substring(0, attributeName.indexOf(':'));
        } else {
            className =
                    attributeName.substring(0, attributeName.lastIndexOf("."));
        }
        v = getAttributeInPrefixSort(getTypeByClassName(className).getSort(), attributeName);
        return createAttributeTerm(null, v, ctx);
    }
    */

    private LogicVariable bindVar(String id, Sort s) {
        LogicVariable v = new LogicVariable(new Name(id), s);
        namespaces().setVariables(variables().extended(v));
        return v;
    }

    private LogicVariable bindVar(LogicVariable v) {
        namespaces().setVariables(variables().extended(v));
        return v;
    }

    private void bindVar() {
        namespaces().setVariables(new Namespace(variables()));
    }

    private Term toZNotation(String literal, Namespace<Function> functions) {
        literal = literal.replace("_", "");
        final boolean negative = (literal.charAt(0) == '-');
        if (negative) {
            literal = literal.substring(1);
        }
        int base = 10;

        if (literal.startsWith("0x")) {
            literal = literal.substring(2);
            base = 16;
        }

        if (literal.startsWith("0b")) {
            literal = literal.substring(2);
            base = 8;
        }

        BigInteger bi = new BigInteger(literal, base);
        return toZNotation(bi, functions);
    }

    private Term toZNotation(BigInteger bi, Namespace<Function> functions) {
        boolean negative = bi.signum() < 0;
        String s = bi.abs().toString();
        Term result = getTermFactory()
                .createTerm(functions.lookup(new Name("#")));

        for (int i = 0; i < s.length(); i++)
            result = getTermFactory().createTerm(
                    functions.lookup(new Name(s.substring(i, i + 1))), result);

        if (negative) {
            result = getTermFactory().createTerm(
                    functions.lookup(new Name("neglit")), result);
        }
        return getTermFactory().createTerm(
                functions.lookup(new Name("Z")), result);
    }

    @Override
    public Sequent visitSeq(KeYParser.SeqContext ctx) {
        Semisequent ant = accept(ctx.ant);
        Semisequent suc = accept(ctx.suc);
        return Sequent.createSequent(ant, suc);
    }

    @Override
    public Sequent visitSeqEOF(KeYParser.SeqEOFContext ctx) {
        return accept(ctx.seq());
    }

    @Override
    public Object visitTermorseq(KeYParser.TermorseqContext ctx) {
        Term head = accept(ctx.head);
        Sequent s = accept(ctx.s);
        Semisequent ss = accept(ctx.ss);
        if (head != null && s == null && ss == null) {
            return head;
        }
        if (head != null && ss != null) {
            // A sequent with only head in the antecedent.
            Semisequent ant = Semisequent.EMPTY_SEMISEQUENT;
            ant = ant.insertFirst(
                    new SequentFormula(head)).semisequent();
            return Sequent.createSequent(ant, ss);
        }
        if (head != null && s != null) {
            // A sequent.  Prepend head to the antecedent.
            Semisequent newAnt = s.antecedent();
            newAnt = newAnt.insertFirst(
                    new SequentFormula(head)).semisequent();
            return Sequent.createSequent(newAnt, s.succedent());
        }
        if (ss != null) {
            return Sequent.createSequent(Semisequent.EMPTY_SEMISEQUENT, ss);
        }
        assert (false);
        return null;
    }

    @Override
    public Semisequent visitSemisequent(KeYParser.SemisequentContext ctx) {
        Semisequent ss = accept(ctx.ss);
        if (ss == null)
            ss = Semisequent.EMPTY_SEMISEQUENT;
        Term head = accept(ctx.term());
        if (head != null)
            ss = ss.insertFirst(new SequentFormula(head)).semisequent();
        return ss;
    }

    private PairOfStringAndJavaBlock getJavaBlock(Token t) {
        PairOfStringAndJavaBlock sjb = new PairOfStringAndJavaBlock();
        String s = t.getText().trim();
        String cleanJava = trimJavaBlock(s);
        sjb.opName = operatorOfJavaBlock(s);

        Debug.out("Modal operator name passed to getJavaBlock: ", sjb.opName);
        Debug.out("Java block passed to getJavaBlock: ", s);

        try {
            try {
                SchemaJavaReader jr = new SchemaRecoder2KeY(services, nss);
                jr.setSVNamespace(schemaVariables());
                try {
                    sjb.javaBlock = jr.readBlockWithProgramVariables(programVariables(), cleanJava);
                } catch (Exception e) {
                    sjb.javaBlock = jr.readBlockWithEmptyContext(cleanJava);
                }
            } catch (Exception e) {
                if (cleanJava.startsWith("{..")) {// do not fallback
                    throw e;
                }
                JavaReader jr = new Recoder2KeY(services, nss);
                try {
                    sjb.javaBlock = jr.readBlockWithProgramVariables(programVariables(), cleanJava);
                } catch (Exception e1) {
                    sjb.javaBlock = jr.readBlockWithEmptyContext(cleanJava);
                }
            }
        } catch (ConvertException e) {
            throw new BuildingException(t, "Could not parse java: '" + cleanJava + "'", e);
        }
        return sjb;
    }

    @Override
    public Object visitMetaId(KeYParser.MetaIdContext ctx) {
        var id = visitSimple_ident(ctx.simple_ident());
        TermTransformer v = AbstractTermTransformer.name2metaop(id);
        if (v == null)
            semanticError(ctx, "Unknown metaoperator: " + id);
        return v;
    }

    @Override
    public Term visitMetaTerm(KeYParser.MetaTermContext ctx) {
        Operator metaId = accept(ctx.metaId());
        List<Term> t = mapOf(ctx.term());
        return capsulateTf(ctx, () -> getTermFactory().createTerm(metaId, t));
    }


    @Override
    public Term visitDotAll(KeYParser.DotAllContext ctx) {
        Term prefix = pop();
        return services.getTermBuilder().allFields(prefix);
    }

    @Override
    public Term visitTermHeap(KeYParser.TermHeapContext ctx) {
        //TODO
        return (Term) super.visitTermHeap(ctx);
    }

    @Override
    public Term visitTermCall(KeYParser.TermCallContext ctx) {
        //TODO
        return (Term) super.visitTermCall(ctx);
    }


    @Override
    public Term visitTermAttribute(KeYParser.TermAttributeContext ctx) {
        Term prefix = accept(ctx.term());
        String memberName = ctx.attrid().getText();
        if (prefix.sort() == getServices().getTypeConverter().getSeqLDT().targetSort()) {
            if ("length".equals(memberName)) {
                return getServices().getTermBuilder().seqLen(prefix);
            } else {
                semanticError(ctx,
                        "There is no attribute '%s'for sequences (Seq), only 'length' is supported.", memberName);
            }
        }
        if (ctx.attrid().clss != null) {
            String className = ctx.attrid().clss.getText();
            String qname = ctx.attrid().id2.getText();
            //Term result = getServices().getJavaInfo().getStaticProgramMethodTerm(qname, (Term[]) args.toArray(), className);
            /*if (result == null) {
                final Sort sort = lookupSort(className);
                if (sort == null) {
                    semanticError(ctx, "Could not find matching sort for " + className);
                }
                KeYJavaType kjt = getServices().getJavaInfo().getKeYJavaType(sort);
                if (kjt == null) {
                    semanticError(ctx, "Found logic sort for " + className +
                            " but no corresponding java type!");
                }
            }*/
        } else {
            memberName = CharMatcher.anyOf("()").trimFrom(memberName);
            Operator v = getAttributeInPrefixSort(prefix.sort(), memberName);
            return createAttributeTerm(prefix, v, ctx);
        }
        return prefix;
    }

    public Term createAttributeTerm(Term prefix, Operator attribute, ParserRuleContext ctx) {
        Term result = prefix;

        if (attribute instanceof SchemaVariable) {
            /*if (!inSchemaMode()) {
                semanticError(null, "Schemavariables may only occur inside taclets.");
            }*/
            SchemaVariable sv = (SchemaVariable) attribute;
            if (sv.sort() instanceof ProgramSVSort
                    || sv.sort() == AbstractTermTransformer.METASORT) {
                semanticError(null, "Cannot use schema variable " + sv + " as an attribute");
            }
            result = getServices().getTermBuilder().select(sv.sort(),
                    getServices().getTermBuilder().getBaseHeap(),
                    prefix,
                    capsulateTf(ctx, () -> getTermFactory().createTerm(attribute)));
        } else {
            if (attribute instanceof LogicVariable) {
                Term attrTerm = capsulateTf(ctx, () -> getTermFactory().createTerm(attribute));
                result = getServices().getTermBuilder().dot(Sort.ANY, result, attrTerm);
            } else if (attribute instanceof ProgramConstant) {
                result = capsulateTf(ctx, () -> getTermFactory().createTerm(attribute));
            } else if (attribute == getServices().getJavaInfo().getArrayLength()) {
                Term finalResult = result;
                result = capsulateTf(ctx, () -> getServices().getTermBuilder().dotLength(finalResult));
            } else {
                ProgramVariable pv = (ProgramVariable) attribute;
                Function fieldSymbol
                        = getServices().getTypeConverter()
                        .getHeapLDT()
                        .getFieldSymbolForPV((LocationVariable) pv,
                                getServices());
                if (pv.isStatic()) {
                    result = getServices().getTermBuilder().staticDot(pv.sort(), fieldSymbol);
                } else {
                    result = getServices().getTermBuilder().dot(pv.sort(), result, fieldSymbol);
                }
            }
        }
        return result;
    }

    private Operator getAttributeInPrefixSort(Sort prefixSort, String attributeName) {
        final JavaInfo javaInfo = getJavaInfo();

        Operator result = schemaVariables().lookup(new Name(attributeName));
        if (result == null) {

            final boolean unambigousAttributeName = attributeName.indexOf(':') != -1;

            if (unambigousAttributeName) {
                result = javaInfo.getAttribute(attributeName);
            } else if (attributeName.equals("length")) {
                try {
                    result = javaInfo.getArrayLength();
                } catch (Exception ex) {
                    throw new BuildingException(ex);
                }
            } else if (attributeName.equals("<inv>")) {
                // The invariant observer "<inv>" is implicit and
                // not part of the class declaration
                // A special case is needed, hence.
                result = javaInfo.getInvProgramVar();
            } else {
                final KeYJavaType prefixKJT = javaInfo.getKeYJavaType(prefixSort);
                if (prefixKJT == null) {
                    semanticError(null, "Could not find type '" + prefixSort + "'. Maybe mispelled or " +
                            "you use an array or object type in a .key-file with missing " +
                            "\\javaSource section.");
                }
                // WATCHOUT why not in DECLARATION MODE
                ProgramVariable var = javaInfo.getCanonicalFieldProgramVariable(attributeName, prefixKJT);
                if (var == null) {
                    LogicVariable logicalvar = (LogicVariable) namespaces().variables().lookup(attributeName);
                    if (logicalvar == null) {
                        semanticError(null, "There is no attribute '" + attributeName +
                                "' declared in type '" + prefixSort + "' and no logical variable of that name.");
                    } else {
                        result = logicalvar;
                    }
                } else {
                    result = var;
                }
            }
        }

        if (result == null && !("length".equals(attributeName))) {
            throwEx(new NotDeclException(null, "Attribute ", attributeName));
        }
        return result;
    }

    @Override
    public String visitAttrid(KeYParser.AttridContext ctx) {
        return ctx.getText();
    }

    private String unescapeString(String string) {
        char[] chars = string.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\\' && i < chars.length - 1) {
                switch (chars[++i]) {
                    case 'n':
                        sb.append("\n");
                        break;
                    case 'f':
                        sb.append("\f");
                        break;
                    case 'r':
                        sb.append("\r");
                        break;
                    case 't':
                        sb.append("\t");
                        break;
                    case 'b':
                        sb.append("\b");
                        break;
                    case ':':
                        sb.append("\\:");
                        break; // this is so in KeY ...
                    default:
                        sb.append(chars[i]);
                        break; // this more relaxed than before, \a becomes a ...
                }
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    @Override
    public Term visitString_literal(KeYParser.String_literalContext ctx) {
        String s = unescapeString(ctx.id.getText());
        return getServices().getTypeConverter()
                .convertToLogicElement(new StringLiteral(s));
    }

    /*
    public Term visitQuery_suffix(KeYParser.Query_suffixContext ctx) {
        String memberName = pop();
        Term prefix = pop();
        String classRef, name;
        boolean brackets = false;
        List<Term> args = accept(ctx.argument_list());
        // true in case class name is not explicitly mentioned as part of memberName
        memberName = CharMatcher.anyOf("()").trimFrom(memberName);
        boolean implicitClassName = memberName.indexOf("::") == -1;

        if (implicitClassName) {
            classRef = prefix.sort().name().toString();
            name = memberName;
        } else {
            String[] parts = memberName.split("::", 2);
            classRef = parts[0];
            name = parts[1];
        }
        KeYJavaType kjt = getTypeByClassName(classRef);
        if (kjt == null)
            semanticError(ctx, "Could not find java type for %s", classRef);
        classRef = kjt.getFullName();

        return getServices().getJavaInfo().getProgramMethodTerm(prefix, name,
                args.toArray(new Term[0]), classRef, implicitClassName);
    }
    */

    @Override
    public Term visitCast(KeYParser.CastContext ctx) {
        final Sort objectSort = getServices().getJavaInfo().objectSort();
        Term result = accept(ctx.term());
        Sort s = accept(ctx.sortId());
        if (s == null) {
            semanticError(ctx, "Tried to cast to unknown type.");
        } else if (objectSort != null
                && !s.extendsTrans(objectSort)
                && result.sort().extendsTrans(objectSort)) {
            semanticError(ctx, "Illegal cast from " + result.sort() +
                    " to sort " + s +
                    ". Casts between primitive and reference types are not allowed. ");
        }
        return getTermFactory().createTerm(
                s.getCastSymbol(getServices()), result);
    }


    private void markHeapAsExplicit(Term a) {
        if (isSelectTerm(a)) {
            explicitHeap.add(a);
            a.subs().forEach(this::markHeapAsExplicit);
        }
    }

    /*
    private Term createStaticAttributeOrMethod(JavaQuery jq, KeYParser.AccesstermContext ctx) {
        final var kjt = jq.kjt;
        String mn = jq.attributeNames;
        if (jq.maybeAttr != null) {
            ProgramVariable maybeAttr = getJavaInfo().getAttribute(mn, kjt);
            if (maybeAttr != null) {
                var op = getAttributeInPrefixSort(kjt.getSort(), mn);
                return createAttributeTerm(null, op, ctx);
            }
        } else {
            var suffix = ctx.atom_suffix(ctx.atom_suffix().size() - 1);
            for (IProgramMethod pm : getJavaInfo().getAllProgramMethods(kjt)) {
                if (pm != null && pm.isStatic() && pm.name().toString().equals(kjt.getFullName() + "::" + mn)) {
                    List<Term> arguments = mapOf(suffix.attribute_or_query_suffix().result.args.argument());
                    Term[] args = arguments.toArray(new Term[0]);
                    return getJavaInfo().getStaticProgramMethodTerm(mn, args, kjt.getFullName());
                }
            }
        }
        assert false;
        return null;
    }
    */

    @Override
    public Object visitBracket_access_heap_upate(KeYParser.Bracket_access_heap_upateContext ctx) {
        Term heap = pop();
        Term target = accept(ctx.target);
        Term val = accept(ctx.val);
        Term objectTerm = target.sub(1);
        Term fieldTerm = target.sub(2);
        return getServices().getTermBuilder().store(heap, objectTerm, fieldTerm, val);
    }


    @Override
    public Object visitBracket_access_heap_term(KeYParser.Bracket_access_heap_termContext ctx) {
        Term heap = pop();

        String id = accept(ctx.simple_ident());
        List<Term> args = accept(ctx.args);
        Function f = functions().lookup(new Name(id));
        if (f == null) {
            semanticError(ctx, "Unknown heap constructor " + id);
        }
        Term[] arguments = args.toArray(new Term[0]);
        Term[] augmentedArgs = new Term[args.size() + 1];
        System.arraycopy(arguments, 0, augmentedArgs, 1, arguments.length);
        augmentedArgs[0] = heap;
        Term result = capsulateTf(ctx, () -> getTermFactory().createTerm(f, augmentedArgs));
        if (!result.sort().name().toString().equals("Heap")) {
            semanticError(ctx, id + " is not a heap constructor ");
        }
        return result;
    }

    @Override
    public Object visitBracket_access_star(KeYParser.Bracket_access_starContext ctx) {
        Term reference = pop();
        Term rangeFrom = toZNotation("0", functions());
        Term lt = getServices().getTermBuilder().dotLength(reference);
        Term one = toZNotation("1", functions());
        Term rangeTo = getTermFactory().createTerm(
                functions().lookup(new Name("sub")), lt, one);
        //TODO construct
        return null;
    }

    @Override
    public Object visitBracket_access_indexrange(KeYParser.Bracket_access_indexrangeContext ctx) {
        Term reference = pop();
        //FIXME This is ugly as hell. How to a have a better check than just a name?
        boolean sequenceAccess = reference.sort().name().toString().equalsIgnoreCase("seq");
        //boolean heapUpdate = reference.sort().name().toString().equalsIgnoreCase("Heap");

        if (sequenceAccess) {
            if (ctx.rangeTo != null) {
                semanticError(ctx, "Range access for sequence terms not allowed");
            }
            Term indexTerm = accept(ctx.indexTerm);
            if (!isIntTerm(indexTerm))
                semanticError(ctx, "Expecting term of sort %s as index of sequence %s, but found: %s",
                        IntegerLDT.NAME, reference, indexTerm);
            return getServices().getTermBuilder().seqGet(Sort.ANY, reference, indexTerm);
        }

        if (ctx.rangeTo != null) {
            Term rangeFrom = accept(ctx.indexTerm);
            Term rangeTo = accept(ctx.rangeTo);
            if (rangeTo != null) {
                if (quantifiedArrayGuard == null) {
                    semanticError(ctx, "Quantified array expressions are only allowed in locations.");
                }
                LogicVariable indexVar = new LogicVariable(new Name("i"),
                        sorts().lookup(new Name("int")));
                Term indexTerm = capsulateTf(ctx, () -> getTermFactory().createTerm(indexVar));

                Function leq = functions().lookup(new Name("leq"));
                Term fromTerm = capsulateTf(ctx, () -> getTermFactory().createTerm(leq, rangeFrom, indexTerm));
                Term toTerm = capsulateTf(ctx, () -> getTermFactory().createTerm(leq, indexTerm, rangeTo));
                Term guardTerm = capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.AND, fromTerm, toTerm));
                quantifiedArrayGuard = capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.AND, quantifiedArrayGuard, guardTerm));
                //TODO check quantifiedArrayGuard!
            }
        }
        Term indexTerm = accept(ctx.indexTerm);
        return getServices().getTermBuilder().dotArr(reference, indexTerm);
    }

    @Override
    public Object visitBoolean_literal(KeYParser.Boolean_literalContext ctx) {
        if (ctx.TRUE() != null)
            return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.TRUE));
        else
            return capsulateTf(ctx, () -> getTermFactory().createTerm(Junctor.FALSE));
    }

    @Override
    public Object visitTermLabeled(KeYParser.TermLabeledContext ctx) {
        Term t = accept(ctx.term());
        if (ctx.LGUILLEMETS() != null) {
            ImmutableArray<TermLabel> labels = accept(ctx.label());
            if (labels.size() > 0) {
                t = getServices().getTermBuilder().addLabel(t, labels);
            }
        }
        return t;
    }

    @Override
    public ImmutableArray<TermLabel> visitLabel(KeYParser.LabelContext ctx) {
        List<TermLabel> labels = mapOf(ctx.single_label());
        return new ImmutableArray(labels);
    }

    @Override
    public TermLabel visitSingle_label(KeYParser.Single_labelContext ctx) {
        String labelName = "";
        TermLabel left = null;
        TermLabel right = null;

        if (ctx.IDENT() != null)
            labelName = ctx.IDENT().getText();
        if (ctx.STAR() != null)
            labelName = ctx.STAR().getText();

        TermLabel label = null;
        List<String> parameters = mapOf(ctx.string_value());
        try {
            SchemaVariable var = schemaVariables().lookup(new Name(labelName));
            if (var instanceof TermLabel) {
                label = (TermLabel) var;
            }
            if (label == null) {
                label = getServices().getProfile()
                        .getTermLabelManager()
                        .parseLabel(labelName, parameters, getServices());
            }
        } catch (Exception ex) {
            throw new BuildingException(ctx, ex);
        }
        return label;
    }

    @Override
    public Object visitAbbreviation(KeYParser.AbbreviationContext ctx) {
        String sc = accept(ctx.name);
        Term a = scm.getTerm(sc);
        if (a == null) {
            throwEx(new NotDeclException(null, "abbreviation", sc));
        }
        return a;
    }

    @Override
    public Term visitIfThenElseTerm(KeYParser.IfThenElseTermContext ctx) {
        var condF = (Term) ctx.condF.accept(this);
        if (condF.sort() != Sort.FORMULA) {
            semanticError(ctx, "Condition of an \\if-then-else term has to be a formula.");
        }
        var thenT = (Term) ctx.thenT.accept(this);
        var elseT = (Term) ctx.elseT.accept(this);
        return capsulateTf(ctx, () -> getTermFactory().createTerm(IfThenElse.IF_THEN_ELSE, condF, thenT, elseT));
    }


    @Override
    public Object visitIfExThenElseTerm(KeYParser.IfExThenElseTermContext ctx) {
        Namespace<QuantifiableVariable> orig = variables();
        List<QuantifiableVariable> exVars = accept(ctx.bound_variables());
        Term condF = accept(ctx.condF);
        if (condF.sort() != Sort.FORMULA) {
            semanticError(ctx, "Condition of an \\ifEx-then-else term has to be a formula.");
        }

        Term thenT = accept(ctx.thenT);
        Term elseT = accept(ctx.elseT);
        ImmutableArray<QuantifiableVariable> exVarsArray
                = new ImmutableArray<>(exVars);
        var result = getTermFactory().createTerm(IfExThenElse.IF_EX_THEN_ELSE,
                new Term[]{condF, thenT, elseT},
                exVarsArray,
                null);
        unbindVars(orig);
        return result;
    }

    @Override
    public Term visitQuantifierterm(KeYParser.QuantifiertermContext ctx) {
        Operator op = null;
        Namespace<QuantifiableVariable> orig = variables();
        if (ctx.FORALL() != null)
            op = Quantifier.ALL;
        if (ctx.EXISTS() != null)
            op = Quantifier.EX;
        List<QuantifiableVariable> vs = accept(ctx.bound_variables());
        Term a1 = accept(ctx.term());
        var a = getTermFactory().createTerm(op,
                new ImmutableArray<>(a1),
                new ImmutableArray<>(vs.toArray(new QuantifiableVariable[0])),
                null);
        unbindVars(orig);
        return a;
    }

    @Override
    public Term visitLocset_term(KeYParser.Locset_termContext ctx) {
        List<Term> terms = mapOf(ctx.location_term());
        return getServices().getTermBuilder().union(terms);
    }

    @Override
    public Object visitLocation_term(KeYParser.Location_termContext ctx) {
        Term obj = accept(ctx.obj);
        Term field = accept(ctx.field);
        return getServices().getTermBuilder().singleton(obj, field);
    }

    @Override
    public Term visitSubstitutionterm(KeYParser.SubstitutiontermContext ctx) {
        SubstOp op = WarySubstOp.SUBST;
        Namespace<QuantifiableVariable> orig = variables();
        AbstractSortedOperator v = accept(ctx.bv);
        unbindVars(orig);
        if (v instanceof LogicVariable)
            bindVar((LogicVariable) v);
        else
            bindVar();

        Term a1 = accept(ctx.replacement);
        Term a2 = accept(ctx.haystack);
        try {
            Term result = getServices().getTermBuilder().subst(op, (QuantifiableVariable) v, a1, a2);
            return result;
        } catch (Exception e) {
            throw new BuildingException(ctx, e);
        } finally {
            unbindVars(orig);
        }
    }

    @Override
    public Term visitUpdateterm(KeYParser.UpdatetermContext ctx) {
        Term u = accept(ctx.term(0));
        Term a2 = accept(ctx.term(1));
        return capsulateTf(ctx, () -> getTermFactory().createTerm(UpdateApplication.UPDATE_APPLICATION, u, a2));
    }

    public List<QuantifiableVariable> visitBound_variables(KeYParser.Bound_variablesContext ctx) {
        List<QuantifiableVariable> seq = ctx.one_bound_variable().stream()
                .map(it -> (QuantifiableVariable) it.accept(this))
                .collect(Collectors.toList());
        return seq;
    }

    @Override
    public QuantifiableVariable visitOne_bound_variable(KeYParser.One_bound_variableContext ctx) {
        //public Object visitOne_schema_bound_variable(KeYParser.One_schema_bound_variableContext ctx) {
        String id = accept(ctx.simple_ident());
        Sort sort = accept(ctx.sortId());

        SchemaVariable ts = schemaVariables().lookup(new Name(id));
        if (ts != null) {
            if (!(ts instanceof VariableSV)) {
                semanticError(ctx, ts + " is not allowed in a quantifier. Note, that you can't "
                        + "use the normal syntax for quantifiers of the form \"\\exists int i;\""
                        + " in taclets. You have to define the variable as a schema variable"
                        + " and use the syntax \"\\exists i;\" instead.");
            }
            bindVar();
            return (QuantifiableVariable) ts;
        }

        if (sort != null && id != null) {
            return bindVar(id, sort);
        }
        return doLookup(new Name(ctx.id.getText()), schemaVariables(), variables());
    }


    @Override
    public Object visitTermModality(KeYParser.TermModalityContext ctx) {
        PairOfStringAndJavaBlock sjb = getJavaBlock(ctx.MODALITY().getSymbol());

        Operator op;
        if (sjb.opName.charAt(0) == '#') {
            /*if (!inSchemaMode()) {
                semanticError(ctx, "No schema elements allowed outside taclet declarations (" + sjb.opName + ")");
            }*/
            op = schemaVariables().lookup(new Name(sjb.opName));
        } else {
            op = Modality.getModality(sjb.opName);
        }
        if (op == null) {
            semanticError(ctx, "Unknown modal operator: " + sjb.opName);
        }

        Term a1 = accept(ctx.term());
        return capsulateTf(ctx, () -> getTermFactory().createTerm(op, new Term[]{a1}, null, sjb.javaBlock));
    }

    @Override
    public List<Term> visitArgument_list(KeYParser.Argument_listContext ctx) {
        return mapOf(ctx.term());
    }

    @Override
    public Object visitChar_literal(KeYParser.Char_literalContext ctx) {
        String s = ctx.CHAR_LITERAL().getText();
        int intVal = 0;
        if (s.length() == 3) {
            intVal = s.charAt(1);
        } else {
            try {
                intVal = Integer.parseInt(s.substring(3, s.length() - 1), 16);
            } catch (NumberFormatException ex) {
                semanticError(ctx, "'" + s + "' is not a valid character.");
            }
        }
        return getTermFactory().createTerm(
                functions().lookup(new Name("C")),
                toZNotation("" + intVal, functions()).sub(0));
    }

    public boolean isClass(String p) {
        return getJavaInfo().getTypeByClassName(p) != null;
    }

    /**
     * Handles "[sort]::a.name.or.something.else"
     *
     * @param ctx
     * @return a Term or an operator, depending the referenced object.
     */
    @Override
    public Object visitFuncpred_name(KeYParser.Funcpred_nameContext ctx) {
        Sort sortId = accept(ctx.sortId()); //TODO
        List<String> parts = mapOf(ctx.name.simple_ident());
        String varfuncid = ctx.name.getText();

        assert parts != null && varfuncid != null;

        boolean javaReference = parts.size() > 1
                && (isPackage(parts.get(0)) || isClass(parts.get(0)));

        if (javaReference) {
            return splitJava(parts);
        }

        if ("skip".equals(varfuncid)) {
            return UpdateJunctor.SKIP;
        }

        Operator op;
        if (varfuncid.endsWith(LIMIT_SUFFIX)) {
            varfuncid = varfuncid.substring(0, varfuncid.length() - 5);
            op = lookupVarfuncId(ctx, varfuncid, sortId);
            if (ObserverFunction.class.isAssignableFrom(op.getClass())) {
                op = getServices().getSpecificationRepository()
                        .limitObs((ObserverFunction) op).first;
            } else {
                semanticError(ctx, "Cannot can be limited: " + op);
            }
        } else {
            op = lookupVarfuncId(ctx, varfuncid, sortId);
        }

        return op;
    }

    public Term visitAccessterm(KeYParser.AccesstermContext ctx) {
        var name = accept(ctx.funcpred_name()); // Term or Operator
        Namespace<QuantifiableVariable> orig = variables();
        List<QuantifiableVariable> boundVars = accept(ctx.bound_variables());
        ImmutableArray<QuantifiableVariable> bv = boundVars != null
                ? new ImmutableArray<>(boundVars.toArray(new QuantifiableVariable[0]))
                : null;
        List<Term> arguments = accept(ctx.argument_list());
        Term[] args = arguments == null ? new Term[0] : arguments.toArray(new Term[0]);

        if (name instanceof JavaQuery) {
            JavaQuery java = (JavaQuery) name;
            Term result = null;

            if (boundVars != null) {
                addWarning(ctx, "Bound variables not supported for java calls");
            }

            for (String attributeName : java.attributeNames) {
                ProgramVariable maybeAttr = getJavaInfo().getAttribute(attributeName, java.kjt);
                if (maybeAttr != null) {
                    var op = getAttributeInPrefixSort(java.kjt.getSort(), attributeName);
                    result = createAttributeTerm(result, op, ctx);
                } else {
                    final var pm = getStaticQuery(java.kjt.getFullName(), attributeName);
                    if (pm != null) {
                        return getJavaInfo().getStaticProgramMethodTerm(attributeName, args, java.kjt.getFullName());
                    } else {
                        semanticError(ctx, "Unknown java attribute: %s", attributeName);
                    }
                }
            }
            return result;
        }

        Operator op = (Operator) name;
        Term result = null;
        if (op instanceof ParsableVariable) {
            if (ctx.argument_list() != null) {
                semanticError(ctx, "You used a variable like a predicate or function.");
            }
            if (boundVars != null)
                addWarning(ctx, "Bounded variable are ignored on a variable");
            result = termForParsedVariable((ParsableVariable) op, ctx);
        } else {
            if (boundVars == null) {
                result = capsulateTf(ctx, () -> getTermFactory().createTerm(op, args));
            } else {
                //sanity check
                assert op instanceof Function;

                for (int i = 0; i < args.length; i++) {
                    if (i < op.arity() && !op.bindVarsAt(i)) {
                        for (QuantifiableVariable qv : args[i].freeVars()) {
                            if (boundVars.contains(qv)) {
                                semanticError(ctx, "Building function term " + op + " with bound variables failed: "
                                        + "Variable " + qv + " must not occur free in subterm " + args[i]);
                            }
                        }
                    }
                }
                //create term
                result = capsulateTf(ctx, () -> getTermFactory().createTerm(op, args, bv, null));
            }
        }
        if (boundVars != null) {
            unbindVars(orig);
        }

        return result;
    }

    @Override
    public Object visitNumber(KeYParser.NumberContext ctx) {
        return toZNotation(ctx.getText(), functions());
    }

    private Term termForParsedVariable(ParsableVariable v, ParserRuleContext ctx) {
        if (v instanceof LogicVariable || v instanceof ProgramVariable) {
            return capsulateTf(ctx, () -> getTermFactory().createTerm(v));
        } else {
            if (v instanceof SchemaVariable) {
                return capsulateTf(ctx, () -> getTermFactory().createTerm(v));
            } else {
                String errorMessage = "";
                if (false) {
                    errorMessage += v + " is not a program, logic or schema variable";
                } else {
                    errorMessage += v + " is not a logic or program variable";
                }
                semanticError(null, errorMessage);
            }
        }
        return null;
    }

    public TermFactory getTermFactory() {
        return getServices().getTermFactory();
    }


    protected ImmutableSet<Modality> opSVHelper(String opName, ImmutableSet<Modality> modalities) {
        if (opName.charAt(0) == '#') {
            return lookupOperatorSV(opName, modalities);
        } else {
            Modality m = Modality.getModality(opName);
            if (m == null) {
                semanticError(null, "Unrecognised operator: " + opName);
            }
            modalities = modalities.add(m);
        }
        return modalities;
    }


    private String getTypeList(ImmutableList<ProgramVariable> vars) {
        StringBuffer result = new StringBuffer();
        final Iterator<ProgramVariable> it = vars.iterator();
        while (it.hasNext()) {
            result.append(it.next().getContainerType().getFullName());
            if (it.hasNext()) result.append(", ");
        }
        return result.toString();
    }

    KeYJavaType getTypeByClassName(String s) {
        KeYJavaType kjt = null;
        try {
            kjt = getJavaInfo().getTypeByClassName(s, null);
        } catch (RuntimeException e) {
            return null;
        }

        return kjt;
    }

    private boolean isPackage(String name) {
        try {
            return getJavaInfo().isPackage(name);
        } catch (RuntimeException e) {
            // may be thrown in cases of invalid java identifiers
            return false;
        }
    }

    protected boolean isHeapTerm(Term term) {
        return term != null && term.sort() ==
                getServices().getTypeConverter().getHeapLDT().targetSort();
    }

    private boolean isSequenceTerm(Term reference) {
        return reference != null && reference.sort().name().equals(SeqLDT.NAME);
    }

    private boolean isIntTerm(Term reference) {
        return reference.sort().name().equals(IntegerLDT.NAME);
    }

    private ImmutableSet<Modality> lookupOperatorSV(String opName, ImmutableSet<Modality> modalities) {
        SchemaVariable sv = schemaVariables().lookup(new Name(opName));
        if (sv == null || !(sv instanceof ModalOperatorSV)) {
            semanticError(null, "Schema variable " + opName + " not defined.");
        }
        ModalOperatorSV osv = (ModalOperatorSV) sv;
        modalities = modalities.union(osv.getModalities());
        return modalities;
    }

    private boolean isImplicitHeap(Term t) {
        return getServices().getTermBuilder().getBaseHeap().equals(t);
    }

    private Term replaceHeap(Term term, Term heap, ParserRuleContext ctx) {
        if (explicitHeap.contains(term)) return term;
        if (isSelectTerm(term)) {
            if (!isImplicitHeap(term.sub(0))) {
                //semanticError(null, "Expecting program variable heap as first argument of: %s", term);
                return term;
            }
            Term[] params = new Term[]{heap, replaceHeap(term.sub(1), heap, ctx), term.sub(2)};
            return capsulateTf(ctx, () -> getServices().getTermFactory().createTerm(term.op(), params));
        } else if (term.op() instanceof ObserverFunction) {
            if (!isImplicitHeap(term.sub(0))) {
                semanticError(null, "Expecting program variable heap as first argument of: %s", term);
            }

            Term[] params = new Term[term.arity()];
            params[0] = heap;
            params[1] = replaceHeap(term.sub(1), heap, ctx);
            for (int i = 2; i < params.length; i++) {
                params[i] = term.sub(i);
            }

            return capsulateTf(ctx, () -> getServices().getTermFactory().createTerm(term.op(), params));

        }
        return term;
    }

    /**
     * Replace standard heap by another heap in an observer function.
     */
    protected Term heapSelectionSuffix(Term term, Term heap, ParserRuleContext ctx) {
        if (!isHeapTerm(heap)) {
            semanticError(null, "Expecting term of type Heap but sort is %s for term %s", heap.sort(), term);
        }
        Term result = replaceHeap(term, heap, ctx);
        return result;
    }

    private IProgramMethod getStaticQuery(String className, String methodName) {
        final JavaInfo javaInfo = getJavaInfo();
        KeYJavaType kjt = getTypeByClassName(className);
        if (kjt != null) {
            final String name = kjt.getFullName() + "::" + methodName;
            for (IProgramMethod pm : javaInfo.getAllProgramMethods(kjt)) {
                if (pm != null && pm.isStatic() && pm.name().toString().equals(name)) {
                    return pm;
                }
            }
        }
        return null;
    }

    private JavaQuery splitJava(List<String> parts) {
        String packageName = null;
        String className = null;
        List<String> attributeName = null;
        int i = 0;
        for (; i < parts.size(); i++) {
            String test = parts.stream().limit(i).collect(Collectors.joining("."));
            if (isPackage(test)) {
                packageName = test;
            } else if (packageName != null) {
                break;
            }
        }

        KeYJavaType kjt = null;
        final int packageEnd = i - 1;
        for (i = 0; i < parts.size(); i++) {
            String test = parts.stream()
                    //.skip(packageEnd)
                    .limit(i + packageEnd)
                    .collect(Collectors.joining("."));
            kjt = getTypeByClassName(test);
            if (kjt != null) {
                className = test;
            } else if (className != null) {
                kjt = getTypeByClassName(className);
                break;
            }
        }
        final var classEnd = i - 1;
        attributeName = parts.stream().skip(classEnd).collect(Collectors.toList());
        return (new JavaQuery(packageName, className, attributeName, kjt));
    }

    private static class PairOfStringAndJavaBlock {
        String opName;
        JavaBlock javaBlock;
    }

    /*private boolean isStaticAttribute(String dotName) {
        final JavaInfo javaInfo = getJavaInfo();
        String[] javaParts = splitJava(dotName);
        KeYJavaType kjt = getTypeByClassName(javaParts[1]);
        if (kjt != null) {
            ProgramVariable maybeAttr = javaInfo.getAttribute(javaParts[2], kjt);
            if (maybeAttr != null) {
                return true;
            }
        }
        return false;
    }*/
}


class JavaQuery {
    final String packageName, className;
    final List<String> attributeNames;
    final KeYJavaType kjt;

    JavaQuery(String packageName, String className, List<String> attributeNames, KeYJavaType kjt) {
        this.packageName = packageName;
        this.className = className;
        this.attributeNames = attributeNames;
        this.kjt = kjt;
    }
}