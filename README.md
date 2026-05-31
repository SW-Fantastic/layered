## 概述

本项目的目的是把C++的header整理为一个元数据文件，用来描述一个动态库的全部功能，
最终目标是完成一个JNI的代理系统，用以实现JNI的自动绑定。


```bash
mkdir build
cd build
cmake ..
cmake --build . --config release
```

## 名称规范

### 导出时使用的Mangled规范：
1. 基本类型，直接使用提供的flag：
   - int: "I"
   - float: "F"
   - double: "D"
   - char: "C"
   - bool: "B"
   - long: "L"
   - short: "S"
   - unsigned short: "Us"
   - unsigned int: "Ui"
   - unsigned long: "Ul"
   - unsigned char: "Uc"
   - unsigned short: "Us"
   - unsigned long: "Ul"
   - void: "V"
2. 枚举类型：使用long的修饰符，即"L"。
3. 数组类型：使用指针的修饰符。
4. 指针类型：基本类型的修饰符 + "p"，例如int的指针为"Ip"。
5. 结构体，类，联合体等复合类型，使用“Vp”，即“void*”。
6. CV修饰符在Mangled后面出现，包裹在中括号内，例如“const int”的Mangled为"I[C]"，"volatile int"的Mangled为"I[V]"，"const volatile int"的Mangled为"I[CV]"。
7. 导出的Metadata不应该包含CV修饰符，它们仅在内部使用。

### 类型名称

1. 基本类型，直接使用本身类型名称的小写形式，例如整数是"int"，浮点数是"float"。
2. 枚举类型，直接使用枚举类型本身的名称
3. 类，结构体，联合体等复合类型，使用类型本身的名称。
4. 别名类型，尽可能穿透别名类型，使用原始类型名称，例如`typedef int MyInt;`，解析得到的名称应为"int"。
5. 复合类型，这种类型指的是对其他类型进行二次封装，修改了它的CV修饰符的情况，此时应复制原始的类型并且调整CV修饰符
   CV修饰符不同的类型，不能被视为一个类型，例如：const int和int是两种类型。
6. CV修饰符位于类型名后，通过“?”分割修饰符和类型名，例如"const int"，应该记录为“int?const”，而”volatile int"则是“int?volatile”。
   对于“const volatile int"，则记录为“int?const&volatile”。
7. 指针类型的名称采用“_Ptr” + 指针维度数 + “_” + 指向类型名称，例如int的指针为“_Ptr1_int”。
8. 数组类型的名称采用“_Arr” + 数组维度数 + “_” + 元素类型名称，例如int的数组为“_Arr1_int”。
9. 函数类型的名称采用“(” + 返回类型的Mangled + “)” + 函数名称 + "@" + 参数类型的Mangled，
   例如“int main(int argc, char** argv)“应该记录为“(I)main@ICpp"。

- - -

以下为备忘信息，项目进行中的参考内容。

## 基本思路

1. 所有C++的Class导出全部展平为C风格的导出，所有的Class的实例方法，全部表达为C风格的函数，通过传入Object的指针实现方法调用。
2. 所有的Struct类型，如果存在字段，导出C风格的Getter和Setter函数。
3. 所有Enum，导出为int类型。
4. 所有未声明结构的Class和Struct，导出为void*类型。
5. 不允许值类型传递，所有类型必须为基本类型或者指针类型。
6. 导出时，不允许有同名函数，如果有同名函数，则在原本函数名后面添加编号作为区分。
7. 不允许导出模板，如果需要使用模板，必须将模板声明为特定类型，例如：vector<T>是不允许的，必须声明为vector<int>。
8. 所有未命名的Enum，导出为常量。
9. 如果导出的时候需要涉及命名空间，则显示使用命名空间，例如：`namespace::ClassName`。
10. 如果遇到宏，忽略它们。
11. 如果遇到C风格的函数，直接导出它。
12. 如果遇到对基础类型的typedef，则导出为它的原始类型。
13. long long导出为long，因为Java中没有long long类型，它会存储为64位，但是会被解释为有符号类型，如果Java想要使用它的值需要进行位运算处理。
14. unsigned int导出为int，因为Java中没有unsigned类型，它存储的数据是正确的，只是会被解释为有符号类型，如果Java想要使用它的值需要进行位运算处理。
15. unsigned char导出为byte，因为Java中没有unsigned char类型。
16. unsigned short导出为short，因为Java中没有unsigned short类型,它会存储为16位，但是会被解释为有符号类型，如果Java想要使用它的值需要进行位运算处理。
17. 输出应该是一个动态库，包含所有C风格导出。
18. 通过该动态库提供的json文件（包含参数类型，函数名称和描述）以及libffi最终实现JNI调用。
19. 通过一个静态的代理方法和注解，Java将会把JNI导向一个统一的Bridge，该方法将会根据提供的json中函数的Id，实现函数在C层面的绑定。
20. 对std命名空间的模板，需要穿透父类从而获取它们的Method，但是不应该穿透它的任何字段和非公有方法。
21. 符号&&并不代表双重引用，而是右值引用。
22. 对于`类型&`符号，代表它是一个引用，需要直接传入它的对象，而不是地址，但通过对指针进行取值，可以得到需要的对象。
23. 类型重解释（reinterpret_cast）在C++中不会检查类型是否兼容，因此需要谨慎使用，一般来说用它来传递指针。
24. 子类到父类的转换，应该使用static_cast，但通常不需要这么做。
25. 父类到子类的转换，应该使用dynamic_cast。
26. C++的protected和public的方法，可以通过FFI的Closure结合SubClass的方式重写。
27. 关于C++层面super的调用，可以通过cobject::SuperClass.method的方式实现，这些函数应该会被导出。

## 进展

我发现libclang的visitChildren是一种类似流式的方式处理AST的，因此需要小心的维护当前的处理范围，这种流式visitor的控制相当麻烦，需要仔细设计。

## C语言处理部分

- [ ] Union的解析
- [x] Struct的解析
- [x] Enum的解析
- [x] 基本类型的解析
- [x] Function的解析
- [ ] Metadata的生成
