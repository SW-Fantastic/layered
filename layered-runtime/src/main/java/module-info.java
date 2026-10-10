module swdc.layered.runtime {

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires jackson.dataformat.msgpack;

    opens org.swdc.layered.library to com.fasterxml.jackson.databind;
    opens org.swdc.layered.calling to com.fasterxml.jackson.databind;

    exports org.swdc.layered.library;
    exports org.swdc.layered.pointers;
    exports org.swdc.layered.calling;
    exports org.swdc.layered.types;
    exports org.swdc.layered;


}