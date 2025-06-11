> 提示：本节课最终源码位于 fastgo 项目的 feature/s01 分支；
> 更详细的课程版本见：Go 项目开发中级实战课：10 | 项目初始化（上）：如何初始化一个 Go 项目仓库？

项目开发的第一步便是初始化一个项目目录，并根据 [golang-standards/project-layout](https://github.com/golang-standards/project-layout) 目录规范，添加必要的目录及文件。

本节课，来给你介绍下如何初始化一个 Go 项目。

初始化一个 Go 项目，大概分为以下几步：

1. 创建项目目录；
2. 初始化目录为 Go 模块；
3. 初始化目录为 Git 仓库；
4. 创建需要的目录；
5. 创建 Hello World 程序。

## 步骤 1：创建项目目录

开发 Go 项目的第 1 步便是创建一个项目目录。现今 Go 模块管理都是用的 Go Modules。虽然，在使用 Go Modules 的情况下，不再需要设置 GOPATH 环境变量。但是为了提高项目的维护性，这里还是建议将项目放在 GOPATH 目录下。

初始化项目目录，操作命令如下：

```shell
$ mkdir -p $GOPATH/src/github.com/onexstack/fastgo # 创建项目目录
$ cd $GOPATH/src/github.com/onexstack/fastgo # 进入到项目目录中
$ echo "## fastgo 项目" >> README.md # 创建一个 README 文件，作为项目的第一个文件
```

## 步骤 2： 初始化目录为 Go 模块

Go 项目都需要将目录初始化为一个 Go 模块。所以，这里我们需要将 fastgo 目录初始化为一个 Go 模块。初始化命令如下：

```shell
$ go mod init github.com/onexstack/fastgo  # 1. 初始化 Go 模块
$ go work init . # 2. 初始化 Go 工作区（仅限多模块管理场景），生成 go.work 文件  
$ go work use . # 添加当前模块到 Go 工作区
```

## 步骤 3： 初始化目录为 Git 仓库

当前 Go 项目基本都是使用 Git 来管理项目源码的。所以，我们接下来还需要将目录初始化为一个 Git 仓库。

初始化为 Git 仓库的第一步，就是在当前目录添加一个 .gitignore 文件，里面包含不期望 Git 跟踪的文件，例如：临时文件等。你可以使用生成工具 [gitignore.io](https://link.juejin.cn/?target=https%3A%2F%2Fwww.toptal.com%2Fdevelopers%2Fgitignore) 来生成 .gitignore：

```shell
# 备份文件
*.bak
*~

# Go 工作区文件。Go 项目开发中，不建议将 Go 工作区文件提交到代码仓库
go.work
go.work.sum

# 日志文件
*.log

# 自定义文件
/_output
```

可以执行以下命令将 Go 项目仓库初始化为一个 Git 仓库：

```shell
$ git init # 初始化当前目录为 Git 仓库
$ git config user.name 孔令飞 # 设置仓库级别用户名
$ git config user.email colin404@foxmail.com # 设置仓库级别邮箱
$ git config --global credential.helper store # 永久保存凭据
$ git add . # 添加所有被 Git 追踪的文件到暂存区
$ git remote add origin https://github.com/onexstack/fastgo # 将本地仓库与远程仓库相关联
$ git commit -m "feat: 第一次提交" # 将暂存区内容添加到本地仓库中
```

之后，我们就可以在该目录下开发代码，并根据需要提交代码。提交后的源码目录内容如下：

```shell
$ ls -A
.git  .gitignore  go.mod  go.work  README.md
```

## 步骤 4： 创建需要的目录

执行以下命令预创建需要的目录：

```shell
$ mkdir -p cmd configs docs scripts
$ ls -F
cmd/  configs/  docs/  go.mod  go.work  README.md  scripts/
```

提前创建一些符合目录规范的空目录可以起到一下 2 个作用：

1. 提前规划目录相当于提前规划未来的功能，将未来要实现的功能以目录的形式固化在项目仓库中，起到记录的作用；
2. 提前创建目录有利于后续文件按照功能存放在预先规划好的目录中，从而使项目更加规范。否则，不同开发者可能会根据各自的开发习惯，创建各种各样的目录结构和目录名称。

因为 Git 默认不会追踪空目录，所以需要再空目录下创建 .keep 文件，创建命令如下：

```
$ touch configs/.keep docs/.keep scripts/.keep cmd/.keep
```

## 步骤 5： 创建 Hello World 程序

创建 [cmd/fg-apiserver/](https://github.com/onexstack/fastgo/tree/feature/s01/cmd/fg-apiserver) 目录（fg 是 fastgo 的简写）：

```shell
$ mkdir -p cmd/fg-apiserver
```

新建 [cmd/fg-apiserver/main.go](https://github.com/onexstack/fastgo/blob/feature/s01/cmd/fg-apiserver/main.go)，内容如下：

```go
package main

import "fmt"

// Go 程序的默认入口函数。阅读项目代码的入口函数.
func main() {
    fmt.Println("Hello World!")
}
```

编译并运行，命令如下：

```shell
$ gofmt -s -w ./ # 格式化 Go 源码
$ go build -o _output/fg-apiserver -v cmd/fg-apiserver/main.go # 编译 fg-apiserver 组件源码, -v 参数用来打印编译过程中的详细信息
$ ls _output/ # _output 为二进制文件保存目录
fg-apiserver
$ _output/fg-apiserver # 启动 fg-apiserver 组件
Hello World!
```