package cl.pleiad.ghosts.view;

import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import cl.pleiad.ghosts.GhostsPlugin;
import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;


public class GViewLavelProvider extends LabelProvider {
	
	private final static String relativePath = "img/";
	public static Image getImageNamed(String name){
		try {

			InputStream inputStream = FileLocator
					.openStream(GhostsPlugin.getDefault().getBundle()
					, new Path(relativePath+name),
					false);

			return new Image(Display.getDefault(), inputStream);
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}
	
	private  static Image ghost_member;
	private final static Image getGhostMember(){
		if(ghost_member== null) ghost_member=getImageNamed("public_co.png");
		return  ghost_member;
	}
	
	public String getText(Object obj) {
		return obj.toString();
	}
	
	public Image getImage(Object obj){
		GTreeNode node = (GTreeNode) obj;
		switch (node.getKind()) {
		case GTreeNode.GHOST_SET: 
			return ghostSetIcon((GhostSet) node.getValue());
					
		case GTreeNode.GBTYPE:  
			return ghostBTypeIcon((GBehaviorType) node.getValue());
		case GTreeNode.GMEMBER: 
			return ghostMemberIcon((GMember) node.getValue());
		}
		
		return null;
	}


	private Image ghostSetIcon(GhostSet value) {
		Image base=PlatformUI.getWorkbench().getSharedImages()
				.getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT);
		if(value.hasProblems()) return decorateAsProblem(base);
		return base;
	}

	private Image decorateAsProblem(Image base) {
		DecorationOverlayIcon decorator=new DecorationOverlayIcon
				(base
				,PlatformUI.getWorkbench().getSharedImages()
					.getImageDescriptor(org.eclipse.ui.ISharedImages.IMG_DEC_FIELD_ERROR)
				,IDecoration.BOTTOM_LEFT);
		
		return decorator.createImage();	
	}

	private Image ghostMemberIcon(GMember member) {
		String imageName= ISharedImages.IMG_FIELD_PUBLIC;
		
		/*if ( member.kind()==Ghost.CONSTRUCTOR )
			  imageName= ISharedImages.img;*/
		
		Image base=JavaUI.getSharedImages().getImage(imageName);
		
		if(member.hasProblems()) return decorateAsProblem(base);
		return base;
	}

	private Image ghostBTypeIcon(GBehaviorType ghost) {
		String imageName= ISharedImages.IMG_OBJS_INTERFACE;
		
		if ( ghost.kind()==Ghost.CLASS )
			  imageName= ISharedImages.IMG_OBJS_CLASS;
		
		Image base=JavaUI.getSharedImages().getImage(imageName);
		
		
		if(ghost.hasProblems()) return decorateAsProblem(base);
		return base;
	}
	
	
	

}
