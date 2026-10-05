package org.swdc.clang.libtooling.generator;

import org.swdc.clang.libtooling.Clang;
import org.swdc.clang.libtooling.ClangLibTools;
import org.swdc.clang.libtooling.context.DescriptorsFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CMakeProjectGenerator {

    private CMakeSourceGenerator generator;

    private File sourceDir;
    private File includeDir;

    private List<File> headerFiles = new ArrayList<>();
    private List<String> commands = null;

    public CMakeProjectGenerator(File projectDir, List<String> compilerArgs, String projectName) {

        generator = new CMakeSourceGenerator(projectDir, projectName);
        commands  = compilerArgs;
        sourceDir = generator.getSourceRoot();
        includeDir = generator.getIncludeDir();

    }

    public void addAPIHeader(File headerFile) {
        headerFiles.add(headerFile);
    }

    public void addLibraryDir(File libraryDir) {
        this.generator.addLibrarySearchDirectory(libraryDir);
    }

    public void addLinkedLibrary(String libraryName) {
        this.generator.addLibraryByName(libraryName);
    }

    public void addIncludeDir(File includeDir) {
        this.generator.addIncludeDirectory(includeDir);
    }

    /**
     * 生成 CMake 工程及对应的 C++ 封装源码。
     *
     * <p>首先输出 CMake 工程文件，随后遍历所有已注册的 API 头文件：
     * 解析头文件内容并解析出描述符，再为每个头文件生成 C++ 包装源码。</p>
     */
    public void generate() {

        DescriptorsFactory factory = new DescriptorsFactory();

        this.generator.generateCMakeProject();
        for (File headerFile : headerFiles) {
            factory.resolve(headerFile, Clang.parseHeader(
                    this.generator.getProjectDir(), commands, Arrays.asList(headerFile)
            ));
        }

        CXXWrapSourceGenerator wrapperGenerator = new CXXWrapSourceGenerator("pdfium",factory);
        wrapperGenerator.generate(includeDir, sourceDir);

    }

}
