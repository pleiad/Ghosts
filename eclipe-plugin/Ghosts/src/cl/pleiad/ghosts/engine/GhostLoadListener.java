package cl.pleiad.ghosts.engine;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Observable;
import java.util.Vector;

import javax.swing.JPopupMenu;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.markers.GhostMarker;

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
