package org.skriptlang.skript.domain;

import java.util.List;

public interface PermissionSubject extends Permissible {
	String primaryGroup();

	List<String> groups();

	String prefix();

	String suffix();

	void setPrimaryGroup(String groupName);

	void addGroups(String... groupNames);

	void removeGroups(String... groupNames);
}
