package org.swdc.layered.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.clang.libtooling.Clang;
import org.swdc.clang.libtooling.generator.CMakeProjectGenerator;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

@Mojo(name = "gen-native-project")
public class LayeredParserGenerate extends AbstractMojo {

    private static final Logger log = LoggerFactory.getLogger(LayeredParserGenerate.class);
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

            Clang.initialize(new File(project.getBasedir(), ".layered"));

            PlatformConfigure platformConfigure = mapper.readValue(configure,PlatformConfigure.class);
            File projectDir = new File(project.getBasedir(), "layered");
            if (!projectDir.exists()) {
                projectDir.mkdirs();
            }

            CMakeProjectGenerator projectGenerator = new CMakeProjectGenerator(
                    projectDir,
                    Collections.emptyList(),
                    platformConfigure.getProjectName()
            );

            String os = System.getProperty("os.name").toLowerCase();
            String osKey = "";
            if (os.contains("windows")) {
                osKey = "windows";
            } else if (os.contains("linux")) {
                osKey = "linux";
            } else if (os.contains("mac") || os.contains("darwin")) {
                osKey = "mac";
            } else {
                throw new MojoExecutionException("Unsupported OS : " + os);
            }

            List<String> arch64 = Arrays.asList(
                    "amd64","x64","x86_64"
            );
            String arch = System.getProperty("os.arch").toLowerCase();
            if (arch64.contains(arch)) {
                osKey += "-x64";
            } else {
                osKey += "-" + arch;
            }

            PlatformSpecified specified = platformConfigure.getPlatforms().get(osKey);
            if (specified == null) {
                throw new MojoExecutionException("No platform specified for : " + osKey);
            }

            for (String includeDir : specified.getIncludeSearchDirs()) {
                File includeDirFile = new File(includeDir).getAbsoluteFile();
                if (!includeDirFile.exists()) {
                    throw new MojoExecutionException("Include dir not found : " + includeDir);
                }
                projectGenerator.addIncludeDir(includeDirFile);
            }

            for (String libraryDir : specified.getLibrarySearchDirs()) {
                File libraryDirFile = new File(libraryDir).getAbsoluteFile();
                if (!libraryDirFile.exists()) {
                    throw new MojoExecutionException("Library dir not found : " + libraryDir);
                }
                projectGenerator.addLibraryDir(libraryDirFile);
            }

            for (String header : specified.getHeaders()) {
                File headerFile = new File(header).getAbsoluteFile();
                if (!headerFile.exists()) {
                    throw new MojoExecutionException("Header file not found : " + header);
                }
                projectGenerator.addAPIHeader(headerFile);
            }

            for (String library : specified.getLibraries()) {
                projectGenerator.addLinkedLibrary(library);
            }

            projectGenerator.generate();

        } catch (Exception e) {
            throw new MojoExecutionException("unknown problem : ", e);
        }

    }

}
