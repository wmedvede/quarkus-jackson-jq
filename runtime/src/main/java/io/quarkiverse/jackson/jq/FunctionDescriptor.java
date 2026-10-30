package io.quarkiverse.jackson.jq;

public class FunctionDescriptor {

    String className;

    String name;

    int nargs;

    public String getName() {
        return name;
    }

    public int getNargs() {
        return nargs;
    }

    public String getClassName() {
        return className;
    }

    public FunctionDescriptor(String name, int nargs, String className) {
        this.name = name;
        this.nargs = nargs;
        this.className = className;
    }

    //VersionRangeSpec version() default @VersionRangeSpec;

}
