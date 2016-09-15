/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.22 $
 */

package edu.columbia.concerns;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.repository.ConcernRepository;
import edu.columbia.concerns.util.ProblemManager;

/**
 * The main plugin class to be used in the desktop.
 * 
 * @author vibhav garg
 */
public class ConcernTagger extends AbstractUIPlugin
{
	/** An ID for the plugin (same as in the plugin.xml file). */
	public static final String ID_PLUGIN = "edu.columbia.concerns";

	// The shared instance.
	private static ConcernTagger singleton;

	// Resource bundle.
	private ResourceBundle resourceBundle;

	// Reference to HSQLDB object
	private ConcernRepository repository;

	/**
	 * The constructor. Loads the resource bundle.
	 */
	public ConcernTagger()
	{
		assert singleton == null;
		
		singleton = this;

		String resourceClass = "edu.columbia.concerns.ConcernTaggerResources";
		
		try
		{
			resourceBundle = ResourceBundle
					.getBundle(resourceClass);
		}
		catch (MissingResourceException lException)
		{
			ProblemManager.reportException(new Exception(
				"Missing Resource file: " + resourceClass));
		}

		// JavaCore.addElementChangedListener(new Checker());
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);

		// Create the default concern model
		ConcernModelFactory.singleton().getConcernModel(getRepository(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		repository.shutdown();
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance.
	 * 
	 * @return The shared instance.
	 */
	public static ConcernTagger singleton()
	{
		return singleton;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 * 
	 * @param pKey
	 *            The key to use for the property lookup
	 * @return A string representing the resource.
	 */
	public static String getResourceString(String pKey)
	{
		final ResourceBundle lBundle = ConcernTagger.singleton()
				.getResourceBundle();
		try
		{
			return (lBundle != null) ? lBundle.getString(pKey) : pKey;
		}
		catch (MissingResourceException lException)
		{
			return pKey;
		}
	}

	/**
	 * @return The plugin's resource bundle
	 */
	public ResourceBundle getResourceBundle()
	{
		return resourceBundle;
	}

	/**
	 * Get the reference to the database
	 * 
	 * @return
	 */
	private ConcernRepository getRepository()
	{
		if (repository == null)
		{
			// E.g., if the workspace is "C:\Workspace" then the
			// directory will be
			// "C:\Workspace\.metadata\plugins\columbia.concerntagger"
			IPath workspacePluginDir = this.getStateLocation();

			// True means create the database if it doesn't exist
			repository = ConcernRepository.openDatabase(workspacePluginDir.append("db")
					.toOSString(), true);
		}

		return repository;
	}
}
