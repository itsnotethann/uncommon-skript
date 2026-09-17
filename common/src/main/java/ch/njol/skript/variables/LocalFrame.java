package ch.njol.skript.variables;

import org.jetbrains.annotations.Nullable;

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
			int index = table.indexOf(name);
			if (index >= 0) {
				Object[] values = this.values;
				return values == null || index >= values.length ? null : values[index];
			}
		}
		VariablesMap map = this.map;
		return map == null ? null : map.getVariable(name);
	}

	void setByName(String name, @Nullable Object value) {
		LocalSlots table = this.table;
		if (table != null) {
			int index = table.indexOf(name);
			if (index >= 0) {
				slots(index, table.size())[index] = value;
				return;
			}
		}
		map().setVariable(name, value);
	}

	@Nullable Object getSlot(LocalSlots table, int index) {
		if (this.table != table)
			return getByName(table.name(index));
		Object[] values = this.values;
		return values == null || index >= values.length ? null : values[index];
	}

	void setSlot(LocalSlots table, int index, @Nullable Object value) {
		if (this.table != table) {
			setByName(table.name(index), value);
			return;
		}
		slots(index, table.size())[index] = value;
	}

	VariablesMap materialize() {
		VariablesMap map = map();
		LocalSlots table = this.table;
		Object[] values = this.values;
		if (table != null && values != null) {
			for (int index = 0; index < values.length; index++) {
				Object value = values[index];
				if (value != null)
					map.setVariable(table.name(index), value);
			}
		}
		this.table = null;
		this.values = null;
		return map;
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
