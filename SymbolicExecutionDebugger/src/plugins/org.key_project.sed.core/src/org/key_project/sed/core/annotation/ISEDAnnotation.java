package org.key_project.sed.core.annotation;

import java.util.Set;

import org.key_project.sed.core.annotation.event.ISEDAnnotationLinkListener;
import org.key_project.sed.core.annotation.impl.AbstractSEDAnnotation;
import org.key_project.sed.core.model.ISEDDebugNode;
import org.key_project.sed.core.model.ISEDDebugTarget;
import org.key_project.sed.core.model.ISEDIDElement;

/**
 * <p>
 * An annotation is of a single type ({@link #getType()}), 
 * may contain additional data (defined by the concrete implementations),
 * is added to {@link ISEDDebugTarget}s and points via links 
 * ({@link ISEDAnnotationLink}) to {@link ISEDDebugNode}s contained in the 
 * subtree of the {@link ISEDDebugTarget}.
 * </p>
 * <p>
 * Implementations should subclass from {@link AbstractSEDAnnotation}.
 * </p>
 * <p>
 * Instances of this interface are created via 
 * {@link ISEDAnnotationType#createAnnotation()}.
 * </p>
 * @author Martin Hentschel
 * @see ISEDAnnotationType
 * @see ISEDAnnotation
 */
public interface ISEDAnnotation extends ISEDAnnotationAppearance, ISEDIDElement {
   /**
    * Property {@link #isEnabled()}.
    */
   public static final String PROP_ENABLED = "enabled";
   
   /**
    * Checks if the annotation is enabled.
    * @return {@code true} enabled, {@code false} disabled.
    */
   public boolean isEnabled();
   
   /**
    * Defines if the annotation is enabled.
    * @param enabled {@code true} enabled, {@code false} disabled.
    */
   public void setEnabled(boolean enabled);
   
   /**
    * Adds the given {@link ISEDAnnotationLink}.
    * @param link The {@link ISEDAnnotationLink} to add.
    */
   public void addLink(ISEDAnnotationLink link);

   /**
    * Removes the given {@link ISEDAnnotationLink}.
    * @param link The {@link ISEDAnnotationLink} to remove.
    */
   public void removeLink(ISEDAnnotationLink link);
   
   /**
    * Returns all contained {@link ISEDAnnotationLink}s.
    * @return All contained {@link ISEDAnnotationLink}s.
    */
   public ISEDAnnotationLink[] getLinks();
   
   /**
    * Returns the {@link ISEDAnnotationLink} at the given index.
    * @param index The index.
    * @return The found {@link ISEDAnnotationLink} or {@code null} if not available.
    */
   public ISEDAnnotationLink getLinkAt(int index);
   
   /**
    * Checks if the given {@link ISEDAnnotationLink} is contained.
    * @param link The {@link ISEDAnnotationLink} to check.
    * @return {@code true} {@link ISEDAnnotationLink} is contained, {@code false} otherwise.
    */
   public boolean containsLink(ISEDAnnotationLink link);
   
   /**
    * Checks if at least one link is available.
    * @return {@code false} no links available, {@code true} at least one link is available.
    */
   public boolean hasLinks();

   /**
    * Returns the number of contained links.
    * @return The number of contained links.
    */
   public int countLinks();

   /**
    * Returns the index of the given link.
    * @param link The link.
    * @return The index of the given link or {@code -1} if not contained.
    */
   public int indexOfLink(ISEDAnnotationLink link);

   /**
    * Checks if this annotation can be deleted from the given {@link ISEDDebugTarget}.
    * @param target The {@link ISEDDebugTarget} to remove annotation from.
    * @return {@code true} can delete, {@code false} can not delete.
    */
   public boolean canDelete(ISEDDebugTarget target);
   
   /**
    * Removes this annotation from the given {@link ISEDDebugTarget}.
    * @param target The {@link ISEDDebugTarget} to remove this annotation from.
    */
   public void delete(ISEDDebugTarget target);
   
   /**
    * Adds the given {@link ISEDAnnotationLinkListener}.
    * @param l The {@link ISEDAnnotationLinkListener} to add.
    */
   public void addAnnotationLinkListener(ISEDAnnotationLinkListener l);
   
   /**
    * Removes the given {@link ISEDAnnotationLinkListener}.
    * @param l The {@link ISEDAnnotationLinkListener} to remove.
    */
   public void removeAnnotationLinkListener(ISEDAnnotationLinkListener l);

   /**
    * Lists all {@link ISEDDebugNode}s which are targets of {@link #getLinks()}.
    * @return The {@link Set} with all available {@link ISEDDebugNode}s.
    */
   public Set<ISEDDebugNode> listLinkTargets();
   
   /**
    * Sets the unique ID which is valid as long as it was never accessed before.
    * @param id The new unique ID to use.
    */
   public void setId(String id);
}