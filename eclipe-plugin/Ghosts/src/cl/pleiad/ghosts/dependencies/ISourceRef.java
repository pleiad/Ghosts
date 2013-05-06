package cl.pleiad.ghosts.dependencies;

import java.io.PrintStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Interface for class members information
 *
 */
public interface ISourceRef {
	
	/**
	 * Returns the line number, in the file, of the reference 
	 * @return the line number
	 */
	public int getLineNumber();
	
	/**
	 * Returns the file to which the reference refers  
	 * @return the file, as a IFile
	 */	
	public IFile getFile();
	
	/**
	 * String representation of a Source Reference
	 * @return the reference, as a String
	 */
	public String toString();
	
	/**
	 * Function that returns the ghost marker in the file
	 * @return a Ghost Marker
	 */
	public IMarker getGhostMarker();
}
