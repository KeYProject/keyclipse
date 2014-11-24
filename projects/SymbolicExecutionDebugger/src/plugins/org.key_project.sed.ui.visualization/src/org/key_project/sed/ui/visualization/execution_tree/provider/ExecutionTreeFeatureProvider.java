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

package org.key_project.sed.ui.visualization.execution_tree.provider;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddBendpointFeature;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveAnchorFeature;
import org.eclipse.graphiti.features.IMoveBendpointFeature;
import org.eclipse.graphiti.features.IMoveConnectionDecoratorFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IPasteFeature;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.IRemoveBendpointFeature;
import org.eclipse.graphiti.features.IRemoveFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAddBendpointContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.context.ILayoutContext;
import org.eclipse.graphiti.features.context.IMoveAnchorContext;
import org.eclipse.graphiti.features.context.IMoveBendpointContext;
import org.eclipse.graphiti.features.context.IMoveConnectionDecoratorContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IPasteContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.IReconnectionContext;
import org.eclipse.graphiti.features.context.IRemoveBendpointContext;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.features.DefaultFeatureProvider;
import org.key_project.sed.core.model.ISEDBranchCondition;
import org.key_project.sed.core.model.ISEDBranchStatement;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.ISEDExceptionalMethodReturn;
import org.key_project.sed.core.model.ISEDExceptionalTermination;
import org.key_project.sed.core.model.ISEDLoopBodyTermination;
import org.key_project.sed.core.model.ISEDLoopCondition;
import org.key_project.sed.core.model.ISEDLoopInvariant;
import org.key_project.sed.core.model.ISEDLoopStatement;
import org.key_project.sed.core.model.ISEDMethodCall;
import org.key_project.sed.core.model.ISEDMethodContract;
import org.key_project.sed.core.model.ISEDMethodReturn;
import org.key_project.sed.core.model.ISEDStatement;
import org.key_project.sed.core.model.ISEDTermination;
import org.key_project.sed.core.model.ISEDThread;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchConditionAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchConditionCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchConditionLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchConditionUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchStatementAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchStatementCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchStatementLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.BranchStatementUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.DebugTargetUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalMethodReturnAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalMethodReturnCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalMethodReturnLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalMethodReturnUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalTerminationAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalTerminationCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalTerminationLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExceptionalTerminationUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExecutionTreeDeleteFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ExecutionTreeRemoveFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopBodyTerminationAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopBodyTerminationCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopBodyTerminationLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopBodyTerminationUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopConditionAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopConditionCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopConditionLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopConditionUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopInvariantAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopInvariantCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopInvariantLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopInvariantUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopStatementAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopStatementCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopStatementLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.LoopStatementUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodCallAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodCallCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodCallLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodCallUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodContractAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodContractCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodContractLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodContractUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodReturnAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodReturnCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodReturnLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.MethodReturnUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.StatementAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.StatementCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.StatementLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.StatementUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.TerminationAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.TerminationCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.TerminationLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.TerminationUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ThreadAddFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ThreadCreateFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ThreadLayoutFeature;
import org.key_project.sed.ui.visualization.execution_tree.feature.ThreadUpdateFeature;
import org.key_project.sed.ui.visualization.execution_tree.service.SEDIndependenceSolver;

/**
 * {@link IFeatureProvider} specific implementation for execution tree diagrams.
 * @author Martin Hentschel
 */
public class ExecutionTreeFeatureProvider extends DefaultFeatureProvider {
   /**
    * Indicates that the diagram is read-only or editable.
    */
   private boolean readOnly = false;
   
   /**
    * Constructor.
    * @param dtp The diagram type provider for that this {@link IFeatureProvider} is used.
    */
   public ExecutionTreeFeatureProvider(IDiagramTypeProvider dtp) {
      super(dtp);
      setIndependenceSolver(new SEDIndependenceSolver());
   }

   /**
    * Returns the used {@link SEDIndependenceSolver}.
    * @return The used {@link SEDIndependenceSolver}.
    */
   public SEDIndependenceSolver getSEDIndependenceSolver() {
      return (SEDIndependenceSolver)getIndependenceSolver();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ICreateFeature[] getCreateFeatures() {
      if (!isReadOnly()) {
         return new ICreateFeature[] {new BranchConditionCreateFeature(this),
                                      new BranchStatementCreateFeature(this),
                                      new ExceptionalTerminationCreateFeature(this),
                                      new LoopBodyTerminationCreateFeature(this),
                                      new LoopConditionCreateFeature(this),
                                      new LoopStatementCreateFeature(this),
                                      new MethodCallCreateFeature(this),
                                      new MethodReturnCreateFeature(this),
                                      new ExceptionalMethodReturnCreateFeature(this),
                                      new StatementCreateFeature(this),
                                      new TerminationCreateFeature(this),
                                      new ThreadCreateFeature(this),
                                      new LoopInvariantCreateFeature(this),
                                      new MethodContractCreateFeature(this)};
      }
      else {
         return new ICreateFeature[0];
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IAddFeature getAddFeature(IAddContext context) {
      if (context.getNewObject() instanceof ISEDBranchCondition) {
         return new BranchConditionAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDBranchStatement) {
         return new BranchStatementAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDExceptionalTermination) {
         return new ExceptionalTerminationAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDLoopBodyTermination) {
         return new LoopBodyTerminationAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDLoopCondition) {
         return new LoopConditionAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDLoopStatement) {
         return new LoopStatementAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDMethodCall) {
         return new MethodCallAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDMethodReturn) {
         return new MethodReturnAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDExceptionalMethodReturn) {
         return new ExceptionalMethodReturnAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDStatement) {
         return new StatementAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDTermination) {
         return new TerminationAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDThread) {
         return new ThreadAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDMethodContract) {
         return new MethodContractAddFeature(this);
      }
      else if (context.getNewObject() instanceof ISEDLoopInvariant) {
         return new LoopInvariantAddFeature(this);
      }
      else {
         return super.getAddFeature(context);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IUpdateFeature getUpdateFeature(IUpdateContext context) {
      Object bo = getBusinessObjectForPictogramElement(context.getPictogramElement());
      if (bo instanceof ISEDDebugTarget) {
         return new DebugTargetUpdateFeature(this);
      }
      else if (bo instanceof ISEDBranchCondition) {
         return new BranchConditionUpdateFeature(this);
      }
      else if (bo instanceof ISEDBranchStatement) {
         return new BranchStatementUpdateFeature(this);
      }
      else if (bo instanceof ISEDExceptionalTermination) {
         return new ExceptionalTerminationUpdateFeature(this);
      }
      else if (bo instanceof ISEDLoopBodyTermination) {
         return new LoopBodyTerminationUpdateFeature(this);
      }
      else if (bo instanceof ISEDLoopCondition) {
         return new LoopConditionUpdateFeature(this);
      }
      else if (bo instanceof ISEDLoopStatement) {
         return new LoopStatementUpdateFeature(this);
      }
      else if (bo instanceof ISEDMethodCall) {
         return new MethodCallUpdateFeature(this);
      }
      else if (bo instanceof ISEDMethodReturn) {
         return new MethodReturnUpdateFeature(this);
      }
      else if (bo instanceof ISEDExceptionalMethodReturn) {
         return new ExceptionalMethodReturnUpdateFeature(this);
      }
      else if (bo instanceof ISEDStatement) {
         return new StatementUpdateFeature(this);
      }
      else if (bo instanceof ISEDTermination) {
         return new TerminationUpdateFeature(this);
      }
      else if (bo instanceof ISEDThread) {
         return new ThreadUpdateFeature(this);
      }
      else if (bo instanceof ISEDMethodContract) {
         return new MethodContractUpdateFeature(this);
      }
      else if (bo instanceof ISEDLoopInvariant) {
         return new LoopInvariantUpdateFeature(this);
      }
      else {
         return super.getUpdateFeature(context);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public ILayoutFeature getLayoutFeature(ILayoutContext context) {
      PictogramElement pictogramElement = context.getPictogramElement();
      Object bo = getBusinessObjectForPictogramElement(pictogramElement);
      if (bo instanceof ISEDBranchCondition) {
          return new BranchConditionLayoutFeature(this);
      }
      else if (bo instanceof ISEDBranchStatement) {
         return new BranchStatementLayoutFeature(this);
      }
      else if (bo instanceof ISEDExceptionalTermination) {
         return new ExceptionalTerminationLayoutFeature(this);
      }
      else if (bo instanceof ISEDLoopCondition) {
         return new LoopConditionLayoutFeature(this);
      }
      else if (bo instanceof ISEDLoopBodyTermination) {
         return new LoopBodyTerminationLayoutFeature(this);
      }
      else if (bo instanceof ISEDLoopStatement) {
         return new LoopStatementLayoutFeature(this);
      }
      else if (bo instanceof ISEDMethodCall) {
         return new MethodCallLayoutFeature(this);
      }
      else if (bo instanceof ISEDMethodReturn) {
         return new MethodReturnLayoutFeature(this);
      }
      else if (bo instanceof ISEDExceptionalMethodReturn) {
         return new ExceptionalMethodReturnLayoutFeature(this);
      }
      else if (bo instanceof ISEDStatement) {
         return new StatementLayoutFeature(this);
      }
      else if (bo instanceof ISEDTermination) {
         return new TerminationLayoutFeature(this);
      }
      else if (bo instanceof ISEDThread) {
         return new ThreadLayoutFeature(this);
      }
      else if (bo instanceof ISEDMethodContract) {
         return new MethodContractLayoutFeature(this);
      }
      else if (bo instanceof ISEDLoopInvariant) {
         return new LoopInvariantLayoutFeature(this);
      }
      else {
         return super.getLayoutFeature(context);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public IDeleteFeature getDeleteFeature(IDeleteContext context) {
      if (!isReadOnly()) {
         return new ExecutionTreeDeleteFeature(this);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IRemoveFeature getRemoveFeature(IRemoveContext context) {
      if (!isReadOnly()) {
         return getRemoveFeatureIgnoreReadonlyState(context);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the {@link IRemoveFeature} for the given {@link IRemoveContext}
    * ignoring the read-only state ({@link #isReadOnly()}).
    * @param removeContext The {@link IRemoveContext} for that an {@link IRemoveFeature} is requested.
    * @return The {@link IRemoveFeature} to use or {@code null} if no {@link IRemoveFeature} is available.
    */
   public IRemoveFeature getRemoveFeatureIgnoreReadonlyState(IRemoveContext removeContext) {
      return new ExecutionTreeRemoveFeature(this);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public IAddBendpointFeature getAddBendpointFeature(IAddBendpointContext context) {
      if (!isReadOnly()) {
         return super.getAddBendpointFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ICustomFeature[] getCustomFeatures(ICustomContext context) {
      if (!isReadOnly()) {
         return super.getCustomFeatures(context);
      }
      else {
         return new ICustomFeature[0];
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IMoveAnchorFeature getMoveAnchorFeature(IMoveAnchorContext context) {
      if (!isReadOnly()) {
         return super.getMoveAnchorFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IMoveBendpointFeature getMoveBendpointFeature(IMoveBendpointContext context) {
      if (!isReadOnly()) {
         return super.getMoveBendpointFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IMoveConnectionDecoratorFeature getMoveConnectionDecoratorFeature(IMoveConnectionDecoratorContext context) {
      if (!isReadOnly()) {
         return super.getMoveConnectionDecoratorFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IMoveShapeFeature getMoveShapeFeature(IMoveShapeContext context) {
      if (!isReadOnly()) {
         return super.getMoveShapeFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IPasteFeature getPasteFeature(IPasteContext context) {
      if (!isReadOnly()) {
         return super.getPasteFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IRemoveBendpointFeature getRemoveBendpointFeature(IRemoveBendpointContext context) {
      if (!isReadOnly()) {
         return super.getRemoveBendpointFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IResizeShapeFeature getResizeShapeFeature(IResizeShapeContext context) {
      if (!isReadOnly()) {
         return super.getResizeShapeFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ICreateConnectionFeature[] getCreateConnectionFeatures() {
      if (!isReadOnly()) {
         return super.getCreateConnectionFeatures();
      }
      else {
         return new ICreateConnectionFeature[0];
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IDirectEditingFeature getDirectEditingFeature(IDirectEditingContext context) {
      if (!isReadOnly()) {
         return super.getDirectEditingFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IReconnectionFeature getReconnectionFeature(IReconnectionContext context) {
      if (!isReadOnly()) {
         return super.getReconnectionFeature(context);
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IFeature[] getDragAndDropFeatures(IPictogramElementContext context) {
      if (!isReadOnly()) {
         return super.getDragAndDropFeatures(context);
      }
      else {
         return new IFeature[0];
      }
   }

   /**
    * Checks if the diagram is read-only or editable.
    * @return {@code true} read-only, {@code false} editable.
    */
   public boolean isReadOnly() {
      return readOnly;
   }

   /**
    * Defines if the diagram is read-only or editable.
    * @param readOnly {@code true} read-only, {@code false} editable.
    */
   public void setReadOnly(boolean readOnly) {
      this.readOnly = readOnly;
   }
}