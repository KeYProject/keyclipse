package de.uka.ilkd.key.gui.utilities;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.LinkedHashMap;

import javax.swing.KeyStroke;

import de.uka.ilkd.key.gui.actions.MainWindowAction;
import de.uka.ilkd.key.gui.macros.ProofMacro;

/**
 * Manages keyboard shortcuts for proof macros and GUI actions.
 * @author bruns
 *
 */
public final class KeyStrokeManager {
    
    /**
     * If true, F keys are used for macros, otherwise CTRL+SHIFT+letter.
     */
    private static final boolean FKEY_MACRO_SCHEME = Boolean.getBoolean("key.gui.fkeyscheme");

    /**
     * This constant holds the typical key to be used for shortcuts
     * (usually {@link java.awt.Event#CTRL_MASK})
     */
    private static final int SHORTCUT_KEY_MASK = 
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    /**
     * This constant holds the typical key combination to be used for auxiliary shortcuts
     * ({@link java.awt.Event#SHIFT_DOWN_MASK} plus usually {@link java.awt.Event#CTRL_MASK})
     */
    private static final int MULTI_KEY_MASK = SHORTCUT_KEY_MASK | KeyEvent.SHIFT_DOWN_MASK;
    
    private static Map<Class<?>, KeyStroke> mapping = new LinkedHashMap<Class<?>, KeyStroke>(30);
    
    static {
        if (FKEY_MACRO_SCHEME) {
            // use F keys for macros, CTRL+SHIFT+letter for other actions
            mapping.put(de.uka.ilkd.key.gui.macros.FullAutoPilotProofMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
            mapping.put(de.uka.ilkd.key.gui.macros.AutoPilotPrepareProofMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F2,0));
            mapping.put(de.uka.ilkd.key.gui.macros.PropositionalExpansionMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F3,0));
            mapping.put(de.uka.ilkd.key.gui.macros.FullPropositionalExpansionMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F4,0));
            mapping.put(de.uka.ilkd.key.gui.macros.TryCloseMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F5,0));
            mapping.put(de.uka.ilkd.key.gui.macros.FinishSymbolicExecutionMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
            mapping.put(de.uka.ilkd.key.gui.macros.OneStepProofMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_F7,0));
            
            mapping.put(de.uka.ilkd.key.gui.actions.QuickSaveAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_S,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.actions.QuickLoadAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_O,MULTI_KEY_MASK));
        } else {
            // use CTRL+SHIFT+letter for macros, F keys for other actions
            mapping.put(de.uka.ilkd.key.gui.macros.FullAutoPilotProofMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_V,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.macros.AutoPilotPrepareProofMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_D,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.macros.PropositionalExpansionMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_A,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.macros.FullPropositionalExpansionMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_S,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.macros.TryCloseMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_C,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.macros.FinishSymbolicExecutionMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_X,MULTI_KEY_MASK));
            mapping.put(de.uka.ilkd.key.gui.macros.OneStepProofMacro.class, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,MULTI_KEY_MASK));
            
            mapping.put(de.uka.ilkd.key.gui.actions.QuickSaveAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_F5,0));
            mapping.put(de.uka.ilkd.key.gui.actions.QuickLoadAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
        }

        // default mappings
        mapping.put(de.uka.ilkd.key.gui.actions.AboutAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_HELP,SHORTCUT_KEY_MASK));
        mapping.put(de.uka.ilkd.key.gui.actions.OpenExampleAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_E,MULTI_KEY_MASK));
        mapping.put(de.uka.ilkd.key.gui.actions.EditMostRecentFileAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_E,SHORTCUT_KEY_MASK));
        mapping.put(de.uka.ilkd.key.gui.actions.OneStepSimplificationToggleAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_T,MULTI_KEY_MASK));
        mapping.put(de.uka.ilkd.key.gui.actions.PrettyPrintToggleAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_P,MULTI_KEY_MASK));
        mapping.put(de.uka.ilkd.key.gui.actions.UnicodeToggleAction.class, KeyStroke.getKeyStroke(KeyEvent.VK_U,MULTI_KEY_MASK));
    }
    
    public static KeyStroke get (ProofMacro macro) {
        return mapping.get(macro.getClass());
    }
    
    public static KeyStroke get (MainWindowAction action) {
        return mapping.get(action.getClass());
    }

}
