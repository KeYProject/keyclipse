package de.uka.ilkd.key.gui.extension.impl;

import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.actions.KeyAction;
import de.uka.ilkd.key.gui.extension.api.ContextMenuAdapter;
import de.uka.ilkd.key.gui.extension.api.ContextMenuKind;
import de.uka.ilkd.key.gui.extension.api.KeYGuiExtension;
import de.uka.ilkd.key.gui.fonticons.FontAwesomeSolid;
import de.uka.ilkd.key.gui.fonticons.IconFontSwing;
import de.uka.ilkd.key.gui.settings.InvalidSettingsInputException;
import de.uka.ilkd.key.gui.settings.SettingsProvider;
import de.uka.ilkd.key.pp.PosInSequent;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.rule.Rule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexander Weigl
 * @version 1 (16.04.19)
 */
@KeYGuiExtension.Info(name = "Test Extension",
        description = "Should only be used for testing of the extension facade",
        priority = 100000,
        optional = true)
public class TestExtension implements KeYGuiExtension,
        KeYGuiExtension.MainMenu,
        KeYGuiExtension.LeftPanel,
        KeYGuiExtension.StatusLine,
        KeYGuiExtension.ContextMenu,
        KeYGuiExtension.Toolbar,
        KeYGuiExtension.Settings {

    KeyAction actionTest = new TestAction();
    private ContextMenuAdapter cmAdapter = new ContextMenuAdapter() {
        @Override
        public List<Action> getContextActions(KeYMediator mediator, ContextMenuKind kind, Proof underlyingObject) {
            return Collections.singletonList(actionTest);
        }

        @Override
        public List<Action> getContextActions(KeYMediator mediator, ContextMenuKind kind, Node underlyingObject) {
            return Collections.singletonList(actionTest);
        }

        @Override
        public List<Action> getContextActions(KeYMediator mediator, ContextMenuKind kind, PosInSequent underlyingObject) {
            return Collections.singletonList(actionTest);
        }

        @Override
        public List<Action> getContextActions(KeYMediator mediator, ContextMenuKind kind, Rule underlyingObject) {
            return Collections.singletonList(actionTest);
        }
    };


    @Override
    public List<Action> getMainMenuActions(MainWindow mainWindow) {
        return Collections.singletonList(actionTest);
    }

    @Override
    public void init(MainWindow window, KeYMediator mediator) {
        System.out.println("TestExtension.init");
    }

    @Override
    public String getTitle() {
        return "Test!";
    }

    @Override
    public JComponent getComponent() {
        return new JLabel("Test");
    }

    @Override
    public List<Action> getContextActions(KeYMediator mediator, ContextMenuKind kind, Object underlyingObject) {
        return cmAdapter.getContextActions(mediator, kind, underlyingObject);
        }

    @Override
    public JToolBar getToolbar(MainWindow mainWindow) {
        JToolBar bar = new JToolBar();
        bar.add(actionTest);
        return bar;
    }

    @Override
    public List<JComponent> getStatusLineComponents() {
        return Collections.singletonList(new JButton(actionTest));
    }

    @Override
    public SettingsProvider getSettings() {
        return new TestSettingsProvider();
    }

    private class TestAction extends KeyAction {
        public TestAction() {
            setName("Test");
            setMenuPath("Test.Test.Test");
            setIcon(IconFontSwing.buildIcon(FontAwesomeSolid.TEETH, 16, Color.BLUE));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null, "Test!");
        }
    }

    private class TestSettingsProvider implements SettingsProvider {
        @Override
        public String getDescription() {
            return "Test Settings";
        }

        @Override
        public JComponent getPanel(MainWindow window) {
            return new JLabel("Test");
        }

        @Override
        public void applySettings(MainWindow window) throws InvalidSettingsInputException {
            System.out.println("TestSettingsProvider.applySettings");
        }
    }
}
