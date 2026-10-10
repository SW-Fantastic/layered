package org.swdc.layered.library;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class LayerLibrary {

    private static final List<String> arch64 = Arrays.asList(
            "amd64","x64","x86_64"
    );

    private boolean loaded = false;

    private List<LibraryResource> descriptors = new ArrayList<>();

    private Map<String, File> resolvedLibraries = new HashMap<>();

    private String libraryName;

    public LayerLibrary(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getLibraryName() {
        return libraryName;
    }

    /**
     * 解析并释放当前平台对应的本地库资源。
     *
     * <p>根据运行时操作系统与 CPU 架构定位 classpath 中的元数据（metadata.json），
     * 将其中声明的类库文件释放到指定的解压目录；当元数据中的库版本与已释放版本不一致时，
     * 会重新释放全部资源。JNI 库会被立即加载，普通类库则记录路径供运行时按需加载。</p>
     *
     * <p>该方法具备幂等性：一旦成功加载，后续调用将直接返回。</p>
     *
     * @param extractFolder 本地库的解压目标根目录
     * @param subDir 是否在目标根目录下再建立以库名命名的子目录
     */
    public void resolve(Class clazz, File extractFolder, boolean subDir) {

        if (clazz == null) {
            throw new NullPointerException("parameter Class cannot be null");
        }

        if (loaded) {
            return;
        }

        descriptors.clear();
        resolvedLibraries.clear();

        File target = new File(extractFolder, subDir ? getLibraryName() : "");
        if (!target.exists()) {
            target.mkdirs();
        }

        String osName = System.getProperty("os.name").trim().toLowerCase();
        String osArch = System.getProperty("os.arch");
        if (arch64.contains(osArch.toLowerCase())) {
            osArch = "x86_64";
        }
        if (osName.contains("win")) {
            osName = "windows";
        } else if (osName.contains("linux")) {
            osName = "linux";
        } else if (osName.contains("mac")) {
            osName = "macos";
        }

        String resourcePrefix = osName + "-" + osArch;
        try {

            ObjectMapper mapper = new ObjectMapper();
            InputStream stream = clazz.getResourceAsStream(resourcePrefix + "/metadata.json");
            LibraryDescriptor libDesc = mapper.readValue(stream, LibraryDescriptor.class);
            List<LibraryResource> descriptors = libDesc.getDescriptors();

            boolean reExtract = false;
            File version = new File(extractFolder, (subDir ? getLibraryName() : "") + File.separator + getLibraryName() + ".version");
            if (version.exists()) {
                String theVersion = Files.readString(version.toPath());
                if (!theVersion.equals(libDesc.getLibraryVersion())) {
                    reExtract = true;
                }
            }

            descriptors.sort(Comparator.comparingInt(c -> c.getDep().size()));
            for (LibraryResource descriptor : descriptors) {

                String name = descriptor.getFileName();
                File targetFile = new File(target, name);

                if (reExtract || !targetFile.exists()) {
                    InputStream inputStream = clazz.getResourceAsStream(resourcePrefix + "/" + name);
                    if (inputStream == null) {
                        throw new RuntimeException("Unable to find resource " + resourcePrefix + "/" + name);
                    }
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (descriptor.isVmLoad()) {
                    // 此参数指示该库是一个JNI库，需要立即加载。
                    System.load(targetFile.getAbsolutePath());
                } else {
                    // 普通类库，该类库初始化的时候通过运行时的API加载。
                    resolvedLibraries.put(descriptor.getName(), targetFile);
                }

            }

            this.descriptors = descriptors;
            Files.write(version.toPath(), libDesc.getLibraryVersion().getBytes(StandardCharsets.UTF_8));
            loaded = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<LibraryResource> getDescriptors() {
        return Collections.unmodifiableList(descriptors);
    }

    public File getResolvedLibrary(String descriptorName) {
        return resolvedLibraries.get(descriptorName);
    }

}
