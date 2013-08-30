package cl.pleiad.ghosts.core;

import java.util.ArrayList;

public class GExtendedClass extends GClass {
	private ArrayList<String> extenders;
	
	public GExtendedClass(String name){
		super(name);
		this.extenders = new ArrayList<String>();
	}
	
	public void addExtender(String className) {
		if (!this.extenders.contains(className))
			this.extenders.add(className);
	}
	
	public void removeExtender(String className) {
		this.extenders.remove(className);
	}
	
	public ArrayList<String> getExtenders() {
		return this.extenders;
	}
	
}
