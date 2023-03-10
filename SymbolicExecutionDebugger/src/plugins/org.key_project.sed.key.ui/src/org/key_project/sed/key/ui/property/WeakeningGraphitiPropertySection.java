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

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.key_project.sed.key.core.model.KeYJoin;

/**
 * {@link ISection} implementation to show the properties of {@link KeYJoin}s.
 * @author Martin Hentschel
 */
public class WeakeningGraphitiPropertySection extends AbstractTruthValueGraphitiPropertySection {
   /**
    * {@inheritDoc}
    */
   @Override
   public KeYJoin getDebugNode(PictogramElement pe) {
      KeYJoin node = null;
      if (pe != null) {
         IDiagramTypeProvider diagramProvider = getDiagramTypeProvider();
         if (diagramProvider != null) {
            IFeatureProvider featureProvider = diagramProvider.getFeatureProvider();
            if (featureProvider != null) {
               Object bo = diagramProvider.getFeatureProvider().getBusinessObjectForPictogramElement(pe);
               node = WeakeningPropertySection.getDebugNode(bo);
            }
         }
      }
      return node;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected AbstractTruthValueComposite createContentComposite(Composite parent) {
      return new WeakeningComposite(parent, getWidgetFactory(), this);
   }
}