package cl.pleiad.ghosts.blacklist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;

import cl.pleiad.ghosts.GhostsPlugin;

/**
 * Class representing those classes to be excluded from Ghosts
 *
 */
public class GBlackList implements IResourceChangeListener{
	
	public final static String FILENAME = "ghosts.blacklist";
	
	protected static HashMap<IJavaProject, GBlackList> lists = new HashMap<IJavaProject, GBlackList>();
	
	/**
	 * Function that returns the blacklist of a given Java Project
	 * @param jproject the java project to be checked for banned classes
	 * @return a new GBlackList representing the banned classes from the project
	 */
	public static GBlackList from(IJavaProject jproject){
		GBlackList list = lists.get(jproject);
		if(list != null) return list;
		list = new GBlackList(jproject.getProject());
		lists.put(jproject, list);
		return list;
	}
	
	
	protected IFile file;
	protected ArrayList<String> blacks = new ArrayList<String>();

	/**
	 * It creates the listener, in the workspace, for the blacklist event
	 * @param parent the project, as a folder
	 * 
	 */	
	protected GBlackList(IProject parent){
		file = parent.getFile(FILENAME);
		if(file != null)
			if (file.exists()) load();
		create();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
		
	/**
	 * Given the blacklist file, it recreates the whole file, ignoring comments,
	 * based in the current blacklist
	 */	
	private void load() {
		BufferedReader in;
		blacks.clear();
		try {
			//TODO find out why is this necessary
			if (!file.exists())
				return;
			in = new BufferedReader(new InputStreamReader(file.getContents(true)));
			String inputLine;
	 
			while ((inputLine = in.readLine()) != null) 
				if(!inputLine.startsWith("#") && inputLine.length() >= 1)
					blacks.add(inputLine.trim());
			in.close();
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * Function to initialize (create) the blacklist file
	 */	
	private void create() {
		URL url;
		try {
			url = new URL("platform:/plugin/"+GhostsPlugin.PLUGIN_ID+"/resources/"+FILENAME);
			if(! file.exists() ) // why?
				file.create(url.openConnection().getInputStream(), true, null);
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * Function that manage the change of resource events.
	 * Given a change, if the change was done in the blacklist,
	 * it reloads the blacklist in the environment
	 * @param event the given resource change event
	 * 
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if(event.getType() != IResourceChangeEvent.POST_CHANGE) return;
		
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					
					//event.getDelta().getResource()
					if (resource != null && resource.getType() != IResource.FILE) return true;
					if(file.equals((IFile)resource)){
						file = (IFile)resource; //may be this is unnecessary
						load();
					}
					return true;
				}
			});
		} catch (CoreException e) {e.printStackTrace();}

	}

	/**
	 * Function that return if a certain package is added
	 * to the blacklist
	 * @param name the name of the package to be found
	 * @return if the given package name is in the blacklist
	 */
	public boolean contains(String name) {
		return this.blacks.contains(name);
	}
}
