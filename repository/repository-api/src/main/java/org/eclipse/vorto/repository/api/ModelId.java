/**
 * Copyright (c) 2015-2016 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.repository.api;

import java.util.regex.Pattern;

import org.eclipse.vorto.repository.api.content.IReferenceType;

/**
 * @author Alexander Edelmann - Robert Bosch (SEA) Pte. Ltd.
 */
public class ModelId implements IReferenceType {
	private String name;
	private String namespace;
	private String version;
	
	private static final Pattern ID_PATTERN = Pattern.compile("([\\w\\.]+).([\\w\\.]+)*.([A-Z][\\w\\.]+):([\\w\\.]+)");
	
	public ModelId() {
	}
	
	public ModelId(String name, String namespace, String version) {
		super();
		this.name = name;
		this.namespace = namespace.toLowerCase();
		this.version = version;
	}
	
	public static ModelId fromReference(String qualifiedName, String version) {
		String name = qualifiedName.substring(qualifiedName.lastIndexOf(".")+1);
		String namespace = qualifiedName.substring(0,qualifiedName.lastIndexOf("."));
		return new ModelId(name,namespace,version);
	}
	
	public static ModelId fromPrettyFormat(String prettyFormat) {
		if (!ID_PATTERN.matcher(prettyFormat).matches()) {
			throw new IllegalArgumentException("Model ID must match pattern <namespace>.<name>:<version>");
		}
		final int versionIndex = prettyFormat.indexOf(":");
		return ModelId.fromReference(prettyFormat.substring(0,versionIndex),prettyFormat.substring(versionIndex+1));
	}
	

	public static ModelId newVersion(ModelId id, String newVersion) {
		return ModelId.fromPrettyFormat(id.getNamespace()+"."+id.getName()+":"+newVersion);
	}
					
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	

	@Override
	public String toString() {
		return "ModelId [name=" + name + ", namespace=" + namespace + ", version=" + version + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelId other = (ModelId) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public String getPrettyFormat() {	
		return namespace + "." + name + ":" +version;
	}
}