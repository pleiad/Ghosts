Ghosts
======

Development of an Eclipse plugin that supports incremental programming with ghosts.

See http://pleiad.cl/ghosts.

The latest version of Ghost os available [here](https://github.com/pleiad/Ghosts/raw/master/plugins/cl.pleiad.ghosts_1.0.0.201305151521.jar).

Ghosts is available as beta version for Eclipse. In order to try Ghosts, you will need the standard 
Eclipse IDE for Java Developers version 3.7.1 or higher. We recommend:

  Eclipse for Windows    [32-bit](http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-jee-indigo-SR2-win32.zip) [64-bit](http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-jee-indigo-SR2-win32-x86_64.zip)	
  
  Eclipse for Mac Cocoa	 [32-bit](http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-jee-indigo-SR2-macosx-cocoa.tar.gz) [64-bit](http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-jee-indigo-SR2-macosx-cocoa-x86_64.tar.gz)
  
  Eclipse for Linux      [32-bit](http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-jee-indigo-SR2-linux-gtk.tar.gz) [64-bit](http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/indigo/SR2/eclipse-jee-indigo-SR2-linux-gtk-x86_64.tar.gz) 
  

Just download it, and copy the jar file to your dropins/ folder inside eclipse:

> $ECLIPSE_DIR/dropins/

For a better experience with Ghosts, please follow these steps:

1.  Activate the Ghosts perspective in Window>>Open perspective>>Other>>Ghosts
2.  Disable the preference “Report problems as you type” (see example) in Preferences>>Java>>Editor>>Report problems as you type
3.  If ghosts are not reified properly, close-&-reopen the project.

To uninstall Ghosts, just remove the jar file in the dropins/ folder and run (the first time you open Eclispe):

> $ECLIPSE_DIR/eclipse -clean
