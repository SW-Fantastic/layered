package org.swdc.layered.module;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "name"
)
public class LoadDescriptor {

    /**
     * 文件名/资源名
     */
    private String fileName;

    /**
     * 该类库的标识符
     */
    private String name;

    /**
     * 是否通过Java加载
     */
    private boolean vmLoad;

    private List<LoadDescriptor> dep;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<LoadDescriptor> getDep() {
        return dep;
    }

    public void setDep(List<LoadDescriptor> dep) {
        this.dep = dep;
    }

    public void setVmLoad(boolean vmLoad) {
        this.vmLoad = vmLoad;
    }

    public boolean isVmLoad() {
        return vmLoad;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
