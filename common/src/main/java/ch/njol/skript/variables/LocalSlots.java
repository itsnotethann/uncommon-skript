package ch.njol.skript.variables;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public final class LocalSlots {

	private final Map<String, Integer> indices = new HashMap<>();
	private final Map<String, Integer> listIndices = new HashMap<>();
	private String[] names = new String[8];
	private boolean[] lists = new boolean[8];
	private int size;

	public int reserve(String name) {
		return reserve(indices, name, false);
	}

	public int reserveList(String root) {
		return reserve(listIndices, root, true);
	}

	private int reserve(Map<String, Integer> target, String name, boolean list) {
		Integer existing = target.get(name);
		if (existing != null)
			return existing;
		if (size == names.length) {
			String[] grownNames = new String[size * 2];
			System.arraycopy(names, 0, grownNames, 0, size);
			names = grownNames;
			boolean[] grownLists = new boolean[size * 2];
			System.arraycopy(lists, 0, grownLists, 0, size);
			lists = grownLists;
		}
		int index = size++;
		names[index] = name;
		lists[index] = list;
		target.put(name, index);
		return index;
	}

	public int indexOf(String name) {
		Integer index = indices.get(name);
		return index == null ? -1 : index;
	}

	public int indexOfList(String root) {
		Integer index = listIndices.get(root);
		return index == null ? -1 : index;
	}

	public int size() {
		return size;
	}

	public String name(int index) {
		return names[index];
	}

	public boolean isList(int index) {
		return lists[index];
	}

}
