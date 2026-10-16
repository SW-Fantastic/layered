void [=getDtorPrefix()]_[=getDtorIndex()]( [=getMetaType().getRawType().getTypeName()] instancePtr ) {

    [=getMetaType().cppTypeFullName()]* instance = reinterpret_cast<[=getMetaType().cppTypeFullName()]*>(instancePtr);
    delete instance;

}