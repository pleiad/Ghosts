package cl.pleiad.ghosts.core;

/*
 * no constructors;
 * no static methods; 
 */

public class GInterface extends GBehaviorType {
	
	public GInterface(String name) {
		super();
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int kind() {
		return INTERFACE;
	}

	@Override
	public GClass asClass() {
		GClass ghost = new GClass(name);
		this.copyContentTo(ghost);
		return ghost;
	}

	@Override
	public GInterface asInterface() {
		return this;
	}
}
