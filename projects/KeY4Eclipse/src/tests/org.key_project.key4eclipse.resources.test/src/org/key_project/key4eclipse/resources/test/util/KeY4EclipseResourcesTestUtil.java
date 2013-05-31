package org.key_project.key4eclipse.resources.test.util;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.key_project.key4eclipse.resources.builder.KeYProjectBuilder;
import org.key_project.key4eclipse.resources.marker.MarkerManager;
import org.key_project.key4eclipse.resources.nature.KeYProjectNature;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties;
import org.key_project.util.eclipse.ResourceUtil;
import org.key_project.util.java.ArrayUtil;
import org.key_project.util.java.IFilter;
import org.key_project.util.test.util.TestUtilsUtil;

import de.uka.ilkd.key.proof.io.ProblemLoaderException;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;
import de.uka.ilkd.key.ui.CustomConsoleUserInterface;

public class KeY4EclipseResourcesTestUtil {

   
   /**
    * Creates a new KeYProject that is an {@link IJavaProject} with a KeYNature.
    * @param name - the project name
    * @return the created KeYProject
    * @throws CoreException
    * @throws InterruptedException
    */
   public static IJavaProject createKeYProject(String name) throws CoreException, InterruptedException{
      IJavaProject javaProject = TestUtilsUtil.createJavaProject(name);
      IProject project = javaProject.getProject();
      IProjectDescription description = project.getDescription();
      //Add KeYNature
      String[] newNatures = ArrayUtil.add(description.getNatureIds(), KeYProjectNature.NATURE_ID);
      description.setNatureIds(newNatures);
      project.setDescription(description, null);
      assertTrue(hasNature(KeYProjectNature.NATURE_ID, javaProject.getProject()) && hasBuilder(KeYProjectBuilder.BUILDER_ID, javaProject.getProject()));
      return javaProject;
   }
   
   
   /**
    * Checks if the given {@link IProject} has the given nature.
    * @param natureId - the given nature
    * @param project - the {@link IProject} to use
    * @return true if the {@link IProject} hat the given nature. False otherwise.
    * @throws CoreException
    */
   public static boolean hasNature(String natureId, IProject project) throws CoreException{
      IProjectDescription description = project.getDescription();
      return ArrayUtil.contains(description.getNatureIds(), natureId);
   }
   
   
   /**
    * Checks if the given {@link IProject} has the given Builder.
    * @param builderId - the given builder
    * @param project - the {@link IProject} to use
    * @return true if the {@link IProject} hat the given builder. False otherwise.
    * @throws CoreException
    */
   public static boolean hasBuilder(String builderId, IProject project) throws CoreException{
      IProjectDescription description = project.getDescription();
      ICommand keyBuilder = ArrayUtil.search(description.getBuildSpec(), new IFilter<ICommand>() {
         @Override
         public boolean select(ICommand element) {
            return element.getBuilderName().equals(KeYProjectBuilder.BUILDER_ID);
         }
      });
      if(keyBuilder != null){
         return keyBuilder.getBuilderName().equals(builderId);
      }
      else return false;
   }
   
   
   /**
    * Enables or disables the AutoBuild function in eclipse.
    * @param enable - true if the AutoBuild should be enabled. False otherwise
    * @throws CoreException
    */
   public static void enableAutoBuild(boolean enable) throws CoreException{
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IWorkspaceDescription desc = workspace.getDescription();
      if(desc.isAutoBuilding() != enable){
         desc.setAutoBuilding(enable);
         workspace.setDescription(desc);
      }
   }
   
   
   /**
    * Runs an {@link IncrementalProjectBuilder}s INCREMENTAL_BUILD for the given {@link IProject} and waits for the build to finish.
    * @param project - the {@link IProject} to use
    * @throws CoreException
    */
   public static void build(IProject project) throws CoreException{
      project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
      TestUtilsUtil.waitForBuild();
   }
   
   
   /**
    * Loads a given proof{@linkIFile} and returns the loaded {@link Proof}.
    * @param file - the {@link IFile} to load
    * @param project - the {@link IProject} to use
    * @return the loaded {@link Proof}
    * @throws CoreException
    * @throws ProblemLoaderException
    */
   public static Proof loadProofFile(File file, IProject project) throws CoreException, ProblemLoaderException{
      File location = ResourceUtil.getLocation(project);
      File bootClassPath = KeYResourceProperties.getKeYBootClassPathLocation(project);
      List<File> classPaths = KeYResourceProperties.getKeYClassPathEntries(project);
      KeYEnvironment<CustomConsoleUserInterface> environment = KeYEnvironment.load(location, classPaths, bootClassPath);
      environment = KeYEnvironment.load(file, null, null);
      return environment.getLoadedProof();
   }
   
   
   /**
    * Returns the proof{@link IFolder} for the given {@link IProject}.
    * @param project - the {@link IProject} to use
    * @return the proof{@link IFolder}
    */
   public static IFolder getProofFolder(IProject project){
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IPath proofFolderPath = project.getFullPath().append("Proofs");
      return root.getFolder(proofFolderPath);
   }
   
   
   /**
    * Collects all {@link IMarker} of the given type for the given {@link IResource}.
    * @param type - the type to use
    * @param res - the {@link IResource} to use
    * @return the {@link LinkedList} with the collected {@link IMarker}
    * @throws CoreException
    */
   public static LinkedList<IMarker> getKeYMarkerByType(String type, IResource res) throws CoreException{
      LinkedList<IMarker> markerList = new LinkedList<IMarker>();
      IMarker[] markers = res.findMarkers(type, true, IResource.DEPTH_INFINITE);
      for(IMarker marker : markers){
         markerList.add(marker);
      }
      return markerList;
   }
   
   
   /**
    * Collects all KeY{@link IMarker} for the given {@link IResource}.
    * @param res - the {@link IResource} to use
    * @return the {@link LinkedList} with the collected {@link IMarker}
    * @throws CoreException
    */
   public static LinkedList<IMarker> getAllKeYMarker(IResource res) throws CoreException{
      LinkedList<IMarker> allMarkerList = getKeYMarkerByType(MarkerManager.CLOSEDMARKER_ID, res);
      allMarkerList.addAll(getKeYMarkerByType(MarkerManager.NOTCLOSEDMARKER_ID, res));
      LinkedList<IMarker> problemLoaderExceptionMarker = getKeYMarkerByType(MarkerManager.PROBLEMLOADEREXCEPTIONMARKER_ID, res);
      if(allMarkerList.isEmpty())
         return problemLoaderExceptionMarker;
      else if(problemLoaderExceptionMarker.isEmpty()){
         return allMarkerList;
      }
      else{
         return null;
      }
   }
}
