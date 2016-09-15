/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.5 $
 */

package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.ui.ConcernView;

/**
 * An action to collapse all trees in the ConcernMapper View.
 */
public class CollapseAllAction extends Action
{
	private ConcernView aViewer;

	/**
	 * Creates the action.
	 * 
	 * @param pViewer
	 *            The viewer controlling this action.
	 */
	public CollapseAllAction(ConcernView pViewer)
	{
		aViewer = pViewer;
		setText(ConcernTagger
				.getResourceString("actions.CollapseAllAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/collapseall.gif"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.CollapseAllAction.ToolTip"));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		aViewer.collapseAll();
	}
}
