package cl.pleiad.ghosts.core;

import org.eclipse.jdt.core.dom.AST;

import cl.pleiad.ghosts.dependencies.TypeRef;

public class GConstructor extends GMethod {

	public GConstructor() {
		super("<no-name>", false);
	}
	
	@Override
	public int kind() {
		return CONSTRUCTOR;
	}
	
	public String toSimpleString(){return this.toStringWithOutReturn();}
	
	
	public void setOwnerType(TypeRef type) {
		super.setOwnerType(type);
		this.name = type.getName();
		super.returnType = new TypeRef("Object", true); //ugly!!!
	}
	
}
