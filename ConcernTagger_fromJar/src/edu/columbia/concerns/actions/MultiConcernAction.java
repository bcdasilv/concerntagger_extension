package edu.columbia.concerns.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.metrics.MetricsView;
import edu.columbia.concerns.ui.ConcernView;
import edu.columbia.concerns.ui.concerntree.ConcernTreeItem;
import edu.columbia.concerns.ui.concerntree.ConcernTreeViewer;
import edu.columbia.concerns.util.ConcernJob;

/**
 * Handles Assign and Unassign initiated by right-clicking one or more
 * concerns in the concern tree. 
 *
 * @author eaddy
 */
public class MultiConcernAction 
	extends Action
	implements ISelectionListener // Listen for selection changes in other views/editors
{
	ConcernTreeViewer concernTreeViewer;
	
	ConcernJob job;

	Set<ConcernTreeItem> selectedConcernItems = new HashSet<ConcernTreeItem>();
	List<IJavaElement> selectedJavaElements = new ArrayList<IJavaElement>();
	List<IJavaElement> javaElementsToUse = null;
	
	StringBuffer label;

	boolean assign;
	
	/**
	 * Creates the action.
	 */
	public MultiConcernAction(ConcernTreeViewer concernTreeViewer, boolean assign)
	{
		this.concernTreeViewer = concernTreeViewer;
		this.assign = assign;

		if (assign)
		{
			label = new StringBuffer(
					ConcernTagger.getResourceString("actions.MultiConcernAction.Assign.Label"));

			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
					ConcernTagger.ID_PLUGIN, "icons/link.png"));
			
			setToolTipText(
					ConcernTagger.getResourceString("actions.MultiConcernAction.Assign.ToolTip"));
		}
		else
		{
			label = new StringBuffer(
					ConcernTagger.getResourceString("actions.MultiConcernAction.Unassign.Label"));

			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
					ConcernTagger.ID_PLUGIN, "icons/link_break.png"));
			
			setToolTipText(
					ConcernTagger.getResourceString("actions.MultiConcernAction.Unassign.ToolTip"));
		}

		job = new ConcernJob(label + "ing...", concernTreeViewer);
		
		updateLabel();
	}

	public void clearConcerns()
	{
		selectedConcernItems.clear();
	}
	
	public void addConcernItem(ConcernTreeItem concernToAssignOrUnassign)
	{
		selectedConcernItems.add(concernToAssignOrUnassign);
	}

	public boolean hasWork()
	{
		return !selectedConcernItems.isEmpty() && hasSelectedJavaElements();
	}
	
	public boolean hasSelectedJavaElements()
	{
		if (javaElementsToUse != null)
			return !javaElementsToUse.isEmpty();
		else
			return !selectedJavaElements.isEmpty();
	}

	public boolean retainOnlyActionableElements()
	{
		javaElementsToUse = new ArrayList<IJavaElement>(selectedJavaElements);
		
		elementPassedTheTest: for(int i = javaElementsToUse.size()-1; i >= 0; --i)
		{
			IJavaElement javaElement = javaElementsToUse.get(i);
			
			for(ConcernTreeItem cti : selectedConcernItems)
			{
				boolean hasAssignment = cti.hasAssignment(javaElement);
				
				if ((assign && !hasAssignment) || (!assign && hasAssignment))
				{
					continue elementPassedTheTest;
				}
			}
			
			javaElementsToUse.remove(i);
		}
		
		updateLabel();
		
		setEnabled(hasWork());
		
		return hasSelectedJavaElements();
	}
	
	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		if (!hasWork())
			return;
		
		if (javaElementsToUse == null)
			javaElementsToUse = selectedJavaElements;
		
		for(ConcernTreeItem selectedConcernItem : selectedConcernItems)
		{
			for(IJavaElement selectedElement : javaElementsToUse)
			{
				if (assign)
				{
					job.addAssignTask(selectedConcernItem.getConcern(), 
							selectedElement, 
							selectedConcernItem.getRelation());
				}
				else
				{
					job.addUnassignTask(selectedConcernItem.getConcern(), 
							selectedElement, 
							selectedConcernItem.getRelation());
				}
			}
		}

		// So we don't try to assign/unassign them again
		javaElementsToUse.clear();
		selectedJavaElements.clear();
		
		if (job.hasWork())
			job.schedule();
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection sel)
	{
		if (part instanceof ConcernView || part instanceof MetricsView)
			return;

		List<IJavaElement> selectedElements = new ArrayList<IJavaElement>();
		boolean ignoreSelection = 
			MultiElementAction.getSelectedJavaElements(part, sel, selectedElements);

		if (ignoreSelection)
			return;
	
		selectedJavaElements = selectedElements;
	}
	
	public void updateLabel()
	{
		StringBuffer buf = new StringBuffer(label);

		List<IJavaElement> elementsToUse;
		if (javaElementsToUse != null)
			elementsToUse = javaElementsToUse;
		else
			elementsToUse = selectedJavaElements;
		
		int count = 0;
		for(IJavaElement javaElement : elementsToUse)
		{
			if (buf.length() > 15)
			{
				// So menu item label doesn't become too long
				buf.append(", ... (+");
				buf.append(elementsToUse.size() - count);
				buf.append(" more)");
				break;
			}
			
			if (count > 0)
				buf.append(',');
			
			buf.append(" \'");
			buf.append(javaElement.getElementName());
			buf.append('\'');
			
			++count;
		}

		setText(buf.toString());
	}
}
