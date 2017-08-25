package com.jardelnovaes.utils.database.neodbsync;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JNDBSync 
{
	private final DatabaseConfigurations dbconfigs;
	
	public JNDBSync() {
		dbconfigs = new DatabaseConfigurations();
		log.debug(dbconfigs.toString());
	}
	
    public static void main( String[] args )
    {
        log.info("Starting DBSync");
        JNDBSync dbSync = new JNDBSync();
        
        
        
        try {
			//loadFromJar("D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/sped-model-1.60.0-SNAPSHOT.jar", null, dbconfigs);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        dbSync.loadJars();
        dbSync.processScanClasses();
        dbSync.processScanPackages();
        
        
        /*
        for(Class<?> clazz : ClassFinder.find("com.neogrid.sped.neodbsync")) {
    		System.out.println("Class: " + clazz.getCanonicalName());
    	}
        */
        
        /*
        for(String packageName : dbconfigs.getTargetDatabase().getScanPackages()){
        	for(Class<?> clazz :ClassFinder.find(packageName)) {
        		System.out.println("Class: " + clazz.getCanonicalName());
        	}
        } 
        */       
        
        dbSync.close();        
        log.info("DBSync has finished its work");
        System.exit(0);
        
        
    }

	public void processScanClasses() {
		for(String className : dbconfigs.getScanClasses()) {
        	try {
        		//TODO Make a loop to get the jars
        		loadFromJar("D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/ng-repository-api-0.9.2-nodeps.jar", className, dbconfigs);
        		loadFromJar("D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/sped-arch-1.60.0-SNAPSHOT.jar", className, dbconfigs);
    			loadFromJar("D:/Neogrid/workspaces/tempWork/NeoDBSync/libs/sped-model-1.60.0-SNAPSHOT.jar", className, dbconfigs);
    		} catch (Exception e) {
    			log.error("Can't scan the classes", e);
    		}
    	}
	}
	
	public void processScanPackages() {
		//TODO Not Implemented
	}
	
	public void loadJars() {
		//TODO Thinking how to load jar dependencies.
	}
    
	public void close() {
		dbconfigs.openSessions();
        dbconfigs.synchronize();        
        dbconfigs.shutdown();
	}
	
    private static void loadFromJar(final String pathToJar, final String classNameLookedFor, final DatabaseConfigurations dbconfigs) 
    		throws Exception {
    	final JarFile jarFile = new JarFile(pathToJar);
    	final Enumeration<JarEntry> e = jarFile.entries();

    	final URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
    	final URLClassLoader cl = URLClassLoader.newInstance(urls);

    	dbconfigs.getEntityJars().add(new File(pathToJar));
    	
    	log.debug("Looking for: " + classNameLookedFor);
    	while (e.hasMoreElements()) {
    	    JarEntry je = e.nextElement();
    	    if(je.isDirectory() || !je.getName().endsWith(".class")){
    	        continue;
    	    }
    	    // -6 because of .class
    	    String classNameFound = je.getName().substring(0,je.getName().length()-6);    	    
    	    classNameFound = classNameFound.replace('/', '.');
    	    log.trace("Class found: " + classNameFound);
    	    Class clazz;
    	    if(classNameLookedFor == null) {
    	    	clazz = cl.loadClass(classNameFound);
    	    	dbconfigs.addEntity(classNameFound, clazz.getSimpleName(), clazz);
    	    } else {
    	    	if(classNameLookedFor.equals(classNameFound)) {
    	    		clazz = cl.loadClass(classNameFound);
    	    		log.trace("Class matched!");
    	    		dbconfigs.addEntity(classNameFound, clazz.getSimpleName(), clazz);
    	    	}
    	    }
    	}
    }
    
    
}
