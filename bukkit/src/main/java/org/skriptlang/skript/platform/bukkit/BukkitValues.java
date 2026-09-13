package org.skriptlang.skript.platform.bukkit;

import java.lang.reflect.Array;
import java.util.List;
import java.util.ServiceLoader;

import org.jetbrains.annotations.Nullable;

public final class BukkitValues {

	private static volatile @Nullable List<BukkitTypeBridge> bridges;
	private static volatile boolean typesRegistered;

	private BukkitValues() {}

	static List<BukkitTypeBridge> bridges() {
		List<BukkitTypeBridge> current = bridges;
		if (current == null) {
			synchronized (BukkitValues.class) {
				current = bridges;
				if (current == null) {
					bridges = current = ServiceLoader.load(BukkitTypeBridge.class, BukkitValues.class.getClassLoader())
						.stream()
						.map(ServiceLoader.Provider::get)
						.toList();
				}
			}
		}
		return current;
	}

	static synchronized void registerSkriptTypes() {
		if (typesRegistered)
			return;
		typesRegistered = true;
		for (BukkitTypeBridge bridge : bridges())
			bridge.registerSkriptTypes();
	}

	public static @Nullable Object adapt(@Nullable Object value, Class<?> type) {
		if (value == null || type.isInstance(value))
			return value;
		if (type.isArray()) {
			if (!(value instanceof Object[] array))
				return value;
			Class<?> component = type.getComponentType();
			Object[] adapted = (Object[]) Array.newInstance(component, array.length);
			for (int i = 0; i < array.length; i++) {
				Object element = adapt(array[i], component);
				if (element != null && !component.isInstance(element))
					return value;
				adapted[i] = element;
			}
			return adapted;
		}
		for (BukkitTypeBridge bridge : bridges()) {
			Object converted = bridge.toBukkit(value, type);
			if (converted != null && type.isInstance(converted))
				return converted;
		}
		return value;
	}

	public static boolean isInstance(@Nullable Object value, Class<?> type) {
		return value != null && type.isInstance(adapt(value, type));
	}
}
