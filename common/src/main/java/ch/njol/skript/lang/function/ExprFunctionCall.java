package ch.njol.skript.lang.function;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class ExprFunctionCall<T> extends SimpleExpression<T> implements KeyProviderExpression<T> {

	private final FunctionReference<?> reference;
	private final Class<? extends T>[] returnTypes;
	private final Class<T> returnType;
	private final boolean directReturn;
	private final Map<PlatformEvent, String[]> cache = Collections.synchronizedMap(new WeakHashMap<>());

	public ExprFunctionCall(FunctionReference<T> function) {
		this(function, CollectionUtils.array(function.getSignature().getReturnType().getC()));
	}

	@SuppressWarnings("unchecked")
	public ExprFunctionCall(FunctionReference<?> reference, Class<? extends T>[] expectedReturnTypes) {
		this.reference = reference;

		Class<?> functionReturnType = reference.getSignature().getReturnType().getC();
		Class<?> returnType = Utils.getComponentType(functionReturnType);
		if (CollectionUtils.containsSuperclass(expectedReturnTypes, returnType)) {
			// Function returns expected type already
			this.returnTypes = new Class[] {returnType};
			this.returnType = (Class<T>) returnType;
			this.directReturn = true;
		} else {
			// Return value needs to be converted
			this.returnTypes = expectedReturnTypes;
			this.returnType = (Class<T>) Utils.getSuperType(expectedReturnTypes);
			this.directReturn = false;
		}
	}

	@Override
	protected T @Nullable [] get(PlatformEvent event) {
		Object[] values;
		Object execute = reference.execute(event);
		if (execute == null) {
			values = null;
		} else if (!execute.getClass().isArray()) {
			values = new Object[] {execute};
		} else {
			values = (Object[]) execute;
		}

		String[] keys = reference.getFunction().returnedKeys();
		reference.getFunction().resetReturnValue();

		//noinspection unchecked
		T[] convertedValues = (T[]) Array.newInstance(returnType, values != null ? values.length : 0);
		if (values == null || values.length == 0) {
			cache.put(event, new String[0]);
			return convertedValues;
		}

		if (directReturn) {
			System.arraycopy(values, 0, convertedValues, 0, values.length);
		} else {
			Converters.convert(values, convertedValues, returnTypes);
		}
		if (keys != null) {
			for (int i = 0; i < convertedValues.length; i++) {
				if (convertedValues[i] == null)
					keys[i] = null;
			}
			convertedValues = ArrayUtils.removeAllOccurrences(convertedValues, null);
			cache.put(event, ArrayUtils.removeAllOccurrences(keys, null));
		} else {
			if (ArrayUtils.contains(convertedValues, null))
				convertedValues = ArrayUtils.removeAllOccurrences(convertedValues, null);
			cache.put(event, generateNumericalKeys(convertedValues.length));
		}
		return convertedValues;
	}

	@Override
	public @NotNull String @NotNull [] getArrayKeys(PlatformEvent event) throws IllegalStateException {
		if (!cache.containsKey(event))
			throw new IllegalStateException();
		return cache.remove(event);
	}

	@Override
	public boolean areKeysRecommended() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> @Nullable Expression<? extends R> getConvertedExpression(Class<R>... to) {
		if (CollectionUtils.containsSuperclass(to, getReturnType()))
			return (Expression<? extends R>) this;

		Class<?> returns = reference.getSignature().getReturnType().getC();
		Class<?> converterType = Utils.getComponentType(returns);

		if (Converters.converterExists(converterType, to))
			return new ExprFunctionCall<>(reference, to);
		return null;
	}

	@Override
	public boolean isSingle() {
		return reference.isSingle();
	}

	@Override
	public Class<? extends T> getReturnType() {
		return returnType;
	}

	@Override
	public Class<? extends T>[] possibleReturnTypes() {
		return Arrays.copyOf(returnTypes, returnTypes.length);
	}

	@Override
	public boolean isLoopOf(String input) {
		return KeyProviderExpression.super.isLoopOf(input);
	}

	@Override
	public String toString(@Nullable PlatformEvent event, boolean debug) {
		return reference.toString(event, debug);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		assert false;
		return false;
	}

	private static String[] generateNumericalKeys(int length) {
		String[] keys = new String[length];
		for (int i = 0; i < length; i++)
			keys[i] = String.valueOf(i);
		return keys;
	}

}