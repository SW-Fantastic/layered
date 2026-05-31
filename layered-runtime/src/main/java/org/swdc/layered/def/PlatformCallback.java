package org.swdc.layered.def;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.pointers.*;

import java.util.List;


public class PlatformCallback extends OpaquePointer {


    public PlatformCallback(Allocator allocator, WritableFunction function) {

        List<WritableParameter> parameters = function.getParameters();
        int[] paramTypes = new int[parameters.size()];
        WritableParameter[] cachedParameters = new WritableParameter[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            WritableParameter parameter = parameters.get(i);
            paramTypes[i] = FFIUtils.getLayerTypeFlag(parameter.getType());
            cachedParameters[i] = parameter;
        }
        int returnType = FFIUtils.getLayerTypeFlag(function.getReturnType());
        long address = ExternalInvoker.createFFICIF(paramTypes, returnType);
        if (address == 0) {
            throw new LinkageError("Cannot create function " + function.getName());
        }
        initPointer(allocator,address,false);

    }

}
