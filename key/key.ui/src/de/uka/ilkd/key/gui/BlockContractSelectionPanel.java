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

package de.uka.ilkd.key.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.speclang.BlockContract;
import de.uka.ilkd.key.speclang.SimpleBlockContract;

// TODO Clean up.
public class BlockContractSelectionPanel extends JPanel {

    private static final long serialVersionUID = 1681443715264203991L;

    private final Services services;
    private final JList<BlockContract> contractList;
    private final TitledBorder border;

    public BlockContractSelectionPanel(final Services services, final boolean multipleSelection)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.services = services;

        //create scroll pane
        JScrollPane scrollPane = new JScrollPane();
        border = new TitledBorder("Contracts");
        scrollPane.setBorder(border);
        Dimension scrollPaneDim = new Dimension(700, 500);
        scrollPane.setPreferredSize(scrollPaneDim);
        scrollPane.setMinimumSize(scrollPaneDim);
        add(scrollPane);

        //create contract list
        contractList = new JList<BlockContract>();
        contractList.setSelectionMode(
                multipleSelection
                        ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                        : ListSelectionModel.SINGLE_SELECTION);
        contractList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if(contractList.isSelectionEmpty()) {
                    contractList.setSelectedIndex(e.getFirstIndex());
                }
            }
        });
        final Services serv = services;
        contractList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 9088858130231994408L;
            private final Font PLAINFONT = getFont().deriveFont(Font.PLAIN);

            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                BlockContract contract = (BlockContract) value;
                Component supComp
                        = super.getListCellRendererComponent(list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus);

                //create label and enclosing panel
                JLabel label = new JLabel();
                label.setText(contract.getHtmlText(serv));
                label.setFont(PLAINFONT);
                FlowLayout lay = new FlowLayout();
                lay.setAlignment(FlowLayout.LEFT);
                JPanel result = new JPanel(lay);
                result.add(label);
                label.setVerticalAlignment(SwingConstants.TOP);

                //set background color
                result.setBackground(supComp.getBackground());

                //set border
                TitledBorder border = new TitledBorder(
                        BorderFactory.createEtchedBorder(),
                        contract.getDisplayName());

                Font borderFont = border.getTitleFont();
                if (borderFont == null) { // MS Windows issues
                    borderFont = result.getFont();
                    if (borderFont == null) {
                        borderFont = PLAINFONT;
                    }
                }
                border.setTitleFont(borderFont.deriveFont(Font.BOLD));
                result.setBorder(border);

                return result;
            }
        });
        scrollPane.setViewportView(contractList);
    }

    public synchronized void addMouseListener(final MouseListener ml)
    {
        contractList.addMouseListener(ml);
    }

    public void setContracts(final BlockContract[] contracts, final String title)
    {
        contractList.setListData(contracts);
        contractList.setSelectedIndex(0);
        if (title != null) {
            border.setTitle(title);
        }
        updateUI();
    }

    public BlockContract getContract()
    {
        final List<BlockContract> selection = contractList.getSelectedValuesList();
        return computeContract(services, selection);
    }

    /**
     * <p>
     * Computes the selected {@link BlockContract}.
     * </p>
     * <p>
     * This method is also used by the KeYIDE (Eclipse) to ensure the same behavior.
     * </p>
     * @param services The {@link Services}
     * @param selection The selected contracts.
     * @return The selected {@link BlockContract} or {@code null} if not available.
     */
    public static BlockContract computeContract(Services services, List<BlockContract> selection) {
       if (selection.isEmpty()) {
           return null;
       }
       else if (selection.size() == 1) {
           return selection.get(0);
       }
       else {
           ImmutableSet<BlockContract> contracts = DefaultImmutableSet.nil();
           for (BlockContract contract : selection) {
               contracts = contracts.add(contract);
           }
           return SimpleBlockContract.combine(contracts, services);
       }
    }
}