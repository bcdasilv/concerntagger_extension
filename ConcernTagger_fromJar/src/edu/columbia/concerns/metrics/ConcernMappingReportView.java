package edu.columbia.concerns.metrics;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.ViewPart;
import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.actions.OpenConcernDomainAction;
import edu.columbia.concerns.actions.SaveConcernMappingReportAction;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.model.IConcernModelProviderEx;
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ISimpleProgressMonitor;
import edu.columbia.concerns.util.ProblemManager;

/**
 * View for reporting a quantitative summary of the concern-to-code mapping
 * 
 * @author Bruno
 * 
 */
public class ConcernMappingReportView extends ViewPart 
   implements IConcernListener, IRefreshableView, IConcernModelProviderEx
{
	
	MetricsJob job = null;
	
	private TableViewer aViewer;
	
	protected ConcernMappingReportTable concernMappingReportTable = new ConcernMappingReportTable();
	
	private MetricsTool metricsTool;
	
	private ConcernModel concernModel;
	
	private EdgeKind concernComponentRelation;
	
	private SaveConcernMappingReportAction saveAction = new SaveConcernMappingReportAction(concernMappingReportTable);
	
	
	
	public void createPartControl(Composite parent)
	{
		// Create a composite to hold the children
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.FILL_BOTH);
		parent.setLayoutData(gridData);

		aViewer = concernMappingReportTable.createTableViewer(parent);

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
			setPartName(domainAndRelation + " Domain - Concern Mapping Report");
		}
		else
		{
			domainAndRelation = "Default";
			setPartName("Concern Mapping Report");
		}
		
		domainAndRelation += " (" + concernComponentRelation + ")"; 

		this.setTitleToolTip("Concern Mapping Report for " + domainAndRelation);
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
				"Building the Concern Mapping Report for '" +
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
					aViewer.setInput(concernMappingReportTable);
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
					getSite().getPage().hideView(ConcernMappingReportView.this);
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

		@Override
		public IStatus run(IProgressMonitor progressMonitor)
		{
			if (concernModel == null || metricsTool == null)
				return null; // Causes null exception to be thrown
			
			myProgressMonitor = progressMonitor;
			
			// Clear the metrics since we are recalculating them
			concernMappingReportTable.clear();
			
			//ConcernMappingReport concernMappingReport = new ConcernMappingReport(concernModel.getConcernDomain().getName());
			
			int totalNumberOfMethods = 0;
			int totalNumberOfFields = 0;
			int totalNumberOfClasses = 0;

			
//			ISelectionService selectionService = 
//			    Workbench.getInstance().getActiveWorkbenchWindow().getSelectionService();
//
//			ISelection selection = selectionService.getSelection();
//			if(selection instanceof IStructuredSelection) {
//			    Object element = ((IStructuredSelection)selection).getFirstElement();
//			    IProject project;
//			    if (element instanceof IResource) {
//			    	project= ((IResource)element).getProject();
//			    	try {
//						if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
//							IJavaProject javaProject = JavaCore.create(project);
//							IPackageFragment[] packages = javaProject.getPackageFragments();
//							for (IPackageFragment mypackage : packages) {
//								// Package fragments include all packages in the
//								// classpath
//								// We will only look at the package from the source
//								// folder
//								// K_BINARY would include also included JARS, e.g.
//								// rt.jar
//								if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
//									totalNumberOfClasses += mypackage.getCompilationUnits().length;
//									System.out.println("Package " + mypackage.getElementName());
//									for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
//										System.out.println("Source file " + unit.getElementName());
//										//Document doc = new Document(unit.getSource());
//										//System.out.println("Has number of lines: " + doc.getNumberOfLines());
//										IType[] allTypes = unit.getAllTypes();
//										for (IType type : allTypes) {
//											IMethod[] methods = type.getMethods();
//											totalNumberOfMethods += methods.length;
//											IField[] fields = type.getFields();
//											totalNumberOfFields += fields.length;
//										}
//									}
//								}
//							}
//						}
//					} catch (JavaModelException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (CoreException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//			    }
//			}
			
			List<Component> allClassesCovered = metricsTool.getAllClassesCovered();
						
			if (myProgressMonitor != null)
				myProgressMonitor.beginTask("Building the Concern Mapping Report...", allClassesCovered.size());
				
			String classesName = "";
			for(Component c: allClassesCovered)
			{
				if (isCanceled())
					return Status.CANCEL_STATUS;
				//System.err.println("allClasses from Concern Model ==>> " + c.getName());
				classesName += c.getName()+"\n";
//				LCCForComponent lccForComponent = metricsTool.getLCCValue(c);
				
				// Once we are finished, we need to refresh the display.
				// However, this must be done on the UI thread.
				safeRefresh();
				if (myProgressMonitor != null)
					myProgressMonitor.worked(1);				
			}
			ProblemManager.reportInfo("allClasses from Concern Model "+classesName , null);
			
//			int numberMethodsMapped = 0;
//			for(Component m: allMethods)
//			{
//				if (isCanceled())
//					return Status.CANCEL_STATUS;
//				LCCForComponent lccForComponent = metricsTool.getLCCValue(m);
//				//concernMappingReportTable.add(lccForComponent);
//				if(lccForComponent.getMeasurement() > 0)
//					numberMethodsMapped++;
//				// Once we are finished, we need to refresh the display.
//				// However, this must be done on the UI thread.
//				safeRefresh();
//				if (myProgressMonitor != null)
//					myProgressMonitor.worked(1);				
//			}
			
			//Just to show the total of components with at least one concern assignment.
//		   MessageConsole myConsole = findConsole("ConcernTagger Console");
//		   MessageConsoleStream out = myConsole.newMessageStream();
//		   out.println("Number of components mapped: "+numberCompsMapped +
//				   " for "+concernModel.getConcernDomain().getName()+" concern domain.");

//		   getSite().getShell().getDisplay().asyncExec (new Runnable() {
//		        public void run() {
//		            MessageDialog.openWarning(getSite().getShell(),"LCC Counting",("Number of components mapped: "+numberCompsMapped +
//				   " for "+concernModel.getConcernDomain().getName()+" concern domain."));
//		        }
//		    });
			
//			ConcernMappingReportMeasure cmrm = new ConcernMappingReportMeasure(ConcernMappingReportMeasure.CLASSES_COVERED, 
//					allClassesCovered.size(), allClassesCovered.size()/totalNumberOfClasses);
//			concernMappingReportTable.add(cmrm);
//			cmrm = new ConcernMappingReportMeasure(ConcernMappingReportMeasure.METHODS_COVERED, 
//					metricsTool.getAllMethodsCovered().size(), metricsTool.getAllMethodsCovered().size()/totalNumberOfMethods);
//			concernMappingReportTable.add(cmrm);
//			cmrm = new ConcernMappingReportMeasure(ConcernMappingReportMeasure.FIELDS_COVERED, 
//					metricsTool.getAllFieldsCovered().size(), metricsTool.getAllFieldsCovered().size()/totalNumberOfFields);
//			concernMappingReportTable.add(cmrm);

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
