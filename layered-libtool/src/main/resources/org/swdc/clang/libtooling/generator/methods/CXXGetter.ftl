[=getFieldType().getRawType().getTypeName()] [=getPrefix()]_[=getGetterIndex()](intptr_t objectPtr) {

    // 转换为原始Object指针
    [=getRecordMetaType().cppTypeFullName()]* nativeObject = reinterpret_cast<[=getRecordMetaType().cppTypeFullName()]*>(objectPtr);
    // 获取Value并转换类型
    [=getFieldType().cppTypeFullName("result")] = nativeObject->[=getFieldName()];
    [=getFieldType().castToRawType("resultVal", "result")]
    return resultVal;

}
