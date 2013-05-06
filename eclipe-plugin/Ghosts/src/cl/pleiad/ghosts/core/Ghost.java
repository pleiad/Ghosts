	package cl.pleiad.ghosts.core;

import java.io.PrintStream;
import java.util.Vector;

import cl.pleiad.ghosts.dependencies.ISourceRef;

/*
 * I could use it:
 * http://stackoverflow.com/questions/1069528/method-chaining-inheritance-dont-play-well-together-java
 * for method chaining
 */
public abstract class Ghost {
	
	public final static int FIELD = 101;
	public final static int METHOD = 102;
	public final static int CONSTRUCTOR = 103;
	
	public final static int CLASS = 201;
	public final static int INTERFACE = 202;
	
	public final static int DIFF = 10;
	public final static int NAME_KIND = 12;
	public final static int OVERLOADED = 15;
	public final static int SUBTYPE = 18; // << not yet available
	public final static int EQUALS = 20;
	
	
	protected String name;
	
	protected Vector<ISourceRef> dependencies;
	// I could use ArrayList, which seems to be a slight faster
	//, because it is unsynchronized
	
	protected Ghost() {
		this.dependencies = new Vector<ISourceRef>(); //initial capacity?		
	}

	public String getName() {
		return name;
	}

	public Vector<ISourceRef> getDependencies() {
		return dependencies;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public abstract String toString();
	
	public void printOn(PrintStream writer) {
		this.printItSelfOn(writer);
		for (ISourceRef ref : dependencies) {
			writer.print(" ->"+ref.toString());
		}
		writer.println();
	}

	protected abstract void printItSelfOn(PrintStream writer);

	public boolean isMember() {
		return false;
	}
	
	public abstract int kind();
	public abstract int similarTo(Ghost other);
	
	
	public abstract boolean hasProblems();
}
