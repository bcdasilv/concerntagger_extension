package edu.columbia.concerns.metrics;

import java.io.PrintStream;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public abstract class MetricsTable
	implements 
		ITableLabelProvider, 
		IContentProvider, 
		IStructuredContentProvider
{
	private final String[] columnNames;
	
	public MetricsTable(String[] columnNames)
	{
		this.columnNames = columnNames;
	}
	
	public TableViewer createTableViewer(Composite parent)
	{
		int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
			| SWT.FULL_SELECTION | SWT.HIDE_SELECTION;

		Table table = new Table(parent, style);
		
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 3;
		table.setLayoutData(gridData);
		
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		initializeTable(table);
		
		TableViewer viewer = new TableViewer(table);
		viewer.setColumnProperties(columnNames);
		viewer.setContentProvider(this);
		viewer.setLabelProvider(this);
		
		return viewer;
	}

	// For outputting to the console or saving to a file
	
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

	public String[] getColumnNames()
	{
		return columnNames;
	}
	
	// Subclasses must implement these
	abstract protected void initializeTable(Table table);
	abstract public void clear();
	abstract protected void outputRows(PrintStream out);

	// ----------------------------------------------------
	// ITableLabelProvider implementation
	// ----------------------------------------------------
	
	@Override
	public Image getColumnImage(Object element, int columnIndex)
	{
		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) { }

	@Override
	public void dispose() { }

	@Override
	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener)	{ }

	// ----------------------------------------------------
	// IContentProvider implementation
	// ----------------------------------------------------
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }
}
