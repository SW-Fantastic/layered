package org.swdc.clang.framework.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.swdc.clang.framework.FileUtils;
import org.swdc.layered.def.WritableFunction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NativeProjectWriter {

    private String projectName;

    private List<File> headerFiles = new ArrayList<>();

    private Map<String,File> libraries = new HashMap<>();

    private List<String> linkLibraries = new ArrayList<>();

    private Map<String, WritableFunction> metadata = new HashMap<>();

    private File sourceRoot;

    public NativeProjectWriter(String projectName, File root) {
        this.sourceRoot = root;
        this.projectName = projectName;
    }

    public void addLibraryHeader(File header) {
        headerFiles.add(header);
    }

    public List<File> getHeaderFiles() {
        return headerFiles;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void addLibrary(String libName, File libraryRoot) {

        List<String> systems = Arrays.asList("windows", "linux", "macos");
        List<String> arches = Arrays.asList("x86_64", "x64", "x86", "arm64", "aarch64");

        try {
            for (String system : systems) {
                for (String arch : arches) {
                    File lib = new File(libraryRoot, system + "-" + arch + "/");
                    if (!lib.exists() || !lib.isDirectory()) {
                        continue;
                    }
                    boolean verified = Files.list(lib.toPath()).map(Path::toString).map(String::toLowerCase)
                            .allMatch(s -> s.endsWith("dll") ||
                                    s.endsWith("so") ||
                                    s.endsWith("dylib") ||
                                    s.endsWith("a") ||
                                    s.endsWith("framework") ||
                                    s.endsWith("lib")
                            );
                    if (!verified) {
                        throw new RuntimeException("Library " + lib.getAbsolutePath() + " is invalid.");
                    }
                }
            }

            libraries.put(libName,libraryRoot);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public void linkLibrary(String library) {
        linkLibraries.add(library);
    }

    private String createCMakeList() {

        StringBuilder result = new StringBuilder();
        result.append("cmake_minimum_required(VERSION 3.15)\n");
        result.append("project(").append(projectName).append(")\n");
        result.append("file(GLOB _SRC src/*.cpp)\n");
        result.append("include_directories(ext)\n");
        result.append("add_library(lib").append(projectName).append(" SHARED ${_SRC} )\n");

        result.append("if(CMAKE_SYSTEM_PROCESSOR MATCHES \"amd64|x86_64|AMD64\")\n");
        result.append("    set(ARCH \"x86_64\")\n");
        result.append("elseif(CMAKE_SYSTEM_PROCESSOR MATCHES \"aarch64|arm64\")\n");
        result.append("    set(ARCH \"aarch64\")\n");
        result.append("else()\n");
        result.append("    message(FATAL_ERROR \"Unsupported architecture: ${CMAKE_SYSTEM_PROCESSOR}\")\n");
        result.append("endif()\n");

        result.append("string(TOLOWER \"${CMAKE_SYSTEM_NAME}\" OS_NAME)\n");
        result.append("if(OS_NAME MATCHES \"darwin\")\n");
        result.append("    set(OS_NAME \"macos\")\n");
        result.append("endif()\n");

        if (!linkLibraries.isEmpty()) {
            for (String lib : linkLibraries) {
                result.append("find_library(_").append(lib).append(" NAMES ").append(lib).append(" PATHS \"${CMAKE_SOURCE_DIR}/lib/").append(lib).append("/${OS_NAME}-${ARCH}\")\n");
                result.append("if(NOT _").append(lib).append(")\n");
                result.append("  message(FATAL_ERROR \"library ").append(lib).append(" not found at path ${CMAKE_SOURCE_DIR}/lib/").append(lib).append("/${OS_NAME}-${ARCH} \")\n");
                result.append("endif()\n");
            }
            result.append("target_link_libraries(lib").append(projectName).append(" PRIVATE ");
            for (String lib : linkLibraries) {
                result.append("${_").append(lib).append("} ");
            }
            result.append(")\n");
        }

        return result.toString();

    }

    public void createProject() {

        File rootFolder = sourceRoot;
        if (rootFolder.exists() && !rootFolder.isDirectory()) {
            throw new RuntimeException("Source root is not a directory");
        } else if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File thirdPartInclude = new File(rootFolder, "ext");
        if (!thirdPartInclude.exists()) {
            thirdPartInclude.mkdirs();
        }

        File srcFolder = new File(rootFolder, "src");
        if (!srcFolder.exists()) {
            srcFolder.mkdirs();
        }

        File includeFolder = new File(rootFolder, "include");
        if (!includeFolder.exists()) {
            includeFolder.mkdirs();
        }

        File libraryFolder = new File(rootFolder, "lib");
        if (!libraryFolder.exists()) {
            libraryFolder.mkdirs();
        }

        try {

            for (File header : headerFiles) {
                if (header.isDirectory()) {
                    FileUtils.copyFolder(header, thirdPartInclude);
                } else {
                    Files.copy(header.toPath(), new File(thirdPartInclude, header.getName()).toPath());
                }
            }

            for (Map.Entry<String, File> lib : libraries.entrySet()) {
                String libName = lib.getKey();
                File libRoot = lib.getValue();
                File copyTarget = new File(libraryFolder, libName);
                if (!copyTarget.exists()) {
                    copyTarget.mkdirs();
                }
                FileUtils.copyFolder(libRoot, copyTarget);
            }

            File cmakeList = new File(rootFolder, "CMakeLists.txt");
            Files.write(cmakeList.toPath(), createCMakeList().getBytes());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeSource(File parsedHeader, SourceContext ctx) {

        File rootFolder = sourceRoot;
        if (rootFolder.exists() && !rootFolder.isDirectory()) {
            throw new RuntimeException("Source root is not a directory");
        } else if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        File thirdPartInclude = new File(rootFolder, "ext");
        if (!thirdPartInclude.exists()) {
            thirdPartInclude.mkdirs();
        }

        File srcFolder = new File(rootFolder, "src");
        if (!srcFolder.exists()) {
            srcFolder.mkdirs();
        }

        File cmakeList = new File(rootFolder, "CMakeLists.txt");
        try {

            Files.write(cmakeList.toPath(), createCMakeList().getBytes());
            for (File header : headerFiles) {
                if (header.isDirectory()) {
                    FileUtils.copyFolder(header, thirdPartInclude);
                } else {
                    Files.copy(header.toPath(), new File(thirdPartInclude, header.getName()).toPath());
                }
            }

            int subfixIdx = parsedHeader.getName().lastIndexOf('.');
            String fileName = parsedHeader.getName().substring(0, subfixIdx);
            fileName ="Api" + fileName.substring(0,1).toUpperCase() + fileName;

            File includeFolder = new File(rootFolder, "include");
            StringBuilder header = new StringBuilder();
            header.append("#ifndef ").append(fileName).append("_H\n");
            header.append("#define ").append(fileName).append("_H\n");
            header.append("#include <stdint.h>\n");
            header.append("#include \"../include/metadata.h\"\n");

            header.append("extern \"C\" {\n");
            header.append(ctx.createHeaderSource("LayerAPI"));
            header.append("}\n");
            header.append("#endif");
            Files.write(new File(includeFolder, fileName + ".h").toPath(), header.toString().getBytes());

            StringBuilder source = new StringBuilder();
            source.append("#include \"../include/").append(fileName).append(".h").append("\"\n");
            source.append("#include \"../ext/").append(parsedHeader.getName()).append("\"\n");
            source.append("extern \"C\" {\n");
            source.append(ctx.createSource());
            source.append("}\n");

            Files.write(new File(srcFolder, fileName + ".cpp").toPath(), source.toString().getBytes());
            metadata.putAll(ctx.getMetadata());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void writeEntryPoint() {
        try {

            StringBuilder source = new StringBuilder();
            source.append("#ifndef _API_META\n");
            source.append("#define _API_META\n");
            source.append("#ifdef _WIN32\n");
            source.append("\t#define LayerAPI __declspec(dllexport)\n");
            source.append("#else\n");
            source.append("\t#define LayerAPI __attribute__((visibility(\"default\")))\n");
            source.append("#endif\n");

            source.append("extern \"C\" const LayerAPI unsigned char* getMetaData();\n");
            source.append("extern \"C\" const LayerAPI int getMetaDataSize();\n");
            source.append("#endif");

            File includeFolder = new File(sourceRoot, "include");
            Files.write(new File(includeFolder, "metadata.h").toPath(), source.toString().getBytes());

            File srcFolder = new File(sourceRoot, "src");
            StringBuilder sourceImpl = new StringBuilder();
            sourceImpl.append("#include \"../include/metadata.h\"\n");
            sourceImpl.append("const unsigned char* getMetaData() {\n");
            sourceImpl.append("\tstatic const unsigned char metaData[] = {\n");
            ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
            byte[] metaDataBytes = mapper.writeValueAsBytes(metadata);
            for (int i = 0; i < metaDataBytes.length; i++) {
                String hex = String.format("0x%02x", metaDataBytes[i] & 0xFF);
                sourceImpl.append(hex);
                if (i + 1 < metaDataBytes.length) {
                    sourceImpl.append(", ");
                }
                if ((i + 1) % 16 == 0) {
                    sourceImpl.append("\n\t\t");
                }
            }
            sourceImpl.append("\t};\n");
            sourceImpl.append("\treturn metaData;\n");
            sourceImpl.append("}\n");

            sourceImpl.append("const int getMetaDataSize(){ \n")
                    .append("\treturn ").append(metaDataBytes.length).append(";\n")
                    .append("}\n");
            Files.write(new File(srcFolder, "metadata.cpp").toPath(), sourceImpl.toString().getBytes());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
