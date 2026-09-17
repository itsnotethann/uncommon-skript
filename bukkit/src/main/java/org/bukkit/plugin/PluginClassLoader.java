package org.bukkit.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
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
import java.util.Arrays;
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
	private static final String BUKKIT_VALUES = "org/skriptlang/skript/platform/bukkit/BukkitValues";
	private static final String BUKKIT_PRIORITY_DESC = "Lorg/bukkit/event/EventPriority;";
	private static final String PLATFORM_PRIORITY_DESC = "Lorg/skriptlang/skript/lang/event/EventPriority;";
	private static final String SKRIPT = "ch/njol/skript/Skript";
	private static final String REGISTER_ADDON_DESC =
		"(Lorg/bukkit/plugin/java/JavaPlugin;)Lch/njol/skript/SkriptAddon;";
	private static final String BUKKIT_EVENT_DESC = "L" + BUKKIT_EVENT + ";";
	private static final String PLUGIN_MANAGER = "org/bukkit/plugin/PluginManager";
	private static final String SIMPLE_PLUGIN_MANAGER = "org/bukkit/plugin/SimplePluginManager";
	private static final String CALL_EVENT = "callEvent";
	private static final String CALL_EVENT_DESC = "(" + BUKKIT_EVENT_DESC + ")V";
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

	private static final Type OBJECT_TYPE = Type.getType(Object.class);

	private record LambdaBridge(String name, String descriptor, Handle implementation, List<Type> parameters) {
	}

	private static boolean isAdaptableType(Type type) {
		return (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) && isAdaptable(type.getInternalName());
	}

	private static boolean hasAdaptableArgument(Type methodType) {
		for (Type argument : methodType.getArgumentTypes()) {
			if (isAdaptableType(argument))
				return true;
		}
		return false;
	}

	private static Type eraseAdaptableArguments(Type methodType) {
		Type[] arguments = methodType.getArgumentTypes();
		for (int i = 0; i < arguments.length; i++) {
			if (isAdaptableType(arguments[i]))
				arguments[i] = OBJECT_TYPE;
		}
		return Type.getMethodType(methodType.getReturnType(), arguments);
	}

	private static boolean isAdaptable(String type) {
		String element = type;
		while (element.startsWith("["))
			element = element.substring(1);
		if (element.startsWith("L") && element.endsWith(";"))
			element = element.substring(1, element.length() - 1);
		return element.startsWith("org/bukkit/") && !element.startsWith("org/bukkit/event/")
			&& !element.startsWith("org/bukkit/plugin/");
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
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			private final List<String> shimSupertypes = new ArrayList<>();
			private final List<LambdaBridge> lambdaBridges = new ArrayList<>();
			private String className;
			private boolean isInterface;
			private boolean canBridge;

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				className = name;
				isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
				canBridge = !isInterface || (version & 0xFFFF) >= Opcodes.V9;
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
				boolean retyped = !implementsShim;
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
						if (CALL_EVENT.equals(method) && CALL_EVENT_DESC.equals(descriptor)
							&& (PLUGIN_MANAGER.equals(owner) || SIMPLE_PLUGIN_MANAGER.equals(owner))) {
							super.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_ADDONS, CALL_EVENT,
								"(L" + PLUGIN_MANAGER + ";" + PLATFORM_EVENT_DESC + ")V", false);
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
					public void visitLdcInsn(Object value) {
						if (value instanceof Type type && type.getSort() == Type.OBJECT) {
							if (BUKKIT_EVENT.equals(type.getInternalName()))
								value = Type.getObjectType(PLATFORM_EVENT);
							else if (BUKKIT_CANCELLABLE.equals(type.getInternalName()))
								value = Type.getObjectType(PLATFORM_CANCELLABLE);
						}
						super.visitLdcInsn(value);
					}

					@Override
					public void visitTypeInsn(int opcode, String type) {
						if (BUKKIT_CANCELLABLE.equals(type))
							type = PLATFORM_CANCELLABLE;
						else if (retyped && BUKKIT_EVENT.equals(type) && opcode != Opcodes.NEW)
							type = PLATFORM_EVENT;
						else if (retyped && type.startsWith("["))
							type = retype(type);
						if ((opcode == Opcodes.CHECKCAST || opcode == Opcodes.INSTANCEOF) && isAdaptable(type)) {
							super.visitLdcInsn(Type.getObjectType(type));
							if (opcode == Opcodes.CHECKCAST) {
								super.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_VALUES, "adapt",
									"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;", false);
							} else {
								super.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_VALUES, "isInstance",
									"(Ljava/lang/Object;Ljava/lang/Class;)Z", false);
								return;
							}
						}
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
						Object[] constants = new Object[arguments.length];
						for (int i = 0; i < arguments.length; i++)
							constants[i] = retypeConstant(arguments[i]);
						Handle metafactory = (Handle) retypeConstant(bootstrap);
						if (canBridge && "java/lang/invoke/LambdaMetafactory".equals(metafactory.getOwner()) && constants.length >= 3
							&& constants[1] instanceof Handle implementation && constants[2] instanceof Type instantiated
							&& implementation.getTag() != Opcodes.H_NEWINVOKESPECIAL && hasAdaptableArgument(instantiated)) {
							constants[1] = bridgeLambda(implementation);
							constants[2] = eraseAdaptableArguments(instantiated);
						}
						super.visitInvokeDynamicInsn(method, retype(descriptor), metafactory, constants);
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
			private Handle bridgeLambda(Handle implementation) {
				Type implementationType = Type.getMethodType(implementation.getDesc());
				List<Type> parameters = new ArrayList<>();
				if (implementation.getTag() != Opcodes.H_INVOKESTATIC)
					parameters.add(Type.getObjectType(implementation.getOwner()));
				parameters.addAll(Arrays.asList(implementationType.getArgumentTypes()));
				Type[] bridgeParameters = parameters.stream()
					.map(parameter -> isAdaptableType(parameter) ? OBJECT_TYPE : parameter)
					.toArray(Type[]::new);
				String name = "lambda$bukkitBridge$" + lambdaBridges.size();
				String descriptor = Type.getMethodDescriptor(implementationType.getReturnType(), bridgeParameters);
				lambdaBridges.add(new LambdaBridge(name, descriptor, implementation, parameters));
				return new Handle(Opcodes.H_INVOKESTATIC, className, name, descriptor, isInterface);
			}

			@Override
			public void visitEnd() {
				for (LambdaBridge bridge : lambdaBridges) {
					MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
						bridge.name(), bridge.descriptor(), null, null);
					visitor.visitCode();
					int slot = 0;
					for (Type parameter : bridge.parameters()) {
						boolean adaptable = isAdaptableType(parameter);
						visitor.visitVarInsn((adaptable ? OBJECT_TYPE : parameter).getOpcode(Opcodes.ILOAD), slot);
						if (adaptable) {
							visitor.visitLdcInsn(parameter);
							visitor.visitMethodInsn(Opcodes.INVOKESTATIC, BUKKIT_VALUES, "adapt",
								"(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;", false);
							visitor.visitTypeInsn(Opcodes.CHECKCAST, parameter.getInternalName());
						}
						slot += parameter.getSize();
					}
					Handle implementation = bridge.implementation();
					int opcode = switch (implementation.getTag()) {
						case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
						case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
						case Opcodes.H_INVOKESPECIAL -> Opcodes.INVOKESPECIAL;
						default -> Opcodes.INVOKEVIRTUAL;
					};
					visitor.visitMethodInsn(opcode, implementation.getOwner(), implementation.getName(),
						implementation.getDesc(), implementation.isInterface());
					visitor.visitInsn(Type.getMethodType(bridge.descriptor()).getReturnType().getOpcode(Opcodes.IRETURN));
					visitor.visitMaxs(0, 0);
					visitor.visitEnd();
				}
				super.visitEnd();
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
				if (!(plugin instanceof JavaPlugin javaPlugin) || javaPlugin.getLoader() == null || javaPlugin.getLoader() == this)
					continue;

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

		boolean paper = false;
		URL resource = findDescriptor("plugin.yml");
		if (resource == null) {
			resource = findDescriptor("paper-plugin.yml");
			paper = resource != null;
		}
		if (resource == null) {
			Bukkit.getBetterLogger().warn("Found JAR '{}' in the plugins folder without a plugin.yml or paper-plugin.yml file.", file.getName());
			return null;
		}

		try (InputStream stream = resource.openStream()) {
			YamlConfiguration configuration = new YamlConfiguration();
			configuration.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));

			List<String> depend = new ArrayList<>();
			List<String> softDepend = new ArrayList<>();
			if (paper) {
				ConfigurationSection server = configuration.getConfigurationSection("dependencies.server");
				if (server != null) {
					for (String dependency : server.getKeys(false)) {
						if (server.getBoolean(dependency + ".required", true))
							depend.add(dependency);
						else
							softDepend.add(dependency);
					}
				}
			} else {
				if (configuration.get("depend") != null)
					depend.addAll((List<String>) configuration.get("depend"));
				if (configuration.get("softdepend") != null)
					softDepend.addAll((List<String>) configuration.get("softdepend"));
			}

			description = new PluginDescriptionFile(
				(String) configuration.get("name"),
				String.valueOf(configuration.get("version")),
				(String) configuration.get("main"),
				Objects.toString(configuration.get("website"), null),
				depend,
				softDepend
			);

			return description;
		} catch (IOException | ClassCastException | InvalidConfigurationException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	private @Nullable URL findDescriptor(String name) {
		URL resource = findResource(name);
		if (resource == null && file.isDirectory())
			resource = getResource(name);
		return resource;
	}
}
