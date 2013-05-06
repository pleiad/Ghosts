package cl.pleiad.ghosts.core;

public class GField extends GMember {
	
	public GField(String name, boolean staticMember) {
		super(name, staticMember);
	}

	@Override
	public String toSimpleString() {
		return name+" : "+this.returnType;
	}

	@Override
	public int kind() {
		return FIELD;
	}

	@Override
	protected boolean similarTypesTo(GMember other) {
		return similarReturnTypesTo(other);
	}

	@Override
	protected int additionalChecks(GMember other) {
		return EQUALS; //no more checks is needed!
	}
	
}
