package edu.columbia.concerns.metrics;

import java.io.PrintStream;
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
public class LCCMetricTableExtended extends MetricsTable {

	private static final String[] columnNames = new String[] { "Component", "LCC", "Fully Mapped", "NOA", "Mapped Attributes", "NOM", "Mapped Methods", "Assigned_Concerns"};
	
	private Set<LCCForComponent> lccMeasurements = new TreeSet<LCCForComponent>();
	
	public LCCMetricTableExtended() {
		super(columnNames);
	}

	public void add(LCCForComponent lccForComponent)
	{
		lccMeasurements.add(lccForComponent);
	}
	
	@Override
	public void initializeTable(Table table)
	{
		TableColumn column;
		column = new TableColumn(table, SWT.LEFT);
		column.setText(columnNames[0]);
		column.setWidth(200);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[1]);
		column.setWidth(100);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[2]);
		column.setWidth(100);		
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[3]);
		column.setWidth(100);		
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[4]);
		column.setWidth(200);	
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[5]);
		column.setWidth(100);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[6]);
		column.setWidth(200);	
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[7]);
		column.setWidth(200);		
	}

	@Override
	public void clear()
	{
		lccMeasurements.clear();
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
		for(LCCForComponent lccForComponent : lccMeasurements)
		{
			out.println(lccForComponent);
		}
	}	
	
	@Override
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex)
	{
		return ((LCCForComponent) element).getColumnText(columnIndex);
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		return lccMeasurements.toArray();
	}	

}
