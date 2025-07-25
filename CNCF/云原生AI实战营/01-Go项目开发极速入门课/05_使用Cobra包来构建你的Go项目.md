> 提示：
>
> 1. 本节课最终源码位于 fastgo 项目的 [feature/s02](https://github.com/onexstack/fastgo/tree/feature/s02) 分支；
> 2. 更详细的课程版本见：[Go 项目开发中级实战课](https://konglingfei.com/cloudai/catalog/intermediate.html)：12 | 应用构建（1）：如何构建一个高质量的 Go 应用？

Go 程序的运行入口是 main 函数，所以我们首先要开发出 main 函数。

可以使用以下 2 种方式来开发一个 main 函数：

1. 手动写一个 main 函数，实现业务逻辑，并启动程序；
2. 使用应用框架，实现一个 main 函数。

## 手动写一个 main 函数，实现业务逻辑，并启动程序。

手动写一个 main 函数，实现业务逻辑示例代码如下：

```go
package main

import (
	"flag"
	"fmt"
)

func main() {
	// 解析命令行参数
	option1 := flag.String("option1", "default_value", "Description of option 1")
	option2 := flag.Int("option2", 0, "Description of option 2")
	flag.Parse()

	// 执行简单的业务逻辑
	fmt.Println("Option 1 value:", *option1)
	fmt.Println("Option 2 value:", *option2)

	// 在这里添加您的业务逻辑代码
}
```

这种 main 不能实现很复杂的功能，当应用程序逻辑复杂的时候，main 函数代码会很难维护。所以，建议使用社区优秀的应用包来实现一个程序，这种程序更加结构化，更易维护。

## 使用应用框架实现 main 函数

社区有很多优秀的 Go 包，例如 [spf13/cobra](https://github.com/spf13/cobra)、[urfave/cli](https://github.com/urfave/cli) 等。但当前用的最多的是 cobra。例如：Kubernetes、Docker、Etcd、Hugo 等，均使用 cobra 构建应用。fastgo 项目也是用 cobra 来构建应用。

一个 Go 项目通常代表一个应用，一个应用通常又包含多个服务（或者组件）。根据 project-layout 目录结构规范，目录的组织方式如下：

```shell
$ tree -F app-a
app-a
├── cmd/
│   ├── component-a/
│   └── component-b/
└── internal/
    ├── component-a/
    └── component-b/
```

在 cmd/ 目录下创建多个目录，例如：component-a、component-b。每个目录下保存了对应服务的 main 源码。cmd/ 目录下的源文件保存了服务二进制文件的启动源码，具体的业务逻辑实现则放在 internal 目录下，并且放在对应的同名目录下。

这种目录组织方式的优点如下：

- 在 cmd/目录下保存不同服务的 main 源码，将一个应用的不同服务 main 函数保存在一个 cmd/目录下，便于查找和维护这些服务源码；
- 将不同服务的业务实现放在 internal 目录下的不同文件中，有利于从目录来物理隔离不同服务的源码，从而提高代码的可维护性。

因为 fastgo 实现了一个 REST API 服务器。所以其服务组件命名为 fg-apiserver。其中 fg 是 fastgo 的简写，用来表示这是一个 fastgo 项目的服务。apiserver 用来明确指代这是一个 REST API 服务器。

根据上面介绍的源码组织方式，需要在 cmd/ 目录下创建 fg-apiserver 目录，fg-apiserver 目录中创建 main.go 文件，用来保存 fg-apiserver 服务的 main 函数入口。代码如下（位于 [cmd/fg-apiserver/main.go](https://github.com/onexstack/fastgo/blob/feature/s02/cmd/fg-apiserver/main.go) 文件中）：

```go
package main

import (
    "os"

    "github.com/onexstack/fastgo/cmd/fg-apiserver/app"
    // 导入 automaxprocs 包，可以在程序启动时自动设置 GOMAXPROCS 配置，
    // 使其与 Linux 容器的 CPU 配额相匹配。
    // 这避免了在容器中运行时，因默认 GOMAXPROCS 值不合适导致的性能问题，
    // 确保 Go 程序能够充分利用可用的 CPU 资源，避免 CPU 浪费。
    _ "go.uber.org/automaxprocs"
)

// Go 程序的默认入口函数。阅读项目代码的入口函数.
func main() {
    // 创建 Go 极速项目
    command := app.NewFastGOCommand()

    // 执行命令并处理错误
    if err := command.Execute(); err != nil {
        // 如果发生错误，则退出程序
        // 返回退出码，可以使其他程序（例如 bash 脚本）根据退出码来判断服务运行状态
        os.Exit(1)
    }
}
```

[cmd/fg-apiserver/main.go](https://github.com/onexstack/fastgo/blob/feature/s02/cmd/fg-apiserver/main.go) 文件中通过 [app.NewFastGOCommand()](https://github.com/onexstack/fastgo/blob/feature/s02/cmd/fg-apiserver/app/server.go#L16) 创建了一个 *cobra.Command 类型的命令实例。

之所以在 [cmd/fg-apiserver/app](https://github.com/onexstack/fastgo/tree/feature/s02/cmd/fg-apiserver/app) 目录下创建 *cobra.Command 类型的实例，而不是在 cmd/fg-apiserver/main.go 文件中创建，是为了保持 cmd/fg-apiserver 目录下源码的简洁性，确保 main 函数代码简洁、易读。

因为将创建命令实例的核心逻辑都保存在了 cmd/fg-apiserver/app 目录中，意味着 cmd/fg-apiserver/main.go 文件没有其他依赖，所以可以通过以下方式来安装服务：

```
# 编译并安装二进制到 $GOPATH/bin
$ go install github.com/onexstack/fastgo/cmd/fg-apiserver@latest
```

[cmd/fg-apiserver/app/server.go](https://github.com/onexstack/fastgo/blob/feature/s02/cmd/fg-apiserver/app/server.go) 文件内容如下：

```go
package app

import (
    "fmt"

    "github.com/spf13/cobra"
)

// NewFastGOCommand 创建一个 *cobra.Command 对象，用于启动应用程序.
func NewFastGOCommand() *cobra.Command {
    cmd := &cobra.Command{
        // 指定命令的名字，该名字会出现在帮助信息中
        Use: "fg-apiserver",
        // 命令的简短描述
        Short: "A very lightweight full go project",
        Long: `A very lightweight full go project, designed to help beginners quickly
        learn Go project development.`,
        // 命令出错时，不打印帮助信息。设置为 true 可以确保命令出错时一眼就能看到错误信息
        SilenceUsage: true,
        // 指定调用 cmd.Execute() 时，执行的 Run 函数
        RunE: func(cmd *cobra.Command, args []string) error {
            fmt.Println("Hello FastGO!")
            return nil
        },
        // 设置命令运行时的参数检查，不需要指定命令行参数。例如：./fg-apiserver param1 param2
        Args: cobra.NoArgs,
    }

    return cmd
}
```

创建 cmd 实例的方法，其实就是根据 cobra.Command类型的字段描述，设置需要的字段。每个字段的含义，可阅读上述代码，这里不再介绍。

上述代码，核心设计如下：

- **声明式 API 设计：**代码使用 Cobra 的声明式结构定义命令行界面，通过简洁的配置表达复杂的命令行行为，提升了可读性和可维护性。这种方式是Go社区推崇的"配置胜于代码"哲学的体现；
- **错误处理优化：**使用RunE替代Run并配合SilenceUsage: true，创建了更专业的错误报告机制。这种设计在大型CLI工具中至关重要，它确保错误信息清晰可见，不被冗长的帮助文本淹没；
- **安全边界设置：**通过Args: cobra.NoArgs强制执行参数验证，防止用户误传参数导致程序异常。这种防御性编程思想体现了对生产环境稳定性的考虑，是区分业余和专业CLI工具的关键细节；
- **自文档化：**代码中的Short和Long字段不仅提供了用户文档，更重要的是它们构成了自助式用户界面，使命令行工具更加用户友好，这是优秀CLI设计的标志特征。

这种设计模式被 Kubernetes、Docker 等知名项目广泛采用，代表了 Go 生态系统中命令行应用的工程最佳实践。

## 编译并测试

执行以下命令编译并运行源码：

```shell
$ go build -v -o _output/fg-apiserver cmd/fg-apiserver/main.go
$ _output/fg-apiserver
2025/03/02 01:56:55 maxprocs: Leaving GOMAXPROCS=32: CPU quota undefined
Hello FastGO!
```

