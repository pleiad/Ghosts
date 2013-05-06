package cl.pleiad.ghosts.view;

import java.util.HashMap;
import java.util.Observer;
import java.util.Vector;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.engine.SGhostEngine;


public class GViewTreeContentProvider implements ITreeContentProvider {

	//private GTreeNode root;
	
	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		/*if(newInput!= null)
			root=(GTreeNode) newInput;*/
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return ((GTreeNode) inputElement).getChildren().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return ((GTreeNode) parentElement).getChildren().toArray();
	}

	@Override
	public Object getParent(Object element) {
		return ((GTreeNode) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return ((GTreeNode) element).hasChildren();
	}
	
	
	/*
	 * Below there is the pior definition, this is also cleaned in the svn 
	 * 
	 */
	
	
	/*private Vector<GhostSet> model;

	
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		if(newInput != null)model=((SGhostEngine) newInput).getProjects();
		//viewer.refresh();	
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof SGhostEngine) 
			return ((SGhostEngine) inputElement).getProjects().toArray();
		
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		// check this!!! may be null in root!
		if(parentElement instanceof GhostSet)
			return ((GhostSet) parentElement).getGhosts().toArray();
		
		if(parentElement instanceof GMember) return new Object[0];
		
		if(parentElement instanceof GBehaviorType)
			return ((GBehaviorType) parentElement).getMembers().toArray();
		
		return null;
	}

	@Override
	public Object getParent(Object element) { //will be too slow!!!
		// check this!!! may be null in root!
		//if(element instanceof GhostSet) return model; 
		if(element instanceof GBehaviorType){
			GBehaviorType ghost = (GBehaviorType) element;
			for (GhostSet set : model)
				for (Ghost other : set.getGhosts())
					if(ghost == other ) return set;
		}
			
		if(element instanceof GMember){
			GMember member= (GMember) element;
			if(member.getOwnerType().isConcrete()){ 
				for (GhostSet set : model)
					for (Ghost ghost : set.getGhosts())
						if(ghost == member) return set;		
			}else{	// could return (Ghost) member.getOwnerType().getRef()
				for (GhostSet set : model)
					for (Ghost ghost : set.getGhosts())
						if(!ghost.isMember() &&
								ghost.getName()
									.equals(member.getOwnerType().getName()))
							return ghost;
			}
		}	
		
		
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		Object[] children = this.getChildren(element); //may be slow!!
		return children!=null && children.length > 0;
	}*/

}
