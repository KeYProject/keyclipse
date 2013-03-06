package de.uka.ilkd.key.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/*
 * Abstract parent class of SequentSearchBar and ProofTreeSearchPanel.
 * Might be used for additional search bars.
 */
public abstract class SearchPanel extends JPanel {

    public JTextField searchField = new JTextField(20);
    private JButton prev = new JButton("Prev");
    private JButton next = new JButton("Next");
    private JButton close = new JButton("Close");
    private final Color ALLERT_COLOR = new Color(255, 178, 178);

    public SearchPanel() {

        // Initialize the Actionlisteners here:

        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });

        next.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                searchNext();
                searchField.requestFocusInWindow();
            }
        });

        prev.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                searchPrevious();
                searchField.requestFocusInWindow();
            }
        });

        searchField.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchNext();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

        registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        searchField.getDocument()
                .addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                search();
            }

            public void insertUpdate(DocumentEvent e) {
                search();
            }

            public void removeUpdate(DocumentEvent e) {
                search();
            }
        });

        createUI();
        setVisible(false);
    }

    @Override
    public void setVisible(boolean vis) {
        super.setVisible(vis);
        if (vis) {
            searchField.selectAll();
            searchField.requestFocus();
        }
    }

    public abstract void searchPrevious();

    public abstract void searchNext();

    /* The boolean return value of this function indicates,
     * whether search was successful or not.
     */
    public abstract boolean search(String s);

    public void search() {
        boolean b = search(searchField.getText());
        if (b) {
            searchField.setBackground(Color.WHITE);
        } else {
            searchField.setBackground(ALLERT_COLOR);
        }
    }

    /* Override this method in case you want a custom UI
     * for the search bar.
     */
    public void createUI() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        add(new JLabel("Search: "));
        add(searchField);
        add(prev);
        prev.setMargin(new java.awt.Insets(2,1,2,1));
        add(next);
        next.setMargin(new java.awt.Insets(2,1,2,1));
        add(close);
        close.setMargin(new java.awt.Insets(2,1,2,1));
    }
}