// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
/* This file has been generated by Stubmaker (de.uka.ilkd.stubmaker)
 * Date: Wed May 14 11:55:45 CEST 2008
 */
package java.awt;

public interface LayoutManager
{

   public void addLayoutComponent(java.lang.String arg0, java.awt.Component arg1);
   public void removeLayoutComponent(java.awt.Component arg0);
   public java.awt.Dimension preferredLayoutSize(java.awt.Container arg0);
   public java.awt.Dimension minimumLayoutSize(java.awt.Container arg0);
   public void layoutContainer(java.awt.Container arg0);
}