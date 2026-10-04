package org.swdc.clang.libtooling;

public class ClangLibTools {

    public static native String parseHeaderFiles(
            String workingDir,
            String[] options,
            String[] sourceSet
    );

}
