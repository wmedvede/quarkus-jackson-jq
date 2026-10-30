package io.quarkiverse.jackson.jq;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

public final class FunctionContext {
    private final FunctionSignature signature;
    private final String locator;

    private FunctionContext(FunctionSignature signature, String locator) {
        this.signature = Objects.requireNonNull(signature, "signature");
        this.locator = Objects.requireNonNull(locator, "locator");
    }

    public static FunctionContext of(FunctionSignature signature, String locator) {
        return new FunctionContext(signature, locator);
    }

    public FunctionSignature getSignature() {
        return signature;
    }

    public String getLocator() {
        return locator;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof FunctionContext that))
            return false;
        return Objects.equals(signature, that.signature)
                && Objects.equals(locator, that.locator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature, locator);
    }

    @Override
    public String toString() {
        return locator + ":" + signature;
    }
}
