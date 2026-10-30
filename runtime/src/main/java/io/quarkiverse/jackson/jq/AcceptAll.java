package io.quarkiverse.jackson.jq;

/**
 * The singleton {@link FunctionFilter} that accepts every function.
 */
enum AcceptAll implements FunctionFilter {
    INSTANCE;

    @Override
    public boolean accept(FunctionContext context) {
        return true;
    }
}
