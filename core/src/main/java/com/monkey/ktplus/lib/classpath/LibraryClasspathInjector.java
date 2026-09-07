package com.monkey.ktplus.lib.classpath;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class LibraryClasspathInjector {
    private static final @Nullable Object UNSAFE = findUnsafe();
    private static final MethodHandle ADD_URL = resolveAddUrlHandle();

    private final ClassLoader classLoader;
    private final @Nullable MethodHandle boundAddUrl;

    public LibraryClasspathInjector(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        if (classLoader instanceof URLClassLoader) {
            this.boundAddUrl = ADD_URL.bindTo(classLoader);
        } else {
            this.boundAddUrl = null;
        }
    }

    public void addJar(Path jar) throws Exception {
        Objects.requireNonNull(jar, "jar");
        URL url = jar.toUri().toURL();

        if (tryPublicAddUrl(classLoader, url)) {
            return;
        }
        if (boundAddUrl != null) {
            try {
                boundAddUrl.invokeExact(url);
                return;
            } catch (Throwable t) {
                throw wrap(t, jar);
            }
        }

        ClassLoader parent = classLoader.getParent();
        if (parent != null && tryPublicAddUrl(parent, url)) {
            return;
        }
        if (parent instanceof URLClassLoader) {
            try {
                ADD_URL.bindTo(parent).invokeExact(url);
                return;
            } catch (Throwable t) {
                throw wrap(t, jar);
            }
        }

        throw new IllegalStateException(
                "Unable to inject library into classpath (unsupported ClassLoader: "
                        + classLoader.getClass().getName()
                        + "): "
                        + jar);
    }

    private static MethodHandle resolveAddUrlHandle() {
        try {
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            try {
                openUrlClassLoaderModule();
                addUrl.setAccessible(true);
                return MethodHandles.lookup().unreflect(addUrl);
            } catch (Exception reflectiveFailure) {
                if (UNSAFE != null) {
                    return privilegedUnreflect(addUrl);
                }
                throw new IllegalStateException(
                        "Cannot access URLClassLoader#addURL. "
                                + "On Java 16+ add --add-opens java.base/java.net=ALL-UNNAMED "
                                + "or use a Paper PluginLoader.",
                        reflectiveFailure);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static boolean tryPublicAddUrl(ClassLoader loader, URL url) throws Exception {
        for (String methodName : new String[] {"addURL", "addJar", "append"}) {
            try {
                Method method = loader.getClass().getMethod(methodName, URL.class);
                method.invoke(loader, url);
                return true;
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Method method = loader.getClass().getMethod(methodName, File.class);
                method.invoke(loader, new File(url.toURI()));
                return true;
            } catch (NoSuchMethodException | IllegalArgumentException ignored) {
            }
        }
        return false;
    }

    private static Exception wrap(Throwable t, Path jar) {
        if (t instanceof Exception) {
            return (Exception) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new IllegalStateException("Failed to inject " + jar, t);
    }

    private static void openUrlClassLoaderModule() throws Exception {
        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModule = Class.class.getMethod("getModule");
        Method addOpens = moduleClass.getMethod("addOpens", String.class, moduleClass);
        Object urlClassLoaderModule = getModule.invoke(URLClassLoader.class);
        Object thisModule = getModule.invoke(LibraryClasspathInjector.class);
        addOpens.invoke(urlClassLoaderModule, "java.net", thisModule);
    }

    private static MethodHandle privilegedUnreflect(Method method) throws Exception {
        Object unsafe = UNSAFE;
        if (unsafe == null) {
            throw new IllegalStateException("sun.misc.Unsafe is unavailable");
        }
        Method staticFieldBase = unsafe.getClass().getMethod("staticFieldBase", Field.class);
        Method staticFieldOffset = unsafe.getClass().getMethod("staticFieldOffset", Field.class);
        Method getObject = unsafe.getClass().getMethod("getObject", Object.class, long.class);

        for (Field trustedLookup : MethodHandles.Lookup.class.getDeclaredFields()) {
            if (trustedLookup.getType() != MethodHandles.Lookup.class
                    || !Modifier.isStatic(trustedLookup.getModifiers())
                    || trustedLookup.isSynthetic()) {
                continue;
            }
            try {
                Object base = staticFieldBase.invoke(unsafe, trustedLookup);
                long offset = (Long) staticFieldOffset.invoke(unsafe, trustedLookup);
                MethodHandles.Lookup lookup = (MethodHandles.Lookup) getObject.invoke(unsafe, base, offset);
                if (lookup != null) {
                    return lookup.unreflect(method);
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Cannot obtain privileged MethodHandles.Lookup");
    }

    private static @Nullable Object findUnsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            for (Field field : unsafeClass.getDeclaredFields()) {
                if (field.getType() != unsafeClass || !Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object unsafe = field.get(null);
                    if (unsafe != null) {
                        return unsafe;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }
}
