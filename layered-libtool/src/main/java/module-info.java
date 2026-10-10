module swdc.layered.libtool {

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires freemarker;

    opens org.swdc.clang.libtooling.generator to freemarker,com.fasterxml.jackson.databind;
    opens org.swdc.clang.libtooling.generator.sources to freemarker;
    opens org.swdc.clang.libtooling.generator.methods to freemarker;

    opens org.swdc.clang.libtooling.metadata to com.fasterxml.jackson.databind;

    exports org.swdc.clang.libtooling;
    exports org.swdc.clang.libtooling.context;
    exports org.swdc.clang.libtooling.generator;
    exports org.swdc.clang.libtooling.metadata;


}