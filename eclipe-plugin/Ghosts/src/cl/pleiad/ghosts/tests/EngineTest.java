package cl.pleiad.ghosts.tests;

import junit.framework.TestCase;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cl.pleiad.ghosts.engine.SGhostEngine;

public class EngineTest extends TestCase {
	private GhostTestHelper project;
	private IWorkspace workspace;
	
    @Before
    public void setUp(){
    	project = GhostTestHelper.getInstance();
    	workspace = ResourcesPlugin.getWorkspace();
    }
	
	@Test
	public void testCreation() {
		int projectCount = workspace.getRoot().getProjects().length;
		int ghostsProjectCount = SGhostEngine.get().getProjects().size();
		assertTrue(projectCount == 1);
		assertTrue(projectCount == ghostsProjectCount);
	}
	
	@Test
	public void testDeletion() {
		int projectCount = workspace.getRoot().getProjects().length;
		assertTrue(projectCount == 1);
		if (project == null)
			System.out.println("derp");
		try {
			project.dispose();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		projectCount = workspace.getRoot().getProjects().length;
		int ghostsProjectCount = SGhostEngine.get().getProjects().size();
		assertTrue(projectCount == 0);
		assertTrue(projectCount == ghostsProjectCount);
	}
	
	/**
	 * Testing Ghost recognition of a non-created class
	 * (Product) while defining a class (Factory)
	 */
	@Test
	public void testBasicAssociation() {
		try {
			StringBuffer source = new StringBuffer();
			source.append("class Factory {" + System.getProperty("line.separator"));
			source.append("private Product a, b, c;" + System.getProperty("line.separator"));
			source.append("}");
			project.createJavaTypeAndPackage("stuff", "Factory.java", source.toString());
			/*
			IJavaElement[] packages = project.getSourceFolder().getChildren();
			String name = ((IPackageFragment) packages[1]).getChildren()[0].getElementName();
			System.out.println(name);
			*/
		} catch (CoreException e) {
			e.printStackTrace();
		}
		assertFalse(SGhostEngine.get().getProjects().isEmpty());
		String projectName = SGhostEngine.get().getProjects().firstElement().toString();
		assertTrue(projectName.equals("TestProject"));
		assertFalse(SGhostEngine.get().getProjects().firstElement().getGhosts().isEmpty());
		//assertTrue(name.equals("Product"));
	}
	
	@After
    public void tearDown() {
		try {
			project.dispose();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		project = null;
		workspace = null;
		System.gc();
    }
}
