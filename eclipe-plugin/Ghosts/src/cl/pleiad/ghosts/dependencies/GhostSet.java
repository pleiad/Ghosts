package cl.pleiad.ghosts.dependencies;

import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.Ghost;


/**
 * Set of Ghosts, for a certain project
 *
 */
public class GhostSet {
	private IJavaProject project;
	private Vector<Ghost> ghosts;
	
	/**
	 * Getter function for the project of the GhostSet
	 * @return a project, represented as a IJavaProject
	 */
	public IJavaProject getProject() {
		return project;
	}
	
	/**
	 * Setter function for the project of the GhostSet
	 * @param project the Java Project which contains the GhostSet
	 * @return the GhostSet
	 */
	public GhostSet setProject(IJavaProject project) {
		this.project = project;
		return this;
	}
	
	/**
	 * Setter function for the Ghosts in the GhostSet
	 * @param ghosts the actual set of Ghosts, as a Vector
	 * @return the GhostSet
	 */	
	public GhostSet setGhosts(Vector<Ghost> ghosts) {
		this.ghosts = ghosts;
		return this;
	}
	
	/**
	 * Getter function for the Ghosts in the GhostSet
	 * @return the set of Ghosts, as a Vector
	 */	
	public Vector<Ghost> getGhosts() {
		if(ghosts==null) ghosts=new Vector<Ghost>();
		return ghosts;
	}
	
	/**
	 * 
	 * @return the string representation of the project
	 */		
	public String toString(){
		return project.getElementName();
	}
	
	/**
	 * Function that removes the references of the ghosts
	 * in the set, from the given file
	 * @param file the file which references will be removed
	 */	
	public void removeRefFrom(IFile file) {
		this.removeRefFrom(file, ghosts);
	}
	
	/**
	 * Function that removes the references of the ghost vector
	 * from the file
	 * @param file the file which references will be removed
	 * @param _ghosts vector of ghosts, to be removed from a file
	 */
	private void removeRefFrom(IFile file, Vector _ghosts){
		Vector<Ghost> discarded=new Vector<Ghost>();
		for (Object _ghost : _ghosts) {
			Ghost ghost= (Ghost) _ghost;
			Vector<ISourceRef> refs=ghost.getDependencies();
			Vector<ISourceRef> toRemove=new Vector<ISourceRef>();
			for (ISourceRef ref : refs)
				if(ref.getFile().equals(file)) toRemove.add(ref);
			
			for (ISourceRef ref : toRemove) refs.remove(ref);
			if(refs.size() == 0)	discarded.add(ghost);
			
			if(!ghost.isMember())
				this.removeRefFrom(file, ((GBehaviorType) ghost).getMembers());

		}
		for (Ghost ghost : discarded)	_ghosts.remove(ghost);
	}
	
	/**
	 * Function that detects errors in a GhostSet
	 * @return true if any ghost, in the set, has problems
	 */
	public boolean hasProblems(){
		for (Ghost ghost : this.getGhosts()) 
			if(ghost.hasProblems()) return true;
		return false;
	}
}
