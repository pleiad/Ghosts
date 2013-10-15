package cl.pleiad.ghosts.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GExtendedClass;
import cl.pleiad.ghosts.core.GField;
import cl.pleiad.ghosts.core.GInterface;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.GVariable;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.markers.GhostMarker;


public class TypeInferencer {
	private CompilationUnit cUnit;
	private CopyOnWriteArrayList<Ghost> ghosts;
	private Object lock;
	private int counter;
	
	public TypeInferencer(CopyOnWriteArrayList<Ghost> ghosts) {
		this.ghosts = ghosts;
		this.lock = new Object();
	}
	
	public CopyOnWriteArrayList<Ghost> getGhosts() {
		return this.ghosts;
	}
	
	public void setCompilationUnit(CompilationUnit cUnit) {
		this.cUnit = cUnit;
	}
	
	public String getCurrentFileName() {
		String name = this.cUnit.getJavaElement().getElementName();
		return name.substring(0, name.lastIndexOf('.'));
	}

	/**
	 * Helper function that obtains, or creates
	 * the Ghost of a certain SimpleType.
	 * @param typeNode the SympleType
	 * @return the corresponding Ghost 
	 */
	public GBehaviorType getGhostConsidering(SimpleType typeNode) {
		ITypeBinding type = typeNode.resolveBinding();
		String qName = type.getQualifiedName();
		GBehaviorType ghost =  this.getGhostType(qName);
		if (ghost != null) 
			return ghost;
		
		int kind = GhostMarker.getStoredKind(cUnit.getJavaElement().getJavaProject(), qName);
		
		if (kind > 0) {
			ghost = new GInterface(qName);
			if(kind == Ghost.CLASS) 
				ghost = new GClass(qName);
			ghost.setMutable(true); //at beginning it is mutable
		} else {
			if (!this.isMutableClass(typeNode)) {
				ghost = new GClass(qName);
				ghost.setMutable(false);
			} else {
				ghost = new GInterface(qName);
				ghost.setMutable(this.isMutableInterface(typeNode));
			}
		}
		ghosts.add(ghost);
		return ghost;
	}	
	
	/**
	 * Function that checks if a new member has already
	 * been defined as a ghost member, or is already
	 * defined as part of a ghost class. If the member
	 * has already been defined, unifies its properties,
	 * otherwise, adds the new ghost member in the project 
	 * @param member the new GMember to be check
	 * @param kind the creation kind of the Ghost
	 * @return the resulting GMember
	 */
	public GMember checkAndUnify(GMember member, int kind) {
		for(Ghost ghost : ghosts) {				
			if(ghost.isMember()) {
				GMember aux = this.checkAndUnifyHelper((GMember)ghost, member, kind);
				if(aux!=null) {
					return aux;
				}
			}
			else if (!ghost.isVariable())
				for(GMember gmember : ((GBehaviorType)ghost).getMembers()) {
					GMember aux=this.checkAndUnifyHelper(gmember, member, kind);
					if(aux!=null){
						return aux;
					}
				}					
		}//if there are not similars!
		
		if(member.getOwnerType().isConcrete()) ghosts.add(member);
		else this.getGhostType(member.getOwnerType().getName())
					.getMembers().add(member);			
			
		return member;
	}
	
	/**
	 * Helper function that checks if two GMembers are either
	 * equal, similar or not equal at all. It rises any problem
	 * found by comparing similar GMembers.
	 * @param ghost the GMember for comparison
	 * @param member the GMember to compare to
	 * @param kind useless?
	 * @return the resulting ghost or null
	 */
	private GMember checkAndUnifyHelper(GMember ghost, GMember member, int kind) {
		int similarity = ghost.similarTo(member);
		if(similarity == Ghost.EQUALS) {
			return (ghost).absorb(member);
		}
		if( Ghost.NAME_KIND <= similarity && similarity < Ghost.OVERLOADED) {
			if(ghost.getReturnType().getName().equals("void"))
				if( ((GMethod)ghost).similarParamTypesTo((GMethod)member) ) {
					ghost.setReturnType(member.getReturnType());
					return ghost.absorb(member);
				}
			
			if(member.getReturnType().getName().equals("void"))
				if( ((GMethod)ghost).similarParamTypesTo((GMethod)member) )
					return ghost.absorb(member);
			
			//System.out.println(ghost+" |similar| "+member);
			GhostMarker.createIncompatibleDefMarkerFrom(ghost);
			GhostMarker.createIncompatibleDefMarkerFrom(member);
		
		}
		
		return null;
	}
	
	/**
	 * Helper Function, to mutate an interface class
	 * to a class.
	 * @param member the GMember of the class
	 */
	public void checkAndMutate(GMember member) {
		TypeRef owner = member.getOwnerType();
		if(!owner.isConcrete()) {
			GBehaviorType ghost = (GBehaviorType) owner.getRef();
			if(ghost.kind() == Ghost.INTERFACE) {
				if(ghost.isMutable()) {
					GBehaviorType newGhost = mutate(ghost);
					owner.setRef(newGhost);
				} else {
					GhostMarker.createIncompatibleDefMarkerFrom(member); 
				}	
			}				
		} 	
	}

	public void checkErrors(GField field) {
		if (field.getReturnType().getName().equals("Undefined")) 
			GhostMarker.createUndefinedTypeMarkerFrom(field);
		else if (field.getReturnType().getName().equals("Wrong Typing")) 
			GhostMarker.createWrongTypeMarkerFrom(field);
	}
	
	public GBehaviorType mutate(GBehaviorType ghost) {
		GBehaviorType newGhost = ghost.asClass();
		ghosts.remove(ghost);
		ghosts.add(newGhost);
		return newGhost;
	}
	
	public GBehaviorType mutate2(GBehaviorType ghost) {
		GExtendedClass newGhost = ghost.asClass().asExtendedClass();
		ghosts.remove(ghost);
		ghosts.add(newGhost);
		return newGhost;
	}
	
	public GExtendedClass getSuperGhost() {
		String name = this.getCurrentFileName();
		for (Ghost ghost : ghosts) {
			if (ghost.kind() == Ghost.CLASS) {
				GExtendedClass gClass = ((GBehaviorType) ghost).asClass().asExtendedClass();
				if (gClass.getExtenders() != null) {
					for (String gname : gClass.getExtenders()) {
						if(name.equals(gname)) {
							return gClass;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Inference function, to obtain the more accurate
	 * type for a certain expression.
	 * @param node the expression to be resolved
	 * @return the best inferred type
	 */
	public TypeRef inferTypeOf(Expression node, int deep) {
		//basic JDT inference
		TypeRef typeRef = getExistingType(node, deep);
		if(typeRef != null) 
			return typeRef;
		switch (node.getNodeType()) {
		// N;
		case ASTNode.SIMPLE_NAME: return inferTypeOfVar((SimpleName) node);
		// method_invk(arg0, ..,<node>);
		case ASTNode.METHOD_INVOCATION: 
		// N.x	
		case ASTNode.FIELD_ACCESS:
		// N.x;
		case ASTNode.SUPER_METHOD_INVOCATION:
		// super.x;
		case ASTNode.SUPER_FIELD_ACCESS:
		// super.x;
		case ASTNode.QUALIFIED_NAME: return inferContextTypeOf(node, deep);
		}
		// everything failed, return Object
		return this.getRootType();
	}

	/**
	 * Inference function, to obtain the more accurate
	 * type for a certain GMember in a expression, 
	 * considering existing declarations.
	 * @param node the expression to be resolved
	 * @param name the name of the looked member
	 * @return the best inferred type
	 */
	public TypeRef inferCurrentTypeOf(Expression node, String name) {
		TypeRef result = null;
		Expression ctxNode = null;
		switch (node.getNodeType()) {
			case ASTNode.FIELD_ACCESS:
				ctxNode = ((FieldAccess) node).getExpression();
				break;
			case ASTNode.METHOD_INVOCATION:
				ctxNode = ((MethodInvocation) node).getExpression();
				break;
			case ASTNode.QUALIFIED_NAME:
				ctxNode = ((QualifiedName) node).getQualifier();
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				ctxNode = ((SuperFieldAccess) node);
				break;
			case ASTNode.SUPER_METHOD_INVOCATION:
				ctxNode = ((SuperMethodInvocation) node);
				break;
		}
		if (ctxNode != null)
			switch (ctxNode.getNodeType()) {
				case ASTNode.THIS_EXPRESSION:
					result = this.getMemberType(this.getCurrentFileName(), name);
					break;
				case ASTNode.QUALIFIED_NAME:
					result = this.getMemberType(this.getCurrentFileName(), name);
					break;
				case ASTNode.SIMPLE_NAME:
					TypeRef aux = this.getMemberType(this.getCurrentFileName()
								  ,((SimpleName)ctxNode).getIdentifier());
					if (aux != null)
						result = this.getMemberType(aux.getName(), name);
					if (result == null) {
						result = this.getVariableType(((SimpleName)ctxNode).getIdentifier(), ctxNode);
					}
					break;
				case ASTNode.FIELD_ACCESS:
					TypeRef parent = getCurrentTypeOf(ctxNode);
					result = this.getMemberType(parent.getName(), name);
					break;
				case ASTNode.METHOD_INVOCATION:
					TypeRef parent2 = getCurrentTypeOf(ctxNode);
					result = this.getMemberType(parent2.getName(), name);
					break;					
				case ASTNode.SUPER_FIELD_ACCESS:
					result = getSuperMemberType(name);
					break;
				case ASTNode.SUPER_METHOD_INVOCATION:
					result = getSuperMemberType(name);
					break;					
			}
		if (result != null)
			return result;
		else
			return this.getRootType();
	}

	/**
	 * Helper Function that returns the superType
	 * reference once the current contexts contains
	 * the updated information.
	 * @param name the name of the super member
	 * @return the super member type.
	 */
	private TypeRef getSuperMemberType(String name) {
		GExtendedClass sGhost = this.getSuperGhost();
		if (sGhost != null)
			for (GMember member : sGhost.getMembers())
				if (member.getName().equals(name))
					return member.getReturnType();
		return null;
	}
	
	/**
	 * Helper functions that verifies if the current node has
	 * an existing type and if this type is correctly matched 
	 * with its declaration type, in the current context.
	 * @param node the node under verification
	 * @param deep recursion deep, to avoid overflow
	 * @return the correct typing, a marker for bad typing or null
	 */
	private TypeRef getExistingType(Expression node, int deep) {
		ITypeBinding type = node.resolveTypeBinding();
		TypeRef result = null;
		TypeRef otherSide = null; //for assignments
		if(type != null) {
			result = this.getTypeRefFrom(type);
			if (node.getNodeType() == ASTNode.SIMPLE_NAME &&
				node.getParent().getNodeType() == ASTNode.ASSIGNMENT && deep == 0) {
				if (((Assignment)node.getParent()).getRightHandSide() == node) {
					Expression left = ((Assignment)node.getParent()).getLeftHandSide();
					otherSide = inferTypeOf(left, deep + 1);
				}
				else if (((Assignment)node.getParent()).getLeftHandSide() == node) {
					Expression right = ((Assignment)node.getParent()).getRightHandSide();
					otherSide = inferTypeOf(right, deep + 1);
				}
				if (otherSide != null &&
					!otherSide.getName().equals("Wrong Typing") &&
					!otherSide.getName().equals("Undefined")) {
					if (result.getName().equals(otherSide.getName()))
						return result;
					if (result.getName().equals("Object") ||
							result.getName().equals("java.lang.Object")	)
						return result;
					if (otherSide.getName().equals("Object") ||
							otherSide.getName().equals("java.lang.Object")	)
						return result;
					ITypeBinding pivot = type;
					while (pivot != null) {
						if(pivot.getSuperclass() != null
						   && pivot.getSuperclass().getQualifiedName().equals(otherSide.getName()))
							return result;
						pivot = pivot.getSuperclass();
					}
					if (otherSide.getRef().getClass().getName().equals("org.eclipse.jdt.core.dom.TypeBinding")) {
						pivot = (ITypeBinding) otherSide.getRef();
						while (pivot != null) {
							if(pivot.getSuperclass() != null
							   && pivot.getSuperclass().getQualifiedName().equals(result.getName()))
								return result;
							pivot = pivot.getSuperclass();
						}						
					}
					return new TypeRef("Wrong Typing", true);
				}
			}
		}
		return result;
	}
	
	/**
	 * Inference function, to obtain the more accurate type
	 * for a certain ASTNode.
	 * @param node the ASTNode to be resolved
	 * @return the best inferred type
	 */
	private TypeRef inferContextTypeOf(ASTNode node, int deep) {
		ArrayList<String> intChar = new ArrayList<String>();
		intChar.add("int");
		intChar.add("char");
		ArrayList<String> intCharString = new ArrayList<String>();
		intCharString.add("int");
		intCharString.add("char");
		intCharString.add("String");
		ASTNode ctxNode = node.getParent();
		switch (ctxNode.getNodeType()) {
		// int a = <node>;
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: 
			return inferTypeOfVar(((VariableDeclarationFragment)ctxNode).getName());
		//a.b
		case ASTNode.FIELD_ACCESS:
			return getCurrentTypeOf(node);	
		// method_invk(arg0, ..,<node>);
		case ASTNode.METHOD_INVOCATION:
			TypeRef current = getCurrentTypeOf(node);
			if (current != null)
				return current;
			else
				return inferTypeOfArg(node,(MethodInvocation)ctxNode);
		//a.b.c
		case ASTNode.QUALIFIED_NAME:
			return getCurrentTypeOf(node);	
		// return <node>;
		case ASTNode.RETURN_STATEMENT: 
			return inferReturnTypeOf(ctxNode);
		// <node>;			
		case ASTNode.EXPRESSION_STATEMENT: 
			return new TypeRef("void", true);
		// do{..}while(<node>);
		case ASTNode.DO_STATEMENT:
		// while(<node>){}
		case ASTNode.WHILE_STATEMENT:
		// if(<node>) ..	
		case ASTNode.IF_STATEMENT:		
			return new TypeRef("boolean", true);
		// <node> = 1; or var = <node>;	
		case ASTNode.ASSIGNMENT:	
			return this.inferAssignmentGuest(node,(Assignment)ctxNode);
		//<operation> <node>;
		case ASTNode.PREFIX_EXPRESSION:
			PrefixExpression preExpr = (PrefixExpression) ctxNode;
			if(preExpr.getOperator() == PrefixExpression.Operator.NOT)
				return new TypeRef("boolean", true);
			else
				return new UnionTypeRef(intChar, true);
		//<node> <operation>;
		case ASTNode.POSTFIX_EXPRESSION:
			return new UnionTypeRef(intChar, true);
		// ... <node> <operation> <node> ... ;
		case ASTNode.INFIX_EXPRESSION:
			InfixExpression expr = (InfixExpression) ctxNode;
			if(expr.getOperator() == InfixExpression.Operator.CONDITIONAL_AND ||
			   expr.getOperator() == InfixExpression.Operator.CONDITIONAL_OR ||
			   expr.getOperator() == InfixExpression.Operator.AND ||
			   expr.getOperator() == InfixExpression.Operator.OR ||
			   expr.getOperator() == InfixExpression.Operator.XOR)
				if (node.getNodeType() == ASTNode.INFIX_EXPRESSION)
					return inferContextTypeOf(ctxNode, deep);
				else
					return new TypeRef("boolean", true);
			else if(expr.getOperator() == InfixExpression.Operator.LESS ||
					expr.getOperator() == InfixExpression.Operator.GREATER ||
					expr.getOperator() == InfixExpression.Operator.LESS_EQUALS ||
					expr.getOperator() == InfixExpression.Operator.GREATER_EQUALS ||
					expr.getOperator() == InfixExpression.Operator.TIMES ||
					expr.getOperator() == InfixExpression.Operator.DIVIDE ||
					expr.getOperator() == InfixExpression.Operator.REMAINDER ||
					expr.getOperator() == InfixExpression.Operator.MINUS ||
					expr.getOperator() == InfixExpression.Operator.LEFT_SHIFT ||
					expr.getOperator() == InfixExpression.Operator.RIGHT_SHIFT_SIGNED ||
					expr.getOperator() == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)
					if (node.getNodeType() == ASTNode.INFIX_EXPRESSION)
						return inferContextTypeOf(ctxNode, deep);
					else
						return new UnionTypeRef(intChar, true);
			else if(expr.getOperator() == InfixExpression.Operator.PLUS)
				if (node.getNodeType() == ASTNode.INFIX_EXPRESSION)
					return inferContextTypeOf(ctxNode, deep);
				else if(inferContextTypeOf(node.getParent(),0).equals(new TypeRef("String", true)))
					return new TypeRef("String", true);
				else if(inferContextTypeOf(node.getParent(),0).equals(new TypeRef("int", true)))
					return new UnionTypeRef(intChar, true);
				else if(inferContextTypeOf(node.getParent(),0).equals(new TypeRef("char", true)))
					return new UnionTypeRef(intChar, true);
				else
					return new UnionTypeRef(intCharString, true);				
			else//== !=
				if (node == expr.getLeftOperand())//TODO not flawless :/
					if (expr.getRightOperand().getNodeType() == ASTNode.INFIX_EXPRESSION) {
						InfixExpression right = (InfixExpression) expr.getRightOperand();	
						return inferContextTypeOf(right.getLeftOperand(), deep);
					}
					else if (deep > 0)
						return this.getRootType();
					else
						return inferTypeOf(expr.getRightOperand(), deep + 1);	
				else if (node == expr.getRightOperand())
					if (expr.getLeftOperand().getNodeType() == ASTNode.INFIX_EXPRESSION) {
						InfixExpression left = (InfixExpression) expr.getLeftOperand();	
						return inferContextTypeOf(left.getLeftOperand(), deep);
					}
					else if (deep > 0)
						return this.getRootType();
					else
						return inferTypeOf(expr.getLeftOperand(), deep + 1);
				else
					return inferContextTypeOf(ctxNode, deep);
		}
		// everything failed, return Object
		//System.out.println(ctxNode.getNodeType());
		return this.getRootType();
	}
	
	/**
	 * helper function to type inner members of
	 * a chained call
	 * @param node the field access or method invocation
	 * @return the current type or null
	 */
	private TypeRef getCurrentTypeOf(ASTNode node) {
		String name = "";
		switch (node.getNodeType()) {
			case ASTNode.FIELD_ACCESS:
				name = ((FieldAccess) node).getName().getIdentifier();
				break;
			case ASTNode.METHOD_INVOCATION:
				name = ((MethodInvocation) node).getName().getIdentifier();
				break;
			case ASTNode.QUALIFIED_NAME:
				name = ((QualifiedName) node).getName().getIdentifier();
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				name = ((SuperFieldAccess) node).getName().getIdentifier();
				break;
			case ASTNode.SUPER_METHOD_INVOCATION:
				name = ((SuperMethodInvocation) node).getName().getIdentifier();
				break;				
			default:
				return null;
		}
		TypeRef result = inferCurrentTypeOf((Expression) node, name);
		//Unknown types
		if (result.getName().equals("java.lang.Object")) {
			TypeRef noName = new TypeRef("noName" + this.counter,false);
			GClass noClass = new GClass("noName" + this.counter++);
			noName.setRef(noClass);
			this.getGhosts().add(noClass);
			return noName;
		}
		else
			return result;
	}
	
	/**
	 * Helper Function that infer the type of an assignment
	 * based on the type of either the right side, or the left
	 * side expression.
	 * @param node the node inside the assignment
	 * @param ctxNode the assignment expression
	 * @return the best inferred type
	 */
	private TypeRef inferAssignmentGuest(ASTNode node, Assignment ctxNode) {
		//var = <node>;
		Expression otherSide = ctxNode.getLeftHandSide();
		//<node> = 1;
		if(node == ctxNode.getLeftHandSide()) otherSide = ctxNode.getRightHandSide();
		//basic JDT inference
		ITypeBinding type = otherSide.resolveTypeBinding();
		if(type != null) return this.getTypeRefFrom(type);
		
		switch (otherSide.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				return inferTypeOfVar((SimpleName)otherSide);
		}	// TODO other cases aren't allowed
		return this.getRootType();
	}

	/**
	 * Inference function, to obtain the more accurate type
	 * for the return of a certain ASTNode.
	 * @param node the ASTNode to be resolved
	 * @return the best inferred return type
	 */
	private TypeRef inferReturnTypeOf(ASTNode node) {
		ASTNode _mth = getDeclaringMethodOrType(node);
		if(_mth.getNodeType() == ASTNode.METHOD_DECLARATION) {
			MethodDeclaration mth = (MethodDeclaration) _mth;
			return getTypeRefFrom(mth.getReturnType2().resolveBinding());
		}
		return this.getRootType();
	}

	/**
	 * Function that infers the type of a method
	 * argument by checking the resolved type of
	 * the method invocation
	 * @param arg the argument within the method
	 * @param mth the method invocation 
	 * @return the type of the argument or Object
	 * as default
	 */
	private TypeRef inferTypeOfArg(ASTNode arg, MethodInvocation mth) {
		int idx = mth.arguments().indexOf(arg);
		if(mth.resolveMethodBinding() != null)
			return this.getTypeRefFrom(mth.resolveMethodBinding().getParameterTypes()[idx]);
		//TODO may be it is in the ghost
		return this.getRootType();
	}

	/**
	 * Function that returns the Type Reference
	 * of a certain variable inside a certain
	 * context.
	 * @param node the SimpleName of the variable
	 * @return the inferred type or Object type
	 */
	private TypeRef inferTypeOfVar(SimpleName node) {
		IVariableBinding varBiding = (IVariableBinding) node.resolveBinding();
		if(varBiding != null) 
			return this.getTypeRefFrom(varBiding.getType());
		ITypeBinding type = this.getDeclaringTypeOfVar(node);
		if(type != null) 
			return this.getTypeRefFrom(type);
		if (node.getParent().getNodeType() == ASTNode.ASSIGNMENT)
			return new TypeRef("Undefined", true);
		return this.getRootType();
	}
		
	/**
	 * Function that lookup for the declaration type of a certain
	 * SimpleName, if it does not find it in the current context,
	 * it keeps looking in the parent context.
	 * @param node the SimpleNode for lookup
	 * @return the type of the node or null if fails
	 */
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node) {
		//Get the corresponding ASTNode object of a certain SimpleName
		ASTNode declNode = this.getDeclaringVarNodeParent(node);
		//lookup in the current context
		while(declNode != null) {	
			ITypeBinding type = null;			
			switch (declNode.getNodeType()) {
			// { <node> }
			case ASTNode.BLOCK: 
				type = getDeclaringTypeOfVar(node, (Block)declNode); break;
			// for each (.. <node> in .. <node>)
			case ASTNode.ENHANCED_FOR_STATEMENT: 
				type = getDeclaringTypeOfVar(node, (EnhancedForStatement)declNode); break;
			// for(.. <node>; .. <node>; .. <node>)
			case ASTNode.FOR_STATEMENT: 
				type = getDeclaringTypeOfVar(node, (ForStatement)declNode); break;
			// catch( .. <node>)
			case ASTNode.CATCH_CLAUSE: 
				type = getDeclaringTypeOfVar(node, (CatchClause)declNode); break;
			// .. <node>(..., .. <node>){..}
			case ASTNode.METHOD_DECLARATION: 
				type = getDeclaringTypeOfVar(node, (MethodDeclaration)declNode); break;
			// int <node>;
			case ASTNode.TYPE_DECLARATION: 
				type = getDeclaringTypeOfVar(node, (TypeDeclaration)declNode); break;
			}
			if(type != null) return type;
			//Go up for the parent Node context
			declNode = this.getDeclaringVarNodeParent(declNode);
		}	
		return null; //fail value
	}
	
	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the TypeDeclaration type, which is equivalent to the type
	 * of the field, inside the TypeDeclaration, that matches the
	 * SimpleName.
	 * @param node the original SimpleName
	 * @param typeNode the deducted TypeDeclaratio
	 * @return the type of the node or null if fails
	 */
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, TypeDeclaration typeNode) {
		for (FieldDeclaration field : typeNode.getFields()) {
			ITypeBinding type = this.getDeclaringTypeOfVar(node, field.fragments(), field.getType());
			if(type != null) return type;
		}
		return null;
	}

	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the MethodDeclaration type, which is equivalent to the type
	 * of the parameter, inside the MethodDeclaration, that matches the
	 * SimpleName.
	 * @param node the original SimpleName
	 * @param typeNode the deducted MethodDeclaration
	 * @return the type of the node or null if fails
	 */
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, MethodDeclaration mthNode) {
		for (Object _param : mthNode.parameters()) {
			ITypeBinding type = this.getDeclaringTypeOfVar(node, (SingleVariableDeclaration) _param);
			if(type != null) return type;
		}
		return null;
	}

	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the CatchClause type, which is equivalent to the type
	 * of the exception, inside the CatchClause, if it matches 
	 * the SimpleName.
	 * @param node the original SimpleName
	 * @param typeNode the deducted CatchClause
	 * @return the type of the node or null if fails
	 */
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, CatchClause catchNode) {
		return this.getDeclaringTypeOfVar(node, catchNode.getException());	
	}

	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the ForStatement type, which is equivalent to the type
	 * of the initializer, inside the ForStatement, that matches the
	 * SimpleName.
	 * @param node the original SimpleName
	 * @param typeNode the deducted ForStatement
	 * @return the type of the node or null if fails
	 */
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, ForStatement forNode) {
		for (Object _init : forNode.initializers()) {
			if(((Expression)_init).getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
				VariableDeclarationExpression varDecl = (VariableDeclarationExpression) _init;
				ITypeBinding type = this.getDeclaringTypeOfVar(node,
						varDecl.fragments(),varDecl.getType());
				if(type!=null) return type;
			}
		}
		return null;
	}
	
	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the EnhancedForStatement type, which is equivalent to the type
	 * of the parameter, inside the EnhancedForStatement, if it matches the
	 * SimpleName.
	 * @param node the original SimpleName
	 * @param typeNode the deducted EnhancedForStatement
	 * @return the type of the node or null if fails
	 */	
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, EnhancedForStatement forNode) {
		return this.getDeclaringTypeOfVar(node, forNode.getParameter());
	}
	
	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the SingleVariableDeclaration type, by basic resolve
	 * Binding.
	 * @param node the original SimpleName
	 * @param typeNode the deducted SingleVariableDeclaration
	 * @return the type of the node or null if fails
	 */		
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, SingleVariableDeclaration param) {
		if(node.getIdentifier().equals(param.getName().getIdentifier()))
			return param.getType().resolveBinding();		
		return null;
	}
	
	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the Block type, which is equivalent to the type
	 * of the variable declaration statement, inside the Block, 
	 * that are after the SimpleName declaration and matches
	 * the SimpleName.
	 * @param node the original SimpleName
	 * @param typeNode the deducted Block
	 * @return the type of the node or null if fails
	 */	
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, Block block) {
		for (Object _stat : block.statements())
			if(((Statement) _stat).getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT){
				VariableDeclarationStatement statement = (VariableDeclarationStatement) _stat;
				if(node.getStartPosition() > statement.getStartPosition()){
					ITypeBinding type = this.getDeclaringTypeOfVar(node,
							statement.fragments(),statement.getType());
					if(type!=null) return type;

				}
			}
		return null;
	}
	
	/**
	 * Helper Function for getDeclaringTypeOfVar(SimpleName node)
	 * for the code fragments. If any of the objects in the fragments
	 * matches, by identifier, the SimpleNode it returns the resolved
	 * Type, or null otherwise.
	 * @param node the original SimpleName
	 * @param fragments the list of code fragments
	 * @param type the Type to be tested
	 * @return the type of the node or null if fails
	 */		
	@SuppressWarnings("rawtypes")
	private ITypeBinding getDeclaringTypeOfVar(SimpleName node, List fragments, Type type) {
		for (Object _decl : fragments)
			if(node.getIdentifier().equals(
				((VariableDeclarationFragment) _decl).getName().getIdentifier()))
				return type.resolveBinding();
		return null;
	}

	/**
	 * Function that returns the parent node of a ASTNode.
	 * @param node the ASTNode
	 * @return the parent ASTNode or null
	 */
	private ASTNode getDeclaringVarNodeParent(ASTNode node) {
		ASTNode parentNode = node.getParent();
		switch (parentNode.getNodeType()) {
			// { <node> }
			case ASTNode.BLOCK:
			// for each (.. <node> in .. <node>)	
			case ASTNode.ENHANCED_FOR_STATEMENT:
			// for(.. <node>; .. <node>; .. <node>)
			case ASTNode.FOR_STATEMENT:
			// catch( .. <node>)	
			case ASTNode.CATCH_CLAUSE:
			// .. <node>(..., .. <node>){..}
			case ASTNode.METHOD_DECLARATION:
			// int <node>;	
			case ASTNode.TYPE_DECLARATION: return parentNode;
			// full classes
			case ASTNode.COMPILATION_UNIT: return null;
		}
		return this.getDeclaringVarNodeParent(parentNode);
	}
	
	/**
	 * Function that verifies if a type binding is either
	 * a ghost type or a type, and returns the type.
	 * @param type the type binding
	 * @return type the corresponding type
	 */
	private TypeRef getTypeRefFrom(ITypeBinding type) {
		String typeName = type.getQualifiedName();
		TypeRef typeRef = new TypeRef(typeName, !type.isRecovered()).setRef(type);
		if(!typeRef.isConcrete())
			typeRef.setRef(this.getGhostType(typeName));
		return typeRef;
	}
	
	/**
	 * Basic return type, Object
	 * @return Object type
	 */
	private TypeRef getRootType() {
		return new TypeRef("java.lang.Object", true);
	}


	/**
	 * Function that creates and returns the reference
	 * to the Marker of a certain Ghost.
	 * @param node the node look for
	 * @param ghost the ghost where the node is looked
	 * @return the Marker Reference
	 */
	public ISourceRef getSourceRef(ASTNode node, ASTNode simple, Ghost ghost) {
		int startChar = node.getStartPosition();
		IFile file = (IFile) this.cUnit.getJavaElement().getResource();
		ISourceRef ref = null;
		try {
			ref = new MarkerSourceRef(
					file, 
					this.cUnit.getLineNumber(startChar), 
					startChar, 
					startChar + node.getLength(),
					ghost.toString(),
					simple);
			ASTNode parent = node;
			//TODO Change here
			while(parent.getNodeType() != ASTNode.TYPE_DECLARATION) {
				parent = this.getDeclaringMethodOrType(parent);
				int line = this.cUnit.getLineNumber(parent.getStartPosition());
				if(file != null &&
					!GhostMarker.existGCxtMarkIn(line, file))
					GhostMarker.createGCxtMark(file, line);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return ref;
	}
	
	/**
	 * Helper function that returns the parent ASTNode
	 * of a certain node. 
	 * @param node the node
	 * @return the parent ASTNode
	 */
	private ASTNode getDeclaringMethodOrType(ASTNode node) {
		ASTNode parent = this.getDeclaringVarNodeParent(node);
		switch (parent.getNodeType()) {
			case ASTNode.TYPE_DECLARATION:
			case ASTNode.METHOD_DECLARATION: return parent;
		}
		return this.getDeclaringVarNodeParent(node);
	}
	
	/**
	 * Helper function that checks if a type is a 
	 * mutable interface or not.
	 * @param typeNode the SimpleType
	 * @return true if it is a mutable interface
	 */
	private boolean isMutableInterface(SimpleType typeNode) {
		try {
			TypeDeclaration decl = (TypeDeclaration) typeNode.getParent();
			for (Object iface : decl.superInterfaceTypes())
				if(typeNode.equals(iface)) 
					return false;
		} catch (Exception _){}	
		return true;
	}
	
	/**
	 * Helper function that checks if a type is a 
	 * mutable class or not.
	 * @param typeNode the SimpleType
	 * @return true if it is a mutable class
	 */
	private boolean isMutableClass(SimpleType typeNode) {
		try {
			TypeDeclaration decl = (TypeDeclaration) typeNode.getParent();
			//check if the super class is none or not
			return ! (decl.getSuperclassType() == typeNode);
		} catch (Exception _){}
		
		return true;
	}
	
	/**	
	 * Helper function to obtain the type (class)
	 * using its name.
	 * @param qName
	 * @return the type (class or interface) or null
	 */
	public GBehaviorType getGhostType(String qName) {
		for (Ghost ghost : ghosts) {
			if(!ghost.isMember() && ghost.getName().equals(qName)) 
				return (GBehaviorType) ghost;
		}
		return null;
	}
	
	/**	
	 * Helper function to obtain the type of a ghost
	 * member.
	 * @param cName the name of the owning context
	 * @param mName the name of the member
	 * @return the TypeRef or null
	 */
	public TypeRef getMemberType(String cName, String mName) {
		for (Ghost ghost : ghosts) {
			if(ghost.isMember()) {
			   if (ghost.getName().equals(mName) &&
				   ((GMember) ghost).getOwnerType().getName().equals(cName))
				return ((GMember) ghost).getReturnType();
			}
			else if(!ghost.isVariable()) {
				for (GMember member: ((GBehaviorType)ghost).getMembers())
					if (member.getName().equals(mName) &&
						member.getOwnerType().getName().equals(cName))
						return member.getReturnType();
			}
		}
		return null;
	}
	
	private TypeRef getVariableType(String varName, Expression ctxNode) {
		for (Ghost ghost : ghosts) {
			if(ghost.isVariable() && ghost.getName().equals(varName)) {
				ASTNode parent = ctxNode.getParent();
				while (parent != null) {
					if (parent.equals(((GVariable) ghost).getContext()))
						return ((GVariable) ghost).getReturnType();
					else
						parent = parent.getParent();
				}
			}
		}
		return null;
	}
}
