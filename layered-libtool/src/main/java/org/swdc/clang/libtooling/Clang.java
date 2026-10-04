package org.swdc.clang.libtooling;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.swdc.clang.libtooling.metadata.AnalysiserMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class Clang {

    private static LayerParserLibrary library = null;

    public synchronized static void initialize(File libAssetFolder) {

        if (library != null) {
            return;
        }
        library = new LayerParserLibrary();
        library.loadLibrary(libAssetFolder,true);

    }

    public static AnalysiserMetadata parseHeader(File workingDir, List<String> parameter, List<File> headerPaths) {

        if (library == null) {
            throw new IllegalStateException("Library has not been initialized");
        }

        if (workingDir == null || !workingDir.exists()) {
            throw new IllegalStateException("Working directory does not exist");
        }

        if (headerPaths == null || headerPaths.isEmpty()) {
            throw new IllegalStateException("No headers were provided");
        }

        if (parameter == null) {
            parameter = new ArrayList<>();
        }

        String[] parameterArray = parameter.toArray(new String[0]);
        String[] pathArray = headerPaths.stream()
                .filter(File::exists)
                .map(File::getAbsolutePath)
                .toArray(String[]::new);

        String workingDirPath = workingDir.getAbsolutePath();

        String result =  ClangLibTools.parseHeaderFiles(
                workingDirPath,
                parameterArray,
                pathArray
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return mapper.readValue(result, AnalysiserMetadata.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

}
