package com.jardelnovaes.utils.database.neodbsync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Entity {
	private String qualifiedName;
	private String simpleName;
	private Class entityClass;
	
	public String getQuery() {
		return "from " + simpleName;
	}
}
