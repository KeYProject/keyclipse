package org.key_project.jmlediting.ui.test.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.key_project.jmlediting.ui.test.preferencepages.JMLColorPreferencePageTest;
import org.key_project.jmlediting.ui.test.preferencepages.JMLKeywordDialogTest;
import org.key_project.jmlediting.ui.test.preferencepages.JMLProfileDialogTest;
import org.key_project.jmlediting.ui.test.preferencepages.ProfilePropertiesTest;
import org.key_project.jmlediting.ui.test.preferencepages.ProfilePropertiesTest2;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ProfilePropertiesTest.class,
      ProfilePropertiesTest2.class, JMLColorPreferencePageTest.class,
      JMLProfileDialogTest.class, JMLKeywordDialogTest.class })
public class PreferenceSuite {

}
