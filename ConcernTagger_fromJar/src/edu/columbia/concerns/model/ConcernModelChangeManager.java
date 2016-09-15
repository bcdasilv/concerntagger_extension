package edu.columbia.concerns.model;

import java.util.ArrayList;
import java.util.List;

public class ConcernModelChangeManager
{
	private int areNotificationsDisabled = 0;
	private List<IConcernListener> aListeners =
		new ArrayList<IConcernListener>();

	ConcernEvent queuedEvents = null;
	
	public void disableNotifications()
	{
		++areNotificationsDisabled;
	}

	public void clearQueuedEvents()
	{
		queuedEvents = null;
	}
	
	/**
	 */
	public void enableNotifications()
	{
		assert areNotificationsDisabled > 0;
		
		--areNotificationsDisabled;
		
		if (areNotificationsDisabled == 0 && queuedEvents != null)
		{
			modelChanged(queuedEvents);
			clearQueuedEvents();
		}
	}
	
	/**
	 * Notifies all observers of a change in the model.
	 * 
	 * @param pChange
	 *            The type of change. See the constants in
	 *            ConcernModelChangeListener.
	 */
	public void modelChanged(ConcernEvent event)
	{
		if (areNotificationsDisabled > 0)
		{
			if (queuedEvents == null)
				queuedEvents = new ConcernEvent();
			
			queuedEvents.addEvent(event);
			return;
		}
	
		for (IConcernListener lListener : aListeners)
		{
			lListener.modelChanged(event);
		}
	}

	/**
	 * Adds a listener to the list.
	 * 
	 * @param pListener
	 *            The listener to add.
	 */
	public void addListener(IConcernListener pListener)
	{
		aListeners.add(pListener);
	}

	/**
	 * Removes a Listener from the list.
	 * 
	 * @param pListener
	 *            The listener to remove.
	 */
	public void removeListener(IConcernListener pListener)
	{
		aListeners.remove(pListener);
	}
}
