package org.swdc.clang.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.swdc.clang.framework.CLangParser;
import org.swdc.clang.framework.ClangContext;
import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.def.NativeFunction;
import org.swdc.clang.framework.def.NativeStructType;
import org.swdc.clang.framework.source.FunctionSourceWriter;
import org.swdc.clang.framework.source.NativeProjectWriter;
import org.swdc.clang.framework.source.SourceContext;
import org.swdc.clang.framework.source.StructSourceWriter;
import org.swdc.clang.framework.source.SourceGenerate;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

public class TestClangParser {

    public static void main(String[] args) {
         testwithComplexC();
         //testwithSimpleC();
        // testWtihVeryComplexC();
    }

    private static void testwithSimpleC() {

        File targetHeader = new File("assets/live2d/include/Live2DCubismCore.h");
        CLangParser parser = new CLangParser(null, null)
                .addHeader(targetHeader);
        parser.parse();
        try {

            SourceGenerate generate = new SourceGenerate();

            NativeProjectWriter projectWriter = new NativeProjectWriter("live2d4j", new File("out/live2d4j"));
            projectWriter.addLibraryHeader(new File("assets/live2d/include"));

            for (File file : parser.getHeaders()) {
                ClangContext context = parser.getContext(file);
                if (context == null) {
                    continue;
                }
                SourceContext sourceContext = generate.createContext();
                StructSourceWriter writer = new StructSourceWriter();
                for (NativeStructType struct : context.getDeclaredStructs()) {
                    writer.createCalls(sourceContext,struct);
                }

                FunctionSourceWriter functionWriter = new FunctionSourceWriter();
                for (NativeFunction function : context.getDeclaredFunctions()) {
                    functionWriter.createCalls(sourceContext,function);
                }

                projectWriter.writeSource(targetHeader, sourceContext);
                System.out.println(sourceContext.createSource());

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void testwithComplexC() {

        File targetHeader = new File("assets/pdfium/include/fpdfview.h");
        File targetHeaderEdit = new File("assets/pdfium/include/fpdf_edit.h");
        File targetSave = new File("assets/pdfium/include/fpdf_save.h");

        CLangParser parser = new CLangParser(Arrays.asList("-v"), Arrays.asList(
                new File("assets/pdfium/include"),
                new File("assets/pdfium/include/cpp")
        )).addHeaders(targetHeader,targetHeaderEdit,targetSave);
        parser.parse();

        try {

            NativeProjectWriter projectWriter = new NativeProjectWriter("pdfium4j", new File("out/pdfium4j"));
            projectWriter.addLibraryHeader(new File("assets/pdfium/include"));
            projectWriter.addLibrary("pdfium", new File("assets/pdfium/dll"));
            projectWriter.linkLibrary("pdfium");
            projectWriter.createProject();

            SourceGenerate generate = new SourceGenerate();
            for (File file : parser.getHeaders()) {

                ClangContext context = parser.getContext(file);
                SourceContext sourceContext = generate.createContext();
                StructSourceWriter writer = new StructSourceWriter();
                for (NativeStructType struct : context.getDeclaredStructs()) {
                    writer.createCalls(sourceContext,struct);
                }

                FunctionSourceWriter functionWriter = new FunctionSourceWriter();
                for (NativeFunction function : context.getDeclaredFunctions()) {
                    functionWriter.createCalls(sourceContext,function);
                }
                projectWriter.writeSource(file, sourceContext);

            }

            projectWriter.writeEntryPoint();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void testWtihVeryComplexC() {
        CLangParser parser = new CLangParser(Arrays.asList("-x","c++","-std=c++17"), Arrays.asList(
                new File("libclang-framework/assets/libtorch/include")
        )).addHeader(new File("libclang-framework/assets/libtorch/include/torch/csrc/jit/api/module.h"));
        parser.parse();
    }

}
