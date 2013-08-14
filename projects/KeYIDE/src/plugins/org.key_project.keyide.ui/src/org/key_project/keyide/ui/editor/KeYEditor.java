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

package org.key_project.keyide.ui.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.key_project.key4eclipse.common.ui.decorator.ProofSourceViewerDecorator;
import org.key_project.key4eclipse.starter.core.util.IProofProvider;
import org.key_project.key4eclipse.starter.core.util.KeYUtil;
import org.key_project.key4eclipse.starter.core.util.ProofUserManager;
import org.key_project.key4eclipse.starter.core.util.event.IProofProviderListener;
import org.key_project.key4eclipse.starter.core.util.event.ProofProviderEvent;
import org.key_project.keyide.ui.editor.input.ProofEditorInput;
import org.key_project.keyide.ui.editor.input.ProofOblInputEditorInput;
import org.key_project.keyide.ui.propertyTester.AutoModePropertyTester;
import org.key_project.keyide.ui.propertyTester.ProofPropertyTester;
import org.key_project.keyide.ui.util.LogUtil;
import org.key_project.keyide.ui.views.ProofTreeContentOutlinePage;
import org.key_project.util.eclipse.ResourceUtil;

import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.gui.KeYSelectionEvent;
import de.uka.ilkd.key.gui.KeYSelectionListener;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.ProofTreeEvent;
import de.uka.ilkd.key.proof.ProofTreeListener;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;
import de.uka.ilkd.key.ui.ConsoleUserInterface;
import de.uka.ilkd.key.ui.CustomConsoleUserInterface;
import de.uka.ilkd.key.ui.UserInterface;

/**
 * This class represents the Editor for viewing KeY-Proofs
 * 
 * @author Christoph Schneider, Niklas Bunzel, Stefan K�sdorf, Marco Drebing
 */
public class KeYEditor extends TextEditor implements IProofProvider {
   /**
    * The unique ID of this editor.
    */
   public static final String EDITOR_ID = "org.key_project.keyide.ui.editor";
   
   /**
    * {@code true} can start auto mode, {@code false} is not allowed to start auto mode.
    */
   private boolean canStartAutomode = true;

   /**
    * {@code true} can apply rules, {@code false} is not allowed to apply rules.
    */
   private boolean canApplyRules = true;

   /**
    * {@code true} can prune proof, {@code false} is not allowed to prune proof.
    */
   private boolean canPruneProof = true;

   /**
    * {@code true} can start SMT solver, {@code false} is not allowed to start SMT solver.
    */
   private boolean canStartSMTSolver = true;
   
   /**
    * The dirty flag.
    */
   private boolean dirtyFlag = false;
      
   /**
    * The used {@link KeYEnvironment}
    */
   private KeYEnvironment<CustomConsoleUserInterface> environment;
   
   /**
    * The current {@link Proof}.
    */
   private Proof currentProof;

   /**
    * The currently shown {@link Node}.
    */
   private Node currentNode; 
   
   private ProofSourceViewerDecorator viewerDecorator;

   /**
    * The provided {@link ProofTreeContentOutlinePage}.
    */
   private ProofTreeContentOutlinePage outline;
   
   /**
    * Contains the registered {@link IProofProviderListener}.
    */
   private List<IProofProviderListener> proofProviderListener = new LinkedList<IProofProviderListener>();
   
   /**
    * Listens for changes on {@link ConsoleUserInterface#isAutoMode()} 
    * of the {@link ConsoleUserInterface} provided via {@link #getEnvironment()}.
    */
   private PropertyChangeListener autoModeActiveListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
         handleAutoModeStartedOrStopped(evt);
      }
   };
   
   /**
    * Listens for changes on {@link #currentProof}.
    */
   private ProofTreeListener proofTreeListener = new ProofTreeListener() {
      @Override
      public void smtDataUpdate(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofStructureChanged(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofPruned(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofIsBeingPruned(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofGoalsChanged(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofGoalsAdded(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofGoalRemoved(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofExpanded(ProofTreeEvent e) {
         handleProofChanged(e);
      }
      
      @Override
      public void proofClosed(ProofTreeEvent e) {
         handleProofChanged(e);
         handleProofClosed(e);
      }
   };

   /**
    * Listens for {@link Node} selection changes.
    */
   private KeYSelectionListener keySelectionListener = new KeYSelectionListener() {
      @Override
      public void selectedProofChanged(KeYSelectionEvent e) {
         handleSelectedProofChanged(e);
      }
      
      @Override
      public void selectedNodeChanged(KeYSelectionEvent e) {
         handleSelectedNodeChanged(e);
      }
   };
   
   /**
    * Constructor to initialize the ContextMenu IDs
    */
   public KeYEditor() {
      setEditorContextMenuId("#KeYEditorContext");
      setRulerContextMenuId("#KeYEditorRulerContext");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void dispose() {
      if (viewerDecorator != null) {
         viewerDecorator.dispose();
      }
      if (getUI() != null) {
         getUI().removePropertyChangeListener(ConsoleUserInterface.PROP_AUTO_MODE, autoModeActiveListener);
      }
      if (environment != null) {
         environment.getMediator().removeKeYSelectionListener(keySelectionListener);
      }
      if (currentProof != null) {
         currentProof.removeProofTreeListener(proofTreeListener);
      }
      if (outline != null) {
         outline.dispose();         
      }
      if (currentProof != null) {
         ProofUserManager.getInstance().removeUserAndDispose(currentProof, this);
      }
      super.dispose();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void init(IEditorSite site, IEditorInput input) throws PartInitException {
      super.init(site, input);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void doSetInput(IEditorInput input) throws CoreException {
      try {
         super.doSetInput(input);
         if (this.environment == null || this.currentProof == null) {
            if (input instanceof ProofOblInputEditorInput) {
               ProofOblInputEditorInput in = (ProofOblInputEditorInput) input;
               this.environment = in.getEnvironment();
               this.currentProof = environment.createProof(in.getProblem());
               ProofUserManager.getInstance().addUser(currentProof, environment, this);
               this.environment.getMediator().setProof(currentProof);
               this.environment.getMediator().setStupidMode(true);
            }
            else if (input instanceof ProofEditorInput) {
               ProofEditorInput in = (ProofEditorInput) input;
               this.canStartAutomode = in.isCanStartAutomode();
               this.canApplyRules = in.isCanApplyRules();
               this.canPruneProof = in.isCanPruneProof();
               this.canStartSMTSolver = in.isCanStartSMTSolver();
               this.environment = in.getEnvironment();
               this.currentProof = in.getProof();
               ProofUserManager.getInstance().addUser(currentProof, environment, this);
               this.environment.getMediator().setProof(currentProof);
               this.environment.getMediator().setStupidMode(true);
            }
            else if (input instanceof FileEditorInput) {
               FileEditorInput fileInput = (FileEditorInput) input;
               File file = ResourceUtil.getLocation(fileInput.getFile());
               Assert.isTrue(file != null, "File \"" + fileInput.getFile() + "\" is not local.");
               this.environment = KeYEnvironment.load(file, null, null);
               this.environment.getMediator().setStupidMode(true);
               Assert.isTrue(getEnvironment().getLoadedProof() != null, "No proof loaded.");
               this.currentProof = getEnvironment().getLoadedProof();
               ProofUserManager.getInstance().addUser(currentProof, environment, this);
            }
         }
         else {
            setCurrentNode(currentNode);
         }
      }
      catch (CoreException e) {
         throw e;
      }
      catch (Exception e) {
         throw new CoreException(LogUtil.getLogger().createErrorStatus(e));
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void createPartControl(Composite parent) {
      super.createPartControl(parent);
      getMediator().addKeYSelectionListener(keySelectionListener);
      getUI().addPropertyChangeListener(ConsoleUserInterface.PROP_AUTO_MODE, autoModeActiveListener);
      ISourceViewer sourceViewer = getSourceViewer();
      viewerDecorator = new ProofSourceViewerDecorator(sourceViewer);
      getCurrentProof().addProofTreeListener(proofTreeListener);
      sourceViewer.setEditable(false);
      if (this.getCurrentNode() != null) {
         setCurrentNode(currentProof.root());
      }
      else {
         Node mediatorNode = environment.getMediator().getSelectedNode();
         setCurrentNode(mediatorNode != null ? mediatorNode : getCurrentProof().root());
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEditable() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void doSaveAs() {
      Shell shell = getSite().getWorkbenchWindow().getShell();
      SaveAsDialog dialog = new SaveAsDialog(shell);
      dialog.setTitle("Save Proof");
      
      IEditorInput input = getEditorInput();
      if(input instanceof ProofOblInputEditorInput){
         IMethod method = ((ProofOblInputEditorInput)input).getMethod();
         IPath methodPath = method.getPath();
         methodPath = methodPath.removeLastSegments(1);
         String name = getCurrentProof().name().toString();
         name = ResourceUtil.validateWorkspaceFileName(name);
         name = name + "." + KeYUtil.PROOF_FILE_EXTENSION;
         methodPath = methodPath.append(name);
         IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(methodPath);
         dialog.setOriginalFile(file);
      }
      else if(input instanceof FileEditorInput){
         FileEditorInput in = (FileEditorInput) input;
         IFile file = in.getFile();
         dialog.setOriginalFile(file);
      }
      dialog.create();
      dialog.open();
      IPath path = dialog.getResult();
      save(path);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void doSave(IProgressMonitor progressMonitor) {
      if(getEditorInput() instanceof FileEditorInput) {
         FileEditorInput input = (FileEditorInput) getEditorInput();
         save(input.getFile().getFullPath());
      }
      else{
         doSaveAs();
      }
   }
   
   /**
    * Saves the current proof at the given {@link IPath}.
    * @param path The {@link IPath} to save proof to.
    */
   private void save(IPath path) {
      try {
         if (path != null) {
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            KeYUtil.saveProof(currentNode.proof(), file);
            setDirtyFlag(false);
            FileEditorInput fileInput = new FileEditorInput(file);
            doSetInput(fileInput);
         }
      }
      catch (Exception e) {
         LogUtil.getLogger().createErrorStatus(e);
      }
   }

   /**
    * Updates the dirty flag.
    * @param dirtyFlag The new dirty flag to set.
    */
   private void setDirtyFlag(boolean dirtyFlag) {
      this.dirtyFlag = dirtyFlag;
      getSite().getShell().getDisplay().syncExec(new Runnable() {
         @Override
         public void run() {
            firePropertyChange(PROP_DIRTY);
         }
      });
   }

   /**
    * This method is called when the {@link Proof} is closed.
    * @param e The {@link ProofTreeEvent}.
    */
   protected void handleProofClosed(ProofTreeEvent e) {
      ProofPropertyTester.updateProperties(); // Make sure that start/stop auto mode buttons are disabled when the proof is closed interactively.
   }

   /**
    * This method is called when the {@link Proof} has changed.
    * @param e The {@link ProofTreeEvent}.
    */
   protected void handleProofChanged(ProofTreeEvent e) {
      setDirtyFlag(true);
   }

   /**
    * This method is called when the selected {@link Proof} has changed.
    * @param e The {@link KeYSelectionEvent}.
    */
   protected void handleSelectedProofChanged(final KeYSelectionEvent e) {
      getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {
            if(e.getSource().getSelectedNode() != null){
               setCurrentNode(e.getSource().getSelectedNode());
            }
         }
      });
   }
   
   /**
    * This method is called when the selected {@link Node} has changed.
    * @param e The {@link KeYSelectionEvent}.
    */
   protected void handleSelectedNodeChanged(final KeYSelectionEvent e) {
      getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
         @Override
         public void run() {
            if(e.getSource().getSelectedNode() != null){
               setCurrentNode(e.getSource().getSelectedNode());
            }
         }
      });
   }

   /**
    * This method is called when the auto mode stops.
    * @param evt The event.
    */
   protected void handleAutoModeStartedOrStopped(PropertyChangeEvent evt) {
      AutoModePropertyTester.updateProperties(); // Make sure that start/stop auto mode buttons are disabled when the proof is closed interactively.
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isDirty() {
      return dirtyFlag;
   }
   
   /**
    * Returns the currently shown {@link Node}.
    * @return The currently shown {@link Node}.
    */
   public Node getCurrentNode() {
      return currentNode;
   }
   
   /**
    * Sets the current {@link Node} and the {@link Document} for the {@link ISourceViewer} of the {@link ProofSourceViewerDecorator}.
    * @param currentNode The current {@link Node} to set.
    */
   public void setCurrentNode(Node currentNode) {
      this.currentNode = currentNode;
      getMediator().setStupidMode(true);
      viewerDecorator.showNode(currentNode, getMediator());
   }
   
   /**
    * Returns the selected {@link PosInSequent}.
    * @return The selected {@link PosInSequent}.
    */
   public PosInSequent getSelectedPosInSequent() {
      return viewerDecorator.getSelectedPosInSequent();
   }

   /**
    * Checks if it is allowed to start the auto mode.
    * @return {@code true} can start auto mode, {@code false} is not allowed to start auto mode.
    */
   public boolean isCanStartAutomode() {
      return canStartAutomode;
   }

   /**
    * Checks if it is allowed to apply rules.
    * @return {@code true} can apply rules, {@code false} is not allowed to apply rules.
    */
   public boolean isCanApplyRules() {
      return canApplyRules;
   }

   /**
    * Checks if it is allowed to prune proof.
    * @return {@code true} can prune proof, {@code false} is not allowed to prune proof.
    */
   public boolean isCanPruneProof() {
      return canPruneProof;
   }

   /**
    * Checks if it is allowed to start SMT solver.
    * @return {@code true} can start SMT solver, {@code false} is not allowed to start SMT solver.
    */
   public boolean isCanStartSMTSolver() {
      return canStartSMTSolver;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
      if (IContentOutlinePage.class.equals(adapter)) {
         synchronized (this) {
            if (outline == null) {
               outline = new ProofTreeContentOutlinePage(getCurrentProof(), getEnvironment());
            }
         }
         return outline;
      }
      else if (Proof.class.equals(adapter)) {
         return getCurrentProof();
      }
      else if (KeYEnvironment.class.equals(adapter)) {
         return getEnvironment();
      }
      else if (UserInterface.class.equals(adapter)) {
         return getUI();
      }
      else if (IProofProvider.class.equals(adapter)) {
         return this;
      }
      else {
         return super.getAdapter(adapter);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public KeYEnvironment<CustomConsoleUserInterface> getEnvironment() {
      return environment;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public CustomConsoleUserInterface getUI() {
      KeYEnvironment<CustomConsoleUserInterface> environment = getEnvironment();
      return environment != null ? environment.getUi() : null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public KeYMediator getMediator() {
      KeYEnvironment<CustomConsoleUserInterface> environment = getEnvironment();
      return environment != null ? environment.getMediator() : null;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public Proof getCurrentProof() {
      return currentProof;
   }

   @Override
   public Proof[] getCurrentProofs() {
      Proof proof = getCurrentProof();
      return proof != null ? new Proof[] {proof} : new Proof[0];
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void addProofProviderListener(IProofProviderListener l) {
      if (l != null) {
         proofProviderListener.add(l);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeProofProviderListener(IProofProviderListener l) {
      if (l != null) {
         proofProviderListener.remove(l);
      }
   }
   
   /**
    * Informs all registered {@link IProofProviderListener} about the event.
    * @param e The {@link ProofProviderEvent}.
    */
   protected void fireCurrentProofsChanged(ProofProviderEvent e) {
      IProofProviderListener[] toInform = proofProviderListener.toArray(new IProofProviderListener[proofProviderListener.size()]);
      for (IProofProviderListener l : toInform) {
         l.currentProofsChanged(e);
      }
   }
}
