package org.swdc.clang.libtooling.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.clang.libtooling.Clang;
import org.swdc.clang.libtooling.context.DescriptorsFactory;
import org.swdc.clang.libtooling.generator.CXXWrapSourceGenerator;
import org.swdc.clang.libtooling.metadata.AnalysiserMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class CHeaderParseTest {

    @BeforeAll
    public static void initLibrary() {
        Clang.initialize(new File("./assets"));
    }

    @Test
    public void testParseSimpleHeader() throws JsonProcessingException {
        File header = new File("../assets/pdfium/include/fpdfview.h");
        AnalysiserMetadata result = Clang.parseHeader(new File("."), new ArrayList<>(), Arrays.asList(
                header
        ));

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        DescriptorsFactory ctx = new DescriptorsFactory();
        ctx.resolve(header, result);
        String resolved = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ctx.getFunctions(header));
        System.out.println("===============[ Result Transformed ]==================");
        System.out.println(resolved);

    }


}
