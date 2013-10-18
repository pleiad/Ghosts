package cl.pleiad.ghosts.tests;

import junit.framework.TestCase;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.engine.SGhostEngine;

public class EngineTest extends TestCase {
	private TestProject project;
	private IWorkspace workspace;
	private TestProjectCompilationParticipant CP;
	
    @Before
    public void setUp(){
    	project = TestProject.getInstance();
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
			project.createJavaTypeAndPackage("stuff", "TestFactory.java", "");
			IType jclass = project.addFieldToJavaType("stuff", "TestFactory", "private Asdf a;");	
			String jmethod = "\tpublic void foo() {\n\t\tint b = a.getB();\n\t}\n";
			project.addMethodToJavaType("stuff", "TestFactory", jmethod);
			project.fullBuild();
			//System.out.println(jclass.getField("a").getSource());
			//System.out.println(jclass.getMethod("foo", null).getSource());
			System.out.println(jclass.getSource());
			System.out.println(project.getProblems());
			/*
			IJavaElement[] packages = project.getSourceFolder().getChildren();
			String name = ((IPackageFragment) packages[1]).getChildren()[0].getElementName();
			System.out.println(name);
			*/
		} catch (CoreException e) {
			e.printStackTrace();
		}
		assertFalse(SGhostEngine.get().getProjects().isEmpty());
		GhostSet projectGS = SGhostEngine.get().getProjects().get(0);
		String projectName = projectGS.toString();
		assertTrue(projectName.equals("TestProject"));
		assertFalse(projectGS.getGhosts().isEmpty());
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
