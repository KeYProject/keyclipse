package de.uka.ilkd.key.gui.joinrule;

import org.key_project.util.collection.ImmutableList;

import de.uka.ilkd.key.gui.InteractiveRuleApplicationCompletion;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import de.uka.ilkd.key.rule.join.ConcreteJoinRule;
import de.uka.ilkd.key.rule.join.JoinIfThenElse;
import de.uka.ilkd.key.rule.join.JoinRule;
import de.uka.ilkd.key.rule.join.JoinRuleBuiltInRuleApp;
import de.uka.ilkd.key.util.Pair;

/**
 * This class completes the instantiation for a join rule application.
 * The user is queried for partner goals and concrete join rule to
 * choose. If in forced mode, all potential partners and the if-then-else
 * join method are chosen (no query is shown to the user).
 * 
 * @author Dominic Scheurer
 */
public class JoinRuleCompletion implements InteractiveRuleApplicationCompletion {
	
	private static final ConcreteJoinRule STD_CONCRETE_JOIN_RULE = JoinIfThenElse.INSTANCE; 

    @Override
    public IBuiltInRuleApp complete(IBuiltInRuleApp app, Goal goal,
            boolean forced) {

        JoinRuleBuiltInRuleApp joinApp = (JoinRuleBuiltInRuleApp) app;
        PosInOccurrence pio = joinApp.posInOccurrence();
        
        ImmutableList<Pair<Goal,PosInOccurrence>> candidates =
                JoinRule.findPotentialJoinPartners(goal, pio);
        
        ImmutableList<Pair<Goal,PosInOccurrence>> chosenCandidates = null;
        ConcreteJoinRule chosenRule = null;
        
        if (forced) {
            chosenCandidates = candidates;
            chosenRule = STD_CONCRETE_JOIN_RULE;
        } else {
            JoinPartnerSelectionDialog dialog =
                    new JoinPartnerSelectionDialog(goal, pio, candidates, goal.proof().getServices());
            dialog.setVisible(true);
            chosenCandidates = dialog.getChosenCandidates();
            chosenRule = dialog.getChosenJoinRule();
        }
        
        if (chosenCandidates == null || chosenCandidates.size() < 1) {
        	return null;
        }
        
        JoinRuleBuiltInRuleApp result = new JoinRuleBuiltInRuleApp(app.rule(), pio);
        result.setJoinPartners(chosenCandidates);
        result.setConcreteRule(chosenRule);
        
        return result;
    }

    @Override
    public boolean canComplete(IBuiltInRuleApp app) {
        return checkCanComplete(app);
    }
    
    public static boolean checkCanComplete(IBuiltInRuleApp app) {
        return app instanceof JoinRuleBuiltInRuleApp;
    }

}
