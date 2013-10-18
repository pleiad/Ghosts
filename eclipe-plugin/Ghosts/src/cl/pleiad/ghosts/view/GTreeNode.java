package cl.pleiad.ghosts.view;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.markers.GhostMarker;


public class GTreeNode {
	
	public final static int ROOT = 1;
	public final static int GHOST_SET = 1001;
	public final static int GBTYPE = 1101;
	public final static int DEPEND_LIST = 1111;
	public final static int GMEMBER = 1201;
	public final static int DEPENDENCY = 1211;
	
	
	public static GTreeNode from(CopyOnWriteArrayList<GhostSet> projects){
		GTreeNode root=new GTreeNode()
							.setKind(ROOT)
							.setName("invisible root")
							.setValue(projects);
	
		for (GhostSet ghostSet : projects)
			root.addChild(from(ghostSet));//TODO here
		
		
		return root;
	} 
		
	private static GTreeNode from(GhostSet ghostSet) {
		GTreeNode current=new GTreeNode()
								.setKind(GHOST_SET)
								.setName(ghostSet.getProject().getElementName())
								.setValue(ghostSet);
		
		Object[] setGhosts = ghostSet.getGhosts().toArray();
		Arrays.sort(setGhosts);
		
		for (Object ghost : setGhosts) {
				if(((Ghost) ghost).isMember())	current.addChild(from((GMember)ghost));
				else if(!((Ghost) ghost).isVariable()) current.addChild(from((GBehaviorType)ghost));
		}
		return current;
	}

	private static GTreeNode from(GMember ghost) {
		GTreeNode current=new GTreeNode()
								.setKind(GMEMBER)
								.setName(ghost.toString())
								.setValue(ghost);
		
		if(!GhostView.refsInMenu)
			for (ISourceRef ref : ghost.getDependencies())
				current.addChild(from(ref));
	
		return current;
	}
	
	private static GTreeNode from(ISourceRef ref) {
		if (ref != null)
			return new GTreeNode()
						.setKind(DEPENDENCY)
						.setName(ref.toString())
						.setValue(ref);
		else
			return null;
	}


	private static GTreeNode from(GBehaviorType ghost) {
		GTreeNode current=new GTreeNode()
								.setKind(GBTYPE)
								.setName(ghost.toString())
								.setValue(ghost);
		
		Object[] members = ghost.getMembers().toArray();
		Arrays.sort(members);
		for (Object mem : members)
			current.addChild(from((GMember) mem));
		
		if(!GhostView.refsInMenu) 
			current.addChild(fromDependencies(ghost.getDependencies()));
		
		return current;
	}


	private static GTreeNode fromDependencies(Vector<ISourceRef> dependencies) {
		GTreeNode dlist=new GTreeNode()
								.setKind(DEPEND_LIST)
								.setName("["+dependencies.size()+"] occurrences")
								.setValue(dependencies);
		
		for (ISourceRef ref : dependencies)
			dlist.addChild(from(ref));
		
		return dlist;
	}


	private String name;
	private Object value;
	private GTreeNode parent;
	private int valueKind;
	private Vector<GTreeNode> children=new Vector<GTreeNode>();
	
	
	public String toString(){
		return name;
	}
	
	public String getName() {
		return name;
	}
	public GTreeNode setName(String name) {
		this.name = name;
		return this;
	}
	public Object getValue() {
		return value;
	}
	public GTreeNode setValue(Object value) {
		this.value = value;
		return this;
	}
	public GTreeNode getParent() {
		return parent;
	}
	public GTreeNode setParent(GTreeNode parent) {
		this.parent = parent;
		return this;
	}
	public int getKind() {
		return valueKind;
	}
	
	public GTreeNode setKind(int valueKind) {
		this.valueKind = valueKind;
		return this;
	}
	
	public Vector<GTreeNode> getChildren() {
		return children;
	}
	
	public GTreeNode addChild(GTreeNode child) {
		if (child != null) {
			child.setParent(this);
			children.add(child);
		}
		return this;
	}
	
	public boolean hasChildren(){
		return children.size() > 0;
	}

	public void replaceGhost(GBehaviorType ghost) {
		GhostSet set=(GhostSet) this.getParent().getValue();
		
		if(GhostMarker.setStoredKind(set.getProject(), ghost.getName(), ghost.kind())){
			set.getGhosts().remove(value);
			set.getGhosts().add(ghost);
			value=ghost;
		}
	}
	
	public boolean isFree(){
		return this.getKind() == GTreeNode.GHOST_SET
				|| this.getParent().getKind() == GTreeNode.GHOST_SET;
	}
	
}
