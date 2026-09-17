package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.ContainerExpression;
import ch.njol.skript.util.Container;
import ch.njol.skript.util.Container.ContainerType;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import com.google.common.collect.PeekingIterator;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Name("Loop")
@Description({
	"Loop sections repeat their code with multiple values.",
	"",
	"A loop will loop through all elements of the given expression, e.g. all players, worlds, items, etc. " +
		"The conditions & effects inside the loop will be executed for every of those elements, " +
		"which can be accessed with ‘loop-<what>’, e.g. <code>send \"hello\" to loop-player</code>. " +
		"When a condition inside a loop is not fulfilled the loop will start over with the next element of the loop. " +
		"You can however use <code>stop loop</code> to exit the loop completely and resume code execution after the end of the loop.",
	"",
	"<b>Loopable Values</b>",
	"All expressions that represent more than one value, e.g. ‘all players’, ‘worlds’, " +
		"etc., as well as list variables, can be looped. You can also use a list of expressions, e.g. <code>loop the victim " +
		"and the attacker</code>, to execute the same code for only a few values.",
	"",
	"<b>List Variables</b>",
	"When looping list variables, you can also use <code>loop-index</code> in addition to <code>loop-value</code> inside " +
		"the loop. <code>loop-value</code> is the value of the currently looped variable, and <code>loop-index</code> " +
		"is the last part of the variable's name (the part where the list variable has its asterisk *)."
})
@Example("""
	loop all players:
		send "Hello %loop-player%!" to loop-player
	""")
@Example("""
	loop items in player's inventory:
		if loop-item is dirt:
			set loop-item to air
	""")
@Example("""
	loop 10 times:
		send title "%11 - loop-value%" and subtitle "seconds left until the game begins" to player for 1 second # 10, 9, 8 etc.
		wait 1 second
	""")
@Example("""
	loop {Coins::*}:
		set {Coins::%loop-index%} to loop-value + 5 # Same as "add 5 to {Coins::%loop-index%}" where loop-index is the uuid of " +
		"the player and loop-value is the number of coins for the player
	""")
@Example("""
	loop shuffled (integers between 0 and 8):
		if all:
			previous loop-value = 1
			loop-value = 4
			next loop-value = 8
		then:
			kill all players
	""")
@Since("1.0")
public class SecLoop extends LoopSection {

	static {
		Skript.registerSection(SecLoop.class, "loop %objects%");
	}

	protected @UnknownNullability Expression<?> expression;

	protected @Nullable TriggerItem actualNext;
	private boolean guaranteedToLoop;
	private boolean loopPeeking;
	protected boolean iterableSingle;
	protected boolean keyed;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs,
	                    int matchedPattern,
	                    Kleenean isDelayed,
	                    ParseResult parseResult,
	                    SectionNode sectionNode,
	                    List<TriggerItem> triggerItems) {
		this.expression = LiteralUtils.defendExpression(exprs[0]);
		if (!LiteralUtils.canInitSafely(expression)) {
			Skript.error("Can't understand this loop: '" + parseResult.expr.substring(5) + "'");
			return false;
		}

		if (!(expression instanceof Variable) && Container.class.isAssignableFrom(expression.getReturnType())) {
			ContainerType type = expression.getReturnType().getAnnotation(ContainerType.class);
			if (type == null)
				throw new SkriptAPIException(expression.getReturnType().getName() + " implements Container but is missing the required @ContainerType annotation");
			this.expression = new ContainerExpression((Expression<? extends Container<?>>) expression, type.value());
		}

		if (expression.isSingle()) {
			Skript.error("Can't loop '" + expression + "' because it's only a single value");
			return false;
		}
		loopPeeking = exprs[0].supportsLoopPeeking();

		guaranteedToLoop = guaranteedToLoop(expression);
		keyed = KeyedIterableExpression.canIterateWithKeys(expression);
		loadOptionalCode(sectionNode);
		this.setInternalNext(this);

		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(PlatformEvent event) {
		LoopState state = loopState(event);
		Iterator<?> iter = state.iterator;
		if (iter == null) {
			if (iterableSingle) {
				Object value = expression.getSingle(event);
				if (value instanceof Container<?> container) {
					// Container may have special behaviour over regular iterator
					iter = container.containerIterator();
				} else if (value instanceof Iterable<?> iterable) {
					iter = iterable.iterator();
				} else {
					iter = Collections.singleton(value).iterator();
				}
			} else {
				iter = keyed
					? ((KeyedIterableExpression<?>) expression).keyedIterator(event)
					: expression.iterator(event);
				if (iter != null && iter.hasNext()) {
					state.iterator = iter;
				} else {
					iter = null;
				}
			}
		}

		Object nextValue = state.next;
		if (iter == null || (!iter.hasNext() && nextValue == null)) {
			exit(event);
			debug(event, false);
			return actualNext;
		} else {
			if (state.hasCurrent)
				state.previous = state.current;
			if (nextValue != null) {
				this.store(event, nextValue);
				state.next = null;
			} else if (iter.hasNext()) {
				this.store(event, iter.next());
			}
			return walk(event, true);
		}
	}

	protected void store(PlatformEvent event, Object next) {
		LoopState state = loopState(event);
		state.current = next;
		state.hasCurrent = true;
		state.counter++;
	}

	@Override
	public @Nullable ExecutionIntent executionIntent() {
		return guaranteedToLoop ? triggerExecutionIntent() : null;
	}

	@Override
	public String toString(@Nullable PlatformEvent event, boolean debug) {
		return "loop " + expression.toString(event, debug);
	}

	public @Nullable Object getCurrent(PlatformEvent event) {
		LoopState state = currentLoopState(event);
		return state == null ? null : state.current;
	}

	public @Nullable Object getNext(PlatformEvent event) {
		if (!loopPeeking)
			return null;
		LoopState state = currentLoopState(event);
		if (state == null)
			return null;
		Object nextValue = state.next;
		if (nextValue != null) {
			return nextValue;
		}
		Iterator<?> iter = state.iterator;
		if (iter == null || !iter.hasNext())
			return null;
		if (iter instanceof PeekingIterator<?> peekingIterator)
			return peekingIterator.peek();
		nextValue = iter.next();
		state.next = nextValue;
		return nextValue;
	}

	public @Nullable Object getPrevious(PlatformEvent event) {
		LoopState state = currentLoopState(event);
		return state == null ? null : state.previous;
	}

	public Expression<?> getLoopedExpression() {
		return expression;
	}

	public boolean isKeyedLoop() {
		return keyed;
	}

	@Override
	public SecLoop setNext(@Nullable TriggerItem next) {
		actualNext = next;
		return this;
	}

	/**
	 * @see LoopSection#setNext(TriggerItem)
	 */
	protected void setInternalNext(TriggerItem item) {
		super.setNext(item);
	}

	@Nullable
	@Override
	public TriggerItem getActualNext() {
		return actualNext;
	}

	@Override
	public void exit(PlatformEvent event) {
		super.exit(event);
	}

	private static boolean guaranteedToLoop(Expression<?> expression) {
		// If the expression is a literal, it's guaranteed to loop if it has at least one value
		if (expression instanceof Literal<?> literal)
			return literal.getAll().length > 0;

		// If the expression isn't a list, then we can't guarantee that it will loop
		if (!(expression instanceof ExpressionList<?> list))
			return false;

		// If the list is an OR list (a, b or c), then it's guaranteed to loop iff all its values are guaranteed to loop
		if (!list.getAnd()) {
			for (Expression<?> expr : list.getExpressions()) {
				if (!guaranteedToLoop(expr))
					return false;
			}
			return true;
		}

		// If the list is an AND list (a, b and c), then it's guaranteed to loop if any of its values are guaranteed to loop
		for (Expression<?> expr : list.getExpressions()) {
			if (guaranteedToLoop(expr))
				return true;
		}

		// Otherwise, we can't guarantee that it will loop
		return false;
	}

	public boolean supportsPeeking() {
		return loopPeeking;
	}

	public Expression<?> getExpression() {
		return expression;
	}

}