// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//

package de.uka.ilkd.key.logic.op;

import de.uka.ilkd.key.collection.ArrayOfBoolean;
import de.uka.ilkd.key.logic.ArrayOfTerm;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.sort.ArrayOfSort;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.logic.sort.Sort;


/** 
 * Abstract sorted operator class offering some common functionality.
 */
abstract class AbstractSortedOperator extends AbstractOperator 
                                      implements SortedOperator {
    
    private static final ArrayOfSort EMPTY_SORT_LIST = new ArrayOfSort();
    
    private final Sort sort;
    private final ArrayOfSort argSorts;

    
    protected AbstractSortedOperator(Name name,
	    			     ArrayOfSort argSorts,
	    		             Sort sort,
	    		             ArrayOfBoolean whereToBind,
	    		             boolean isRigid) {
	super(name, 
	      argSorts == null ? 0 : argSorts.size(), 
              whereToBind, 
              isRigid);
	assert sort != null;
	this.argSorts = argSorts == null ? EMPTY_SORT_LIST : argSorts;
	this.sort = sort;
    }
    
    
    protected AbstractSortedOperator(Name name,
	    			     Sort[] argSorts,
	    		             Sort sort,
	    		             Boolean[] whereToBind,
	    		             boolean isRigid) {
	this(name, 
             new ArrayOfSort(argSorts), 
             sort, 
             new ArrayOfBoolean(whereToBind), 
             isRigid);
    }    
    
    
    protected AbstractSortedOperator(Name name,
	    			     ArrayOfSort argSorts,
	    		             Sort sort,
	    		             boolean isRigid) {
	this(name, argSorts, sort, null, isRigid);
    }    
    
    
    protected AbstractSortedOperator(Name name,
	    			     Sort[] argSorts,
	    		             Sort sort,
	    		             boolean isRigid) {
	this(name, new ArrayOfSort(argSorts), sort, null, isRigid);
    }
    
    
    protected AbstractSortedOperator(Name name,
	    		             Sort sort,
	    		             boolean isRigid) {
	this(name, (ArrayOfSort) null, sort, null, isRigid);
    }    
    

    @Override
    public final Sort sort(ArrayOfTerm terms) {
	return sort;
    }
    
    
    /**
     * checks if a given Term could be subterm (at the at'th subterm
     * position) of a term with this function at its top level. The
     * validity of the given subterm is NOT checked.  
     * @param at theposition of the term where this method should check 
     * the validity.  
     * @param possibleSub the subterm to be ckecked.
     * @return true iff the given term can be subterm at the indicated
     * position
     */
    private boolean possibleSub(int at, Term possibleSub) {
	Sort sort = possibleSub.sort();
	
	return sort == AbstractMetaOperator.METASORT
	       || sort instanceof ProgramSVSort
	       || argSort(at) == AbstractMetaOperator.METASORT
	       || argSort(at) instanceof ProgramSVSort
	       || sort.extendsTrans(argSort(at));
    }
    
    
    protected boolean additionalValidTopLevel2(Term term) {
	return true;
    }
    
    
    @Override
    protected final boolean additionalValidTopLevel(Term term) {
	for(int i = 0, n = arity(); i < n; i++) {
            if(!possibleSub(i, term.sub(i))) { 
		return false;
	    }
	}
	return additionalValidTopLevel2(term);
    }
    
    
    @Override    
    public final Sort argSort(int i) {
	return argSorts.getSort(i);
    }
    
    
    @Override
    public final ArrayOfSort argSorts() {
	return argSorts;
    }
    
    
    @Override
    public final Sort sort() {
	return sort;
    }
}