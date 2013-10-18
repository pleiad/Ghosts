package cl.pleiad.ghosts.refactoring;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;

import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.TypeInferencer;

public class ASTGBRewriteVisitor extends ASTVisitor {

	private ArrayList<SimpleType> results;
	private Ghost ghost;

	public ASTGBRewriteVisitor(Ghost g) {
		super();
		results = new ArrayList<SimpleType>();
		ghost = g;
	}

	public ArrayList<SimpleType> getResults() {
		return results;
	}
	
	public boolean visit(CompilationUnit unit) {
		return super.visit(unit);
	}
	
	public void endVisit(SimpleType typeNode) {
		super.endVisit(typeNode);
		if ((ghost.kind() == Ghost.CLASS || ghost.kind() == Ghost.INTERFACE)
			&& ghost.getName().equals(typeNode.getName().toString())) {
			ASTNode parent = typeNode.getParent();
			switch(parent.getNodeType()) {
				case ASTNode.FIELD_DECLARATION:
				case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					results.add(typeNode);//change the Type
					break;	
			}
		}
		
	}
	
	public void endVisit(ClassInstanceCreation newNode) {
		super.endVisit(newNode);
		if (ghost.kind() == Ghost.CLASS || ghost.kind() == Ghost.INTERFACE) {
			if (newNode.getType().isSimpleType() && 
				newNode.getType().toString().equals(ghost.getName()))
				results.add((SimpleType) newNode.getType());	
		}
	}
	
	public void endVisit(CastExpression castNode) {
		super.endVisit(castNode);
		if (ghost.kind() == Ghost.CLASS || ghost.kind() == Ghost.INTERFACE) {
			if (castNode.getType().isSimpleType() && 
					castNode.getType().toString().equals(ghost.getName()))
				results.add((SimpleType) castNode.getType());	
		}
	}
	
	public void endVisit(MethodDeclaration mthNode) {
		super.endVisit(mthNode);
		if (ghost.kind() == Ghost.CLASS || ghost.kind() == Ghost.INTERFACE) {
			if (mthNode.getReturnType2() != null &&
				mthNode.getReturnType2().isSimpleType() && 
				mthNode.getReturnType2().toString().equals(ghost.getName()))
				results.add((SimpleType) mthNode.getReturnType2());
			
		}
	}
	
}
