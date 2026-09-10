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
package ch.njol.skript.lang;

import ch.njol.skript.effects.EffExit;
import ch.njol.skript.effects.EffReturn;
import org.skriptlang.skript.lang.event.PlatformEvent;

/**
 * A {@link Section} implementing this interface can execute a task when
 * it is exited by an {@link EffExit 'exit'} or
 * {@link EffReturn 'return'} effect.
 */
public interface SectionExitHandler {

	/**
	 * Exits the section
	 * @param event The involved event
	 */
	void exit(PlatformEvent event);

}
