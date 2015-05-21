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

package de.uka.ilkd.key.gui.joinrule;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLDocument;

import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.utilities.WrapLayout;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.pp.LogicPrinter;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.join.JoinProcedure;
import de.uka.ilkd.key.util.Pair;

/**
 * JDialog for selecting a subset of candidate goals as
 * partners for a join rule application.
 * 
 * @author Dominic Scheurer
 */
public class JoinPartnerSelectionDialog extends JDialog {
   private static final long serialVersionUID = -1460097562546341922L;
   
   /** The tooltip hint for the checkbox. */
   private static final String CB_SELECT_CANDIDATE_HINT =
         "Select to add shown state as a join partner.";
   
   /** The initial size of this dialog. */
   private static final Dimension INITIAL_SIZE =
         new Dimension(850, 450);
   
   /** The font for the JEditorPanes. Should
    *  resemble the standard font of KeY for proofs etc. */
   private static final Font TXT_AREA_FONT =
         new Font(Font.MONOSPACED, Font.PLAIN, 14);
   
   /** Comparator for goals; sorts by serial nr. of the node */
   private static Comparator<Pair<Goal, PosInOccurrence>> GOAL_COMPARATOR =
      new Comparator<Pair<Goal, PosInOccurrence>>() {
         @Override
         public int compare(Pair<Goal, PosInOccurrence> o1,
               Pair<Goal, PosInOccurrence> o2) {
            return o1.first.node().serialNr() - o2.first.node().serialNr();
         }
      };
   
   private LinkedList<Pair<Goal, PosInOccurrence>> candidates = null;
   private Services services = null;
   
   /** The chosen goals. */
   private SortedSet<Pair<Goal, PosInOccurrence>> chosenGoals =
         new TreeSet<Pair<Goal,PosInOccurrence>>(GOAL_COMPARATOR);
   
   /** The chosen join method. */
   private JoinProcedure chosenRule = JoinProcedure.getJoinProcedures().head();
   
   private JEditorPane txtPartner1 = null;
   private JEditorPane txtPartner2 = null;
   private JComboBox<String> cmbCandidates = null;
   private JCheckBox cbSelectCandidate = null;
   private ButtonGroup bgJoinMethods = null;
   
   private JScrollPane scrpPartner1 = null;
   private JScrollPane scrpPartner2 = null;
   
   private JoinPartnerSelectionDialog() {
      super(
            MainWindow.getInstance(),
            "Select partner node for join operation",
            true);
      
      setLocation(MainWindow.getInstance().getLocation());
      
      // Text areas for goals to join
      txtPartner1 = new JEditorPane();
      txtPartner2 = new JEditorPane();
      for (JEditorPane jep : new JEditorPane[] {txtPartner1, txtPartner2}) {
         jep.setEditable(false);
         jep.setContentType("text/html");
         
         // Set font
         String cssRule = "body { font-family: " + TXT_AREA_FONT.getFamily() + "; " +
               "font-size: " + TXT_AREA_FONT.getSize() + "pt; }";
         ((HTMLDocument) jep.getDocument()).getStyleSheet().addRule(cssRule);
      }
      
      scrpPartner1 = new JScrollPane(txtPartner1);
      scrpPartner2 = new JScrollPane(txtPartner2);
      
      // Goal selection dropdown field and checkbox
      cmbCandidates = new JComboBox<String>();
      cmbCandidates.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
      cmbCandidates.addItemListener(new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {   
            Pair<Goal, PosInOccurrence> selectedCandidate =
                  getSelectedCandidate();
            
            setHighlightedSequentForArea(
                  selectedCandidate.first, selectedCandidate.second, txtPartner2);
            
            if (chosenGoals.contains(selectedCandidate)) {
               cbSelectCandidate.setSelected(true);
            } else {
               cbSelectCandidate.setSelected(false);
            }
         }
      });
      addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            
            int halfWidth = getWidth() / 2;
            scrpPartner1.setPreferredSize(new Dimension(halfWidth, scrpPartner1.getHeight()));
            scrpPartner2.setPreferredSize(new Dimension(halfWidth, scrpPartner2.getHeight()));
         }
      });
      
      cbSelectCandidate = new JCheckBox();
      cbSelectCandidate.setToolTipText(CB_SELECT_CANDIDATE_HINT);
      cbSelectCandidate.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (cbSelectCandidate.isSelected()) {
               chosenGoals.add(getSelectedCandidate());
            } else {
               chosenGoals.remove(getSelectedCandidate());
            }
         }
      });
      
      bgJoinMethods = new ButtonGroup();
      for (final JoinProcedure rule : JoinProcedure.getJoinProcedures()) {
    	  JRadioButton rb = new JRadioButton(rule.toString());
    	  rb.setSelected(true);
    	  
    	  rb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					chosenRule = rule;
				}
    	  	});
    	  bgJoinMethods.add(rb);
      }
      
      // Join state container
      JPanel joinStateContainer = new JPanel();
      joinStateContainer.setLayout(new BoxLayout(joinStateContainer, BoxLayout.Y_AXIS));
      joinStateContainer.add(scrpPartner1);
      
      TitledBorder joinStateContainerTitle =
            BorderFactory.createTitledBorder("State to join");
      joinStateContainerTitle.setTitleJustification(TitledBorder.LEFT);
      joinStateContainer.setBorder(joinStateContainerTitle);
      
      // Join partner container
      JPanel partnerContainer = new JPanel();
      partnerContainer.setLayout(new BoxLayout(partnerContainer, BoxLayout.Y_AXIS));
      partnerContainer.add(scrpPartner2);
      
      JPanel selectionContainer = new JPanel();
      selectionContainer.setLayout(new BoxLayout(selectionContainer, BoxLayout.X_AXIS));
      selectionContainer.add(cbSelectCandidate);
      selectionContainer.add(cmbCandidates);
      
      partnerContainer.add(selectionContainer);
      
      TitledBorder txtPartner2Title =
            BorderFactory.createTitledBorder("Potential join partners");
      txtPartner2Title.setTitleJustification(TitledBorder.LEFT);
      partnerContainer.setBorder(txtPartner2Title);
      
      // Upper container
      JPanel upperContainer = new JPanel();
      upperContainer.setLayout(new BoxLayout(upperContainer, BoxLayout.X_AXIS));
      upperContainer.add(joinStateContainer);
      upperContainer.add(partnerContainer);
      
      // Join rules container
      JPanel joinRulesContainer = new JPanel();
      joinRulesContainer.setLayout(new WrapLayout());
      
      for (Enumeration<AbstractButton> e = bgJoinMethods.getElements(); e.hasMoreElements();) {
    	  JRadioButton rb = (JRadioButton) e.nextElement();
    	  joinRulesContainer.add(rb);
      }
      
      TitledBorder joinRulesContainerTitle =
              BorderFactory.createTitledBorder("Concrete join rule to apply");
      joinRulesContainerTitle.setTitleJustification(TitledBorder.LEFT);
      joinRulesContainerTitle.setBorder(joinStateContainerTitle);
      joinRulesContainer.setBorder(joinRulesContainerTitle);
      
      // Control buttons container: OK / Cancel
      JButton okButton = new JButton("OK");
      JButton chooseAllButton = new JButton("Choose All");
      JButton cancelButton = new JButton("Cancel");
      
      okButton.setAlignmentX(CENTER_ALIGNMENT);
      chooseAllButton.setAlignmentX(CENTER_ALIGNMENT);
      cancelButton.setAlignmentX(CENTER_ALIGNMENT);
      
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            setVisible(false);
            dispose();
         }
      });
      
      chooseAllButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            for (Pair<Goal, PosInOccurrence> candidate : candidates) {
               chosenGoals.add(candidate);
            }
            setVisible(false);
         }
      });
      
      cancelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            chosenGoals = null;
            setVisible(false);
         }
      });
      
      JPanel ctrlBtnsContainer = new JPanel();
      ctrlBtnsContainer.setLayout(new BoxLayout(ctrlBtnsContainer, BoxLayout.X_AXIS));
      ctrlBtnsContainer.add(Box.createHorizontalGlue());
      ctrlBtnsContainer.add(okButton);
      Dimension fillerDim = new Dimension(30, 40);
      ctrlBtnsContainer.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
      ctrlBtnsContainer.add(chooseAllButton);
      ctrlBtnsContainer.add(new Box.Filler(fillerDim, fillerDim, fillerDim));
      ctrlBtnsContainer.add(cancelButton);
      ctrlBtnsContainer.add(Box.createHorizontalGlue());
      
      JPanel lowerContainer = new JPanel();
      lowerContainer.setLayout(new BoxLayout(lowerContainer, BoxLayout.Y_AXIS));
      lowerContainer.add(joinRulesContainer);
      lowerContainer.add(ctrlBtnsContainer);
      
      // Add components to content pane
      getContentPane().add(upperContainer, BorderLayout.CENTER);
      getContentPane().add(lowerContainer, BorderLayout.SOUTH);
      
      setSize(INITIAL_SIZE);
   }
   
   /**
    * Creates a new join partner selection dialog.
    * 
    * @param joinGoal The first (already chosen) join partner.
    * @param pio Position of Update-Modality-Postcondition formula in
    *    the joinNode.
    * @param candidates Potential join candidates.
    * @param services The services object.
    */
   public JoinPartnerSelectionDialog(
         Goal joinGoal,
         PosInOccurrence pio,
         ImmutableList<Pair<Goal,PosInOccurrence>> candidates,
         Services services) {
      
      this();
      this.services = services;

      this.candidates = new LinkedList<Pair<Goal,PosInOccurrence>>();
      
      for (Pair<Goal,PosInOccurrence> candidate : candidates) {
         int insPos = Collections.binarySearch(
               this.candidates, candidate, GOAL_COMPARATOR);
         
         insPos = (insPos + 1) * -1;
         this.candidates.add(insPos, candidate);
      }
      
      setHighlightedSequentForArea(joinGoal, pio, txtPartner1);
      loadCandidates();
      
   }
   
   /**
    * @return All chosen join partners.
    */
   public ImmutableList<Pair<Goal, PosInOccurrence>> getChosenCandidates() {
      ImmutableSLList<Pair<Goal, PosInOccurrence>> result = ImmutableSLList.nil();
      
      if (chosenGoals != null) {
         return result.append(chosenGoals);
      } else {
         return result;
      }
   }
   
   /**
    * @return The chosen join rule.
    */
   public JoinProcedure getChosenJoinRule() {
      return chosenRule;
   }
   
   /**
    * @return The candidate chosen at the moment (by the combo box).
    */
   private Pair<Goal, PosInOccurrence> getSelectedCandidate() {
      return getNthCandidate(cmbCandidates.getSelectedIndex());
   }
   
   /**
    * Returns the n-th candidate in the list.
    * 
    * @param n Index of the join candidate.
    * @return The n-th candidate in the list.
    */
   private Pair<Goal, PosInOccurrence> getNthCandidate(int n) {
      int i = 0;
      for (Pair<Goal, PosInOccurrence> elem : candidates) {
         if (i == n) {
            return elem;
         }
         i++;
      }
      
      return null;
   }
   
   /**
    * Loads the join candidates into the combo box, initializes
    * the partner editor pane with the text of the first candidate.
    */
   private void loadCandidates() {
	   if (candidates.size() < 1) {
		   return;
	   }
	   
      for (Pair<Goal, PosInOccurrence> candidate : candidates) {
         cmbCandidates.addItem("Node " + candidate.first.node().serialNr());
      }
      
      setHighlightedSequentForArea(
            candidates.getFirst().first, candidates.getFirst().second, txtPartner2);
   }
   
   /**
    * Adds the given goal to the given editor pane, with the portion that
    * corresponds to the given position highlighted in bold.
    * 
    * @param goal Goal to add.
    * @param pio Position indicating subterm to highlight.
    * @param area The editor pane to add the highlighted goal to.
    */
   private void setHighlightedSequentForArea(Goal goal, PosInOccurrence pio, JEditorPane area) {
      
      String subterm = LogicPrinter.quickPrintTerm(pio.subTerm(), services);
      
      // Render subterm to highlight as a regular expression.
      // Note: Four backslashs in replacement expression will result in
      //       one backslash in the resulting string.
      subterm = subterm.replaceAll("\\s", "\\\\s");
      subterm = subterm.replaceAll("(\\\\s)+", "\\\\E\\\\s*\\\\Q");
      subterm = "\\Q" + subterm + "\\E";
      if (subterm.endsWith("\\Q\\E")) {
         subterm = subterm.substring(0, subterm.length() - 4);
      }
      
      // Find a match
      String sequent = LogicPrinter.quickPrintSequent(goal.sequent(), services);
      Pattern p = Pattern.compile(subterm);
      Matcher m = p.matcher(sequent);
      
      // Original sequent (without highlighted text) as fallback
      String newText = sequent;
      
      if (m.find()) {
         // Assemble new text
         String before = sequent.substring(0, m.start() - 1);
         String main = "<b>" + sequent.substring(m.start(), m.end()) + "</b>";
         String after = sequent.substring(m.end());
         
         newText = before + main + after;
      }
      
      // Treat spaces and newlines: Are ignored in HTML text area
      newText = newText.replace("\n", "<br>");
      newText = newText.replace(" ", "&nbsp;");
      
      area.setText(newText);
      
   }
   
}
