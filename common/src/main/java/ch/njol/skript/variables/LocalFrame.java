package ch.njol.skript.variables;

import ch.njol.skript.lang.Variable;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.util.IndexTrackingTreeMap;

import java.util.Map;
import java.util.TreeMap;

final class LocalFrame {

	private @Nullable LocalSlots table;
	private Object @Nullable [] values;
	private @Nullable VariablesMap map;

	LocalFrame(@Nullable LocalSlots table) {
		this.table = table;
	}

	LocalFrame(VariablesMap map) {
		this.map = map;
	}

	@Nullable Object getByName(String name) {
		LocalSlots table = this.table;
		if (table != null) {
			int separator = name.indexOf(Variable.SEPARATOR);
			if (separator < 0) {
				int index = table.indexOf(name);
				if (index >= 0)
					return valueAt(index);
			} else {
				int index = table.indexOfList(name.substring(0, separator));
				if (index >= 0) {
					Object node = valueAt(index);
					return node == null ? null : descend(asNode(node), name, separator + 2);
				}
			}
		}
		VariablesMap map = this.map;
		return map == null ? null : map.getVariable(name);
	}

	void setByName(String name, @Nullable Object value) {
		LocalSlots table = this.table;
		if (table != null) {
			int separator = name.indexOf(Variable.SEPARATOR);
			if (separator < 0) {
				int index = table.indexOf(name);
				if (index >= 0) {
					slots(index, table.size())[index] = value;
					return;
				}
			} else {
				int index = table.indexOfList(name.substring(0, separator));
				if (index >= 0) {
					store(table, index, name, separator + 2, value);
					return;
				}
			}
		}
		map().setVariable(name, value);
	}

	@Nullable Object getSlot(LocalSlots table, int index, @Nullable String path) {
		if (this.table != table)
			return getByName(name(table, index, path));
		Object stored = valueAt(index);
		if (path == null || stored == null)
			return path == null ? stored : null;
		return descend(asNode(stored), path, 0);
	}

	void setSlot(LocalSlots table, int index, @Nullable String path, @Nullable Object value) {
		if (this.table != table) {
			setByName(name(table, index, path), value);
			return;
		}
		if (path == null) {
			slots(index, table.size())[index] = value;
			return;
		}
		store(table, index, path, 0, value);
	}

	boolean setSlotList(LocalSlots table, int index, String[] keys, Object[] values, int count) {
		if (this.table != table)
			return false;
		IndexTrackingTreeMap<Object> node = IndexTrackingTreeMap.buildSorted(
			VariablesMap.VARIABLE_NAME_COMPARATOR, keys, values, count);
		if (node == null)
			return false;
		slots(index, table.size())[index] = node;
		return true;
	}

	VariablesMap materialize() {
		VariablesMap map = map();
		LocalSlots table = this.table;
		Object[] values = this.values;
		if (table != null && values != null) {
			for (int index = 0; index < values.length; index++) {
				Object value = values[index];
				if (value == null)
					continue;
				if (table.isList(index))
					flatten(map, table.name(index), asNode(value));
				else
					map.setVariable(table.name(index), value);
			}
		}
		this.table = null;
		this.values = null;
		return map;
	}

	private void store(LocalSlots table, int index, String path, int from, @Nullable Object value) {
		if (value == null && isStar(path, from)) {
			slots(index, table.size())[index] = null;
			return;
		}
		Object stored = valueAt(index);
		if (stored == null) {
			if (value == null)
				return;
			stored = newNode();
			slots(index, table.size())[index] = stored;
		}
		insert(asNode(stored), path, from, value);
	}

	private static void insert(Map<String, Object> node, String path, int from, @Nullable Object value) {
		while (true) {
			int next = path.indexOf(Variable.SEPARATOR, from);
			String key = next < 0 ? path.substring(from) : path.substring(from, next);

			if (next < 0) {
				if (value == null) {
					Object existing = node.get(key);
					if (existing instanceof TreeMap) {
						//noinspection unchecked
						((Map<String, Object>) existing).remove(null);
					} else {
						node.remove(key);
					}
					return;
				}
				Object previous = node.put(key, value);
				if (previous instanceof TreeMap) {
					//noinspection unchecked
					Map<String, Object> childNode = (Map<String, Object>) previous;
					childNode.put(null, value);
					node.put(key, childNode);
				}
				return;
			}

			Object child = node.get(key);

			if (child instanceof TreeMap) {
				//noinspection unchecked
				Map<String, Object> childNode = (Map<String, Object>) child;
				if (isStar(path, next + 2)) {
					Object own = childNode.get(null);
					if (own == null)
						node.remove(key);
					else
						node.put(key, own);
					return;
				}
				node = childNode;
			} else if (value == null) {
				return;
			} else {
				Map<String, Object> childNode = newNode();
				if (child != null)
					childNode.put(null, child);
				node.put(key, childNode);
				node = childNode;
			}
			from = next + 2;
		}
	}

	private static @Nullable Object descend(Map<String, Object> node, String path, int from) {
		while (true) {
			int next = path.indexOf(Variable.SEPARATOR, from);
			if (next < 0 && isStar(path, from))
				return node;
			String key = next < 0 ? path.substring(from) : path.substring(from, next);
			Object child = node.get(key);
			if (child == null)
				return null;
			if (next < 0)
				return child instanceof TreeMap ? ((TreeMap<?, ?>) child).get(null) : child;
			if (!(child instanceof TreeMap))
				return null;
			//noinspection unchecked
			node = (Map<String, Object>) child;
			from = next + 2;
		}
	}

	private static void flatten(VariablesMap map, String prefix, Map<String, Object> node) {
		for (Map.Entry<String, Object> entry : node.entrySet()) {
			String key = entry.getKey();
			String name = key == null ? prefix : prefix + Variable.SEPARATOR + key;
			Object value = entry.getValue();
			if (value instanceof TreeMap) {
				//noinspection unchecked
				flatten(map, name, (Map<String, Object>) value);
			} else {
				map.setVariable(name, value);
			}
		}
	}

	private static boolean isStar(String path, int from) {
		return path.length() == from + 1 && path.charAt(from) == '*';
	}

	private static String name(LocalSlots table, int index, @Nullable String path) {
		String root = table.name(index);
		return path == null ? root : root + Variable.SEPARATOR + path;
	}

	private static Map<String, Object> newNode() {
		return new IndexTrackingTreeMap<>(VariablesMap.VARIABLE_NAME_COMPARATOR);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asNode(Object value) {
		return (Map<String, Object>) value;
	}

	private @Nullable Object valueAt(int index) {
		Object[] values = this.values;
		return values == null || index >= values.length ? null : values[index];
	}

	private VariablesMap map() {
		VariablesMap map = this.map;
		if (map == null)
			this.map = map = new VariablesMap();
		return map;
	}

	private Object[] slots(int index, int size) {
		Object[] values = this.values;
		if (values == null) {
			this.values = values = new Object[Math.max(index + 1, size)];
		} else if (index >= values.length) {
			Object[] grown = new Object[Math.max(index + 1, size)];
			System.arraycopy(values, 0, grown, 0, values.length);
			this.values = values = grown;
		}
		return values;
	}

}
