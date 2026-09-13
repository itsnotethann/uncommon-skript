package org.bukkit.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PluginClassLoader extends URLClassLoader {
	private PluginDescriptionFile description;
	private ProtectionDomain protectionDomain;
	private PluginManager pluginManager;
	private File file;

	public PluginClassLoader(PluginManager pluginManager, File file, ClassLoader parent) throws MalformedURLException {
		super(new URL[] { file.toURI().toURL() }, parent);

		this.pluginManager = pluginManager;
		this.file = file;
	}

	private static final String BUKKIT_EVENT = "org/bukkit/event/Event";
	private static final String PLATFORM_EVENT = "org/skriptlang/skript/lang/event/PlatformEvent";
	private static final String BUKKIT_CANCELLABLE = "org/bukkit/event/Cancellable";
	private static final String PLATFORM_CANCELLABLE = "org/skriptlang/skript/lang/event/Cancellable";
	private static final String BUKKIT_CANCELLABLE_DESC = "L" + BUKKIT_CANCELLABLE + ";";
	private static final String PLATFORM_CANCELLABLE_DESC = "L" + PLATFORM_CANCELLABLE + ";";
	private static final String BUKKIT_ADDONS = "org/skriptlang/skript/platform/bukkit/BukkitAddons";
	private static final String PRIORITIES = "org/skriptlang/skript/platform/bukkit/BukkitEventPriorities";
	private static final String BUKKIT_PRIORITY_DESC = "Lorg/bukkit/event/EventPriority;";
	private static final String PLATFORM_PRIORITY_DESC = "Lorg/skriptlang/skript/lang/event/EventPriority;";
	private static final String SKRIPT = "ch/njol/skript/Skript";
	private static final String REGISTER_ADDON_DESC =
		"(Lorg/bukkit/plugin/java/JavaPlugin;)Lch/njol/skript/SkriptAddon;";
	private static final String BUKKIT_EVENT_DESC = "L" + BUKKIT_EVENT + ";";
	private static final String PLATFORM_EVENT_DESC = "L" + PLATFORM_EVENT + ";";

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		URL resource = findResource(name.replace('.', '/') + ".class");
		if (resource == null)
			throw new ClassNotFoundException(name);

		byte[] bytes;
		try (InputStream stream = resource.openStream()) {
			bytes = stream.readAllBytes();
		} catch (IOException exception) {
			throw new ClassNotFoundException(name, exception);
		}

		byte[] remapped = retypeEvents(bytes);

		return defineClass(name, remapped, 0, remapped.length, protectionDomain());
	}

	private static String retype(String descriptor) {
		if (descriptor == null)
			return null;
		return descriptor
			.replace(BUKKIT_EVENT_DESC, PLATFORM_EVENT_DESC)
			.replace(BUKKIT_CANCELLABLE_DESC, PLATFORM_CANCELLABLE_DESC);
	}

	private static Object retypeConstant(Object constant) {
		if (constant instanceof Type type)
			return Type.getType(retype(type.getDescriptor()));
		if (constant instanceof Handle handle) {
			return new Handle(handle.getTag(), handle.getOwner(), handle.getName(),
				retype(handle.getDesc()), handle.isInterface());
		}
		return constant;
	}

	private static Object[] retypeFrame(Object[] entries, int count) {
		if (entries == null)
			return null;
		Object[] retyped = entries.clone();
		for (int i = 0; i < count && i < retyped.length; i++) {
			if (BUKKIT_EVENT.equals(retyped[i]))
				retyped[i] = PLATFORM_EVENT;
			else if (BUKKIT_CANCELLABLE.equals(retyped[i]))
				retyped[i] = PLATFORM_CANCELLABLE;
			else if (retyped[i] instanceof String entry && entry.startsWith("["))
				retyped[i] = retype(entry);
		}
		return retyped;
	}

	private boolean declaredByShim(List<String> shimSupertypes, String name, String descriptor) {
		for (String type : shimSupertypes) {
			try {
				Class<?> shim = Class.forName(type.replace('/', '.'), false, getParent());
				for (Method method : shim.getMethods()) {
					if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor))
						return true;
				}
				for (Class<?> current = shim; current != null; current = current.getSuperclass()) {
					for (Method method : current.getDeclaredMethods()) {
						if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor))
							return true;
					}
				}
			} catch (ClassNotFoundException | LinkageError ignored) {
			}
		}
		return false;
	}

	private byte[] retypeEvents(byte[] bytes) {
		ClassReader reader = new ClassReader(bytes);
		ClassWriter writer = new ClassWriter(0);

		reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			private final List<String> shimSupertypes = new ArrayList<>();

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				if (superName != null && superName.startsWith("org/bukkit/"))
					shimSupertypes.add(superName);
				if (interfaces != null) {
					for (String type : interfaces) {
						if (type.startsWith("org/bukkit/"))
							shimSupertypes.add(type);
					}
				}
				super.visit(version, access, name, signature, superName, interfaces);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				return super.visitField(access, name, retype(descriptor), retype(signature), value);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				boolean implementsShim = declaredByShim(shimSupertypes, name, descriptor);
				String declared = implementsShim ? descriptor : retype(descriptor);
				boolean retyped = !declared.equals(descriptor);
				MethodVisitor delegate = super.visitMethod(access, name, declared,
					implementsShim ? signature : retype(signature), exceptions);
				return new MethodVisitor(Opcodes.ASM9, delegate) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean isInterface) {
						if (BUKKIT_EVENT.equals(owner) && opcode != Opcodes.INVOKESPECIAL) {
							if ("getEventName".equals(method)) {
								super.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_ADDONS, "eventName",
									"(" + PLATFORM_EVENT_DESC + ")Ljava/lang/String;", false);
								return;
							}
							if ("isAsynchronous".equals(method)) {
								super.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_ADDONS, "isAsynchronous",
									"(" + PLATFORM_EVENT_DESC + ")Z", false);
								return;
							}
						}
						if (BUKKIT_CANCELLABLE.equals(owner)) {
							super.visitMethodInsn(Opcodes.INVOKEINTERFACE, PLATFORM_CANCELLABLE, method,
								retype(descriptor), true);
							return;
						}
						if ("getEventPriority".equals(method) && ("()" + BUKKIT_PRIORITY_DESC).equals(descriptor)) {
							super.visitMethodInsn(opcode, owner, method, "()" + PLATFORM_PRIORITY_DESC, isInterface);
							super.visitMethodInsn(Opcodes.INVOKESTATIC, PRIORITIES, "toBukkit",
								"(" + PLATFORM_PRIORITY_DESC + ")" + BUKKIT_PRIORITY_DESC, false);
							return;
						}
						if (SKRIPT.equals(owner) && "registerAddon".equals(method) && REGISTER_ADDON_DESC.equals(descriptor)) {
							super.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_ADDONS, "registerAddon", descriptor, false);
							return;
						}
						if (owner.startsWith("org/bukkit/")) {
							Type[] arguments = Type.getArgumentTypes(descriptor);
							if (retyped && arguments.length > 0
								&& BUKKIT_EVENT.equals(arguments[arguments.length - 1].getInternalName()))
								super.visitTypeInsn(Opcodes.CHECKCAST, BUKKIT_EVENT);
							super.visitMethodInsn(opcode, owner, method, descriptor, isInterface);
							return;
						}
						super.visitMethodInsn(opcode, owner, method, retype(descriptor), isInterface);
					}

					@Override
					public void visitTypeInsn(int opcode, String type) {
						if (BUKKIT_CANCELLABLE.equals(type))
							type = PLATFORM_CANCELLABLE;
						else if (retyped && BUKKIT_EVENT.equals(type) && opcode != Opcodes.NEW)
							type = PLATFORM_EVENT;
						else if (retyped && type.startsWith("["))
							type = retype(type);
						super.visitTypeInsn(opcode, type);
					}

					@Override
					public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
						super.visitMultiANewArrayInsn(retype(descriptor), dimensions);
					}

					@Override
					public void visitFieldInsn(int opcode, String owner, String field, String descriptor) {
						super.visitFieldInsn(opcode, owner, field,
							owner.startsWith("org/bukkit/") ? descriptor : retype(descriptor));
					}

					@Override
					public void visitInvokeDynamicInsn(String method, String descriptor, Handle bootstrap, Object... arguments) {
						Object[] retyped = new Object[arguments.length];
						for (int i = 0; i < arguments.length; i++)
							retyped[i] = retypeConstant(arguments[i]);
						super.visitInvokeDynamicInsn(method, retype(descriptor),
							(Handle) retypeConstant(bootstrap), retyped);
					}

					@Override
					public void visitLocalVariable(String variable, String descriptor, String signature, Label start, Label end, int index) {
						super.visitLocalVariable(variable, retyped ? retype(descriptor) : descriptor,
							retyped ? retype(signature) : signature, start, end, index);
					}

					@Override
					public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
						if (!retyped) {
							super.visitFrame(type, numLocal, local, numStack, stack);
							return;
						}
						super.visitFrame(type, numLocal, retypeFrame(local, numLocal), numStack, retypeFrame(stack, numStack));
					}
				};
			}
		}, 0);

		return writer.toByteArray();
	}

	private synchronized ProtectionDomain protectionDomain() {
		if (protectionDomain == null) {
			CodeSource codeSource = new CodeSource(getURLs()[0], (CodeSigner[]) null);
			protectionDomain = new ProtectionDomain(codeSource, null, this, null);
		}
		return protectionDomain;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClass0(name, resolve, true);
	}

	private Class<?> loadClass0(String name, boolean resolve, boolean checkPlugins) throws ClassNotFoundException {
		try {
			return super.loadClass(name, resolve);
		} catch (ClassNotFoundException ignored) {}

		if (checkPlugins) {
			for (Plugin plugin : pluginManager.getPlugins()) {
				if (!(plugin instanceof JavaPlugin))
					continue;

				JavaPlugin javaPlugin = (JavaPlugin) plugin;

				try {
					Class<?> clazz = javaPlugin.getLoader().loadClass0(name, resolve, false);
					ClassLoader loader = clazz.getClassLoader();

					if (loader instanceof PluginClassLoader) {
						PluginClassLoader pluginLoader = (PluginClassLoader) loader;
						PluginDescriptionFile description = pluginLoader.getDescription();
						String pluginName = description.getName();

						if (description == null)
							return clazz;

						PluginDescriptionFile thisDescription = plugin.getDescription();

						if ((thisDescription.getDepend().contains(pluginName) || thisDescription.getSoftDepend().contains(pluginName)) || thisDescription.getName().equals(pluginName))
							return clazz;

						Bukkit.getBetterLogger().warn("Plugin '{}' loaded class '{}' from non-dependency plugin '{}'.",
							thisDescription.getName(), clazz.getName(), description.getName());
					}
				} catch (ClassNotFoundException ignored) {
				}
			}
		}

		throw new ClassNotFoundException(name);
	}

	public @Nullable PluginDescriptionFile getDescription() {
		if (description != null)
			return description;

		URL resource = findResource("plugin.yml"); // Only searches this loader’s URLs
		if (resource == null && file.isDirectory())
			resource = getResource("plugin.yml");
		if (resource == null) {
			Bukkit.getBetterLogger().warn("Found JAR '{}' in the plugins folder without a plugin.yml file.", file.getName());
			return null;
		}

		try (InputStream stream = resource.openStream()) {
			YamlConfiguration configuration = new YamlConfiguration();
			configuration.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));

			description = new PluginDescriptionFile(
				(String) configuration.get("name"),
				String.valueOf(configuration.get("version")),
				(String) configuration.get("main"),
				Objects.toString(configuration.get("website"), null),
				(List<String>) configuration.get("depend"),
				(List<String>) configuration.get("softdepend")
			);

			return description;
		} catch (IOException | ClassCastException | InvalidConfigurationException exception) {
			exception.printStackTrace();
			return null;
		}
	}
}
