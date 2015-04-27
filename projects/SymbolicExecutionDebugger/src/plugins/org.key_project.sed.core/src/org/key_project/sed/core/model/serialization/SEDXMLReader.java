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

package org.key_project.sed.core.model.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.key_project.sed.core.annotation.ISEDAnnotation;
import org.key_project.sed.core.annotation.ISEDAnnotationLink;
import org.key_project.sed.core.annotation.ISEDAnnotationType;
import org.key_project.sed.core.model.ISEDBranchCondition;
import org.key_project.sed.core.model.ISEDConstraint;
import org.key_project.sed.core.model.ISEDDebugElement;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.ISEDTermination;
import org.key_project.sed.core.model.ISEDThread;
import org.key_project.sed.core.model.ISEDValue;
import org.key_project.sed.core.model.ISEDVariable;
import org.key_project.sed.core.model.impl.AbstractSEDBaseMethodReturn;
import org.key_project.sed.core.model.memory.ISEDMemoryBaseMethodReturn;
import org.key_project.sed.core.model.memory.ISEDMemoryDebugNode;
import org.key_project.sed.core.model.memory.ISEDMemoryGroupable;
import org.key_project.sed.core.model.memory.ISEDMemoryStackFrameCompatibleDebugNode;
import org.key_project.sed.core.model.memory.SEDMemoryBranchCondition;
import org.key_project.sed.core.model.memory.SEDMemoryBranchStatement;
import org.key_project.sed.core.model.memory.SEDMemoryConstraint;
import org.key_project.sed.core.model.memory.SEDMemoryDebugTarget;
import org.key_project.sed.core.model.memory.SEDMemoryExceptionalMethodReturn;
import org.key_project.sed.core.model.memory.SEDMemoryExceptionalTermination;
import org.key_project.sed.core.model.memory.SEDMemoryLoopBodyTermination;
import org.key_project.sed.core.model.memory.SEDMemoryLoopCondition;
import org.key_project.sed.core.model.memory.SEDMemoryLoopInvariant;
import org.key_project.sed.core.model.memory.SEDMemoryLoopStatement;
import org.key_project.sed.core.model.memory.SEDMemoryMethodCall;
import org.key_project.sed.core.model.memory.SEDMemoryMethodContract;
import org.key_project.sed.core.model.memory.SEDMemoryMethodReturn;
import org.key_project.sed.core.model.memory.SEDMemoryStatement;
import org.key_project.sed.core.model.memory.SEDMemoryTermination;
import org.key_project.sed.core.model.memory.SEDMemoryThread;
import org.key_project.sed.core.model.memory.SEDMemoryValue;
import org.key_project.sed.core.model.memory.SEDMemoryVariable;
import org.key_project.sed.core.util.SEDAnnotationUtil;
import org.key_project.util.java.ObjectUtil;
import org.key_project.util.java.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>
 * Instances of this class are used to read proprietary XML file
 * created via {@link SEDXMLWriter}. The result is a {@link List} of
 * contained {@link ISEDDebugTarget}s. The created instances are memory
 * instances and contains only the saved values. An execution at runtime, like
 * termination, step over, etc. is not possible.
 * </p>
 * <p>
 * The main use case of the serialization is to persistent an actual
 * {@link ISEDDebugTarget} as oracle file which is later used in test cases
 * to compare a given {@link ISEDDebugTarget} with the loaded instance
 * of the oracle file.
 * </p>
 * @author Martin Hentschel
 * @see SEDXMLWriter
 */
public class SEDXMLReader {
   /**
    * The {@link ILaunch} to use.
    */
   private final ILaunch launch;
   
   /**
    * Is this {@link ISEDDebugTarget} executable meaning that
    * suspend, resume, step operations and disconnect are supported?;
    */
   private final boolean executable;   
   
   /**
    * Constructor.
    */
   public SEDXMLReader() {
      this(null, false);
   }
   
   /**
    * Constructor.
    * @param launch The {@link ILaunch} to use.
    * @param executable {@code true} Support suspend, resume, etc.; {@code false} Do not support suspend, resume, etc.
    */
   public SEDXMLReader(ILaunch launch, boolean executable) {
      this.launch = launch;
      this.executable = executable;
   }

   /**
    * Parses the given XML content.
    * @param xml The XML content to parse.
    * @return The contained {@link ISEDDebugTarget}s in the given XML content.
    * @throws ParserConfigurationException Occurred Exception.
    * @throws SAXException Occurred Exception.
    * @throws IOException Occurred Exception.
    */
   public List<ISEDDebugTarget> read(String xml) throws ParserConfigurationException, SAXException, IOException {
      return xml != null ? read(new ByteArrayInputStream(xml.getBytes())) : null;
   }
   
   /**
    * Parses the given XML content.
    * @param xml The XML content to parse.
    * @return The contained {@link ISEDDebugTarget}s in the given XML content.
    * @throws ParserConfigurationException Occurred Exception.
    * @throws SAXException Occurred Exception.
    * @throws IOException Occurred Exception.
    * @throws CoreException Occurred Exception.
    */
   public List<ISEDDebugTarget> read(IFile file) throws ParserConfigurationException, SAXException, IOException, CoreException {
      return file != null ? read(file.getContents()) : null;
   }
   
   /**
    * Parses the given XML content defined by the {@link InputStream}.
    * @param in The {@link InputStream} with the XML content to parse.
    * @return The contained {@link ISEDDebugTarget}s in the given XML content.
    * @throws ParserConfigurationException Occurred Exception.
    * @throws SAXException Occurred Exception.
    * @throws IOException Occurred Exception.
    */
   public List<ISEDDebugTarget> read(InputStream in) throws ParserConfigurationException, SAXException, IOException {
      if (in != null) {
         try {
            // Parse XML document
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            SEDSAXHandler handler = new SEDSAXHandler();
            saxParser.parse(in, handler);
            // Create call stacks
            Set<Entry<ISEDMemoryDebugNode, List<String>>> entries = handler.getCallStackEntriesMap().entrySet();
            for (Entry<ISEDMemoryDebugNode, List<String>> entry : entries) {
               List<ISEDDebugNode> callStack = new LinkedList<ISEDDebugNode>();
               for (String nodeRefId : entry.getValue()) {
                  ISEDDebugElement element = handler.getElementById(nodeRefId);
                  if (element == null) {
                     throw new SAXException("Referenced node with ID \"" + nodeRefId + "\" is not available in model.");
                  }
                  if (!(element instanceof ISEDDebugNode)) {
                     throw new SAXException("Referenced node with ID \"" + nodeRefId + "\" refers to wrong model object \"" + element + "\".");
                  }
                  callStack.add((ISEDDebugNode)element);
               }
               entry.getKey().setCallStack(callStack.toArray(new ISEDDebugNode[callStack.size()]));
            }
            // Set known terminations
            Set<Entry<SEDMemoryThread, List<String>>> terminationEntries = handler.getTerminationEntriesMap().entrySet();
            for (Entry<SEDMemoryThread, List<String>> entry : terminationEntries) {
               for (String nodeRefId : entry.getValue()) {
                  ISEDDebugElement element = handler.getElementById(nodeRefId);
                  if (element == null) {
                     throw new SAXException("Referenced node with ID \"" + nodeRefId + "\" is not available in model.");
                  }
                  if (!(element instanceof ISEDTermination)) {
                     throw new SAXException("Referenced node with ID \"" + nodeRefId + "\" refers to wrong model object \"" + element + "\".");
                  }
                  entry.getKey().addTermination((ISEDTermination)element);
               }
            }
            // Set relevant constraints
            Set<Entry<SEDMemoryValue, List<String>>> relevantConstraintEntries = handler.getRelevantConstraintsMap().entrySet();
            for (Entry<SEDMemoryValue, List<String>> entry : relevantConstraintEntries) {
               for (String constraintRefId : entry.getValue()) {
                  ISEDDebugElement element = handler.getElementById(constraintRefId);
                  if (element == null) {
                     throw new SAXException("Referenced constraint with ID \"" + constraintRefId + "\" is not available in model.");
                  }
                  if (!(element instanceof ISEDConstraint)) {
                     throw new SAXException("Referenced constraint with ID \"" + constraintRefId + "\" refers to wrong model object \"" + element + "\".");
                  }
                  entry.getKey().addRelevantConstraint((ISEDConstraint)element);
               }
            }
            // Inject child references
            Set<Entry<ISEDMemoryDebugNode, List<ChildReference>>> childReferences = handler.getNodeChildReferences().entrySet();
            for (Entry<ISEDMemoryDebugNode, List<ChildReference>> entry : childReferences) {
               for (ChildReference references : entry.getValue()) {
                  ISEDDebugElement element = handler.getElementById(references.getId());
                  if (element == null) {
                     throw new SAXException("Referenced node with ID \"" + references.getId() + "\" is not available in model.");
                  }
                  if (!(element instanceof ISEDDebugNode)) {
                     throw new SAXException("Referenced node with ID \"" + references.getId() + "\" refers to wrong model object \"" + element + "\".");
                  }
                  entry.getKey().addChild((ISEDDebugNode)element);
               }
            }
            // Inject group start references
            Set<Entry<ISEDMemoryDebugNode, List<GroupEndReference>>> groupEndReferences = handler.getGroupEndReferences().entrySet();
            for (Entry<ISEDMemoryDebugNode, List<GroupEndReference>> entry : groupEndReferences) {
               for (GroupEndReference references : entry.getValue()) {
                  ISEDDebugElement element = handler.getElementById(references.getId());
                  if (element == null) {
                     throw new SAXException("Referenced node with ID \"" + references.getId() + "\" is not available in model.");
                  }
                  if (!(element instanceof ISEDBranchCondition)) {
                     throw new SAXException("Referenced node with ID \"" + references.getId() + "\" refers to wrong model object \"" + element + "\".");
                  }
                  entry.getKey().addGroupStartCondition((ISEDBranchCondition)element);
               }
            }
            // Inject method return conditions
            Set<Entry<AbstractSEDBaseMethodReturn, String>> returnConditions = handler.getMethodReturnConditionReferences().entrySet();
            for (Entry<AbstractSEDBaseMethodReturn, String> entry : returnConditions) {
               ISEDDebugElement element = handler.getElementById(entry.getValue());
               if (element == null) {
                  throw new SAXException("Referenced node with ID \"" + entry.getValue() + "\" is not available in model.");
               }
               if (!(element instanceof ISEDBranchCondition)) {
                  throw new SAXException("Referenced node with ID \"" + entry.getValue() + "\" refers to wrong model object \"" + element + "\".");
               }
               if (entry.getKey() instanceof SEDMemoryMethodReturn) {
                  ((SEDMemoryMethodReturn) entry.getKey()).setMethodReturnCondition((ISEDBranchCondition)element);
               }
               else if (entry.getKey() instanceof SEDMemoryExceptionalMethodReturn) {
                  ((SEDMemoryExceptionalMethodReturn) entry.getKey()).setMethodReturnCondition((ISEDBranchCondition)element);
               }
               else {
                  throw new SAXException("Unsupported method return \"" + entry.getKey() + "\".");
               }
            }
            // Return result
            return handler.getResult();
         }
         finally {
            in.close();
         }
      }
      else {
         return null;
      }
   }
   
   /**
    * SAX implementation of {@link DefaultHandler} used to parse XML content
    * created via {@link SEDXMLWriter}.
    * @author Martin Hentschel
    */
   private class SEDSAXHandler extends DefaultHandler {
      /**
       * The found {@link ISEDDebugTarget}s.
       */
      private final List<ISEDDebugTarget> result = new LinkedList<ISEDDebugTarget>();
      
      /**
       * The current {@link SEDMemoryDebugTarget}.
       */
      private SEDMemoryDebugTarget target;
      
      /**
       * The current {@link SEDMemoryThread}.
       */
      private SEDMemoryThread thread;
      
      /**
       * The parent hierarchy filled by {@link #startElement(String, String, String, Attributes)}
       * and emptied by {@link #endElement(String, String, String)}.
       */
      private final Deque<ISEDMemoryDebugNode> parentStack = new LinkedList<ISEDMemoryDebugNode>();
      
      /**
       * The parent hierarchy of variables and values filled by {@link #startElement(String, String, String, Attributes)}
       * and emptied by {@link #endElement(String, String, String)}.
       */
      private final Deque<IDebugElement> variablesValueStack = new LinkedList<IDebugElement>();
      
      /**
       * Maps {@link ISEDMemoryDebugNode} to the IDs of their calls tacks.
       */
      private final Map<ISEDMemoryDebugNode, List<String>> callStackEntriesMap = new HashMap<ISEDMemoryDebugNode, List<String>>();
      
      /**
       * Maps {@link ISEDThread} to the IDs of their known termination nodes.
       */
      private final Map<SEDMemoryThread, List<String>> terminationEntriesMap = new HashMap<SEDMemoryThread, List<String>>();

      /**
       * Maps the element ID ({@link ISEDDebugElement#getId()}) to the its {@link ISEDDebugElement} instance.
       */
      private final Map<String, ISEDDebugElement> elementIdMapping = new HashMap<String, ISEDDebugElement>();
      
      /**
       * Maps the annotation ID ({@link ISEDAnnotation#getId()}) to the its {@link ISEDAnnotation} instance.
       */
      private final Map<String, ISEDAnnotation> annotationIdMapping = new HashMap<String, ISEDAnnotation>();
      
      /**
       * Maps {@link ISEDMemoryDebugNode} to its child references.
       */
      private final Map<ISEDMemoryDebugNode, List<ChildReference>> nodeChildReferences = new HashMap<ISEDMemoryDebugNode, List<ChildReference>>();
      
      /**
       * Maps {@link ISEDMemoryDebugNode} to its group end references.
       */
      private final Map<ISEDMemoryDebugNode, List<GroupEndReference>> groupEndReferences = new HashMap<ISEDMemoryDebugNode, List<GroupEndReference>>();
      
      /**
       * Maps {@link AbstractSEDBaseMethodReturn}s to their method return conditions.
       */
      private final Map<AbstractSEDBaseMethodReturn, String> methodReturnConditionReferences = new HashMap<AbstractSEDBaseMethodReturn, String>();
      
      /**
       * Maps {@link SEDMemoryValue}d to the IDs of their relevant constraints.
       */
      private Map<SEDMemoryValue, List<String>> relevantConstraintsMap = new HashMap<SEDMemoryValue, List<String>>();
      
      /**
       * {@inheritDoc}
       */
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
         ISEDMemoryDebugNode parent = parentStack.peekFirst();
         IDebugElement parentVariableOrValue = variablesValueStack.peekFirst();
         if (isCallStackEntry(uri, localName, qName)) {
            List<String> callStack = callStackEntriesMap.get(parent);
            if (callStack == null) {
               callStack = new LinkedList<String>();
               callStackEntriesMap.put(parent, callStack);
            }
            callStack.add(getNodeIdRef(attributes));
         }
         else if (isRelevantConstraint(uri, localName, qName)) {
            if (parentVariableOrValue instanceof SEDMemoryValue) {
               List<String> entriesList = relevantConstraintsMap.get((SEDMemoryValue)parentVariableOrValue);
               if (entriesList == null) {
                  entriesList = new LinkedList<String>();
                  relevantConstraintsMap.put((SEDMemoryValue)parentVariableOrValue, entriesList);
               }
               entriesList.add(getConstraintIdRef(attributes));
            }
            else {
               throw new SAXException("Can't add relevant constraint to parent.");
            }
         }
         else if (isTerminationEntry(uri, localName, qName)) {
            if (parent == null) {
               List<String> entriesList = terminationEntriesMap.get(thread);
               if (entriesList == null) {
                  entriesList = new LinkedList<String>();
                  terminationEntriesMap.put(thread, entriesList);
               }
               entriesList.add(getNodeIdRef(attributes));
            }
            else {
               throw new SAXException("Can't add termination entry to parent.");
            }
         }
         else if (isCallStateVariable(uri, localName, qName)) {
            IVariable variable = createVariable(target, (IStackFrame)parent, uri, localName, qName, attributes);
            if (variablesValueStack.isEmpty()) {
               if (parent instanceof ISEDMemoryBaseMethodReturn) {
                  ((ISEDMemoryBaseMethodReturn)parent).addCallStateVariable(variable);
                  variablesValueStack.addFirst(variable);
               }
               else {
                  throw new SAXException("Can't add call state variable to parent.");
               }
            }
            else {
               throw new SAXException("Can't add call state variable to parent.");
            }
         }
         else {
            Object obj = createElement(target, parent != null ? parent : thread, thread, parentVariableOrValue, uri, localName, qName, attributes, annotationIdMapping, methodReturnConditionReferences);
            if (obj instanceof ISEDDebugElement) {
               ISEDDebugElement element = (ISEDDebugElement)obj;
               elementIdMapping.put(element.getId(), element);
            }
            if (obj instanceof ChildReference) {
               List<ChildReference> refs = nodeChildReferences.get(parent);
               if (refs == null) {
                  refs = new LinkedList<ChildReference>();
                  nodeChildReferences.put(parent, refs);
               }
               refs.add((ChildReference)obj);
            }
            else if (obj instanceof GroupEndReference) {
               List<GroupEndReference> refs = groupEndReferences.get(parent);
               if (refs == null) {
                  refs = new LinkedList<GroupEndReference>();
                  groupEndReferences.put(parent, refs);
               }
               refs.add((GroupEndReference)obj);
            }
            else if (obj instanceof SEDMemoryDebugTarget) {
               target = (SEDMemoryDebugTarget)obj;
               result.add(target);
            }
            else if (obj instanceof ISEDConstraint) {
               if (parent != null) {
                  parent.addConstraint((ISEDConstraint) obj);
               }
               else {
                  thread.addConstraint((ISEDConstraint) obj);
               }
            }
            else if (obj instanceof IVariable) {
               IVariable variable = (IVariable)obj;
               if (variablesValueStack.isEmpty()) {
                  if (parent instanceof ISEDMemoryStackFrameCompatibleDebugNode) {
                     ((ISEDMemoryStackFrameCompatibleDebugNode)parent).addVariable(variable);
                  }
                  else if (parent == null && thread != null) {
                     thread.addVariable(variable);
                  }
                  else {
                     throw new SAXException("Can't add variable to parent.");
                  }
               }
               else {
                  if (parentVariableOrValue instanceof SEDMemoryValue) {
                     ((SEDMemoryValue)parentVariableOrValue).addVariable(variable);
                  }
                  else {
                     throw new SAXException("Can't add variable to parent.");
                  }
               }
               variablesValueStack.addFirst(variable);
            }
            else if (obj instanceof IValue) {
               IValue value = (IValue)obj;
               if (parentVariableOrValue instanceof SEDMemoryVariable) {
                  ((SEDMemoryVariable)parentVariableOrValue).setValue(value);
               }
               else {
                  throw new SAXException("Can't add value to parent.");
               }
               variablesValueStack.addFirst(value);
            }
            else if (obj instanceof SEDMemoryThread) {
               thread = (SEDMemoryThread)obj;
               if (target != null) {
                  target.addSymbolicThread(thread);
               }
               else {
                  throw new SAXException("Model is in inconsistent state.");
               }
            }
            else if (obj instanceof ISEDMemoryDebugNode) {
               ISEDMemoryDebugNode child = (ISEDMemoryDebugNode)obj; 
               parentStack.addFirst(child);
               if (isMethodReturnCondition(uri, localName, qName)) {
                  ((SEDMemoryMethodCall)parent).addMethodReturnCondition((ISEDBranchCondition)child);
               }
               else if (isGroupEndCondition(uri, localName, qName)) {
                  ((ISEDMemoryGroupable)parent).addGroupEndCondition((ISEDBranchCondition)child);
               }
               else {
                  if (parent != null) {
                     parent.addChild(child);
                  }
                  else if (thread != null) {
                     thread.addChild(child);
                  }
                  else {
                     throw new SAXException("Model is in inconsistent state.");
                  }
               }
            }
            else if (obj instanceof ISEDAnnotation) {
               ISEDAnnotation annotation = (ISEDAnnotation)obj;
               annotationIdMapping.put(annotation.getId(), annotation);
               target.registerAnnotation(annotation);
            }
            else if (obj instanceof ISEDAnnotationLink) {
               ISEDAnnotationLink link = (ISEDAnnotationLink)obj;
               link.getSource().addLink(link);
            }
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
         if (isCallStateVariable(uri, localName, qName) || 
             isVariable(uri, localName, qName) || 
             isValue(uri, localName, qName)) {
            variablesValueStack.removeFirst();
         }
         else if (isConstraint(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isRelevantConstraint(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isCallStackEntry(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isTerminationEntry(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isAnnotation(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isAnnotationLink(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isAnnotationLink(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isChildReferences(uri, localName, qName)) {
            // Nothing to do
         }
         else if (isGroupEndConditionReference(uri, localName, qName)) {
            // Nothing to do
         }
         else {
            if (!parentStack.isEmpty()) {
               parentStack.removeFirst();
            }
            else if (thread != null) {
               thread = null;
            }
            else if (target != null) {
               target = null;
            }
            else if (SEDXMLWriter.TAG_LAUNCH.equals(qName)) {
               // Nothing to do, but still valid.
            }
            else {
               throw new SAXException("Model is in inconsistent state.");
            }
         }
      }

      /**
       * Returns the found {@link ISEDDebugTarget}s.
       * @return The found {@link ISEDDebugTarget}s.
       */
      public List<ISEDDebugTarget> getResult() {
         return result;
      }

      /**
       * Returns the mapping of {@link ISEDDebugNode}s to their call stacks.
       * @return The mapping of {@link ISEDDebugNode}s to their call stacks.
       */
      public Map<ISEDMemoryDebugNode, List<String>> getCallStackEntriesMap() {
         return callStackEntriesMap;
      }

      /**
       * Returns the mapping of {@link SEDMemoryThread}s to their call stacks.
       * @return The mapping of {@link SEDMemoryThread}s to their call stacks.
       */
      public Map<SEDMemoryThread, List<String>> getTerminationEntriesMap() {
         return terminationEntriesMap;
      }

      /**
       * Returns the mapping of {@link SEDMemoryValue}s to their relevant constraints.
       * @return The mapping of {@link SEDMemoryValue}s to their relevant constraints.
       */
      public Map<SEDMemoryValue, List<String>> getRelevantConstraintsMap() {
         return relevantConstraintsMap;
      }

      /**
       * Returns the node child references.
       * @return The node child references.
       */
      public Map<ISEDMemoryDebugNode, List<ChildReference>> getNodeChildReferences() {
         return nodeChildReferences;
      }

      /**
       * Returns the group end references.
       * @return The group end references.
       */
      public Map<ISEDMemoryDebugNode, List<GroupEndReference>> getGroupEndReferences() {
         return groupEndReferences;
      }

      /**
       * Returns the method return conditions.
       * @return The method return conditions.
       */
      public Map<AbstractSEDBaseMethodReturn, String> getMethodReturnConditionReferences() {
         return methodReturnConditionReferences;
      }

      /**
       * Returns the instantiated {@link ISEDDebugElement} with the give ID.
       * @param id The ID.
       * @return The instantiated {@link ISEDDebugElement} or {@code null} if not available.
       */
      public ISEDDebugElement getElementById(String id) {
         return elementIdMapping.get(id);
      }
   }
   
   /**
    * Checks if the given tag name represents a constraint.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents a constraint, {@code false} represents something else.
    */
   protected boolean isConstraint(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_CONSTRAINT.equals(qName);
   }

   /**
    * Checks if the given tag name represents a relevant constraint.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents a constraint, {@code false} represents something else.
    */
   protected boolean isRelevantConstraint(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_RELEVANT_CONSTRAINT.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents a call stack entry.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents a call stack entry, {@code false} represents something else.
    */
   protected boolean isCallStackEntry(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_CALL_STACK_ENTRY.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents a termination entry.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents a termination entry, {@code false} represents something else.
    */
   protected boolean isTerminationEntry(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_TERMINATION_ENTRY.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link ISEDAnnotation}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link ISEDAnnotation}, {@code false} represents something else.
    */
   protected boolean isAnnotation(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_ANNOTATION.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link ISEDAnnotationLink}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link ISEDAnnotationLink}, {@code false} represents something else.
    */
   protected boolean isAnnotationLink(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_ANNOTATION_LINK.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents a {@link ChildReference}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link ChildReference}, {@code false} represents something else.
    */
   protected boolean isChildReferences(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_CHILD_REFERENCE.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link ISEDBranchCondition}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link ISEDBranchCondition}, {@code false} represents something else.
    */
   protected boolean isGroupEndCondition(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_GROUP_END_CONDITION.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link ISEDBranchCondition}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link ISEDBranchCondition}, {@code false} represents something else.
    */
   protected boolean isMethodReturnCondition(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_METHOD_RETURN_CONDITIONS.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents a {@link GroupEndReference}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents a {@link GroupEndReference}, {@code false} represents something else.
    */
   protected boolean isGroupEndConditionReference(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_GROUP_END_CONDITION_REFERENCE.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link IVariable}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link IVariable}, {@code false} represents something else.
    */
   protected boolean isCallStateVariable(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_CALL_STATE_VARIABLE.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link IVariable}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link IVariable}, {@code false} represents something else.
    */
   protected boolean isVariable(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_VARIABLE.equals(qName);
   }
   
   /**
    * Checks if the given tag name represents an {@link IValue}.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @return {@code true} represents an {@link IValue}, {@code false} represents something else.
    */
   protected boolean isValue(String uri, String localName, String qName) {
      return SEDXMLWriter.TAG_VALUE.equals(qName);
   }
   
   /**
    * Creates an {@link Object} for the element defined by the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param parentVariableOrValue The parent {@link ISEDVariable} / {@link ISEDValue} if available or {@code null} otherwise.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @param methodReturnConditionReferences The method return conditions.
    * @return The created {@link Object}.
    * @throws SAXException Occurred Exception.
    */
   protected Object createElement(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, IDebugElement parentVariableOrValue, String uri, String localName, String qName, Attributes attributes, Map<String, ISEDAnnotation> annotationIdMapping, Map<AbstractSEDBaseMethodReturn, String> methodReturnConditionReferences) throws SAXException {
      if (SEDXMLWriter.TAG_LAUNCH.equals(qName)) {
         return null; // Nothing to do
      }
      else if (SEDXMLWriter.TAG_CHILD_REFERENCE.equals(qName)) {
         return new ChildReference(getNodeIdRef(attributes));
      }
      else if (SEDXMLWriter.TAG_GROUP_END_CONDITION_REFERENCE.equals(qName)) {
         return new GroupEndReference(getNodeIdRef(attributes));
      }
      else if (SEDXMLWriter.TAG_DEBUG_TARGET.equals(qName)) {
         return createDebugTarget(uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_BRANCH_CONDITION.equals(qName) ||
               SEDXMLWriter.TAG_METHOD_RETURN_CONDITIONS.equals(qName) ||
               SEDXMLWriter.TAG_GROUP_END_CONDITION.equals(qName)) {
         return createBranchCondition(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_BRANCH_STATEMENT.equals(qName)) {
         return createBranchStatement(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_EXCEPTIONAL_TERMINATION.equals(qName)) {
         return createExceptionalTermination(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_LOOP_BODY_TERMINATION.equals(qName)) {
         return createLoopBodyTermination(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_LOOP_CONDITION.equals(qName)) {
         return createLoopCondition(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_LOOP_STATEMENT.equals(qName)) {
         return createLoopStatement(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_METHOD_CALL.equals(qName)) {
         return createMethodCall(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_METHOD_RETURN.equals(qName)) {
         return createMethodReturn(target, parent, thread, uri, localName, qName, attributes, methodReturnConditionReferences);
      }
      else if (SEDXMLWriter.TAG_EXCEPTIONAL_METHOD_RETURN.equals(qName)) {
         return createExceptionalMethodReturn(target, parent, thread, uri, localName, qName, attributes, methodReturnConditionReferences);
      }
      else if (SEDXMLWriter.TAG_STATEMENT.equals(qName)) {
         return createStatement(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_TERMINATION.equals(qName)) {
         return createTermination(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_THREAD.equals(qName)) {
         return createThread(target, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_VARIABLE.equals(qName)) {
         return createVariable(target, (IStackFrame)parent, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_VALUE.equals(qName)) {
         return createValue(target, (ISEDVariable)parentVariableOrValue, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_METHOD_CONTRACT.equals(qName)) {
         return createMethodContract(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_LOOP_INVARIANT.equals(qName)) {
         return createLoopInvariant(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_ANNOTATION.equals(qName)) {
         return createAnnotation(target, parent, thread, uri, localName, qName, attributes);
      }
      else if (SEDXMLWriter.TAG_ANNOTATION_LINK.equals(qName)) {
         return createAnnotationLink(target, parent, thread, uri, localName, qName, attributes, annotationIdMapping);
      }
      else if (SEDXMLWriter.TAG_CONSTRAINT.equals(qName)) {
         return createConstraint(target, parent, thread, uri, localName, qName, attributes);
      }
      else {
         throw new SAXException("Unknown tag \"" + qName + "\".");
      }
   }
   
   protected SEDMemoryConstraint createConstraint(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryConstraint constraint = new SEDMemoryConstraint(target, getName(attributes));
      constraint.setId(getId(attributes));
      return constraint;
   }

   protected ISEDAnnotationLink createAnnotationLink(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes, Map<String, ISEDAnnotation> annotationIdMapping) throws SAXException {
      String sourceId = getAnnotationLinkSource(attributes);
      String targetId = getAnnotationLinkTarget(attributes);
      if (!ObjectUtil.equals(targetId, parent.getId())) {
         throw new SAXException("Annotation link is contained in wrong node.");
      }
      ISEDAnnotation annotation = annotationIdMapping.get(sourceId);
      if (annotation == null) {
         throw new SAXException("Annotation with ID \"" + sourceId + "\" is not available.");
      }
      ISEDAnnotationLink link = annotation.getType().createLink(annotation, parent);
      link.setId(getId(attributes));
      String content = getAnnotationContent(attributes);
      if (content != null) {
         annotation.getType().restoreAnnotationLink(link, content);
      }
      return link;
   }
   
   protected ISEDAnnotation createAnnotation(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      String typeId = getTypeId(attributes);
      ISEDAnnotationType type = SEDAnnotationUtil.getAnnotationtype(typeId);
      if (type == null) {
         throw new SAXException("Annotation type with type ID \"" + typeId + "\" does not exit.");
      }
      ISEDAnnotation annotation = type.createAnnotation();
      annotation.setId(getId(attributes));
      annotation.setEnabled(isEnabled(attributes));
      boolean highlightBackground = isHighlightBackground(attributes);
      RGB backgroundColor = getBackgroundColor(attributes);
      boolean highlightForeground = isHighlightForeground(attributes);
      RGB foregroundColor = getForegroundColor(attributes);
      if (annotation.isHighlightBackground() != highlightBackground) {
         annotation.setCustomHighlightBackground(highlightBackground);
      }
      if (!ObjectUtil.equals(annotation.getBackgroundColor(), backgroundColor)) {
         annotation.setCustomBackgroundColor(backgroundColor);
      }
      if (annotation.isHighlightForeground() != highlightForeground) {
         annotation.setCustomHighlightForeground(highlightForeground);
      }
      if (!ObjectUtil.equals(annotation.getForegroundColor(), foregroundColor)) {
         annotation.setCustomForegroundColor(foregroundColor);
      }
      String content = getAnnotationContent(attributes);
      if (content != null) {
         type.restoreAnnotation(annotation, content);
      }
      return annotation;
   }

   protected SEDMemoryValue createValue(ISEDDebugTarget target, ISEDVariable parent, String uri, String localName, String qName, Attributes attributes) {
      SEDMemoryValue value = new SEDMemoryValue(target, parent);
      value.setId(getId(attributes));
      value.setAllocated(isAllocated(attributes));
      value.setReferenceTypeName(getReferenceTypeName(attributes));
      value.setValueString(getValueString(attributes));
      value.setMultiValued(isMultiValued(attributes));
      return value;
   }
   
   protected SEDMemoryVariable createVariable(ISEDDebugTarget target, IStackFrame stackFrame, String uri, String localName, String qName, Attributes attributes) {
      SEDMemoryVariable variable = new SEDMemoryVariable(target, stackFrame);
      variable.setId(getId(attributes));
      variable.setName(getName(attributes));
      variable.setReferenceTypeName(getReferenceTypeName(attributes));
      return variable;
   }
   
   /**
    * Creates a {@link SEDMemoryBranchCondition} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryBranchCondition}.
    */
   protected SEDMemoryBranchCondition createBranchCondition(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) {
      SEDMemoryBranchCondition branchCondition = new SEDMemoryBranchCondition(target, parent, thread);
      fillDebugNode(branchCondition, attributes);
      return branchCondition;
   }
   
   /**
    * Creates a {@link SEDMemoryDebugTarget} instance for the content in the given tag.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryDebugTarget}.
    */
   protected SEDMemoryDebugTarget createDebugTarget(String uri, String localName, String qName, Attributes attributes) {
      SEDMemoryDebugTarget target = new SEDMemoryDebugTarget(launch, executable);
      target.setId(getId(attributes));
      target.setName(getName(attributes));
      target.setModelIdentifier(getModelIdentifier(attributes));
      return target;
   }

   /**
    * Creates a {@link SEDMemoryBranchStatement} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryBranchStatement}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryBranchStatement createBranchStatement(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryBranchStatement branchStatement = new SEDMemoryBranchStatement(target, parent, thread);
      branchStatement.setSourcePath(getSourcePath(attributes));
      fillDebugNode(branchStatement, attributes);
      fillStackFrame(branchStatement, attributes);
      return branchStatement;
   }

   /**
    * Creates a {@link SEDMemoryExceptionalTermination} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryExceptionalTermination}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryExceptionalTermination createExceptionalTermination(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryExceptionalTermination termination = new SEDMemoryExceptionalTermination(target, parent, thread, isVerified(attributes));
      fillDebugNode(termination, attributes);
      fillStackFrame(termination, attributes);
      return termination;
   }
   
   /**
    * Creates a {@link SEDMemoryLoopBodyTermination} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryLoopBodyTermination}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryLoopBodyTermination createLoopBodyTermination(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryLoopBodyTermination termination = new SEDMemoryLoopBodyTermination(target, parent, thread, isVerified(attributes));
      fillDebugNode(termination, attributes);
      fillStackFrame(termination, attributes);
      return termination;
   }
   
   /**
    * Creates a {@link SEDMemoryLoopCondition} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryLoopCondition}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryLoopCondition createLoopCondition(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryLoopCondition loopCondition = new SEDMemoryLoopCondition(target, parent, thread);
      loopCondition.setSourcePath(getSourcePath(attributes));
      fillDebugNode(loopCondition, attributes);
      fillStackFrame(loopCondition, attributes);
      return loopCondition;
   }
   
   /**
    * Creates a {@link SEDMemoryLoopStatement} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryLoopStatement}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryLoopStatement createLoopStatement(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryLoopStatement loopStatement = new SEDMemoryLoopStatement(target, parent, thread);
      loopStatement.setSourcePath(getSourcePath(attributes));
      fillDebugNode(loopStatement, attributes);
      fillStackFrame(loopStatement, attributes);
      return loopStatement;
   }
   
   /**
    * Creates a {@link SEDMemoryMethodCall} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryMethodCall}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryMethodCall createMethodCall(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryMethodCall methodCall = new SEDMemoryMethodCall(target, parent, thread);
      methodCall.setSourcePath(getSourcePath(attributes));
      fillDebugNode(methodCall, attributes);
      fillStackFrame(methodCall, attributes);
      return methodCall;
   }
   
   /**
    * Creates a {@link SEDMemoryMethodReturn} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryMethodReturn}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryMethodReturn createMethodReturn(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes, Map<AbstractSEDBaseMethodReturn, String> methodReturnConditionReferences) throws SAXException {
      SEDMemoryMethodReturn methodReturn = new SEDMemoryMethodReturn(target, parent, thread);
      methodReturn.setSourcePath(getSourcePath(attributes));
      fillDebugNode(methodReturn, attributes);
      fillStackFrame(methodReturn, attributes);
      String methodReturnCondition = getMethodReturnCondition(attributes);
      if (!StringUtil.isEmpty(methodReturnCondition)) {
         methodReturnConditionReferences.put(methodReturn, methodReturnCondition);
      }
      return methodReturn;
   }
   
   /**
    * Creates a {@link SEDMemoryExceptionalMethodReturn} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryExceptionalMethodReturn}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryExceptionalMethodReturn createExceptionalMethodReturn(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes, Map<AbstractSEDBaseMethodReturn, String> methodReturnConditionReferences) throws SAXException {
      SEDMemoryExceptionalMethodReturn methodReturn = new SEDMemoryExceptionalMethodReturn(target, parent, thread);
      methodReturn.setSourcePath(getSourcePath(attributes));
      fillDebugNode(methodReturn, attributes);
      fillStackFrame(methodReturn, attributes);
      String methodReturnCondition = getMethodReturnCondition(attributes);
      if (!StringUtil.isEmpty(methodReturnCondition)) {
         methodReturnConditionReferences.put(methodReturn, methodReturnCondition);
      }
      return methodReturn;
   }

   /**
    * Creates a {@link SEDMemoryStatement} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryStatement}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryStatement createStatement(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryStatement statement = new SEDMemoryStatement(target, parent, thread);
      statement.setSourcePath(getSourcePath(attributes));
      fillDebugNode(statement, attributes);
      fillStackFrame(statement, attributes);
      return statement;
   }
   
   /**
    * Creates a {@link SEDMemoryMethodContract} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryStatement}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryMethodContract createMethodContract(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryMethodContract methodContract = new SEDMemoryMethodContract(target, parent, thread);
      methodContract.setSourcePath(getSourcePath(attributes));
      fillDebugNode(methodContract, attributes);
      fillStackFrame(methodContract, attributes);
      methodContract.setPreconditionComplied(isPreconditionComplied(attributes));
      methodContract.setHasNotNullCheck(hasNotNullCheck(attributes));
      methodContract.setNotNullCheckComplied(isNotNullCheckComplied(attributes));
      return methodContract;
   }
   
   /**
    * Creates a {@link SEDMemoryLoopInvariant} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryStatement}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryLoopInvariant createLoopInvariant(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryLoopInvariant loopInvariant = new SEDMemoryLoopInvariant(target, parent, thread);
      loopInvariant.setSourcePath(getSourcePath(attributes));
      fillDebugNode(loopInvariant, attributes);
      fillStackFrame(loopInvariant, attributes);
      loopInvariant.setInitiallyValid(isInitiallyValid(attributes));
      return loopInvariant;
   }
   
   /**
    * Creates a {@link SEDMemoryTermination} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param parent The parent {@link ISEDDebugNode} or {@code null} if not available.
    * @param thread The parent {@link ISEDThread} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryTermination}.
    * @throws SAXException Occurred Exception.
    */   
   protected SEDMemoryTermination createTermination(ISEDDebugTarget target, ISEDDebugNode parent, ISEDThread thread, String uri, String localName, String qName, Attributes attributes) throws SAXException {
      SEDMemoryTermination termination = new SEDMemoryTermination(target, parent, thread, isVerified(attributes));
      fillDebugNode(termination, attributes);
      fillStackFrame(termination, attributes);
      return termination;
   }
   
   /**
    * Creates a {@link SEDMemoryThread} instance for the content in the given tag.
    * @param target The parent {@link ISEDDebugTarget} or {@code null} if not available.
    * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
    * @param localName  The local name (without prefix), or the empty string if Namespace processing is not being performed.
    * @param qName The qualified name (with prefix), or the empty string if qualified names are not available.
    * @param attributes The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
    * @return The created {@link SEDMemoryThread}.
    */   
   protected SEDMemoryThread createThread(ISEDDebugTarget target, String uri, String localName, String qName, Attributes attributes) {
      SEDMemoryThread thread = new SEDMemoryThread(target, executable);
      fillDebugNode(thread, attributes);
      return thread;
   }
   
   /**
    * Fills the attributes of the given {@link ISEDMemoryDebugNode}.
    * @param node The {@link ISEDMemoryDebugNode} to fill.
    * @param attributes The {@link Attributes} which provides the content.
    */
   protected void fillDebugNode(ISEDMemoryDebugNode node, Attributes attributes) {
      node.setId(getId(attributes));
      node.setName(getName(attributes));
      node.setPathCondition(getPathCondition(attributes));
      if (node instanceof ISEDMemoryGroupable) {
         ((ISEDMemoryGroupable) node).setGroupable(isGroupable(attributes));
      }
   }

   /**
    * Fills the attributes of the given {@link ISEDMemoryStackFrameCompatibleDebugNode}.
    * @param node The {@link ISEDMemoryStackFrameCompatibleDebugNode} to fill.
    * @param attributes The {@link Attributes} which provides the content.
    * @throws SAXException Occurred Exception.
    */
   protected void fillStackFrame(ISEDMemoryStackFrameCompatibleDebugNode node, Attributes attributes) throws SAXException {
      node.setLineNumber(getLineNumber(attributes));
      node.setCharStart(getCharStart(attributes));
      node.setCharEnd(getCharEnd(attributes));
   }

   /**
    * Returns the ID value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */   
   protected String getId(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_ID);
   }
   
   /**
    * Returns the node id reference value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getNodeIdRef(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_NODE_ID_REF);
   }
   
   /**
    * Returns the constraint id reference value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getConstraintIdRef(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_CONSTRAINT_ID_REF);
   }
   
   /**
    * Returns the name value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getName(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_NAME);
   }

   /**
    * Returns the path condition value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getPathCondition(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_PATH_CONDITION);
   }
   
   /**
    * Returns the source path value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getSourcePath(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_SOURCE_PATH);
   }
   
   /**
    * Returns the method return condition ID.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getMethodReturnCondition(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_METHOD_RETURN_CONDITION);
   }
   
   /**
    * Returns the model identifier value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getModelIdentifier(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_MODEL_IDENTIFIER);
   }
   
   /**
    * Returns the line number value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    * @throws SAXException Occurred Exception.
    */
   protected int getLineNumber(Attributes attributes) throws SAXException {
      try {
         String value = attributes.getValue(SEDXMLWriter.ATTRIBUTE_LINE_NUMBER);
         if (value != null) {
            return Integer.parseInt(value);
         }
         else {
            return -1;
         }
      }
      catch (NumberFormatException e) {
         throw new SAXException(e);
      }
   }
   
   /**
    * Returns the char start value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    * @throws SAXException Occurred Exception.
    */
   protected int getCharStart(Attributes attributes) throws SAXException {
      try {
         String value = attributes.getValue(SEDXMLWriter.ATTRIBUTE_CHAR_START);
         if (value != null) {
            return Integer.parseInt(value);
         }
         else {
            return -1;
         }
      }
      catch (NumberFormatException e) {
         throw new SAXException(e);
      }
   }
   
   /**
    * Returns the char end value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    * @throws SAXException Occurred Exception.
    */
   protected int getCharEnd(Attributes attributes) throws SAXException {
      try {
         String value = attributes.getValue(SEDXMLWriter.ATTRIBUTE_CHAR_END);
         if (value != null) {
            return Integer.parseInt(value);
         }
         else {
            return -1;
         }
      }
      catch (NumberFormatException e) {
         throw new SAXException(e);
      }
   }
   
   /**
    * Returns the multi valued value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isMultiValued(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_MULTI_VALUED));
   }
   
   /**
    * Returns the allocated value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isAllocated(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_ALLOCATED));
   }
   
   /**
    * Returns the verified value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isVerified(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_VERIFIED));
   }
   
   /**
    * Returns the not null check complied value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isNotNullCheckComplied(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_NOT_NULL_CHECK_COMPLIED));
   }
   
   /**
    * Returns the has not null check value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean hasNotNullCheck(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_HAS_NOT_NULL_CHECK));
   }
   
   /**
    * Returns the precondition complied value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isPreconditionComplied(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_PRECONDITION_COMPLIED));
   }
   
   /**
    * Returns the initially valid value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isInitiallyValid(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_INITIALLY_VALID));
   }
   
   /**
    * Returns the groupable value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isGroupable(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_GROUPABLE));
   }
   
   /**
    * Returns the value string value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getValueString(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_VALUE_STRING);
   }
   
   /**
    * Returns the reference type name value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getReferenceTypeName(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_REFERENCE_TYPE_NAME);
   }
   
   /**
    * Returns the type ID value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getTypeId(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_TYPE_ID);
   }
   
   /**
    * Returns the annotation content value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getAnnotationContent(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_CONTENT);
   }
   
   /**
    * Returns the enabled value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isEnabled(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_ENABLED));
   }
   
   /**
    * Returns the highlight background value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isHighlightBackground(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_HIGHLIGHT_BACKGROUND));
   }
   
   /**
    * Returns the highlight foreground value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected boolean isHighlightForeground(Attributes attributes) {
      return Boolean.parseBoolean(attributes.getValue(SEDXMLWriter.ATTRIBUTE_HIGHLIGHT_FOREGROUND));
   }
   
   /**
    * Returns the background color value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected RGB getBackgroundColor(Attributes attributes) {
      return StringConverter.asRGB(attributes.getValue(SEDXMLWriter.ATTRIBUTE_BACKGROUND_COLOR));
   }
   
   /**
    * Returns the foreground color value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected RGB getForegroundColor(Attributes attributes) {
      return StringConverter.asRGB(attributes.getValue(SEDXMLWriter.ATTRIBUTE_FOREGROUND_COLOR));
   }
   
   /**
    * Returns the annotation link source value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getAnnotationLinkSource(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_ANNOTATION_LINK_SOURCE);
   }
   
   /**
    * Returns the annotation link target value.
    * @param attributes The {@link Attributes} which provides the content.
    * @return The value.
    */
   protected String getAnnotationLinkTarget(Attributes attributes) {
      return attributes.getValue(SEDXMLWriter.ATTRIBUTE_ANNOTATION_LINK_TARGET);
   }

   /**
    * Represents temporary a child reference.
    * @author Martin Hentschel
    */
   protected static class ChildReference {
      /**
       * The target ID.
       */
      private final String id;

      /**
       * Constructor.
       * @param id The target ID.
       */
      public ChildReference(String id) {
         this.id = id;
      }

      /**
       * Returns the target ID.
       * @return The target ID.
       */
      public String getId() {
         return id;
      }
   }

   /**
    * Represents temporary a group end reference.
    * @author Martin Hentschel
    */
   protected static class GroupEndReference {
      /**
       * The target ID.
       */
      private final String id;

      /**
       * Constructor.
       * @param id The target ID.
       */
      public GroupEndReference(String id) {
         this.id = id;
      }

      /**
       * Returns the target ID.
       * @return The target ID.
       */
      public String getId() {
         return id;
      }
   }
}