package de.uka.ilkd.key.gui.ext.exploration;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JOptionPane;

import de.uka.ilkd.key.core.KeYSelectionEvent;
import de.uka.ilkd.key.core.KeYSelectionListener;
import de.uka.ilkd.key.gui.IconFactory;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.actions.MainWindowAction;
import de.uka.ilkd.key.gui.ext.KeYExtConst;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.label.OriginTermLabel;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.settings.TermLabelSettings;

/**
 * Action to toggle {@link TermLabelSettings#getUseOriginLabels()}.
 *
 * @author lanzinger
 */
public class ToggleOriginLabelsAction extends MainWindowAction {

    /**
     * Create a new action.
     *
     * @param mainWindow the main window.
     */
    public ToggleOriginLabelsAction(MainWindow mainWindow) {
        super(mainWindow);

        setName("Toggle Origin Labels");
        setIcon(IconFactory.originIcon(MainWindow.TOOLBAR_ICON_SIZE));
        setEnabled(getMediator().getSelectedProof() != null);

        getMediator().addKeYSelectionListener(new KeYSelectionListener() {

            @Override
            public void selectedProofChanged(KeYSelectionEvent e) {
                setEnabled(getMediator().getSelectedProof() != null);

                handleAction();
            }

            @Override
            public void selectedNodeChanged(KeYSelectionEvent e) {
                setEnabled(getMediator().getSelectedProof() != null);
            }
        });

        putValue(KeYExtConst.PATH, "Origin Term Labels");
        putValue(Action.LONG_DESCRIPTION, "Toggle origin labels.");
    }

    private void handleAction() {
        Proof proof = mainWindow.getMediator().getSelectedProof();

        if (proof != null) {
            Services services = proof.getServices();
            TermLabelSettings settings = proof.getSettings().getTermLabelSettings();

            if (!settings.getUseOriginLabels()) {
                for (Proof p : services.getSpecificationRepository().getAllProofs()) {
                    for (Goal g : p.openGoals()) {
                        g.setSequent(OriginTermLabel.removeOriginLabels(g.sequent(), services));
                    }
                }

                services.getSpecificationRepository().map(
                        term -> OriginTermLabel.removeOriginLabels(term, services),
                        services);
            }

            mainWindow.getMediator().getSelectionModel().fireSelectedNodeChanged();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Proof proof = mainWindow.getMediator().getSelectedProof();

        if (proof != null) {
            TermLabelSettings settings = proof.getSettings().getTermLabelSettings();
            settings.setUseOriginLabels(!settings.getUseOriginLabels());
            handleAction();

            if (settings.getUseOriginLabels()) {
                JOptionPane.showMessageDialog(
                        mainWindow,
                        "Origin labels will be added when the proof is reloaded.",
                        "Origin",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        mainWindow,
                        "Origin labels have been removed from "
                                + "all open goals and all proof obligations.",
                        "Origin",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}
