package org.key_project.ui.interactionlog;

import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.extension.api.KeYGuiExtension;
import de.uka.ilkd.key.gui.fonticons.KeYIcons;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Weigl
 * @version 1 (13.02.19)
 */
@KeYGuiExtension.Info(name = "Interaction Logging", optional = true, priority = 10000)
public class InteractionLogExt implements KeYGuiExtension,
        KeYGuiExtension.LeftPanel, KeYGuiExtension.MainMenu {
    private static final Icon INTERACTION_LOG_ICON = KeYIcons.INTERLOG_ICON.getIcon();

    private InteractionLogView interactionLogView = new InteractionLogView();

    @Override
    public void init(MainWindow window, KeYMediator mediator) {
        interactionLogView.setMediator(mediator);
        interactionLogView.setMainWindow(window);
    }

    @Override
    public String getTitle() {
        return "Interaction Log";
    }

    @Override
    public Icon getIcon() {
        return INTERACTION_LOG_ICON;
    }

    @Override
    public JComponent getComponent() {
        return interactionLogView;
    }

    @Override
    public int priority() {
        return 1500;
    }

    @Override
    public List<Action> getMainMenuActions(MainWindow mainWindow) {
        return Arrays.asList(
                interactionLogView.getActionAddUserNote(),
                interactionLogView.getActionExportProofScript(),
                interactionLogView.getActionJumpIntoTree(),
                interactionLogView.getActionLoad(),
                interactionLogView.getActionSave(),
                interactionLogView.getActionTryReapply(),
                interactionLogView.getActionKPSExport(),
                interactionLogView.getActionToggleFavourite(),
                interactionLogView.getActionExportMarkdown(),
                interactionLogView.getActionMUCopyClipboard(),
                interactionLogView.getActionPauseLogging());
    }
}
