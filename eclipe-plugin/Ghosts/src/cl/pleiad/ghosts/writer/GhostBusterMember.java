package cl.pleiad.ghosts.writer;

import java.util.Vector;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;


import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import cl.pleiad.ghosts.core.GConstructor;
import cl.pleiad.ghosts.core.GField;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.TypeRef;

public class GhostBusterMember extends AbstractGhostBuster {
	
	public static void bust(GMember member,IPath path,TypeDeclaration owner) throws Exception{
		GhostBusterMember buster=new GhostBusterMember();
		buster.setGhost(member);
		buster.setPath(path);
		buster.setOwnerNode(owner);
		buster.write();
	}
	
	
	protected BodyDeclaration node;
	
	protected IPath path;
	
	public void setPath(IPath path) {
		this.path = path;
	}
	
	public GhostBusterMember setOwnerNode(TypeDeclaration _astNode){
		this.ownerNode = _astNode;
		ASTNode nn=ownerNode.getParent();
		this.ast = nn.getAST();
		this.writer=ASTRewrite.create(ast);
		return this;
	}

	protected void applyChanges() throws Exception {
		ListRewrite memberList = writer.getListRewrite(ownerNode,
				TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		memberList.insertLast(node, null);

		ITextFileBufferManager bufferManager = FileBuffers
				.getTextFileBufferManager(); 

		bufferManager.connect(path, null);
		ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
		IDocument document = textFileBuffer.getDocument();

		writer.rewriteAST(document, null).apply(document);

		textFileBuffer
				.commit(null /* ProgressMonitor */, false /* Overwrite */); // (3)

		bufferManager.disconnect(path, null);
	}


	protected void createDeclaration() throws Exception{
		switch (ghost.kind()) {
			case Ghost.FIELD: createField(); break;
			case Ghost.METHOD: createMethod(false);break;
			case Ghost.CONSTRUCTOR: createMethod(true);break;
		}
	}

	protected void createMethod(boolean isConstructor) {
		GMethod gmember = (GMethod) ghost;
		MethodDeclaration mth=ast.newMethodDeclaration();
		mth.setName(this.getGhostName());
		mth.modifiers().add(getPublicModifier());
		int i=1;
		for (TypeRef type : gmember.getParamTypes()) {
			SingleVariableDeclaration param=ast.newSingleVariableDeclaration();
			param.setType(getTypeFrom(type));
			param.setName(ast.newSimpleName("t"+(i++))); 
			mth.parameters().add(param);
		}
		node = mth;
		
		if(!ownerNode.isInterface()) mth.setBody(ast.newBlock());
		
		if(isConstructor) mth.setConstructor(isConstructor);
		else{
			Type returnType=getTypeFrom(gmember.getReturnType());
			mth.setReturnType2(returnType);
			if (!ownerNode.isInterface()) {
				if(gmember.getReturnType().getName().equals("void")) return;
				ReturnStatement rst=ast.newReturnStatement();
				rst.setExpression(getDefaultValueFor(gmember.getReturnType().getName()));
				mth.getBody().statements().add(rst);
			}
		}
	}

	private Expression getDefaultValueFor(String qName) {
		
		if(qName.equals("byte") 
				  ||qName.equals("short")
				  ||qName.equals("int")
				  ||qName.equals("long")
				  ||qName.equals("float")
				  ||qName.equals("double"))
					return ast.newNumberLiteral("0");
				
		
		if(qName.equals("char")) return ast.newCharacterLiteral();
		
		if(qName.equals("boolean")) return ast.newBooleanLiteral(false);
		
		
		
		return ast.newNullLiteral();
	}

	protected void createField() {
		GField gfield = (GField) ghost;
		
		VariableDeclarationFragment varDecl =ast.newVariableDeclarationFragment();
		varDecl.setName(this.getGhostName());
		
		FieldDeclaration field=ast.newFieldDeclaration(varDecl);
		field.modifiers().add(getPublicModifier());
		field.setType(getTypeFrom(gfield.getReturnType()));
		node = field;		
	}
	
	private Type getTypeFrom(TypeRef typeRef) {
		Code code=null;
		if(typeRef.getName().equals("byte")) code=PrimitiveType.BYTE;
		if(typeRef.getName().equals("short")) code=PrimitiveType.SHORT;
		if(typeRef.getName().equals("char")) code=PrimitiveType.CHAR;
		if(typeRef.getName().equals("int")) code=PrimitiveType.INT;
		if(typeRef.getName().equals("long")) code=PrimitiveType.LONG;
		if(typeRef.getName().equals("float")) code=PrimitiveType.FLOAT;
		if(typeRef.getName().equals("double")) code=PrimitiveType.DOUBLE;
		if(typeRef.getName().equals("boolean")) code=PrimitiveType.BOOLEAN;
		if(typeRef.getName().equals("void")) code=PrimitiveType.VOID;
		
		if(code!=null) return ast.newPrimitiveType(code);
		
		return ast.newSimpleType(ast.newName(typeRef.getName()));
	}

}
