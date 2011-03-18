// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//
package de.uka.ilkd.key.proof.init;

import java.io.File;
import java.io.FileNotFoundException;

import de.uka.ilkd.key.gui.configuration.ProofSettings;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.parser.*;
import de.uka.ilkd.key.proof.CountingBufferedReader;
import de.uka.ilkd.key.proof.ProblemLoader;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.ProofAggregate;
import de.uka.ilkd.key.speclang.SLEnvInput;
import de.uka.ilkd.key.util.ProgressMonitor;


/** 
 * Represents an input from a .key user problem file producing an environment
 * as well as a proof obligation.
 */
public final class KeYUserProblemFile extends KeYFile implements ProofOblInput {

    private Term problemTerm = null;
    private String problemHeader = "";
    
    private KeYParser lastParser;
    
    
    //-------------------------------------------------------------------------
    //constructors
    //------------------------------------------------------------------------- 
    
    /** 
     * Creates a new representation of a KeYUserFile with the given name,
     * a rule source representing the physical source of the input, and
     * a graphical representation to call back in order to report the progress
     * while reading.
     */
    public KeYUserProblemFile(String name, 
                              File file, 
                              ProgressMonitor monitor) {
        super(name, file, monitor);
    }
    
    
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------
        
    @Override
    public void read() throws ProofInputException {
        if(initConfig == null) {
            throw new IllegalStateException("InitConfig not set.");
        }	
        
        //read activated choices
        try {
            ProofSettings settings = getPreferences();
            
            ParserConfig pc = new ParserConfig
                (initConfig.getServices(), 
                 initConfig.namespaces());
            KeYParser problemParser = new KeYParser
                (ParserMode.PROBLEM, new KeYLexer(getNewStream(),
                        initConfig.getServices().getExceptionHandler()), 
                        file.toString(), pc, pc, null, null);    
            problemParser.parseWith();            
        
            settings.getChoiceSettings()
                    .updateWith(problemParser.getActivatedChoices());           
            
            initConfig.setActivatedChoices(settings.getChoiceSettings()
        	      		                   .getDefaultChoicesAsSet());
        
        } catch (antlr.ANTLRException e) {
            throw new ProofInputException(e);      
        } catch (FileNotFoundException fnfe) {
            throw new ProofInputException(fnfe);
        }        
	
        //read in-code specifications
        SLEnvInput slEnvInput = new SLEnvInput(readJavaPath(), 
        				       readClassPath(), 
        				       readBootClassPath());
        slEnvInput.setInitConfig(initConfig);
        slEnvInput.read();
                
        //read key file itself
	super.read();        
    }    


    @Override
    public void readProblem() throws ProofInputException {
        if (initConfig == null) {
            throw new IllegalStateException("KeYUserProblemFile: InitConfig not set.");
        }
        
        try {
            CountingBufferedReader cinp = 
                new CountingBufferedReader
                    (getNewStream(),monitor,getNumberOfChars()/100);
            DeclPicker lexer = new DeclPicker(new KeYLexer(cinp,initConfig.getServices().getExceptionHandler()));
            
            final ParserConfig normalConfig 
                = new ParserConfig(initConfig.getServices(), initConfig.namespaces());
            final ParserConfig schemaConfig 
                = new ParserConfig(initConfig.getServices(), initConfig.namespaces());
            
            KeYParser problemParser 
                    = new KeYParser(ParserMode.PROBLEM, 
                                    lexer, 
                                    file.toString(), 
                                    schemaConfig, 
                                    normalConfig,
                                    initConfig.getTaclet2Builder(),
                                    initConfig.getTaclets()); 
            problemTerm = problemParser.parseProblem();
            String searchS = "\\problem";            

	    if(problemTerm == null) {
	       boolean chooseDLContract = problemParser.getChooseContract() != null;
	       if(chooseDLContract)
  	         searchS = "\\chooseContract";
	       else {
	         throw new ProofInputException("No \\problem or \\chooseContract in the input file!");
	       }
	    }

            problemHeader = lexer.getText();
            if(problemHeader != null && 
               problemHeader.lastIndexOf(searchS) != -1){
                problemHeader = problemHeader.substring(
                    0, problemHeader.lastIndexOf(searchS));
            }
            initConfig.setTaclets(problemParser.getTaclets());
            lastParser = problemParser;
        } catch (antlr.ANTLRException e) {
            throw new ProofInputException(e);
        } catch (FileNotFoundException fnfe) {
            throw new ProofInputException(fnfe);
        }
    }

    
    @Override
    public ProofAggregate getPO() throws ProofInputException {
        assert problemTerm != null;
	String name = name();
        ProofSettings settings = getPreferences();
        return ProofAggregate.createProofAggregate(
                new Proof(name, 
                          problemTerm, 
                          problemHeader,
                          initConfig.createTacletIndex(), 
                          initConfig.createBuiltInRuleIndex(),
                          initConfig.getServices(), 
                          settings), 
                name);
    }
    
    
    @Override
    public boolean implies(ProofOblInput po) {
        return equals(po);
    }
    
    
    /** 
     * Reads a saved proof of a .key file.
     */
    public void readProof(ProblemLoader prl) throws ProofInputException {
        try {
            lastParser.proof(prl);
        } catch(antlr.ANTLRException e) {
            throw new ProofInputException(e);
        }
    }
        
    
    @Override
    public boolean equals(Object o){
        if(!(o instanceof KeYUserProblemFile)) {
            return false;
        }
        final KeYUserProblemFile kf = (KeYUserProblemFile) o;
        return kf.file.file().getAbsolutePath()
                             .equals(file.file().getAbsolutePath());
    }
    
    
    @Override
    public int hashCode() {
        return file.file().getAbsolutePath().hashCode();
    }
}
