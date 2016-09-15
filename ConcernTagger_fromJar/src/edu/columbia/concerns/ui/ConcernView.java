/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.49 $
 */

package edu.columbia.concerns.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.actions.CheckConsistencyAction;
import edu.columbia.concerns.actions.CollapseAllAction;
import edu.columbia.concerns.actions.LoadAssignmentsAction;
import edu.columbia.concerns.actions.LoadConcernsAction;
import edu.columbia.concerns.actions.NewConcernAction;
import edu.columbia.concerns.actions.NewConcernDomainAction;
import edu.columbia.concerns.actions.OpenConcernDomainAction;
import edu.columbia.concerns.actions.RemoveAllAssignmentsAction;
import edu.columbia.concerns.actions.RemoveAllConcernsAction;
import edu.columbia.concerns.actions.RenameConcernDomainAction;
import edu.columbia.concerns.actions.ResetDatabaseAction;
import edu.columbia.concerns.actions.SetConcernComponentRelationAction;
import edu.columbia.concerns.actions.ShowMetricsAction;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.model.IConcernModelProviderEx;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.ui.concerntree.ConcernTreeViewer;

/**
 * Implements a view of the Concern model associated with the ConcernMapper
 * plug-in.
 */
public class ConcernView 
	extends 
		ViewPart 
	implements
		IConcernListener, IConcernModelProviderEx, IPropertyChangeListener
{
	private ConcernTreeViewer aViewer;
	//private FilterAction aFilterAction;
	private EdgeKind concernComponentRelation = EdgeKind.CONTAINS;
	private ConcernModel concernModel;

	/**
	 * This is a callback that will allow us to create the aViewer and
	 * initialize it.
	 * 
	 * @param pParent
	 *            The parent widget.
	 */
	@Override
	public void createPartControl(Composite pParent)
	{
		String concernDomain = OpenConcernDomainAction.extractConcernDomainFromSecondaryId(
				getViewSite().getSecondaryId());

		concernModel = ConcernModelFactory.singleton().getConcernModel(concernDomain);
		
		updateTitleAndToolTip();

		GridLayout lLayout = new GridLayout();
		lLayout.numColumns = 1;
		lLayout.horizontalSpacing = 0; // remove if not needed
		lLayout.verticalSpacing = 0; // remove if not needed
		lLayout.marginHeight = 0; // remove if not needed
		lLayout.marginWidth = 0; // remove if not needed
		pParent.setLayout(lLayout);
		pParent.setLayoutData(new GridData(GridData.FILL_BOTH));

		aViewer = new ConcernTreeViewer(pParent, this, this, getViewSite());
		aViewer.init(pParent);

		// Add elements to the action bars
		IActionBars lBars = getViewSite().getActionBars();
		fillLocalToolBar(lBars.getToolBarManager());
		fillToolBarMenu(lBars.getMenuManager());

		// Add this view as a listener for model and property change events
		concernModel.addListener(this);
		ConcernTagger.singleton().getPreferenceStore().addPropertyChangeListener(this);

		// Artificial call to refresh the view
		modelChanged(ConcernEvent.createAllConcernsChanged());
	}

	public void updateTitleAndToolTip()
	{
		if (concernModel.getConcernDomain().isDefault())
		{
			this.setPartName("Concerns");
			this.setTitleToolTip("Concerns (Relation: " + concernComponentRelation + ")");
		}
		else
		{
			this.setPartName(concernModel.getConcernDomain().getName());
			this.setTitleToolTip(concernModel.getConcernDomain().getName() + 
				" Concerns (Relation: " + concernComponentRelation + ")");
		}
	}
	
	@Override
	public void setConcernDomain(String concernDomain)
	{
		OpenConcernDomainAction.openConcernDomainHelper(getViewSite(), concernDomain);
	}
	
	public void setConcernComponentRelation(EdgeKind edgeKind)
	{
		if (concernComponentRelation == edgeKind)
			return;
		
		concernComponentRelation = edgeKind;

		updateTitleAndToolTip();
		
		ConcernEvent event = ConcernEvent.createConcernComponentRelationChangedEvent();
		
		modelChanged(event);
		ConcernModelFactory.singleton().modelChanged(event);
	}

	public EdgeKind getConcernComponentRelation()
	{
		return concernComponentRelation;
	}
	
	public ConcernModel getModel()
	{
		return concernModel;
	}
	
	/**
	 * Collapses the element tree.
	 */
	public void collapseAll()
	{
		aViewer.collapseAll();
	}

	/**
	 * @see edu.columbia.concerns.model.IConcernListener#modelChanged(int)
	 * @param pType
	 *            The type of change to the model. See the constants in
	 *            ConcernModel
	 */
	public void modelChanged(ConcernEvent event)
	{
		if (event.isChangedDomainName())
		{
			// Renaming our concern domain is a big deal since is stored in
			// the view's secondary id, which is used by Eclipse to restore
			// views on opening.  We have to 'restart' the view by creating
			// a new one and closing the old one.
			
			OpenConcernDomainAction.openConcernDomainHelper(getSite(), 
					concernModel.getConcernDomain().getName());
			
			closeMe();
			return;
		}		

		boolean updateActionState = false;

		Display lDisplay = aViewer.getControl().getDisplay();
		
		if (aViewer.getControl().isDisposed() || lDisplay.isDisposed())
		{
			updateActionState = true;
		}

		aViewer.refresh(event);
		
		// Updates the action buttons to reflect the state of the plugin
		if (updateActionState)
			getViewSite().getActionBars().updateActionBars();
	}

	// Is there a better way?
	public void closeMe()
	{
		Display lDisplay = aViewer.getControl().getDisplay();
		
		lDisplay.asyncExec(new Runnable()
			{
				public void run()
				{
					getSite().getPage().hideView(ConcernView.this);
				}
			}
		);
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 * @param pEvent
	 *            the property change event object describing which property
	 *            changed and how
	 */
	public void propertyChange(PropertyChangeEvent pEvent)
	{
		// No properties currently affect the way concerns are viewed
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus()
	{
		aViewer.setFocus();
		ConcernModelFactory.singleton().setActiveConcernModelProvider(this);
	}

	/**
	 * Adds the action to the toolbar.
	 * 
	 * @param pManager
	 *            The toolbar manager.
	 */
	private void fillLocalToolBar(IToolBarManager pManager)
	{
		pManager.add(new NewConcernAction(aViewer.getTree().getShell(), 
				aViewer, null ));
		pManager.add(new Separator());
		pManager.add(new CollapseAllAction(this));
	}

	/**
	 * Adds the actions to the menu.
	 * 
	 * @param pManager
	 *            the menu manager.
	 */
	private void fillToolBarMenu(IMenuManager pManager)
	{
		IStatusLineManager statusLineManager = 
			getViewSite().getActionBars().getStatusLineManager();
		
		// Don't disable the same concern domain since we might want to open
		// a new view
		pManager.add( new OpenConcernDomainAction(this, false) );
		
		pManager.add( new NewConcernDomainAction(this, getSite()) );
		pManager.add( new RenameConcernDomainAction(this, getViewSite() ));
		pManager.add( new Separator() );
		pManager.add( new LoadConcernsAction(this, statusLineManager) );
		pManager.add( new LoadAssignmentsAction(this, statusLineManager) );
		pManager.add( new SetConcernComponentRelationAction(this) );
		pManager.add( new Separator() );
		pManager.add( new CheckConsistencyAction(this, statusLineManager) );
		pManager.add( new ShowMetricsAction(concernModel.getConcernDomain(), getSite()) );
		pManager.add( new Separator() );
		pManager.add( new RemoveAllConcernsAction(this, statusLineManager) );
		pManager.add( new RemoveAllAssignmentsAction(this, statusLineManager) );
		pManager.add( new ResetDatabaseAction(this) );
	}

	/**
	 * Called when the view is closed. Deregister the view as a listener to the
	 * model.
	 */
	@Override
	public void dispose()
	{
		if (concernModel != null)
			concernModel.removeListener(this);
		
		ConcernTagger.singleton().getPreferenceStore()
				.removePropertyChangeListener(this);

		if (aViewer != null)
			aViewer.dispose();
		
		ConcernModelFactory.singleton().clearActiveConcernModelProvider(concernModel);
		super.dispose();
	}
}
