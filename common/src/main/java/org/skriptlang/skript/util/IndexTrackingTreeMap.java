package org.skriptlang.skript.util;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

/**
 * A {@link TreeMap} that supports automatically assigning the next available
 * positive integer key, represented as a string.
 *
 * <p>In addition to arbitrary string keys, this map can be used with
 * positive integer string keys such as {@code "1"}, {@code "2"}, and
 * {@code "3"}. The {@link #add(Object)} method inserts a value using the
 * next available integer key.</p>
 *
 * @param <V> the type of mapped values
 */
public class IndexTrackingTreeMap<V> extends TreeMap<String, V> {

	private final Set<String> mapIndices = new HashSet<>();

	private @Nullable Set<Integer> numericalIndices;
	private boolean dense = true;
	private int numericCount = 0;
	private int nextIndex = 1;
	private int maxIndex = -1;

	public IndexTrackingTreeMap() {
		super();
	}

	public IndexTrackingTreeMap(Comparator<? super String> comparator) {
		super(comparator);
	}

	public static <V> @Nullable IndexTrackingTreeMap<V> buildSorted(Comparator<? super String> comparator,
																   String[] keys, V[] values, int count) {
		if (count <= 0)
			return null;
		for (int i = 0; i < count; i++) {
			if (keys[i] == null || values[i] == null)
				return null;
		}
		for (int i = 1; i < count; i++) {
			if (comparator.compare(keys[i - 1], keys[i]) >= 0)
				return null;
		}
		IndexTrackingTreeMap<V> map = new IndexTrackingTreeMap<>(comparator);
		map.putAll(new SortedEntries<>(comparator, keys, values, count));
		if (map.size() != count)
			return null;
		map.rebuildTracking(keys, values, count);
		return map;
	}

	private void rebuildTracking(String[] keys, Object[] values, int count) {
		mapIndices.clear();
		numericalIndices = null;
		numericCount = 0;
		maxIndex = -1;
		for (int i = 0; i < count; i++) {
			String key = keys[i];
			if (values[i] instanceof Map)
				mapIndices.add(key);
			int index = parsePositiveInt(key);
			if (index < 0)
				continue;
			numericCount++;
			if (index > maxIndex)
				maxIndex = index;
		}
		if (numericCount == maxIndex || (numericCount == 0 && maxIndex < 0)) {
			dense = true;
			nextIndex = maxIndex < 0 ? 1 : maxIndex + 1;
			return;
		}
		dense = false;
		Set<Integer> indices = sparseIndices();
		nextIndex = 1;
		while (indices.contains(nextIndex))
			nextIndex++;
	}

	private static final class SortedEntries<V> extends AbstractMap<String, V> implements SortedMap<String, V> {

		private final Comparator<? super String> comparator;
		private final String[] keys;
		private final V[] values;
		private final int count;

		SortedEntries(Comparator<? super String> comparator, String[] keys, V[] values, int count) {
			this.comparator = comparator;
			this.keys = keys;
			this.values = values;
			this.count = count;
		}

		@Override
		public int size() {
			return count;
		}

		@Override
		public Comparator<? super String> comparator() {
			return comparator;
		}

		@Override
		public Set<Entry<String, V>> entrySet() {
			return new AbstractSet<>() {
				@Override
				public Iterator<Entry<String, V>> iterator() {
					return new Iterator<>() {
						private int index;

						@Override
						public boolean hasNext() {
							return index < count;
						}

						@Override
						public Entry<String, V> next() {
							if (index >= count)
								throw new NoSuchElementException();
							int at = index++;
							return new SimpleImmutableEntry<>(keys[at], values[at]);
						}
					};
				}

				@Override
				public int size() {
					return count;
				}
			};
		}

		@Override
		public SortedMap<String, V> subMap(String from, String to) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SortedMap<String, V> headMap(String to) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SortedMap<String, V> tailMap(String from) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String firstKey() {
			return keys[0];
		}

		@Override
		public String lastKey() {
			return keys[count - 1];
		}

	}

	@Override
	public V put(String key, V value) {
		V previous = super.put(key, value);

		if (previous == null && value != null) {
			handleInsert(key, parsePositiveInt(key), value);
		} else if (previous != null && value == null) {
			handleRemove(key, previous);
		} else if (previous != null) {
			handleReplace(key, previous, value);
		}

		return previous;
	}

	/**
	 * Adds the given value under the first available positive integer key.
	 *
	 * @param value the value to add, cannot be null
	 */
	public void add(V value) {
		Preconditions.checkNotNull(value, "value");
		String key = String.valueOf(nextIndex);

		super.put(key, value);
		handleInsert(key, nextIndex, value);
	}

	@Override
	public V remove(Object key) {
		V value = super.remove(key);
		if (value != null && key instanceof String index)
			handleRemove(index, value);
		return value;
	}

	@Override
	public void clear() {
		super.clear();
		numericalIndices = null;
		dense = true;
		numericCount = 0;
		mapIndices.clear();
		nextIndex = 1;
		maxIndex = -1;
	}

	/**
	 * Finds the first available positive integer index that is not currently
	 * used as a key in this map.
	 *
	 * <p>This method inspects tracked numeric keys and returns the smallest
	 * missing index, starting at {@code 1}.</p>
	 *
	 * @return the next available positive integer index
	 */
	public int nextOpenIndex() {
		return nextIndex;
	}

	public boolean consecutive() {
		return nextIndex == maxIndex + 1;
	}

	/**
	 * Returns an unmodifiable view of the keys that map to other {@link Map} instances.
	 *
	 * @return a collection of all keys pointing to a map
	 */
	public @UnmodifiableView Collection<String> mapIndices() {
		return Collections.unmodifiableCollection(mapIndices);
	}

	public boolean hasMapIndices() {
		return !mapIndices.isEmpty();
	}

	private void handleInsert(String key, int index, V value) {
		if (value instanceof Map)
			mapIndices.add(key);

		if (index < 0)
			return;

		numericCount++;
		if (dense && index == (maxIndex < 0 ? 1 : maxIndex + 1)) {
			maxIndex = index;
			nextIndex = index + 1;
			return;
		}

		dense = false;
		maxIndex = Math.max(maxIndex, index);
		Set<Integer> indices = sparseIndices();
		indices.add(index);
		while (indices.contains(nextIndex))
			nextIndex++;
		compact();
	}

	private void handleReplace(String key, V previous, V value) {
		if (value instanceof Map) {
			mapIndices.add(key);
		} else if (previous instanceof Map) {
			mapIndices.remove(key);
		}
	}

	private void handleRemove(String key, V previous) {
		if (previous instanceof Map)
			mapIndices.remove(key);

		int index = parsePositiveInt(key);
		if (index < 0)
			return;

		numericCount--;

		if (dense && index == maxIndex) {
			maxIndex = numericCount == 0 ? -1 : numericCount;
			nextIndex = numericCount + 1;
			return;
		}

		if (dense) {
			dense = false;
			sparseIndices();
		} else if (numericalIndices != null) {
			numericalIndices.remove(index);
		}

		if (index == maxIndex)
			recomputeMaxIndex();
		nextIndex = Math.max(1, Math.min(nextIndex, index));
		compact();
	}

	private void compact() {
		if (numericCount != maxIndex)
			return;
		dense = true;
		numericalIndices = null;
		nextIndex = maxIndex + 1;
	}

	private void recomputeMaxIndex() {
		Set<Integer> indices = sparseIndices();
		while (maxIndex >= 0 && !indices.contains(maxIndex))
			maxIndex--;
	}

	private Set<Integer> sparseIndices() {
		Set<Integer> indices = numericalIndices;
		if (indices == null) {
			indices = new HashSet<>();
			for (String key : keySet()) {
				int index = parsePositiveInt(key);
				if (index >= 0)
					indices.add(index);
			}
			numericalIndices = indices;
		}
		return indices;
	}

	private int parsePositiveInt(String string) {
		if (string == null || string.isBlank() || string.charAt(0) == '0') // Don't handle leading-zero integers
			return -1;

		int value = 0;
		try {
			for (int i = 0; i < string.length(); i++) {
				char c = string.charAt(i);
				if (!isDigit(c))
					return -1;
				value = Math.addExact(value * 10, c - '0');
			}
		} catch (ArithmeticException e) { // overflow
			return -1;
		}

		return value;
	}

	private boolean isDigit(int codepoint) {
		return codepoint >= '0' && codepoint <= '9';
	}

}