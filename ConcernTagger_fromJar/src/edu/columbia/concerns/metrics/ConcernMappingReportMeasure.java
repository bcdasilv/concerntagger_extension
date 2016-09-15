package edu.columbia.concerns.metrics;

import java.util.Collection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Type;

import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.util.NameWithEmbeddedNumbersComparer;

/**
 * This class encapsulates the lcc count for a given component
 * 
 * @author Bruno
 * 
 */
public class ConcernMappingReportMeasure implements Comparable<ConcernMappingReportMeasure> {
	
	public static final int CLASSES_COVERED = 0;
	public static final int METHODS_COVERED = 1;
	public static final int FIELDS_COVERED = 2;
	public static final int LOC_COVERED = 3;
	
	private final int metric;
	
	private int value;
	
	private int percent;
	
	
	public ConcernMappingReportMeasure(int metric, int v, int p)
	{
		this.metric = metric;
		value = v;
		percent = p;
	}

	public String getColumnText(int index)
	{
		if (index == 0){
			switch(getMetric()){
				case CLASSES_COVERED: return "Classes covered";
				case METHODS_COVERED: return "Methods covered";
				case FIELDS_COVERED: return "Fields covered";
				case LOC_COVERED: return "LOC covered"; 
				default: return "Invalid metric";
			}
		}else if (index == 1)
			return String.valueOf(value);
		else
			return String.valueOf(percent);
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(getColumnText(0));	// Metric
		buf.append(",");
		buf.append(getColumnText(1));	// Value
		buf.append(",");
		buf.append(getColumnText(2));	// Percent
		return buf.toString();
	}

	@Override
	public int compareTo(ConcernMappingReportMeasure arg0) {
		if (getMetric() == arg0.getMetric())
			return 0;
		else
			return 1;
	}

	public int getMetric() {
		return metric;
	}
	
}
