package cl.pleiad.ghosts.tests;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

public class TestProjectCompilationParticipant extends CompilationParticipant {
	
	@Override
	public boolean isActive(IJavaProject project) {
		return project.getElementName() == "TestProject";
	}

}
