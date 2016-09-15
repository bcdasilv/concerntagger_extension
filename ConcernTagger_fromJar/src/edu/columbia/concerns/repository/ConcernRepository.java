package edu.columbia.concerns.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.util.ProblemManager;

/**
 * Class that provides connection to the HSQLDB database. All SQL queries are
 * run through this class.
 * 
 * @author vgarg
 * 
 */
public class ConcernRepository implements DBConstants
{
	// name of the database.
	protected String databaseLocation;

	// connection to the HSQLDB database.
	protected Connection con = null;

	private EnumMap<EdgeKind, Map<Integer, Set<Component>>> assignmentMap;
	
	private Map<String, Component> handleToComponentCache =
		new HashMap<String, Component>();
	
	private Map<Component, List<Component>> componentToChildrenCache =
		new HashMap<Component, List<Component>>();
	
	static
	{
		try
		{
			// load driver class
			Class.forName("org.hsqldb.jdbcDriver");
		}
		catch (ClassNotFoundException e)
		{
			ProblemManager.reportException(e);
		}
	}

	/**
	 * Constructor. Checks whether the path is relative. If so, attaches the
	 * user home directory to the path.
	 * 
	 * @param databaseLocation
	 */
	private ConcernRepository(String databaseLocation)
	{
		// E.g. "C:\Workspace\.metadata\plugins\columbia.concerntagger\db"
		this.databaseLocation = databaseLocation;
		
		this.assignmentMap = 
			new EnumMap<EdgeKind, Map<Integer, Set<Component>>>(EdgeKind.class); 
	}

	public static ConcernRepository openDatabase(String dirToSearch, boolean createIfNeeded)
	{
		String databaseLocation = resolveDatabaseDirectory(dirToSearch);
		if (databaseLocation != null)
		{
			// There is already a database at this location
			ConcernRepository repository = new ConcernRepository(databaseLocation);
			
			repository.verifyComponentKinds();
			repository.verifyEdgeKinds();
			
			return repository;
		}
		else if (!createIfNeeded)
		{
			// There is no database at this location
			return null;
		}
		else
		{
			// There is no database at this location so create one

			assert !doesDatabaseExist(dirToSearch);

			ConcernRepository repository = new ConcernRepository(dirToSearch);
			DBReset.resetDatabase(repository);
			
			repository.verifyComponentKinds();
			repository.verifyEdgeKinds();
			
			return repository;
		}
	}

	// -----------------------------------------------------
	// CONCERN DOMAIN METHODS
	// -----------------------------------------------------

	public ConcernDomain getConcernDomain(String concernDomainName,
			IConcernListener changeListener)
	{
		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERN_DOMAIN_BY_NAME,
					concernDomainName);
			if (resultSet.next())
			{
				return new ConcernDomain(this, changeListener, resultSet);
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	public List<ConcernDomain> getConcernDomains(IConcernListener changeListener)
	{
		List<ConcernDomain> concernDomains = new ArrayList<ConcernDomain>();

		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERN_DOMAINS);
			while (resultSet.next())
			{
				concernDomains.add(new ConcernDomain(this, changeListener,
						resultSet));
			}
		}
		catch (SQLException ex)
		{
			ProblemManager.reportException(ex);
		}

		return concernDomains;
	}

	public ConcernDomain createConcernDomain(
			String name,
			String description,
			String shortName,
			String kind,
			IConcernListener changeListener)
	{
		Concern rootConcern = getOrCreateRootConcern(name, changeListener);
		if (rootConcern == null)
			return null; // Database error?

		List<Object> params = new ArrayList<Object>();
		params.add(rootConcern.getId());
		params.add(name);
		params.add(shortName);
		params.add(description);
		params.add(kind);

		try
		{
			PreparedStatement statement = createPreparedStatement(CONCERN_DOMAIN_SQL, params);
			statement.executeUpdate();
			statement.close();
			con.commit();

			return getConcernDomain(name, changeListener);
		}
		catch (SQLException e)
		{
			rollback();
			
			if (e.getMessage().indexOf("Violation of unique constraint") >= 0)
			{
				ProblemManager.reportException(e, 
					"Failed to create concern domain. Concern domain '" + 
					name + "' already exists.");
			}
			else
			{
				ProblemManager.reportException(e, 
					"Failed to create concern domain '" + name + "'.");
			}
			
			return null;
		}
	}

	/**
	 * Updates the concern name.
	 * 
	 * @param oldName
	 * @param newName
	 */
	public void renameConcernDomain(String oldName, String newName)
	{
		try
		{
			PreparedStatement statement = createPreparedStatement(
					UPDATE_CONCERN_DOMAIN_NAME, newName, oldName);
			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
		}
	}
	
	// -----------------------------------------------------
	// CONCERN METHODS
	// -----------------------------------------------------

	public Concern getConcern(String domainName, String concernName,
			IConcernListener changeListener)
	{
		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERN_FROM_NAME,
					concernName);
			if (resultSet.next())
			{
				return new Concern(this, changeListener, resultSet);
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	public Concern getConcern(Integer concernId,
			IConcernListener changeListener)
	{
		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERN_FROM_ID, concernId);
			if (resultSet.next())
			{
				return new Concern(this, changeListener, resultSet);
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	/**
	 * Gets the list of concerns from the concern table
	 */
	public List<Concern> getConcerns(IConcernListener changeListener)
	{
		List<Concern> concerns = new ArrayList<Concern>();

		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERNS);
			while (resultSet.next())
			{
				concerns.add(new Concern(this, changeListener, resultSet));
			}
		}
		catch (SQLException ex)
		{
			ProblemManager.reportException(ex);
		}

		return concerns;
	}

	private Concern getOrCreateRootConcern(
			String concernDomainName,
			IConcernListener changeListener)
	{
		String concernDomainRootConcernName = concernDomainName + "-" + DEFAULT_ROOT_CONCERN_NAME; 
		
		Concern rootConcern = getConcern(concernDomainName, concernDomainRootConcernName, changeListener);
		if (rootConcern != null)
			return rootConcern;

		// The root concern has not been created for this domain so create it
		
		// Make sure you do the getConcerns() *before* calling
		// createConcern(ROOT)
		List<Concern> children = getConcerns(changeListener);

		rootConcern = createConcern(changeListener,
				concernDomainRootConcernName, "", "", "");

		for (Concern childConcern : children)
		{
			// Convert older concern tagger databases from a concern list
			// to a concern tree by making all the existing concerns a
			// child of the newly created root concern.  Only do this
			// if the concern doesn't have a parent and it's not the root
			// concern of some other concern domain.
			
			if (childConcern.getParent() == null && !childConcern.isRoot())
			{
				rootConcern.addChild(childConcern);
			}
		}

		return rootConcern;
	}

	public Concern getParentConcern(Concern child)
	{
		try
		{
			ResultSet resultSet = executeQuery(GET_PARENT_CONCERN, 
					child.getId(),
					getEdgeKindId(EdgeKind.CONTAINS));
			if (resultSet.next())
			{
				return new Concern(this, child.getChangeListener(), resultSet);
			}
			else
			{
				return null;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	public List<Concern> getChildConcerns(Concern concern)
	{
		List<Concern> children = new ArrayList<Concern>();

		try
		{
			ResultSet resultSet = executeQuery(GET_CHILD_CONCERNS, 
					concern.getId(),
					getEdgeKindId(EdgeKind.CONTAINS));
			while (resultSet.next())
			{
				children.add(new Concern(this, concern.getChangeListener(),
						resultSet));
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}

		return children;
	}
	
	public boolean hasChildConcerns(Concern concern)
	{
		try
		{
			ResultSet resultSet = executeQuery(GET_CHILD_CONCERNS, 
					concern.getId(), 
					getEdgeKindId(EdgeKind.CONTAINS));
			return resultSet.next();
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return false;
		}
	}

	public boolean addChildConcern(Concern parent, Concern child)
	{
		assert !parent.equals(child);

		List<Object> params = new ArrayList<Object>();

		try
		{
			// First remove any existing parent edge for this child
			params.add(child.getId());
			params.add(this.getEdgeKindId(EdgeKind.CONTAINS));

			PreparedStatement statement = createPreparedStatement(
					REMOVE_CONCERN_EDGE_FOR_EDGE_KIND, params);
			statement.executeUpdate();
			statement.close();

			// Now create a new parent edge

			params.clear();
			params.add(parent.getId());
			params.add(child.getId());
			params.add(this.getEdgeKindId(EdgeKind.CONTAINS));

			statement = createPreparedStatement(CONCERN_EDGE_SQL, params);
			statement.executeUpdate();
			statement.close();

			con.commit();

			return true;
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return false;
		}
	}

	/**
	 * Add the concern to the database.
	 */
	public Concern createConcern(IConcernListener changeListener,
			String name, String shortName, String description, String color)
	{
		try
		{
			Integer id = getNextSequenceNumber("concern_id_seq", CONCERN_TABLE);

			List<Object> params = new ArrayList<Object>();
			params.add(id);
			params.add(name);
			params.add(shortName);
			params.add(description);
			params.add(color);

			PreparedStatement statement = createPreparedStatement(CONCERN_SQL,
					params);
			statement.executeUpdate();
			statement.close();
			
			con.commit();

			return getConcern(id, changeListener);
		}
		catch (SQLException e)
		{
			rollback();

			if (e.getMessage().indexOf("Violation of unique constraint") >= 0)
			{
				ProblemManager.reportException(e, 
					"Failed to create concern. Concern '" + 
					name + "' already exists.");
			}
			else
			{
				ProblemManager.reportException(e, 
					"Failed to create concern '" + name + "'.");
			}
			
			return null;
		}
	}

	/**
	 * Removes a concern from concern table. If concern had assigned components,
	 * those edges are removed. All parent/child relationships with other
	 * concerns are severed. If a concern domain is associated with the concern,
	 * it is removed as well.
	 */
	public int removeConcernAndChildren(Concern concern)
	{
		int numRemoved = 0;
		
		// Remove all children
		for(Concern child : concern.getChildren())
		{
			numRemoved += removeConcernAndChildren(child);
		}
		
		try
		{
			PreparedStatement statement = createPreparedStatement(
					REMOVE_ALL_CONCERN_COMPONENT_EDGES_FOR_CONCERN, 
					concern.getId());
			statement.executeUpdate();
			statement.close();

			List<Object> params = new ArrayList<Object>();
			params.add(concern.getId()); // from_id
			params.add(concern.getId()); // to_id

			statement = createPreparedStatement(REMOVE_CONCERN_EDGE, params);
			statement.executeUpdate();
			statement.close();

			statement = createPreparedStatement(REMOVE_CONCERN, concern.getId());
			statement.executeUpdate();
			statement.close();

			statement = createPreparedStatement(REMOVE_CONCERN_DOMAIN, concern.getId());
			statement.executeUpdate();
			statement.close();

			invalidateAssignmentCache(concern.getId());
			
			con.commit();
			return 1;
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return 0;
		}
	}

	/**
	 * Updates the concern name.
	 */
	public void renameConcern(int concernId, String newName)
	{
		try
		{
			PreparedStatement statement = createPreparedStatement(
					UPDATE_CONCERN_NAME, newName, concernId);
			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
		}
	}

	// -----------------------------------------------------
	// COMPONENT METHODS
	// -----------------------------------------------------

	/**
	 * Adds a component to the database. If the domain is provided, adds the
	 * domain too.
	 * 
	 * @param name
	 * @param componentKind
	 * @param handle
	 * @param beginLine
	 * @param beginColumn
	 * @param endLine
	 * @param endColumn
	 * @param numLines
	 * @param componentDomain
	 * @return
	 */
	public Component createComponent(	String name,
	                                 	ComponentKind componentKind,
	                                 	String handle,
	                                 	Integer beginLine,
	                                 	Integer beginColumn,
	                                 	Integer endLine,
	                                 	Integer endColumn,
	                                 	Integer numLines,
	                                 	ComponentDomain componentDomain)
	{
		PreparedStatement statement = null;
		Integer compSeqNum = null;

		try
		{
			compSeqNum = getNextSequenceNumber("COMPONENT_ID_SEQ", COMPONENT_TABLE);

			List<Object> params = new ArrayList<Object>();
			params.add(compSeqNum);
			params.add(name);
			params.add(getComponentKindId(componentKind));
			params.add(handle);
			params.add(beginLine);
			params.add(beginColumn);
			params.add(endLine);
			params.add(endColumn);
			params.add(numLines);
			
			statement = createPreparedStatement(COMPONENT_INSERT_SQL, params);

			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			
			if (e.getMessage().startsWith("Violation of unique constraint"))
			{
				ProblemManager.reportException(e,
						"Program element handle '" + handle
						+ "' is already present in database");
			}
			else
			{
				ProblemManager.reportException(e);
			}

			return null;
		}

		if (componentDomain == null)
			return getComponent(compSeqNum);

		assert compSeqNum != null;

		componentDomain.setId(compSeqNum);
		try
		{
			statement = createPreparedStatement(COMPONENT_DOMAIN_INSERT, componentDomain
					.getValuesAsList());
			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return null;
		}
		
		return getComponent(compSeqNum);
	}

	public void renameComponent(Integer componentId, String newName)
	{
		try
		{
			PreparedStatement statement = createPreparedStatement(
					UPDATE_COMPONENT_NAME, newName, componentId);
			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
		}
	}

	public void updateSourceRange(	Integer componentId,
                                 	Integer beginLine,
                                 	Integer beginColumn,
                                 	Integer endLine,
                                 	Integer endColumn,
                                 	Integer numLines)
	{
		List<Object> params = new ArrayList<Object>();
		params.add(beginLine);
		params.add(beginColumn);
		params.add(endLine);
		params.add(endColumn);
		params.add(numLines);
		params.add(componentId);
		
		try
		{
			PreparedStatement statement = createPreparedStatement(
					UPDATE_COMPONENT_SOURCE_RANGE, params);
			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
		}
	}
	
	/**
	 * Adds the component edge.
	 * 
	 * @param edge
	 * @throws SQLException
	 */
	public void connectComponents(Component from, Component to, EdgeKind edgeKind)
	{
		assert(edgeKind != null);
		
		List<Object> params = new ArrayList<Object>();
		params.add(from.getId());
		params.add(to.getId());
		params.add(getEdgeKindId(edgeKind));
		
		try
		{
			PreparedStatement statement = createPreparedStatement(
					COMPONENT_EDGE_SQL, params);
			statement.executeUpdate();
			statement.close();
			con.commit();
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e, 
				"Failed to create '" + edgeKind.name() + "' component edge " +
				"from component " + from.getId() + " to " + to.getId() + ".");
		}
	}

	/**
	 * Checks if an edge exists between two components
	 * 
	 * @param compFromId
	 * @param compToId
	 * @return
	 */
	public boolean isConnected(Component from, Component to)
	{
		try
		{
			ResultSet resultSet = executeQuery(
					DBConstants.CHECK_COMPONENT_EDGE_SQL, from.getId(), to.getId());

			boolean found = resultSet.next();

			// Why do we need to do this? Why not everywhere else?
			resultSet.close();
			resultSet.getStatement().close();

			return found;
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e, true);
			return false;
		}
	}
	
	/**
	 * Gets the component associated with the component id.
	 * 
	 * @param componentId
	 * @return
	 * @throws SQLException
	 */
	public Component getComponent(Integer componentId)
	{
		try
		{
			Component component = null;

			ResultSet resultSet = executeQuery(GET_COMPONENT_BY_ID,
					componentId);
			if (resultSet.next())
			{
				component = getOrCreateComponent(resultSet);
			}

			resultSet.close();
			resultSet.getStatement().close();
			return component;
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}
	
	private Component getOrCreateComponent(ResultSet resultSet)
	{
		String handle = Component.getHandleFromResultSet(resultSet);
		
		Component component = handleToComponentCache.get(handle);
		if (component == null)
		{
			component = new Component(this, resultSet);
			handleToComponentCache.put(handle, component);
		}
		
		return component;
	}

	/**
	 * Gets the component associated with the handle.
	 * 
	 * @param handle
	 * @return
	 * @throws SQLException
	 */
	public Component getComponent(String handle)
	{
		Component cache = handleToComponentCache.get(handle);
		if (cache != null)
			return cache;
		
		try
		{
			Component component = null;

			ResultSet resultSet = executeQuery(GET_COMPONENT_BY_HANDLE,
					handle);
			if (resultSet.next())
			{
				component = getOrCreateComponent(resultSet);
			}

			resultSet.close();
			resultSet.getStatement().close();
			return component;
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	/**
	 * Return total number of components for a particular component kind
	 * 
	 * @param coKind
	 * @return
	 * @throws SQLException
	 */
	public List<Component> getComponents(ComponentKind componentKind)
	{
		List<Component> components = new ArrayList<Component>();

		// get all components for a concern of a particular kind
		try
		{
			ResultSet resultSet = executeQuery(GET_COMPONENTS_OF_KIND,
					getComponentKindId(componentKind));
			while (resultSet.next())
			{
				Component component = getOrCreateComponent(resultSet); 
				components.add(component);
			}

			return components;
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	/**
	 * Return total number of components for a particular component kind
	 * 
	 * @param coKind
	 * @return
	 * @throws SQLException
	 */
	public List<Component> getComponents()
	{
		List<Component> components = new ArrayList<Component>();

		// get all components for a concern of a particular kind
		try
		{
			ResultSet resultSet = executeQuery(GET_COMPONENTS);
			while (resultSet.next())
			{
				Component component = getOrCreateComponent(resultSet); 
				components.add(component);
			}

			return components;
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}
	
	/**
	 * Returns the list of child components for a component and their associated
	 * concern edges.
	 * 
	 * @param con
	 * @param componentId
	 * @param componentTypeId
	 * @param concernId
	 * @return
	 * @throws SQLException
	 */
	public List<Component> getChildComponents(Component component, 
			boolean orderByStartLine)
	{
		// get all child components for the component

		List<Component> children = componentToChildrenCache.get(component);
		if (children != null)
			return children;
			
		children = new ArrayList<Component>();

		try
		{
			ResultSet resultSet = getChildComponentResults(component.getId(), 
					orderByStartLine);
			if (resultSet == null)
				return children;
			
			// create the list of components and edge
			while (resultSet.next())
			{
				Component child = getOrCreateComponent(resultSet); 
				children.add(child);
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}

		componentToChildrenCache.put(component, children);
		
		return children;
	}

	/**
	 * Returns the list of child components for a component and their associated
	 * concern edges.
	 * 
	 * @param con
	 * @param componentId
	 * @param componentTypeId
	 * @param concernId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getChildComponentResults(int componentId, 
			boolean orderByStartLine)
	{
		try
		{
			return executeQuery((orderByStartLine ? 
							DBConstants.GET_COMPONENT_CHILDREN_ORDERED :
							DBConstants.GET_COMPONENT_CHILDREN), 
							componentId,
							getEdgeKindId(EdgeKind.CONTAINS));
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return null;
		}
	}

	// -----------------------------------------------------
	// ASSIGNMENT METHODS
	// -----------------------------------------------------

	public boolean assign(Concern concern, Component component, EdgeKind edgeKind)
	{
		if (isAssigned(concern, component, edgeKind))
			return false;
		
		try
		{
			List<Object> params = new ArrayList<Object>();
			params.add(concern.getId());
			params.add(component.getId());
			params.add(getEdgeKindId(edgeKind));

			PreparedStatement preparedStatement = createPreparedStatement(
					CONCERN_COMPONENT_EDGE_SQL, params);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			
			// Invalidate the cache so we refetch it
			invalidateAssignmentCache(concern.getId(), edgeKind);
			
			con.commit();
			return true;
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return false;
		}
	}
	
	/**
	 * Remove a concern component edge
	 * 
	 * @param concernName
	 * @param componentHandle
	 */
	public boolean unassign(Concern concern, String componentHandle, EdgeKind edgeKind)
	{
		List<Object> params = new ArrayList<Object>();
		params.add(concern.getId());
		params.add(componentHandle);
		params.add(getEdgeKindId(edgeKind));

		try
		{
			PreparedStatement statement = createPreparedStatement(
					REMOVE_CONCERN_COMPONENT_EDGE, params);

			int numUnassigned = statement.executeUpdate();
			statement.close();
			
			// Invalidate the cache so we refetch it
			invalidateAssignmentCache(concern.getId(), edgeKind);
			
			con.commit();
			return numUnassigned != 0;
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return false;
		}
	}

	/**
	 * Remove a concern component edge
	 * 
	 * @param concernName
	 * @param componentHandle
	 */
	public int unassign(Concern concern, EdgeKind edgeKind)
	{
		try
		{
			PreparedStatement statement = createPreparedStatement(
					REMOVE_ALL_CONCERN_COMPONENT_EDGES_FOR_EDGE_KIND, 
					concern.getId(),
					getEdgeKindId(edgeKind));

			int numUnassigned = statement.executeUpdate();
			statement.close();
			
			// Invalidate the cache so we refetch it
			invalidateAssignmentCache(concern.getId(), edgeKind);
			
			con.commit();
			return numUnassigned;
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return 0;
		}
	}

	/**
	 * Remove a concern component edge
	 * 
	 * @param concernName
	 * @param componentHandle
	 */
	public int unassign(EdgeKind edgeKind)
	{
		try
		{
			PreparedStatement statement = createPreparedStatement(
					REMOVE_ALL_CONCERN_COMPONENT_EDGES_FOR_EDGE_KIND, 
					getEdgeKindId(edgeKind));

			int numUnassigned = statement.executeUpdate();
			statement.close();

			// Invalidate the cache so we refetch it
			invalidateAssignmentCache(edgeKind);
			
			con.commit();
			return numUnassigned;
		}
		catch (SQLException e)
		{
			rollback();
			ProblemManager.reportException(e);
			return 0;
		}
	}
	
	public boolean isAssigned(int concernId, EdgeKind edgeKind)
	{
		return !getAssignmentsFromCache(concernId, edgeKind).isEmpty();
	}
	
	public boolean isAssigned(Concern concern, Component component, EdgeKind edgeKind)
	{
		return getAssignmentsFromCache(concern.getId(), edgeKind).contains(component);
	}

	public Collection<Component> getAssignments(Concern concern, EdgeKind edgeKind)
	{
		return getAssignmentsFromCache(concern.getId(), edgeKind);
	}
	
	public boolean hasAssignedConcerns(ConcernDomain concernDomain,
	                                   int componentId, 
	                                   EdgeKind edgeKind)
	{
		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERNS_FOR_COMPONENT,
					componentId, getEdgeKindId(edgeKind));
			while (resultSet.next())
			{
				Concern candidateConcern = 
					new Concern(this, null, resultSet);
				
				if (candidateConcern.isInConcernDomain(concernDomain))
				{
					return true;
				}
			}
			
			return false;
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
			return false;
		}
	}
	
	public List<Concern> getAssignedConcerns(	ConcernDomain concernDomain,
												int componentId,
												EdgeKind edgeKind,
												IConcernListener changeListener)
	{
		List<Concern> assignedConcerns = null;
		
		try
		{
			ResultSet resultSet = executeQuery(GET_CONCERNS_FOR_COMPONENT,
					componentId, getEdgeKindId(edgeKind));
			while (resultSet.next())
			{
				Concern candidateConcern = 
					new Concern(this, changeListener, resultSet);
				
				if (candidateConcern.isInConcernDomain(concernDomain))
				{
					if (assignedConcerns == null)
						assignedConcerns = new ArrayList<Concern>();
					
					assignedConcerns.add(candidateConcern);
				}
			}
			
			resultSet = null; // Helps GC?
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}
		
		return assignedConcerns;
	}

	public Collection<Component> getAssignmentsFromCache(Integer concernId, EdgeKind edgeKind)
	{
		Map<Integer, Set<Component>> assignmentMapForEdge = 
			assignmentMap.get(edgeKind);

		if (assignmentMapForEdge == null)
		{
			assignmentMapForEdge = new HashMap<Integer, Set<Component>>();
			assignmentMap.put(edgeKind, assignmentMapForEdge);
		}
		
		// See if we've already cached the assignments for this concern
		Set<Component> assignedComponents = assignmentMapForEdge.get(concernId);
		if (assignedComponents != null)
			return assignedComponents;

		// Cache miss: fill the cache
		assignedComponents = new TreeSet<Component>();
		assignmentMapForEdge.put(concernId, assignedComponents);
		
		try
		{
			ResultSet resultSet = executeQuery(GET_COMPONENTS_FOR_CONCERN,
					concernId, getEdgeKindId(edgeKind));
			while (resultSet.next())
			{
				Component component = getOrCreateComponent(resultSet);
				assignedComponents.add(component);					
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportError("Failed to Access Concern Assignments",
					null, 
					"Failed to retrieve components assigned to concern '" +
					concernId + "'.\n" + e, true);
		}

		return assignedComponents;
	}

	public void invalidateAssignmentCache(Integer concernId, EdgeKind edgeKind)
	{
		Map<Integer, Set<Component>> assignmentsForConcern = 
			assignmentMap.get(edgeKind);
		
		if (assignmentsForConcern != null)
			assignmentsForConcern.put(concernId, null);
	}

	public void invalidateAssignmentCache(Integer concernId)
	{
		for(Map<Integer, Set<Component>> assignmentsForConcern : assignmentMap.values())
		{
			if (assignmentsForConcern != null)
				assignmentsForConcern.remove(concernId);
		}
	}
	
	public void invalidateAssignmentCache(EdgeKind edgeKind)
	{
		assignmentMap.put(edgeKind, null);
	}
	
	// -----------------------------------------------------
	// HELPER METHODS
	// -----------------------------------------------------

	private static String resolveDatabaseDirectory(String dir)
	{
		// Assume the real path to the database file is
		// C:\Workspace\.metadata\.plugins\columbia.concerntagger\db.scripts
		// The path that HSQLDB wants to see is
		// C:\Workspace\.metadata\.plugins\columbia.concerntagger\db

		// See if they specified
		// C:\Workspace\.metadata\.plugins\columbia.concerntagger\db
		if (doesDatabaseExist(dir))
			return dir;

		// See if they specified
		// C:\Workspace\.metadata\.plugins\columbia.concerntagger

		String tryPath = dir + File.separator + "db";
		if (doesDatabaseExist(tryPath))
			return tryPath;

		// See if they specified
		// C:\Workspace

		tryPath = dir + File.separator + 
				".metadata" + File.separator + 
				".plugins" + File.separator +
				"columbia.concerntagger" + File.separator + 
				"db";
		
		if (doesDatabaseExist(tryPath))
			return tryPath;

		// See if they specified
		// C:\

		tryPath = dir + File.separator + 
				"workspace" + File.separator +
				".metadata" + File.separator + 
				".plugins" + File.separator +
				"columbia.concerntagger" + File.separator + 
				"db";
		
		if (doesDatabaseExist(tryPath))
			return tryPath;
		
		return null;
	}

	private static boolean doesDatabaseExist(String path)
	{
		return new File(path + ".properties").exists();
	}

	/**
	 * Shutsdown the database. HSQLDB requires this to commit changes when a
	 * database is closed.
	 */
	public void shutdown()
	{
		try
		{
			// String home = System.getProperty("user.home");
			// System.out.println(home);

			Statement statement = getConnection().createStatement();
			statement.execute("SHUTDOWN");
		}
		catch (Exception e)
		{
			try
			{
				con.close();
				ProblemManager.reportException(e);
			}
			catch (SQLException e1)
			{
				ProblemManager.reportException(e);
				ProblemManager.reportException(e1);
			}
		}
	}

	public void resetDatabase()
	{
		// Reset database to its initial state
		DBReset.resetDatabase(this);
		
		// Invalidate the assignment cache
		this.assignmentMap = 
			new EnumMap<EdgeKind, Map<Integer, Set<Component>>>(EdgeKind.class); 
	}
	
	/**
	 * Execute a query statement. The list of values should match the number of
	 * parameters in the sql.
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws SQLException
	 */

	private ResultSet executeQuery(String sql) throws SQLException
	{
		return createPreparedStatement(sql).executeQuery();
	}

	private ResultSet executeQuery(String sql, Integer value)
			throws SQLException
	{
		return createPreparedStatement(sql, value).executeQuery();
	}

	private ResultSet executeQuery(String sql, Integer value1, Integer value2)
		throws SQLException
	{
		return createPreparedStatement(sql, value1, value2).executeQuery();
	}
	
	private ResultSet executeQuery(String sql, String value) throws SQLException
	{
		return createPreparedStatement(sql, value).executeQuery();
	}

	/**
	 * Create a prepared statment for an sql and associated list of values.
	 * 
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql,
			List<Object> params) throws SQLException
	{
		assert params != null;
		assert sql != null;

		PreparedStatement pstmt;

		// prepare statement
		try
		{
			pstmt = getConnection().prepareStatement(sql);
		}
		catch (NullPointerException e)
		{
			ProblemManager.reportException(e,
					"Failed to execute SQL statement: '" + sql +
					"' with parameters: '" + params.toString() + "'");
			return null;
		}

		int i = 1;
		for (Object param : params)
		{
			// set string
			if (param instanceof String)
			{
				pstmt.setString(i, (String) param);
			}
			// set integer
			else if (param instanceof Integer)
			{
				pstmt.setInt(i, (Integer) param);
			}
			// else set as object
			else
			{
				assert false;
				pstmt.setObject(i, param);
			}

			++i;
		}
		
		return pstmt;
	}

	/**
	 * Create a prepared statement for an sql and associated list of values.
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql, int value)
			throws SQLException
	{
		// prepare statement
		PreparedStatement pstmt = getConnection().prepareStatement(sql);
		pstmt.setInt(1, value);
		return pstmt;
	}

	/**
	 * Create a prepared statement for an sql and associated list of values.
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql, String value1, int value2)
			throws SQLException
	{
		// prepare statement
		PreparedStatement pstmt = getConnection().prepareStatement(sql);
		pstmt.setString(1, value1);
		pstmt.setInt(2, value2);
		return pstmt;
	}

	/**
	 * Create a prepared statement for an sql and associated list of values.
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql, int value1, int value2)
			throws SQLException
	{
		// prepare statement
		PreparedStatement pstmt = getConnection().prepareStatement(sql);
		pstmt.setInt(1, value1);
		pstmt.setInt(2, value2);
		return pstmt;
	}
	
	/**
	 * Create a prepared statement for an sql and associated list of values.
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql, String value)
			throws SQLException
	{
		// prepare statement
		PreparedStatement pstmt = getConnection().prepareStatement(sql);
		pstmt.setString(1, value);
		return pstmt;
	}

	/**
	 * Create a prepared statment for an sql and associated list of values.
	 * 
	 * @param sql
	 * @param values
	 * @return
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql, String value1, String value2)
			throws SQLException
	{
		// prepare statement
		PreparedStatement pstmt = getConnection().prepareStatement(sql);
		pstmt.setString(1, value1);
		pstmt.setString(2, value2);
		return pstmt;
	}
	
	/**
	 * Create a prepared statement for an sql.
	 * 
	 * @param sql
	 * @return Prepared statement.
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql)
			throws SQLException
	{
		return getConnection().prepareStatement(sql);
	}

	private void rollback()
	{
		try
		{
			con.rollback();
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}
	}

	/**
	 * Gets a connection to the database.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public synchronized Connection getConnection() throws SQLException
	{
		if (con == null || con.isClosed())
		{
			con = DriverManager.getConnection("jdbc:hsqldb:file:"
					+ databaseLocation);
			con.setAutoCommit(false);
		}

		return con;
	}

	/**
	 * Load edge_kind table into map.
	 * 
	 * @throws SQLException
	 */
	private void verifyEdgeKinds()
	{
		try
		{
			ResultSet resultSet = executeQuery("select * from edge_kind");
			while (resultSet.next())
			{
				String edgeKindName = resultSet.getString("name");
				int edgeKindId = resultSet.getInt("edge_kind_id");
				assert edgeKindId == 
					EdgeKind.valueOfIgnoreCase(edgeKindName).ordinal() + 1;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}
	}
	
	/**
	 * Load component_kind table into map.
	 * 
	 * @throws SQLException
	 */
	private void verifyComponentKinds()
	{
		try
		{
			ResultSet resultSet = executeQuery("select * from component_kind");
			while (resultSet.next())
			{
				String componentKindName = resultSet.getString("name");
				int componentKindId = resultSet.getInt("id");
				assert componentKindId ==
					ComponentKind.valueOfIgnoreCase(componentKindName).ordinal() + 1;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}
	}

	/**
	 * Gets the database value for a component kind
	 * 
	 * @param componentKindEnum
	 * @return
	 */
	public int getComponentKindId(ComponentKind componentKindEnum)
	{
		return componentKindEnum.ordinal() + 1;
	}

	/**
	 * Gets the database value for a edge kind
	 * 
	 * @param edgeKindEnum
	 * @return
	 */
	public int getEdgeKindId(EdgeKind edgeKindEnum)
	{
		return edgeKindEnum.ordinal() + 1;
	}

	/**
	 * Generates the next integer for the column in a table. It assumes we have
	 * a single connection. We should change this to use sequences.
	 * 
	 * @param con
	 * @param sequenceName
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public Integer getNextSequenceNumber(String sequenceName, String tableName)
	{
		try
		{
			ResultSet resultSet = executeQuery(
					"select max(" + sequenceName + ") from " + tableName);

			if (resultSet.next())
			{
				return resultSet.getInt(1) + 1;
			}
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e);
		}

		ProblemManager.reportError("Failed to Create Item", 
				"Unable to create sequence", tableName);
		return null;
	}
}
