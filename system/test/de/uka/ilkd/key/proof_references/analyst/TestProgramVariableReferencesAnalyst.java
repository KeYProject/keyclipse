package de.uka.ilkd.key.proof_references.analyst;

import de.uka.ilkd.key.proof_references.AbstractProofReferenceTestCase;
import de.uka.ilkd.key.proof_references.reference.IProofReference;

/**
 * Tests for {@link ProgramVariableReferencesAnalyst}.
 * @author Martin Hentschel
 */
public class TestProgramVariableReferencesAnalyst extends AbstractProofReferenceTestCase {
   /**
    * Tests "ConditionalsAndOther#forEquals".
    */
   public void testConditionalsAndOther_forEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "forEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#forBoolean".
    */
   public void testConditionalsAndOther_forBoolean() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "forBoolean", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#doWhileEquals".
    */
   public void testConditionalsAndOther_doWhileEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "doWhileEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#doWhileBoolean".
    */
   public void testConditionalsAndOther_doWhileBoolean() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "doWhileBoolean", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#whileEquals".
    */
   public void testConditionalsAndOther_whileEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "whileEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#whileBoolean".
    */
   public void testConditionalsAndOther_whileBoolean() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "whileBoolean", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#throwsNestedException".
    */
   public void testConditionalsAndOther_throwsNestedException() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "throwsNestedException", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::ew"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther.ExceptionWrapper::wrapped"));
   }
   
   /**
    * Tests "ConditionalsAndOther#throwsException".
    */
   public void testConditionalsAndOther_throwsException() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "throwsException", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::e"));
   }
   
   /**
    * Tests "ConditionalsAndOther#methodCallCondtional".
    */
   public void testConditionalsAndOther_methodCallCondtional() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "methodCallCondtional", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#methodCallAssignment".
    */
   public void testConditionalsAndOther_methodCallAssignment() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "methodCallAssignment", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#methodCall".
    */
   public void testConditionalsAndOther_methodCall() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "methodCall", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#returnComplex".
    */
   public void testConditionalsAndOther_returnComplex() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "returnComplex", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#returnEquals".
    */
   public void testConditionalsAndOther_returnEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "returnEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#returnBoolean".
    */
   public void testConditionalsAndOther_returnBoolean() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "returnBoolean", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionIncrementsAndDecrements".
    */
   public void testConditionalsAndOther_questionIncrementsAndDecrements() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionIncrementsAndDecrements", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"));
   }
   
   /**
    * Tests "ConditionalsAndOther#ifIncrementsAndDecrements".
    */
   public void testConditionalsAndOther_ifIncrementsAndDecrements() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifIncrementsAndDecrements", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionGreater".
    */
   public void testConditionalsAndOther_questionGreater() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionGreater", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionGreaterEquals".
    */
   public void testConditionalsAndOther_questionGreaterEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionGreaterEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionNotEquals".
    */
   public void testConditionalsAndOther_questionNotEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionNotEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionEquals".
    */
   public void testConditionalsAndOther_questionEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionLessEquals".
    */
   public void testConditionalsAndOther_questionLessEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionLessEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionLess".
    */
   public void testConditionalsAndOther_questionLess() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionLess", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#questionBoolean".
    */
   public void testConditionalsAndOther_questionBoolean() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "questionBoolean", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#ifGreater".
    */
   public void testConditionalsAndOther_ifGreater() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifGreater", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }

   /**
    * Tests "ConditionalsAndOther#ifGreaterEquals".
    */
   public void testConditionalsAndOther_ifGreaterEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifGreaterEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }

   /**
    * Tests "ConditionalsAndOther#ifNotEquals".
    */
   public void testConditionalsAndOther_ifNotEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifNotEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }

   /**
    * Tests "ConditionalsAndOther#ifEquals".
    */
   public void testConditionalsAndOther_ifEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#ifLessEquals".
    */
   public void testConditionalsAndOther_ifLessEquals() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifLessEquals", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#ifLess".
    */
   public void testConditionalsAndOther_ifLess() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifLess", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::y"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ConditionalsAndOther#ifBoolean".
    */
   public void testConditionalsAndOther_ifBoolean() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "ifBoolean", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::b"));
   }
   
   /**
    * Tests "ConditionalsAndOther#switchInt".
    */
   public void testConditionalsAndOther_switchInt() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ConditionalsAndOther/ConditionalsAndOther.java", 
                      "ConditionalsAndOther", 
                      "switchInt", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ConditionalsAndOther::x"));
   }
   
   /**
    * Tests "ArrayLength".
    */
   public void testArrayLength() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/ArrayLength/ArrayLength.java", 
                      "ArrayLength", 
                      "main", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ArrayLength::length"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ArrayLength::array"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ArrayLength::my"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ArrayLength.MyClass::length"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "ArrayLength.MyClass::array"));
   }
   
   /**
    * Tests "Assignments#assignmentWithSelf".
    */
   public void testAssignments_assignmentWithSelf() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "assignmentWithSelf", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::next"));
   }
   
   /**
    * Tests "Assignments#nestedArray".
    */
   public void testAssignments_nestedArray() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "nestedArray", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::myClass"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyClass::child"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyChildClass::childArray"));
   }

   /**
    * Tests "Assignments#nestedValueAdd".
    */
   public void testAssignments_nestedValueAdd() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "nestedValueAdd", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::myClass"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyClass::child"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyChildClass::thirdChildValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyChildClass::anotherChildValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyChildClass::childValue"));
   }

   /**
    * Tests "Assignments#nestedValue".
    */
   public void testAssignments_nestedValue() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "nestedValue", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::myClass"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyClass::child"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyChildClass::anotherChildValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments.MyChildClass::childValue"));
   }

   /**
    * Tests "Assignments#assign".
    */
   public void testAssignments_assign() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "assign", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }

   /**
    * Tests "Assignments#mod".
    */
   public void testAssignments_mod() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "mod", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }

   /**
    * Tests "Assignments#div".
    */
   public void testAssignments_div() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "div", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }

   /**
    * Tests "Assignments#mul".
    */
   public void testAssignments_mul() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "mul", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }

   /**
    * Tests "Assignments#sub".
    */
   public void testAssignments_sub() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "sub", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }

   /**
    * Tests "Assignments#add".
    */
   public void testAssignments_add() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "add", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }
   
   /**
    * Tests "Assignments#decrementArrayBegin".
    */
   public void testAssignments_decrementArrayBegin() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "decrementArrayBegin", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::array"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"));
   }
   
   /**
    * Tests "Assignments#decrementArrayEnd".
    */
   public void testAssignments_decrementArrayEnd() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "decrementArrayEnd", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::array"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::anotherValue"));
   }
   
   /**
    * Tests "Assignments#incrementArrayBegin".
    */
   public void testAssignments_incrementArrayBegin() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "incrementArrayBegin", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::array"));
   }
   
   /**
    * Tests "Assignments#incrementArrayEnd".
    */
   public void testAssignments_incrementArrayEnd() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "incrementArrayEnd", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::array"));
   }
   
   /**
    * Tests "Assignments#decrementBegin".
    */
   public void testAssignments_decrementBegin() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "decrementBegin", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }
   
   /**
    * Tests "Assignments#decrementEnd".
    */
   public void testAssignments_decrementEnd() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "decrementEnd", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }
   
   /**
    * Tests "Assignments#incrementBegin".
    */
   public void testAssignments_incrementBegin() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "incrementBegin", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }
   
   /**
    * Tests "Assignments#incrementEnd".
    */
   public void testAssignments_incrementEnd() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/Assignments/Assignments.java", 
                      "Assignments", 
                      "incrementEnd", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "Assignments::value"));
   }
   
   /**
    * Tests "assignment".
    */
   public void testAssignment() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment/Assignment.java", 
                      "Assignment", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment.Assignment::b"), 
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment.Enum::b"));
   }
   
   /**
    * Tests "assignment_array2".
    */
   public void testAssignment_array2() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_array2/Assignment_array2.java", 
                      "Assignment_array2", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_array2.Assignment_array2::index"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_array2.Assignment_array2::as"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_array2.Assignment_array2::val"));
   }
   
   /**
    * Tests "assignment_read_attribute".
    */
   public void testAssignment_read_attribute() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_read_attribute/Assignment_read_attribute.java", 
                      "Assignment_read_attribute", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_read_attribute.Class::b"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_read_attribute.Class::a"));
   }
   
   /**
    * Tests "assignment_read_length".
    */
   public void testAssignment_read_length() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_read_length/Assignment_read_length.java", 
                      "Assignment_read_length", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_read_length.Assignment_read_length::b"));
   }
   
   /**
    * Tests "assignment_to_primitive_array_component".
    */
   public void testAssignment_to_primitive_array_component() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_to_primitive_array_component/Assignment_to_primitive_array_component.java", 
                      "Assignment_to_primitive_array_component", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_to_primitive_array_component.Assignment_to_primitive_array_component::index"),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_to_primitive_array_component.Assignment_to_primitive_array_component::bs"));
   }
   
   /**
    * Tests "assignment_to_reference_array_component".
    */
   public void testAssignment_to_reference_array_component() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_to_reference_array_component/Assignment_to_reference_array_component.java", 
                      "Assignment_to_reference_array_component", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_to_reference_array_component.Assignment_to_reference_array_component::bs"));
   }
   
   /**
    * Tests "assignment_write_attribute".
    */
   public void testAssignment_write_attribute() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_write_attribute/Assignment_write_attribute.java", 
                      "Assignment_write_attribute", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_write_attribute.Class::a"));
   }
   
   /**
    * Tests "assignment_write_static_attribute".
    */
   public void testAssignment_write_static_attribute() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/assignment_write_static_attribute/Assignment_write_static_attribute.java", 
                      "Assignment_write_static_attribute", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "assignment_write_static_attribute.Assignment_write_static_attribute::b"));
   }
   
   /**
    * Tests "activeUseStaticFieldReadAccess2".
    */
   public void testActiveUseStaticFieldReadAccess2() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/activeUseStaticFieldReadAccess2/ActiveUseStaticFieldReadAccess2.java", 
                      "ActiveUseStaticFieldReadAccess2", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "activeUseStaticFieldReadAccess2.Class::i"));
   }
   
   /**
    * Tests "activeUseStaticFieldWriteAccess2".
    */
   public void testActiveUseStaticFieldWriteAccess2() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/activeUseStaticFieldWriteAccess2/ActiveUseStaticFieldWriteAccess2.java", 
                      "ActiveUseStaticFieldWriteAccess2", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "activeUseStaticFieldWriteAccess2.Class::a"));
   }
   
   /**
    * Tests "eval_order_access4".
    */
   public void testEval_order_access4() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/eval_order_access4/Eval_order_access4.java", 
                      "Eval_order_access4", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "eval_order_access4.Class::a"));
   }
   
   /**
    * Tests "eval_order_array_access5".
    */
   public void testEval_order_array_access5() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/eval_order_array_access5/Eval_order_array_access5.java", 
                      "Eval_order_array_access5", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "eval_order_array_access5.Class::a"));
   }
   
   /**
    * Tests "variableDeclarationAssign".
    */
   public void testVariableDeclarationAssign() throws Exception {
      doReferenceTest(keyRepDirectory, 
                      "examples/_testcase/proofReferences/variableDeclarationAssign/VariableDeclarationAssign.java", 
                      "VariableDeclarationAssign", 
                      "caller", 
                      false,
                      new ProgramVariableReferencesAnalyst(),
                      new ExpectedProofReferences(IProofReference.ACCESS, "variableDeclarationAssign.VariableDeclarationAssign::a"));
   }
}
