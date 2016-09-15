package edu.columbia.concerns.model;

import edu.columbia.concerns.repository.EdgeKind;

public interface IConcernModelProviderEx extends IConcernModelProvider
{
	void setConcernDomain(String concernDomain);
	void setConcernComponentRelation(EdgeKind edgeKind);
}
