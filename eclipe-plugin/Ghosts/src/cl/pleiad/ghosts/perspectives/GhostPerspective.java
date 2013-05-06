package cl.pleiad.ghosts.perspectives;

import org.eclipse.core.resources.IFolder;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import cl.pleiad.ghosts.view.GhostView;


public class GhostPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		
		layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT
				, 0.25f, editorArea);
		
		layout.addView("cl.pleiad.ghosts.view.GhostView", IPageLayout.BOTTOM
				, 0.5f, IPageLayout.ID_PROJECT_EXPLORER);
		
		layout.addView("org.eclipse.ui.views.AllMarkersView", 
				IPageLayout.BOTTOM, 0.3f, editorArea);
	}

}
