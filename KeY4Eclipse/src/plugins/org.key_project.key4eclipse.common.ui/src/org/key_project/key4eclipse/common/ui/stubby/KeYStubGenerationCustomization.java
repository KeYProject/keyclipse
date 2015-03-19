package org.key_project.key4eclipse.common.ui.stubby;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.key_project.key4eclipse.common.ui.util.LogUtil;
import org.key_project.key4eclipse.starter.core.property.KeYClassPathEntry;
import org.key_project.key4eclipse.starter.core.property.KeYClassPathEntry.KeYClassPathEntryKind;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties.UseBootClassPathKind;
import org.key_project.stubby.core.customization.IGeneratorCustomization;
import org.key_project.stubby.ui.customization.AbstractStubGenerationCustomization;
import org.key_project.stubby.ui.customization.IStubGenerationCustomization;
import org.key_project.util.java.ObjectUtil;

/**
 * {@link IStubGenerationCustomization} for KeY.
 * @author Martin Hentschel
 */
public class KeYStubGenerationCustomization extends AbstractStubGenerationCustomization {
   /**
    * Stub folder will be not part of class paths.
    */
   private Button doNotUseButton;
   
   /**
    * Stub folder will be part of class path.
    */
   private Button classPathButton;
   
   /**
    * Stub folder will be part of class path.
    */
   private Button bootClassPathButton;

   /**
    * {@inheritDoc}
    */
   @Override
   public Control createComposite(Composite parent) {
      Group keyGroup = new Group(parent, SWT.NONE);
      keyGroup.setText("KeY paths");
      keyGroup.setLayout(new GridLayout(3, false));
      doNotUseButton = new Button(keyGroup, SWT.RADIO);
      doNotUseButton.setText("&Not considered");
      classPathButton = new Button(keyGroup, SWT.RADIO);
      classPathButton.setText("&Class Path");
      bootClassPathButton = new Button(keyGroup, SWT.RADIO);
      bootClassPathButton.setText("&Boot Class Path");
      return keyGroup;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setStubFolderPath(String stubFolderPath) {
      if (isBootClassPath(stubFolderPath)) {
         doNotUseButton.setSelection(false);
         classPathButton.setSelection(false);
         bootClassPathButton.setSelection(true);
      }
      else if (isPartOfClassPath(stubFolderPath)) {
         doNotUseButton.setSelection(false);
         classPathButton.setSelection(true);
         bootClassPathButton.setSelection(false);
      }
      else {
         doNotUseButton.setSelection(true);
         classPathButton.setSelection(false);         
         bootClassPathButton.setSelection(false);
      }
   }

   /**
    * Checks if the stub folder is part of the class path.
    * @param stubFolderPath The stub folder path.
    * @return {@code true} is part of class path, {@code false} is not part of class path.
    */
   protected boolean isPartOfClassPath(String stubFolderPath) {
      try {
         String fullPath = computeFullPath(stubFolderPath);
         List<KeYClassPathEntry> entries = KeYResourceProperties.getClassPathEntries(getProject());
         return KeYResourceProperties.searchClassPathEntry(entries, 
                                                           KeYClassPathEntryKind.WORKSPACE, 
                                                           fullPath) != null;
      }
      catch (CoreException e) {
         LogUtil.getLogger().logError(e);
         return false;
      }
   }
   
   /**
    * Checks if the given stub folder is the boot class path.
    * @param stubFolderPath The stub folder path.
    * @return {@code true} is boot class path, {@code false} is not boot class path.
    */
   protected boolean isBootClassPath(String stubFolderPath) {
      return isBootClassPath(getProject(), stubFolderPath);
   }
   
   /**
    * Checks if the given stub folder is the boot class path.
    * @param project The {@link IProject} to check.
    * @param stubFolderPath The stub folder path.
    * @return {@code true} is boot class path, {@code false} is not boot class path.
    */
   public static boolean isBootClassPath(IProject project, String stubFolderPath) {
      try {
         if (UseBootClassPathKind.WORKSPACE.equals(KeYResourceProperties.getUseBootClassPathKind(project))) {
            String path = KeYResourceProperties.getBootClassPath(project);
            String fullPath = computeFullPath(project, stubFolderPath);
            return ObjectUtil.equals(fullPath, path);
         }
         else {
            return false;
         }
      }
      catch (CoreException e) {
         LogUtil.getLogger().logError(e);
         return false;
      }
   }
   
   /**
    * Computes the full path.
    * @param stubFolderPath The stub folder path.
    * @return The full path.
    */
   protected String computeFullPath(String stubFolderPath) {
      return computeFullPath(getProject(), stubFolderPath);
   }
   
   /**
    * Computes the full path.
    * @param project The parent {@link IProject}.
    * @param stubFolderPath The stub folder path.
    * @return The full path.
    */
   public static String computeFullPath(IProject project, String stubFolderPath) {
      return project.getFullPath().toString() + "/" + stubFolderPath;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public IGeneratorCustomization createGeneratorCustomization() {
      return new KeYGeneratorCustomization(classPathButton.getSelection(),
                                           bootClassPathButton.getSelection());
   }
}
