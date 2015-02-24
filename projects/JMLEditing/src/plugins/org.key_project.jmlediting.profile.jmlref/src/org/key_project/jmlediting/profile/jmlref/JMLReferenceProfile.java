package org.key_project.jmlediting.profile.jmlref;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.key_project.jmlediting.core.parser.DefaultJMLParser;
import org.key_project.jmlediting.core.parser.IJMLParser;
import org.key_project.jmlediting.core.profile.AbstractJMLProfile;
import org.key_project.jmlediting.core.validation.IJMLValidator;
import org.key_project.jmlediting.profile.jmlref.behavior.BehaviorKeyword;
import org.key_project.jmlediting.profile.jmlref.behavior.ExceptionalBehaviorKeyword;
import org.key_project.jmlediting.profile.jmlref.behavior.NormalBehaviorKeyword;
import org.key_project.jmlediting.profile.jmlref.bound_mod.NonNullBoundModKeyword;
import org.key_project.jmlediting.profile.jmlref.bound_mod.NullableBoundModKeyword;
import org.key_project.jmlediting.profile.jmlref.loop.DecreasingKeyword;
import org.key_project.jmlediting.profile.jmlref.loop.LoopInvariantKeyword;
import org.key_project.jmlediting.profile.jmlref.model.GhostKeyword;
import org.key_project.jmlediting.profile.jmlref.model.ModelKeyword;
import org.key_project.jmlediting.profile.jmlref.model.RepresentsKeyword;
import org.key_project.jmlediting.profile.jmlref.model.SetKeyword;
import org.key_project.jmlediting.profile.jmlref.model.SuchThatKeyword;
import org.key_project.jmlediting.profile.jmlref.other.AlsoKeyword;
import org.key_project.jmlediting.profile.jmlref.other.HelperKeyword;
import org.key_project.jmlediting.profile.jmlref.other.InvariantKeyword;
import org.key_project.jmlediting.profile.jmlref.other.NonNullKeyword;
import org.key_project.jmlediting.profile.jmlref.other.NullableKeyword;
import org.key_project.jmlediting.profile.jmlref.other.PureKeyword;
import org.key_project.jmlediting.profile.jmlref.primary.InvariantForKeyword;
import org.key_project.jmlediting.profile.jmlref.primary.KeywordJMLPrimary;
import org.key_project.jmlediting.profile.jmlref.quantifier.ExistentialQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.quantifier.ForallQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.quantifier.MaxQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.quantifier.MinQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.quantifier.NumOfQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.quantifier.ProductQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.quantifier.QuantifierPrimary;
import org.key_project.jmlediting.profile.jmlref.quantifier.SumQuantifierKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.AccessibleKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.AssignableKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.EnsuresKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.requires.RequiresKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.requires.SameKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.spec_expression.OldKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.spec_expression.ResultKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.storeref.EverythingKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.storeref.NotSpecifiedKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.storeref.NothingKeyword;
import org.key_project.jmlediting.profile.jmlref.type.BigIntKeyword;
import org.key_project.jmlediting.profile.jmlref.type.RealKeyword;
import org.key_project.jmlediting.profile.jmlref.usercontent.SpecExpressionContentDescription;
import org.key_project.jmlediting.profile.jmlref.validator.LoopInvariantValidator;
import org.key_project.jmlediting.profile.jmlref.visibility.PrivateKeyword;
import org.key_project.jmlediting.profile.jmlref.visibility.ProtectedKeyword;
import org.key_project.jmlediting.profile.jmlref.visibility.PublicKeyword;
import org.key_project.jmlediting.profile.jmlref.visibility.SpecProtectedKeyword;
import org.key_project.jmlediting.profile.jmlref.visibility.SpecPublicKeyword;

/**
 * Models JML with respect to the JML reference manual.
 * http://www.eecs.ucf.edu/~leavens/JML/jmlrefman/.
 *
 *
 * @author Moritz Lichter
 *
 */
public class JMLReferenceProfile extends AbstractJMLProfile {

   /**
    * Creates a new profile instance with the given supported keyword.
    *
    * @param lang
    *           the keyword locale for AE/BE
    */
   public JMLReferenceProfile(final KeywordLocale lang) {
      this.getSupportedKeywordsInternal()
            .addAll(
                  Arrays.asList(new EnsuresKeyword(), new AssignableKeyword(),
                        new AccessibleKeyword(), new RequiresKeyword(),
                        new BehaviorKeyword(lang),
                        new ExceptionalBehaviorKeyword(lang),
                        new NormalBehaviorKeyword(lang), new AlsoKeyword(),
                        new HelperKeyword(), new PureKeyword(),
                        new PrivateKeyword(), new ProtectedKeyword(),
                        new PublicKeyword(), new SpecProtectedKeyword(),
                        new SpecPublicKeyword(), new EverythingKeyword(),
                        new NothingKeyword(), new NotSpecifiedKeyword(),
                        new ResultKeyword(), new OldKeyword(),
                        new SameKeyword(), new ForallQuantifierKeyword(),
                        new ExistentialQuantifierKeyword(),
                        new MinQuantifierKeyword(), new MaxQuantifierKeyword(),
                        new ProductQuantifierKeyword(),
                        new SumQuantifierKeyword(),
                        new NumOfQuantifierKeyword(),
                        new NonNullBoundModKeyword(),
                        new NullableBoundModKeyword(), new InvariantKeyword(),
                        new LoopInvariantKeyword(), new DecreasingKeyword(),
                        new InvariantForKeyword(), new SuchThatKeyword(),
                        new SetKeyword(), new ModelKeyword(),
                        new GhostKeyword(), new RepresentsKeyword(),
                        new NonNullKeyword(), new NullableKeyword(),
                        new RealKeyword(), new BigIntKeyword()));

      this.getSupportedPrimariesInternal().addAll(
            Arrays.asList(new KeywordJMLPrimary(), new QuantifierPrimary()));

      this.getSupportedContentDescriptionsInternal().addAll(
            Arrays.asList(new SpecExpressionContentDescription()));

   }

   /**
    * Creates a {@link JMLReferenceProfile} without a restriction to American
    * oder British English.
    */
   public JMLReferenceProfile() {
      this(KeywordLocale.BOTH);
   }

   @Override
   public String getName() {
      return "JML Reference";
   }

   @Override
   public String getIdentifier() {
      return "org.key_project.jmlediting.profile.jmlref";
   }

   @Override
   public IJMLParser createParser() {
      return new DefaultJMLParser(this);
   }

   @Override
   public Set<IJMLValidator> getValidators() {
      final Set<IJMLValidator> validator = new HashSet<IJMLValidator>();
      validator.add(new LoopInvariantValidator());
      return validator;
   }
}
