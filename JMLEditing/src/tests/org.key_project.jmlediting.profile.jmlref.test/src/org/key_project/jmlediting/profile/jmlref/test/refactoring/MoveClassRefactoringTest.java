package org.key_project.jmlediting.profile.jmlref.test.refactoring;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.key_project.jmlediting.core.profile.JMLPreferencesHelper;
import org.key_project.util.jdt.JDTUtil;
import org.key_project.util.test.util.TestUtilsUtil;

public class MoveClassRefactoringTest{

    private static final String PROJECT_NAME = "MoveClassRefactoringTest";

    private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

    private static IFolder srcFolder;
    private static IProject project;
    private static IFolder oracleFolder;

    private static IJavaProject javaProject;
    final String REF_CLASS_NAME = "Main";
    final String CLASS_NAME_MOVE = "Settings";
    final String TESTPATH = "data\\template\\refactoringMoveTest\\moveClassTest";


    @BeforeClass
    public static void initProject() throws CoreException, InterruptedException {
        TestUtilsUtil.closeWelcomeView();

        javaProject = TestUtilsUtil.createJavaProject(PROJECT_NAME);
        project = javaProject.getProject();
        srcFolder = project.getFolder(JDTUtil.getSourceFolderName());      
        oracleFolder = TestUtilsUtil.createFolder(project, "oracle");

        JMLPreferencesHelper.setProjectJMLProfile(javaProject.getProject(), JMLPreferencesHelper.getDefaultJMLProfile());
    }

    @AfterClass
    public static void deleteProject() throws CoreException {
        project.delete(true, null);
    }
    
    @After public void deleteTestPackage() throws CoreException {
        RefactoringTestUtil.deleteAllPackagesFromFolder(srcFolder);
    }

    @Test
    public void test1SimpleMove() throws InterruptedException, CoreException {

        RefactoringTestUtil.runMoveClassTest(TESTPATH+"\\test1", srcFolder, oracleFolder, bot, CLASS_NAME_MOVE, "test1p1", "test1p2", javaProject);
        
    }

    @Test
    public void test2MoveComplexPackage() throws InterruptedException, CoreException {
        
        RefactoringTestUtil.runMoveClassTest(TESTPATH+"\\test2", srcFolder, oracleFolder, bot, CLASS_NAME_MOVE, "test2p1", "test2p2.complex", javaProject);
      
    }

    @Test
    public void test3MoveUseOps() throws InterruptedException, CoreException {
        
        RefactoringTestUtil.runMoveClassTest(TESTPATH+"\\test3", srcFolder, oracleFolder, bot, CLASS_NAME_MOVE, "test3p1", "test3p2", javaProject);
       
        } 
    
    @Test
    public void test4MoveComplexUseOpsBackwards() throws InterruptedException, CoreException {
        
        RefactoringTestUtil.runMoveClassTest(TESTPATH+"\\test4", srcFolder, oracleFolder, bot, CLASS_NAME_MOVE, "test4p2.complex", "test4p2", javaProject);
        
    }
}
