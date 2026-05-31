package org.swdc.clang.framework.source;

import org.swdc.clang.framework.ClangUtils;
import org.swdc.clang.framework.def.*;
import org.swdc.clang.framework.meta.Call;
import org.swdc.layered.def.NativeDefinition;
import org.swdc.layered.def.WritableFunction;
import org.swdc.layered.def.WritableParameter;

import java.util.Arrays;
import java.util.Collections;

/**
 * Struct的FFI源码生成器。
 *
 * 为Struct生成原生的创建，销毁函数以及各个字段的Getter和Setter。
 * 解析的时候，即使Struct是嵌套在其他结构内部的，它也会被展开到特定的作用域内，
 * 因此，生成源码的时候只需要把它当做顶级的Struct即可。
 *
 */
public class StructSourceWriter extends AbstractSourceWriter<NativeStructType> {

    @Override
    public void createCalls(SourceContext context, NativeStructType structType) {

        if (!structType.getFields().isEmpty()) {

            String structName = structType.getName().replace("::", "_");
            Call ctor = createCtor(structType);
            Call dtor = createDtor(structType);

            String symbolCreate = structName + "__new";
            String symbolDelete = structName + "__delete";

            context.addCall(symbolCreate, createCtorMetadata(symbolCreate,structType), ctor);
            context.addCall(symbolDelete, createDtorMetadata(symbolDelete,structType),dtor);

            for (NativeField field : structType.getFields()) {

                String setterSymbol = structName + "_set_" + field.getName();
                String getterSymbol = structName + "_get_" + field.getName();

                Call getter = createFieldGetter(structType, field);
                Call setter = createFieldSetter(structType, field);

                context.addCall(getterSymbol , createGetterMetadata(getterSymbol, field), getter);
                context.addCall(setterSymbol, createSetterMetadata(setterSymbol,field), setter);

            }

        }

    }


    private WritableFunction createGetterMetadata(String name, NativeField field) {

        AbstractNativeType type = field.getType();
        WritableFunction function = new WritableFunction();
        function.setName(name);
        function.setReturnType(type.getType());
        function.setParameters(Collections.singletonList(
                WritableParameter.create(0, "object", NativeDefinition.POINTER)
        ));

        return function;
    }


    private Call createFieldGetter(NativeStructType struct, NativeField field) {

        AbstractNativeType type = field.getType();
        String castType = null;

        if (type instanceof BasicType) {
            castType = type.getName();
        } else if (
                type instanceof NativePointerType ||
                type instanceof NativeArrayType ||
                type instanceof NativeClassType ||
                type instanceof NativeStructType
        ) {
            castType = "intptr_t";
        } else if (type instanceof NativeEnumType) {
            castType = "long";
        }

        TypeParameterized self = new TypeParameterized();
        self.setName("object");
        self.setType(struct);

        String mangled = ClangUtils.generateMangled(Arrays.asList(self),true);

        StringBuilder body = new StringBuilder();
        body.append("\t").append(struct.getName()).append("* obj = reinterpret_cast<").append(struct.getName()).append("*>(").append("object").append(");\n");
        body.append("\t").append("if (obj == nullptr) {\n\t\t return 0;\n\t } \n");
        if (type instanceof NativeArrayType || type instanceof NativeStructType || type instanceof NativeClassType) {
            body.append("\treturn reinterpret_cast<").append(castType).append(">(&(obj->").append(field.getName()).append("));");
        } else  {
            if (type instanceof NativePointerType) {
                body.append("\treturn reinterpret_cast<").append(castType).append(">(obj->").append(field.getName()).append(");");
            } else {
                body.append("\treturn static_cast<").append(castType).append(">(obj->").append(field.getName()).append(");");
            }
        }

        Call call = new Call();
        call.setMangled(mangled);
        call.setReturnTypeSource(castType);
        call.setParametersSource("(intptr_t object)");
        call.setBodySource(body.toString());
        call.setMangledReturnType(ClangUtils.generateMangled(
                Collections.singletonList(field), true
        ));
        return call;
    }

    private WritableFunction createSetterMetadata(String name, NativeField field) {

        WritableFunction callback = null;
        NativeDefinition fieldType = null;
        AbstractNativeType type = field.getType();
        if (type instanceof BasicType) {
            BasicType basicType = (BasicType) type;
            fieldType = basicType.getType();
        } else if (type instanceof NativeEnumType) {
            NativeEnumType enumType = (NativeEnumType) type;
            fieldType = enumType.getType();
        } else {
            // 除了基本类型之外，所有其他类型都通过指针类型传递。
            fieldType = NativeDefinition.POINTER;
            if (field.getType() instanceof NativePointerType) {
                NativePointerType pointerType = (NativePointerType) type;
                if (pointerType.getPointeeType() != null && pointerType.getPointeeType() instanceof NativeFunction) {
                    // 这是一个callback函数。
                    NativeFunction nativeFunction = (NativeFunction) pointerType.getPointeeType();
                    FunctionSourceWriter sourceWriter = new FunctionSourceWriter();
                    callback = sourceWriter.createMetadata(nativeFunction);
                }
            }

        }


        WritableFunction function = new WritableFunction();
        function.setName(name);
        function.setReturnType(NativeDefinition.VOID);
        function.setParameters(Arrays.asList(
                WritableParameter.create(0, "object", NativeDefinition.POINTER),
                WritableParameter.create(1, "source", callback ,fieldType)
        ));
        return function;
    }

    private Call createFieldSetter(NativeStructType struct, NativeField field) {

        TypeParameterized self = new TypeParameterized();
        self.setName("object");
        self.setType(struct);
        String mangled = ClangUtils.generateMangled(Arrays.asList(self, field),true);

        TypeParameterized returnType = new TypeParameterized();
        returnType.setName("result");
        returnType.setType(BasicType.VOID);

        AbstractNativeType type = field.getType();
        if (type instanceof NativeArrayType) {

            StringBuilder body = new StringBuilder();
            body.append("\t").append(struct.getName()).append("* obj = reinterpret_cast<").append(struct.getName()).append("*>(").append("object").append(");\n");
            body.append("\t").append("void* value = reinterpret_cast<void*>(").append("source").append(");\n");
            body.append("\t").append("if (obj == nullptr || value == nullptr) {\n\t\t return;\n\t\t } \n");
            body.append("\t").append("memcpy(obj->").append(field.getName()).append(", value, sizeof(").append(field.getType().getName()).append("));\n");
            Call call = new Call();
            call.setMangled(mangled);
            call.setReturnTypeSource("void");
            call.setMangledReturnType(ClangUtils.generateMangled(
                    Collections.singletonList(returnType),true)
            );
            call.setParametersSource("(intptr_t object, intptr_t source)");
            call.setBodySource(body.toString());
            return call;

        } else {

            String paramType = null;

            boolean objectType = false;

            StringBuilder sb = new StringBuilder();
            sb.append("\t").append(struct.getName()).append("* obj = reinterpret_cast<").append(struct.getName()).append("*>(").append("object").append(");\n");
            sb.append("\tif (obj == nullptr) { \n\t\treturn;\n\t}\n");

            if (type instanceof NativePointerType) {

                NativePointerType pointer = (NativePointerType) type;
                AbstractNativeType pointee = pointer.getPointeeType();
                while (pointee instanceof NativePointerType) {
                    pointee = ((NativePointerType) pointee).getPointeeType();
                }


                String castType = pointer.castFromBaseType().as("value");
                String castSource = pointer.castFromBaseType().as("");
                sb.append("\t").append(castType).append(" = reinterpret_cast<").append(castSource).append(">(").append("source").append(");\n");


                sb.append("\tobj->").append(field.getName()).append(" = value;");
                paramType = "intptr_t source";

            } else {

                paramType = type.castAsBaseType().as("source");

                String castSource = type.castFromBaseType().as("");
                String castType = type.castFromBaseType().as("value");

                objectType = type instanceof NativeStructType || type instanceof NativeClassType;
                if (objectType) {
                    // 值类型的object不能跨语言传递，所以这里必须是指针，
                    // 这种指针是指向特定Object的。
                    castType = castType + "*";
                }

                if (objectType) {
                    // 不是基本类型也不是指针类型，则需要解引用
                    sb.append("\t").append(castType).append(" = reinterpret_cast<").append(castSource).append(">(").append("source").append(");\n");
                    sb.append("\tif (value == nullptr) { \n\t\treturn;\n\t}\n");
                    sb.append("\tobj->").append(field.getName()).append(" = *(value);");
                } else {
                    sb.append("\t").append(castType).append(" = static_cast<").append(castSource).append(">(").append("source").append(");\n");
                    sb.append("\tobj->").append(field.getName()).append(" = value;");
                }

            }

            Call call = new Call();
            call.setMangled(mangled);
            call.setMangledReturnType(ClangUtils.generateMangled(
                    Collections.singletonList(returnType),true)
            );
            call.setReturnTypeSource("void");
            call.setParametersSource("(intptr_t object, " + paramType + ")");
            call.setBodySource(sb.toString());
            return call;

        }

    }

    private WritableFunction createCtorMetadata(String name, NativeStructType structType) {



        WritableFunction ctor = new WritableFunction();
        ctor.setConstructor(true);
        ctor.setParameters(Collections.emptyList());
        ctor.setReturnType(NativeDefinition.POINTER);
        ctor.setName(name);
        return ctor;

    }

    private Call createCtor(NativeStructType struct) {



        String mangled = ClangUtils.generateMangled(Collections.emptyList(),true);

        StringBuilder sb = new StringBuilder();
        sb.append("\t").append(struct.getName()).append("* obj = new ").append(struct.getName()).append("();\n");
        sb.append("\treturn reinterpret_cast<intptr_t>(obj);");

        TypeParameterized returnType = new TypeParameterized();
        returnType.setName("result");
        returnType.setType(struct);

        Call call = new Call();
        call.setMangled(mangled);
        call.setReturnTypeSource("intptr_t");
        call.setMangledReturnType(ClangUtils.generateMangled(
                Collections.singletonList(returnType),true
        ));
        call.setParametersSource("()");
        call.setBodySource(sb.toString());
        return call;

    }

    private WritableFunction createDtorMetadata(String name, NativeStructType structType) {

        WritableFunction dtor = new WritableFunction();
        dtor.setConstructor(true);
        dtor.setParameters(Arrays.asList(
                WritableParameter.create(0, "object", NativeDefinition.POINTER)
        ));
        dtor.setReturnType(NativeDefinition.VOID);
        dtor.setName(name);
        return dtor;

    }

    private Call createDtor(NativeStructType struct) {

        TypeParameterized param = new TypeParameterized();
        param.setName("object");
        param.setType(struct);

        String mangled = ClangUtils.generateMangled(Arrays.asList(param),true);

        StringBuilder sb = new StringBuilder();
        sb.append("\t").append(struct.getName()).append("* obj = reinterpret_cast<").append(struct.getName()).append("*>(").append("object").append(");\n");
        sb.append("\tif (obj == nullptr) { \n\t\treturn;\n\t}\n");
        sb.append("\tdelete obj;");

        TypeParameterized returnType = new TypeParameterized();
        returnType.setName("result");
        returnType.setType(BasicType.VOID);

        Call call = new Call();
        call.setMangled(mangled);
        call.setReturnTypeSource("void");
        call.setParametersSource("(intptr_t object)");
        call.setMangledReturnType(ClangUtils.generateMangled(
                Collections.singletonList(returnType),true
        ));
        call.setBodySource(sb.toString());
        return call;

    }

}
