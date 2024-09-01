参考资料

> https://subingwen.cn/linux/library/
>
> 

# 编译过程

```shell
1、预处理-Pre-Processing //.i文件
# -E 选项指示编译器仅对输入文件进行预处理
g++ -E test.cpp -o test.i //.i文件

2、编译-Compiling // .s文件
# -S 编译选项告诉 g++ 在为 C++ 代码产生了汇编语言文件后停止编译
# g++ 产生的汇编语言文件的缺省扩展名是 .s
g++ -S test.i -o test.s

3、汇编-Assembling // .o文件
# -c 选项告诉 g++ 仅把源代码编译为机器语言的目标代码
# 缺省时 g++ 建立的目标代码文件有一个 .o 的扩展名。
g++ -c test.s -o test.o

4、链接-Linking // bin文件
# -o 编译选项来为将产生的可执行文件用指定的文件名
g++ test.o -o test

```



## g++重要编译参数

1、-g 编译带调试信息的可执行文件  

```shell
# -g 选项告诉 GCC 产生能被 GNU 调试器GDB使用的调试信息，以调试程序。
# 产生带调试信息的可执行文件test
g++ -g test.cpp
```

2、-O[n] 优化源代码

```shell
## 所谓优化，例如省略掉代码中从未使用过的变量、直接将常量表达式用结果值代替等等，这些操作会缩减目标文件所包含的代码量，提高最终生成的可执行文件的运行效率。

# -O 选项告诉 g++ 对源代码进行基本优化。这些优化在大多数情况下都会使程序执行的更快。 -O2选项告诉 g++ 产生尽可能小和尽可能快的代码。 如-O2，-O3，-On（n 常为0–3）
# -O 同时减小代码的长度和执行时间，其效果等价于-O1
# -O0 表示不做优化
# -O1 为默认优化
# -O2 除了完成-O1的优化之外，还进行一些额外的调整工作，如指令调整等。
# -O3 则包括循环展开和其他一些与处理特性相关的优化工作。
# 选项将使编译的速度比使用 -O 时慢， 但通常产生的代码执行速度会更快。

# 使用 -O2优化源代码，并输出可执行文件
g++ -O2 test.cpp
```

3、-l 和 -L 指定库文件 | 指定库文件路径

```shell
# -l参数(小写)就是用来指定程序要链接的库，-l参数紧接着就是库名
# 在/lib和/usr/lib和/usr/local/lib里的库直接用-l参数就能链接

# 链接glog库
g++ -lglog test.cpp
# 通常不需要显式指定 -l 标志来链接标准库，比如 C 标准库 libc，-lc 通常不是必需的。

# 如果库文件没放在上面三个目录里，需要使用-L参数(大写)指定库文件所在目录
# -L参数跟着的是库文件所在的目录名

# 链接mytest库，libmytest.so在/home/bing/mytestlibfolder目录下
g++ -L/home/bing/mytestlibfolder -lmytest test.cpp
```

当你的程序需要链接多个库时，库的链接顺序可能会对最终生成的可执行文件产生影响。链接器按照指定的顺序处理库，这意味着如果一个库依赖于另一个库中定义的符号（函数或变量），那么被依赖的库必须先于依赖它的库被链接。

假设你有一个程序 `test.cpp`，它需要链接两个库 `libA.so` 和 `libB.so`。其中 `libB.so` 依赖于 `libA.so` 中定义的一些函数。在这种情况下，正确的链接命令应该是：

```sh
g++ -L/path/to/libA -L/path/to/libB -lA -lB test.cpp -o test
```

在这个命令中：
- `-L/path/to/libA` 指定了 `libA.so` 的位置。
- `-L/path/to/libB` 指定了 `libB.so` 的位置。
- `-lA` 表示链接 `libA.so`。
- `-lB` 表示链接 `libB.so`。
- `test.cpp` 是源代码文件。
- `-o test` 指定输出可执行文件名为 `test`。

因为 `libB.so` 依赖于 `libA.so`，所以 `-lA` 必须在 `-lB` 之前出现，以确保链接器能够先处理 `libA.so`，从而让 `libB.so` 能够正确地找到它所需的符号。

如果我们反向指定库的顺序，即先指定 `-lB` 后指定 `-lA`，则可能会遇到未定义符号的错误，因为 `libB.so` 尝试引用 `libA.so` 中的符号时，链接器尚未处理 `libA.so`：

```sh
g++ -L/path/to/libA -L/path/to/libB -lB -lA test.cpp -o test
```

这通常会导致类似这样的链接错误：
```shell
undefined reference to `function_in_libA'
```

如果多个库位于不同的目录，并且这些目录也需要按照一定的顺序指定，则可以使用多个 `-L` 参数来指定路径。例如：

```sh
g++ -L/path/to/libA -L/path/to/libB -L/path/to/libC -lA -lB -lC test.cpp -o test
```

在这个例子中，`/path/to/libA`、`/path/to/libB` 和 `/path/to/libC` 分别是 `libA.so`、`libB.so` 和 `libC.so` 的路径。如果 `libB.so` 依赖于 `libA.so`，而 `libC.so` 又依赖于 `libB.so`，那么库的链接顺序 `-lA -lB -lC` 是正确的。

总之，确保依赖关系正确的库先被链接是非常重要的，这样可以避免链接期间出现未定义符号的错误。

4、I 指定头文件搜索目录

```shell
# -I
# /usr/include目录一般是不用指定的，gcc知道去那里找，但是如果头文件不在/usr/icnclude
# 里我们就要用-I参数指定了，比如头文件放在/myinclude目录里，那编译命令行就要加上-
# I/myinclude 参数了，如果不加你会得到一个”xxxx.h: No such file or directory”的错
# 误。-I参数可以用相对路径，比如头文件在当前目录，可以用-I.来指定。上面我们提到的–cflags参
# 数就是用来生成-I参数的。

g++ -I/myinclude test.cpp
```

5、-Wall 打印警告信息  

```shell
# 打印出gcc提供的警告信息
g++ -Wall test.cpp
```

6、-w 关闭警告信息

```shell
# 关闭所有警告信息
g++ -w test.cpp
```

7、-std=c++11 设置编译标准

```shell
# 使用 c++11 标准编译 test.cpp
g++ -std=c++11 test.cpp
```

8、-o 指定输出文件名

```shell
# 指定即将产生的文件名

# 指定输出可执行文件名为test
g++ test.cpp -o test
```

9、-D 定义宏

```shell
# 在使用gcc/g++编译的时候定义宏

# 常用场景：
# -DDEBUG 定义DEBUG宏，可能文件中有DEBUG宏部分的相关信息，用个DDEBUG来选择开启或关闭DEBUG

示例代码
#include <stdio.h>
int main()
{
    #ifdef DEBUG
    	printf("DEBUG LOG\n");
    #endif
    	printf("in\n");
}
// 1. 在编译的时候，使用gcc -DDEBUG main.cpp
// DEBUG 被简单地定义为存在，而不是被赋予一个具体的值。
// -D 选项后面跟随的是宏名，如果想要给宏赋值，可以在宏名后加上 = 并跟随一个值。

// 2. printf("DEBUG LOG\n"); 代码可以被执行
```



# 库的本质

不管是Linux还是Windows中的库文件其本质和工作模式都是相同的，只不过在不同的平台上库对应的文件格式和文件后缀不同。程序中调用的库有两种 静态库和动态库，不管是哪种库文件本质是还是源文件，只不过是二进制格式只有计算机能够识别，作为一个普通人就无能为力了。

在项目中使用库一般有两个目的，一个是为了使程序更加简洁不需要在项目中维护太多的源文件，另一方面是为了源代码保密，毕竟不是所有人都想把自己编写的程序开源出来。

当我们拿到了库文件（动态库、静态库）之后要想使用还必须有这些库中提供的API函数的声明，也就是头文件，把这些都添加到项目中，就可以快乐的写代码了。

# 静态库

在Linux中静态库由程序 ar 生成，现在静态库已经不像之前那么普遍了，这主要是由于程序都在使用动态库。关于静态库的命名规则如下：

- 在Linux中静态库以lib作为前缀, 以.a作为后缀, 中间是库的名字自己指定即可, 即: libxxx.a
- 在Windows中静态库一般以lib作为前缀, 以lib作为后缀, 中间是库的名字需要自己指定, 即: libxxx.lib

## 生成静态链接库

生成静态库，需要先对源文件进行汇编操作 (使用参数 -c) 得到二进制格式的目标文件 (.o 格式), 然后在通过 ar工具将目标文件打包就可以得到静态库文件了 (libxxx.a)。

使用ar工具创建静态库的时候需要三个参数：

- 参数c：创建一个库，不管库是否存在，都将创建。
- 参数s：创建目标文件索引，这在创建较大的库时能加快时间。
- 参数r：在库中插入模块(替换)。默认新的成员添加在库的结尾处，如果模块名已经在库中存在，则替换同名的模块。

<img src="image/image-20240901093030492.png" alt="image-20240901093030492" style="zoom:67%;" />

生成静态链接库的具体步骤如下:

1、需要将源文件进行汇编, 得到 .o 文件, 需要使用参数 -c

```shell
# 执行如下操作, 默认生成二进制的 .o 文件
# -c 参数位置没有要求
$ gcc 源文件(*.c) -c	
```

2、将得到的 .o 进行打包, 得到静态库

```shell
$ ar rcs 静态库的名字(libxxx.a) 原材料(*.o)
```

3、发布静态库

```shell
# 发布静态库
1. 提供头文件 **.h
2. 提供制作出来的静态库 libxxx.a
```



## 静态库制作举例

### 准备测试程序

在某个目录中有如下的源文件, 用来实现一个简单的计算器：

```shell
# 目录结构 add.c div.c mult.c sub.c -> 算法的源文件, 函数声明在头文件 head.h
# main.c中是对接口的测试程序, 制作库的时候不需要将 main.c 算进去
.
├── add.c
├── div.c
├── include
│   └── head.h
├── main.c
├── mult.c
└── sub.c
```

加法计算源文件 add.c：

```c
#include <stdio.h>
#include "head.h"

int add(int a, int b)
{
    return a+b;
}
```

减法计算源文件 sub.c：

```c
#include <stdio.h>
#include "head.h"

int subtract(int a, int b)
{
    return a-b;
}
```

乘法计算源文件 mult.c：

```c
#include <stdio.h>
#include "head.h"

int multiply(int a, int b)
{
    return a*b;
}
```

减法计算的源文件 div.c：

```c
#include <stdio.h>
#include "head.h"

double divide(int a, int b)
{
    return (double)a/b;
}
```

头文件 head.h：

```c
#ifndef _HEAD_H
#define _HEAD_H
// 加法
int add(int a, int b);
// 减法
int subtract(int a, int b);
// 乘法
int multiply(int a, int b);
// 除法
double divide(int a, int b);
#endif
```

测试文件main.c：

```c
#include <stdio.h>
#include "head.h"

int main()
{
    int a = 20;
    int b = 12;
    printf("a = %d, b = %d\n", a, b);
    printf("a + b = %d\n", add(a, b));
    printf("a - b = %d\n", subtract(a, b));
    printf("a * b = %d\n", multiply(a, b));
    printf("a / b = %f\n", divide(a, b));
    return 0;
}
```







### 生成静态库



## 静态库的使用

# 动态库



## 生成动态链接库



## 动态库制作举例



## 动态库的使用



## 解决动态库无法加载问题

### 库的工作原理



### 动态链接器



### 解决方案



### 验证



# 优缺点

## 静态库





## 动态库