package org.swdc.layered.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.swdc.clang.framework.CLangParser;
import org.swdc.clang.framework.ClangContext;
import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.def.NativeFunction;
import org.swdc.clang.framework.def.NativeStructType;
import org.swdc.clang.framework.source.*;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

@Mojo(name = "gen-native-project")
public class LayeredParserGenerate extends AbstractMojo {

    @Parameter(property = "includes", required = true)
    private String[] includes;

    @Parameter(property = "headers", required = true)
    private String[] headers;

    @Parameter(property = "libraries", required = true)
    private Map<String,String> libraries;

    @Parameter(property = "name")
    private String name;

    private static final List<String> arch64 = Arrays.asList(
            "amd64","x64","x86_64"
    );

    private static final List<String> arm64 = Arrays.asList(
            "aarch64","arm64"
    );


    @Inject
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        StructSourceWriter writer = new StructSourceWriter();
        FunctionSourceWriter functionWriter = new FunctionSourceWriter();

        List<File> headerList = new ArrayList<>();
        List<File> includesList = new ArrayList<>();
        for (String includeDir: includes) {
            for (String header: headers) {
                File headerFile = new File(includeDir, header);
                if (headerFile.exists() && headerFile.isFile()) {
                    headerList.add(headerFile);
                }
            }
            includesList.add(new File(project.getBasedir(), includeDir));
        }



        CLangParser parser = new CLangParser(Arrays.asList("-v"), includesList);
        parser.addHeaders(headerList.toArray(new File[0]));
        parser.parse();

        NativeProjectWriter projectWriter = new NativeProjectWriter(name, new File(project.getBasedir(), "layered"));
        for (Map.Entry<String,String> entry:  libraries.entrySet()) {
            File libDir = new File(project.getBasedir(), entry.getValue());
            projectWriter.addLibrary(entry.getKey(), libDir);
            projectWriter.linkLibrary(entry.getKey());
        }
        for (File file : includesList) {
            projectWriter.addLibraryHeader(file);
        }

        projectWriter.createProject();

        SourceGenerate generate = new SourceGenerate();
        for (File file : parser.getHeaders()) {
            ClangContext context = parser.getContext(file);
            SourceContext sourceContext = generate.createContext();
            for (NativeStructType struct : context.getDeclaredStructs()) {
                writer.createCalls(sourceContext,struct);
            }
            for (NativeFunction function : context.getDeclaredFunctions()) {
                functionWriter.createCalls(sourceContext,function);
            }
            projectWriter.writeSource(file, sourceContext);
        }

        projectWriter.writeEntryPoint();

    }

}
