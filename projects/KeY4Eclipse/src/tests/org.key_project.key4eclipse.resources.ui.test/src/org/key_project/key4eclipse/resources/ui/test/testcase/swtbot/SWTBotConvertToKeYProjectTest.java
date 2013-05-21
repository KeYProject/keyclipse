package org.key_project.key4eclipse.resources.ui.test.testcase.swtbot;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Test;
import org.key_project.key4eclipse.resources.ui.test.util.KeY4EclipseResourcesUiTestUtil;
import org.key_project.util.test.util.TestUtilsUtil;

public class SWTBotConvertToKeYProjectTest extends TestCase{

   private SWTWorkbenchBot bot;
   
   
   /**
    * Creates a new {@link IJavaProject} and converts it into a KeYProject. The used view is the ProjectExplorer.
    * @throws CoreException
    * @throws InterruptedException
    */
   @Test
   public void testConvertToKeYProjectInProjectExplorer() throws CoreException, InterruptedException{
      bot = new SWTWorkbenchBot();
      TestUtilsUtil.closeWelcomeView(bot);
      //change project name and open projectexplorer. then test for navigator
      IJavaProject project = TestUtilsUtil.createJavaProject("SWTBotConvertToKeYProjectTest_testConvertToKeYProjectInProjectExplorer");
      openProjectExplorer(bot);
      selectViewByTitle(bot, "Project Explorer");
      TestUtilsUtil.selectInTree(bot.tree(), project.getProject().getName());
      TestUtilsUtil.clickContextMenu(bot.tree(), "Convert to KeY Project");
      TestUtilsUtil.waitForBuild();
      KeY4EclipseResourcesUiTestUtil.assertKeYNature(project.getProject());
   }
   
   
   /**
    * Creates a new {@link IJavaProject} and converts it into a KeYProject. The used view is the PackageExplorer.
    * @throws CoreException
    * @throws InterruptedException
    */
   @Test
   public void testConvertToKeYProjectInPackageExplorer() throws CoreException, InterruptedException{
      bot = new SWTWorkbenchBot();
      TestUtilsUtil.closeWelcomeView(bot);
      IJavaProject project = TestUtilsUtil.createJavaProject("SWTBotConvertToKeYProjectTest_testConvertToKeYProjectInPackageExplorer");
      openPackageExplorer(bot);
      selectViewByTitle(bot, "Package Explorer");
      TestUtilsUtil.selectInTree(bot.tree(), project.getProject().getName());
      TestUtilsUtil.clickContextMenu(bot.tree(), "Convert to KeY Project");
      TestUtilsUtil.waitForBuild();
      KeY4EclipseResourcesUiTestUtil.assertKeYNature(project.getProject());
   }

   
   /**
    * Creates a new {@link IJavaProject} and converts it into a KeYProject. The used view is the ProjectNavigator.
    * @throws CoreException
    * @throws InterruptedException
    */
   @Test
   public void testConvertToKeYProjectInNavigator() throws CoreException, InterruptedException{
      bot = new SWTWorkbenchBot();
      TestUtilsUtil.closeWelcomeView(bot);
      IJavaProject project = TestUtilsUtil.createJavaProject("SWTBotConvertToKeYProjectTest_testConvertToKeYProjectInNavigator");
      openNavigator(bot);
      selectViewByTitle(bot, "Navigator");
      TestUtilsUtil.selectInTree(bot.tree(), project.getProject().getName());
      TestUtilsUtil.clickContextMenu(bot.tree(), "Convert to KeY Project");
      TestUtilsUtil.waitForBuild();
      KeY4EclipseResourcesUiTestUtil.assertKeYNature(project.getProject());
   }
   
   
   /**
    * Opens the ProjectExplorer view for the given {@link SWTWorkbenchBot}
    * @param bot - the {@link SWTWorkbenchBot} to use
    */
   public static void openProjectExplorer(SWTWorkbenchBot bot){
      bot.menu("Window").menu("Show View").menu("Other...").click();
      SWTBotShell shell = bot.shell("Show View");
      shell.activate();
      bot.tree().expandNode("General").select("Project Explorer");
      bot.button("OK").click();
   }
   
   
   /**
    * Opens the PackageExplorer view for the given {@link SWTWorkbenchBot}
    * @param bot - the {@link SWTWorkbenchBot} to use
    */
   public static void openPackageExplorer(SWTWorkbenchBot bot){
      bot.menu("Window").menu("Show View").menu("Other...").click();
      SWTBotShell shell = bot.shell("Show View");
      shell.activate();
      bot.tree().expandNode("Java").select("Package Explorer");
      bot.button("OK").click();
   }
   
   
   /**
    * Opens the ProjectsNavigator view for the given {@link SWTWorkbenchBot}
    * @param bot - the {@link SWTWorkbenchBot} to use
    */
   public static void openNavigator(SWTWorkbenchBot bot){
      bot.menu("Window").menu("Show View").menu("Other...").click();
      SWTBotShell shell = bot.shell("Show View");
      shell.activate();
      bot.tree().expandNode("General").select("Navigator");
      bot.button("OK").click();
   }
   
   
   /**
    * Selects the view with the given title for the given {@link SWTWorkbenchBot}.
    * @param bot - the {@link SWTWorkbenchBot} to use
    * @param title - the title to use
    */
   public static void selectViewByTitle(SWTWorkbenchBot bot, String title){
      SWTBotView viewBot = bot.viewByTitle(title);
      viewBot.show();
      viewBot.setFocus();
   }
}
