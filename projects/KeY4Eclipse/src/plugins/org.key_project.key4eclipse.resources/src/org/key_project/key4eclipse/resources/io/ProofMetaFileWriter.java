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

package org.key_project.key4eclipse.resources.io;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.key_project.key4eclipse.common.ui.util.LogUtil;
import org.key_project.key4eclipse.resources.builder.ProofElement;
import org.key_project.key4eclipse.resources.util.KeYResourcesUtil;
import org.key_project.util.eclipse.ResourceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.IProgramVariable;
import de.uka.ilkd.key.proof_references.reference.IProofReference;
import de.uka.ilkd.key.speclang.ClassAxiom;
import de.uka.ilkd.key.speclang.ClassInvariant;
import de.uka.ilkd.key.speclang.Contract;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;
import de.uka.ilkd.key.ui.CustomUserInterface;

/**
 * Writer for the meta files.
 * @author Stefan K�sdorf
 */
public class ProofMetaFileWriter {
   
   /**
    * {@link LinkedHashSet} with the full names of all types already added to the meta file.
    */
   private LinkedHashSet<String> addedTypes;
   private ProofElement pe;
   private Document doc;
   
   
   public ProofMetaFileWriter(ProofElement pe){
      this.pe = pe;
      this.addedTypes = new LinkedHashSet<String>();
      this.doc = null;
   }
   
   /**
    * Creates the meta file for the given {@link ProofElement}.
    * @param pe - the {@link ProofElement} to use
    * @throws ProofMetaFileException
    */
   public void writeMetaFile() {
      try{
         IFile metaIFile = pe.getMetaFile();
         this.addedTypes = new LinkedHashSet<String>();
         createDoument();
   
         TransformerFactory transFactory = TransformerFactory.newInstance();
         Transformer transformer = transFactory.newTransformer();
         DOMSource source = new DOMSource(doc);
         if(!metaIFile.exists()){
            metaIFile.create(null, true, null);
         }
         else{
            metaIFile.refreshLocal(IResource.DEPTH_ZERO, null);
            ResourceAttributes resAttr = metaIFile.getResourceAttributes();
            resAttr.setReadOnly(false);
            metaIFile.setResourceAttributes(resAttr);
         }
         File metaFile = metaIFile.getLocation().toFile();
         StreamResult result = new StreamResult(metaFile);
         transformer.transform(source, result);
         metaIFile.refreshLocal(IResource.DEPTH_ZERO, null);
         ResourceAttributes resAttr = metaIFile.getResourceAttributes();
         resAttr.setReadOnly(true);
         metaIFile.setResourceAttributes(resAttr);
      } catch (Exception e){
         LogUtil.getLogger().logError(e);
      }
   }
   
   
   /**
    * Creates the {@link Document} for the meta file of the given {@link ProofElement}.
    * @param pe - the {@link ProofElement} to use
    * @return the created {@link Document}
    * @throws ParserConfigurationException
    * @throws ProofReferenceException 
    * @throws CoreException 
    * @throws IOException 
    */
   private void createDoument() throws Exception{
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      
      doc = docBuilder.newDocument();
      
      Element rootElement = doc.createElement("proofMetaFile");
      doc.appendChild(rootElement);
      
      Element proofFileMD5 = doc.createElement("proofFileMD5");
      String md5 = ResourceUtil.computeContentMD5(pe.getProofFile());
      proofFileMD5.setAttribute("md5", md5);
      rootElement.appendChild(proofFileMD5);
      
      Element proofStatus = doc.createElement("proofStatus");
      String status = String.valueOf(pe.getProofClosed());
      proofStatus.setAttribute("proofClosed", status);
      rootElement.appendChild(proofStatus);
      
      Element markerMessage = doc.createElement("markerMessage");
      markerMessage.setAttribute("message", pe.getMarkerMsg());
      rootElement.appendChild(markerMessage);
      
      Element usedTypes = createUsedTypes();
      rootElement.appendChild(usedTypes);
      
      Element usedContracts = createUsedContracts();
      rootElement.appendChild(usedContracts);
   }
   
   private Element createUsedTypes() throws ProofReferenceException{
      Element usedTypes = doc.createElement("usedTypes");
      HashSet<IProofReference<?>> proofReferences = pe.getProofReferences();
      for(IProofReference<?> proofRef : proofReferences){
         KeYJavaType kjt = getKeYJavaType(proofRef);
         if(!KeYResourcesUtil.filterKeYJavaType(kjt) && !addedTypes.contains(kjt.getFullName())){
            Element typElement = createTypeElement(usedTypes, getKeYJavaTypeFromEnv(kjt, pe.getKeYEnvironment()));
            usedTypes.appendChild(typElement);
         }
      }
      return usedTypes;
   }
   
   
   private Element createTypeElement(Element usedTypes,KeYJavaType kjt){
      addedTypes.add(kjt.getFullName());
      Element typeElement = doc.createElement("type");
      typeElement.setAttribute("name", kjt.getFullName());
      ImmutableList<KeYJavaType> subTypes = pe.getKeYEnvironment().getServices().getJavaInfo().getAllSubtypes(kjt);
      for(KeYJavaType subType : subTypes){
         Element subTypeElement = doc.createElement("subType");
         subTypeElement.setAttribute("name", subType.getFullName());
         typeElement.appendChild(subTypeElement);
      }
      return typeElement;
   }
   
   
   /**
    * Returns the equivalent {@link KeYJavaType} from the given {@link KeYEnvironment} for the given {@link KeYJavaType}.
    * @param kjt - the {@link KeYJavaType} to use
    * @param environment - the {@link KeYEnvironment} to use
    * @return the {@link KeYJavaType} form the {@link KeYEnvironment}
    */
   private KeYJavaType getKeYJavaTypeFromEnv(KeYJavaType kjt, KeYEnvironment<CustomUserInterface> environment){
      Set<KeYJavaType> envKjts = environment.getJavaInfo().getAllKeYJavaTypes();
      for(KeYJavaType envKjt : envKjts){
         if(envKjt.getFullName().equals(kjt.getFullName())){
            return envKjt;
         }
      }
      return null;
   }
   
   
   /**
    * Returns the {@link KeYJavaType} for the given {@link IProofReference}.
    * @param proofRef - the {@link IProofReference} to use
    * @return the {@link KeYJavaType}
    * @throws ProofReferenceException 
    */
   private KeYJavaType getKeYJavaType(IProofReference<?> proofRef) throws ProofReferenceException{
      KeYJavaType kjt = null;
      Object target = proofRef.getTarget();
      if(IProofReference.ACCESS.equals(proofRef.getKind())){
         if(target instanceof IProgramVariable){
            IProgramVariable progVar = (IProgramVariable) target;
            kjt = progVar.getKeYJavaType();
         }
         else {
            throw new ProofReferenceException("Wrong target type " + target.getClass() + " found. Expected IProgramVariable");
         }
      }
      else if(IProofReference.CALL_METHOD.equals(proofRef.getKind()) || 
            IProofReference.INLINE_METHOD.equals(proofRef.getKind())){
         if(target instanceof IProgramMethod){
            IProgramMethod progMeth = (IProgramMethod) target;
            kjt = progMeth.getContainerType();
         }
         else {
            throw new ProofReferenceException("Wrong target type " + target.getClass() + " found. Expected IProgramMethod");
         }
      }
      else if(IProofReference.USE_AXIOM.equals(proofRef.getKind())){
         if(target instanceof ClassAxiom){
            ClassAxiom classAx = (ClassAxiom) target;
            kjt = classAx.getKJT();
         }
         else {
            throw new ProofReferenceException("Wrong target type " + target.getClass() + " found. Expected ClassAxiom");
         }
      }
      else if(IProofReference.USE_CONTRACT.equals(proofRef.getKind())){
         if(target instanceof Contract){
            Contract contract = (Contract) target;
            kjt = contract.getKJT();
         }
         else {
            throw new ProofReferenceException("Wrong target type " + target.getClass() + " found. Expected Contract");
         }
      }
      else if(IProofReference.USE_INVARIANT.equals(proofRef.getKind())){
         if(target instanceof ClassInvariant){
            ClassInvariant classInv = (ClassInvariant) target;
            kjt = classInv.getKJT();
         }
         else {
            throw new ProofReferenceException("Wrong target type " + target.getClass() + " found. Expected ClassInvariant");
         }
      }
      else {
         throw new ProofReferenceException("Unknow proof reference kind found: " + proofRef.getKind());
      }
      return kjt;
   }
   
   
   private Element createUsedContracts() throws ProofReferenceException{
      Element usedContractsElement = doc.createElement("usedContracts");
      List<ProofElement> usedContractsProofElements = pe.getUsedContracts();
      for(ProofElement usedContractProofElement : usedContractsProofElements){
         Element usedContractElement = doc.createElement("usedContract");
         usedContractElement.setAttribute("proofFile", usedContractProofElement.getProofFile().getFullPath().toString());
         usedContractsElement.appendChild(usedContractElement);
      }
      return usedContractsElement;
   }
}