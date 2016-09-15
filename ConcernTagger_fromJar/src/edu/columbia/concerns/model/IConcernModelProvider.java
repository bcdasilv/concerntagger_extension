package edu.columbia.concerns.model;

import edu.columbia.concerns.repository.EdgeKind;

public interface IConcernModelProvider
{
	ConcernModel getModel();
	EdgeKind getConcernComponentRelation();
}
