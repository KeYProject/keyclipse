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

package org.key_project.sed.ui.visualization.util;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.internal.command.ICommand;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.editor.DiagramEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.key_project.util.eclipse.job.AbstractDependingOnObjectsJob;
import org.key_project.util.eclipse.swt.SWTUtil;
import org.key_project.util.eclipse.view.editorInView.AbstractEditorInViewView;
import org.key_project.util.eclipse.view.editorInView.GlobalEnablementWrapperAction;
import org.key_project.util.eclipse.view.editorInView.IGlobalEnablement;
import org.key_project.util.java.CollectionUtil;

/**
 * <p>
 * Extended {@link DiagramEditor} which allows to hide the palette.
 * </p>
 * <p>
 * This class allows also to execute {@link IFeature} and Graphiti {@link ICommand}
 * instances via {@link #executeFeature(IFeature, IContext)} and
 * {@link #executeCommand(ICommand)}.
 * </p>
 * <p>
 * This editor realizes also the {@link IGlobalEnablement} which is required
 * for the usage in an {@link AbstractEditorInViewView} to disable keyboard
 * shortcuts when a message is shown. This is possible, because keyboard
 * shortcuts registered via {@link #registerAction(IAction)} are wrapped
 * with a {@link GlobalEnablementWrapperAction} which is registered in
 * {@link #childGlobalEnablements}. The global enabled state of this editor
 * is shared with all contained child {@link IGlobalEnablement}s. Other
 * {@link IGlobalEnablement} can be registered in sub classes via 
 * {@link #registerGlobalEnablement(IGlobalEnablement)}.
 * </p>
 * @author Martin Hentschel
 */
@SuppressWarnings("restriction")
public class PaletteHideableDiagramEditor extends DiagramEditor implements IGlobalEnablement {   
   /**
    * The global enablement state which is shared with child {@link IGlobalEnablement} ({@link #childGlobalEnablements}).
    */
   private boolean globalEnabled;
   
   /**
    * Contains child {@link IGlobalEnablement} which have always the same global enablement state.
    */
   private List<IGlobalEnablement> childGlobalEnablements = new LinkedList<IGlobalEnablement>();

   /**
    * Is the default selection synchronization (e.g. with debug view) enabled?
    */
   private boolean defaultSelectionSynchronizationEnabled = true;
   
   /**
    * Constructor.
    */
   public PaletteHideableDiagramEditor() {
      setActionRegistry(new ActionRegistry() {
         @Override
         public void registerAction(IAction action) {
            if (action instanceof IGlobalEnablement) {
               registerGlobalEnablement((IGlobalEnablement)action);
               super.registerAction(action);
            }
            else {
               GlobalEnablementWrapperAction wrapper = new GlobalEnablementWrapperAction(action); // Required to disable keyboard shortcuts if a message is shown.
               registerGlobalEnablement(wrapper);
               super.registerAction(wrapper);
            }
         }
      });
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void dispose() {
      for (IGlobalEnablement child : childGlobalEnablements) {
         child.dispose();
      }
      childGlobalEnablements.clear();
      if (getEditingDomain() != null) { // Check if already disposed
         super.dispose();
      }
   }
   
   /**
    * Executes the given {@link IFeature} with the given {@link IContext}.
    * @param feature The {@link IFeature} to execute.
    * @param context the {@link IContext} to use.
    */
   public void executeFeatureInJob(String jobName, 
                                   final IFeature feature, 
                                   final IContext context) {
      new AbstractDependingOnObjectsJob(jobName, this, PlatformUI.getWorkbench()) {
         @Override
         protected IStatus run(IProgressMonitor monitor) {
            try {
               SWTUtil.checkCanceled(monitor);
               context.putProperty(GraphitiUtil.CONTEXT_PROPERTY_MONITOR, monitor);
               getDiagramBehavior().executeFeature(feature, context);
               return Status.OK_STATUS;
            }  
            catch (OperationCanceledException e) {
               return Status.CANCEL_STATUS;
            }
            catch (Exception e) {
               return LogUtil.getLogger().createErrorStatus(e);
            }
         }
      }.schedule();
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void selectionChanged(IWorkbenchPart part, ISelection selection) {
      if (isDefaultSelectionSynchronizationEnabled()) {
         // Synchronize for instance selection with debug view
         super.selectionChanged(part, selection);
      }
      else {
         // Code from GraphicalEditor#selectionChanged(...)
         if (this.equals(getSite().getPage().getActiveEditor())) {
            updateActions(getSelectionActions());
         }
      }
   }

   /**
    * Selects all {@link PictogramElement}s of the given business objects.
    * @param businessObjects The business objects to select their {@link PictogramElement}s.
    */
   public void selectBusinessObjects(Object[] businessObjects) {
      List<PictogramElement> pictogramElements = new LinkedList<PictogramElement>();
      for (Object bo : businessObjects) {
         PictogramElement[] referencingPes = getPictogramElements(bo);
         CollectionUtil.addAll(pictogramElements, referencingPes);
      }
      selectPictogramElements(pictogramElements.toArray(new PictogramElement[pictogramElements.size()]));
   }
   
   /**
    * <p>
    * Returns all {@link PictogramElement}s for the given business object.
    * </p>
    * <p>
    * The implementation is influenced by the selection synchronization of
    * {@link DiagramEditorInternal#selectionChanged(IWorkbenchPart, ISelection)}.
    * </p>
    * @param bo The given business object for that {@link PictogramElement}s are needed.
    * @return The found {@link PictogramElement}s.
    */
   protected PictogramElement[] getPictogramElements(Object bo) {
      if (bo instanceof EObject) {
         // Find the Pictogram Elements for the given domain object via the standard link service
         List<PictogramElement> referencingPes = Graphiti.getLinkService().getPictogramElements(getDiagramTypeProvider().getDiagram(), (EObject) bo);
         return referencingPes.toArray(new PictogramElement[referencingPes.size()]);
      }
      else {
         // For non-EMF domain objects use the registered notification service for finding
         return getDiagramTypeProvider().getNotificationService().calculateRelatedPictogramElements(new Object[] { bo });
      }
   }
   
   /**
    * Checks if the grid is visible.
    * @return {@code true} grid is visible, {@code false} grid is hidden.
    */
   public boolean isGridVisible() {
      Object prop = getGraphicalViewer().getProperty(SnapToGrid.PROPERTY_GRID_VISIBLE);
      return prop instanceof Boolean && ((Boolean)prop).booleanValue();
   }
   
   /**
    * Sets the grid visible or hides it.
    * @param showGrid {@code true} show grid, {@code false} hide grid.
    */
   public void setGridVisible(boolean showGrid) {
      IAction action = getActionRegistry().getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY);
      if (action != null) {
         action.run();
      }
   }
   
   /**
    * Checks if the default selection synchronization (e.g. with debug view) is enabled.
    * @return {@code true} do default synchronization, {@code false} don't do synchronization with other workbench parts.
    */
   public boolean isDefaultSelectionSynchronizationEnabled() {
      return defaultSelectionSynchronizationEnabled;
   }

   /**
    * Defines if the default selection synchronization (e.g. with debug view) is enabled.
    * @param defaultSelectionSynchronizationEnabled {@code true} do default synchronization, {@code false} don't do synchronization with other workbench parts.
    */
   public void setDefaultSelectionSynchronizationEnabled(boolean defaultSelectionSynchronizationEnabled) {
      this.defaultSelectionSynchronizationEnabled = defaultSelectionSynchronizationEnabled;
   }
   
   /**
    * Registers the new child {@link IGlobalEnablement} in {@link #childGlobalEnablements}
    * and sets the global enablement state on it to the state of this {@link IGlobalEnablement}.
    * @param globalEnablement The child {@link IGlobalEnablement} to register.
    */
   public void registerGlobalEnablement(IGlobalEnablement globalEnablement) {
      childGlobalEnablements.add(globalEnablement);
      globalEnablement.setGlobalEnabled(isGlobalEnabled());
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isGlobalEnabled() {
      return globalEnabled;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setGlobalEnabled(boolean globalEnabled) {
      this.globalEnabled = globalEnabled;
      for (IGlobalEnablement child : childGlobalEnablements) {
         child.setGlobalEnabled(globalEnabled);
      }
   }
}