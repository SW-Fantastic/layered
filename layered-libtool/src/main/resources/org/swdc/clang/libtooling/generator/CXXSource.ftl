#include"[=getIncludePath()]"

<#list getSources()?keys as declare>
[=getSources()[declare]]

</#list>