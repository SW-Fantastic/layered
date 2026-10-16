[=getReturnType().getRawType().getTypeName()] [=getPrefix()]_[=getIndex()](
    <#list getParamTypes() as param>
     [=param.getRawType().getTypeName()] arg[=param?index]<#if param_has_next>, </#if>
    </#list>
) {

    <#list getParamTypes() as param>
    [=param.castFromRawType("argV" + param?index, "arg" + param?index)]
    </#list>
    [=getReturnType().cppTypeFullName()]* result = new [=getReturnType().cppTypeFullName()](
        <#list getParamTypes() as param>
        argV[=param?index]<#if param_has_next>, </#if>
        </#list>
    );

    return reinterpret_cast<intptr_t>(result);

}
