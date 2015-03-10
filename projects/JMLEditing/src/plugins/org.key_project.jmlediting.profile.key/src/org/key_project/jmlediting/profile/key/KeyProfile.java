package org.key_project.jmlediting.profile.key;

import static org.key_project.jmlediting.core.parser.ParserBuilder.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.key_project.jmlediting.core.parser.DefaultJMLParser;
import org.key_project.jmlediting.core.parser.IJMLParser;
import org.key_project.jmlediting.core.parser.ParseFunction;
import org.key_project.jmlediting.core.profile.syntax.IJMLPrimary;
import org.key_project.jmlediting.core.profile.syntax.IKeyword;
import org.key_project.jmlediting.profile.jmlref.JMLReferenceProfile;
import org.key_project.jmlediting.profile.jmlref.KeywordLocale;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.AccessibleKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.AssignableKeyword;
import org.key_project.jmlediting.profile.jmlref.spec_keyword.spec_expression.ExpressionParser;
import org.key_project.jmlediting.profile.key.locset.EmptyKeywod;
import org.key_project.jmlediting.profile.key.locset.InfiniteUnionKeyword;
import org.key_project.jmlediting.profile.key.locset.IntersetOperatorKeyword;
import org.key_project.jmlediting.profile.key.locset.LocSetEverythingKeyword;
import org.key_project.jmlediting.profile.key.locset.LocSetKeyword;
import org.key_project.jmlediting.profile.key.locset.ReachLocsParser;
import org.key_project.jmlediting.profile.key.locset.SetMinusOperatorKeyword;
import org.key_project.jmlediting.profile.key.locset.SetUnionOperatorKeyword;
import org.key_project.jmlediting.profile.key.other.DynamicLogicPrimary;
import org.key_project.jmlediting.profile.key.other.IndexKeyword;
import org.key_project.jmlediting.profile.key.other.InvKeyword;
import org.key_project.jmlediting.profile.key.other.KeyAccessibleKeyword;
import org.key_project.jmlediting.profile.key.other.KeyAssignableKeyword;
import org.key_project.jmlediting.profile.key.other.StrictlyNothingKeyword;
import org.key_project.jmlediting.profile.key.other.StrictlyPureKeyword;
import org.key_project.jmlediting.profile.key.seq.SeqConcatKeyword;
import org.key_project.jmlediting.profile.key.seq.SeqDefKeyword;
import org.key_project.jmlediting.profile.key.seq.SeqEmptyKeyword;
import org.key_project.jmlediting.profile.key.seq.SeqExpressionParser;
import org.key_project.jmlediting.profile.key.seq.SeqKeyword;
import org.key_project.jmlediting.profile.key.seq.SeqPrimary;
import org.key_project.jmlediting.profile.key.seq.SeqSingletonKeyword;
import org.key_project.jmlediting.profile.key.seq.ValuesKeyword;

public class KeyProfile extends JMLReferenceProfile {

   public KeyProfile() {
      super(KeywordLocale.AMERICAN);

      final Set<IKeyword> supportedKeywords = this
            .getSupportedKeywordsInternal();
      final Set<IJMLPrimary> supportedPrimaries = this
            .getSupportedPrimariesInternal();

      // Add strictly keywords
      supportedKeywords.add(new StrictlyPureKeyword());
      supportedKeywords.add(new StrictlyNothingKeyword());
      // Disable informal descriptions in Assignable/Accessible keywords
      replace(supportedKeywords, AssignableKeyword.class,
            new KeyAssignableKeyword());
      replace(supportedKeywords, AccessibleKeyword.class,
            new KeyAccessibleKeyword());

      supportedKeywords.add(new InvKeyword());
      supportedPrimaries.add(new DynamicLogicPrimary());

      // Support for LocSetExpressions
      // Add everything for a different sort
      supportedKeywords.add(new LocSetEverythingKeyword());
      // All other keywords
      supportedKeywords.addAll(Arrays.asList(new EmptyKeywod(),
            new InfiniteUnionKeyword(), new IntersetOperatorKeyword(),
            new ReachLocsParser(), new SetMinusOperatorKeyword(),
            new SetUnionOperatorKeyword(), new LocSetKeyword()));

      // Allows \inv as access on a not toplevel object just as for x[3].\inv
      this.putExtension(ExpressionParser.ADDITIONAL_PRIMARY_SUFFIXES,
            separateBy('.', keywords(InvKeyword.class, this)),
            ParseFunction.class);

      // Support for seq expression
      supportedKeywords.addAll(Arrays.asList(new SeqKeyword(),
            new SeqConcatKeyword(), new SeqDefKeyword(), new SeqEmptyKeyword(),
            new SeqSingletonKeyword(), new ValuesKeyword()));
      supportedPrimaries.add(new SeqPrimary());
      this.putExtension(ExpressionParser.ADDITIONAL_PRIMARY_SUFFIXES,
            SeqExpressionParser.seqSuffix(this), ParseFunction.class);

      // Other keywords
      supportedKeywords.addAll(Arrays.asList(new IndexKeyword()));
   }

   private static void replace(final Set<IKeyword> keywords,
         final Class<? extends IKeyword> toReplace, final IKeyword keyword) {
      final Iterator<IKeyword> iter = keywords.iterator();
      while (iter.hasNext()) {
         final IKeyword k = iter.next();
         if (k.getClass().equals(toReplace)) {
            iter.remove();
            break;
         }
      }
      keywords.add(keyword);
   }

   @Override
   public String getName() {
      return "Key Profile";
   }

   @Override
   public String getIdentifier() {
      return "org.key_project.jmlediting.profile.key";
   }

   @Override
   public IJMLParser createParser() {
      return new DefaultJMLParser(this);
   }

}
