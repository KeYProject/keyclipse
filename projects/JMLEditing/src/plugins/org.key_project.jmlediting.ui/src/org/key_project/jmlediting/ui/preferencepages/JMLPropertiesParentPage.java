package org.key_project.jmlediting.ui.preferencepages;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPropertyPage;

/**
 * This class provides the top level entry for JML preferences in properties or
 * preference windows. Currently it does not contain anything.
 *
 * @author Moritz Lichter
 *
 */
@SuppressWarnings("restriction")
public class JMLPropertiesParentPage extends PropertyAndPreferencePage
      implements IWorkbenchPropertyPage {

   /**
    * Creates a new instance.
    */
   public JMLPropertiesParentPage() {
   }

   @Override
   protected Control createContents(final Composite parent) {
      final Group group = new Group(parent, SWT.NONE);
      group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      group.setLayout(new GridLayout(1, true));
      group.setText("JML Editing");
      final Button button = new Button(group, SWT.CHECK);
      button.setText("Use Support for JML");
      // TODO: Selection undone
      button.setSelection(true);
      return parent;
   }

   @Override
   protected Control createPreferenceContent(final Composite composite) {
      return null;
   }

   @Override
   protected boolean hasProjectSpecificOptions(final IProject project) {
      return false;
   }

   @Override
   protected String getPreferencePageID() {
      return null;
   }

   @Override
   protected String getPropertyPageID() {
      return null;
   }

   @Override
   protected void performDefaults() {
      // TODO Auto-generated method stub
      super.performDefaults();
   }

   @Override
   protected void performApply() {
      // TODO Auto-generated method stub
      super.performApply();
   }
}
