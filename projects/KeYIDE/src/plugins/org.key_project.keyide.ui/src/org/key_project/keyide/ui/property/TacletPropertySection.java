package org.key_project.keyide.ui.property;

import java.util.Map;
import java.util.Map.Entry;

import org.key_project.util.java.StringUtil;

import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.gui.configuration.ChoiceSelector;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;

/**
 * Shows the taclet options of the current {@link Proof}.
 * @author Martin Hentschel
 */
public class TacletPropertySection extends AbstractKeyValueNodePropertySection {
   /**
    * {@inheritDoc}
    */
   @Override
   protected void updateShownContent(KeYMediator mediator, Node node) {
      // Reset old values
      resetTexts();
      // Show new values
      if (node != null) {
         Proof proof = node.proof();
         Map<String,String> values = proof.getSettings().getChoiceSettings().getDefaultChoices();
         for (Entry<String, String> entry : values.entrySet()) {
            String tooltip = ChoiceSelector.getExplanation(entry.getKey());
            tooltip = StringUtil.trim(tooltip);
            showText(entry.getKey(), entry.getValue(), tooltip);
         }
      }
   }
}
