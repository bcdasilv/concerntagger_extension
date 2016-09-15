/**
 * 
 */

package edu.columbia.concerns.actions;

import java.io.File;

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
import edu.columbia.concerns.util.ConcernARFFFile;

/**
 * @author eaddy
 * 
 */
public class LoadConcernsAction extends Action
{
	private IConcernModelProvider concernModelProvider;
	private IStatusLineManager statusLineManager;
	
	public LoadConcernsAction(IConcernModelProvider concernModelProvider,
	                          IStatusLineManager statusLineManager)
	{
		this.concernModelProvider = concernModelProvider;
		this.statusLineManager = statusLineManager;
		
		setText(ConcernTagger
				.getResourceString("actions.LoadConcernsAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/concerns.png"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.LoadConcernsAction.ToolTip"));
	}

	@Override
	public void run()
	{
		final FileDialog fileOpenDialog = new FileDialog(PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getShell());
		fileOpenDialog.setText(ConcernTagger
				.getResourceString("actions.LoadConcernsAction.DialogTitle"));
		fileOpenDialog.setFilterNames(new String[] { ConcernTagger
				.getResourceString("actions.LoadConcernsAction.DialogFilterName") });
		fileOpenDialog.setFilterExtensions(new String[] { ConcernTagger
				.getResourceString("actions.LoadConcernsAction.DialogFilterExt") });
		fileOpenDialog.open();

		final String[] fileNames = fileOpenDialog.getFileNames();
		if (fileNames == null || fileNames.length == 0)
			return;

		Job job = new Job("Loading concerns...")
		{
			@Override
			protected IStatus run(IProgressMonitor progressMonitor)
			{
				readConcernFiles(fileOpenDialog.getFilterPath(), fileNames, 
						progressMonitor, statusLineManager);

				return Status.OK_STATUS;
			}
	
		};
			
		job.setUser(true);
		job.schedule();
	}

	private void readConcernFiles(final String dir, final String[] fileNames,
			IProgressMonitor progressMonitor, IStatusLineManager statusLineManager)
	{
		for (String fileName : fileNames)
		{
			String path = dir + File.separator + fileName;

			ConcernARFFFile concernArffFile = new ConcernARFFFile(path, 
					concernModelProvider.getModel(), progressMonitor, statusLineManager);

			concernArffFile.read();
		}
	}
}
