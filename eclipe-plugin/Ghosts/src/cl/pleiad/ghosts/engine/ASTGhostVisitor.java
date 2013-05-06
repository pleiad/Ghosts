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
import cl.pleiad.ghosts.dependencies.TypeRef;
import cl.pleiad.ghosts.markers.GhostMarker;

/**
 * Visitor Class for the Ghost Engine
 *
 */
public class ASTGhostVisitor extends ASTVisitor {

	private Vector<Ghost> ghosts;
	private CompilationUnit cUnit;
	private GBlackList blackList;
	
	/**
	 * Getter function for the Ghosts Vector
	 * @return the ghost Vector
	 */
	public Vector<Ghost> getGhosts() {
		return ghosts;
	}
	
	/**
	 * Getter function for the Ghosts Vector
	 * @param initList the initial Vector of Ghosts
	 * @return the Visitor, updated
	 */
	public ASTGhostVisitor setGhosts(Vector<Ghost> initList) {
		this.ghosts = initList;
		return this;
	}

	public ASTGhostVisitor() {
		super();
		ghosts = new Vector<Ghost>();
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
		if(similarity == Ghost.EQUALS) return (ghost).absorb(member);
		
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
			GhostMarker.createJavaProblemMarkersFrom(ghost);
			GhostMarker.createJavaProblemMarkersFrom(member);
		
		}
		
		return null;
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
	private GMember checkAndUnify(GMember member,int kind) {
		for(Ghost ghost : ghosts) {				
			if(ghost.isMember()) {
				GMember aux = this.checkAndUnifyHelper((GMember)ghost, member, kind);
				if(aux!=null) return aux;
			} else
				for(GMember gmember : ((GBehaviorType)ghost).getMembers()) {
					GMember aux=this.checkAndUnifyHelper(gmember, member, kind);
					if(aux!=null) return aux;
				}					
		}//if there are not similars!
		
		if(member.getOwnerType().isConcrete()) ghosts.add(member);
		else this.getGhostType(member.getOwnerType().getName())
					.getMembers().add(member);			
			
		return member;
	}
	
	/**
	 * Inference function, to obtain the more accurate
	 * type for a certain expression.
	 * @param node the expression to be resolved
	 * @return the best inferred type
	 */
	private TypeRef inferTypeOf(Expression node) {
		//basic JDT inference
		ITypeBinding type = node.resolveTypeBinding();
		if(type!=null)
			return this.getTypeRefFrom(type);
		switch (node.getNodeType()) {
		// N;
		case ASTNode.SIMPLE_NAME: return inferTypeOfVar((SimpleName) node);
		// method_invk(arg0, ..,<node>);
		case ASTNode.METHOD_INVOCATION: 
		// N.x	
		case ASTNode.FIELD_ACCESS:
		// N.x;
		case ASTNode.QUALIFIED_NAME: return inferContextTypeOf(node);
		}
		
		// everything failed, return Object
		return this.getRootType();
	}
	
	/**
	 * Inference function, to obtain the more accurate type
	 * for a certain ASTNode.
	 * @param node the ASTNode to be resolved
	 * @return the best inferred type
	 */
	private TypeRef inferContextTypeOf(ASTNode node) {
		ASTNode ctxNode = node.getParent();
		switch (ctxNode.getNodeType()) {
		// int a = <node>;
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: 
			return inferTypeOfVar(((VariableDeclarationFragment)ctxNode).getName());
		// method_invk(arg0, ..,<node>);
		case ASTNode.METHOD_INVOCATION: 
			return inferTypeOfArg(node,(MethodInvocation)ctxNode);
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
		}
		// everything failed, return Object
		return this.getRootType();
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
		TypeRef typeRef = new TypeRef(type.getQualifiedName(), !type.isRecovered()).setRef(type);
		if(!typeRef.isConcrete())
			typeRef.setRef(this.getGhostType(type.getQualifiedName()));
		return typeRef;
	}
	
	/**
	 * Basic return type, Object
	 * @return Object type
	 */
	private TypeRef getRootType() {
		return new TypeRef("java.lang.Object", true);
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
		mth.setOwnerType(this.inferTypeOf(mthNode.getExpression()));
		//not a ghost
		if(mth.getOwnerType().isNonLocal()) 
			return;
		mth.setReturnType(this.inferTypeOf(mthNode));
		for (Object _arg : mthNode.arguments()) {
			Expression arg = (Expression) _arg;
			mth.getParamTypes().add(this.inferTypeOf(arg));
		}
		mth.getDependencies().add(this.getSourceRef(mthNode,mth));
		this.checkAndUnify(mth, Ghost.METHOD);
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
			gcons.getParamTypes().add(this.inferTypeOf(arg));
		}
		TypeRef owner = this.inferTypeOf(newNode);
		gcons.setOwnerType(owner);
		//not a ghost
		if(owner.isNonLocal()) 
			return;
		gcons.getDependencies().add(this.getSourceRef(newNode, gcons));
		gcons = (GConstructor)this.checkAndUnify(gcons, Ghost.CONSTRUCTOR);
		this.checkAndMutate(gcons);
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
		field.setReturnType(this.inferTypeOf(fieldNode));
		field.setOwnerType(this.inferTypeOf(fieldNode.getExpression()));
		//not a ghost
		if(field.getOwnerType().isNonLocal()) 
			return;
		field.getDependencies().add(this.getSourceRef(fieldNode,field));
		//check ifExist
		field=(GField) this.checkAndUnify(field, Ghost.FIELD);
		this.checkAndMutate(field);
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
		field.setReturnType(this.inferTypeOf(nameNode));
		field.setOwnerType(this.inferTypeOf(nameNode.getQualifier())); 
		//not a ghost
		if(field.getOwnerType().isNonLocal()) 
			return;
		// unknown.unknown2 is not supported!
		field.getDependencies().add(this.getSourceRef(nameNode, field));
		//check ifExist
		field = (GField) this.checkAndUnify(field, Ghost.FIELD);
		this.checkAndMutate(field);
	}
	
	/**
	 * Helper Function, to mutate an interface class
	 * to a class.
	 * @param member the GMember of the class
	 */
	private void checkAndMutate(GMember member) {
		TypeRef owner = member.getOwnerType();
		if(!owner.isConcrete()) {
			GBehaviorType ghost = (GBehaviorType) owner.getRef();
			if(ghost.kind() == Ghost.INTERFACE) {
				if(ghost.isMutable()) {
					GBehaviorType newGhost = ghost.asClass();
					ghosts.remove(ghost);
					ghosts.add(newGhost);
					owner.setRef(newGhost);
				} else {
					GhostMarker.createJavaProblemMarkersFrom(member); 
				}	
			}				
		} 	
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
			Ghost ghost = this.getGhostConsidering(typeNode);
			ghost.getDependencies().add(this.getSourceRef(typeNode,ghost));

		}
	}

	/**
	 * Function that creates and returns the reference
	 * to the Marker of a certain Ghost.
	 * @param node the node look for
	 * @param ghost the ghost where the node is looked
	 * @return the Marker Reference
	 */
	private ISourceRef getSourceRef(ASTNode node, Ghost ghost) {
		int startChar = node.getStartPosition();
		IFile file = (IFile) this.cUnit.getJavaElement().getResource();
		ISourceRef ref = null;
		try {
			ref = new MarkerSourceRef(
					file, 
					this.cUnit.getLineNumber(startChar), 
					startChar, 
					startChar + node.getLength(),
					ghost.toString());
			ASTNode parent = node;
			do {
				parent = this.getDeclaringMethodOrType(parent);
				int line = this.cUnit.getLineNumber(parent.getStartPosition());
				if(!GhostMarker.existGCxtMarkIn(line, file))
					GhostMarker.createGCxtMark(file, line);
			} while(parent.getNodeType() != ASTNode.TYPE_DECLARATION);
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
		return this.getDeclaringVarNodeParent(parent); //will never happen!!
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
	 * Helper function that obtains, or creates
	 * the Ghost of a certain SimpleType.
	 * @param typeNode the SympleType
	 * @return the corresponding Ghost 
	 */
	private GBehaviorType getGhostConsidering(SimpleType typeNode) {
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
			ghost.setMutable(true); //at begining it is mutable
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
	 * Helper function to obtain the type (class)
	 * using its name.
	 * @param qName
	 * @return the type (class or interface) or null
	 */
	private GBehaviorType getGhostType(String qName) {
		for (Ghost ghost : ghosts) {
			if(!ghost.isMember() && ghost.getName().equals(qName)) 
				return (GBehaviorType) ghost;
		}
		return null;
	}

	public boolean visit(CompilationUnit unit) {
		this.cUnit = unit; //may be is not need with getRoot!!
		return super.visit(unit);
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
	
}
