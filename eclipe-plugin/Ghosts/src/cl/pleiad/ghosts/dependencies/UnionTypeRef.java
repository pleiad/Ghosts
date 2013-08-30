package cl.pleiad.ghosts.dependencies;

import java.util.ArrayList;
import java.util.Collections;

public class UnionTypeRef extends TypeRef {
	public ArrayList<String> names;

	public UnionTypeRef(ArrayList<String> names, boolean concrete) {
		super("Union", concrete);
		this.names = names;
		Collections.sort(this.names);
	}
	
	public ArrayList<String> getNames() {
		return this.names;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Union{");
		result.append(this.names.get(0));
		for (int i = 1; i < this.names.size(); i++)
			result.append("," + this.names.get(i));
		result.append("}");
		return result.toString();
	}
	
	@Override
	public boolean equals(TypeRef ref) {
		if (ref.getName() == "Union") {
			int i = 0;
			for (String name : this.names)
				for (String name2 : ((UnionTypeRef) ref).names)
					if (name.equals(name2))
						i++;
			if (i < this.names.size() && i < ((UnionTypeRef) ref).names.size())
				return false;
			else
				return concrete == ref.isConcrete();
		}
		else
			for (String name3 : this.names)
				if (name3.equals(ref.getName()))
					return concrete == ref.isConcrete();
	
		return false;
	}

}
