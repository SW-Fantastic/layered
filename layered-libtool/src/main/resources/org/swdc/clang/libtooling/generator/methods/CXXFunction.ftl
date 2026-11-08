[=getReturnType().getRawType().getTypeName()] [=getPrefix()]_[=getIndex()](
    <#list getParamTypes() as param>
     [=param.getRawType().getTypeName()] arg[=param?index]<#if param_has_next>, </#if>
    </#list>
) {

    <#list getParamTypes() as param>
    [=param.castFromRawType("argV" + param?index, "arg" + param?index)]
    </#list>

    <#if isCallback()>
        [=getFunctionMetaType().castFromRawType("self", "argV0")]
        <#if needResult()>
        return self(
            <#list getCbParamTypes() as param>
            argV[=param?index + 1]<#if param_has_next>, </#if>
            </#list>
        );
        <#else>
        self(
            <#list getCbParamTypes() as param>
            argV[=param?index + 1]<#if param_has_next>, </#if>
            </#list>
        );
        </#if>
    <#else>

        <#if needResult()>
        [=getReturnType().cppTypeFullName()] result = [=getName()](
            <#list getParamTypes() as param>
            argV[=param?index]<#if param_has_next>, </#if>
            </#list>
        );

        [=getReturnType().castToRawType("resultVal", "result")]
        return resultVal;
        <#else>
        [=getName()](
            <#list getParamTypes() as param>
            argV[=param?index]<#if param_has_next>, </#if>
            </#list>
        );
        </#if>

    </#if>


}