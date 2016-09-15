package edu.columbia.concerns.metrics;

import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.actions.OpenConcernDomainAction;
import edu.columbia.concerns.actions.SaveLCCMetricAction;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.model.IConcernModelProviderEx;
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ISimpleProgressMonitor;

/**
 * View for the LCC Metric
 * 
 * @author Bruno
 * 
 */
public class LCCMetricView extends ViewPart 
   implements IConcernListener, IRefreshableView, IConcernModelProviderEx
{
	
	MetricsJob job = null;
	
	private TableViewer aViewer;
	
//	protected LCCMetricTable lccMetricTable = new LCCMetricTable();
	protected LCCMetricTableExtended lccMetricTableExtended = new LCCMetricTableExtended();
	
	private MetricsTool metricsTool;
	
	private ConcernModel concernModel;
	
	private EdgeKind concernComponentRelation;
	
	private SaveLCCMetricAction saveAction = new SaveLCCMetricAction(lccMetricTableExtended);
	
	private int numberCompsMapped;
	
	public void createPartControl(Composite parent)
	{
		// Create a composite to hold the children
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.FILL_BOTH);
		parent.setLayoutData(gridData);

		aViewer = lccMetricTableExtended.createTableViewer(parent);

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
			setPartName(domainAndRelation + " Domain - LCC Metric");
		}
		else
		{
			domainAndRelation = "Default";
			setPartName("LCC Metric");
		}
		
		domainAndRelation += " (" + concernComponentRelation + ")"; 

		this.setTitleToolTip("LCC metric for " + domainAndRelation);
		saveAction.setSuggestedPrefix(domainAndRelation);
	}	
	
	@Override
	public void setFocus() {
		aViewer.getControl().setFocus();

	}

	@Override
	public void refresh() {
		if (job != null)
			job.cancel();
		
		// Calculate the metrics in a concurrent worker job
		job = new MetricsJob(
				"Calculating LCC metric for '" +
				concernModel.getConcernDomain().toString() + "'");
		job.schedule();
		
	}

	@Override
	public void modelChanged(ConcernEvent event) {
		if (event.isChangedDomainName())
		{
			// Renaming our concern domain is a big deal since is stored in
			// the view's secondary id, which is used by Eclipse to restore
			// views on opening.  We have to 'restart' the view by creating
			// a new one and closing the old one.
			
			closeMe();
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
					aViewer.setInput(lccMetricTableExtended);
					aViewer.refresh();
				}
			}
		);
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
					getSite().getPage().hideView(LCCMetricView.this);
				}
			}
		);
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
	
//	public IStatus doMetrics(IProgressMonitor progressMonitor) {
//	
//	// Calculate concern metrics for this concern
//	LCCForComponent lccForComponent = metricsTool.getLCCValues((LCCMetricTable) lccMetricTable, 
//			progressMonitor);
//	
//	// Once we are finished, we need to refresh the display
//	//comenteado por bruno 11/04/11 lccMetricTable.refresh();
//	//talvez usar o safeRefresh()
//	safeRefresh();
//	
//	return Status.OK_STATUS;
//}		

	//-----------------------------------------------------
	// HELPER CLASSES
	//-----------------------------------------------------
	
	/**
	 * Calculates the LCC metric in a separate job thread.
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

//		@Override
//		public IStatus run(IProgressMonitor progressMonitor)
//		{
//			if (lccMetricTable == null || metricsTool == null)
//				return null; // Causes null exception to be thrown
//			
//			myProgressMonitor = progressMonitor;
//			
//			// Clear the metrics since we are recalculating them
//			lccMetricTable.clear();
//			
//			IStatus status = doMetrics(myProgressMonitor);
//			
//			if (myProgressMonitor != null)
//				myProgressMonitor.done();
//			
//			return status;
//		}

		@Override
		public IStatus run(IProgressMonitor progressMonitor)
		{
			if (concernModel == null || metricsTool == null)
				return null; // Causes null exception to be thrown
			
			myProgressMonitor = progressMonitor;
			
			// Clear the metrics since we are recalculating them
			lccMetricTableExtended.clear();
			
			List<Component> allClassesCovered = metricsTool.getAllClassesCovered();
			
			if (myProgressMonitor != null)
				myProgressMonitor.beginTask("Calculating LCC...", allClassesCovered.size());
			
			numberCompsMapped = 0;
			for(Component c: allClassesCovered)
			{
				if (isCanceled())
					return Status.CANCEL_STATUS;
				LCCForComponent lccForComponent = metricsTool.getLCCValue(c);
				lccMetricTableExtended.add(lccForComponent);
				if(lccForComponent.getMeasurement() > 0)
					numberCompsMapped++;
				// Once we are finished, we need to refresh the display.
				// However, this must be done on the UI thread.
				safeRefresh();
				if (myProgressMonitor != null)
					myProgressMonitor.worked(1);				
			}
			
			//Just to show the total of components with at least one concern assignment.
//		   MessageConsole myConsole = findConsole("ConcernTagger Console");
//		   MessageConsoleStream out = myConsole.newMessageStream();
//		   out.println("Number of components mapped: "+numberCompsMapped +
//				   " for "+concernModel.getConcernDomain().getName()+" concern domain.");
		   getSite().getShell().getDisplay().asyncExec (new Runnable() {
		        public void run() {
		            MessageDialog.openWarning(getSite().getShell(),"LCC Counting",("Number of components mapped: "+numberCompsMapped +
				   " for "+concernModel.getConcernDomain().getName()+" concern domain."));
		        }
		    });

			if (myProgressMonitor != null)
				myProgressMonitor.done();
			
			//Just to see the output in the console view
			//lccMetricTable.outputRows(System.out);
			return Status.OK_STATUS;
		}

//	   private MessageConsole findConsole(String name) {
//		    ConsolePlugin plugin = ConsolePlugin.getDefault();
//		    IConsoleManager conMan = plugin.getConsoleManager();
//		    IConsole[] existing = conMan.getConsoles();
//		    for (int i = 0; i < existing.length; i++)
//		       if (name.equals(existing[i].getName()))
//		          return (MessageConsole) existing[i];
//		    //no console found, so create a new one
//		    MessageConsole myConsole = new MessageConsole(name, null);
//		    conMan.addConsoles(new IConsole[]{myConsole});
//		    return myConsole;
//		 }
	   
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
	public EdgeKind getConcernComponentRelation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConcernComponentRelation(EdgeKind edgeKind) {
		// TODO Auto-generated method stub
		refresh();
		
	}

//	@Override
//	public EdgeKind getConcernComponentRelation()
//	{
//		return concernComponentRelation;
//	}
//
	
	@Override
	public ConcernModel getModel()
	{
		return concernModel;
	}
	
	private void fillLocalToolBar(IToolBarManager pManager)
	{
		//pManager.add( new RefreshMetricsAction(this) );
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
		
		//pManager.add( new SetConcernComponentRelationAction(this) );
	}
		
}
