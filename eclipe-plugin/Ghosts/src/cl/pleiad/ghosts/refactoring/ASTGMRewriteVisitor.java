package cl.pleiad.ghosts.refactoring;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.dependencies.TypeInferencer;
import cl.pleiad.ghosts.dependencies.TypeRef;

public class ASTGMRewriteVisitor extends ASTVisitor {
	
	private ArrayList<SimpleName> results;
	private TypeInferencer inferencer;
	private Ghost ghost;
	
	public ASTGMRewriteVisitor(Ghost g, GhostSet context, CompilationUnit cu) {
		super();
		results = new ArrayList<SimpleName>();
		ghost = g;
		inferencer = new TypeInferencer(context.getGhosts());
		inferencer.setCompilationUnit(cu);
	}

	public ArrayList<SimpleName> getResults() {
		return results;
	}
	
	public boolean visit(CompilationUnit unit) {
		return super.visit(unit);
	}
	
	public void endVisit(SimpleName nameNode) {
		super.endVisit(nameNode);
		if ((ghost.kind() == Ghost.FIELD || ghost.kind() == Ghost.METHOD)
			&& ghost.getName().equals(nameNode.getIdentifier())) {
			GMember member = (GMember) ghost;
			TypeRef ownerType = null;
			TypeRef returnType = inferencer.inferTypeOf(nameNode, 0);
			switch (nameNode.getParent().getNodeType()) {
				case ASTNode.QUALIFIED_NAME:
				case ASTNode.METHOD_INVOCATION:
				case ASTNode.ASSIGNMENT:
					//TODO check when renaming Ghost that is not in the current file
					ownerType = new TypeRef(inferencer.getCurrentFileName(), true);
					break;
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					if (nameNode.getParent().getParent().getNodeType() 
						== ASTNode.FIELD_DECLARATION)
						ownerType = new TypeRef(inferencer.getCurrentFileName(), true);
					break;
			}
			if (ownerType != null &&
 				member.getOwnerType().equals(ownerType) &&
				member.getReturnType().equals(returnType))
				results.add(nameNode);
		}
	}

}
