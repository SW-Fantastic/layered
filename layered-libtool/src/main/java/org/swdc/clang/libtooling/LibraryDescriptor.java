package org.swdc.clang.libtooling;

import java.util.ArrayList;
import java.util.List;

public class LibraryDescriptor {

    private String libraryName;

    private String libraryVersion;

    private List<NativeDescriptor> descriptors = new ArrayList<>();

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

    public List<NativeDescriptor> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(List<NativeDescriptor> descriptors) {
        this.descriptors = descriptors;
    }
}
