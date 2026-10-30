package io.quarkiverse.jackson.jq;

public final class Utils {

    private Utils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T createInstance(String className) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(
                className,
                true,
                Thread.currentThread().getContextClassLoader());
        return (T) clazz.getDeclaredConstructor().newInstance();
    }
}
