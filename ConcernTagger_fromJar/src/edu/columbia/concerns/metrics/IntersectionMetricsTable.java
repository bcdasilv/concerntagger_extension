package edu.columbia.concerns.metrics;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.util.NameWithEmbeddedNumbersComparer;

public class IntersectionMetricsTable
	extends MetricsTable
{
	private static final String[] columnNames = new String[] { "Concern", "Count",
		"Concerns Tangled With" };
	
	Map<Concern, Set<Concern>> metrics = 
		new TreeMap<Concern, Set<Concern>>(new NameWithEmbeddedNumbersComparer());

	public IntersectionMetricsTable()
	{
		super(columnNames);
	}

	public void add(Concern concern, Set<Concern> tangledWithConcerns)
	{
		metrics.put(concern, tangledWithConcerns);
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
		column.setWidth(50);
		column = new TableColumn(table, SWT.LEFT);
		column.setText(columnNames[2]);
		column.setWidth(400);
	}

	@Override
	public void clear()
	{
		metrics.clear();
	}
	
	@Override
	protected void outputRows(PrintStream out)
	{
		for(Map.Entry<Concern, Set<Concern>> concernAndConcernsItsTangledWith : 
			metrics.entrySet())
		{
			out.print("\"" + 
					getColumnTextHelper(concernAndConcernsItsTangledWith, true, 0) + 
					"\"");
			out.print(',');
			out.print(
					getColumnTextHelper(concernAndConcernsItsTangledWith, true, 1));
			out.print(',');
			out.println("\"" + 
					getColumnTextHelper(concernAndConcernsItsTangledWith, true, 2) + 
					"\"");
		}
	}

	// ----------------------------------------------------
	// ITableLabelProvider implementation
	// ----------------------------------------------------
	
	@Override
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex)
	{
		return getColumnTextHelper((Map.Entry<Concern, Set<Concern>>) element,
				true,
				columnIndex);
	}

	// ----------------------------------------------------
	// IStructuredContentProvider implementation
	// ----------------------------------------------------
	
	@Override
	public Object[] getElements(Object inputElement)
	{
		return metrics.entrySet().toArray();
	}

	// ----------------------------------------------------
	// HELPER METHODS
	// ----------------------------------------------------
	
	private String getColumnTextHelper(	Map.Entry<Concern, Set<Concern>> concernAndConcernsItsTangledWith,
	                                   	boolean useShortNamesForTangledConcerns,
	                                   	int columnIndex)
	{
		switch(columnIndex)
		{
		case 0: 
			return concernAndConcernsItsTangledWith.getKey().getDisplayName();
		case 1: 
			return String.valueOf(
					concernAndConcernsItsTangledWith.getValue().size());
		case 2: 
			StringBuffer buf = new StringBuffer();

			for(Concern tangledConcern : 
				concernAndConcernsItsTangledWith.getValue())
			{
				if (buf.length() > 0)
					buf.append(", ");

				buf.append(tangledConcern.getShortDisplayName());
			}

			return buf.toString();

		default: 
			return "<ERROR>";
		}
	}
}
