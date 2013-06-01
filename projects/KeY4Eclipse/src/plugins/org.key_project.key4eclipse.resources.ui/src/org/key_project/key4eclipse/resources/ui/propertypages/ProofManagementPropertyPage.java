package org.key_project.key4eclipse.resources.ui.propertypages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.key_project.key4eclipse.common.ui.property.AbstractProjectPropertyPage;
import org.key_project.key4eclipse.resources.property.KeYProjectProperties;
import org.key_project.key4eclipse.resources.ui.util.LogUtil;

public class ProofManagementPropertyPage extends AbstractProjectPropertyPage {

   private Button enableEfficentProofManagementButton;
   
   private Button autoDeleteProofFilesButton;
   
   private Button hideMefaFiles;

   
   /**
    * {@inheritDoc}
    */
   @Override
   protected Control createContents(Composite parent) {
      initializeDialogUnits(parent);
      Composite root = new Composite(parent, SWT.NONE);
      root.setLayout(new GridLayout(1, false));
      Group builderSettings = new Group(root, SWT.NONE);
      builderSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      builderSettings.setLayout(new GridLayout(1, false));
      builderSettings.setText("Builder settings");
      Composite builderSettingsComposite = new Composite(builderSettings, SWT.NONE);
      builderSettingsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      builderSettingsComposite.setLayout(new GridLayout(1, false));
      enableEfficentProofManagementButton = new Button(builderSettingsComposite, SWT.CHECK);
      enableEfficentProofManagementButton.setText("Efficient proof management enabled");
      setSelectionForEnableEfficientProofManagementButton();
      autoDeleteProofFilesButton = new Button(builderSettingsComposite, SWT.CHECK);
      autoDeleteProofFilesButton.setText("Delete unnecessary proof files automatically");
      setSelectionForAutoDeleteProofFilesButton();
      hideMefaFiles = new Button(builderSettingsComposite, SWT.CHECK);
      hideMefaFiles.setText("Hide meta files");
      setSelectionForHideMetaFilesButton();
      
      return root;
   }

   
   /**
    * Sets the selection for the EnableEfficientProofManagementButton CheckBox.
    */
   private void setSelectionForEnableEfficientProofManagementButton(){
      try {
         IProject project = getProject();
         enableEfficentProofManagementButton.setSelection(KeYProjectProperties.isEnableEfficientProofManagement(project));
      }
      catch (CoreException e) {
         LogUtil.getLogger().logError(e);
         LogUtil.getLogger().openErrorDialog(getShell(), e);
         enableEfficentProofManagementButton.setEnabled(false);
      }
   }
   

   /**
    * Sets the selection for the AutoDeleteProofFilesButton CheckBox.
    */
   private void setSelectionForAutoDeleteProofFilesButton(){
      try {
         IProject project = getProject();
         autoDeleteProofFilesButton.setSelection(KeYProjectProperties.isAutoDeleteProofFiles(project));
      }
      catch (CoreException e) {
         LogUtil.getLogger().logError(e);
         LogUtil.getLogger().openErrorDialog(getShell(), e);
         autoDeleteProofFilesButton.setEnabled(false);
      }
   }
   
   
   /**
    * Sets the selection for the HideMetaFilesButton CheckBox.
    */
   private void setSelectionForHideMetaFilesButton(){
      try {
         IProject project = getProject();
         hideMefaFiles.setSelection(KeYProjectProperties.isHideMetaFiles(project));
      }
      catch (CoreException e) {
         LogUtil.getLogger().logError(e);
         LogUtil.getLogger().openErrorDialog(getShell(), e);
         hideMefaFiles.setEnabled(false);
      }
   }
   
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean performOk() {
      try {
         IProject project = getProject();
         KeYProjectProperties.setEnableEfficientProofManagement(project, enableEfficentProofManagementButton.getSelection());
         KeYProjectProperties.setAutoDeleteProofFiles(project, autoDeleteProofFilesButton.getSelection());
         KeYProjectProperties.setHideMetaFiles(project, hideMefaFiles.getSelection());
         return super.performOk();
      }
      catch (CoreException e) {
         LogUtil.getLogger().logError(e);
         LogUtil.getLogger().openErrorDialog(getShell(), e);
         return false;
      }
   }
   
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void performDefaults() {
      enableEfficentProofManagementButton.setSelection(false);
      autoDeleteProofFilesButton.setSelection(false);
      hideMefaFiles.setSelection(false);
      super.performDefaults();
   }
}
