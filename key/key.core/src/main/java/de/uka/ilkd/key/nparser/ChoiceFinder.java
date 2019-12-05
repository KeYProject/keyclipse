package de.uka.ilkd.key.nparser;

import de.uka.ilkd.key.logic.Choice;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Namespace;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Alexander Weigl
 * @version 1 (28.10.19)
 */
public class ChoiceFinder extends AbstractBuilder<Object> {
    @NotNull
    private final ChoiceInformation choiceInformation;

    public ChoiceFinder() {
        choiceInformation = new ChoiceInformation();
    }

    public ChoiceFinder(@NotNull ChoiceInformation choiceInformation) {
        this.choiceInformation = choiceInformation;
    }

    public ChoiceFinder(Namespace<Choice> choices) {
        choiceInformation = new ChoiceInformation(choices);
    }

    @Override
    public Object visitDecls(KeYParser.DeclsContext ctx) {
        ctx.option_decls().forEach(this::accept);
        ctx.options_choice().forEach(this::accept);
        return null;
    }

    @Override
    public Object visitChoice(KeYParser.ChoiceContext ctx) {
        String category = ctx.category.getText();
        List<String> options = new ArrayList<>(ctx.choice_option.size());
        ctx.choice_option.forEach(it -> {
            options.add(it.getText());
        });
        if (options.isEmpty()) {
            options.add("on");
            options.add("off");
        }

        seq().put(category, new HashSet<>(options));
        choiceInformation.setDefaultOption(category, options.get(0));
        options.forEach(it ->
                choices().add(new Choice(it, category))
        );
        return null;
    }

    @Override
    public Choice visitActivated_choice(KeYParser.Activated_choiceContext ctx) {
        var cat = ctx.cat.getText();
        var ch = ctx.choice_.getText();
        if (activatedChoicesCategories().contains(cat)) {
            throw new IllegalArgumentException("You have already chosen a different option for " + cat);
        }
        activatedChoicesCategories().add(cat);
        var name = cat + ":" + ch;
        var c = (Choice) choices().lookup(new Name(name));
        if (c == null) {
            semanticError(ctx, "Choice %s not previously declared", name);
        } else {
            activatedChoices().add(c);
        }
        return c;
    }

    @NotNull
    public ChoiceInformation getChoiceInformation() {
        return choiceInformation;
    }

    //region access functions
    private Set<Choice> activatedChoices() {
        return choiceInformation.getActivatedChoices();
    }

    private HashSet<String> activatedChoicesCategories() {
        return choiceInformation.getActivatedChoicesCategories();
    }

    private HashSet<String> options() {
        return choiceInformation.getActivatedChoicesCategories();
    }

    private Namespace<Choice> choices() {
        return choiceInformation.getChoices();
    }

    private Map<String, Set<String>> seq() {
        return choiceInformation.getFoundChoicesAndOptions();
    }
    //endregion
}
