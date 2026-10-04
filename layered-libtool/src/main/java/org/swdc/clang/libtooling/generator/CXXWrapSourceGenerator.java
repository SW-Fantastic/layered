package org.swdc.clang.libtooling.generator;

import freemarker.template.Template;
import org.swdc.clang.libtooling.context.DescriptorsFactory;
import org.swdc.clang.libtooling.context.FunctionMetaType;
import org.swdc.clang.libtooling.context.MetaTypeUtils;
import org.swdc.clang.libtooling.generator.sources.RenderableFunction;
import org.swdc.clang.libtooling.generator.sources.RenderableSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class CXXWrapSourceGenerator extends AbstractSourceGenerator{

    private DescriptorsFactory descriptorsFactory;

    private String prefix;

    public CXXWrapSourceGenerator(String prefix, DescriptorsFactory factory) {
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

        Path sourcePath = sourceDir.toPath().toAbsolutePath();
        Path headerPath = includeDir.toPath().toAbsolutePath();

        Template funcSourceTemplate = getTemplate("CXXFunction");
        Template funcDeclTemplate = getTemplate("CXXFunctionDeclare");


        int generatedIndex = 0;
        for (File header : descriptorsFactory.getParsedHeaders()) {

            String fileName = MetaTypeUtils.fileName(header);
            File targetIncludeFile = headerPath.resolve("api_" + fileName + ".h").toFile();
            File targetSourceFile = sourcePath.resolve("api_" + fileName + ".cpp").toFile();

            String includePath = targetSourceFile.toPath().toAbsolutePath().getParent()
                    .relativize(targetIncludeFile.toPath().toAbsolutePath())
                    .toString();

            RenderableSource source = new RenderableSource(prefix, includePath);


            for (FunctionMetaType metaType : descriptorsFactory.getFunctions(header)) {

                if (metaType.isCallback()) {
                    continue;
                }

                try {

                    RenderableFunction function = new RenderableFunction(
                            prefix, generatedIndex++, metaType
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

                System.out.println(declareWriter);

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

                System.out.println(sourceFileWriter);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

    }

    @Override
    protected List<String> getTemplateNames() {
        return List.of(
                "CXXFunction.ftl",
                "CXXFunctionDeclare.ftl",
                "CXXFunction.ftl",
                "CXXSource.ftl",
                "CXXHeader.ftl"
        );
    }
}
