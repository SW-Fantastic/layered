package org.swdc.clang.libtooling.context;

import java.util.HashMap;
import java.util.Map;

public class RecordMetaType extends AbstractMetaType {

    private Map<String, AbstractMetaType> fields = new HashMap<>();

    public Map<String, AbstractMetaType> getFields() {
        return fields;
    }

    public void setFields(Map<String, AbstractMetaType> fields) {
        this.fields = fields;
    }

    @Override
    public MetaType getRawType() {
        return MetaType.INTPTR_T;
    }

    @Override
    public String getMangledName() {

        String typeName = "";
        if (getNamespace().isBlank()) {
            typeName = getName();
        } else {
            typeName = getNamespace() + "::" + getName();
        }

        String modifier = MetaTypeUtils.cppMangledModifier(this);
        return modifier + typeName;
    }

    @Override
    public String castFromRawType(String varName, String paramName) {

        if (varName == null || paramName == null || varName.isBlank() || paramName.isBlank()) {
            throw new IllegalArgumentException("varName or paramName cannot be null or blank");
        }

        String target = cppTypeFullName() + "*";
        return target + " " + varName + " = *(reinterpret_cast<" + target + ">(" + paramName + "));";
    }

    /**
     * 将记录（结构体/类）类型的C++对象转换为原始类型（intptr_t）的C++语句生成。
     *
     * <p>该方法在堆上以移动语义构造一个新的对象（std::move），并将其指针重新解释为
     * intptr_t 返回，因此转换结果拥有对象的所有权，需要由调用方负责释放。</p>
     *
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名
     * @return 生成把C++对象转为intptr_t的C++类型转换语句
     * @throws IllegalArgumentException 当 varName 或 paramName 为 null 或空白时抛出
     */
    @Override
    public String castToRawType(String varName, String paramName) {

        if (varName == null || paramName == null || varName.isBlank() || paramName.isBlank()) {
            throw new IllegalArgumentException("varName or paramName cannot be null or blank");
        }
        return "intptr_t " + varName + " = reinterpret_cast<intptr_t>(new " + cppTypeFullName() + "(std::move(" + paramName + ")));";

    }

    /**
     * 将记录（结构体/类）类型的C++对象以借用方式转换为原始类型（intptr_t）的C++语句生成。
     *
     * <p>该方法直接将被转换对象的指针重新解释为 intptr_t，不进行对象拷贝或移动，
     * 因此转换结果仅借用原有对象，不拥有其所有权，调用方无需也不应释放该对象。</p>
     *
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名
     * @return 生成把C++对象指针借用转换为intptr_t的C++类型转换语句
     * @throws IllegalArgumentException 当 varName 或 paramName 为 null 或空白时抛出
     */
    @Override
    public String castToRawTypeBorrowed(String varName, String paramName) {

        if (varName == null || paramName == null || varName.isBlank() || paramName.isBlank()) {
            throw new IllegalArgumentException("varName or paramName cannot be null or blank");
        }
        return "intptr_t " + varName + " = reinterpret_cast<intptr_t>(" + paramName + ");";

    }

    @Override
    public void merge(AbstractMetaType other) {

        if (!(other instanceof RecordMetaType)) {
            throw new IllegalArgumentException("Cannot merge different meta types");
        }

        RecordMetaType otherRecord = (RecordMetaType)other;
        if (!getMangledName().equals(otherRecord.getMangledName())) {
            throw new IllegalArgumentException("Cannot merge different meta types");
        }

        for (String key : fields.keySet()) {
            AbstractMetaType field = fields.get(key);
            if (field == null) {
                this.fields.put(key, otherRecord.fields.get(key));
            }
        }

    }
}
