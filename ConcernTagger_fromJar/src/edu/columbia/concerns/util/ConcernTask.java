package edu.columbia.concerns.util;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;

import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.ui.concerntree.ConcernTreeItem;

public class ConcernTask
{
	Concern newConcern = null;
	
	Concern oldConcern = null;

	IJavaElement element = null;

	EdgeKind oldRelation = null;
	EdgeKind newRelation = null;
	
	boolean processChildrenToo = false;

	boolean unassignFromOldConcern = false;
	
	private ConcernTask()
	{ }
	
	/**
	 * Removes the concern or assignment.  When removing a concern,
	 * subconcerns will also be removed.  When removing an assignment, just
	 * that assignment will be removed. 
	 * @param concernToRemove
	 * 	Must be non-null.
	 * @param elementToRemove
	 * 	The element to unassign, or null if removing a concern.
	 * @param relationToRemove
	 * 	The relation of the assignment to unassign, or null if removing a concern. 
	 */
	public static ConcernTask createRemovalOrUnassignTask(	Concern concernToRemove, 
	                                                      	IJavaElement elementToRemove,
	                                                      	EdgeKind relationToRemove)
	{
		ConcernTask task = new ConcernTask();
		task.oldConcern = concernToRemove;
		task.element = elementToRemove;
		task.oldRelation = relationToRemove;
		task.processChildrenToo = false;
		task.unassignFromOldConcern = true;
		
		assert task.oldConcern != null || task.element != null;
		
		return task;
	}

	/**
	 * Removes the item and its children items.  When removing a
	 * concern item, removes all child concerns.  When removing an
	 * assignment, also removes subassignments (assignments of the
	 * element's children). 
	 * <P>
	 * Called when DEL is pressed on concern or assignment items in the concern
	 * tree, or when user rights clicks concern and assignment items and selects
	 * Remove.
	 */
	public static ConcernTask createRemovalTask(ConcernTreeItem cti)
	{
		ConcernTask task = createRemovalOrUnassignTask(cti.getConcern(), cti.getJavaElement(),
				cti.getRelation());
		task.processChildrenToo = true;
		return task;
	}

	/**
	 * Creates a simple assignment task.
	 */
	public static ConcernTask createAssignmentTask(	Concern concernToAssign, 
	                                               	IJavaElement elementToAssign,
	                                               	EdgeKind relationToAssign)
	{
		ConcernTask task = new ConcernTask();
		task.newConcern = concernToAssign;
		task.element = elementToAssign;
		task.newRelation = relationToAssign;
		return task;
	}

	/**
	 * Called when dragging an item from the concern tree to another item in
	 * the (possibly different) concern tree. 
	 */
	public static ConcernTask createCopyMoveTask(	ConcernTreeItem itemSource,
	                                                ConcernTreeItem itemDestination,
	                                                boolean move)
	{
		ConcernTask task = new ConcernTask();
		
		task.oldConcern = itemSource.getConcern();
		task.oldRelation = itemSource.getRelation();
		task.element = itemSource.getJavaElement();
		task.newConcern = itemDestination.getConcern();
		task.newRelation = itemDestination.getRelation();
		task.processChildrenToo = true;
		task.unassignFromOldConcern = move;

		assert task.oldConcern != null;
		
		// Can only copy/move to a concern item
		assert task.newConcern != null;
		assert itemDestination.getJavaElement() == null;
		
		return task;
	}
	
	public IStatus run(IProgressMonitor monitor)
	{
		if (oldConcern != null && newConcern != null && element == null)
		{
			if (monitor != null)
				monitor.subTask(oldConcern.getDisplayName());

			// User dragged a concern and dropped it onto another concern
			newConcern.addChild(oldConcern);
		}
		else if (oldConcern != null && newConcern == null && element == null)
		{
			if (monitor != null)
				monitor.subTask(oldConcern.getDisplayName());

			// User pressed DEL on a concern
			oldConcern.remove();
		}
		else if (element != null)
		{
			// User is moving an assignment from one concern to another
			// (in which case newConcern != null), OR
			// User pressed DEL on an assignment (in which case newConcern
			// == null and oldConcern is the assignment to be deleted), OR
			// User right-clicked on a java element and selected Assign
			// (in which case newConcern != null)
			
			if (monitor != null)
				monitor.subTask(element.getElementName());

			// A java element in the concern tree may be 'virtual', i.e.,
			// only its children are assigned to the concern, not the element
			// itself.  For example, if a member is assigned to a concern, it
			// will be shown beneath its class item, which is not assigned.
			// In this case, we want to only unassign/assign the member
			// elements actually assigned to the concern.
			
			assert newConcern == null || newRelation != null;

			if (processChildrenToo)
			{
				assert oldConcern != null;
				assert oldRelation != null;
				
				Collection<Component> assignedComponents =
					Component.getAssignmentRecursive(oldConcern, element, oldRelation);
				
				for(Component assignedComponent : assignedComponents)
				{
					if (monitor != null && monitor.isCanceled())
						return Status.CANCEL_STATUS;
					
					// We are moving an assignment from one concern to another
					// in the ConcernMapper tree. Unassign the element from the
					// original concern.
					if (unassignFromOldConcern)
						oldConcern.unassign(assignedComponent, oldRelation);
					
					if (newConcern != null)
					{
						newConcern.assign(assignedComponent, newRelation);
					}
				}
			}
			else
			{
				// We are moving an assignment from one concern to another
				// in the ConcernMapper tree. Unassign the element from the
				// original concern.
				if (unassignFromOldConcern)
				{
					assert oldConcern != null;
					assert oldRelation != null;
					oldConcern.unassign(element, oldRelation);
				}
				
				if (newConcern != null)
				{
					newConcern.assign(element, newRelation);
				}
			}
		}
		else
		{
			assert false; // Can't happen
		}
		
		return Status.OK_STATUS;
	}
}
