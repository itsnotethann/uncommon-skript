package org.skriptlang.skript.testhost;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

final class Recorder {

	private final Map<String, List<String>> marks = new java.util.concurrent.ConcurrentHashMap<>();
	private final List<String> problems = new ArrayList<>();

	static String normalise(String line) {
		int end = line.length();
		while (end > 0 && (line.charAt(end - 1) == '\r' || line.charAt(end - 1) == '\n'))
			end--;
		return end == line.length() ? line : line.substring(0, end);
	}

	void mark(String script, String text) {
		text = normalise(text);
		System.out.println("MARK " + script + ": " + text);
		List<String> list = marks.computeIfAbsent(script, key -> new ArrayList<>());
		synchronized (list) {
			list.add(text);
		}
	}

	void log(Level level, String message) {
		if (level.intValue() < Level.WARNING.intValue())
			return;
		synchronized (problems) {
			problems.add(message);
		}
	}

	private List<String> marks(String script) {
		List<String> list = marks.get(script);
		if (list == null)
			return List.of();
		synchronized (list) {
			return new ArrayList<>(list);
		}
	}

	boolean reached(Map<String, TestHost.Expected> expected) {
		for (Map.Entry<String, TestHost.Expected> entry : expected.entrySet()) {
			if (marks(entry.getKey()).size() < entry.getValue().marks.size())
				return false;
		}
		return true;
	}

	int compare(Map<String, TestHost.Expected> expected) {
		List<String> logged;
		synchronized (problems) {
			logged = new ArrayList<>(problems);
		}
		Set<String> unclaimed = new LinkedHashSet<>(logged);
		int failures = 0;
		for (Map.Entry<String, TestHost.Expected> entry : expected.entrySet()) {
			String script = entry.getKey();
			TestHost.Expected want = entry.getValue();
			List<String> got = marks(script);
			List<String> report = new ArrayList<>();
			if (!got.equals(want.marks)) {
				int size = Math.max(got.size(), want.marks.size());
				for (int i = 0; i < size; i++) {
					String g = i < got.size() ? got.get(i) : "<missing>";
					String w = i < want.marks.size() ? want.marks.get(i) : "<none>";
					report.add((g.equals(w) ? "    " : "  ! ") + "expected [" + w + "] got [" + g + "]");
				}
			}
			for (String error : want.errors) {
				String match = logged.stream().filter(line -> line.contains(error)).findFirst().orElse(null);
				if (match == null)
					report.add("  ! expected a warning or error containing [" + error + "]");
				else
					unclaimed.remove(match);
			}
			if (report.isEmpty()) {
				System.out.println("PASS " + script);
			} else {
				failures++;
				System.out.println("FAIL " + script);
				report.forEach(System.out::println);
			}
		}
		for (String script : marks.keySet()) {
			if (!expected.containsKey(script)) {
				failures++;
				System.out.println("FAIL " + script + " marked without a staged script");
			}
		}
		if (!unclaimed.isEmpty()) {
			failures++;
			System.out.println("FAIL unexpected warnings or errors:");
			unclaimed.forEach(line -> System.out.println("  ! " + line));
		}
		return failures;
	}
}
