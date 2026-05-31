
首先请以管理员身份在PowerShell中运行以下命令，
它的含义是允许执行PowerShell的脚本，从而让CMake可以正确的Bootstrap必要的工具链，比如vcpkg。
```
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```
接着你需要一个vcpkg，请在github下载一份它的Release版本，解压到native目录的vcpkg目录下，
CMake会自动配置这个项目范围的Vcpkg，然后，你可以使用以下命令来执行CMake开始构建流程：
```
cmake -B build
```