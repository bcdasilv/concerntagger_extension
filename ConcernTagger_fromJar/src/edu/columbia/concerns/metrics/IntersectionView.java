package edu.columbia.concerns.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ISimpleProgressMonitor;

/**
 * View for metrics
 * 
 * @author Marc Eaddy
 * 
 */
public class IntersectionView 
	extends 
		ViewPart
	implements 
		IConcernListener, IConcernModelProviderEx, IRefreshableView
{
	private TableViewer viewer;

	private ConcernModel concernModelLhs;
	private EdgeKind concernComponentRelationLhs = null;

	private ConcernModel concernModelRhs = null;
	private EdgeKind concernComponentRelationRhs = null;
	
	MetricsJob job = null;
	
	IntersectionMetricsTable intersectionMetrics = new IntersectionMetricsTable();

	// Put this after intersectionMetrics above is instantiated!
	private SaveMetricsAction saveAction = new SaveMetricsAction(intersectionMetrics);
	
	@Override
	public void createPartControl(Composite parent)
	{
		// Create a composite to hold the children
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.FILL_BOTH);
		parent.setLayoutData(gridData);

		viewer = intersectionMetrics.createTableViewer(parent);

		// Add elements to the action bars
		IActionBars lBars = getViewSite().getActionBars();
		fillLocalToolBar(lBars.getToolBarManager());
		fillToolBarMenu(lBars.getMenuManager());
		
		String concernDomain = OpenConcernDomainAction.extractConcernDomainFromSecondaryId(
				getViewSite().getSecondaryId());

		setConcernDomain(concernDomain, true);
		setConcernDomain(concernDomain, false);
	}

	public void updateTitleToolTip()
	{
		String lhs = "";
		if (concernModelLhs != null && !concernModelLhs.getConcernDomain().isDefault())
		{
			lhs = concernModelLhs.getConcernDomain().getName() +
				" (Relation: " + concernComponentRelationLhs + ")";
		}
		
		String rhs = "";
		if (concernModelRhs != null && !concernModelRhs.getConcernDomain().isDefault())
		{
			rhs = concernModelRhs.getConcernDomain().getName() +
				" (Relation: " + concernComponentRelationRhs + ")";
		}
		
		String toolTip;
		
		saveAction.setSuggestedPrefix("");
		
		if (!lhs.isEmpty())
		{
			if (!rhs.isEmpty())
			{
				toolTip = "Concern intersection metrics for " + 
						lhs + " and " + rhs;

				saveAction.setSuggestedPrefix(
						lhs.replace("Relation: ", "") + "-" + 
						rhs.replace("Relation: ", ""));
			}
			else
			{
				toolTip = "Concern intersection metrics for " + lhs;
			}
		}
		else
		{
			toolTip = "Concern intersection metrics";
		}
		
		setTitleToolTip(toolTip);
	}
	
	@Override
	public void setConcernDomain(String concernDomain)
	{
		setConcernDomain(concernDomain, true); // Set LHS
	}

	public void setConcernDomain(String concernDomain, boolean left)
	{
		if (left)
		{
			if (concernModelLhs != null && 
				concernModelLhs.getConcernDomain() != null &&
				concernModelLhs.getConcernDomain().equals(concernDomain))
			{
				return;
			}
		}
		else
		{
			if (concernModelRhs != null && 
				concernModelRhs.getConcernDomain() != null &&
				concernModelRhs.getConcernDomain().equals(concernDomain))
			{
				return;
			}
		}
		
		// E.g., if the workspace is "C:\Workspace" then the
		// directory will be
		// "C:\Workspace\.metadata\plugins\columbia.concerntagger"
		IPath workspacePluginDir = ConcernTagger.singleton().getStateLocation();

		// True means create the database if it doesn't exist
		ConcernRepository hsqldb = ConcernRepository.openDatabase(workspacePluginDir.append("db")
				.toOSString(), true);
	
		if (left)
			concernModelLhs = ConcernModelFactory.singleton().getConcernModel(
					hsqldb, concernDomain);
		else
			concernModelRhs = ConcernModelFactory.singleton().getConcernModel(
					hsqldb, concernDomain);

		// Make sure we have a valid relation
		if (left)
		{
			if (concernComponentRelationLhs == null)
				concernComponentRelationLhs = ConcernModelFactory.singleton().getConcernComponentRelation();
		}
		else
		{
			if (concernComponentRelationRhs == null)
				concernComponentRelationRhs = ConcernModelFactory.singleton().getConcernComponentRelation();
		}
		
		if (left)
			concernModelLhs.addListener(this);
		else
			concernModelRhs.addListener(this);

		updateTitleToolTip();
	}
	
	@Override
	public void setConcernComponentRelation(EdgeKind edgeKind)
	{
		concernComponentRelationLhs = edgeKind;
		updateTitleToolTip();
	}

	@Override
	public void setFocus()
	{
		viewer.getControl().setFocus();
	}

	@Override
	public void refresh()
	{
		if (job != null)
			job.cancel();
	
		if (concernModelLhs == null || 
			concernModelRhs == null ||
			concernComponentRelationLhs == null ||
			concernComponentRelationRhs == null)
		{
			// Still waiting for user to select left and right
			// hand concerns to intersect
			return;
		}
		
		// Calculate the metrics in a concurrent worker job
		job = new MetricsJob(
				"Calculating intersection metrics for '" +
				concernModelLhs.getConcernDomain().toString() + "'");
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
		if (viewer == null || 
			viewer.getControl() == null ||
			viewer.getControl().isDisposed() ||
			viewer.getControl().getDisplay() == null ||
			viewer.getControl().getDisplay().isDisposed())
		{
			return null;
		}
		else
		{
			return viewer.getControl().getDisplay();
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
					viewer.setInput(intersectionMetrics);
					viewer.refresh();
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
					getSite().getPage().hideView(IntersectionView.this);
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
			if (concernModelLhs == null)
				return null; // Causes null exception to be thrown
			
			myProgressMonitor = progressMonitor;
			
			// Clear the metrics since we are recalculating them
			intersectionMetrics.clear();
			
			if (myProgressMonitor != null)
				myProgressMonitor.beginTask("Concern", 
						concernModelLhs.getNumConcerns());
			
			IStatus status = processRecursive(concernModelLhs.getRoot());
			
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

				Set<Concern> tangledConcernsRhs = new TreeSet<Concern>();

				// Concerns A is considered tangled with concern B iff any
				// component assigned (directly or indirectly, as determined
				// by the aggregation rules) to A is also assigned (directly
				// or indirectly) to B.						
				
				Set<Component> assignmentsToA = new HashSet<Component>(); 
				concern.getAssignmentsRecursive(concernComponentRelationLhs,
						assignmentsToA);

				// Use a set so we don't check the same component twice
				Set<Component> assignmentsToCheck = new HashSet<Component>();
				
				for(Component assignmentToA : assignmentsToA)
				{
					// Check all direct assignments to A
					assignmentsToCheck.add(assignmentToA);
					
					// All descendants of components directly assigned to A
					// are considered indirectly assigned to A
					assignmentsToCheck.addAll(assignmentToA.getDescendants());

					// Check if any ancestor of the component is directly
					// assigned to B since this would mean the component
					// is indirectly assigned to B, and thus tangled with A
					assignmentsToCheck.addAll(assignmentToA.getAncestors());
				}
				
				for(Component assignmentToCheck : assignmentsToCheck)
				{
					Collection<Concern> tangledConcernsForThisAssignment =
						concernModelRhs.getAssignedConcerns(assignmentToCheck, 
								concernComponentRelationRhs);
					
					if (tangledConcernsForThisAssignment != null)
						tangledConcernsRhs.addAll(tangledConcernsForThisAssignment);
				}

				intersectionMetrics.add(concern, tangledConcernsRhs);

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
		return concernComponentRelationLhs;
	}

	@Override
	public ConcernModel getModel()
	{
		return concernModelLhs;
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
		pManager.add( new LeftRightMenuAction("Left", IntersectionView.this) );
		pManager.add( new LeftRightMenuAction("Right", new RhsListener()) );
	}
	
	class LeftRightMenuAction
		extends Action
		implements IMenuCreator
	{
		private Menu menu = null;
		private IConcernModelProviderEx concernModelListener;
		
		public LeftRightMenuAction(String prefix,
		                           IConcernModelProviderEx concernModelListener)
		{
			this.setText(prefix + "-Hand Side");
			
			this.concernModelListener = concernModelListener;
			
			setMenuCreator(this);
		}
		
		@Override
		public void dispose()
		{ 
			if (menu != null && !menu.isDisposed())
			{
				menu.dispose();
				menu = null;
			}
		}

		@Override
		public Menu getMenu(Control parent)
		{
			return null;
		}

		@Override
		public Menu getMenu(Menu parent)
		{
			dispose();
			
			menu = new Menu(parent);
			fillMenu(menu);
			return menu;
		}
		
		public void fillMenu(Menu menu)
		{
			assert menu != null;
			assert !menu.isDisposed();
			
			for(MenuItem child : menu.getItems())
			{
				assert !child.isDisposed();
				child.dispose();
			}
			
			// V
			//	Left-Hand Side >
			//				Domain >
			//					ECMA Spec
			//					Bugs
			//				Relation >
			//					CONTAINS
			//					RELATED_TO
			//	Right-Hand Side >
			//				Domain >
			//					ECMA Spec
			//					Bugs
			//				Relation >
			//					CONTAINS
			//					RELATED_TO

			//				Domain >
			//					ECMA Spec
			//					Bugs


			// true means disable the currently selected domain
			OpenConcernDomainAction domainMenuAction = 
				new OpenConcernDomainAction(concernModelListener, true);
			
			MenuItem domainMenuItem =  new MenuItem(menu, SWT.CASCADE);
			domainMenuItem.setText("Domain");

			// Create Domain's > and child items
			Menu domainChildMenu = new Menu(domainMenuItem);
			domainMenuItem.setMenu(domainMenuAction.getMenu(domainChildMenu));

			//				Relation >
			//					CONTAINS
			//					RELATED_TO

			// Create Relations' >
			MenuItem relationMenuItem = new MenuItem(menu, SWT.CASCADE);
			relationMenuItem.setText("Relation");
			
			SetConcernComponentRelationAction relationMenuAction = 
				new SetConcernComponentRelationAction(concernModelListener);
			
			// Create Relations' > and child items
			Menu relationChildMenu = new Menu(relationMenuItem);
			relationMenuItem.setMenu(relationMenuAction.getMenu(relationChildMenu));
		}
	}
	
	class RhsListener implements IConcernModelProviderEx
	{
		@Override
		public EdgeKind getConcernComponentRelation()
		{
			return concernComponentRelationRhs;
		}

		@Override
		public void setConcernComponentRelation(EdgeKind edgeKind)
		{
			concernComponentRelationRhs = edgeKind;
			updateTitleToolTip();
		}

		@Override
		public void setConcernDomain(String concernDomain)
		{
			 // Set RHS
			IntersectionView.this.setConcernDomain(concernDomain, false);
		}

		@Override
		public ConcernModel getModel()
		{
			return concernModelRhs;
		}
	}
}
