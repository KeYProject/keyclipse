package org.key_project.sed.ui.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Creates the symbolic debug perspective.
 * @author Martin Hentschel
 */
public class SymbolicDebugPerspectiveFactory implements IPerspectiveFactory {
   /**
    * The ID of this perspective.
    */
   public static final String PERSPECTIVE_ID = "org.key_project.sed.ui.perspective";
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void createInitialLayout(IPageLayout layout) {
      // Get the editor area.
      String editorArea = layout.getEditorArea();
      // Put the Resource Explorer on the left.
      IFolderLayout leftFolder = layout.createFolder("left", IPageLayout.LEFT, 0.3f, editorArea);
      leftFolder.addView(IDebugUIConstants.ID_DEBUG_VIEW);
      // Perspective Shortcuts
      layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective");
      layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaHierarchyPerspective");
      layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaBrowsingPerspective");
      layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective");
   }
}