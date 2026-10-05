[=getReturnType().getRawType().getTypeName()] [=getPrefix()]_[=getIndex()](
    <#list getParamTypes() as param>
     [=param.getRawType().getTypeName()] arg[=param?index]<#if param_has_next>, </#if>
    </#list>
);