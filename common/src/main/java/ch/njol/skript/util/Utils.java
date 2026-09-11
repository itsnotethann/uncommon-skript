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
package ch.njol.skript.util;

import ch.njol.skript.registrations.Classes;
import ch.njol.util.Checker;
import ch.njol.util.NonNullPair;
import ch.njol.util.Pair;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.base.Preconditions;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.util.ClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class.
 * 
 * @author Peter Güttinger
 */
public abstract class Utils {

	public final static Random random = new Random();
	protected final static Deque<WordEnding> plurals = new LinkedList<>();

	static {
		plurals.add(new WordEnding("axe", "axes")); // not complete since we have battleaxe, etc.
		plurals.add(new WordEnding("x", "xes"));

		plurals.add(new WordEnding("ay", "ays"));
		plurals.add(new WordEnding("ey", "eys"));
		plurals.add(new WordEnding("iy", "iys"));
		plurals.add(new WordEnding("oy", "oys"));
		plurals.add(new WordEnding("uy", "uys"));
		plurals.add(new WordEnding("kie", "kies"));
		plurals.add(new WordEnding("zombie", "zombies", true));
		plurals.add(new WordEnding("y", "ies"));

		plurals.add(new WordEnding("wife", "wives", true)); // we have to do the -ife -> ives first
		plurals.add(new WordEnding("life", "lives"));
		plurals.add(new WordEnding("knife", "knives", true));
		plurals.add(new WordEnding("ive", "ives"));

		plurals.add(new WordEnding("lf", "lves")); // self shelf elf wolf half etc.
		plurals.add(new WordEnding("thief", "thieves", true));
		plurals.add(new WordEnding("ief", "iefs")); // chiefs, fiefs, briefs

		plurals.add(new WordEnding("hoof", "hooves"));

		plurals.add(new WordEnding("fe", "ves"));// most -f words' plurals can end in -fs as well as -ves

		plurals.add(new WordEnding("h", "hes"));

		plurals.add(new WordEnding("man", "men"));

		plurals.add(new WordEnding("ui", "uis")); // gui fix
		plurals.add(new WordEnding("api", "apis")); // api fix
		plurals.add(new WordEnding("nautilus", "nautiluses", true));
		plurals.add(new WordEnding("us", "i"));

		plurals.add(new WordEnding("hoe", "hoes", true));
		plurals.add(new WordEnding("toe", "toes", true));
		plurals.add(new WordEnding("foe", "foes", true));
		plurals.add(new WordEnding("woe", "woes", true));
		plurals.add(new WordEnding("o", "oes"));

		plurals.add(new WordEnding("alias", "aliases", true));
		plurals.add(new WordEnding("gas", "gases", true));

		plurals.add(new WordEnding("child", "children")); // grandchild, etc.

		plurals.add(new WordEnding("sheep", "sheep", true));
		plurals.add(new WordEnding("fangs", "fangs"));

		// general ending
		plurals.add(new WordEnding("", "s"));
	}

	private Utils() {}
	
	public static String join(final Object[] objects) {
		assert objects != null;
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < objects.length; i++) {
			if (i != 0)
				b.append(", ");
			b.append(Classes.toString(objects[i]));
		}
		return "" + b.toString();
	}
	
	public static String join(final Iterable<?> objects) {
		assert objects != null;
		final StringBuilder b = new StringBuilder();
		boolean first = true;
		for (final Object o : objects) {
			if (!first)
				b.append(", ");
			else
				first = false;
			b.append(Classes.toString(o));
		}
		return "" + b.toString();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> boolean isEither(@Nullable T compared, @Nullable T... types) {
		return CollectionUtils.contains(types, compared);
	}
	
	public static Pair<String, Integer> getAmount(String s) {
		if (s.matches("\\d+ of .+")) {
			return new Pair<>(s.split(" ", 3)[2], Utils.parseInt("" + s.split(" ", 2)[0]));
		} else if (s.matches("\\d+ .+")) {
			return new Pair<>(s.split(" ", 2)[1], Utils.parseInt("" + s.split(" ", 2)[0]));
		} else if (s.matches("an? .+")) {
			return new Pair<>(s.split(" ", 2)[1], 1);
		}
		return new Pair<>(s, Integer.valueOf(-1));
	}
	
//	public final static class AmountResponse {
//		public final String s;
//		public final int amount;
//		public final boolean every;
//
//		public AmountResponse(final String s, final int amount, final boolean every) {
//			this.s = s;
//			this.amount = amount;
//			this.every = every;
//		}
//
//		public AmountResponse(final String s, final boolean every) {
//			this.s = s;
//			amount = -1;
//			this.every = every;
//		}
//
//		public AmountResponse(final String s, final int amount) {
//			this.s = s;
//			this.amount = amount;
//			every = false;
//		}
//
//		public AmountResponse(final String s) {
//			this.s = s;
//			amount = -1;
//			every = false;
//		}
//	}
//
//	public final static AmountResponse getAmountWithEvery(final String s) {
//		if (s.matches("\\d+ of (all|every) .+")) {
//			return new AmountResponse("" + s.split(" ", 4)[3], Utils.parseInt("" + s.split(" ", 2)[0]), true);
//		} else if (s.matches("\\d+ of .+")) {
//			return new AmountResponse("" + s.split(" ", 3)[2], Utils.parseInt("" + s.split(" ", 2)[0]));
//		} else if (s.matches("\\d+ .+")) {
//			return new AmountResponse("" + s.split(" ", 2)[1], Utils.parseInt("" + s.split(" ", 2)[0]));
//		} else if (s.matches("an? .+")) {
//			return new AmountResponse("" + s.split(" ", 2)[1], 1);
//		} else if (s.matches("(all|every) .+")) {
//			return new AmountResponse("" + s.split(" ", 2)[1], true);
//		}
//		return new AmountResponse(s);
//	}

	@Deprecated
	public static Class<?>[] getClasses(Plugin plugin, String basePackage, String... subPackages) throws IOException {
		return getClasses(plugin.getClass(), getFile(plugin), basePackage, subPackages);
	}

	public static Class<?>[] getClasses(Class<?> source, @Nullable File jarFile, String basePackage, String... subPackages) throws IOException {
		List<Class<?>> classes = new ArrayList<>();

		ClassLoader loader = ClassLoader.builder()
			.basePackage(basePackage)
			.addSubPackages(subPackages)
			.deep(true)
			.initialize(true)
			.forEachClass(classes::add)
			.build();
		if (jarFile != null) {
			loader.loadClasses(source, jarFile);
		} else {
			loader.loadClasses(source);
		}

		return classes.toArray(new Class[0]);
	}

	private static boolean shouldLoad(String entryName, String basePath, String[] subPaths) {
		// entryName is jar/path form: ch/njol/skript/expressions/Foo.class
		if (!entryName.startsWith(basePath)) return false;

		if (subPaths.length == 0) return true;

		int afterBase = basePath.length();
		for (String sub : subPaths) {
			// sub is like "expressions/" but we want it relative to basePath
			if (entryName.startsWith(sub, afterBase)) return true;
		}
		return false;
	}

	/**
	 * The first invocation of this method uses reflection to invoke the protected method JavaPlugin#getFile() to get the plugin's jar file.
	 * 
	 * @return The jar file of the plugin.
	 */
	@Nullable
	public static File getFile(Plugin plugin) {
		try {
			Method getFile = JavaPlugin.class.getDeclaredMethod("getFile");
			getFile.setAccessible(true);
			return (File) getFile.invoke(plugin);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			assert false;
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
		return null;
	}

	/**
	 * @deprecated Use {@link #isPlural(String)} instead.
	 */
	@Deprecated(forRemoval = true, since = "2.14")
	public static NonNullPair<String, Boolean> getEnglishPlural(String word) {
		PluralResult result = isPlural(word);
		return new NonNullPair<>(result.updated, result.plural);
	}

	/**
	 * Stores the result of {@link #isPlural(String)}.
	 *
	 * @param updated The singular version of the passed word, if a single variant exists.
	 * @param plural Whether the word is plural.
	 */
	public record PluralResult(String updated, boolean plural) {

	}

	/**
	 * Returns whether a word is plural. If it is, {@code updated} contains the single variant of the word.
	 * Otherwise, {@code updated == word}.
	 *
	 * @param word The word to check.
	 * @return A pair with the updated word and a boolean indicating whether it was plural.
	 */
	public static PluralResult isPlural(String word) {
		Preconditions.checkNotNull(word, "word cannot be null");

		if (word.isEmpty()) {
			return new PluralResult("", false);
		}

		if (couldBeSingular(word)) {
			return new PluralResult(word, false);
		}

		for (WordEnding ending : plurals) {
			if (ending.isCompleteWord()) {
				// Complete words shouldn't be used as partial pieces
				if (word.length() != ending.plural().length()) {
					continue;
				}
			}

			if (word.endsWith(ending.plural())) {
				return new PluralResult(
					word.substring(0, word.length() - ending.plural().length()) + ending.singular(),
					true
				);
			}

			if (word.endsWith(ending.plural().toUpperCase(Locale.ENGLISH))) {
				return new PluralResult(
					word.substring(0, word.length() - ending.plural().length())
						+ ending.singular().toUpperCase(Locale.ENGLISH),
					true
				);
			}
		}

		return new PluralResult(word, false);
	}

	private static boolean couldBeSingular(String word) {
		for (WordEnding ending : plurals) {
			if (ending.singular().isBlank())
				continue;

			if (ending.isCompleteWord() && ending.singular().length() != word.length())
				continue; // Skip complete words

			if (word.endsWith(ending.singular()) || word.toLowerCase().endsWith(ending.singular())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a singular/plural word override for the given words.
	 * This is inserted first in the list of words to be checked: it will always be matched
	 * and will override all other plurality rules.
	 * This will only match the word <s>exactly</s>, and will not apply to derivations of the word.
	 *
	 * @param singular The singular form of the word
	 * @param plural   The plural form of the word
	 */
	public static void addPluralOverride(String singular, String plural) {
		Utils.plurals.addFirst(new WordEnding(singular, plural, true));
	}

	/**
	 * Gets the english plural of a word.
	 *
	 * @param word
	 * @return The english plural of the given word
	 */
	public static String toEnglishPlural(String word) {
		assert word != null && word.length() != 0;
		for (WordEnding ending : plurals) {
			if (ending.isCompleteWord()) {
				// Complete words shouldn't be used as partial pieces
				if (word.length() != ending.singular().length())
					continue;
			}
			if (word.endsWith(ending.singular()))
				return word.substring(0, word.length() - ending.singular().length()) + ending.plural();
		}
		assert false;
		return word + "s";
	}

	/**
	 * Gets the plural of a word (or not if p is false)
	 *
	 * @param s
	 * @param p
	 * @return The english plural of the given word, or the word itself if p is false.
	 */
	public static String toEnglishPlural(final String s, final boolean p) {
		if (p)
			return toEnglishPlural(s);
		return s;
	}

	protected record WordEnding(String singular, String plural, boolean isCompleteWord) {

		public WordEnding(String singular, String plural) {
			this(singular, plural, false);
		}

		public String singular() {
			return singular;
		}

		public String plural() {
			return plural;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) return true;
			if (!(object instanceof WordEnding ending)) return false;
			return Objects.equals(singular, ending.singular) && Objects.equals(plural, ending.plural);
		}

		@Override
		public int hashCode() {
			return Objects.hash(singular, plural);
		}

	}
	
	/**
	 * Adds 'a' or 'an' to the given string, depending on the first character of the string.
	 * 
	 * @param s The string to add the article to
	 * @return The given string with an appended a/an and a space at the beginning
	 * @see #A(String)
	 * @see #a(String, boolean)
	 */
	public static String a(final String s) {
		return a(s, false);
	}
	
	/**
	 * Adds 'A' or 'An' to the given string, depending on the first character of the string.
	 * 
	 * @param s The string to add the article to
	 * @return The given string with an appended A/An and a space at the beginning
	 * @see #a(String)
	 * @see #a(String, boolean)
	 */
	public static String A(final String s) {
		return a(s, true);
	}
	
	/**
	 * Adds 'a' or 'an' to the given string, depending on the first character of the string.
	 * 
	 * @param s The string to add the article to
	 * @param capA Whether to use a capital a or not
	 * @return The given string with an appended a/an (or A/An if capA is true) and a space at the beginning
	 * @see #a(String)
	 */
	public static String a(final String s, final boolean capA) {
		assert s != null && s.length() != 0;
		if ("aeiouAEIOU".indexOf(s.charAt(0)) != -1) {
			if (capA)
				return "An " + s;
			return "an " + s;
		} else {
			if (capA)
				return "A " + s;
			return "a " + s;
		}
	}
	
	/**
	 * Gets the collision height of solid or partially-solid blocks at the center of the block.
	 * This is mostly for use in the EffTeleport teleport effect.
	 * <p>
	 * This version operates on numeric ids, thus only working on
	 * Minecraft 1.12 or older.
	 * 
	 * @param type
	 * @return The block's height at the center
	 */
	public static double getBlockHeight(final int type, final byte data) {
		switch (type) {
			case 26: // bed
				return 9. / 16;
			case 44: // slabs
			case 126:
				return (data & 0x8) == 0 ? 0.5 : 1;
			case 78: // snow layer
				return data == 0 ? 1 : (data % 8) * 2. / 16;
			case 85: // fences & gates
			case 107:
			case 113:
			case 139: // cobblestone wall
				return 1.5;
			case 88: // soul sand
				return 14. / 16;
			case 92: // cake
				return 7. / 16;
			case 93: // redstone repeater
			case 94:
			case 149: // redstone comparator
			case 150:
				return 2. / 16;
			case 96: // trapdoor
				return (data & 0x4) == 0 ? ((data & 0x8) == 0 ? 3. / 16 : 1) : 0;
			case 116: // enchantment table
				return 12. / 16;
			case 117: // brewing stand
				return 14. / 16;
			case 118: // cauldron
				return 5. / 16;
			case 120: // end portal frame
				return (data & 0x4) == 0 ? 13. / 16 : 1;
			case 127: // cocoa plant
				return 12. / 16;
			case 140: // flower pot
				return 6. / 16;
			case 144: // mob head
				return 0.5;
			case 151: // daylight sensor
				return 6. / 16;
			case 154: // hopper
				return 10. / 16;
			default:
				return 1;
		}
	}

	final static Map<String, String> chat = new HashMap<>();
	final static Map<String, String> englishChat = new HashMap<>();
	
	private final static Pattern stylePattern = Pattern.compile("<([^<>]+)>");

	/**
	 * Replaces &lt;chat styles&gt; in the message
	 *
	 * @param message
	 * @return message with localised chat styles converted to Minecraft's format
	 */
	public static @NotNull String replaceChatStyles(String message) {
		return message;
	}

	/**
	 * Replaces english &lt;chat styles&gt; in the message. This is used for messages in the language file as the language of colour codes is not well defined while the language is
	 * changing, and for some hardcoded messages.
	 *
	 * @param message
	 * @return message with english chat styles converted to Minecraft's format
	 */
	public static String replaceEnglishChatStyles(final String message) {
		return message;
	}

	private static final Pattern HEX_PATTERN = Pattern.compile("(?i)#{0,2}[0-9a-f]{6}");
	
	/**
	 * Gets a random value between <tt>start</tt> (inclusive) and <tt>end</tt> (exclusive)
	 * 
	 * @param start
	 * @param end
	 * @return <tt>start + random.nextInt(end - start)</tt>
	 */
	public static int random(final int start, final int end) {
		if (end <= start)
			throw new IllegalArgumentException("end (" + end + ") must be > start (" + start + ")");
		return start + random.nextInt(end - start);
	}

	/**
	 * @see #highestDenominator(Class, Class[])
	 */
	public static Class<?> getSuperType(final Class<?>... classes) {
		return highestDenominator(Object.class, classes);
	}

	/**
	 * Searches for the highest common denominator of the given types;
	 * in other words, the first supertype they all share.
	 *
	 * <h3>Arbitrary Selection</h3>
	 * Classes may have <b>multiple</b> highest common denominators: interfaces that they share
	 * which do not extend each other.
	 * This method selects a <b>superclass</b> first (where possible)
	 * but its selection of interfaces is quite random.
	 * For this reason, it is advised to specify a "best guess" class as the first parameter, which will be selected if
	 * it's appropriate.
	 * Note that if the "best guess" is <i>not</i> a real supertype, it can never be selected.
	 *
	 * @param bestGuess The fallback class to guess
	 * @param classes The types to check
	 * @return The most appropriate common class of all provided
	 * @param <Found> The highest common denominator found
	 * @param <Type> The input type spread
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <Found, Type extends Found> Class<Found> highestDenominator(Class<? super Found> bestGuess, @NotNull Class<? extends Type> @NotNull ... classes) {
		assert classes.length > 0;
		Class<?> chosen = classes[0];
		outer:
		for (final Class<?> checking : classes) {
			assert checking != null && !checking.isArray() && !checking.isPrimitive() : checking;
			if (chosen.isAssignableFrom(checking))
				continue;
			Class<?> superType = checking;
			do if (superType != Object.class && superType.isAssignableFrom(chosen)) {
				chosen = superType;
				continue outer;
			}
			while ((superType = superType.getSuperclass()) != null);
			for (final Class<?> anInterface : checking.getInterfaces()) {
				superType = highestDenominator(Object.class, anInterface, chosen);
				if (superType != Object.class) {
					chosen = superType;
					continue outer;
				}
			}
			return (Class<Found>) bestGuess;
		}
		if (!bestGuess.isAssignableFrom(chosen)) // we struck out on a type we don't want
			return (Class<Found>) bestGuess;
		// Cloneable is about as useful as object as super type
		// However, it lacks special handling used for Object supertype
		// See #1747 to learn how it broke returning items from functions
		return (Class<Found>) (chosen == Cloneable.class ? bestGuess : chosen == Object.class ? bestGuess : chosen);
	}
	
	/**
	 * Parses a number that was validated to be an integer but might still result in a {@link NumberFormatException} when parsed with {@link Integer#parseInt(String)} due to
	 * overflow.
	 * This method will return {@link Integer#MIN_VALUE} or {@link Integer#MAX_VALUE} respectively if that happens.
	 * 
	 * @param s
	 * @return The parsed integer, {@link Integer#MIN_VALUE} or {@link Integer#MAX_VALUE} respectively
	 */
	public static int parseInt(final String s) {
		assert s.matches("-?\\d+");
		try {
			return Integer.parseInt(s);
		} catch (final NumberFormatException e) {
			return s.startsWith("-") ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Parses a number that was validated to be an integer but might still result in a {@link NumberFormatException} when parsed with {@link Long#parseLong(String)} due to
	 * overflow.
	 * This method will return {@link Long#MIN_VALUE} or {@link Long#MAX_VALUE} respectively if that happens.
	 * 
	 * @param s
	 * @return The parsed long, {@link Long#MIN_VALUE} or {@link Long#MAX_VALUE} respectively
	 */
	public static long parseLong(final String s) {
		assert s.matches("-?\\d+");
		try {
			return Long.parseLong(s);
		} catch (final NumberFormatException e) {
			return s.startsWith("-") ? Long.MIN_VALUE : Long.MAX_VALUE;
		}
	}
	
	/**
	 * Gets class for name. Throws RuntimeException instead of checked one.
	 * Use this only when absolutely necessary.
	 * @param name Class name.
	 * @return The class.
	 */
	public static Class<?> classForName(String name) {
		Class<?> c;
		try {
			c = Class.forName(name);
			return c;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found!");
		}
	}
	
	/**
	 * Finds the index of the last in a {@link List} that matches the given {@link Checker}.
	 *
	 * @param list the {@link List} to search.
	 * @param checker the {@link Checker} to match elements against.
	 * @return the index of the element found, or -1 if no matching element was found.
	 */
	public static <T> int findLastIndex(List<T> list, Checker<T> checker) {
		int lastIndex = -1;
		for (int i = 0; i < list.size(); i++) {
			if (checker.check(list.get(i)))
				lastIndex = i;
		}
		return lastIndex;
	}

	public static boolean isInteger(Number... numbers) {
		for (Number number : numbers) {
			if (Double.class.isAssignableFrom(number.getClass()) || Float.class.isAssignableFrom(number.getClass()))
				return false;
		}
		return true;
	}

	/**
	 * @param cls The class.
	 * @return The component of cls if cls is an array, otherwise cls.
	 */
	public static Class<?> getComponentType(Class<?> cls) {
		if (cls != null && cls.isArray()) {
			return cls.componentType();
		}
		return cls;
	}
	
}
