package cl.pleiad.ghosts.backends;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class GhostFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(element instanceof ICompilationUnit) {
            return !((ICompilationUnit) element).getResource().getFileExtension().equals("ghost");
        }
		return true;
	}

}
