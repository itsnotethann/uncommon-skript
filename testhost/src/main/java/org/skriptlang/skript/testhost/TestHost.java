package org.skriptlang.skript.testhost;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestHost implements PlatformProvider {

	private static final long TIMEOUT_MILLIS = 30_000;
	private static final long SETTLE_MILLIS = 1_500;

	private static TickLoop loop;
	private static int phase;
	private static boolean everyScript;
	private static final Set<Path> reportedSkips = new HashSet<>();
	private static final Recorder recorder = new Recorder();

	private final File serverDirectory;
	private final Logger logger = LoggerFactory.getLogger("Skript");
	private final EventBus eventBus = new TestEventBus();
	private final PlatformEnvironment environment;
	private @Nullable AddonRegistry addonRegistry;

	private TestHost(File serverDirectory) {
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
				return TestHost.this.serverDirectory;
			}

			@Override
			public Logger logger() {
				return logger;
			}
		};
	}

	static TickLoop loop() {
		return loop;
	}

	static int phase() {
		return phase;
	}

	static Recorder recorder() {
		return recorder;
	}

	public static void main(String[] args) throws Exception {
		Path scripts = Path.of(args[0]);
		Path work = Path.of(args[1]);
		phase = Integer.parseInt(args[2]);
		everyScript = args[3].equals("all");
		String loaderThreads = args[4];

		Path scriptsFolder = work.resolve("Skript").resolve("scripts");
		stage(scripts, scriptsFolder);
		if (args.length > 5)
			stageAddons(Path.of(args[5]), work.resolve("Skript").resolve("addons"));
		configure(work.resolve("Skript").resolve("config.sk"), loaderThreads);

		loop = new TickLoop();
		Platform.install(new TestHost(work.toFile()));

		CountDownLatch loaded = new CountDownLatch(1);
		CountDownLatch enabled = new CountDownLatch(1);
		Throwable[] failure = new Throwable[1];
		Runnable boot = () -> {
			try {
				Skript skript = new Skript();
				Skript.onRegistration(TestSyntax::register);
				Skript.onFinishedLoading(loaded::countDown);
				skript.setEnabled(true);
				skript.onEnable();
				Skript.closeUnsafeSkript();
			} catch (Throwable throwable) {
				failure[0] = throwable;
				loaded.countDown();
			} finally {
				enabled.countDown();
			}
		};

		if (Boolean.getBoolean("testhost.bootBeforeTicking")) {
			Thread bootThread = new Thread(boot, "TestHost-Boot");
			bootThread.setDaemon(true);
			bootThread.start();
			if (!enabled.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				System.out.println("BOOTTEST FAIL phase " + phase + ": boot did not return before the tick loop started");
				System.exit(2);
			}
			loop.start();
		} else {
			loop.start();
			loop.sync(boot, 0, -1);
		}

		enabled.await();
		if (failure[0] != null) {
			failure[0].printStackTrace();
			System.exit(2);
		}
		if (!loaded.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
			System.out.println("BOOTTEST FAIL: scripts did not finish loading");
			System.exit(2);
		}

		Map<String, Expected> expected = readExpected(scripts);
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (System.currentTimeMillis() < deadline && !recorder.reached(expected))
			Thread.sleep(50);
		Thread.sleep(SETTLE_MILLIS);

		CountDownLatch disabled = new CountDownLatch(1);
		loop.sync(() -> {
			try {
				Skript skript = Skript.getInstance();
				if (skript.isEnabled()) {
					skript.onDisable();
					skript.setEnabled(false);
				}
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			} finally {
				disabled.countDown();
			}
		}, 0, -1);
		disabled.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

		int failures = recorder.compare(expected);
		loop.shutdown();
		System.out.println(failures == 0
			? "BOOTTEST PASS phase " + phase
			: "BOOTTEST FAIL phase " + phase + ": " + failures + " script(s)");
		System.exit(failures == 0 ? 0 : 1);
	}

	private static void stage(Path source, Path target) throws IOException {
		Files.createDirectories(target);
		try (Stream<Path> old = Files.list(target)) {
			for (Path path : old.toList()) {
				if (Files.isRegularFile(path))
					Files.delete(path);
			}
		}
		for (Path path : scripts(source))
			Files.copy(path, target.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	private static void stageAddons(Path source, Path target) throws IOException {
		Files.createDirectories(target);
		List<Path> jars;
		try (Stream<Path> files = Files.list(source)) {
			jars = files.filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().toList();
		}
		if (jars.isEmpty()) {
			System.out.println("BOOTTEST FAIL: no addon jars in " + source);
			System.exit(2);
		}
		for (Path jar : jars)
			Files.copy(jar, target.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	private static void configure(Path config, String loaderThreads) throws IOException {
		if (!Files.exists(config))
			return;
		String text = Files.readString(config, StandardCharsets.UTF_8);
		text = text.replaceAll("(?m)^script loader thread size:.*$", "script loader thread size: " + loaderThreads);
		Files.writeString(config, text, StandardCharsets.UTF_8);
	}

	private static List<Path> scripts(Path source) throws IOException {
		try (Stream<Path> files = Files.list(source)) {
			return files
				.filter(path -> path.getFileName().toString().endsWith(".sk"))
				.filter(path -> everyScript || Files.exists(phaseExpectation(path)))
				.filter(path -> !skipped(path))
				.sorted()
				.toList();
		}
	}

	private static boolean skipped(Path script) {
		Path expectation = expectation(script);
		try {
			if (!Files.exists(expectation))
				return false;
			List<String> lines = Files.readAllLines(expectation, StandardCharsets.UTF_8);
			if (lines.isEmpty() || !lines.getFirst().startsWith("skip:"))
				return false;
			if (reportedSkips.add(script))
				System.out.println("SKIP " + script.getFileName() + " in phase " + phase + ":" + lines.getFirst().substring(5));
			return true;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Path phaseExpectation(Path script) {
		return sibling(script, phase == 1 ? ".expected" : ".phase" + phase + ".expected");
	}

	private static Path expectation(Path script) {
		Path specific = phaseExpectation(script);
		return Files.exists(specific) ? specific : sibling(script, ".expected");
	}

	private static Path sibling(Path script, String suffix) {
		String file = script.getFileName().toString();
		return script.resolveSibling(file.substring(0, file.length() - 3) + suffix);
	}

	private static Map<String, Expected> readExpected(Path scripts) throws IOException {
		Map<String, Expected> expected = new TreeMap<>();
		for (Path path : scripts(scripts)) {
			Path expectation = expectation(path);
			Expected entry = new Expected();
			if (Files.exists(expectation)) {
				for (String line : Files.readAllLines(expectation, StandardCharsets.UTF_8)) {
					if (line.isBlank())
						continue;
					if (line.startsWith("!"))
						entry.errors.add(line.substring(1).trim());
					else
						entry.marks.add(line);
				}
			}
			expected.put(path.getFileName().toString(), entry);
		}
		return expected;
	}

	static final class Expected {
		final List<String> marks = new ArrayList<>();
		final List<String> errors = new ArrayList<>();
	}

	@Override
	public PlatformEnvironment environment() {
		return environment;
	}

	@Override
	public LogSink logSink() {
		return (level, message, error) -> {
			recorder.log(level, message);
			if (level.intValue() >= Level.SEVERE.intValue())
				logger.error(message, error);
			else if (level.intValue() >= Level.WARNING.intValue())
				logger.warn(message, error);
			else
				logger.info(message, error);
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
			addonRegistry = ServiceLoader.load(AddonRegistry.class, TestHost.class.getClassLoader())
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
