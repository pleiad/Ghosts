package cl.pleiad.ghosts.engine;

import java.io.PrintStream;
import java.util.Observable;
import java.util.Vector;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.markers.GhostMarker;

public class GhostListener extends Observable implements IElementChangedListener, IResourceChangeListener {
	
	public GhostListener() {		
		this.registerListeners();
	}

	protected void registerListeners() {
	}
	
	@Override
	public void elementChanged(ElementChangedEvent event) {	
	}

	protected void notifyChangesToObservers() {
		super.setChanged();
		super.notifyObservers();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
	}   
}
