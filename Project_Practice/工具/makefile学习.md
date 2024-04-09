# makefile介绍

make命令执行时，需要一个makefile文件，以告诉make命令需要怎样的去编译和链接程序。

首先，我们用一个示例来说明makefile的书写规则，以便给大家一个感性认识。这个示例来源于GNU的make使用手册，在这个示例中，工程有8个c文件，和3个头文件，我们要写一个makefile来告诉make命令如何编译和链接这几个文件。规则是：

1. 如果这个工程没有编译过，那么所有c文件都要编译并被链接。
2. 如果这个工程的某几个c文件被修改，那么只编译被修改的c文件，并链接目标程序。
3. 如果这个工程的头文件被改变了，那么需要编译引用了该头文件的c文件，并链接目标程序。

只要我们的makefile写得够好，所有的这一切，只用一个make命令就可以完成，make命令会智能地根据当前的文件修改的情况来确定哪些文件需要重编译，从而自动编译所需要的文件和链接目标程序。

## makefile的规则

在讲述makefile之前，还是先来粗略地看一看makefile的基本规则。

```makefile
target ... : prerequisites ...
    recipe
    ...
    ...
```

- `target`可以是一个object file（目标文件），也可以是一个可执行文件，还可以是一个标签（label）。对于标签这种特性，在后续的“伪目标”章节中会有叙述。
- `prerequisites`生成该target所依赖的文件和/或target。
- `recipe`该target要执行的命令（任意的shell命令）。

这是一个文件的依赖关系，也就是说，target 这一个或多个的目标文件依赖于 prerequisites 中的文件，其生成规则定义在 command 中。说白一点就是说：prerequisites 中如果有文件比 target 文件要新的话，recipe 所定义的命令就会被执行。

这就是 makefile 的规则，也是 makefile 中最核心的内容。

说到底，makefile 的东西就这一点，好像这篇文档也该结束了，哈哈。还不尽然，这是 makefile 的主线和核心，但要写好一个 makefile 还不够，我会在后面一点一点地结合我的工作经验给你慢慢道来。内容还多着呢。:)

## 一个示例

正如前面所说，如果一个工程有3个头文件和8个C文件，为了完成前面所述的那三个规则，我们的 makefile 应该是下面的这个样子的。

```makefile
edit : main.o kbd.o command.o display.o \
        insert.o search.o files.o utils.o
    cc -o edit main.o kbd.o command.o display.o \
        insert.o search.o files.o utils.o

main.o : main.c defs.h
    cc -c main.c
kbd.o : kbd.c defs.h command.h
    cc -c kbd.c
command.o : command.c defs.h command.h
    cc -c command.c
display.o : display.c defs.h buffer.h
    cc -c display.c
insert.o : insert.c defs.h buffer.h
    cc -c insert.c
search.o : search.c defs.h buffer.h
    cc -c search.c
files.o : files.c defs.h buffer.h command.h
    cc -c files.c
utils.o : utils.c defs.h
    cc -c utils.c
clean :
    rm edit main.o kbd.o command.o display.o \
        insert.o search.o files.o utils.o
```

反斜杠（ `\` ）是换行符的意思。这样便于 makefile 的阅读。我们可以把这个内容保存在名字为“makefile”或“Makefile”的文件中，然后在该目录下直接输入命令 `make` 就可以生成执行文件 edit。如果要删除可执行文件和所有的中间目标文件，那么，只要简单地执行一下 `make clean` 就可以了。

在这个makefile中，目标文件（target）包含：可执行文件 edit 、中间目标文件（ `*.o` ），依赖文件（prerequisites）就是冒号后面的那些 `.c` 文件和 `.h` 文件。每一个 `.o` 文件都有一组依赖文件，而这些 `.o` 文件又是可执行文件 `edit` 的依赖文件。依赖关系的实质就是说明了目标文件是由哪些文件生成的。

在定义好依赖关系后，后续的 recipe 行定义了如何生成目标文件的操作系统命令，一定要以一个 `Tab` 键作为开头。记住，make并不管命令是怎么工作的，它只管执行所定义的命令。make会比较targets文件和prerequisites文件的修改日期，如果prerequisites文件的日期要比targets文件的日期要新，或者target不存在的话，那么，make就会执行后续定义的命令。

这里要说明一点的是， `clean` 不是一个文件，它只不过是一个动作名字，有点像C语言中的label一样，其冒号后什么也没有，那么，make就不会自动去找它的依赖性，也就不会自动执行其后所定义的命令。要执行其后的命令，就要在make命令后明显得指出这个label的名字。这样的方法非常有用，可以在一个makefile中定义不用编译或是和编译无关的命令，比如程序的打包，程序的备份，等等。

## make是如何工作的

在默认的方式下，也就是我们只输入 `make` 命令。那么，

1. make会在当前目录下找名字叫“Makefile”或“makefile”的文件。
2. 如果找到，它会找文件中的第一个目标文件（target），在上面的例子中，他会找到“edit”这个文件，并把这个文件作为最终的目标文件。
3. 如果edit文件不存在，或是edit所依赖的后面的 `.o` 文件的文件修改时间要比 `edit` 这个文件新，那么，它就会执行后面所定义的命令来生成 `edit` 这个文件。
4. 如果 `edit` 所依赖的 `.o` 文件也不存在，那么make会在当前文件中找目标为 `.o` 文件的依赖性，如果找到则再根据那一个规则生成 `.o` 文件。
5. 当然，你的C文件和头文件是存在的啦，于是make会生成 `.o` 文件，然后再用 `.o` 文件生成make的终极任务，也就是可执行文件 `edit` 。

这就是整个make的依赖性，make会一层又一层地去找文件的依赖关系，直到最终编译出第一个目标文件。在寻找的过程中，如果出现错误，比如最后被依赖的文件找不到，那么make就会直接退出，并报错，而对于所定义的命令的错误，或是编译不成功，make根本不理。make只管文件的依赖性，即，如果在找到依赖关系之后，冒号后面的文件不存在，那么对不起，我就不工作啦。

通过上述分析，我们知道，像clean这种，没有被第一个目标文件直接或间接关联，那么它后面所定义的命令将不会被自动执行，不过，我们可以显示要make执行。即命令—— `make clean` ，以此来清除所有的目标文件，以便重编译。

于是在我们编程中，如果这个工程已被编译过了，当我们修改了其中一个源文件，比如 `file.c` ，那么根据我们的依赖性，我们的目标 `file.o` 会被重编译，于是 `file.o` 的文件也是最新的，于是 `file.o` 的文件修改时间要比 `edit` 要新，所以 `edit` 也会被重新链接（详见 `edit` 目标文件后定义的命令）。

而如果我们改变了 `command.h` ，那么， `kdb.o` 、 `command.o` 和 `files.o` 都会被重编译，并且， `edit` 会被重链接。

## makefile中使用变量

在上面的例子中，先让我们看看edit的规则：

```makefile
edit : main.o kbd.o command.o display.o \
        insert.o search.o files.o utils.o
    cc -o edit main.o kbd.o command.o display.o \
        insert.o search.o files.o utils.o
```

可以看到 `.o` 文件的字符串被重复了两次，如果工程需要加入一个新的 `.o` 文件，那么需要在两个地方加（应该是三个地方，还有一个地方在clean中）。当然，我们的makefile并不复杂，所以在两个地方加也无所谓，但如果makefile变得复杂，那么就有可能会忘掉一个需要加入的地方，而导致编译失败。所以，为了makefile的易维护，在makefile中可以使用变量。makefile的变量也就是一个字符串，理解成C语言中的宏可能会更好。

比如，我们声明一个变量，叫 `objects` ， `OBJECTS` ， `objs` ， `OBJS` ， `obj` 或是 `OBJ` ，反正不管什么啦，只要能够表示obj文件就行了。在makefile一开始就这样定义：

```makefile
objects = main.o kbd.o command.o display.o \
     insert.o search.o files.o utils.o
```

于是，就可以很方便地在makefile中以 `$(objects)` 的方式来使用这个变量，于是改良版makefile就变成下面这个样子：

```makefile
objects = main.o kbd.o command.o display.o \
    insert.o search.o files.o utils.o

edit : $(objects)
    cc -o edit $(objects)
main.o : main.c defs.h
    cc -c main.c
kbd.o : kbd.c defs.h command.h
    cc -c kbd.c
command.o : command.c defs.h command.h
    cc -c command.c
display.o : display.c defs.h buffer.h
    cc -c display.c
insert.o : insert.c defs.h buffer.h
    cc -c insert.c
search.o : search.c defs.h buffer.h
    cc -c search.c
files.o : files.c defs.h buffer.h command.h
    cc -c files.c
utils.o : utils.c defs.h
    cc -c utils.c
clean :
    rm edit $(objects)
```

于是如果有新的 `.o` 文件加入，只需简单地修改一下 `objects` 变量就可以了。

关于变量更多的话题，我会在后续给你一一道来。

## 让make自动推导

GNU的make很强大，它可以自动推导文件以及文件依赖关系后面的命令，于是我们就没必要去在每一个 `.o` 文件后都写上类似的命令，因为，我们的make会自动识别，并自己推导命令。

只要make看到一个 `.o` 文件，它就会自动的把 `.c` 文件加在依赖关系中，如果make找到一个 `whatever.o` ，那么 `whatever.c` 就会是 `whatever.o` 的依赖文件。并且 `cc -c whatever.c` 也会被推导出来，于是，我们的makefile再也不用写得这么复杂。新makefile又出炉了。

```makefile
objects = main.o kbd.o command.o display.o \
    insert.o search.o files.o utils.o

edit : $(objects)
    cc -o edit $(objects)

main.o : defs.h
kbd.o : defs.h command.h
command.o : defs.h command.h
display.o : defs.h buffer.h
insert.o : defs.h buffer.h
search.o : defs.h buffer.h
files.o : defs.h buffer.h command.h
utils.o : defs.h

.PHONY : clean
clean :
    rm edit $(objects)
```

这种方法就是make的“隐式规则”。上面文件内容中， `.PHONY` 表示 `clean` 是个伪目标文件。

关于更为详细的“隐式规则”和“伪目标文件”，我会在后续给你一一道来。

## makefile的另一种风格

既然 make 可以自动推导命令，那么我看到那堆 `.o` 和 `.h` 的依赖就有点不爽，那么多的重复的 `.h` ，能不能把其收拢起来，好吧，没有问题，这个对于make来说很容易，谁叫它提供了自动推导命令和文件的功能呢？来看看最新风格的makefile吧。

```makefile
objects = main.o kbd.o command.o display.o \
    insert.o search.o files.o utils.o

edit : $(objects)
    cc -o edit $(objects)

$(objects) : defs.h
kbd.o command.o files.o : command.h
display.o insert.o search.o files.o : buffer.h

.PHONY : clean
clean :
    rm edit $(objects)
```

这里 `defs.h` 是所有目标文件的依赖文件， `command.h` 和 `buffer.h` 是对应目标文件的依赖文件。

这种风格能让我们的makefile变得很短，但我们的文件依赖关系就显得有点凌乱了。鱼和熊掌不可兼得。还看你的喜好了。我是不喜欢这种风格的，一是文件的依赖关系看不清楚，二是如果文件一多，要加入几个新的 `.o` 文件，那就理不清楚了。

## 清空目录的规则

每个Makefile中都应该写一个清空目标文件（ `.o` ）和可执行文件的规则，这不仅便于重编译，也很利于保持文件的清洁。这是一个“修养”（呵呵，还记得我的《编程修养》吗）。一般的风格都是：

```makefile
clean:
    rm edit $(objects)
```

更为稳健的做法是：

```makefile
.PHONY : clean
clean :
    -rm edit $(objects)
```

前面说过， `.PHONY` 表示 `clean` 是一个“伪目标”。而在 `rm` 命令前面加了一个小减号的意思就是，也许某些文件出现问题，不要管，继续做后面的事。当然， `clean` 的规则不要放在文件的开头，不然，这就会变成make的默认目标，相信谁也不愿意这样。不成文的规矩是——“clean从来都是放在文件的最后”。

上面就是一个makefile的概貌，也是makefile的基础，下面还有很多makefile的相关细节，准备好了吗？准备好了就来。

## Makefile里有什么？

Makefile里主要包含了五个东西：显式规则、隐式规则、变量定义、指令和注释。

1. 显式规则。显式规则说明了如何生成一个或多个目标文件。这是由Makefile的书写者明显指出要生成的文件、文件的依赖文件和生成的命令。
2. 隐式规则。由于make有自动推导的功能，所以隐式规则可以让我们比较简略地书写Makefile，这是由make所支持的。
3. 变量的定义。在Makefile中可以定义一系列的变量，变量一般都是字符串，这个有点像C语言中的宏，当Makefile被执行时，其中的变量都会被扩展到相应的引用位置上。
4. 指令。其包括了三个部分，一个是在一个Makefile中引用另一个Makefile，就像C语言中的include一样；另一个是指根据某些情况指定Makefile中的有效部分，就像C语言中的预编译#if一样；还有就是定义一个多行的命令。有关这一部分的内容，会在后续的部分中讲述。
5. 注释。Makefile中只有行注释，和UNIX的Shell脚本一样，其注释是用 `#` 字符，这个就像C/C++中的 `//` 一样。如果要在Makefile中使用 `#` 字符，可以用反斜杠进行转义，如： `\#` 。

最后，还值得一提的是，在Makefile中的命令，必须要以 `Tab` 键开始。

## Makefile的文件名

默认的情况下，make命令会在当前目录下按顺序寻找文件名为 `GNUmakefile` 、 `makefile` 和 `Makefile` 的文件。在这三个文件名中，最好使用 `Makefile` 这个文件名，因为这个文件名在排序上靠近其它比较重要的文件，比如 `README`。最好不要用 `GNUmakefile`，因为这个文件名只能由GNU `make` ，其它版本的 `make` 无法识别，但是基本上来说，大多数的 `make` 都支持 `makefile` 和 `Makefile` 这两种默认文件名。

当然，也可以使用别的文件名来书写Makefile，比如：“Make.Solaris”，“Make.Linux”等，如果要指定特定的Makefile，可以使用make的 `-f` 或 `--file` 参数，如： `make -f Make.Solaris` 或 `make --file Make.Linux` 。如果你使用多条 `-f` 或 `--file` 参数，可以指定多个makefile。

## 包含其它Makefile

在Makefile使用 `include` 指令可以把别的Makefile包含进来，这很像C语言的 `#include` ，被包含的文件会原模原样的放在当前文件的包含位置。 `include` 的语法是：

```makefile
include <filenames>...
```

`<filenames>` 可以是当前操作系统Shell的文件模式（可以包含路径和通配符）。

在 `include` 前面可以有一些空字符，但是绝不能是 `Tab` 键开始。 `include` 和 `<filenames>` 可以用一个或多个空格隔开。举个例子，有这样几个Makefile： `a.mk` 、 `b.mk` 、 `c.mk` ，还有一个文件叫 `foo.make` ，以及一个变量 `$(bar)` ，其包含了 `bish` 和 `bash` ，那么，下面的语句：

```makefile
include foo.make *.mk $(bar)
```

等价于：

```makefile
include foo.make a.mk b.mk c.mk bish bash
```

当 `make` 遇到 `include` 指令时，它会按照以下顺序寻找指定的文件：

1. **当前目录**：首先，在当前目录下寻找。
2. **通过 `-I` 或 `--include-dir` 参数指定的目录**：如果在执行 `make` 命令时指定了 `-I` 或 `--include-dir` 参数，`make` 会在这些指定的目录下寻找。
3. **预设搜索路径**：如果在上述位置都没有找到，`make` 会按照预设的路径顺序去搜索，这些路径包括但不限于 `<prefix>/include`（`<prefix>` 通常是 `/usr/local/bin`）、`/usr/gnu/include`、`/usr/local/include`、`/usr/include`。

使用环境变量 `.INCLUDE_DIRS` 可以查看或设置 `make` 搜索包含文件的目录列表。需要注意的是，使用 `-I` 参数会改变 `make` 的搜索路径，它会让 `make` 优先在 `-I` 参数指定的目录中搜索包含的 Makefile，这可能会导致忽略一些默认或者预设的路径。因此，如果需要包含来自这些默认目录的文件，应该小心使用 `-I` 参数。

正确使用 `include` 指令和 `-I` 参数有助于保持 `Makefile` 的组织性和可移植性，同时也能确保 `make` 能够正确地找到所有需要的文件。

如果有文件没有找到的话，make会生成一条警告信息，但不会马上出现致命错误。它会继续载入其它的文件，一旦完成makefile的读取，make会再重试这些没有找到，或是不能读取的文件，如果还是不行，make才会出现一条致命信息。如果想让make不理那些无法读取的文件，而继续执行，你可以在include前加一个减号“-”。如：

```makefile
-include <filenames>...
```

其表示，无论include过程中出现什么错误，都不要报错继续执行。如果要和其它版本 `make` 兼容，可以使用 `sinclude` 代替 `-include` 。

## 环境变量MAKEFILES=

如果当前环境中定义了环境变量 `MAKEFILES` ，那么make会把这个变量中的值做一个类似于 `include` 的动作。这个变量中的值是其它的Makefile，用空格分隔。只是，它和 `include` 不同的是，从这个环境变量中引入的Makefile的“目标”不会起作用，如果环境变量中定义的文件发现错误，make也会不理。

但是在这里我还是建议不要使用这个环境变量，因为只要这个变量一被定义，那么当你使用make时，所有的Makefile都会受到它的影响，这绝不是你想看到的。在这里提这个事，只是为了告诉大家，也许有时候你的Makefile出现了怪事，那么你可以看看当前环境中有没有定义这个变量。

## make的工作方式=

GNU的make工作时的执行步骤如下：（想来其它的make也是类似）

1. 读入所有的Makefile。
2. 读入被include的其它Makefile。
3. 初始化文件中的变量。
4. 推导隐式规则，并分析所有规则。
5. 为所有的目标文件创建依赖关系链。
6. 根据依赖关系，决定哪些目标要重新生成。
7. 执行生成命令。

1-5步为第一个阶段，6-7为第二个阶段。第一个阶段中，如果定义的变量被使用了，那么，make会把其展开在使用的位置。但make并不会马上完全展开，make使用的是拖延战术，如果变量出现在依赖关系的规则中，那么仅当这条依赖被决定要使用了，变量才会在其内部展开。

当然，这个工作方式你不一定要清楚，但是知道这个方式你也会对make更为熟悉。有了这个基础，后续部分也就容易看懂了。

# 书写规则

规则包含两个部分，一个是依赖关系，一个是生成目标的方法。

在Makefile中，规则的顺序是很重要的，因为，Makefile中只应该有一个最终目标，其它的目标都是被这个目标所连带出来的，所以一定要让make知道你的最终目标是什么。一般来说，定义在Makefile中的目标可能会有很多，但是第一条规则中的目标将被确立为最终的目标。如果第一条规则中的目标有很多个，那么，第一个目标会成为最终的目标。make所完成的也就是这个目标。

好了，还是让我们来看一看如何书写规则。

## 规则举例

```makefile
foo.o: foo.c defs.h       # foo模块
    cc -c -g foo.c
```

看到这个例子，各位应该不是很陌生了，前面也已说过， `foo.o` 是我们的目标， `foo.c` 和 `defs.h` 是目标所依赖的源文件，而只有一个命令 `cc -c -g foo.c` （以Tab键开头）。这个规则告诉我们两件事：

1. 文件的依赖关系， `foo.o` 依赖于 `foo.c` 和 `defs.h` 的文件，如果 `foo.c` 和 `defs.h` 的文件日期要比 `foo.o` 文件日期要新，或是 `foo.o` 不存在，那么依赖关系发生。
2. 生成或更新 `foo.o` 文件，就是那个 cc 命令。它说明了如何生成 `foo.o` 这个文件。（当然，foo.c 文件 include 了defs.h 文件）

## 规则的语法

```makefile
targets : prerequisites
    command
    ...
```

或是这样：

```makefile
targets : prerequisites ; command
    command
    ...
```

targets 是文件名，以空格分开，可以使用通配符。一般来说，我们的目标基本上是一个文件，但也有可能是多个文件。

command 是命令行，如果其不与“target : prerequisites”在一行，那么，必须以 `Tab` 键开头，如果和 prerequisites在一行，那么可以用分号做为分隔。（见上）

prerequisites 也就是目标所依赖的文件（或依赖目标）。如果其中的某个文件要比目标文件要新，那么，目标就被认为是“过时的”，被认为是需要重生成的。这个在前面已经讲过了。

如果命令太长，你可以使用反斜杠（ `\` ）作为换行符。make对一行上有多少个字符没有限制。规则告诉make两件事，文件的依赖关系和如何生成目标文件。

一般来说，make会以UNIX的标准Shell，也就是 `/bin/sh` 来执行命令。

## 在规则中使用通配符

如果我们想定义一系列比较类似的文件，我们很自然地就想起使用通配符。make支持三个通配符： `*` ， `?` 和 `~` 。这是和Unix的B-Shell是相同的。

波浪号（ `~` ）字符在文件名中也有比较特殊的用途。如果是 `~/test` ，这就表示当前用户的 `$HOME` 目录下的test目录。而 `~hchen/test` 则表示用户hchen的宿主目录下的test 目录。（这些都是Unix下的小知识了，make也支持）而在Windows或是 MS-DOS下，用户没有宿主目录，那么波浪号所指的目录则根据环境变量“HOME”而定。

通配符代替了一系列的文件，如 `*.c` 表示所有后缀为c的文件。需要注意的是，如果我们的文件名中有通配符，如： `*` ，那么可以用转义字符 `\` ，如 `\*` 来表示真实的 `*` 字符，而不是任意长度的字符串。

好吧，还是先来看几个例子吧：

```makefile
clean:
    rm -f *.o
```

其实在这个 `clean:` 后面可以加上你想做的一些事情，比如想在编译完后看看main.c的源代码，你可以在加上cat这个命令，例子如下：

```makefile
clean:
    cat main.c
    rm -f *.o
```

其结果试一下就知道的。 上面这个例子不多说了，这是操作系统Shell所支持的通配符。

```makefile
print: *.c
    lpr -p $?
    touch print
```

上面这个例子说明了通配符也可以在我们的规则中，目标print依赖于所有的 `.c` 文件。其中的 `$?` 是一个自动变量，它表示依赖列表中所有比目标文件更新的文件名，以空格分隔。换句话说，这个变量包含了所有自上次构建以来发生了改变的依赖文件。

具体来说：

* `lpr -p $?`：这一行命令会打印所有已更新的 `.c` 源文件。`lpr` 是一个命令行工具，用于发送文件到打印队列。`-p` 参数可能是指定给 `lpr` 的一个选项，具体含义取决于系统和 `lpr` 的版本。
* `touch print`：这一行命令会更新 `print` 文件的时间戳，无论 `print` 是否存在。这通常是为了标记目标已经被构建或处理过了，以避免在下一次 `make` 调用时重新执行相同的命令，除非 `.c` 文件又一次发生了变化。这在 Makefile 中有一个特别的用途。它通常被用来标记一个规则的目标已经被成功处理了。`make` 命令依赖于文件的时间戳来决定是否需要重新执行某个规则。如果依赖文件的时间戳比目标文件新，`make` 会重新执行规则。

```makefile
objects = *.o
```

上面这个例子，表示通配符同样可以用在变量中。通配符将在 Makefile 的规则中使用 `$(objects)` 时被展开。如果你要让通配符在变量中展开，也就是让objects的值是所有 `.o` 的文件名的集合，那么，你可以这样：

```makefile
objects := $(wildcard *.o)
```

Makefile 中的 `$(wildcard *.o)` 函数会在 Makefile 被解析时立即展开 `*.o`，将当前目录下所有以 `.o` 结尾的文件名赋值给 `objects` 变量。这意味着 `objects` 变量将包含一个由空格分隔的 `.o` 文件列表。

另给一个变量使用通配符的例子：

1. 列出一确定文件夹中的所有 `.c` 文件。
   ```makefile
   objects := $(wildcard *.c)
   ```
2. 列出(1)中所有文件对应的 `.o` 文件，在（3）中我们可以看到它是由make自动编译出的:
   ```makefile
   $(patsubst %.c,%.o,$(wildcard *.c))
   ```
3. 由(1)(2)两步，可写出编译并链接所有 `.c` 和 `.o` 文件
   ```makefile
   objects := $(patsubst %.c,%.o,$(wildcard *.c))
   foo : $(objects)
       cc -o foo $(objects)
   ```

这种用法由关键字“wildcard”，“patsubst”指出，关于Makefile的关键字，我们将在后面讨论。

## 文件搜寻=

在一些大的工程中，有大量的源文件，我们通常的做法是把这许多的源文件分类，并存放在不同的目录中。所以，当make需要去找寻文件的依赖关系时，你可以在文件前加上路径，但最好的方法是把一个路径告诉make，让make在自动去找。

Makefile文件中的特殊变量 `VPATH` 就是完成这个功能的，如果没有指明这个变量，make只会在当前的目录中去找寻依赖文件和目标文件。如果定义了这个变量，那么，make就会在当前目录找不到的情况下，到所指定的目录中去找寻文件了。

```makefile
VPATH = src:../headers
```

上面的定义指定两个目录，“src”和“../headers”，make会按照这个顺序进行搜索。目录由“冒号”分隔。（当然，当前目录永远是最高优先搜索的地方）

另一个设置文件搜索路径的方法是使用make的“vpath”关键字（注意，它是全小写的），这不是变量，这是一个make的关键字，这和上面提到的那个VPATH变量很类似，但是它更为灵活。它可以指定不同的文件在不同的搜索目录中。这是一个很灵活的功能。它的使用方法有三种：

- `vpath <pattern> <directories>` ：为符合模式 `<pattern>`的文件指定搜索目录 `<directories>`。
- `vpath <pattern>` ：清除符合模式 `<pattern>`的文件的搜索目录。
- `vpath` ：清除所有已被设置好了的文件搜索目录。

vpath使用方法中的 `<pattern>`需要包含 `%` 字符。 `%` 的意思是匹配零或若干字符，（需引用 `%` ，使用 `\` ）例如， `%.h` 表示所有以 `.h` 结尾的文件。`<pattern>`指定了要搜索的文件集，而 `<directories>`则指定了< pattern>的文件集的搜索的目录。例如：

```makefile
vpath %.h ../headers
```

该语句表示，要求make在“../headers”目录下搜索所有以 `.h` 结尾的文件。（如果某文件在当前目录没有找到的话）

我们可以连续地使用vpath语句，以指定不同搜索策略。如果连续的vpath语句中出现了相同的 `<pattern>` ，或是被重复了的 `<pattern>`，那么，make会按照vpath语句的先后顺序来执行搜索。如：

```makefile
vpath %.c foo
vpath %   blish
vpath %.c bar
```

其表示 `.c` 结尾的文件，先在“foo”目录，然后是“blish”，最后是“bar”目录。

```makefile
vpath %.c foo:bar
vpath %   blish
```

而上面的语句则表示 `.c` 结尾的文件，先在“foo”目录，然后是“bar”目录，最后才是“blish”目录。

## 伪目标

最早先的一个例子中，我们提到过一个“clean”的目标，这是一个“伪目标”，

```makefile
clean:
    rm *.o temp
```

正像我们前面例子中的“clean”一样，既然我们生成了许多文件编译文件，我们也应该提供一个清除它们的“目标”以备完整地重编译而用。 （以“make clean”来使用该目标）

因为，我们并不生成“clean”这个文件。“伪目标”并不是一个文件，只是一个标签，由于“伪目标”不是文件，所以make无法生成它的依赖关系和决定它是否要执行。我们只有通过显式地指明这个“目标”才能让其生效。当然，“伪目标”的取名不能和文件名重名，不然其就失去了“伪目标”的意义了。

当然，为了避免和文件重名的这种情况，我们可以使用一个特殊的标记“.PHONY”来显式地指明一个目标是“伪目标”，向make说明，不管是否有这个文件，这个目标就是“伪目标”。

```makefile
.PHONY : clean
```

只要有这个声明，不管是否有“clean”文件，要运行“clean”这个目标，只有“make clean”这样。于是整个过程可以这样写：

```makefile
.PHONY : clean
clean :
    rm *.o temp
```

伪目标一般没有依赖的文件。但是，我们也可以为伪目标指定所依赖的文件。伪目标同样可以作为“默认目标”，只要将其放在第一个。一个示例就是，如果你的Makefile需要一口气生成若干个可执行文件，但你只想简单地敲一个make完事，并且，所有的目标文件都写在一个Makefile中，那么你可以使用“伪目标”这个特性：

```makefile
all : prog1 prog2 prog3
.PHONY : all

prog1 : prog1.o utils.o
    cc -o prog1 prog1.o utils.o

prog2 : prog2.o
    cc -o prog2 prog2.o

prog3 : prog3.o sort.o utils.o
    cc -o prog3 prog3.o sort.o utils.o
```

我们知道，Makefile中的第一个目标会被作为其默认目标。我们声明了一个“all”的伪目标，其依赖于其它三个目标。由于默认目标的特性是，总是被执行的，但由于“all”又是一个伪目标，伪目标只是一个标签不会生成文件，所以不会有“all”文件产生。于是，其它三个目标的规则总是会被执行。也就达到了我们一口气生成多个目标的目的。 `.PHONY : all` 声明了“all”这个目标为“伪目标”。（注：这里的显式“.PHONY : all” 不写的话一般情况也可以正确的执行，这样make可通过隐式规则推导出， “all” 是一个伪目标，执行make不会生成“all”文件，而执行后面的多个目标。建议：显式写出是一个好习惯。）

随便提一句，从上面的例子我们可以看出，目标也可以成为依赖。所以，伪目标同样也可成为依赖。看下面的例子：

```makefile
.PHONY : cleanall cleanobj cleandiff

cleanall : cleanobj cleandiff
    rm program

cleanobj :
    rm *.o

cleandiff :
    rm *.diff
```

“make cleanall”将清除所有要被清除的文件。“cleanobj”和“cleandiff”这两个伪目标有点像“子程序”的意思。我们可以输入“make cleanall”和“make cleanobj”和“make cleandiff”命令来达到清除不同种类文件的目的。

## 多目标

Makefile的规则中的目标可以不止一个，其支持多目标，有可能我们的多个目标同时依赖于一个文件，并且其生成的命令大体类似。于是我们就能把其合并起来。当然，多个目标的生成规则的执行命令不是同一个，这可能会给我们带来麻烦，不过好在我们可以使用一个自动变量 `$@` （自动变量 `$@` 指的是当前规则中的目标名（Target name）。它对应于规则左边冒号 `:` 的名称。自动变量在规则的命令中被展开，即代表了正在被构建或是应该被更新的文件名称）。看一个例子吧。

```makefile
bigoutput littleoutput : text.g
    generate text.g -$(subst output,,$@) > $@
```

命令行中的 `$@` 将分别被 `bigoutput` 和 `littleoutput` 替换，这取决于 Make 正在构建哪个目标。

* `generate text.g -$(subst output,,$@)` 是调用 `generate` 命令，并将 `text.g` 作为参数传递给它。同时，使用 `subst` 函数替换 `$@` 中的 `output` 文本为空字符串，这样如果 `$@` 是 `bigoutput`，那么替换结果是 `big`；如果 `$@` 是 `littleoutput`，替换结果是 `little`。

* `>` 是重定向操作符，它将 `generate` 命令的输出重定向到 `$@` 指定的文件中，即 `bigoutput` 或 `littleoutput`。

上述规则等价于：

```makefile
bigoutput : text.g
    generate text.g -big > bigoutput
littleoutput : text.g
    generate text.g -little > littleoutput
```


## 静态模式=

静态模式可以更加容易地定义多目标的规则，可以让我们的规则变得更加的有弹性和灵活。我们还是先来看一下语法：

```makefile
<targets ...> : <target-pattern> : <prereq-patterns ...>
    <commands>
    ...
```

targets定义了一系列的目标文件，可以有通配符。是目标的一个集合。

target-pattern是指明了targets的模式，也就是的目标集模式。

prereq-patterns是目标的依赖模式，它对target-pattern形成的模式再进行一次依赖目标的定义。

这样描述这三个东西，可能还是没有说清楚，还是举个例子来说明一下吧。如果我们的 `<target-pattern>`定义成 `%.o` ，意思是我们的 `<target>`;集合中都是以 `.o` 结尾的，而如果我们的 `<prereq-patterns>`定义成 `%.c` ，意思是对 `<target-pattern>`所形成的目标集进行二次定义，其计算方法是，取 `<target-pattern>`模式中的 `%` （也就是去掉了 `.o` 这个结尾），并为其加上 `.c` 这个结尾，形成的新集合。

所以，我们的“目标模式”或是“依赖模式”中都应该有 `%` 这个字符，如果你的文件名中有 `%` 那么你可以使用反斜杠 `\` 进行转义，来标明真实的 `%` 字符。

看一个例子：

```makefile
objects = foo.o bar.o

all: $(objects)

$(objects): %.o: %.c
    $(CC) -c $(CFLAGS) $< -o $@
```

上面的例子中，指明了我们的目标从$object中获取， `%.o` 表明要所有以 `.o` 结尾的目标，也就是 `foo.o bar.o` ，也就是变量 `$object `集合的模式，而依赖模式`%.c `则取模式`%.o `的`%`，也就是`foo `` ``bar `，并为其加下`.c `的后缀，于是，我们的依赖目标就是`foo.c `` ``bar.c `。而命令中的`$<` 和 `$@`则是自动化变量，`$<` 表示第一个依赖文件， `$@` 表示目标集（也就是“foo.o bar.o”）。于是，上面的规则展开后等价于下面的规则：

```makefile
foo.o : foo.c
    $(CC) -c $(CFLAGS) foo.c -o foo.o
bar.o : bar.c
    $(CC) -c $(CFLAGS) bar.c -o bar.o
```

试想，如果我们的 `%.o` 有几百个，那么我们只要用这种很简单的“静态模式规则”就可以写完一堆规则，实在是太有效率了。“静态模式规则”的用法很灵活，如果用得好，那会是一个很强大的功能。再看一个例子：

```makefile
files = foo.elc bar.o lose.o

$(filter %.o,$(files)): %.o: %.c
    $(CC) -c $(CFLAGS) $< -o $@
$(filter %.elc,$(files)): %.elc: %.el
    emacs -f batch-byte-compile $<
```

`$(filter %.o,$(files))`表示调用Makefile的filter函数，过滤“$files”集，只要其中模式为“%.o”的内容。其它的内容，我就不用多说了吧。这个例子展示了Makefile中更大的弹性。

## 自动生成依赖性=

在Makefile中，我们的依赖关系可能会需要包含一系列的头文件，比如，如果我们的main.c中有一句 `#include "defs.h"` ，那么我们的依赖关系应该是：

```makefile
main.o : main.c defs.h
```

但是，如果是一个比较大型的工程，你必需清楚哪些C文件包含了哪些头文件，并且，你在加入或删除头文件时，也需要小心地修改Makefile，这是一个很没有维护性的工作。为了避免这种繁重而又容易出错的事情，我们可以使用C/C++编译的一个功能。大多数的C/C++编译器都支持一个“-M”的选项，即自动找寻源文件中包含的头文件，并生成一个依赖关系。例如，如果我们执行下面的命令:

```makefile
cc -M main.c
```

其输出是：

```makefile
main.o : main.c defs.h
```

于是由编译器自动生成的依赖关系，这样一来，你就不必再手动书写若干文件的依赖关系，而由编译器自动生成了。需要提醒一句的是，如果你使用GNU的C/C++编译器，你得用 `-MM` 参数，不然， `-M` 参数会把一些标准库的头文件也包含进来。

gcc -M main.c的输出是:

```shell
main.o: main.c defs.h /usr/include/stdio.h /usr/include/features.h \
    /usr/include/sys/cdefs.h /usr/include/gnu/stubs.h \
    /usr/lib/gcc-lib/i486-suse-linux/2.95.3/include/stddef.h \
    /usr/include/bits/types.h /usr/include/bits/pthreadtypes.h \
    /usr/include/bits/sched.h /usr/include/libio.h \
    /usr/include/_G_config.h /usr/include/wchar.h \
    /usr/include/bits/wchar.h /usr/include/gconv.h \
    /usr/lib/gcc-lib/i486-suse-linux/2.95.3/include/stdarg.h \
    /usr/include/bits/stdio_lim.h
```

gcc -MM main.c的输出则是:

```makefile
main.o: main.c defs.h
```

那么，编译器的这个功能如何与我们的Makefile联系在一起呢。因为这样一来，我们的Makefile也要根据这些源文件重新生成，让 Makefile 自己依赖于源文件？这个功能并不现实，不过我们可以有其它手段来迂回地实现这一功能。GNU组织建议把编译器为每一个源文件的自动生成的依赖关系放到一个文件中，为每一个 `name.c` 的文件都生成一个 `name.d` 的Makefile文件， `.d` 文件中就存放对应 `.c` 文件的依赖关系。

于是，我们可以写出 `.c` 文件和 `.d` 文件的依赖关系，并让make自动更新或生成 `.d` 文件，并把其包含在我们的主Makefile中，这样，我们就可以自动化地生成每个文件的依赖关系了。

这里，我们给出了一个模式规则来产生 `.d` 文件：

```
%.d:%.c
@set-e;rm-f$@;\
$(CC)-M$(CPPFLAGS)$<>$@.$$$$;\
sed's,\($*\)\.o[ :]*,\1.o $@ : ,g'<$@.$$$$>$@;\
rm-f$@.$$$$
```

这个规则的意思是，所有的 `.d` 文件依赖于 `.c` 文件， `rm -f $@` 的意思是删除所有的目标，也就是 `.d` 文件，第二行的意思是，为每个依赖文件 `$<` ，也就是 `.c` 文件生成依赖文件， `$@` 表示模式 `%.d` 文件，如果有一个C文件是name.c，那么 `%` 就是 `name` ， `$$$$` 意为一个随机编号，第二行生成的文件有可能是“name.d.12345”，第三行使用sed命令做了一个替换，关于sed命令的用法请参看相关的使用文档。第四行就是删除临时文件。

总而言之，这个模式要做的事就是在编译器生成的依赖关系中加入 `.d` 文件的依赖，即把依赖关系：

```
main.o :main.c defs.h
```

转成：

```
main.o main.d :main.c defs.h
```

于是，我们的 `.d` 文件也会自动更新了，并会自动生成了，当然，你还可以在这个 `.d` 文件中加入的不只是依赖关系，包括生成的命令也可一并加入，让每个 `.d` 文件都包含一个完整的规则。一旦我们完成这个工作，接下来，我们就要把这些自动生成的规则放进我们的主Makefile中。我们可以使用Makefile的“include”命令，来引入别的Makefile文件（前面讲过），例如：

```
sources=foo.cbar.c

include $(sources:.c=.d)
```

上述语句中的 `$(sources:.c=.d)` 中的 `.c=.d` 的意思是做一个替换，把变量 `$(sources)` 所有 `.c` 的字串都替换成 `.d` ，关于这个“替换”的内容，在后面我会有更为详细的讲述。当然，你得注意次序，因为include是按次序来载入文件，最先载入的 `.d` 文件中的目标会成为默认目标。

# 书写命令

# 使用变量

## 变量的基础

变量在声明时需要给予初值，而在使用时，需要给在变量名前加上 `$` 符号，但最好用小括号 `()` 或是大括号 `{}` 把变量给包括起来。如果你要使用真实的 `$` 字符，那么你需要用 `$$` 来表示。

变量可以使用在许多地方，如规则中的“目标”、“依赖”、“命令”以及新的变量中。先看一个例子：

```makefile
objects = program.o foo.o utils.o
program : $(objects)
    cc -o program $(objects)

$(objects) : defs.h
```

变量会在使用它的地方精确地展开，就像C/C++中的宏一样，例如：

```makefile
foo = c
prog.o : prog.$(foo)
    $(foo)$(foo) -$(foo) prog.$(foo)
```

展开后得到：

```makefile
prog.o : prog.c
    cc -c prog.c
```

当然，千万不要在你的Makefile中这样干，这里只是举个例子来表明Makefile中的变量在使用处展开的真实样子。可见其就是一个“替代”的原理。

另外，给变量加上括号完全是为了更加安全地使用这个变量，在上面的例子中，如果你不想给变量加上括号，那也可以，但我还是强烈建议你给变量加上括号。

## 变量中的变量

在定义变量的值时，我们可以使用其它变量来构造变量的值，在Makefile中有两种方式来用变量定义变量的值。

先看第一种方式，也就是简单的使用 `=` 号，在 `=` 左侧是变量，右侧是变量的值，右侧变量的值可以定义在文件的任何一处，也就是说，右侧中的变量不一定非要是已定义好的值，其也可以使用后面定义的值。如：

```makefile
foo = $(bar)
bar = $(ugh)
ugh = Huh?

all:
    echo $(foo)
```

我们执行“make all”将会打出变量 `$(foo)` 的值是 `Huh?` （ `$(foo)` 的值是 `$(bar)` ， `$(bar)` 的值是 `$(ugh)` ， `$(ugh)` 的值是 `Huh?` ）可见，变量是可以使用后面的变量来定义的。

这个功能有好的地方，也有不好的地方，好的地方是，我们可以把变量的真实值推到后面来定义，如：

```makefile
CFLAGS = $(include_dirs) -O
include_dirs = -Ifoo -Ibar
```

当 `CFLAGS` 在命令中被展开时，会是 `-Ifoo -Ibar -O` 。但这种形式也有不好的地方，那就是递归定义，如：

```makefile
CFLAGS = $(CFLAGS) -O
```

或：

```makefile
A = $(B)
B = $(A)
```

这会让make陷入无限的变量展开过程中去，当然，make是有能力检测这样的定义，并会报错。还有就是如果在变量中使用函数，那么，这种方式会让make运行时非常慢，更糟糕的是，会使两个make的函数“wildcard”和“shell”发生不可预知的错误。因为不知道这两个函数会被调用多少次。

为了避免上面的这种方法，我们可以使用make中另一种用变量来定义变量的方法。这种方法使用的是 `:=` 操作符，如：

```makefile
x := foo
y := $(x) bar
x := later
```

其等价于：

```makefile
y := foo bar
x := later
```

值得一提的是，这种方法，前面的变量不能使用后面的变量，只能使用前面已定义好了的变量。如果是这样：

```makefile
y := $(x) bar
x := foo
```

那么，y的值是“bar”，而不是“foo bar”。

上面都是一些比较简单的变量使用了，让我们来看一个复杂的例子，其中包括了make的函数、条件表达式和一个系统变量“MAKELEVEL”的使用：

```makefile
ifeq (0,${MAKELEVEL})
cur-dir   := $(shell pwd)
whoami    := $(shell whoami)
host-type := $(shell arch)
MAKE := ${MAKE} host-type=${host-type} whoami=${whoami}
endif
```

关于条件表达式和函数，我们后面再说，对于系统变量“MAKELEVEL”，其意思是，如果我们的make有一个嵌套执行的动作（参见前面的“嵌套使用make”），那么，这个变量会记录了我们的当前Makefile的调用层数。

下面再介绍两个定义变量时需要知道的，请先看一个例子，如果我们要定义一个变量，其值是一个空格，那么我们可以这样来：

```makefile
nullstring :=
space := $(nullstring) # end of the line
```

nullstring是一个Empty变量，其中什么也没有，而我们的space的值是一个空格。因为在操作符的右边是很难描述一个空格的，这里采用的技术很管用，先用一个Empty变量来标明变量的值开始了，而后面采用“#”注释符来表示变量定义的终止，这样，我们可以定义出其值是一个空格的变量。请注意这里关于“#”的使用，注释符“#”的这种特性值得我们注意，如果我们这样定义一个变量：

```makefile
dir := /foo/bar    # directory to put the frobs in
```

dir这个变量的值是“/foo/bar”，后面还跟了4个空格，如果我们这样使用这个变量来指定别的目录——“$(dir)/file”那么就完蛋了。

还有一个比较有用的操作符是 `?=` ，先看示例：

```makefile
FOO ?= bar
```

其含义是，如果FOO没有被定义过，那么变量FOO的值就是“bar”，如果FOO先前被定义过，那么这条语将什么也不做，其等价于：

```makefile
ifeq ($(origin FOO), undefined)
    FOO = bar
endif
```

# 使用条件判断

使用条件判断，可以让make根据运行时的不同情况选择不同的执行分支。条件表达式可以是比较变量的值，或是比较变量和常量的值。

## 示例

下面的例子，判断 `$(CC)` 变量是否 `gcc` ，如果是的话，则使用GNU函数编译目标。

```makefile
libs_for_gcc = -lgnu
normal_libs =

foo: $(objects)
ifeq ($(CC),gcc)
    $(CC) -o foo $(objects) $(libs_for_gcc)
else
    $(CC) -o foo $(objects) $(normal_libs)
endif
```

可见，在上面示例的这个规则中，目标 `foo` 可以根据变量 `$(CC)` 值来选取不同的函数库来编译程序。

我们可以从上面的示例中看到三个关键字： `ifeq` 、 `else` 和 `endif` 。 `ifeq` 的意思表示条件语句的开始，并指定一个条件表达式，表达式包含两个参数，以逗号分隔，表达式以圆括号括起。 `else` 表示条件表达式为假的情况。 `endif` 表示一个条件语句的结束，任何一个条件表达式都应该以 `endif` 结束。

当我们的变量 `$(CC)` 值是 `gcc` 时，目标 `foo` 的规则是：

```makefile
foo: $(objects)
    $(CC) -o foo $(objects) $(libs_for_gcc)
```

而当我们的变量 `$(CC)` 值不是 `gcc` 时（比如 `cc` ），目标 `foo` 的规则是：

```makefile
foo: $(objects)
    $(CC) -o foo $(objects) $(normal_libs)
```

当然，我们还可以把上面的那个例子写得更简洁一些：

```makefile
libs_for_gcc = -lgnu
normal_libs =

ifeq ($(CC),gcc)
    libs=$(libs_for_gcc)
else
    libs=$(normal_libs)
endif

foo: $(objects)
    $(CC) -o foo $(objects) $(libs)
```

# 使用函数

在Makefile中可以使用函数来处理变量，从而让我们的命令或是规则更为的灵活和具有智能。make 所支持的函数也不算很多，不过已经足够我们的操作了。函数调用后，函数的返回值可以当做变量来使用。

## 函数的调用语法

函数调用，很像变量的使用，也是以 `$` 来标识的，其语法如下：

```makefile
$(<function> <arguments>)
```

或是:

```makefile
${<function> <arguments>}
```

这里， `<function>` 就是函数名，make支持的函数不多。 `<arguments>` 为函数的参数，参数间以逗号 `,` 分隔，而函数名和参数之间以“空格”分隔。函数调用以 `$` 开头，以圆括号或花括号把函数名和参数括起。感觉很像一个变量，是不是？函数中的参数可以使用变量，为了风格的统一，函数和变量的括号最好一样，如使用 `$(subst a,b,$(x))` 这样的形式，而不是 `$(subst a,b, ${x})` 的形式。因为统一会更清楚，也会减少一些不必要的麻烦。

还是来看一个示例：

```makefile
comma:= ,
empty:=
space:= $(empty) $(empty)
foo:= a b c
bar:= $(subst $(space),$(comma),$(foo))
```

在这个示例中， `$(comma)` 的值是一个逗号。 `$(space)` 使用了 `$(empty)` 定义了一个空格， `$(foo)` 的值是 `a b c` ， `$(bar)` 的定义用，调用了函数 `subst` ，这是一个替换函数，这个函数有三个参数，第一个参数是被替换字串，第二个参数是替换字串，第三个参数是替换操作作用的字串。这个函数也就是把 `$(foo)` 中的空格替换成逗号，所以 `$(bar)` 的值是 `a,b,c` 。

## 字符串处理函数

### subst

```makefile
$(subst <from>,<to>,<text>)
```

* 名称：字符串替换函数
* 功能：把字串 `<text>` 中的 `<from>` 字符串替换成 `<to>` 。
* 返回：函数返回被替换过后的字符串。
* 示例：
  > ```makefile
  > $(subst ee,EE,feet on the street)
  > ```
  >

把 `feet on the street` 中的 `ee` 替换成 `EE` ，返回结果是 `fEEt on the strEEt` 。

# make 的运行

一般来说，最简单的就是直接在命令行下输入make命令，make命令会找当前目录的makefile来执行，一切都是自动的。但也有时你也许只想让make重编译某些文件，而不是整个工程，而又有的时候你有几套编译规则，你想在不同的时候使用不同的编译规则，等等。本章节就是讲述如何使用make命令的。

## make的退出码

make命令执行后有三个退出码：

- 0 表示成功执行。
- 1 如果make运行时出现任何错误，其返回1。
- 2 如果你使用了make的“-q”选项，并且make使得一些目标不需要更新，那么返回2。

Make的相关参数我们会在后续章节中讲述。

## 指定Makefile

前面我们说过，GNU make找寻默认的Makefile的规则是在当前目录下依次找三个文件——“GNUmakefile”、“makefile”和“Makefile”。其按顺序找这三个文件，一旦找到，就开始读取这个文件并执行。

当前，我们也可以给make命令指定一个特殊名字的Makefile。要达到这个功能，我们要使用make的 `-f` 或是 `--file` 参数（ `--makefile` 参数也行）。例如，我们有个makefile的名字是“hchen.mk”，那么，我们可以这样来让make来执行这个文件：

```
make –f hchen.mk
```

如果在make的命令行中不只一次地使用了 `-f` 参数，那么，所有指定的makefile将会被连在一起传递给make执行。

## 指定目标

一般来说，make的最终目标是makefile中的第一个目标，而其它目标一般是由这个目标连带出来的。这是make的默认行为。当然，一般来说，makefile中的第一个目标是由许多个目标组成，你可以指示make，让其完成你所指定的目标。要达到这一目的很简单，需在make命令后直接跟目标的名字就可以完成（如前面提到的“make clean”形式）。

任何在makefile中的目标都可以被指定成终极目标，但是除了以 `- ` 打头，或是包含了 `= ` 的目标，因为有这些字符的目标，会被解析成命令行参数或是变量。甚至没有被我们明确写出来的目标也可以成为make的终极目标，也就是说，只要make可以找到其隐含规则推导规则，那么这个隐含目标同样可以被指定成终极目标。

有一个make的环境变量叫 `MAKECMDGOALS ` ，这个变量中会存放你所指定的终极目标的列表，如果在命令行上，你没有指定目标，那么，这个变量是空值。这个变量可以使用在一些比较特殊的情形下。比如下面的例子：

```makefile
sources = foo.c bar.c
ifneq ( $(MAKECMDGOALS),clean)
    include $(sources:.c=.d)
endif
```

基于上面的这个例子，只要我们输入的命令不是“make clean”，那么makefile会自动包含“foo.d”和“bar.d”这两个makefile。

使用指定终极目标的方法可以很方便地编译程序，例如下面这个例子：

```makefile
.PHONY: all
all: prog1 prog2 prog3 prog4
```

从这个例子中，我们可以看到，这个makefile中有四个需要编译的程序——“prog1”， “prog2”，“prog3”和 “prog4”，我们可以使用“make all”命令来编译所有的目标（如果把all置成第一个目标，那么只需执行“make”），也可以使用 “make prog2”来单独编译目标“prog2”。

即然make可以指定所有makefile中的目标，那么也包括“伪目标”，于是可以根据这种性质来让makefile根据指定的目标来完成不同的事。在Unix世界中，软件发布时，特别是GNU这种开源软件发布时，其makefile都包含了编译、安装、打包等功能。我们可以参照这种规则来书写我们的makefile中的目标。

* all: 这个伪目标是所有目标的目标，其功能一般是编译所有的目标。
* clean: 这个伪目标功能是删除所有被make创建的文件。
* install: 这个伪目标功能是安装已编译好的程序，其实就是把目标执行文件拷贝到指定的目标中去。
* print: 这个伪目标的功能是例出改变过的源文件。
* tar: 这个伪目标功能是把源程序打包备份。也就是一个tar文件。
* dist: 这个伪目标功能是创建一个压缩文件，一般是把tar文件压成Z文件。或是gz文件。
* TAGS: 这个伪目标功能是更新所有的目标，以备完整地重编译使用。
* check和test: 这两个伪目标一般用来测试makefile的流程。

当然一个项目的makefile中也不一定要书写这样的目标，这些东西都是GNU的东西，但是我想，GNU搞出这些东西一定有其可取之处（等你的 UNIX下的程序文件一多时你就会发现这些功能很有用了），这里只不过是说明了，如果你要书写这种功能，最好使用这种名字命名你的目标，这样规范一些，规范的好处就是——不用解释，大家都明白。而且如果你的makefile中有这些功能，一是很实用，二是可以显得你的makefile很专业（不是那种初学者的作品）。

## 检查规则

有时候，不想让makefile中的规则执行起来，只想检查一下我们的命令，或是执行的序列。于是可以使用make命令的下述参数：

`-n`, `--just-print`, `--dry-run`, `--recon`：不执行参数，这些参数只是打印命令，不管目标是否更新，把规则和连带规则下的命令打印出来，但不执行，这些参数对于我们调试makefile很有用处。

`-t`, `--touch`：这个参数的意思就是把目标文件的时间更新，但不更改目标文件。也就是说，make假装编译目标，但不是真正的编译目标，只是把目标变成已编译过的状态。

`-q`, `--question`：这个参数的行为是找目标的意思，也就是说，如果目标存在，那么其什么也不会输出，当然也不会执行编译，如果目标不存在，其会打印出一条出错信息。

`-W<span> <file>`, `--what-if=<file>`, `--assume-new=<file>`, `--new-file=<file>`：这个参数需要指定一个文件。一般是是源文件（或依赖文件），Make会根据规则推导来运行依赖于这个文件的命令，一般来说，可以和“-n”参数一同使用，来查看这个依赖文件所发生的规则命令。

另外一个很有意思的用法是结合 `-p` 和 `-v` 来输出makefile被执行时的信息（这个将在后面讲述）。

# 隐含规则

# 使用make更新函数库文件
