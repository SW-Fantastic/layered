package org.swdc.clang.libtooling.context;

import org.swdc.clang.libtooling.metadata.*;

import java.io.File;
import java.util.*;

/**
 * 这个类的作用是把直接来自lib-tooling的描述符和元数据转换为用于生成Wrapper的描述符和元数据。
 */
public class DescriptorsFactory {

    private Map<File,AnalysiserMetadata> metadata = new HashMap<>();
    private Map<File, Map<String, FunctionMetaType>> resolvedFunction = new HashMap<>();
    private Map<String, FunctionMetaType> accessors =  new HashMap<>();

    private List<File> parsedHeaders = new ArrayList<>();

    private Map<String, RecordMetaType> resolvedRecord = new HashMap<>();
    private Map<String, PointerMetaType> resolvedPointer = new HashMap<>();
    private Map<String, BuiltInMetaType> resolvedBuiltIn = new HashMap<>();

    public List<File> getParsedHeaders() {
        return parsedHeaders;
    }

    /**
     * 解析指定头文件的分析元数据，并将其中的函数描述符转换为元数据类型。
     *
     * <p>解析流程：
     * <ul>
     *   <li>若该头文件已被解析过，则直接返回，避免重复处理</li>
     *   <li>记录已解析的头文件，并缓存其对应的分析元数据</li>
     *   <li>遍历元数据中的所有函数描述符，逐个通过 transform 转换为函数元数据类型</li>
     * </ul>
     *
     * @param headerFile 待解析的头文件
     * @param metadata 该头文件对应的分析元数据
     */
    public void resolve(File headerFile, AnalysiserMetadata metadata) {

        if (parsedHeaders.contains(headerFile)) {
            return;
        }

        parsedHeaders.add(headerFile);
        this.metadata.put(headerFile, metadata);
        for (FunctionDescriptor functionDescriptor : metadata.getFunctions().values()) {
            transform(headerFile, functionDescriptor);
        }

    }

    public List<FunctionMetaType> getFunctions(File headerFile) {
        if (resolvedFunction.containsKey(headerFile)) {
            return List.copyOf(resolvedFunction.get(headerFile).values());
        }
        return Collections.emptyList();
    }

    public List<RecordMetaType> getRecords() {
        return List.copyOf(resolvedRecord.values());
    }

    
    /**
     * 将抽象描述符递归转换为元数据类型
     *
     * <p>根据描述符的具体类型进行转换：
     * <ul>
     *   <li>PointerDescriptor - 转换为指针类型，循环处理多级指针并逐级设置 const/volatile 修饰</li>
     *   <li>BuiltInDescriptor - 转换为内置类型（int、long、short、char、float、double、void 等）</li>
     *   <li>RecordDescriptor - 转换为记录类型，递归解析其所有字段类型</li>
     *   <li>FunctionDescriptor - 转换为函数类型，递归解析返回值和各参数类型</li>
     * </ul>
     * 转换过程中会复用已解析的同名类型（通过 merge 合并），避免重复构建。
     *
     * @param header 描述符所属的头文件，用于定位对应的元数据
     * @param descriptor 待转换的抽象描述符
     * @return 转换后的元数据类型
     * @throws IllegalArgumentException 当头文件未知、描述符类型未知或无法解析的类型/字段/参数时抛出
     */
    private AbstractMetaType transform(File header, AbstractDescriptor descriptor) {

        AnalysiserMetadata metadata = this.metadata.get(header);
        if (metadata == null) {
            throw new IllegalArgumentException("Unknown header file: " + header);
        }

        if (descriptor instanceof PointerDescriptor) {
            PointerMetaType built = null;
            PointerMetaType cursor = null;
            while (descriptor instanceof PointerDescriptor) {

                PointerDescriptor pointerDescriptor = (PointerDescriptor)descriptor;
                if (built == null) {
                    cursor = new PointerMetaType();
                    built = cursor;
                } else {
                    PointerMetaType pointer = new PointerMetaType();
                    cursor.setTargetType(pointer);
                    cursor = pointer;
                }

                cursor.setConst(pointerDescriptor.getConst());
                cursor.setVolatile(pointerDescriptor.getVolatile());
                descriptor = metadata.findDescriptor(pointerDescriptor.getTargetId());

            }

            AbstractMetaType pointee = transform(header,descriptor);
            cursor.setTargetType(pointee);

            if (resolvedPointer.containsKey(cursor.getMangledName())) {
                PointerMetaType exists = resolvedPointer.get(cursor.getMangledName());
                exists.merge(cursor);
                return exists;
            }

            return built;

        } else if (descriptor instanceof BuiltInDescriptor) {

            BuiltInDescriptor builtInDescriptor = (BuiltInDescriptor)descriptor;
            String typeName = builtInDescriptor
                    .getName()
                    .toLowerCase()
                    .trim();

            BuiltInMetaType builtIn = null;
            if (typeName.equals("unsigned int") || typeName.equals("int")) {
                builtIn = new BuiltInMetaType(MetaType.INT);
            } else if (typeName.equals("unsigned long") || typeName.equals("long")) {
                builtIn = new BuiltInMetaType(MetaType.LONG);
            } else if (typeName.equals("unsigned short") || typeName.equals("short")) {
                builtIn = new BuiltInMetaType(MetaType.SHORT);
            } else if (typeName.equals("unsigned char") || typeName.equals("char")) {
                builtIn = new BuiltInMetaType(MetaType.CHAR);
            } else if (typeName.equals("unsigned long long") ||  typeName.equals("long long")) {
                builtIn = new BuiltInMetaType(MetaType.LONG);
            } else if (typeName.equals("float")) {
                builtIn = new BuiltInMetaType(MetaType.FLOAT);
            } else if (typeName.equals("double")) {
                builtIn = new BuiltInMetaType(MetaType.DOUBLE);
            } else if (typeName.equals("void")) {
                builtIn = new BuiltInMetaType(MetaType.VOID);
            } else if (typeName.equals("size_t")) {
                builtIn = new BuiltInMetaType(MetaType.SIZE_T);
            } else {
                throw new IllegalArgumentException("Unknown built-in type: " + typeName);
            }

            builtIn.setName(builtInDescriptor.getName());
            builtIn.setUnsigned(builtInDescriptor.isUnsigned());
            builtIn.setVolatile(builtInDescriptor.getVolatile());
            builtIn.setConst(builtInDescriptor.getConst());
            if (resolvedBuiltIn.containsKey(builtIn.getMangledName())) {
                BuiltInMetaType exists = resolvedBuiltIn.get(builtIn.getMangledName());
                exists.merge(builtIn);
                return exists;
            }
            resolvedBuiltIn.put(builtIn.getMangledName(), builtIn);

            return builtIn;

        } else if (descriptor instanceof RecordDescriptor) {

            RecordDescriptor recordDescriptor = (RecordDescriptor)descriptor;

            RecordMetaType recordMeta = new RecordMetaType();
            recordMeta.setName(recordDescriptor.getName());
            recordMeta.setNamespace(recordDescriptor.getNamespace());
            recordMeta.setConst(recordDescriptor.getConst());
            recordMeta.setVolatile(recordDescriptor.getVolatile());

            Map<String, AbstractMetaType> metaFields = new HashMap<>();
            for (Map.Entry<String,String> field :  recordDescriptor.getFields().entrySet()) {

                AbstractDescriptor fieldType = metadata.findDescriptor(field.getValue());
                if (fieldType == null) {
                    throw new IllegalArgumentException("Unknown field type: " + field.getValue());
                }
                AbstractMetaType metaType = transform(header,fieldType);
                metaFields.put(field.getKey(), metaType);

            }

            recordMeta.setFields(metaFields);
            if (resolvedRecord.containsKey(recordMeta.getMangledName())) {
                RecordMetaType exists = resolvedRecord.get(recordMeta.getMangledName());
                exists.merge(recordMeta);
                return exists;
            }
            resolvedRecord.put(recordMeta.getMangledName(), recordMeta);
            return recordMeta;

        } else if (descriptor instanceof FunctionDescriptor) {

            FunctionDescriptor functionDescriptor = (FunctionDescriptor)descriptor;
            AbstractDescriptor returnType = metadata.findDescriptor(functionDescriptor.getReturnTypeId());
            if (returnType == null) {
                throw new IllegalArgumentException("Unknown function type: " + functionDescriptor.getReturnTypeId());
            }

            List<AbstractMetaType> parameters = new ArrayList<>();
            for (int index = 0; index < functionDescriptor.getParamCount(); index++) {

                String paramType = functionDescriptor.getParamTypeIds().get(index);
                AbstractDescriptor paramTypeDesc =  metadata.findDescriptor(paramType);
                if (paramTypeDesc == null) {
                    throw new IllegalArgumentException("Unknown parameter type: " + paramType);
                }

                AbstractMetaType metaType = transform(header,paramTypeDesc);
                parameters.add(metaType);

            }

            FunctionMetaType functionMeta = new FunctionMetaType();
            functionMeta.setName(functionDescriptor.getName());
            functionMeta.setNamespace(functionDescriptor.getNamespace());
            functionMeta.setConst(functionDescriptor.getConst());
            functionMeta.setVolatile(functionDescriptor.getVolatile());
            functionMeta.setReturnType(transform(header, returnType));
            functionMeta.setParameterTypes(parameters);
            functionMeta.setCallback(functionDescriptor.isCallback());

            for (Map<String, FunctionMetaType> declared: this.resolvedFunction.values()) {
                if (declared.containsKey(functionMeta.getMangledName())) {
                    FunctionMetaType exists = declared.get(functionMeta.getMangledName());
                    exists.merge(functionMeta);
                    return exists;
                }
            }

            Map<String, FunctionMetaType> resolvedFunctions = this.resolvedFunction.get(header);
            if (resolvedFunctions == null) {
                resolvedFunctions = new HashMap<>();
            }
            
            if (resolvedFunctions.containsKey(functionMeta.getMangledName())) {
                FunctionMetaType exists = resolvedFunctions.get(functionMeta.getMangledName());
                exists.merge(functionMeta);
                return exists;
            }
            
            resolvedFunctions.put(functionMeta.getMangledName(), functionMeta);
            this.resolvedFunction.put(header, resolvedFunctions);

            return functionMeta;

        }

        throw new IllegalArgumentException("Unknown descriptor type: " + descriptor.getClass().getName());

    }

    /**
     * 获取（或创建）指定记录字段的 setter 访问器函数元信息。
     * <p>
     * 根据记录类型与字段名构造一个用于写入该字段值的访问器：
     * 函数名为 "set" + 字段名，命名空间为 "记录命名空间::记录名"，
     * 返回类型为 void，参数依次为（记录自身, 字段值），
     * 并继承字段类型的 const / volatile 限定。
     * 若已存在相同修饰名（mangled name）的访问器，则将其与新建元信息合并后返回既有实例。
     *
     * @param record    目标记录的元类型，其字段集合不允许为空
     * @param fieldName 待生成 setter 的字段名称，必须已存在于记录的字段定义中
     * @return 该字段 setter 对应的函数元类型；若访问器已存在则返回合并后的既有实例
     * @throws IllegalArgumentException 当记录未定义任何字段，或字段名称为 null / 不存在时抛出
     */
    public FunctionMetaType getRecordFieldSetter(RecordMetaType record, String fieldName) {

        if (record.getFields().isEmpty()) {
            throw new IllegalArgumentException("No record fields defined");
        }
        if (fieldName == null || !record.getFields().containsKey(fieldName)) {
            throw new IllegalArgumentException("Unknown field name: " + fieldName);
        }

        AbstractMetaType fieldType = record.getFields().get(fieldName);

        FunctionMetaType functionMeta = new FunctionMetaType();
        functionMeta.setName("set" + fieldName);
        functionMeta.setNamespace(record.getNamespace() + "::" + record.getName());
        functionMeta.setReturnType(resolvedBuiltIn.get("void"));
        functionMeta.setConst(fieldType.isConst());
        functionMeta.setVolatile(fieldType.isVolatile());
        functionMeta.setParameterTypes(Arrays.asList(record, fieldType));
        if (accessors.containsKey(functionMeta.getMangledName())) {
            FunctionMetaType exists = accessors.get(functionMeta.getMangledName());
            exists.merge(functionMeta);
            return exists;
        }
        accessors.put(functionMeta.getMangledName(), functionMeta);
        return functionMeta;
        
    }

    /**
     * 获取指定记录类型中某个字段的 getter 函数元数据。
     * <p>
     * 根据记录类型与字段名构造对应的 getter 函数元数据，包括函数名（"get" + 字段名）、
     * 所属命名空间（记录命名空间 :: 记录名）、返回类型以及 const/volatile 限定符，
     * 并将该记录类型作为函数的首个参数。若该访问器已存在，则合并后返回已存在的实例。
     *
     * @param record 记录类型，需包含字段定义
     * @param fieldName 字段名称，不能为 null 且必须存在于记录字段中
     * @return 对应字段的 getter 函数元数据；若已存在同签名访问器则返回合并后的实例
     * @throws IllegalArgumentException 当记录未定义任何字段或字段名不存在时抛出
     */
    public FunctionMetaType getRecordFieldGetter(RecordMetaType record, String fieldName) {

        if (record.getFields().isEmpty()) {
            throw new IllegalArgumentException("No record fields defined");
        }
        if (fieldName == null || !record.getFields().containsKey(fieldName)) {
            throw new IllegalArgumentException("Unknown field name: " + fieldName);
        }

        AbstractMetaType fieldType = record.getFields().get(fieldName);

        FunctionMetaType functionMeta = new FunctionMetaType();
        functionMeta.setName("get" + fieldName);
        functionMeta.setNamespace(record.getNamespace() + "::" + record.getName());
        functionMeta.setReturnType(fieldType);
        functionMeta.setConst(fieldType.isConst());
        functionMeta.setVolatile(fieldType.isVolatile());
        functionMeta.setParameterTypes(Arrays.asList(record));
        if (accessors.containsKey(functionMeta.getMangledName())) {
            FunctionMetaType exists = accessors.get(functionMeta.getMangledName());
            exists.merge(functionMeta);
            return exists;
        }
        accessors.put(functionMeta.getMangledName(), functionMeta);
        return functionMeta;

    }


}
