package edu.columbia.concerns.ui.concerntree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;

import edu.columbia.concerns.actions.MoveConcernsToTopAction;
import edu.columbia.concerns.actions.MultiConcernAction;
import edu.columbia.concerns.actions.NewConcernAction;
import edu.columbia.concerns.actions.RemoveMultipleItemsAction;
import edu.columbia.concerns.actions.RenameConcernAction;
import edu.columbia.concerns.actions.RevealInEditorAction;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;

public class ConcernTreeViewer 
	extends TreeViewer
	implements IConcernModelProvider
{
	private IConcernModelProvider concernModelProvider;
	
	private IViewPart viewPart;

	private IStatusLineManager statusLineManager;
	
	private ConcernTreeLabelProvider labelProvider; 

	private JavaSearchActionGroup javaSearchActions;

	private MultiConcernAction assignAction;
	private MultiConcernAction unassignAction;
	
	/**
	 * Enables highlighting of concerns in concern tree based
	 * on the Java element currently selected in Project Explorer,
	 * Editor, etc.
	 */
	private static final boolean highlightPackageExplorerSelection = true;
	
	public ConcernTreeViewer(	Composite parent, 
								IConcernModelProvider concernModelProvider,
								IViewPart viewPart,
								IViewSite viewSite)
	{
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		this.concernModelProvider = concernModelProvider;
		this.viewPart = viewPart;

		this.statusLineManager = viewSite.getActionBars().getStatusLineManager();
		
		// Specify our layout
		
		GridData lGridData = new GridData();
		lGridData.verticalAlignment = GridData.FILL;
		lGridData.horizontalAlignment = GridData.FILL;
		lGridData.grabExcessHorizontalSpace = true;
		lGridData.grabExcessVerticalSpace = true;

		getControl().setLayoutData(lGridData);

		// Specify a custom content provider that provides a
		// combined tree showing concerns and their assignments
		
		ConcernTreeContentProvider contentProvider = 
			new ConcernTreeContentProvider(concernModelProvider);
		setContentProvider(contentProvider);

		// Specify custom sort order for concerns and assignments
		
		setSorter(new ConcernTreeSorter());
		
		// Specify a custom label provider that can highlight 
		// tree items based on Java elements selected in other
		// views (Package Explorer, Editor, Search Results, etc.)
		
		this.labelProvider = new ConcernTreeLabelProvider(this,
				highlightPackageExplorerSelection, statusLineManager);
		setLabelProvider(labelProvider);

		// Listen for selection changes in Package Explorer, Editor, etc.
		if (highlightPackageExplorerSelection)
			viewPart.getSite().getPage().addSelectionListener(labelProvider);

		// Initialize right-mouse menu item that allows selected items from
		// other views/editors to be assigned to multiple selected concerns
		
		this.assignAction = new MultiConcernAction(this, true);
		this.unassignAction = new MultiConcernAction(this, false);
		
		// Listen for selection changes in Package Explorer, Editor, etc.
		viewPart.getSite().getPage().addSelectionListener(assignAction);
		viewPart.getSite().getPage().addSelectionListener(unassignAction);

		// Broadcast selection changes to other editors, views, etc.
		viewPart.getSite().setSelectionProvider(this);

		// Handle drag-n-drop
		hookDragAndDrop(concernModelProvider);

		// Listen for the DELETE key
		hookDELETEKey(viewSite);

		// Listen for double click events
		hookDoubleClick();

		// Specify our custom right-mouse menu
		hookContextMenu();
	}

	public void init(Composite pParent)
	{
		Composite lBottomFrame = new Composite(pParent, 0);
		lBottomFrame.setLayout(new GridLayout(2, false));
		lBottomFrame.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
				| GridData.GRAB_HORIZONTAL));
	}

	// ----------------------------------------------------
	// IConcernModelProvider implementation
	// ----------------------------------------------------
	
	@Override
	public ConcernModel getModel()
	{
		return concernModelProvider.getModel();
	}
	
	@Override
	public EdgeKind getConcernComponentRelation()
	{
		return concernModelProvider.getConcernComponentRelation();
	}

	// ----------------------------------------------------
	// PUBLIC METHODS
	// ----------------------------------------------------
	
	public List<ConcernTreeItem> getSelectedItems()
	{
		List<ConcernTreeItem> selectedConcernTreeItems = 
			new ArrayList<ConcernTreeItem>();
		
		ISelection selection = getSelection();
		if (!(selection instanceof IStructuredSelection))
			return null;

		IStructuredSelection selectedConcernItems = (IStructuredSelection) selection;

		Iterator selectionIter = selectedConcernItems.iterator();
		while (selectionIter.hasNext())
		{
			selectedConcernTreeItems.add((ConcernTreeItem) selectionIter.next());
		}
		
		return selectedConcernTreeItems;
	}

	public void refresh(ConcernEvent event)
	{
		Display lDisplay = getControl().getDisplay();
		
		// Setting the input must be done asynchronously.
		// see:
		// http://docs.jboss.org/jbosside/cookbook/build/en/html/Example6.html#d0e996
		lDisplay.asyncExec(new RefreshRunner(event));
	}

	public void setFocus()
	{
		getTree().setFocus();
	}

	public void dispose()
	{
		if (highlightPackageExplorerSelection)
			viewPart.getSite().getPage().removeSelectionListener(labelProvider);

		if (javaSearchActions != null)
		{
			javaSearchActions.dispose();
			javaSearchActions = null;
		}

		if (labelProvider != null)
		{
			labelProvider.dispose();
			labelProvider = null;
		}
	}
	
	// ----------------------------------------------------
	// PRIVATE HELPER METHODS
	// ----------------------------------------------------

	private void hookDragAndDrop(IConcernModelProvider concernModelProvider)
	{
		// Initializes the tree viewer to support drop actions

		int lOps = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] lTransfers = 
			new Transfer[] { LocalSelectionTransfer.getInstance() };

		addDropSupport(lOps, lTransfers, 
				new ConcernTreeDropAdapter(concernModelProvider, this));
		
		// Permit any ConcernTreeItem to be a drag source
		addDragSupport(lOps, lTransfers, new DragSourceAdapter());

		// Listen for our own selection changes
		
		this.addSelectionChangedListener( new ISelectionChangedListener()
			{
				@Override
				public void selectionChanged(SelectionChangedEvent event)
				{
					// LocalSelectionTransfer is a kind of 'data channel' used
					// by all the views to pass selected objects from the view
					// to the drop target.
					
					// Update the data channel with the currently selected objects
					LocalSelectionTransfer.getInstance().setSelection(getSelection());

					List<ConcernTreeItem> selectedItems = getSelectedItems();
					statusLineManager.setMessage(selectedItems.size() + 
							" concern tree item" +
							(selectedItems.size() == 1 ? "" : "s") +
							" selected");

				}
			}
		);
	}
	
	private void hookDELETEKey(IViewSite viewSite)
	{
		IActionBars actionBars = viewSite.getActionBars();
		actionBars.setGlobalActionHandler(
			ActionFactory.DELETE.getId(),
			new Action()
			{
				@Override
				public void run()
				{
					RemoveMultipleItemsAction removeAction = new RemoveMultipleItemsAction(
							ConcernTreeViewer.this);
					removeAction.addItemsToRemove(getSelectedItems());
					removeAction.run();
				}
			}
		);	
	}
	
	private void hookDoubleClick()
	{
		addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent pEvent)
			{
				List<ConcernTreeItem> selectedConcernItems = getSelectedItems();

				if (selectedConcernItems.size() != 1)
					return; // Even possible?

				IJavaElement javaElement = selectedConcernItems.get(0).getJavaElement();

				if (javaElement != null)
					(new RevealInEditorAction(javaElement)).run();
			}
		});
	}

	/**
	 * Registers the context menu on the view.
	 */
	private void hookContextMenu()
	{
		MenuManager lMenuManager = new MenuManager("#PopupMenu");
		lMenuManager.setRemoveAllWhenShown(true);
		lMenuManager.addMenuListener(new IMenuListener()
			{
				public void menuAboutToShow(IMenuManager pManager)
				{
					fillContextMenu(pManager);
				}
			}
		);

		Menu lMenu = lMenuManager.createContextMenu(getControl());
		getControl().setMenu(lMenu);
		viewPart.getSite().registerContextMenu(lMenuManager, this);
	}
	
	/**
	 * Fills the context menu based on the type of selection.
	 * 
	 * @param pManager
	 */
	private void fillContextMenu(IMenuManager pManager)
	{
		List<ConcernTreeItem> selectedConcernItems = getSelectedItems();

		boolean allSelectedItemsAreConcerns = !selectedConcernItems.isEmpty();

		for(ConcernTreeItem item : selectedConcernItems)
		{
			if (item.getJavaElement() != null)
			{
				allSelectedItemsAreConcerns = false;
				break;
			}
		}

		addMenuItem_AssignUnassign(	pManager, selectedConcernItems, allSelectedItemsAreConcerns);
		addMenuItem_NewConcern(		pManager, selectedConcernItems, allSelectedItemsAreConcerns);
		addMenuItem_MoveToTop(		pManager, selectedConcernItems, allSelectedItemsAreConcerns);
		addMenuItem_Rename(			pManager, selectedConcernItems, allSelectedItemsAreConcerns);
		addMenuItem_JavaElements(	pManager, selectedConcernItems, allSelectedItemsAreConcerns);
		addMenuItem_Remove(			pManager, selectedConcernItems);
		
		// What is this for?
		pManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void addMenuItem_AssignUnassign(	IMenuManager pManager, 
	                                        	List<ConcernTreeItem> selectedConcernItems, 
	                                        	boolean allSelectedItemsAreConcerns)
	{
		// Can only assign to concern items
		if (!allSelectedItemsAreConcerns || selectedConcernItems.size() == 0)
			return;

		assignAction.clearConcerns();
		
		for(ConcernTreeItem item : selectedConcernItems)
		{
			assert item.getJavaElement() == null;
			assignAction.addConcernItem(item);
		}
		
		// Make sure the user has selected some Java elements in
		// Package Explorer, Editor, etc.
		assignAction.retainOnlyActionableElements();

		pManager.add(assignAction);

		unassignAction.clearConcerns();
		
		for(ConcernTreeItem item : selectedConcernItems)
		{
			assert item.getJavaElement() == null;
			unassignAction.addConcernItem(item);
		}
		
		// Make sure the user has selected some Java elements in
		// Package Explorer, Editor, etc.
		unassignAction.retainOnlyActionableElements();

		pManager.add(unassignAction);
	}

	private void addMenuItem_NewConcern(IMenuManager pManager, 
	                                    List<ConcernTreeItem> selectedConcernItems,
	                                    boolean allSelectedItemsAreConcerns)
	{
		if (selectedConcernItems.size() == 0)
		{
			// User right-clicked on empty space in the tree 
			pManager.add( new NewConcernAction(this.getTree().getShell(), 
					this, null ) );
		}
		else if (allSelectedItemsAreConcerns && selectedConcernItems.size() == 1)
		{
			// User right-clicked on a concern
			Concern selectedConcern = selectedConcernItems.get(0).getConcern(); 
			pManager.add( new NewConcernAction(this.getTree().getShell(), 
					this,  selectedConcern) );
		}
	}

	private void addMenuItem_MoveToTop( IMenuManager pManager, 
	                                    List<ConcernTreeItem> selectedConcernItems,
	                                    boolean allSelectedItemsAreConcerns)
	{
		if (!allSelectedItemsAreConcerns || selectedConcernItems.isEmpty())
			return; // No items to move to the top

		MoveConcernsToTopAction action = new MoveConcernsToTopAction(this);
		
		for(ConcernTreeItem cti : selectedConcernItems)
		{
			Concern concern = cti.getConcern();
			
			if (cti.getJavaElement() == null && !concern.getParent().isRoot())
				action.addConcern(concern);
		}

		action.setEnabled(action.hasWork());
		
		pManager.add(action);
	}
	
	private void addMenuItem_Remove(IMenuManager pManager, 
	                                List<ConcernTreeItem> selectedConcernItems)
	{
		if (selectedConcernItems.size() == 0)
			return; // No items to remove

		RemoveMultipleItemsAction removeAction = new RemoveMultipleItemsAction(concernModelProvider);
		removeAction.addItemsToRemove(selectedConcernItems);
		pManager.add(removeAction);
	}

	private void addMenuItem_Rename(IMenuManager pManager, 
	                                      List<ConcernTreeItem> selectedConcernItems,
	                                      boolean allSelectedItemsAreConcerns)
	{
		// Can only rename a single selected concern
		if (!allSelectedItemsAreConcerns || selectedConcernItems.size() != 1)
			return;

		ConcernTreeItem cti = selectedConcernItems.get(0);

		pManager.add(new RenameConcernAction(this, cti.getConcern()));
	}

	private void addMenuItem_JavaElements(IMenuManager pManager, 
	                                      List<ConcernTreeItem> selectedConcernItems,
	                                      boolean allSelectedItemsAreConcerns)
	{
		if (javaSearchActions != null)
		{
			javaSearchActions.dispose();
			javaSearchActions = null;
		}
		
		if (allSelectedItemsAreConcerns || selectedConcernItems.size() != 1)
			return;

		ConcernTreeItem cti = selectedConcernItems.get(0);

		IJavaElement javaElement = cti.getJavaElement();
		
		// We provide the context menu for searching
		
		javaSearchActions = new JavaSearchActionGroup(viewPart);
		
		javaSearchActions.setContext(new ActionContext(
				new StructuredSelection(javaElement)));
		GroupMarker lSearchGroup = new GroupMarker("group.search");
		pManager.add(lSearchGroup);
		javaSearchActions.fillContextMenu(pManager);
		javaSearchActions.setContext(null);
		pManager.remove(lSearchGroup);

		// They selected a single type or member
		pManager.add(new RevealInEditorAction(javaElement));
	}

	private final class RefreshRunner implements Runnable
	{
		ConcernEvent events;
		
		public RefreshRunner(ConcernEvent events)
		{
			this.events = events;
		}
		
		public void run()
		{
			boolean debug = true;
			
			if (getControl() == null || getControl().isDisposed())
				return;
			
			// make sure the tree still exists
			if (getInput() == null)
			{
				setInput(concernModelProvider);
			}
			else if (events.isChangedConcernComponentRelation() ||
					events.isChangedAllConcerns() ||
					(events.getConcern() != null && events.getConcern().isRoot()))
			{
				// The currently selected item may have been removed
				// so clear it
				ConcernTreeViewer.super.setSelection((ISelection) null);
				
				// Force a redraw of the tree and labels
				ConcernTreeViewer.super.refresh();
			}
			else
			{
				if (debug)
					System.out.println("Refreshing concern tree... " + 
							concernModelProvider.getConcernComponentRelation());
				
				// The currently selected item may have been removed
				// so clear it
				//ConcernTreeViewer.super.setSelection((ISelection) null);

				Set<Widget> widgetsToRefresh = new HashSet<Widget>();
				Set<Widget> widgetsToRecreate = new HashSet<Widget>();

				for(ConcernEvent event : events)
				{
					for(Widget widget : findItems(event))
					{
						ConcernTreeItem cti = (ConcernTreeItem) widget.getData();
						
						if (	event.isAssign() || 
								event.isUnassign() || 
								event.isChangedConcernChildren())
						{
							gatherWidgetsAffectedByAssignmentOrMove(cti, 
									widgetsToRecreate, widgetsToRefresh);
						}
						else if (event.isUpdateConcernLabel() || 
								event.isUpdateElementLabel())
						{
							// The concern was renamed or we need to update
							// the highlighting of a concern or element item
							gatherWidgetsAffectedByLabelUpdate(cti, 
									widgetsToRefresh);
						}
					}
				}

				// Recreate and refresh the tree items
				
				for(Widget widgetToRecreate : widgetsToRecreate)
				{
					// Ignore a recreation request if an item above this
					// one is recreated since all the children will be recreated
					if (!hasAncestor(widgetToRecreate, widgetsToRecreate, false))
					{
						if (debug)
							System.out.println("Recreating: " + 
									widgetToRecreate.getData() + 
									" " + getConcernComponentRelation());
						
						internalRefresh(widgetToRecreate, widgetToRecreate.getData(), 
								true, true);
					}
				}
				
				for(Widget widgetToRefresh : widgetsToRefresh)
				{
					// Whereas recreating also recreates all children, refreshing
					// only refreshes the current item.  However, if an item
					// above this one is recreated then the label will already
					// be refreshed.
					if (!hasAncestor(widgetToRefresh, widgetsToRecreate, true))
					{
						if (debug)
							System.out.println("Refreshing: " + 
									widgetToRefresh.getData() + 
									" " + getConcernComponentRelation());
						
						doUpdateItem((Item) widgetToRefresh, widgetToRefresh.getData());
					}
				}
			}
		}

		private boolean hasAncestor(Widget descendant, Set<Widget> set, boolean checkSelf)
		{
			Item parent;
			
			if (checkSelf)
				parent = (Item) descendant;
			else
				parent = ConcernTreeViewer.this.getParentItem((Item) descendant);
			
			while (parent != null)
			{
				if (set.contains(parent))
					return true;

				parent = ConcernTreeViewer.this.getParentItem(parent);
			}
			
			return false;
		}
		
		private void gatherWidgetsAffectedByAssignmentOrMove(	
		                                ConcernTreeItem cti, 
		                                Set<Widget> itemsToRecreate,
		                                Set<Widget> itemsToRefresh)
		{
			if (cti.getJavaElement() != null)
			{
				// We only recreate concern items
				cti = cti.getParentConcernItem();
				assert cti.getJavaElement() == null;
			}

			Widget widget = findItem(cti);
			assert widget != null;
			
			// Assignments have changed so we need to recreate the
			// concern item (i.e, refetch the assignments)
			itemsToRecreate.add(widget);
			
			// Every item from here to the top level needs its
			// label refreshed
			gatherWidgetsAffectedByLabelUpdate(cti.getParent(), itemsToRefresh);
		}

		private void gatherWidgetsAffectedByLabelUpdate(ConcernTreeItem cti, 
		                                 Set<Widget> itemsToRefresh)
		{
			if (cti == null)
				return;
			
			Widget widget = findItem(cti);
			
			// Every item from here to the top level needs its
			// label refreshed
			
			do
			{
				itemsToRefresh.add(widget);
				
				cti = cti.getParent();
				widget = findItem(cti);

			} while (cti != null);
		}
	}
}
