package org.swdc.clang.libtooling.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.clang.libtooling.Clang;
import org.swdc.clang.libtooling.context.DescriptorsFactory;
import org.swdc.clang.libtooling.generator.CMakeProjectGenerator;
import org.swdc.clang.libtooling.generator.CMakeSourceGenerator;
import org.swdc.clang.libtooling.generator.CXXWrapSourceGenerator;
import org.swdc.clang.libtooling.metadata.AnalysiserMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class CMakeProjectTest {

    @BeforeAll
    public static void initLibrary() {
        Clang.initialize(new File("./assets"));
    }

    @Test
    public void generateTest() {

        CMakeProjectGenerator projectGenerator = new CMakeProjectGenerator(
                new File("./assets/cmake_test"),
                Collections.emptyList(),
                "project_test"
        );

        projectGenerator.addAPIHeader(new File("../assets/pdfium/include/fpdfview.h"));
        projectGenerator.addAPIHeader(new File("../assets/pdfium/include/fpdf_edit.h"));
        projectGenerator.generate();

    }

}
