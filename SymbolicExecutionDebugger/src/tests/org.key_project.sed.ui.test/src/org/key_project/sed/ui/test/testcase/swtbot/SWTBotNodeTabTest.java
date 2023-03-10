/*******************************************************************************
 * Copyright (c) 2014 Karlsruhe Institute of Technology, Germany
 *                    Technical University Darmstadt, Germany
 *                    Chalmers University of Technology, Sweden
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Technical University Darmstadt - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.key_project.sed.ui.test.testcase.swtbot;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;
import org.key_project.sed.core.model.ISEDebugTarget;
import org.key_project.sed.core.model.ISEMethodCall;
import org.key_project.sed.core.model.ISEMethodReturn;
import org.key_project.sed.core.model.ISEStatement;
import org.key_project.sed.core.model.ISEThread;
import org.key_project.util.test.util.SWTBotTabbedPropertyList;

/**
 * Tests the property page tab "Node".
 * @author Martin Hentschel
 */
public class SWTBotNodeTabTest extends AbstractSWTBotPropertyTabTest {
   /**
    * Tests the shown values and the existence of tab "Node".
    */
   @Test
   public void testValuesAndTabExistence() throws Exception {
      doFixedExampleTest(createFixedExampleSteps());
   }
   
   /**
    * Creates the test steps to execute.
    * @return The created test steps.
    */
   public static ITestSteps createFixedExampleSteps() {
      return new AbstractTestSteps() {
         @Override
         public void assertThread(SWTBotTree debugTree, SWTBotView propertiesView, SWTBotTabbedPropertyList tabs, ISEThread thread) throws Exception {
            assertTrue(tabs.selectTabItem("Node"));
            assertEquals("Fixed Example Thread", propertiesView.bot().text(0).getText());
            assertEquals("pc1", propertiesView.bot().text(1).getText());
         }
         
         @Override
         public void assertStatement(SWTBotTree debugTree, SWTBotView propertiesView, SWTBotTabbedPropertyList tabs, ISEStatement statement) throws Exception {
            assertTrue(tabs.selectTabItem("Node"));
            assertEquals("int x = 1;", propertiesView.bot().text(0).getText());
            assertEquals("pc2", propertiesView.bot().text(1).getText());
         }
         
         @Override
         public void assertDebugTarget(SWTBotTree debugTree, SWTBotView propertiesView, SWTBotTabbedPropertyList tabs, ISEDebugTarget target) throws Exception {
            assertFalse(tabs.hasTabItem("Node"));
         }

         @Override
         public void assertMethodReturn(SWTBotTree debugTree, SWTBotView propertiesView, SWTBotTabbedPropertyList tabs, ISEMethodReturn methodReturn) throws Exception {
            assertTrue(tabs.selectTabItem("Node"));
            assertEquals("return -1", propertiesView.bot().text(0).getText());
            assertEquals("pc14", propertiesView.bot().text(1).getText());
            assertEquals("A Return Condition", propertiesView.bot().text(2).getText());
         }

         @Override
         public void assertMethodCall(SWTBotTree debugTree, SWTBotView propertiesView, SWTBotTabbedPropertyList tabs, ISEMethodCall methodCall) throws Exception {
            assertTrue(tabs.selectTabItem("Node"));
            assertEquals("foo(result)", propertiesView.bot().text(0).getText());
            assertEquals("pc11", propertiesView.bot().text(1).getText());
         }
      };
   }
}