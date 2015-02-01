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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.key_project.key4eclipse.starter.core.util.KeYUtil.SourceLocation;
import org.key_project.sed.core.model.ISEDBranchCondition;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.core.model.ISEDTermination;
import org.key_project.sed.core.model.ISourcePathProvider;
import org.key_project.sed.core.model.memory.SEDMemoryBranchCondition;
import org.key_project.sed.key.core.model.IKeYSEDDebugNode;
import org.key_project.sed.key.core.model.KeYBranchCondition;
import org.key_project.sed.key.core.model.KeYBranchStatement;
import org.key_project.sed.key.core.model.KeYConstraint;
import org.key_project.sed.key.core.model.KeYDebugTarget;
import org.key_project.sed.key.core.model.KeYExceptionalMethodReturn;
import org.key_project.sed.key.core.model.KeYExceptionalTermination;
import org.key_project.sed.key.core.model.KeYLoopBodyTermination;
import org.key_project.sed.key.core.model.KeYLoopCondition;
import org.key_project.sed.key.core.model.KeYLoopInvariant;
import org.key_project.sed.key.core.model.KeYLoopStatement;
import org.key_project.sed.key.core.model.KeYMethodCall;
import org.key_project.sed.key.core.model.KeYMethodContract;
import org.key_project.sed.key.core.model.KeYMethodReturn;
import org.key_project.sed.key.core.model.KeYStatement;
import org.key_project.sed.key.core.model.KeYTermination;
import org.key_project.sed.key.core.model.KeYThread;
import org.key_project.sed.key.core.model.KeYVariable;
import org.key_project.util.jdt.JDTUtil;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionBaseMethodReturn;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionBlockStartNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionBranchCondition;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionBranchStatement;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionConstraint;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionExceptionalMethodReturn;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionLoopCondition;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionLoopInvariant;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionLoopStatement;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionMethodCall;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionMethodReturn;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionNode;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionOperationContract;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionStatement;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionTermination;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionTermination.TerminationKind;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionVariable;

/**
 * Provides utility methods which are used by {@link IKeYSEDDebugNode}
 * implementations to compute their content and child {@link IKeYSEDDebugNode}s.
 * @author Martin Hentschel
 */
public final class KeYModelUtil {
   /**
    * Forbid instances.
    */
   private KeYModelUtil() {
   }

   /**
    * <p>
    * Creates new {@link IKeYSEDDebugNode}s for new {@link IExecutionNode}s
    * which are added after the existing {@link IKeYSEDDebugNode}s.
    * </p>
    * <p>
    * The assumption is that new children are only added in the end and
    * that existing children are never replaced or removed.
    * </p>
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param oldChildren The existing {@link IKeYSEDDebugNode}s to keep.
    * @param executionChildren The {@link IExecutionNode}s of the execution tree to create debug model representations for.
    * @return The created debug model representations.
    * @throws DebugException Occurred Exception.
    */
   public static IKeYSEDDebugNode<?>[] updateChildren(IKeYSEDDebugNode<?> parent, 
                                                      IKeYSEDDebugNode<?>[] oldChildren, 
                                                      IExecutionNode<?>[] executionChildren) throws DebugException {
      if (executionChildren != null) {
         IKeYSEDDebugNode<?>[] result = new IKeYSEDDebugNode<?>[executionChildren.length];
         // Add old children
         System.arraycopy(oldChildren, 0, result, 0, oldChildren.length);
         // Create new children
         for (int i = oldChildren.length; i < executionChildren.length; i++) {
            result[i] = createChild(parent, executionChildren[i]);
         }
         return result;
      }
      else {
         return new IKeYSEDDebugNode<?>[0];
      }
   }
   
   /**
    * Creates new {@link IKeYSEDDebugNode}s for the given {@link IExecutionNode}s.
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param executionChildren The {@link IExecutionNode}s of the execution tree to create debug model representations for.
    * @return The created debug model representations.
    * @throws DebugException Occurred Exception.
    */
   public static IKeYSEDDebugNode<?>[] createChildren(IKeYSEDDebugNode<?> parent, 
                                                      IExecutionNode<?>[] executionChildren) throws DebugException {
      if (executionChildren != null) {
         IKeYSEDDebugNode<?>[] result = new IKeYSEDDebugNode<?>[executionChildren.length];
         for (int i = 0; i < executionChildren.length; i++) {
            result[i] = createChild(parent, executionChildren[i]);
         }
         return result;
      }
      else {
         return new IKeYSEDDebugNode<?>[0];
      }
   }

   /**
    * Creates an {@link IKeYSEDDebugNode} for the given {@link IExecutionNode}
    * as child of the given parent {@link IKeYSEDDebugNode}.
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param executionNode The {@link IExecutionNode} of the execution tree.
    * @return The created {@link IKeYSEDDebugNode}.
    * @throws DebugException Occurred Exception.
    */
   protected static IKeYSEDDebugNode<?> createChild(IKeYSEDDebugNode<?> parent, IExecutionNode<?> executionNode) throws DebugException {
      KeYDebugTarget target = parent.getDebugTarget();
      KeYThread thread = parent.getThread();
      return createNode(target, thread, parent, executionNode);
   }
   
   /**
    * Creates the {@link KeYMethodReturn} for the given {@link IExecutionMethodReturn}.
    * @param target The {@link KeYDebugTarget} to use.
    * @param thread The parent {@link KeYThread}.
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param executionNode The {@link IExecutionNode} of the execution tree.
    * @return The created {@link IKeYSEDDebugNode}.
    * @throws DebugException Occurred Exception.
    */
   public static IKeYSEDDebugNode<?> createNode(KeYDebugTarget target, 
                                                KeYThread thread, 
                                                IKeYSEDDebugNode<?> parent,
                                                IExecutionNode<?> executionNode) throws DebugException {
      IKeYSEDDebugNode<?> result = target.getDebugNode(executionNode);
      if (result != null) {
         if (parent != null) {
            if (result.getParent() == null) {
               result.setParent(parent);
            }
            else {
               Assert.isTrue(result.getParent() == parent);
            }
         }
      }
      else {
         if (executionNode instanceof IExecutionBranchCondition) {
            result = new KeYBranchCondition(target, parent, thread, (IExecutionBranchCondition)executionNode);
         }
         else if (executionNode instanceof IExecutionBranchStatement) {
            result = new KeYBranchStatement(target, parent, thread, (IExecutionBranchStatement)executionNode);
         }
         else if (executionNode instanceof IExecutionLoopCondition) {
            result = new KeYLoopCondition(target, parent, thread, (IExecutionLoopCondition)executionNode);
         }
         else if (executionNode instanceof IExecutionLoopStatement) {
            result = new KeYLoopStatement(target, parent, thread, (IExecutionLoopStatement)executionNode);
         }
         else if (executionNode instanceof IExecutionMethodCall) {
            result = new KeYMethodCall(target, parent, thread, (IExecutionMethodCall)executionNode);
         }
         else if (executionNode instanceof IExecutionMethodReturn) {
            IExecutionMethodReturn executionReturn = ((IExecutionMethodReturn)executionNode);
            IKeYSEDDebugNode<?> callNode = target.getDebugNode(executionReturn.getMethodCall());
            Assert.isTrue(callNode instanceof KeYMethodCall);
            KeYMethodCall keyCall = (KeYMethodCall)callNode;
            result = createMethodReturn(target, thread, parent, keyCall, executionReturn);
         }
         else if (executionNode instanceof IExecutionExceptionalMethodReturn) {
            IExecutionExceptionalMethodReturn executionReturn = ((IExecutionExceptionalMethodReturn)executionNode);
            IKeYSEDDebugNode<?> callNode = target.getDebugNode(executionReturn.getMethodCall());
            Assert.isTrue(callNode instanceof KeYMethodCall);
            KeYMethodCall keyCall = (KeYMethodCall)callNode;
            result = createExceptionalMethodReturn(target, thread, parent, keyCall, executionReturn);
         }
         else if (executionNode instanceof IExecutionStatement) {
            result = new KeYStatement(target, parent, thread, (IExecutionStatement)executionNode);
         }
         else if (executionNode instanceof IExecutionOperationContract) {
            result = new KeYMethodContract(target, parent, thread, (IExecutionOperationContract)executionNode);
         }
         else if (executionNode instanceof IExecutionLoopInvariant) {
            result = new KeYLoopInvariant(target, parent, thread, (IExecutionLoopInvariant)executionNode);
         }
         else if (executionNode instanceof IExecutionTermination) {
            IExecutionTermination terminationExecutionNode = (IExecutionTermination)executionNode;
            result = createTermination(target, thread, parent, terminationExecutionNode);
         }
         else {
            throw new DebugException(LogUtil.getLogger().createErrorStatus("Not supported execution node \"" + executionNode + "\"."));
         }
      }
      return result;
   }
   
   /**
    * Creates the {@link KeYMethodReturn} for the given {@link IExecutionMethodReturn}.
    * @param target The {@link KeYDebugTarget} to use.
    * @param thread The parent {@link KeYThread}.
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param keyCall The {@link KeYMethodCall} which is returned by the given {@link IExecutionMethodReturn}.
    * @param executionReturn The {@link IExecutionMethodReturn} of the execution tree.
    * @return The {@link KeYMethodReturn} for the given {@link IExecutionTermination}.
    * @throws DebugException Occurred Exception.
    */
   public static KeYMethodReturn createMethodReturn(KeYDebugTarget target, 
                                                    KeYThread thread, 
                                                    IKeYSEDDebugNode<?> parent, 
                                                    KeYMethodCall keyCall, 
                                                    IExecutionMethodReturn executionReturn) throws DebugException {
      synchronized (keyCall) {
         KeYMethodReturn resultReturn = (KeYMethodReturn)keyCall.getMethodReturn(executionReturn);
         if (resultReturn != null) {
            // Reuse method return created by the method call and set its parent now
            if (resultReturn.getParent() == null) {
               resultReturn.setParent(parent);
            }
            else {
               Assert.isTrue(resultReturn.getParent() == parent);
            }
            return resultReturn;
         }
         else {
            // Create new method return
            return new KeYMethodReturn(target, parent, thread, keyCall, executionReturn);
         }
      }
   }
   
   /**
    * Creates the {@link KeYExceptionalMethodReturn} for the given {@link IExecutionExceptionalMethodReturn}.
    * @param target The {@link KeYDebugTarget} to use.
    * @param thread The parent {@link KeYThread}.
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param keyCall The {@link KeYMethodCall} which is returned by the given {@link IExecutionExceptionalMethodReturn}.
    * @param executionReturn The {@link IExecutionExceptionalMethodReturn} of the execution tree.
    * @return The {@link KeYExceptionalMethodReturn} for the given {@link IExecutionTermination}.
    * @throws DebugException Occurred Exception.
    */
   public static KeYExceptionalMethodReturn createExceptionalMethodReturn(KeYDebugTarget target, 
                                                                          KeYThread thread, 
                                                                          IKeYSEDDebugNode<?> parent, 
                                                                          KeYMethodCall keyCall, 
                                                                          IExecutionExceptionalMethodReturn executionReturn) throws DebugException {
      synchronized (keyCall) {
         KeYExceptionalMethodReturn resultReturn = (KeYExceptionalMethodReturn)keyCall.getMethodReturn(executionReturn);
         if (resultReturn != null) {
            // Reuse exceptional method return created by the method call and set its parent now
            if (resultReturn.getParent() == null) {
               resultReturn.setParent(parent);
            }
            else {
               Assert.isTrue(resultReturn.getParent() == parent);
            }
            return resultReturn;
         }
         else {
            // Create new exceptional method return
            return new KeYExceptionalMethodReturn(target, parent, thread, keyCall, executionReturn);
         }
      }
   }
   
   /**
    * Creates the termination node for the given {@link IExecutionTermination}.
    * @param target The {@link KeYDebugTarget} to use.
    * @param thread The parent {@link KeYThread}.
    * @param parent The parent {@link IKeYSEDDebugNode} in the debug model.
    * @param terminationExecutionNode The {@link IExecutionTermination} of the execution tree.
    * @return The termination node for the given {@link IExecutionTermination}.
    * @throws DebugException Occurred Exception.
    */
   public static IKeYSEDDebugNode<?> createTermination(KeYDebugTarget target, 
                                                       KeYThread thread, 
                                                       IKeYSEDDebugNode<?> parent, 
                                                       IExecutionTermination terminationExecutionNode) throws DebugException {
      synchronized (thread) {
         ISEDTermination terminationNode = thread.getTermination(terminationExecutionNode);
         if (terminationNode != null) {
            // Reuse method return created by the method call and set its parent now
            if (terminationNode.getParent() == null) {
               if (terminationNode instanceof KeYExceptionalTermination) {
                  ((KeYExceptionalTermination)terminationNode).setParent(parent);
               }
               else if (terminationNode instanceof KeYTermination) {
                  ((KeYTermination)terminationNode).setParent(parent);
               }
               else if (terminationNode instanceof KeYLoopBodyTermination) {
                  ((KeYLoopBodyTermination)terminationNode).setParent(parent);
               }
               else {
                  throw new DebugException(LogUtil.getLogger().createErrorStatus("Not supported termination \"" + terminationNode + "\"."));
               }
            }
            else {
               Assert.isTrue(terminationNode.getParent() == parent);
            }
            return (IKeYSEDDebugNode<?>)terminationNode;
         }
         else {
            // Create new termination
            if (terminationExecutionNode.getTerminationKind() == TerminationKind.EXCEPTIONAL) {
               return new KeYExceptionalTermination(target, parent, thread, terminationExecutionNode);
            }
            else if (terminationExecutionNode.getTerminationKind() == TerminationKind.NORMAL) {
               return new KeYTermination(target, parent, thread, terminationExecutionNode);
            }
            else if (terminationExecutionNode.getTerminationKind() == TerminationKind.LOOP_BODY) {
               return new KeYLoopBodyTermination(target, parent, thread, terminationExecutionNode);
            }
            else {
               throw new DebugException(LogUtil.getLogger().createErrorStatus("Not supported termination kind \"" + terminationExecutionNode.getTerminationKind() + "\"."));
            }
         }
      }
   }
   
   /**
    * Tries to update the given {@link SourceLocation} of the given
    * {@link IStackFrame} with the location provided by JDT. If possible
    * the new location is returned and the original location otherwise.
    * @param frame The {@link IStackFrame} which defines the file to parse.
    * @param sourceLocation The {@link SourceLocation} which describes the {@link ASTNode} to update location from.
    * @return The updated {@link SourceLocation} or the initial {@link SourceLocation}.
    * @throws DebugException Occurred Exception.
    */
   public static SourceLocation updateLocationFromAST(IStackFrame frame,
                                                      SourceLocation sourceLocation) throws DebugException {
      try {
         ASTNode statementNode = findASTNode(frame, sourceLocation);
         return updateLocationFromAST(sourceLocation, statementNode);
      }
      catch (Exception e) {
         throw new DebugException(LogUtil.getLogger().createErrorStatus(e));
      }
   }
   
   /**
    * Tries to update the given {@link SourceLocation} of the given
    * {@link IStackFrame} with the location provided by JDT. If possible
    * the new location is returned and the original location otherwise.    * @param locationToUpdate The {@link SourceLocation} to return if no {@link ASTNode} is defined.
    * @param nodeToExtractLocationFrom An optional {@link ASTNode} which source location should replace the given one.
    * @return The updated {@link SourceLocation} or the initial {@link SourceLocation}.
    */
   public static SourceLocation updateLocationFromAST(SourceLocation locationToUpdate,
                                                      ASTNode nodeToExtractLocationFrom) {
      SourceLocation result = locationToUpdate;
      if (nodeToExtractLocationFrom != null) {
         result = new SourceLocation(-1, 
                                     nodeToExtractLocationFrom.getStartPosition(), 
                                     nodeToExtractLocationFrom.getStartPosition() + nodeToExtractLocationFrom.getLength());
      }
      return result;
   }
   
   /**
    * Searches the {@link ASTNode} in JDT which described by the given 
    * {@link IStackFrame} and the {@link SourceLocation}.
    * @param frame The {@link IStackFrame} which defines the file to parse.
    * @param sourceLocation The {@link SourceLocation} which describes the {@link ASTNode} to return.
    * @return The found {@link ASTNode} or {@code null} if not available.
    */
   public static ASTNode findASTNode(IStackFrame frame,
                                     SourceLocation sourceLocation) {
      ASTNode statementNode = null;
      if (sourceLocation != null && sourceLocation.getCharEnd() >= 0) {
         ICompilationUnit compilationUnit = findCompilationUnit(frame);
         if (compilationUnit != null) {
            ASTNode root = JDTUtil.parse(compilationUnit, sourceLocation.getCharStart(), sourceLocation.getCharEnd() - sourceLocation.getCharStart());
            statementNode = ASTNodeByEndIndexSearcher.search(root, sourceLocation.getCharEnd());
         }
      }
      return statementNode;
   }

   /**
    * Tries to find an {@link ICompilationUnit} for the given {@link IStackFrame}.
    * @param frame The given {@link IStackFrame} for that is an {@link ICompilationUnit} required.
    * @return The found {@link ICompilationUnit}.
    */
   public static ICompilationUnit findCompilationUnit(IStackFrame frame) {
      ICompilationUnit result = null;
      if (frame != null) {
         Object source = frame.getLaunch().getSourceLocator().getSourceElement(frame);
         if (source instanceof IFile) {
            IJavaElement element = JavaCore.create((IFile)source);
            if (element instanceof ICompilationUnit) {
               result = (ICompilationUnit)element;
            }
         }
      }
      return result;
   }

   /**
    * Creates debug model representations for the {@link IExecutionVariable}s
    * contained in the given {@link IExecutionNode}.
    * @param debugNode The {@link IKeYSEDDebugNode} which should be used as parent.
    * @param executionNode The {@link IExecutionNode} to return its variables.
    * @return The contained {@link KeYVariable}s as debug model representation.
    */
   public static KeYVariable[] createCallStateVariables(IKeYSEDDebugNode<?> debugNode, 
                                                        IExecutionBaseMethodReturn<?> executionNode) throws DebugException {
      try {
         if (executionNode != null && !executionNode.isDisposed() && debugNode != null) {
            IExecutionVariable[] variables = executionNode.getCallStateVariables();
            return createVariables(debugNode, variables);
         }
         else {
            return new KeYVariable[0];
         }
      }
      catch (ProofInputException e) {
         throw new DebugException(LogUtil.getLogger().createErrorStatus("Can't compute call state variables.", e));
      }
   }

   /**
    * Creates debug model representations for the {@link IExecutionVariable}s
    * contained in the given {@link IExecutionNode}.
    * @param debugNode The {@link IKeYSEDDebugNode} which should be used as parent.
    * @param executionNode The {@link IExecutionNode} to return its variables.
    * @return The contained {@link KeYVariable}s as debug model representation.
    */
   public static KeYVariable[] createVariables(IKeYSEDDebugNode<?> debugNode, 
                                               IExecutionNode<?> executionNode) throws DebugException {
      try {
         if (executionNode != null && !executionNode.isDisposed() && debugNode != null) {
            IExecutionVariable[] variables = executionNode.getVariables();
            return createVariables(debugNode, variables);
         }
         else {
            return new KeYVariable[0];
         }
      }
      catch (ProofInputException e) {
         throw new DebugException(LogUtil.getLogger().createErrorStatus("Can't compute variables.", e));
      }
   }

   /**
    * Creates debug model representations for the {@link IExecutionVariable}s
    * contained in the given {@link IExecutionNode}.
    * @param debugNode The {@link IKeYSEDDebugNode} which should be used as parent.
    * @param variables The {@link IExecutionVariable}s.
    * @return The contained {@link KeYVariable}s as debug model representation.
    */
   public static KeYVariable[] createVariables(IKeYSEDDebugNode<?> debugNode, 
                                               IExecutionVariable[] variables) {
      if (variables != null) {
         KeYVariable[] result = new KeYVariable[variables.length];
         for (int i = 0; i < variables.length; i++) {
            result[i] = new KeYVariable(debugNode.getDebugTarget(), (IStackFrame)debugNode, variables[i]);
         }
         return result;
      }
      else {
         return new KeYVariable[0];
      }
   }

   /**
    * Converts the given call stack of {@link IExecutionNode} into 
    * a call stack of {@link ISEDDebugNode}s.
    * @param debugTarget The {@link KeYDebugTarget} which maps {@link IExecutionNode}s to {@link ISEDDebugNode}s.
    * @param callStack The call stack to convert.
    * @return The converted call stack.
    */
   public static IKeYSEDDebugNode<?>[] createCallStack(KeYDebugTarget debugTarget, IExecutionNode<?>[] callStack) {
      if (debugTarget != null && callStack != null) {
         IKeYSEDDebugNode<?>[] result = new IKeYSEDDebugNode<?>[callStack.length];
         int i = 0;
         for (IExecutionNode<?> executionNode : callStack) {
            IKeYSEDDebugNode<?> debugNode = debugTarget.getDebugNode(executionNode);
            Assert.isNotNull(debugNode, "Can't find debug node for execution node \"" + executionNode + "\".");
            result[i] = debugNode;
            i++;
         }
         return result;
      }
      else {
         return new IKeYSEDDebugNode<?>[0];
      }
   }

   /**
    * Creates debug model representations for the {@link IExecutionConstraint}s
    * contained in the given {@link IExecutionNode}.
    * @param debugNode The {@link IKeYSEDDebugNode} which should be used as parent.
    * @param executionNode The {@link IExecutionNode} to return its constraints.
    * @return The contained {@link KeYConstraint}s as debug model representation.
    */
   public static KeYConstraint[] createConstraints(IKeYSEDDebugNode<?> debugNode, 
                                                   IExecutionNode<?> executionNode) {
      if (executionNode != null && !executionNode.isDisposed() && debugNode != null) {
         IExecutionConstraint[] constraints = executionNode.getConstraints();
         if (constraints != null) {
            KeYConstraint[] result = new KeYConstraint[constraints.length];
            for (int i = 0; i < constraints.length; i++) {
               result[i] = new KeYConstraint(debugNode.getDebugTarget(), constraints[i]);
            }
            return result;
         }
         else {
            return new KeYConstraint[0];
         }
      }
      else {
         return new KeYConstraint[0];
      }
   }

   /**
    * Creates {@link ISEDBranchCondition}s for all completed code blocks.
    * @param child The child {@link IKeYSEDDebugNode}.
    * @return The created {@link ISEDBranchCondition}.
    * @throws DebugException Occurred Exception.
    */
   public static SEDMemoryBranchCondition[] createCompletedBlocksConditions(IKeYSEDDebugNode<?> child) throws DebugException {
      try {
         ImmutableList<IExecutionBlockStartNode<?>> completedBlocks = child.getExecutionNode().getCompletedBlocks();
         if (completedBlocks != null && completedBlocks.size() >= 1) {
            SEDMemoryBranchCondition[] result = new SEDMemoryBranchCondition[completedBlocks.size()];
            int i = 0;
            for (IExecutionBlockStartNode<?> completedBlock : completedBlocks) {
               IKeYSEDDebugNode<?> parent = child.getDebugTarget().getDebugNode(completedBlock);
               Assert.isNotNull(parent);
               result[i] = new SEDMemoryBranchCondition(child.getDebugTarget(), parent, child.getThread());
               result[i].addChild(child);
               result[i].setCallStack(KeYModelUtil.createCallStack(parent.getDebugTarget(), parent.getExecutionNode().getCallStack()));
               result[i].setName(child.getExecutionNode().getFormatedBlockCompletionCondition(completedBlock));
               result[i].setPathCondition(parent.getPathCondition());
               if (parent instanceof ISourcePathProvider) {
                  result[i].setSourcePath(((ISourcePathProvider) parent).getSourcePath());
               }
               i++;
            }
            return result;
         }
         else {
            return new SEDMemoryBranchCondition[0];
         }
      }
      catch (ProofInputException e) {
         throw new DebugException(LogUtil.getLogger().createErrorStatus("Can't compute method return condition.", e));
      }
   }

   /**
    * Computes the group end conditions.
    * @param node The {@link IKeYSEDDebugNode} for which group end conditions are requested.
    * @return The up to now known group end conditions.
    * @throws DebugException Occurred Exception.
    */
   public static ISEDBranchCondition[] computeGroupEndConditions(IKeYSEDDebugNode<? extends IExecutionBlockStartNode<?>> node) throws DebugException {
      ImmutableList<IExecutionNode<?>> completions = node.getExecutionNode().getBlockCompletions();
      ISEDBranchCondition[] result = new ISEDBranchCondition[completions.size()];
      int i = 0;
      for (IExecutionNode<?> completion : completions) {
         IKeYSEDDebugNode<?> keyCompletion = KeYModelUtil.createNode(node.getDebugTarget(), node.getThread(), null, completion);
         result[i] = keyCompletion.getGroupStartCondition(node);
         i++;
      }
      return result;
   }

   public static void sortyByOccurrence(ISEDDebugNode current, SEDMemoryBranchCondition[] conditions) throws DebugException {
      final Map<SEDMemoryBranchCondition, Integer> occurrenceOrder = computeOccurrenceOrder(current, conditions);
      Arrays.sort(conditions, new Comparator<SEDMemoryBranchCondition>() {
         @Override
         public int compare(SEDMemoryBranchCondition first, SEDMemoryBranchCondition second) {
            Integer firstValue = occurrenceOrder.get(first);
            Integer secondValue = occurrenceOrder.get(second);
            if (firstValue != null && secondValue != null) {
               return firstValue - secondValue;
            }
            else {
               return 0; // Something went wrong, can't compare
            }
         }
      });
   }

   protected static Map<SEDMemoryBranchCondition, Integer> computeOccurrenceOrder(ISEDDebugNode current, SEDMemoryBranchCondition[] conditions) throws DebugException {
      Map<ISEDDebugNode, SEDMemoryBranchCondition> starts = new HashMap<ISEDDebugNode, SEDMemoryBranchCondition>();
      for (SEDMemoryBranchCondition condition : conditions) {
         starts.put(condition.getParent(), condition);
      }
      Map<SEDMemoryBranchCondition, Integer> result = new HashMap<SEDMemoryBranchCondition, Integer>();
      int removedCount = 0;
      while (current != null && !starts.isEmpty()) {
         SEDMemoryBranchCondition condition = starts.remove(current);
         if (condition != null) {
            result.put(condition, conditions.length - removedCount);
            removedCount++;
         }
         current = current.getParent();
      }
      return result;
   }
}