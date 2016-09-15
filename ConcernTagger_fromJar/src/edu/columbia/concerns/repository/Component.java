package edu.columbia.concerns.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import edu.columbia.concerns.util.ProblemManager;

/**
 * Represents a component
 * 
 * @author vgarg
 * 
 */
public class Component implements Comparable<Component>
{
	private int id;
	private String name;
	private int kindId;
	private String handle;
	private SourceRange sourceRange;

	private IJavaElement javaElement = null;

	private ConcernRepository repository;

	/**
	 * Creates a new component for the current row in the result set.
	 * 
	 * @param resultSet
	 */
	public Component(ConcernRepository hsqldb, ResultSet resultSet)
	{
		this.repository = hsqldb;

		try
		{
			id 			= resultSet.getInt(		1);
			name 		= resultSet.getString(	2);
			kindId 		= resultSet.getInt(		3);
			handle 		= resultSet.getString(	4);
			
			sourceRange = new SourceRange(	resultSet.getInt(5),
											resultSet.getInt(6),
											resultSet.getInt(7),
											resultSet.getInt(8),
											resultSet.getInt(9));
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e, true);
		}
	}
	
	public static Component createComponent(ConcernRepository repository,
	                                        IJavaElement element,
	                                        SourceRange sourceRange,
	                                        String name,
	                                        ComponentDomain componentDomain)
	{
		ComponentKind componentKind;
		switch(element.getElementType())
		{
		case IJavaElement.FIELD: 
			componentKind = ComponentKind.FIELD; 
			break;
		case IJavaElement.INITIALIZER:
		case IJavaElement.METHOD: 
			componentKind = ComponentKind.METHOD;
			break;
		case IJavaElement.TYPE: 
			componentKind = ComponentKind.CLASS; 
			break;
		case IJavaElement.JAVA_PROJECT: 
			componentKind = ComponentKind.PROJECT; 
			break;
		case IJavaElement.PACKAGE_FRAGMENT: 
			componentKind = ComponentKind.PACKAGE; 
			break;
		case IJavaElement.COMPILATION_UNIT: 
			componentKind = ComponentKind.FILE; 
			break;
		default:
		{
			assert false; 
			return null;
		}
		}
		
		// This creates a row for the component and sets the id
		return repository.createComponent(	name, 
											componentKind,
											element.getHandleIdentifier(),
											sourceRange.getBeginLine(),
											sourceRange.getBeginColumn(),
											sourceRange.getEndLine(),
											sourceRange.getEndColumn(),
											sourceRange.getNumSourceLines(),
											componentDomain);
	}
	
	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
	
	// This is only called to fix old names
	public void updateName(String newName)
	{
		assert !this.name.equals(newName);
		
		//ProblemManager.reportInfo( 
		//		"Updating component name for: " + toString(), 
		//		"Old: " + this.name + ", New: " + newName );
		
		repository.renameComponent(id, newName);
		this.name = newName;
	}

	public boolean isKind(ComponentKind kind)
	{
		return repository.getComponentKindId(kind) == kindId;
	}
	
	public SourceRange getSourceRange()
	{
		return sourceRange;
	}
	
	// This is only called to fix incorrect ranges
	public void updateSourceRange(SourceRange sourceRange)
	{
		assert !this.sourceRange.equals(sourceRange);

		if (this.sourceRange.getNumSourceLines() != sourceRange.getNumSourceLines())
		{
			// We only expect the col and numLines to be off by 1
			ProblemManager.reportInfo( 
					"Updating source range for: " + toString(), 
					"\nOld: " + this.sourceRange + "\nNew: " + sourceRange );
		}
		
		repository.updateSourceRange(id, 
				sourceRange.getBeginLine(),
				sourceRange.getBeginColumn(),
				sourceRange.getEndLine(),
				sourceRange.getEndColumn(),
				sourceRange.getNumSourceLines());
		
		this.sourceRange = sourceRange;
	}

	public String getHandle()
	{
		return handle;
	}

	public IJavaElement getJavaElement()
	{
		if (javaElement == null)
			javaElement = JavaCore.create(handle);

		return javaElement;
	}

	public Component getParent()
	{
		IJavaElement parentElement = getJavaElement().getParent();
		assert parentElement != null;
		
		return repository.getComponent(parentElement.getHandleIdentifier());
	}
	
	public Component addChild(Component child)
	{
		repository.connectComponents(this, child, EdgeKind.CONTAINS);
		return child;
	}
	
	public List<Component> getChildren()
	{
		return repository.getChildComponents(this, false);
	}

	public List<Component> getChildrenOrderedByStartingLine()
	{
		return repository.getChildComponents(this, true);
	}

	public Collection<Component> getDescendants()
	{
		return getDescendants(new ArrayList<Component>());
	}
	
	public Collection<Component> getAncestors()
	{
		List<Component> ancestors = new ArrayList<Component>();

		Component parent = getParent();
		while (parent != null)
		{
			ancestors.add(parent);
			parent = parent.getParent();
		}
		
		return ancestors;
	}
	
	public Collection<Component> getDescendants(Collection<Component> descendants)
	{
		descendants.add(this);
		
		for(Component child : getChildren())
		{
			child.getDescendants(descendants);
		}
		
		return descendants;
	}
	
	/**
	 * Checks if an edge exists between two components
	 * 
	 * @param compFromId
	 * @param compToId
	 * @return
	 */
	public boolean isConnected(Component component)
	{
		return repository.isConnected(this, component);
	}

	public List<Concern> getAssignedConcerns(ConcernDomain concernDomain, 
			EdgeKind edgeKind)
	{
		return repository.getAssignedConcerns(concernDomain, 
				getId(), edgeKind, null);
	}
	
	/**
	 * @see getAssignmentRecursive(IJavaElement, EdgeKind)
	 */
	public Collection<Component> getAssignmentsRecursive(	Concern concern,
															EdgeKind edgeKind)
	{
		return getAssignmentRecursive(concern, getJavaElement(), edgeKind);
	}
	
	/**
	 * Obtains the java elements assigned to the concern that are at or
	 * immediate children of the specified parent element.
	 * @param parent
	 * @param edgeKind
	 * @return 
	 */
	public static Collection<Component> getAssignmentRecursive(	Concern concern,
																IJavaElement parent,
																EdgeKind edgeKind)
	{
		List<Component> nodes = new ArrayList<Component>();
		
		// Find all the assigned elements for this concern that are
		// children of the specified element
		
		for(Component assignedComponent : concern.getAssignments(edgeKind))
		{
			IJavaElement assignedElement = assignedComponent.getJavaElement();
			
			// Skip elements that are not children of the specified element
			if (!assignedElement.equals(parent) &&
				(assignedElement.getParent() == null ||
				!assignedElement.getParent().equals(parent)))
				continue;
			
			nodes.add(assignedComponent);
		}

		return nodes;
	}
	
	/**
	 * Determines if pElement can be included in a concern model. This is a copy
	 * of the method in ConcernModel
	 * 
	 * @param pElement
	 *            The element to test
	 * @return true if pElement is of a type that is supported by the concern
	 *         model.
	 */
	public static boolean isJavaElementAssignable(IJavaElement pElement)
	{
		if (pElement == null)
			return false;
		else if (!(pElement instanceof IMember))
			return false;

		try
		{
			IMember member = (IMember) pElement;

			if (member.isBinary())
			{
				return false;
			}
			else if (member instanceof IType)
			{
				IType type = (IType) member;

				// TODO: remove restriction on types later
				if (type.isAnonymous() || type.isLocal())
				{
					return false;
				}
			}

			return true;
		}
		catch (JavaModelException lException)
		{
			ProblemManager.reportException(lException, true);
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return id;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Component)
		{
			return equals((Component) o);
		}
		else if (o instanceof IJavaElement)
		{
			return equals((IJavaElement) o);
		}
		else
		{
			return false;
		}
	}

	public boolean equals(Component rhs)
	{
		// since we have unique ids this will work
		return id == rhs.id;
	}

	public boolean equals(IJavaElement rhs)
	{
		// since we have unique handles this will work
		return rhs.getHandleIdentifier().equals(handle);
	}

	@Override
	public int compareTo(Component arg0)
	{
		return id - arg0.id;
	}

	public static String getHandleFromResultSet(ResultSet resultSet)
	{
		try
		{
			return resultSet.getString(4);
		}
		catch (SQLException e)
		{
			ProblemManager.reportException(e, true);
			return "";
		}		
	}
}
