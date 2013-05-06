package cl.pleiad.ghosts;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import cl.pleiad.ghosts.blacklist.GBlackList;
import cl.pleiad.ghosts.engine.SGhostEngine;


/**
 * The activator class controls the plug-in life cycle
 */
public class GhostsPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "cl.pleiad.ghosts"; //$NON-NLS-1$

	// The shared instance
	private static GhostsPlugin plugin;
	
	/**
	 * The constructor
	 */
	public GhostsPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		//org.eclipse.jdt.ui.PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS
		SGhostEngine.get(); //initialize the engine and load ghosts!
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static GhostsPlugin getDefault() {
		return plugin;
	}

}
