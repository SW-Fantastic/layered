package org.swdc.clang.libtooling.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import freemarker.template.Template;
import org.swdc.clang.libtooling.context.*;
import org.swdc.clang.libtooling.generator.sources.RenderableField;
import org.swdc.clang.libtooling.generator.sources.RenderableFunction;
import org.swdc.clang.libtooling.generator.sources.RenderableSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

public class CXXWrapSourceGenerator extends AbstractSourceGenerator{

    private DescriptorsFactory descriptorsFactory;

    private AtomicInteger generatedIndex = new AtomicInteger(0);

    private CXXMetadata metadata = new CXXMetadata();

    private String prefix;

    public CXXWrapSourceGenerator(String prefix,DescriptorsFactory factory) {
        this.descriptorsFactory = factory;
        this.prefix = prefix;
    }
    

    /**
     * 生成 C++ 包装代码文件
     *
     * <p>遍历所有已解析的头文件，为每个头文件生成对应的 C++ 包装头文件和源文件。
     * 生成的代码包含所解析函数声明及其实现，用于提供 JNI 调用的原生实现。</p>
     *
     * @param includeDir 生成的 C++ 头文件输出目录
     * @param sourceDir 生成的 C++ 源文件输出目录
     * @throws IllegalArgumentException 当 includeDir 或 sourceDir 为 null 时抛出
     * @throws RuntimeException 当模板处理或文件写入失败时抛出
     */
    public void generate(File includeDir, File sourceDir) {

        if (sourceDir == null || includeDir == null) {
            throw new IllegalArgumentException("sourceFile or headerFile is null");
        }

        generateAPISource(includeDir, sourceDir);
        generateAccessorSource(includeDir, sourceDir);
        generateMetadata(includeDir, sourceDir);

    }

    /**
     * 生成结构体字段访问器（Getter/Setter）的 C++ 包装代码
     *
     * <p>遍历所有已解析的记录类型，为其每个字段生成对应的 Getter 和 Setter 包装函数，
     * 并统一输出到 api_field_accessors.h 头文件与 api_field_accessors.cpp 源文件中，
     * 供 JNI 层通过字段名直接读写原生结构体成员。</p>
     *
     * @param includeDir 生成的 C++ 头文件输出目录
     * @param sourceDir 生成的 C++ 源文件输出目录
     * @throws IllegalArgumentException 当 includeDir 或 sourceDir 为 null 时抛出
     * @throws RuntimeException 当模板处理或文件写入失败时抛出
     */
    private void generateAccessorSource(File includeDir, File sourceDir) {

        if (sourceDir == null || includeDir == null) {
            throw new IllegalArgumentException("sourceFile or headerFile is null");
        }

        String prefix = "api_" + this.prefix + "_accessor_";

        File accessorHeader = new File(includeDir, "api_field_accessors.h");
        File accessorSource = new File(sourceDir, "api_field_accessors.cpp");
        String accessorInclude = accessorSource.toPath().toAbsolutePath().getParent()
                .relativize(accessorHeader.toPath().toAbsolutePath())
                .toString();

        Template getterTemplate = getTemplate("methods/CXXGetter");
        Template getterDeclareTemplate = getTemplate("methods/CXXGetterDeclare");

        Template setterTemplate = getTemplate("methods/CXXSetter");
        Template setterDeclareTemplate = getTemplate("methods/CXXSetterDeclare");

        RenderableSource theSource = new RenderableSource(prefix, accessorInclude);

        for (File file : descriptorsFactory.getParsedHeaders()) {
            String includePath = accessorSource.toPath().toAbsolutePath().getParent()
                    .relativize(file.toPath().toAbsolutePath())
                    .toString();
            theSource.addInclude(includePath);
        }

        for (RecordMetaType recordMetaType : descriptorsFactory.getRecords()) {

            if (recordMetaType.isConst() || recordMetaType.isVolatile()) {
                continue;
            }

            for (Map.Entry<String, AbstractMetaType> fieldEnt : recordMetaType.getFields().entrySet()) {

                int getterIndex = generatedIndex.getAndIncrement();
                int setterIndex = generatedIndex.getAndIncrement();
                RenderableField field = new RenderableField(
                        recordMetaType,
                        fieldEnt.getValue(),
                        fieldEnt.getKey(),
                        prefix,
                        getterIndex,
                        setterIndex
                );

                try {

                    StringWriter getterWriter = new StringWriter();
                    getterTemplate.process(field, getterWriter);

                    StringWriter getterDeclWriter = new StringWriter();
                    getterDeclareTemplate.process(field, getterDeclWriter);

                    StringWriter setterWriter = new StringWriter();
                    setterTemplate.process(field, setterWriter);

                    StringWriter setterDeclareWriter = new StringWriter();
                    setterDeclareTemplate.process(field, setterDeclareWriter);

                    FunctionMetaType getter = descriptorsFactory.getRecordFieldGetter(recordMetaType, fieldEnt.getKey());
                    FunctionMetaType setter = descriptorsFactory.getRecordFieldSetter(recordMetaType, fieldEnt.getKey());

                    theSource.addSource(getter.getMangledName(), getterDeclWriter.toString(), getterWriter.toString());
                    theSource.addSource(setter.getMangledName(), setterDeclareWriter.toString(), setterWriter.toString());

                    WritableFunction metaGetter = createFunctionMetadata(getter, prefix, getterIndex);
                    metadata.addFunction(metaGetter);

                    WritableFunction metaSetter = createFunctionMetadata(setter, prefix, setterIndex);
                    metadata.addFunction(metaSetter);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            try {

                Template headerTemplate = getTemplate("CXXHeader");
                StringWriter headerWriter = new StringWriter();
                headerTemplate.process(theSource, headerWriter);

                Template sourceTemplate = getTemplate("CXXSource");
                StringWriter sourceWriter = new StringWriter();
                sourceTemplate.process(theSource, sourceWriter);

                FileOutputStream headerOs = new FileOutputStream(accessorHeader);
                headerOs.write(headerWriter.toString().getBytes(StandardCharsets.UTF_8));
                headerOs.close();

                FileOutputStream sourceOs = new FileOutputStream(accessorSource);
                sourceOs.write(sourceWriter.toString().getBytes(StandardCharsets.UTF_8));
                sourceOs.close();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void generateMetadata(File includeDir, File sourceDir) {

        try {


            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            byte[] metaJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(metadata);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzos = new GZIPOutputStream(bos);
            gzos.write(metaJson);
            gzos.close();

            StringBuilder sourceImpl =  new StringBuilder();
            byte[] compressed = bos.toByteArray();
            for (int i = 0; i < compressed.length; i++) {
                String hex = String.format("0x%02x", compressed[i] & 0xFF);
                sourceImpl.append(hex);
                if (i + 1 < compressed.length) {
                    sourceImpl.append(", ");
                }
                if ((i + 1) % 16 == 0) {
                    sourceImpl.append("\n\t\t");
                }
            }


            File metaHeader = new File(includeDir, "api_metadata.h");
            File metaSource = new File(sourceDir, "api_metadata.cpp");

            List<String> includes = new ArrayList<>();
            for (File file : descriptorsFactory.getParsedHeaders()) {
                String includePath = metaSource.toPath().toAbsolutePath().getParent()
                        .relativize(file.toPath().toAbsolutePath())
                        .toString();
                includes.add(includePath);
            }

            String metaIncludePath = metaSource.toPath().toAbsolutePath().getParent()
                    .relativize(metaHeader.toPath().toAbsolutePath())
                    .toString();

            Map<String, Object> context = new HashMap<>();
            context.put("includePath", metaIncludePath);
            context.put("metaData", sourceImpl.toString());
            context.put("metaDataSize", compressed.length);
            context.put("includes", includes);

            Template metaHeaderTemplate = getTemplate("CXXMetadataHeader");
            Template metaSourceTemplate = getTemplate("CXXMetadataSource");

            StringWriter metaHeaderWriter = new StringWriter();
            metaHeaderTemplate.process(context, metaHeaderWriter);

            StringWriter metaSourceWriter = new StringWriter();
            metaSourceTemplate.process(context, metaSourceWriter);

            FileOutputStream metaHeaderOs = new FileOutputStream(metaHeader);
            metaHeaderOs.write(metaHeaderWriter.toString().getBytes(StandardCharsets.UTF_8));
            metaHeaderOs.close();

            FileOutputStream metaSourceOs = new FileOutputStream(metaSource);
            metaSourceOs.write(metaSourceWriter.toString().getBytes(StandardCharsets.UTF_8));
            metaSourceOs.close();

            File metaJsonFile = new File(sourceDir, "api_metadata.json");
            FileOutputStream metaJsonOs = new FileOutputStream(metaJsonFile);
            metaJsonOs.write(metaJson);
            metaJsonOs.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 为已解析的头文件批量生成 C++ 包装 API 的声明头文件与实现源文件。
     * <p>
     * 遍历 {@link DescriptorsFactory} 中解析到的所有头文件，针对每个头文件：
     * 过滤回调类型的函数，使用 FreeMarker 模板生成函数声明与实现，最终分别写入
     * 对应目录下的 {@code api_<文件名>.h} 与 {@code api_<文件名>.cpp} 文件，
     * 若目标文件已存在则先删除再重新写入。
     *
     * @param includeDir 生成的头文件输出目录
     * @param sourceDir  生成的源文件输出目录
     * @throws IllegalArgumentException 当 includeDir 或 sourceDir 为 null 时抛出
     * @throws RuntimeException         当模板处理或文件写入失败时抛出
     */
    private void generateAPISource(File includeDir, File sourceDir) {

        if (sourceDir == null || includeDir == null) {
            throw new IllegalArgumentException("sourceFile or headerFile is null");
        }

        Path sourcePath = sourceDir.toPath().toAbsolutePath();
        Path headerPath = includeDir.toPath().toAbsolutePath();

        Template funcSourceTemplate = getTemplate("methods/CXXFunction");
        Template funcDeclTemplate = getTemplate("methods/CXXFunctionDeclare");


        for (File header : descriptorsFactory.getParsedHeaders()) {

            String fileName = MetaTypeUtils.fileName(header);
            String prefix = "api_" + this.prefix;
            File targetIncludeFile = headerPath.resolve("api_" + fileName + ".h").toFile();
            File targetSourceFile = sourcePath.resolve("api_" + fileName + ".cpp").toFile();

            String includePath = targetSourceFile.toPath().toAbsolutePath().getParent()
                    .relativize(targetIncludeFile.toPath().toAbsolutePath())
                    .toString();

            String sourceIncludePath = targetSourceFile.toPath().toAbsolutePath().getParent()
                    .relativize(header.toPath().toAbsolutePath())
                    .toString();

            RenderableSource source = new RenderableSource(prefix, includePath);
            source.addInclude(sourceIncludePath);

            for (FunctionMetaType metaType : descriptorsFactory.getFunctions(header)) {

                if (metaType.isCallback()) {
                    continue;
                }

                try {

                    int index = generatedIndex.getAndIncrement();
                    RenderableFunction function = new RenderableFunction(
                            prefix, index, metaType
                    );

                    StringWriter sourceWriter = new StringWriter();
                    funcSourceTemplate.process(function,sourceWriter);

                    StringWriter declareWriter = new StringWriter();
                    funcDeclTemplate.process(function,declareWriter);

                    source.addSource(
                            metaType.getMangledName(),
                            declareWriter.toString(),
                            sourceWriter.toString()
                    );

                    WritableFunction meta = createFunctionMetadata(metaType, prefix ,index);
                    metadata.addFunction(meta);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            try {

                StringWriter declareWriter = new StringWriter();
                Template headerTemplate = getTemplate("CXXHeader");
                headerTemplate.process(source, declareWriter);

                if (targetIncludeFile.exists()) {
                    if(!targetIncludeFile.delete()) {
                        throw new RuntimeException("Unable to delete file: " + targetIncludeFile.getAbsolutePath());
                    }
                }

                FileOutputStream headerStream = new FileOutputStream(targetIncludeFile);
                headerStream.write(declareWriter.toString().getBytes(StandardCharsets.UTF_8));
                headerStream.close();

                StringWriter sourceFileWriter = new StringWriter();
                Template sourceTemplate = getTemplate("CXXSource");
                sourceTemplate.process(source, sourceFileWriter);

                if (targetSourceFile.exists()) {
                    if(!targetSourceFile.delete()) {
                        throw new RuntimeException("Unable to delete file: " + targetSourceFile.getAbsolutePath());
                    }
                }

                FileOutputStream sourceStream = new FileOutputStream(targetSourceFile);
                sourceStream.write(sourceFileWriter.toString().getBytes(StandardCharsets.UTF_8));
                sourceStream.close();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

    }


    /**
     * 从函数元数据类型创建WritableFunction对象
     * <p>
     * 该方法从函数元数据中提取参数类型和返回类型，生成用于代码生成的函数元信息对象。
     *
     * @param metaType       函数元数据类型，包含函数的参数和返回类型信息
     * @param functionIndex  函数索引，用于生成唯一的函数标识符
     * @return 包含完整函数元数据的WritableFunction对象
     */
    private WritableFunction createFunctionMetadata(FunctionMetaType metaType, String prefix, int functionIndex) {

        List<MetaType> params = new ArrayList<>();
        List<AbstractMetaType> types = metaType.getParameterTypes();
        for (int index = 0; index < metaType.getParameterTypes().size(); ++index) {
            AbstractMetaType type = types.get(index);
            params.add(type.getRawType());
        }

        MetaType returnType = metaType.getReturnType().getRawType();
        return new WritableFunction(
                prefix + "_" + functionIndex,
                metaType.getMangledName(),
                params,
                returnType
        );

    }


    @Override
    protected List<String> getTemplateNames() {
        return List.of(
                "methods/CXXFunction.ftl",
                "methods/CXXFunctionDeclare.ftl",
                "methods/CXXGetter.ftl",
                "methods/CXXGetterDeclare.ftl",
                "methods/CXXSetter.ftl",
                "methods/CXXSetterDeclare.ftl",
                "CXXFunction.ftl",
                "CXXSource.ftl",
                "CXXHeader.ftl",
                "CXXMetadataHeader.ftl",
                "CXXMetadataSource.ftl"
        );
    }
}
