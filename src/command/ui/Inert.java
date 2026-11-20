package command.ui;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

/**
 * Stand-ins for Discord objects a synthetic message has no real version of.
 * Rest actions can be built on and queued, and do nothing.
 */
final class Inert {

    private Inert() { }

    /** A value for a method that has no meaningful answer on a synthetic message. */
    static Object defaultFor(Class<?> type, Object self, Method m) {
        if (type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        if (type == double.class) return 0d;
        if (type == float.class) return 0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        if (type == java.util.List.class) return Collections.emptyList();
        if (type == java.util.Optional.class) return java.util.Optional.empty();
        if (type == String.class) return "";
        if (type.isInterface()) return of(type);
        return null;
    }

    /** An object of the interface whose every method does nothing and returns another such object. */
    static Object of(Class<?> type) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, m, args) -> m.getName().equals("toString") ? "Inert" : defaultFor(m.getReturnType(), proxy, m));
    }
}
