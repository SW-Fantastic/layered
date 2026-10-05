void [=getPrefix()]_[=getSetterIndex()](
    intptr_t objectPtr,
    [=getFieldType().getRawType().getTypeName()] value
) {

    // 转换为原始Object指针
    [=getRecordMetaType().cppTypeFullName()]* nativeObject = reinterpret_cast<[=getRecordMetaType().cppTypeFullName()]*>(objectPtr);
    // 转换类型并赋值
    [=getFieldType().castFromRawType("targetVal", "value")]
    nativeObject->[=getFieldName()] = targetVal;

}
