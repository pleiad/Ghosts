package cl.pleiad.ghosts.core;

import java.io.PrintStream;

import org.eclipse.jdt.core.dom.ASTNode;

import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.dependencies.TypeRef;
import cl.pleiad.ghosts.markers.GhostMarker;

public class GVariable extends Ghost {
	
	protected TypeRef returnType;
	protected ASTNode context;
	
	public GVariable (String name) {
		this.name = name;
	}
	
	public TypeRef getReturnType() {
		return returnType;
	}
	
	public GVariable setReturnType(TypeRef type) {
		this.returnType = type;
		return this;
	}
	
	public ASTNode getContext() {
		return context;
	}
	
	public GVariable setContext(ASTNode context) {
		this.context = context;
		return this;
	}
	

	@Override
	public String toString() {
		return name+" : "+this.returnType;
	}

	@Override
	protected void printItSelfOn(PrintStream writer) {
		writer.print(this.toString());
	}

	@Override
	public int kind() {
		return VARIABLE;
	}

	@Override
	public int similarTo(Ghost other) {
		if(other.kind() != kind()) return DIFF;
		
		if(!this.context.equals(((GVariable)other).context)) return DIFF;
		
		if(!other.getName().equals(this.getName())) return DIFF;
		
		if(!similarReturnTypesTo((GMember)other))	return NAME_KIND;
		return 0;
	}

	@Override
	public boolean hasProblems() {
		for (ISourceRef ref : this.getDependencies())
			try {
				if(ref != null && ref.getGhostMarker() != null && ref.getGhostMarker().exists())
						if(ref.getGhostMarker().getType() == GhostMarker.GHOSTS_PROBLEM_ID)
							return true;
			} catch (Exception e) {	e.printStackTrace();}
		return false;
	}
	
	public boolean similarReturnTypesTo(GMember other) {
		return returnType.equals(other.returnType);
	}
	
	@Override
	public boolean isVariable() {
		return true;
	}

}
