package cl.pleiad.ghosts.view;

import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.dependencies.ISourceRef;
import cl.pleiad.ghosts.engine.SGhostEngine;
import cl.pleiad.ghosts.markers.GhostMarker;
import cl.pleiad.ghosts.writer.GhostBusterMember;
import cl.pleiad.ghosts.writer.GhostBusterType;
import cl.pleiad.ghosts.writer.NonGhostImporter;


@SuppressWarnings("restriction")
public class GhostView extends ViewPart implements Observer,IDoubleClickListener{
	
	
//	protected abstract class SelectableAction {
//
//		protected GTreeNode ghostNode;
//		
//		protected SelectableAction(boolean enabled){
//			super();
//			//this.ghostNode = _node;
//			this.setText(this.getActionText());
//			this.setEnabled(enabled);
//		}
//		
//		protected abstract String getActionText();
//		protected abstract boolean selectionCriteria(GTreeNode selection);
//		
////		@Override
////		public void selectionChanged(SelectionChangedEvent event) {
////			IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
////			if(!selection.isEmpty())
////				for (Object selected : selection.toArray()) {
////					if( ! this.selectionCriteria((GTreeNode)selected))
////								continue;
////					
////					this.setEnabled(true);
////					ghostNode = (GTreeNode)selected;
////					return;
////				}
////			this.setEnabled(false);
////		}
//		
//	}
	
	protected class GhostBusterAction extends Action{

//		protected GhostBusterAction(GTreeNode _node) {
//			super(_node);
//		}

		protected GTreeNode ghostNode;
		public GhostBusterAction setGhostNode(GTreeNode _node){
			this.ghostNode = _node;
			if (_node.getKind() == GTreeNode.GBTYPE)
				this.setEnabled(!((GBehaviorType)_node.getValue()).getDependencies().isEmpty());
			return this;
		}
		
		protected GhostBusterAction() {
			super("Bust it!");
		}

		private IJavaProject bust(GTreeNode node) {
			switch (node.getKind()) {
			case GTreeNode.GHOST_SET:
				return this.writeChangesFrom((GhostSet) node.getValue(),node);
			case GTreeNode.GBTYPE:
				return this.writeChangesFrom((GBehaviorType) node.getValue(),node.getParent());
			case GTreeNode.GMEMBER:
				return this.writeChangesFrom((GMember) node.getValue(),node.getParent());
			}
			return null;
		}
		
		public void run() {
			IJavaProject project=null;
			
			/*if(ghostNode!=null) project=this.bust(ghostNode);
			else{
				IStructuredSelection sel =  (IStructuredSelection)viewer.getSelection();
				for (Object selected : sel.toArray())
					if(((GTreeNode)selected).isFree())
						project=this.bust((GTreeNode)selected);
				
			}*/
			project=this.bust(ghostNode!=null?ghostNode:getSingleSelection(viewer.getSelection()));
			
			if(project!=null) SGhostEngine.get().loadGhostsFrom(project);
			
		}
		
		private IJavaProject nodeToJavaProject(GTreeNode node){
			return ((GhostSet)node.getValue()).getProject();
		}
		
		private IJavaProject writeChangesFrom(GMember selected,GTreeNode node) {
			IJavaProject project= nodeToJavaProject(node);
			
			IType type;
			try {
				type = project.findType(selected.getOwnerType().getName());
				CompilationUnit cunit = SGhostEngine.parse(type.getCompilationUnit());
				for (Object _typeDecl : cunit.types()) { //beware!! of several types
					TypeDeclaration typeDecl = (TypeDeclaration) _typeDecl;
					if(typeDecl.getName().getFullyQualifiedName()
							.equals(selected.getOwnerType().getName())){
						GhostBusterMember.bust(selected, type.getPath(), typeDecl);
						break;
					}
				}
			} catch (Exception e) { e.printStackTrace(); return null;}
			//notify file and references!!! simpler -> project
			return project;
		}
		
		private IJavaProject writeChangesFrom(GhostSet selected,GTreeNode node) {
			IJavaProject project=null;
			for (Ghost ghost : selected.getGhosts()){
				IJavaProject partial=ghost.isMember()?
						this.writeChangesFrom((GMember)ghost,node):
						this.writeChangesFrom((GBehaviorType)ghost,node);
				if(partial!=null) project=partial;		
			}
			return project;
		}

		private IJavaProject writeChangesFrom(GBehaviorType selected, GTreeNode node) {
			
			IJavaProject project= nodeToJavaProject(node);
			try{
				for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) {
					if(root.getKind()==IPackageFragmentRoot.K_SOURCE){
						IPackageFragment pkg=root.getPackageFragment(""); //default pkg
						GhostBusterType buster=new GhostBusterType()
													.setPkg(pkg);
						buster.setGhost(selected);
						buster.write();
					break;}
				}
			}catch (Exception e) { e.printStackTrace(); return null;}
			//notify file and references!!! simpler -> project
			return project;
		}


		public ImageDescriptor getImageDescriptor(){
			return ImageDescriptor.createFromImage(
							GViewLabelProvider.getImageNamed("busted16.ico"));	
		}

		
		
//		public void checkSelection(SelectionChangedEvent event) {
//			IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
//			if(!selection.isEmpty())
//				for (Object selected : selection.toArray()) {
//					if( ! ((GTreeNode)selected).isFree())
//								continue;
//					
//					this.setEnabled(true);
//					return;
//				}
//			this.setEnabled(false);
//		}
		public void checkSelection(GTreeNode selected) {
			if(selected.isFree())	this.setEnabled(true);
			else					this.setEnabled(false);
		}
	}
	
	protected class ImportAction extends Action {
		
//		protected ImportAction(GTreeNode _node) {
//			super(_node);
//		}

		protected GBehaviorType ghost;
		public ImportAction setGhostNode(GBehaviorType _ghost){
			this.ghost = _ghost;
			return this;
		}
		
		protected ImportAction() {
			super("Import from existing package");
		}

		protected String qName;
		

		public void run(){
			qName = this.getQName();
			if(qName == null) return;
			IJavaProject project = null;
			
			for (ISourceRef ref : ghost.getDependencies()) {
				IFile file = ref.getFile();
				project = JavaCore.create(file.getProject());
				try {
					CompilationUnit cunit = SGhostEngine.parse(JavaCore.createCompilationUnitFrom(file));
					NonGhostImporter.importType(qName,file.getLocation(),
							(TypeDeclaration)cunit.types().get(0)); //beware!! of several types
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			
			if(project!= null) SGhostEngine.get().loadGhostsFrom(project);
		}
		
		protected String getQName(){
			OpenTypeSelectionDialog dialog= new OpenTypeSelectionDialog(
					JavaPlugin.getActiveWorkbenchShell(), true,
				    PlatformUI.getWorkbench().getProgressService(), null, 
				    IJavaSearchConstants.TYPE);
				dialog.setTitle(JavaUIMessages.OpenTypeAction_dialogTitle);
				dialog.setMessage(JavaUIMessages.OpenTypeAction_dialogMessage);
				dialog.setBlockOnOpen(true);
				dialog.setInitialPattern(ghost.getName());
			
			if(dialog.open() != Dialog.OK) return null;
			
			return ((BinaryType)dialog.getFirstResult()).getFullyQualifiedName();
		}

	}
	
	protected class GotoMarker extends Action {
		protected ISourceRef ref;
		
		public GotoMarker setRef(ISourceRef _ref){
			ref = _ref;
			this.setText(ref.toString());
			return this;
		}
		
		public void run(){
			if(ref==null) return;
			
			try {
				
				IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage(), ref.getGhostMarker());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	protected class GPopUpMenu implements ISelectionChangedListener{

		private GBehaviorType ghost;
		private GTreeNode node;
		
		private Action asClass =new Action("as Class") {
			public ImageDescriptor getImageDescriptor(){
				return ImageDescriptor.createFromImage(
						 JavaUI.getSharedImages()
						 	.getImage(
						 			ISharedImages.IMG_OBJS_CLASS));
			}
			public void run(){
				changeFor(ghost.asClass());
			}
		};
		
		private Action asInterface =new Action("as Interface") {
			public ImageDescriptor getImageDescriptor(){
				return ImageDescriptor.createFromImage(
						 JavaUI.getSharedImages()
						 	.getImage(
						 			ISharedImages.IMG_OBJS_INTERFACE));
			}
			public void run(){
				changeFor(ghost.asInterface());
			}
		};
		
		private void changeFor(GBehaviorType newGhost){
			IJavaProject project=((GhostSet)node.getParent().getValue()).getProject();
			node.replaceGhost(newGhost);
			
			try {
				GhostMarker.createStoredKind(project,newGhost);
			} catch (CoreException e) { e.printStackTrace(); }
			
			SGhostEngine.get().loadGhostsFrom(project);//TODO update only dependencies!
			update(null, null);
		}
		
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			
			menuMgr.removeAll();
			asInterface.setEnabled(true);
			asClass.setEnabled(true);
			
			node = getSingleSelection(event.getSelection());
			if(node==null) return;
			
			ghostBuster.checkSelection(node);
			rename.checkSelection(node);
			
			if(node.getKind()==GTreeNode.GBTYPE) {
				this.ghost=(GBehaviorType) node.getValue();
				menuMgr.add(new ImportAction().setGhostNode(ghost));
				this.addMutateAction();
				menuMgr.add(new GhostBusterAction().setGhostNode(node));
				menuMgr.add(new RenameAction().setGhostNode(node));
				this.addRefsAction(ghost,true);
			}	
				
			if(node.getKind()==GTreeNode.GMEMBER){
				boolean bustable=node.isFree();
				if(bustable) menuMgr.add(new GhostBusterAction().setGhostNode(node));
				menuMgr.add(new RenameAction().setGhostNode(node));
				this.addRefsAction((GMember) node.getValue(),bustable);
			}
		}
		
		private void addRefsAction(Ghost ghost,boolean separator){
			if(refsInMenu){
				if(separator) menuMgr.add(new Separator());
				for (ISourceRef ref : ghost.getDependencies())
					menuMgr.add(new GotoMarker().setRef(ref));
			}	
		}
		
		private void addMutateAction() {
			Action action= asInterface;
			if(ghost.kind()==Ghost.INTERFACE) action = asClass;

			action.setEnabled(ghost.isMutable());
			menuMgr.add(action);
		}
		
	}
	
	protected class RenameAction extends Action {
		
		protected GTreeNode ghostNode;
		public RenameAction setGhostNode(GTreeNode _node){
			this.ghostNode = _node;
			if (_node.getKind() == GTreeNode.GBTYPE)
				this.setEnabled(!((GBehaviorType)_node.getValue()).getDependencies().isEmpty());
			return this;
		}
		
		protected RenameAction() {
			super("Rename");
		}

		private IJavaProject rename (GTreeNode node) {
			switch (node.getKind()) {
			case GTreeNode.GHOST_SET:
				return this.writeChangesFrom((GhostSet) node.getValue(),node);
			case GTreeNode.GBTYPE:
				return this.writeChangesFrom((GBehaviorType) node.getValue(),node.getParent());
			case GTreeNode.GMEMBER:
				return this.writeChangesFrom((GMember) node.getValue(),node.getParent());
			}
			return null;
		}
		
		public void run() {
			IJavaProject project=null;
			project = this.rename(ghostNode!=null?ghostNode:getSingleSelection(viewer.getSelection()));
			if(project != null) SGhostEngine.get().loadGhostsFrom(project);
		}
		
		private IJavaProject nodeToJavaProject(GTreeNode node){
			if (node.getValue() instanceof GhostSet)
				return ((GhostSet)node.getValue()).getProject();
			else
				return nodeToJavaProject(node.getParent());
		}
		
		private IJavaProject writeChangesFrom(GhostSet selected, GTreeNode node) {
			IJavaProject project = selected.getProject();
			return project;
		}
		
		private IJavaProject writeChangesFrom(Ghost selected, GTreeNode node) {
			IJavaProject project = nodeToJavaProject(node);
			ImageIcon icon = new ImageIcon(getClass().getResource("/img/ghostac.jpg"));
			String newName = (String) JOptionPane.showInputDialog(null, "Insert the new name", 
							 "Ghost Rename", JOptionPane.QUESTION_MESSAGE, icon, null, null);
			IFile file = null;
			AST ast = null;
			ASTRewrite rewrite = null;
			IPath path = null;
			for (ISourceRef reference : selected.getDependencies()) {
				if (file == null) {
					file = reference.getFile();
					path = file.getFullPath();
					ast = reference.getNode().getAST();
					rewrite = ASTRewrite.create(ast);
				}
				else if(!file.equals(reference.getFile())) {
					dumpToFile(ast, rewrite, file, path, reference);
					file = reference.getFile();
					path = file.getFullPath();
					ast = reference.getNode().getAST();
					rewrite = ASTRewrite.create(ast);
				}
				if (reference.getNode() != null) {
					if (reference.getNode().getNodeType() == ASTNode.SIMPLE_TYPE) {
						SimpleType newType = ast.newSimpleType(ast.newSimpleName(newName));
						rewrite.replace((SimpleType) reference.getNode(), newType, null);
					}
					else if (reference.getNode().getNodeType() == ASTNode.SIMPLE_NAME) {
						if (((SimpleName) reference.getNode()).getIdentifier().equals(selected.getName())) {
							SimpleName nName = ast.newSimpleName(newName);
							rewrite.replace((SimpleName) reference.getNode(), nName, null);
						}
					}
				}
				if (reference.equals(selected.getDependencies().
						get(selected.getDependencies().size()-1))) {
					dumpToFile(ast, rewrite, file, path, reference);
				}
			}
			//notify file and references!!! simpler -> project
			return project;
		}
		
		public void dumpToFile(AST ast, ASTRewrite rewrite, IFile file, IPath path, ISourceRef reference) {
			ITextFileBufferManager bufferManager = FileBuffers
					.getTextFileBufferManager();
			try{
				bufferManager.connect(path, LocationKind.IFILE, null);
				ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
				IDocument document = textFileBuffer.getDocument();
				rewrite.rewriteAST(document, null).apply(document);
				textFileBuffer.commit(null, true);
				bufferManager.disconnect(path, LocationKind.IFILE, null);
			}catch (Exception e) { e.printStackTrace();}
		}

		public ImageDescriptor getImageDescriptor(){
			return ImageDescriptor.createFromImage(
							GViewLabelProvider.getImageNamed("ghost16_2.ico"));	
		}

		public void checkSelection(GTreeNode selected) {
			if(selected.isFree())	this.setEnabled(true);
			else					this.setEnabled(false);
		}
	}
	
	
	private TreeViewer viewer;
	private MenuManager menuMgr= new MenuManager();
	private Menu mainMenu;
	private GViewTreeContentProvider contentProvider=new GViewTreeContentProvider();
	
	
	private GhostBusterAction ghostBuster = new GhostBusterAction();
	private RenameAction rename = new RenameAction();
	
	private Action refresh=new Action("Refresh") {
		public ImageDescriptor getImageDescriptor(){
			return ImageDescriptor.createFromImage(
					 PlatformUI.getWorkbench().getSharedImages()
					 	.getImage(org.eclipse.ui.ISharedImages.IMG_ELCL_SYNCED));
		}
		public void run(){
			update(null, null);
		}
	};
	
	public static boolean refsInMenu=false;
	private Action toggleRefs=new Action("References in PopUp",SWT.TOGGLE){
		public ImageDescriptor getImageDescriptor(){
			return ImageDescriptor.createFromImage(
					 PlatformUI.getWorkbench().getSharedImages()
					 	.getImage(org.eclipse.ui.ide.IDE.SharedImages.IMG_OPEN_MARKER));
		}
		public void run(){
			refsInMenu=!refsInMenu;
			update(null,null);
		}
	};
	
	private GTreeNode getSingleSelection(ISelection _selection){
		IStructuredSelection selection = (IStructuredSelection) _selection; 
		if (selection.size() != 1)
			return null;
		return (GTreeNode) selection.getFirstElement();
	}
	
	@Override
	public void doubleClick(DoubleClickEvent event) {
		GTreeNode node = this.getSingleSelection(event.getSelection());
		if(node==null) return;
		
		if (node.getKind() != GTreeNode.DEPENDENCY){
			
			if(viewer.getExpandedState(node)) viewer.collapseToLevel(node, TreeViewer.ALL_LEVELS);
			else viewer.expandToLevel(node, 1);
		
			return;
		}
		new GotoMarker().setRef((ISourceRef) node.getValue()).run();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		this.viewer=new TreeViewer(parent,SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new GViewLabelProvider());
		viewer.setInput(GTreeNode.from(SGhostEngine.get().getProjects()));
		//viewer.addSelectionChangedListener((ISelectionChangedListener) this.ghostBuster);
		this.ghostBuster.setEnabled(false);
		viewer.addDoubleClickListener(this);
		
		mainMenu=menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(mainMenu);
		//mainMenu.setVisible(false);
		viewer.addSelectionChangedListener(new GPopUpMenu());
		
		this.createToolbar();
		
		SGhostEngine.get().addObserver(this);
		//viewer.expandToLevel(2);
		viewer.expandAll();
	}

	private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(ghostBuster);
        mgr.add(toggleRefs);
        mgr.add(refresh);
    }

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void update(Observable o, Object arg) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				viewer.setInput(GTreeNode.from(SGhostEngine.get().getProjects()));
				viewer.refresh();
				//viewer.expandToLevel(2);
				viewer.expandAll();	
				
				/*System.out.println("accept: "+
				PlatformUI.getPreferenceStore().contains(PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS)
					);
				
				System.out.println("temp_problem: "+
						PlatformUI.getPreferenceStore().getBoolean(
								PreferenceConstants.EDITOR_CORRECTION_INDICATION));
				*/
			}
		});
	}

	
	public void dispose(){
		SGhostEngine.get().deleteObserver(this);
		//viewer.removeSelectionChangedListener((ISelectionChangedListener) this.ghostBuster);
		super.dispose();
	}
}
