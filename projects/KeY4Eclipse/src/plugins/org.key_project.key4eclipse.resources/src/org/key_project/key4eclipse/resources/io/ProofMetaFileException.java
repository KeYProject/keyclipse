package org.key_project.key4eclipse.resources.io;

/**
 * Exception for invalid metaFiles.
 * @author Stefan K�sdorf
 */
@SuppressWarnings("serial")
public class ProofMetaFileException extends Exception{
   
   /**
    * The Constructor
    */
   public ProofMetaFileException(String message){
      super(message);
   }
}
