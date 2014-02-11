/*******************************************************************************
 * Copyright (c) 2013 Karlsruhe Institute of Technology, Germany 
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

package org.key_project.key4eclipse.resources.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.key_project.key4eclipse.resources.io.ProofMetaFileReader;
import org.key_project.key4eclipse.resources.io.ProofMetaFileTypeElement;
import org.key_project.key4eclipse.resources.io.ProofMetaFileWriter;
import org.key_project.key4eclipse.resources.marker.MarkerManager;
import org.key_project.key4eclipse.resources.property.KeYProjectProperties;
import org.key_project.key4eclipse.resources.util.KeYResourcesUtil;
import org.key_project.key4eclipse.resources.util.LogUtil;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties;
import org.key_project.key4eclipse.starter.core.util.KeYUtil;
import org.key_project.key4eclipse.starter.core.util.KeYUtil.SourceLocation;
import org.key_project.util.eclipse.ResourceUtil;
import org.key_project.util.java.ObjectUtil;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.Main;
import de.uka.ilkd.key.java.JavaSourceElement;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.abstraction.Type;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.SingleProof;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.JavaProfile;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.io.ProblemLoaderException;
import de.uka.ilkd.key.proof.io.ProofSaver;
import de.uka.ilkd.key.proof.mgt.AxiomJustification;
import de.uka.ilkd.key.proof.mgt.ProofEnvironment;
import de.uka.ilkd.key.proof.mgt.RuleJustification;
import de.uka.ilkd.key.proof_references.ProofReferenceUtil;
import de.uka.ilkd.key.rule.BuiltInRule;
import de.uka.ilkd.key.rule.OneStepSimplifier;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.speclang.Contract;
import de.uka.ilkd.key.strategy.StrategyProperties;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;
import de.uka.ilkd.key.symbolic_execution.util.SymbolicExecutionUtil;
import de.uka.ilkd.key.ui.CustomConsoleUserInterface;
import de.uka.ilkd.key.util.MiscTools;
import de.uka.ilkd.key.util.Pair;
import de.uka.ilkd.key.util.ProofStarter;

/**
 * The ProofManager is responsible for the maintasks during the build. It runs and saves the 
 * proofs, creates marker, initializes threads and manages the folderstructure.
 * @author Stefan K�sdorf
 */
public class ProofManager {

   private KeYEnvironment<CustomConsoleUserInterface> environment;
   private MarkerManager markerManager;
   private IFolder mainProofFolder;
   private IProject project;
   private LinkedList<ProofElement> proofElements;
   private LinkedList<IFile> changedJavaFiles;
   private List<ProofElement> proofsToDo = Collections.synchronizedList(new LinkedList<ProofElement>());
   private List<Pair<ByteArrayOutputStream, ProofElement>> proofsToSave = Collections.synchronizedList(new LinkedList<Pair<ByteArrayOutputStream, ProofElement>>());

   
   /**
    * The Constructor that loads the {@link KeYEnvironment}. If that fails the problemLoaderException will be set.
    * @param project - the {@link IProject} to use
    * @throws CoreException
    * @throws ProblemLoaderException 
    */
   public ProofManager(IProject project) throws CoreException, ProblemLoaderException{
      markerManager = new MarkerManager();
      mainProofFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(project.getFullPath().append(KeYResourcesUtil.PROOF_FOLDER_NAME));
      this.project = project;
      try {
         File location = KeYUtil.getSourceLocation(project);
         File bootClassPath = KeYResourceProperties.getKeYBootClassPathLocation(project);
         List<File> classPaths = KeYResourceProperties.getKeYClassPathEntries(project);
         environment = KeYEnvironment.load(location, classPaths, bootClassPath);
      }
      catch (ProblemLoaderException e) {
         handleProblemLoaderException(e);
         throw e;
      }
   }
   
   
   /**
    * Disposes this {@link ProofManager}.
    */
   public void dispose() {
      if (environment != null) {
         environment.dispose();
      }
   }
   
   
   /**
    * Runs the {@link Proof}s available in the {@link IProject} dependent on the ProofManageMentProperties.
    * @param monitor - the {@link IProgressMonitor}
    * @throws Exception
    */
   public void runProofs(IProgressMonitor monitor) throws Exception{
      runProofs(new LinkedList<IFile>(), monitor);
   }


   /**
    * Runs the {@link Proof}s available in the {@link IProject} dependent on the ProofManageMentProperties.
    * @param changedJavaFiles - {@link LinkedList} with all changed java{@link IFile}s
    * @param monitor - the {@link IProgressMonitor}
    * @throws Exception
    */
   public void runProofs(LinkedList<IFile> changedJavaFiles, IProgressMonitor monitor) throws Exception{
      markerManager.deleteKeYMarkerByType(project, IResource.DEPTH_ZERO, MarkerManager.PROBLEMLOADEREXCEPTIONMARKER_ID);
      
      proofElements = getAllProofElements();
      
      this.changedJavaFiles = changedJavaFiles;
      //set up monitor
      monitor.beginTask("Build all proofs", proofElements.size());
      initThreads(changedJavaFiles, monitor);
      checkContractRecursion();
      cleanMarker();
      if(KeYProjectProperties.isAutoDeleteProofFiles(project)){
         cleanProofFolder(getAllFiles(), mainProofFolder);
      }
      monitor.done();
   }
   
   
//   /**
//    * Deletes the main Proof{@link IFolder} and runs all {@link Proof}s for the {@link IProject}.
//    * @throws Exception
//    */
//   public void clean(IProgressMonitor monitor) throws Exception{
//      if(mainProofFolder != null){
//         mainProofFolder.delete(true, null);
//      }
//   }
   
   
   /**
    * Collects all {@link Proof}s available in the {@link IProject} and returns their
    * java{@link IFiles}, {@link SourceLocation}, {@link IMarker}, proof{@link IFolder} and {@link Contract} as 
    * {@link ProofElement}s in a {@link LinkedList}.
    * @return - the {@link LinkedList} with all {@link ProofElement}s
    * @throws CoreException 
    */
   private LinkedList<ProofElement> getAllProofElements() throws CoreException {
      Set<KeYJavaType> kjts = environment.getJavaInfo().getAllKeYJavaTypes();
      KeYJavaType[] kjtsarr = KeYUtil.sortKeYJavaTypes(kjts);
      LinkedList<ProofElement> proofElements = new LinkedList<ProofElement>();
      for (KeYJavaType type : kjtsarr) {
         ImmutableSet<IObserverFunction> targets = environment.getSpecificationRepository().getContractTargets(type);
         Type javaType = type.getJavaType();
         IFile javaFile = null;
         SourceLocation scl = null;
         if(javaType instanceof JavaSourceElement){
            JavaSourceElement javaElement = (JavaSourceElement) javaType;
            String fileName = SymbolicExecutionUtil.getSourcePath(javaElement.getPositionInfo());
            IPath location = new Path(fileName);
            IPath relatviePath = location.makeRelativeTo(project.getLocation().removeLastSegments(1));
            javaFile = ResourcesPlugin.getWorkspace().getRoot().getFile(relatviePath);
            scl = KeYUtil.convertToSourceLocation(javaElement.getPositionInfo());
            scl = KeYUtil.updateToMethodNameLocation(javaFile, scl);
         }
         for (IObserverFunction target : targets) {
            if(target instanceof IProgramMethod){
               IProgramMethod progMethod = (IProgramMethod) target;
               if(progMethod.getContainerType().getJavaType().equals(javaType)){
                  scl = KeYUtil.convertToSourceLocation(progMethod.getPositionInfo());
                  scl = KeYUtil.updateToMethodNameLocation(javaFile, scl);
               }
            }
            ImmutableSet<Contract> contracts = environment.getSpecificationRepository().getContracts(type, target);
            for (Contract contract : contracts) {
               IFolder proofFolder = getProofFolder(javaFile);
               IFile proofFile = getProofFile(contract.getName(), proofFolder.getFullPath());
               IFile metaFile = getProofMetaFile(proofFile);
               LinkedHashSet<IMarker> oldMarker = markerManager.getOldProofMarker(javaFile, scl, proofFile);
               proofElements.add(new ProofElement(javaFile, scl, environment, proofFolder, proofFile, metaFile, oldMarker, contract));
            }
         }
      }
      return proofElements;
   }

   /**
    * Returns the proofFolder for the given java{@link IFile}.
    * @param javaFile - the java{@link IFile} to use
    * @return the proof{@link IFolder}
    */
   private IFolder getProofFolder(IFile javaFile){
      IPath proofFolderPath = mainProofFolder.getFullPath();
      IPath javaToProofPath = javaToProofPath(javaFile.getFullPath());
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFolder proofFolder = root.getFolder(proofFolderPath.append(javaToProofPath));
      return proofFolder;
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
    * Initializes the {@link Thread}s and creates the {@link ProofRunnable}s. While 
    * the {@link Thread}s are alive completed {@link Proof}s are saved periodically. 
    * @param proofElements
    * @param changedJavaFiles
    * @param monitor
    * @throws Exception 
    */
   private void initThreads(LinkedList<IFile> changedJavaFiles, IProgressMonitor monitor) throws Exception {
      proofsToDo = Collections.synchronizedList(KeYResourcesUtil.cloneLinkedList(proofElements));

      int numOfThreads = KeYProjectProperties.getNumberOfThreads(project);
      int numOfProofs = proofElements.size();
      if(numOfProofs < numOfThreads){
         numOfThreads = numOfProofs;
      }
      if (KeYProjectProperties.isEnableMultiThreading(project) && numOfThreads >= 2) {
         Thread[] threads = new Thread[numOfThreads];
         for (int i = 0; i < numOfThreads; i++) {

            ProofRunnable run = new ProofRunnable(cloneEnvironment(), monitor);
            Thread thread = new Thread(run);
            threads[i] = thread;
         }
         for(Thread thread : threads){
            thread.start();
         }
         while(threadsAlive(threads)){
            ObjectUtil.sleep(1000);
            saveProofsFormList();
         }
         saveProofsFormList();
         
      }
      else {
         ProofRunnable run = new ProofRunnable(environment, monitor);
         run.run();
         saveProofsFormList();
      }
   }
   
   
   /**
    * Returns if the given {@link Thread}s are alive.
    * @param threads - the {@link Thread}s th check
    * @return true if at least one {@link Thread} is alive. false otherwise
    */
   private boolean threadsAlive(Thread[] threads){
      for(Thread thread : threads){
         if (thread.isAlive()){
            return true;
         }
      }
      return false;
   }
   
   
   /**
    * Clones the global {@link KeYEnvironment}.
    * @return the cloned {@link KeYEnvironment}
    */
   private KeYEnvironment<CustomConsoleUserInterface> cloneEnvironment(){
      InitConfig sourceInitConfig = environment.getInitConfig();
      // Create new profile which has separate OneStepSimplifier instance
      JavaProfile profile = new JavaProfile();
      // Create new InitConfig and initialize it with value from initial one.
      InitConfig initConfig = new InitConfig(environment.getServices().copy(profile, false));
      initConfig.setActivatedChoices(sourceInitConfig.getActivatedChoices());
      initConfig.setSettings(sourceInitConfig.getSettings());
      initConfig.setTaclet2Builder(sourceInitConfig.getTaclet2Builder());
      initConfig.setTaclets(sourceInitConfig.getTaclets());
      // Create new ProofEnvironment and initialize it with values from initial one.
      ProofEnvironment env = initConfig.getProofEnv();
      env.setJavaModel(sourceInitConfig.getProofEnv().getJavaModel());
      env.setRuleConfig(sourceInitConfig.getProofEnv().getRuleConfig());
      for (Taclet taclet : sourceInitConfig.activatedTaclets()) {
         env.getJustifInfo().addJustification(taclet, sourceInitConfig.getProofEnv().getJustifInfo().getJustification(taclet));
      }
      for (BuiltInRule rule : initConfig.builtInRules()) {
         RuleJustification origJusti = sourceInitConfig.getProofEnv().getJustifInfo().getJustification(rule);
         if (origJusti == null) {
            assert rule instanceof OneStepSimplifier;
            origJusti = AxiomJustification.INSTANCE;
         }
         env.getJustifInfo().addJustification(rule, origJusti);
      }
      KeYEnvironment<CustomConsoleUserInterface> keyEnv = new KeYEnvironment<CustomConsoleUserInterface>(new CustomConsoleUserInterface(false), initConfig);
      return keyEnv;
   }
   
   
   /**
    * Saves all {@link Pair}s from the proofsToSave list and creates the meta{@link IFile}s
    * @throws Exception 
    */
   private void saveProofsFormList() throws Exception{
      while(!proofsToSave.isEmpty()){
         Pair<ByteArrayOutputStream, ProofElement> pairToSave = proofsToSave.remove(0);
         ByteArrayOutputStream out = pairToSave.first;
         ProofElement pe = pairToSave.second;
         saveProof(out, pe);
         ProofMetaFileWriter pmfw = new ProofMetaFileWriter(pe);
         pmfw.writeMetaFile();
      }
   }
   
   
   /**
    * Saves the given {@link ByteOutputStream} into the proof{@link IFile} of the given {@link ProofElement}.
    * @param out - the {@link ByteArrayOutputStream} to use
    * @param pe - the {@link ProofElement} to use
    * @throws CoreException
    */
   private void saveProof(ByteArrayOutputStream out, ProofElement pe) throws CoreException{
      // Save proof file content
      IFile file = pe.getProofFile();
      createProofFolder(file);
      if (file.exists()) {
         file.setContents(new ByteArrayInputStream(out.toByteArray()), true, true, null);
      }
      else {
         file.create(new ByteArrayInputStream(out.toByteArray()), true, null);
      }
   }

   
   /**
    * Creates the proofFolder for the given proof{@link IFile}
    * @param proofFile - the proof{@link IFile} to use
    * @return the created {@link IFolder}
    * @throws CoreException
    */
   private IFolder createProofFolder(IFile proofFile) throws CoreException{
      IFolder proofFolder = null;
      IPath proofFolderPath = proofFile.getFullPath().removeLastSegments(1);
      IPath currentProofFolderPath = null;
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for(int i = 0; i < proofFolderPath.segmentCount(); i++){
         if(currentProofFolderPath == null){
            currentProofFolderPath = new Path(proofFolderPath.segment(i));
         }
         else{
            currentProofFolderPath = currentProofFolderPath.append(proofFolderPath.segment(i));
            proofFolder = root.getFolder(currentProofFolderPath);
            if(!proofFolder.exists()){
               proofFolder.create(true, true, null);
            }
         }
         
      }
      return proofFolder;
   }
   
   
   /**
    * Collects all proof- and meta{@link IFile}s from the given {@link LinkedList} of {@link ProofElement}s.
    * @param proofElements - the {@link ProofElement}s to use
    * @return a {@link LinkedList} with all proof- and meta{@link IFile}s
    */
   private LinkedList<IFile> getAllFiles(){
      LinkedList<IFile> proofFiles = new LinkedList<IFile>();
      for(ProofElement pe : proofElements){
         proofFiles.add(pe.getProofFile());
         proofFiles.add(pe.getMetaFile());
      }
      return proofFiles;
   }
   

   /**
    * Checks all given {@link ProofElement}s for cycling contract recursion
    * @param proofElements - the {@link ProofElement} to check
    * @throws CoreException
    */
   private void checkContractRecursion() throws CoreException{
      findCycles();
      removeAllRecursiveMarker();
      for(LinkedList<ProofElement> cycle : cycles){
         markerManager.setRecursionMarker(cycle);
      }
      restoreOldMarkerForRemovedCycles();
   }
   
   private void restoreOldMarkerForRemovedCycles() throws CoreException{
      for(ProofElement pe : proofElements){
         if(pe.getMarker().isEmpty()){
            markerManager.setMarker(pe); //TODO restore whole message!
         }
      }
   }
   
   private LinkedHashSet<LinkedList<ProofElement>> cycles;
   private void findCycles(){
      cycles = new LinkedHashSet<LinkedList<ProofElement>>();
      for(ProofElement pe : proofElements){
         LinkedList<ProofElement> cycle = new LinkedList<ProofElement>();
         cycle.add(pe);
         searchCycle(cycle);
      }
   }
   
   private void searchCycle(LinkedList<ProofElement> cycle){
      LinkedList<ProofElement> succs = cycle.getLast().getUsedContracts();
      for(ProofElement pe : succs){
         if(pe.equals(cycle.getFirst())){
            //cycle found
            cycles.add(cycle);
         }
         else if(cycle.contains(pe)){
            return;
         }
         else{
            LinkedList<ProofElement> tmpCycle = KeYResourcesUtil.cloneLinkedList(cycle);
            tmpCycle.add(pe);
            searchCycle(tmpCycle); 
         }
      }
   }
   
   
   private void removeAllRecursiveMarker() throws CoreException{
      for(ProofElement pe : proofElements){
         LinkedHashSet<IMarker> peMarker = pe.getMarker();
         LinkedList<IMarker> toBeRemoved = new LinkedList<IMarker>();
         for(IMarker marker : peMarker){
            if(marker != null && MarkerManager.RECURSIONMARKER_ID.equals(marker.getType())){
               toBeRemoved.add(marker);
            }
         }
         while(!toBeRemoved.isEmpty()){
            IMarker marker = toBeRemoved.removeFirst();
            pe.removeMarker(marker);
            marker.delete();
         }
      }
      markerManager.deleteKeYMarkerByType(project, IResource.DEPTH_INFINITE, MarkerManager.RECURSIONMARKER_ID);
   }
   
   
   private void cleanMarker() throws CoreException{
      LinkedList<IMarker> peMarker = new LinkedList<IMarker>();
      for(ProofElement pe : proofElements){
         if(pe.getMarker() != null){
            peMarker.addAll(pe.getMarker());
         }
      }
      LinkedList<IMarker> allMarker = markerManager.getAllKeYMarker(project, IResource.DEPTH_INFINITE);
      for(IMarker marker : allMarker){
         if(!peMarker.contains(marker)){
            marker.delete();
         }
      }
   }
   
   
   /**
    * Cleans every {@link IFolder} in the given {@link IFolder} by removing all files which are not 
    * listed in the given {@link LinkedList}. Empty {@link IFolder} will be deleted too.
    * @param proofFiles - the {@link LinkedList} to use
    * @param folder - the {@link IFolder} to start at
    * @throws CoreException
    */
   private void cleanProofFolder(LinkedList<IFile> proofFiles, IFolder folder) throws CoreException{
      if(folder.exists()){
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
   }
   
   
   /**
    * Handles the occurrence of a {@link ProblemLoaderException}. All KeYMarker will be deleted and a ProblemException{@link IMarker} will be set.
    * @param e - the {@link ProblemLoaderException}
    * @throws CoreException
    */
   private void handleProblemLoaderException(ProblemLoaderException e) throws CoreException{
      //remove all KeYMarker in the whole project
      markerManager.deleteAllKeYMarker(project, IResource.DEPTH_INFINITE);
      //add the ProblemExceptionMarker
      markerManager.setProblemLoaderExceptionMarker(project, e.getMessage());
   }
   
   
//   /**
//    * Collects all Java{@link IFile}s of the {@link IProject}.
//    * @return the {@link LinkedList} with all Java{@link IFile}s
//    * @throws JavaModelException
//    */
//   private LinkedList<IFile> collectAllJavaFilesForProject() throws JavaModelException{
//      IJavaProject javaProject = JavaCore.create(project);
//      LinkedList<IFile> javaFiles = new LinkedList<IFile>();
//      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
//      IPath sourcePath = project.getFullPath().append("src");
//      IPackageFragment[] packageFragments = javaProject.getPackageFragments();
//      for(IPackageFragment packageFragment : packageFragments){
//         ICompilationUnit[] units = packageFragment.getCompilationUnits();
//         for(ICompilationUnit unit : units){            
//            IPath filePath = unit.getResource().getFullPath();
//            IFile javaFile = root.getFile(unit.getResource().getFullPath());
//            if(!javaFiles.contains(javaFile) && sourcePath.isPrefixOf(filePath)){
//               javaFiles.add(javaFile);
//            }
//         }
//      }
//      return javaFiles;
//   }
   
   
   /**
    * Returns and removes the first element in the proofsToDo list.
    * @return the first element of the list.
    */
   private ProofElement getProofToDo() {
      ProofElement pe = null;
      synchronized (proofsToDo) {
         if (!proofsToDo.isEmpty()) {
            pe = proofsToDo.remove(0);
         }
      }
      return pe;
   }
   
   
   /**
    * Returns the proof{@link IFile} for the given {@link String} and {@link IPath}.
    * @param name - the name for the {@link IFile}
    * @param path - the {@link IPath} for the {@link IFile} 
    * @return - the {@link IFile} for the Proof
    */
   private IFile getProofFile(String name, IPath path) {
      if (path != null && name != null) {
         name = ResourceUtil.validateWorkspaceFileName(name);
         name = name + "." + KeYUtil.PROOF_FILE_EXTENSION;
         path = path.append(name);
         IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
         return file;
      }
      else return null;
   }   
   
   /**
    * Collects all {@link IType}s of the project.
    * @return a {@link LinkedList} with all {@link IType}s
    * @throws JavaModelException
    */
   private LinkedList<IType> collectAllJavaITypes() throws JavaModelException{
      LinkedList<IType> typeList = new LinkedList<IType>();
      IJavaProject javaProject = JavaCore.create(project);
      IPackageFragment[] packageFragments = javaProject.getPackageFragments();
      for(IPackageFragment packageFragment : packageFragments){
         ICompilationUnit[] units = packageFragment.getCompilationUnits();
         for(ICompilationUnit unit : units){
            IType[] types = unit.getTypes();
            for(IType type : types){
               typeList.add(type);
               typeList.addAll(collectAllJavaITypesForIType(type));
            }
         }
      }
      return typeList;
   }
   
   
   /**
    * Collects all {@link IType}s of the gien {@link IType}.
    * @param type - the {@link IType} to use
    * @return all {@link IType}s of the given {@link IType}
    * @throws JavaModelException
    */
   private LinkedList<IType> collectAllJavaITypesForIType(IType type) throws JavaModelException{
      LinkedList<IType> types = new LinkedList<IType>();
      IType[] subTypes = type.getTypes();
      for(IType subType : subTypes){
         types.add(subType);
         types.addAll(collectAllJavaITypesForIType(subType));
      }
      return types;
   }
   
   
   /**
    * Returns the metaFile of the given proof{@link IFile}
    * @param proofFile - the proof{@link IFile} to use
    * @return the meta{@link IFile}
    */
   private IFile getProofMetaFile(IFile proofFile){
      IPath proofFilePath = proofFile.getFullPath();
      IPath proofMetaFilePath = proofFilePath.removeFileExtension().addFileExtension(KeYResourcesUtil.META_FILE_EXTENSION);
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      IFile proofMetaFile = root.getFile(proofMetaFilePath);
      return proofMetaFile;
   }
   
   
   /**
    * If the {@link ProofElement}s proof{@link IFile} exists the {@link Proof} stored in this {@link IFile} will be loaded. When the {@link Proof} is 
    * loaded and if it's not closed yet, the automode will be started. If the {@link IFile} doesn't exists a new Proof will be 
    * created and the automode will be started.
    * @param ProofElement - the {@link ProofElement} for the {@link Proof}
    * @throws Exception
    */
   private void processProof(ProofElement pe) throws Exception{
      long proofStart = System.currentTimeMillis();
      IFile file = pe.getProofFile();
      Proof proof = null;
      if(!file.exists()){
         proof = createProof(pe);
      }
      else {
//         proof = loadProof(pe); //TODO: Wait for BugFix
         if(proof == null){
            proof = createProof(pe);
         }
      }
      long proofDuration = System.currentTimeMillis()-proofStart;
      if(proof != null){
         pe.setProofClosed(proof.closed());
         pe.setProofReferences(ProofReferenceUtil.computeProofReferences(proof));
         pe.setUsedContracts(KeYResourcesUtil.getUsedContractsProofElements(pe, proofElements));
         pe.setMarkerMsg(generateProofMarkerMessage(pe, proof, proofDuration));
         markerManager.setMarker(pe);
         ByteArrayOutputStream out = generateSaveProof(proof, pe.getProofFile());
         proofsToSave.add(new Pair<ByteArrayOutputStream, ProofElement>(out, pe));
         proof.dispose();
      }
   }
   
   
   /**
    * Creates a {@link Proof} for the given {@link ProofElement} and runs the AutoMode.
    * @param pe - the given {@link ProofElement}
    * @return the created {@link Proof}.
    * @throws ProofInputException 
    */
   private Proof createProof(ProofElement pe) throws ProofInputException{
         Proof proof = pe.getKeYEnvironment().createProof(pe.getProofObl());   
         
         StrategyProperties strategyProperties = proof.getSettings().getStrategySettings().getActiveStrategyProperties();
         strategyProperties.setProperty(StrategyProperties.STOPMODE_OPTIONS_KEY, StrategyProperties.STOPMODE_NONCLOSE);         
         proof.getSettings().getStrategySettings().setActiveStrategyProperties(strategyProperties);
         
         ProofStarter ps = new ProofStarter(false);
         ps.init(new SingleProof(proof, pe.getProofObl().name()));
         ps.start();
         
         OneStepSimplifier oss = MiscTools.findOneStepSimplifier(proof);
         if (oss != null) {
            oss.refresh(null);
         }
         return proof;
   }
   
   /**
    * Loads the {@link Proof} of the given {@link ProofElement} and runs the AutoMode.
    * @return the loaded {@link Proof}.}
    * @param ProofElement - the given {@link ProofElement}
    */
   private Proof loadProof(ProofElement pe){
      Proof proof = null;
      try{
         File file = pe.getProofFile().getLocation().toFile();
         Profile profile = pe.getKeYEnvironment().getInitConfig().getProfile();
         KeYEnvironment<CustomConsoleUserInterface> loadEnv = KeYEnvironment.load(profile, file, null, null);
         proof = loadEnv.getLoadedProof();
         if (proof != null) {
//            if (!proof.closed()){
//               loadEnv.getUi().startAndWaitForAutoMode(proof);
//            }
         }
      }catch(Exception e){
         LogUtil.getLogger().logError(e);
      }
      return proof;
   }
   
   

   
   private String generateProofMarkerMessage(ProofElement pe, Proof proof, long time){
      StringBuffer sb = new StringBuffer();
      boolean closed = pe.getProofClosed();
      String newLine ="\n";
      
      sb.append(closed? "Closed Proof:" : "Open Proof:");
      sb.append(newLine);
      sb.append(pe.getContract().getName());
      sb.append(newLine);
      sb.append(newLine);
      if(!proof.closed()){
         boolean uncloseable = false;
         for(Goal goal : proof.openEnabledGoals()){
            if(false){//goal uncloseable //TODO ask martin for missing SymbolicExecutionUtil.hasApplicableRules(next)
               uncloseable = true;
               break;
            }
         }
         if(uncloseable){
            sb.append("Reason: Goal can't be closed automatically");
            sb.append(newLine);
            sb.append("Hint: Check code and specifications for bugs or continue proof");
            sb.append(newLine);
            sb.append(newLine);
         }
         else{
            sb.append("Reason: Max. Rule Applications reached");
            sb.append(newLine);
            sb.append("Hint: Continue proof automatic- or interactively");
            sb.append(newLine);
            sb.append(newLine);
         }
      }
      
      sb.append("Time: " + time / 1000 + "." + time % 1000 + " s");
      sb.append(newLine);
      sb.append("Nodes: " + proof.countNodes());
      sb.append(newLine);
      sb.append("Branches: " + proof.countBranches());
      sb.append(newLine);
      
      return sb.toString();
   }
   
   
   /**
    * Creates the {@link ByteOutputStream} for the given {@link Proof}.
    * @param proof - the {@link Proof} to use
    * @return the {@link ByteOutputStream} for the given {@link Proof}
    * @throws CoreException
    */
   private ByteArrayOutputStream generateSaveProof(Proof proof, IFile file) throws CoreException {
      Assert.isNotNull(proof);
      try {
         File location = ResourceUtil.getLocation(file);
         // Create proof file content
         ProofSaver saver = new ProofSaver(proof, location.getAbsolutePath(), Main.INTERNAL_VERSION);
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         String errorMessage = saver.save(out);
         if (errorMessage != null) {
            throw new CoreException(LogUtil.getLogger().createErrorStatus(errorMessage));
         }
         else {
            return out;
         }
      }
      catch (IOException e) {
         throw new CoreException(LogUtil.getLogger().createErrorStatus(e.getMessage(), e));
      }
   }
   
   
   /**
    * Checks if the MD5 of the proof{@link IFile} is different to the MD5 stored in the metafile.
    * @param proofFile - the proof{@link IFile} to use
    * @param pmfr - the {@link ProofMetaFileReader} to use
    * @return false if both MD5s are equal. true otherwise
    * @throws CoreException 
    * @throws IOException 
    */
   private boolean MD5changed(IFile proofFile, ProofMetaFileReader pmfr) throws IOException, CoreException{
      if(proofFile.exists()){
         String metaFilesProofMD5 = pmfr.getProofFileMD5();
         String proofFileHasCode = ResourceUtil.computeContentMD5(proofFile);
         if(metaFilesProofMD5.equals(proofFileHasCode)){
            return false;
         }
         else{
            return true;
         }
      }
      else{
         return true;
      }
   }
   
   
   /**
    * Checks if a type or a subtype from the metafile were changed.  
    * @param pmfr - the {@link ProofMetaFileReader} to use
    * @param javaTypes the {@link LinkedList} with all changed java{@link IFile}s
    * @return true if a type or a subtype was changed. false otherwise
    * @throws JavaModelException
    */
   private boolean typeOrSubTypeChanged(ProofElement pe, ProofMetaFileReader pmfr, LinkedList<IType> javaTypes) throws JavaModelException{
      LinkedList<ProofMetaFileTypeElement> typeElements = pmfr.getTypeElements();
      for(ProofMetaFileTypeElement te : typeElements){
         if(typeChanged(te.getType(), javaTypes)){
            return true;
         }
         else if(subTypeChanged(pe, te, javaTypes)){
            return true;
         }
      }
      return false;
   }
   
   
   /**
    * Checks if the given type was changed.
    * @param type - the type to check
    * @param javaTypes - all {@link IType}s of the project
    * @return true if the type was changed. false otherwise
    * @throws JavaModelException
    */
   private boolean typeChanged(String type, LinkedList<IType> javaTypes) throws JavaModelException{
      IFile javaFile = getJavaFileForType(type, javaTypes);
      //check if type has changed itself
      if(changedJavaFiles.contains(javaFile)){
         return true;
      }
      else {
         return false;
      }
   }
   
   
   /**
    * Chacks if any subTypes of the given {@link ProofMetaFileTypeElement} were changed.
    * @param te - the {@link ProofMetaFileTypeElement} to use
    * @param javaTypes - all {@link IType}s of the project
    * @return true if any subTypes were changed. false otherwise
    * @throws JavaModelException
    */
   private boolean subTypeChanged(ProofElement pe, ProofMetaFileTypeElement te, LinkedList<IType> javaTypes) throws JavaModelException{
      String type = te.getType();
      KeYJavaType kjt = getkeYJavaType(pe.getKeYEnvironment(), type);
//    ImmutableList<KeYJavaType> envSubKjts = environment.getJavaInfo().getAllSubtypes(kjt);
    ImmutableList<KeYJavaType> envSubKjts = pe.getKeYEnvironment().getJavaInfo().getAllSubtypes(kjt);
      
      LinkedList<String> subTypes = te.getSubTypes();
      
      if(envSubKjts.size() != subTypes.size()){
         return true;
      }
      else {
         for(String subType : subTypes){
            if(typeChanged(subType, javaTypes)){
               return true;
            }
         }
      }
      return false;
   }
   
   
   /**
    * Checks if any superTypes of the given {@link KeYJavaType} were changed.
    * @param kjt - the {@link KeYJavaType} to use
    * @param changedJavaFiles - all changed java{@link IFile}s
    * @param javaTypes - all {@link IType}s of the project
    * @return true if any superTypes were changed. false otherwise
    * @throws JavaModelException
    */
   private boolean superTypeChanged(ProofElement pe, List<IFile> changedJavaFiles, LinkedList<IType> javaTypes) throws JavaModelException{
      KeYJavaType kjt = pe.getContract().getKJT();
      KeYJavaType envKjt = getkeYJavaType(pe.getKeYEnvironment(), kjt.getFullName());
      if(envKjt != null){
         ImmutableList<KeYJavaType> envSuperKjts = pe.getKeYEnvironment().getJavaInfo().getAllSupertypes(envKjt);
         for(KeYJavaType envSuperKjt : envSuperKjts){
            IFile javaFile = getJavaFileForType(envSuperKjt.getFullName(), javaTypes);
            if(changedJavaFiles.contains(javaFile)){
               return true;
            }
         }
         return false;
      }
      else{
         return true;
      }
   }
   
   
   /**
    * Returns the java{@link IFile} for the given metaType.
    * @param metaType - the given type
    * @param typeList - all {@link IType}s of the project
    * @return the java{@link IFile} for the given type
    * @throws JavaModelException
    */
   private IFile getJavaFileForType(String metaType, LinkedList<IType> typeList) throws JavaModelException{
      for(IType iType : typeList){
         String typeName = iType.getFullyQualifiedName('.');
         if(typeName.equalsIgnoreCase(metaType)){
            IPath filePath = iType.getResource().getFullPath();
            IFile javaFile = ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
            return javaFile;
         }
      }
      
      return null;
   }
   
   
   /**
    * Returns the {@link KeYJavaType} for the given fullName.
    * @param type - the types full name
    * @return the {@link KeYJavaType}
    */
   private KeYJavaType getkeYJavaType(KeYEnvironment<CustomConsoleUserInterface> env, String type){
      Set<KeYJavaType> envKjts = env.getServices().getJavaInfo().getAllKeYJavaTypes();
      for(KeYJavaType kjt : envKjts){
         if(type.equals(kjt.getFullName())){
            return kjt;
         }
      }
      return null;
   }

   
   /**
    * Inner class to process the proofs in threads.
    * @author Stefan K�sdorf
    */
   private class ProofRunnable implements Runnable {
      
      private final KeYEnvironment<CustomConsoleUserInterface> environment;
      private final IProgressMonitor monitor;
      
      public ProofRunnable(KeYEnvironment<CustomConsoleUserInterface> environment, IProgressMonitor monitor){
         this.environment = environment;
         this.monitor = monitor;
      }
      
      @Override
      public void run() {
         try{
            ProofElement pe;
            while ((pe = getProofToDo()) != null) {
               pe.setKeYEnvironment(environment);
               pe.setProofObl(pe.getContract().createProofObl(environment.getInitConfig(), pe.getContract()));
               
               monitor.subTask("Building " + pe.getProofObl().name());
               
               if(!KeYProjectProperties.isEnableBuildRequiredProofsOnly(project)){
                  processProof(pe);
               }
               else{
                  IFile metaFile = getProofMetaFile(pe.getProofFile());
                  if(pe.getMarker().isEmpty()){
                     processProof(pe);
                  }
                  else if(metaFile.exists()){
                     try{
                        ProofMetaFileReader pmfr = new ProofMetaFileReader(metaFile);
                        LinkedList<IType> javaTypes = collectAllJavaITypes();
                        if(MD5changed(pe.getProofFile(), pmfr) || typeOrSubTypeChanged(pe, pmfr, javaTypes) || superTypeChanged(pe, changedJavaFiles, javaTypes)){
                           processProof(pe);
                        }
                        else{
                           pe.setProofClosed(pmfr.getProofClosed());
                           pe.setMarkerMsg(pmfr.getMarkerMessage());
                           pe.setUsedContracts(KeYResourcesUtil.getProofElementsByProofFiles(pmfr.getUsedContracts(), proofElements));
                        }
                     } catch (Exception e) {
                        LogUtil.getLogger().logError(e);
                        processProof(pe);
                     }
                  }
                  else{
                     processProof(pe);
                  }
               }
               
               monitor.worked(1);
            }
            environment.dispose();
         } catch(Exception e){
            LogUtil.getLogger().logError(e);
         }
      }
   }
}
