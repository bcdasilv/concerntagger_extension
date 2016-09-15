/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.17 $
 */

package edu.columbia.concerns.ui.concerntree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.ConcernDomain;
import edu.columbia.concerns.repository.EdgeKind;

/**
 * Content provider for a Concern model. This class understands the internal
 * structure of a concern model that contains Java Elements. It organizes the
 * element in a concern model in a forest structure. The roots of the trees are
 * concerns in a concern model. The direct children of these elements are
 * non-inner types. For types, the tree hierarchy is the same as the declarative
 * hierarchy, with elements having as children the elements they declare.
 */
class ConcernTreeContentProvider implements IStructuredContentProvider,
		ITreeContentProvider
{
	private IConcernModelProvider concernModelProvider;

	private static final Object[] EMPTY_ARRAY = new Object[] { }; 
	private static final Object[] ARRAY_VALUE_TRUE = EMPTY_ARRAY; 
	private static final Object[] ARRAY_VALUE_FALSE = null; 
	
	public ConcernTreeContentProvider(IConcernModelProvider concernModelProvider)
	{
		this.concernModelProvider = concernModelProvider;
	}

	/**
	 * Returns the elements to display in the viewer when its input is set to
	 * the given element. These elements can be presented as rows in a table,
	 * items in a list, etc. The result is not modified by the viewer.
	 * 
	 * This method expects as input a concern model.
	 * 
	 * @param pInput
	 *            the concern model to view.
	 * @return The objects.s
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object pInput)
	{
		assert pInput instanceof IConcernModelProvider;

		concernModelProvider = (IConcernModelProvider) pInput;
		
		ConcernDomain concernDomain = concernModelProvider.getModel().getConcernDomain();
		if (concernDomain == null)
		{
			// Will happen if the concern model was initialized with
			// an invalid concern domain name
			return new Object[0];
		}

		return getConcernChildren(null, concernDomain.getRoot(), false);
	}

	/**
	 * Returns ElementNode objects describing the children of this node.
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 * @param pParent
	 *            the parent element
	 * @return an array of child elements
	 */
	public Object[] getChildren(Object pParent)
	{
		if (pParent instanceof ConcernTreeItem)
		{
			return getChildren((ConcernTreeItem) pParent, false);
		}
		else
		{
			assert false; // Shouldn't happen
			return new Object[0];
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 * @param pElement
	 *            the element
	 * @return <code>true</code> if the given element has children, and
	 *         <code>false</code> if it has no children
	 */
	public boolean hasChildren(Object pElement)
	{
		if (pElement instanceof ConcernTreeItem)
		{
			return getChildren((ConcernTreeItem) pElement, true) == ARRAY_VALUE_TRUE;
		}
		else
		{
			assert false; // Shouldn't happen
			return false;
		}
	}
	
	/**
	 * Returns the parent of this node.
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 * @param pElement
	 *            the element
	 * @return the parent element, or <code>null</code> if it has none or if
	 *         the parent cannot be computed
	 */
	public Object getParent(Object pElement)
	{
		if (pElement instanceof ConcernTreeItem)
		{
			ConcernTreeItem cti = (ConcernTreeItem) pElement;
			
			ConcernTreeItem parent = cti.getParent();
			if (parent != null)
				return parent;
			else
				return concernModelProvider;
		}
		else
		{
			return null;
		}
	}
	
	private Object[] getChildren(ConcernTreeItem parent, boolean justReturnTrueOrFalse)
	{
		IJavaElement assignedElement = parent.getJavaElement();

		if (assignedElement != null)
		{
			return getJavaElementChildren(parent, assignedElement, justReturnTrueOrFalse); 
		}
		else
		{
			return getConcernChildren(parent, parent.getConcern(), justReturnTrueOrFalse);
		}
	}	
		
	/*
	 * Builds the tree hierarchy for a concern. @param pConcern The concern to
	 * analyze. @return The top-level elements for a concern.
	 */
	private Object[] getConcernChildren(ConcernTreeItem parent, 
	                                    Concern parentConcern,
	                                    boolean justReturnTrueOrFalse)
	{
		List<ConcernTreeItem> ctis = null;

		assert parent == null || parent.getConcern().equals(parentConcern);
		
		if (justReturnTrueOrFalse)
		{
			if (parentConcern.hasChildren())
				return ARRAY_VALUE_TRUE;
		}
		else
		{
			for (Concern child : parentConcern.getChildren())
			{
				assert !child.equals(parentConcern);
				
				if (ctis == null)
					ctis = new ArrayList<ConcernTreeItem>();
				
				ctis.add( new ConcernTreeItem(concernModelProvider, parent, child) );
			}
		}

		EdgeKind concernComponentRelation = concernModelProvider.getConcernComponentRelation();
		
		Collection<Component> assignedComponents = 
			parentConcern.getAssignments(concernComponentRelation);
		
		for (Component component : assignedComponents)
		{
			IJavaElement assignedElement = component.getJavaElement();
			
			if (!(assignedElement instanceof IType) &&
				assignedElement.getParent() instanceof IType)
			{
				assignedElement = assignedElement.getParent();
			}

			if (justReturnTrueOrFalse)
			{
				return ARRAY_VALUE_TRUE;
			}
			else
			{
				ConcernTreeItem cti = new ConcernTreeItem(concernModelProvider, 
						parent, assignedElement);

				if (ctis == null)
					ctis = new ArrayList<ConcernTreeItem>();
				
				if (!ctis.contains(cti))
					ctis.add(cti);
			}
		}

		if (justReturnTrueOrFalse)
			return ARRAY_VALUE_FALSE;
		else
			return ctis == null ? EMPTY_ARRAY : ctis.toArray();
	}

	// This duplicates Concern.getAssignedComponents(IJavaElement, EdgeKind)
	// for efficiency.
	private Object[] getJavaElementChildren(ConcernTreeItem parent, 
	                                        IJavaElement javaElementParent,
	                                        boolean justReturnTrueOrFalse)
	{
		List<ConcernTreeItem> ctis = null;
		
		// Find all the assigned elements for this concern that are
		// children of the specified element
		
		EdgeKind concernComponentRelation = concernModelProvider.getConcernComponentRelation();
		
		for(Component assignedComponent : parent.getConcern().getAssignments(concernComponentRelation))
		{
			IJavaElement assignedElement = assignedComponent.getJavaElement();
			
			// Skip ourself
			if (assignedElement.equals(javaElementParent))
				continue;

			// Skip elements that are not children of the specified element
			if (assignedElement.getParent() == null ||
				!assignedElement.getParent().equals(javaElementParent))
				continue;
			
			if (justReturnTrueOrFalse)
			{
				return ARRAY_VALUE_TRUE;
			}
			else
			{
				ConcernTreeItem cti = new ConcernTreeItem(concernModelProvider, 
						parent, assignedElement);
				
				if (ctis == null)
					ctis = new ArrayList<ConcernTreeItem>();

				// Not sure why I don't check !ctis.contained(cti) like I
				// did for the other getChildren() method
				ctis.add(cti);
			}
		}

		if (justReturnTrueOrFalse)
			return ARRAY_VALUE_FALSE;
		else
			return ctis == null ? EMPTY_ARRAY : ctis.toArray();
	}
	
	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose()
	{}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 * @param pViewer
	 *            the viewer
	 * @param pOldInput
	 *            the old input element, or <code>null</code> if the viewer
	 *            did not previously have an input
	 * @param pNewInput
	 *            the new input element, or <code>null</code> if the viewer
	 *            does not have an input
	 */
	public void inputChanged(Viewer pViewer, Object pOldInput, Object pNewInput)
	{}
}
