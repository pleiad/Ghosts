package cl.pleiad.ghosts.engine;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.dependencies.GhostSet;

public class GhostLoadListener extends GhostListener {
	
	public GhostLoadListener() {
	}

	@Override
	protected void registerListeners() {
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_RECONCILE);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,IResourceChangeEvent.POST_CHANGE);
	}
	
	@Override
	public void elementChanged(ElementChangedEvent event) {
		//System.out.println("changed!! "+event);
		// TODO do not trace file removed or changed in the background!
		IJavaElementDelta delta = event.getDelta();
		//if(delta.getAffectedChildren().length > 0){
			IJavaElement part = delta.getElement();
			for (GhostSet set : SGhostEngine.get().getProjects()) 
				if (set.getProject().getElementName()
						.equals(part.getJavaProject().getElementName())) {
					//this.removeGhostMarkersFrom((IFile) part.getResource());
					//this.removeProblemMarkersFrom((IFile) part.getResource());
					set.removeRefFrom((IFile) part.getResource());
					ASTGhostVisitor visitor = new ASTGhostVisitor()
									.setBlackList(GBlackList.from(set.getProject()))
									.setGhosts(set.getGhosts());
					SGhostEngine.get().loadGhostsFrom((ICompilationUnit) part, visitor);
					this.notifyChangesToObservers();
					return;
				}
		//}		
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		//TODO it catches them
		IResourceDelta root = event.getDelta();
		ResourceGhostVisitor visitor = new ResourceGhostVisitor(this);
		try {
			root.accept(visitor);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
