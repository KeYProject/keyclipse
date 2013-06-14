package de.uka.ilkd.key.gui.actions;

import java.awt.event.ActionEvent;

import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.configuration.ProofIndependentSettings;

public class AutoSave extends MainWindowAction {

    /**
     * 
     */
    private static final long serialVersionUID = -2598146925208531491L;

    public AutoSave(MainWindow mainWindow) {
        super(mainWindow);
        setTooltip("If ticked, side-proofs are saved automatically (without save dialog pop up)," +
        		"when they are being left.");
        setName("Auto Save");
        setSelected(ProofIndependentSettings.DEFAULT_INSTANCE.getGeneralSettings().autoSave());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean sel = isSelected();
        ProofIndependentSettings.DEFAULT_INSTANCE.getGeneralSettings().autoSave(sel);
    }

}
