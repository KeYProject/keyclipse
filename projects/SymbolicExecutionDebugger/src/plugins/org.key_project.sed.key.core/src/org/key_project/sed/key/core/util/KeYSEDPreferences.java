/*******************************************************************************
 * Copyright (c) 2014 Karlsruhe Institute of Technology, Germany
 *                    Technical University Darmstadt, Germany
 *                    Chalmers University of Technology, Sweden
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Technical University Darmstadt - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.key_project.sed.key.core.util;

import org.eclipse.jface.preference.IPreferenceStore;
import org.key_project.sed.key.core.Activator;

/**
 * <p>
 * Provides access to the preferences of the Symbolic Execution Debugger based on KeY.
 * </p>
 * <p>
 * Default values are defined via {@link KeYSEDPreferencesInitializer}.
 * </p>
 * @author Martin Hentschel
 * @see KeYSEDPreferencesInitializer.
 */
public class KeYSEDPreferences {
   /**
    * Preference key for the maximal number of set nodes per branch on run.
    */
   public static final String MAXIMAL_NUMBER_OF_SET_NODES_PER_BRANCH_ON_RUN = "org.key_project.sed.key.core.preference.maximalNumberOfSetNodesPerBranchOnRun";

   /**
    * Preference key to show method return values in debug nodes.
    */
   public static final String SHOW_METHOD_RETURN_VALUES_IN_DEBUG_NODES = "org.key_project.sed.key.core.preference.showMethodReturnValuesInDebugNodes";

   /**
    * Preference key to enable or disables the variables of selected debug nodes.
    */
   public static final String SHOW_VARIABLES_OF_SELECTED_DEBUG_NODE = "org.key_project.sed.key.core.preference.showVariablesOfSelectedDebugNode";

   /**
    * Preference key to define if KeY's main window should be shown or not.
    */
   public static final String SHOW_KEY_MAIN_WINDOW = "org.key_project.sed.key.core.preference.showKeYMainWindow";

   /**
    * Preference key to define that branch conditions are merged or not.
    */
   public static final String MERGE_BRANCH_CONDITIONS = "org.key_project.sed.key.core.preference.mergeBranchConditions";

   /**
    * Preference key to define that unicode characters are used or not.
    */
   public static final String USE_UNICODE = "org.key_project.sed.key.core.preference.useUnicode";

   /**
    * Preference key to define that pretty printing is used or not.
    */
   public static final String USE_PRETTY_PRINTING = "org.key_project.sed.key.core.preference.usePrettyPrinting";

   /**
    * Preference key to define that the full signature will be shown on method return nodes.
    */
   public static final String SHOW_SIGNATURE_ON_METHOD_RETURN_NODES = "org.key_project.sed.key.core.preference.showSignatureOnMethodReturnNodes";

   /**
    * Preference key to define how variables are computed.
    */
   public static final String VARIABLES_ARE_COMPUTED_FROM_UPDATES = "org.key_project.sed.key.core.preference.variablesAreOnlyComputedFromUpdates";

   /**
    * Preference key to enable truth value evaluation.
    */
   public static final String TRUTH_VALUE_EVALUATION_ENABLED = "org.key_project.sed.key.core.preference.truthValueEvaluationEnabled";

   /**
    * Preference key to enable or disable highlighting of reached source code.
    */
   public static final String HIGHLIGHT_REACHED_SOURCE_CODE = "org.key_project.sed.key.core.preference.highlightReachedSourceCode";
   
   /**
    * Returns the managed {@link IPreferenceStore}.
    * @return The managed {@link IPreferenceStore}.
    */
   public static IPreferenceStore getStore() {
      return Activator.getDefault().getPreferenceStore();
   }
   
   /**
    * Returns the maximal number of set nodes per branch on run.
    * @return The maximal number of set nodes per branch on run.
    */
   public static int getMaximalNumberOfSetNodesPerBranchOnRun() {
      return getStore().getInt(MAXIMAL_NUMBER_OF_SET_NODES_PER_BRANCH_ON_RUN);
   }
   
   /**
    * Returns the default maximal number of set nodes per branch on run.
    * @return The default maximal number of set nodes per branch on run.
    */
   public static int getDefaultMaximalNumberOfSetNodesPerBranchOnRun() {
      return getStore().getDefaultInt(MAXIMAL_NUMBER_OF_SET_NODES_PER_BRANCH_ON_RUN);
   }
   
   /**
    * Sets the maximal number of set nodes per branch on run.
    * @param value The maximal number of set nodes per branch on run.
    */
   public static void setMaximalNumberOfSetNodesPerBranchOnRun(int value) {
      getStore().setValue(MAXIMAL_NUMBER_OF_SET_NODES_PER_BRANCH_ON_RUN, value);
   }
   
   /**
    * Sets the default maximal number of set nodes per branch on run.
    * @param defaultValue The default maximal number of set nodes per branch on run.
    */
   public static void setDefaultMaximalNumberOfSetNodesPerBranchOnRun(int defaultValue) {
      getStore().setDefault(MAXIMAL_NUMBER_OF_SET_NODES_PER_BRANCH_ON_RUN, defaultValue);
   }
   
   /**
    * Checks if method return values are shown in debug node.
    * @return Show method return values in debug node?
    */
   public static boolean isShowMethodReturnValuesInDebugNode() {
      return getStore().getBoolean(SHOW_METHOD_RETURN_VALUES_IN_DEBUG_NODES);
   }
   
   /**
    * Checks if method return values are shown in debug node by default.
    * @return Show method return values in debug node by default?
    */
   public static boolean isDefaultShowMethodReturnValuesInDebugNode() {
      return getStore().getDefaultBoolean(SHOW_METHOD_RETURN_VALUES_IN_DEBUG_NODES);
   }
   
   /**
    * Sets if method return values are shown in debug node.
    * @param value Show method return values in debug node?
    */
   public static void setShowMethodReturnValuesInDebugNode(boolean value) {
      getStore().setValue(SHOW_METHOD_RETURN_VALUES_IN_DEBUG_NODES, value);
   }
   
   /**
    * Sets if method return values are shown in debug node by default.
    * @param defaultValue Show method return values in debug node by default?
    */
   public static void setDefaultShowMethodReturnValuesInDebugNode(boolean defaultValue) {
      getStore().setDefault(SHOW_METHOD_RETURN_VALUES_IN_DEBUG_NODES, defaultValue);
   }
   
   /**
    * Checks if KeY's main window should be shown or not.
    * @return Show or hide KeY's main window.
    */
   public static boolean isShowKeYMainWindow() {
      return getStore().getBoolean(SHOW_KEY_MAIN_WINDOW);
   }
   
   /**
    * Checks if KeY's main window should be shown or not by default.
    * @return Show or hide KeY's main window.
    */
   public static boolean isDefaultShowKeYMainWindow() {
      return getStore().getDefaultBoolean(SHOW_KEY_MAIN_WINDOW);
   }
   
   /**
    * Sets if KeY's main window should be shown or not.
    * @param value Show or hide KeY's main window.
    */
   public static void setShowKeYMainWindow(boolean value) {
      getStore().setValue(SHOW_KEY_MAIN_WINDOW, value);
   }
   
   /**
    * Sets if KeY's main window should be shown or not by default.
    * @param defaultValue Show or hide KeY's main window.
    */
   public static void setDefaultShowKeYMainWindow(boolean defaultValue) {
      getStore().setDefault(SHOW_KEY_MAIN_WINDOW, defaultValue);
   }
   
   /**
    * Checks if variables should be shown of selected debug node.
    * @return Show or hide KeY's main window.
    */
   public static boolean isShowVariablesOfSelectedDebugNode() {
      return getStore().getBoolean(SHOW_VARIABLES_OF_SELECTED_DEBUG_NODE);
   }
   
   /**
    * Checks if variables should be shown of selected debug node by default.
    * @return Show or hide KeY's main window.
    */
   public static boolean isDefaultShowVariablesOfSelectedDebugNode() {
      return getStore().getDefaultBoolean(SHOW_VARIABLES_OF_SELECTED_DEBUG_NODE);
   }
   
   /**
    * Sets if variables should be shown of selected debug node.
    * @param value Show or hide KeY's main window.
    */
   public static void setShowVariablesOfSelectedDebugNode(boolean value) {
      getStore().setValue(SHOW_VARIABLES_OF_SELECTED_DEBUG_NODE, value);
   }
   
   /**
    * Sets if variables should be shown of selected debug node by default.
    * @param defaultValue Show or hide KeY's main window.
    */
   public static void setDefaultShowVariablesOfSelectedDebugNode(boolean defaultValue) {
      getStore().setDefault(SHOW_VARIABLES_OF_SELECTED_DEBUG_NODE, defaultValue);
   }
   
   /**
    * Checks if branch conditions are merged or not.
    * @return Merge branch conditions?
    */
   public static boolean isMergeBranchConditions() {
      return getStore().getBoolean(MERGE_BRANCH_CONDITIONS);
   }
   
   /**
    * Checks if branch conditions are merged or not by default.
    * @return Merge branch conditions by default?
    */
   public static boolean isDefaultMergeBranchConditions() {
      return getStore().getDefaultBoolean(MERGE_BRANCH_CONDITIONS);
   }
   
   /**
    * Sets if branch conditions are merged or not.
    * @param value Merge branch conditions?
    */
   public static void setMergeBranchConditions(boolean value) {
      getStore().setValue(MERGE_BRANCH_CONDITIONS, value);
   }
   
   /**
    * Sets if branch conditions are merged or not by default.
    * @param defaultValue Merge branch conditions by default?
    */
   public static void setDefaultMergeBranchConditions(boolean defaultValue) {
      getStore().setDefault(MERGE_BRANCH_CONDITIONS, defaultValue);
   }
   
   /**
    * Checks if pretty printing is used or not.
    * @return Use pretty printing?
    */
   public static boolean isUseUnicode() {
      return getStore().getBoolean(USE_UNICODE);
   }
   
   /**
    * Checks if pretty printing is used or not by default.
    * @return Use pretty printing?
    */
   public static boolean isDefaultUseUnicode() {
      return getStore().getDefaultBoolean(USE_UNICODE);
   }
   
   /**
    * Sets if pretty printing is used or not.
    * @param value Use pretty printing?
    */
   public static void setUseUnicode(boolean value) {
      getStore().setValue(USE_UNICODE, value);
   }
   
   /**
    * Sets if pretty printing is used or not by default.
    * @param defaultValue Use pretty printing?
    */
   public static void setDefaultUseUnicode(boolean defaultValue) {
      getStore().setDefault(USE_UNICODE, defaultValue);
   }
   
   /**
    * Checks if pretty printing is used or not.
    * @return Use pretty printing?
    */
   public static boolean isUsePrettyPrinting() {
      return getStore().getBoolean(USE_PRETTY_PRINTING);
   }
   
   /**
    * Checks if pretty printing is used or not by default.
    * @return Use pretty printing?
    */
   public static boolean isDefaultUsePrettyPrinting() {
      return getStore().getDefaultBoolean(USE_PRETTY_PRINTING);
   }
   
   /**
    * Sets if pretty printing is used or not.
    * @param value Use pretty printing?
    */
   public static void setUsePrettyPrinting(boolean value) {
      getStore().setValue(USE_PRETTY_PRINTING, value);
   }
   
   /**
    * Sets if pretty printing is used or not by default.
    * @param defaultValue Use pretty printing?
    */
   public static void setDefaultUsePrettyPrinting(boolean defaultValue) {
      getStore().setDefault(USE_PRETTY_PRINTING, defaultValue);
   }
   
   /**
    * Checks if the full signature will be shown or not.
    * @return Use pretty printing?
    */
   public static boolean isShowSignatureOnMethodReturnNodes() {
      return getStore().getBoolean(SHOW_SIGNATURE_ON_METHOD_RETURN_NODES);
   }
   
   /**
    * Checks if the full signature will be shown or not by default.
    * @return Use pretty printing?
    */
   public static boolean isDefaultShowSignatureOnMethodReturnNodes() {
      return getStore().getDefaultBoolean(SHOW_SIGNATURE_ON_METHOD_RETURN_NODES);
   }
   
   /**
    * Sets if the full signature will be shown or not.
    * @param value Use pretty printing?
    */
   public static void setShowSignatureOnMethodReturnNodes(boolean value) {
      getStore().setValue(SHOW_SIGNATURE_ON_METHOD_RETURN_NODES, value);
   }
   
   /**
    * Sets the full signature will be shown or not by default.
    * @param defaultValue Use pretty printing?
    */
   public static void setDefaultShowSignatureOnMethodReturnNodes(boolean defaultValue) {
      getStore().setDefault(SHOW_SIGNATURE_ON_METHOD_RETURN_NODES, defaultValue);
   }
   
   /**
    * Checks if variables are computed from sequent or visible type structure.
    * @return Use pretty printing?
    */
   public static boolean isVariablesAreOnlyComputedFromUpdates() {
      return getStore().getBoolean(VARIABLES_ARE_COMPUTED_FROM_UPDATES);
   }
   
   /**
    * Checks if variables are computed from sequent or visible type structure by default.
    * @return Use pretty printing?
    */
   public static boolean isDefaultVariablesAreOnlyComputedFromUpdates() {
      return getStore().getDefaultBoolean(VARIABLES_ARE_COMPUTED_FROM_UPDATES);
   }
   
   /**
    * Sets if variables are computed from sequent or visible type structure.
    * @param value Use pretty printing?
    */
   public static void setVariablesAreOnlyComputedFromUpdates(boolean value) {
      getStore().setValue(VARIABLES_ARE_COMPUTED_FROM_UPDATES, value);
   }
   
   /**
    * Sets if variables are computed from sequent or visible type structure by default.
    * @param defaultValue Use pretty printing?
    */
   public static void setDefaultVariablesAreOnlyComputedFromUpdates(boolean defaultValue) {
      getStore().setDefault(VARIABLES_ARE_COMPUTED_FROM_UPDATES, defaultValue);
   }
   
   /**
    * Checks if truth value evaluation is enabled.
    * @return Is truth value evaluation enabled?
    */
   public static boolean isTruthValueEvaluationEnabled() {
      return getStore().getBoolean(TRUTH_VALUE_EVALUATION_ENABLED);
   }
   
   /**
    * Checks if truth value evaluation is enabled by default.
    * @return Is truth value evaluation enabled?
    */
   public static boolean isDefaultTruthValueEvaluationEnabled() {
      return getStore().getDefaultBoolean(TRUTH_VALUE_EVALUATION_ENABLED);
   }
   
   /**
    * Sets if truth value evaluation is enabled.
    * @param value Enable truth value evaluation?
    */
   public static void setTruthValueEvaluationEnabled(boolean value) {
      getStore().setValue(TRUTH_VALUE_EVALUATION_ENABLED, value);
   }
   
   /**
    * Sets if truth value evaluation is enabled by default.
    * @param defaultValue Enable truth value evaluation?
    */
   public static void setDefaultTruthValueEvaluationEnabled(boolean defaultValue) {
      getStore().setDefault(TRUTH_VALUE_EVALUATION_ENABLED, defaultValue);
   }
   
   /**
    * Checks if reached source code is highlighted is enabled.
    * @return Is reached source code highlighting enabled?
    */
   public static boolean isHighlightReachedSourceCode() {
      return getStore().getBoolean(HIGHLIGHT_REACHED_SOURCE_CODE);
   }
   
   /**
    * Checks if reached source code is highlighted by default.
    * @return Is reached source code highlighting enabled by default?
    */
   public static boolean isDefaultHighlightReachedSourceCode() {
      return getStore().getDefaultBoolean(HIGHLIGHT_REACHED_SOURCE_CODE);
   }
   
   /**
    * Sets if truth value evaluation is enabled.
    * @param value Enable source code highlighting.
    */
   public static void setHighlightReachedSourceCode(boolean value) {
      getStore().setValue(HIGHLIGHT_REACHED_SOURCE_CODE, value);
   }
   
   /**
    * Sets if reached source code is highlighted by default.
    * @param defaultValue Enable source code highlighting.
    */
   public static void setDefaultHighlightReachedSourceCode(boolean defaultValue) {
      getStore().setDefault(HIGHLIGHT_REACHED_SOURCE_CODE, defaultValue);
   }
}