/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.23 $
 */

package edu.columbia.concerns.decorators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.ui.ConcernViewPreferencePage;

/**
 * Decorates elements in Package Explorer, Outline, Type Hierarchy,
 * Search Results, etc.
 */
public class AssignedElementDecorator 
	extends 
		LabelProvider 
	implements
		ILightweightLabelDecorator,
		IConcernListener, 
		IPropertyChangeListener
{
	ConcernTagger concernMapper;
	ConcernModel concernModel;

	Font boldFont = null;
	
	/**
	 * Creates the new label decorator.
	 */
	public AssignedElementDecorator()
	{
		concernMapper = ConcernTagger.singleton();
		concernMapper.getPreferenceStore().addPropertyChangeListener(this);

		// We want to be notified when the active concern model changes
		ConcernModelFactory.singleton().addListener(this);
		
		concernModel = ConcernModelFactory.singleton().getModel();
		concernModel.addListener(this);
	}

	/**
	 * Decorates elements belonging to the concern model in the JDT views.
	 * 
	 * @param pElement
	 *            The element being decorated
	 * @param pDecoration
	 *            The decoration to add to the element's label
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object,
	 *      org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object pElement, IDecoration pDecoration)
	{
		boolean isSuffixEnabled = concernMapper.getPreferenceStore().getBoolean(
				ConcernViewPreferencePage.P_SUFFIX_ENABLED);

		boolean isHighlightingEnabled = concernMapper.getPreferenceStore().getBoolean(
				ConcernViewPreferencePage.P_BOLD_ENABLED);

		if (!isSuffixEnabled && !isHighlightingEnabled)
			return;
		
		if (!(pElement instanceof IJavaElement))
			return;

		IJavaElement javaElement = (IJavaElement) pElement;
		if (/*javaElement.getElementType() != IJavaElement.COMPILATION_UNIT &&*/
			!Component.isJavaElementAssignable(javaElement))
		{
			return;
		}
		
		EdgeKind concernComponentRelation = ConcernModelFactory.singleton().getConcernComponentRelation();
		
		// Get the names of the concerns from the concern model
		Collection<Concern> assignedConcerns = 
			concernModel.getAssignedConcerns(javaElement, concernComponentRelation);

		// add the decorations
		if (isSuffixEnabled && assignedConcerns != null)
		{
			StringBuffer buf = new StringBuffer(" ~ ");
			
			boolean first = true;
			
			for(Concern concern : assignedConcerns)
			{
				if (!first)
					buf.append(", ");
				
				buf.append(concern.getShortDisplayName());
				
				first = false;
			}
			
			pDecoration.addSuffix(buf.toString());
		}

		if (!isHighlightingEnabled)
			return;
		
		if ((assignedConcerns == null || assignedConcerns.size() == 0)/* &&
			!concernModel.areDescendantComponentsAssigned(javaElement.getHandleIdentifier(), 
					concernComponentRelation)*/)
		{
			return;
		}
		
		if (boldFont == null)
			boldFont = PlatformUI.getWorkbench().getThemeManager()
			.getCurrentTheme().getFontRegistry().getBold("Text Font");
		
		pDecoration.setFont(boldFont);
	}

	/**
	 * Gets the ConcernMapper decorator.
	 * 
	 * @return The decorator.
	 */
	public static AssignedElementDecorator getDecorator()
	{
		// Can we use PlatformUI.getWorkbench() instead?
		IDecoratorManager lDecoratorManager = 
			ConcernTagger.singleton().getWorkbench().getDecoratorManager();

		if (lDecoratorManager.getEnabled("ca.mcgill.cs.serg.cm.decorator"))
		{
			return (AssignedElementDecorator) lDecoratorManager
					.getBaseLabelProvider("ca.mcgill.cs.serg.cm.decorator");
		}
		else
		{
			return null;
		}
	}

	/**
	 * Refreshes decorations when a change in the Concern Model is reported.
	 * 
	 * @param type
	 * @see edu.columbia.concerns.model.IConcernListener#modelChanged(int)
	 * @param type
	 *            The type of change to the model. See the constants in
	 *            ConcernModel
	 */
	@Override
	public void modelChanged(ConcernEvent events)
	{
		if (events.isChangedActiveConcernModel())
		{
			this.concernModel.removeListener(this);
			this.concernModel = ConcernModelFactory.singleton().getModel();

			// We want to be notified when any concerns or assignments are
			// changed in the active concern model
			this.concernModel.addListener(this);
		} 
		
		if (	events.isUpdateConcernLabel() || 
				events.isChangedConcernComponentRelation() ||
				events.isChangedActiveConcernModel())
		{
			refresh(null); // Refresh all Java elements
		}
		else
		{
			// Refresh only the elements affected by the (un)assignment
			
			List<Object> changedElements = null;
			
			for(ConcernEvent event : events)
			{
				if (!event.isAssign() && !event.isUnassign())
					continue;
				
				if (changedElements == null)
					changedElements = new ArrayList<Object>();
				
				IJavaElement javaElementAssignedOrUnassigned = event.getJavaElement();
				
				changedElements.add(javaElementAssignedOrUnassigned);
			}

			if (changedElements != null)
				refresh(changedElements.toArray());
		}
	}

	void refresh(Object[] elements)
	{
		Display.getDefault().asyncExec(new RefreshLabelsRunner(elements));
	}
	
	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 * @param pEvent
	 *            the property change event object describing which property
	 *            changed and how
	 */
	public void propertyChange(PropertyChangeEvent pEvent)
	{
		boldFont = null; // User may have changed the font
		refresh(null);
	}

	private final class RefreshLabelsRunner implements Runnable
	{
		AssignedElementDecorator labelProvider;
		Object[] elements;
		
		public RefreshLabelsRunner(Object[] elements)
		{
			this.labelProvider = getDecorator();
			this.elements = elements;
		}
		
		public void run()
		{
			if (labelProvider != null)
			{
				fireLabelProviderChanged(new LabelProviderChangedEvent(labelProvider, 
						elements));
			}
		}
	}
}

