package edu.columbia.concerns.metrics;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.actions.OpenConcernDomainAction;
import edu.columbia.concerns.actions.RefreshMetricsAction;
import edu.columbia.concerns.actions.SaveMetricsAction;
import edu.columbia.concerns.actions.SetConcernComponentRelationAction;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.model.IConcernModelProviderEx;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ISimpleProgressMonitor;

/**
 * View for metrics
 * 
 * @author vibhav.garg
 * 
 */
public class MetricsView 
	extends 
		ViewPart
	implements 
		IConcernListener, IConcernModelProviderEx, IRefreshableView
{
	private TableViewer aViewer;
	private MetricsTool metricsTool;

	private ConcernModel concernModel;
	private EdgeKind concernComponentRelation;

	MetricsJob job = null;
	
	ConcernMetricsTable concernMetricsTable = new ConcernMetricsTable();

	// Put this after concernMetricsTable above is instantiated!
	private SaveMetricsAction saveAction = new SaveMetricsAction(concernMetricsTable);
	
	@Override
	public void createPartControl(Composite parent)
	{
		// Create a composite to hold the children
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.FILL_BOTH);
		parent.setLayoutData(gridData);

		aViewer = concernMetricsTable.createTableViewer(parent);
		
		// Add elements to the action bars
		IActionBars lBars = getViewSite().getActionBars();
		fillLocalToolBar(lBars.getToolBarManager());
		fillToolBarMenu(lBars.getMenuManager());
		
		String concernDomain = OpenConcernDomainAction.extractConcernDomainFromSecondaryId(
				getViewSite().getSecondaryId());

		setConcernDomain(concernDomain);
	}
	
	@Override
	public void setConcernDomain(String concernDomain)
	{
		if (concernModel != null && 
			concernModel.getConcernDomain() != null &&
			concernModel.getConcernDomain().equals(concernDomain))
		{
			return;
		}
		
		// E.g., if the workspace is "C:\Workspace" then the
		// directory will be
		// "C:\Workspace\.metadata\plugins\columbia.concerntagger"
		IPath workspacePluginDir = ConcernTagger.singleton().getStateLocation();

		// True means create the database if it doesn't exist
		ConcernRepository hsqldb = ConcernRepository.openDatabase(workspacePluginDir.append("db")
				.toOSString(), true);
		
		concernModel = ConcernModelFactory.singleton().getConcernModel(
				hsqldb, concernDomain);

		// This is gheto since it ignores the relation established by the
		// launching concern view, it just uses the currently active one
		concernComponentRelation = ConcernModelFactory.singleton().getConcernComponentRelation();

		metricsTool = new MetricsTool(this);

		concernModel.addListener(this);

		updateTitleAndToolTip();
		
		refresh();
	}
	
	public void updateTitleAndToolTip()
	{
		String domainAndRelation;
		
		if (!concernModel.getConcernDomain().isDefault())
		{
			domainAndRelation = concernModel.getConcernDomain().getName();
			setPartName(domainAndRelation + " Metrics");
		}
		else
		{
			domainAndRelation = "Default";
			setPartName("Concern Metrics");
		}
		
		domainAndRelation += " (" + concernComponentRelation + ")"; 

		this.setTitleToolTip("Concern metrics for " + domainAndRelation);
		saveAction.setSuggestedPrefix(domainAndRelation);
	}

	@Override
	public void setConcernComponentRelation(EdgeKind edgeKind)
	{
		if (concernComponentRelation == edgeKind)
			return;
		
		concernComponentRelation = edgeKind;

		updateTitleAndToolTip();
		
		refresh();
	}
	
	@Override
	public void setFocus()
	{
		aViewer.getControl().setFocus();
	}

	@Override
	public void refresh()
	{
		if (job != null)
			job.cancel();
		
		// Calculate the metrics in a concurrent worker job
		job = new MetricsJob(
				"Calculating scattering metrics for '" +
				concernModel.getConcernDomain().toString() + "'");
		job.schedule();
	}

	@Override
	public void dispose()
	{
		if (job != null)
			job.cancel();
		super.dispose();
	}

	private Display safeGetDisplay()
	{
		if (aViewer == null || 
			aViewer.getControl() == null ||
			aViewer.getControl().isDisposed() ||
			aViewer.getControl().getDisplay() == null ||
			aViewer.getControl().getDisplay().isDisposed())
		{
			return null;
		}
		else
		{
			return aViewer.getControl().getDisplay();
		}
	}
	
	public void safeRefresh()
	{
		Display display = safeGetDisplay();
		if (display == null)
			return;
		
		// Once we are finished, we need to refresh the display.
		// However, this must be done on the UI thread.
		display.asyncExec(new Runnable() 
			{
				public void run() 
				{
					aViewer.setInput(concernMetricsTable);
					aViewer.refresh();
				}
			}
		);
	}
	
	@Override
	public void modelChanged(ConcernEvent event)
	{
		if (event.isChangedDomainName())
		{
			// Renaming our concern domain is a big deal since is stored in
			// the view's secondary id, which is used by Eclipse to restore
			// views on opening.  We have to 'restart' the view by creating
			// a new one and closing the old one.
			
			closeMe();
		}		
	}

	// Is there a better way?
	public void closeMe()
	{
		Display display = safeGetDisplay();
		if (display == null)
			return;
		
		display.asyncExec(new Runnable()
			{
				public void run()
				{
					getSite().getPage().hideView(MetricsView.this);
				}
			}
		);
	}

	//-----------------------------------------------------
	// HELPER CLASSES
	//-----------------------------------------------------
	
	/**
	 * Calculates the concern metrics in a separate job thread.
	 */
	private final class MetricsJob 
		extends Job
		implements
			ISimpleProgressMonitor
	{
		IProgressMonitor myProgressMonitor;
		
		private MetricsJob(String name)
		{
			super(name);
		}

		@Override
		public IStatus run(IProgressMonitor progressMonitor)
		{
			if (concernModel == null || metricsTool == null)
				return null; // Causes null exception to be thrown
			
			myProgressMonitor = progressMonitor;
			
			// Clear the metrics since we are recalculating them
			concernMetricsTable.clear();
			
			if (myProgressMonitor != null)
				myProgressMonitor.beginTask("Concern", concernModel.getNumConcerns());
			
			IStatus status = processRecursive(concernModel.getRoot());
			
			if (myProgressMonitor != null)
				myProgressMonitor.done();
			
			return status;
		}

		private IStatus processRecursive(Concern concern)
		{
			if (isCanceled())
				return Status.CANCEL_STATUS;
			
			if (!concern.isRoot())
			{
				if (myProgressMonitor != null)
					myProgressMonitor.subTask(concern.getDisplayName());
				
				// Calculate concern metrics for this concern
				MetricsForConcern metrics = metricsTool.getMetricsForConcern(concern, 
						this);
				if (metrics != null)
					concernMetricsTable.add(metrics);

				// Once we are finished, we need to refresh the display.
				// However, this must be done on the UI thread.
				safeRefresh();

				if (myProgressMonitor != null)
					myProgressMonitor.worked(1);
			}
			
			for(Concern child : concern.getChildren())
			{
				IStatus status = processRecursive(child);
				if (!status.isOK())
					return status;
			}
			
			return Status.OK_STATUS;
		}
		
		@Override
		public boolean isCanceled()
		{
			if (myProgressMonitor == null)
				return false;
			else 
				return myProgressMonitor.isCanceled();
		}
	}

	@Override
	public EdgeKind getConcernComponentRelation()
	{
		return concernComponentRelation;
	}

	@Override
	public ConcernModel getModel()
	{
		return concernModel;
	}

	private void fillLocalToolBar(IToolBarManager pManager)
	{
		pManager.add( new RefreshMetricsAction(this) );
		pManager.add(saveAction);
	}
	
	/**
	 * Adds the actions to the menu.
	 * 
	 * @param pManager
	 *            the menu manager.
	 */
	private void fillToolBarMenu(IMenuManager pManager)
	{
		// True means disable currently selected domain
		pManager.add( new OpenConcernDomainAction(this, true) );
		
		pManager.add( new SetConcernComponentRelationAction(this) );
	}
}
