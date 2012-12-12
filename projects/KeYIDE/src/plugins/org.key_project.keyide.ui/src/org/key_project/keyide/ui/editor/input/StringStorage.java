package org.key_project.keyide.ui.editor.input;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;


/**
 * This class is used to provide non-file input to the editor.
 * 
 * @author Christoph Schneider, Niklas Bunzel, Stefan K�sdorf, Marco Drebing
 */
//TODO: Rename class, maybe into ProofStorage
public class StringStorage implements IStorage {
   
   private String proofString;
   private String name;
   
   
   /**
    * Constructor.
    * @param input The textbody of this storage
    * @param name The name of this {@link IStorage}.
    */
   public StringStorage(String input, String name){
      this.proofString=input;
      this.name=name;
   }

   /** 
    * {@inheritDoc}
    */
   @Override
   public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
      return null;
   }

   /** 
    * {@inheritDoc}
    */
   @Override
   public InputStream getContents() throws CoreException {
      return new ByteArrayInputStream(proofString.getBytes());
   }

   /** 
    * {@inheritDoc}
    */
   @Override
   public IPath getFullPath() {
      return null;
   }

   /** 
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return name;
   }

   /** 
    * {@inheritDoc}
    */
   @Override
   public boolean isReadOnly() {
      return true;
   }

}
