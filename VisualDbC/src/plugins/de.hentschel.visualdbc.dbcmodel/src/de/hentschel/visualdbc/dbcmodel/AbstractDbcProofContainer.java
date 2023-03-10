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

/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package de.hentschel.visualdbc.dbcmodel;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Abstract Dbc Proof Container</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link de.hentschel.visualdbc.dbcmodel.AbstractDbcProofContainer#getProofs <em>Proofs</em>}</li>
 * </ul>
 * </p>
 *
 * @see de.hentschel.visualdbc.dbcmodel.DbcmodelPackage#getAbstractDbcProofContainer()
 * @model abstract="true"
 * @generated
 */
public interface AbstractDbcProofContainer extends EObject {
   /**
    * Returns the value of the '<em><b>Proofs</b></em>' containment reference list.
    * The list contents are of type {@link de.hentschel.visualdbc.dbcmodel.DbcProof}.
    * <!-- begin-user-doc -->
    * <p>
    * If the meaning of the '<em>Proofs</em>' containment reference list isn't clear,
    * there really should be more of a description here...
    * </p>
    * <!-- end-user-doc -->
    * @return the value of the '<em>Proofs</em>' containment reference list.
    * @see de.hentschel.visualdbc.dbcmodel.DbcmodelPackage#getAbstractDbcProofContainer_Proofs()
    * @model containment="true"
    * @generated
    */
   EList<DbcProof> getProofs();

} // AbstractDbcProofContainer