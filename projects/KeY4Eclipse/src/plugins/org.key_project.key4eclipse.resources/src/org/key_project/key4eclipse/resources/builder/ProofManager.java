package org.key_project.key4eclipse.resources.builder;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.key_project.key4eclipse.resources.marker.MarkerManager;
import org.key_project.key4eclipse.resources.util.KeY4EclipseResourcesUtil;
import org.key_project.key4eclipse.resources.util.LogUtil;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties;
import org.key_project.key4eclipse.starter.core.util.KeYUtil;
import org.key_project.util.eclipse.ResourceUtil;

import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.proof.ProblemLoaderException;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.init.ProofOblInput;
import de.uka.ilkd.key.speclang.FunctionalOperationContract;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;
import de.uka.ilkd.key.ui.CustomConsoleUserInterface;

public class ProofManager {

   private KeYEnvironment<CustomConsoleUserInterface> environment;
   private MarkerManager markerManager;
   private IFolder mainProofFolder;
   private IProject project;   
   
   /**
    * The Constructor - sets up the {@link MarkerManager}, the main Proof{@link IFolder} and tries 
    * to load the {@link KeYEnvironment}. If that fails the problemLoaderException will be set.
    * @param project - the {@link IProject} to use
    * @throws CoreException
    * @throws ProblemLoaderException 
    */
   public ProofManager(IProject project) throws CoreException, ProblemLoaderException{
      markerManager = new MarkerManager();
      mainProofFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(project.getFullPath().append("Proofs"));
      this.project = project;
      try {
         File location = ResourceUtil.getLocation(project);
         File bootClassPath = KeYResourceProperties.getKeYBootClassPathLocation(project);
         List<File> classPaths = KeYResourceProperties.getKeYClassPathEntries(project);
         environment = KeYEnvironment.load(location, classPaths, bootClassPath);
      }
      catch (ProblemLoaderException e) {
         handleProblemLoaderException(e);
         throw e;
      }
   }
   
   
   private IFile getFileFromErrorMessage(String msg) {
      StringBuffer sb = new StringBuffer(msg);
      int lineBreak = sb.indexOf("\n");
      sb.delete(0, lineBreak+1);
      int cutIndex = sb.indexOf(", line");
      sb.delete(cutIndex, sb.length()+1);
      IPath path = new Path(sb.toString());
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFile file = root.getFileForLocation(path);
      if(file.exists()){
         return file;
      }
      else{
         return null;
      }
   }
   
   
   private void handleProblemLoaderException(ProblemLoaderException e) throws CoreException{
      //remove all KeYMarker in the whole project
      LinkedList<IFile> allFiles = collectAllJavaFilesForProject();
      for(IFile file : allFiles){
         deleteMarker(file);
      }
      //add the ProblemExceptionMarker
      IFile file = getFileFromErrorMessage(e.getMessage());
      markerManager.setProblemLoaderExceptionMarker(file, e.getMessage());
   }
   
   
   public void runAllProofs() throws Exception{
      LinkedList<IFile> proofFiles = new LinkedList<IFile>();
      LinkedList<IFile> javaFiles = collectAllJavaFilesForProject();
      deleteAllKeYMarker(javaFiles);
      LinkedList<ProofElement> proofElements = getAllProofElements(javaFiles);
      for(ProofElement pe : proofElements){
         if(pe.hasProof){
            Proof proof = processProof(pe.getProofObl(), pe.getProofFile());
            markerManager.setMarker(proof, pe.getMethod(), pe.getProofFile());
            if(proof != null){
               proofFiles.add(pe.getProofFile());
               KeY4EclipseResourcesUtil.saveProof(proof, pe.getProofFile());
            }
         }
      }
      cleanProofFolder(proofFiles, mainProofFolder);
      
   }
   
   public void runSelectedProofs(LinkedList<IFile> javaFiles) throws Exception{
      deleteAllKeYMarker(javaFiles);
      LinkedList<ProofElement> proofElements = getAllProofElements(javaFiles);
      for(ProofElement pe : proofElements){
         if(pe.hasProof){
            Proof proof = processProof(pe.getProofObl(), pe.getProofFile());
            markerManager.setMarker(proof, pe.getMethod(), pe.getProofFile());
            if(proof != null){
               KeY4EclipseResourcesUtil.saveProof(proof, pe.getProofFile());
            }
         }
      }
      
      cleanProofFolderSelective(getProofElementsByFile(proofElements));
   }
   
   
   private void cleanProofFolderSelective(LinkedList<LinkedList<ProofElement>> proofElementsByFile){
      for(LinkedList<ProofElement> pel : proofElementsByFile){
         ProofElement proofElement = pel.get(0);
         IFolder proofFolder = null;
         LinkedList<IFile> proofFiles = new LinkedList<IFile>();
         if(proofElement.hasProof){
            IContainer container = proofElement.getProofFile().getParent();
            if(container.getType() == IResource.FOLDER){
               proofFolder = (IFolder) container;
            }
            for(ProofElement pe : pel){
               proofFiles.add(pe.getProofFile());
            }
         }
         else{
            proofFolder = getProofFolderForJavaFile(proofElement.getJavaFile());
         }
         //delete proof files which don't belong to any method
         try{
            deleteUnnecessaryProofFiles(proofFolder, proofFiles);
         } catch (CoreException e){
            LogUtil.getLogger().createErrorStatus(e);
         }
      }
   }
   
   
   private LinkedList<LinkedList<ProofElement>> getProofElementsByFile(LinkedList<ProofElement> proofElements){
      //TODO: nicht der reihe nache einsortieren sondern der datei nach. dann immer die sortierten aus dem input l�schen
      LinkedList<LinkedList<ProofElement>> proofElementsByFile = new LinkedList<LinkedList<ProofElement>>();
      IResource currentResource = null;
      LinkedList<ProofElement> sameFile = new LinkedList<ProofElement>();
      for(ProofElement pe : proofElements){
         if(currentResource == null){
            currentResource = pe.getJavaFile();
            sameFile.add(pe);
         }
         else if(currentResource.equals(pe.getJavaFile())){
            sameFile.add(pe);
         }
         else{
            proofElementsByFile.add(sameFile);
            sameFile = new LinkedList<ProofElement>();
            currentResource = pe.getJavaFile();
            sameFile.add(pe);
         }
      }
      proofElementsByFile.add(sameFile);
      return proofElementsByFile;
   }
   
   
   private void deleteAllKeYMarker(LinkedList<IFile> files) throws CoreException{
      for(IFile file : files){
         deleteMarker(file);
      }
   }
   
   private LinkedList<ProofElement> getAllProofElements(LinkedList<IFile> files) throws ProofInputException, CoreException{
      LinkedList<ProofElement> proofElements = new LinkedList<ProofElement>();
      
      for(IFile file : files){
         IFolder proofFolder = createProofFolder(file);
         LinkedList<IMethod> methods = getResourceMethods(file);
         if(methods.size() == 0){
            proofElements.add(new ProofElement(null, null, null, file));
         }
         else{
            for(IMethod method : methods){
               LinkedList<ProofOblInput> obls = createProofOblsForMethod(method);
               for(ProofOblInput obl : obls){
                  IFile proofFile = getProofFile(obl.name(), proofFolder.getFullPath());
                  proofElements.add(new ProofElement(obl, method, proofFile, file));
               }
            }
         }
      }
      return proofElements;
   }
   
   
   private void cleanProofFolder(LinkedList<IFile> proofFiles, IFolder folder) throws CoreException{
      IResource[] members = folder.members();
      for(IResource res : members){
         if(res.getType() == IResource.FILE){
            if(!proofFiles.contains(res)){
               res.delete(true, null);
            }
         }
         else if(res.getType() == IResource.FOLDER){
            cleanProofFolder(proofFiles, (IFolder) res);
         }
      }
      if(folder.members().length == 0){
         folder.delete(true, null);
      }
   }
   
   
   /**
    * Deletes the main Proof{@link IFolder} and runs all {@link Proof}s for the {@link IProject}.
    * @throws Exception
    */
   public void clean() throws Exception{
      deleteResource(mainProofFolder);
      runAllProofs();
   }
   
   
   /**
    * Deletes all KeY-{@link IMarker} from the given {@link IResource}.
    * @param res - the given {@link IResource}
    * @throws CoreException
    */
   public void deleteMarker(IResource res) throws CoreException{
      markerManager.deleteMarker(res);
   }
   
   
   /**
    * Deletes the given {@link IResource} and every empty {@link IFolder} above.
    * @param res - the {@link IResource} to be deleted
    * @throws CoreException
    */
   public void deleteResource(IResource res) throws CoreException{
      if(res != null){
         IContainer container = res.getParent();
         res.delete(true, null);
         if(container.getType() == IResource.FOLDER){
            IFolder folder = (IFolder) container;
            deleteUnnecessaryFolders(folder);
         }
      }
   }
   
   
   /**
    * Deletes the Proof-{@link IFolder} for the given Java-{@link IResource}.
    * @param res - the Java-{@link IResource}
    * @throws CoreException
    */
   public void deleteProofFolderForJavaFile(IResource res) throws CoreException{
      IFolder folder = getProofFolderForJavaFile(res);
      deleteResource(folder);
   }

   
   /**
    * Deletes all not longer required Proof-{@link IFile}s from the given {@link IFolder}. After that all empty {@link IFolder}s above will be deleted.
    * @param folder - the Proof-{@link IFolder}
    * @param files - the {@link IFile}s to keep
    * @throws CoreException
    */
   private void deleteUnnecessaryProofFiles(IFolder folder, LinkedList<IFile> files) throws CoreException{
      IResource[] members = folder.members();
      for(IResource member : members){
         if(member.getType() == IResource.FILE){
            if(!files.contains(member)){
               member.delete(true, null);
            }
         }
      }
      members = folder.members();
      if(members.length == 0){
         deleteUnnecessaryFolders(folder);
      }
   }
   
   
   /**
    * Deletes every {@link IFolder} with no members including and above the given {@link IFolder}.
    * @param folder - the given {@link IFolder}
    * @throws CoreException
    */
   private void deleteUnnecessaryFolders(IFolder folder) throws CoreException{
      if(folder.members().length == 0){
         IContainer container = folder.getParent();
         folder.delete(true, null);
         if(container.getType() == IResource.FOLDER){
            IFolder parentFolder = (IFolder) container;
            deleteUnnecessaryFolders(parentFolder);
         }
      }
   }
   
   
   /**
    * Creates the proofFolderStructure for the given {@link IResource} which must be a javaFile.
    * @param res - the given {@link IResource}
    * @return the created {@link IFolder}
    * @throws CoreException
    */
   private IFolder createProofFolder(IResource res) throws CoreException{
//      IFolder mainProofFolder = createMainProofFolder(res.getProject());
      IFolder proofFolder = mainProofFolder;
      if(!proofFolder.exists()){
         proofFolder.create(true, true, null);
      }
      IPath proofPath = javaToProofPath(res.getFullPath());
      IPath currentProofFolderPath = mainProofFolder.getFullPath();
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for(String folderName : proofPath.segments()){
         currentProofFolderPath = currentProofFolderPath.append(folderName);
          proofFolder = root.getFolder(currentProofFolderPath);
         if(!proofFolder.exists()){
            proofFolder.create(true, true, null);
         }
      }
      return proofFolder;
   }
   
   
   /**
    * If the given {@link IFile} exists the {@link Proof} stored in this {@link IFile} will be loaded. When the {@link Proof} is 
    * loaded and if it's not closed yet, the automode will be started. If the {@link IFile} doesn't exists a new Proof will be 
    * created and the automode will be started.
    * @param obl - the {@link ProofOblInput} for the {@link Proof}
    * @param file - the {@link IFile} of the {@link Proof}
    * @return - the created {@link Proof}
    * @throws Exception
    */
   private Proof processProof(ProofOblInput obl, IFile file) throws Exception{      
      Proof proof;
      if(!file.exists()){
         proof = createProof(obl);
      }
      else{
         File proofFile = file.getLocation().toFile();
         proof = loadProof(proofFile);
         if(proof == null){
            proof = createProof(obl);
         }
      }
      return proof;  
   }
   
   
   /**
    * Creates a {@link Proof} for the given {@link ProofOblInput} and runs the AutoMode.
    * @param obl - the given {@link ProofOblInput}
    * @return the created {@link Proof}
    */
   private Proof createProof(ProofOblInput obl){
      try{
         Proof proof = environment.createProof(obl);
         environment.getUi().startAndWaitForAutoMode(proof);
         return proof;
      }catch(ProofInputException e){
         LogUtil.getLogger().createErrorStatus(e);
         return null;
      }
   }
   
   
   /**
    * Loads the {@link Proof} of the given {@link IFile} and runs the AutoMode.
    * @param file - the given {@link IFile}
    * @return the loaded {@link Proof}
    */
   private Proof loadProof(File file){
      try{
         environment = KeYEnvironment.load(file, null, null);
         Proof proof = environment.getLoadedProof();
         if(!proof.closed()){
            environment.getUi().startAndWaitForAutoMode(proof);
         }
         return proof;
      }catch(ProblemLoaderException e){
         LogUtil.getLogger().createErrorStatus(e);
         return null;
      }
   }
   
   
   /**
    * Returns the Proof-{@link IFile} for the given {@link String} and {@link IPath}.
    * @param name - the name for the {@link IFile}
    * @param path - the {@link IPath} for the {@link IFile} 
    * @return - the {@link IFile} for the Proof
    */
   private IFile getProofFile(String name, IPath path){
      if(path != null && name != null){
         name = makePathValid(name);
         name = name + ".proof";
         path = path.append(name);
         IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
         return file;
      }
      else return null;
   }
   
   
   /**
    * Collects all {@link IMethod}s in the given {@link IResource}.
    * @param res - the given {@link IResource}
    * @return - the {@link LinkedList} with all {@link IMethod}s
    * @throws JavaModelException
    */
   private LinkedList<IMethod> getResourceMethods(IResource res) throws JavaModelException{
      ICompilationUnit unit = (ICompilationUnit) JavaCore.create(res);
      LinkedList<IMethod> methods = new LinkedList<IMethod>();
      IType[] types = unit.getAllTypes();
      for(IType type : types){
         IMethod[] tmp = type.getMethods();
         for(IMethod method : tmp){
            methods.add(method);
         }
      }
      return methods;
   }

   
   /**
    * Creates all {@link ProofOblInput}s for the given {@link IMethod}.
    * @param method - the {@link IMethod to use
    * @return - the {@link LinkedList} with all {@link ProofOblInput}s
    * @throws ProofInputException
    */
   private LinkedList<ProofOblInput> createProofOblsForMethod(IMethod method) throws ProofInputException{
      LinkedList<ProofOblInput> proofObls = new LinkedList<ProofOblInput>();
      ImmutableSet<FunctionalOperationContract>  contracts = createContractsForMethod(method);
      Iterator<FunctionalOperationContract> it = contracts.iterator();
      while (it.hasNext()) {
         FunctionalOperationContract contract = it.next();
         ProofOblInput input = contract.createProofObl(environment.getInitConfig(), contract);
         proofObls.add(input);
      }
      return proofObls;
   }
      
   
   /**
    * Collects all {@link FunctionalOperationContract}s of the given {@link IMethod}.
    * @param method - the given {@link Method}.
    * @param environment - the {@link KeYEnvironment} for this {@link IMethod}.
    * @return - An {@link ImmutableSet<FunctionOperationContract>} that holds all {@link FunctionalOperationContract}s found for the given {@link IMethod}.
    * @throws ProofInputException
    */
   private ImmutableSet<FunctionalOperationContract> createContractsForMethod(IMethod method) throws ProofInputException {
      if (method != null && method.exists()) {
            if(environment.getInitConfig() != null){
               IProgramMethod pm = KeYUtil.getProgramMethod(method, environment.getJavaInfo());
               if(pm != null){
                  KeYJavaType type = pm.getContainerType();
                  ImmutableSet<FunctionalOperationContract> operationContracts = environment.getSpecificationRepository().getOperationContracts(type, pm);
                  return operationContracts;
               }
            }
      }
      return null;
   }
   
   
   /**
    * Replaces invalid characters in the given {@link String} with '_' and returns a valid {@link String}.
    * @param str - the {@link String} to be made valid.
    * @return the valid {@link String}
    */
   //TODO: In Util stecken?
   private String makePathValid(String str){
      String tmp;
      for(int i = 1; i<=str.length();i++){
         tmp = str.substring(0, i);
         Path path = new Path(tmp);
         if(!path.isValidSegment(tmp)){
            StringBuilder strbuilder = new StringBuilder(str);
            strbuilder.setCharAt(i-1, '_');
            str = strbuilder.toString();
         }
      }
      return str;
   }
   
   
   /**
    * Returns the Proof-{@link IFolder} for a given Java-{@link IResource}
    * @param res - the given Java-{@link IResource}
    * @return - the Proof-{@link IFolder}
    */
   private IFolder getProofFolderForJavaFile(IResource res){
      IPath proofFolderPath = mainProofFolder.getFullPath();
      IPath innerProofFolderPath = javaToProofPath(res.getFullPath());
      proofFolderPath = proofFolderPath.append(innerProofFolderPath);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFolder folder = root.getFolder(proofFolderPath);
      if(folder.exists()){
         return folder;
      }
      else{
         return null;
      }
   }
   
   
   /**
    * Returns the Java-{@link IResource} for the given Proof-{@link IResource}
    * @param res - the given Proof-{@link IResource}
    * @return the Java-{@link IFile}
    */
   public IFile getJavaFileForProofFile(IResource res){
      IContainer container = res.getParent();
      if(container.getType() == IResource.FOLDER){
         IFolder folder = (IFolder) container;
         IPath path = proofToJavaPath(folder.getFullPath());
         IPath javaPath = res.getProject().getFullPath().append("src").append(path);
         IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
         IFile javaFile = root.getFile(javaPath);
         if(javaFile.exists()){
            return javaFile;
         }
      }
      return null;
   }
   
   
   /**
    * Converts a javaFiles {@link IPath} to a proofFolder {@link Path}.
    * @param path - the JavaFile {@link IPath}.
    * @return
    */
   private IPath javaToProofPath(IPath path){
      while(path.segmentCount() > 0){
         if(!path.segment(0).equals("src")){
            path = path.removeFirstSegments(1);
         }
         else{
            path = path.removeFirstSegments(1);
            break;
         }
      }
      return path;
   }
   
   
   /**
    * Converts a proofFolders {@link IPath} to a sourceFolder {@link IPath}.
    * @param path the given proofFolder {@link IPath}
    * @return the sourceFolder {@link IPath} for the given proofFolder {@link IPath}
    */
   private IPath proofToJavaPath(IPath path){
      while(path.segmentCount() > 0){
         if(!path.segment(0).equals("Proofs")){
            path = path.removeFirstSegments(1);
         }
         else{
            path = path.removeFirstSegments(1);
            break;
         }
      }
      return path;
   }
   
   
   /**
    * Collects all Java{@link IFile}s of the {@link IProject}.
    * @return the {@link LinkedList} with all Java{@link IFile}s
    * @throws JavaModelException
    */
   private LinkedList<IFile> collectAllJavaFilesForProject() throws JavaModelException{
      IJavaProject javaProject = JavaCore.create(project);
      LinkedList<IFile> javaFiles = new LinkedList<IFile>();
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IPath sourcePath = project.getFullPath().append("src");
      IPackageFragment[] packageFragments = javaProject.getPackageFragments();
      for(IPackageFragment packageFragment : packageFragments){
         ICompilationUnit[] units = packageFragment.getCompilationUnits();
         for(ICompilationUnit unit : units){
            IPath filePath = unit.getResource().getFullPath();
            IFile file = root.getFile(unit.getResource().getFullPath());
            
            if(!javaFiles.contains(file) && sourcePath.isPrefixOf(filePath)){
               javaFiles.add(file);
            }
         }
      }
      return javaFiles;
   }
   
   
}
