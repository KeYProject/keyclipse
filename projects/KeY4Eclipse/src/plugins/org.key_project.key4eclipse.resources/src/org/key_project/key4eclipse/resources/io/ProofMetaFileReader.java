package org.key_project.key4eclipse.resources.io;

import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reader for the meta files.
 * @author Stefan K�sdorf
 */
public class ProofMetaFileReader {
   
//   private Element rootElement;
   private String proofFileMD5;
   private boolean proofClosed;
   private String markerMessage;
   private LinkedList<ProofMetaFileTypeElement> typeElemens = new LinkedList<ProofMetaFileTypeElement>();
   private LinkedList<IFile> usedContracts = new LinkedList<IFile>();
   
   /**
    * The Constructor that automatically reads the given meta{@link IFile} and Provides the content.
    * @param metaIFile
    * @throws ParserConfigurationException 
    * @throws Exception
    */
   public ProofMetaFileReader(IFile metaIFile) throws Exception{ //No there are more --> Change Exception to ProofMetaFileContentException. Other exceptions are never thrown. It is legal for me to throw SAXException 
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = docFactory.newDocumentBuilder();
      try{
         Document doc = dBuilder.parse(metaIFile.getContents());
         
         checkMetaFileFormat(doc);
         
         Element rootElement = doc.getDocumentElement();
         this.proofFileMD5 = readMD5(rootElement);
         this.markerMessage = readMarkerMessage(rootElement);
         this.proofClosed = readProofStatus(rootElement);
         this.typeElemens = readAllTypeElements(rootElement);
         this.usedContracts = readUsedContracts(rootElement);
         
      } catch (SAXException e){
         throw new ProofMetaFileException("Invalid XML File");
      }
   }
   
   
   /**
    * Returns the read MD5 Sum.
    * @return - the MD5 Sum
    */
   public String getProofFileMD5() {
      return proofFileMD5;
   }
   
   
   public boolean getProofClosed(){
      return proofClosed;
   }
   
   public String getMarkerMessage(){
      return markerMessage;
   }


   /**
    * Return the {@link LinkedList} with all {@link ProofMetaFileTypeElement}s.
    * @return the {@link ProofMetaFileTypeElement}s
    */
   public LinkedList<ProofMetaFileTypeElement> getTypeElements() {
      return typeElemens;
   }
   
   
   public LinkedList<IFile> getUsedContracts(){
      return usedContracts;
   }
   
   private void checkMetaFileFormat(Document doc) throws ProofMetaFileException{
      NodeList documentNodes = doc.getChildNodes();
      Element rootElement = null;;
      if(documentNodes.getLength() == 1){
         rootElement = doc.getDocumentElement();
      }
      else{
         throw new ProofMetaFileException("No root found in this file");
      }
      //check if rootElement has 5 childeren
      if("proofMetaFile".equals(rootElement.getTagName())){
         NodeList rootNodeList = rootElement.getChildNodes();
         if(rootNodeList.getLength() == 5){
            //check md5Format
            Node md5Node = rootNodeList.item(0);
            if(!"proofFileMD5".equals(md5Node.getNodeName())){
               throw new ProofMetaFileException("Missing root entries. Found " + md5Node.getNodeName() + " | Expected proofFileMD5");
            }
            Node proofStatusNode = rootNodeList.item(1);
            if(!"proofStatus".equals(proofStatusNode.getNodeName())){
               throw new ProofMetaFileException("Missing root entries. Found " + proofStatusNode.getNodeName() + " | Expected proofStatus");
            }
            Node markerMsgNode = rootNodeList.item(2);
            if(!"markerMessage".equals(markerMsgNode.getNodeName())){
               throw new ProofMetaFileException("Missing root entries. Found " + markerMsgNode.getNodeName() + " | Expected markerMessage");
            }
            //check usedTypes format
            Node usedTypes = rootNodeList.item(3);
            if("usedTypes".equals(usedTypes.getNodeName())){
               //check type format
               NodeList usedTypesNodeList = usedTypes.getChildNodes();
               for(int i = 0; i < usedTypesNodeList.getLength(); i++){
                  Node type = usedTypesNodeList.item(i);
                  if("type".equals(type.getNodeName())){
                     //check subType format
                     NodeList typeNodeList = type.getChildNodes();
                     for(int j = 0; j < typeNodeList.getLength(); j++){
                        Node subType = typeNodeList.item(j);
                        if(!"subType".equals(subType.getNodeName())){
                           throw new ProofMetaFileException("Invalid subType entry. Found " + subType.getNodeName() + " | Expected subType");
                        }
                     }
                  }
                  else{
                     throw new ProofMetaFileException("Invalid type entry. Found " + type.getNodeName() + " | Expected type");
                  }
               }
            }
            else{
               throw new ProofMetaFileException("Missing root entries. Found " + rootNodeList.item(1).getNodeName() + " | Expected usedTypes");
            }
            //check usedContracts format
            Node usedContracts = rootNodeList.item(4);
            if("usedContracts".equals(usedContracts.getNodeName())){
               NodeList usedContractsNodeList = usedContracts.getChildNodes();
               for(int i = 0; i < usedContractsNodeList.getLength(); i++){
                  Node usedContract = usedContractsNodeList.item(i);
                  if(!"usedContract".equals(usedContract.getNodeName())){
                     throw new ProofMetaFileException("Invalid usedContract entry. Found " + usedContract.getNodeName() + " | Expected usedContract");
                  }
               }
            }
            else{
               throw new ProofMetaFileException("Missing root entries. Found " + usedContracts.getNodeName() + " | Expected usedContracts");
            }
         }
         else{
            throw new ProofMetaFileException("Missing root entries");
         }
      }
   }


   /**
    * Reads the MD5 Sum form the metaFile.
    * @return - the MD5 Sum
    * @throws ProofMetaFileException
    */
   private String readMD5(Element rootElement) throws ProofMetaFileException{
      NodeList nodeList = rootElement.getChildNodes();
      Node node = nodeList.item(0);
      NamedNodeMap attrMap = node.getAttributes();
      if(attrMap.getLength() == 1){
         Node attrNode = attrMap.item(0);
         if("md5".equals(attrNode.getNodeName())){
            String md5 = attrNode.getNodeValue();
            return md5;
         }
         else{
            throw new ProofMetaFileException("No md5 attribute found for proofFileMD5");
         }
      }
      else{
         throw new ProofMetaFileException("To many attributes for proofFileMD5");
      }
   }
   
   
   private boolean readProofStatus(Element rootElement) throws ProofMetaFileException{
      NodeList nodeList = rootElement.getChildNodes();
      Node node = nodeList.item(1);
      NamedNodeMap attrMap = node.getAttributes();
      if(attrMap.getLength() == 1){
         Node attrNode = attrMap.item(0);
         if("proofClosed".equals(attrNode.getNodeName())){
            boolean status = Boolean.parseBoolean(attrNode.getNodeValue());
            return status;
         }
         else{
            throw new ProofMetaFileException("No proofClosed attribute found for proofStatus");
         }
      }
      else{
         throw new ProofMetaFileException("To many attributes for proofStatus");
      }
   }
   
   
   private String readMarkerMessage(Element rootElement) throws ProofMetaFileException{
      NodeList nodeList = rootElement.getChildNodes();
      Node node = nodeList.item(2);
      NamedNodeMap attrMap = node.getAttributes();
      if(attrMap.getLength() == 1){
         Node attrNode = attrMap.item(0);
         if("message".equals(attrNode.getNodeName())){
            String message = attrNode.getNodeValue();
            return message;
         }
         else{
            throw new ProofMetaFileException("No message attribute found for markerMessage");
         }
      }
      else{
         throw new ProofMetaFileException("To many attributes for markerMessage");
      }
   }
   
   
   /**
    * Reads all types stored in the meta file.
    * @return a {@link LinkedList} with all read types
    * @throws ProofMetaFileException
    */
   private LinkedList<ProofMetaFileTypeElement> readAllTypeElements(Element rootElement) throws ProofMetaFileException{
      LinkedList<ProofMetaFileTypeElement> typeElements = new LinkedList<ProofMetaFileTypeElement>();
      NodeList rootNodeList = rootElement.getChildNodes();
      Node usedTypes = rootNodeList.item(3);
      NodeList usedTypesNodeList = usedTypes.getChildNodes();
      for(int i = 0; i < usedTypesNodeList.getLength(); i++){
         Node type = usedTypesNodeList.item(i);
         NamedNodeMap attrMap = type.getAttributes();
         if(attrMap.getLength() == 1){
            Node attrNode = attrMap.item(0);
            if("name".equals(attrNode.getNodeName())){
               String name = attrNode.getNodeValue();
               typeElements.add(new ProofMetaFileTypeElement(name, readAllSubTypes(type)));
            }
            else{
               throw new ProofMetaFileException("No type attribute found for this type");
            }
         }
         else{
            throw new ProofMetaFileException("To many attributes for this type");
         }
      }
      return typeElements;
   }
   
   
   /**
    * Reads all subTypes for the given {@link Node}.
    * @param type - the {@link Node} to use
    * @return - a {@link LinkedList} with all subTypes
    * @throws ProofMetaFileException
    */
   private LinkedList<String> readAllSubTypes(Node type) throws ProofMetaFileException{
      LinkedList<String> subTypeList = new LinkedList<String>();
      NodeList nodeList = type.getChildNodes();
      for(int i = 0; i < nodeList.getLength(); i++){
         Node subType = nodeList.item(i);
         NamedNodeMap attrMap = subType.getAttributes();
         if(attrMap.getLength() == 1){
            Node attrNode = attrMap.item(0);
            if("name".equals(attrNode.getNodeName())){
               String name = attrNode.getNodeValue();
               subTypeList.add(name);
            }
            else{
               throw new ProofMetaFileException("No type attribute found for this subtype");
            }
         }
         else{
            throw new ProofMetaFileException("To many attributes for this subtype");
         }
      }
      return subTypeList;
   }
   
   private LinkedList<IFile> readUsedContracts(Element rootElement) throws ProofMetaFileException{
      LinkedList<IFile> usedContracts = new LinkedList<IFile>();
      NodeList rootNodeList = rootElement.getChildNodes();
      Node usedContractsNode = rootNodeList.item(4);
      NodeList usedContractsNodeList = usedContractsNode.getChildNodes();
      for(int i = 0; i < usedContractsNodeList.getLength(); i++){
         Node node = usedContractsNodeList.item(i);
         NamedNodeMap attrMap = node.getAttributes();
         if(attrMap.getLength() == 1){
            Node attrNode = attrMap.item(0);
            if("proofFile".equals(attrNode.getNodeName())){
               String pathString = attrNode.getNodeValue();
               IPath proofFilePath = new Path(pathString);
               IFile proofFile = ResourcesPlugin.getWorkspace().getRoot().getFile(proofFilePath);
               usedContracts.add(proofFile);
            }
            else{
               throw new ProofMetaFileException("No type attribute found for this usedContract");
            }
         }
         else{
            throw new ProofMetaFileException("To many attributes for this usedContract");
         }
      }
      return usedContracts;
   }
}