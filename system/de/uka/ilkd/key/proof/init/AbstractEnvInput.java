// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.proof.init;

import java.io.File;
import java.util.List;



/**
 * A simple EnvInput which includes default rules and a Java path.
 */
public abstract class AbstractEnvInput implements EnvInput {

    protected final String name;
    protected final String javaPath;    
    protected final List<File> classPath;
    protected final File bootClassPath;
    protected final Includes includes;    
    
    protected InitConfig initConfig;
    

    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------

    public AbstractEnvInput(String name,
	    		    String javaPath,
	    		    List<File> classPath,
	    		    File bootClassPath) {
	this.name     = name;
	this.javaPath = javaPath;
	this.classPath = classPath;
	this.bootClassPath = bootClassPath;
	this.includes = new Includes();
    }



    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------

    @Override
    public final String name() {
	return name;
    }

    @Override
    public final int getNumberOfChars() {
	return 1;
    }
        
    
    @Override
    public final void setInitConfig(InitConfig initConfig) {
	this.initConfig = initConfig;
    }
    
    
    @Override
    public final Includes readIncludes() throws ProofInputException {
	assert initConfig != null;
	return includes;
    }
    
    
    @Override
    public final String readJavaPath() throws ProofInputException {
	return javaPath;
    }


    @Override
    public final List<File> readClassPath() throws ProofInputException {
        return classPath;
    }
    

    @Override
    public File readBootClassPath() {
        return bootClassPath;
    }
}
