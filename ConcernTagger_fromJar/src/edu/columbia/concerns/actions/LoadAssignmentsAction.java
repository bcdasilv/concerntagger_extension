/**
 * 
 */
package edu.columbia.concerns.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.swt.widgets.FileDialog;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.util.ConcernAssignmentARFFFile;

/**
 * @author eaddy
 * 
 */
public class LoadAssignmentsAction extends Action
{
	private IConcernModelProvider concernModelProvider;
	private IStatusLineManager statusLineManager;
	
	public LoadAssignmentsAction(IConcernModelProvider concernModelProvider,
	                             IStatusLineManager statusLineManager)
	{
		this.concernModelProvider = concernModelProvider;
		this.statusLineManager = statusLineManager;
		
		setText(ConcernTagger
				.getResourceString("actions.LoadAssignmentsAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/table_link.png"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.LoadAssignmentsAction.ToolTip"));
	}

	@Override
	public void run()
	{
		final FileDialog fileOpenDialog = new FileDialog(PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getShell());
		fileOpenDialog
				.setText(ConcernTagger
						.getResourceString("actions.LoadAssignmentsAction.DialogTitle"));
		fileOpenDialog
				.setFilterNames(new String[] { ConcernTagger
						.getResourceString("actions.LoadAssignmentsAction.DialogFilterName") });
		fileOpenDialog
				.setFilterExtensions(new String[] { ConcernTagger
						.getResourceString("actions.LoadAssignmentsAction.DialogFilterExt") });
		fileOpenDialog.open();

		final String[] fileNames = fileOpenDialog.getFileNames();
		if (fileNames == null || fileNames.length == 0)
			return;

		Job job = new Job("Loading concern assignments...")
			{
				@Override
				protected IStatus run(IProgressMonitor progressMonitor)
				{
					readAssignmentFiles(fileOpenDialog.getFilterPath(), fileNames, 
							progressMonitor, statusLineManager);

					return Status.OK_STATUS;
				}
		
			};
				
		job.setUser(true);
		job.schedule();
	}

	private void readAssignmentFiles(final String dir, final String[] fileNames, 
			IProgressMonitor progressMonitor, IStatusLineManager statusLineManager)
	{
		for (String fileName : fileNames)
		{
			String path = dir + java.io.File.separator + fileName;

			ConcernAssignmentARFFFile asf = 
				new ConcernAssignmentARFFFile(	path, 
												concernModelProvider.getModel(), 
												concernModelProvider.getConcernComponentRelation(),
												progressMonitor,
												statusLineManager);
			asf.read();
		}
	}
}
