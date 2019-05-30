package de.uka.ilkd.key.gui.ext.exploration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import de.uka.ilkd.key.control.TermLabelVisibilityManager;
import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.gui.IconFactory;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.NodeInfoWindow;
import de.uka.ilkd.key.gui.nodeviews.SequentView;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.IntIterator;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.PosInTerm;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.label.OriginTermLabel;
import de.uka.ilkd.key.logic.label.OriginTermLabel.Origin;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.pp.IdentitySequentPrintFilter;
import de.uka.ilkd.key.pp.InitialPositionTable;
import de.uka.ilkd.key.pp.LogicPrinter;
import de.uka.ilkd.key.pp.NotationInfo;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.pp.ProgramPrinter;
import de.uka.ilkd.key.pp.Range;
import de.uka.ilkd.key.pp.SequentPrintFilter;
import de.uka.ilkd.key.pp.SequentPrintFilterEntry;
import de.uka.ilkd.key.pp.SequentViewLogicPrinter;
import de.uka.ilkd.key.pp.ShowSelectedSequentPrintFilter;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.ProofTreeAdapter;
import de.uka.ilkd.key.proof.ProofTreeEvent;
import de.uka.ilkd.key.util.pp.UnbalancedBlocksException;

/**
 * This window visualizes the {@link OriginTermLabel}s of a term and its sub-terms.
 *
 * @author lanzinger
 */
public final class OriginTermLabelWindow extends NodeInfoWindow {

    private static final long serialVersionUID = -2791483814174192622L;

    /**
     * The window's initial width.
     */
    public static final int WIDTH = 1280;

    /**
     * The window's initial height.
     */
    public static final int HEIGHT = 720;

    /**
     * The background color to use to highlight a sub-term.
     */
    public static final Color HIGHLIGHT_COLOR = Color.ORANGE;

    /**
     * The title of the tree view.
     */
    public static final String TREE_TITLE = "Selected formula as tree";

    /**
     * The title of the term view.
     */
    public static final String VIEW_TITLE = "Selected formula";

    /**
     * The title for the origin information for the selected term.
     *
     * @see #ORIGIN_TITLE
     * @see #SUBTERM_ORIGINS_TITLE
     */
    public static final String ORIGIN_INFO_TITLE = "Origin information";

    /**
     * The title for the selected term's origin.
     *
     * @see #ORIGIN_INFO_TITLE
     */
    public static final String ORIGIN_TITLE = "Origin of formula";

    /**
     * The title for the origin of the selected term's sub-terms and former sub-terms.
     *
     * @see #ORIGIN_INFO_TITLE
     */
    public static final String SUBTERM_ORIGINS_TITLE = "Origins of (former) subformulas and subterms";

    /**
     * The gap between a term and its origin in the tree view.
     */
    public static final int TREE_CELL_GAP = 20;

    /**
     * The gap between the window's components
     */
    public static final int COMPONENT_GAP = 20;

    private TermView view;
    private JTree tree;

    private JButton nodeLinkButton;
    private Action nodeLinkAction = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            KeYMediator mediator = MainWindow.getInstance().getMediator();

            if (!mediator.getSelectedProof().equals(getNode().proof())) {
                int choice = JOptionPane.showOptionDialog(
                        OriginTermLabelWindow.this,
                        "The proof containing this node is not currently selected."
                                + " Do you want to select it?",
                        "Switch Proof?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        null);

                if (choice == 0) {
                    mediator.getSelectionModel().setSelectedProof(getNode().proof());
                } else {
                    return;
                }
            }

            mediator.getSelectionModel().setSelectedNode(getNode());
        }
    };

    private Services services;
    private PosInOccurrence termPio;
    private Sequent sequent;

    /**
     * Creates a new {@link OriginTermLabelWindow}.
     *
     * @param mainWindow the main window.
     * @param pos the position of the term whose origin shall be visualized.
     * @param node the node representing the proof state for which the term's origins shall be
     *  visualized.
     * @param services services.
     */
    public OriginTermLabelWindow(PosInOccurrence pos, Node node, Services services) {
        super(node, "Origin for node " + node.serialNr() + ": "
                + (pos == null
                    ? "whole sequent"
                    : LogicPrinter.quickPrintTerm(pos.subTerm(), services)
                        .replaceAll("\\s+", " ")));

        // TermView can only print sequents or formulas, not terms.
        if (pos != null) {
            while (!pos.subTerm().sort().equals(Sort.FORMULA)) {
                pos = pos.up();
            }
        }

        this.services = services;
        this.termPio = pos;
        this.sequent = node.sequent();

        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(IconFactory.keyLogo());
        setLocationRelativeTo(null);
        setVisible(true);

        JMenuBar menuBar = new JMenuBar();
        {
            JMenu menu = new JMenu("Origin");
            menu.setMnemonic(KeyEvent.VK_O);

            JMenuItem gotoNodeItem = new JMenuItem();
            gotoNodeItem.setAction(nodeLinkAction);
            gotoNodeItem.setText("Go to node");
            gotoNodeItem.setToolTipText("Go to the proof node associated with this window");
            menu.add(gotoNodeItem);

            JMenuItem closeItem = new JMenuItem("Close");
            closeItem.setIcon(IconFactory.quit(16));
            closeItem.setToolTipText("Close this window");
            closeItem.addActionListener(event -> {
                OriginTermLabelWindow.this.dispose();
            });
            menu.add(closeItem);

            menuBar.add(menu);
            setJMenuBar(menuBar);
        }

        JPanel headPane = new JPanel();
        {

            headPane.add(new JLabel("Showing origin information for "));
            nodeLinkButton = new JButton();
            headPane.add(nodeLinkButton);
            headPane.add(new JLabel(" in proof \"" + node.proof().name().toString() + "\""));
            nodeLinkButton.setAction(nodeLinkAction);

            updateNodeLink();

            node.proof().addRuleAppListener(event -> {
                updateNodeLink();
            });
            node.proof().addProofTreeListener(new ProofTreeAdapter() {

                @Override
                public void proofStructureChanged(ProofTreeEvent e) {
                    updateNodeLink();
                }

                @Override
                public void proofPruned(ProofTreeEvent e) {
                    updateNodeLink();
                }

                @Override
                public void proofGoalsChanged(ProofTreeEvent e) {
                    updateNodeLink();
                }

                @Override
                public void proofExpanded(ProofTreeEvent e) {
                    updateNodeLink();
                }
            });
        }

        JSplitPane bodyPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bodyPane.setResizeWeight(0.5);
        bodyPane.setOneTouchExpandable(true);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(headPane, BorderLayout.PAGE_START);
        contentPane.add(bodyPane, BorderLayout.CENTER);
        setContentPane(contentPane);

        DefaultTreeModel treeModel = buildModel(pos);
        {
            tree = new JTree(treeModel);
            tree.setCellRenderer(new CellRenderer());
            ToolTipManager.sharedInstance().registerComponent(tree);

            tree.addTreeSelectionListener(e -> {
                TreeNode source = (TreeNode) tree.getLastSelectedPathComponent();

                if (source != null) {
                    ImmutableList<Integer> path = getPosTablePath(source.pos);

                    highlightInView(path);
                }

                revalidate();
                repaint();
            });

            JScrollPane treeScrollPane = new JScrollPane(tree,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            treeScrollPane.setBorder(new TitledBorder(TREE_TITLE));

            treeScrollPane.setPreferredSize(new Dimension(WIDTH / 2, HEIGHT));
            bodyPane.add(treeScrollPane);

            treeScrollPane.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    tree.setSize(treeScrollPane.getViewport().getSize());
                    tree.setUI(new BasicTreeUI());
                }
            });
        }

        {
            view = new TermView(pos, node, MainWindow.getInstance());
            view.setPreferredSize(new Dimension(WIDTH / 2, HEIGHT));

            view.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    PosInSequent pis = view.getLastPosInSequent();

                    if (pis == null) {
                        return;
                    }

                    ImmutableList<Integer> path = getPosTablePath(
                            convertPio(pis.getPosInOccurrence()));
                    highlightInView(path);
                    highlightInTree(getTreePath(path));

                    revalidate();
                    repaint();
                }
            });

            JScrollPane viewScrollPane = new JScrollPane(view,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            viewScrollPane.setBorder(new TitledBorder(
                    VIEW_TITLE + " (" + posSemisequentStr(pos) + ")"));

            view.printSequent();

            bodyPane.add(viewScrollPane);

            viewScrollPane.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    view.printSequent();
                }
            });
        }

        bodyPane.setDividerLocation(WIDTH / 2);
    }

    private String posSemisequentStr(PosInOccurrence pos) {
        if (pos == null) {
            return "whole sequent";
        } else if (pos.isInAntec()) {
            return "in antecendent";
        } else {
            return "in succedent";
        }
    }

    private void updateNodeLink() {
        Node node = getNode();

        if (!node.proof().find(node)) {
            nodeLinkButton.setText("DELETED NODE");
            nodeLinkAction.setEnabled(false);

            unregister(this);
        } else if (nodeLinkButton.isEnabled()) {
            nodeLinkButton.setText(node.serialNr() + ": " + node.name());
        }
    }

    /**
     * Convert a pio on the sequent to a pio on {@code this.termPio.subTerm()}.
     *
     * @param pio a pio on the sequent.
     * @return a pio on {@code this.termPio.subTerm()}.
     */
    private PosInOccurrence convertPio(PosInOccurrence pio) {
        if (termPio == null) {
            return pio;
        } else if (pio == null) {
            return new PosInOccurrence(
                    termPio.sequentFormula(),
                    termPio.posInTerm(),
                    termPio.isInAntec());
        } else {
            PosInTerm completePos = termPio.posInTerm();

            IntIterator it = pio.posInTerm().iterator();
            while (it.hasNext()) {
                completePos = completePos.down(it.next());
            }

            return new PosInOccurrence(
                    termPio.sequentFormula(),
                    completePos,
                    termPio.isInAntec());
        }
    }

    private DefaultTreeModel buildModel(PosInOccurrence pos) {
        TreeNode root = new TreeNode(pos);
        DefaultTreeModel result = new DefaultTreeModel(root);
        buildModel(root, pos, result);
        return result;
    }

    private void buildModel(
            TreeNode parentNode,
            PosInOccurrence parentPos,
            DefaultTreeModel treeModel) {
        if (parentPos == null) {
            int index = 0;

            ImmutableList<SequentFormula> children = sequent.antecedent().asList();

            for (SequentFormula child : children) {
                PosInOccurrence childPos = new PosInOccurrence(child, PosInTerm.getTopLevel(), true);
                TreeNode childNode = new TreeNode(childPos);

                treeModel.insertNodeInto(childNode, parentNode, index);
                buildModel(childNode, childPos, treeModel);

                ++index;
            }

            children = sequent.succedent().asList();

            for (SequentFormula child : children) {
                PosInOccurrence childPos = new PosInOccurrence(child, PosInTerm.getTopLevel(), false);
                TreeNode childNode = new TreeNode(childPos);

                treeModel.insertNodeInto(childNode, parentNode, index);
                buildModel(childNode, childPos, treeModel);

                ++index;
            }
        } else {
            ImmutableArray<Term> children = parentPos.subTerm().subs();

            for (int i = 0; i < children.size(); ++i) {
                TreeNode childNode = new TreeNode(parentPos.down(i));

                treeModel.insertNodeInto(childNode, parentNode, i);
                buildModel(childNode, parentPos.down(i), treeModel);
            }
        }
    }

    private void highlightInTree(TreePath path) {
        tree.getSelectionModel().setSelectionPath(path);
    }

    private void highlightInView(ImmutableList<Integer> path) {
        view.removeHighlight(view.getColorHighlight(HIGHLIGHT_COLOR));
        view.printSequent();

        try {
            Range range = view.posTable.rangeForPath(path);
            range = new Range(range.start() + 1, range.end() + 1);

            view.paintHighlight(range, view.getColorHighlight(HIGHLIGHT_COLOR));
        } catch (ArrayIndexOutOfBoundsException e) {
            // The path does not point to a valid sub-term.
            // E.g., this can happen if pretty-printing is activated and the user selects
            // the sub-term "#" of some number
            // (which only exists in the view when pretty-printing is deactivated.)
            // We simply ignore this error and do not paint any highlights.
        }
    }

    private ImmutableList<Integer> getPosTablePath(PosInOccurrence pos) {
        if (pos == null) {
            return ImmutableSLList.<Integer>nil().prepend(0);
        }

        InitialPositionTable posTable = view.posTable;

        ImmutableList<Integer> path = posTable.pathForPosition(pos, view.getFilter());

        if (termPio != null) {
            ImmutableList<Integer> prefixPath = posTable.pathForPosition(
                    termPio, view.getFilter());

            final int n = prefixPath.size();

            for (int i = 0; i < n; ++i) {
                assert path.head() == prefixPath.head();

                path = path.tail();
                prefixPath = prefixPath.tail();
            }

            path = path.prepend(0).prepend(0);
        }

        return path;
    }

    private TreePath getTreePath(ImmutableList<Integer> posTablePath) {
        if (termPio != null) {
            posTablePath = posTablePath.tail().tail();
        } else {
            posTablePath = posTablePath.tail();
        }

        TreeNode lastNode = (TreeNode) tree.getModel().getRoot();
        TreePath result = new TreePath(lastNode);

        if (posTablePath != null) {
            for (int i : posTablePath) {
                lastNode = (TreeNode) lastNode.getChildAt(i);

                result = result.pathByAddingChild(lastNode);
            }
        }

        return result;
    }

    /**
     * Returns the term's {@link OriginTermLabel}. If the term has no such label, iterates over its
     * parents until it finds one with a label.
     *
     * @param pio the position of the term.
     * @return the {@link OriginTermLabel} of the nearest possible parent term, or {@code null} if
     *  no parent term has an {@link OriginTermLabel}.
     */
    private OriginTermLabel getOriginLabel(PosInOccurrence pio) {
        if (pio == null) {
            return null;
        }

        Term term = pio.subTerm();
        OriginTermLabel originLabel =
                (OriginTermLabel) term.getLabel(OriginTermLabel.NAME);

        while (originLabel == null && !pio.isTopLevel()) {
            pio = pio.up();
            term = pio.subTerm();

            originLabel =
                    (OriginTermLabel) term.getLabel(OriginTermLabel.NAME);
        }

        return originLabel;
    }

    private String getTooltipText(PosInOccurrence pio) {
        if (pio == null) {
            return null;
        }

        OriginTermLabel label = getOriginLabel(pio);
        return "<html>Origin of selected term: <b>" + label.getOrigin() +
                "</b><hr>Origin of (former) sub-terms:<br>" +
                label.getSubtermOrigins().stream()
                .map(o -> "" + o + "<br>").reduce("", String::concat);
    }

    private class CellRenderer extends DefaultTreeCellRenderer {

        private static final long serialVersionUID = -7479404026154193661L;

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value,
                boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            TreeNode node = (TreeNode) value;

            PosInOccurrence pio = node.pos;
            Term term = node.term;
            assert pio.subTerm().equals(term);

            BasicTreeUI ui = (BasicTreeUI) tree.getUI();

            JLabel termTextLabel = (JLabel) super.getTreeCellRendererComponent(
                    tree, value, selected, expanded,
                    leaf, row, hasFocus);
            termTextLabel.setText(getShortTermText(term));
            termTextLabel.setBackground(OriginTermLabelWindow.this.getBackground());

            JLabel originTextLabel = new JLabel();
            OriginTermLabel originLabel = getOriginLabel(pio);

            if (originLabel != null) {
                originTextLabel.setText(getShortOriginText(originLabel.getOrigin()));
                originTextLabel.setHorizontalAlignment(SwingConstants.TRAILING);
            }

            JPanel result = new JPanel(new BorderLayout(TREE_CELL_GAP, TREE_CELL_GAP));

            final int indent =
                    (ui.getLeftChildIndent() + ui.getRightChildIndent()) * node.getLevel();

            result.setPreferredSize(new Dimension(
                    tree.getWidth() - indent,
                    super.getPreferredSize().height));

            result.add(termTextLabel, BorderLayout.LINE_START);
            result.add(originTextLabel, BorderLayout.LINE_END);

            result.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            result.setBackground(Color.WHITE);

            if (originLabel != null) {
                result.setToolTipText(OriginTermLabelWindow.this.getTooltipText(pio));
            }

            return result;
        }

        private String getShortOriginText(Origin origin) {
            return origin.specType.toString();
        }

        private String getShortTermText(Term term) {
            String text;

            if (term == null) {
                text = LogicPrinter.quickPrintSequent(sequent, services);
            } else {
                text = LogicPrinter.quickPrintTerm(term, services);
            }

            int endIndex = text.indexOf("\n");

            if (endIndex != text.length() - 1) {
                return text.replaceAll("\\s+", " ") + " ...";
            } else {
                return text.substring(0, text.indexOf("\n")).replaceAll("\\s+", " ");
            }
        }
    }

    private class TreeNode extends DefaultMutableTreeNode {

        private PosInOccurrence pos;
        private Term term;

        private TreeNode(PosInOccurrence pos) {
            super(pos);
            this.pos = pos;

            if (pos != null) {
                this.term = pos.subTerm();
            }
        }
    }

    private class TermView extends SequentView {

        private InitialPositionTable posTable = new InitialPositionTable();
        private Node node;

        TermView(PosInOccurrence pos, Node node, MainWindow mainWindow) {
            super(mainWindow);
            this.node = node;

            final NotationInfo ni = new NotationInfo();
            if (services != null) {
                ni.refresh(services,
                        NotationInfo.DEFAULT_PRETTY_SYNTAX, NotationInfo.DEFAULT_UNICODE_ENABLED);
            }

            setLogicPrinter(new SequentViewLogicPrinter(
                    new ProgramPrinter(), ni, services, new TermLabelVisibilityManager()) {

                @Override
                public void printSequent(SequentPrintFilter filter,
                        boolean finalbreak) {
                    try {
                        ImmutableList<SequentPrintFilterEntry> antec = filter.getFilteredAntec();
                        ImmutableList<SequentPrintFilterEntry> succ  = filter.getFilteredSucc();
                        markStartSub();
                        startTerm(antec.size()+succ.size());
                        layouter.beginC(1).ind();
                        printSemisequent(antec);

                        if (pos == null) {
                            layouter.brk(1,-1);
                            printSequentArrow();
                            layouter.brk(1);
                        }

                        printSemisequent(succ);
                        if (finalbreak) {
                            layouter.brk(0);
                        }
                        markEndSub();
                        layouter.end();
                    } catch (IOException e) {
                        throw new RuntimeException (
                                "IO Exception in pretty printer:\n"+e);
                    } catch (UnbalancedBlocksException e) {
                        throw new RuntimeException (
                                "Unbalanced blocks in pretty printer:\n"+e);
                    }
                }
            });

            if (pos != null) {
                setFilter(new ShowSelectedSequentPrintFilter(pos));
            } else {
                setFilter(new IdentitySequentPrintFilter());
            }

            // Register tooltip
            setToolTipText("");
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            PosInSequent pis = getPosInSequent(event.getPoint());

            if (pis == null) {
                return null;
            }

            return OriginTermLabelWindow.this.getTooltipText(convertPio(pis.getPosInOccurrence()));
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Point p = event.getPoint();
            return new Point(p.x + 10, p.y);
        }

        @Override
        public SequentPrintFilter getFilter() {
            return super.getFilter();
        }

        @Override
        public String getTitle() {
            return "Selected term";
        }

        @Override
        public boolean isMainSequentView() {
            return false;
        }

        @Override
        public final synchronized void printSequent() {
            getLogicPrinter().update(getFilter(), computeLineWidth());
            setText(getSyntaxHighlighter().process(getLogicPrinter().toString(), node));
            posTable = getLogicPrinter().getInitialPositionTable();

            updateHidingProperty();
        }
    }
}
