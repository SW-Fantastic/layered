package org.swdc.layered.plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PlatformConfigure {

    private static final List<String> arch64 = Arrays.asList(
            "amd64","x64","x86_64"
    );

    private static final List<String> arm64 = Arrays.asList(
            "aarch64","arm64"
    );

    private Map<String, ParserConfigure> platforms;

    private String name;

    public Map<String, ParserConfigure> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Map<String, ParserConfigure> platforms) {
        this.platforms = platforms;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParserConfigure getConfigure() {

        String system = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (system.contains("linux")) {
            system = "linux";
        } else if (system.contains("windows")) {
            system = "windows";
        } else if (system.contains("mac")) {
            system = "osx";
        }

        if (arch64.contains(arch)) {
            arch = "x64";
        }
        if (arm64.contains(system)) {
            arch = "aarch64";
        }

        String key = system + "-" + arch;
        if (platforms.containsKey(key)) {
            return platforms.get(key);
        }
        throw new RuntimeException("No such architecture: " + name + "-"  + arch + "supported : "+ String.join(",",platforms.keySet()));

    }

}
