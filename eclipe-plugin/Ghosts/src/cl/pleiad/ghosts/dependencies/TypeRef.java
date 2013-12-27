package cl.pleiad.ghosts.dependencies;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Class that contains the necessarily information
 * of a given Type, either local to the project
 * or external, for checking proposes
 *
 */
public class TypeRef {
	protected String name;
	protected boolean concrete;
	private Object ref;
	 
	public TypeRef setConcrete(boolean concrete) {
		this.concrete = concrete;
		return this;
	}

	/**
	 * It refers to the condition of proper class,
	 * both Abstract and Interface classes are not
	 * concrete
	 * @return true if the class is concrete, false otherwise
	 */
	public boolean isConcrete() {
		return concrete;
	}

	public String getName() {
		return name;
	}

	public TypeRef setName(String name) {
		this.name = name;
		return this;
	}

	public TypeRef(String name, boolean concrete){
		this.name = name;
		this.concrete = concrete;
	}
	
	public String toString(){
		return /*"["+(concrete?"":"?")+*/name/*+"]"*/;}
	
	public boolean equals(TypeRef ref) {
		if (ref.getName() == "Union" && this.getName() == "Union") {
			return ((UnionTypeRef) this).equals((UnionTypeRef) ref);
		}
		else if (this.getName() == "Union")
			return ((UnionTypeRef) this).equals(ref);
		else if (ref.getName() == "Union")
			return ((UnionTypeRef) ref).equals(this);
		else
			return compareType(name,ref.getName()) &&
					concrete == ref.isConcrete();
	}

	public Object getRef() {
		return ref;
	}

	public TypeRef setRef(Object ref) {
		this.ref = ref;
		return this;
	}
	
	public boolean isNonLocal() {
		if (ref != null)
			return isConcrete() && !((ITypeBinding) ref).isFromSource();
		else
			return isConcrete();// basic returns
	}
	
	public boolean compareType(String n1, String n2) {
		if (!n1.equals(n2)) {
			boolean result = n1.equals("double") || n1.equals("float") || n1.equals("int");
			result = result && (n2.equals("double") || n2.equals("float") || n2.equals("int"));
			return result;
		}
		else
			return true;
	}
}
