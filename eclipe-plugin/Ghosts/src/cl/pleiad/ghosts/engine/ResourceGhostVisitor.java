package cl.pleiad.ghosts.engine;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;

public class ResourceGhostVisitor implements IResourceDeltaVisitor {
	
	private GhostLoadListener listener;
	
	public ResourceGhostVisitor(GhostLoadListener listener) {
		super();
		this.listener = listener;
	}
    
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
        switch (delta.getKind()) {
	        case IResourceDelta.ADDED :
	        	/*
	        	 * The project cannot be used right away, so the strategy here 
	        	 * is to wait for the src folder to be initialized
	        	 */
	        	if (delta.getResource().getType() == IResource.FOLDER &&
	        		delta.getResource().getName().equals("src")) {
	        		//System.out.println("added: src :D");
	        		Job job = new LoadProjectJob((IProject) delta.getResource().getProject());
					job.setPriority(Job.INTERACTIVE);
					job.schedule();
					listener.notifyChangesToObservers();
	        	}        		
	            break;
	        case IResourceDelta.REMOVED :
	        	if (delta.getResource().getType() == IResource.PROJECT) {
	        		String projectName = delta.getResource().getName();
	    			SGhostEngine.get().removeProjectByName(projectName);
	    			//System.out.println("removed: "+projectName);
	        	}       		
	            break;    
	        case IResourceDelta.CHANGED :
	        	if (delta.getResource().getType() == IResource.ROOT)
	    			for (IResourceDelta projectDelta : delta.getAffectedChildren(IResource.PROJECT))
	    				if (projectDelta.getFlags() == IResourceDelta.OPEN) {
	    					//System.out.println("open: "+projectDelta.getResource().getName());
	    					Job job = new LoadProjectJob((IProject) projectDelta.getResource());
	    					job.setPriority(Job.INTERACTIVE);
	    					job.schedule();
	    					listener.notifyChangesToObservers(); //In the job will be a slightly faster
	    				}
	            break;
        }
    return true;
    }
	
	protected class LoadProjectJob extends Job {

		private IProject project;

		protected LoadProjectJob(IProject _pjt) {
			super("Loading Project: " + _pjt.getName());
			this.project = _pjt;
		}

		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (project.isOpen()) {
					if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
						// project.set
						SGhostEngine.get().loadGhostsFrom(JavaCore.create(project));				
					}
				}
				else {
					SGhostEngine.get().removeProject(JavaCore.create(project));
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
			return Status.OK_STATUS;
		}

	}
}
