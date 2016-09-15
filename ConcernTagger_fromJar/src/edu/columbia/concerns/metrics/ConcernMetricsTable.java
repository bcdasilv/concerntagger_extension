package edu.columbia.concerns.metrics;

import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ConcernMetricsTable 
	extends MetricsTable
{
	private static final String[] columnNames = new String[] { "Concern Name", "DOSC",
		"DOSM", "CDC", "CDO", "SLOC" };

	private Set<MetricsForConcern> metrics = new TreeSet<MetricsForConcern>();

	public ConcernMetricsTable()
	{
		super(columnNames);
	}

	public void add(MetricsForConcern metricsForConcern)
	{
		metrics.add(metricsForConcern);
	}

	// ----------------------------------------------------
	// MetricsTable overrides
	// ----------------------------------------------------

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
		column.setWidth(50);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[4]);
		column.setWidth(50);
		column = new TableColumn(table, SWT.CENTER);
		column.setText(columnNames[5]);
		column.setWidth(50);
	}

	@Override
	public void clear()
	{
		metrics.clear();
	}

	@Override
	protected void outputRows(PrintStream out)
	{
		for(MetricsForConcern metricsForConcern : metrics)
		{
			out.println(metricsForConcern);
		}
	}

	//	----------------------------------------------------
	//	ITableLabelProvider implementation
	//	----------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex)
	{
		return ((MetricsForConcern) element).getColumnText(columnIndex);
	}

	//	----------------------------------------------------
	//	IStructuredContentProvider implementation
	//	----------------------------------------------------

	@Override
	public Object[] getElements(Object inputElement)
	{
		return metrics.toArray();
	}
}
