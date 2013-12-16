package cl.pleiad.ghosts.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import cl.pleiad.ghosts.markers.GhostMarker;
import cl.pleiad.ghosts.view.GViewLabelProvider;




public class GhostJavaElementDecorator extends BaseLabelProvider implements ILightweightLabelDecorator{

	public final static ImageDescriptor ghostDecorator= ImageDescriptor.createFromImage(GViewLabelProvider.getImageNamed("ghost_dot.png"));
	
	@Override
	public void decorate(Object element, IDecoration decoration) {
		IJavaElement javaElement = (IJavaElement) element;
		boolean flag=false;
		switch (javaElement.getElementType()) {
			case IJavaElement.COMPILATION_UNIT: //flag=hasGhosts((ICompilationUnit) element); break;
			case IJavaElement.PACKAGE_FRAGMENT: //flag=hasGhosts((IPackageFragment) element); break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT: flag=hasGhosts(javaElement); break; //IPackageFragmentRoot
			case IJavaElement.JAVA_PROJECT: flag=((IJavaProject) javaElement).isOpen() 
												&& hasGhostsInProject((IJavaProject)javaElement) ; break;
		}
		if(! flag) return;
		decoration.addOverlay(ghostDecorator, IDecoration.TOP_RIGHT);
	}
	
	private boolean hasGhosts(IJavaElement element) { //ICompilationUnit
		try {
			IResource resource=element.getResource();
			if(resource==null) return false;
			return resource.findMarkers(GhostMarker.CONTEXT_ID, true, IResource.DEPTH_ONE).length > 0;
		} catch (CoreException e) { e.printStackTrace(); }
		return false;
	}

	private boolean hasGhostsInProject(IJavaProject project) { //ICompilationUnit
		
		try {
			for (IJavaElement element : project.getAllPackageFragmentRoots())
				if(hasGhosts(element)) return true;
		} catch (JavaModelException e) {e.printStackTrace();}
		
		return false;
	}
	
	
}
