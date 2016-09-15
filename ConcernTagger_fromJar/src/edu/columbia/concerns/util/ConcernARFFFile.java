/**
 * 
 */
package edu.columbia.concerns.util;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;

import edu.columbia.concerns.model.ConcernModel;
import edu.columbia.concerns.repository.Concern;

/**
 * Parses AARF files that contain a list of concerns.
 * <P>
 * We expect one &#064;ATTRIBUTE declarations, followed by a &#064;DATA
 * declaration, followed by the instance data.
 * <P>
 * Here's a minimal example:
 * <P>
 * &#064;ATTRIBUTE concern-name string<BR>
 * &#064;DATA<BR>
 * Logging<BR>
 * <P>
 * Other than these requirements, we try to be as permissive as possible and
 * ignore stuff we don't understand.
 * 
 * @author eaddy
 */
public class ConcernARFFFile extends ARFFFile
{
	private int concernNameCol = -1;
	private static final String CONCERN_NAME_ATTR_NAME = "concern-name";

	private int concernShortNameCol = -1;
	private static final String CONCERN_SHORT_NAME_ATTR_NAME = "concern-short-name";
	
	public ConcernARFFFile(String path, 
	                       ConcernModel concernModel, 
	                       IProgressMonitor progressMonitor, 
	                       IStatusLineManager statusLineManager)
	{
		super(path, concernModel, progressMonitor, statusLineManager);
	}

	@Override
	public Boolean onAttribute(List<String> fields)
	{
		if (fields.get(1).compareToIgnoreCase(CONCERN_NAME_ATTR_NAME) == 0)
		{
			if (concernNameCol != -1)
				return true; // Already assigned, ignore
			else if (!verifyAttributeDataType(fields, "string"))
				return false;
			else
				concernNameCol = currentFieldIndex;
		}
		else if (fields.get(1).compareToIgnoreCase(CONCERN_SHORT_NAME_ATTR_NAME) == 0)
		{
			if (concernShortNameCol != -1)
				return true; // Already assigned, ignore
			if (!verifyAttributeDataType(fields, "string"))
				return false;
			else
				concernShortNameCol = currentFieldIndex;
		}

		return true;
	}
	
	@Override
	public Boolean onDataInstance(List<String> cols, String raw_line)
	{
		if (concernNameCol < 0)
		{
			ProblemManager.reportError("Invalid ARFF File", 
					"Expected attribute '" + CONCERN_NAME_ATTR_NAME + "'.", 
					"File: " + path + ", Line: " + currentLine, 
					true);
			return false; // Halt further processing
		}

		assert currentFieldIndex >= 1;

		// Make sure there are enough columns

		int maxCol = Math.max(concernNameCol, concernShortNameCol);

		if (maxCol >= cols.size())
		{
			ProblemManager.reportError("Invalid ARFF Data Instance",
					"Not enough columns for data instance '" + raw_line +
					"'. Got " + cols.size() +
					", expected " + (maxCol + 1) + ". Ignoring.",
					"File: " + path + ", Line: " + currentLine, 
					true);
			return true; // Continue processing
		}
		
		// Parse Concern Name

		String concernPath = cols.get(concernNameCol);
		if (concernPath == null)
		{
			ProblemManager.reportError("Invalid ARFF Data Instance", 
					"Data instance '" + raw_line +
						"' has an empty concern-name. Ignoring.",
					"File: " + path + ", Line: " + currentLine, 
					true);
			return true; // Continue processing
		}

		String concernShortName = null;
		
		if (concernShortNameCol >= 0)
		{
			concernShortName = cols.get(concernShortNameCol);
			if (concernPath == null)
			{
				ProblemManager.reportError("Invalid ARFF Data Instance", 
						"Data instance '" + raw_line +
							"' has an empty concern-short-name. Ignoring.",
						"File: " + path + ", Line: " + currentLine, 
						true);
			}
		}

		// If concern path is a hierarchy, this will create multiple
		// concerns
		Concern concern = concernModel.createConcern(concernPath, concernShortName);
		if (concern == null)
		{
			// See if it's because of an invalid name
			String reason = Concern.isNameValid(concernPath);
			if (reason != null)
			{
				ProblemManager.reportError("Invalid Concern Name", 
						"Concern name '" + concernPath + "' is invalid, ignoring.",
						reason + "\nData instance: " + raw_line + 
							", File: " + path + ", Line: " + currentLine,
						true);
			}
			else
			{
				ProblemManager.reportError("Failed to Create Concern", 
				   		"Failed to create concern '" + concernPath + "', ignoring.",
				   		"Data instance: " + raw_line + 
				   		", File: " + path + ", Line: " + currentLine + ".",
				   		true);
			}
			return true; // Continue processing
		}
		else
		{
			++validInstances;
			progressMonitor.subTask(concern.getDisplayName());
			return true;
		}
	}
}
