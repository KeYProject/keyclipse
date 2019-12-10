package de.uka.ilkd.key.nparser.varexp;

import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.logic.sort.GenericSort;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.rule.VariableCondition;
import de.uka.ilkd.key.rule.conditions.*;
import de.uka.ilkd.key.rule.tacletbuilder.TacletBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.uka.ilkd.key.nparser.varexp.ArgumentType.SORT;
import static de.uka.ilkd.key.nparser.varexp.ArgumentType.TYPE_RESOLVER;
import static de.uka.ilkd.key.rule.conditions.TypeComparisonCondition.Mode.*;

/**
 * @author Alexander Weigl
 * @version 1 (12/9/19)
 */
public class TacletBuilderManipulators {
    private static final ArgumentType TR = TYPE_RESOLVER;
    private static final ArgumentType KJT = ArgumentType.JAVA_TYPE;
    private static final ArgumentType PV = ArgumentType.VARIABLE;
    private static final ArgumentType USV = ArgumentType.VARIABLE;
    private static final ArgumentType TSV = ArgumentType.VARIABLE;
    private static final ArgumentType ASV = ArgumentType.VARIABLE;
    private static final ArgumentType FSV = ArgumentType.VARIABLE;
    private static final ArgumentType SV = ArgumentType.VARIABLE;
    private static final ArgumentType TLSV = ArgumentType.VARIABLE;
    private static final ArgumentType S = ArgumentType.STRING;


    /**
     *
     */
    public static final AbstractConditionBuilder ABSTRACT_OR_INTERFACE
            = new ConstructorBasedBuilder("enumConst", AbstractOrInterfaceType.class, TR);

    /**
     *
     */
    public static final AbstractConditionBuilder SAME = new AbstractConditionBuilder("same", TR, TR) {
        @Override
        public TypeComparisonCondition build(Object[] arguments, boolean negated) {
            return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1], negated ? NOT_SAME : TypeComparisonCondition.Mode.SAME);
        }
    };

    /**
     *
     */
    public static final AbstractConditionBuilder IS_SUBTYPE = new AbstractConditionBuilder("sub", TR, TR) {
        @Override
        public TypeComparisonCondition build(Object[] arguments, boolean negated) {
            return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1], negated ? NOT_IS_SUBTYPE : TypeComparisonCondition.Mode.IS_SUBTYPE);
        }
    };

    /**
     *
     */
    public static final AbstractConditionBuilder STRICT = new AbstractConditionBuilder("scrictSub", TR, TR) {
        @Override
        public boolean isSuitableFor(String name) {
            if (super.isSuitableFor(name)) return true;
            return "\\strict\\sub".equalsIgnoreCase(name);
        }

        @Override
        public TypeComparisonCondition build(Object[] arguments, boolean negated) {
            if (negated) throw new IllegalArgumentException("Negation is not supported");
            return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1], STRICT_SUBTYPE);
        }
    };


    /**
     *
     */
    public static final AbstractConditionBuilder DISJOINT_MODULO_NULL = new AbstractConditionBuilder(
            "disjointModuloNull", TR, TR) {
        @Override
        public TypeComparisonCondition build(Object[] arguments, boolean negated) {
            if (negated) throw new IllegalArgumentException("Negation is not supported");
            return new TypeComparisonCondition((TypeResolver) arguments[0],
                    (TypeResolver) arguments[1], DISJOINTMODULONULL);
        }
    };

    /**
     *
     */
    public static final AbstractTacletBuilderCommand NEW_LABEL
            = new ConstructorBasedBuilder("newLabel", NewJumpLabelCondition.class, SV);

    /**
     *
     */
    public static final AbstractTacletBuilderCommand NEW_JAVATYPE = new AbstractTacletBuilderCommand("newJava", TR, KJT) {
        @Override
        public void build(TacletBuilder tacletBuilder, Object[] arguments, boolean negated) {
            if (negated) throw new IllegalArgumentException("Negation is not supported");
            KeYJavaType kjt = (KeYJavaType) arguments[1];
            tacletBuilder.addVarsNew((SchemaVariable) arguments[0], kjt.getJavaType());
        }
    };

    static class NotFreeInTacletBuilderCommand extends AbstractTacletBuilderCommand {
        public NotFreeInTacletBuilderCommand(@NotNull ArgumentType... argumentsTypes) {
            super("notFreeIn", argumentsTypes);
        }

        @Override
        public void build(TacletBuilder tacletBuilder, Object[] arguments, boolean negated) {
            SchemaVariable x = (SchemaVariable) arguments[0];
            for (int i = 1; i < arguments.length; i++) {
                tacletBuilder.addVarsNotFreeIn(x, (SchemaVariable) arguments[i]);
            }
        }
    }

    public static final AbstractTacletBuilderCommand FREE_1 = new NotFreeInTacletBuilderCommand(SV, SV);
    public static final AbstractTacletBuilderCommand FREE_2 = new NotFreeInTacletBuilderCommand(SV, SV, SV);
    public static final AbstractTacletBuilderCommand FREE_3 = new NotFreeInTacletBuilderCommand(SV, SV, SV, SV);
    public static final AbstractTacletBuilderCommand FREE_4 = new NotFreeInTacletBuilderCommand(SV, SV, SV, SV, SV);
    public static final AbstractTacletBuilderCommand FREE_5 = new NotFreeInTacletBuilderCommand(SV, SV, SV, SV, SV, SV);

    private static final List<TacletBuilderCommand> tacletBuilderCommands = new ArrayList<>(32);
    public static final AbstractTacletBuilderCommand NEW_TYPE_OF = new AbstractTacletBuilderCommand(
            "newTypeOf", SV, SV) {

        @Override
        public void build(TacletBuilder tacletBuilder, Object[] arguments, boolean negated) {
            if (negated) throw new IllegalArgumentException("Negation is not supported");
            tacletBuilder.addVarsNewDependingOn((SchemaVariable) arguments[0], (SchemaVariable) arguments[1]);

        }
    };
    public static final AbstractTacletBuilderCommand NEW_DEPENDING_ON = new AbstractTacletBuilderCommand(
            "newDependingOn", SV, SV) {
        @Override
        public void build(TacletBuilder tb, Object[] arguments, boolean negated) {
            if (negated) throw new IllegalArgumentException("Negation is not supported");
            tb.addVarsNewDependingOn((SchemaVariable) arguments[0],
                    (SchemaVariable) arguments[1]);
        }
    };

    public static final AbstractConditionBuilder FREE_LABEL_IN_VARIABLE
            = new ConstructorBasedBuilder("freeLabelInVariable", FreeLabelInVariableCondition.class, SV, SV);
    public static final AbstractConditionBuilder DIFFERENT
            = new ConstructorBasedBuilder("different", DifferentInstantiationCondition.class, SV, SV);
    public static final AbstractConditionBuilder FINAL
            = new ConstructorBasedBuilder("final", FinalReferenceCondition.class, SV);
    public static final AbstractConditionBuilder ENUM_CONST
            = new ConstructorBasedBuilder("enumConst", EnumConstantCondition.class, SV);
    public static final AbstractConditionBuilder LOCAL_VARIABLE
            = new ConstructorBasedBuilder("localVariable", LocalVariableCondition.class, SV);
    public static final AbstractConditionBuilder ARRAY_LENGTH
            = new ConstructorBasedBuilder("arrayLength", ArrayLengthCondition.class, SV);
    public static final AbstractConditionBuilder ARRAY
            = new ConstructorBasedBuilder("array", ArrayTypeCondition.class, SV);
    public static final AbstractConditionBuilder REFERENCE_ARRAY
            = new ConstructorBasedBuilder("referenceArray", ArrayComponentTypeCondition.class, SV);
    public static final AbstractConditionBuilder MAY_EXPAND_METHOD_2
            = new ConstructorBasedBuilder("mayExpandMethod", MayExpandMethodCondition.class, SV, SV);
    public static final AbstractConditionBuilder MAY_EXPAND_METHOD_3
            = new ConstructorBasedBuilder("mayExpandMethod", MayExpandMethodCondition.class, SV, SV, SV);
    public static final AbstractConditionBuilder STATIC_METHOD
            = new ConstructorBasedBuilder("staticMethod", StaticMethodCondition.class, SV, SV, SV);
    public static final AbstractConditionBuilder THIS_REFERENCE
            = new ConstructorBasedBuilder("thisReference", IsThisReference.class, SV, SV, SV);
    public static final AbstractConditionBuilder REFERENCE
            = new ConstructorBasedBuilder("reference", TypeCondition.class, SV, SV, SV);
    public static final AbstractConditionBuilder ENUM_TYPE
            = new ConstructorBasedBuilder("reference", EnumTypeCondition.class, SV, SV, SV);
    public static final AbstractConditionBuilder CONTAINS_ASSIGNMENT
            = new ConstructorBasedBuilder("containsAssignment", ContainsAssignmentCondition.class, SV);
    public static final AbstractConditionBuilder FIELD_TYPE
            = new ConstructorBasedBuilder("fieldType", FieldTypeToSortCondition.class, SV, SORT);
    public static final AbstractConditionBuilder STATIC_REFERENCE
            = new ConstructorBasedBuilder("staticReference", StaticReferenceCondition.class, SV);
    public static final TacletBuilderCommand DIFFERENT_FIELDS
            = new ConstructorBasedBuilder("differentFields", DifferentFields.class, SV, SV);
    public static final AbstractConditionBuilder SAME_OBSERVER
            = new ConstructorBasedBuilder("sameObserver", SameObserverCondition.class, PV, PV);
    public static AbstractConditionBuilder applyUpdateOnRigid
            = new ConstructorBasedBuilder("applyUpdateOnRigid", ApplyUpdateOnRigidCondition.class, USV, SV, SV);
    public static final AbstractConditionBuilder DROP_EFFECTLESS_ELEMENTARIES
            = new ConstructorBasedBuilder("dropEffectlessElementaries",
            DropEffectlessElementariesCondition.class, USV, SV, SV);
    public static final AbstractConditionBuilder SIMPLIFY_ITE_UPDATE
            = new ConstructorBasedBuilder("simplifyIfThenElseUpdate",
            SimplifyIfThenElseUpdateCondition.class, FSV, USV, USV, FSV, SV);
    public static final AbstractConditionBuilder SUBFORMULAS
            = new ConstructorBasedBuilder("subFormulas", SubFormulaCondition.class, FSV);
    public static final AbstractConditionBuilder STATIC_FIELD
            = new ConstructorBasedBuilder("staticField", StaticFieldCondition.class, FSV);
    public static final AbstractConditionBuilder SUBFORMULA
            = new ConstructorBasedBuilder("subFormula", SubFormulaCondition.class, FSV);
    public static final TacletBuilderCommand DROP_EFFECTLESS_STORES
            = new ConstructorBasedBuilder("dropEffectlessStores", DropEffectlessStoresCondition.class, TSV, TSV, TSV, TSV, TSV);
    public static final AbstractConditionBuilder EQUAL_UNIQUE
            = new ConstructorBasedBuilder("equalUnique", EqualUniqueCondition.class, TSV, TSV, FSV);
    public static final AbstractConditionBuilder META_DISJOINT
            = new ConstructorBasedBuilder("metaDisjoint", MetaDisjointCondition.class, TSV, TSV);
    public static final AbstractConditionBuilder IS_OBSERVER
            = new ConstructorBasedBuilder("isObserver", ObserverCondition.class, TSV, TSV);
    public static final AbstractConditionBuilder CONSTANT = new ConstructorBasedBuilder("constant", ConstantCondition.class, ASV);

    static class JavaTypeToSortConditionBuilder extends AbstractConditionBuilder {
        private final boolean elmen;

        public JavaTypeToSortConditionBuilder(@NotNull String triggerName, boolean forceElmentary) {
            super(triggerName, SV, SORT);
            this.elmen = forceElmentary;
        }

        @Override
        public VariableCondition build(Object[] arguments, boolean negated) {
            SchemaVariable v = (SchemaVariable) arguments[0];
            Sort s = (Sort) arguments[1];
            if (!(s instanceof GenericSort)) {
                throw new IllegalArgumentException("Generic sort is expected. Got: " + s);
            } else if (!JavaTypeToSortCondition.checkSortedSV(v)) {
                throw new IllegalArgumentException("Expected schema variable of kind EXPRESSION or TYPE, but is " + v);
            } else {
                return new JavaTypeToSortCondition(v, (GenericSort) s, this.elmen);
            }
        }
    }

    public static final AbstractConditionBuilder HAS_SORT = new JavaTypeToSortConditionBuilder("hasSort",false);
    public static final AbstractConditionBuilder HAS_ELEM_SORT = new JavaTypeToSortConditionBuilder("hasElementarySort",true);

    public static final AbstractConditionBuilder LABEL
            = new ConstructorBasedBuilder("label", TermLabelCondition.class, S, TLSV);

    static {
        register(SAME_OBSERVER, SIMPLIFY_ITE_UPDATE,
                ABSTRACT_OR_INTERFACE, SAME, IS_SUBTYPE,
                STRICT, DISJOINT_MODULO_NULL, NEW_JAVATYPE,
                FREE_1, FREE_2, FREE_3, FREE_4, FREE_5, NEW_TYPE_OF, NEW_DEPENDING_ON,
                FREE_LABEL_IN_VARIABLE, DIFFERENT, FINAL, ENUM_CONST,
                LOCAL_VARIABLE, ARRAY_LENGTH, ARRAY, REFERENCE_ARRAY, MAY_EXPAND_METHOD_2,
                MAY_EXPAND_METHOD_3, STATIC_METHOD, THIS_REFERENCE, REFERENCE,
                ENUM_TYPE, CONTAINS_ASSIGNMENT, FIELD_TYPE, STATIC_REFERENCE, DIFFERENT_FIELDS,
                SAME_OBSERVER, applyUpdateOnRigid, DROP_EFFECTLESS_ELEMENTARIES, SIMPLIFY_ITE_UPDATE,
                SUBFORMULAS, STATIC_FIELD, SUBFORMULA, DROP_EFFECTLESS_STORES, EQUAL_UNIQUE,
                META_DISJOINT, IS_OBSERVER, CONSTANT, HAS_SORT, LABEL, NEW_LABEL, HAS_ELEM_SORT
        );
    }

    public static void register(TacletBuilderCommand... cb) {
        for (var a : cb) {
            register(a);
        }
    }

    public static void register(TacletBuilderCommand cb) {
        tacletBuilderCommands.add(cb);
    }

    public static List<TacletBuilderCommand> getConditionBuilders() {
        return Collections.unmodifiableList(tacletBuilderCommands);
    }

    public static List<TacletBuilderCommand> getConditionBuildersFor(String name) {
        return tacletBuilderCommands.stream().filter(it -> it.isSuitableFor(name)).collect(Collectors.toList());
    }
}