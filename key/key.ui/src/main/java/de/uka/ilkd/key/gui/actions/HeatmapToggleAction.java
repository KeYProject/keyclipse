package de.uka.ilkd.key.gui.actions;

import de.uka.ilkd.key.core.KeYSelectionEvent;
import de.uka.ilkd.key.core.KeYSelectionListener;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.extension.api.KeYExtConstants;
import de.uka.ilkd.key.gui.fonticons.KeYIcons;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.settings.ProofIndependentSettings;
import de.uka.ilkd.key.settings.SettingsListener;
import de.uka.ilkd.key.settings.ViewSettings;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class HeatmapToggleAction extends MainWindowAction {
    private static final long serialVersionUID = 1L;

    public static final Icon ICON_SELECTED = KeYIcons.HEATMAP_DEACTIVATE.getIcon();
    public static final Icon ICON_NOT_SELECTED = KeYIcons.HEATMAP_ACTIVATE.getIcon();

    public HeatmapToggleAction(MainWindow mainWindow) {
        super(mainWindow);
        setName("Toggle heatmap");
        putValue(KeYExtConstants.PATH, "Heatmap");
        setEnabled(getMediator().getSelectedProof() != null);
        putValue(Action.LONG_DESCRIPTION, "Enable or disable age heatmaps in the sequent view.");

        setIcon();
        addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(SELECTED_KEY)) {
                setIcon();
            }
        });

        ViewSettings vs = ProofIndependentSettings.DEFAULT_INSTANCE.getViewSettings();
        setSelected(vs.isShowHeatmap());
        final SettingsListener setListener = e -> setSelected(vs.isShowHeatmap());
        vs.addSettingsListener(setListener);

        final KeYSelectionListener selListener = new KeYSelectionListener() {
            @Override
            public void selectedNodeChanged(KeYSelectionEvent e) {
                final Proof proof = getMediator().getSelectedProof();
                setEnabled(proof != null);
            }

            @Override
            public void selectedProofChanged(KeYSelectionEvent e) {
                selectedNodeChanged(e);
            }
        };
        getMediator().addKeYSelectionListener(selListener);
    }

    private void setIcon() {
        setIcon(isSelected()
                ? ICON_SELECTED
                : ICON_NOT_SELECTED);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ViewSettings vs = ProofIndependentSettings.DEFAULT_INSTANCE.getViewSettings();
        vs.setHeatmapOptions(!vs.isShowHeatmap(), vs.isHeatmapSF(),
                vs.isHeatmapNewest(), vs.getMaxAgeForHeatmap());
    }
}
