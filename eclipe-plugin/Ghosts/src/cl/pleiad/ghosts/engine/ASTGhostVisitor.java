package cl.pleiad.ghosts.engine;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GConstructor;
import cl.pleiad.ghosts.core.GExtendedClass;
import cl.pleiad.ghosts.core.GField;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.TypeInferencer;
import cl.pleiad.ghosts.dependencies.TypeRef;

/**
 * Visitor Class for the Ghost Engine
 *
 */
public class ASTGhostVisitor extends ASTVisitor {
	
	private GBlackList blackList;
	private TypeInferencer inferencer;

	public ASTGhostVisitor() {
		super();
		inferencer = new TypeInferencer(new Vector<Ghost>(), new Vector<GExtendedClass>());
	}
	
	public ASTGhostVisitor setGhosts(Vector<Ghost> initList, Vector<GExtendedClass> vector) {
		inferencer = new TypeInferencer(initList, vector);
		return this;
	}	
	
	/**
	 * Getter function for the Ghosts Vector
	 * @return the ghost Vector
	 */
	public Vector<Ghost> getGhosts() {
		return inferencer.getGhosts();
	}
	
	/**
	 * Getter function for the GExtendedClass Vector
	 * @return the ghost Vector
	 */
	public Vector<GExtendedClass> getEGhosts() {
		return inferencer.getEGhosts();
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
		inferencer.setCompilationUnit(unit); //may be is not need with getRoot!!
		return super.visit(unit);
	}	
	
	// all types that I want, are here, but not sure if I need only this!!
	public void endVisit(SimpleType typeNode) {
		super.endVisit(typeNode);
		addNewGhost(typeNode);
	}
	
	//type declaration visitor
	public void endVisit(TypeDeclaration typeNode) {
		//for the purpose of finding "extends" statements
		super.endVisit(typeNode);
		if (typeNode.getSuperclassType() != null) {
			String superName = typeNode.getSuperclassType().toString();
			GBehaviorType superGhost = inferencer.getGhostType(superName);
			if (superGhost != null) {
				GExtendedClass superGClass = (GExtendedClass) inferencer.mutate2(superGhost);
				superGClass.addExtender(inferencer.getCurrentFileName());
			}
			else {
				GExtendedClass superGClass = new GExtendedClass(superName);
				superGClass.getDependencies().add(inferencer.getSourceRef(typeNode,superGClass));
				inferencer.getGhosts().add(superGClass);
				inferencer.getEGhosts().add(superGClass);
			}
		}
	}
	
	//Catch clause visitor
	public void endVisit(CatchClause catchNode) {
		super.endVisit(catchNode);
		SingleVariableDeclaration excep = catchNode.getException();
		if(excep.resolveBinding() != null) {
			String excName = excep.getType().toString();
			GBehaviorType excGhost = inferencer.getGhostType(excName);
			if (excGhost != null) {
				GClass excGClass = (GClass) inferencer.mutate(excGhost);
				if (excGClass.getSuperCls() == null ||
						excGClass.getSuperCls().getName() != "Exception") {
						excGClass.setSuperCls(new TypeRef("Exception", true).setRef(new Exception()));
					}
					else
						return;
			}		
		}
	}
	
	//throw visitor
	public void endVisit(ThrowStatement throwNode) {
		super.endVisit(throwNode);
		Expression expr = throwNode.getExpression();
		String typeName = expr.resolveTypeBinding().getName();
		int nodeType = expr.getNodeType();
		GBehaviorType excGhost = inferencer.getGhostType(typeName);
		if (excGhost != null) {
			GClass excGClass = (GClass) inferencer.mutate(excGhost);
			if (excGClass.getSuperCls() == null ||
				excGClass.getSuperCls().getName() != "Exception") {
				excGClass.setSuperCls(new TypeRef("Exception", true).setRef(new Exception()));
				if (nodeType == ASTNode.SIMPLE_NAME) //when is predefined in the file
					excGClass.getDependencies().add(inferencer.getSourceRef(throwNode,excGhost));
			}
			else
				return;
		}
	}	
	
	//method declaration visitor
	@SuppressWarnings("unchecked")
	public void endVisit(MethodDeclaration mthDNode) {
		//for the purpose of finding throws statements
		super.endVisit(mthDNode);
		List<SimpleName> exceptions = mthDNode.thrownExceptions();
		for (Iterator<SimpleName> excs = exceptions.iterator(); excs.hasNext();) {
			SimpleName e = excs.next();
			GBehaviorType excGhost = inferencer.getGhostType(e.resolveBinding().getName());
			if (excGhost != null) {
				GClass excGClass = (GClass) inferencer.mutate(excGhost);
				if (excGClass.getSuperCls() == null ||
					excGClass.getSuperCls().getName() != "Exception") {
					excGClass.setSuperCls(new TypeRef("Exception", true).setRef(new Exception()));
					excGClass.getDependencies().add(inferencer.getSourceRef(mthDNode,excGhost));
				}
			}
			else if (e.resolveBinding().isRecovered()){
				GClass excGClass = new GClass(e.resolveBinding().getName());
				excGClass.setSuperCls(new TypeRef("Exception", true).setRef(new Exception()));
				excGClass.getDependencies().add(inferencer.getSourceRef(mthDNode,excGClass));
				inferencer.getGhosts().add(excGClass);
			}
		}
			
	}
	
	//super field access visitor
	public void endVisit(SuperFieldAccess fieldNode) {
		super.endVisit(fieldNode);
		GExtendedClass superGhost = inferencer.getSuperGhost();
		//TODO check this later
		if (superGhost != null) {
			GField field = new GField(fieldNode.getName().getIdentifier(), false);
			field.setReturnType(inferencer.inferTypeOf(fieldNode, 0));
			TypeRef superRef = new TypeRef(superGhost.getName(), false);
			superRef.setRef(inferencer.getGhostType(superGhost.getName()));
			field.setOwnerType(superRef);
			//not a ghost
			if(field.getOwnerType().isNonLocal()) 
				return;
			field.getDependencies().add(inferencer.getSourceRef(fieldNode,field));
			//check ifExist
			field=(GField) inferencer.checkAndUnify(field, Ghost.FIELD);
			inferencer.checkAndMutate(field);
		}
	}
	
	//super method invocation visitor
	public void endVisit(SuperMethodInvocation mthNode) {
		super.endVisit(mthNode);
		GExtendedClass superGhost = inferencer.getSuperGhost();
		if (superGhost != null) {
			//TODO need a check!!!
			GMethod mth = new GMethod(mthNode.getName().getIdentifier(), false);
			TypeRef superRef = new TypeRef(superGhost.getName(), false);
			superRef.setRef(inferencer.getGhostType(superGhost.getName()));
			mth.setOwnerType(superRef);
			//not a ghost
			if(mth.getOwnerType().isNonLocal()) 
				return;
			mth.setReturnType(inferencer.inferTypeOf(mthNode, 0));
			for (Object _arg : mthNode.arguments()) {
				Expression arg = (Expression) _arg;
				mth.getParamTypes().add(inferencer.inferTypeOf(arg, 0));
			}
			mth.getDependencies().add(inferencer.getSourceRef(mthNode,mth));
			inferencer.checkAndUnify(mth, Ghost.METHOD);
		}
	}
	
	//super constructor invocation visitor
	public void endVisit(SuperConstructorInvocation ctrNode) {
		super.endVisit(ctrNode);
		GExtendedClass superGhost = inferencer.getSuperGhost();
		if (superGhost != null) {
			GConstructor gcons = new GConstructor();
			for (Object _arg : ctrNode.arguments()) {
				Expression arg = (Expression) _arg;
				gcons.getParamTypes().add(inferencer.inferTypeOf(arg, 0));
			}
			TypeRef superRef = new TypeRef(superGhost.getName(), false);
			superRef.setRef(inferencer.getGhostType(superGhost.getName()));
			gcons.setOwnerType(superRef);
			//not a ghost
			if(gcons.getOwnerType().isNonLocal()) 
				return;
			gcons.getDependencies().add(inferencer.getSourceRef(ctrNode, gcons));
			gcons = (GConstructor)inferencer.checkAndUnify(gcons, Ghost.CONSTRUCTOR);
			inferencer.checkAndMutate(gcons);
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
		mth.setOwnerType(inferencer.inferTypeOf(mthNode.getExpression(), 0));
		//not a ghost
		if(mth.getOwnerType().isNonLocal()) 
			return;
		mth.setReturnType(inferencer.inferTypeOf(mthNode, 0));
		for (Object _arg : mthNode.arguments()) {
			Expression arg = (Expression) _arg;
			mth.getParamTypes().add(inferencer.inferTypeOf(arg, 0));
		}
		mth.getDependencies().add(inferencer.getSourceRef(mthNode,mth));
		inferencer.checkAndUnify(mth, Ghost.METHOD);
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
			gcons.getParamTypes().add(inferencer.inferTypeOf(arg, 0));
		}
		TypeRef owner = inferencer.inferTypeOf(newNode, 0);
		gcons.setOwnerType(owner);
		//not a ghost
		if(owner.isNonLocal()) 
			return;
		gcons.getDependencies().add(inferencer.getSourceRef(newNode, gcons));
		gcons = (GConstructor)inferencer.checkAndUnify(gcons, Ghost.CONSTRUCTOR);
		inferencer.checkAndMutate(gcons);
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
		field.setReturnType(inferencer.inferTypeOf(fieldNode, 0));
		field.setOwnerType(inferencer.inferTypeOf(fieldNode.getExpression(), 0));
		//not a ghost
		if(field.getOwnerType().isNonLocal()) 
			return;
		field.getDependencies().add(inferencer.getSourceRef(fieldNode,field));
		//check ifExist
		field=(GField) inferencer.checkAndUnify(field, Ghost.FIELD);
		inferencer.checkAndMutate(field);
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
		field.setReturnType(inferencer.inferTypeOf(nameNode, 0));
		field.setOwnerType(inferencer.inferTypeOf(nameNode.getQualifier(), 0)); 
		//not a ghost
		if(field.getOwnerType().isNonLocal()) 
			return;
		// unknown.unknown2 is not supported!
		field.getDependencies().add(inferencer.getSourceRef(nameNode, field));
		//check ifExist
		field = (GField) inferencer.checkAndUnify(field, Ghost.FIELD);
		inferencer.checkAndMutate(field);
	}
	
	/**
	 * Helper function that checks if a certain binding is
	 * a Ghost and if it is creates the corresponding Ghost
	 */
	private void addNewGhost(SimpleType typeNode) {
		ITypeBinding type = typeNode.resolveBinding();
		if(type.isRecovered()) {
			if (!(type.isClass() || type.isInterface())) 
				return; //TODO enhance										
			if(blackList.contains(type.getName())) 
				return; // or QualifiedName
			Ghost ghost = inferencer.getGhostConsidering(typeNode);//here
			ghost.getDependencies().add(inferencer.getSourceRef(typeNode,ghost));

		}
	}
}
