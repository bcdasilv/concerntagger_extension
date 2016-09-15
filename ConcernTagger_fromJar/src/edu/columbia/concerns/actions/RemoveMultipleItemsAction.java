/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.12 $
 */

package edu.columbia.concerns.actions;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.ui.concerntree.ConcernTreeItem;
import edu.columbia.concerns.util.ConcernJob;

/**
 * An action to delete a list of elements in a model.
 */
public class RemoveMultipleItemsAction extends Action
{
	ConcernJob job = null;
	
	/**
	 * Creates the action.
	 */
	public RemoveMultipleItemsAction(IConcernModelProvider concernModelProvider)
	{
		job = new ConcernJob("Removing/unassigning...", concernModelProvider);
		
		setText(ConcernTagger.getResourceString("actions.RemoveMultipleItemsAction.Label"));
		//setImageDescriptor( AbstractUIPlugin.imageDescriptorFromPlugin(
		//		ConcernMapper.ID_PLUGIN, "icons/delete.png"));
		setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor( ISharedImages.IMG_TOOL_DELETE)); 
		setToolTipText(ConcernTagger
				.getResourceString("actions.RemoveMultipleItemsAction.ToolTip"));
	}

	/**
	 * Called when DEL is pressed on concern or assignment items in the concern
	 * tree, or when user rights clicks concern and assignment items and selects
	 * Remove. Removes all children concerns and assignments.
	 */
	public void addItemsToRemove(List<ConcernTreeItem> itemsToRemove)
	{
		for(ConcernTreeItem itemToRemove : itemsToRemove)
		{
			job.addRemovalTask(itemToRemove);
		}
	}

	/**
	 * Called when user right clicks on a Java element and selects Unassign
	 */
	public void addItemToUnassign(Concern concern, 
	                              IJavaElement element, 
	                              EdgeKind relation)
	{
		job.addUnassignTask(concern, element, relation);
	}
	
	@Override
	public boolean isEnabled() 
	{
		return job.hasWork();
	}
	
	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		if (job.hasWork() && shouldProceed())
		{
			job.schedule();
		}
	}

	private boolean shouldProceed()
	{
		String msg = ConcernTagger.getResourceString("actions.RemoveMultipleItemsAction.WarningOverwrite"); 
		
		msg = msg.replace("$NUM$", Integer.toString(job.getWorkItemCount()));
		
		return MessageDialog.openQuestion(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
			ConcernTagger.getResourceString("actions.RemoveMultipleItemsAction.QuestionDialogTitle"),
			msg);
	}
}
