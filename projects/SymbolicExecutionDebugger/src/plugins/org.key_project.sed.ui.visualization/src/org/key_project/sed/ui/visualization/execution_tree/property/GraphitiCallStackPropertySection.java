package org.key_project.sed.ui.visualization.execution_tree.property;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.ui.property.AbstractSEDDebugNodeTabComposite;
import org.key_project.sed.ui.property.CallStackTabComposite;

/**
 * {@link ISection} implementation to show the properties of {@link ISEDDebugNode}s.
 * @author Martin Hentschel
 */
public class GraphitiCallStackPropertySection extends AbstractGraphitiDebugNodePropertySection {
   /**
    * {@inheritDoc}
    */
   @Override
   protected AbstractSEDDebugNodeTabComposite createContentComposite(Composite parent, int style) {
      return new CallStackTabComposite(parent, SWT.NONE, getWidgetFactory());
   }
}