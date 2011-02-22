//This file is part of KeY - Integrated Deductive Software Design
//Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                    Universitaet Koblenz-Landau, Germany
//                    Chalmers University of Technology, Sweden
//
//The KeY system is protected by the GNU General Public License. 
//See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.smt;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantLock;

import de.uka.ilkd.key.gui.Main;
import de.uka.ilkd.key.gui.configuration.PathConfig;
import de.uka.ilkd.key.gui.configuration.ProofSettings;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.smt.SMTSolverResult.ThreeValuedTruth;
import de.uka.ilkd.key.smt.SolverSession.InternResult;
import de.uka.ilkd.key.smt.launcher.AbstractProcess;
import de.uka.ilkd.key.smt.taclettranslation.DefaultTacletSetTranslation;
import de.uka.ilkd.key.util.Debug;


final public class SMTSolverImplementation extends AbstractProcess implements SMTSolver, Runnable {

    
    private static int fileCounter = 0;
    
    private static synchronized int getNextFileID(){
	fileCounter++;
	return fileCounter;
    }
    
    private static int IDCounter =0;
    private final int ID = IDCounter++;
    


    /**
     * The path for the file
     */
    private static final String fileDir = PathConfig.KEY_CONFIG_DIR
	    + File.separator + "smt_formula";

    /**
     * the file base name to be used to store the SMT translation
     */
    private static final String FILE_BASE_NAME = "smt_formula";
    
    /** true, if this solver was checked if installed */
    private boolean installwaschecked = false;
    /** true, if last check showed solver is installed */
    private boolean isinstalled = false;
    /** true, if the current run is for test uss only (for example checking, if the Solver is installed) */
    private boolean inTestMode = false;

    /** determines whether taclets are used for this solver.*/
    private boolean useTaclets = true;
    /** Only for testing*/
    private Collection<Taclet> tacletsForTest = null;
    
    private SolverSession session = null;
    
    public SolverSession getSession(){return session;}
    
    private boolean useForMultipleRule = true;
    
    private String   executionCommand = getDefaultExecutionCommand();
    
    private SMTProblem problem;
    private SolverListener listener;
    private StringBuffer problemFormula = new StringBuffer();
    private ExternalProcessLauncher processLauncher = new ExternalProcessLauncher();
    private Services services;
    private SMTSolverResult finalResult;
    
    private ReentrantLock lockStateVariable = new ReentrantLock();
    private ReentrantLock lockInterruptionVariable = new ReentrantLock();
    
    
    private Thread    thread;
    private SolverTimeout solverTimeout;
    private ReasonOfInterruption reasonOfInterruption = ReasonOfInterruption.NoInterruption;
    private Object userTag;
    private SolverState solverState=SolverState.Waiting;
    private SolverType type;
    
    public SMTSolverImplementation(){
	//isInstalled(true);
    }
    
    public SMTSolverImplementation(SMTProblem problem, SolverListener listener, Services services, SolverType myType){
	this.problem = problem;
	this.listener = listener;
	this.services = services;
	this.type = myType;
    }

    @Override
    public ReasonOfInterruption getReasonOfInterruption() {
	lockInterruptionVariable.lock();
	ReasonOfInterruption reason = reasonOfInterruption;
	lockInterruptionVariable.unlock();
        return reason;
    }
    
    public void setReasonOfInterruption(
            ReasonOfInterruption reasonOfInterruption) {
	lockInterruptionVariable.lock();
	this.reasonOfInterruption = reasonOfInterruption;
	lockInterruptionVariable.unlock();
    }
    
    @Override
    public SolverType getType(){
	return type;
    }

    
    @Override
    public Object getUserTag() {
        return userTag;
    }
    
    public void setUserTag(Object o){
	userTag = o;
    }
    
    public SolverTimeout getSolverTimeout(){
	return solverTimeout;
    }
    
    public long getStartTime(){
	if(solverTimeout == null){
	    return -1;
	}
	return solverTimeout.scheduledExecutionTime();
    }
    
    public long getTimeout(){
	if(solverTimeout == null){
	    return -1;
	}
	return solverTimeout.getTimeout();
    }
    
    
    public SolverState getState(){
	lockStateVariable.lock();
	SolverState b = solverState;
	lockStateVariable.unlock();
	return b;
    }
    
    private  void setSolverState(SolverState value){
	lockStateVariable.lock();
	solverState = value;
	lockStateVariable.unlock();
    }
    
    public boolean wasInterrupted(){
	return getReasonOfInterruption() != ReasonOfInterruption.NoInterruption;
    }

    /**
     * Get the command for executing the external prover.
     * This is a hardcoded default value. It might be overridden by user settings
     * @param filename the location, where the file is stored.
     * @param formula the formula, that was created by the translator
     * @return Array of Strings, that can be used for executing an external decider.
     */
//    protected abstract String getExecutionCommand(String filename,
//	    				            String formula);
  
    public SMTTranslator getTranslator(Services services) {
	try{
 	    return new SmtLibTranslator(services);
	
	}catch(Exception e){
	    System.err.println("Error: An error occurred while obtaining an SmtLibTranslator: Trying to use the default translator...");
	    e.printStackTrace();
	    return new SmtLibTranslator(services);
	}
    }

    
    private String getFinalExecutionCommand(String filename, String formula) {
	//get the Command from user settings
	String toReturn = ProofSettings.DEFAULT_SETTINGS.getSMTSettings().getExecutionCommand(this);
	if (toReturn == null || toReturn.length() == 0) {
	    toReturn = this.type.getExecutionCommand(filename, formula);
	} else {
	    //replace the placeholder with filename and fomula
	    toReturn = toReturn.replaceAll("%f", filename);
	    toReturn = toReturn.replaceAll("%p", formula);
	}
	return toReturn;
    }
    
    
    
    @Override
    public void run() {
	setSolverState(SolverState.Running);
	listener.processStarted(this, problem);
	String s;
	try {
	    s = translateToCommand(problem.getTerm(), services);
        } catch (Throwable e) {
            interruptionOccurred(e);
	    listener.processInterrupted(this, problem, e);
	   setSolverState(SolverState.Stopped);
	    return;
        }
	LinkedList<String> list = new LinkedList<String>();
	while(s.indexOf(' ')!=-1){
	     int index = s.indexOf(' ');
             list.add(s.substring(0,s.indexOf(' ')));
	     s = s.substring(index+1,s.length());
	}
	list.add(s);
	String [] array = new String[list.size()];
	
	
	try {
	    String result[] = processLauncher.launch(list.toArray(array));
	    this.finalResult = type.interpretAnswer(result[ExternalProcessLauncher.RESULT], 
		        result[ExternalProcessLauncher.ERROR], 
		        Integer.parseInt(result[ExternalProcessLauncher.EXIT_CODE]));
	//    Thread.sleep(8000);
        } catch (Throwable e) {
            interruptionOccurred(e);
        }finally{
            solverTimeout.cancel();
            setSolverState(SolverState.Stopped);
            listener.processStopped(this, problem);
        }
        

	
    }
    
 
    private void interruptionOccurred(Throwable e){
	ReasonOfInterruption reason = getReasonOfInterruption();
	switch(reason){
	case Exception:
	case NoInterruption:
	    setReasonOfInterruption(ReasonOfInterruption.Exception);
            listener.processInterrupted(this, problem, e);
	    break;
	case Timeout:
	    listener.processTimeout(this, problem);
	    break;
	case User:
	    listener.processUser(this,problem);
	    break;		
	}
    }
    
    public String name(){
	return type.getName();
    }

   

    private static String toStringLeadingZeros(int n, int width) {
	String rv = "" + n;
	while (rv.length() < width) {
	    rv = "0" + rv;
	}
	return rv;
    }

    /**
     * Constructs a date for use in log filenames.
     */
    private static String getCurrentDateString() {
	Calendar c = Calendar.getInstance();
	StringBuffer sb = new StringBuffer();
	String dateSeparator = "-";
	String dateTimeSeparator = "_";
        sb.append(toStringLeadingZeros(c.get(Calendar.YEAR), 4)).append(
                dateSeparator).append(
                toStringLeadingZeros(c.get(Calendar.MONTH) + 1, 2)).append(
                dateSeparator).append(
                toStringLeadingZeros(c.get(Calendar.DATE), 2)).append(
                dateTimeSeparator).append(toStringLeadingZeros(c.get(Calendar.HOUR_OF_DAY), 2)).
                append("h").append(toStringLeadingZeros(c.get(Calendar.MINUTE), 2)).append("m").
                append(toStringLeadingZeros(c.get(Calendar.SECOND), 2)).append("s")
		        .append('.').append(
			toStringLeadingZeros(c.get(Calendar.MILLISECOND), 2));
	return sb.toString();
    }

    /**
     * store the text to a file.
     * @param text the text to be stored.
     * @return the path where the file was stored to.
     */
    private final File storeToFile(String text) throws IOException {
	// create directory where to put the files marked as delete on exit
	final File smtFileDir = new File(fileDir);
	smtFileDir.mkdirs();
	smtFileDir.deleteOnExit();
	
	// create the actual file marked as delete on exit
	final File smtFile = new File(smtFileDir, FILE_BASE_NAME +"_"+name()+"_"+"_"+getNextFileID()+"_"+ getCurrentDateString());
	
	
	smtFile.deleteOnExit();
	
	// write the content out to the created file
	//final BufferedWriter out = new BufferedWriter(new FileWriter(smtFile));
	final FileWriter out = new FileWriter(smtFile);
	out.write(text);
	out.close();

	//store the text permanent to a file 
	if (!this.inTestMode && ProofSettings.DEFAULT_SETTINGS.getSMTSettings().getSaveFile() &&
		Main.getInstance() != null) {
	    String path = ProofSettings.DEFAULT_SETTINGS.getSMTSettings().getSaveToFile();
	    	path = finalizePath(path);
		try {
		    final BufferedWriter out2 = new BufferedWriter(new FileWriter(path));
		    out2.write(text);
		    out2.close();
		} catch (IOException e) {
		    throw new RuntimeException("Could not store to file " + path + ".");
		}
	   
	}
	
	return smtFile;
    }
    
    private String finalizePath(String path){
	Calendar c = Calendar.getInstance();
	String date = c.get(Calendar.YEAR)+"-"+c.get(Calendar.MONTH)+"-"+c.get(Calendar.DATE);
	String time = c.get(Calendar.HOUR_OF_DAY)+"-"+c.get(Calendar.MINUTE)+"-"+c.get(Calendar.SECOND);
	
        path = path.replaceAll("%d", date);
        path = path.replaceAll("%s", this.getTitle());
        path = path.replaceAll("%t", time);
        path = path.replaceAll("%i", Integer.toString(getNextFileID()));
        return path;
    }


    /** Read the input until end of file and return contents in a
     * single string containing all line breaks. */
    static String read(InputStream in) throws IOException {
	BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	StringBuffer sb = new StringBuffer();

	int x = reader.read();
	while (x > -1) {
	    sb.append((char) x);
	    x = reader.read();
	}
	return sb.toString();
    }


    
    private String translateToCommand(String formula, Services services) throws IOException{
	final File loc;
	try {
	    //store the translation to a file                                
	    loc = this.storeToFile(formula);
	} catch (IOException e) {
	    Debug.log4jError("The file with the formula could not be written." + e, SMTSolverImplementation.class.getName());
	    final IOException io = new IOException("Could not create or write the input file " +
		    "for the external prover. Received error message:\n" + e.getMessage());
	    io.initCause(e);
	    throw io;
	} 

	//get the commands for execution
	return this.getFinalExecutionCommand(loc.getAbsolutePath(), formula);
    }
    

    private String translateToCommand(Term term, Services services) throws IllegalFormulaException, IOException {
	
	SMTTranslator trans = this.getTranslator(services);
	instantiateTaclets(trans);
	
 
	String formula = trans.translateProblem(term, services).toString();

	saveTacletTranslation(trans);
	
	return translateToCommand(formula, services);
    }
    

    

    
    private static boolean checkEnvVariable(String cmd){
	String filesep = System.getProperty("file.separator");
	String path =  System.getenv("PATH");
	String [] res = path.split(System.getProperty("path.separator"));
	for(String s : res){
	    File file = new File(s+ filesep + cmd);
	    if(file.exists()){
		return true;
	    }
	}
	
	return false;

    }
    
    
    public static boolean isInstalled(String cmd){
	    
	    int first = cmd.indexOf(" ");
	    if(first >= 0){
		cmd = cmd.substring(0, first);
	    }
	    
	    if(checkEnvVariable(cmd)){
		return true;
	    } else{
		File file = new File(cmd);
		return file.exists();
		
	    }
    }
    
    /**
     * check, if this solver is installed and can be used.
     * @param recheck if false, the solver is not checked again, if a cached value for this exists.
     * @return true, if it is installed.
     */
    public boolean isInstalled(boolean recheck) {
	if (recheck | !installwaschecked) {
	    String cmd = getExecutionCommand();
	    isinstalled = isInstalled(cmd); 
	    if(isinstalled){
		 installwaschecked = true;		      
	    }

	}
	return isinstalled;
    }
    
    protected String getTestFile() {
	return System.getProperty("key.home")
	    + File.separator + "examples"
	    + File.separator + "_testcase"
	    + File.separator + "smt"
	    + File.separator + "ornot.key";
    }
    
    /**
     * get the hard coded execution command from this solver.
     * The filename od a problem is indicated by %f, the problem itsself with %p
     */
    public String getDefaultExecutionCommand() {
	return type.getExecutionCommand("%f", "%p");
    }
    
    
    
  
    
    public void useTaclets(boolean b){
	this.useTaclets = b;
    }
   

    
    
    private Collection<Taclet> getTaclets(){
	 
	 if(tacletsForTest != null){
	     return tacletsForTest;
	 }
	 return session.getTaclets();
    }
    
   
    private void saveTacletTranslation(SMTTranslator trans) {
	if (!this.inTestMode
	        && ProofSettings.DEFAULT_SETTINGS
	                .getTacletTranslationSettings().isSaveToFile()
	        && ProofSettings.DEFAULT_SETTINGS
	                .getTacletTranslationSettings().isUsingTaclets()) {

	    DefaultTacletSetTranslation translation = (DefaultTacletSetTranslation) ((AbstractSMTTranslator) trans)
		    .getTacletSetTranslation();

	    if (translation != null) {
		String path = ProofSettings.DEFAULT_SETTINGS
	        .getTacletTranslationSettings().getFilename();
		path = finalizePath(path);
		translation.storeToFile(path);
	    }

	}
    }
    
    private void instantiateTaclets(SMTTranslator trans) throws IllegalFormulaException{
	if(!ProofSettings.DEFAULT_SETTINGS.getTacletTranslationSettings().isUsingTaclets() || !useTaclets ){
	    trans.setTacletsForAssumptions(new LinkedList<Taclet>());
	   
	}else{
	    trans.setTacletsForAssumptions(getTaclets());
	}
	
	
	 
	
	
    }
    
    public void setTacletsForTest(Collection<Taclet> set){
	tacletsForTest = set;
    }
    
    public void prepareSolver(LinkedList<InternResult> terms, Services services, Collection<Taclet> taclets) {
	init();
	
	session = new SolverSession(terms, services, taclets);

        
    }
    
    @Override
    public void interrupt() {
        solverTimeout.cancel();
	thread.interrupt();
    }
    
    @Override
    public void interrupt(ReasonOfInterruption reason) {
	// order of assignments is important;
	setReasonOfInterruption(reason);
	setSolverState(SolverState.Stopped);
	if(solverTimeout != null){
	    solverTimeout.cancel();
	}
	if(thread != null){
	    thread.interrupt();
	}
        
    }
    
    @Override
    public void start(SolverTimeout timeout) {
	    thread = new Thread(this);
	    solverTimeout = timeout;
	    thread.start();

        
    }
    
    
    @Override
    public String[] atStart() throws Exception{

	LinkedList<String> list = new LinkedList<String>();
	InternResult term = session.nextTerm();
	//session.addResult(SMTSolverResult.createUnknownResult("",name()),session.currentTerm());
	if(term != null){
	    String s;
	    if(term.getFormula() != null){
		s = translateToCommand(term.getFormula(), session.getServices());
	    }else{
		s = translateToCommand(term.getRealTerm(), session.getServices()); 
	    }
	     
	    
	    while(s.indexOf(' ')!=-1){
		int index = s.indexOf(' ');

		list.add(s.substring(0,s.indexOf(' ')));
		s = s.substring(index+1,s.length());
	    }
	    list.add(s);

	    
	}

	return list.toArray(new String[list.size()]);
    }
    
    @Override
    public boolean atEnd(InputStream result, InputStream error, int exitStatus) throws Exception{
	
	String text = read(result);
	result.close();

	
	String err = read(error);
	error.close();
	SMTSolverResult res = type.interpretAnswer(text, err, exitStatus);
	
	if(session.currentTerm()!= null){
	     
	   session.currentTerm().setResult(res);
	   if(res.isValid() == ThreeValuedTruth.TRUE){
	       session.incrementSolved();
	   }
	   
	
	}
	//listener.eventCycleFinished(this,res);
	
	
	return !session.hasNextTerm();
    }
    
    public String getInfo() {
        return "";
    }
    
    public boolean wasSuccessful() {
	return this.finalResult.isValid() == ThreeValuedTruth.TRUE;
    }
    
    public SMTSolverResult getFinalResult() {
	return finalResult;
    }
    

    public int getMaxCycle() {

        return session.getTermSize();
    }
    
    public String toString(){
	return name()+" (ID: " +ID+")";
    }
    
    public String getTitle(){
	return name();
    }

    public boolean useForMultipleRule() {
        return useForMultipleRule;
    }
    

    public void useForMultipleRule(boolean b) {
	useForMultipleRule = b;
        
    }


    public void setExecutionCommand(String s) {
        
        executionCommand = s;
    }
    
    public String getExecutionCommand(){
	return executionCommand;
    }
    

    
    


    
    
}
