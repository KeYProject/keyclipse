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

import org.eclipse.debug.core.DebugException;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.key_project.sed.core.model.ISENode;
import org.key_project.sed.core.model.ISEDebugTarget;
import org.key_project.sed.core.model.ISELoopInvariant;
import org.key_project.sed.core.model.ISEThread;
import org.key_project.sed.core.model.memory.SEMemoryLoopInvariant;
import org.key_project.sed.ui.visualization.execution_tree.provider.IExecutionTreeImageConstants;

/**
 * Implementation of {@link ICreateFeature} for {@link ISELoopInvariant}s.
 * @author Martin Hentschel
 */
public class LoopInvariantCreateFeature extends AbstractDebugNodeCreateFeature {
   /**
    * Constructor.
    * @param fp The {@link IFeatureProvider} which provides this {@link IAddFeature}.
    */
   public LoopInvariantCreateFeature(IFeatureProvider fp) {
       super(fp, "Loop Invariant", "Create a new Loop Invariant");
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public String getCreateImageId() {
      return IExecutionTreeImageConstants.IMG_LOOP_INVARIANT;
   }

   /**
    * {@inheritDoc}
    */   
   @Override
   public String getNodeType() {
      return "Loop Invariant";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ISENode createNewDebugNode(ISEDebugTarget target,
                                              ISENode parent,
                                              ISEThread thread,
                                              String name) throws DebugException {
      SEMemoryLoopInvariant result = new SEMemoryLoopInvariant(target, parent, thread);
      result.setName(name);
      return result;
   }
}