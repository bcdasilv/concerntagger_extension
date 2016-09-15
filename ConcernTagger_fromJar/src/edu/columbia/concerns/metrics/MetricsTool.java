package edu.columbia.concerns.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.ComponentKind;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.ConcernDomain;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;
import edu.columbia.concerns.util.ISimpleProgressMonitor;
import edu.columbia.concerns.util.ProblemManager;

public class MetricsTool
{
    final static double epsilon = 0.00001;
	
	List<Component> allClasses;
	List<Component> allMethods;
	List<Component> allFields;
	
	IConcernModelProvider concernModelProvider;

	public MetricsTool(IConcernModelProvider concernModelProvider)
	{
		this.concernModelProvider = concernModelProvider;

		allClasses = concernModelProvider.getModel().getComponents(ComponentKind.CLASS);
		allMethods = concernModelProvider.getModel().getComponents(ComponentKind.METHOD);
		allFields = concernModelProvider.getModel().getComponents(ComponentKind.FIELD);
	}
	
	/* Created by Bruno: 29/08/2011 */
	public List<Component> getAllClassesCovered(){
		allClasses = concernModelProvider.getModel().getComponents(ComponentKind.CLASS);
		return allClasses;
	}
	
	/* Created by Bruno: 08/05/2012 */
	public List<Component> getAllMethodsCovered(){
		allMethods = concernModelProvider.getModel().getComponents(ComponentKind.METHOD);
		return allMethods;
	}
	
	/* Created by Bruno: 08/05/2012 */
	public List<Component> getAllFieldsCovered(){
		allFields = concernModelProvider.getModel().getComponents(ComponentKind.FIELD);
		return allFields;
	}
	
	/**
	 * [Bruno / 02-Oct-2014] A more complete LCbC metric table
	 */
	public LCCMetricTableExtended getCompleteLCbCMetricsTable()
	{
		LCCMetricTableExtended metricsTable = new LCCMetricTableExtended();
		
		for (Component clazz : allClasses) {
			LCCForComponent lcbc = getLCCValue(clazz);
			metricsTable.add(lcbc);
		}

		return metricsTable;
	}

	/**
	 * Calculates metrics for all concerns in the concern domain
	 */
	public ConcernMetricsTable getMetricsForAllConcerns()
	{
		ConcernMetricsTable metricsTable = new ConcernMetricsTable();
		
		getMetricsForConcernAndChildren(concernModelProvider.getModel().getRoot(), 
				metricsTable);

		return metricsTable;
	}

	/**
	 * Calculates metrics for a concern and its children (recursive)
	 */
	public void getMetricsForConcernAndChildren(Concern concern, 
	                                            ConcernMetricsTable metricsTable)
	{
		MetricsForConcern metricsForConcern = getMetricsForConcern(concern, null); 
		metricsTable.add(metricsForConcern);
		
		for (Concern child : concern.getChildren())
		{
			getMetricsForConcernAndChildren(child, metricsTable);
		}
	}

	/**
	 * Calculates metrics for a concern
	 */
	public MetricsForConcern getMetricsForConcern(final Concern concern, 
	                                              final ISimpleProgressMonitor progressMonitor)
	{
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;
		
		MetricsForConcern metricForConcern = new MetricsForConcern(concern);

		Map<Component, Integer> componentContributionsForConcern =
			getContributionsForAllComponents(concern, 
					concernModelProvider.getConcernComponentRelation());
		
		// Calculate DOSC, CDC, and SLOCs
		
		DOSResult doscResult = calculateDegreeOfScattering(
				componentContributionsForConcern,
				ComponentKind.CLASS,
				allClasses.size(),
				progressMonitor);
		
		metricForConcern.addMetric(doscResult.dos);

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		// Calculate DOSM and CDO
		
		DOSResult dosmResult = calculateDegreeOfScattering(
				componentContributionsForConcern,
				ComponentKind.METHOD, 
				allMethods.size(), 
				progressMonitor);
		
		metricForConcern.addMetric(dosmResult.dos);

		// Add CDC
		
		int cdc = doscResult.assignedComponents;
		metricForConcern.addMetric(cdc);
		
		// Add CDO
		
		int cdo = dosmResult.assignedComponents;
		metricForConcern.addMetric(cdo);

		if (cdo < cdc)
		{
			ProblemManager.reportError("Suspicious Metrics", 
					"'" + concern.getDisplayName() + "' concern has more type " +
					"assigned to it than methods",
					"CDC: " + cdc + ", CDO: " + cdo + 
					"\nThis can happen if the types are interfaces or the concern " +
					"is assigned to a type's fields but not its methods.");
		}
		
		// Add total SLOCs (calculated at the class level)

		metricForConcern.addMetric(doscResult.totalAssignedSlocs);

		return metricForConcern;
	}

	/**
	 * Aggregate assignments for a concern.
	 * <P>
	 * A concern's assignments include all assignments beneath it
	 * in the Concern Tree view, including assignments beneath its
	 * child concerns.  If a type is assigned the assumption is
	 * that we treat its children (methods, fields, and inner types)
	 * as also being assigned.
	 *
	 * @param concern
	 * @param concernComponentRelation
	 * @return
	 */
	private static Map<Component, Integer> getContributionsForAllComponents(
			Concern concern, 
			EdgeKind concernComponentRelation)
	{
		// Get assignments for the concern and its child concerns
		
		Set<Component> allAssignedComponents = new HashSet<Component>();
		concern.getAssignmentsRecursive(concernComponentRelation, 
				allAssignedComponents);

		// We need to consider components that are directly assigned as
		// well as component's whose ancestors or descendants are assigned.
		
		// If the component or one of its ancestors is directly assigned,
		// its contribution is its source line count.  Otherwise, its
		// contribution is the sum of the contributions of its children.
		
		Map<Component, Integer> componentContributions = 
			new HashMap<Component, Integer>(allAssignedComponents.size()*2);  
		
		for(Component assignedComponent : allAssignedComponents)
		{
			int assignedComponentsSlocs = 
				assignedComponent.getSourceRange().getNumSourceLines();

			// The component is directly assigned so record its contribution
			Integer assignedComponentsOldContribution = 
				componentContributions.put(assignedComponent, assignedComponentsSlocs);
			assert assignedComponentsOldContribution == null ||
				assignedComponentsOldContribution <= assignedComponentsSlocs;
			
			// A child contributes all the way up the code model tree:
			// enclosing type, enclosing file, enclosing package, and
			// finally enclosing project.
			//
			// Since we only calculate metrics at or below the type
			// level, setting the contribution for parents above the
			// type level is not really necessary
			for(Component ancestor : assignedComponent.getAncestors())
			{
				if (assignedComponent.isKind(ComponentKind.CLASS) &&
					ancestor.isKind(ComponentKind.CLASS))
				{
					// Inner types never contribute to their outer
					// class's contribution; however, they do contribute
					// to the file, package, etc.
					continue;
				}
				
				// When the declaring type is not assigned its contribution
				// is the sum of its methods' and fields' contributions
				
				Integer parentsOldContribution = componentContributions.get(ancestor);
				
				int parentsNewContribution = assignedComponentsSlocs;
				if (parentsOldContribution != null)
				{
					parentsNewContribution += parentsOldContribution;
				}
				
				// Children can only contribute to their parent's
				// contribution if the parent hasn't already contributed
				// its max amount
				
				int parentsSlocs = ancestor.getSourceRange().getNumSourceLines();
				if (parentsNewContribution <= parentsSlocs)
				{
					// Update the parent's contribution
					componentContributions.put(ancestor, parentsNewContribution);
				}
			}

			// Descendants of an assigned component are also treated
			// as assigned (including inner types)
			
			for(Component descendant : assignedComponent.getDescendants())
			{
				int descendantsSlocs = descendant.getSourceRange().getNumSourceLines();

				Integer descendantsOldContribution = 
					componentContributions.put(descendant, descendantsSlocs);
				
				// If the descendant's current contribution > 0 but
				// < descendantSlocs, we've already visited one of
				// its assigned descendants.  If the contribution
				// == descendantSlocs, then the descendant is already
				// assigned and we've already visited it, in which
				// case we didn't change the contribution.
				
				assert descendantsOldContribution == null ||
					descendantsOldContribution <= descendantsSlocs;
			}
		}
		
		return componentContributions;
	}

	/**
	 * Calculate Degree of Scattering (DOS)
	 */
	public DOSResult calculateDegreeOfScattering(Map<Component, Integer> componentContributions,
	                                    ComponentKind componentKind,
	                                    final int totalComponentsOfKind, // |T|
	                                    ISimpleProgressMonitor progressMonitor)
	{
		DOSResult result = new DOSResult();
		
		result.totalAssignedSlocs = getContributedSourceLines(
				componentContributions,
				componentKind,
				progressMonitor);
		
		if (result.totalAssignedSlocs == 0)
		{
			// Concern has no SLOCs assigned to it - IGNORE 
			return result;
		}

        // Since the sum of the concentration over all components is 1,
        // the average concentration is just 1 / # components.
		final float meanConcentration = 1.0f / totalComponentsOfKind; 	// 1 / |T|
		final float meanConcentrationSquared = meanConcentration * meanConcentration;
		
		float devianceSquaredSum = 0;
		float totalConcentration = 0;
		
		int slocsAssignedToConcernCheck = 0;

		for (Map.Entry<Component, Integer> componentContribution : 
				componentContributions.entrySet())
		{
			if (progressMonitor != null && progressMonitor.isCanceled())
				return result;

			int assignedSlocs =
				getContributedSourceLines(componentContribution, componentKind);

			// Skip components that are not the right kind
			if (assignedSlocs == 0)
				continue;
			
			++result.assignedComponents;
			
	        // Concentration is the number of source lines shared by the concern
	        // and the component, divided by the total number of the concern's
	        // source lines.  The more a concern is concentrated in a component,
	        // the more of the concern's source lines will be in the component.
			float actualConcentration = 
				assignedSlocs / (float) result.totalAssignedSlocs;
			
			if (actualConcentration > 0 && actualConcentration < 1)
				actualConcentration += 0;
			
			float deviance = actualConcentration - meanConcentration;
			devianceSquaredSum += deviance * deviance;

			// For later sanity checks
			totalConcentration += actualConcentration;
			slocsAssignedToConcernCheck += assignedSlocs;
		}

		assert isNear(totalConcentration, 1.0f);
		assert slocsAssignedToConcernCheck == result.totalAssignedSlocs;

		// Accumulate the deviances of the components that were not assigned
		devianceSquaredSum += meanConcentrationSquared *
			(totalComponentsOfKind - result.assignedComponents); 
		
		result.dos = 1.0f - (totalComponentsOfKind * devianceSquaredSum) / 
			(totalComponentsOfKind - 1.0f);
		
		return result;
	}

	/*
	private void printAssignmentStatistics(
	          Map<Component, Integer> componentContributions)
	{
		int mappedTypes = 0;
		int mappedMethods = 0;
		int mappedFields = 0;
		
		int mappedSourceLines = 0;
		
		for(Map.Entry<Component, Integer> componentContribution : 
			componentContributions.entrySet())
		{
			Component componentWithContribution = componentContribution.getKey();
			int contribution = componentContribution.getValue();
			
			if (componentWithContribution.isKind(ComponentKind.FIELD))
			{
				mappedSourceLines += contribution;
				++mappedFields;
			}
			else if (componentWithContribution.isKind(ComponentKind.METHOD))
			{
				mappedSourceLines += contribution;
				++mappedMethods;
			}
			else if (componentWithContribution.isKind(ComponentKind.CLASS))
			{
				mappedSourceLines += contribution;
				++mappedTypes;
			}
		}
		
		System.out.println("Total number of classes = " + allClasses.size());
		System.out.println("Total number of mapped classes = " + mappedTypes);
		System.out.println("% of classes mapped = "
				+ (mappedTypes * 100.0f / allClasses.size()));
		System.out.println("Total number of methods = " + allMethods.size());
		System.out.println("Total number of mapped methods = " + mappedMethods);

		System.out.println("% of methods mapped = "
				+ (mappedMethods * 100.0f / allMethods.size()));
		System.out.println("Total number of fields = " + allFields.size());
		System.out.println("Total number of mapped fields = "
				+ mappedFields);
		System.out.println("% of fields mapped = "
				+ (mappedFields * 100.0f / allFields.size()));

		int totalSourceLinesInProgram = getTotalSourceLines();

		System.out.println("Total SLOC = " + totalSourceLinesInProgram);
		System.out.println("Mapped SLOC = " + mappedSourceLines);
		System.out.println("% of SLOC mapped = "
				+ (mappedSourceLines * 100.0f / totalSourceLinesInProgram));
	}
	*/

	/**
	 * Returns the number of source lines contributed by all components of
	 * the given kind.
	 */
	private int getContributedSourceLines(Map<Component, Integer> componentContributions,
	                                      ComponentKind componentKind,
	                                      ISimpleProgressMonitor progressMonitor)
	{
		int totalSlocsAssignedToConcern = 0;

		for(Map.Entry<Component, Integer> componentContribution : 
			componentContributions.entrySet())
		{
			if (progressMonitor != null && progressMonitor.isCanceled())
				return -1;

			totalSlocsAssignedToConcern += 
				getContributedSourceLines(componentContribution, componentKind);
		}
		
		return totalSlocsAssignedToConcern;
	}

	/**
	 * @return
	 * 	0 if the component isn't of the specified kind,
	 * 	otherwise returns the component's contribution 
	 */
	private int getContributedSourceLines(Map.Entry<Component, Integer> componentContribution,
	                                      ComponentKind componentKind)
	{
		Component assignedComponent = componentContribution.getKey();
		
		if (!assignedComponent.isKind(componentKind))
			return 0;
		
		int contribution = componentContribution.getValue();

		assert contribution > 0;

		// An component's contribution should never be more than its
		// line count
		assert contribution <= assignedComponent.getSourceRange().getNumSourceLines();

		// Only classes can have contributions less than their line
		// count since the class may have children assigned but not
		// be assigned itself
		
		assert contribution == assignedComponent.getSourceRange().getNumSourceLines() ||
			assignedComponent.isKind(ComponentKind.CLASS);
		
		return contribution;
	}
	
	/* 
	 * Created by Bruno (11/04/2011) 
	 * We have to get all the components (classes and interfaces).
	 * Then, for each component get the underlying fields and methods that may be
	 * associated to a concern. Check if one of them has concern assignments. If so,
	 * check for each concern assignment of the given element if the enclosing component 
	 * has also an assignment for such a concern. If so, the LCC should not count this 
	 * concern twice. Besides, it's necessary to check if the LCC has already counted this 
	 * concern for another element of the component. LCC counts the number of concerns in 
	 * the component, no matter how many times a concern is tagged over the component's
	 * fields and methods.
	 * 
	 * Comments Update - Bruno (08/05/2012)
	 * This method can also be called by using methods and fields as comp parameters
	 * */
	public LCCForComponent getLCCValue(Component comp){
		
		LCCForComponent lccForComponent = new LCCForComponent(comp);
		
		//getting all concerns assignment to this component 
		//I'm not discriminating the different kinds of relations. Just including the assignments for all relations
		Collection<Concern> concernsAssignedToComponentAllRelations = new ArrayList<Concern>();
		for (EdgeKind e: EdgeKind.values()){
			List<Concern> assignedConcernsTemp = concernModelProvider.getModel().getAssignedConcerns(comp, e);
			if (assignedConcernsTemp!=null)
				concernsAssignedToComponentAllRelations.addAll(assignedConcernsTemp);
		}
		
		if(comp.isKind(ComponentKind.CLASS) && !concernsAssignedToComponentAllRelations.isEmpty())
			lccForComponent.setFullyMapped(true);
			
		//obtaining the list of component's children
		Collection<Component> compChildren = comp.getChildren();
		int numberOfMethods = 0 , methodsCovered = 0;
		int numberOfFields = 0, fieldsCovered = 0;
		for (Component child: compChildren)
		{
			if (child.isKind(ComponentKind.METHOD))
				numberOfMethods++;
			else if (child.isKind(ComponentKind.FIELD))
				numberOfFields++;

			Collection<Concern> concernsAssignedToChildAllRelations = new ArrayList<Concern>();
			for (EdgeKind e: EdgeKind.values()){
				List<Concern> assignedConcernsTemp = concernModelProvider.getModel().getAssignedConcerns(child, e);
				if (assignedConcernsTemp!=null)
					concernsAssignedToChildAllRelations.addAll(assignedConcernsTemp);
			}
			if(!concernsAssignedToChildAllRelations.isEmpty()){
				if (child.isKind(ComponentKind.METHOD))
					methodsCovered++;
				else if (child.isKind(ComponentKind.FIELD))
					fieldsCovered++;
				//Now asserting no multiple counting for the same concern in the component
				for (Concern c: concernsAssignedToChildAllRelations)
				{
					if (!concernsAssignedToComponentAllRelations.contains(c))
						concernsAssignedToComponentAllRelations.add(c);
				}
			}
		}
		
	    //lccForComponent.setMeasurement(concernsAssignedToComponentAllRelations.size());
	    lccForComponent.setAssignedConcerns(concernsAssignedToComponentAllRelations);
	    
	    lccForComponent.setNumberOfMethods(numberOfMethods);
	    lccForComponent.setNumberOfFields(numberOfFields);
	    lccForComponent.setMethodsCovered(methodsCovered);
	    lccForComponent.setFieldsCovered(fieldsCovered);
	    
		return lccForComponent;
	}	
	
    static boolean isNear(float lhs, float rhs)
    {
        return Math.abs(lhs - rhs) < epsilon;
    }

    /**
     * Command-line interface.
     */
	public static void main(String[] args)
			throws IOException
	{
		String pathToWorkspaceOrDatabase = null;
		String concernDomainName = null;

		if (args != null)
		{
			if (args.length >= 1)
			{
				pathToWorkspaceOrDatabase = args[0];
			}

			if (args.length >= 2)
			{
				concernDomainName = args[1];
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		if (pathToWorkspaceOrDatabase == null)
		{
//			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Enter Eclipse workspace or database directory (may contain spaces): ");
			pathToWorkspaceOrDatabase = reader.readLine();
//			reader.close();
		}

		ConcernRepository hsqldb = ConcernRepository.openDatabase(pathToWorkspaceOrDatabase, false);
		if (hsqldb == null)
		{
			System.err.println("Failed to open database: "
					+ pathToWorkspaceOrDatabase);
			return;
		}
		
		List<ConcernDomain> concernDomains = hsqldb.getConcernDomains(null);
		List<String> concernDomainsNames = new ArrayList<String>();
		for (ConcernDomain concernDomain : concernDomains) 
			concernDomainsNames.add(concernDomain.getName());
		
		if (concernDomainName == null)
		{
//			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Enter concern domain name (one of: " + concernDomains.toString() + "): ");
			concernDomainName = reader.readLine();
//			reader.close();
		}
		reader.close();
		if (concernDomainName != null && !concernDomainsNames.contains(concernDomainName))
		{
			System.err.println("Unknown concern domain: '" + concernDomainName + "'. " +
					"Expected one of: " + concernDomains.toString());
			return;
		}

		// TODO: Must pick concern-component relation
		
		// This call is needed to initialize the default IConcernModelProvider
		ConcernModelFactory.singleton().getConcernModel(
					hsqldb, concernDomainName);

		MetricsTool metricsTool = new MetricsTool(ConcernModelFactory.singleton());

		//[Bruno / 02-Oct-2014]: Choose which metric table do you need or all of them.
//		MetricsTable metricsTable = metricsTool.getMetricsForAllConcerns();
		MetricsTable metricsTable = metricsTool.getMetricsForAllConcerns();

		//[Bruno] Just to debug
		metricsTable.output(System.out);

		System.out.println();
//		metricsTool.printAssignmentStatistics();
		
		hsqldb.shutdown();
	}
	
	class DOSResult
	{
		float dos = Float.NaN;
		int totalAssignedSlocs;
		int assignedComponents;
	}
}
