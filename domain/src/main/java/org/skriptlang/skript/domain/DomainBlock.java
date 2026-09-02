package org.skriptlang.skript.domain;

import java.util.Map;

public interface DomainBlock {
	Map<String, String> properties();

	DomainBlock withProperty(String key, String value);

	DomainBlock withProperties(Map<String, String> properties);

	String state();
}
