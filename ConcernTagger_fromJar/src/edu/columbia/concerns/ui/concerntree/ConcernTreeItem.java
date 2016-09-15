/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.14 $
 */

package edu.columbia.concerns.ui.concerntree;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.Comparer;

/**
 * Represents a sub-concern or Java element item in the Concern
 * Tree.  Note: The top-level items are all Concern objects.
 */
public class ConcernTreeItem implements IAdaptable
{
	private IConcernModelProvider provider;
	
	private ConcernTreeItem parent;

	private Concern concern = null;
	private boolean hasChildConcerns = false;

	// The Java element wrapped by this object.
	private IJavaElement javaElement = null;

	// The relationship between the concern and the element
	private EdgeKind concernComponentRelation = null;

	private static JavaElementLabelProvider aProvider = new JavaElementLabelProvider(
			JavaElementLabelProvider.SHOW_SMALL_ICONS |
			JavaElementLabelProvider.SHOW_PARAMETERS);

	// We leak these
	private static Image parentConcernWithAssignmentImage = null;
	private static Image parentConcernWithoutAssignmentImage = null;
	private static Image leafConcernWithAssignmentsImage = null;
	private static Image leafConcernWithoutAssignmentsImage = null;
	
	/**
	 * Create a tree item that represents a concern.
	 * 
	 * @param concern
	 *            The concern that the tree item will represent.
	 */
	protected ConcernTreeItem(IConcernModelProvider provider,
	                          ConcernTreeItem parent,
	                          Concern concern)
	{
		this.parent = parent;
		this.provider = provider;
		this.concern = concern;
		this.hasChildConcerns = concern.hasChildren();
	}
	
	/**
	 * Create a tree item that represents a Java element.
	 * <P>
	 * Java element tree items always have a parent tree item for the
	 * concern the element is assigned to.  The same Java element may
	 * be assigned to multiple concerns and each of these assignments
	 * will become a child item of the concern.
	 * 
	 * @param parent
	 *            The concern the element is assigned to.
	 * @param javaElement
	 *            The Java element that the tree item will represent.
	 * @param concernComponentRelation
	 * 			  The relationship between the concern and the element.
	 */
	protected ConcernTreeItem(IConcernModelProvider provider,
	                          ConcernTreeItem parent,
	                          IJavaElement javaElement)
	{
		this.parent = parent;
		this.provider = provider;
		
		assert parent != null;
		this.concern = parent.getConcern();
		
		this.javaElement = javaElement;
	}

	public ConcernTreeItem getParent()
	{
		return parent;
	}

	public ConcernTreeItem getParentConcernItem()
	{
		assert javaElement != null;
	
		ConcernTreeItem parentConcernItem = parent;
		
		while (parentConcernItem != null && parentConcernItem.javaElement != null)
		{
			parentConcernItem = parentConcernItem.getParent();
		}

		assert parentConcernItem != null;
		assert parentConcernItem.concern.equals(concern);
		
		return parentConcernItem;
	}
	
	/**
	 * @return The Java element wrapped by this node.
	 */
	public IJavaElement getJavaElement()
	{
		return javaElement;
	}
	
	/**
	 * @return The relationship between the concern and the component
	 */
	public EdgeKind getRelation()
	{
		return provider.getConcernComponentRelation();
	}

	/**
	 * @return The name of the concern the element wrapped by this object is in.
	 */
	public Concern getConcern()
	{
		return concern;
	}

	public boolean hasAssignment(IJavaElement element)
	{
		if (javaElement == null)
		{
			return concern.isAssigned(element, getRelation());
		}
		else
		{
			// For element items, we want to avoid retrieving and
			// storing the assignments redundantly, since they are
			// already stored in our concern item ancestor.
			// 
			// Walk up the parent until we find the concern item
			// and connect up our assignments.  This
			
			return getParentConcernItem().concern.isAssigned(element, 
					getRelation());
		}
	}
	
	/**
	 * Determines if two ConcernTreeItems are equal. Two element nodes are equal if
	 * both of their wrapped objects are equal and if their concerns are equal.
	 * 
	 * @param pObject
	 *            The element to compare to.
	 * @return true if pObject is equal to this object.
	 */
	@Override
	public boolean equals(Object rhs)
	{
		if (rhs instanceof ConcernTreeItem)
		{
			return equals((ConcernTreeItem) rhs);
		}
		else if (rhs instanceof ConcernEvent)
		{
			// This is called when ConcernTreeViewer.findItems() is called
			return equals((ConcernEvent) rhs);
		}
		else
		{
			return false;
		}
	}

	public boolean equals(ConcernTreeItem rhs)
	{
		return Comparer.safeEquals(this.javaElement, rhs.javaElement) &&
				Comparer.safeEquals(this.getConcern(), rhs.getConcern());
	}

	public boolean equals(ConcernEvent event)
	{
		Concern eventConcern = event.getConcern();
		IJavaElement eventElement = event.getJavaElement();
		
		// If the item is an element, then the event must be related to the element
		if (javaElement != null)
		{
			if (eventElement == null || !javaElement.equals(eventElement))
				return false;
			
			// If the item is an assignment, verify that our assignment matches 
			else if (eventConcern != null)
			{
				return concern.equals(eventConcern) &&
					concernComponentRelation.equals(event.getRelation());
			}
			else
			{
				return true;
			}
		}
		// The item is a concern item
		else if (eventConcern != null)
		{
			return concern.equals(eventConcern);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 * @return the hash code
	 */
	@Override
	public int hashCode()
	{
		String code = concern.toString();
		if (javaElement != null)
			code += javaElement.getElementName();

		return code.hashCode();
	}

	@Override
	public String toString()
	{
		if (javaElement != null)
		{
			aProvider.turnOn(JavaElementLabelProvider.SHOW_TYPE);
			String result = aProvider.getText(javaElement) + " -> " + 
				concern.getDisplayName() +
				" " + getRelation().name();
			aProvider.turnOff(JavaElementLabelProvider.SHOW_TYPE);
			return result;
		}
		else
			return concern.getDisplayName() + " " + getRelation().name();
	}
	
	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 * @param pAdapter
	 *            the adapter class to look up
	 * @return a object castable to the given class, or <code>null</code> if
	 *         this object does not have an adapter for the given class
	 */
	public Object getAdapter(Class pAdapter)
	{
		if (pAdapter == IJavaElement.class)
		{
			return javaElement;
		}
		else if (pAdapter == Concern.class)
		{
			return concern;
		}
		else
		{
			return null;
		}
	}

	public String getText()
	{
		if (javaElement != null)
			return aProvider.getText(javaElement);
		else
			return concern.getDisplayName();
	}

	public Image getImage()
	{
		if (parentConcernWithAssignmentImage == null)
			parentConcernWithAssignmentImage = AbstractUIPlugin.imageDescriptorFromPlugin(
					ConcernTagger.ID_PLUGIN, "icons/lightbulbs.ico").createImage();

		if (parentConcernWithoutAssignmentImage == null)
			parentConcernWithoutAssignmentImage = AbstractUIPlugin.imageDescriptorFromPlugin(
					ConcernTagger.ID_PLUGIN, "icons/lightbulbs_off.ico").createImage();
		
		if (leafConcernWithAssignmentsImage == null)
			leafConcernWithAssignmentsImage = AbstractUIPlugin.imageDescriptorFromPlugin(
					ConcernTagger.ID_PLUGIN, "icons/lightbulb.png").createImage(); 

		if (leafConcernWithoutAssignmentsImage == null)
			leafConcernWithoutAssignmentsImage = AbstractUIPlugin.imageDescriptorFromPlugin(
					ConcernTagger.ID_PLUGIN, "icons/lightbulb_off.png").createImage(); 
		
		if (javaElement != null)
		{
			return aProvider.getImage(javaElement);
		}
		
		boolean hasAssignments = concern.isAssigned(getRelation());
		
		if (hasChildConcerns)
		{
			return hasAssignments ? 
					parentConcernWithAssignmentImage :
					parentConcernWithoutAssignmentImage;
		}
		else
		{
			return hasAssignments ? 
					leafConcernWithAssignmentsImage : 
					leafConcernWithoutAssignmentsImage;
		}
	}
}
