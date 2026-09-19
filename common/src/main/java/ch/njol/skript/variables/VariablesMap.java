package ch.njol.skript.variables;

import ch.njol.skript.lang.Variable;
import ch.njol.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.util.IndexTrackingTreeMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A map for storing variables in a sorted and efficient manner.
 */
final class VariablesMap {

	/**
	 * The comparator for comparing variable names.
	 */
	static final Comparator<String> VARIABLE_NAME_COMPARATOR = (s1, s2) -> {
		if (s1 == null)
			return s2 == null ? 0 : -1;

		if (s2 == null)
			return 1;

		int len1 = s1.length();
		int len2 = s2.length();

		// Fast path: assume both strings are pure positive integers without leading zeros.
		// This is the dominant case for list indices (e.g. {list::1} through {list::1000}).
		// For these, numeric order == length order, then lexicographic order.
		// This does cause an extra partial loop over non-integer strings, but ints are much more common and it'll fail-fast.
		char firstChar1 = len1 > 0 ? s1.charAt(0) : 0;
		char firstChar2 = len2 > 0 ? s2.charAt(0) : 0;
		if (firstChar1 >= '1' && firstChar1 <= '9' && firstChar2 >= '1' && firstChar2 <= '9') {
			int i = 1;
			// Check if the rest of the characters are digits as well
			while (i < len1 && isDigit(s1.charAt(i))) {
				i++;
			}
			if (i == len1) { // all of s1 are digits
				i = 1;
				// Check if the rest of the characters are digits as well
				while (i < len2 && isDigit(s2.charAt(i))) {
					i++;
				}
				if (i == len2) { // all of s2 are digits
					if (len1 != len2)
						return len1 - len2;
					return s1.compareTo(s2);
				}
			}
		}

		int i = 0;
		int j = 0;

		boolean lastNumberNegative = false;
		boolean afterDecimalPoint = false;
		while (i < len1 && j < len2) {
			char c1 = s1.charAt(i);
			char c2 = s2.charAt(j);

			if (isDigit(c1) && isDigit(c2)) {
				// Numbers/digits are treated differently from other characters.

				// The index after the last digit
				int i2 = StringUtils.findLastDigit(s1, i);
				int j2 = StringUtils.findLastDigit(s2, j);

				// Amount of leading zeroes
				int z1 = 0;
				int z2 = 0;

				// Skip leading zeroes (except for the last if all 0's)
				if (!afterDecimalPoint) {
					if (c1 == '0') {
						while (i < i2 - 1 && s1.charAt(i) == '0') {
							i++;
							z1++;
						}
					}
					if (c2 == '0') {
						while (j < j2 - 1 && s2.charAt(j) == '0') {
							j++;
							z2++;
						}
					}
				}
				// Keep in mind that c1 and c2 may not have the right value (e.g. s1.charAt(i)) for the rest of this block

				// If the number is prefixed by a '-', it should be treated as negative, thus inverting the order.
				// If the previous number was negative, and the only thing separating them was a '.',
				//  then this number should also be in inverted order.
				boolean previousNegative = lastNumberNegative;

				// i - z1 contains the first digit, so i - z1 - 1 may contain a `-` indicating this number is negative
				lastNumberNegative = i - z1 > 0 && s1.charAt(i - z1 - 1) == '-';
				int isPositive = (lastNumberNegative | previousNegative) ? -1 : 1;

				// Different length numbers (99 > 9)
				if (!afterDecimalPoint && i2 - i != j2 - j)
					return ((i2 - i) - (j2 - j)) * isPositive;

				// Iterate over the digits
				while (i < i2 && j < j2) {
					char d1 = s1.charAt(i);
					char d2 = s2.charAt(j);

					// If the digits differ, return a value dependent on the sign
					if (d1 != d2)
						return (d1 - d2) * isPositive;

					i++;
					j++;
				}

				// Different length numbers (1.99 > 1.9)
				if (afterDecimalPoint && i2 - i != j2 - j)
					return ((i2 - i) - (j2 - j)) * isPositive;

				// If the numbers are equal, but either has leading zeroes,
				//  more leading zeroes is a bigger number (01 > 1)
				// The original intention was for them to be smaller, but the author wrote it wrong so they're bigger :/
				// Best to just keep it like that than to change it and break existing variable orderings
				if (z1 != z2)
					return (z1 - z2) * isPositive;

				afterDecimalPoint = true;
			} else {
				// Normal characters
				if (c1 != c2)
					return c1 - c2;

				// Reset the last number flags if we're exiting a number.
				if (c1 != '.') {
					lastNumberNegative = false;
					afterDecimalPoint = false;
				}

				i++;
				j++;
			}
		}
		if (i < len1)
			return lastNumberNegative ? -1 : 1;
		if (j < len2)
			return lastNumberNegative ? 1 : -1;
		return 0;
	};

	private static boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}

	/**
	 * The map that stores all non-list variables.
	 */
	final HashMap<String, Object> hashMap = new HashMap<>();
	/**
	 * The tree of variables, branched by the list structure of the variables.
	 */
	final TreeMap<String, Object> treeMap = new TreeMap<>();

	private @Nullable LocalSlots slotTable;
	private Object @Nullable [] slotValues;

	VariablesMap() {
	}

	VariablesMap(@Nullable LocalSlots slotTable) {
		this.slotTable = slotTable;
	}

	/**
	 * Returns the internal value of the requested variable.
	 * <p>
	 * <b>Do not modify the returned value!</b>
	 *
	 * @param name the name of the variable, possibly a list variable.
	 * @return an {@link Object} for a normal variable or a
	 * {@code Map<String, Object>} for a list variable,
	 * or {@code null} if the variable is not set.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	Object getVariable(String name) {
		LocalSlots slotTable = this.slotTable;
		if (slotTable != null) {
			int separator = name.indexOf(Variable.SEPARATOR);
			if (separator < 0) {
				int index = slotTable.indexOf(name);
				if (index >= 0)
					return slotAt(index);
			} else {
				int index = slotTable.indexOfList(name.substring(0, separator));
				if (index >= 0) {
					Object node = slotAt(index);
					return node == null ? null : descend(asNode(node), name, separator + Variable.SEPARATOR.length());
				}
			}
		}
		if (!name.endsWith("*")) {
			// Not a list variable, quick access from the hash map
			return hashMap.get(name);
		} else {
			// List variable, search the tree branches
			String[] split = Variables.splitVariableName(name);
			Map<String, Object> parent = treeMap;

			// Iterate over the parts of the variable name
			for (int i = 0; i < split.length; i++) {
				String n = split[i];
				if (n.equals("*")) {
					// End of variable name, return map
					assert i == split.length - 1;
					return parent;
				}

				// Check if the current (sub-)tree has the expected child node
				Object childNode = parent.get(n);
				if (childNode == null)
					return null;

				// Continue the iteration if the child node is a tree itself
				if (childNode instanceof Map) {
					// Continue iterating with the subtree
					parent = (Map<String, Object>) childNode;
					assert i != split.length - 1;
				} else {
					// ..., otherwise the list variable doesn't exist here
					return null;
				}
			}
			return null;
		}
	}

	/**
	 * Sets the given variable to the given value.
	 * <p>
	 * This method accepts list variables,
	 * but these may only be set to {@code null}.
	 *
	 * @param name the variable name.
	 * @param value the variable value, {@code null} to delete the variable.
	 */
	@SuppressWarnings("unchecked")
	void setVariable(String name, @Nullable Object value) {
		LocalSlots slotTable = this.slotTable;
		if (slotTable != null) {
			int separator = name.indexOf(Variable.SEPARATOR);
			if (separator < 0) {
				int index = slotTable.indexOf(name);
				if (index >= 0) {
					slots(index, slotTable.size())[index] = value;
					return;
				}
			} else {
				int index = slotTable.indexOfList(name.substring(0, separator));
				if (index >= 0) {
					store(slotTable, index, name, separator + Variable.SEPARATOR.length(), value);
					return;
				}
			}
		}
		// First update the hash map easily
		if (!name.endsWith("*")) {
			if (value == null)
				hashMap.remove(name);
			else
				hashMap.put(name, value);
		}

		// Then update the tree map by going down the branches
		String[] split = Variables.splitVariableName(name);
		TreeMap<String, Object> parent = treeMap;

		// Iterate over the parts of the variable name
		for (int i = 0; i < split.length; i++) {
			String childNodeName = split[i];
			Object childNode = parent.get(childNodeName);

			if (childNode == null) {
				// Expected child node not found
				if (i == split.length - 1) {
					// End of the variable name reached, set variable if needed
					if (value != null)
						parent.put(childNodeName, value);

					break;
				} else if (value != null) {
					// Create child node, add it to parent and continue iteration
					childNode = new IndexTrackingTreeMap<>(VARIABLE_NAME_COMPARATOR);

					parent.put(childNodeName, childNode);
					parent = (TreeMap<String, Object>) childNode;
				} else {
					// Want to set variable to null, bu variable is already null
					break;
				}
			} else if (childNode instanceof TreeMap) {
				// Child node found
				TreeMap<String, Object> childNodeMap = ((TreeMap<String, Object>) childNode);

				if (i == split.length - 1) {
					// End of variable name reached, adjust child node accordingly
					if (value == null)
						childNodeMap.remove(null);
					else
						childNodeMap.put(null, value);

					break;
				} else if (i == split.length - 2 && split[i + 1].equals("*")) {
					// Second to last part of variable name
					assert value == null;

					// Delete all indices of the list variable from hashMap
					deleteFromHashMap(StringUtils.join(split, Variable.SEPARATOR, 0, i + 1), childNodeMap);

					// If the list variable itself has a value ,
					//  e.g. list `{mylist::3}` while variable `{mylist}` also has a value,
					//  then adjust the parent for that
					Object currentChildValue = childNodeMap.get(null);
					if (currentChildValue == null)
						parent.remove(childNodeName);
					else
						parent.put(childNodeName, currentChildValue);

					break;
				} else {
					// Continue iteration
					parent = childNodeMap;
				}
			} else {
				// Ran into leaf node
				if (i == split.length - 1) {
					// If we arrived at the end of the variable name, update parent
					if (value == null)
						parent.remove(childNodeName);
					else
						parent.put(childNodeName, value);

					break;
				} else if (value != null) {
					// Need to continue iteration, create new child node and put old value in it
					TreeMap<String, Object> newChildNodeMap = new IndexTrackingTreeMap<>(VARIABLE_NAME_COMPARATOR);
					newChildNodeMap.put(null, childNode);

					// Add new child node to parent
					parent.put(childNodeName, newChildNodeMap);
					parent = newChildNodeMap;
				} else {
					break;
				}
			}
		}
	}

	/**
	 * Deletes all indices of a list variable from the {@link #hashMap}.
	 *
	 * @param parent the list variable prefix,
	 *                  e.g. {@code list} for {@code list::*}.
	 * @param current the map of the list variable.
	 */
	@SuppressWarnings("unchecked")
	void deleteFromHashMap(String parent, TreeMap<String, Object> current) {
		for (Entry<String, Object> e : current.entrySet()) {
			if (e.getKey() == null)
				continue;
			String childName = parent + Variable.SEPARATOR + e.getKey();

			// Remove from hashMap
			hashMap.remove(childName);

			// Recurse if needed
			Object val = e.getValue();
			if (val instanceof TreeMap) {
				deleteFromHashMap(childName, (TreeMap<String, Object>) val);
			}
		}
	}

	/**
	 * Creates a copy of this map.
	 *
	 * @return the copy.
	 */
	public VariablesMap copy() {
		VariablesMap copy = new VariablesMap();

		copy.hashMap.putAll(hashMap);

		TreeMap<String, Object> treeMapCopy = copyTreeMap(treeMap);
		copy.treeMap.putAll(treeMapCopy);

		LocalSlots table = this.slotTable;
		Object[] values = this.slotValues;
		if (table != null && values != null)
			drain(copy, table, values);

		return copy;
	}

	VariablesMap materialize() {
		LocalSlots table = this.slotTable;
		Object[] values = this.slotValues;
		this.slotTable = null;
		this.slotValues = null;
		if (table != null && values != null)
			drain(this, table, values);
		return this;
	}

	@Nullable Object getSlot(LocalSlots table, int index, @Nullable String path) {
		if (this.slotTable != table)
			return getVariable(slotName(table, index, path));
		Object stored = slotAt(index);
		if (path == null || stored == null)
			return path == null ? stored : null;
		return descend(asNode(stored), path, 0);
	}

	void setSlot(LocalSlots table, int index, @Nullable String path, @Nullable Object value) {
		if (this.slotTable != table) {
			setVariable(slotName(table, index, path), value);
			return;
		}
		if (path == null) {
			slots(index, table.size())[index] = value;
			return;
		}
		store(table, index, path, 0, value);
	}

	private static void drain(VariablesMap target, LocalSlots table, Object[] values) {
		for (int index = 0; index < values.length; index++) {
			Object value = values[index];
			if (value == null)
				continue;
			if (table.isList(index))
				flattenSlots(target, table.name(index), asNode(value));
			else
				target.setVariable(table.name(index), value);
		}
	}

	private static void flattenSlots(VariablesMap target, String prefix, Map<String, Object> node) {
		for (Entry<String, Object> entry : node.entrySet()) {
			String key = entry.getKey();
			String name = key == null ? prefix : prefix + Variable.SEPARATOR + key;
			Object value = entry.getValue();
			if (value instanceof TreeMap) {
				flattenSlots(target, name, asNode(value));
			} else {
				target.setVariable(name, value);
			}
		}
	}

	private void store(LocalSlots table, int index, String path, int from, @Nullable Object value) {
		if (value == null && isStar(path, from)) {
			slots(index, table.size())[index] = null;
			return;
		}
		Object stored = slotAt(index);
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
			Object child = node.get(key);

			if (next < 0) {
				if (child instanceof TreeMap) {
					Map<String, Object> childNode = asNode(child);
					if (value == null)
						childNode.remove(null);
					else
						childNode.put(null, value);
				} else if (value == null) {
					node.remove(key);
				} else {
					node.put(key, value);
				}
				return;
			}

			if (child instanceof TreeMap) {
				Map<String, Object> childNode = asNode(child);
				if (isStar(path, next + Variable.SEPARATOR.length())) {
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
			from = next + Variable.SEPARATOR.length();
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
			node = asNode(child);
			from = next + Variable.SEPARATOR.length();
		}
	}

	private static boolean isStar(String path, int from) {
		return path.length() == from + 1 && path.charAt(from) == '*';
	}

	private static String slotName(LocalSlots table, int index, @Nullable String path) {
		String root = table.name(index);
		return path == null ? root : root + Variable.SEPARATOR + path;
	}

	private static Map<String, Object> newNode() {
		return new IndexTrackingTreeMap<>(VARIABLE_NAME_COMPARATOR);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asNode(Object value) {
		return (Map<String, Object>) value;
	}

	private @Nullable Object slotAt(int index) {
		Object[] values = this.slotValues;
		return values == null || index >= values.length ? null : values[index];
	}

	private Object[] slots(int index, int size) {
		Object[] values = this.slotValues;
		if (values == null) {
			this.slotValues = values = new Object[Math.max(index + 1, size)];
		} else if (index >= values.length) {
			Object[] grown = new Object[Math.max(index + 1, size)];
			System.arraycopy(values, 0, grown, 0, values.length);
			this.slotValues = values = grown;
		}
		return values;
	}

	/**
	 * Makes a deep copy of the given {@link TreeMap}.
	 * <p>
	 * The 'deep copy' means that each subtree of the given tree is copied
	 * as well.
	 *
	 * @param original the original tree map.
	 * @return the copy.
	 */
	@SuppressWarnings("unchecked")
	private static TreeMap<String, Object> copyTreeMap(TreeMap<String, Object> original) {
		TreeMap<String, Object> copy = new IndexTrackingTreeMap<>(VARIABLE_NAME_COMPARATOR);

		for (Entry<String, Object> child : original.entrySet()) {
			String key = child.getKey();
			Object value = child.getValue();

			// Copy by recursion if the child is a TreeMap
			if (value instanceof TreeMap) {
				value = copyTreeMap((TreeMap<String, Object>) value);
			}

			copy.put(key, value);
		}

		return copy;
	}

}