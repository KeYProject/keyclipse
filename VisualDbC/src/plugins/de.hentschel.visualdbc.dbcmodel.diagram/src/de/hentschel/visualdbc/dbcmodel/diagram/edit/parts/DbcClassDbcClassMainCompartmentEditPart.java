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

package de.hentschel.visualdbc.dbcmodel.diagram.edit.parts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ShapeCompartmentEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.CreationEditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.DragDropEditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.ResizableCompartmentEditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.figures.ResizableCompartmentFigure;
import org.eclipse.gmf.runtime.draw2d.ui.figures.ConstrainedToolbarLayout;
import org.eclipse.gmf.runtime.notation.View;

import de.hentschel.visualdbc.dbcmodel.diagram.edit.policies.DbcClassDbcClassMainCompartmentCanonicalEditPolicy;
import de.hentschel.visualdbc.dbcmodel.diagram.edit.policies.DbcClassDbcClassMainCompartmentItemSemanticEditPolicy;
import de.hentschel.visualdbc.dbcmodel.diagram.part.Messages;

/**
 * @generated
 */
public class DbcClassDbcClassMainCompartmentEditPart extends
      ShapeCompartmentEditPart {

   /**
    * @generated
    */
   public static final int VISUAL_ID = 7050;

   /**
    * @generated
    */
   public DbcClassDbcClassMainCompartmentEditPart(View view) {
      super(view);
   }

   /**
    * @generated
    */
   public String getCompartmentName() {
      return Messages.DbcClassDbcClassMainCompartmentEditPart_title;
   }

   /**
    * @generated
    */
   public IFigure createFigure() {
      ResizableCompartmentFigure result = (ResizableCompartmentFigure) super
            .createFigure();
      result.setTitleVisibility(false);
      return result;
   }

   /**
    * @generated
    */
   protected void createDefaultEditPolicies() {
      super.createDefaultEditPolicies();
      installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE,
            new ResizableCompartmentEditPolicy());
      installEditPolicy(EditPolicyRoles.SEMANTIC_ROLE,
            new DbcClassDbcClassMainCompartmentItemSemanticEditPolicy());
      installEditPolicy(EditPolicyRoles.CREATION_ROLE, new CreationEditPolicy());
      installEditPolicy(EditPolicyRoles.DRAG_DROP_ROLE,
            new DragDropEditPolicy());
      installEditPolicy(EditPolicyRoles.CANONICAL_ROLE,
            new DbcClassDbcClassMainCompartmentCanonicalEditPolicy());
   }

   /**
    * @generated
    */
   protected void setRatio(Double ratio) {
      if (getFigure().getParent().getLayoutManager() instanceof ConstrainedToolbarLayout) {
         super.setRatio(ratio);
      }
   }

}