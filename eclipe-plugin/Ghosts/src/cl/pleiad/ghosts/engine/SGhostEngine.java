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
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.markers.GhostMarker;



public class SGhostEngine extends Observable implements IElementChangedListener, IResourceChangeListener {

	private static SGhostEngine uniqueInstance;
	
	public static SGhostEngine get() {
		if(uniqueInstance == null) 
			uniqueInstance = new SGhostEngine();
		return uniqueInstance;
	}
	
	private Vector<GhostSet> projects;
	
	protected SGhostEngine() {		
		this.registerListeners();
		projects = new Vector<GhostSet>();	
		loadGhostsFromOpenJavaProjects();
	}

	protected void registerListeners() {
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_RECONCILE);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,IResourceChangeEvent.POST_CHANGE);
	}
	
	public void loadGhostsFrom(IJavaProject project) {	
		this.removeProject(project);
		
		ASTGhostVisitor visitor = new ASTGhostVisitor()
								. setBlackList(GBlackList.from(project));
	
			try { 
				for (IPackageFragment fPkg : project.getPackageFragments())
					for (ICompilationUnit cUnit : fPkg.getCompilationUnits())
						this.loadGhostsFrom(cUnit, visitor);
			} catch (JavaModelException e) { e.printStackTrace(); }
			
		projects.add(new GhostSet()
						.setProject(project)
						.setGhosts(visitor.getGhosts()));
	}

	protected void loadGhostsFrom(ICompilationUnit cUnit, ASTGhostVisitor visitor) {
		this.removeGhostMarkersFrom((IFile)cUnit.getResource());
		
		CompilationUnit unit = parse(cUnit);
		//unit.
		unit.accept(visitor);
		
		this.removeProblemMarkersFrom((IFile)cUnit.getResource());
	}

	private boolean isHiddenByGhost(IMarker ghost,IMarker problem) throws CoreException {
		int gStart = (Integer) ghost.getAttribute(IMarker.CHAR_START),
			gEnd =	(Integer) ghost.getAttribute(IMarker.CHAR_END),
			pStart = (Integer) problem.getAttribute(IMarker.CHAR_START),
			pEnd =	(Integer) problem.getAttribute(IMarker.CHAR_END);
			
		return gStart <= pStart && pEnd <= gEnd;
	}
	
	private void removeProblemMarkersFrom(IFile file) {
		try {
			IMarker[] ghosts = file.findMarkers(GhostMarker.ID, false, IResource.DEPTH_ONE);
			IMarker[] ghosts2 = file.findMarkers(GhostMarker.GHOSTS_PROBLEM_ID, false, IResource.DEPTH_ONE);
			for (IMarker problem : file.findMarkers(GhostMarker.JAVA_PROBLEM_ID, false, IResource.DEPTH_ONE)) {
				for (int i = 0; i < (ghosts.length + ghosts2.length); i ++) {
					IMarker ghost = i < ghosts.length?ghosts[i]:ghosts2[i-ghosts.length];
					if(problem.exists() &&
							(isHiddenByGhost(ghost, problem))) {// ||
							 //isCannotResolvedTypeOfGhost(problem))){ // this produces problems with blacklist
						problem.delete();
						break;}
				}	
			}
		} catch (CoreException e) {	e.printStackTrace(); }
	}

	private boolean isCannotResolvedTypeOfGhost(IMarker problem) throws CoreException {
		// ugly!!!!
		String msg = (String) problem.getAttribute(IMarker.MESSAGE),
			   errorMsg = " cannot be resolved to a type";
		if(msg.indexOf(errorMsg) < 0) return false; 

		//TODO ghost is not checked!!
		
		return true;
	}

	private void removeGhostMarkersFrom(IFile file) {
		try {
			GhostMarker.deleteMarks(file);
		} catch (CoreException e) { e.printStackTrace(); }
	}

	public static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3); 
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}
	
	public Vector<GhostSet> getProjects() {
		return projects;
	}

	private void loadGhostsFromOpenJavaProjects() {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			try {
				if (project.isOpen() && 
					project.isNatureEnabled(JavaCore.NATURE_ID)) {
					IJavaProject jProject=JavaCore.create(project);
					this.loadGhostsFrom(jProject);
				}

			} catch (CoreException e) { e.printStackTrace(); }
		}
	}
	
	public void printOn(PrintStream writer) {
		writer.println("**------- GHOSTS");
		for (GhostSet pjt : this.projects) {
			writer.println("JavaProject: "+pjt);
			for (Ghost ghost : pjt.getGhosts()) {
				ghost.printOn(writer);
			}
		}
		writer.println("<<<<<<<<<");
	}
	
	@Override
	public void elementChanged(ElementChangedEvent event) {
		//System.out.println("changed!! "+event);
		// TODO do not trace file removed or changed in the background!
		IJavaElementDelta delta = event.getDelta();
		//if(delta.getAffectedChildren().length > 0){
			IJavaElement part=delta.getElement();
			for (GhostSet set : projects) 
				if (set.getProject().getElementName()
						.equals(part.getJavaProject().getElementName())) {
					//this.removeGhostMarkersFrom((IFile) part.getResource());
					//this.removeProblemMarkersFrom((IFile) part.getResource());
					set.removeRefFrom((IFile) part.getResource());
					ASTGhostVisitor visitor = new ASTGhostVisitor()
									.setBlackList(GBlackList.from(set.getProject()))
									.setGhosts(set.getGhosts());
					this.loadGhostsFrom((ICompilationUnit) part, visitor);
					this.notifyChangesToObservers();
					return;
				}
		//}		
	}

	protected void notifyChangesToObservers() {
		super.setChanged();
		super.notifyObservers();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		//System.out.println("BBBBBBBBBBBBBBBBchanged!! "+event);
		//TODO it catches them
		IResourceDelta root = event.getDelta();		
		if (root.getResource().getType() == IResource.ROOT)
			for (IResourceDelta projectDelta : root.getAffectedChildren(IResource.PROJECT))
				if (projectDelta.getFlags() == IResourceDelta.OPEN) {
					Job job = new LoadProjectJob((IProject) projectDelta.getResource());
					job.setPriority(Job.INTERACTIVE);
					job.schedule();	
					this.notifyChangesToObservers(); //In the job will be a slightly faster
				}
	}   
	
	protected class LoadProjectJob extends Job {

		private IProject project;

		protected LoadProjectJob(IProject _pjt) {
			super("Loading Project: " + _pjt.getName());
			this.project = _pjt;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {

				if (project.isOpen()) {
					if (project.isNatureEnabled(JavaCore.NATURE_ID))
						// project.set
						loadGhostsFrom(JavaCore.create(project));
				} else
					removeProject(JavaCore.create(project));
			} catch (CoreException e) {
				e.printStackTrace();
			}

			return Status.OK_STATUS;
		}

	}
	private boolean removeProject(IJavaProject project) {
		for (GhostSet set : projects)
			if (set.getProject().getElementName()
					.equals(project.getElementName())) {
				projects.removeElement(set);
				return true;
			}
		return false;
	}
	
}
