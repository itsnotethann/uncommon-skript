/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.NonNullIterator;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Represents a literal, i.e. a static value like a number or a string.
 *
 * @see UnparsedLiteral
 */
public class SimpleLiteral<T> implements Literal<T>, DefaultExpression<T> {

	protected final Class<T> type;

	private final boolean isDefault;
	private final boolean and;

	protected final Expression<?> source;

	/**
	 * The data of the literal. May not be null or contain null, but may be empty.
	 */
	protected transient T[] data;

	public SimpleLiteral(T[] data, Class<T> type, boolean and) {
		this(data, type, and, null);
	}

	public SimpleLiteral(T[] data, Class<T> type, boolean and, @Nullable Expression<?> source) {
		this(data, type, and, false, source);
	}

	public SimpleLiteral(T[] data, Class<T> type, boolean and, boolean isDefault, @Nullable Expression<?> source) {
		assert data != null;
		assert type != null;
		this.data = data;
		this.type = type;
		this.and = data.length <= 1 || and;
		this.isDefault = isDefault;
		this.source = source == null ? this : source;
	}

	public SimpleLiteral(T data, boolean isDefault) {
		this(data, isDefault, null);
	}

	public SimpleLiteral(T data, boolean isDefault, @Nullable Expression<?> source) {
		assert data != null;
		//noinspection unchecked
		this.data = (T[]) Array.newInstance(data.getClass(), 1);
		this.data[0] = data;
		//noinspection unchecked
		type = (Class<T>) data.getClass();
		and = true;
		this.isDefault = isDefault;
		this.source = source == null ? this : source;
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean init() {
		return true;
	}

	private T[] data() {
		return Arrays.copyOf(data, data.length);
	}

	@Override
	public T[] getArray() {
		return this.data();
	}

	@Override
	public T[] getArray(PlatformEvent event) {
		return this.data();
	}

	@Override
	public T[] getAll() {
		return this.data();
	}

	@Override
	public T[] getAll(PlatformEvent event) {
		return this.data();
	}

	@Override
	public T getSingle() {
		return CollectionUtils.getRandom(data);
	}

	@Override
	public T getSingle(PlatformEvent event) {
		return getSingle();
	}

	@Override
	public Class<T> getReturnType() {
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> @Nullable Literal<? extends R> getConvertedExpression(Class<R>... to) {
		if (CollectionUtils.containsSuperclass(to, type))
			return (Literal<? extends R>) this;
		R[] parsedData = Converters.convert(this.data(), to, (Class<R>) Utils.getSuperType(to));
		if (parsedData.length != data.length)
			return null;
		return new ConvertedLiteral<>(this, parsedData, (Class<R>) Utils.getSuperType(to));
	}

	@Override
	public String toString(@Nullable PlatformEvent event, boolean debug) {
		if (debug)
			return "[" + Classes.toString(data, getAnd(), StringMode.DEBUG) + "]";
		return Classes.toString(data, getAnd());
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	@Override
	public boolean isSingle() {
		return !getAnd() || data.length <= 1;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

	@Override
	public boolean check(PlatformEvent event, Predicate<? super T> checker, boolean negated) {
		return SimpleExpression.check(data, checker, negated, getAnd());
	}

	@Override
	public boolean check(PlatformEvent event, Predicate<? super T> checker) {
		return SimpleExpression.check(data, checker, false, getAnd());
	}

	@Nullable
	private ClassInfo<? super T> returnTypeInfo;

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		ClassInfo<? super T> returnTypeInfo = this.returnTypeInfo;
		if (returnTypeInfo == null)
			this.returnTypeInfo = returnTypeInfo = Classes.getSuperClassInfo(getReturnType());
		final Changer<? super T> changer = returnTypeInfo.getChanger();
		return changer == null ? null : changer.acceptChange(mode);
	}

	@Override
	public void change(final PlatformEvent event, final Object @Nullable [] delta, final ChangeMode mode) throws UnsupportedOperationException {
		final ClassInfo<? super T> returnTypeInfo = this.returnTypeInfo;
		if (returnTypeInfo == null)
			throw new UnsupportedOperationException();
		final Changer<? super T> changer = returnTypeInfo.getChanger();
		if (changer == null)
			throw new UnsupportedOperationException();
		changer.change(getArray(), delta, mode);
	}

	@Override
	public boolean getAnd() {
		return and;
	}

	@Override
	public boolean setTime(final int time) {
		return false;
	}

	@Override
	public int getTime() {
		return 0;
	}

	@Override
	public NonNullIterator<T> iterator(final PlatformEvent event) {
		return new NonNullIterator<>() {
			private int i = 0;

			@Override
			@Nullable
			protected T getNext() {
				if (i == data.length)
					return null;
				return data[i++];
			}
		};
	}

	@Override
	public boolean isLoopOf(final String input) {
		return false;
	}

	@Override
	public Expression<?> getSource() {
		return source;
	}

	@Override
	public Expression<T> simplify() {
		return this;
	}

}