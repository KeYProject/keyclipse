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

package org.key_project.sed.core.test.testcase.swtbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.junit.Test;
import org.key_project.sed.core.annotation.ISEDAnnotationLink;
import org.key_project.sed.core.annotation.ISEDAnnotationType;
import org.key_project.sed.core.annotation.impl.CommentAnnotation;
import org.key_project.sed.core.annotation.impl.CommentAnnotationLink;
import org.key_project.sed.core.annotation.impl.CommentAnnotationType;
import org.key_project.sed.core.annotation.impl.SearchAnnotation;
import org.key_project.sed.core.annotation.impl.SearchAnnotationType;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.serialization.SEDXMLReader;
import org.key_project.sed.core.model.serialization.SEDXMLWriter;
import org.key_project.sed.core.test.util.TestSedCoreUtil;
import org.key_project.sed.core.util.SEDAnnotationUtil;
import org.key_project.sed.core.util.SEDPreferenceUtil;
import org.key_project.util.test.testcase.AbstractSetupTestCase;
import org.key_project.util.test.util.TestUtilsUtil;

/**
 * Tests {@link SEDXMLWriter} and {@link SEDXMLReader}.
 * @author Martin Hentschel
 */
public class SWTBotSerializationTest extends AbstractSetupTestCase {
   /**
    * Tests the reading and writing process via
    * {@link SEDXMLWriter#toXML(IDebugTarget[], String)},
    * {@link SEDXMLWriter#toXML(ILaunch, String)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, java.io.OutputStream)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, java.io.OutputStream)}
    * {@link SEDXMLReader#read(java.io.InputStream)},
    * {@link SEDXMLReader#read(String)} and
    * {@link SEDXMLReader#read(IFile)}.
    */
   @Test
   public void testWritingAndReading_withoutVariables_and_withoutCallStack() throws Exception {
      doTestWritingAndReading("SWTBotSerializationTest_testWritingAndReading_withoutVariables_and_withoutCallStack", false, false, true);
   }
   
   /**
    * Tests the reading and writing process via
    * {@link SEDXMLWriter#toXML(IDebugTarget[], String)},
    * {@link SEDXMLWriter#toXML(ILaunch, String)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, java.io.OutputStream)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, java.io.OutputStream)}
    * {@link SEDXMLReader#read(java.io.InputStream)},
    * {@link SEDXMLReader#read(String)} and
    * {@link SEDXMLReader#read(IFile)}.
    */
   @Test
   public void testWritingAndReading_withoutCallStack() throws Exception {
      doTestWritingAndReading("SWTBotSerializationTest_testWritingAndReading_withoutCallStack", true, false, true);
   }
   
   /**
    * Tests the reading and writing process via
    * {@link SEDXMLWriter#toXML(IDebugTarget[], String)},
    * {@link SEDXMLWriter#toXML(ILaunch, String)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, java.io.OutputStream)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, java.io.OutputStream)}
    * {@link SEDXMLReader#read(java.io.InputStream)},
    * {@link SEDXMLReader#read(String)} and
    * {@link SEDXMLReader#read(IFile)}.
    */
   @Test
   public void testWritingAndReading_withoutVariables() throws Exception {
      doTestWritingAndReading("SWTBotSerializationTest_testWritingAndReading_withoutVariables", false, true, false);
   }
   
   /**
    * Tests the reading and writing process via
    * {@link SEDXMLWriter#toXML(IDebugTarget[], String)},
    * {@link SEDXMLWriter#toXML(ILaunch, String)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, org.eclipse.core.resources.IFile)},
    * {@link SEDXMLWriter#write(ILaunch, String, java.io.OutputStream)},
    * {@link SEDXMLWriter#write(IDebugTarget[], String, java.io.OutputStream)}
    * {@link SEDXMLReader#read(java.io.InputStream)},
    * {@link SEDXMLReader#read(String)} and
    * {@link SEDXMLReader#read(IFile)}.
    */
   @Test
   public void testWritingAndReading() throws Exception {
      doTestWritingAndReading("SWTBotSerializationTest_testWritingAndReading", true, true, false);
   }
   
   /**
    * Does the test steps of {@link #testWritingAndReading()} and
    * {@link #testWritingAndReading_withoutVariables()}.
    * @param saveVariables Save variables?
    * @param saveCallStack Save call stack?
    * @throws Exception Occurred Exception.
    */
   protected void doTestWritingAndReading(String testName, 
                                          boolean saveVariables,
                                          boolean saveCallStack,
                                          boolean saveConstraints) throws Exception {
      // Close welcome view
      SWTWorkbenchBot bot = new SWTWorkbenchBot();
      IPerspectiveDescriptor defaultPerspective = TestUtilsUtil.getActivePerspective();
      // Disable compact view
      boolean originalCompactView = SEDPreferenceUtil.isShowCompactExecutionTree();
      SEDPreferenceUtil.setShowCompactExecutionTree(true);
      SWTBotTree debugTree = null;
      File tempFile = null;
      try {
         TestUtilsUtil.closeWelcomeView(bot);
         // Open symbolic debug perspective
         TestSedCoreUtil.openSymbolicDebugPerspective();
         // Launch fixed example
         TestSedCoreUtil.launchFixedExample();
         // Find the launched ILaunch in the debug view
         SWTBotView debugView = TestSedCoreUtil.getDebugView(bot);
         debugTree = debugView.bot().tree();
         bot.waitUntil(Conditions.treeHasRows(debugTree, 1));
         // Get the launch
         ISEDDebugTarget target = TestSedCoreUtil.waitUntilDebugTreeHasDebugTarget(bot, debugTree);
         assertNotNull(target);
         ILaunch launch = target.getLaunch();
         assertNotNull(launch);
         // Simulate comments
         ISEDAnnotationType commentType = SEDAnnotationUtil.getAnnotationtype(CommentAnnotationType.TYPE_ID);
         CommentAnnotation commentAnnotation = (CommentAnnotation)commentType.createAnnotation();
         commentAnnotation.setCommentType("MyCommentType");
         CommentAnnotationLink firstCommentLink = (CommentAnnotationLink)commentType.createLink(commentAnnotation, target.getSymbolicThreads()[0].getChildren()[0]);
         CommentAnnotationLink secondCommentLink = (CommentAnnotationLink)commentType.createLink(commentAnnotation, target.getSymbolicThreads()[0].getChildren()[0].getChildren()[0]);
         firstCommentLink.setComment("Comment of first Link");
         secondCommentLink.setComment("Comment of second Link");
         target.registerAnnotation(commentAnnotation);
         firstCommentLink.getTarget().addAnnotationLink(firstCommentLink);
         secondCommentLink.getTarget().addAnnotationLink(secondCommentLink);
         commentAnnotation.setEnabled(false);
         // Simulate search
         ISEDAnnotationType searchType = SEDAnnotationUtil.getAnnotationtype(SearchAnnotationType.TYPE_ID);
         SearchAnnotation searchAnnotation = (SearchAnnotation)searchType.createAnnotation();
         searchAnnotation.setSearch("My Search Content");
         searchAnnotation.setEnabled(true);
         searchAnnotation.setCustomHighlightBackground(Boolean.FALSE);
         searchAnnotation.setCustomHighlightForeground(Boolean.TRUE);
         searchAnnotation.setCustomBackgroundColor(new RGB(255, 128, 0));
         searchAnnotation.setCustomForegroundColor(new RGB(0, 255, 128));
         ISEDAnnotationLink firstSearchLink = searchType.createLink(searchAnnotation, target.getSymbolicThreads()[0].getChildren()[0]);
         ISEDAnnotationLink secondSearchLink = searchType.createLink(searchAnnotation, target.getSymbolicThreads()[0].getChildren()[0].getChildren()[0]);
         target.registerAnnotation(searchAnnotation);
         firstSearchLink.getTarget().addAnnotationLink(firstSearchLink);
         secondSearchLink.getTarget().addAnnotationLink(secondSearchLink);
         // Serialize launch to String
         SEDXMLWriter writer = new SEDXMLWriter();
         String xml = writer.toXML(launch, SEDXMLWriter.DEFAULT_ENCODING, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         SEDXMLReader reader = new SEDXMLReader();
         List<ISEDDebugTarget> read = reader.read(xml);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize targets to String
         xml = writer.toXML(launch.getDebugTargets(), SEDXMLWriter.DEFAULT_ENCODING, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(xml);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to File
         tempFile = File.createTempFile(testName, ".xml");
         writer.write(launch, SEDXMLWriter.DEFAULT_ENCODING, new FileOutputStream(tempFile), saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(new FileInputStream(tempFile));
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to File
         tempFile = File.createTempFile(testName, ".xml");
         writer.write(launch.getDebugTargets(), SEDXMLWriter.DEFAULT_ENCODING, new FileOutputStream(tempFile), saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(new FileInputStream(tempFile));
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to File
         IProject project = TestUtilsUtil.createProject(testName);
         IFile workspaceFile = project.getFile("Test.xml");
         writer.write(launch, SEDXMLWriter.DEFAULT_ENCODING, workspaceFile, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(workspaceFile);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);

         // Serialize launch to File
         writer.write(launch.getDebugTargets(), SEDXMLWriter.DEFAULT_ENCODING, workspaceFile, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(workspaceFile);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to String without encoding
         xml = writer.toXML(launch, null, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(xml);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to String without encoding
         xml = writer.toXML(launch.getDebugTargets(), null, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(xml);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to File without encoding
         writer.write(launch, null, new FileOutputStream(tempFile, false), saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(new FileInputStream(tempFile));
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize launch to File without encoding
         writer.write(launch, null, workspaceFile, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(workspaceFile);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);

         // Serialize launch to File without encoding
         writer.write(launch.getDebugTargets(), null, workspaceFile, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         read = reader.read(workspaceFile);
         // Compare models
         assertNotNull(read);
         assertEquals(1, read.size());
         TestSedCoreUtil.compareDebugTarget(target, read.get(0), true, saveVariables, saveCallStack, saveConstraints);
         
         // Serialize null to String
         xml = writer.toXML((ILaunch)null, SEDXMLWriter.DEFAULT_ENCODING, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         try {
            read = reader.read(xml);
            fail("Reading an empty String should not be possible.");
         }
         catch (Exception e) {
         }
         
         // Serialize null to String
         xml = writer.toXML((IDebugTarget[])null, SEDXMLWriter.DEFAULT_ENCODING, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from String
         try {
            read = reader.read(xml);
            fail("Reading an empty String should not be possible.");
         }
         catch (Exception e) {
         }
         
         // Serialize null to File
         writer.write((ILaunch)null, SEDXMLWriter.DEFAULT_ENCODING, new FileOutputStream(tempFile, false), saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from File
         try {
            read = reader.read(new FileInputStream(tempFile));
            fail("Reading an empty String should not be possible.");
         }
         catch (Exception e) {
         }
         
         // Serialize null to File
         writer.write((IDebugTarget[])null, SEDXMLWriter.DEFAULT_ENCODING, new FileOutputStream(tempFile, false), saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from File
         try {
            read = reader.read(new FileInputStream(tempFile));
            fail("Reading an empty String should not be possible.");
         }
         catch (Exception e) {
         }
         
         // Serialize null to File
         writer.write((ILaunch)null, SEDXMLWriter.DEFAULT_ENCODING, workspaceFile, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from File
         try {
            read = reader.read(new FileInputStream(tempFile));
            fail("Reading an empty String should not be possible.");
         }
         catch (Exception e) {
         }
         
         // Serialize null to File
         writer.write((ISEDDebugTarget[])null, SEDXMLWriter.DEFAULT_ENCODING, workspaceFile, saveVariables, saveCallStack, saveConstraints, null);
         // Read launch from File
         try {
            read = reader.read(new FileInputStream(tempFile));
            fail("Reading an empty String should not be possible.");
         }
         catch (Exception e) {
         }
      }
      finally {
         SEDPreferenceUtil.setShowCompactExecutionTree(originalCompactView);
         // Terminate and remove all launches
         TestSedCoreUtil.terminateAndRemoveAll(debugTree);
         if (tempFile != null) {
            tempFile.delete();
         }
         TestUtilsUtil.openPerspective(defaultPerspective);
      }
   }
}