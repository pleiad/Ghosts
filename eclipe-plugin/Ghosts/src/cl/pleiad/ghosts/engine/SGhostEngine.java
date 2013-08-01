package cl.pleiad.ghosts.engine;

import java.io.PrintStream;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.markers.GhostMarker;
import cl.pleiad.ghosts.view.GhostView;

public class SGhostEngine {

	private static SGhostEngine uniqueInstance;
	
	public static SGhostEngine get() {
		if(uniqueInstance == null) 
			uniqueInstance = new SGhostEngine();
		return uniqueInstance;
	}
	
	private Vector<GhostSet> projects;
	private Vector<GhostListener> listeners;
	
	protected SGhostEngine() {
		projects = new Vector<GhostSet>();
		listeners = new Vector<GhostListener>();
		listeners.add(new GhostLoadListener());	
		this.registerListeners();
		loadGhostsFromOpenJavaProjects();
	}

	protected void registerListeners() {
		for (GhostListener listener : listeners)
			listener.registerListeners();
	}

	//TODO make it work
	public void loadNewProject(IJavaProject project) {	
		this.removeProject(project);		
		ASTGhostVisitor visitor = new ASTGhostVisitor()
								. setBlackList(GBlackList.from(project));
		projects.add(new GhostSet()
						.setProject(project)
						.setGhosts(visitor.getGhosts(), visitor.getEGhosts()));
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
						.setGhosts(visitor.getGhosts(), visitor.getEGhosts()));
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
   
	public boolean removeProject(IJavaProject project) {
		for (GhostSet set : projects)
			if (set.getProject().getElementName()
					.equals(project.getElementName())) {
				projects.removeElement(set);
				return true;
			}
		return false;
	}

	public boolean removeProjectByName(String project) {
		for (GhostSet set : projects)
			if (set.getProject().getElementName()
					.equals(project)) {
				projects.removeElement(set);
				return true;
			}
		return false;
	}

	public void addObserver(GhostView ghostView) {
		for (GhostListener listener : listeners)
			listener.addObserver(ghostView);
	}

	public void deleteObserver(GhostView ghostView) {
		for (GhostListener listener : listeners)
			listener.deleteObserver(ghostView);	
	}
	
}
