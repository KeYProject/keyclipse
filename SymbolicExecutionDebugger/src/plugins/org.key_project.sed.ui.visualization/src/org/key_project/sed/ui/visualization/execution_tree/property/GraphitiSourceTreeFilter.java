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

package org.key_project.sed.ui.visualization.execution_tree.property;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.platform.AbstractPropertySectionFilter;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.IWorkbenchPart;
import org.key_project.sed.ui.visualization.view.ExecutionTreeView;
import org.key_project.util.eclipse.WorkbenchUtil;

/**
 * {@link IFilter} implementation used to define if a {@link GraphitiSourcePropertySection}
 * is available or not.
 * @author Martin Hentschel
 */
public class GraphitiSourceTreeFilter extends AbstractPropertySectionFilter {
   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean accept(PictogramElement pe) {
      IWorkbenchPart part = WorkbenchUtil.getActivePart();
      if (part != null) {
         if (IDebugUIConstants.ID_DEBUG_VIEW.equals(part.getSite().getId())) {
            part = WorkbenchUtil.findView(ExecutionTreeView.VIEW_ID);
         }
         GraphitiSourcePropertySection section = new GraphitiSourcePropertySection();
         section.setInput(part, null);
         return section.getStackFrame(pe) != null;
      }
      else {
         return false;
      }
   }
}