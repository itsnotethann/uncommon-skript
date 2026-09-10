package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("Replace")
@Description("Replaces all occurrences of a given text with another text. Please note that you can only change variables and a few expressions, e.g. a <a href='./expressions.html#ExprMessage'>message</a> or a line of a sign.")
@Examples({"replace \"<item>\" in {textvar} with \"%item%\"",
	"replace every \"&\" with \"§\" in line 1",
	"# The following acts as a simple chat censor, but it will e.g. censor mass, hassle, assassin, etc. as well:",
	"on chat:",
	"	replace all \"kys\", \"idiot\" and \"noob\" with \"****\" in the message",
	" ",
	"replace all stone and dirt in player's inventory and player's top inventory with diamond"})
@Since("2.0, 2.2-dev24 (replace in multiple strings and replace items in inventory), 2.5 (replace first, case sensitivity)")
public class EffReplace extends Effect {

	static {
		Skript.registerEffect(EffReplace.class,
			"replace [all|every|first:[the] first] %strings% in %strings% with %string% [case:with case sensitivity]",
			"replace [all|every|first:[the] first] %strings% with %string% in %strings% [case:with case sensitivity]",
			"(replace [with|using] regex|regex replace) %strings% in %strings% with %string%",
			"(replace [with|using] regex|regex replace) %strings% with %string% in %strings%");
	}

	@SuppressWarnings("null")
	private Expression<?> haystack, needles, replacement;
	private boolean replaceString;
	private boolean replaceRegex;
	private boolean replaceFirst;
	private boolean caseSensitive = false;

	@SuppressWarnings({"null"})
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		haystack =  expressions[1 + matchedPattern % 2];
		replaceString = matchedPattern < 4;
		replaceFirst = matchedPattern > 1 && matchedPattern < 4;
		if (replaceString && !ChangerUtils.acceptsChange(haystack, ChangeMode.SET, String.class)) {
			Skript.error(haystack + " cannot be changed and can thus not have parts replaced.");
			return false;
		}
		if (SkriptConfig.caseSensitive.value() || parseResult.hasTag("case")) {
			caseSensitive = true;
		}
		needles = expressions[0];
		replacement = expressions[2 - matchedPattern % 2];
		return true;
	}

	@SuppressWarnings("null")
	@Override
	protected void execute(PlatformEvent event) {
		Object[] needles = this.needles.getAll(event);
		if (haystack instanceof ExpressionList<?> list) {
			for (Expression<?> haystackExpr : list.getExpressions()) {
				replace(event, needles, haystackExpr);
			}
		} else {
			replace(event, needles, haystack);
		}
	}

	private void replace(PlatformEvent event, Object[] needles, Expression<?> haystackExpr) {
		Object[] haystack = haystackExpr.getAll(event);
		Object replacement = this.replacement.getSingle(event);
		if (replacement == null || haystack == null || haystack.length == 0 || needles == null || needles.length == 0)
			return;
		if (!replaceString) return;
		Function<String, String> replaceFunction = getReplaceFunction(needles, (String) replacement);
		//noinspection unchecked
		((Expression<String>) haystackExpr).changeInPlace(event, replaceFunction);
	}

	private @NotNull Function<String, String> getReplaceFunction(Object[] needles, String replacement) {
		Function<String, String> replaceFunction;
		if (replaceRegex) {
			List<Pattern> patterns = new ArrayList<>(needles.length);
			for (Object needle : needles) {
				try {
					patterns.add(Pattern.compile((String) needle));
				} catch (Exception ignored) { }
			}
			replaceFunction = haystackString -> {
				for (Pattern pattern : patterns) {
					Matcher matcher = pattern.matcher(haystackString);
					if (replaceFirst) {
						haystackString = matcher.replaceFirst(replacement);
					} else {
						haystackString = matcher.replaceAll(replacement);
					}
				}
				return haystackString;
			};
		} else if (replaceFirst) {
			replaceFunction = haystackString -> {
				for (Object needle : needles) {
					assert needle != null;
					haystackString = StringUtils.replaceFirst(haystackString, (String) needle, Matcher.quoteReplacement(replacement), caseSensitive);
				}
				return haystackString;
			};
		} else {
			replaceFunction = haystackString -> {
				for (Object needle : needles) {
					assert needle != null;
					haystackString = StringUtils.replace(haystackString, (String) needle, replacement, caseSensitive);
				}
				return haystackString;
			};
		}
		return replaceFunction;
	}

	@Override
	public String toString(@Nullable PlatformEvent event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);

		builder.append("replace");
		if (replaceFirst)
			builder.append("the first");
		if (replaceRegex)
			builder.append("regex");
		builder.append(needles, "in", haystack, "with", replacement);
		if (caseSensitive)
			builder.append("with case sensitivity");

		return builder.toString();
	}

}
