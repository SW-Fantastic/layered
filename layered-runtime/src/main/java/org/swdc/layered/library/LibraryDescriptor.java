package org.swdc.layered.library;


import java.util.ArrayList;
import java.util.List;

public class LibraryDescriptor {

    private String libraryName;

    private String libraryVersion;

    private List<LibraryResource> descriptors = new ArrayList<>();

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

    public List<LibraryResource> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(List<LibraryResource> descriptors) {
        this.descriptors = descriptors;
    }

}
