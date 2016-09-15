package edu.columbia.concerns.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;


import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.repository.ConcernDomain;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.repository.EdgeKind;

public class ConcernModelFactory
	extends ConcernModelChangeManager
	implements IConcernModelProvider
{
	Map<String, ConcernModel> concernModels = new HashMap<String, ConcernModel>();

	static ConcernModel defaultConcernModel = null;
	static EdgeKind defaultConcernComponentRelation = EdgeKind.RELATED_TO;

	static IConcernModelProvider activeConcernModelProvider = null;
	
	static ConcernModelFactory factorySingleton = new ConcernModelFactory();
	
	// Can't create directly
	private ConcernModelFactory()
	{ }
	
	public static ConcernModelFactory singleton()
	{
		return factorySingleton;
	}
	
	public ConcernModel getConcernModel(String concernDomain)
	{
		// E.g., if the workspace is "C:\Workspace" then the
		// directory will be
		// "C:\Workspace\.metadata\plugins\columbia.concerntagger"
		IPath workspacePluginDir = ConcernTagger.singleton().getStateLocation();

		// True means create the database if it doesn't exist
		ConcernRepository hsqldb = ConcernRepository.openDatabase(workspacePluginDir.append("db")
				.toOSString(), true);

		return getConcernModel(hsqldb, concernDomain);
	}
	
	public ConcernModel getConcernModel(ConcernRepository hsqldb, String concernDomain)
	{
		if (concernDomain != null &&
			concernDomain.equals(ConcernRepository.DEFAULT_CONCERN_DOMAIN_NAME))
		{
			concernDomain = null;
		}
		
		ConcernDomain defaultConcernDomain = null;
		if (defaultConcernModel != null)
		{
			defaultConcernDomain = defaultConcernModel.getConcernDomain();
		}
		
		if (defaultConcernDomain != null && 
			((concernDomain == null && defaultConcernDomain.isDefault()) ||
				defaultConcernDomain.getName().equals(concernDomain)))
		{
			return defaultConcernModel;
		}
		
		ConcernModel concernModel = concernModels.get(concernDomain);
		if (concernModel != null)
			return concernModel;
		
		concernModel = new ConcernModel(hsqldb, concernDomain);
			concernModels.put(concernDomain, concernModel);

		// The first concern model created becomes the default
		if (defaultConcernModel == null)
			defaultConcernModel = concernModel;
			
		return concernModel;
	}

	public ConcernModel getDefaultConcernModel()
	{
		return defaultConcernModel;
	}
	
	public void setActiveConcernModelProvider(IConcernModelProvider concernModelProvider)
	{
		assert concernModelProvider != null;
	
		if (activeConcernModelProvider == null || 
			!activeConcernModelProvider.equals(concernModelProvider))
		{
			activeConcernModelProvider = concernModelProvider;
			
			modelChanged(ConcernEvent.createActiveConcernModelChangedEvent());
		}
	}

	public void clearActiveConcernModelProvider(ConcernModelChangeManager concernModel)
	{
		if (activeConcernModelProvider != null && 
			activeConcernModelProvider.equals(concernModel))
		{
			activeConcernModelProvider = null;
		}
	}

	@Override
	public EdgeKind getConcernComponentRelation()
	{
		if (activeConcernModelProvider != null)
			return activeConcernModelProvider.getConcernComponentRelation();
		else
			return defaultConcernComponentRelation;
	}

	@Override
	public ConcernModel getModel()
	{
		if (activeConcernModelProvider != null)
			return activeConcernModelProvider.getModel();
		else
			return defaultConcernModel;
	}
}
