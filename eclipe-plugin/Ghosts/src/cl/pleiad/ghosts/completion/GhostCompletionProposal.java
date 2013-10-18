package cl.pleiad.ghosts.completion;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import cl.pleiad.ghosts.GhostsPlugin;
import cl.pleiad.ghosts.core.GMember;
import cl.pleiad.ghosts.core.GMethod;
import cl.pleiad.ghosts.core.Ghost;

public class GhostCompletionProposal implements IJavaCompletionProposal {
	
	private GMember member;
	private int fOffset;
	private int fLength;
	
	public GhostCompletionProposal(GMember g, int offset, int length) {
		member = g;
		fOffset = offset - length;
		fLength = length;
	}
	
	@Override
	public void apply(IDocument document) {
		try {
			String replacement = getCompletition();
			document.replace(fOffset, fLength, replacement);
		} catch (BadLocationException x) {
			x.printStackTrace();
		}
		
	}

	private String getCompletition() {
		if (member.kind() == Ghost.FIELD)
			return member.getName();
		else
			return ((GMethod) member).toCompletitionString();
	}
	
	@Override
	public String getAdditionalProposalInfo() {
		return member.toString();
	}

	@Override
	public IContextInformation getContextInformation() {
		// TODO wtf is this for?
		return null;
	}

	@Override
	public String getDisplayString() {
		return member.toSimpleString();
	}

	@Override
	public Image getImage() {
		InputStream inputStream;
		try {
			inputStream = FileLocator
					.openStream(GhostsPlugin.getDefault().getBundle()
					, new Path("img/ghostac.jpg"),
					false);
			return new Image(Display.getDefault(), inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Point getSelection(IDocument document) {
		// TODO Understand why is this needed
		return null;
	}

	@Override
	public int getRelevance() {
		return 1000;
	}

}
