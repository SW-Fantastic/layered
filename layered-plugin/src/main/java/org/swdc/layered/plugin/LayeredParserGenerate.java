package org.swdc.layered.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Inject
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ObjectMapper mapper = new ObjectMapper();
        File configure = new File(project.getBasedir(), "layered.json");
        if (!configure.exists()) {
            throw new MojoExecutionException("No layered.json file found, add the configure file in project root.");
        }

        try {
            PlatformConfigure platformConfigure = mapper.readValue(configure,PlatformConfigure.class);
            ParserConfigure parserConfigure = platformConfigure.getConfigure();

            StructSourceWriter writer = new StructSourceWriter();
            FunctionSourceWriter functionWriter = new FunctionSourceWriter();

            List<File> headerList = new ArrayList<>();
            List<File> includesList = new ArrayList<>();
            for (String includeDir: parserConfigure.getIncludeDirs()) {
                for (String header: parserConfigure.getHeaders()) {
                    File headerFile = new File(includeDir, header);
                    if (headerFile.exists() && headerFile.isFile()) {
                        headerList.add(headerFile);
                    }
                }
                if(includeDir.startsWith("/")) {
                    includesList.add(new File(includeDir));
                } else {
                    includesList.add(new File(project.getBasedir(), includeDir));
                }
            }

            CLangParser parser = new CLangParser(parserConfigure.getParameters(), includesList);
            parser.addHeaders(headerList.toArray(new File[0]));
            parser.parse();

            NativeProjectWriter projectWriter = new NativeProjectWriter(platformConfigure.getName(),new File(project.getBasedir(), "layered"));
            for (Map.Entry<String,String> entry:  parserConfigure.getLibraries().entrySet()) {
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
        } catch (Exception e) {
            throw new MojoExecutionException("unknown problem : ", e);
        }




    }

}
