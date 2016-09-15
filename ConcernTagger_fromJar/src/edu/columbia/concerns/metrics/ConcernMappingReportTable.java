package edu.columbia.concerns.metrics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import edu.columbia.concerns.repository.Component;

/**
 * Metric Table for LCC
 * 
 * @author Bruno
 * 
 */
public class ConcernMappingReportTable extends MetricsTable {

	private static final String[] columnNames = new String[] { "Metric", "Value", "%"};
	
	public ArrayList<ConcernMappingReportMeasure> mappingReportMeasures = 
		new ArrayList<ConcernMappingReportMeasure>(4);
	
	public ConcernMappingReportTable() {
		super(columnNames);
	}
	
	@Override
	public void initializeTable(Table table)
	{
		TableColumn column;
		column = new TableColumn(table, SWT.LEFT);
		column.setText(columnNames[0]);
		column.setWidth(100);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[1]);
		column.setWidth(50);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[2]);
		column.setWidth(50);		
	}

	public void add(ConcernMappingReportMeasure cmrm)
	{
		mappingReportMeasures.add(cmrm);
	}
	
	@Override
	public void clear()
	{
		mappingReportMeasures.clear();
	}	
	
	// Overriding from MetricsTable in order to output also the csv without concern
	public void output(PrintStream out)
	{
		StringBuffer buf = new StringBuffer();
		
		for(String name : getColumnNames())
		{
			if (buf.length() > 0)
				buf.append(',');
			
			buf.append(name);
		}
		
		out.println(buf.toString());
		outputRows(out);
	}

	@Override
	protected void outputRows(PrintStream out)
	{
		for(ConcernMappingReportMeasure cmrm : mappingReportMeasures)
		{
			out.println(cmrm);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex)
	{
		return ((ConcernMappingReportMeasure) element).getColumnText(columnIndex);
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		return mappingReportMeasures.toArray();
	}

}
