package de.uka.ilkd.key.symbolic_execution.util;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.uka.ilkd.key.collection.ImmutableArray;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSLList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.ApplyStrategy.ApplyStrategyInfo;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.Choice;
import de.uka.ilkd.key.logic.DefaultVisitor;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Namespace;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermFactory;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.label.TermLabelManager.TermLabelConfiguration;
import de.uka.ilkd.key.logic.op.Function;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.IProgramVariable;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.JavaProfile;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.mgt.AxiomJustification;
import de.uka.ilkd.key.proof.mgt.ProofEnvironment;
import de.uka.ilkd.key.proof.mgt.RuleJustification;
import de.uka.ilkd.key.proof.mgt.RuleJustificationInfo;
import de.uka.ilkd.key.rule.BuiltInRule;
import de.uka.ilkd.key.rule.OneStepSimplifier;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.strategy.StrategyProperties;
import de.uka.ilkd.key.symbolic_execution.profile.SimplifyTermProfile;
import de.uka.ilkd.key.symbolic_execution.profile.SymbolicExecutionJavaProfile;
import de.uka.ilkd.key.symbolic_execution.rule.QuerySideProofRule;
import de.uka.ilkd.key.symbolic_execution.strategy.SymbolicExecutionStrategy;
import de.uka.ilkd.key.util.ProofStarter;
import de.uka.ilkd.key.util.Triple;

/**
 * Provides utility methods for side proofs.
 * @author Martin Hentschel
 */
public final class SideProofUtil {
   /**
    * Forbid instances.
    */
   private SideProofUtil() {
   }
   
   /**
    * Computes a general {@link Sequent} to prove in a side proof which
    * contains all {@link SequentFormula} of the original {@link Sequent}
    * except the given current {@link SequentFormula} and those which
    * contains modalities and queries.
    * @param goalSequent The original {@link Sequent} of the current {@link Goal}.
    * @param currentSF The {@link SequentFormula} to ignore.
    * @return The general initial {@link Sequent}.
    */
   public static Sequent computeGeneralSequentToProve(Sequent goalSequent, SequentFormula currentSF) {
      Sequent sequentToProve = Sequent.EMPTY_SEQUENT;
      for (SequentFormula sf : goalSequent.antecedent()) {
         if (sf != currentSF) {
            if (!SideProofUtil.containsModalityOrQuery(sf)) {
               sequentToProve = sequentToProve.addFormula(sf, true, false).sequent();
            }
         }
      }
      for (SequentFormula sf : goalSequent.succedent()) {
         if (sf != currentSF) {
            if (!SideProofUtil.containsModalityOrQuery(sf)) {
               sequentToProve = sequentToProve.addFormula(sf, false, false).sequent();
            }
         }
      }
      return sequentToProve;
   }
   
   /**
    * Starts the side proof and evaluates the {@link Sequent} to prove into a single {@link Term}.
    * @param services The {@link Services} to use.
    * @param proof The {@link Proof} from on which the side proof si performed.
    * @param originalSequent The original {@link Sequent} from which the side proof was created.
    * @param sequentToProve The {@link Sequent} to prove in a side proof.
    * @param label The {@link TermLabel} which is used to compute the result.
    * @param description The side proof description.
    * @param splittingOption The splitting options to use.
    * @return The result {@link Term}.
    * @throws ProofInputException Occurred Exception.
    */
   public static Term evaluateInSideProof(Services services, 
                                          Proof proof, 
                                          Sequent originalSequent,
                                          Sequent sequentToProve, 
                                          TermLabel label, 
                                          String description, 
                                          String splittingOption) throws ProofInputException {
      List<Triple<Term, Set<Term>, Node>> resultValuesAndConditions = SideProofUtil.computeResultsAndConditions(services, 
                                                                                                                proof, 
                                                                                                                sequentToProve, 
                                                                                                                label, 
                                                                                                                description, 
                                                                                                                StrategyProperties.METHOD_NONE, // Stop at methods to avoid endless executions and scenarios in which a precondition or null pointer check can't be shown
                                                                                                                StrategyProperties.LOOP_NONE, // Stop at loops to avoid endless executions and scenarios in which the invariant can't be shown to be initially valid or preserved.
                                                                                                                StrategyProperties.QUERY_OFF, // Stop at queries to to avoid endless executions and scenarios in which a precondition or null pointer check can't be shown
                                                                                                                splittingOption, 
                                                                                                                false);
      ImmutableList<Term> goalCondtions = ImmutableSLList.<Term>nil();
      for (Triple<Term, Set<Term>, Node> triple : resultValuesAndConditions) {
         List<Term> negatedGoalConditions = new LinkedList<Term>();
         for (Term term : triple.second) {
            negatedGoalConditions.add(services.getTermBuilder().not(term));
         }
         Term conditionsTerm = services.getTermBuilder().or(negatedGoalConditions);
         Term goalCondition = services.getTermBuilder().or(conditionsTerm, triple.first);
         goalCondition = SymbolicExecutionUtil.replaceSkolemConstants(originalSequent, goalCondition, services);
         goalCondtions = goalCondtions.append(goalCondition);
      }
      return services.getTermBuilder().and(goalCondtions);
   }
   
   /**
    * <p>
    * Starts the side proof and extracts the result {@link Term} and conditions.
    * </p>
    * <p>
    * New used names are automatically added to the {@link Namespace} of the {@link Services}.
    * </p>
    * @param services The {@link Services} to use.
    * @param proof The {@link Proof} from on which the side proof si performed.
    * @param sequentToProve The {@link Sequent} to prove in a side proof.
    * @param label The {@link TermLabel} which is used to compute the result.
    * @param description The side proof description.
    * @param splittingOption The splitting options to use.
    * @param addNamesToServices {@code true} defines that used names in result and conditions are added to the namespace of the given {@link Services}, {@code false} means that names are not added.
    * @return The found result {@link Term} and the conditions.
    * @throws ProofInputException Occurred Exception.
    */
   public static List<Triple<Term, Set<Term>, Node>> computeResultsAndConditions(Services services, 
                                                                                 Proof proof, 
                                                                                 Sequent sequentToProve, 
                                                                                 TermLabel label, 
                                                                                 String description,
                                                                                 String methodTreatment,
                                                                                 String loopTreatment,
                                                                                 String queryTreatment,
                                                                                 String splittingOption,
                                                                                 boolean addNamesToServices) throws ProofInputException {
      // Execute side proof
      ApplyStrategyInfo info = SideProofUtil.startSideProof(proof, sequentToProve, methodTreatment, loopTreatment, queryTreatment, splittingOption, true);
      try {
         // Extract results and conditions from side proof
         List<Triple<Term, Set<Term>, Node>> conditionsAndResultsMap = new LinkedList<Triple<Term, Set<Term>, Node>>();
         for (Goal resultGoal : info.getProof().openGoals()) {
            if (SymbolicExecutionUtil.hasApplicableRules(resultGoal)) {
               throw new IllegalStateException("Not all applicable rules are applied.");
            }
            Sequent sequent = resultGoal.sequent();
            Set<Term> resultConditions = new LinkedHashSet<Term>();
            List<Term> results = new LinkedList<Term>();
            for (SequentFormula sf : sequent.antecedent()) {
               if (sf.formula().containsLabel(label)) {
                  Term result = sf.formula();
                  result = removeLabelRecursive(services.getTermFactory(), result, label);
                  result = services.getTermBuilder().not(result);
                  results.add(result);
               }
            }
            for (SequentFormula sf : sequent.succedent()) {
               if (sf.formula().containsLabel(label)) {
                  Term result = sf.formula();
                  result = removeLabelRecursive(services.getTermFactory(), result, label);
                  results.add(result);
               }
            }
            Term result;
            if (results.isEmpty()) {
               result = services.getTermBuilder().tt();
            }
            else {
               result = services.getTermBuilder().or(results);
            }
            conditionsAndResultsMap.add(new Triple<Term, Set<Term>, Node>(result, resultConditions, resultGoal.node()));
         }
         return conditionsAndResultsMap;
      }
      finally {
         SideProofUtil.disposeOrStore(description, info);
      }
   }

   /**
    * Adds the given {@link TermLabel} to the given {@link Term} and to all of its children.
    * @param tf The {@link TermFactory} to use.
    * @param term The {@link Term} to add label to.
    * @param label The {@link TermLabel} to add.
    * @return A new {@link Term} with the given {@link TermLabel}.
    */
   public static Term addLabelRecursive(TermFactory tf, Term term, TermLabel label) {
      List<Term> newSubs = new LinkedList<Term>();
      for (Term oldSub : term.subs()) {
         newSubs.add(addLabelRecursive(tf, oldSub, label));
      }
      List<TermLabel> newLabels = new LinkedList<TermLabel>();
      for (TermLabel oldLabel : term.getLabels()) {
         newLabels.add(oldLabel);
      }
      newLabels.add(label);
      return tf.createTerm(term.op(), new ImmutableArray<Term>(newSubs), term.boundVars(), term.javaBlock(), new ImmutableArray<TermLabel>(newLabels));
   }
   
   /**
    * Removes the given {@link TermLabel} from the given {@link Term} and from all of its children.
    * @param tf The {@link TermFactory} to use.
    * @param term The {@link Term} to remove label from.
    * @param label The {@link TermLabel} to remove.
    * @return A new {@link Term} without the given {@link TermLabel}.
    */
   public static Term removeLabelRecursive(TermFactory tf, Term term, TermLabel label) {
      // Update children
      List<Term> newSubs = new LinkedList<Term>();
      ImmutableArray<Term> oldSubs = term.subs();
      for (Term oldSub : oldSubs) {
         newSubs.add(removeLabelRecursive(tf, oldSub, label));
      }
      // Update label
      List<TermLabel> newLabels = new LinkedList<TermLabel>();
      ImmutableArray<TermLabel> oldLabels = term.getLabels();
      for (TermLabel oldLabel : oldLabels) {
         if (oldLabel != label) {
            newLabels.add(oldLabel);
         }
      }
      return tf.createTerm(term.op(), new ImmutableArray<Term>(newSubs), term.boundVars(), term.javaBlock(), new ImmutableArray<TermLabel>(newLabels));
   }
   
   /**
    * Starts the side proof and evaluates the {@link Sequent} to prove into a single {@link Term}.
    * @param services The {@link Services} to use.
    * @param proof The {@link Proof} from on which the side proof si performed.
    * @param sequentToProve The {@link Sequent} to prove in a side proof.
    * @param operator The {@link Operator} which is used to compute the result.
    * @param description The side proof description.
    * @param splittingOption The splitting options to use.
    * @return The result {@link Term}.
    * @throws ProofInputException Occurred Exception.
    */
   public static Term evaluateInSideProof(Services services, 
                                          Proof proof, 
                                          Sequent sequentToProve, 
                                          Operator operator, 
                                          String description, 
                                          String splittingOption) throws ProofInputException {
      List<Triple<Term, Set<Term>, Node>> resultValuesAndConditions = SideProofUtil.computeResultsAndConditions(services, 
                                                                                                                proof, 
                                                                                                                sequentToProve, 
                                                                                                                operator, 
                                                                                                                description, 
                                                                                                                StrategyProperties.METHOD_NONE, // Stop at methods to avoid endless executions and scenarios in which a precondition or null pointer check can't be shown
                                                                                                                StrategyProperties.LOOP_NONE, // Stop at loops to avoid endless executions and scenarios in which the invariant can't be shown to be initially valid or preserved.
                                                                                                                StrategyProperties.QUERY_OFF, // Stop at queries to to avoid endless executions and scenarios in which a precondition or null pointer check can't be shown
                                                                                                                splittingOption, 
                                                                                                                false);
      ImmutableList<Term> goalCondtions = ImmutableSLList.<Term>nil();
      for (Triple<Term, Set<Term>, Node> triple : resultValuesAndConditions) {
         List<Term> negatedGoalConditions = new LinkedList<Term>();
         for (Term term : triple.second) {
            negatedGoalConditions.add(services.getTermBuilder().not(term));
         }
         Term conditionsTerm = services.getTermBuilder().or(negatedGoalConditions);
         Term goalCondition = services.getTermBuilder().or(conditionsTerm, triple.first);
         goalCondition = SymbolicExecutionUtil.replaceSkolemConstants(triple.third.sequent(), goalCondition, services);
         goalCondtions = goalCondtions.append(goalCondition);
      }
      return services.getTermBuilder().and(goalCondtions);
   }
   
   /**
    * <p>
    * Starts the side proof and extracts the result {@link Term} and conditions.
    * </p>
    * <p>
    * New used names are automatically added to the {@link Namespace} of the {@link Services}.
    * </p>
    * @param services The {@link Services} to use.
    * @param proof The {@link Proof} from on which the side proof si performed.
    * @param sequentToProve The {@link Sequent} to prove in a side proof.
    * @param operator The {@link Operator} which is used to compute the result.
    * @param description The side proof description.
    * @param splittingOption The splitting options to use.
    * @param addNamesToServices {@code true} defines that used names in result and conditions are added to the namespace of the given {@link Services}, {@code false} means that names are not added.
    * @return The found result {@link Term} and the conditions.
    * @throws ProofInputException Occurred Exception.
    */
   public static List<Triple<Term, Set<Term>, Node>> computeResultsAndConditions(Services services, 
                                                                                 Proof proof, 
                                                                                 Sequent sequentToProve, 
                                                                                 Operator operator, 
                                                                                 String description,
                                                                                 String methodTreatment,
                                                                                 String loopTreatment,
                                                                                 String queryTreatment,
                                                                                 String splittingOption,
                                                                                 boolean addNamesToServices) throws ProofInputException {
      // Execute side proof
      ApplyStrategyInfo info = SideProofUtil.startSideProof(proof, sequentToProve, methodTreatment, loopTreatment, queryTreatment, splittingOption, true);
      try {
         // Extract relevant things
         Set<Operator> relevantThingsInSequentToProve = SideProofUtil.extractRelevantThings(info.getProof().getServices(), sequentToProve);
         // Extract results and conditions from side proof
         List<Triple<Term, Set<Term>, Node>> conditionsAndResultsMap = new LinkedList<Triple<Term, Set<Term>, Node>>();
         for (Goal resultGoal : info.getProof().openGoals()) {
            if (SymbolicExecutionUtil.hasApplicableRules(resultGoal)) {
               throw new IllegalStateException("Not all applicable rules are applied.");
            }
            Sequent sequent = resultGoal.sequent();
            boolean newPredicateIsSequentFormula = isOperatorASequentFormula(sequent, operator);
            Set<Term> resultConditions = new LinkedHashSet<Term>();
            Term result = null;
            for (SequentFormula sf : sequent.antecedent()) {
               if (newPredicateIsSequentFormula) {
                  if (sf.formula().op() == operator) {
                     throw new IllegalStateException("Result predicate found in antecedent.");
                  }
                  else {
                     Term constructedResult = constructResultIfContained(services, sf, operator);
                     if (constructedResult != null) {
                        throw new IllegalStateException("Result predicate found in antecedent.");
                     }
                  }
               }
               if (!SideProofUtil.isIrrelevantCondition(services, sequentToProve, relevantThingsInSequentToProve, sf)) {
                  if (resultConditions.add(sf.formula()) && addNamesToServices) {
                     addNewNamesToNamespace(services, sf.formula());
                  }
               }
            }
            for (SequentFormula sf : sequent.succedent()) {
               if (newPredicateIsSequentFormula) {
                  if (sf.formula().op() == operator) {
                     if (result != null) {
                        throw new IllegalStateException("Result predicate found multiple times in succedent.");
                     }
                     result = sf.formula().sub(0);
                  }
               }
               else {
                  Term constructedResult = constructResultIfContained(services, sf, operator);
                  if (constructedResult != null) {
                     if (result != null) {
                        throw new IllegalStateException("Result predicate found multiple times in succedent.");
                     }
                     result = constructedResult;
                  }
               }
               if (result == null) {
                  if (!SideProofUtil.isIrrelevantCondition(services, sequentToProve, relevantThingsInSequentToProve, sf)) {
                     if (resultConditions.add(services.getTermBuilder().not(sf.formula())) && addNamesToServices) {
                        addNewNamesToNamespace(services, sf.formula());
                     }
                  }
               }
            }
            if (result == null) {
               result = services.getTermBuilder().ff();
            }
            conditionsAndResultsMap.add(new Triple<Term, Set<Term>, Node>(result, resultConditions, resultGoal.node()));
         }
         return conditionsAndResultsMap;
      }
      finally {
         SideProofUtil.disposeOrStore(description, info);
      }
   }
   
   private static Term constructResultIfContained(Services services, SequentFormula sf, Operator operator) {
      return constructResultIfContained(services, sf.formula(), operator);
   }
   
   private static Term constructResultIfContained(Services services, Term term, Operator operator) {
      if (term.op() == operator) {
         return term.sub(0);
      }
      else {
         Term result = null;
         int i = 0;
         while (result == null && i < term.arity()) {
            result = constructResultIfContained(services, term.sub(i), operator);
            i++;
         }
         if (result != null) {
            List<Term> newSubs = new LinkedList<Term>();
            for (int j = 0; j < term.arity(); j++) {
               if (j == i - 1) {
                  newSubs.add(result);
               }
               else {
                  newSubs.add(term.sub(j));
               }
            }
            result = services.getTermFactory().createTerm(term.op(), new ImmutableArray<Term>(newSubs), term.boundVars(), term.javaBlock(), term.getLabels());
         }
         return result;
      }
   }

   private static boolean isOperatorASequentFormula(Sequent sequent, final Operator operator) {
      return JavaUtil.search(sequent, new IFilter<SequentFormula>() {
         @Override
         public boolean select(SequentFormula element) {
            return element.formula().op() == operator;
         }
      }) != null;
   }

   /**
    * Makes sure that all used {@link Name}s in the given {@link Term}
    * are registered in the {@link Namespace}s of the given {@link Services}.
    * @param services The {@link Services} to use.
    * @param term The {@link Term} to check its {@link Name}s.
    */
   public static void addNewNamesToNamespace(Services services, Term term) {
      final Namespace functions = services.getNamespaces().functions();
      final Namespace progVars = services.getNamespaces().programVariables();
      // LogicVariables are always local bound
      term.execPreOrder(new DefaultVisitor() {
         @Override
         public void visit(Term visited) {
            if (visited.op() instanceof Function) {
               functions.add(visited.op());
            }
            else if (visited.op() instanceof IProgramVariable) {
               progVars.add(visited.op());
            }
         }
      });
   }
   
   /**
    * Checks if the given {@link SequentFormula} contains a modality or query.
    * @param sf The {@link SequentFormula} to check.
    * @return {@code true} contains at least one modality or query, {@code false} contains no modalities and no queries.
    */
   public static boolean containsModalityOrQuery(SequentFormula sf) {
      return containsModalityOrQuery(sf.formula());
   }

   /**
    * Checks if the given {@link Term} contains a modality or query.
    * @param term The {@link Term} to check.
    * @return {@code true} contains at least one modality or query, {@code false} contains no modalities and no queries.
    */
   public static boolean containsModalityOrQuery(Term term) {
      ContainsModalityOrQueryVisitor visitor = new ContainsModalityOrQueryVisitor();
      term.execPostOrder(visitor);
      return visitor.isContainsModalityOrQuery();
   } 
   
   /**
    * Utility method used by {@link QuerySideProofRule#containsModalityOrQuery(Term)}.
    * @author Martin Hentschel
    */
   protected static class ContainsModalityOrQueryVisitor extends DefaultVisitor {
      /**
       * The result.
       */
      boolean containsModalityOrQuery = false;

      /**
       * {@inheritDoc}
       */
      @Override
      public void visit(Term visited) {
         if (visited.op() instanceof Modality) {
            containsModalityOrQuery = true;
         }
         else if (visited.op() instanceof IProgramMethod) {
            containsModalityOrQuery = true;
         }
      }

      /**
       * Returns the result.
       * @return {@code true} contains at least one modality or query, {@code false} contains no modalities and no queries.
       */
      public boolean isContainsModalityOrQuery() {
         return containsModalityOrQuery;
      }
   }
   
   /**
    * Extracts all {@link Operator}s from the given {@link Sequent} which
    * represents relevant things.
    * @param services The {@link Services} to use.
    * @param sequentToProve The {@link Sequent} to extract relevant things from.
    * @return The found relevant things.
    */
   public static Set<Operator> extractRelevantThings(final Services services, 
                                                     Sequent sequentToProve) {
      final Set<Operator> result = new HashSet<Operator>();
      for (SequentFormula sf : sequentToProve) {
         sf.formula().execPreOrder(new DefaultVisitor() {
            @Override
            public void visit(Term visited) {
               if (isRelevantThing(services, visited)) {
                  result.add(visited.op());
               }
            }
         });
      }
      return result;
   }
   
   /**
    * Checks if the given {@link Term} describes a relevant thing. 
    * Relevant things are:
    * <ul>
    *    <li>IProgramVariable</li>
    *    <li>Functions of type Heap</li>
    *    <li>Functions of a Java type</li>
    * </ul>
    * @param services The {@link Services} to use.
    * @param term The {@link Term} to check.
    * @return {@code true} is relevant thing, {@code false} is not relevant.
    */
   protected static boolean isRelevantThing(Services services, Term term) {
      if (term.op() instanceof IProgramVariable) {
         return true;
      }
      else if (term.op() instanceof Function) {
         HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
         if (SymbolicExecutionUtil.isHeap(term.op(), heapLDT)) {
            return true;
         }
         else {
            KeYJavaType kjt = services.getJavaInfo().getKeYJavaType(term.sort());
            return kjt != null;
         }
      }
      else {
         return false;
      }
   }

   /**
    * Checks if the given {@link SequentFormula} is a relevant condition.
    * @param services The {@link Services} to use.
    * @param initialSequent The initial {@link Sequent} of the side proof.
    * @param relevantThingsInSequentToProve The relevant things found in the initial {@link Sequent}.
    * @param sf The {@link SequentFormula} to check.
    * @return {@code true} {@link SequentFormula} is relevant condition, {@code false} {@link SequentFormula} is not a relevant condition.
    */
   public static boolean isIrrelevantCondition(Services services, 
                                               Sequent initialSequent, 
                                               Set<Operator> relevantThingsInSequentToProve, 
                                               SequentFormula sf) {
      return initialSequent.antecedent().contains(sf) || // Conditions which already exist in the initial sequent are irrelevant
             initialSequent.succedent().contains(sf) || // Conditions which already exist in the initial sequent are irrelevant
//             isInOrOfAntecedent(initialSequent, sf) ||
             containsModalityOrQuery(sf) || // Conditions with modalities or queries are irrelevant
             containsIrrelevantThings(services, sf, relevantThingsInSequentToProve); // Conditions which contains not relevant things are irrelevant
   }

//   public static boolean isInOrOfAntecedent(Sequent initialSequent, SequentFormula sf) {
//      Term term = sf.formula();
//      boolean result = false;
//      Iterator<SequentFormula> iter = initialSequent.antecedent().iterator();
//      while (!result && iter.hasNext()) {
//         SequentFormula next = iter.next();
//         if (isInOr(next.formula(), term)) {
//            result = true;
//         }
//      }
//      return result;
//   }
//
//   public static boolean isInOr(Term term, Term toCheck) {
//      if (term.op() == Junctor.OR) {
//         boolean result = false;
//         Iterator<Term> iter = term.subs().iterator();
//         while (!result && iter.hasNext()) {
//            result = isInOr(iter.next(), toCheck);
//         }
//         return result;
//      }
//      else {
//         return term == toCheck;
//      }
//   }

   /**
    * Checks if the given {@link SequentFormula} contains irrelevant things
    * (things which are not contained in the relevantThingsToProve and are no Java types)
    * @param services The {@link Services} to use.
    * @param sf The {@link SequentFormula} to check.
    * @param relevantThings The relevant things.
    * @return {@code true} The {@link SequentFormula} contains irrelevant things, {@code false} the {@link SequentFormula} contains no irrelevant things.
    */
   public static boolean containsIrrelevantThings(Services services,
                                              SequentFormula sf,
                                              Set<Operator> relevantThings) {
      ContainsIrrelevantThingsVisitor visitor = new ContainsIrrelevantThingsVisitor(services, relevantThings);
      sf.formula().execPostOrder(visitor);
      return visitor.isContainsIrrelevantThings();
   }
   
   /**
    * Utility class used by {@link QuerySideProofRule#containsIrrelevantThings(Services, SequentFormula, Set)}.
    * @author Martin Hentschel
    */
   protected static class ContainsIrrelevantThingsVisitor extends DefaultVisitor {
      /**
       * The {@link Services} to use.
       */
      private Services services;
      
      /**
       * The relevant things.
       */
      private Set<Operator> relevantThings;
      
      /**
       * The result.
       */
      boolean containsIrrelevantThings = false;
      
      /**
       * Constructor.
       * @param services The {@link Services} to use.
       * @param relevantThings The relevant things.
       */
      public ContainsIrrelevantThingsVisitor(Services services, Set<Operator> relevantThings) {
         this.services = services;
         this.relevantThings = relevantThings;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void visit(Term visited) {
         if (isRelevantThing(services, visited)) {
            if (!SymbolicExecutionUtil.isSelect(services, visited) &&
                !SymbolicExecutionUtil.isBoolean(services, visited.op()) &&
                !SymbolicExecutionUtil.isNumber(visited.op())) {
               if (!relevantThings.contains(visited.op())) {
                  containsIrrelevantThings = true;
               }
            }
         }
      }

      /**
       * Returns the result.
       * @return The {@link SequentFormula} contains irrelevant things, {@code false} the {@link SequentFormula} contains no irrelevant things.
       */
      public boolean isContainsIrrelevantThings() {
         return containsIrrelevantThings;
      }
   }
   
   /**
    * Starts a site proof for the given {@link Sequent}.
    * @param proof The parent {@link Proof} of the site proof to do.
    * @param sequentToProve The {@link Sequent} to prove.
    * @return The proof result represented as {@link ApplyStrategyInfo} instance.
    * @throws ProofInputException Occurred Exception
    */
   public static ApplyStrategyInfo startSideProof(Proof proof,
                                                  Sequent sequentToProve,
                                                  boolean useSimplifyTermProfile) throws ProofInputException {
      return startSideProof(proof, 
                            sequentToProve, 
                            StrategyProperties.METHOD_NONE,
                            StrategyProperties.LOOP_NONE,
                            StrategyProperties.QUERY_OFF,
                            StrategyProperties.SPLITTING_OFF,
                            useSimplifyTermProfile);
   }
   
   /**
    * Starts a site proof for the given {@link Sequent}.
    * @param proof The parent {@link Proof} of the site proof to do.
    * @param sequentToProve The {@link Sequent} to prove.
    * @return The proof result represented as {@link ApplyStrategyInfo} instance.
    * @throws ProofInputException Occurred Exception
    */
   public static ApplyStrategyInfo startSideProof(Proof proof,
                                                  Sequent sequentToProve,
                                                  String methodTreatment,
                                                  String loopTreatment,
                                                  String queryTreatment,
                                                  String splittingOption,
                                                  boolean useSimplifyTermProfile) throws ProofInputException {
      ProofStarter starter = createSideProof(proof, sequentToProve, useSimplifyTermProfile);
      return startSideProof(proof, starter, methodTreatment, loopTreatment, queryTreatment, splittingOption);
   }
   
   /**
    * Creates a new {@link ProofStarter} which contains a new site proof
    * of the given {@link Proof}.
    * @param proof The given {@link Proof}.
    * @param sequentToProve The {@link Sequent} to proof in a new site proof.
    * @return The created {@link ProofStarter} with the site proof.
    * @throws ProofInputException Occurred Exception.
    */
   public static ProofStarter createSideProof(Proof proof,
                                              Sequent sequentToProve, 
                                              boolean useSimplifyTermProfile) throws ProofInputException {
      // Make sure that valid parameters are given
      assert sequentToProve != null;
      // Create ProofStarter
      ProofStarter starter = new ProofStarter(false);
      // Configure ProofStarter
      ProofEnvironment env = cloneProofEnvironmentWithOwnOneStepSimplifier(proof, useSimplifyTermProfile); // New OneStepSimplifier is required because it has an internal state and the default instance can't be used parallel.
      starter.init(sequentToProve, env);
      return starter;
   }
   
   /**
    * Starts a site proof.
    * @param proof The original {@link Proof}.
    * @param starter The {@link ProofStarter} with the site proof.
    * @param splittingOption The splitting option to use.
    * @return The site proof result.
    */
   public static ApplyStrategyInfo startSideProof(Proof proof, 
                                                  ProofStarter starter,
                                                  String methodTreatment,
                                                  String loopTreatment,
                                                  String queryTreatment,
                                                  String splittingOption) {
      assert starter != null;
      starter.setMaxRuleApplications(10000);
      StrategyProperties sp = !proof.isDisposed() ? 
                              proof.getSettings().getStrategySettings().getActiveStrategyProperties() : // Is a clone that can be modified
                              new StrategyProperties();
      SymbolicExecutionStrategy.setDefaultStrategyProperties(sp, false, true, true, false, false);
      sp.setProperty(StrategyProperties.METHOD_OPTIONS_KEY, methodTreatment);
      sp.setProperty(StrategyProperties.LOOP_OPTIONS_KEY, loopTreatment);
      sp.setProperty(StrategyProperties.QUERY_OPTIONS_KEY, queryTreatment);
      sp.setProperty(StrategyProperties.SPLITTING_OPTIONS_KEY, splittingOption);
      sp.setProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY, StrategyProperties.QUANTIFIERS_NON_SPLITTING);
      starter.setStrategy(sp);
      // Execute proof in the current thread
      return starter.start();
   }

   /**
    * Extracts the value for the formula with the given {@link Operator}
    * from the given {@link Goal}.
    * @param goal The {@link Goal} to search the {@link Operator} in.
    * @param operator The {@link Operator} for the formula which should be extracted.
    * @return The value of the formula with the given {@link Operator}.
    */
   public static Term extractOperatorValue(Goal goal, final Operator operator) {
      assert goal != null;
      return extractOperatorValue(goal.node(), operator);
   }

   /**
    * Extracts the value for the formula with the given {@link Operator}
    * from the given {@link Node}.
    * @param node The {@link Node} to search the {@link Operator} in.
    * @param operator The {@link Operator} for the formula which should be extracted.
    * @return The value of the formula with the given {@link Operator}.
    */
   public static Term extractOperatorValue(Node node, final Operator operator) {
      Term operatorTerm = extractOperatorTerm(node, operator);
      return operatorTerm != null ? operatorTerm.sub(0) : null;
   }
   
   /**
    * Extracts the operator term for the formula with the given {@link Operator}
    * from the site proof result ({@link ApplyStrategyInfo}).
    * @param info The site proof result.
    * @param operator The {@link Operator} for the formula which should be extracted.
    * @return The operator term of the formula with the given {@link Operator}.
    * @throws ProofInputException Occurred Exception.
    */
   public static Term extractOperatorTerm(ApplyStrategyInfo info, Operator operator) throws ProofInputException {
      // Make sure that valid parameters are given
      assert info != null;
      if (info.getProof().openGoals().size() != 1) {
         throw new ProofInputException("Assumption that return value extraction has one goal does not hold because " + info.getProof().openGoals().size() + " goals are available.");
      }
      // Get node of open goal
      return extractOperatorTerm(info.getProof().openGoals().head(), operator);
   }

   /**
    * Extracts the operator term for the formula with the given {@link Operator}
    * from the given {@link Goal}.
    * @param goal The {@link Goal} to search the {@link Operator} in.
    * @param operator The {@link Operator} for the formula which should be extracted.
    * @return The operator term of the formula with the given {@link Operator}.
    */
   public static Term extractOperatorTerm(Goal goal, final Operator operator) {
      assert goal != null;
      return extractOperatorTerm(goal.node(), operator);
   }

   /**
    * Extracts the operator term for the formula with the given {@link Operator}
    * from the given {@link Node}.
    * @param node The {@link Node} to search the {@link Operator} in.
    * @param operator The {@link Operator} for the formula which should be extracted.
    * @return The operator term of the formula with the given {@link Operator}.
    */
   public static Term extractOperatorTerm(Node node, final Operator operator) {
      assert node != null;
      // Search formula with the given operator in sequent
      SequentFormula sf = JavaUtil.search(node.sequent(), new IFilter<SequentFormula>() {
         @Override
         public boolean select(SequentFormula element) {
            return JavaUtil.equals(element.formula().op(), operator);
         }
      });
      if (sf != null) {
         return sf.formula();
      }
      else {
         return null;
      }
   }
   
   /**
    * Creates a copy of the {@link ProofEnvironment} of the given {@link Proof}
    * which has his own {@link OneStepSimplifier} instance. Such copies are
    * required for instance during parallel usage of site proofs because
    * {@link OneStepSimplifier} has an internal state.
    * @param source The {@link Proof} to copy its {@link ProofEnvironment}.
    * @return The created {@link ProofEnvironment} which is a copy of the environment of the given {@link Proof} but with its own {@link OneStepSimplifier} instance.
    */
   public static ProofEnvironment cloneProofEnvironmentWithOwnOneStepSimplifier(Proof source, boolean useSimplifyTermProfile) {
      assert source != null;
      assert !source.isDisposed();
      // Get required source instances
      final ProofEnvironment sourceEnv = source.env();
      InitConfig sourceInitConfig = sourceEnv.getInitConfig();
      RuleJustificationInfo sourceJustiInfo = sourceEnv.getJustifInfo();
      // Create new profile which has separate OneStepSimplifier instance
      JavaProfile profile;
      if (useSimplifyTermProfile) {
         profile = new SimplifyTermProfile() {
            @Override
            protected ImmutableList<TermLabelConfiguration> computeTermLabelConfiguration() {
               Profile sourceProfile = sourceEnv.getInitConfig().getProfile();
               if (sourceProfile instanceof SymbolicExecutionJavaProfile) {
                  ImmutableList<TermLabelConfiguration> result = super.computeTermLabelConfiguration();
                  result = result.prepend(SymbolicExecutionJavaProfile.getSymbolicExecutionTermLabelConfigurations()); // Make sure that the term labels of symbolic execution are also supported by the new environment.
                  return result;
               }
               else {
                  return super.computeTermLabelConfiguration();
               }
            }
         };
      }
      else {
         profile = new JavaProfile() {
            @Override
            protected ImmutableList<TermLabelConfiguration> computeTermLabelConfiguration() {
               Profile sourceProfile = sourceEnv.getInitConfig().getProfile();
               if (sourceProfile instanceof SymbolicExecutionJavaProfile) {
                  ImmutableList<TermLabelConfiguration> result = super.computeTermLabelConfiguration();
                  result = result.prepend(SymbolicExecutionJavaProfile.getSymbolicExecutionTermLabelConfigurations()); // Make sure that the term labels of symbolic execution are also supported by the new environment.
                  return result;
               }
               else {
                  return super.computeTermLabelConfiguration();
               }
            }
         };
      }
      // Create new InitConfig
      InitConfig initConfig = new InitConfig(source.getServices().copy(profile, true));
      // Set modified taclet options in which runtime exceptions are banned.
      ImmutableSet<Choice> choices = sourceInitConfig.getActivatedChoices();
      choices = choices.remove(new Choice("allow", "runtimeExceptions"));
      choices = choices.add(new Choice("ban", "runtimeExceptions"));
      initConfig.setActivatedChoices(choices);
      // Initialize InitConfig with settings from the original InitConfig.
      initConfig.setSettings(sourceInitConfig.getSettings());
      initConfig.setTaclet2Builder(sourceInitConfig.getTaclet2Builder());
      initConfig.setTaclets(sourceInitConfig.getTaclets());
      // Create new ProofEnvironment and initialize it with values from initial one.
      ProofEnvironment env = new ProofEnvironment(initConfig);
      env.setJavaModel(sourceEnv.getJavaModel());
      env.setNumber(sourceEnv.getNumber());
      env.setRuleConfig(sourceEnv.getRuleConfig());
      for (Taclet taclet : initConfig.activatedTaclets()) {
         env.getJustifInfo().addJustification(taclet, sourceJustiInfo.getJustification(taclet));
      }
      for (BuiltInRule rule : initConfig.builtInRules()) {
         RuleJustification origJusti = sourceJustiInfo.getJustification(rule);
         if (origJusti == null) {
            assert rule instanceof OneStepSimplifier;
            origJusti = AxiomJustification.INSTANCE;
         }
         env.getJustifInfo().addJustification(rule, origJusti);
      }
      return env;
   }

   /**
    * <p>
    * Stores or disposes the {@link Proof} of the {@link ApplyStrategyInfo} in {@link SideProofStore#DEFAULT_INSTANCE}.
    * </p>
    * <p>
    * This method should be called whenever a side proof is no longer needed
    * and should be disposed or stored for debugging purposes.
    * </p>
    * @param description The description.
    * @param info The {@link ApplyStrategyInfo} to store or dispose its {@link Proof}.
    */
   public static void disposeOrStore(String description, ApplyStrategyInfo info) {
      if (info != null) {
         if (SideProofStore.DEFAULT_INSTANCE.isEnabled()) {
            SideProofStore.DEFAULT_INSTANCE.addProof(description, info.getProof());
         }
         else {
            info.getProof().dispose();
         }
      }
   }
}
