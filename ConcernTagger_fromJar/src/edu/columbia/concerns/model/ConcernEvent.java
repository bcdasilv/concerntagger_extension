package edu.columbia.concerns.model;

import java.util.Iterator;

import org.eclipse.jdt.core.IJavaElement;

import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;

public class ConcernEvent implements Iterable<ConcernEvent>
{
	private enum Reason
	{
		ASSIGNED,
		UNASSIGNED,
		UPDATE_LABEL,
		REMOVED_ELEMENT,
		CHANGED_CONCERN_CHILDREN,
		CHANGED_ALL_CONCERNS,
		CHANGED_DOMAIN_NAME,
		CHANGED_ACTIVE_CONCERN_MODEL,
		CHANGED_CONCERN_COMPONENT_RELATION,
	};
	
	Reason reason = null;
	Concern concern = null;
	IJavaElement element = null;
	EdgeKind concernComponentRelation = null;

	ConcernEvent next = null;

	public ConcernEvent()
	{ }
	
	private ConcernEvent getFreshEvent(Reason reason)
	{
		if (this.reason == null)
		{
			this.reason = reason; 
			return this;
		}
		else
		{
			if (next == null)
				next = new ConcernEvent();

			return next.getFreshEvent(reason);
		}
	}
	
	public static ConcernEvent createAssignEvent(	Concern concern, 
	                                                  	IJavaElement element,
	                                                  	EdgeKind concernComponentRelation)
	{
		ConcernEvent event = new ConcernEvent();
		return event.addAssignEvent(concern, element, concernComponentRelation);
	}
	
	public static ConcernEvent createUnassignEvent(	Concern concern, 
	                                                    	IJavaElement element,
	                                                    	EdgeKind concernComponentRelation)
	{
		ConcernEvent event = new ConcernEvent();
		return event.addUnassignEvent(concern, element, concernComponentRelation);
	}
	
	public static ConcernEvent createRemovalEvent(IJavaElement element)
	{
		ConcernEvent event = new ConcernEvent();
		return event.addRemovalEvent(element);
	}

	public static ConcernEvent createUpdateLabelEvent(Concern concern)
	{
		ConcernEvent event = new ConcernEvent();
		return event.addUpdateLabelEvent(concern);
	}

	public static ConcernEvent createUpdateLabelEvent(IJavaElement element)
	{
		ConcernEvent event = new ConcernEvent();
		return event.addUpdateLabelEvent(element);
	}
	
	public static ConcernEvent createChildrenChangedEvent(Concern concern)
	{
		ConcernEvent event = new ConcernEvent();
		return event.addChildrenChangedEvent(concern);
	}

	public static ConcernEvent createAllConcernsChanged()
	{
		ConcernEvent event = new ConcernEvent();
		return event.addAllConcernsChangedEvent();
	}

	public static ConcernEvent createDomainNameChangedEvent()
	{
		ConcernEvent event = new ConcernEvent();
		return event.addDomainNameChangedEvent();
	}

	public static ConcernEvent createActiveConcernModelChangedEvent()
	{
		ConcernEvent event = new ConcernEvent();
		return event.addActiveConcernModelChangedEvent();
	}

	public static ConcernEvent createConcernComponentRelationChangedEvent()
	{
		ConcernEvent event = new ConcernEvent();
		return event.addConcernComponentRelationChangedEvent();
	}
	
	public ConcernEvent addEvent(ConcernEvent eventToAdd)
	{
		ConcernEvent event = getFreshEvent(eventToAdd.reason);
		event.concern = eventToAdd.concern;
		event.element = eventToAdd.element;
		event.concernComponentRelation = eventToAdd.concernComponentRelation;
		event.next = eventToAdd.next;
		return event;
	}

	public ConcernEvent addAssignEvent(	Concern concern, 
	                            IJavaElement element,
	                            EdgeKind concernComponentRelation)
	{
		ConcernEvent event = getFreshEvent(Reason.ASSIGNED);
		event.concern = concern;
		event.element = element;
		event.concernComponentRelation = concernComponentRelation;
		return event;
	}

	public ConcernEvent addUnassignEvent(Concern concern, 
	                             IJavaElement element,
	                             EdgeKind concernComponentRelation)
	{
		ConcernEvent event = getFreshEvent(Reason.UNASSIGNED);
		event.concern = concern;
		event.element = element;
		event.concernComponentRelation = concernComponentRelation;
		return event;
	}
	
	public ConcernEvent addRemovalEvent(IJavaElement element)
	{
		ConcernEvent event = getFreshEvent(Reason.REMOVED_ELEMENT);
		event.element = element;
		return event;
	}

	public ConcernEvent addUpdateLabelEvent(Object o)
	{
		if (o instanceof Concern)
		{
			return addUpdateLabelEvent((Concern) o);
		}
		else if (o instanceof IJavaElement)
		{
			return addUpdateLabelEvent((IJavaElement) o);
		}
		else
		{
			assert false;
			return null; 
		}
	}
	
	public ConcernEvent addUpdateLabelEvent(Concern concern)
	{
		ConcernEvent event = getFreshEvent(Reason.UPDATE_LABEL);
		event.concern = concern;
		return event;
	}

	public ConcernEvent addUpdateLabelEvent(IJavaElement element)
	{
		ConcernEvent event = getFreshEvent(Reason.UPDATE_LABEL);
		event.element = element;
		return event;
	}
	
	public ConcernEvent addChildrenChangedEvent(Concern concern)
	{
		ConcernEvent event = getFreshEvent(Reason.CHANGED_CONCERN_CHILDREN);
		event.concern = concern;
		return event;
	}

	public ConcernEvent addAllConcernsChangedEvent()
	{
		return getFreshEvent(Reason.CHANGED_ALL_CONCERNS);
	}

	public ConcernEvent addDomainNameChangedEvent()
	{
		return getFreshEvent(Reason.CHANGED_DOMAIN_NAME);
	}

	public ConcernEvent addActiveConcernModelChangedEvent()
	{
		return getFreshEvent(Reason.CHANGED_ACTIVE_CONCERN_MODEL);
	}

	public ConcernEvent addConcernComponentRelationChangedEvent()
	{
		return getFreshEvent(Reason.CHANGED_CONCERN_COMPONENT_RELATION);
	}
	
	public Concern getConcern()
	{
		return concern;
	}

	public IJavaElement getJavaElement()
	{
		return element;
	}
	
	public EdgeKind getRelation()
	{
		return concernComponentRelation;
	}
	
	public boolean isAssign()
	{
		return reason == Reason.ASSIGNED;
	}

	public boolean isUnassign()
	{
		return reason == Reason.UNASSIGNED;
	}

	public boolean isUpdateLabel()
	{
		return reason == Reason.UPDATE_LABEL;
	}

	public boolean isUpdateConcernLabel()
	{
		return isUpdateLabel() && concern != null;
	}

	public boolean isUpdateElementLabel()
	{
		return isUpdateLabel() && element != null;
	}
	
	public boolean isChangedConcernChildren()
	{
		return reason == Reason.CHANGED_CONCERN_CHILDREN;
	}

	public boolean isChangedAllConcerns()
	{
		return reason == Reason.CHANGED_ALL_CONCERNS;
	}

	public boolean isChangedDomainName()
	{
		return reason == Reason.CHANGED_DOMAIN_NAME;
	}

	public boolean isChangedActiveConcernModel()
	{
		return reason == Reason.CHANGED_ACTIVE_CONCERN_MODEL;
	}

	public boolean isChangedConcernComponentRelation()
	{
		return reason == Reason.CHANGED_CONCERN_COMPONENT_RELATION;
	}

	@Override
	public Iterator<ConcernEvent> iterator()
	{
		return new ConcernModelEventIterator(this);
	}
	
	@Override
	public String toString()
	{
		if (element != null && concern != null)
			return element.getElementName() + " -> " + concern.getDisplayName();
		else if (element != null)
			return element.getElementName();
		else
			return concern.getDisplayName();
	}
}

class ConcernModelEventIterator implements Iterator<ConcernEvent>
{
	ConcernEvent cursor;
	
	public ConcernModelEventIterator(ConcernEvent start)
	{
		cursor = start;
	}
	
	@Override
	public boolean hasNext()
	{
		return cursor != null;
	}

	@Override
	public ConcernEvent next()
	{
		ConcernEvent result = cursor;
		cursor = cursor.next;
		return result;
	}

	@Override
	public void remove()
	{
		assert false; // Not supported
	}
}
