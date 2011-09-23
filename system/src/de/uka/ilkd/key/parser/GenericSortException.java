// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.parser;

import antlr.Token;
import de.uka.ilkd.key.logic.sort.Sort;

public class GenericSortException extends antlr.SemanticException {
    /**
     * 
     */
    private static final long serialVersionUID = 7887443025957191925L;
    String cat;
    String filename;
    Sort   sort;
    String reason;
    
    public GenericSortException(String cat, String reason,
				Sort sort, Token t, String filename) {
	super("GenericSort");
	this.cat      = cat;
	this.reason   = reason;
	this.filename = filename;
	this.sort     = sort;
	this.line     = t.getLine();
	this.column   = t.getColumn();
    }

    public GenericSortException(String cat, String reason, Sort sort, 
			    String filename, int line, int column) {
	super("GenericSort");
	this.cat      = cat;
	this.reason   = reason;
	this.filename = filename;
	this.sort     = sort;
	this.line     = line;
	this.column   = column;
    }

    /**
     * Returns a clean error message (no line number/column information)
     * @deprecated
     */
    @Deprecated
    public String getErrorMessage ()
    {
	return getMessage();
    }

    /**
     * Returns a clean error message (no line number/column information)
     */
    public String getMessage ()
    {
	String errmsg = cat+"\n  "+sort+"\n";
	return errmsg + reason;
    }
    
    /**
     * Returns a string representation of this exception.
     */
    public String toString() {
	return filename+"("+this.getLine()+", "+this.getColumn()+"): "
	    +getMessage();
    }
}
