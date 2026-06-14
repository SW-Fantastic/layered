package org.swdc.clang.framework.source;

import org.swdc.clang.framework.ClangUtils;
import org.swdc.clang.framework.def.*;
import org.swdc.clang.framework.meta.Call;
import org.swdc.layered.def.NativeDefinition;
import org.swdc.layered.def.WritableFunction;
import org.swdc.layered.def.WritableParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 函数的源码生成器。
 * 生成C/C++的函数FFI接口，把各类函数的参数，返回值都整理为基本类型，并且生成函数调用的源码，
 * 这里不处理实例方法。
 */
public class FunctionSourceWriter extends AbstractSourceWriter<NativeFunction> {

    @Override
    public void createCalls(SourceContext context, NativeFunction type) {

        if (type.isCallback()) {
            return;
        }


        String mangled =  ClangUtils.generateMangled(type.getParameters(),true);
        AbstractNativeType nativeReturnType = type.getReturnType().getType();
        String returnTypeSource = nativeReturnType.castAsBaseType().as("");

        StringBuilder parametersSource = new StringBuilder("(");

        for (int i = 0; i < type.getParameters().size(); ++i) {

            NativeFunctionParam param = type.getParameters().get(i);
            AbstractNativeType paramType = param.getType();
            String paramSource = paramType.castAsBaseType().as(param.getName());
            parametersSource.append(paramSource);
            if (i < type.getParameters().size() - 1) {
                parametersSource.append(", ");
            }

        }

        parametersSource.append(")");

        Call call = new Call();
        call.setReturnTypeSource(returnTypeSource);
        call.setParametersSource(parametersSource.toString());
        call.setMangled(mangled);
        call.setBodySource(createSource(type));
        call.setMangledReturnType(ClangUtils.generateMangled(
                Collections.singletonList(type.getReturnType()), true
        ));
        context.addCall(type.getRawName(),createMetadata(type), call);

    }


    public WritableFunction createMetadata(NativeFunction function) {

        AbstractNativeType returnType = function.getReturnType().getType();

        WritableFunction writableFunction = new WritableFunction();
        writableFunction.setName(function.getRawName());
        writableFunction.setReturnType(returnType.getType());

        List<WritableParameter> params = new ArrayList<>();
        for (NativeFunctionParam param : function.getParameters()) {
            AbstractNativeType paramType = param.getType();
            WritableFunction callback = null;
            long nested = 0;
            if (paramType instanceof NativePointerType) {
                NativePointerType nativePointerType = (NativePointerType)paramType;
                if (nativePointerType.getPointeeType() instanceof NativeFunction) {
                    NativeFunction paramFunction = (NativeFunction)nativePointerType.getPointeeType();
                    callback = createMetadata(paramFunction);
                }

                nested = 1;
                NativePointerType current = nativePointerType;
                while (current != null && current.getPointeeType() instanceof NativePointerType) {
                    nested++;
                    current = (NativePointerType)current.getPointeeType();
                }
            } else if (paramType instanceof NativeArrayType) {
                NativeArrayType nativeArrayType = (NativeArrayType)paramType;
                nested = nativeArrayType.getArraySize();
            }
            params.add(WritableParameter.create(param.getIndex(), param.getName(),callback, paramType.getType(), nested));
        }

        writableFunction.setParameters(params);

        return writableFunction;
    }

    public String createSource(NativeFunction function) {

        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < function.getParameters().size(); index ++) {

            NativeFunctionParam param = function.getParameters().get(index);
            AbstractNativeType paramType = param.getType();

            if (paramType instanceof NativePointerType) {

                NativePointerType pointerType = (NativePointerType) paramType;
                if (pointerType.getPointeeType() instanceof NativeFunction) {

                    AbstractNativeType pointeeType = pointerType.getPointeeType();
                    NativeFunction nativeFunction = (NativeFunction) pointeeType;

                    String castTarget = nativeFunction.castFromBaseType().as("");
                    String castSource = nativeFunction.castFromBaseType().as("val_" + param.getName());


                    stringBuilder.append("\t").append(castSource).append(" = reinterpret_cast<").append(castTarget).append(">(").append(param.getName()).append(");\n");

                } else {

                    String castTarget = paramType.castFromBaseType().as("val_" + param.getName());
                    String castSource = paramType.castFromBaseType().as("");

                    stringBuilder.append("\t").append(castTarget).append(" = reinterpret_cast<").append(castSource).append(">(").append(param.getName()).append(");\n");

                }

            } else if (paramType instanceof NativeArrayType) {

                NativeArrayType arrayType = (NativeArrayType) param.getType();
                String castTarget = arrayType.castFromBaseTypeRef().as("val_" + param.getName());
                String castSource = arrayType.castFromBaseType().as("");

                stringBuilder.append("\t").append(castTarget).append(" = reinterpret_cast<").append(castSource).append(">(").append(param.getName()).append(");\n");

            } else if (paramType instanceof NativeStructType || paramType instanceof NativeClassType) {

                String castTarget = paramType.castFromBaseType().as("val_" + param.getName());
                String castSource = paramType.castFromBaseType().as("");

                stringBuilder.append("\t").append(castTarget).append(" = *(reinterpret_cast<").append(castSource).append("*>(").append(param.getName()).append("));\n");

            } else {

                String castTarget = paramType.castFromBaseType().as("val_" + param.getName());
                String castSource = paramType.castFromBaseType().as("");

                stringBuilder.append("\t").append(castTarget).append(" = static_cast<").append(castSource).append(">(").append(param.getName()).append(");\n");

            }

        }

        TypeParameterized fnReturn =  function.getReturnType();
        AbstractNativeType retType = fnReturn.getType();
        if (retType.getType() == NativeDefinition.VOID) {

            stringBuilder.append("\t").append(function.getRawName()).append("(");
            for (int index = 0; index < function.getParameters().size(); ++index) {
                NativeFunctionParam param = function.getParameters().get(index);
                stringBuilder.append("val_").append(param.getName());
                if (index < function.getParameters().size() - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append(");\n");

        } else {

            String retTypeSource = retType.castFromBaseType().as("the_result");
            String retTypeCast = retType.castAsBaseType().as("");

            stringBuilder.append("\t").append(retTypeSource).append(" = ").append(function.getRawName()).append("(");
            for (int index = 0; index < function.getParameters().size(); ++index) {
                NativeFunctionParam param = function.getParameters().get(index);
                stringBuilder.append("val_").append(param.getName());
                if (index < function.getParameters().size() - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append(");\n");
            if (retType instanceof NativeArrayType || retType instanceof NativeStructType || retType instanceof NativeClassType) {
                stringBuilder.append("\treturn reinterpret_cast<").append(retTypeCast).append(">(").append("&the_result").append(");\n");
            } else {
                if (retType instanceof NativePointerType) {
                    stringBuilder.append("\treturn reinterpret_cast<").append(retTypeCast).append(">(").append("the_result").append(");\n");
                } else {
                    stringBuilder.append("\treturn static_cast<").append(retTypeCast).append(">(").append("the_result").append(");\n");
                }
            }

        }


        return stringBuilder.toString();

    }

}
