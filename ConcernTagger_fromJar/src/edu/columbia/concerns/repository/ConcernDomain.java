package edu.columbia.concerns.repository;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.actions.OpenConcernDomainAction;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.util.Comparer;
import edu.columbia.concerns.util.ProblemManager;

public class ConcernDomain implements Comparable<ConcernDomain>
{
	private ConcernRepository hsqldb;

	private Integer id;
	private String name;
	private String description;
	private String shortName;
	private String kind;

	private IConcernListener changeListener;

	private Concern root;

	public ConcernDomain(ConcernRepository hsqldb,
			IConcernListener changeListener, 
			ResultSet resultSet)
	{
		this.hsqldb = hsqldb;
		this.changeListener = changeListener;

		try
		{
			this.id 			= resultSet.getInt(		1);
			this.name 			= resultSet.getString(	2);
			this.shortName 		= resultSet.getString(	3);
			this.description 	= resultSet.getString(	4);
			this.kind 			= resultSet.getString(	5);
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e, true);
		}

		root = hsqldb.getConcern(id, changeListener);
	}

	public boolean isDefault()
	{
		return name.equals(ConcernRepository.DEFAULT_CONCERN_DOMAIN_NAME);
	}
	
	public boolean rename(String newName)
	{
		if (name.equals(newName))
			return false;

		hsqldb.renameConcernDomain(name, newName);

		// Safe to update locally cached name
		name = newName;
		
		if (changeListener != null)
		{
			changeListener.modelChanged(ConcernEvent.createDomainNameChangedEvent());
		}
		
		return true;
	}
	
	public String getName()
	{
		return name;
	}

	public String getKind()
	{
		return kind;
	}

	public Concern getRoot()
	{
		return root;
	}

	public Concern getConcernByName(String concernName)
	{
		return root.findByName(concernName);
	}

	public Concern getConcernByPath(String concernPath)
	{
		return root.findByPath(concernPath);
	}
	
	public static String isNameValid(String name)
	{
		if (name == null)
		{
			return ConcernTagger.getResourceString("NullName");
		}
		else if (name.isEmpty())
		{
			return ConcernTagger.getResourceString("EmptyName");
		}
		else if (name.indexOf(OpenConcernDomainAction.ID_DOMAIN_SEP) >= 0 ||
				name.indexOf(OpenConcernDomainAction.ID_COUNT_SEP) >= 0 ||
				name.indexOf(':' /*ViewFactory.ID_SEP*/ ) >= 0)
		{
			return ConcernTagger.getResourceString("ConcernDomains.IllegalCharacter");
		}
		else if (ConcernModelFactory.singleton().getModel().hasConcernDomain(name))
		{
			return ConcernTagger.getResourceString("NameInUse");
		}
		else if (name.equals(ConcernRepository.DEFAULT_CONCERN_DOMAIN_NAME))
		{
			return ConcernTagger.getResourceString("NotAllowed");
		}
		else
		{
			return null; // Name is valid
		}
	}

	@Override
	public int compareTo(ConcernDomain that)
	{
		int res = Comparer.safeCompare(this.id, that.id);
		if (res != 0)
			return res;

		res = Comparer.safeCompare(this.name, that.name);
		if (res != 0)
			return res;

		return Comparer.safeCompare(this.kind, that.kind);
	}
	
	@Override
	public boolean equals(Object that)
	{
		if (that == null || !(that instanceof ConcernDomain))
			return false;
		
		return compareTo((ConcernDomain) that) == 0;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
}
