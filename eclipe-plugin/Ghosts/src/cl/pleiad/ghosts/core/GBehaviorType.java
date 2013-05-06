package cl.pleiad.ghosts.core;

import java.io.PrintStream;
import java.util.Vector;

public abstract class GBehaviorType extends Ghost {
	protected Vector<GMember> members;
	protected boolean mutable;
	
	public Vector<GMember> getMembers() {
		return members;
	}

	protected GBehaviorType() {
		super();
		members = new Vector<GMember>(); //initial capacity?
	}
	
	/*public boolean equal(Object other){
		return this.name.equals(((GBehaviorType) other).name);
	}*/
	
	public void printItSelfOn(PrintStream writer) {
		writer.print(this.toString());
		writer.print("{ ");
		for (GMember member : this.getMembers()) {
			writer.print("\t");
			member.printOn(writer);	
		}
		writer.print("\n}");
	}
	
	public int similarTo(Ghost other) {
		if(other.kind() == kind() && other.getName().equals(this.getName()))
			return EQUALS;
		return DIFF;
	}

	public boolean isMutable() {
		return mutable;
	}
	
	public void setMutable(boolean _mut) {
		this.mutable = _mut;
	}

	public abstract GClass asClass() ;

	public abstract GInterface asInterface();
	
	protected void copyContentTo(GBehaviorType ghost) {
		for (GMember member : this.getMembers()) {
			member.getOwnerType().setRef(ghost);
			ghost.getMembers().add(member);
		}
		ghost.getDependencies().addAll(this.getDependencies());
	}

	
	public boolean hasProblems() {
		for (GMember member : this.getMembers()) 
			if(member.hasProblems()) 
				return true;
		return false;
	}
}
