package cl.pleiad.ghosts.core;

import java.util.Vector;

import cl.pleiad.ghosts.dependencies.TypeRef;


public class GMethod extends GMember {


	private Vector<TypeRef> paramTypes;
	
	public GMethod(String name, boolean staticMember) {
		super(name, staticMember);
		paramTypes = new Vector<TypeRef>(); //initial capacity?
	}	


	public Vector<TypeRef> getParamTypes() {
		return paramTypes;
	}	
	
	@Override
	public String toSimpleString() {
		return this.toStringWithOutReturn()+" : "+returnType;
	}

	protected String toStringWithOutReturn(){
		return name+"("+this.paramTypesAsLine()+")";
	}
	

	private String paramTypesAsLine() {
		String line="";
		for (TypeRef type : paramTypes) {
			line+=","+type;
		}
		return line.length() > 0?line.substring(1):line;
	}
	
	@Override
	public int kind() {
		return METHOD;
	}
	
	public boolean similarTypesTo(GMember other){
		boolean partial = this.similarParamTypesTo((GMethod)other);
		//if(kind()==CONSTRUCTOR) return partial;
		return partial && this.similarReturnTypesTo(other);
	}


	public boolean similarParamTypesTo(GMethod other) {
		if( this.getParamTypes().size() != other.getParamTypes().size()) return false;
		
		for (int i = 0; i < paramTypes.size(); i++)
			if(! paramTypes.get(i).equals(other.getParamTypes().get(i))) return false;
		
		return true;
	}


	@Override
	protected int additionalChecks(GMember other) {
		if(!similarParamTypesTo((GMethod)other)) return OVERLOADED;
		
		return EQUALS;
	}

}
