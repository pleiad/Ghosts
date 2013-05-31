package cl.pleiad.ghosts.engine;

import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GConstructor;
import cl.pleiad.ghosts.core.GField;
import cl.pleiad.ghosts.core.GInterface;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.dependencies.MarkerSourceRef;
import cl.pleiad.ghosts.dependencies.TypeInferer;
import cl.pleiad.ghosts.dependencies.TypeRef;
import cl.pleiad.ghosts.markers.GhostMarker;

/**
 * Visitor Class for the Ghost Engine
 *
 */
public class ASTGhostVisitor extends ASTVisitor {
	
	private GBlackList blackList;
	private TypeInferer inferer;

	public ASTGhostVisitor() {
		super();
		inferer = new TypeInferer(new Vector<Ghost>());
	}
	
	public ASTGhostVisitor setGhosts(Vector<Ghost> initList) {
		inferer = new TypeInferer(initList);
		return this;
	}	
	
	/**
	 * Getter function for the Ghosts Vector
	 * @return the ghost Vector
	 */
	public Vector<Ghost> getGhosts() {
		return inferer.getGhosts();
	}
	
	/**
	 * Setter function for the blacklist.
	 * @param blackList
	 * @return the visitor, updated
	 */
	public ASTGhostVisitor setBlackList(GBlackList blackList) {
		this.blackList = blackList;
		return this;
	}

	public boolean visit(CompilationUnit unit) {
		inferer.setCompilationUnit(unit); //may be is not need with getRoot!!
		return super.visit(unit);
	}	
	
	/* TODO support it!
	public void endVisit(SuperFieldAccess fieldNode){
		super.endVisit(fieldNode);
		System.out.println("Sfield: "+fieldNode.getName()
				+"\nresolvT: "+(fieldNode.resolveTypeBinding()!=null)
				+"\nresolvF: "+(fieldNode.resolveFieldBinding()!=null));
	}
	
	public void endVisit(SuperMethodInvocation mthNode){
		super.endVisit(mthNode);
		System.out.println("MthINVK: "+mthNode.getName()
				+"\nresolvM: "+(mthNode.resolveMethodBinding() != null)
				+"\nresolvT: "+(mthNode.resolveTypeBinding() != null));
		// Do I need to check the args? I dont think so, let see later.
	}

	*/
	
	// all types that I want, are here, but not sure if I need only this!!
	public void endVisit(SimpleType typeNode) {
		super.endVisit(typeNode);
		ITypeBinding type = typeNode.resolveBinding();
		if(type.isRecovered()) {
			if (!(type.isClass() || type.isInterface())) 
				return; //TODO enhance										
			if(blackList.contains(type.getName())) 
				return; // or QualifiedName
			Ghost ghost = inferer.getGhostConsidering(typeNode);//here
			ghost.getDependencies().add(inferer.getSourceRef(typeNode,ghost));

		}
	}	
	
	//Method invocation visitor
		public void endVisit(MethodInvocation mthNode) {
			super.endVisit(mthNode);
			if(mthNode.resolveMethodBinding() != null) return;
			//TODO need a check!!!
			GMethod mth = new GMethod(mthNode.getName().getIdentifier(), false);
			if(mthNode.getExpression() == null) {
				//TODO not yet supported foo() without expression!!
				System.err.println(mthNode+" wo left-expr Not supported yet!");
				return;
			}
			mth.setOwnerType(inferer.inferTypeOf(mthNode.getExpression()));//here
			//not a ghost
			if(mth.getOwnerType().isNonLocal()) 
				return;
			mth.setReturnType(inferer.inferTypeOf(mthNode));//here
			for (Object _arg : mthNode.arguments()) {
				Expression arg = (Expression) _arg;
				mth.getParamTypes().add(inferer.inferTypeOf(arg));//here
			}
			mth.getDependencies().add(inferer.getSourceRef(mthNode,mth));//here
			inferer.checkAndUnify(mth, Ghost.METHOD);
		}
		
		//class instance creation visitor
		public void endVisit(ClassInstanceCreation newNode) {
			super.endVisit(newNode);
			if(newNode.resolveConstructorBinding() != null) {
				//TODO do it better!!! check types [may I don't need to check]
				if(newNode.resolveConstructorBinding().getParameterTypes().length
				== newNode.arguments().size()) 
					return;
			}
			GConstructor gcons = new GConstructor();
			for (Object _arg : newNode.arguments()) {
				Expression arg = (Expression) _arg;
				gcons.getParamTypes().add(inferer.inferTypeOf(arg));//here
			}
			TypeRef owner = inferer.inferTypeOf(newNode);//here
			gcons.setOwnerType(owner);
			//not a ghost
			if(owner.isNonLocal()) 
				return;
			gcons.getDependencies().add(inferer.getSourceRef(newNode, gcons));//here
			gcons = (GConstructor)inferer.checkAndUnify(gcons, Ghost.CONSTRUCTOR);//here
			inferer.checkAndMutate(gcons);
		}
		
		//field access creation visitor (this.a , mth().a and maybe a.a)
		public void endVisit(FieldAccess fieldNode) {
			super.endVisit(fieldNode);
			if(fieldNode.resolveFieldBinding() != null)/* &&
				!fieldNode.resolveFieldBinding().getType().isRecovered())*/
					return;
			//creating field to compare
			//TODO check this later
			GField field = new GField(fieldNode.getName().getIdentifier(), false);
			field.setReturnType(inferer.inferTypeOf(fieldNode));//here
			field.setOwnerType(inferer.inferTypeOf(fieldNode.getExpression()));//here
			//not a ghost
			if(field.getOwnerType().isNonLocal()) 
				return;
			field.getDependencies().add(inferer.getSourceRef(fieldNode,field));//here
			//check ifExist
			field=(GField) inferer.checkAndUnify(field, Ghost.FIELD);//here
			inferer.checkAndMutate(field);//here
		}

		//qualified name visitor (only interesting in field access foo.bar.baz)
		public void endVisit(QualifiedName nameNode) {
			super.endVisit(nameNode);
			//java.lang.String a = lang.foo + java.lang;
			if(nameNode.resolveBinding() != null) 
				return;
			//if nameNode.resolveBinding() == TypeBinding we are in a static member!
			//creating field to compare
			//TODO not supported yet!!
			GField field = new GField(nameNode.getName().getIdentifier(), false);
			field.setReturnType(inferer.inferTypeOf(nameNode));//here
			field.setOwnerType(inferer.inferTypeOf(nameNode.getQualifier()));//here 
			//not a ghost
			if(field.getOwnerType().isNonLocal()) 
				return;
			// unknown.unknown2 is not supported!
			field.getDependencies().add(inferer.getSourceRef(nameNode, field));//here
			//check ifExist
			field = (GField) inferer.checkAndUnify(field, Ghost.FIELD);//here
			inferer.checkAndMutate(field);
		}
	
}
