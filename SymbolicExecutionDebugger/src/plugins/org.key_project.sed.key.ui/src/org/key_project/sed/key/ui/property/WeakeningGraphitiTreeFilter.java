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

package org.key_project.sed.key.ui.property;

import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.ui.platform.AbstractPropertySectionFilter;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.IWorkbenchPart;
import org.key_project.sed.key.core.model.KeYJoin;
import org.key_project.util.eclipse.WorkbenchUtil;

/**
 * {@link IFilter} implementation used to define if a {@link WeakeningGraphitiPropertySection}
 * is available or not.
 * @author Martin Hentschel
 */
public class WeakeningGraphitiTreeFilter extends AbstractPropertySectionFilter {
   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean accept(PictogramElement pe) {
      IWorkbenchPart part = WorkbenchUtil.getActivePart();
      if (part != null) {
         WeakeningGraphitiPropertySection section = new WeakeningGraphitiPropertySection();
         section.setInput(part, null);
         KeYJoin node = section.getDebugNode(pe);
         return node != null && node.isTruthValueTracingEnabled();
      }
      else {
         return false;
      }
   }
}