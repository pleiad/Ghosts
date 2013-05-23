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

public class GhostDeleteListener extends GhostListener {
	
	public GhostDeleteListener() {
	}

	@Override
	protected void registerListeners() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,IResourceChangeEvent.PRE_DELETE);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource project = event.getResource();
		if (project.getType() == IResource.PROJECT) {
			String projectName = project.getName();
			System.out.println("removing: "+projectName);
			SGhostEngine.get().removeProjectByName(projectName);
		}
	}   
	
}
