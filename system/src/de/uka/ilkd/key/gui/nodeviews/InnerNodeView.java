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

import java.awt.Color;
import java.util.Iterator;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.configuration.Config;
import de.uka.ilkd.key.gui.configuration.ConfigChangeAdapter;
import de.uka.ilkd.key.gui.configuration.ConfigChangeListener;
import de.uka.ilkd.key.gui.notification.events.GeneralFailureEvent;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.PosInTerm;
import de.uka.ilkd.key.logic.op.FormulaSV;
import de.uka.ilkd.key.logic.op.ModalOperatorSV;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.logic.op.SchemaVariable;
import de.uka.ilkd.key.logic.op.SkolemTermSV;
import de.uka.ilkd.key.logic.op.TermSV;
import de.uka.ilkd.key.logic.op.UpdateSV;
import de.uka.ilkd.key.logic.op.VariableSV;
import de.uka.ilkd.key.pp.IdentitySequentPrintFilter;
import de.uka.ilkd.key.pp.InitialPositionTable;
import de.uka.ilkd.key.pp.LogicPrinter;
import de.uka.ilkd.key.pp.ProgramPrinter;
import de.uka.ilkd.key.pp.Range;
import de.uka.ilkd.key.pp.SequentPrintFilter;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import de.uka.ilkd.key.rule.IfFormulaInstSeq;
import de.uka.ilkd.key.rule.IfFormulaInstantiation;
import de.uka.ilkd.key.rule.NewDependingOn;
import de.uka.ilkd.key.rule.NewVarcond;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.rule.TacletApp;
import de.uka.ilkd.key.rule.inst.GenericSortInstantiations;

public class InnerNodeView extends SequentView {
    
    private SequentPrintFilter filter;
    private InitialPositionTable posTable;
    private ConfigChangeListener configChangeListener = new ConfigChangeAdapter(this);

    private static void writeSVModifiers(StringBuffer out, SchemaVariable sv) {
        boolean started = false;
        if (sv.isRigid() && !(sv instanceof VariableSV)) {
            if (!started) {
                out.append("[");
            }
            out.append("rigid");
            started = true;
        }
        if (sv instanceof ProgramSV && ((ProgramSV) sv).isListSV()) {
            if (!started) {
                out.append("[");
            } else {
                out.append(", ");
            }
            out.append("list");
            started = true;
        }

        if (started) {
            out.append("]");
        }
    }

    private static void writeTacletSchemaVariable(StringBuffer out,
            SchemaVariable schemaVar) {
        if (schemaVar instanceof ModalOperatorSV) {
            final ModalOperatorSV modalOpSV = (ModalOperatorSV) schemaVar;
            String sep = "";
            for (final Operator op : modalOpSV.getModalities()) {
                out.append(sep);
                out.append(op.name());
                sep = ", ";
            }
            out.append(" } " + modalOpSV.name());
        } else if (schemaVar instanceof TermSV) {
            out.append("\\term");
        } else if (schemaVar instanceof FormulaSV) {
            out.append("\\formula");
        } else if (schemaVar instanceof UpdateSV) {
            out.append("\\update");
        } else if (schemaVar instanceof ProgramSV) {
            out.append("\\program");
        } else if (schemaVar instanceof VariableSV) {
            out.append("\\variables");
        } else if (schemaVar instanceof SkolemTermSV) {
            out.append("\\skolemTerm");
        } else {
            out.append("?");
        }
        writeSVModifiers(out, schemaVar);
        if (!(schemaVar instanceof FormulaSV || schemaVar instanceof UpdateSV)) {
            out.append(" " + schemaVar.sort().declarationString());
        }
        out.append(" " + schemaVar.name());
    }

    public static void writeTacletSchemaVariablesHelper(StringBuffer out,
            final Taclet t) {
        ImmutableSet<SchemaVariable> schemaVars = t.getIfFindVariables();
        ImmutableList<NewVarcond> lnew = t.varsNew();
        while (!lnew.isEmpty()) {
            schemaVars = schemaVars.add(lnew.head().getSchemaVariable());
            lnew = lnew.tail();
        }
        Iterator<NewDependingOn> newDepIt = t.varsNewDependingOn();
        while (newDepIt.hasNext()) {
            schemaVars = schemaVars.add(newDepIt.next().first());
        }

        if (schemaVars.size() > 0) {
            out.append("\\schemaVariables {\n");
            for (SchemaVariable schemaVar1 : schemaVars) {
                final SchemaVariable schemaVar = schemaVar1;
                // write indentation
                out.append("  ");
                // write declaration
                writeTacletSchemaVariable(out, schemaVar);
                // write newline
                out.append(";\n");
            }
            out.append("}\n");
        }
    }
    String sequentOnly;
    String tacletDescription;
    public boolean showTaclet = false;

    void toggleText() {
        showTaclet = !showTaclet;
        if (showTaclet) {
            setText(sequentOnly + "\n" + tacletDescription);
        } else {
            setText(sequentOnly);
        }
    }

    public InnerNodeView(Node node, KeYMediator mediator) {

        filter = new IdentitySequentPrintFilter(node.sequent());
        LogicPrinter printer = new LogicPrinter(new ProgramPrinter(null),
                mediator.getNotationInfo(),
                mediator.getServices());
        printer.printSequent(null, filter);
        sequentOnly = printer.toString();
        tacletDescription = getTacletDescription(mediator, node, filter);
        setText(sequentOnly);
        posTable = printer.getInitialPositionTable();
        Config.DEFAULT.addConfigChangeListener(configChangeListener);
        updateUI();

        RuleApp app = node.getAppliedRuleApp();
        if (app != null) {
            highlightRuleAppPosition(app);
        }

        /*
         * Few custom options for the button in the top row of each 
         * SequentView
         */
        titleButton.setBorderColor(new Color(140, 170, 120));
        titleButton.setToolTipText("Toggle taclet info");

    }

    /**
     * <p>
     * Computes the text to show in this {@link JTextArea} which consists of the
     * sequent including the applied rule.
     * </p>
     * <p>
     * This information is also relevant for other tools like the Symbolic
     * Execution Debugger.
     * </p>
     *
     * @param mediator The {@link KeYMediator} to use.
     * @param node The {@link Node} to use.
     * @param filter The {@link SequentPrintFilter} to use.
     * @param printer The {@link LogicPrinter} to use.
     * @return The text to show.
     */
    public String getTacletDescription(KeYMediator mediator,
            Node node,
            SequentPrintFilter filter) {

        RuleApp app = node.getAppliedRuleApp();
        String s = "";

        if (app != null) {
            s += "The following rule was applied on this node: \n\n";
            if (app.rule() instanceof Taclet) {
                LogicPrinter tacPrinter = new LogicPrinter(new ProgramPrinter(null),
                        mediator.getNotationInfo(),
                        mediator.getServices(),
                        true);
                tacPrinter.printTaclet((Taclet) (app.rule()));
                s += tacPrinter;
            } else {
                s = s + app.rule();
            }

            if (app instanceof TacletApp) {
                TacletApp tapp = (TacletApp) app;
                if (tapp.instantiations().getGenericSortInstantiations()
                        != GenericSortInstantiations.EMPTY_INSTANTIATIONS) {
                    s = s + "\n\nWith sorts:\n";
                    s = s
                            + tapp.instantiations().getGenericSortInstantiations();
                }

                StringBuffer sb = new StringBuffer("\n\n");
                writeTacletSchemaVariablesHelper(sb, tapp.taclet());
                s = s + sb;
            }

//                  s = s + "\n\nApplication justified by: ";
//                  s = s + mediator.getSelectedProof().env().getJustifInfo()
//                                      .getJustification(app, mediator.getServices())+"\n";            
        } else {
            // Is this case possible?
            s += "No rule was applied on this node.";
        }
        return s;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Config.DEFAULT.addConfigChangeListener(configChangeListener);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        unregisterListener();
    }

    public void unregisterListener() {
        if (configChangeListener != null) {
            Config.DEFAULT.removeConfigChangeListener(configChangeListener);
        }
    }

    @Override
    protected void finalize() {
        try {
            unregisterListener();
        } catch (Throwable e) {
            MainWindow.getInstance().notify(new GeneralFailureEvent(e.getMessage()));
        } finally {
            try {
                super.finalize();
            } catch (Throwable e) {
                MainWindow.getInstance().notify(new GeneralFailureEvent(e.getMessage()));
            }
        }
    }
    static final Highlighter.HighlightPainter RULEAPP_HIGHLIGHTER =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(0.5f, 1.0f, 0.5f, 0.4f));
    static final Highlighter.HighlightPainter IF_FORMULA_HIGHLIGHTER =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(0.8f, 1.0f, 0.8f, 0.5f));

    private void highlightRuleAppPosition(RuleApp app) {
        try {
            setHighlighter(new DefaultHighlighter());

            // Set the find highlight first and then the if highlights
            // This seems to make cause the find one to be painted 
            // over the if one.

            final Range r;
            if (app.posInOccurrence() == null) {
                // rule does not have a find-part
                r = null;
            } else {
                r = highlightPos(app.posInOccurrence(), RULEAPP_HIGHLIGHTER);
            }

            if (app instanceof TacletApp) {
                highlightIfFormulas((TacletApp) app);
            } else if (app instanceof IBuiltInRuleApp) {
                highlightIfInsts((IBuiltInRuleApp) app);
            }

        } catch (BadLocationException badLocation) {
            System.err.println("NonGoalInfoView tried to "
                    + "highlight an area "
                    + "that does not exist.");
            System.err.println("Exception:" + badLocation);
        }
    }

    /**
     * @param tapp The taclet app for which the if formulae should be
     * highlighted.
     * @throws BadLocationException
     */
    private void highlightIfFormulas(TacletApp tapp)
            throws BadLocationException {
        final ImmutableList<IfFormulaInstantiation> ifs = tapp.ifFormulaInstantiations();
        if (ifs == null) {
            return;
        }
        for (final IfFormulaInstantiation inst2 : ifs) {
            if (!(inst2 instanceof IfFormulaInstSeq)) {
                continue;
            }
            final IfFormulaInstSeq inst = (IfFormulaInstSeq) inst2;
            final PosInOccurrence pos =
                    new PosInOccurrence(inst.getConstrainedFormula(),
                    PosInTerm.TOP_LEVEL,
                    inst.inAntec());
            final Range r = highlightPos(pos, IF_FORMULA_HIGHLIGHTER);
        }
    }

    private void highlightIfInsts(IBuiltInRuleApp bapp)
            throws BadLocationException {
        final ImmutableList<PosInOccurrence> ifs = bapp.ifInsts();
        for (PosInOccurrence pio : ifs) {
            final Range r = highlightPos(pio, IF_FORMULA_HIGHLIGHTER);
        }
    }

    /**
     * @param pos the PosInOccurrence that should be highlighted.
     * @param light the painter for the highlight.
     * @return the range of characters that was highlighted.
     * @throws BadLocationException
     */
    private Range highlightPos(PosInOccurrence pos,
            HighlightPainter light)
            throws BadLocationException {
        ImmutableList<Integer> path = posTable.pathForPosition(pos, filter);
        Range r = posTable.rangeForPath(path);
        getHighlighter().addHighlight(r.start(), r.end(), light);
        return r;
    }

    public String getTitle() {
        return "Inner Node";
    }
}
