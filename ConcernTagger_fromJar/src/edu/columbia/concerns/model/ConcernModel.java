/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.24 $
 */

package edu.columbia.concerns.model;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;

import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.ComponentKind;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.ConcernDomain;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ProblemManager;

/**
 * A Concern Model represents a collection of concerns. This class also
 * implements the Observable role of the Observer design pattern and a Facade to
 * use the concern model. It uses the database as the underlying model.
 */
public class ConcernModel 
	extends 
		ConcernModelChangeManager 
	implements 
		IConcernListener
{
	private ConcernDomain concernDomain = null;

	// reference to the database class
	private ConcernRepository repository;

	/**
	 * Creates a new, empty concern model.
	 */
	public ConcernModel(ConcernRepository hsqldb, String concernDomainName)
	{
		this.repository = hsqldb;
		initConcernDomain(concernDomainName);
	}

	public void initConcernDomain(String concernDomainName)
	{
		if (concernDomainName == null)
		{
			// Get the default domain
			concernDomain = repository.getConcernDomain(ConcernRepository.DEFAULT_CONCERN_DOMAIN_NAME, 
					this);
			
			if (concernDomain == null)
			{
				List<ConcernDomain> concernDomains = repository.getConcernDomains(this);
				if (concernDomains != null && concernDomains.size() > 0)
					concernDomain = concernDomains.get(0);
			}
				
			if (concernDomain == null)
			{
				// This is the first time the database has been opened so create
				// the default concern domain.
				
				disableNotifications();
				concernDomain = createConcernDomain(
						ConcernRepository.DEFAULT_CONCERN_DOMAIN_NAME,
						"",
						"",
						ConcernRepository.DEFAULT_CONCERN_DOMAIN_KIND,
						this);
				enableNotifications();
			}
		}
		else
		{
			// Get the requested domain
			concernDomain = repository.getConcernDomain(concernDomainName, this);
			if (concernDomain == null)
			{
				ProblemManager.reportError("Concern Domain Not Found", 
						"Concern domain '" + concernDomainName + "' was not found.", 
						null);
			}
		}
	}
	
	// ACCESSOR METHODS

	public Concern getRoot()
	{
		if (concernDomain == null)
			return null;
		else
			return concernDomain.getRoot();
	}

	public List<ConcernDomain> getConcernDomains(IConcernListener changeListener)
	{
		return repository.getConcernDomains(changeListener);
	}
	
	public ConcernDomain getConcernDomain()
	{
		return concernDomain;
	}

	/**
	 * Returns an array containing all the concerns in the concern model.
	 * 
	 * @return The names of the concerns in the concern model.
	 */
	public int getNumConcerns()
	{
		return concernDomain.getRoot().getDescendantCount();
	}
	// CONCERN METHODS

	public Concern getConcernByName(String concernName)
	{
		if (concernDomain == null)
			return null;
		else
			return concernDomain.getConcernByName(concernName);
	}

	public Concern getConcernByPath(String concernPath)
	{
		if (concernDomain == null)
			return null;
		else
			return concernDomain.getConcernByPath(concernPath);
	}
	
	/**
	 * Determines whether pName is the name of an existing concern.
	 * 
	 * @param pName
	 *            The name to test for.
	 * @return true if pName is the name of a concern in the concern model.
	 */
	public boolean hasConcern(String concernName)
	{
		if (concernDomain == null)
			return false;
		else
			return concernDomain.getConcernByName(concernName) != null;
	}

	public boolean hasConcernDomain(String concernDomainName)
	{
		for(ConcernDomain concernDomain : getConcernDomains(null))
		{
			if (concernDomain.getName().equals(concernDomainName))
				return true;
		}
		
		return false;
	}
	
	public Concern createConcern(String concernPath, String concernShortName)
	{
		return getRoot().createChild("/" + concernPath, concernShortName, this);
	}
	
	public ConcernDomain createConcernDomain(String name, String shortName,
			String description, String kind,
			IConcernListener changeListener)
	{
		return repository.createConcernDomain(name, shortName, description, kind, changeListener);
	}
	
	public int removeAllConcerns()
	{
		disableNotifications();

		int numRemoved = 0;
		
		// Remove all concerns except the ROOT element
		for(Concern topLevelChild : concernDomain.getRoot().getChildren())
		{
			numRemoved += topLevelChild.remove();
		}

		// Convert the individual CONCERN_CHILD_CHANGED events into a single
		// ALL_CONCERNS_CHANGED event
		
		clearQueuedEvents();
		enableNotifications();
		
		if (numRemoved > 0)
		{
			modelChanged(ConcernEvent.createAllConcernsChanged());
		}
		
		return numRemoved;
	}
	
	public int removeAssignments(EdgeKind concernComponentRelation)
	{
		int numUnassigned = repository.unassign(concernComponentRelation); 

		if (numUnassigned > 0)
		{
			modelChanged(ConcernEvent.createAllConcernsChanged());
		}
		
		return numUnassigned;
	}

	/**
	 * Removes all the concerns from the database.
	 */
	public void resetDatabase()
	{
		repository.resetDatabase();
		concernDomain = null;
		initConcernDomain(null);
		modelChanged(ConcernEvent.createAllConcernsChanged());
	}

	// COMPONENT METHODS

	public List<Component> getComponents(ComponentKind kind)
	{
		return repository.getComponents(kind);
	}

	public List<Component> getComponents()
	{
		return repository.getComponents();
	}
	
	public Component getComponent(String javaElementHandle)
	{
		return repository.getComponent(javaElementHandle);
	}

	public List<Concern> getAssignedConcerns(IJavaElement javaElement, 
			EdgeKind edgeKind)
	{
		Component component = repository.getComponent(javaElement.getHandleIdentifier());
		if (component == null)
			return null;
		else
			return getAssignedConcerns(component, edgeKind);
	}

	public List<Concern> getAssignedConcerns(Component component, 
			EdgeKind edgeKind)
	{
		return component.getAssignedConcerns(concernDomain, edgeKind);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof ConcernModel))
			return false;
		
		ConcernModel that = (ConcernModel) obj;
	
		return this.getConcernDomain().equals(that.getConcernDomain());
	}
	
	@Override
	public String toString()
	{
		if (concernDomain == null)
			return "<null>";
		else
		{
			return concernDomain.toString();
		}
	}
}
