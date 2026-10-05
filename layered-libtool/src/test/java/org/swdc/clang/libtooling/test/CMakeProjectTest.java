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

        File projectDir = new File("./assets/cmake_test");
        CMakeProjectGenerator projectGenerator = new CMakeProjectGenerator(
                projectDir,
                Collections.emptyList(),
                "project_test"
        );

        projectGenerator.addIncludeDir(new File(projectDir, "libs/pdfium/include"));
        projectGenerator.addLibraryDir(new File(projectDir,"libs/pdfium/libs"));
        projectGenerator.addLinkedLibrary("pdfium");

        projectGenerator.addAPIHeader(new File(projectDir,"libs/pdfium/include/fpdfview.h"));
        projectGenerator.addAPIHeader(new File(projectDir,"libs/pdfium/include/fpdf_edit.h"));

        projectGenerator.generate();

    }

}
