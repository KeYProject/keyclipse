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

import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.key_project.sed.core.model.ISENode;
import org.key_project.sed.core.model.ISELoopBodyTermination;
import org.key_project.sed.ui.visualization.execution_tree.provider.IExecutionTreeImageConstants;

/**
 * Implementation of {@link IAddFeature} for {@link ISELoopBodyTermination}s.
 * @author Martin Hentschel
 */
public class LoopBodyTerminationAddFeature extends AbstractDebugNodeAddFeature {
   /**
    * Constructor.
    * @param fp The {@link IFeatureProvider} which provides this {@link IAddFeature}.
    */
   public LoopBodyTerminationAddFeature(IFeatureProvider fp) {
      super(fp);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean canAddBusinessObject(Object businessObject) {
      return businessObject instanceof ISELoopBodyTermination;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getImageId(ISENode node) {
      ISELoopBodyTermination terminationNode = (ISELoopBodyTermination)node;
      if (terminationNode.isVerified()) {
         return IExecutionTreeImageConstants.IMG_LOOP_BODY_TERMINATION;
      }
      else {
         return IExecutionTreeImageConstants.IMG_LOOP_BODY_TERMINATION_NOT_VERIFIED;
      }
   }
}