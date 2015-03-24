package org.key_project.jmlediting.core.profile.syntax;

import java.util.List;

import org.key_project.jmlediting.core.dom.IASTNode;
import org.key_project.jmlediting.core.utilities.CommentRange;
import org.key_project.jmlediting.core.utilities.JMLValidationError;
import org.key_project.jmlediting.core.validation.IJMLValidationContext;

public interface IJMLValidator {
   /**
    * Method for checking if a given JMLComments Specifications (Represented by
    * a node) are valid.
    *
    * @param context
    *           the context to validate in
    * @param node
    *           the node to validate
    *
    * @return a List of IMarkers if some Specifications are invalid or an empty
    *         list if they are all valid
    */
   List<JMLValidationError> validate(CommentRange c,
         IJMLValidationContext context, IASTNode node);
}
