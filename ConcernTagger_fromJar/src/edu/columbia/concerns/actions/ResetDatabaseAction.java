package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;

/**
 * Action that resets the database.
 * 
 * @author vgarg
 * 
 */
public class ResetDatabaseAction extends Action
{
	private IConcernModelProvider concernModelProvider;
	
	public ResetDatabaseAction(IConcernModelProvider concernModelProvider)
	{
		this.concernModelProvider = concernModelProvider;
		
		setText(ConcernTagger
				.getResourceString("actions.ResetDatabaseAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/exclamation_point.gif"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.ResetDatabaseAction.ToolTip"));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		boolean resetOK = MessageDialog.openQuestion(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
			ConcernTagger.getResourceString("actions.ResetDatabaseAction.DialogTitle"),
			ConcernTagger.getResourceString("actions.ResetDatabaseAction.DialogMessage"));

		if (resetOK)
		{
			concernModelProvider.getModel().resetDatabase();
		}
	}

}
