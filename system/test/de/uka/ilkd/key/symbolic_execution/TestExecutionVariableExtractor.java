package de.uka.ilkd.key.symbolic_execution;

/**
 * Tests for {@link ExecutionVariableExtractor}.
 * @author Martin Hentschel
 */
public class TestExecutionVariableExtractor extends AbstractSymbolicExecutionTestCase {
   /**
    * Tests example: examples/_testcase/set/variablesConditionalCycle
    */
   public void testVariablesConditionalCycle() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesConditionalCycle/test/VariablesConditionalCycle.java", 
                "VariablesConditionalCycle", 
                "main", 
                null,
                "examples/_testcase/set/variablesConditionalCycle/oracle/VariablesConditionalCycle.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variablesSimpleCycle
    */
   public void testVariablesSimpleCycle() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesSimpleCycle/test/VariablesSimpleCycle.java", 
                "VariablesSimpleCycle", 
                "main", 
                "something != null",
                "examples/_testcase/set/variablesSimpleCycle/oracle/VariablesSimpleCycle.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variablesWithQuantifier
    */
   public void testVariablesWithQuantifier() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesWithQuantifier/test/EnoughInfoReturn.java", 
                "EnoughInfoReturn", 
                "passwordChecker", 
                "passwords != null",
                "examples/_testcase/set/variablesWithQuantifier/oracle/EnoughInfoReturn.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variablesVariableArrayIndex
    */
   public void testVariableArrayIndex() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesVariableArrayIndex/test/VariableArrayIndex.java", 
                "VariableArrayIndex", 
                "magic", 
                "array != null && array.length >= 1 && index >= 0 && index < array.length",
                "examples/_testcase/set/variablesVariableArrayIndex/oracle/VariableArrayIndex.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }

   /**
    * Tests example: examples/_testcase/set/variablesConditionalValuesTest
    */
   public void testVariablesConditionalValuesTest_next() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesConditionalValuesTest/test/ConditionalValuesTest.java", 
                "ConditionalValuesTest", 
                "mainNext", 
                null,
                "examples/_testcase/set/variablesConditionalValuesTest/oracle/ConditionalValuesTest_next.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variablesConditionalValuesTest
    */
   public void testVariablesConditionalValuesTest() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesConditionalValuesTest/test/ConditionalValuesTest.java", 
                "ConditionalValuesTest", 
                "main", 
                null,
                "examples/_testcase/set/variablesConditionalValuesTest/oracle/ConditionalValuesTest.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variableVariablesArrayTest
    */
   public void testVariableVariablesArrayTest() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variableVariablesArrayTest/test/VariablesArrayTest.java", 
                "VariablesArrayTest", 
                "arrayTest", 
                null,
                "examples/_testcase/set/variableVariablesArrayTest/oracle/VariablesArrayTest.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variablesLocalVariablesTest
    */
   public void testVariablesLocalVariablesTest() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesLocalVariablesTest/test/LocalVariablesTest.java", 
                "LocalVariablesTest", 
                "main", 
                null,
                "examples/_testcase/set/variablesLocalVariablesTest/oracle/LocalVariablesTest.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
   
   /**
    * Tests example: examples/_testcase/set/variablesUpdateVariablesTest
    */
   public void testUpdateVariablesTest() throws Exception {
      doSETTest(keyRepDirectory, 
                "examples/_testcase/set/variablesUpdateVariablesTest/test/UpdateVariablesTest.java", 
                "UpdateVariablesTest", 
                "main", 
                null,
                "examples/_testcase/set/variablesUpdateVariablesTest/oracle/UpdateVariablesTest.xml",
                false,
                true,
                false,
                false,
                DEFAULT_MAXIMAL_SET_NODES_PER_RUN,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true);
   }
}
