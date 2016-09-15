package edu.columbia.concerns.metrics;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.util.NameWithEmbeddedNumbersComparer;

/**
 * Class to keep track of all metrics associated with an object. The object can
 * be a concern or a component
 * 
 * @author vgarg
 * 
 */
public class MetricsForConcern implements Comparable<MetricsForConcern>
{
	private Concern concern;
	List<Object> metrics;

	public MetricsForConcern(Concern concern)
	{
		this.concern = concern;
	}

	public Concern getConcern()
	{
		return concern;
	}

	public List<Object> getMeasurements()
	{
		return metrics;
	}

	public void addMetric(float metric)
	{
		if (metrics == null)
			metrics = new ArrayList<Object>();

		metrics.add(metric);
	}

	public void addMetric(int metric)
	{
		if (metrics == null)
			metrics = new ArrayList<Object>();

		metrics.add(metric);
	}

	public String getColumnText(int index)
	{
		if (index == 0)
			return concern.getDisplayName();
		else
			return getValueWithPrecision(index-1);
	}
	
	public String getValueWithPrecision(int index)
	{
		Object objVal = metrics.get(index);
		if (objVal instanceof Integer)
			return objVal.toString();
		
		float val = ((Float) objVal).floatValue();
		if (Float.isNaN(val))
			return String.valueOf(val);
		else
			return String.format("%5.3f", Math.abs(val));
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("\"" + getColumnText(0) + "\"");	// Concern Name (quoted)
		buf.append(",");
		buf.append(getColumnText(1));	// DOSC
		buf.append(",");
		buf.append(getColumnText(2));	// DOSM
		buf.append(",");
		buf.append(getColumnText(3));	// CDC
		buf.append(",");
		buf.append(getColumnText(4));	// CDO
		buf.append(",");
		buf.append(getColumnText(5));	// SLOC
		return buf.toString();
	}

	@Override
	public int compareTo(MetricsForConcern arg0)
	{
		return NameWithEmbeddedNumbersComparer.compareTo(concern, arg0.getConcern());
	}
}
