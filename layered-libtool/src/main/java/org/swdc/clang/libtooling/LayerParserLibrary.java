package org.swdc.clang.libtooling;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LayerParserLibrary {

    private boolean loaded = false;

    private static final List<String> arch64 = Arrays.asList(
            "amd64","x64","x86_64"
    );

    public String getLibraryName() {
        return "layer-parser";
    }

    public void loadLibrary(File extractFolder, boolean subDir) {

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
            List<NativeDescriptor> descriptors = libDesc.getDescriptors();

            boolean reExtract = false;
            File version = new File(extractFolder, (subDir ? getLibraryName() : "") + File.separator + getLibraryName() + ".version");
            if (version.exists()) {
                String theVersion = Files.readString(version.toPath());
                if (!theVersion.equals(libDesc.getLibraryVersion())) {
                    reExtract = true;
                }
            }

            descriptors.sort(Comparator.comparingInt(c -> c.getDep().size()));
            for (NativeDescriptor descriptor : descriptors) {

                String name = descriptor.getFileName();
                File targetFile = new File(target, name);

                if (reExtract || !targetFile.exists()) {
                    InputStream inputStream = getClass().getResourceAsStream(resourcePrefix + "/" + name);
                    if (inputStream == null) {
                        throw new RuntimeException("Unable to find resource " + resourcePrefix + "/" + name);
                    }
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                System.load(targetFile.getAbsolutePath());

            }

            Files.write(version.toPath(), libDesc.getLibraryVersion().getBytes(StandardCharsets.UTF_8));
            loaded = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
