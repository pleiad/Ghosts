package cl.pleiad.ghosts.engine;

import java.util.Observable;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;


public class GhostListener extends Observable implements IElementChangedListener, IResourceChangeListener {
	
	public GhostListener() {		
		this.registerListeners();
	}

	protected void registerListeners() {
	}
	
	@Override
	public void elementChanged(ElementChangedEvent event) {	
	}

	protected void notifyChangesToObservers() {
		super.setChanged();
		super.notifyObservers();
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
	}   
}
