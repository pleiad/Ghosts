package cl.pleiad.ghosts.core;

import java.io.PrintStream;

import org.eclipse.core.runtime.CoreException;

import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.dependencies.TypeRef;
import cl.pleiad.ghosts.markers.GhostMarker;


public abstract class GMember extends Ghost implements Comparable<GMember>{
	
	protected TypeRef returnType;
	protected boolean staticMember; 	
	protected TypeRef ownerType;
	
	public TypeRef getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(TypeRef ownerType) {
		this.ownerType = ownerType;
	}

	public boolean isStaticMember() {
		return staticMember;
	}
	
	
	public int compareTo(GMember other) { //compared by name
		if(this.kind() == CONSTRUCTOR) return -1;
		if(other.kind() == CONSTRUCTOR) return 1;
		return this.name.compareTo(other.getName());
	}

	/*public void setStaticMember(boolean staticMember) {
		this.staticMember = staticMember;
	}*/

	protected GMember(String name, boolean staticMember) {
		super();
		this.name = name;
		this.staticMember = staticMember;
	}
	

	public TypeRef getReturnType() {
		return returnType;
	}

	public void setReturnType(TypeRef returnType) {
		this.returnType = returnType;
		//return this;
	}
	
	public void printItSelfOn(PrintStream writer){
		writer.print(this.toString());
	}

	public boolean isMember(){return true;}

	public void setStaticMember(boolean staticMember) {
		this.staticMember = staticMember;
	}

	public GMember absorb(GMember member) {
		this.getDependencies().addAll(member.getDependencies());			
		return this;
	}
	
	
	public int similarTo(Ghost other) {
		if(other.kind() != kind()) return DIFF;
		
		if(!this.getOwnerType().equals(((GMember)other).getOwnerType())) return DIFF;
		
		if(!other.getName().equals(this.getName())) return DIFF;
		
		if(!similarReturnTypesTo((GMember)other))	return NAME_KIND;
			
		return additionalChecks((GMember) other);
	}

	protected abstract int additionalChecks(GMember other);

	protected abstract boolean similarTypesTo(GMember other);

	public boolean similarReturnTypesTo(GMember other) {
		return returnType.equals(other.returnType);
	}
	
	public String toString() {
		return this.toSimpleString() +  (ownerType.isConcrete()?" in "+ownerType.getName():"");
	}
	public abstract String toSimpleString();
	
	public boolean hasProblems() {
		for (ISourceRef ref : this.getDependencies())
			try {
				//TODO check if this affects the view
				if(ref.getGhostMarker() != null &&
				   ref.getGhostMarker().getType() == GhostMarker.GHOSTS_PROBLEM_ID)
					return true;
			} catch (Exception e) {	e.printStackTrace();}
		return false;
	}
}
