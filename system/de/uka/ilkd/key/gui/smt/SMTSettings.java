// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//
package de.uka.ilkd.key.gui.smt;

import java.util.*;

import de.uka.ilkd.key.gui.GUIEvent;
import de.uka.ilkd.key.gui.Main;
import de.uka.ilkd.key.gui.configuration.Settings;
import de.uka.ilkd.key.gui.configuration.SettingsListener;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.smt.AbstractSMTSolver;
import de.uka.ilkd.key.smt.CVC3Solver;
import de.uka.ilkd.key.smt.SMTRule;
import de.uka.ilkd.key.smt.SimplifySolver;
import de.uka.ilkd.key.smt.YicesSolver;
import de.uka.ilkd.key.smt.Z3Solver;

/** This class encapsulates the information which 
 *  decision procedure should be used.
 */
public class SMTSettings implements Settings {
    
   
    /** String used in the Settings to store the active rule */
    private static final String ACTIVE_RULE  = "[DecisionProcedure]ActiveRule";
    
    private static final String TIMEOUT="[DecisionProcedure]SolverTimeout";
    

    private static final String SAVEFILE_PATH="[DecisionProcedure]savefile_path";
    
    
    private static final String SHOW_SMT_RES_DIA="[DecisionProcedure]showSMTResDialog";
    
    private static final String MULTIPLEPROVERS="[DecisionProcedure]multprovers";
    
    private static final String CACHE_GOALS = "[DecisionProcedure]cache_goals";
    
    private static final String PROGRESS_DIALOG_MODE = "[DecisionProcedure]pd_mode";



    /** the list of registered SettingListener */
    private LinkedList<SettingsListener> listenerList = new LinkedList<SettingsListener>();
    

    
    private LinkedList<SMTRule> smtRules = new LinkedList<SMTRule>();
    
    public final static int    PROGRESS_MODE_USER = 0;
    public final static int    PROGRESS_MODE_CLOSE = 1;
    public final static int    PROGRESS_MODE_CLOSE_FIRST = 2;
   
    
    
    
    private static Collection<AbstractSMTSolver>  solvers = new LinkedList<AbstractSMTSolver>();
    
    /** the currently active rule */    
    private SMTRule activeSMTRule = SMTRule.EMPTY_RULE;
    
    /** the value of the timeout in tenth of seconds.*/
    private int timeout = 60;
    
    private static SMTSettings instance;
    
    private static String EXECSTR = "[DecisionProcedure]Exec";


    
    /** the string separating different solver-command values. */
    private static final String execSeperator1 = ":"; 
    /** The String separating solvernames from commands in the settingsfile */
    private static final String execSeperator2 = "="; 
    
    /** the string separating different solvers
      */
    private static final String multSeparator1 = ":";
    
    /**the string separating solvernames from the value */
    private static final String multSeparator2 = "=";
    
    
    private String multProversSettings=null;
    
    private int progressDialogMode = PROGRESS_MODE_USER;
    
    private String file = "";
    
    private boolean cacheGoals=false;
    
    public int getProgressDialogMode(){
	return progressDialogMode;
    }
    
    public void setProgressDialogMode(int mode){
	progressDialogMode = mode;
    }
    
    public void setSaveToFile(String f){
	file = f;
    }
    
    public String getSaveToFile(){
	return file;
    }
    
    public boolean isCachingGoals(){
	return cacheGoals;
    }

    public void setCacheGoals(boolean b){
	cacheGoals = b;
    }

    
    
    /**
     * This is a singleton.
     */
    private SMTSettings() {
	super();
    }
    
    /** adds a listener to the settings object 
     * @param l the listener
     */
    public void addSettingsListener(SettingsListener l) {
        listenerList.add(l);
    }
    
    /**
     * retrieves the rule of the specified name or returns <code>null</code> if
     * no such rule exists
     * @param name the String unambiguously specifying a rule 
     * @return the found SMTRule or <code>null</code> 
     */
    public SMTRule findRuleByName(String name){
	
	for(SMTRule rule : getSMTRules()){
	    if(rule.name().toString().equals(name)){
		return rule;
	    }
	}
	return SMTRule.EMPTY_RULE;
    }
    
    
    public AbstractSMTSolver findSolverByName(String name){
	for(AbstractSMTSolver solver : getSolvers()){
		    if(solver.name().equals(name)){
			return solver;
		    }
		} 
	

	return null;
    }


    
    
    /** sends the message that the state of this setting has been
     * changed to its registered listeners (not thread-safe)
     */
    protected void fireSettingsChanged() {
	
        for (SettingsListener aListenerList : listenerList) {
            aListenerList.settingsChanged(new GUIEvent(this));
        }

        if(Main.instance != null){
            Main.instance.updateSMTSelectMenu();
        }
      
    }
    

    
    public SMTRule getActiveSMTRule(){
	return activeSMTRule;
    }

    
    private void setSolversAndRules(){
	
	AbstractSMTSolver z3 = new Z3Solver();
	AbstractSMTSolver simplify = new SimplifySolver();
	AbstractSMTSolver yices = new YicesSolver();
	AbstractSMTSolver cvc3 = new CVC3Solver();

	solvers.add(z3);
	solvers.add(simplify);
	solvers.add(yices);
	solvers.add(cvc3);
	smtRules.add(new SMTRule(new Name("Z3_PROVER"),z3));
	smtRules.add(new SMTRule(new Name("YICES_PROVER"),yices));
	smtRules.add(new SMTRule(new Name("SIMPLIFY_PROVER"),simplify));
	smtRules.add(new SMTRule(new Name("CVC3_PROVER"),cvc3));
	smtRules.add(new SMTRule(new Name("MULTIPLE_PROVERS"),z3,simplify,yices,cvc3));
	
	//solvers = s;
	
    }
    
    public final Collection<AbstractSMTSolver> getSolvers(){
	
	return solvers;
    }
    
    
    public Collection<SMTRule> getSMTRules(){

	return smtRules;
    }
    

    /**
     * Returns a list of all installed rules, sorted alphabetically by rule name.
     */
    public Collection<SMTRule> getInstalledRules(){
	Collection<SMTRule> toReturn = new LinkedList<SMTRule>();
	
	for(SMTRule rule : getSMTRules()){
	    if(rule.getInstalledSolvers().size() > 0){
		toReturn.add(rule);
	    }
	}
	
	return toReturn;
    }
    
    
    
    /**
     * returns the timeout specifying the maximal amount of time an external prover
     * is run
     * @return the timeout in tenth of seconds
     */
    public int getTimeout() {
	return this.timeout;
    }

    private final static String EQUALITY = "#####";
    
    private String decode(String s){
	return s.replaceAll(EQUALITY, "=");
    }
    
    private String encode(String s){
	return s.replaceAll("=", EQUALITY);
    }
    

    /** gets a Properties object and has to perform the necessary
     * steps in order to change this object in a way that it
     * represents the stored settings
     */
    public void readSettings(Properties props) {
	

	
	
	String timeoutstring = props.getProperty(TIMEOUT);
	if (timeoutstring != null) {
	    int curr = Integer.parseInt(timeoutstring);
	    if (curr > 0) {
		this.timeout = curr;
	    }
	}
	
	this.readExecutionString(props);
	
	multProversSettings = props.getProperty(MULTIPLEPROVERS);
	readMultProversString();
	
	

	
        String sd = props.getProperty(SHOW_SMT_RES_DIA);
        this.showSMTResDialog = !(sd == null) && sd.equals("true");
    

    	
    	String cg = props.getProperty(CACHE_GOALS);
    	this.cacheGoals = !(cg == null) && cg.equals("true");
    	
    	file = props.getProperty(SAVEFILE_PATH,"");
    	
    	String pd = props.getProperty(PROGRESS_DIALOG_MODE);
    	int mode;
    	try{
    	    mode = Integer.parseInt(pd);
    	}catch(NumberFormatException e){
    	   mode = PROGRESS_MODE_USER;
    	}
    	
    	progressDialogMode = mode;
    	
    	
    	// Read the active rule at the end of the method to guarantee
    	// that the execution commands have been read yet.
	String ruleString = props.getProperty(ACTIVE_RULE);

	this.activeSMTRule = findRuleByName(ruleString);
	// Use only the rule if the corresponding solvers 
	// are installed.
	if(!activeSMTRule.isUsable()){
	    this.activeSMTRule = SMTRule.EMPTY_RULE;
	}

    }
    

    
    /**
     * read the execution strings from the properties file
     * @param props
     */
    private void readExecutionString(Properties props) {
	String allCommands = props.getProperty(EXECSTR);
	//all value pairs are stored separated by a |
	if (allCommands != null) {
	    String[] valuepairs = allCommands.split(execSeperator1);
	    for (String s : valuepairs) {
		String[] vals = s.split(execSeperator2);
		if (vals.length == 2) {
		    AbstractSMTSolver solver = findSolverByName(vals[0]);
		    if(solver != null){
			setExecutionCommand(solver,decode(vals[1]));
			solver.isInstalled(true);
		    }
		}
	    }
	}
    }
    
    
    /**
     * read the multiple provers strings from the properties file, stored in multProversSettings
     */
    private void readMultProversString()
    {
	
	
	if(multProversSettings != null){
	    String[] valuepairs = multProversSettings.split(multSeparator1);
	    for(String s : valuepairs){
		String[] vals = s.split(multSeparator2);
		if(vals.length == 2){
		    AbstractSMTSolver solver = findSolverByName(vals[0]);
		    if(solver != null){
			solver.useForMultipleRule(vals[1].equals("true"));
		    }
			
			
			
		   
		}
	    }
	}
    }
    
    /**
     * write the Execution Commands to the file
     * @param prop
     */
    private void writeExecutionString(Properties prop) {
	String toStore = "";
	for (AbstractSMTSolver solver : getSolvers()) {
	     
	     String comm = encode(solver.getExecutionCommand());
	    	if (comm == null) {
			comm = "";
	    	}
	    	toStore = toStore + solver.name() + execSeperator2 + comm + execSeperator1;
	    }
	
	//remove the las two || again
	if (toStore.length() >= execSeperator1.length()){
	    //if the program comes here, a the end ad extra || was added.
	    toStore = toStore.substring(0, toStore.length()-execSeperator1.length());
	}
	prop.setProperty(EXECSTR, toStore);
    }
    
    /**
     * Write the values, that specify whether a prover is used for the rule 'multiple provers'. 
     */
    private void writeMultipleProversString(Properties prop) {
	String toStore = "";
	
	for(AbstractSMTSolver solver : solvers){
	    String value = solver.useForMultipleRule()? "true" : "false";
	    toStore = toStore + solver.name() + multSeparator2 + value + multSeparator1; 
	}


	if (toStore.length() >= multSeparator1.length()){
	    toStore = toStore.substring(0, toStore.length()-multSeparator1.length());
	}
	prop.setProperty(MULTIPLEPROVERS, toStore);
    }
    

    
    /**
     * Set a execution command for a certain solver.
     * @param s the solver, which uses this command.
     * @param command the command to use
     */
    public void setExecutionCommand(AbstractSMTSolver s, String command) {
	
	s.setExecutionCommand(command);
	
    }
    
    /**
     * get the execution command for a certain rule.
     * @param solver the solver
     * @return the execution command
     */
    public String getExecutionCommand(AbstractSMTSolver solver) {
	return solver.getExecutionCommand();
    }
    



    


    
    public boolean getMultipleUse(AbstractSMTSolver solver){
	return solver.useForMultipleRule();
    }
    
    
    
    
    /**
     * removes the specified listener form the listener list
     * @param l the listener
     */
    public void removeSettingsListener(SettingsListener l) {
	listenerList.remove(l);
    }

    /**
     * if the specified rule is known it is set as active rule, otherwise or specifying <code>null</code>
     * deactivates the rule. 
     */
    public void setActiveSMTRule(SMTRule rule){
	if(activeSMTRule != rule){
	    if(rule == null){
		activeSMTRule = SMTRule.EMPTY_RULE;
	    }else{
		this.activeSMTRule = rule;
	    }
	 
	    fireSettingsChanged();
	}
	

    }


    /**
     * sets the timeout until an external prover is terminated
     * @param t the timeout in tenth of seconds
     */
    public void setTimeout(int t) {
	if (t > 0 && t != timeout) {
	    this.timeout = t;
	    this.fireSettingsChanged();
	}
    }

    /**
     * updates the current available SMT rules
     * @param profile the active Profile 
     */
    public void updateSMTRules(Profile profile) {
	//Load the available SMTRules...	
	/*for (Rule r : profile.
		getStandardRules().getStandardBuiltInRules()) {
	    if(r instanceof SMTRuleNew){
		this.smtRules.add((SMTRuleNew)r);
	    }
	}*/
	
    }
    
    private boolean saveFile = false;


    
    public void setSaveFile(boolean sf) {
	if (sf != this.saveFile) {
	    this.saveFile = sf;
	    this.fireSettingsChanged();
	}
    }
    
    /**
     * @return true, if a created problem file should be saved.
     */
    public boolean getSaveFile() {
	return this.saveFile;
    }
    
    private boolean showSMTResDialog = false;
    

    
    public void setSMTResDialog(boolean b){
	if(b!=this.showSMTResDialog){
	    this.showSMTResDialog = b;
	    this.fireSettingsChanged();
	}
    }
    
    public boolean getShowSMTResDialog(){
	return this.showSMTResDialog;
    }
    
    /**
     * true, if the argument should be used for test
     * TODO implement?
     */
    public boolean useRuleForTest(int arg) {
	return true;
    }

    
    
    /** implements the method required by the Settings interface. The
     * settings are written to the given Properties object. Only entries of the form 
     * <key> = <value> (,<value>)* are allowed.
     * @param props the Properties object where to write the settings as (key, value) pair
     */
    public void writeSettings(Properties props) {	
        props.setProperty(ACTIVE_RULE, "" + activeSMTRule.name());
        props.setProperty(TIMEOUT, "" + this.timeout);
      
        /*if (this.saveFile)
            props.setProperty(SAVEFILE, "true");
        else {
            props.setProperty(SAVEFILE, "false");
        }*/
        if (this.showSMTResDialog)
            props.setProperty(SHOW_SMT_RES_DIA, "true");
        else {
            props.setProperty(SHOW_SMT_RES_DIA, "false");
        }

        
        if (this.cacheGoals)
            props.setProperty(CACHE_GOALS, "true");
        else {
            props.setProperty(CACHE_GOALS, "false");
        }
        
        props.setProperty(PROGRESS_DIALOG_MODE,Integer.toString(progressDialogMode));

        props.setProperty(SAVEFILE_PATH,this.file);
        
        
        

       
        this.writeExecutionString(props);
        this.writeMultipleProversString(props);
    }

    public static SMTSettings getInstance() {
	if (instance == null) {
	    instance = new SMTSettings();
	    instance.setSolversAndRules();
	}
	
	return instance;
    }





 

}