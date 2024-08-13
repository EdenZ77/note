**解释疑惑**

Operator VS Controller

- Operator是对一个对象进行维护、操作的一组自动化的工具。
- Controller是实现Operator的一部分，是手段、方法。

# 了解kubebuilder

## kubebuilder简介

- github仓库 https://github.com/kubernetes-sigs/kubebuilder
- 官方文档 https://book.kubebuilder.io/introduction.html
- 中文翻译 https://xuejipeng.github.io/kubebuilder-doc-cn/ 但是此文档还未完成，后续是否继续未知，所以朋友们尽量看官方文档。

kubebuilder是一个脚手架工具，通过几个简单的命令就可以创建代码框架，然后填写我们的业务逻辑就可以完成目标。



## kubebuilder安装

```shell
# download kubebuilder and install locally.
curl -L -o kubebuilder "https://go.kubebuilder.io/dl/latest/$(go env GOOS)/$(go env GOARCH)"
chmod +x kubebuilder && sudo mv kubebuilder /usr/local/bin/

kubebuilder version
```



## kubebuilder命令行工具解析

### 全局flags

- --help 帮助文档
- --plugins strings 指定插件，插件的可选项如下

```shell
root@eden:~# kubebuilder --help
CLI tool for building Kubernetes extensions and tools.

Usage:
  kubebuilder [flags]
  kubebuilder [command]

Examples:
The first step is to initialize your project:
    kubebuilder init [--plugins=<PLUGIN KEYS> [--project-version=<PROJECT VERSION>]]

<PLUGIN KEYS> is a comma-separated list of plugin keys from the following table
and <PROJECT VERSION> a supported project version for these plugins.

                             Plugin keys | Supported project versions
-----------------------------------------+----------------------------
               base.go.kubebuilder.io/v4 |                          3
 deploy-image.go.kubebuilder.io/v1-alpha |                          3
                    go.kubebuilder.io/v4 |                          3
         grafana.kubebuilder.io/v1-alpha |                          3
      kustomize.common.kubebuilder.io/v2 |                          3

For more specific help for the init command of a certain plugins and project version
configuration please run:
    kubebuilder init --help --plugins=<PLUGIN KEYS> [--project-version=<PROJECT VERSION>]

Default plugin keys: "go.kubebuilder.io/v4" # 注意：默认值
Default project version: "3"                # 注意：默认值


Available Commands:
  alpha       Alpha-stage subcommands
  completion  Load completions for the specified shell
  create      Scaffold a Kubernetes API or webhook
  edit        Update the project configuration
  help        Help about any command
  init        Initialize a new project
  version     Print the kubebuilder version

Flags:
  -h, --help                     help for kubebuilder
      --plugins strings          plugin keys to be used for this subcommand execution
      --project-version string   project version (default "3")

Use "kubebuilder [command] --help" for more information about a command.
```

具体的说明在这里 https://book.kubebuilder.io/plugins/available-plugins



### 子命令























利用kubebuilder创建operator代码的框架

简单分析生成的代码

完成我们的代码

部署我们的代码









