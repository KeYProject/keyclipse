// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.rule;

import de.uka.ilkd.key.collection.*;
import de.uka.ilkd.key.gui.ContractConfigurator;
import de.uka.ilkd.key.gui.Main;
import de.uka.ilkd.key.java.*;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.ClassDeclaration;
import de.uka.ilkd.key.java.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.expression.operator.New;
import de.uka.ilkd.key.java.reference.*;
import de.uka.ilkd.key.java.statement.Throw;
import de.uka.ilkd.key.java.visitor.ProgramContextAdder;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.OpReplacer;
import de.uka.ilkd.key.proof.init.ContractPO;
import de.uka.ilkd.key.proof.mgt.ComplexRuleJustificationBySpec;
import de.uka.ilkd.key.proof.mgt.RuleJustificationBySpec;
import de.uka.ilkd.key.rule.inst.ContextStatementBlockInstantiation;
import de.uka.ilkd.key.speclang.FunctionalOperationContract;
import de.uka.ilkd.key.util.MiscTools;
import de.uka.ilkd.key.util.Pair;
import de.uka.ilkd.key.util.Triple;


/**
 * Implements the rule which inserts operation contracts for a method call.
 */
public final class UseOperationContractRule implements BuiltInRule {
    
    public static final UseOperationContractRule INSTANCE 
                                            = new UseOperationContractRule();    

    private static final Name NAME = new Name("Use Operation Contract");
    private static final TermBuilder TB = TermBuilder.DF;
    
    private Term lastFocusTerm;
    private Instantiation lastInstantiation;   
    

    //-------------------------------------------------------------------------
    //constructors
    //------------------------------------------------------------------------- 
    
    private UseOperationContractRule() {
    }
    
    
    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------
    
    private static Pair<Expression,MethodOrConstructorReference> getMethodCall(
	    						JavaBlock jb,
	    				                Services services) {
	final Expression actualResult;
        final MethodOrConstructorReference mr;
        
        final SourceElement activeStatement = MiscTools.getActiveStatement(jb);
        //active statement must be method reference or assignment with
        //method reference
        if(activeStatement instanceof MethodReference) {
            actualResult = null;
            mr = (MethodReference) activeStatement;
        } else if(activeStatement instanceof New 
        	  && ((New)activeStatement).getTypeDeclarationCount() == 0) {
            actualResult = null;
            mr = (New) activeStatement;
        } else if(activeStatement instanceof CopyAssignment) {
            final CopyAssignment ca = (CopyAssignment) activeStatement;
            final Expression lhs = ca.getExpressionAt(0);
            final Expression rhs = ca.getExpressionAt(1);
            if((rhs instanceof MethodReference 
        	|| rhs instanceof New 
        	   && ((New)rhs).getTypeDeclarationCount() == 0)
               && (lhs instanceof LocationVariable 
                   || lhs instanceof FieldReference)) {
        	actualResult = lhs;
        	mr = (MethodOrConstructorReference) rhs;
            } else {
        	return null;
            }
        } else {
            return null;
        }
        
        //constructor may not refer to anonymous class
        if(mr instanceof New 
           && ((New)mr).getTypeReference().getKeYJavaType().getJavaType() 
                       instanceof ClassDeclaration
           && ((ClassDeclaration)((New)mr).getTypeReference()
        	                          .getKeYJavaType()
        	                          .getJavaType()).isAnonymousClass()) {
            return null;
        }

        //receiver must be simple
        final ReferencePrefix rp = mr.getReferencePrefix();
        if(rp != null 
           && !ProgramSVSort.SIMPLEEXPRESSION.canStandFor(rp, null, services)
           && !(rp instanceof ThisReference)
           && !(rp instanceof SuperReference)
           && !(rp instanceof TypeReference)) {
            return null;
        } else {
            return new Pair<Expression,MethodOrConstructorReference>(
        	    				actualResult, 
        	    				mr);
        }
    }
    
    
    private static KeYJavaType getStaticPrefixType(
	    				MethodOrConstructorReference mr,
	                                Services services,
	                                ExecutionContext ec) {
	if(mr instanceof MethodReference) { 
	    return ((MethodReference)mr).determineStaticPrefixType(services, 
		                         ec);
	} else {
	    New n = (New) mr;
	    return n.getKeYJavaType(services, ec);
	}
    }
    
    
    private static ProgramMethod getProgramMethod(
	    				   MethodOrConstructorReference mr,
	    				   KeYJavaType staticType, 
	    				   ExecutionContext ec,
	    				   Services services) {
	ProgramMethod result;	
	if(mr instanceof MethodReference) { //from MethodCall.java
	    MethodReference methRef = (MethodReference) mr;
	    if(ec != null) {
		result = methRef.method(services, staticType, ec);
		if(result == null) {
		    // if a method is declared protected and prefix and
		    // execContext are in different packages we have to
		    // simulate visibility rules like being in prefixType
		    result = methRef.method(services, 
			    staticType, 
			    methRef.getMethodSignature(services, ec), 
			    staticType);
		}
	    } else {
		result = methRef.method(services, 
			staticType, 
			methRef.getMethodSignature(services, ec), 
			staticType);
	    }
	} else {
	    New n = (New) mr;
	    ImmutableList<KeYJavaType> sig = ImmutableSLList.<KeYJavaType>nil();
	    for(Expression e : n.getArguments()) {
		sig = sig.append(e.getKeYJavaType(services, ec));
	    }
	    result = services.getJavaInfo().getConstructor(staticType, sig);
	}
	return result;
    }
    
    
    private static Term getActualSelf(MethodOrConstructorReference mr,
	    		              ProgramMethod pm,
	    		              ExecutionContext ec, 
	    		              Services services) {
	final TypeConverter tc = services.getTypeConverter();
	final ReferencePrefix rp = mr.getReferencePrefix();
	if(pm.isStatic() || pm.isConstructor()) {
	    return null;
	} else if(rp == null 
		  || rp instanceof ThisReference 
		  || rp instanceof SuperReference) {
	    return tc.findThisForSort(pm.getContainerType().getSort(), ec);
	} else if(rp instanceof FieldReference
		  && ((FieldReference) rp).referencesOwnInstanceField()) {
	    final ReferencePrefix rp2 
	    	= ((FieldReference)rp).setReferencePrefix(
	    					ec.getRuntimeInstance());
	    return tc.convertToLogicElement(rp2);
	} else {
	    return tc.convertToLogicElement(rp, ec);
	}
    }
    
    
    private static ImmutableList<Term> getActualParams(
	    					MethodOrConstructorReference mr,
	    					ExecutionContext ec,
	    					Services services) {        
	ImmutableList<Term> result = ImmutableSLList.<Term>nil();
	for(Expression expr : mr.getArguments()) {
	    Term actualParam 
	    	= services.getTypeConverter().convertToLogicElement(expr, ec);
	    result = result.append(actualParam);
	}
	return result;
    }
    
       
    /**
     * Returns the operation contracts which are applicable for the passed 
     * operation and the passed modality
     */
    private static ImmutableSet<FunctionalOperationContract> getApplicableContracts(
	    						  Services services, 
                                                          ProgramMethod pm, 
                                                          KeYJavaType kjt,
                                                          Modality modality) {
        ImmutableSet<FunctionalOperationContract> result 
                = services.getSpecificationRepository()
                          .getOperationContracts(kjt, pm, modality);
        
        //in box modalities, diamond contracts may be applied as well
        if(modality == Modality.BOX) {
            result = result.union(services.getSpecificationRepository()
                                          .getOperationContracts(kjt, 
                                        	  		 pm,
                                        	  		 Modality.DIA));
        }

        return result;
    }

    
    /**
     * Chooses a contract to be applied. 
     * This is done either automatically or by asking the user.
     */
    private static FunctionalOperationContract configureContract(Services services, 
                                                       ProgramMethod pm,
                                                       KeYJavaType kjt,
                                                       Modality modality) {
	ImmutableSet<FunctionalOperationContract> contracts
                = getApplicableContracts(services, pm, kjt, modality);
	for(FunctionalOperationContract c : contracts) {
	    if(!services.getProof().mgt().isContractApplicable(c)) {
		contracts = contracts.remove(c);
	    }
	}
	assert !contracts.isEmpty();
        if(Main.getInstance().mediator().autoMode()) {
            return services.getSpecificationRepository()
                           .combineOperationContracts(contracts);
        } else {
            FunctionalOperationContract[] contractsArr 
            	= contracts.toArray(new FunctionalOperationContract[contracts.size()]);
            ContractConfigurator cc 
                    = new ContractConfigurator(Main.getInstance(),
                                               services,
                                               contractsArr,
                                               "Contracts for " + pm.getName(),
                                               true);
            if(cc.wasSuccessful()) {
                return (FunctionalOperationContract) cc.getContract();
            } else {
                return null;
            }
        }
    }
    
    
    /**
     * @return (assumption, anon update, anon heap)
     */
    private static Triple<Term,Term,Term> createAnonUpdate(ProgramMethod pm, 
	                                     	    	   Term mod, 
	                                     	    	   Services services) {
	assert pm != null;
	assert mod != null;
	
	final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
	final Name methodHeapName 
		= new Name(TB.newName(services, "heapAfter_" + pm.getName()));
	final Function methodHeapFunc
		= new Function(methodHeapName, heapLDT.targetSort());
	services.getNamespaces().functions().addSafely(methodHeapFunc);
	final Name anonHeapName 
		= new Name(TB.newName(services, "anonHeap_" + pm.getName()));
	final Function anonHeapFunc = new Function(anonHeapName,
					           heapLDT.targetSort());
	services.getNamespaces().functions().addSafely(anonHeapFunc);
	final Term anonHeap = TB.func(anonHeapFunc);	
	final Term assumption = TB.equals(TB.anon(services, 
				          TB.heap(services), 
				          mod,
				          anonHeap),
		               TB.func(methodHeapFunc)); 
	final Term anonUpdate = TB.elementary(services,
		               		      heapLDT.getHeap(),
		               		      TB.func(methodHeapFunc));
	
	return new Triple<Term,Term,Term>(assumption, anonUpdate, anonHeap);
    } 
    
    
    private static Term getFreePost(ProgramMethod pm,
	    		     	    KeYJavaType kjt,
	    		     	    Term resultTerm,
	    		     	    Term selfTerm,
	    		     	    Term heapAtPre,
	    		     	    Services services) {
        final Term result;
        if(pm.isConstructor()) {
            assert resultTerm == null;
            assert selfTerm != null;
            result = TB.and(new Term[]{
        	      TB.not(TB.equals(selfTerm, TB.NULL(services))),
                      OpReplacer.replace(TB.heap(services), 
        	                  	 heapAtPre, 
        	                   	 TB.not(TB.created(services, 
        	                   	                   selfTerm))),
                      TB.created(services, selfTerm),
                      TB.exactInstance(services, kjt.getSort(), selfTerm)});            
        } else if(resultTerm != null) {
            result = TB.reachableValue(services, 
        	                       resultTerm, 
        	                       pm.getKeYJavaType());
        } else {
            result = TB.tt();
        }
        return result;
    }
    
    
    private static PosInProgram getPosInProgram(JavaBlock jb) {
        ProgramElement pe = jb.program();        
   
        PosInProgram result = PosInProgram.TOP;
        
        if (pe instanceof ProgramPrefix) {
            ProgramPrefix curPrefix = (ProgramPrefix)pe;
       
            final ImmutableArray<ProgramPrefix> prefix 
            	= curPrefix.getPrefixElements();
            final int length = prefix.size();
                
            //fail fast check      
            curPrefix = prefix.get(length-1);//length -1 >= 0 as prefix array 
                                             //contains curPrefix as first element

            pe = curPrefix.getFirstActiveChildPos().getProgram(curPrefix);

            assert pe instanceof CopyAssignment 
                   || pe instanceof MethodReference
                   || pe instanceof New;
        
            int i = length - 1;
            do {
                result = curPrefix.getFirstActiveChildPos().append(result);
                i--;
                if (i >= 0) {
                    curPrefix = prefix.get(i);
                }
            } while(i >= 0);       

        } else {
            assert pe instanceof CopyAssignment 
                   || pe instanceof MethodReference
                   || pe instanceof New;
        }
        return result;
    }
    
    
    private static StatementBlock replaceStatement(JavaBlock jb, 
                                                   StatementBlock replacement) {
        PosInProgram pos = getPosInProgram(jb);
        int lastPos = pos.last();
        ContextStatementBlockInstantiation csbi = 
            new ContextStatementBlockInstantiation(pos, 
                                                   pos.up().down(lastPos+1), 
                                                   null, 
                                                   jb.program());
        final NonTerminalProgramElement result = 
            ProgramContextAdder.INSTANCE.start(
                        (JavaNonTerminalProgramElement)jb.program(), 
                        replacement, 
                        csbi);
        return (StatementBlock) result;
    }
        

    private Instantiation instantiate(Term focusTerm, Services services) {
	//result cached?
	if(focusTerm == lastFocusTerm) {
	    return lastInstantiation;
	}

	//compute
	final Instantiation result = computeInstantiation(focusTerm, services);
	
	//cache and return
	lastFocusTerm = focusTerm;
	lastInstantiation = result;
	return result;	
    }

    

    //-------------------------------------------------------------------------
    //public interface
    //------------------------------------------------------------------------- 
    
    /**
     * Computes instantiation for contract rule on passed focus term.
     * Internally only serves as helper for instantiate(). 
     */
    public static Instantiation computeInstantiation(Term focusTerm, Services services) {
	//leading update?
	final Term u;
	final Term progPost;
	if(focusTerm.op() instanceof UpdateApplication) {
	    u = UpdateApplication.getUpdate(focusTerm);
	    progPost = UpdateApplication.getTarget(focusTerm);
	} else {
	    u = TB.skip();
	    progPost = focusTerm;
	}
	
	//focus (below update) must be modality term
	if(progPost.op() != Modality.BOX && progPost.op() != Modality.DIA) {
	    return null;
	}
	final Modality mod = (Modality) progPost.op();
	
        //active statement must be method call or new
        final Pair<Expression,MethodOrConstructorReference> methodCall
        	= getMethodCall(progPost.javaBlock(), services);
        if(methodCall == null) {
            return null;
        }
        final Expression actualResult = methodCall.first;        
        final MethodOrConstructorReference mr = methodCall.second;        
      
        //arguments of method call must be simple expressions
	final ExecutionContext ec 
		= MiscTools.getInnermostExecutionContext(progPost.javaBlock(), 
						   	 services); 	        
        for(Expression arg : mr.getArguments()) {
            if(!ProgramSVSort.SIMPLEEXPRESSION
        	             .canStandFor(arg, ec, services)) {
        	return null;
            }
        }
 
        //collect further information
	final KeYJavaType staticType = getStaticPrefixType(mr, services, ec);
	assert staticType != null;
	final ProgramMethod pm = getProgramMethod(mr, 
		                                  staticType,
		                                  ec, 
		                                  services);
	assert pm != null;
	final Term actualSelf 
		= getActualSelf(mr, pm, ec, services);
	final ImmutableList<Term> actualParams
		= getActualParams(mr, ec, services);
	
	//cache and return result
	final Instantiation result
		= new Instantiation(u, 
			     	    progPost, 
			     	    mod,
			     	    actualResult,
			     	    actualSelf,
			     	    staticType,
			     	    mr,
			     	    pm,
			     	    actualParams);
	return result;
    }
    
    
    @Override
    public boolean isApplicable(Goal goal, 
                                PosInOccurrence pio) {        
	//focus must be top level succedent
	if(pio == null || !pio.isTopLevel() || pio.isInAntec()) {
	    return false;
	}

	//instantiation must succeed
	final Instantiation inst = instantiate(pio.subTerm(), 
		                               goal.proof().getServices());
	if(inst == null) {
	    return false;
	}

        //there must be applicable contracts for the operation
        final ImmutableSet<FunctionalOperationContract> contracts 
                = getApplicableContracts(goal.proof().getServices(), 
                	                 inst.pm, 
                	                 inst.staticType, 
                	                 inst.mod);
        if(contracts.isEmpty()) {
            return false;
        }

        //applying a contract here must not create circular dependencies 
        //between proofs
        for(FunctionalOperationContract contract : contracts) {
            if(goal.proof().mgt().isContractApplicable(contract)) {
        	return true;
            }
        }
        return false;
    }

    
    @Override    
    public ImmutableList<Goal> apply(Goal goal, 
	    			     Services services, 
	    			     RuleApp ruleApp) {
	//get instantiation
	final Instantiation inst 
		= instantiate(ruleApp.posInOccurrence().subTerm(), services);
        final JavaBlock jb = inst.progPost.javaBlock();
        
        //configure contract
        final FunctionalOperationContract contract;
        if(ruleApp instanceof ContractRuleApp) {
            //the contract is already fixed 
            //(probably because we're in the process of reading in a 
            //proof from a file)
            contract = (FunctionalOperationContract)((ContractRuleApp) ruleApp)
                                           .getInstantiation();            
        } else { 
            contract = configureContract(services, 
        	    		         inst.pm, 
        	    		         inst.staticType, 
        	    		         inst.mod);
            if(contract == null) {
        	return null;
            }
        }
        assert contract.getTarget().equals(inst.pm);
        
	//prepare heapBefore_method
     	final LocationVariable heapAtPreVar 
     		= TB.heapAtPreVar(services, 
     				  "heapBefore_" + inst.pm.getName(), 
     				  true);
     	goal.addProgramVariable(heapAtPreVar);
        final Term heapAtPre = TB.var(heapAtPreVar);

        //create variables for result and exception
        final ProgramVariable resultVar 
        	= inst.pm.isConstructor()
        	  ? TB.selfVar(services, inst.staticType, true)
                  : TB.resultVar(services, inst.pm, true);
        if(resultVar != null) {
            goal.addProgramVariable(resultVar);
        }
        assert inst.pm.isConstructor() 
               || !(inst.actualResult != null && resultVar == null);
        final ProgramVariable excVar = TB.excVar(services, inst.pm, true);
        assert excVar != null;
        goal.addProgramVariable(excVar);
        
        //translate the contract
        final Term contractSelf 
        	= OpReplacer.replace(TB.heap(services), 
        		             heapAtPre,
        		             inst.pm.isConstructor() 
        		               ? TB.var(resultVar) 
        		               : inst.actualSelf);
        final ImmutableList<Term> contractParams
        	= OpReplacer.replace(TB.heap(services), 
        			    heapAtPre, 
        			    inst.actualParams);
        final Term contractResult
        	= inst.pm.isConstructor() || resultVar == null 
        	  ? null 
                  : TB.var(resultVar);
        final Term pre  = contract.getPre(TB.heap(services), 
        				  contractSelf, 
        				  contractParams, 
        				  services);
        final Term post = contract.getPost(TB.heap(services),
        	                           contractSelf, 
        				   contractParams, 
                                           contractResult, 
                                           TB.var(excVar), 
                                           heapAtPre,
                                           services);
        final Term mod = contract.getMod(TB.heap(services),
        	                         contractSelf,
        	                         contractParams, 
        	                         services);
        final Term mby = contract.hasMby() 
                         ? contract.getMby(TB.heap(services), 
                        	 	   contractSelf, 
                        	 	   contractParams, 
                        	 	   services) 
                         : null;
        
        //split goal into three/four branches
        final ImmutableList<Goal> result;
        final Goal preGoal, postGoal, excPostGoal, nullGoal;
        final ReferencePrefix rp = inst.mr.getReferencePrefix();
        if(rp != null 
           && !(rp instanceof ThisReference) 
           && !(rp instanceof SuperReference)
           && !(rp instanceof TypeReference)) {
            result = goal.split(4);
            postGoal = result.tail().tail().tail().head();
            excPostGoal = result.tail().tail().head();
            preGoal = result.tail().head();
            nullGoal = result.head();
            nullGoal.setBranchLabel("Null reference (" 
        	                    + inst.actualSelf 
        	                    + " = null)");
        } else {
            result = goal.split(3);
            postGoal = result.tail().tail().head();
            excPostGoal = result.tail().head();
            preGoal = result.head();
            nullGoal = null;
        }
        preGoal.setBranchLabel("Pre");
        postGoal.setBranchLabel("Post");
        excPostGoal.setBranchLabel("Exceptional Post");
        
        //prepare common stuff for the three branches
        final Triple<Term,Term,Term> anonAssumptionAndUpdateAndHeap 
        	= createAnonUpdate(inst.pm, mod, services);
        final Term anonAssumption = anonAssumptionAndUpdateAndHeap.first;
        final Term anonUpdate     = anonAssumptionAndUpdateAndHeap.second;
        final Term anonHeap       = anonAssumptionAndUpdateAndHeap.third;
        final Term heapAtPreUpdate = TB.elementary(services, 
        					   heapAtPreVar, 
        					   TB.heap(services));
        
        final Term excNull = TB.equals(TB.var(excVar), TB.NULL(services));
        final Term excCreated = TB.created(services, TB.var(excVar));
        final Term freePost = getFreePost(inst.pm,
	    		     		  inst.staticType,
	    		     		  contractResult,
	    		     		  contractSelf,
	    		     		  heapAtPre,
	    		     		  services);  
        final Term freeExcPost = inst.pm.isConstructor() 
                                 ? freePost 
                                 : TB.tt();        
        final Term postAssumption 
        	= TB.applySequential(new Term[]{inst.u, heapAtPreUpdate}, 
        		   	     TB.and(anonAssumption,
        		   		    TB.apply(anonUpdate,
        		   	                     TB.and(new Term[]{excNull, 
                                	     	                       freePost, 
                                	     	                       post}))));
        final Term excPostAssumption 
        	= TB.applySequential(new Term[]{inst.u, heapAtPreUpdate}, 
        		   TB.and(anonAssumption,
                                  TB.apply(anonUpdate,
                                           TB.and(new Term[]{TB.not(excNull),
                                	     	             excCreated, 
                                	     	             freeExcPost, 
                                	     	             post}))));
       
        //create "Pre" branch
	Term reachableState = TB.wellFormedHeap(services);
	int i = 0;
	for(Term arg : contractParams) {
	    KeYJavaType argKJT = contract.getTarget().getParameterType(i++);
	    reachableState = TB.and(reachableState,
		                    TB.reachableValue(services, arg, argKJT));
	}
	final ContractPO po 
		= services.getSpecificationRepository()
		          .getPOForProof(goal.proof());
	final Term mbyOk;	
	if(po != null && po.getMbyAtPre() != null && mby != null) {
	    mbyOk = TB.and(TB.leq(TB.zero(services), mby, services),
		           TB.lt(mby, po.getMbyAtPre(), services));
	} else {
	    mbyOk = TB.tt();
	}
        preGoal.changeFormula(new SequentFormula(
        			TB.applySequential(new Term[]{inst.u, 
        						      heapAtPreUpdate}, 
        	                                   TB.and(new Term[]{pre, 
        	                                	   	     reachableState, 
        	                                	   	     mbyOk}))),
                              ruleApp.posInOccurrence());
       
        //create "Post" branch
	final StatementBlock resultAssign;
	if(inst.actualResult == null) {
	    resultAssign = new StatementBlock();
	} else {
	    final CopyAssignment ca 
	    	= new CopyAssignment(inst.actualResult, resultVar);
	    resultAssign = new StatementBlock(ca);
	}        
        final StatementBlock postSB 
        	= replaceStatement(jb, resultAssign);
        final Term normalPost 
            	= TB.apply(anonUpdate,
                           TB.prog(inst.mod,
                                   JavaBlock.createJavaBlock(postSB),
                                   inst.progPost.sub(0)));
        postGoal.addFormula(new SequentFormula(TB.wellFormed(services, 
        	                                                 anonHeap)), 
        	            true, 
        	            false);
        postGoal.changeFormula(new SequentFormula(TB.apply(inst.u, 
        						       normalPost)),
        	               ruleApp.posInOccurrence());
        postGoal.addFormula(new SequentFormula(postAssumption), 
        	            true, 
        	            false);
        
        //create "Exceptional Post" branch
        final StatementBlock excPostSB 
            = replaceStatement(jb, new StatementBlock(new Throw(excVar)));
        final Term excPost
            = TB.apply(anonUpdate,
                       TB.prog(inst.mod,
                               JavaBlock.createJavaBlock(excPostSB), 
                               inst.progPost.sub(0)));
        excPostGoal.addFormula(new SequentFormula(TB.wellFormed(services, 
                				                    anonHeap)), 
                	       true, 
                	       false);        
        excPostGoal.changeFormula(new SequentFormula(TB.apply(inst.u, 
        						          excPost)),
        	                  ruleApp.posInOccurrence());        
        excPostGoal.addFormula(new SequentFormula(excPostAssumption), 
        	               true, 
        	               false);
        
        
        //create "Null Reference" branch
        if(nullGoal != null) {
            final Term actualSelfNotNull 
            	= TB.not(TB.equals(inst.actualSelf, TB.NULL(services)));
            nullGoal.changeFormula(new SequentFormula(TB.apply(
        	    				inst.u, 
        					actualSelfNotNull)),
        	                   ruleApp.posInOccurrence());                    
        }
        
        //create justification
        final RuleJustificationBySpec just 
        	= new RuleJustificationBySpec(contract);
        final ComplexRuleJustificationBySpec cjust 
            	= (ComplexRuleJustificationBySpec)
            	    goal.proof().env().getJustifInfo().getJustification(this);
        cjust.add(ruleApp, just);
        
        return result;
    }
    
    
    @Override    
    public Name name() {
        return NAME;
    }


    @Override    
    public String displayName() { 
        return NAME.toString();
    }
    

    @Override
    public String toString() {
        return displayName();
    }
    
    
    
    //-------------------------------------------------------------------------
    //inner classes
    //-------------------------------------------------------------------------

    public static final class Instantiation {
	public final Term u;
	public final Term progPost;
	public final Modality mod;
	public final Expression actualResult;
	public final Term actualSelf;	
	public final KeYJavaType staticType;	
	public final MethodOrConstructorReference mr;
	public final ProgramMethod pm;
	public final ImmutableList<Term> actualParams;
	
	public Instantiation(Term u, 
			     Term progPost, 
			     Modality mod,
			     Expression actualResult,
			     Term actualSelf,
			     KeYJavaType staticType,
			     MethodOrConstructorReference mr,
			     ProgramMethod pm,
			     ImmutableList<Term> actualParams) {
	    assert u != null;
	    assert u.sort() == Sort.UPDATE;
	    assert progPost != null;
	    assert progPost.sort() == Sort.FORMULA;
	    assert mod != null;
	    assert mr != null;
	    assert pm != null;
	    assert actualParams != null;
	    this.u = u;
	    this.progPost = progPost;
	    this.mod = mod;
	    this.actualResult = actualResult;
	    this.actualSelf = actualSelf;
	    this.staticType = staticType;
	    this.mr = mr;
	    this.pm = pm;
	    this.actualParams = actualParams;
	}
    }    
}