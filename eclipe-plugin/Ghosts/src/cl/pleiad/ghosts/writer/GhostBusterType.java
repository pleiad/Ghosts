package cl.pleiad.ghosts.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;

import javax.annotation.Resource;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
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

		bufferManager.connect(path, null);
		ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
		IDocument document = textFileBuffer.getDocument();

		writer.rewriteAST(document, null).apply(document);

		textFileBuffer
				.commit(null /* ProgressMonitor */, false /* Overwrite */); // (3)

		bufferManager.disconnect(path, null);
		
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

	@Override
	protected void createDeclaration() throws Exception {
		cUnit=SGhostEngine.parse(pkg.createCompilationUnit(getCompilationUnitName(), "", false, null));
		this.ast=cUnit.getAST();
		this.writer=ASTRewrite.create(ast); 
		
		ownerNode = ast.newTypeDeclaration();
		ownerNode.setName(this.getGhostName());
		ownerNode.setInterface(ghost.kind()==Ghost.INTERFACE);
		ownerNode.modifiers().add(this.getPublicModifier());
	}
	
	private String getCompilationUnitName(){
		return ghost.getName()+".java";
	}

	public GhostBusterType setPkg(IPackageFragment pkg) {
		this.pkg = pkg;
		return this;
	}

}
