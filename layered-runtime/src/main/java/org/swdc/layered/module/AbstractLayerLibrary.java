package org.swdc.layered.module;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.swdc.layered.def.PlatformModule;
import org.swdc.layered.pointers.Allocator;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public abstract class AbstractLayerLibrary {

    private static final List<String> arch64 = Arrays.asList(
            "amd64","x64","x86_64"
    );

    private Map<String, File> loadedModules = new HashMap<>();

    private Map<String, LoadDescriptor> loadedLoads = new HashMap<>();

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
            InputStream stream = getClass().getResourceAsStream(resourcePrefix + "/metadata.json");
            LibraryDescriptor libDesc = mapper.readValue(stream, LibraryDescriptor.class);
            List<LoadDescriptor> descriptors = libDesc.getDescriptors();

            boolean reExtract = false;
            File version = new File(extractFolder, (subDir ? getLibraryName() : "") + File.separator + getLibraryName() + ".version");
            if (version.exists()) {
                String theVersion = Files.readString(version.toPath());
                if (!theVersion.equals(libDesc.getLibraryVersion())) {
                    reExtract = true;
                }
            }

            descriptors.sort(Comparator.comparingInt(c -> c.getDep().size()));
            for (LoadDescriptor descriptor : descriptors) {

                String name = descriptor.getFileName();
                File targetFile = new File(target, name);

                if (reExtract || !targetFile.exists()) {
                    InputStream inputStream = getClass().getResourceAsStream(resourcePrefix + "/" + name);
                    if (inputStream == null) {
                        throw new RuntimeException("Unable to find resource " + resourcePrefix + "/" + name);
                    }
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (descriptor.isVmLoad()) {
                    System.load(targetFile.getAbsolutePath());
                } else {
                    loadedLoads.put(descriptor.getName(), descriptor);
                    loadedModules.put(descriptor.getName(),targetFile);
                }

            }

            Files.write(version.toPath(), libDesc.getLibraryVersion().getBytes(StandardCharsets.UTF_8));
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
        LoadDescriptor descriptor = loadedLoads.get(moduleName);
        File file = loadedModules.get(moduleName);
        if (descriptor == null ||file == null) {
            return null;
        }
        List<File> deps = new ArrayList<>();
        collectDeps(descriptor, deps);
        return PlatformModule.load(allocator, file, deps);
    }


    public void collectDeps(LoadDescriptor descriptor, List<File> sorted) {
        for (LoadDescriptor d : descriptor.getDep()) {
            if (d.getDep() != null && !d.getDep().isEmpty()) {
                collectDeps(d, sorted);
            }
            File file = loadedModules.get(d.getName());
            if (file == null || !file.exists()) {
                throw new RuntimeException(new LinkageError("Can not find dep module : " + d.getName()));
            }
            sorted.add(file);
        }

    }

}
