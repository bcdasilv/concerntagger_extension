package edu.columbia.concerns.actions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.metrics.MetricsTable;
import edu.columbia.concerns.util.ProblemManager;

public class SaveLCCMetricAction extends Action {
	private MetricsTable metricsTable;
	private String suggestedPrefix = "";
	
	public SaveLCCMetricAction(MetricsTable metricsTable)
	{
		this.metricsTable = metricsTable;

		setText(ConcernTagger
				.getResourceString("actions.SaveLCCMetricAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/action_save.gif"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.SaveLCCMetricAction.ToolTip"));
	}
	
	public void setSuggestedPrefix(String suggestedPrefix)
	{
		this.suggestedPrefix = suggestedPrefix;
	}

	@Override
	public void run()
	{
		String fileExt = ConcernTagger.getResourceString("actions.SaveLCCMetricAction.FileExt");
		
		final FileDialog fileSaveDialog = new FileDialog(PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
		fileSaveDialog.setText(ConcernTagger
				.getResourceString("actions.SaveLCCMetricAction.DialogTitle"));
		fileSaveDialog.setFilterNames(new String[] { ConcernTagger
				.getResourceString("actions.SaveLCCMetricAction.DialogFilterName"),
				"All Files (*.*)"});
		fileSaveDialog.setFilterExtensions(new String[] { 
				"*" + fileExt,
				"*.*"});

		String suggested = suggestedPrefix;
		if (!suggested.isEmpty())
			suggested += ".";
		
		suggested += "metrics" + fileExt; 

		fileSaveDialog.setFileName(suggested);
		
		String path = fileSaveDialog.open();
		if (path == null || path.isEmpty())
			return;

		if (path.indexOf('.') == -1)
			path += fileExt;
		
		try
		{
			FileOutputStream stream = new FileOutputStream(path);
			PrintStream out = new PrintStream(stream);
			metricsTable.output(out);
			out.close();
			out = null;
		}
		catch (IOException e)
		{
			ProblemManager.reportException(e);
		}
	}
}
