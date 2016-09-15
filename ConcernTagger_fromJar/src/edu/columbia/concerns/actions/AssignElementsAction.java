package edu.columbia.concerns.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ConcernJob;

public class AssignElementsAction extends MultiElementAction
{
	protected AssignMenuItemListener clickListener = new AssignMenuItemListener();

	/**
	 * Populate the dynamic menu
	 */ 
	@Override
	protected void fillMenu(Menu parent, List<Concern> concerns)
	{
		assert parent != null;
		assert !parent.isDisposed();

		if (!selectedJavaElements.isEmpty())
		{
			parent.setEnabled(true);
			
			fillMenuRecursive(parent, concerns, selectedJavaElements,
					concernModelProvider.getConcernComponentRelation());

			String assignAllLabel = getNewConcernMenuItemText();
			
			for(MenuItem menuItem : parent.getItems())
			{
				String text = menuItem.getText();
				
				if (text.isEmpty())
				{
					menuItem.dispose();
				}
				else if (text.equals(assignAllLabel))
				{
					menuItem.dispose();
					break;
				}
			}

			// If there were concerns in the model, add a separator before
			// the New Concern item
			
			boolean hasItemsToAssign = parent.getItemCount() > 0;
			
			if (hasItemsToAssign)
			{
				new MenuItem(parent, SWT.SEPARATOR);
			}
			
			// Add the "New concern..." item
			MenuItem lNewConcernItem = new MenuItem(parent, SWT.PUSH);
			lNewConcernItem.addSelectionListener(clickListener);
			lNewConcernItem.setText(assignAllLabel);
		}
		else
		{
			parent.setEnabled(false);
		}
	}

	/**
	 * Builds the cascading 'Assign' menu
	 */
	private void fillMenuRecursive(	Menu parent, 
									List<Concern> concerns,
									List<IJavaElement> selectedJavaElements,
									EdgeKind concernComponentRelation)
	{
		Set<Character> mnemonicsUsed = new HashSet<Character>(); 
		
		for (Concern concern : concerns)
		{
			// See if any of the selected elements are already
			// assigned to the concern
			//boolean isAssigned = isAssigned(concern, selectedJavaElements, 
			//		concernComponentRelation);
			boolean isAssigned = false; // Inaccurate but much faster
			
			List<Concern> children = concern.getChildren();

			MenuItem lMenuItem = null;
			Menu childMenu = null;

			// For the 'Assign' menu, create menu items for all concerns,
			// regardless of whether they are assigned.
			
			// See if we already created the item
			for(MenuItem menuItem : parent.getItems())
			{
				Object data = menuItem.getData();
				if (data != null && 
					data.equals(concern))
				{
					lMenuItem = menuItem;
					childMenu = lMenuItem.getMenu();
					break;
				}
			}

			// Lazily create the concern menu item
			if (lMenuItem == null)
			{
				if (!children.isEmpty())
				{
					lMenuItem = new MenuItem(parent, SWT.CASCADE);
				}
				else
				{
					lMenuItem = new MenuItem(parent, SWT.PUSH);

					// Can't click on cascading menu
					lMenuItem.addSelectionListener(clickListener);
				}

				lMenuItem.setData(concern);
				lMenuItem.setText(getConcernNameWithMnemonic(concern, mnemonicsUsed));
			}

			lMenuItem.setEnabled(!children.isEmpty() || !isAssigned);
			
			if (!children.isEmpty())
			{
				// The 'Assign' menu is hierarchical, so we create
				// a cascading menu for the children
				
				assert lMenuItem != null;
				
				if (childMenu == null)
				{
					childMenu = new Menu(lMenuItem);
				}
				
				fillMenuRecursive(childMenu, children, selectedJavaElements, 
						concernComponentRelation);

				lMenuItem.setMenu(childMenu);
			}
			
			children = null; // Helps GC
		}
		
		mnemonicsUsed = null; // Helps GC
	}

	private class AssignMenuItemListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent pEvent)
		{
			MenuItem menuItemClicked = (MenuItem) pEvent.widget;
			assert menuItemClicked != null; 
			
			Concern targetConcern = null;
			
			Object data = menuItemClicked.getData();
			if (data == null)
			{
				Shell shell;
				if (aJavaEditor != null)
					shell = aJavaEditor.getSite().getShell();
				else
					shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				NewConcernAction nca = new NewConcernAction(shell, 
						concernModelProvider, null);
				nca.run();
				
				targetConcern = nca.getConcernJustAdded();
			}
			else
			{
				targetConcern = (Concern) data;
				assert targetConcern != null;
			}

			ConcernJob job = new ConcernJob("Assigning", concernModelProvider);
			
			EdgeKind concernComponentRelation = 
				concernModelProvider.getConcernComponentRelation();
			
			for(IJavaElement javaElement : selectedJavaElements)
			{
				job.addAssignTask(targetConcern, javaElement, concernComponentRelation);
			}

			job.schedule();
		}
	}
}
