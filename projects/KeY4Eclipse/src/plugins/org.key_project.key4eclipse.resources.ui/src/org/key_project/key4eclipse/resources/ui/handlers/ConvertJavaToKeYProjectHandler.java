package org.key_project.key4eclipse.resources.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.key_project.key4eclipse.common.ui.handler.AbstractSaveExecutionHandler;
import org.key_project.key4eclipse.resources.nature.KeYProjectNature;
import org.key_project.util.eclipse.swt.SWTUtil;
import org.key_project.util.java.ArrayUtil;

@SuppressWarnings("restriction")
public class ConvertJavaToKeYProjectHandler extends AbstractSaveExecutionHandler {

   
   /**
    * {@inheritDoc}
    */
   @Override
   protected Object doExecute(ExecutionEvent event) throws Exception {
      ISelection selection = HandlerUtil.getCurrentSelection(event);
      Object[] elements = SWTUtil.toArray(selection);
      for(Object obj : elements){
         if (obj instanceof JavaProject){
            obj = ((JavaProject) obj).getProject();
         }
         if (obj instanceof IProject){
            IProject project = (IProject) obj;
            IProjectDescription description = project.getDescription();
            String[] newNatures = ArrayUtil.add(description.getNatureIds(), KeYProjectNature.NATURE_ID);
            description.setNatureIds(newNatures);
            project.setDescription(description, null);            
         }
      }
      return null;
   }


}
