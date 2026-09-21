package org.skriptlang.skript.testhost;

import ch.njol.skript.util.Version;

import java.util.ArrayList;
import java.util.List;

public final class VersionOrderTest {

	private record Case(String left, String right, int expected, String why) { }

	private static final List<Case> TABLE = List.of(
		new Case("2.9.1", "2.10.0", -1, "minor is compared numerically, not lexicographically"),
		new Case("2.16.2", "2.16.2", 0, "identical versions"),
		new Case("2.16.3", "2.16.2", 1, "revision"),

		new Case("2.16.2", "2.16.2-alpha.1", 1, "a release outranks a prerelease of the same version"),
		new Case("2.16.2-alpha.1", "2.16.2", -1, "and the other way round"),

		new Case("2.16.2-alpha.3", "2.16.2-alpha.20", -1, "digit-only identifiers compare as integers"),
		new Case("2.16.2-alpha.20", "2.16.2-alpha.3", 1, "and the other way round"),
		new Case("2.16.2-alpha.9", "2.16.2-alpha.10", -1, "9 < 10, which string order gets wrong"),

		new Case("2.16.2-alpha", "2.16.2-beta", -1, "non-numeric identifiers compare lexicographically"),
		new Case("2.16.2-beta", "2.16.2-alpha", 1, "and the other way round"),

		new Case("2.16.2-alpha", "2.16.2-alpha.1", -1, "a shorter identifier list sorts first"),
		new Case("2.16.2-alpha.1", "2.16.2-alpha", 1, "and the other way round"),
		new Case("2.16.2-alpha.1", "2.16.2-alpha.1", 0, "identical postfixes")
	);

	private static int sign(int value) {
		return Integer.compare(value, 0);
	}

	public static void main(String[] args) {
		int failures = 0;
		for (Case test : TABLE) {
			int actual;
			try {
				actual = sign(new Version(test.left()).compareTo(new Version(test.right())));
			} catch (RuntimeException e) {
				System.out.println("FAIL " + test.left() + " ? " + test.right()
					+ "  threw " + e);
				failures++;
				continue;
			}
			if (actual == test.expected()) {
				System.out.println("ok   " + pad(test.left()) + " ? " + pad(test.right())
					+ " = " + actual);
			} else {
				System.out.println("FAIL " + pad(test.left()) + " ? " + pad(test.right())
					+ " = " + actual + ", expected " + test.expected()
					+ "   (" + test.why() + ")");
				failures++;
			}
		}
		System.out.println("VERSIONTEST " + (failures == 0 ? "PASS" : "FAIL")
			+ " " + (TABLE.size() - failures) + "/" + TABLE.size());
		if (failures != 0)
			System.exit(1);
	}

	private static String pad(String value) {
		return value.length() >= 18 ? value : value + " ".repeat(18 - value.length());
	}

}
