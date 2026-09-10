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
package ch.njol.skript.expressions.arithmetic;

import ch.njol.skript.lang.Expression;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;

public class ArithmeticExpressionInfo<T> implements ArithmeticGettable<T> {
	
	private final Expression<? extends T> expression;
	
	public ArithmeticExpressionInfo(Expression<? extends T> expression) {
		this.expression = expression;
	}

	@Override
	@Nullable
	public T get(PlatformEvent event) {
		T object = expression.getSingle(event);
		return object == null ? Arithmetics.getDefaultValue(expression.getReturnType()) : object;
	}

	@Override
	public Class<? extends T> getReturnType() {
		return expression.getReturnType();
	}

}
