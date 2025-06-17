因为配置几乎是每个服务都需要的能力，所以，在使用 cobra 开发完二进制命令的主体框架之后，还需要实现配置文件解析能力。

Go 项目开发中配置解析源有多种，例如：命令行参数、环境变量、配置文件等。但是推荐的配置解析源是配置文件，因为配置文件更易维护。

Go 社区提供了很多优秀的 Go 包可以读取 YAML、JSON 等格式的配置文件，但是目前用的最多的是 [spf13/viper](https://github.com/spf13/viper) 包。

本节课，就来展示下如何使用 [spf13/viper](https://github.com/spf13/viper) 包实现服务的配置文件解析功能。

## 配置文件解析思路

在 Go 项目开发中，服务的配置能力一般通过以下 2 步来实现：

1. 解析配置文件；
2. 读取配置。

其中，配置文件考虑到易读性，通常使用格式更加易读的 YAML 格式。并且使用 [spf13/viper](https://github.com/spf13/viper) 包来解析。关于 viper 包的使用可参考：[docs/book/viper.md](https://github.com/onexstack/miniblog/blob/master/docs/book/viper.md) 文档。

## 解析配置文件

viper 包和 cobra 都是由 spf13 大神开发，所以二者天然具备了很高的集成能力。cobra 包提供了 OnInitialize 函数，该函数可以用来在程序运行时运行指定的钩子函数。

所以，我们可以通过设置钩子函数，来让程序运行时加载并读取配置。更新 [cmd/fg-apiserver/app/server.go](https://github.com/onexstack/fastgo/blob/feature/s03/cmd/fg-apiserver/app/server.go) 文件，更新内容如下：

```go
package app

import (
    "encoding/json"
    ...
    "github.com/spf13/viper"

    "github.com/onexstack/fastgo/cmd/fg-apiserver/app/options"
)

var configFile string // 配置文件路径

// NewFastGOCommand 创建一个 *cobra.Command 对象，用于启动应用程序.
func NewFastGOCommand() *cobra.Command {
    // 创建默认的应用命令行选项
    opts := options.NewServerOptions()

    cmd := &cobra.Command{
        ...
        RunE: func(cmd *cobra.Command, args []string) error {
            // 将 viper 中的配置解析到选项 opts 变量中.
            if err := viper.Unmarshal(opts); err != nil {
                return err
            }

            // 对命令行选项值进行校验.
            if err := opts.Validate(); err != nil {
                return err
            }

            fmt.Printf("Read MySQL host from Viper: %s\n\n", viper.GetString("mysql.host"))

            jsonData, _ := json.MarshalIndent(opts, "", "  ")
            fmt.Println(string(jsonData))

            return nil
        },
        // 设置命令运行时的参数检查，不需要指定命令行参数。例如：./fg-apiserver param1 param2
        Args: cobra.NoArgs,
    }

    // 初始化配置函数，在每个命令运行时调用
    // 在 Cobra 解析命令行参数后，执行 RunE 前自动调用
    cobra.OnInitialize(onInitialize)

    // cobra 支持持久性标志(PersistentFlag)，该标志可用于它所分配的命令以及该命令下的每个子命令
    // 推荐使用配置文件来配置应用，便于管理配置项
    cmd.PersistentFlags().StringVarP(&configFile, "config", "c", filePath(), "Path to the fg-apiserver configuration file.")

    return cmd
}
```

上述代码会通过 [options.NewServerOptions()](https://github.com/onexstack/fastgo/blob/feature/s03/cmd/fg-apiserver/app/server.go#L24) 函数调用，创建了一个配置结构体变量 opts。在 RunE 方法中，通过 viper.Unmarshal(opts) 函数调用，将 viper 读取配置文件后的配置内容解码到 opts 配置结构体变量中。

之后，调用 opts 的 [Validate](https://github.com/onexstack/fastgo/blob/feature/s03/cmd/fg-apiserver/app/options/options.go#L54) 方法，来判断配置项是否合法。并使用以下 2 种方法读取并打印配置项内容：