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
public class LCCForComponent implements Comparable<LCCForComponent> {
	
	private Component component;
	private int lccValue;
	private boolean isFullyMapped;
	private Collection<Concern> assignedConcerns;
	private String packagePath;
	
	private int numberOfMethods;
	private int numberOfFields;
	private int methodsCovered;
	private int fieldsCovered;
	
	public LCCForComponent(Component component)
	{
		this.component = component;
		lccValue = 0;
		int elementType = component.getJavaElement().getElementType();
		if (elementType == IJavaElement.COMPILATION_UNIT){
			CompilationUnit compUnit = (CompilationUnit) component.getJavaElement();
			packagePath = compUnit.getPackage().getName().getFullyQualifiedName();
		} 
		else if (elementType == IJavaElement.TYPE) {
			IType iType = (IType)component.getJavaElement();
			packagePath = iType.getFullyQualifiedName();
		}else
			packagePath = "";
	}
	
	public Component getComponent()
	{
		return component;
	}
	
	public String getPackage(){
		return packagePath;
	}

	public int getMeasurement()
	{
		return lccValue;
	}
	
	public void setMeasurement(int m)
	{
		lccValue = m;
	}
	
	public void setAssignedConcerns(Collection<Concern> assignedConcerns) {
		this.assignedConcerns = assignedConcerns;
		lccValue = assignedConcerns.size();
	}

	public Collection<Concern> getAssignedConcerns() {
		return assignedConcerns;
	}

	public String getColumnText(int index)
	{
		if (index == 0)
			return (packagePath);
			//return (packagePath+"."+component.getName());
		else if (index == 1)
			return getValue(); // lcbc
		else if (index == 2)
			return String.valueOf(isFullyMapped()); // is Fully mapped?		
		else if (index == 3)
			return String.valueOf(getNumberOfFields()); // NOA
		else if (index == 4)
			return String.valueOf(getFieldsCovered()); // Mapped Attributes
		else if (index == 5)
			return String.valueOf(getNumberOfMethods()); // NOM
		else if (index == 6)
			return String.valueOf(getMethodsCovered()); // Mapped methods
		else {
			if((assignedConcerns != null) && (assignedConcerns.size()>0)){
				String temp = "\"";
				for (Concern con : assignedConcerns)
					temp += con.getDisplayName()+",";
				temp = temp.substring(0, temp.length()-1);
				return (temp+"\"");
			}
			return "";
		}
	}
	
	public String getValue()
	{
		return String.valueOf(lccValue);
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		//buf.append("\"" + getColumnText(0) + "\"");	// Component Name (quoted)
		buf.append(getColumnText(0));	// Component Name (quoted)
		buf.append(",");
		buf.append(getColumnText(1));	// LCC
		buf.append(",");
		buf.append(getColumnText(2));	// NOA
		buf.append(",");
		buf.append(getColumnText(3));	// Mapped Attributes
		buf.append(",");
		buf.append(getColumnText(4));	// NOM
		buf.append(",");
		buf.append(getColumnText(5));	// Mapped Methods
		buf.append(",");		
		buf.append(getColumnText(6));	// Concerns list
		return buf.toString();
	}

	@Override
	public int compareTo(LCCForComponent arg0) {
		//return NameWithEmbeddedNumbersComparer.compareTo(component, arg0.getComponent());
		if (component.getName().equals(arg0.getComponent().getName()))
			return 0;
		else
			return 1;
	}

	public void setMethodsCovered(int methodsCovered) {
		this.methodsCovered = methodsCovered;
	}

	public int getMethodsCovered() {
		return methodsCovered;
	}

	public void setFieldsCovered(int fieldsCovered) {
		this.fieldsCovered = fieldsCovered;
	}

	public int getFieldsCovered() {
		return fieldsCovered;
	}

	public void setNumberOfMethods(int numberOfMethods) {
		this.numberOfMethods = numberOfMethods;
	}

	public int getNumberOfMethods() {
		return numberOfMethods;
	}

	public void setNumberOfFields(int numberOfFields) {
		this.numberOfFields = numberOfFields;
	}

	public int getNumberOfFields() {
		return numberOfFields;
	}

	public boolean isFullyMapped() {
		return isFullyMapped;
	}

	public void setFullyMapped(boolean isFullyMapped) {
		this.isFullyMapped = isFullyMapped;
	}
}
