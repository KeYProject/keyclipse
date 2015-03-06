package org.key_project.jmlediting.core.test.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ SpecificationStatementParserTest.class,
      BehaviorParserTest.class, LexicalHelperTest.class,
      StoreRefParserTest.class, ParserBuilderTest.class,
      ParserRecoveryTest.class, ExpressionParserTest.class,
      QuantifierParserTest.class, RequiresParserTest.class,
      InvariantForTest.class, MeasuredByTest.class, SignalsTest.class,
      SignalsOnlyTest.class })
public class ParserTestSuite {

}
