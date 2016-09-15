package edu.columbia.concerns.actions;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;

public class UnassignElementsAction extends MultiElementAction
{
	protected UnassignItemListener clickListener = new UnassignItemListener();
	
	/**
	 * Populate the dynamic menu
	 */ 
	@Override
	protected void fillMenu(Menu parent, List<Concern> concerns)
	{
		assert parent != null;
		assert !parent.isDisposed();

		// Always recreate the Unassign menu since it is small
		for(MenuItem child : parent.getItems())
		{
			assert !child.isDisposed();
			child.dispose();
		}
		
		if (!selectedJavaElements.isEmpty())
		{
			fillMenuRecursive(parent, concerns, selectedJavaElements,
					concernModelProvider.getConcernComponentRelation());

			// If there were concerns in the model, add a separator before the New
			// Concern item
			if (parent.getItemCount() > 0)
			{
				parent.setEnabled(true);
				
				new MenuItem(parent, SWT.SEPARATOR);

				// Add the "Unassign All..." item
				MenuItem lNewConcernItem = new MenuItem(parent, SWT.PUSH);
				lNewConcernItem.addSelectionListener(clickListener);
				lNewConcernItem.setText("Unassign All");
			}
			else
			{
				//parent.setEnabled(false);
			}
		}
		else
		{
			//parent.setEnabled(false);
		}
	}

	/**
	 * Builds the flat 'Unassign' menu
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
			boolean isAssigned = isAssigned(concern, selectedJavaElements, 
					concernComponentRelation);
			
			// For the 'Unassign' menu, only show assigned concerns
			
			if (isAssigned)
			{
				MenuItem lMenuItem = new MenuItem(parent, SWT.PUSH);
				lMenuItem.setData(concern);
				lMenuItem.setText(getConcernNameWithMnemonic(concern, mnemonicsUsed));
				lMenuItem.addSelectionListener(clickListener);
			}
			
			List<Concern> children = concern.getChildren();

			if (!children.isEmpty())
			{
				// The 'Unassign' menu is flat, so we pass in the same
				// parent menu
				
				fillMenuRecursive(parent, children, selectedJavaElements, 
						concernComponentRelation);
			}
			
			children = null; // Helps GC
		}
		
		mnemonicsUsed = null; // Helps GC
	}


	private class UnassignItemListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent pEvent)
		{
			MenuItem menuItemClicked = (MenuItem) pEvent.widget;
			assert menuItemClicked != null; 
			
			Concern targetConcern = null;
			
			Object data = menuItemClicked.getData();
			if (data != null)
			{
				targetConcern = (Concern) data;
				assert targetConcern != null;
			}

			// Use the RemoveMultipleItemsAction for unassigning since
			// it provides a confirmation prompt
			RemoveMultipleItemsAction removeAction = 
				new RemoveMultipleItemsAction(concernModelProvider);
			
			EdgeKind concernComponentRelation = 
				concernModelProvider.getConcernComponentRelation();
			
			for(IJavaElement javaElement : selectedJavaElements)
			{
				if (targetConcern != null)
				{
					// False means, if the element is a type, only unassign
					// the type, not its members
					removeAction.addItemToUnassign(targetConcern, javaElement,
							concernComponentRelation);
				}
				else
				{
					Collection<Concern> assignedConcerns =
						concernModelProvider.getModel().getAssignedConcerns(
								javaElement, concernComponentRelation);

					if (assignedConcerns == null)
						continue;
					
					for(Concern assignedConcern : assignedConcerns)
					{
						// False means, if the element is a type, only unassign
						// the type, not its members
						
						removeAction.addItemToUnassign(assignedConcern, 
								javaElement,
								concernComponentRelation);
					}
				}
			}

			removeAction.run();
		}
	}
}
