package org.swdc.layered.anno;

public class TypeMeta {

    private Unsigned unsigned;

    private ByteString charset;

    private Cast cast;

    private ConstArray array;

    public TypeMeta(Unsigned isUnsigned, ByteString charset, Cast cast, ConstArray array) {
        this.unsigned = isUnsigned;
        this.charset = charset;
        this.cast = cast;
        this.array = array;
    }

    public ByteString getCharset() {
        return charset;
    }

    public Unsigned getUnsigned() {
        return unsigned;
    }

    public Cast getCast() {
        return cast;
    }

    public ConstArray getArray() {
        return array;
    }
}

