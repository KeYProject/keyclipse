// This file is part of KeY - Integrated Deductive Software Design 
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany 
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2013 Karlsruhe Institute of Technology, Germany 
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General 
// Public License. See LICENSE.TXT for details.
//

package de.uka.ilkd.key.gui.nodeviews;

import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.prooftree.ProofTreeView;
import de.uka.ilkd.key.util.GuiUtilities;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Kai Wallisch
 */
public class MainFrame extends JScrollPane {

    public void setSequentView(SequentView sequentView) {
        setViewportView(new MainFrameBody(sequentView));

        // Additional option to show taclet info in case of: sequentView instanceof InnerNodeView
        ProofTreeView ptv = MainWindow.getInstance().proofTreeView;
        if (ptv != null) {
            ptv.tacletInfoToggle.setSequentView(sequentView);
        }
    }

    public MainFrame() {

        setBorder(new EmptyBorder(0, 0, 0, 0));
        getVerticalScrollBar().setUnitIncrement(30);
        getHorizontalScrollBar().setUnitIncrement(30);

        // FIXME put this somewhere descent
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                "copy");
        getActionMap().put("copy", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                GuiUtilities.copyHighlightToClipboard(MainWindow.getInstance().leafNodeView);
            }
        });

        setSequentView(new EmptySequent());
    }

    private class MainFrameBody extends JPanel {

        public MainFrameBody(SequentView sequentView) {

            setLayout(new GridBagLayout());
            setBackground(sequentView.getBackground());

            GridBagConstraints gbc = new GridBagConstraints();

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            add(javax.swing.Box.createGlue(), gbc);

            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.gridwidth = 2;
            add(sequentView, gbc);

            if (sequentView instanceof InnerNodeView) {
                gbc.gridy = 2;
                add(((InnerNodeView) sequentView).tacletInfo, gbc);
            }

            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.weighty = 1.0;
            add(javax.swing.Box.createGlue(), gbc);

            setBorder(new TitledBorder(sequentView.getTitle()));

        }
    }
}