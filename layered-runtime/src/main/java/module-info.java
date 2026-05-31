module swdc.layered.runtime {

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires jackson.dataformat.msgpack;

    exports org.swdc.layered.module;
    exports org.swdc.layered.pointers;
    exports org.swdc.layered;
    exports org.swdc.layered.def;
    exports org.swdc.layered.anno;

    opens org.swdc.layered.def to com.fasterxml.jackson.databind;
    opens org.swdc.layered.module to com.fasterxml.jackson.databind;
    opens org.swdc.layered.anno to com.fasterxml.jackson.databind;

}