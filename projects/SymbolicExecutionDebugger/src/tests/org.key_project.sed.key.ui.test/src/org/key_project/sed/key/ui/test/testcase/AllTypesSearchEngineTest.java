package org.key_project.sed.key.ui.test.testcase;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.key_project.key4eclipse.util.eclipse.BundleUtil;
import org.key_project.key4eclipse.util.test.util.TestUtilsUtil;
import org.key_project.sed.key.ui.jdt.AllTypesSearchEngine;
import org.key_project.sed.key.ui.test.Activator;

/**
 * Tests for {@link AllTypesSearchEngine}
 * @author Martin Hentschel
 */
public class AllTypesSearchEngineTest extends TestCase {
    /**
     * Executes a search in that inner and anonymous types are filtered.
     */
    public void testSearchResult_noInnerAnonymousTypes_noAnnotations() throws CoreException, InterruptedException {
        // Create projects with test content
        IJavaProject javaProject = TestUtilsUtil.createJavaProject("AllTypesSearchEngineTest_testSearchResult_noInnerAnonymousTypes_noAnnotations");
        IFolder srcFolder = javaProject.getProject().getFolder("src");
        BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/typesAndMethods", srcFolder);
        javaProject.open(null);
        assertTrue(javaProject.isOpen());
        // Execute search
        IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, IJavaSearchScope.SOURCES);
        AllTypesSearchEngine engine = new AllTypesSearchEngine();
        IType[] types = engine.searchTypes(new NullProgressMonitor(), searchScope);
        for (IType type : types) {
            System.out.println(type.getFullyQualifiedName());
        }
        doCompareTestResult(types, 
                            "InMainPackage", 
                            "a.InAPackage", 
                            "anonymous.AnonymousTypesParent", 
                            "b.b.b.InBbBPackage", 
                            "innerTypes.InnerTypesParent", 
                            "signatures.MethodsWithSignatures", 
                            "types.AClass", 
                            "types.AEnum", 
                            "types.AInterface");
    }
    
    /**
     * Executes a search in that inner and anonymous types are included.
     */
    public void testSearchResult_withInnerAnonymousTypes_noAnnotations() throws CoreException, InterruptedException {
        // Create projects with test content
        IJavaProject javaProject = TestUtilsUtil.createJavaProject("AllTypesSearchEngineTest_testSearchResult_withInnerAnonymousTypes_noAnnotations");
        IFolder srcFolder = javaProject.getProject().getFolder("src");
        BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/typesAndMethods", srcFolder);
        javaProject.open(null);
        assertTrue(javaProject.isOpen());
        // Execute search
        IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, IJavaSearchScope.SOURCES);
        AllTypesSearchEngine engine = new AllTypesSearchEngine();
        engine.setIncludeInnerAndAnonymousTypes(true);
        IType[] types = engine.searchTypes(new NullProgressMonitor(), searchScope);
        doCompareTestResult(types, 
                            "InMainPackage", 
                            "a.InAPackage", 
                            "anonymous.AnonymousTypesParent", 
                            "anonymous.AnonymousTypesParent$1",
                            "anonymous.AnonymousTypesParent$2",
                            "anonymous.AnonymousTypesParent$3",
                            "b.b.b.InBbBPackage", 
                            "innerTypes.InnerTypesParent", 
                            "innerTypes.InnerTypesParent$InnerEnum", 
                            "innerTypes.InnerTypesParent$InnerEnum$InnerInnerEnum", 
                            "innerTypes.InnerTypesParent$InnerInterface", 
                            "innerTypes.InnerTypesParent$InnerInterface$InnerInnerInterface", 
                            "innerTypes.InnerTypesParent$InnerClass", 
                            "innerTypes.InnerTypesParent$InnerClass$InnerInnerClss", 
                            "signatures.MethodsWithSignatures", 
                            "types.AClass", 
                            "types.AEnum", 
                            "types.AInterface");
    }
    
    /**
     * Executes a search in that inner and anonymous types are filtered.
     */
    public void testSearchResult_noInnerAnonymousTypes_withAnnotations() throws CoreException, InterruptedException {
        // Create projects with test content
        IJavaProject javaProject = TestUtilsUtil.createJavaProject("AllTypesSearchEngineTest_testSearchResult_noInnerAnonymousTypes_withAnnotations");
        IFolder srcFolder = javaProject.getProject().getFolder("src");
        BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/typesAndMethods", srcFolder);
        javaProject.open(null);
        assertTrue(javaProject.isOpen());
        // Execute search
        IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, IJavaSearchScope.SOURCES);
        AllTypesSearchEngine engine = new AllTypesSearchEngine();
        engine.setIncludeAnnotations(true);
        IType[] types = engine.searchTypes(new NullProgressMonitor(), searchScope);
        doCompareTestResult(types, 
                            "InMainPackage", 
                            "a.InAPackage", 
                            "anonymous.AnonymousTypesParent", 
                            "b.b.b.InBbBPackage", 
                            "innerTypes.InnerTypesParent", 
                            "signatures.MethodsWithSignatures", 
                            "types.AAnnotation", 
                            "types.AClass", 
                            "types.AEnum", 
                            "types.AInterface");
    }
    
    /**
     * Executes a search in that inner and anonymous types are included.
     */
    public void testSearchResult_withInnerAnonymousTypes_withAnnotations() throws CoreException, InterruptedException {
        // Create projects with test content
        IJavaProject javaProject = TestUtilsUtil.createJavaProject("AllTypesSearchEngineTest_testSearchResult_withInnerAnonymousTypes_withAnnotations");
        IFolder srcFolder = javaProject.getProject().getFolder("src");
        BundleUtil.extractFromBundleToWorkspace(Activator.PLUGIN_ID, "data/typesAndMethods", srcFolder);
        javaProject.open(null);
        assertTrue(javaProject.isOpen());
        // Execute search
        IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject}, IJavaSearchScope.SOURCES);
        AllTypesSearchEngine engine = new AllTypesSearchEngine();
        engine.setIncludeAnnotations(true);
        engine.setIncludeInnerAndAnonymousTypes(true);
        IType[] types = engine.searchTypes(new NullProgressMonitor(), searchScope);
        doCompareTestResult(types, 
                            "InMainPackage", 
                            "a.InAPackage", 
                            "anonymous.AnonymousTypesParent", 
                            "anonymous.AnonymousTypesParent$1",
                            "anonymous.AnonymousTypesParent$2",
                            "anonymous.AnonymousTypesParent$3",
                            "b.b.b.InBbBPackage", 
                            "innerTypes.InnerTypesParent", 
                            "innerTypes.InnerTypesParent$InnerAnnotation", 
                            "innerTypes.InnerTypesParent$InnerAnnotation$InnerInnerAnnotation", 
                            "innerTypes.InnerTypesParent$InnerEnum", 
                            "innerTypes.InnerTypesParent$InnerEnum$InnerInnerEnum", 
                            "innerTypes.InnerTypesParent$InnerInterface", 
                            "innerTypes.InnerTypesParent$InnerInterface$InnerInnerInterface", 
                            "innerTypes.InnerTypesParent$InnerClass", 
                            "innerTypes.InnerTypesParent$InnerClass$InnerInnerClss", 
                            "signatures.MethodsWithSignatures", 
                            "types.AAnnotation", 
                            "types.AClass", 
                            "types.AEnum", 
                            "types.AInterface");
    }
    
    /**
     * Compares the found types with the expected one.
     * @param result The found {@link IType}.
     * @param expectedTypes The expected type full qualified names.
     */
    public void doCompareTestResult(IType[] result, String... expectedTypes) {
        assertNotNull(result);
        assertEquals(expectedTypes.length, result.length);
        for (int i = 0; i < result.length; i++) {
            assertEquals(expectedTypes[i], result[i].getFullyQualifiedName());
        }
    }
}