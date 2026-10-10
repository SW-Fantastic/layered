package org.swdc.layered.library;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "name"
)
public class LibraryResource {

    /**
     * 文件名/资源名
     */
    private String fileName;

    /**
     * 该类库的标识符
     */
    private String name;

    private List<LibraryResource> dep;

    private boolean vmLoad;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LibraryResource> getDep() {
        return dep;
    }

    public void setDep(List<LibraryResource> dep) {
        this.dep = dep;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isVmLoad() {
        return vmLoad;
    }

    public void setVmLoad(boolean vmLoad) {
        this.vmLoad = vmLoad;
    }
}
