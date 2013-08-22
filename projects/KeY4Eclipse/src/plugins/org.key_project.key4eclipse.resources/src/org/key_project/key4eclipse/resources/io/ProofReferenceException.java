package org.key_project.key4eclipse.resources.io;

/**
 * Exception for invalid metaFiles.
 * @author Stefan K�sdorf
 */
@SuppressWarnings("serial")
public class ProofReferenceException extends Exception{
   
   /**
    * The Constructor
    */
   public ProofReferenceException(String message){
      super(message);
   }
}
