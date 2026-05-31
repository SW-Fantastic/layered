package org.swdc.layered.module;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.swdc.layered.def.PlatformModule;
import org.swdc.layered.pointers.Allocator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public abstract class AbstractLayerLibrary {

    private static final List<String> arch64 = Arrays.asList(
            "amd64","x64","x86_64"
    );

    private Map<String, File> loadedModules = new HashMap<>();

    private Allocator allocator = null;

    private volatile boolean loaded = false;

    public abstract String getLibraryName();

    public void loadLibrary( File extractFolder, boolean subDir) {

        if (loaded) {
            return;
        }

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
            JavaType metaList = mapper.getTypeFactory().constructParametricType(List.class, LoadDescriptor.class);
            InputStream stream = getClass().getResourceAsStream(resourcePrefix + "/metadata.json");
            List<LoadDescriptor> descriptors = mapper.readValue(stream, metaList);

            descriptors.sort(Comparator.comparingInt(c -> c.getDep().size()));
            for (LoadDescriptor descriptor : descriptors) {

                String name = descriptor.getFileName();
                File targetFile = new File(target, name);

                if (!targetFile.exists()) {
                    InputStream inputStream = getClass().getResourceAsStream(resourcePrefix + "/" + name);
                    if (inputStream == null) {
                        throw new RuntimeException("Unable to find resource " + resourcePrefix + "/" + name);
                    }
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (descriptor.isVmLoad()) {
                    System.load(targetFile.getAbsolutePath());
                } else {
                    loadedModules.put(descriptor.getName(),targetFile);
                }

            }

            loaded = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 在默认的位置加载库。
     * @return 是否成功
     */
    public void loadLibrary() {

        String home = System.getProperty("user.home");
        File libRoot = new File(home, ".layer");
        if (!libRoot.exists()) {
            libRoot.mkdirs();
        }
        loadLibrary(libRoot);

    }

    /**
     * 在指定的目录下查找并且加载库
     * @param extractFolder 指定的库目录
     * @return 是否成功
     */
    public void loadLibrary(File extractFolder) {
        loadLibrary(extractFolder,true);
    }

    public synchronized PlatformModule loadModule(String moduleName) {
        if (!loadedModules.containsKey(moduleName)) {
            return null;
        }
        if (allocator == null) {
            allocator = new Allocator();
        }
        File file = loadedModules.get(moduleName);
        return PlatformModule.load(allocator, file);
    }


}
