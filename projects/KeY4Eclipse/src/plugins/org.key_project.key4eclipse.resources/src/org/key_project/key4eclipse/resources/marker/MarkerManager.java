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

package org.key_project.key4eclipse.resources.marker;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.key_project.key4eclipse.resources.builder.ProofElement;
import org.key_project.key4eclipse.resources.util.KeYResourcesUtil;
import org.key_project.key4eclipse.resources.util.LogUtil;
import org.key_project.key4eclipse.starter.core.util.KeYUtil.SourceLocation;
import org.key_project.util.java.StringUtil;

/**
 * Provides methods to create and delete all KeY{@link IMarker}.
 * @author Stefan K�sdorf
 */
public class MarkerManager {
   
   public final static String CLOSEDMARKER_ID = "org.key_project.key4eclipse.resources.ui.marker.proofClosedMarker";
   public final static String NOTCLOSEDMARKER_ID = "org.key_project.key4eclipse.resources.ui.marker.proofNotClosedMarker";
   public final static String PROBLEMLOADEREXCEPTIONMARKER_ID = "org.key_project.key4eclipse.resources.ui.marker.problemLoaderExceptionMarker";
   public final static String RECURSIONMARKER_ID = "org.key_project.key4eclipse.resources.ui.marker.cycleDetectedMarker";
   public final static String MARKER_ATTRIBUTE_OUTDATED = "org.key_project.key4eclipse.resources.ui.marker.attribute.outdated";
   
   
   /**
    * Creates the {@link MarkerManager#CLOSEDMARKER_ID} or {@link MarkerManager#NOTCLOSEDMARKER_ID} for the given {@link ProofElement}.
    * @param pe - the {@link ProofElement to use.
    * @throws CoreException
    */
   public void setMarker(ProofElement pe) {

      try{
         IMarker proofMarker = pe.getProofMarker();
         if(proofMarker != null){
            proofMarker.delete();
         }
         pe.setProofMarker(null);
         SourceLocation scl = pe.getSourceLocation();
         if(scl != null){
            IMarker marker = null;
            if (pe.getProofClosed()) {
               marker = pe.getJavaFile().createMarker(CLOSEDMARKER_ID);
               if (marker.exists()) {
                  marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
               }
            }
            else {
               marker = pe.getJavaFile().createMarker(NOTCLOSEDMARKER_ID);
               if (marker.exists()) {
                  marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
               }
            }
            marker.setAttribute(IMarker.MESSAGE, pe.getMarkerMsg());
            marker.setAttribute(IMarker.LINE_NUMBER, scl.getLineNumber()); // Required for compatibility with other tools like FeatureIDE even if char start and end is defined.
            marker.setAttribute(IMarker.LOCATION, "line " + scl.getLineNumber()); // Otherwise value "Unknown" is shown in Problems-View
            marker.setAttribute(IMarker.CHAR_START, scl.getCharStart());
            marker.setAttribute(IMarker.CHAR_END, scl.getCharEnd());
            marker.setAttribute(IMarker.SOURCE_ID, pe.getProofFile().getFullPath().toString());
            marker.setAttribute(MarkerManager.MARKER_ATTRIBUTE_OUTDATED, false);
            pe.setProofMarker(marker);
         }
      } catch(CoreException e){
         LogUtil.getLogger().logError(e);
      }
   }
   
   
   /**
    * Creates the {@link MarkerManager#RECURSIONMARKER_ID} for the given {@ink ProofElement}.
    * @param pe - the {@link ProofElement} to use
    * @throws CoreException
    */
   public void setRecursionMarker(List<ProofElement> cycle) {
      try{
         ProofElement pe = cycle.get(0);
         IMarker proofMarker = pe.getProofMarker();
         if(proofMarker != null){
            proofMarker.delete();
         }
         pe.setProofMarker(null);
   
         IMarker marker = pe.getJavaFile().createMarker(RECURSIONMARKER_ID);
         if (marker.exists()) {
            marker.setAttribute(IMarker.MESSAGE, generateCycleDetectedMarkerMessage(cycle));
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            marker.setAttribute(IMarker.LINE_NUMBER, pe.getSourceLocation().getLineNumber()); // Required for compatibility with other tools like FeatureIDE even if char start and end is defined.
            marker.setAttribute(IMarker.LOCATION, "line " + pe.getSourceLocation().getLineNumber()); // Otherwise value "Unknown" is shown in Problems-View
            marker.setAttribute(IMarker.CHAR_START, pe.getSourceLocation().getCharStart());
            marker.setAttribute(IMarker.CHAR_END, pe.getSourceLocation().getCharEnd());
            marker.setAttribute(IMarker.SOURCE_ID, pe.getProofFile().getFullPath().toString());
            marker.setAttribute(MarkerManager.MARKER_ATTRIBUTE_OUTDATED, pe.getOutdated());
            updateOutdatedProofMessage(marker, pe.getOutdated());
            pe.addRecursionMarker(marker);
         }
      }
      catch(CoreException e){
         LogUtil.getLogger().logError(e);
      }
   }
   
   
   public void setOutdated(ProofElement pe){
      pe.setBuild(true);
      if(!pe.getOutdated()){
         pe.setOutdated(true);
         IMarker proofMarker = pe.getProofMarker();
         if(proofMarker != null && proofMarker.exists()){
            try {
               proofMarker.setAttribute(MarkerManager.MARKER_ATTRIBUTE_OUTDATED, true);
               updateOutdatedProofMessage(proofMarker, true);
            }
            catch (CoreException e) {
               LogUtil.getLogger().logError(e);
            }
         }
         List<IMarker> recursionMarker = pe.getRecursionMarker();
         for(IMarker marker : recursionMarker){
            if(marker != null && marker.exists()){
               try {
                  marker.setAttribute(MarkerManager.MARKER_ATTRIBUTE_OUTDATED, true);
                  updateOutdatedProofMessage(marker, true);
               }
               catch (CoreException e) {
                  LogUtil.getLogger().logError(e);
               }
            }
         }
      }
   }
         
      
   private void updateOutdatedProofMessage(IMarker marker, boolean outdated) throws CoreException{
      String message = marker.getAttribute(IMarker.MESSAGE, "");
      String appendix = StringUtil.NEW_LINE + StringUtil.NEW_LINE + "Outdated proof - new build required!";
      StringBuilder sb = new StringBuilder(message);
      if(outdated){
         sb.append(appendix);
      }
      else{
         int appendixIndex = sb.indexOf(appendix);
         if(appendixIndex != -1){
            sb.delete(appendixIndex, sb.length()-1);
         }
      }
      marker.setAttribute(IMarker.MESSAGE, sb.toString());
   }
   
   
   /**
    * Checks if the given {@link IProject} contains any outed {@link IMarker}.
    * @param project - the {@link IProject} to check
    * @return true if there is at least one outdated {@link IMarker}
    */
   public boolean hasOutdatedMarker(IProject project){
      List<IMarker> markerList = getKeYMarkerByType(project, IResource.DEPTH_INFINITE, MarkerManager.CLOSEDMARKER_ID, MarkerManager.NOTCLOSEDMARKER_ID, MarkerManager.RECURSIONMARKER_ID);
      for(IMarker marker : markerList){
         boolean outdated = Boolean.valueOf(marker.getAttribute(MarkerManager.MARKER_ATTRIBUTE_OUTDATED, true));
         if(outdated){
            return true;
         }
      }
      return false;
   }
   
   
   /** 
    * Generates the message for the recursion{@link IMarker} of the given cycle,
    * @param cycle - the cycle to use
    * @return the marker message
    */
   private String generateCycleDetectedMarkerMessage(List<ProofElement> cycle){
      StringBuffer sb = new StringBuffer();
      sb.append("Cycle detected:");
      for(ProofElement pe : cycle){
         sb.append(StringUtil.NEW_LINE);
         sb.append(pe.getContract().getName());
      }
      return sb.toString();
   }
      
   
   /**
    * Creates the ProofLoaderException{@link IMarker} for the given {@link IResource}.
    * @param res - the {@link IResource} to use
    * @throws CoreException
    */
   public void setProblemLoaderExceptionMarker(IResource res, int lineNumber, String msg) throws CoreException{
      deleteKeYMarkerByType(res.getProject(), IResource.DEPTH_INFINITE, MarkerManager.PROBLEMLOADEREXCEPTIONMARKER_ID);
      IMarker marker = res.createMarker(PROBLEMLOADEREXCEPTIONMARKER_ID);
      if (marker.exists()) {
         marker.setAttribute(IMarker.MESSAGE, msg);
         marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
         if(res instanceof IFile && lineNumber != -1){
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
         }
      }
   }
   
   
   public IMarker getProofMarker(IFile javaFile, SourceLocation scl, IFile proofFile){
      IMarker proofMarker = null;
      List<IMarker> markerList = null;
      markerList = getKeYMarkerByType(javaFile, IResource.DEPTH_ZERO, CLOSEDMARKER_ID, NOTCLOSEDMARKER_ID);
      for(IMarker marker : markerList){
         if(marker != null && marker.exists()){
            try{
               Integer startChar = (Integer) marker.getAttribute(IMarker.CHAR_START);
               Integer endChar = (Integer) marker.getAttribute(IMarker.CHAR_END);
               String source = marker.getAttribute(IMarker.SOURCE_ID, null);
               if(startChar != null && scl.getCharStart() == startChar 
                  && endChar != null && scl.getCharEnd() == endChar 
                  && source != null && source.equals(proofFile.getFullPath().toString())){
                  proofMarker = marker;
                  break;
               }
            } catch(CoreException e){
               LogUtil.getLogger().logError(e);
            }
         }
      }
      return proofMarker;
   }
   
   
   public List<IMarker> getRecursionMarker(IFile javaFile, SourceLocation scl, IFile proofFile){
      List<IMarker> recursionMarker = new LinkedList<IMarker>();
      List<IMarker> markerList = null;
      markerList = getKeYMarkerByType(javaFile, IResource.DEPTH_ZERO, RECURSIONMARKER_ID);
      for(IMarker marker : markerList){
         if(marker != null && marker.exists()){
            try{
               Integer startChar = (Integer) marker.getAttribute(IMarker.CHAR_START);
               Integer endChar = (Integer) marker.getAttribute(IMarker.CHAR_END);
               String source = marker.getAttribute(IMarker.SOURCE_ID, null);
               if(scl.getCharStart() == startChar && scl.getCharEnd() == endChar && source != null && source.equals(proofFile.getFullPath().toString())){
                  recursionMarker.add(marker);
               }
            } catch (CoreException e) {
               return new LinkedList<IMarker>();
            }
         }
      }
      return recursionMarker;
   }


   /**
    * Collects all KeY{@link IMarker} for the given {@link IResource}.
    * @param res - the {@link IResource} to use
    * @return a {@link LinkedList} with all KeY{@link IMarker}
    * @throws CoreException
    */
   public LinkedList<IMarker> getAllKeYMarker(IResource res, int depth) {
      return getKeYMarkerByType(res, depth, CLOSEDMARKER_ID, NOTCLOSEDMARKER_ID, PROBLEMLOADEREXCEPTIONMARKER_ID, RECURSIONMARKER_ID);
   }
   
   
   /**
    * Returns all {@link IMarker} matching the given types for the given {@link IResource}.
    * @param res - the {@link IResource} to use
    * @param depth - the depth to use
    * @param types - the types to look for
    * @return a {@link LinkedList} with all matching {@link IMarker}
    * @throws CoreException
    */
   public LinkedList<IMarker> getKeYMarkerByType(IResource res, int depth, String... types){
      LinkedList<IMarker> markerList = new LinkedList<IMarker>();
      for(String type : types){
         if(CLOSEDMARKER_ID.equals(type) || NOTCLOSEDMARKER_ID.equals(type) || PROBLEMLOADEREXCEPTIONMARKER_ID.equals(type) || RECURSIONMARKER_ID.equals(type)){
            try {
               markerList.addAll(KeYResourcesUtil.arrayToList(res.findMarkers(type, true, depth)));
            }
            catch (CoreException e) {
               LogUtil.getLogger().logError(e);
            }
         }
      }
      return markerList;
   }
   
   
   public void deleteAllKeYMarker(IResource res, int depth) throws CoreException{
      deleteKeYMarkerByType(res, depth, CLOSEDMARKER_ID, NOTCLOSEDMARKER_ID, PROBLEMLOADEREXCEPTIONMARKER_ID, RECURSIONMARKER_ID);
   }
   
   
   /**
    * Removes all KeYResource {@link IMarker} from the given {@link IResource} matching the given types using the given depth.
    * @param res - the {@link IResource} to use
    * @param type - the type to be delete
    * @param depth - the depth to use
    * @throws CoreException
    */
   public void deleteKeYMarkerByType(IResource res, int depth, String... types) throws CoreException{
      for(String type : types){
         if(CLOSEDMARKER_ID.equals(type)){
            res.deleteMarkers(CLOSEDMARKER_ID, true, depth);
         } else if(NOTCLOSEDMARKER_ID.equals(type)){
            res.deleteMarkers(NOTCLOSEDMARKER_ID, true, depth);
         } else if(PROBLEMLOADEREXCEPTIONMARKER_ID.equals(type)){
            res.deleteMarkers(PROBLEMLOADEREXCEPTIONMARKER_ID, true, depth);
         } else if(RECURSIONMARKER_ID.equals(type)){
            res.deleteMarkers(RECURSIONMARKER_ID, true, depth);
         }
      }
   }
}
