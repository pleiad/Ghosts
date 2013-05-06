package cl.pleiad.ghosts.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.dependencies.MarkerSourceRef;




public class GhostMarker{
	public static final String ID = "cl.pleiad.ghosts.GhostMarker";
	public static final String CONTEXT_ID = "cl.pleaid.ghosts.GContextMarker";
	public static final String JAVA_PROBLEM_ID = "org.eclipse.jdt.core.problem";
	public static final String GHOSTS_PROBLEM_ID = "cl.pleiad.ghosts.GJavaProblem";
	
	public static final String STORED_KIND_ID= "cl.pleiad.ghosts.StoredKind";
	public static final String Att_storedKind = "ghost.StoredKind";
	public static final String Att_ghostName = "ghost.Name";
	
	public static final String Att_isJavaProblem = "Att_isJavaProblem";
	public static final String Att_ghostSignature = "Att_ghostSignature";
	
	
	public static void createGCxtMark(IResource file,int line) throws CoreException{
		
		IMarker marker;
			marker = file.createMarker(GhostMarker.CONTEXT_ID);
			marker.setAttribute(IMarker.LINE_NUMBER, line);
			marker.setAttribute(IMarker.MESSAGE,"In this context, there are ghosts");
			//marker.setAttribute(IMarker.CHAR_START, startChar);
			//marker.setAttribute(IMarker.CHAR_END, endChar);

	}
	
	public static boolean existGCxtMarkIn(int line, IResource file) throws CoreException{
		for (IMarker mark : file.findMarkers(
				GhostMarker.CONTEXT_ID, false, IResource.DEPTH_ONE)) {
			if(((Integer)mark.getAttribute(IMarker.LINE_NUMBER)) == line) return true;
		}
		return false;
	}
	
	public static void deleteMarks(IResource file) throws CoreException{
			file.deleteMarkers(GhostMarker.ID, false, IResource.DEPTH_ONE);
			file.deleteMarkers(GhostMarker.CONTEXT_ID, false, IResource.DEPTH_ONE);
			file.deleteMarkers(GHOSTS_PROBLEM_ID, false, IResource.DEPTH_ONE);
	}

	public static void createJavaProblemMarkersFrom(GMember ghost) {

		for (ISourceRef _ref : ghost.getDependencies()) {
			MarkerSourceRef ref= (MarkerSourceRef) _ref;
			IFile file=ref.getFile();
			IMarker ex_ghost= ((MarkerSourceRef) _ref).getGhostMarker();
			if(ex_ghost.exists())
				try {
					IMarker problem=file.createMarker(GHOSTS_PROBLEM_ID);
					
					problem.setAttributes(ex_ghost.getAttributes());
					problem.setAttribute(IMarker.TRANSIENT, true);
					problem.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					problem.setAttribute(IMarker.MESSAGE, "Incompatible definition: "+ex_ghost.getAttribute(IMarker.MESSAGE));
					//problem.setAttribute(GhostMarker.Att_isJavaProblem, true);
					ex_ghost.delete();
					ref.setMarker(problem);
				} catch (CoreException e) { e.printStackTrace(); }
		}
	}

	public static void createStoredKind(IJavaProject project, GBehaviorType ghost) throws CoreException{
		IMarker marker=project.getResource().createMarker(GhostMarker.STORED_KIND_ID);
		marker.setAttribute(GhostMarker.Att_storedKind, ghost.kind());
		marker.setAttribute(GhostMarker.Att_ghostName, ghost.getName());
	}

	public static int getStoredKind(IJavaProject javaProject, String qName) {
		try {
			for (IMarker mark : javaProject.getResource().findMarkers(GhostMarker.STORED_KIND_ID, false, IResource.DEPTH_ONE))
				if(qName.equals((String)mark.getAttribute(Att_ghostName)))
					return (Integer)mark.getAttribute(Att_storedKind);
		} catch (CoreException e) { e.printStackTrace(); }
		return -1;
	}
	
	public static boolean setStoredKind(IJavaProject javaProject, String qName, int newKind) {
		try {
			for (IMarker mark : javaProject.getResource().findMarkers(GhostMarker.STORED_KIND_ID, false, IResource.DEPTH_ONE))
				if(qName.equals((String)mark.getAttribute(Att_ghostName))){
					mark.setAttribute(Att_storedKind, newKind);
					return true;
				}
		} catch (CoreException e) { e.printStackTrace(); }
		return false;
	}
}
