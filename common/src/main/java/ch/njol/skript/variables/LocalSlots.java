package ch.njol.skript.variables;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public final class LocalSlots {

	private final Map<String, Integer> indices = new HashMap<>();
	private String[] names = new String[8];
	private int size;

	public int reserve(String name) {
		Integer existing = indices.get(name);
		if (existing != null)
			return existing;
		if (size == names.length) {
			String[] grown = new String[size * 2];
			System.arraycopy(names, 0, grown, 0, size);
			names = grown;
		}
		int index = size++;
		names[index] = name;
		indices.put(name, index);
		return index;
	}

	public int indexOf(String name) {
		Integer index = indices.get(name);
		return index == null ? -1 : index;
	}

	public int size() {
		return size;
	}

	public String name(int index) {
		return names[index];
	}

}
