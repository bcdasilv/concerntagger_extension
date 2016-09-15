package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.metrics.IRefreshableView;

/**
 * Class that refreshes the metrics view.
 * 
 * @author vgarg
 * 
 */
public class RefreshMetricsAction extends Action
{
	private IRefreshableView refreshableView;

	public RefreshMetricsAction(IRefreshableView refreshableView)
	{
		this.refreshableView = refreshableView;
		setText(ConcernTagger
				.getResourceString("actions.RefreshMetricAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/action_refresh.gif"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.RefreshMetricAction.ToolTip"));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		if (refreshableView != null)
			refreshableView.refresh();
	}
}
