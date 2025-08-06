# 07｜构建模式：Go Module的6类常规操作
## 为当前module添加一个依赖

在一个项目的初始阶段，我们会经常为项目引入第三方包，并借助这些包完成特定功能。即便是项目进入了稳定阶段，随着项目的演进，我们偶尔还需要在代码中引入新的第三方包。那么我们如何为一个 Go Module 添加一个新的依赖包呢？

我们还是以上一节课中讲过的 module-mode 项目为例。如果我们要为这个项目增加一个新依赖：github.com/google/uuid，那需要怎么做呢？

我们首先会更新源码，就像下面代码中这样：

```go
package main

import (
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
)

func main() {
	logrus.Println("hello, go module mode")
	logrus.Println(uuid.NewString())
}
```

新源码中，我们通过 import 语句导入了 github.com/google/uuid，并在 main 函数中调用了 uuid 包的函数 NewString。此时，如果我们直接构建这个module，我们会得到一个错误提示：

```shell
$go build
main.go:4:2: no required module provides package github.com/google/uuid; to add it:
	go get github.com/google/uuid
```

Go编译器提示我们，如果我们要增加这个依赖，可以手动执行 go get 命令。那我们就来按照提示手工执行一下这个命令：

```shell
$go get github.com/google/uuid
go: downloading github.com/google/uuid v1.3.0
go get: added github.com/google/uuid v1.3.0
```

你会发现，go get 命令将我们新增的依赖包下载到了本地 module 缓存里，并在 go.mod 文件的 require 段中新增了一行内容：

```go
require (
	github.com/google/uuid v1.3.0 //新增的依赖
	github.com/sirupsen/logrus v1.8.1
)
```

这新增的一行表明，我们当前项目依赖的是 uuid 的 v1.3.0 版本。我们也可以使用 go mod tidy 命令，在执行构建前自动分析源码中的依赖变化，识别新增依赖项并下载它们：

```shell
$go mod tidy
go: finding module for package github.com/google/uuid
go: found github.com/google/uuid in github.com/google/uuid v1.3.0
```

对于我们这个例子而言，手工执行 go get 新增依赖项，和执行 go mod tidy 自动分析和下载依赖项的最终效果，是等价的。但对于复杂的项目变更而言，逐一手工添加依赖项显然很没有效率，go mod tidy 是更佳的选择。

## go get

```shell
C:\Users\eden>go get github.com/sirupsen/logrus
go: go.mod file not found in current directory or any parent directory.
        'go get' is no longer supported outside a module.
        To build and install a command, use 'go install' with a version,
        like 'go install example.com/cmd@latest'
        For more information, see https://golang.org/doc/go-get-install-deprecation
        or run 'go help get' or 'go help install'.
```

这个提示表明 Go 在 1.16 版本后对 `go get` 命令的行为进行了重大调整，要求必须在 Go Modules 模式下使用该命令。

当前目录（或其父目录）中没有 `go.mod` 文件，Go 认为你处于传统 GOPATH 模式。自 Go 1.16 起，非 Go Modules 模式下的 `go get` 已被弃用，不再支持直接下载依赖到 `$GOPATH/src`。

如果你只是想安装一个命令行工具（如 `goimports`），可以使用 `go install` 替代：

```shell
go install golang.org/x/tools/cmd/goimports@latest  # 安装最新版本到 $GOPATH/bin
```

`go install` 仅用于安装可执行文件，不会修改 `go.mod`（因为它不依赖项目上下文）。



## 升级/降级依赖的版本

在实际开发工作中，如果我们认为Go命令自动帮我们确定的某个依赖的版本存在一些问题，比如，引入了不必要复杂性导致可靠性下降、性能回退等等，我们可以手工将它降级为之前发布的某个兼容版本。

那这个操作依赖于什么原理呢？

答案就是我们上一节课讲过“语义导入版本”机制。我们再来简单复习一下，Go Module 的版本号采用了语义版本规范，也就是版本号使用 vX.Y.Z 的格式。其中X是主版本号，Y为次版本号（minor），Z为补丁版本号（patch）。主版本号相同的两个版本，较新的版本是兼容旧版本的。如果主版本号不同，那么两个版本是不兼容的。

我们还是以上面提到过的 logrus 为例，logrus 现在就存在着多个发布版本，我们可以通过下面命令来进行查询：

```shell
$go list -m -versions github.com/sirupsen/logrus
github.com/sirupsen/logrus v0.1.0 v0.1.1 v0.2.0 v0.3.0 v0.4.0 v0.4.1 v0.5.0 v0.5.1 v0.6.0 v0.6.1 v0.6.2 v0.6.3 v0.6.4 v0.6.5 v0.6.6 v0.7.0 v0.7.1 v0.7.2 v0.7.3 v0.8.0 v0.8.1 v0.8.2 v0.8.3 v0.8.4 v0.8.5 v0.8.6 v0.8.7 v0.9.0 v0.10.0 v0.11.0 v0.11.1 v0.11.2 v0.11.3 v0.11.4 v0.11.5 v1.0.0 v1.0.1 v1.0.3 v1.0.4 v1.0.5 v1.0.6 v1.1.0 v1.1.1 v1.2.0 v1.3.0 v1.4.0 v1.4.1 v1.4.2 v1.5.0 v1.6.0 v1.7.0 v1.7.1 v1.8.0 v1.8.1
```

在这个例子中，基于初始状态执行的 go mod tidy 命令，帮我们选择了 logrus 的最新发布版本 v1.8.1。如果你觉得这个版本存在某些问题，想将 logrus 版本降至某个之前发布的兼容版本，比如 v1.7.0， 那么我们可以在项目的module根目录下，执行带有版本号的 go get 命令：

```shell
$go get github.com/sirupsen/logrus@v1.7.0
go: downloading github.com/sirupsen/logrus v1.7.0
go get: downgraded github.com/sirupsen/logrus v1.8.1 => v1.7.0
```

从这个执行输出的结果，我们可以看到，go get 命令下载了 logrus v1.7.0 版本，并将 go.mod 中对 logrus 的依赖版本从 v1.8.1 降至 v1.7.0。

当然我们也可以使用万能命令 go mod tidy 来帮助我们降级，但前提是首先要用 go mod edit 命令，明确告知我们要依赖 v1.7.0版本，而不是 v1.8.1，这个执行步骤是这样的：

```shell
$go mod edit -require=github.com/sirupsen/logrus@v1.7.0
$go mod tidy
go: downloading github.com/sirupsen/logrus v1.7.0
```

降级后，我们再假设 logrus v1.7.1 版本是一个安全补丁升级，修复了一个严重的安全漏洞，而且我们必须使用这个安全补丁版本，这就意味着我们需要将logrus依赖从 v1.7.0 升级到 v1.7.1。

我们可以使用与降级同样的步骤来完成升级，这里我只列出了使用 go get 实现依赖版本升级的命令和输出结果，你自己动手试一下。

```shell
$go get github.com/sirupsen/logrus@v1.7.1
go: downloading github.com/sirupsen/logrus v1.7.1
go get: upgraded github.com/sirupsen/logrus v1.7.0 => v1.7.1
```

好了，到这里你就学会了如何对项目依赖包的版本进行升降级了。

但是你可能会发现一个问题，在前面的例子中，Go Module的依赖的主版本号都是1。根据我们上节课中学习的语义导入版本的规范，在Go Module构建模式下，当依赖的主版本号为0或1的时候，我们在Go源码中导入依赖包，不需要在包的导入路径上增加版本号，也就是：

```plain
import github.com/user/repo/v0 等价于 import github.com/user/repo
import github.com/user/repo/v1 等价于 import github.com/user/repo
```

但是，如果我们要依赖的module的主版本号大于1，这又要怎么办呢？接着我们就来看看这个场景下该如何去做。

## 添加一个主版本号大于1的依赖

按照语义版本规范，如果我们要为项目引入主版本号大于1的依赖，比如 v2.0.0，那么由于这个版本与v1、v0开头的包版本都不兼容，我们在导入 v2.0.0 包时，不能再直接使用 github.com/user/repo，而要使用像下面代码中那样不同的包导入路径：

```plain
import github.com/user/repo/v2/xxx
```

也就是说，如果我们要为Go项目添加主版本号大于1的依赖，我们就需要使用“语义导入版本”机制， **在声明它的导入路径的基础上，加上版本号信息**。我们以“向 module-mode 项目添加 github.com/go-redis/redis 依赖包的 v7 版本”为例，看看添加步骤。

首先，我们在源码中，以空导入的方式导入 v7 版本的 github.com/go-redis/redis 包：

```go
package main

import (
	_ "github.com/go-redis/redis/v7" // “_”为空导入
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
)

func main() {
	logrus.Println("hello, go module mode")
	logrus.Println(uuid.NewString())
}
```

接下来的步骤就与添加兼容依赖一样，我们通过 go get 获取 redis 的 v7 版本：

```shell
$go get github.com/go-redis/redis/v7
go: downloading github.com/go-redis/redis/v7 v7.4.1
go: downloading github.com/go-redis/redis v6.15.9+incompatible
go get: added github.com/go-redis/redis/v7 v7.4.1
```

我们可以看到，go get 为我们选择了 go-redis v7 版本下当前的最新版本 v7.4.1。

不过呢，这里说的是为项目添加一个主版本号大于1的依赖的步骤。有些时候，出于要使用依赖包最新功能特性等原因，我们可能需要将某个依赖的版本升级为其不兼容版本，也就是主版本号不同的版本，这又该怎么做呢？

我们还以 go-redis/redis 这个依赖为例，将这个依赖从 v7 版本升级到最新的 v8 版本看看。

## 升级依赖版本到一个不兼容版本

我们前面说了，按照语义导入版本的原则，不同主版本的包的导入路径是不同的。所以，同样地，我们这里也需要先将代码中redis包导入路径中的版本号改为v8：

```go
import (
	_ "github.com/go-redis/redis/v8"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
)
```

接下来，我们再通过go get来获取v8版本的依赖包：

```shell
$go get github.com/go-redis/redis/v8
go: downloading github.com/go-redis/redis/v8 v8.11.1
go: downloading github.com/dgryski/go-rendezvous v0.0.0-20200823014737-9f7001d12a5f
go: downloading github.com/cespare/xxhash/v2 v2.1.1
go get: added github.com/go-redis/redis/v8 v8.11.1
```

这样，我们就完成了向一个不兼容依赖版本的升级。是不是很简单啊！

但是项目继续演化到一个阶段的时候，我们可能还需要移除对之前某个包的依赖。

## 移除一个依赖

我们还是看前面 go-redis/redis 示例，如果我们这个时候不需要再依赖 go-redis/redis 了，你会怎么做呢？你可能会删除掉代码中对 redis 的空导入这一行，之后再利用 go build 命令成功地构建这个项目。

但你会发现，与添加一个依赖时Go命令给出友好提示不同，这次 go build 没有给出任何关于项目已经将 go-redis/redis删除的提示，并且 go.mod 里 require 段中的 go-redis/redis/v8 的依赖依旧存在着。

我们再通过 go list 命令列出当前 module 的所有依赖，你也会发现 go-redis/redis/v8 仍出现在结果中：

```shell
$go list -m all
github.com/bigwhite/module-mode
github.com/cespare/xxhash/v2 v2.1.1
github.com/davecgh/go-spew v1.1.1
... ...
github.com/go-redis/redis/v8 v8.11.1
... ...
gopkg.in/yaml.v2 v2.3.0
```

其实，要想彻底从项目中移除 go.mod 中的依赖项，仅从源码中删除对依赖项的导入语句还不够。这是因为如果源码满足成功构建的条件，go build 命令是不会“多管闲事”地清理 go.mod 中多余的依赖项的。

我们还得用 go mod tidy 命令，将这个依赖项彻底从 Go Module 构建上下文中清除掉。go mod tidy 会自动分析源码依赖，而且将不再使用的依赖从 go.mod 和 go.sum 中移除。

到这里，其实我们已经分析了Go Module依赖包管理的5个常见情况了，但其实还有一种特殊情况，需要我们借用vendor机制。

## 特殊情况：使用vendor

你可能会感到有点奇怪，为什么 Go Module 的维护，还有要用 vendor 的情况？

其实，vendor 机制虽然诞生于GOPATH构建模式主导的年代，但在Go Module构建模式下，它依旧被保留了下来，并且成为了Go Module构建机制的一个很好的补充。特别是在一些不方便访问外部网络，并且对Go应用构建性能敏感的环境，比如在一些内部的持续集成或持续交付环境（CI/CD）中，使用 vendor 机制可以实现与 Go Module 等价的构建。

和GOPATH构建模式不同，Go Module构建模式下，我们再也无需手动维护 vendor 目录下的依赖包了，Go提供了可以快速建立和更新 vendor 的命令，我们还是以前面的 module-mode 项目为例，通过下面命令为该项目建立 vendor：

```shell
$go mod vendor
$tree -LF 2 vendor
vendor
├── github.com/
│   ├── google/
│   ├── magefile/
│   └── sirupsen/
├── golang.org/
│   └── x/
└── modules.txt
```

我们看到，go mod vendor 命令在 vendor 目录下，创建了一份这个项目的依赖包的副本，并且通过 vendor/modules.txt记录了 vendor 下的 module 以及版本。

如果我们要基于 vendor 构建，而不是基于本地缓存的 Go Module 构建，我们需要在 go build 后面加上 -mod=vendor 参数。

在Go 1.14及以后版本中，如果Go项目的顶层目录下存在 vendor 目录，那么 go build 默认也会优先基于 vendor 构建，除非你给 go build 传入 -mod=mod 的参数。

# 向前兼容性和toolchain规则

> https://tonybai.com/2023/09/10/understand-go-forward-compatibility-and-toolchain-rule/
>
> https://tonybai.com/2025/01/14/understand-go-and-toolchain-in-go-dot-mod/

## Go 1.21版本之前的向前兼容性问题

在Go 1.21版本之前，Go module中的go directive用于声明建议的Go版本，但并不强制实施。例如:

```
// go.mod
module demo1

go 1.20
```

上面go.mod文件中的 go directive 表示建议使用 Go 1.20 及以上版本编译本module代码，但并不强制禁止使用低于 1.20版本的Go对module进行编译。你也可以使用 Go 1.19 版本，甚至是 Go 1.15 版本编译这个module的代码。

但Go官方对于这种使用低版本(比如L)编译器编译 go directive 为高版本(比如H)的Go module的结果没有作出任何承诺和保证，**其结果也是不确定的**。

如果你比较幸运，在module中没有使用高版本(从L+1到H)引入go的新语法特性，那么编译是可以通过的。

如果你更加幸运，你module中的代码没有使用到任何从L+1到H版本中带有语法行为变更、bug或安全漏洞的代码，那么编译出的可执行程序运行起来也可以是正常的。

相反，你可能会遇到编译失败、运行失败甚至运行时行为出现breaking change的问题，而这些都是**不确定的**。

让我们来看一个例子。我们用Go 1.18泛型语法编写一个泛型函数Print：

```go
// toolchain-directive/demo1/mymodule.go
package mymodule

func Print[T any](s T) {
    println(s)
}

// toolchain-directive/demo1/go.mod
module mymodule

go 1.18
```

如果你尝试使用Go 1.17版本来构建这个模块，你将会遇到类似以下的错误：

```go
$go version
go version go1.17 darwin/amd64

$go build
# mymodule
./mymodule.go:3:6: missing function body
./mymodule.go:3:11: syntax error: unexpected [, expecting (
note: module requires Go 1.18
```

这些错误信息具有一定的误导性，它们指向的是语法错误，而不是问题的本质：这段代码使用了Go 1.18版本中才引入的泛型特性。虽然确实打印了一条有用的提示(note: module requires Go 1.18)，但对于规模大一些的项目来说，在满屏的编译错误中，这条提示很容易被忽略。

向前兼容性问题会导致Go开发者的体验不佳！因此，从Go 1.21版本开始，Go团队在向前兼容性方面对Go进行了改善，尽量以确定性代替上述的问题带来的不确定性。

下面我们就来看看Go 1.21版本在向前兼容性方面的策略调整。

## Go 1.21版本后的向前兼容性策略

Go 1.21及更高版本中，go.mod 文件中的go指令声明了使用模块所需的最低Go版本。Go 1.21及更高版本的Go工具链在遇到 go.mod 中go指令行中的Go版本高于自身时会怎么做呢？下面我们通过四个场景的示例来看一下。

**场景一**

当前本地工具链 go 1.22.0，go.mod 中go指令行为 go 1.23.0：

```
module scene1

go 1.23.0
```

执行构建：

```
$go build
go: downloading go1.23.0 (darwin/amd64)
......
```

Go自动下载当前 go module 中go指令行中的Go工具链版本并对当前 module 进行构建。

**场景二**

当前本地工具链 go 1.22.0，go.mod 中go指令行为 go 1.22.0，但当前 module 依赖的 github.com/bigwhite/a 的 go.mod中go指令行为 go 1.23.1：

```
module scene2

go 1.22.0

require (
	github.com/bigwhite/a v1.0.0
) 

replace github.com/bigwhite/a => ../a
```

执行构建：

```
$go build
go: module ../a requires go >=1.23.1 (running go 1.22.0)
```

Go发现当前 go module 依赖的 go module 中go指令行中的Go版本比当前 module 的更新，则会输出错误提示！

**场景三**

当前本地工具链 go 1.22.0，go.mod 中go指令行为go 1.22.0，但当前 module 依赖的 github.com/bigwhite/a 的 go.mod中go指令行为 go 1.23.1，而依赖的 github.com/bigwhite/b 的 go.mod 中go指令行为 go 1.23.2：

```
module scene3

go 1.22.0

require (
	github.com/bigwhite/a v1.0.0
	github.com/bigwhite/b v1.0.0
) 

replace github.com/bigwhite/a => ../a
replace github.com/bigwhite/b => ../b
```

执行构建：

```
$go build
go: module ../b requires go >=1.23.2 (running go 1.22.0)
```

Go发现当前 go module 依赖的 go module 中go指令行中的Go版本比当前 module 的更新，则会输出错误提示！并且选择了满足依赖构建的最小的Go工具链版本。

**场景四**

当前本地工具链 go 1.22.0，go.mod 中go指令行为 go 1.23.0，但当前 module 依赖的 github.com/bigwhite/a 的 go.mod中go指令行为 go 1.23.1，而依赖的 github.com/bigwhite/b 的 go.mod 中go指令行为 go 1.23.2：

```
module scene4

go 1.23.0

require (
	github.com/bigwhite/a v1.0.0
	github.com/bigwhite/b v1.0.0
) 

replace github.com/bigwhite/a => ../a
replace github.com/bigwhite/b => ../b
```

执行构建：

```
$go build
go: downloading go1.23.0 (darwin/amd64)
......
```

Go发现当前 go module 依赖的 go module 中go指令行中的Go版本与当前 module 的兼容，但比本地Go工具链版本更新，则会下载当前 go module 中go指令行中的Go版本进行构建。

从以上场景的执行情况来看，只有选择了当前 go module 的工具链版本时，才会继续构建下去，如果本地找不到这个版本的工具链，go会自动下载该版本工具链再进行编译(前提是 GOTOOLCHAIN=auto )。如果像场景2和场景3那样，依赖的 module 的最低 Go version 大于当前 module 的 go version，那么Go会提示错误并结束编译！后续你需要显式指定要使用的工具链才能继续编译！以场景3为例，通过 GOTOOLCHAIN 显式指定工具链，我们可以看到下面结果：

```shell
// demo2/scene3

$GOTO0LCHAIN=go1.22.2 go build
go: downloading go1.22.2 (darwin/amd64)
^C

$G0TO0LCHAIN=go1.23.3 go build
go: downloading go1.23.3 (darwin/amd64)
......
```

我们看到，go完全相信我们显式指定的工具链版本，即使是不满足依赖 module 的最低go版本要求的！

想必大家已经感受到支持新向前兼容规则带来的复杂性了！这里我们还没有显式使用到 toolchain 指令行呢！但其实，在上述场景中，虽然我们没有在 go.mod 中显式使用 toolchain 指令行，但Go模块会使用隐式的 toolchain 指令行，其隐式的默认值为 toolchain goV，其中V来自go指令行中的Go版本，比如 go1.22.0 等。

接下来我们就简单地看看 toolchain 指令行，我们的宗旨是尽量让事情变简单，而不是变复杂！

## toolchain指令行与GOTOOLCHAIN

[Go mod的参考手册](https://go.dev/ref/mod#go-mod-file-toolchain)告诉我们：toolchain指令仅在模块为主模块且默认工具链的版本低于建议的工具链版本时才有效，并建议：Go toolchain指令行中的go工具链版本不能低于在go指令行中声明的所需Go版本。（主模块（Main Module）是指：当前执行 Go 命令时所在的模块上下文，是构建的起点。Go 工具会基于主模块的 go.mod 文件来解析所有依赖关系）

也就是说如果对 toolchain 没有特殊需求，我们还是尽量隐式的使用 toolchain，即保持 toolchain 与go指令行中的go版本一致。

另外一个影响go工具链版本选择的是GOTOOLCHAIN环境变量，它的值决定了go命令的行为，特别是当 go.mod 文件中指定的Go版本（通过go或toolchain指令）与当前运行go命令的版本不同时，GOTOOLCHAIN 的作用就体现出来了。

GOTOOLCHAIN可以设置为以下几种形式：

......



- 如果 go.mod 中有 toolchain 行且指定的工具链比当前默认的工具链更新，则切换到 toolchain 行指定的工具链。
- 如果 go.mod 中没有有效的 toolchain 行（例如 toolchain default 或没有 toolchain 行），但go指令行指定的版本比当前默认的工具链更新，则切换到与go指令行版本相对应的工具链（例如go 1.23.1对应go1.23.1工具链）。 
- 在切换时，go命令会优先在本地路径（PATH环境变量）中寻找工具链的可执行文件，如果找不到，则会下载并使用。



# Go工作区

> https://polarisxu.studygolang.com/posts/go/workspace/

## 缘起

本地有两个项目，分别是两个 module：mypkg 和 example

```shell
$ cd ~/
$ mkdir polarisxu
$ cd polarisxu
$ mkdir mypkg example
$ cd mypkg
$ go mod init github.com/polaris1119/mypkg
$ touch bar.go
```

在 bar.go 中增加如下示例代码：

```go
package mypkg

func Bar() {
    println("This is package mypkg")
}
```

接着，在 example 模块中处理：

```shell
$ cd ~/polarisxu/example
$ go mod init github.com/polaris1119/example
$ touch main.go
```

在 main.go 中增加如下内容：

```go
package main

import (
    "github.com/polaris1119/mypkg"
)

func main() {
    mypkg.Bar()
}
```

这时候，如果我们运行 go mod tidy，肯定会报错，因为我们的 mypkg 包根本没有提交到 github 上，肯定找不到。

```shell
$ go mod tidy
....
fatal: repository 'https://github.com/polaris1119/mypkg/' not found
```

我们当然可以提交 mypkg 到 github，但我们每修改一次 mypkg，就需要提交（而且每次提交之后需要在 example 中 go get 最新版本），否则 example 中就没法使用上最新的。

针对这种情况，目前是建议通过 replace 来解决，即在 example 中的 go.mod 增加如下 replace：

```go
module github.com/polaris1119/example

go 1.19

require github.com/polaris1119/mypkg v0.0.0

replace github.com/polaris1119/mypkg => ../mypkg
```

再次运行 go run main.go，输出如下：

```shell
$ go run main.go
This is package mypkg
```

当都开发完成后，我们需要手动删除 replace，然后修改 mypkg 版本号并执行 go mod tidy 后提交。这还是挺不方便的，如果本地有多个 module，每一个都得这么处理。

## 工作区模式

针对上面的这个问题，Michael Matloob 提出了 Workspace Mode（工作区模式）。要使用工作区，请确保 Go 版本在 1.18+。

我本地当前版本：

```shell
$ go version
go version go1.19.2 darwin/arm64
```

通过 go help work 可以看到 work 相关命令：

```shell
$ go help work
Work provides access to operations on workspaces.

Note that support for workspaces is built into many other commands, not
just 'go work'.
......
```

根据这个提示，我们初始化 workspace：

```shell
$ cd ~/polarisxu
$ go work init mypkg example
$ tree
.
├── example
│   ├── go.mod
│   └── main.go
├── go.work
└── mypkg
    ├── bar.go
    └── go.mod
```

注意几点：

- 多个子模块应该在一个目录下（或其子目录），比如这里的 polarisxu 目录。（这不是必须的，但更好管理，否则 go work init 需要提供正确的子模块路径）
- go work init 需要在 polarisxu 目录执行；
- go work init 之后跟上需要本地开发的子模块目录名；

打开 go.work 看看长什么样：

```go
go 1.19

use (
    ./example
    ./mypkg
)
```

go.work 文件的语法和 go.mod 类似（go.work 优先级高于 go.mod），因此也支持 replace。

注意：实际项目中，多个模块之间可能还依赖其他模块，建议在 go.work 所在目录执行 `go work sync`。

现在，我们将 example/go.mod 中的 replace、require 语句删除，再次执行 go run main.go（在 example 目录下），得到了正常的输出。也可以在 polarisxu 目录下，这么运行：go run example/main.go，也能正常。

注意，go.work 不需要提交到 Git 中，因为它只是你本地开发使用的。

当你开发完成，应该先提交 mypkg 包到 GitHub，然后在 example 下面执行 go get：

```
$ go get -u github.com/polaris1119/mypkg@latest
```

然后禁用 workspace（通过 GOWORK=off 禁用），再次运行 example 模块，是否正确：

```
$ cd ~/polarisxu/example
$ GOWORK=off go run main.go
```



|       特性       |     `go work init .`      |     `go work use .`     |
| :--------------: | :-----------------------: | :---------------------: |
|     **目的**     |       创建新工作区        |  向现有工作区添加模块   |
|   **前提条件**   | 当前目录没有 go.work 文件 |    已有 go.work 文件    |
|   **目录要求**   |   当前目录必须有 go.mod   |  当前目录必须有 go.mod  |
|   **文件操作**   |   创建新的 go.work 文件   | 更新已有的 go.work 文件 |
| **典型使用场景** |  初始化多模块项目根目录   | 向工作区动态添加新模块  |

