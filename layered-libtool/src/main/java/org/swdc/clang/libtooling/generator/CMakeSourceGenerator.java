package org.swdc.clang.libtooling.generator;

import freemarker.template.Template;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CMake 工程生成器，用于创建 CMake 项目结构及配置文件。
 */
public class CMakeSourceGenerator extends AbstractSourceGenerator {

    /**
     * 项目目录，用于存放生成的 CMake 工程结构。
     */
    private final File projectDir;

    /**
     * 项目名称，用于命名生成的 CMake 配置文件。
     */
    private final String projectName;

    /**
     * 库名称列表，用于在 CMakeLists.txt 中指定链接的库。
     */
    private final List<String> libraryNames = new ArrayList<>();

    /**
     * 库搜索目录，用于在 CMakeLists.txt 中指定库的搜索路径。
     */
    private final List<String> librarySearchPaths = new ArrayList<>();

    /**
     * 头文件包含目录，用于在 CMakeLists.txt 中指定头文件的搜索路径。
     */
    private final List<String> includes = new ArrayList<>();

    public CMakeSourceGenerator(File projectDir, String projectName) {
        this.projectDir = projectDir;
        this.projectName = projectName;
    }

    public File getSourceRoot() {
        return new File(projectDir, "src");
    }

    public File getIncludeDir() {
        return new File(projectDir, "include");
    }

    /**
     * 生成 CMake 工程目录结构及 CMakeLists.txt 文件。
     *
     * <p>处理流程：</p>
     * <ul>
     *   <li>若工程目录不存在则递归创建；</li>
     *   <li>删除已存在的 CMakeLists.txt 以便重新生成；</li>
     *   <li>创建 include、libs、src 三个子目录（已存在时跳过）；</li>
     *   <li>加载 CMakeLists 模板并以当前对象作为数据模型渲染输出。</li>
     * </ul>
     *
     * @throws RuntimeException 当目录创建失败或模板处理过程中发生异常时抛出
     */
    public void generateCMakeProject() {

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            if(!projectDir.mkdirs()) {
                throw new RuntimeException("Unable to create project directory");
            }
        }

        File cmakeLists = new File(projectDir, "CMakeLists.txt");
        if (cmakeLists.exists() && !cmakeLists.delete()) {
            throw new RuntimeException("Unable to delete CMakeLists.txt file");
        }

        try {

            File includeDir = new File(projectDir, "include");
            File srcDir = new File(projectDir, "src");
            File libsDir = new File(projectDir, "libs");

            if (!includeDir.exists() && !includeDir.mkdir()) {
                throw new RuntimeException("Unable to create include directory");
            }
            if (!libsDir.exists() && !libsDir.mkdir()) {
                throw new RuntimeException("Unable to create libs directory");
            }
            if (!srcDir.exists() && !srcDir.mkdir()) {
                throw new RuntimeException("Unable to create src directory");
            }

            Template template = getTemplate("CMakeLists");
            template.process(this, new FileWriter(cmakeLists));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 添加头文件包含目录。
     * 校验目录是否存在且为目录，若位于项目目录下则记录相对路径，否则记录绝对路径。
     *
     * @param includeDirectory 待添加的包含目录
     */
    public void addIncludeDirectory(File includeDirectory) {

        if (!includeDirectory.exists() || !includeDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid include directory: " + includeDirectory.getAbsolutePath());
        }

        if (includeDirectory.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
            // 如果是项目目录下的包含路径，则使用相对路径
            String includeDir = projectDir.toPath().relativize(includeDirectory.toPath()).toString();
            includes.add(includeDir);
        } else {
            // 否则使用绝对路径
            includes.add(includeDirectory.getAbsolutePath());
        }

    }

    /**
     * 添加库搜索目录。
     * 校验目录是否存在且为目录，若位于项目目录下则记录相对路径，否则记录绝对路径。
     *
     * @param librarySearchDirectory 待添加的库搜索目录
     */
    public void addLibrarySearchDirectory(File librarySearchDirectory) {

        if (!librarySearchDirectory.exists() || !librarySearchDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid library search directory: " + librarySearchDirectory.getAbsolutePath());
        }

        if (librarySearchDirectory.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
            // 如果是项目目录下的库搜索路径，则使用相对路径
            String libSearchDir = projectDir.toPath().relativize(librarySearchDirectory.toPath()).toString();
            librarySearchPaths.add(libSearchDir);
        } else {
            // 否则使用绝对路径
            librarySearchPaths.add(librarySearchDirectory.getAbsolutePath());
        }

    }

    /**
     * 获取头文件包含目录列表
     *
     * @return 头文件包含目录列表的只读视图（不可修改）
     */
    public List<String> getIncludeDirectories() {
        return Collections.unmodifiableList(includes);
    }

    /**
     * 获取格式化后的头文件包含目录字符串
     * <p>
     * 将路径中的反斜杠统一替换为正斜杠，并为每个路径添加双引号包裹，
     * 最终以换行符拼接为可直接写入 CMakeLists.txt 的字符串。
     *
     * @return 格式化后的包含目录字符串；若包含目录为空则返回空字符串
     */
    public String getIncludeDirectoriesStr() {
        if (includes.isEmpty()) {
            return "";
        }
        return includes.stream()
                .map(s -> s.replaceAll("\\\\", "/"))
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(" \n"));
    }

    /**
     * 添加库名称。
     * <p>
     * 将库名称转为小写后，若其后缀为已知的库文件后缀（dll、so、dylib、lib、a、framework），
     * 则去除后缀后记录；若不含后缀，则直接记录小写名称；
     * 若含有点号但后缀不在已知范围内，则不做任何处理。
     *
     * @param libraryName 待添加的库名称，可以包含库文件后缀
     */
    public void addLibraryByName(String libraryName) {
        String lowerCase = libraryName.toLowerCase();
        int posDot = lowerCase.lastIndexOf('.');
        if (posDot != -1) {
            List<String> subfixs = Arrays.asList(
                    "dll", "so", "dylib","lib","a","framework"
            );
            String extension = lowerCase.substring(posDot + 1);
            if (subfixs.contains(extension)) {
                libraryNames.add(lowerCase.substring(0, posDot));
            }
        } else {
            libraryNames.add(lowerCase);
        }
    }

    /**
     * 获取库搜索路径列表
     *
     * @return 只读的库搜索路径列表，无法被外部修改
     */
    public List<String> getLibrarySearchPaths() {
        return Collections.unmodifiableList(librarySearchPaths);
    }

    /**
     * 获取库搜索路径的字符串表示
     *
     * 将每个库搜索路径中的反斜杠统一替换为正斜杠，并分别用双引号包裹，
     * 以换行分隔，便于直接用于生成 CMakeLists 中的路径列表。
     *
     * @return 以换行分隔、每项用双引号包裹的库搜索路径字符串；列表为空时返回空字符串
     */
    public String getLibrarySearchPathStr() {
        if (librarySearchPaths.isEmpty()) {
            return "";
        }
        return librarySearchPaths.stream()
                .map(s -> s.replaceAll("\\\\", "/"))
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(" \n"));
    }

    public List<String> getLibraryNames() {
        return Collections.unmodifiableList(libraryNames);
    }
    
    /**
     * 获取库名称的字符串表示
     *
     * 将所有库名称分别用双引号包裹，并以空格分隔，
     * 便于直接用于生成 CMakeLists 中的库链接列表。
     *
     * @return 以空格分隔、每项用双引号包裹的库名称字符串；列表为空时返回空字符串
     */
    public String getLibraryNamesStr() {
        if (libraryNames.isEmpty()) {
            return "";
        }
        return libraryNames.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(" \n"));
    }
    
    public File getProjectDir() {
        return projectDir;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectTargetName() {
        return projectName.trim()
                .replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    protected List<String> getTemplateNames() {
        return List.of(
                "CMakeLists.ftl"
        );
    }
}
