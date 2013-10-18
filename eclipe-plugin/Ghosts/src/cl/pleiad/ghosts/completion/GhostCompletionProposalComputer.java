package cl.pleiad.ghosts.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import cl.pleiad.ghosts.core.GBehaviorType;
import cl.pleiad.ghosts.core.GClass;
import cl.pleiad.ghosts.core.GExtendedClass;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.Ghost;
import cl.pleiad.ghosts.dependencies.GhostSet;
import cl.pleiad.ghosts.engine.SGhostEngine;

public class GhostCompletionProposalComputer implements IJavaCompletionProposalComputer {
	
	@Override
	public List<ICompletionProposal> computeCompletionProposals(
			ContentAssistInvocationContext context, IProgressMonitor pm) {
		if(context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext jcontext = (JavaContentAssistInvocationContext) context;
			String project = jcontext.getProject().getElementName();
			ArrayList<String> prefixes = new ArrayList<String>();
			ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
			if(jcontext.getExpectedType() == null) {//Ghost Classes
				try {
					boolean method = isMethod(jcontext);
					boolean incomplete = isIncomplete(jcontext);
					prefixes = getPrefix(jcontext.getViewer(),jcontext.getInvocationOffset());
					prefixes.remove("");
					if (prefixes.isEmpty())
						prefixes.add("");
					Collections.reverse(prefixes);
					String prefix = prefixes.get(0);
					String declaration = getDeclaration(jcontext.getViewer(), 
							jcontext.getInvocationOffset() - prefix.length() - 1, prefix);
					if (prefixes.size() == 1) {
						if (declaration == null)
							list = getSpecialProposals(jcontext.getCompilationUnit(), prefix, project, 
									jcontext.getInvocationOffset(), method, incomplete);
						else
							list = getProposals(project, declaration, jcontext.getInvocationOffset());
					}
					else {
						list = getChainedProposals(jcontext.getCompilationUnit(), prefixes, 
								declaration, project, jcontext.getInvocationOffset(), method, incomplete);
					}
					return list;
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			else if (hasProblems(project)) { //doesn't seems to be necessary
				try {
					boolean method = isMethod(jcontext);
					boolean incomplete = isIncomplete(jcontext);
					prefixes = getPrefix(jcontext.getViewer(),jcontext.getInvocationOffset());
					prefixes.remove("");
					if (prefixes.isEmpty())
						return list;
					Collections.reverse(prefixes);
					String prefix = prefixes.get(0);
					if (prefixes.size() == 1) {
						list = getSpecialProposals(jcontext.getCompilationUnit(), prefix, project, 
								jcontext.getInvocationOffset(), method, incomplete);
					}
					else {
						list = getChainedProposals(jcontext.getCompilationUnit(), prefixes, 
								null, project, jcontext.getInvocationOffset(), method, incomplete);
					}
					return list;
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return Collections.emptyList();
	}

	private boolean isMethod(JavaContentAssistInvocationContext jcontext)
			throws BadLocationException {
		return jcontext.getViewer().getDocument().getChar(jcontext.getInvocationOffset() - 2)
				 == ')';
	}
	
	private boolean isIncomplete(JavaContentAssistInvocationContext jcontext)
			throws BadLocationException {
		return jcontext.getViewer().getDocument().getChar(jcontext.getInvocationOffset() - 1)
				 != '.';
	}

	@Override
	public List<IContextInformation> computeContextInformation(
			ContentAssistInvocationContext context, IProgressMonitor pm) {
		// TODO don't know what to do here
		return Collections.emptyList();
	}

	@Override
	public String getErrorMessage() {
		return "I dont know what happened :S";
	}

	@Override
	public void sessionEnded() {
		//to check end conditions
	}

	@Override
	public void sessionStarted() {
		//to check start conditions
	}
	
	private ArrayList<String> getPrefix(ITextViewer viewer, int offset) throws BadLocationException {
		ArrayList<String> prefixes = new ArrayList<String>();
		IDocument doc = viewer.getDocument();
		if (doc == null || offset > doc.getLength())
			return null;
		int length = 0;
		--offset;
		while (offset >= 0 && (Character.isJavaIdentifierPart(doc.getChar(offset)) ||
				doc.getChar(offset) == ')' || doc.getChar(offset) == '.')) {	
			while (offset >= 0 && (Character.isJavaIdentifierPart(doc.getChar(offset)) ||
					doc.getChar(offset) == ')')) {
				if (doc.getChar(offset) == ')') {
					length--;
					for (int i = offset; i > 0; i--) {
						if (doc.getChar(i) == '(')
							i = 0;
						length++;
						offset--;
					}
					offset--;
				}
				length++;
				offset--;
			}
			prefixes.add(doc.get(offset + 1, length));
			if (doc.getChar(offset) == '.') {
				length = 0;
				offset--;
			}
		}
		return prefixes;
	}
	
	private String getDeclaredType(ITextViewer viewer, int offset) throws BadLocationException {
		IDocument doc = viewer.getDocument();
		if (doc == null || offset > doc.getLength())
			return null;
		int length = 0;
		--offset;
		while (--offset >= 0 && (Character.isJavaIdentifierPart(doc.getChar(offset)) ||
				doc.getChar(offset) == ' ')) {
			length++;
		}
		return doc.get(offset + 1, length).trim();
	}
	
	private String getDeclaration(ITextViewer viewer, int offset, String prefix) throws BadLocationException {
		if (prefix == null || prefix.equals("") || prefix.equals("this") || prefix.equals("super"))
			return null;
		IDocument doc = viewer.getDocument();
		if (doc == null || offset > doc.getLength())
			return null;
		int start = 0;
		int end = 0;
		char pivot = prefix.charAt(0);
		ArrayList<Integer> options = new ArrayList<Integer>();
		ArrayList<String> optionsN = new ArrayList<String>();
		for (int i = 0; i < offset; i++) {
			if (Character.isJavaIdentifierPart(doc.getChar(i)) && i != 0) {
				if (!Character.isJavaIdentifierPart(doc.getChar(i-1)) 
					&& doc.getChar(i) == pivot) {
					start = i;
					end = i;
				}
			}
			if (end == (i - 1) && prefix.length() == 1 && doc.getChar(i - 1) == pivot 
				&& !Character.isJavaIdentifierPart(doc.getChar(i)))
				options.add(start);
			else if (end == (i - 1) &&  (end - start + 1) < prefix.length() 
					&& doc.getChar(i) == prefix.charAt((end - start + 1))) {
				end++;
				if ((end - start + 1) == prefix.length())
					options.add(start);
			}
		}
		for (int s : options) {
			for (int i = s; i > 0; i--) {
				if (Character.isJavaIdentifierPart(doc.getChar(i)) &&
					!Character.isJavaIdentifierPart(doc.getChar(i - 1))) {
					if (doc.getChar(i - 1) != '.')
						optionsN.add(getDeclaredType(viewer,i));
					i = 0;
				}
			}
		}
		Collections.reverse(optionsN);
		for (String res : optionsN)
			if(!res.equals("") && res != null)
				return res;
		return null;
	}
	
	private ArrayList<ICompletionProposal> getProposals(String project, String declaration, int offset) {
		ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
		Ghost ghost = null;
		for (GhostSet proj : SGhostEngine.get().getProjects()) {
			if (proj.getProject().getElementName().equals(project))
				for (Ghost g : proj.getGhosts())
					if (g.getName().equals(declaration))
						ghost = g;
		}
		if (ghost != null) {
			GClass gClass = ((GBehaviorType) ghost).asClass();
			for (GMember member : gClass.getMembers()) {
				if (!member.isDeclared)
					list.add(new GhostCompletionProposal(member, offset, 0));
			}
		}
		return list;
	}
	
	private ArrayList<ICompletionProposal> getSpecialProposals(ICompilationUnit iCU, 
			String prefix, String project, int offset, boolean isMethod, boolean isIncomplete) {
		ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
		Ghost ghost = null;
		String name = iCU.getElementName();
		name = name.substring(0, name.lastIndexOf('.'));
		if (prefix.equals("super")) {
			int i = 0;
			for (GhostSet proj : SGhostEngine.get().getProjects()) {
				if (i == 1)
					break;
				if (proj.getProject().getElementName().equals(project))
					for (Ghost g : proj.getGhosts()) {
						if (g.kind() == Ghost.CLASS) {
							GExtendedClass gClass = ((GBehaviorType) g).asClass().asExtendedClass();
							if (gClass.getExtenders() != null) {
								for (String gname : gClass.getExtenders()) {
									if(name.equals(gname)) {
										ghost = gClass;
										i = 1;
										break;
									}
								}
							}
						}
				}
			}
			if (ghost != null) {
				GClass gClass = ((GBehaviorType) ghost).asClass();
				for (GMember member : gClass.getMembers()) {
					if (!member.isDeclared)
						list.add(new GhostCompletionProposal(member, offset, 0));
				}
			}
		}
		else if(prefix.equals("this")) {
			int i = 0;
			for (GhostSet proj : SGhostEngine.get().getProjects()) {
				if (i == 1)
					break;
				if (proj.getProject().getElementName().equals(project)) {
					for (Ghost g : proj.getGhosts()) {
						if (g.kind() == Ghost.CONSTRUCTOR ||
							g.kind() == Ghost.FIELD ||
							g.kind() == Ghost.METHOD) {
							if(name.equals(((GMember) g).getOwnerType().getName())) {
								if (!((GMember) g).isDeclared)
									list.add(new GhostCompletionProposal((GMember) g, offset, 0));
							}
						}
					}
					i = 1;
				}
			}			
		}
		else {
			int i = 0;
			for (GhostSet proj : SGhostEngine.get().getProjects()) {
				if (i == 1)
					break;
				if (proj.getProject().getElementName().equals(project)) {
					String type = "";
					if (isIncomplete) {
						for (Ghost g : proj.getGhosts()) {
							if (g.kind() == Ghost.CONSTRUCTOR ||
								g.kind() == Ghost.FIELD ||
								g.kind() == Ghost.METHOD) {
								if (g.getName().startsWith(prefix) && !((GMember) g).isDeclared)
									list.add(new GhostCompletionProposal((GMember) g, offset, prefix.length()));
							}
						}
						return list;
					}
					for (Ghost g : proj.getGhosts()) {
						if (g.kind() == Ghost.CONSTRUCTOR ||
							g.kind() == Ghost.FIELD ||
							g.kind() == Ghost.METHOD) {
							if(name.equals(((GMember) g).getOwnerType().getName()) &&
							   prefix.equals(((GMember) g).getName())) { //what to do if not a Ghost?
								if (g.kind() != Ghost.FIELD && !isMethod) {
									i = 1;
								}
								else {
									type = ((GMember) g).getReturnType().getName();
									i = 1;
								}
							}
						}
					}
					for (Ghost g : proj.getGhosts()) {
						if (g.kind() == Ghost.CLASS &&
							g.getName().equals(type)) {
							for (GMember member : ((GClass) g).getMembers()) {
								if (!member.isDeclared)
									list.add(new GhostCompletionProposal(member, offset, 0));
							}
						}
					}
				}
			}			
		}
		return list;
	}
	
	private ArrayList<ICompletionProposal> getChainedProposals(
			ICompilationUnit iCU, ArrayList<String> prefixes,
			String declaration, String project, int invocationOffset,
			boolean method, boolean incomplete) {
		ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
		GClass ghost = null;
		GhostSet cProject = null;
		int index = 0;
		String type = null;
		for (GhostSet proj : SGhostEngine.get().getProjects()) {
			if (proj.getProject().getElementName().equals(project))
				cProject = proj;
		}
		if (prefixes.isEmpty())
			return null;
		else if (prefixes.get(0).equals("super")) {
			String firstType = iCU.getElementName();
			firstType = firstType.substring(0, firstType.lastIndexOf('.'));
			for (Ghost g : cProject.getGhosts()) {
				if (g.kind() == Ghost.CLASS) {
					GExtendedClass gClass = ((GBehaviorType) g).asClass().asExtendedClass();
					if (gClass.getExtenders() != null) {
						for (String gname : gClass.getExtenders()) {
							if(firstType.equals(gname)) {
								for (GMember m : gClass.getMembers()) {
									if (m.getName().equals(prefixes.get(1))) {
										index = 2;
										type = m.getReturnType().getName();
										break;	
									}
								}
								if (incomplete && type  == null) {
									type = gClass.getName();
									index = 1;
									break;
								}
							}
						}
					}
				}
			}
		}
		else {
			int i = 0;
			if (prefixes.get(0).equals("this"))
				i = 1;
			String firstType = iCU.getElementName();
			firstType = firstType.substring(0, firstType.lastIndexOf('.'));
			for (Ghost g : cProject.getGhosts()) {
				if (g.kind() == Ghost.CONSTRUCTOR ||
					g.kind() == Ghost.FIELD ||
					g.kind() == Ghost.METHOD) {
					if(firstType.equals(((GMember)g).getOwnerType().getName()) 
					   && prefixes.get(i).equals(g.getName())) {
						if (prefixes.get(0).equals("this"))
							index = 2;
						else
							index = 1;
						type = ((GMember)g).getReturnType().getName();
						break;
					}
				}
			}
			if (incomplete && type  == null)
				type = firstType;
			
		}
		ghost = getLatestPrefix(type, index, method, incomplete, prefixes, cProject.getGhosts());
		for (GMember member : ghost.getMembers()) {
			if (incomplete) {
				if (member.getName().startsWith(prefixes.get(prefixes.size()-1)) && !member.isDeclared)
					list.add(new GhostCompletionProposal(member, invocationOffset, prefixes.get(prefixes.size()-1).length()));
			}
			else if (!member.isDeclared)
				list.add(new GhostCompletionProposal(member, invocationOffset, 0));
		}
		if (ghost.getName().equals("") && incomplete && prefixes.get(0).equals("this"))
			for (Ghost g : cProject.getGhosts()) {
				if (g.kind() == Ghost.CONSTRUCTOR ||
						g.kind() == Ghost.FIELD ||
						g.kind() == Ghost.METHOD) {
					if (g.getName().startsWith(prefixes.get(1)) && !((GMember) g).isDeclared)
						list.add(new GhostCompletionProposal((GMember) g, invocationOffset, prefixes.get(1).length()));
				}
			}
		return list;
	}
	
	private GClass getLatestPrefix(String type, int index, boolean method, boolean incomplete,
			ArrayList<String> prefixes, CopyOnWriteArrayList<Ghost> ghosts) {
		for (Ghost g : ghosts) {
			if (g.kind() == Ghost.CLASS) {
				if (incomplete && index == prefixes.size() - 1 && g.getName().equals(type))
					return (GClass) g;
				if (g.getName().equals(type)) {
					if (index >= prefixes.size())
						return (GClass) g;
					else {
						for (GMember member : ((GClass) g).getMembers())
							if(member.getName().equals(prefixes.get(index))) {
								if (!method && member.kind() != Ghost.FIELD &&
									index == prefixes.size() - 1)
									return new GClass("");
								return getLatestPrefix(member.getReturnType().getName(), 
										index+1, method, incomplete, prefixes, ghosts);
							}
					}
				}
			}
		}
		return new GClass("");
	}
	
	private boolean hasProblems(String project) {
		for (GhostSet proj : SGhostEngine.get().getProjects()) {
			if (proj.getProject().getElementName().equals(project)
				&& proj.hasProblems())
				return true;
		}
		return false;
	}

}
