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


package de.uka.ilkd.key.proof;

import junit.framework.TestCase;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Semisequent;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.proof.BuiltInRuleAppIndex;
import de.uka.ilkd.key.proof.BuiltInRuleIndex;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.RuleAppIndex;
import de.uka.ilkd.key.proof.TacletAppIndex;
import de.uka.ilkd.key.proof.TacletIndex;
import de.uka.ilkd.key.rule.TacletForTests;

/** class tests the goal, especially the split and set back mechanism. */

public class TestGoal extends TestCase {

        Proof proof;

        public TestGoal(String name) {
                super(name);
        }

        public void setUp() {
                TacletForTests.parse();
                proof = new Proof(new Services());

        }

        public void tearDown() {
                proof = null;
        }

        public void testSetBack0() {
                Sequent seq = Sequent
                                .createSuccSequent(Semisequent.EMPTY_SEMISEQUENT
                                                .insert(0,
                                                                new SequentFormula(
                                                                                TacletForTests.parseTerm("A")))
                                                .semisequent());

                Node root = new Node(proof, seq);
                proof.setRoot(root);
                Goal g = new Goal(root, new RuleAppIndex(new TacletAppIndex(
                                new TacletIndex()), new BuiltInRuleAppIndex(
                                new BuiltInRuleIndex())));
                ImmutableList<Goal> lg = g.split(3);
                lg.head().addNoPosTacletApp(
                                TacletForTests.getRules().lookup("imp_right"));
                lg.tail()
                                .head()
                                .addNoPosTacletApp(
                                                TacletForTests.getRules()
                                                                .lookup("imp_left"));
                lg.tail()
                                .tail()
                                .head()
                                .addNoPosTacletApp(
                                                TacletForTests.getRules()
                                                                .lookup("or_right"));
                // just check if the test is trivially correct because of rules
                // not found
                assertNotNull(lg.head().indexOfTaclets().lookup("imp_right"));

                ImmutableList<Goal> lg0 = lg.head().split(3);
                ImmutableList<Goal> lg00 = lg0.tail().head().split(8);
                ImmutableList<Goal> lg1 = lg.tail().tail().head().split(2);
                proof.add(lg1.append(lg00).append(lg0.head())
                                .append(lg0.tail().tail().head())
                                .append(lg.tail().head()));
                proof.pruneProof(lg.tail().head());
                assertTrue(proof.openGoals().size() == 1);
                assertNull("Taclet Index of set back goal contains rule \"imp-right\" that were not "
                                + "there before", proof.openGoals().head()
                                .indexOfTaclets().lookup("imp_right"));
                assertNull("Taclet Index of set back goal contains rule \"or-right\"that were not "
                                + "there before", proof.openGoals().head()
                                .indexOfTaclets().lookup("or_right"));
                assertNull("Taclet Index of set back goal contains rule \"imp-left\" that were not "
                                + "there before", proof.openGoals().head()
                                .indexOfTaclets().lookup("imp_left"));

        }

        public void testSetBack1() {
                Sequent seq = Sequent
                                .createSuccSequent(Semisequent.EMPTY_SEMISEQUENT
                                                .insert(0,
                                                                new SequentFormula(
                                                                                TacletForTests.parseTerm("A")))
                                                .semisequent());
                Node root = new Node(proof, seq);
                proof.setRoot(root);
                Goal g = new Goal(
                                root,
                                new RuleAppIndex(
                                                new TacletAppIndex(
                                                                new TacletIndex()),
                                                new BuiltInRuleAppIndex(
                                                                new BuiltInRuleIndex())));
                ImmutableList<Goal> lg = g.split(3);
                lg.head().addNoPosTacletApp(
                                TacletForTests.getRules().lookup("imp_right"));
                lg.tail()
                                .head()
                                .addNoPosTacletApp(
                                                TacletForTests.getRules()
                                                                .lookup("imp_left"));
                lg.tail()
                                .tail()
                                .head()
                                .addNoPosTacletApp(
                                                TacletForTests.getRules()
                                                                .lookup("or_right"));
                // just check if the test is trivially correct because of rules
                // not found
                assertNotNull(lg.head().indexOfTaclets().lookup("imp_right"));

                ImmutableList<Goal> lg0 = lg.head().split(4);
                lg0.head().addNoPosTacletApp(
                                TacletForTests.getRules().lookup("or_left"));
                lg0.tail()
                                .head()
                                .addNoPosTacletApp(
                                                TacletForTests.getRules()
                                                                .lookup("or_left"));
                ImmutableList<Goal> lg1 = lg.tail().tail().head().split(2);
                proof.add(lg1.append(lg0).append(lg.tail().head()));
                proof.pruneProof(lg0.tail().head());

                 assertTrue(proof.openGoals().size()==4);

                 assertTrue(proof.openGoals().contains(lg1.head()));
                 assertNotNull(lg1.head().indexOfTaclets().lookup("or_right"));
                 //
                 assertTrue(lg1.head().indexOfTaclets().lookup("or_left")==null);
                 proof.remove2(lg1.head());


                 assertTrue(proof.openGoals().contains(lg1.tail().head()));
                 assertNotNull(lg1.tail().head().indexOfTaclets().lookup("or_right"));
                 //
                 assertTrue(lg1.tail().head().indexOfTaclets().lookup("or_left")==null);
                 proof.remove2(lg1.tail().head());

                 if (proof.openGoals().head().indexOfTaclets().lookup("imp_right")!=null) {
                 assertNotNull
                 (proof.openGoals().tail().head().indexOfTaclets().lookup("imp_left"));
                 } else {
                 assertNotNull
                 (proof.openGoals().head().indexOfTaclets().lookup("imp_left"));
                 assertNotNull
                 (proof.openGoals().tail().head().indexOfTaclets().lookup("imp_right"));
                 }
                 assertNull(proof.openGoals().head().indexOfTaclets().lookup("or_left"));
                 assertNull(proof.openGoals().tail().head().indexOfTaclets().lookup("or_left"));

        }

}
