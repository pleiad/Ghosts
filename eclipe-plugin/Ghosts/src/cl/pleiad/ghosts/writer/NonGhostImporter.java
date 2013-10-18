package cl.pleiad.ghosts.writer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.IDocument;


public class NonGhostImporter extends AbstractGhostBuster {

	private ImportDeclaration import0;

	@Override
	protected void applyChanges() throws Exception {
		ListRewrite memberList = writer.getListRewrite(ownerNode.getParent(),
				CompilationUnit.IMPORTS_PROPERTY);
		memberList.insertLast(import0, null);

		ITextFileBufferManager bufferManager = FileBuffers
				.getTextFileBufferManager(); 

		bufferManager.connect(path, LocationKind.IFILE, null);
		ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
		IDocument document = textFileBuffer.getDocument();

		writer.rewriteAST(document, null).apply(document);

		textFileBuffer
				.commit(null /* ProgressMonitor */, false /* Overwrite */); // (3)

		bufferManager.disconnect(path, LocationKind.IFILE, null);
	}

	@Override
	protected void createDeclaration() throws Exception {
		import0 = ast.newImportDeclaration();
		import0.setName(ast.newName(qName));
	}

	protected IPath path;
	protected String qName;
	
	public void setPath(IPath path) {
		this.path = path;
	}

	public static void importType(String qName, IPath location,
			TypeDeclaration typeDeclaration) throws Exception {
		NonGhostImporter buster=new NonGhostImporter();
		buster.setQName(qName);
		buster.setPath(location);
		buster.setOwnerNode(typeDeclaration);
		buster.write();
	}

	public NonGhostImporter setOwnerNode(TypeDeclaration _astNode){
		this.ownerNode = _astNode;
		ASTNode nn=ownerNode.getParent();
		this.ast = nn.getAST();
		this.writer=ASTRewrite.create(ast);
		return this;
	}
	
	public NonGhostImporter setQName(String qName) {
		this.qName = qName;
		return this;
	}

}
