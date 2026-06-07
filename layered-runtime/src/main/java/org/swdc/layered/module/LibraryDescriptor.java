package org.swdc.layered.module;

import java.util.ArrayList;
import java.util.List;

public class LibraryDescriptor {

    private String libraryName;

    private String libraryVersion;

    private List<LoadDescriptor> descriptors = new ArrayList<>();

    public String getLibraryName() {
        return libraryName;
    }

    public String getLibraryVersion() {
        return libraryVersion;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public void setLibraryVersion(String libraryVersion) {
        this.libraryVersion = libraryVersion;
    }

    public List<LoadDescriptor> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(List<LoadDescriptor> descriptors) {
        this.descriptors = descriptors;
    }
}
