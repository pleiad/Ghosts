package cl.pleiad.ghosts.dependencies;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;

import cl.pleiad.ghosts.markers.GhostMarker;

/**
 * Class that contains the necessarily information
 * of a given GhostMarker in a file 
 *
 */
public class MarkerSourceRef implements ISourceRef {

	private IMarker ghostMarker;
	private ASTNode node;
	
	public MarkerSourceRef(IFile file,int line, int startChar
			,int endChar,String description, ASTNode n) throws CoreException{
		ghostMarker=file.createMarker(GhostMarker.ID);
		ghostMarker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
		ghostMarker.setAttribute(IMarker.LINE_NUMBER, line);
		ghostMarker.setAttribute(IMarker.CHAR_START, startChar);
		ghostMarker.setAttribute(IMarker.CHAR_END, endChar);
		ghostMarker.setAttribute(IMarker.MESSAGE, description);
		node = n;
	}
	
	
	public IMarker getGhostMarker() {
		return ghostMarker;
	}
	
	public ASTNode getNode() {
		return node;
	}

	@Override
	public int getLineNumber() {
		try {
			if(ghostMarker.exists())
				return (Integer) ghostMarker.getAttribute(IMarker.LINE_NUMBER);
		} 
		catch (CoreException e) { e.printStackTrace();}
		return -1;
	}

	@Override
	public IFile getFile() {
		return (IFile)ghostMarker.getResource();
	}

	public String toString(){
		return "in "+this.getFile().getName()+" : L"+this.getLineNumber();
	}


	public MarkerSourceRef setMarker(IMarker problem) {
		this.ghostMarker = problem;
		return this;
	}
}
