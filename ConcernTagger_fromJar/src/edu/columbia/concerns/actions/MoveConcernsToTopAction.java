package edu.columbia.concerns.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Concern;

public class MoveConcernsToTopAction extends Action
{
	IConcernModelProvider concernModelProvider;
	List<Concern> concerns = new ArrayList<Concern>();
	
	public MoveConcernsToTopAction(IConcernModelProvider concernModelProvider)
	{
		this.concernModelProvider = concernModelProvider;

		setText(ConcernTagger
				.getResourceString("actions.MoveConcernToRootAction.Top.Label"));
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				ConcernTagger.ID_PLUGIN, "icons/arrow_up.png"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.MoveConcernToRootAction.Top.ToolTip"));
	}
	
	public void addConcern(Concern concern)
	{
		concerns.add(concern);
	}
	
	public boolean hasWork()
	{
		return !concerns.isEmpty();
	}

	@Override
	public void run()
	{
		for(Concern concern : concerns)
		{
			concernModelProvider.getModel().getRoot().addChild(concern);
		}
	}

}
