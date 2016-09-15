package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProviderEx;
import edu.columbia.concerns.repository.EdgeKind;

public class SetConcernComponentRelationAction 
	extends Action 
	implements IMenuCreator
{
	private Menu menu = null;
	private IConcernModelProviderEx concernModelProvider;
	
	public SetConcernComponentRelationAction(
	              IConcernModelProviderEx concernModelProvider)
	{
		this.concernModelProvider = concernModelProvider;
		
		setText(ConcernTagger
				.getResourceString("actions.SetConcernComponentRelationAction.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/link_go.png"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.SetConcernComponentRelationAction.ToolTip"));

		setMenuCreator(this);
	}

	//-----------------------------------------------------
	// IMenuCreator implementation
	//-----------------------------------------------------
	
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
		
		for(EdgeKind edgeKind : EdgeKind.values())
		{
			MenuItem lMenuItem = new MenuItem(menu, SWT.PUSH);
			lMenuItem.setText(edgeKind.name());
			lMenuItem.addSelectionListener( new SetRelationMenuListener() );
			
			if (concernModelProvider.getConcernComponentRelation() == edgeKind)
			{
				lMenuItem.setEnabled(false);
			}
		}
		
		setEnabled(menu.getItems().length > 0);
	}

	private final class SetRelationMenuListener extends SelectionAdapter
	{
		@Override
		public void widgetSelected(SelectionEvent selectionEvent)
		{
			String edgeKindString = ((MenuItem) selectionEvent.widget).getText();

			EdgeKind edgeKind = EdgeKind.valueOfIgnoreCase(edgeKindString);
			
			concernModelProvider.setConcernComponentRelation(edgeKind);

			// Update the enabled/disabled state
			fillMenu(menu);
		}
	}
}
