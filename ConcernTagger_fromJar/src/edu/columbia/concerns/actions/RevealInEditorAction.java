package edu.columbia.concerns.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.util.ProblemManager;

public class RevealInEditorAction extends Action
{
	IJavaElement javaElement;

	public RevealInEditorAction(IJavaElement javaElement)
	{
		this.javaElement = javaElement;
		setText(ConcernTagger
				.getResourceString("actions.RevealInEditorAction.Label"));
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_ELEMENT));
		setToolTipText(ConcernTagger
				.getResourceString("actions.RevealInEditorAction.ToolTip"));
	}

	@Override
	public void run()
	{
		if (javaElement == null)
			return;

		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();

		IResource resource = javaElement.getResource();

		if (resource instanceof IFile)
		{
			try
			{
				JavaUI.revealInEditor(IDE.openEditor(page, (IFile) resource),
						javaElement);
			}
			catch (PartInitException lException)
			{
				ProblemManager.reportException(lException);
			}
		}
	}
}
