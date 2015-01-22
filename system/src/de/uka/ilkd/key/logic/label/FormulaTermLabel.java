// This file is part of KeY - Integrated Deductive Software Design
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2014 Karlsruhe Institute of Technology, Germany
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General
// Public License. See LICENSE.TXT for details.
//

package de.uka.ilkd.key.logic.label;

import java.util.Collection;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Sequent;

/**
 * Label attached to a predicates for instance in postconditions, loop invariants or precondition checks of applied operation contracts.
 */
public class FormulaTermLabel implements TermLabel {
   /**
    * The unique name of this label.
    */
   public static final Name NAME = new Name("F");

   /**
    * The name used in {@link Services#getCounter(String)} to keep track
    * of the already used IDs.
    */
   public static final String PROOF_COUNTER_NAME = "F_LABEL_COUNTER";

   /**
    * The prefix of the name used in {@link Services#getCounter(String)} to 
    * keep track of the already used sub IDs.
    */
   public static final String PROOF_COUNTER_SUB_PREFIX = "F_LABEL_SUB_COUNTER_";
   
   /**
    * Separator between multiple before IDs.
    */
   public static final String BEFORE_ID_SEPARATOR = ";";
   
   /**
    * The unique ID of this term label in the {@link Sequent}.
    */
   private final String id;
   
   /**
    * The optional previous IDs of the label this one is derived from separated by {@value #BEFORE_ID_SEPARATOR}.
    */
   private final String beforeId;
   
   /**
    * Constructor.
    * @param majorId The major part of the unique ID.
    * @param minorId The minor part of the unique ID.
    */
   public FormulaTermLabel(int majorId, int minorId) {
       this(majorId, minorId, null);
   }
   
   /**
    * Constructor.
    * @param majorId The major part of the unique ID.
    * @param minorId The minor part of the unique ID.
    * @param beforeId The optional previous ID of the label this one is derived from.
    */
   public FormulaTermLabel(int majorId, int minorId, Collection<String> beforeIds) {
       this.id = majorId + "." + minorId;
       if (beforeIds != null) {
          StringBuffer sb = new StringBuffer();
          boolean afterFirst = false;
          for (String id : beforeIds) {
             if (id != null) {
                if (afterFirst) {
                   sb.append(BEFORE_ID_SEPARATOR);
                }
                else {
                   afterFirst = true;
                }
                sb.append(id);
             }
          }
          this.beforeId = sb.toString();
       }
       else {
          this.beforeId = null;
       }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object o) {
       return this == o;
   }

   /**
    * {@inheritDoc}
    */
   public String toString() {
       return NAME.toString() + 
              "(" + 
             id + 
             (beforeId != null ? ", " + beforeId : "") +
             ")";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getChild(int i) {
	   switch (i) {
	      case 0 : return id;
	      case 1 : return beforeId;
  	      default : return null;
	   }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getChildCount() {
      if (beforeId != null) {
         return 2;
      }
      else {
         return 1;
      }
   }

   /**
    * Returns the unique ID of this label in the {@link Sequent}.
    * @return The unique ID of this label in the {@link Sequent}.
    */
   public String getId() {
      return id;
   }
   
   /**
    * Returns the major part of the unique ID.
    * @return The major part of the unique ID.
    */
   public int getMajorId() {
      int index = id.indexOf(".");
      return Integer.parseInt(id.substring(0, index));
   }
   
   /**
    * Returns the minor part of the unique ID.
    * @return The minor part of the unique ID.
    */
   public int getMinorId() {
      int index = id.indexOf(".");
      return Integer.parseInt(id.substring(index + 1));
   }

   /**
    * Returns the optional previous IDs of the label this one is derived from.
    * @return The optional previous IDs of the label this one is derived from.
    */
   public String[] getBeforeIds() {
      return beforeId != null ? beforeId.split(BEFORE_ID_SEPARATOR) : new String[0];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Name name() {
      return NAME;
   }

   /**
    * Creates a new label sub ID.
    * @param services The {@link Services} to use.
    * @param label The parent {@link FormulaTermLabel} which provides the major ID.
    * @return The new label sub ID.
    */
   public static int newLabelSubID(Services services, FormulaTermLabel label) {
      return newLabelSubID(services, label.getMajorId());
   }

   /**
    * Creates a new label sub ID.
    * @param services The {@link Services} to use.
    * @param labelId The parent {@link FormulaTermLabel} which provides the major ID.
    * @return The new label sub ID.
    */
   public static int newLabelSubID(Services services, int labelId) {
      return services.getCounter(FormulaTermLabel.PROOF_COUNTER_SUB_PREFIX + labelId).getCountPlusPlus();
   }
}