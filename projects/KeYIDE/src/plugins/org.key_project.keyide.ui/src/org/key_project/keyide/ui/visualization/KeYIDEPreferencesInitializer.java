package org.key_project.keyide.ui.visualization;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.key_project.keyide.ui.util.KeYIDEPreferences;

public class KeYIDEPreferencesInitializer extends AbstractPreferenceInitializer {
   /**
    * {@inheritDoc}
    */
   @Override
   public void initializeDefaultPreferences() {
      KeYIDEPreferences.setDefaultSwitchToKeyPerspective(MessageDialogWithToggle.PROMPT);
   }
}
