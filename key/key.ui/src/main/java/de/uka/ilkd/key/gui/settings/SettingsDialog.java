package de.uka.ilkd.key.gui.settings;

import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.actions.KeyAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Weigl
 * @version 1 (08.04.19)
 */
public class SettingsDialog extends JDialog {
    private final MainWindow mainWindow;
    private final SettingsUi ui;
    private Action actionCancel = new CancelAction();
    private Action actionAccept = new AcceptAction();
    private Action actionApply = new ApplyAction();
    private List<SettingsProvider> providers;

    public SettingsDialog(MainWindow owner) {
        super(owner, Dialog.ModalityType.TOOLKIT_MODAL);
        setTitle("Settings");

        mainWindow = owner;
        ui = new SettingsUi(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.add(ui);
        JPanel buttonBar = createButtonBar();
        root.add(buttonBar, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(600, 400);
    }

    private JPanel createButtonBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton(actionCancel);
        JButton btnApply = new JButton(actionApply);
        JButton btnAccept = new JButton(actionAccept);
        p.add(btnAccept);
        p.add(btnApply);
        p.add(btnCancel);
        return p;
    }

    public void setSettingsProvider(List<SettingsProvider> providers) {
        this.providers = providers;
        this.ui.setSettingsProvider(providers);
    }

    SettingsUi getUi() {
        return ui;
    }

    private List<Exception> apply() {
        List<Exception> exc = new LinkedList<>();
        for (SettingsProvider it : providers) {
            try {
                it.applySettings(mainWindow);
            } catch (Exception e) {
                exc.add(e);
            }
        }
        return exc;
    }

    private boolean showErrors(List<Exception> apply) {
        if (!apply.isEmpty()) {
            String msg = apply.stream().map(Throwable::getMessage)
                    .collect(Collectors.joining("<br>", "<html>", "</html>"));
            JOptionPane.showMessageDialog(this, msg,
                    "Error in Settings",
                    JOptionPane.ERROR_MESSAGE);
        }
        return apply.isEmpty();
    }

    private class CancelAction extends KeyAction {
        public CancelAction() {
            setName("Cancel");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }

    private class AcceptAction extends KeyAction {
        public AcceptAction() {
            setName("Accept");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(!showErrors(apply()));
        }
    }

    private class ApplyAction extends KeyAction {
        public ApplyAction() {
            setName("Apply");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showErrors(apply());
        }
    }
}
