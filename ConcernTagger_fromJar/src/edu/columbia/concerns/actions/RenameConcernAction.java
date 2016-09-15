/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.7 $
 */

package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.ui.concerntree.ConcernTreeViewer;
import edu.columbia.concerns.util.ARFFFile;

/**
 * An action to rename a concern to the model.
 */
public class RenameConcernAction extends Action
{
	private ConcernTreeViewer aViewer;
	private Concern concern; // The concern to rename

	/**
	 * Creates the action.
	 * 
	 * @param pConcern
	 *            The view from where the action is triggered
	 * @param pViewer
	 *            The viewer controlling this action.
	 */
	public RenameConcernAction(ConcernTreeViewer pViewer, Concern pConcern)
	{
		aViewer = pViewer;
		concern = pConcern;
		
		setText(ConcernTagger
				.getResourceString("actions.RenameConcernAction.Label"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.RenameConcernAction.ToolTip"));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		InputDialog lDialog = new InputDialog(
				aViewer.getTree().getShell(),
				ConcernTagger
						.getResourceString("actions.RenameConcernAction.DialogTitle"),
				ConcernTagger
						.getResourceString("actions.RenameConcernAction.DialogLabel"),
				concern.getDisplayName(), 
				new IInputValidator()
					{
						public String isValid(String concernName)
						{
							if (concern.getDisplayName().equals(concernName))
							{
								return ConcernTagger.getResourceString("SameName");
							}
							else if (concernName != null &&
									!concernName.isEmpty() &&
									concern.findByName(concernName) != null)
							{
								return ConcernTagger.getResourceString("NameInUse");
							}
							else
							{
								return Concern.isNameValid(concernName);
							}
						}
					}
				);

		if (lDialog.open() == Window.OK)
		{
			String escapedName = ARFFFile.escape(lDialog.getValue()); 
			concern.rename(escapedName);
		}
	}
}
