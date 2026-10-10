package org.swdc.layered.plugin;

import java.util.HashMap;
import java.util.Map;

public class PlatformConfigure {


    private String projectName;

    private Map<String, PlatformSpecified> platforms = new HashMap<>();

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Map<String, PlatformSpecified> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Map<String, PlatformSpecified> platforms) {
        this.platforms = platforms;
    }
}
