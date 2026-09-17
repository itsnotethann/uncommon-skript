package org.skriptlang.skript.testhost.bench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;

import ch.njol.skript.Skript;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.AddonHandle;
import org.skriptlang.skript.platform.AddonRegistry;
import org.skriptlang.skript.platform.EventBus;
import org.skriptlang.skript.platform.LogSink;
import org.skriptlang.skript.platform.Platform;
import org.skriptlang.skript.platform.PlatformEnvironment;
import org.skriptlang.skript.platform.PlatformProvider;
import org.skriptlang.skript.platform.PlatformScheduler;
import org.skriptlang.skript.testhost.TickLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BenchHost implements PlatformProvider {

	private static final long TIMEOUT_MILLIS = 60_000;

	private static final int WARMUP_ROUNDS = Integer.getInteger("bench.warmup", 8);
	private static final int ROUNDS = Integer.getInteger("bench.rounds", 15);
	private static final int FIRES = Integer.getInteger("bench.fires", 200);

	private static TickLoop loop;

	private final File serverDirectory;
	private final Logger logger = LoggerFactory.getLogger("Skript");
	private final EventBus eventBus = new BenchEventBus();
	private final PlatformEnvironment environment;
	private @Nullable AddonRegistry addonRegistry;

	private BenchHost(File serverDirectory) {
		this.serverDirectory = serverDirectory;
		this.environment = new PlatformEnvironment() {
			@Override
			public String platformVersion() {
				return "1.21.0";
			}

			@Override
			public boolean isPrimaryThread() {
				return loop.isPrimaryThread();
			}

			@Override
			public File serverDirectory() {
				return BenchHost.this.serverDirectory;
			}

			@Override
			public Logger logger() {
				return logger;
			}
		};
	}

	public static void main(String[] args) throws Exception {
		Path scripts = Path.of(args[0]);
		Path work = Path.of(args[1]);
		Path results = work.resolve("results.csv");

		stage(scripts, work.resolve("Skript").resolve("scripts"));
		configure(work.resolve("Skript").resolve("config.sk"));

		loop = new TickLoop();
		Platform.install(new BenchHost(work.toFile()));

		CountDownLatch loaded = new CountDownLatch(1);
		Throwable[] failure = new Throwable[1];
		loop.start();
		loop.sync(() -> {
			try {
				Skript skript = new Skript();
				Skript.onRegistration(BenchSyntax::register);
				Skript.onFinishedLoading(loaded::countDown);
				skript.setEnabled(true);
				skript.onEnable();
				Skript.closeUnsafeSkript();
			} catch (Throwable throwable) {
				failure[0] = throwable;
				loaded.countDown();
			}
		}, 0, -1);

		if (!loaded.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			System.out.println("BENCH FAIL: scripts did not finish loading");
			System.exit(2);
		}
		if (failure[0] != null) {
			failure[0].printStackTrace();
			System.exit(2);
		}

		List<String> names = new ArrayList<>(BenchSyntax.names());
		names.sort(String::compareTo);
		if (names.isEmpty()) {
			System.out.println("BENCH FAIL: no bench triggers were loaded");
			System.exit(2);
		}

		Map<String, Long> measured = new LinkedHashMap<>();
		CountDownLatch done = new CountDownLatch(1);
		loop.sync(() -> {
			try {
				for (String name : names)
					measured.put(name, measure(name));
			} finally {
				done.countDown();
			}
		}, 0, -1);
		done.await();

		report(measured, readBaseline());
		write(results, measured);
		System.out.println();
		System.out.println("wrote " + results.toAbsolutePath());

		loop.shutdown();
		System.exit(0);
	}

	private static long measure(String name) {
		BenchEvent event = new BenchEvent(name);
		EventBus bus = Skript.eventBus();
		for (int round = 0; round < WARMUP_ROUNDS; round++)
			fire(bus, event);
		long best = Long.MAX_VALUE;
		for (int round = 0; round < ROUNDS; round++)
			best = Math.min(best, fire(bus, event));
		return best;
	}

	private static long fire(EventBus bus, BenchEvent event) {
		long start = System.nanoTime();
		for (int i = 0; i < FIRES; i++)
			bus.fire(event);
		return (System.nanoTime() - start) / FIRES;
	}

	private static void report(Map<String, Long> measured, Map<String, Long> baseline) {
		int width = Math.max(5, measured.keySet().stream().mapToInt(String::length).max().orElse(5));
		String header = "%-" + width + "s  %12s";
		String row = "%-" + width + "s  %12d";
		System.out.println();
		if (baseline.isEmpty()) {
			System.out.println(String.format(header, "bench", "ns/fire"));
		} else {
			System.out.println(String.format(header + "  %12s  %8s", "bench", "ns/fire", "baseline", "delta"));
		}
		for (Map.Entry<String, Long> entry : measured.entrySet()) {
			Long before = baseline.get(entry.getKey());
			if (before == null || before == 0) {
				System.out.println(String.format(row, entry.getKey(), entry.getValue()));
			} else {
				double delta = (entry.getValue() - before) * 100.0 / before;
				System.out.println(String.format(row + "  %12d  %+7.1f%%", entry.getKey(), entry.getValue(), before, delta));
			}
		}
	}

	private static Map<String, Long> readBaseline() throws IOException {
		Map<String, Long> baseline = new LinkedHashMap<>();
		String path = System.getProperty("bench.baseline");
		if (path == null || path.isBlank())
			return baseline;
		Path file = Path.of(path);
		if (!Files.exists(file)) {
			System.out.println("bench.baseline does not exist: " + file.toAbsolutePath());
			return baseline;
		}
		for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			int comma = line.lastIndexOf(',');
			if (comma < 0 || line.startsWith("bench,"))
				continue;
			baseline.put(line.substring(0, comma).trim(), Long.parseLong(line.substring(comma + 1).trim()));
		}
		return baseline;
	}

	private static void write(Path results, Map<String, Long> measured) throws IOException {
		Files.createDirectories(results.getParent());
		StringBuilder text = new StringBuilder("bench,ns_per_fire\n");
		for (Map.Entry<String, Long> entry : measured.entrySet())
			text.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
		Files.writeString(results, text.toString(), StandardCharsets.UTF_8);
	}

	private static void stage(Path source, Path target) throws IOException {
		Files.createDirectories(target);
		try (Stream<Path> old = Files.list(target)) {
			for (Path path : old.toList()) {
				if (Files.isRegularFile(path))
					Files.delete(path);
			}
		}
		try (Stream<Path> files = Files.list(source)) {
			for (Path path : files.filter(path -> path.getFileName().toString().endsWith(".sk")).sorted().toList())
				Files.copy(path, target.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void configure(Path config) throws IOException {
		if (!Files.exists(config))
			return;
		String text = Files.readString(config, StandardCharsets.UTF_8);
		text = text.replaceAll("(?m)^script loader thread size:.*$", "script loader thread size: 0");
		text = text.replaceAll("(?m)^(\\s*)type: CSV\\s*$", "$1type: disabled");
		Files.writeString(config, text, StandardCharsets.UTF_8);
	}

	@Override
	public PlatformEnvironment environment() {
		return environment;
	}

	@Override
	public LogSink logSink() {
		return (level, message, error) -> {
			if (level.intValue() >= Level.SEVERE.intValue())
				logger.error(message, error);
			else if (level.intValue() >= Level.WARNING.intValue())
				logger.warn(message, error);
		};
	}

	@Override
	public PlatformScheduler scheduler() {
		return loop;
	}

	@Override
	public EventBus eventBus() {
		return eventBus;
	}

	@Override
	public synchronized AddonRegistry addonRegistry() {
		if (addonRegistry == null) {
			addonRegistry = ServiceLoader.load(AddonRegistry.class, BenchHost.class.getClassLoader())
				.findFirst()
				.orElseGet(EmptyAddonRegistry::new);
		}
		return addonRegistry;
	}

	private static final class EmptyAddonRegistry implements AddonRegistry {

		@Override
		public @Nullable AddonHandle load(File jar) {
			return null;
		}

		@Override
		public List<AddonHandle> addons() {
			return List.of();
		}

		@Override
		public @Nullable AddonHandle providingAddon(Class<?> from) {
			return null;
		}
	}
}
