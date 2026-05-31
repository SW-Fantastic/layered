package org.swdc.layered.def;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.pointers.AddressPointer;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.OpaquePointer;

import java.util.List;

public class PlatformCall extends OpaquePointer {

    private long functionAddr;

    private int[] argTypes;

    private WritableParameter[] parameters;

    private int returnType;

    public PlatformCall(Allocator allocator, WritableFunction function, long functionAddr) {

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

        initPointer(allocator, address,false);
        this.functionAddr = functionAddr;
        this.argTypes = paramTypes;
        this.parameters = cachedParameters;
        this.returnType = returnType;

    }

    public WritableParameter[] getParameters() {
        return parameters;
    }

    public NativeDefinition getReturnType() {
        return FFIUtils.getDefinitionType(returnType);
    }

    public void call(OpaquePointer result, OpaquePointer ...args) {

        if (args.length != argTypes.length) {
            throw new  IllegalArgumentException("Illegal number of arguments: " + args.length + " != " + argTypes.length);
        }

        AddressPointer values = null;
        if (args.length > 0) {
            values = getAllocator().allocateAddress(args.length);
            for (int i = 0; i < args.length; i++) {
                values.set(i, args[i]);
            }
        } else {
            values = null;
        }


        ExternalInvoker.call(
                getAddress(),
                result.getAddress(),
                functionAddr,
                values == null ? 0 : values.getAddress(),
                argTypes
        );

        if (values != null) {
            values.free();
        }
    }


    @Override
    protected void deAllocate() {
        if (functionAddr != 0) {
            functionAddr = 0;
        }
        if (isNull()) {
            return;
        }
        ExternalInvoker.destroyFFICIF(getAddress());
    }

}
