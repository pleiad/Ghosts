package cl.pleiad.ghosts.engine;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
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
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GConstructor;
import cl.pleiad.ghosts.core.GExtendedClass;
import cl.pleiad.ghosts.core.GField;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.GVariable;
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
		inferencer = new TypeInferencer(new CopyOnWriteArrayList<Ghost>());
	}
	
	public ASTGhostVisitor setGhosts(CopyOnWriteArrayList<Ghost> initList) {
		inferencer = new TypeInferencer(initList);
		return this;
	}	
	
	/**
	 * Getter function for the Ghosts Vector
	 * @return the ghost Vector
	 */
	public CopyOnWriteArrayList<Ghost> getGhosts() {
		return inferencer.getGhosts();
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

	//simple name visitor
	public void endVisit(SimpleName nameNode) {
		super.endVisit(nameNode);
		if (nameNode.getParent().getNodeType() == ASTNode.ASSIGNMENT) {
			String name = "";
			if(nameNode.resolveTypeBinding() != null) {
				name = nameNode.resolveTypeBinding().getName();
				if (isNotGhost(name))		
					return;
			}
			//creating field to compare
			GField field = new GField(nameNode.getIdentifier(), false, false);
			field.setReturnType(inferencer.inferTypeOf(nameNode, 0));
			field.setOwnerType(new TypeRef(inferencer.getCurrentFileName(), true));
			field.getDependencies().add(inferencer.getSourceRef(nameNode, nameNode, field));
			//check ifExist
			inferencer.checkErrors(field);
			field = (GField) inferencer.checkAndUnify(field, Ghost.FIELD);
			inferencer.checkAndMutate(field);
		}
	}	
	
	// all types that I want, are here, but not sure if I need only this!!
	public void endVisit(SimpleType typeNode) {
		super.endVisit(typeNode);
		addNewGhost(typeNode);
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
		GBehaviorType excGhost = inferencer.getGhostType(typeName);
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
					excGClass.getDependencies().add(inferencer.getSourceRef(mthDNode, e, excGhost));
				}
			}
			else if (e.resolveBinding().isRecovered()){
				GClass excGClass = new GClass(e.resolveBinding().getName());
				excGClass.setSuperCls(new TypeRef("Exception", true).setRef(new Exception()));
				excGClass.getDependencies().add(inferencer.getSourceRef(mthDNode, e, excGClass));
				inferencer.getGhosts().add(excGClass);
			}
		}
			
	}
	
	//super field access visitor
	public void endVisit(SuperFieldAccess fieldNode) {
		super.endVisit(fieldNode);
		GExtendedClass superGhost = inferencer.getSuperGhost();
		if (superGhost != null) {
			GField field = new GField(fieldNode.getName().getIdentifier(), false, false);
			field.setReturnType(inferencer.inferTypeOf(fieldNode, 0));
			TypeRef superRef = new TypeRef(superGhost.getName(), false);
			superRef.setRef(inferencer.getGhostType(superGhost.getName()));
			field.setOwnerType(superRef);
			//not a ghost
			if(field.getOwnerType().isNonLocal()) 
				return;
			field.getDependencies().add(inferencer.getSourceRef(fieldNode, fieldNode.getName(),field));
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
			GMethod mth = new GMethod(mthNode.getName().getIdentifier(), false, false);
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
			mth.getDependencies().add(inferencer.getSourceRef(mthNode, mthNode.getName(),mth));
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
			//TODO check that this is never necessary
			gcons.getDependencies().add(inferencer.getSourceRef(ctrNode, null, gcons));
			gcons = (GConstructor)inferencer.checkAndUnify(gcons, Ghost.CONSTRUCTOR);
			inferencer.checkAndMutate(gcons);
		}
	}	
	
	//Method invocation visitor
	public void endVisit(MethodInvocation mthNode) {
		super.endVisit(mthNode);
		if(mthNode.resolveMethodBinding() != null) return;
		GMethod mth = new GMethod(mthNode.getName().getIdentifier(), false, false);
		if(mthNode.getExpression() == null) {
			mth.setOwnerType(new TypeRef(inferencer.getCurrentFileName(), true));
		}
		else {
			mth.setOwnerType(inferencer.inferTypeOf(mthNode.getExpression(), 0));
			//not a ghost
			if(mth.getOwnerType().isNonLocal()) 
				return;
		}
		mth.setReturnType(inferencer.inferTypeOf(mthNode, 0));
		for (Object _arg : mthNode.arguments()) {
			Expression arg = (Expression) _arg;
			mth.getParamTypes().add(inferencer.inferTypeOf(arg, 0));
		}
		mth.getDependencies().add(inferencer.getSourceRef(mthNode, mthNode.getName(), mth));
		inferencer.checkAndUnify(mth, Ghost.METHOD);
	}
		
	//class instance creation visitor
	public void endVisit(ClassInstanceCreation newNode) {
		super.endVisit(newNode);
		if(newNode.resolveConstructorBinding() != null) {
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
		gcons.getDependencies().add(inferencer.getSourceRef(newNode, newNode.getType(), gcons));
		gcons = (GConstructor)inferencer.checkAndUnify(gcons, Ghost.CONSTRUCTOR);
		inferencer.checkAndMutate(gcons);
	}
		
	//field access creation visitor (this.a , mth().a and maybe a.a)
	public void endVisit(FieldAccess fieldNode) {
		super.endVisit(fieldNode);
		if(fieldNode.resolveFieldBinding() != null) {
			String name = fieldNode.resolveFieldBinding().getType().getName();
			if (isNotGhost(name))		
				return;
		}
		//creating field to compare
		GField field = new GField(fieldNode.getName().getIdentifier(), false, false);
		field.setReturnType(inferencer.inferTypeOf(fieldNode, 0));
		field.setOwnerType(inferencer.inferTypeOf(fieldNode.getExpression(), 0));
		//not a ghost
		if(field.getOwnerType().isNonLocal()) 
			return;
		field.getDependencies().add(inferencer.getSourceRef(fieldNode, fieldNode.getName(), field));
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
		GField field = new GField(nameNode.getName().getIdentifier(), false, false);
		field.setReturnType(inferencer.inferTypeOf(nameNode, 0));
		field.setOwnerType(inferencer.inferTypeOf(nameNode.getQualifier(), 0)); 
		//not a ghost
		if(field.getOwnerType().isNonLocal()) 
			return;
		// unknown.unknown2 is not supported!
		field.getDependencies().add(inferencer.getSourceRef(nameNode, nameNode.getName(), field));
		//check ifExist
		field = (GField) inferencer.checkAndUnify(field, Ghost.FIELD);
		inferencer.checkAndMutate(field);
	}
	
	/**
	 * Helper function that checks if a certain binding is
	 * a Ghost and if it is creates the corresponding Ghost
	 */
	@SuppressWarnings("unchecked")
	private void addNewGhost(SimpleType typeNode) {
		ITypeBinding type = typeNode.resolveBinding();
		if(type.isRecovered()) {
			if (!(type.isClass() || type.isInterface())) 
				return;										
			if(blackList.contains(type.getName())) 
				return; // or QualifiedName
			Ghost ghost = inferencer.getGhostConsidering(typeNode);
			ghost.getDependencies().add(inferencer.getSourceRef(typeNode, typeNode, ghost));
			ASTNode parent = typeNode.getParent();
			//extends ghost
			if (parent.getNodeType() == ASTNode.TYPE_DECLARATION) {
				GExtendedClass superGClass = (GExtendedClass) inferencer.mutate2((GBehaviorType)ghost);
				superGClass.addExtender(inferencer.getCurrentFileName());
			}
			//variable declaration
			else if (parent.getNodeType() == ASTNode.FIELD_DECLARATION ||
					parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION ||
					parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				List<VariableDeclarationFragment> variables = null;
				switch (parent.getNodeType()) {
					case ASTNode.FIELD_DECLARATION:
						variables = ((FieldDeclaration) parent).fragments();
						break;
					case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
						variables = ((VariableDeclarationExpression) parent).fragments();
						break;
					case ASTNode.VARIABLE_DECLARATION_STATEMENT:
						variables = ((VariableDeclarationStatement) parent).fragments();
						break;						
				}
				for (VariableDeclarationFragment var : variables) {
					String name = var.getName().getIdentifier();
					TypeRef typeRef = new TypeRef(ghost.getName(),false).setRef(ghost);
					if (parent.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
						GField field = new GField(name, false, true);
						field.setReturnType(typeRef);
						if (var.getInitializer() != null)
							field.setOwnerType(inferencer.inferTypeOf(var.getInitializer(), 0));
						else {
							TypeRef owner = new TypeRef(inferencer.getCurrentFileName(),true);
							owner.setRef(var.resolveBinding().getType());
							field.setOwnerType(owner);
						}
						field.getDependencies().add(inferencer.getSourceRef(typeNode, var.getName(), field));
						field = (GField) inferencer.checkAndUnify(field, Ghost.FIELD);
						inferencer.checkAndMutate(field);
					}
					else {
						GVariable gVar = new GVariable(name).setReturnType(typeRef);
						gVar.setContext(parent.getParent());
						gVar.getDependencies().add(inferencer.getSourceRef(typeNode, null, gVar));
						inferencer.getGhosts().add(gVar);
					}

				}
			}

		}
	}
	
	/**
	 * Helper function that finds if a certain
	 * name corresponds to a Ghost.
	 */	
	private boolean isNotGhost(String name) {
		int count = 0;
		for (Ghost ghost: inferencer.getGhosts())
			if (ghost.getName().equals(name))
				count++;
		return count == 0;
	}
}
