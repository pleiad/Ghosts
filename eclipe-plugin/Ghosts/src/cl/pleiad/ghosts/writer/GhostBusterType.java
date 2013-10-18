package cl.pleiad.ghosts.writer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jface.text.IDocument;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.TypeRef;
import cl.pleiad.ghosts.engine.SGhostEngine;


public class GhostBusterType extends AbstractGhostBuster {

	private CompilationUnit cUnit;
	private IPackageFragment pkg;
	
	@Override
	protected void applyChanges() throws Exception {
		
		ListRewrite memberList = writer.getListRewrite(cUnit
				,CompilationUnit.TYPES_PROPERTY);
		memberList.insertLast(ownerNode, null);

		IPath path= this.getPath();
		
		ITextFileBufferManager bufferManager = FileBuffers
				.getTextFileBufferManager(); 

		bufferManager.connect(path, LocationKind.IFILE, null);
		ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
		IDocument document = textFileBuffer.getDocument();

		writer.rewriteAST(document, null).apply(document);

		textFileBuffer
				.commit(null /* ProgressMonitor */, false /* Overwrite */); // (3)

		bufferManager.disconnect(path, LocationKind.IFILE, null);
		
		applyBodyChanges();
	}
	
	private IPath getPath(){
		return ((ICompilationUnit)cUnit.getJavaElement()).getPath();
	}

	private void applyBodyChanges() throws Exception {
		cUnit = SGhostEngine.parse(pkg.getCompilationUnit(getCompilationUnitName()));
		for (GMember member : ((GBehaviorType)ghost).getMembers()) {
			GhostBusterMember.bust(member, getPath(),(TypeDeclaration)cUnit.types().get(0));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void createDeclaration() throws Exception {
		cUnit=SGhostEngine.parse(pkg.createCompilationUnit(getCompilationUnitName(), "", false, null));
		this.ast=cUnit.getAST();
		this.writer=ASTRewrite.create(ast); 
		
		ownerNode = ast.newTypeDeclaration();
		ownerNode.setName(this.getGhostName());
		ownerNode.setInterface(ghost.kind()==Ghost.INTERFACE);
		if (ghost.kind() != Ghost.INTERFACE) {
			GClass ghostC = ((GBehaviorType)ghost).asClass();
			if (ghostC.getSuperCls() != null) {
				TypeRef superC = ghostC.getSuperCls();
				ownerNode.setSuperclassType(this.getTypeByName(superC.getName()));
			}
		}
		ownerNode.modifiers().add(this.getPublicModifier());
	}
	
	private String getCompilationUnitName(){
		return ghost.getName()+".java";
	}

	public GhostBusterType setPkg(IPackageFragment pkg) {
		this.pkg = pkg;
		return this;
	}
	
	private Type getTypeByName(String typeName) {
		if (typeName.equals("boolean"))
			return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
		else if(typeName.equals("byte"))
			return ast.newPrimitiveType(PrimitiveType.BYTE);
		else if(typeName.equals("char"))
			return ast.newPrimitiveType(PrimitiveType.CHAR);
		else if(typeName.equals("double"))
			return ast.newPrimitiveType(PrimitiveType.DOUBLE);
		else if(typeName.equals("float"))
			return ast.newPrimitiveType(PrimitiveType.FLOAT);
		else if(typeName.equals("int") || typeName.equals("Union")) //most generic
			return ast.newPrimitiveType(PrimitiveType.INT);
		else if(typeName.equals("long"))
			return ast.newPrimitiveType(PrimitiveType.LONG);
		else if(typeName.equals("short"))
			return ast.newPrimitiveType(PrimitiveType.SHORT);
		else if(typeName.equals("void"))
			return ast.newPrimitiveType(PrimitiveType.VOID);
		else {
			SimpleName tName = ast.newSimpleName(typeName);
			Type t = ast.newSimpleType(tName);
			return t;
		}
	}

}
