package org.swdc.layered.def;

import org.swdc.layered.anno.*;

import java.lang.reflect.AnnotatedElement;

public class FFIUtils {

    public static int getLayerTypeFlag(NativeDefinition definition) {
        switch (definition) {
            case UNSIGNED_INT:
            case INT:
                return CIFTypes.INTEGER;
            case FLOAT:
                return CIFTypes.FLOAT;
            case DOUBLE:
                return CIFTypes.DOUBLE;
            case UNSIGNED_LONG:
            case LONG:
                return CIFTypes.LONG;
            case UNSIGNED_CHAR:
            case CHAR:
                return CIFTypes.CHAR;
            case POINTER:
            case ARRAY:
                return CIFTypes.POINTER;
            case BOOL:
                return CIFTypes.BOOLEAN;
            case VOID:
                return CIFTypes.VOID;
        }
        throw new RuntimeException("Cannot determine layer type flag");
    }

    public static NativeDefinition getDefinitionType(int layerFlag) {
        if (layerFlag == CIFTypes.VOID) {
            return NativeDefinition.VOID;
        } else if (layerFlag == CIFTypes.INTEGER || layerFlag == CIFTypes.UNSIGNED_INTEGER) {
            return NativeDefinition.INT;
        } else if (layerFlag == CIFTypes.FLOAT) {
            return NativeDefinition.FLOAT;
        } else if (layerFlag == CIFTypes.CHAR || layerFlag == CIFTypes.UNSIGNED_CHAR) {
            return NativeDefinition.CHAR;
        } else if (layerFlag == CIFTypes.BOOLEAN) {
            return NativeDefinition.BOOL;
        } else if (layerFlag == CIFTypes.DOUBLE) {
            return NativeDefinition.DOUBLE;
        } else if (layerFlag == CIFTypes.LONG || layerFlag == CIFTypes.UNSIGNED_LONG) {
            return NativeDefinition.LONG;
        } else if (layerFlag == CIFTypes.POINTER) {
            return NativeDefinition.POINTER;
        }
        throw new RuntimeException("Unsupported flag " + layerFlag);
    }

    public static TypeMeta extractMeta(AnnotatedElement element) {

        Cast cast = element.getAnnotation(Cast.class);
        CastValue castValue = element.getAnnotation(CastValue.class);
        Unsigned unsigned = element.getAnnotation(Unsigned.class);
        ByteString byteString = element.getAnnotation(ByteString.class);
        ConstArray constArray = element.getAnnotation(ConstArray.class);

        return new TypeMeta(unsigned, byteString ,cast,castValue,constArray );
    }

}
