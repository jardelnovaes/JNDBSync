package com.jardelnovaes.utils.database.neodbsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseConfiguration {
	private static final String CONFIG_KEY = "%s.db.%s";
	
	@Setter(value=AccessLevel.PROTECTED)
	private DatabaseConfigurationType dbId;
	private String url;
	private String dialect; 
	private String driver; 
	private String user;
	private String password;
	
	@Getter(value=AccessLevel.PROTECTED)
	@Setter(value=AccessLevel.NONE)
	private Properties props;
	
	public DatabaseConfiguration(final DatabaseConfigurationType dbId){
		this.dbId = dbId;
		loadConfig();
	}

	private void loadConfig() {
		final String group = dbId.name().toLowerCase();		
		props = new Properties();		
		try {
			@Cleanup
			final InputStream input = new FileInputStream(getFile("config.properties"));			
			props.load(input);
			
		} catch (Exception e) {			
			e.printStackTrace();
		}
		
		url = getConfigValue(group, "url");
		dialect = getConfigValue(group, "dialect");
		driver = getConfigValue(group, "driver");
		user = getConfigValue(group, "user");
		password = getConfigValue(group, "password");
	}
	
	private String getConfigValue(final String group, final String key){
		return props.getProperty(String.format(CONFIG_KEY, group, key));
	}
	
	private File getFile(String fileName) {		
		ClassLoader classLoader = getClass().getClassLoader();		
		return new File(classLoader.getResource(fileName).getFile());
	}
	
	@Override
	public String toString() {
		return "{ DBType: " + dbId.name() + ", Dialect: " + dialect + ", Driver: " + driver + ", User: " + user + ", URL: "+ url + "}";
	}
}
