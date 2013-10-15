package cl.pleiad.ghosts.tests;

import static org.junit.Assert.*;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.junit.Test;

import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GConstructor;
import cl.pleiad.ghosts.core.GField;
import cl.pleiad.ghosts.core.GInterface;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.TypeRef;

public class CoreTest {
	GInterface a = new GInterface("Foo");
	GInterface b = new GInterface("Foo");
	GClass c = new GClass("Bar");
	GClass d = new GClass("Bar2");
	GField e = new GField("a",true, false);
	GField f = new GField("b",false, false);
	GMethod g = new GMethod("a",true, false);
	GMethod h = new GMethod("b",false, false);
	GConstructor i = new GConstructor();
	GConstructor j = new GConstructor();
	
	@Test
	public void initializationTest() {
		assertTrue(a.kind() == Ghost.INTERFACE);
		assertTrue(c.kind() == Ghost.CLASS);
		assertTrue(e.kind() == Ghost.FIELD);
		assertTrue(g.kind() == Ghost.METHOD);
		assertTrue(i.kind() == Ghost.CONSTRUCTOR);
		assertFalse(a.isMember());
		assertFalse(c.isMember());
		assertTrue(e.isMember());
		assertTrue(g.isMember());
		assertTrue(i.isMember());
		assertFalse(a.hasProblems());
		assertFalse(c.hasProblems());
		assertFalse(e.hasProblems());
		assertFalse(g.hasProblems());
		assertFalse(i.hasProblems());
	}
	
	@Test
	public void mutationTest() {
		a.asClass();
		assertTrue(a.kind() == Ghost.INTERFACE);
		c.asInterface();
		assertTrue(c.kind() == Ghost.CLASS);
	}
	
	@Test
	public void comparisonTest() {
		e.setOwnerType(new TypeRef("Foo", false));
		f.setOwnerType(new TypeRef("Foo", false));
		g.setOwnerType(new TypeRef("Foo", false));
		h.setOwnerType(new TypeRef("Foo", false));
		i.setOwnerType(new TypeRef("Foo", false));
		j.setOwnerType(new TypeRef("Bar", false));
		e.setReturnType(new TypeRef("Integer", true));
		f.setReturnType(new TypeRef("Integer", true));
		g.setReturnType(new TypeRef("Boolean", true));
		h.setReturnType(new TypeRef("Integer", true));
		i.setReturnType(new TypeRef("Foo", false));
		j.setReturnType(new TypeRef("Bar", false));
		assertTrue(a.similarTo(b) == Ghost.EQUALS);
		assertTrue(c.similarTo(d) == Ghost.DIFF);
		assertTrue(a.similarTo(c) == Ghost.DIFF);
		assertTrue(a.similarTo(d) == Ghost.DIFF);
		assertTrue(e.similarTo(f) == Ghost.DIFF);
		assertTrue(g.similarTo(h) == Ghost.DIFF);
		assertTrue(e.similarTo(h) == Ghost.DIFF);
		assertTrue(g.similarTo(f) == Ghost.DIFF);
		assertTrue(a.similarTo(e) == Ghost.DIFF);
		assertTrue(g.similarTo(c) == Ghost.DIFF);
		assertTrue(e.compareTo(f) == -1);
		assertTrue(e.compareTo(g) == 0);
		assertTrue(g.compareTo(h) == -1);
		assertTrue(g.compareTo(i) == 1);
		assertTrue(i.compareTo(j) == -1);
	}	

}
