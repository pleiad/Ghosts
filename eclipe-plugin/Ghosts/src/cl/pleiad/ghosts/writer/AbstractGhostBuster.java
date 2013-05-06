package cl.pleiad.ghosts.writer;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import cl.pleiad.ghosts.core.Ghost;


public abstract class AbstractGhostBuster {
	
	protected Ghost ghost;
	protected TypeDeclaration ownerNode;
	protected AST ast;
	protected ASTRewrite writer;
	
	public void setGhost(Ghost _ghost){ this.ghost = _ghost;}
	
	public void write() throws Exception{
		this.createDeclaration();
		this.applyChanges();
	}
	protected abstract void applyChanges() throws Exception;

	protected abstract void createDeclaration()throws Exception;
	
	protected Modifier getPublicModifier() {
		return ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD);
	}

	protected SimpleName getGhostName() {
		return ast.newSimpleName(ghost.getName());
	}

}
