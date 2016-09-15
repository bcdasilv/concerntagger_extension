package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.metrics.MetricsView;
import edu.columbia.concerns.repository.ConcernDomain;
import edu.columbia.concerns.util.ProblemManager;

public class ShowMetricsAction extends Action
{
	private ConcernDomain concernDomain;
	private IWorkbenchPartSite site;

	public ShowMetricsAction(	ConcernDomain concernDomain,
								IWorkbenchPartSite site)
	{
		this.concernDomain = concernDomain;
		this.site = site;
		
		setText(ConcernTagger
				.getResourceString("actions.ShowMetricsViewAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/chart_bar.png"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.ShowMetricsViewAction.ToolTip"));
	}

	@Override
	public void run()
	{
		try
		{
			// From plugin.xml
			String viewId = "edu_columbia_concerns_MetricsView";
			
			String secondaryId = OpenConcernDomainAction.createSecondaryId(
					viewId, 
					concernDomain.getName());
			
			IViewReference firstView = site.getPage().findViewReference(viewId);
			if (firstView == null)
			{
				// There are no Metrics views currently.  We have to create
				// the first one.
				
				IViewPart viewPart = site.getPage().showView(viewId);
				
				assert viewPart != null;
				assert viewPart instanceof MetricsView;
				
				MetricsView metricsView = (MetricsView) viewPart;
				metricsView.setConcernDomain(concernDomain.getName());
				
				site.getPage().activate(metricsView);
			}
			else
			{
				// There is at least one metrics view.  The remaining metrics
				// views must use a secondary id
				
				IViewReference domainSpecificView =
					site.getPage().findViewReference(viewId, secondaryId);
				
				if (domainSpecificView != null)
				{
					// There is already a metrics view open for this concern
					// domain so activiate it
					site.getPage().activate(domainSpecificView.getPart(false));
				}
				else
				{
					IViewPart viewPart = site.getPage().showView(viewId, secondaryId, 
							IWorkbenchPage.VIEW_ACTIVATE | IWorkbenchPage.VIEW_CREATE);
					
					assert viewPart != null;
					assert viewPart instanceof MetricsView;
					
					MetricsView metricsView = (MetricsView) viewPart;
					metricsView.setConcernDomain(concernDomain.getName());
					site.getPage().activate(metricsView);
				}
			}
		}
		catch (PartInitException e)
		{
			ProblemManager.reportException(e);
		}
	}
}
