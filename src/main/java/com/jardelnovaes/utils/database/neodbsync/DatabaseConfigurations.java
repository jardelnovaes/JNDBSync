package com.jardelnovaes.utils.database.neodbsync;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.query.Query;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class DatabaseConfigurations {	
	private static final String PROP_EXCLUDE_PACKAGES = "exclude.packages";
	private static final String PRO_SCAN_PACKAGES = "scan.packages";
	private static final String PRO_SCAN_CLASSES = "scan.classes";
	
	private Set<String> scanClasses;
	
	private Set<String> scanPackages;
	
	private Set<String> excludePackages;
	
	private final Set<Entity> entities;
	
	private DatabaseConfiguration sourceDatabase;
	private DatabaseConfiguration targetDatabase;
	
	private Session sourceSession;
	private Session targetSession;	
	private Set<File> entityJars;
	
	public DatabaseConfigurations() {
		sourceDatabase = new DatabaseConfiguration(DatabaseConfigurationType.SOURCE);
		targetDatabase = new DatabaseConfiguration(DatabaseConfigurationType.TARGET);
		entityJars = new LinkedHashSet<File>();
		loadProps();
		entities = new LinkedHashSet<Entity>();
	}
	
	
	public void openSessions(){		
		sourceSession = buildSessionFactory(getConfiguration(sourceDatabase)).openSession();
		targetSession = buildSessionFactory(getConfiguration(targetDatabase)).openSession();		
	}
	
	@Override
	public String toString() {		
		return sourceDatabase.toString() + " - " + targetDatabase.toString();
	}
	
	private void loadProps() {
		scanClasses = getPropValues(PRO_SCAN_CLASSES);
		if(!scanClasses.isEmpty()) {
			scanPackages = getPropValues(PRO_SCAN_PACKAGES);
			excludePackages = getPropValues(PROP_EXCLUDE_PACKAGES);
		}
		
	}
	
	private Set<String> getPropValues(final String key) {
		final String value = (String)sourceDatabase.getProps().get(key);
		return (value != null) ? new LinkedHashSet<String>(Arrays.asList(value.split(","))) : new LinkedHashSet<String>();	
	}
	
	public void addEntity(final String qualifiedName, final String simpleName, final Class clazz) {
		entities.add(new Entity(qualifiedName, simpleName, clazz));
	}
	
	private Configuration getConfiguration(final DatabaseConfiguration dbConfig){
		//final Configuration configuration = new Configuration().configure();		
		final Configuration configuration = new Configuration()				
				.setProperty("hibernate.connection.driver_class", dbConfig.getDriver())
				.setProperty("hibernate.connection.url", dbConfig.getUrl())
				.setProperty("hibernate.connection.username", dbConfig.getUser())
				.setProperty("hibernate.connection.password", dbConfig.getPassword())
				.setProperty("hibernate.dialect", dbConfig.getDialect())
				.setProperty("hibernate.show_sql", String.valueOf(log.isDebugEnabled()))
				.setProperty("hibernate.format_sql", String.valueOf(log.isDebugEnabled()))
				.setProperty("hibernate.hbm2ddl.auto", "validate")
				.setProperty("hibernate.archive.autodetection", "hbm") //"class,hbm" 
				.setProperty("exclude-unlisted-classes", "false");
		//hibernate.cfg.xml
		//TODO Use it if will implement a option to load by Annotated Class
		for(Entity entity : getEntities()) {
				configuration.addAnnotatedClass(entity.getEntityClass());
				//configuration.addClass(entity.getEntityClass());
			
		}
		
		// .setProperty("hibernate.archive.autodetection", "class,hbm") 
		
		/*
		for(File jar : entityJars) {			
			configuration.addJar(jar);			
		}
		*/
    	return configuration;
	}
	
	private SessionFactory buildSessionFactory(final Configuration configuration) {
        try {
        	StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().
        	applySettings(configuration.getProperties());
            return configuration.buildSessionFactory(builder.build());
        } catch (Throwable ex) {
            log.error("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
  
    public void shutdown() {    	
    	sourceSession.close();
    	targetSession.close();
    }
    
    private Object getFieldValue(final Entity entity, final String fieldName, final Object item) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    	final Field field = entity.getEntityClass().getDeclaredField(fieldName);
    	if (field != null) {
    		field.setAccessible(true);
    		return field.get(item);
    	}
    	return "";
    }
    
    private void updateObject(final Object sourceItem, Object targetItem) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
    	for (Field sourceField : sourceItem.getClass().getDeclaredFields()) {
    		log.debug("Field name: " +  sourceField.getName() + " - Is Static: " + Modifier.isStatic(sourceField.getModifiers()));
    		if(!Modifier.isStatic(sourceField.getModifiers())){
    			Field targetField = targetItem.getClass().getDeclaredField(sourceField.getName());
    			targetField.setAccessible(true);
    			sourceField.setAccessible(true);
    			targetField.set(targetItem, sourceField.get(sourceItem));    			
    		}
    	}
	}
    
    private String getPrimaryKeyFieldName(final ClassMetadata entityMetadata, final Entity entity) {    	
    	return entityMetadata.getIdentifierPropertyName();
    }
    
    private Object getPrimaryKeyFieldValue(final ClassMetadata entityMetadata, final Object item) {    	
    	return entityMetadata.getIdentifier(item);	
    }     
    
    public void synchronize() {
    	for(Entity entity : getEntities()) { 
    		Query query = sourceSession.createQuery(entity.getQuery());
    		
    		//TODO Make a logic to query N by N rows (avoiding tables with a lot of data)
    		final ClassMetadata entityMetadata =  sourceSession.getSessionFactory().getClassMetadata(entity.getEntityClass());
    		final String pkName = getPrimaryKeyFieldName(entityMetadata, entity);
    		final String hql = entity.getQuery() + " where " + pkName + " = :id";
    		final List<?> items  = query.getResultList();    		
    		
    		for(Object item: items) {
    			try {
    				targetSession.beginTransaction();
    				final List<?> existedItem = getItemInTheTarget(entity, entityMetadata, hql, item);
    				save(item, existedItem);
				} catch (Exception e) {
					log.error("Error trying to save " + entity.getQualifiedName() + " into the target", e);
					targetSession.getTransaction().rollback();
				}
    			
    			
    		}
    	}
    }


	private List<?> getItemInTheTarget(final Entity entity, final ClassMetadata entityMetadata, final String hql,
			Object item) {
		final Object idValue = getPrimaryKeyFieldValue(entityMetadata, item);    				
		log.debug("Entity: " + entity.getQualifiedName() + "- id: " + idValue);
		Query alreadyExists = targetSession.createQuery(hql)
				.setParameter("id", idValue);
		
		return alreadyExists.getResultList();
	}

	private void save(final Object item, final List<?> existedItem) throws IllegalAccessException, NoSuchFieldException {
		if(existedItem != null && !existedItem.isEmpty()){
			updateObject(item, existedItem.get(0));
			targetSession.save(existedItem.get(0));
		} else {
			targetSession.save(item);
		}
		targetSession.getTransaction().commit();
	}
}



