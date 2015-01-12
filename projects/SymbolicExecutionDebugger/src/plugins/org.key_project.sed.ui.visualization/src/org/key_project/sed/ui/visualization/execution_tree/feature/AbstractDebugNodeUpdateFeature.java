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

package org.key_project.sed.ui.visualization.execution_tree.feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.features.context.impl.AreaContext;
import org.eclipse.graphiti.features.context.impl.LayoutContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.key_project.sed.core.annotation.ISEDAnnotation;
import org.key_project.sed.core.model.ISEDBranchCondition;
import org.key_project.sed.core.model.ISEDDebugElement;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.ISEDGroupable;
import org.key_project.sed.core.model.ISEDThread;
import org.key_project.sed.core.util.ISEDIterator;
import org.key_project.sed.core.util.NodeUtil;
import org.key_project.sed.core.util.SEDGroupPreorderIterator;
import org.key_project.sed.core.util.SEDPreorderIterator;
import org.key_project.sed.ui.visualization.execution_tree.util.ExecutionTreeStyleUtil;
import org.key_project.sed.ui.visualization.util.GraphitiUtil;
import org.key_project.sed.ui.visualization.util.LogUtil;
import org.key_project.util.java.ArrayUtil;
import org.key_project.util.java.CollectionUtil;
import org.key_project.util.java.IFilterWithException;
import org.key_project.util.java.StringUtil;

/**
 * <p>
 * Provides a basic implementation of {@link IUpdateFeature} for {@link ISEDDebugNode}s.
 * </p>
 * </p>
 * A subtree is constructed as follows during execution of {@link #update(IUpdateContext)}
 * 
 * <ol>
 *    <li>Update label of current node via {@link #updateName(PictogramElement, IProgressMonitor)} </li>
 *    <li>
 *       Update sub tree via {@link #updateChildren(PictogramElement, IProgressMonitor)}
 *       <ol>
 *          <li>
 *             Add missing graphical representations in a tree where each branch is left centered.
 *             Result is a list of leaf nodes computed via {@link #updateChildrenLeftAligned(ISEDDebugElement, IProgressMonitor, int)}
 *             <ol>
 *                <li>Iterate over subtree in order.</li>
 *                <li>First branch (ends in first leaf node) is completely left centered with x = 0.</li>
 *                <li>
 *                   If a further branch is detected, the maximal width of the previous 
 *                   branch is computed via {@link #computeSubTreeBounds(ISEDDebugNode)}
 *                   and the x coordinate is the maximal bound (x + width) + a given offset of two grid units.
 *                </li>
 *             </ol>
 *          </li>
 *          <li>
 *             Center whole sub tree starting from its branches leaf nodes via {@link #centerChildren(Set, IProgressMonitor)}.
 *             <ol>
 *                <li>Iterate over all given leaf nodes. (Start with the found one via {@link #updateChildrenLeftAligned(ISEDDebugElement, IProgressMonitor, int)} and continue with nodes which children are completly centered)</li>
 *                <li>
 *                   If leaf node has children (added during step 4) compute x offset to center branch under his children.
 *                </li>
 *                <li>
 *                   Go back to parents until root is reached (parent is {@code null} or multiple children are detected.
 *                   During backward iteration collect maximal width of the elements.
 *                </li>
 *                <li>
 *                   If the iteration stopped because the parent has multiple children,
 *                   at the parent to leaf node to layout it later on same way. 
 *                </li>
 *                <li>
 *                   Go back to starting child (leaf node) and center each element with the computed maximal width.
 *                </li>
 *                <li>
 *                   If parents maximal width is greater than the maximal width of the children move the children again to the right to center them.
 *                </li>
 *             </ol>
 *          </li>
 *          <li>
 *             Move righter branches if the width of a modified branch was expanded via {@link #updateParents(PictogramElement, IProgressMonitor)}.
 *             <ol>
 *                <li>Find most left node via {@link #findMostLeftSiblingPE(ISEDDebugNode)}</li>
 *                <li>Compute distance to move as most right node of branch + offset - most left sibling</li>
 *                <li>Move all righter nodes via {@link #moveRighterNodes(ISEDDebugNode, int, IProgressMonitor)}</li>
 *             </ol>
 *          </li>
 *       </ol>
 *    </li>
 * </ol>
 * <p>
 * @author Martin Hentschel
 */
public abstract class AbstractDebugNodeUpdateFeature extends AbstractUpdateFeature {
   /**
    * Key used in {@link UpdateContext#getProperty(Object)} which specifies that the style has to be updated. 
    * The value is an instance of {@link Boolean}.
    */
   public static final String KEY_UPDATE_STYLE = "updateStyle";
   
   /**
    * Key used in {@link UpdateContext#getProperty(Object)} to specify the changed {@link ISEDDebugNode}
    * for which the style of its {@link PictogramElement} has to be updated.
    * The value is an instance of {@link ISEDDebugNode}.
    */
   public static final String KEY_SED_NODE = "sedNode";
   
   /**
    * The maximal x coordinate which is used by the previous
    * {@link ISEDDebugTarget} in {@link #updateChildren(PictogramElement, IProgressMonitor)}.
    */
   private int maxX;
   
   /**
    * The OFFSET between two nodes
    */
   protected final int OFFSET = getDiagram().getGridUnit() * 2;
   
   /**
    * The OFFSET between the Rect of a Method an the Methodnodes
    */
   protected final int METOFF = getDiagram().getGridUnit();
   
   /**
    * Determines if updated is called by expand or not
    */
   private boolean calledByExpand = false;
   
   /**
    * Constructor.
    * @param fp The {@link IFeatureProvider} which provides this {@link IUpdateFeature}.
    */   
   public AbstractDebugNodeUpdateFeature(IFeatureProvider fp) {
      super(fp);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean canUpdate(IUpdateContext context) {
      Object updateStyle = context.getProperty(KEY_UPDATE_STYLE);
      if (updateStyle instanceof Boolean && ((Boolean)updateStyle).booleanValue()) {
         return context.getPictogramElement() != null;
      }
      else {
         Object bo = getBusinessObjectForPictogramElement(context.getPictogramElement());
         return canUpdateBusinessObject(bo);
      }
   }
   
   /**
    * Checks if the give business object can be handled by this {@link IUpdateFeature}.
    * @param businessObject The business object to check.
    * @return {@code true} can update, {@code false} can not update.
    */
   protected abstract boolean canUpdateBusinessObject(Object businessObject);

   /**
    * {@inheritDoc}
    */
   @Override
   public IReason updateNeeded(IUpdateContext context) {
      Object updateStyle = context.getProperty(KEY_UPDATE_STYLE);
      if (updateStyle instanceof Boolean && ((Boolean)updateStyle).booleanValue()) {
         return Reason.createTrueReason("Style is out of date.");
      }
      else {
         try {
            PictogramElement pe = context.getPictogramElement();
            Object bo = getBusinessObjectForPictogramElement(pe);
            
            if(NodeUtil.canBeGrouped(bo)) {
               ISEDGroupable groupStart = (ISEDGroupable) bo;
               if(pe.getGraphicsAlgorithm() instanceof org.eclipse.graphiti.mm.algorithms.Rectangle || groupStart.isCollapsed()) {
                  return Reason.createFalseReason();
               }
            }

            if (isNameUpdateNeeded(pe)) {
               return Reason.createTrueReason("Name is out of date.");
            }
            else {
               if (isChildrenUpdateNeeded(pe)) {
                  return Reason.createTrueReason("New children available.");
               }
               else {
                  return Reason.createFalseReason();
               }
            }
         }
         catch (DebugException e) {
            LogUtil.getLogger().logError(e);
            return Reason.createFalseReason(e.getMessage());
         }
      }
   }
   
   /**
    * Checks if the shown name in the given {@link PictogramElement}
    * is equal to the name defined by his business object 
    * ({@link ISEDDebugNode#getName()}).
    * @param pictogramElement The {@link PictogramElement} to check.
    * @return {@code true} name is different and an update is required, {@code false} name is the same and no update is required.
    * @throws DebugException Occurred Exception.
    */
   protected boolean isNameUpdateNeeded(PictogramElement pictogramElement) throws DebugException {
      Text text = findNameText(pictogramElement);
      if (text != null) {
         String pictogramName = text.getValue();
         String businessName = getBusinessName(pictogramElement);
         return !StringUtil.equalIgnoreWhiteSpace(businessName, pictogramName);
      }
      else {
         return false;
      }
   }
   
   /**
    * Checks if all child {@link ISEDDebugNode} of the {@link ISEDDebugNode}
    * which is the business object of the given {@link PictogramElement} have
    * a graphical representation. 
    * @param pictogramElement The {@link PictogramElement} to check.
    * @return {@code false} all children have graphical representation, {@code true} at least one child has no graphical representation.
    * @throws DebugException Occurred Exception
    */
   protected boolean isChildrenUpdateNeeded(PictogramElement pictogramElement) throws DebugException {
      return !haveAllBusinessObjectChildrenHaveGraphicalRepresentation(pictogramElement);
   }
   
   /**
    * Checks if all child {@link ISEDDebugNode} of the {@link ISEDDebugNode}
    * which is the business object of the given {@link PictogramElement} have
    * a graphical representation. 
    * @param pictogramElement The {@link PictogramElement} to check.
    * @return {@code true} all children have graphical representation, {@code false} at least one child has no graphical representation.
    * @throws DebugException Occurred Exception
    */
   protected boolean haveAllBusinessObjectChildrenHaveGraphicalRepresentation(PictogramElement pictogramElement) throws DebugException {
      Object bo = getBusinessObjectForPictogramElement(pictogramElement);
      boolean childrenHavePictogramElement = true;
      if (bo instanceof ISEDDebugNode) {
         ISEDDebugNode[] children = NodeUtil.getChildren((ISEDDebugNode)bo);
         int i = 0;
         while (childrenHavePictogramElement && i < children.length) {
            PictogramElement childPE = getPictogramElementForBusinessObject(children[i]);
            childrenHavePictogramElement = childPE != null;
            i++;
         }
      }
      return childrenHavePictogramElement;
   }

   /**
    * This method returns always the {@link PictogramElement} of the node.
    * @param bo The businessobject
    * @return The {@link PictogramElement} of the node.
    */
   protected PictogramElement getPictogramElementForBusinessObject(Object bo) {
      if(NodeUtil.canBeGrouped(bo)) {
         return getPictogramElementForBusinessObject(bo, 1);
      }
      
      return getPictogramElementForBusinessObject(bo, 0);
   }
   
   /**
    * If the given {@link Object} opens a group and i = 0, this method
    * will return the {@link PictogramElement} of the rect.
    * Otherwise it will return the {@link PictogramElement} of the node.
    * @param bo The BusinessObject to get the {@link PictogramElement} for. 
    * @param i The number for the {@link PictogramElement}. 0 if the BusinessObject
    * is groupable and the {@link PictogramElement} of the rect is needed. 1 otherwise. 
    * @return The specifiec {@link PictogramElement}.
    */
   protected PictogramElement getPictogramElementForBusinessObject(Object bo, int i) {
      if(i < 0 || i > 1)
         return null;
      
      if(i == 0)
         return getFeatureProvider().getPictogramElementForBusinessObject(bo);

      PictogramElement[] pes = getFeatureProvider().getAllPictogramElementsForBusinessObject(bo);
      return pes == null || pes.length < 1 ? null : pes[1];
   }
   
   /**
    * Returns the name defined by the business object of the given {@link PictogramElement}
    * which is {@link ISEDDebugNode#getName()}.
    * @param pictogramElement The {@link PictogramElement} for that the business name is needed.
    * @return The name defined by the business object of the given {@link PictogramElement}.
    * @throws DebugException The business name.
    */
   protected String getBusinessName(PictogramElement pictogramElement) throws DebugException {
      Object bo = getBusinessObjectForPictogramElement(pictogramElement);
      if (bo instanceof ISEDDebugNode) {
         return ((ISEDDebugNode)bo).getName();
      }
      else {
         return null;
      }
   }
   
   /**
    * Finds the {@link Text} which shows the name ({@link ISEDDebugNode#getName()}).
    * @param pictogramElement The {@link PictogramElement} to search the {@link Text} in.
    * @return The found {@link Text} or {@code null} if no one was found.
    */
   protected Text findNameText(PictogramElement pictogramElement) {
      Text result = null;
      if (pictogramElement.getGraphicsAlgorithm() instanceof Text) {
         result = (Text)pictogramElement.getGraphicsAlgorithm();
      }
      else if (pictogramElement instanceof ContainerShape && pictogramElement.getGraphicsAlgorithm() instanceof RoundedRectangle) {
         ContainerShape cs = (ContainerShape)pictogramElement;
         for (Shape shape : cs.getChildren()) {
            result = findNameText(shape);
         }
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean update(IUpdateContext context) {
      Object updateStyle = context.getProperty(KEY_UPDATE_STYLE);
      if (updateStyle instanceof Boolean && ((Boolean)updateStyle).booleanValue()) {
         Object nodeProp = context.getProperty(KEY_SED_NODE);
         ISEDDebugNode bo = nodeProp instanceof ISEDDebugNode ? (ISEDDebugNode)nodeProp : null;
         if (bo == null) {
            bo = (ISEDDebugNode)getFeatureProvider().getBusinessObjectForPictogramElement(context.getPictogramElement());
         }
         return updateStyle(context.getPictogramElement(), bo);
      }
      else {
         try {
            // Define monitor to use
            IProgressMonitor monitor = GraphitiUtil.getProgressMonitor(context);
            // Update name
            PictogramElement pictogramElement = context.getPictogramElement();

            monitor.beginTask("Update element: " + pictogramElement, 3);

            boolean success = updateName(pictogramElement, new SubProgressMonitor(monitor, 1));
            monitor.worked(1);

            // Update children, they have the correct layout after this step
            if (success) {
               success = updateChildren(pictogramElement, new SubProgressMonitor(monitor, 1));
            }
            monitor.worked(1);
            // Update parents, because children maybe have now a bigger width and overlap with other branches
            if (success) {
               success = updateParents(pictogramElement, new SubProgressMonitor(monitor, 1));
            }
            monitor.worked(1);
            // adjust the rects, because nodes may overlap them after the update
            if(success) {
               Object bo = getBusinessObjectForPictogramElement(pictogramElement);
               ISEDDebugNode node = bo instanceof ISEDDebugNode ? (ISEDDebugNode)bo : null;
               
               // needed for the reselect of the diagram
               if(node == null && bo instanceof ISEDDebugTarget)
               {
                  ISEDThread[] threads = ((ISEDDebugTarget) bo).getSymbolicThreads();
                  for(ISEDThread thread : threads) {
                     adjustRects(thread, monitor);
                  }
               }
               
               if(node != null) {
                  adjustRects(node, monitor);
               }
            }
            monitor.done();
            return success;
         }
         catch (DebugException e) {
            LogUtil.getLogger().logError(e);
            return false;
         }
      }
   }

   /**
    * Updates the shown name in the given {@link PictogramElement}.
    * @param pictogramElement The {@link PictogramElement} to update.
    * @param monitor The {@link IProgressMonitor} to use.
    * @return {@code true}, if update process was successful
    * @throws DebugException Occurred Exception.
    */
   protected boolean updateName(PictogramElement pictogramElement, 
                                IProgressMonitor monitor) throws DebugException {
      try {
         if (!monitor.isCanceled()) {
            // Set name in pictogram model
            monitor.beginTask("Update labels", 1);
            Text text = findNameText(pictogramElement);
            if (text != null) {
               // Change value
               String businessName = getBusinessName(pictogramElement);
               text.setValue(businessName);
               // Optimize layout
               LayoutContext layoutContext = new LayoutContext(pictogramElement);
               layoutContext.putProperty(AbstractDebugNodeLayoutFeature.WIDTH_TO_SET, AbstractDebugNodeAddFeature.computeInitialWidth(getDiagram(), businessName, text.getFont()));
               layoutContext.putProperty(AbstractDebugNodeLayoutFeature.HEIGHT_TO_SET, AbstractDebugNodeAddFeature.computeInitialHeight(getDiagram(), businessName, text.getFont()));
               
               getFeatureProvider().layoutIfPossible(layoutContext);
               // Add children
               return true;
            }
            else {
               return false;
            }
         }
         else {
            return false;
         }
      }
      finally {
         monitor.worked(1);
         monitor.done();
      }
   }
   
   /**
    * Updates the children of the {@link ISEDDebugNode} represented
    * by the given {@link PictogramElement}.
    * @param pictogramElement The {@link PictogramElement} to update.
    * @param monitor The {@link IProgressMonitor} to use.
    * @return {@code true}, if update process was successful
    * @throws DebugException Occurred Exception.
    */
   protected boolean updateChildren(PictogramElement pictogramElement, IProgressMonitor monitor) throws DebugException {
      monitor.beginTask("Update children", IProgressMonitor.UNKNOWN);
      maxX = 0;
      try {
         if (!monitor.isCanceled()) {
            Object[] bos = getAllBusinessObjectsForPictogramElement(pictogramElement);
            int i = 0;
            while (i < bos.length && !monitor.isCanceled()) {
               if (bos[i] instanceof ISEDDebugElement) {
                  // Add all children left aligned
                  Set<ISEDDebugNode> leafs = updateChildrenLeftAligned((ISEDDebugElement)bos[i], monitor, maxX);
                  maxX += OFFSET;
                  monitor.worked(1);

                  // Center sub tree
                  centerChildren(new HashSet<ISEDDebugNode>(leafs), monitor);
                  
                  if(calledByExpand) {
                     // re-center subtrees
                     for(ISEDDebugNode leaf : leafs) {
                        PictogramElement leafPE = getPictogramElementForBusinessObject(leaf);
                        if(leafPE != null) {
                           GraphicsAlgorithm leafGA = leafPE.getGraphicsAlgorithm();
                           int mostLeftSub = findInSubtree(leaf, true, false);
                           int mostRightSub = findInSubtree(leaf, false, false);
                           int toMove = leafGA.getX() - mostLeftSub - ((mostRightSub - mostLeftSub) - leafGA.getWidth()) / 2;
                           
                           moveSubTreeHorizontal(leaf, toMove, false, monitor);
                           moveRighterNodes(leaf, toMove, monitor);
                           updateParents(leafPE, monitor);
                        }
                     }
                  }
                  
                  // Check if we need a customized layout
                  // Check if we need to adjust the rects
                  if(bos[i] instanceof ISEDDebugNode) {
                     adjustSubtreeIfSmaller((ISEDDebugNode) bos[i], monitor);
                     adjustRects((ISEDDebugNode) bos[i], monitor);
                  }
                  // needed for the reselect of the diagram
                  else if(bos[i] instanceof ISEDDebugTarget)
                  {
                     ISEDThread[] threads = ((ISEDDebugTarget) bos[i]).getSymbolicThreads();
                     for(ISEDThread thread : threads) {
                        adjustRects(thread, monitor);
                     }
                  }
                  
                  // Adjustment for siblings are needed
                  if(calledByExpand) {
                     // re-center subtrees
                     for(ISEDDebugNode leaf : leafs) {
                        int mostLeftSub = findInSubtree(leaf, true, false);
                        int mostRightPrev = findInSiblingBranch(leaf, true, false);
                        if(mostRightPrev > -1 && mostRightPrev + OFFSET > mostLeftSub) {
                           int toMove = mostRightPrev + OFFSET - mostLeftSub; 
                           moveSubTreeHorizontal(leaf, toMove, true, monitor);
                           moveRightAndAbove(leaf, toMove, monitor);
                        }
                     }
                  }

                  monitor.worked(1);
               }
               i++;
            }
         }
         return true;
      }
      finally {
         monitor.done();
      }
   }

   /**
    * Creates for each element starting at the given business object
    * a graphical representation and forms a left aligned tree.
    * @param bo The business object to create graphical representations for.
    * @param monitor The {@link IProgressMonitor} to use.
    * @param initialX The initial X value which is used if no parentPE is defined.
    * @return The found leaf {@link ISEDDebugNode}s.
    * @throws DebugException Occurred Exception.
    */
   protected Set<ISEDDebugNode> updateChildrenLeftAligned(ISEDDebugElement bo, 
                                                          IProgressMonitor monitor,
                                                          int initialX) throws DebugException {
      Set<ISEDDebugNode> leafs = new LinkedHashSet<ISEDDebugNode>();
      ISEDIterator iter = new SEDPreorderIterator(bo);

      while (iter.hasNext() && !monitor.isCanceled()) {
         ISEDDebugElement next = iter.next();
         
         // Ignore the bo, because either it is ISEDDebugTarget (the very first bo)
         // which has no graphical representation or its a parentnode which
         // already has a graphical representation
         if(next == bo) {
            continue;
         }

         ISEDDebugNode nextNode = (ISEDDebugNode)next;
         PictogramElement nextPE = getPictogramElementForBusinessObject(next);
         if (nextPE == null) {          
            createGraphicalRepresentationForNode(nextNode, initialX);
            nextPE = getPictogramElementForBusinessObject(nextNode);
            if (nextPE != null) {
               // Update maxX to make sure that ISEDDebugTargets don't overlap each other.
               GraphicsAlgorithm nextGA = nextPE.getGraphicsAlgorithm();
               
               // If we add a new group we need to set the correct width of the rect 
               if(NodeUtil.canBeGrouped(nextNode)) {
                  GraphicsAlgorithm rectGA = getPictogramElementForBusinessObject(nextNode, 0).getGraphicsAlgorithm();
                  rectGA.setWidth(rectGA.getWidth() + 2 * METOFF);
               }

               if(nextGA.getX() + nextGA.getWidth() > maxX)
                  maxX = nextGA.getX() + nextGA.getWidth();
               
               // If a node in a group is added, the height of the parent group rect has to be checked
               if(NodeUtil.getGroupStartNode(nextNode) != null) {
                  updateGroupRectHeights(nextNode, monitor);
               }
            }
            
            if (ArrayUtil.isEmpty(NodeUtil.getChildren(nextNode))) {
               leafs.add(nextNode);
            }
         }
         // Handle expand (not needed for the basic add of new nodes), endnode reached         else if(!ArrayUtil.isEmpty(nextNode.getGroupStartConditions()) && !leafs.contains(nextNode)){
            // if we process the expand of a group we need to switch to the SEDGroupPreorderIterator
            if(iter instanceof SEDPreorderIterator && NodeUtil.canBeGrouped(bo)) {
               iter = new SEDGroupPreorderIterator((ISEDGroupable) bo, nextNode, false);
            }
            int mostLeftXAbove = findAbove(nextNode, true);            // Adjust the remaining endnodes and their subtrees as if there were just placed under their parents            moveSubTreeHorizontal(nextNode, mostLeftXAbove - nextPE.getGraphicsAlgorithm().getX(), true, monitor);                        int mostLeftSub = findInSubtree(nextNode, true, false);            int mostRightXInPrev = findInSiblingBranch(nextNode, true, false);                        // Since the subtree can now overlap branches on the left, adjust them again            if(mostRightXInPrev != -1 && mostRightXInPrev + OFFSET > mostLeftSub) {               moveSubTreeHorizontal(nextNode, mostRightXInPrev + OFFSET - mostLeftSub, true, monitor);            }
            
            // Use that last added node as leaf            leafs.add(NodeUtil.getParent(nextNode));
            calledByExpand = true;         }
         monitor.worked(1);
      }
      return leafs;
   }
   
   /**
    * Creates a new graphical representation for the given {@link ISEDDebugNode}.
    * @param node The {@link ISEDDebugNode} for that a graphical representation is needed.
    * @param initialX The initial X value which is used if no parentPE is defined.
    * @throws DebugException Occurred Exception.
    */
   protected void createGraphicalRepresentationForNode(ISEDDebugNode node,
                                                       int initialX) throws DebugException { 
      AreaContext areaContext = new AreaContext();
      ISEDDebugNode parent = NodeUtil.getParent(node);
      if(parent != null)
      {
         PictogramElement pe = getPictogramElementForBusinessObject(parent);
         if(pe == null) {
            // If auto-collapse is on, we need to create the BC first
            if(parent instanceof ISEDBranchCondition) {
               createGraphicalRepresentationForNode(parent, initialX);
               pe = getPictogramElementForBusinessObject(parent);
            }
            else {
               return;
            }
         }
         
         GraphicsAlgorithm parentGA = pe.getGraphicsAlgorithm();

         int areaX = -1;
         int areaY = parentGA.getY() + parentGA.getHeight() + OFFSET;
         
         ISEDDebugNode previousSibling = ArrayUtil.getPrevious(NodeUtil.getChildren(parent), node);
         
         // If we have a previous sibling, add the new node next to it
         if (previousSibling != null) {
            areaX = findInSubtree(previousSibling, false, true) + OFFSET;
         }
         
         // If we dont have previous siblings use the most left x from parents
         if(areaX == -1) {
            areaX = findAbove(node, true);
         }

         areaContext.setX(areaX);
         areaContext.setY(areaY);
      }
      else {
         areaContext.setLocation(initialX, getDiagram().getGridUnit());
      }

      AddContext addContext = new AddContext(areaContext, node);
      addContext.setTargetContainer(getDiagram());
      // Execute add feature manually because getFeatureProvider().addIfPossible(addContext) changes the selection
      IAddFeature feature = getFeatureProvider().getAddFeature(addContext);
      if (feature != null && feature.canExecute(addContext)) {
         feature.execute(addContext);
      }
   }
   
   /**
    * Centers all nodes starting from the given leaf nodes.
    * @param leafs All leaf nodes.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occurred Exception
    */
   protected void centerChildren(final Set<ISEDDebugNode> leafs, 
                                 IProgressMonitor monitor) throws DebugException {
      // Contains all already centered nodes
      final Set<ISEDDebugNode> doneNodes = new HashSet<ISEDDebugNode>();
      while (!leafs.isEmpty() && !monitor.isCanceled()) {
         // Get leaf to center which is the first one which children are already centered (all children are contained in doneNodes) or if no centering of the child is required (not part of leafs)
         final ISEDDebugNode next = CollectionUtil.searchAndRemoveWithException(leafs, new IFilterWithException<ISEDDebugNode, DebugException>() {
            @Override
            public boolean select(ISEDDebugNode element) throws DebugException {
               boolean allChildrenDone = true;
               ISEDDebugNode[] children = NodeUtil.getChildren(element);
               int i = 0;
               while (allChildrenDone && i < children.length) {
                  if (!doneNodes.contains(children[i]) && leafs.contains(children[i])) {
                     allChildrenDone = false;
                  }
                  i++;
               }
               return allChildrenDone;
            }
         }); 
         final PictogramElement nextPE = getPictogramElementForBusinessObject(next);
         final GraphicsAlgorithm nextGA = nextPE.getGraphicsAlgorithm();
         // Compute new x margin to center current branch under his children 
         int xMargin = 0;
         int xStart = nextGA.getX();
         boolean removeChildrenRequired = false;
         boolean isGroupStart = NodeUtil.canBeGrouped(next);

         // Go back to root or branch split and collect descendants while computing max width
         // If a parent node has more than one child it is treated as leaf node in a further iteration by adding it to leafs
         List<PictogramElement> descendantsPE = new LinkedList<PictogramElement>();
         int maxWidth = 0;
         ISEDDebugNode current = next;
         PictogramElement currentPE = nextPE;

         do {
            // Mark element as centered because it will be done 
            // before the next leaf node will be treated in outer most loop
            doneNodes.add(current);
            
            currentPE = getPictogramElementForBusinessObject(current); 
            descendantsPE.add(currentPE);
            
            int currentWidth = currentPE.getGraphicsAlgorithm().getWidth();

            if(NodeUtil.canBeGrouped(current) && 
                  (next == current && NodeUtil.getChildren(current).length < 2 || next != current && !isParentGroup(next, current))) {
               PictogramElement rectPE = getPictogramElementForBusinessObject(current , 0);
               currentWidth = rectPE.getGraphicsAlgorithm().getWidth();
               descendantsPE.add(rectPE);
            }

            if (currentWidth > maxWidth) {
               maxWidth = currentWidth;
               if(removeChildrenRequired)
                  xStart = currentPE.getGraphicsAlgorithm().getX();           
            }
            
            ISEDDebugNode child = current;
            current = NodeUtil.getParent(child);
            
            if(current != null) {
               ISEDDebugNode[] children = NodeUtil.getChildren(current);
               
               if (children.length != 1) {
                  // Update parent only if all of his subbranches are correctly centered
                  if(doneNodes.containsAll(new HashSet<ISEDDebugNode>(Arrays.asList(children)))){
                     leafs.add(current);
                  }
                  current = null;
               }
            }
         } while (current != null && !monitor.isCanceled());
         
         boolean subtreeShiftRequired = false;
         ISEDDebugNode[] children = NodeUtil.getChildren(next);
         if (!ArrayUtil.isEmpty(children) && children.length > 1)
         {            
            int subTreeWidth = findInSubtree(next, false, false) - findInSubtree(next, true, false);
            
            if(maxWidth <= subTreeWidth)
            {
               maxWidth = nextGA.getWidth();
               xMargin = calcXMargin(children, nextGA.getWidth());
               xStart = calcXStart(children);

               // Make sure that the new position is not more left than the old one
               // because this area is reserved for the previous branch and they should not overlap
               if (xMargin + xStart < nextGA.getX() && (!isGroupStart || !((ISEDGroupable) next).isCollapsed())) {
                  // Overlap possible, so keep old xStart
                  xMargin = 0;
                  xStart = nextGA.getX();
                  removeChildrenRequired = true;  
               }
            }
            else {
               subtreeShiftRequired = true;
               xStart = findInParents(next, true);
            }
         }
         
         // Center collected descendants based on the computed maximal element width
         Iterator<PictogramElement> descendantIter = descendantsPE.iterator();
         while (descendantIter.hasNext() && !monitor.isCanceled()) {
            PictogramElement pe = descendantIter.next();
            GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();

            ga.setX(xMargin + xStart + (maxWidth - ga.getWidth()) / 2);
         }
         
         if(subtreeShiftRequired) {
            int toMove = nextGA.getX() - calcXStart(children) - calcXMargin(children, nextGA.getWidth());
            moveSubTreeHorizontal(next, toMove, false, monitor);
            
            ISEDGroupable nextGroupStart = NodeUtil.getGroupStartNode(next);

            if(nextGroupStart != null) {
               resizeRectsIfNeeded(nextGroupStart, monitor);
            }
         }
         
         
         monitor.worked(1);

         // Center children again if required
         if (removeChildrenRequired && !ArrayUtil.isEmpty(NodeUtil.getChildren(next))) {
            int offset = nextGA.getX() - calcXStart(children) - calcXMargin(children, nextGA.getWidth());
            // Center children again only if offset is positive, because otherwise an overlap with the branch next to the left is possible
            if (offset > 0) {
               SEDPreorderIterator iter = new SEDPreorderIterator(next);
               while (iter.hasNext()) {
                  ISEDDebugElement nextChild = iter.next();
                  if (nextChild != next) {
                     PictogramElement nextChildPE = getPictogramElementForBusinessObject(nextChild);
                     if (nextChildPE != null) {
                        nextChildPE.getGraphicsAlgorithm().setX(nextChildPE.getGraphicsAlgorithm().getX() + offset);
                     }
                     
                     if(NodeUtil.canBeGrouped(nextChild)) {
                        nextChildPE = getPictogramElementForBusinessObject(nextChild, 0);
                        if (nextChildPE != null) {
                           nextChildPE.getGraphicsAlgorithm().setX(nextChildPE.getGraphicsAlgorithm().getX() + offset);
                        }
                     }
                  }
               }
            }
         }
      }
   }
   
   /**
    * Calculates the needed margin for the layout.
    * @param children The {@link ISEDDebugNode}s to calculate the margin to.
    * @param width The width of the parent node
    * @return The value of the margin.
    */
   protected int calcXMargin(ISEDDebugNode[] children, int width) {
      ISEDDebugNode firstChild = ArrayUtil.getFirst(children);
      ISEDDebugNode lastChild = ArrayUtil.getLast(children);
      PictogramElement firstChildPE = getPictogramElementForBusinessObject(firstChild);
      PictogramElement lastChildPE = getPictogramElementForBusinessObject(lastChild);
      int childWidth = lastChildPE.getGraphicsAlgorithm().getX() + lastChildPE.getGraphicsAlgorithm().getWidth() -
                       firstChildPE.getGraphicsAlgorithm().getX();
      return (childWidth - width) / 2;
   }
   
   /**
    * Calculates the needed x start position for the layout.
    * @param children The {@link ISEDDebugNode}s to calculate the start position to.
    * @return The value of the start position.
    */
   protected int calcXStart(ISEDDebugNode[] children) {
      PictogramElement firstChildPE = getPictogramElementForBusinessObject(ArrayUtil.getFirst(children));
      return firstChildPE.getGraphicsAlgorithm().getX();
   }
   
   /**
    * Checks if we have a bigger node in the upper branches. If that's the case
    * it will adjust the layout of the subtree if needed.
    * @param node The node to check.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occured exception.
    */
   protected void adjustSubtreeIfSmaller(ISEDDebugNode node, IProgressMonitor monitor) throws DebugException {
      int mostLeftPrevious = findInSiblingBranch(node, true, true);
      int mostLeftFollowing = findInSiblingBranch(node, false, true);

      boolean isLeft = hasSibling(node, true);
      
      if(mostLeftFollowing > -1 || mostLeftPrevious > -1)
      {
         int newChildrenSubtreeWidth = findInSubtree(node, false, false) - findInSubtree(node, true, false);
         int biggestWidth = findBiggestWidthInPartTreeAbove(node);
         
         // The new node/s is/are bigger than the current Branch
         if(newChildrenSubtreeWidth > biggestWidth)
         {
            ISEDDebugNode mostLeftNode = findBiggestNodeInParentBranches(node);
            GraphicsAlgorithm mlnGA = getPictogramElementForBusinessObject(mostLeftNode).getGraphicsAlgorithm();
            
            int mostLeftUnderBig = findInSubtree(ArrayUtil.getFirst(NodeUtil.getChildren(mostLeftNode)), true, true);
            
            if(mlnGA.getX() < mostLeftUnderBig) {
               // if the updated node is groupable we need to add an extra METOFF to
               // the subtree width for the space between the children and the right side of the rect
               int diff = (newChildrenSubtreeWidth + (NodeUtil.canBeGrouped(node) ? METOFF : 0) - biggestWidth) / 4;
               moveSmallSubtree(node, mostLeftNode, diff, isLeft, monitor);
               adjustRects(mostLeftNode, monitor);
            }
         }
      }
   }
   
   /**
    * Adjusts all nodes which overlap rects.
    * @param node The node to start with
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occured exception.
    */
   protected void adjustRects(ISEDDebugNode node, IProgressMonitor monitor) throws DebugException {
      monitor.beginTask("Adjust rectangles", IProgressMonitor.UNKNOWN);
      ISEDDebugNode startNode = NodeUtil.getGroupStartNode(node) != null ? (ISEDDebugNode) NodeUtil.getGroupStartNode(node) : node;
      ISEDIterator iter = new SEDPreorderIterator(startNode);
      while (iter.hasNext() && !monitor.isCanceled()) {
         ISEDDebugElement next = iter.next();
         
         if(next instanceof ISEDDebugNode) {            compute((ISEDDebugNode) next, monitor);
         }
         monitor.worked(1);
      }
      monitor.done();
   }
   
   /** TODO
    * Executes the adjustment of the nodes.
    * @param node The current {@link ISEDDebugNode} to adjust.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occured Exception.
    */
   private void compute(ISEDDebugNode node, IProgressMonitor monitor) throws DebugException {
      // Either the node or the rect if groupable
      PictogramElement pe = getPictogramElementForBusinessObject(node, 0);
      ISEDGroupable groupStart = NodeUtil.getGroupStartNode(node);
      
      // if the node has no graphical representation or is not in a group, return
      if(pe == null || groupStart == null) {
         return;
      }
      
      GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();

      PictogramElement groupStartPE = getPictogramElementForBusinessObject(groupStart, 0);
      GraphicsAlgorithm groupStartGA = groupStartPE.getGraphicsAlgorithm();
      
      // We only need to adjust something if the space between the new node and his group rect is smaller than the method offset 
      if(ga.getX() < groupStartGA.getX() + METOFF)
      {
         LinkedList<ISEDGroupable> groups = new LinkedList<ISEDGroupable>();
         ISEDGroupable group = groupStart;
         
         // At first we need to gather all rects we have to adjust
         while(group != null)
         {
            PictogramElement groupPE = getPictogramElementForBusinessObject(group, 0);

            if(groupPE != null) {
               GraphicsAlgorithm groupGA = groupPE.getGraphicsAlgorithm();
               
               // if the new node overlaps the method add it to the grouplist
               if(groupGA.getX() + METOFF > ga.getX()) {
                  groups.addFirst(group);
               }
               else {
                  break;
               }
            }
            
            group = NodeUtil.getGroupStartNode((ISEDDebugNode) group);
         }

         // The gathered rects will be processed from outer to inner
         for(int i = 0; i < groups.size(); i++) {
            int metoffAmount = groups.size() - i;
            
            groupStart = groups.get(i);
            ISEDGroupable outerGroup = NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart);
//            System.out.println("AG: " + groupStart);
            // if the node overlaps only the outer rect, only a 
            // shift to the right is needed
            // (if its possible that the outer rect is not the biggest width of
            // the complete tree, this lead in a slightly wrong layout)
            if(i == 0 && outerGroup == null) {
               int toMove = groupStartGA.getX() + METOFF - ga.getX();
               moveRightAndAbove(node, toMove, monitor);
               moveSubTreeHorizontal(node, toMove, true, monitor);
//               System.out.println("compute: :W");
               continue;
            }

            PictogramElement outerPE = getPictogramElementForBusinessObject(outerGroup, 0);
            GraphicsAlgorithm outerGA = outerPE.getGraphicsAlgorithm();
            groupStartPE = getPictogramElementForBusinessObject(groupStart, 0);
            groupStartGA = groupStartPE.getGraphicsAlgorithm();
            
            int mostRightInPrev = findInSiblingBranch(node, true, false);
            int toMove = groupStartGA.getX() + METOFF - ga.getX();
            
            // The group is overlapping the branch on the left
            if(mostRightInPrev > outerGA.getX() + METOFF && mostRightInPrev + OFFSET >= groupStartGA.getX()) {
               moveRightAndAbove(node, toMove, monitor);
               moveSubTreeHorizontal(node, toMove, true, monitor);
            }
            // if there is no previous branch or it is far enough away
            else {
               int checkRange = metoffAmount;
               int nearestXOnLeft = mostRightInPrev > outerGA.getX() + METOFF ? mostRightInPrev + OFFSET : outerGA.getX() + METOFF;
               
               while(nearestXOnLeft > groupStartGA.getX() - toMove - checkRange * METOFF) {
                  checkRange--;
               }
               
               // There is enough space to the outer rect, so it's possbile
               if(checkRange > 0) {
                  groupStartGA.setX(ga.getX() - (checkRange + 1) * METOFF);
//                  System.out.println("compute: D:");
               }
               // There is not enough space to the next outer rect
               else {
                  group = groupStart;
                  int diff = groupStartGA.getX() + METOFF - ga.getX();
                  boolean enoughSpace = false;

                  LinkedList<GraphicsAlgorithm> groups2 = new LinkedList<GraphicsAlgorithm>();
                  int mostRight = -1;
                  int outX = -1;
                  // At first we need to gather all rects we have to adjust
                  while(group != null)
                  {
                     ISEDGroupable outGroup = NodeUtil.getGroupStartNode((ISEDDebugNode) group);
                     
                     PictogramElement groupPE = getPictogramElementForBusinessObject(group, 0);
                     PictogramElement outGroupPE = getPictogramElementForBusinessObject(outGroup, 0);
                     
                     if(groupPE != null && outGroupPE != null) {
                        GraphicsAlgorithm groupGA = groupPE.getGraphicsAlgorithm();
                        GraphicsAlgorithm outGroupGA = outGroupPE.getGraphicsAlgorithm();
                        
                        // add all groups which fit into the space
                        groups2.addFirst(groupGA);
//                           System.out.println("Group: " + group);
                        // We have enough space to the left
                        if(outGroupGA.getX() + METOFF < groupGA.getX()) {
                           mostRight = findInSiblingBranch((ISEDDebugNode) group, true, false);
                           if(mostRight == -1 || mostRight + OFFSET <= outGroupGA.getX() + METOFF || mostRight + OFFSET < groupGA.getX()) {
                              enoughSpace = true;
                              outX = mostRight == -1 || mostRight + OFFSET <= outGroupGA.getX() + METOFF ? outGroupGA.getX() + METOFF : mostRight + OFFSET;
                           }
                           break;
                        }
                     }
                     group = outGroup;
                  }
                  
                  // if there is enough space, it's possible to move the groups
                  if(enoughSpace) {
                     toMove = groups2.getFirst().getX() - outX;
//                     if(toMove >= diff || !calledByExpand) {
                        for(GraphicsAlgorithm groupGA : groups2) {
                           groupGA.setX(groupGA.getX() - diff);
                        }
//                        System.out.println(node);
//                        System.out.println("compute: Na toll");
//                     }
//                     else {
////                        groups2.getFirst().setX(groups2.getFirst().getX() - toMove);
//                        System.out.println("2M: " + toMove + ", D: " + diff);
//                        for(GraphicsAlgorithm groupGA : groups2) {
//                           groupGA.setX(groupGA.getX() - toMove);
//                        }
//                        moveRightAndAbove(node, diff - toMove, monitor);
//                        moveSubTreeHorizontal(node, diff - toMove, true, monitor);
//                        System.out.println(node);
//                        System.out.println("compute: Na toll2: " + (diff - toMove));
//                        return;
//                     }
                  }
                  // if there is not enough space, but we have a prev branch
                  // we move the rects as much as we can to the left and
                  // move the nodes the remaining difference afterwards
                  else if(mostRight > -1) {
//                     GraphicsAlgorithm gga = getPictogramElementForBusinessObject(g).getGraphicsAlgorithm();
//                     System.out.println("G: " + g + ", GX: " + gga.getX());
                     toMove = groups2.getFirst().getX() - mostRight - OFFSET;
                     for(GraphicsAlgorithm groupGA : groups2) {
//                        System.out.println("RX: " + groupGA.getX());
//                        groupGA.setX(groupGA.getX() - toMove);
                     }
                     moveRightAndAbove(node, diff - toMove, monitor);
                     moveSubTreeHorizontal(node, diff - toMove, true, monitor);
//                     System.out.println("compute: :O1");
                  }
                  // if we dont have enough space and no prev branch
                  // the nodes have to move the complete difference
                  else {
                     moveRightAndAbove(node, diff, monitor);
                     moveSubTreeHorizontal(node, diff, true, monitor);
//                     System.out.println("compute: :O2");
                  }
               }
            }
         }
      }
      
      resizeRectsIfNeeded(groupStart, monitor);
      updateParents(groupStartPE, monitor);
   }

   /**
    * The subtree of the given {@link PictogramElement} may overlap
    * with other branches on the right side. This method moves all branches
    * right to the given {@link PictogramElement} to the right and re-centers
    * the parent nodes.
    * @param pictogramElement The {@link PictogramElement} which was updated.
    * @param monitor The {@link IProgressMonitor} to use.
    * @return {@code true}, if update process was successful
    * @throws DebugException Occurred Exception.
    */
   protected boolean updateParents(PictogramElement pictogramElement, 
                                   IProgressMonitor monitor) throws DebugException {
      monitor.beginTask("Update parents", IProgressMonitor.UNKNOWN);
      try {
         if (!monitor.isCanceled()) {
            Object[] bos = getAllBusinessObjectsForPictogramElement(pictogramElement);
            int i = 0;
            while (i < bos.length && !monitor.isCanceled()) {
               if (bos[i] instanceof ISEDDebugNode) {
                  ISEDDebugNode node = (ISEDDebugNode)bos[i];
                  ISEDDebugNode parent = NodeUtil.getParent(node);
                  if (parent != null) {
                     // Find most left node in righter nodes
                     int mostLeftFollowing = findInSiblingBranch(node, false, true);
                     if(mostLeftFollowing > - 1) {
                        // Compute maximal branch x and width
                        int maxXOnParents = findInParents(node, false);
                        int maxXInChildren = findInSubtree(node, false, true);
                        int maxXOfBranch = maxXOnParents > maxXInChildren ? maxXOnParents : maxXInChildren;
                        // Compute distance to move righter nodes
                        int distance = maxXOfBranch + OFFSET - mostLeftFollowing;
                        if (distance != 0) {
                           // Move righter nodes by the given distance
                           moveRighterNodes(node, distance, monitor);
                        }
                     }
                  }
               }
               i++;
            }
         }
         return true;
      }
      finally {
         monitor.done();
      }
   }
   
   /**
    * Updates des height of all rects which are affected by the given {@link ISEDDebugNode}.
    * @param node The node that updates the height.
    * @parem monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occured Exception.
    */
   protected void updateGroupRectHeights(ISEDDebugNode node, IProgressMonitor monitor) throws DebugException {
      GraphicsAlgorithm ga = getPictogramElementForBusinessObject(node).getGraphicsAlgorithm();
      
      ISEDGroupable groupStart = NodeUtil.getGroupStartNode(node);
      boolean isGroupEnd = node.getGroupStartCondition((ISEDDebugNode) groupStart) != null;
      
      int methodMaxY = ga.getY() + ga.getHeight() + (isGroupEnd ? -ga.getHeight() / 2 : OFFSET);

      do
      {
         GraphicsAlgorithm rectGA = getPictogramElementForBusinessObject(groupStart, 0).getGraphicsAlgorithm();
         ISEDBranchCondition[] bcs = groupStart.getGroupEndConditions();

         // Check if an exisiting groupend is already placed depper in the tree
         for(ISEDBranchCondition bc : bcs) {
            ISEDDebugNode groupEnd = bc.getChildren()[0];
            PictogramElement groupEndPE = getPictogramElementForBusinessObject(groupEnd);
            if(groupEndPE != null) {
               GraphicsAlgorithm groupEndGA = groupEndPE.getGraphicsAlgorithm();
               if(groupEndGA.getY() + groupEndGA.getHeight() / 2 > methodMaxY &&
                     groupEnd.getGroupStartCondition((ISEDDebugNode) NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart)) == null) {
                  methodMaxY = groupEndGA.getY() + groupEndGA.getHeight() / 2;
               }
            }
         }
         
         // if the new y value is higher than the rect we need to adjust the height
         if(methodMaxY > rectGA.getY() + rectGA.getHeight())
         {
            rectGA.setHeight(methodMaxY - rectGA.getY());
            
            // Pushes the nodes down to the end of the rect
            for(ISEDBranchCondition bc : bcs) {
               ISEDDebugNode groupEnd = bc.getChildren()[0];
               
               if(groupEnd == node) {
                  break;
               }
               
               PictogramElement groupEndPE = getPictogramElementForBusinessObject(groupEnd);
               
               if(groupEndPE != null) {
                  GraphicsAlgorithm groupEndGA = groupEndPE.getGraphicsAlgorithm();
                  if(groupEndGA.getY() + groupEndGA.getHeight() / 2 < rectGA.getY() + rectGA.getHeight()) {
                     moveSubTreeVertical(groupEnd, rectGA.getY() + rectGA.getHeight() - groupEndGA.getY() -  groupEndGA.getHeight() / 2, monitor);
                  }
               }
            }
         }

         // if the new node is a groupend we can place it ontop of the bottom line of the rect
         if(isGroupEnd) {
            ga.setY(rectGA.getY() + rectGA.getHeight() - ga.getHeight() / 2);
         }
         
         // Remove not used space between rects (the bottom lien of different rects
         // will be ontop each other in certain situations)
         shrinkRectHeights(groupStart);
         
         methodMaxY = rectGA.getY() + rectGA.getHeight() + ga.getHeight() + OFFSET;
         
         groupStart = NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart);
         isGroupEnd = node.getGroupStartCondition((ISEDDebugNode) groupStart) != null;

      } while(groupStart != null && !monitor.isCanceled());
   }
   
   /**
    * Updates des width of all rects which are affected by the given {@link ISEDDebugNode}.
    * @param node The node that updates the height.
    * @throws DebugException Occured Exception.
    */
   protected void updateGroupRectWidths(ISEDDebugNode node) throws DebugException {
      GraphicsAlgorithm ga = getPictogramElementForBusinessObject(node).getGraphicsAlgorithm();

      ISEDGroupable groupStart = NodeUtil.getGroupStartNode(node);
      
      int methodMaxX = ga.getX() + ga.getWidth() + METOFF;
      while(groupStart != null)
      {
         GraphicsAlgorithm groupStartGA = getPictogramElementForBusinessObject(groupStart, 0).getGraphicsAlgorithm();

         if(methodMaxX > groupStartGA.getX() + groupStartGA.getWidth()) {
            int diff = methodMaxX - groupStartGA.getX() - groupStartGA.getWidth();
            groupStartGA.setWidth(groupStartGA.getWidth() + diff);
         }

         methodMaxX = groupStartGA.getX() + groupStartGA.getWidth() + METOFF;
         groupStart = NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart);
      }
   }
   
   /**
    * Adjusts the height of rectangles. For example if a rect is collapsed.
    * @param groupStart 
    * @throws DebugException
    */
   protected void shrinkRectHeights(ISEDGroupable groupStart) throws DebugException {
      GraphicsAlgorithm rectGA = null;
      do
      {
         ISEDBranchCondition[] bcs = groupStart.getGroupEndConditions();
         rectGA = getPictogramElementForBusinessObject(groupStart, 0).getGraphicsAlgorithm();
         
         int height = 0;
         // find the groupend with the biggest height
         for(ISEDBranchCondition bc : bcs) {
            ISEDDebugNode groupEnd = bc.getChildren()[0];
            PictogramElement groupEndPE = getPictogramElementForBusinessObject(groupEnd);
                             
            if(groupEndPE != null && groupEndPE.getGraphicsAlgorithm().getHeight() > height) {
               height = groupEndPE.getGraphicsAlgorithm().getHeight();
            }
         }
         
         int diff = rectGA.getY() + rectGA.getHeight() - findDeepestYInGroup(groupStart) - OFFSET - height / 2;

         if(diff != 0)
         {
            rectGA.setHeight(rectGA.getHeight() - diff);
   
            // This loop moves all nodes between two rects upwards if the height of the inner rect
            // is less than before.
            for(int i = 0; i < bcs.length; i++) {
               ISEDDebugNode groupEnd = bcs[i].getChildren()[0];
               
               // If the groupend ends another (more outside) group too then ignore it
               if(groupEnd.getGroupStartCondition((ISEDDebugNode) NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart)) != null) {
                  continue;
               }
               
               PictogramElement groupEndPE = getPictogramElementForBusinessObject(groupEnd);
                                
               if(groupEndPE != null) {
                  ISEDGroupable outerGroup = NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart);
                  ISEDIterator iter = outerGroup == null ? new SEDPreorderIterator(groupEnd) : new SEDGroupPreorderIterator(outerGroup, groupEnd, true);
                  // Iterate over the group nodes
                  while (iter.hasNext()) {
                     ISEDDebugElement next = iter.next();            
                     
                     if(next instanceof ISEDDebugNode) {
                        ISEDDebugNode nextNode = (ISEDDebugNode) next;
                        
                        if(nextNode.getGroupStartCondition((ISEDDebugNode) outerGroup) != null && !nextNode.equals(groupEnd)) {
                           continue;
                        }
                        
                        // move the node
                        PictogramElement pe = getPictogramElementForBusinessObject(nextNode);
                        if (pe != null) {
                           GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
                           ga.setY(ga.getY() - diff);
                        }
                        
                        // and the rect if present
                        if(NodeUtil.canBeGrouped(nextNode)) {
                           pe = getPictogramElementForBusinessObject(nextNode, 0);
                           if (pe != null) {
                              GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
                              ga.setY(ga.getY() - diff);
                           }
                        }
                     }
                  }
               }
            }
         }
         groupStart = NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart);
      } while(groupStart != null);
   }
   
   /**
    * Adjusts the position and the width of affected rectangles.
    * @param groupStart The group to start with.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occured Exception.
    */
   protected void resizeRectsIfNeeded(ISEDGroupable groupStart, IProgressMonitor monitor) throws DebugException {
      do
      {
         PictogramElement groupStartPE = getPictogramElementForBusinessObject(groupStart, 0);

         if(groupStartPE != null)
         {
            GraphicsAlgorithm rectGA = groupStartPE.getGraphicsAlgorithm();
            
            int mostLeftX = findInGroup(groupStart, true) - METOFF;

            if(mostLeftX > rectGA.getX() && NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart) != null) {
               rectGA.setX(mostLeftX);
            }

            rectGA.setWidth(findInGroup(groupStart, false) + METOFF - rectGA.getX());
         }

         groupStart = NodeUtil.getGroupStartNode((ISEDDebugNode) groupStart);
      } while(groupStart != null);
   }
   
   /**
    * Finds the biggest width of the upper part of the current branch or, if 
    * there is no upper part, the parent branch.
    * @param start The {@link ISEDDebugNode} to start with.
    * @return The width of the biggest node.
    * @throws DebugException Occured Exception.
    */
   protected int findBiggestWidthInPartTreeAbove(ISEDDebugNode start) throws DebugException {
      ISEDDebugNode node = start;
      int width = -1;
      
      node = NodeUtil.getParent(node);
      
      while(node != null && (NodeUtil.getChildren(node).length == 1 || width == -1)) {
         PictogramElement nodePE = getPictogramElementForBusinessObject(node);
         if(nodePE != null)
         {
            GraphicsAlgorithm nodeGA = nodePE.getGraphicsAlgorithm();
            if(nodeGA.getWidth() > width || width == -1) {
               width = nodeGA.getWidth();
            }
         }
         
         node = NodeUtil.getParent(node);
      }
      return width;
   }
   
   /**
    * Finds and returns the biggest node in parent branches.
    * @param start Tthe {@link ISEDDebugNode} to start from.
    * @return The biggest node.
    * @throws DebugException Occured Exception.
    */
   protected ISEDDebugNode findBiggestNodeInParentBranches(ISEDDebugNode start) throws DebugException {
      ISEDDebugNode node = start;
      ISEDDebugNode biggestNode = null;

      while(node != null) {
         node = NodeUtil.getParent(node);
         
         if(NodeUtil.getChildren(node).length > 1) {
            break;
         }
      }
      
      int width = -1;
      while(node != null) {
         PictogramElement nodePE = getPictogramElementForBusinessObject(node);
         if(nodePE != null)
         {
            GraphicsAlgorithm nodeGA = nodePE.getGraphicsAlgorithm();
            if(nodeGA.getWidth() > width || width == -1) {
               width = nodeGA.getWidth();
               biggestNode = node;
            }
         }
         
         node = NodeUtil.getParent(node);
      }

      return biggestNode;
   }

   /**
    * Iterates over the parents of the given {@link ISEDDebugNode} until
    * the beginning of the branch is reached and computes the minimal (x) or maximal 
    * (x + width) x value of the visited {@link ISEDDebugNode}s.
    * @param start The {@link ISEDDebugNode} to start.
    * @param mostLeft The {@link Boolean} to search either the most left x value ({@code true}) or the most right ({@code false}).
    * @return The most minmal or maximal x value of parent {@link ISEDDebugNode}s in the same branch.
    * @throws DebugException Occurred Exception.
    */
   protected int findAbove(ISEDDebugNode start, boolean mostLeft) throws DebugException {
      int mostX = -1;
      ISEDDebugNode node = NodeUtil.getParent(start);
      
      while (node != null) {
         PictogramElement pe = getPictogramElementForBusinessObject(node, (isParentGroup(start, node) ? 1 : 0));
         mostX = compare(pe, mostLeft, mostX);         
         // Select parent for next loop iteration
         node = NodeUtil.getParent(node);
         if (node != null && NodeUtil.getChildren(node).length != 1 && mostX > -1) {
            node = null;
         }
      }
      return mostX;
   }
   
   /**
    * Iterates over the parents of the given {@link ISEDDebugNode} until
    * there can't be more left or right nodes for this branch and computes
    * the minimal (x) or maximal (x + width) x value of the visited {@link ISEDDebugNode}s.
    * @param start The {@link ISEDDebugNode} to start.
    * @param mostLeft The {@link Boolean} to search either the most left x value ({@code true}) or the most right ({@code false}).
    * @return The most minmal or maximal x value of parent {@link ISEDDebugNode}s in the same branch.
    * @throws DebugException Occurred Exception.
    */
   protected int findInParents(ISEDDebugNode start, boolean mostLeft) throws DebugException {
      int mostX = -1;
      ISEDDebugNode node = start;
      
      while (node != null) {
         PictogramElement pe = getPictogramElementForBusinessObject(node, (isParentGroup(start, node) ? 1 : 0));
         mostX = compare(pe, mostLeft, mostX);
         // Select parent for next loop iteration
         ISEDDebugNode child = node;
         node = NodeUtil.getParent(node);
         
         if (node != null && NodeUtil.getChildren(node).length != 1
               && (mostLeft && !ArrayUtil.isFirst(NodeUtil.getChildren(node), child)
                   || !mostLeft && !ArrayUtil.isLast(NodeUtil.getChildren(node), child))) {
            node = null;
         }
      }
      return mostX;
   }
   
   /**
    * Find either the most left or most right x in the previous or following branch.
    * @param node The {@link ISEDDebugNode} in the current branch.
    * @param previousBranch {@code true} if the search takes place in the previous branch, {@code false} otherwise.
    * @param mostLeft {@code true} if the most left should be searched, {@code false} otherwhise.
    * @return The most left or most right x of the previous or following branch.
    * @throws DebugException Occured Exception.
    */
   protected int findInSiblingBranch(ISEDDebugNode node, boolean previousBranch, boolean mostLeft) throws DebugException {
      do
      {
         ISEDDebugNode parent = NodeUtil.getParent(node);
         
         if(parent == null) {
            return -1;
         }

         ISEDDebugNode[] children = NodeUtil.getChildren(parent);
         
         if(previousBranch && !ArrayUtil.isFirst(children, node)) {
            return findInSubtree(ArrayUtil.getPrevious(children, node), mostLeft, true);
         }
         else if(!previousBranch && !ArrayUtil.isLast(children, node)) {
            return findInSubtree(ArrayUtil.getFollowing(children, node), mostLeft, true);
         }
         node = parent;
      } while(true);
   }
   
   /**
    * Iterates over the most left or right children of the given {@link ISEDDebugNode}
    * and computes the minimal (x) or maximal (x + width) x value of the visited child {@link ISEDDebugNode}s.
    * @param start The {@link ISEDDebugNode} to start.
    * @param mostLeft The {@link Boolean} to search either the most left x value ({@code true}) or the most right ({@code false}).
    * @param checkStartNode {@code true} if the start node should be checked too, {@code false} otherwhise.
    * @return The most minimal or maximal x value of most left or right child {@link ISEDDebugNode}s.
    * @throws DebugException Occurred Exception.
    */
   protected int findInSubtree(ISEDDebugNode node, boolean mostLeft, boolean checkStartNode) throws DebugException {
      int mostX = -1;
      if(!checkStartNode) {
         node = mostLeft ? ArrayUtil.getFirst(NodeUtil.getChildren(node)) : ArrayUtil.getLast(NodeUtil.getChildren(node));
      }
      while (node != null) {
         PictogramElement pe = getPictogramElementForBusinessObject(node, 0);
         mostX = compare(pe, mostLeft, mostX); 
         // Select child for next loop iteration
         node = mostLeft ? ArrayUtil.getFirst(NodeUtil.getChildren(node)) : ArrayUtil.getLast(NodeUtil.getChildren(node));
      }
      return mostX;
   }
   
   /**
    * Iterates over the most left or right children of the given {@link ISEDDebugNode} until
    * the end of the group is reached and computes the minimal (x) or maximal (x + width) 
    * x value of the visited child {@link ISEDDebugNode}s.
    * @param groupStart The {@link ISEDGroupable} to iterate over.
    * @param mostLeft The {@link Boolean} to search either the most left x value ({@code true}) or the most right ({@code false}).
    * @return The most minimal or maximal x value of most left or right child {@link ISEDDebugNode}s.
    * @throws DebugException Occurred Exception.
    */
   protected int findInGroup(ISEDGroupable groupStart, boolean mostLeft) throws DebugException {
      int mostX = -1;
      ISEDDebugNode node = (ISEDDebugNode) groupStart;
      
      while (node != null) {
         PictogramElement pe = NodeUtil.canBeGrouped(node) && (ISEDGroupable) node != groupStart ? getPictogramElementForBusinessObject(node, 0) : getPictogramElementForBusinessObject(node);
         mostX = compare(pe, mostLeft, mostX);
         // Select child for next loop iteration
         ISEDDebugNode parent = node;
         node = mostLeft ? ArrayUtil.getFirst(NodeUtil.getChildren(node)) : ArrayUtil.getLast(NodeUtil.getChildren(node));
         
         if(node != null && NodeUtil.getGroupStartNode(node) == null
               || parent.getGroupStartCondition((ISEDDebugNode) groupStart) != null) {
            node = null;
         }
      }
      
      return mostX;
   }
   
   /**
    * Compares the position of the given {@link PictogramElement} with the given int value
    * and returns the new value with respect to the given boolean value.
    * @param pe The {@link PictogramElement} which values shall be compared.
    * @param mostLeft {@code true} for the smaler x value, {@false} otherwise.
    * @param mostX The reference value.
    * @return The new value.
    */
   private int compare(PictogramElement pe, boolean mostLeft, int mostX) {
      if (pe != null) {
         GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
         if(mostLeft && (ga.getX() < mostX || mostX == -1)) {
            mostX = ga.getX();
         }
         else if(!mostLeft && ga.getX() + ga.getWidth() > mostX) {
            mostX = ga.getX() + ga.getWidth();
         }
      }
      return mostX;
   }
   
   /**
    * Searches the deepest y value of the specific group besides the group end nodes.
    * @param groupStart The group to search in.
    * @return The highest y value of the group besides group end nodes.
    * @throws DebugException
    */
   protected int findDeepestYInGroup(ISEDGroupable groupStart) throws DebugException {
      int deepestY = 0;
      int groupAmount = -1;
      ISEDIterator iter = new SEDGroupPreorderIterator(groupStart);

      while (iter.hasNext()) {
         ISEDDebugElement next = iter.next();
         if(next instanceof ISEDDebugNode)
         {
            ISEDDebugNode nextNode = (ISEDDebugNode) next;
            // Either we are outside of the Group or we have reached a groupendnode
            if(NodeUtil.getGroupStartNode(nextNode) == null
                  || nextNode.getGroupStartCondition((ISEDDebugNode) groupStart) != null) {
               continue;
            }
            
            PictogramElement pe = getPictogramElementForBusinessObject(nextNode);
            if (pe != null) {
               GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
               if (ga.getY() + ga.getHeight() > deepestY) {
                  deepestY = ga.getY() + ga.getHeight();

                  if(!ArrayUtil.isEmpty(nextNode.getGroupStartConditions())) {
                     nextNode = NodeUtil.getParent(nextNode.getInnerMostVisibleGroupStartCondition());
                  }
                  
                  ISEDGroupable innerGroup = NodeUtil.getGroupStartNode(nextNode);
                  
                  groupAmount = -1;
                  // We need to compute the amount of Groups inside the startgroup
                  while(innerGroup != null && innerGroup != groupStart) {
                     groupAmount++;
                     innerGroup = NodeUtil.getGroupStartNode((ISEDDebugNode) innerGroup);
                  }
               }
            }
         }
      }
      
      return groupAmount > 0 ? deepestY + groupAmount * OFFSET : deepestY;
   }
   
   /**
    * Checks if the given {@link ISEDDebugNode} potentialGroupNode contains
    * the given {@link ISEDDebugNode} source.
    * @param source The potentially contained node.
    * @param potentialGroupNode The potential node, that may contain the source node.
    * @return {@code true} if the potentialGroupNode contains the source node, {@code false} otherwhise.
    * @throws DebugException Occured Exception.
    */
   protected boolean isParentGroup(ISEDDebugNode source, ISEDDebugNode potentialGroupNode) throws DebugException {
      if(NodeUtil.canBeGrouped(potentialGroupNode)) {
         ISEDGroupable outerGroup = NodeUtil.getGroupStartNode(source);
         
         while(outerGroup != null) {
            if(outerGroup == (ISEDGroupable) potentialGroupNode) {
               return true;
            }
            outerGroup = NodeUtil.getGroupStartNode((ISEDDebugNode) outerGroup);
         }
      }
      
      return false;
   }
   
   /**
    * Checks if the branch of the given {@link ISEDDebugNode} has either
    * a previous or a following sibling based on the given boolean value.
    * @param node The node of the branch that shoudl be checked.
    * @param previousBranch {@code true} if check for previous branch, {@code false} otherwhise.
    * @return {@code true} if there is the searched branch, {@code false} otherwhise.
    * @throws DebugException Occured Exception.
    */
   protected boolean hasSibling(ISEDDebugNode node, boolean previousBranch) throws DebugException {
      ISEDDebugNode parent = NodeUtil.getParent(node);
      // Move to the branchsplitting.
      while(parent != null && NodeUtil.getChildren(parent).length < 2) {
         node = parent;
         parent = NodeUtil.getParent(node);
      }
      
      if(parent == null) {
         return false;
      }
      // Check if we have a sibling.
      return (previousBranch ? 
            ArrayUtil.getPrevious(NodeUtil.getChildren(parent), node) :
            ArrayUtil.getFollowing(NodeUtil.getChildren(parent), node)) == null;
   }
   
   /**
    * Returns the next child, that is bigger than the giben width or null if there is none.
    * @param node The {@link ISEDDebugNode} to start at.
    * @param width The reference width.
    * @param monitor The {@link IProgressMonitor} to use.
    * @return The next bigger child or null.
    * @throws DebugException
    */
   protected ISEDDebugNode returnBiggerChildOrNull(ISEDDebugNode node, int width, IProgressMonitor monitor) throws DebugException {
      ISEDIterator iter = new SEDPreorderIterator(node);
      while (iter.hasNext()) {
         ISEDDebugElement next = iter.next();
         
         if(next instanceof ISEDDebugNode) {
            ISEDDebugNode nextNode = (ISEDDebugNode) next;
            PictogramElement nextPE = getPictogramElementForBusinessObject(nextNode);
            if(nextPE != null) {
               GraphicsAlgorithm nextGA = nextPE.getGraphicsAlgorithm();
               if(nextGA.getWidth() > width) {
                  return nextNode;
               }
            }
         }
      }
      
      return null;
   }  

   /**
    * Moves all nodes which x coordinates are more to the right as the 
    * given node by the given distance.
    * @param node The {@link ISEDDebugNode} to start moving.
    * @param distance The distance to move.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occurred Exception.
    */
   protected void moveRighterNodes(ISEDDebugNode node, 
                                   int distance, 
                                   IProgressMonitor monitor) throws DebugException {
      if (node != null) {
         ISEDDebugNode parent = NodeUtil.getParent(node);
         while (parent != null && !monitor.isCanceled()) {
            ISEDDebugNode[] siblings = NodeUtil.getChildren(parent);
            int index = ArrayUtil.indexOf(siblings, node);
            if (index < 0) {
               throw new DebugException(LogUtil.getLogger().createErrorStatus("Child \"" + node + "\" is not contained in parent's children \"" + Arrays.toString(siblings) + "\"."));
            }
            // Move subtree of all siblings
            for (int i = index + 1; i < siblings.length; i++) {
               moveSubTreeHorizontal(siblings[i], distance, true, monitor);
            }
            // Re-center parent
            GraphicsAlgorithm parentGA = getPictogramElementForBusinessObject(parent).getGraphicsAlgorithm();
            parentGA.setX(calcXStart(siblings) + calcXMargin(siblings, parentGA.getWidth()));

            if(NodeUtil.getGroupStartNode(parent) != null) {
               updateGroupRectWidths(parent);
            }
            // Define node for next loop iteration
            node = parent;
            parent = NodeUtil.getParent(node);
         }
      }
   }

   /**
    * Move all nodes which x coordinates are more to the right or above
    * the given node by the given distance.
    * @param start The {@link ISEDDebugNode} to start.
    * @param distance The distance to move.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occurred Exception.
    */
   protected void moveRightAndAbove(ISEDDebugNode start, int distance, IProgressMonitor monitor) throws DebugException {
      ISEDDebugNode node = start;
      do
      {
         ISEDDebugNode parent = NodeUtil.getParent(node);
         
         if(parent == null) {
            return;
         }
         
         if(NodeUtil.getChildren(parent).length > 1) {
            moveRighterNodes(node, distance, monitor);
            return;
         }

         PictogramElement parentPE = getPictogramElementForBusinessObject(parent);
         if(parentPE != null)
         {
            GraphicsAlgorithm parentGA = parentPE.getGraphicsAlgorithm();
            parentGA.setX(parentGA.getX() + distance);
            
            if(NodeUtil.canBeGrouped(parent) && !isParentGroup(start, parent)) {
               parentPE = getPictogramElementForBusinessObject(parent, 0);
               parentGA = parentPE.getGraphicsAlgorithm();
               parentGA.setX(parentGA.getX() + distance);
            }
         }
         node = parent;
      } while(node != null);
   }
   
   /**
    * Handles the layout if there is a big node with a small subtree.
    * (The procedure can be looked up in "Guided Navigation in SETs".) 
    * @param node One new node in the subtree of the big node.
    * @param endNode The big node.
    * @param distance The distance that needs to be moved.
    * @param isLeft {@code true} if the branch of the given node is the first, {@code false} otherwhise.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occured Exception.
    */
   protected void moveSmallSubtree(ISEDDebugNode node, ISEDDebugNode endNode, int distance, boolean isLeft, IProgressMonitor monitor) throws DebugException {
      if(node == null) {
         return;
      }
      
      boolean firstBranch = true;
      ISEDDebugNode parent = NodeUtil.getParent(node);
      while (parent != null && parent != endNode && !monitor.isCanceled()) {
         ISEDDebugNode[] siblings = NodeUtil.getChildren(parent);
         int index = ArrayUtil.indexOf(siblings, node);
         if (index < 0) {
            throw new DebugException(LogUtil.getLogger().createErrorStatus("Child \"" + node + "\" is not contained in parent's children \"" + Arrays.toString(siblings) + "\"."));
         }
         
         // If we are at a branchsplit move all subbranches.
         if(siblings.length > 1) {
            if(firstBranch) {
               moveSubTreeHorizontal(siblings[index], (isLeft ? -3 : -1) * distance, true, monitor);
               
               if(isLeft) {
                  for (int i = index + 1; i < siblings.length; i++) {
                     moveSubTreeHorizontal(siblings[i], distance, true, monitor);
                  }
               }
               else {
                  for (int i = index - 1; i > -1; i--) {
                     moveSubTreeHorizontal(siblings[i], -distance, true, monitor);
                  }
               }
               
               firstBranch = false;
            }
            else {
               
               for(int i = 0; i < siblings.length; i++) {
                  // if we process the left half of the siblings
                  if(i < index) {
                     moveSubTreeHorizontal(siblings[i], (isLeft ? -3 : -1) * distance, true, monitor);
                  }
                  else if(i == index) {
                     // if the specific branch is in the higher area of the siblings
                     if(index > (siblings.length - 1) / 2)
                        moveSubTreeHorizontal(siblings[i], 3 * distance, true, monitor);
                     // if the specific branch is in the lower area of the siblings
                     else if(index <= (siblings.length - 1) / 2)
                        moveSubTreeHorizontal(siblings[i], -distance, true, monitor);
                  }
                  // if we process the right half of the siblings
                  else if(i > index) {
                     moveSubTreeHorizontal(siblings[i], distance, true, monitor);
                  }
               }
            }
            distance /= 2;
         }

         // Define node for next loop iteration
         node = parent;
         parent = NodeUtil.getParent(node);
      }
   }

   /**
    * Moves all nodes in the sub tree starting at the given {@link ISEDDebugNode}
    * horizontal by the given distance.
    * @param root The {@link ISEDDebugNode} to start moving.
    * @param distance The distance to move in x direction.
    * @param moveRoot {@code true} if the given {@link ISEDDebugNode} shall be moved too, {@code false} otherwhise.
    * @param monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occurred Exception
    */
   protected void moveSubTreeHorizontal(ISEDDebugNode root, 
                              int distance,
                              boolean moveRoot,
                              IProgressMonitor monitor) throws DebugException {
      ISEDIterator iter = new SEDPreorderIterator(root);
      while (iter.hasNext() && !monitor.isCanceled()) {
         ISEDDebugElement next = iter.next();
         
         if(next == root && !moveRoot) {
            continue;
         }
         
         if(next instanceof ISEDDebugNode) {
            ISEDDebugNode node = (ISEDDebugNode) next;
            PictogramElement pe = getPictogramElementForBusinessObject(node);
            if (pe != null) {
               GraphicsAlgorithm peGA = pe.getGraphicsAlgorithm();
               peGA.setX(peGA.getX() + distance);

               if(NodeUtil.canBeGrouped(node))
               {
                  pe = getPictogramElementForBusinessObject(node, 0);
                  if (pe != null) {
                     peGA = pe.getGraphicsAlgorithm();
                     peGA.setX(peGA.getX() + distance);
                  }
               }
               
               if(NodeUtil.getGroupStartNode(node) != null) {
                  updateGroupRectWidths(node);
               }
            }  
         }
      }
   }
   
   /**
    * Moves all nodes in the subtree starting at the given {@link ISEDDebugNode}
    * vertical by the given distance.
    * @param root The {@link ISEDDebugNode} to start moving.
    * @param distance The distance to move in x direction.
    * @parem monitor The {@link IProgressMonitor} to use.
    * @throws DebugException Occurred Exception
    */
   protected void moveSubTreeVertical(ISEDDebugNode root, int distance, IProgressMonitor monitor) throws DebugException {
      ISEDIterator iter = new SEDPreorderIterator(root);
      while (iter.hasNext() && !monitor.isCanceled()) {
         ISEDDebugElement next = iter.next();
         PictogramElement pe = getPictogramElementForBusinessObject(next);
         if (pe != null) {
            GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
            ga.setY(ga.getY() + distance);
            
            if(NodeUtil.canBeGrouped((ISEDDebugNode) next)) {
               pe = getPictogramElementForBusinessObject(next, 0);
               if (pe != null) {
                  ga = pe.getGraphicsAlgorithm();
                  ga.setY(ga.getY() + distance);
               }
            }
         }
      }
   }

   /**
    * Updates the style of the given {@link PictogramElement}.
    * @param pe The {@link PictogramElement} to update.
    * @param node The {@link ISEDDebugNode} as business object of the given {@link PictogramElement}.
    * @return {@code true} successful, {@code false} not succesful.
    */
   protected boolean updateStyle(PictogramElement pe, ISEDDebugNode node) {
      if (pe instanceof Shape) {
         Shape shape = (Shape)pe;
         if (shape.getGraphicsAlgorithm() instanceof RoundedRectangle) {
            RoundedRectangle rr = (RoundedRectangle)shape.getGraphicsAlgorithm();
            ISEDAnnotation[] annotations = node.computeUsedAnnotations();
            String newStyleId = ExecutionTreeStyleUtil.computeDebugNodeStyleId(annotations);
            if (!newStyleId.equals(rr.getStyle().getId())) {
               // Replace and update style
               rr.setStyle(ExecutionTreeStyleUtil.getStyleForDebugNode(newStyleId, annotations, getDiagram()));
            }
            else {
               // Update style
               ExecutionTreeStyleUtil.getStyleForDebugNode(newStyleId, annotations, getDiagram());
            }
         }
         else if (shape.getGraphicsAlgorithm() instanceof Text) {
            Text text = (Text)shape.getGraphicsAlgorithm();
            ISEDAnnotation[] annotations = node.computeUsedAnnotations();
            String newStyleId = ExecutionTreeStyleUtil.computeDebugNodeTextStyleId(annotations);
            if (!newStyleId.equals(text.getStyle().getId())) {
               // Replace and update style
               text.setStyle(ExecutionTreeStyleUtil.getStyleForDebugNodeText(newStyleId, annotations, getDiagram()));
            }
            else {
               // Update style
               ExecutionTreeStyleUtil.getStyleForDebugNodeText(newStyleId, annotations, getDiagram());
            }
         }
      }
      return true;
   }
}