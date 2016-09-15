package edu.columbia.concerns.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;


import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.ui.concerntree.ConcernTreeItem;

public class ConcernJob extends Job
{
	List<ConcernTask> todo = new ArrayList<ConcernTask>();
	
	ConcernModel concernModel;
	EdgeKind concernComponentRelation;
	IConcernModelProvider concernModelProvider;

	public ConcernJob(String description, IConcernModelProvider concernModelProvider)
	{
		super(description);
	
		this.concernModelProvider = concernModelProvider;
	}

	public void clearWork()
	{
		todo.clear();
	}
	
	public int getWorkItemCount()
	{
		return todo == null ? 0 : todo.size();
	}
	
	public boolean hasWork()
	{
		return getWorkItemCount() > 0;
	}

	/**
	 * Called when user right clicks an element from outside of the concern tree
	 * and selects Unassign, or right clicks a concern item and selects Unassign. 
	 */
	public void addUnassignTask(	Concern concernToRemove, 
	                                IJavaElement elementToRemove,
	                                EdgeKind relationToRemove)
	{
		todo.add(ConcernTask.createRemovalOrUnassignTask(concernToRemove, elementToRemove, 
				relationToRemove));
	}

	/**
	 * Called when DEL is pressed on concern or assignment items in the concern
	 * tree, or when user rights clicks concern and assignment items and selects
	 * Remove. Removes all children concerns and assignments.
	 */
	public void addRemovalTask(ConcernTreeItem cti)
	{
		todo.add(ConcernTask.createRemovalTask(cti));
	}

	/**
	 * Called when user right-clicks a Java element and selects Assign or
	 * when they right-click one or more concern items and selects Assign.
	 */
	public void addAssignTask(	Concern concernToAssign, 
	                            IJavaElement elementToAssign,
	                            EdgeKind relationToAssign)
	{
		todo.add(ConcernTask.createAssignmentTask(concernToAssign, elementToAssign,
				relationToAssign));
	}

	/**
	 * Called when a Java element from outside the concern tree is dropped
	 * onto onto a concern item in the tree.
	 */
	public void addAssignTask(	ConcernTreeItem concernItemToAssign,
	                            IJavaElement elementToAssign)
	{
		// Can only assign to a concern item
		assert concernItemToAssign.getJavaElement() == null;
		
		todo.add(ConcernTask.createAssignmentTask(concernItemToAssign.getConcern(), 
				elementToAssign, concernItemToAssign.getRelation()));
	}
	
	/**
	 * Called when dragging an item from the concern tree to another item in
	 * the (possibly different) concern tree. 
	 */
	public void addCopyMoveTask(ConcernTreeItem source, 
	                            ConcernTreeItem destination,
	                            boolean move)
	{
		todo.add(ConcernTask.createCopyMoveTask(source, destination, move));
	}

	@Override
	public IStatus run(IProgressMonitor monitor)
	{
		if (!hasWork())
			return Status.OK_STATUS; // Nothing to do
		
		// Copy these just in case they change in the interim
		this.concernModel = concernModelProvider.getModel();
		
		boolean showProgress = todo.size() > 5;
		
		if (showProgress)
		{
			monitor.beginTask("Processing " + todo.size() + " items", todo.size());
		}

		try
		{
			concernModel.disableNotifications();

			for(int i = todo.size() - 1; i >=0; --i)
			{
				ConcernTask task = todo.remove(i);

				// Did the user ask us to quit?
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				
				IStatus status = task.run(showProgress ? monitor : null);
				if (!status.isOK())
					return status;
	
				if (showProgress)
					monitor.worked(1);	
			}

			assert !hasWork();

			return Status.OK_STATUS;
		}
		finally
		{
			clearWork();
			
			if (showProgress)
				monitor.done();

			concernModel.enableNotifications();
		}
	}
}
