package cl.pleiad.ghosts.core;

import cl.pleiad.ghosts.dependencies.TypeRef;

public class GClass extends GBehaviorType {
	private TypeRef superCls;
	
	public GClass(String name){
		super();
		this.name= name;
	}

	public TypeRef getSuperCls() {
		return superCls;
	}
	public GClass setSuperCls(TypeRef superCls) {
		this.superCls = superCls;
		return this;
	}

	@Override
	public String toString() {
		return name;//+superCls.getName();
	}

	@Override
	public int kind() {
		return CLASS;
	}

	public boolean isMutable(){
		if(!super.mutable) return false;
		
		for (GMember member : this.getMembers())
			if(member.kind()!=METHOD) return false;
	
		return true;
	}

	@Override
	public GClass asClass() {
		return this;
	}

	@Override
	public GInterface asInterface() {
		GInterface ghost=new GInterface(name);
		ghost.setMutable(true);
		this.copyContentTo(ghost);
		return ghost;
	}
	
	public GExtendedClass asExtendedClass() {
		GExtendedClass ghost = new GExtendedClass(name);
		this.copyContentTo(ghost);
		return ghost;	
	}

	@Override
	public boolean extended() {
		return false;
	}
}
