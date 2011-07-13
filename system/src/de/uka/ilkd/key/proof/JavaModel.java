// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.proof;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class JavaModel {

    private final String modelDir;
    private final String modelTag;
    private final String descr;
    private final String classPath;
    private final String bootClassPath;
   
    public static final JavaModel NO_MODEL = new JavaModel();
   
    private JavaModel() {
	modelDir = null;
	modelTag = null;
	descr = "no model";
	classPath = null;
	bootClassPath = null;
    }

    public JavaModel(String modelDir, 
	    	     String modelTag,
	    	     List<File> classPath,
	    	     File bootClassPath) {
	this.modelDir = (new File(modelDir)).getAbsolutePath();
	this.modelTag = modelTag;
	this.descr = "model "+(new File(modelDir)).getName()+"@"
	    +DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date());
	StringBuffer sb = new StringBuffer();
	if(classPath != null && !classPath.isEmpty()) {
	    for(File f : classPath) {
		sb.append(f.getAbsolutePath() + ", ");
	    }
	    sb.setLength(sb.length() - 2);
	}
	this.classPath = sb.toString();
	this.bootClassPath = bootClassPath == null 
	                     ? null 
	                     : bootClassPath.getAbsolutePath();
    }
   
    public String getModelDir() {
	return modelDir;
    }
   
    public String getModelTag() {
	return modelTag;
    }
    
    public String getClassPath() {
	return classPath;
    }
    
    public String getBootClassPath() {
	return bootClassPath;
    }
   
    public boolean isEmpty() {
	return this == NO_MODEL;
    }
   
    public String description() {
	return descr;
    }

    
    @Override    
    public boolean equals(Object o) {
	if (!(o instanceof JavaModel)) {
	    return false;
	} else if(getModelTag() == null) {
	    return ((JavaModel)o).getModelTag() == null;
	} else {
	    return getModelTag().equals(((JavaModel)o).getModelTag());
	}
    }
	

    @Override    
    public int hashCode() {
	if (getModelTag() == null) {
	    return 42;
	} else {
	    return getModelTag().hashCode();
	}
    }

    
    @Override
    public String toString() {
        return "---Program model---\nModel dir: "+modelDir+
	    "\nModel tag: "+modelTag+"\nDescription: "+descr;
    }
}
