
# Layered native

本项目是Layer项目的Native部分，构建工具是CMake，主要在Windows平台下开发，
编译本项目至少需要预留100GB磁盘空间。

这个项目主要包含两个target，分别用于处理C++和C语言头文件的layer库，以及用于
支撑本项目正常运作的layeredRuntime运行时库，前者的主要作用是分析C/C++头文件，
提取描述符并传递给Layered-Libtool，后者将会依据这些描述符生成Wrapper源码，
同时，这些描述符也将会被转译为Metadata嵌入到生成的Wrapper源码中，以便运行时库
在进行本地调用的时候，对函数的参数以及返回值等进行校验和转换。

在开始之前，请预先下载一个vcpkg发行包，并将它解压至本项目的“vcpkg”目录下，
本项目的CMake文件会自动检测这个“vcpkg”目录并使用它配置项目的依赖项。

> 注意，llvm和clang由于一些历史原因需要本地进行编译，从而避免DIASDK的绝对路径问题
> 因此依赖库的编译的过程会非常漫长，但是不用担心，llvm的版本被overlay目录的描述符锁定了，
> 通常它只需要编译一次。

## windows构建本项目的额外注意事项

请务必在Visual Studio安装ATL库和对应版本的MFC库，并且使用RelWithDebInfo配置构建，或者使用Release构建。

## 项目生成指令

```bash
cmake -B build -DCMAKE_BUILD_TYPE=Release 
```

