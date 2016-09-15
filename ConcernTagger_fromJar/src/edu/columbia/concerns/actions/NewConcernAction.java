/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.6 $
 */

package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.util.ARFFFile;

/**
 * An action to add a new concern to the model.
 */
public class NewConcernAction extends Action
{
	private Shell shell;
	private IConcernModelProvider concernModelProvider;
	private Concern parent;

	private Concern concernJustAdded = null;
	
	/**
	 * Creates the action.
	 * 
	 * @param pViewer
	 *            The view from where the action is triggered
	 */
	public NewConcernAction(Shell shell, 
							IConcernModelProvider concernModelProvider,
							Concern parent)
	{
		this.shell = shell;
		this.concernModelProvider = concernModelProvider;
		this.parent = parent;

		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/lightbulb_add.png"));
		
		if (parent == null || parent.isRoot())
		{
			setText(ConcernTagger
					.getResourceString("actions.NewConcernAction.Top.Label"));
			setToolTipText(ConcernTagger
					.getResourceString("actions.NewConcernAction.Top.ToolTip"));
		}
		else
		{
			setText(ConcernTagger
					.getResourceString("actions.NewConcernAction.Child.Label"));
			setToolTipText(ConcernTagger
					.getResourceString("actions.NewConcernAction.Child.ToolTip"));
		}
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		InputDialog dialog = new InputDialog(
				shell,
				ConcernTagger.getResourceString("actions.NewConcernAction.DialogTitle"),
				ConcernTagger.getResourceString("actions.NewConcernAction.DialogLabel"),
				"", 
				new IInputValidator()
				{
					public String isValid(String concernName)
					{
						if (concernName != null &&
							!concernName.isEmpty() &&
							concernModelProvider.getModel().getConcernByName(concernName) != null)
						{
							return ConcernTagger.getResourceString("NameInUse");
						}
						else
						{
							return Concern.isNameValid(concernName);
						}
					}
				});

		if (dialog.open() != Window.OK)
			return;

		ConcernModel concernModel = concernModelProvider.getModel();
		
		String newConcernName = ARFFFile.escape(dialog.getValue());
		
		concernJustAdded = concernModel.createConcern(newConcernName, "");
		if (parent != null && !parent.isRoot() && concernJustAdded != null)
		{
			// Move concern from root to the specific parent
			parent.addChild(concernJustAdded);
		}
	}
	
	public Concern getConcernJustAdded()
	{
		return concernJustAdded;
	}
}
