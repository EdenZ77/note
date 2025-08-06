为了能够让你更好的学习 Kubernetes 的源码，本节课首先来介绍下 Kubernetes 的源码仓库结构，以及每个源码目录的作用

## Kubernetes 仓库结构介绍

Kubernetes 源码量比较大，如果没有一个合理的仓库结构设计，维护起来会很困难。Kubernetes 社区在项目迭代过程中，也在不断优化项目的目录结构以适配最新的功能代码以及代码设计方法。

v1.30.2 版本中，Kubernetes 源码目录结构如下：

```shell
├── api/ # 存放 API 定义相关文件，例如：OpenAPI 文档
├── build/ # 包含了使用容器来构建 Kubernetes 组件的脚本
├── CHANGELOG/ # 存放 Kubernetes 的 CHANGELOG。master 分支会存放所有版本的CHANGELOG。在 tag 分支只会存放对应tag的CHANGELOG
├── cluster/ # 存放一些脚本和工具，用于创建、更新和管理Kubernetes集群
├── cmd # 存放Kubernetes的各种命令行工具的入口文件（即main文件），包括kubelet、kube-apiserver、kube-controller-manager、kube-scheduler等
├── docs # 存放设计、开发文档或用户文档等。为了精简 Kubernetes 仓库，docs目录下的文件已挪至https://github.com/kubernetes/community项目中
├── hack # 包含了一些脚本，这些脚本用来管理Kubernetes项目，例如：代码生成、组件安装、代码构建、代码测试等
│   ├── boilerplate # 各种类型文件的版权头信息。在生成代码时，对应文件类型的版权头信息取自该目录中对应的文件
│   ├── lib # 存放shell脚本库，包含一些通用的Shell函数
│   ├── make-rules # 存放 Makefile 文件
├── Makefile -> build/root/Makefile
├── _output # 保存一些构建产物或者其他临时文件
├── pkg # 存放大部分Kubernetes的核心代码。这里有API对象的定义、客户端库、认证/授权/审计机制、网络插件、存储插件等。这些代码可被项目内部或外部直接引用
│   ├── api # 包含了跟 API 相关的一些功能函数
│   ├── apis # 包含了 Kubernetes 中内置资源的定义、版本转换、默认值设置、参数校验等功能
│   ├── auth # 包含了授权相关的功能实现
│   ├── controller # 包含了kubernetes各类controller实现
│   ├── controlplane # 包含了kube-apiserver的核心实现
│   ├── credentialprovider
│   ├── features # 包含了kubernetes内置的feature gate
│   ├── generated # 包含了Kubernetes中所有的生成文件。当前只包含了openapi。但是该目录目的是存放更多的生成文件
│   ├── kubeapiserver # 包含了kube-apiserver相关的核心包，当前有adminssion初始化、认证、授权相关的包
│   ├── kubectl # kubectl 命令行工具具体实现
│   ├── kubelet # kubelet 具体实现
│   ├── kubemark # kubemark 具体实现
│   ├── printers # 实现 Kubernetes 对象的打印和显示功能
│   ├── probe # 管理 Kubernetes 的健康检查探针
│   ├── proxy # kube-proxy 具体实现
│   ├── registry # kube-apiserver核心代码实现，包含了资源的CURD、资源注册等
│   ├── scheduler # kube-scheduler 具体实现
│   ├── util # 提供一些通用的工具函数和辅助函数
│   ├── volume # 实现 Kubernetes 存储卷的管理
├── plugin # 存放Kubernetes的各种插件，包括网络插件、设备插件、调度插件、认证插件、授权插件等。这些插件使的Kubernetes更加灵活和强大
├── staging # 存放些即将被移动到其它仓库的代码
├── test # 存放测试工具及测试数据
├── third_party # 存放第三方工具、代码或其他组件
└── vendor # 存放项目依赖的库代码，一般为第三方库代码
```

上面列出了 Kubernetes 源码目录下的一些核心的目录，还有其他一些文件和目录没有列出来，但大家阅读时，通过文件和文件名，不难知道它们的功能。

## Staging 目录

这里要特别介绍下 /staging 目录，很多刚开始看 kubernetes 源码的同学，对这个目录的命名和作用都有点不太明白。

staging 目录是一个暂存区，用来暂时保存未来会发布到其自己代码仓库的项目。暂存区中的项目会被 publishing-bot 机器人定期同步到 k8s.io 组织中，作为 k8s.io 组织的一级项目而存在，其模块名为 k8s.io/xxx，例如 [api](https://github.com/kubernetes/api/blob/master/go.mod#L3) 包的模块名如下：

```
// This is a generated file. Do not edit directly.

module k8s.io/api

go 1.24.0
```

**将这些项目发布为 k8s.io 组织的一级项目，可以方便其他开发者的引用。要注意的是，这些代码虽然以独立项目发布，但是都在 kubernetes 主项目中维护，位于目录 kubernetes/staging/ ，这里面的代码代码被定期同步到各个独立项目中。**

在 Go 项目中，我们如果要使用 api 包，引用的是其模块名：

```go
package kubelet

import (
    ...
    v1 "k8s.io/api/core/v1"
    ...
)
```

另外，Go 模块 k8s.io/api 其对应的 GitHub 仓库是 https://github.com/kubernetes/api。在实际执行 go get 命令时，go 工具会先访问 https://k8s.io/api，然后 k8s.io 服务器将模块下载地址转发到 https://github.com/kubernetes/api。

staging 目录当前暂存了以下项目（为了方便表述，我后面会统称这些包为 staging 包）：

![img](image/FqcNsf4XvBuDR3nqcS54DyODkh-G)

staging 目录中的代码是权威的，也就是说它是代码的唯一副本，如果你要对 k8s.io/api  包的代码进行变更，你只能修改 kubernetes/staging/src/k8s.io/api 目录下的代码。换句话说，k8s.io/api 仓库是只读的，你不能对其进行任何代码修改。

### Kubernetes 代码如何导入 staging 包？

staging 目录下的包，也会被 Kubernetes 仓库中的其他代码导入并使用，例如 [pkg/kubelet/kubelet.go](https://github.com/kubernetes/kubernetes/blob/v1.28.3/pkg/kubelet/kubelet.go#L46)：

```go
package kubelet

import (
    ...
    "k8s.io/client-go/informers"
    v1 "k8s.io/api/core/v1"
    metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
    "k8s.io/apimachinery/pkg/types"
    "k8s.io/apimachinery/pkg/util/wait"
    "k8s.io/klog/v2"
    "k8s.io/kubernetes/pkg/api/v1/resource"
    "k8s.io/kubernetes/pkg/features"
    "k8s.io/kubernetes/pkg/kubelet/cadvisor"
    ...
)
```

因为，staging 目录下的项目模块名为 k8s.io/xxx，所以即使 kubernetes 代码需要用，也需要按模块名进行导入 k8s.io/xxx。但这时候，会出现一个问题，如果 kubernetes 需要添加 A 功能，这个功能涉及到 xxx 包的修改，这时候有 2 种方案：

1. 先将 xxx 包发布到 k8s.io/xxx 仓库下，再来升级 kubernetes 仓库下 go.mod 文件中 k8s.io/xxx 包的版本，然后再开发 A 功能；
2. 使用 Go 模块的包替代功能，直接将 k8s.io/xxx 替换为 staging 目录下的 xxx 模块。

方案 1 会有一个问题，直接把 xxx 的变更先发布到 k8s.io/xxx 仓库中，再开发 A 功能，如果在开发过程中，发现 xxx 包需要适配，就需要频繁的将适配的内容发布到 k8s.io/xxx 仓库中，会造成 k8s.io/xxx 仓库被频繁发布了不稳定的代码，不合理。

Kubernetes 选择了第 2 种方法，因为第 2 种方法最便捷，也最合理，例如 [go.mod](https://github.com/kubernetes/kubernetes/blob/v1.28.3/go.mod#L134)：

```
replace (
	k8s.io/api => ./staging/src/k8s.io/api
	k8s.io/apiextensions-apiserver => ./staging/src/k8s.io/apiextensions-apiserver
	k8s.io/apimachinery => ./staging/src/k8s.io/apimachinery
	k8s.io/apiserver => ./staging/src/k8s.io/apiserver
	k8s.io/cli-runtime => ./staging/src/k8s.io/cli-runtime
	k8s.io/client-go => ./staging/src/k8s.io/client-go
	k8s.io/cloud-provider => ./staging/src/k8s.io/cloud-provider
	k8s.io/cluster-bootstrap => ./staging/src/k8s.io/cluster-bootstrap
	k8s.io/code-generator => ./staging/src/k8s.io/code-generator
	k8s.io/component-base => ./staging/src/k8s.io/component-base
	k8s.io/component-helpers => ./staging/src/k8s.io/component-helpers
	k8s.io/controller-manager => ./staging/src/k8s.io/controller-manager
	k8s.io/cri-api => ./staging/src/k8s.io/cri-api
	k8s.io/csi-translation-lib => ./staging/src/k8s.io/csi-translation-lib
	k8s.io/dynamic-resource-allocation => ./staging/src/k8s.io/dynamic-resource-allocation
	k8s.io/endpointslice => ./staging/src/k8s.io/endpointslice
	k8s.io/kms => ./staging/src/k8s.io/kms
	k8s.io/kube-aggregator => ./staging/src/k8s.io/kube-aggregator
	k8s.io/kube-controller-manager => ./staging/src/k8s.io/kube-controller-manager
	k8s.io/kube-proxy => ./staging/src/k8s.io/kube-proxy
	k8s.io/kube-scheduler => ./staging/src/k8s.io/kube-scheduler
	k8s.io/kubectl => ./staging/src/k8s.io/kubectl
	k8s.io/kubelet => ./staging/src/k8s.io/kubelet
	k8s.io/legacy-cloud-providers => ./staging/src/k8s.io/legacy-cloud-providers
	k8s.io/metrics => ./staging/src/k8s.io/metrics
	k8s.io/mount-utils => ./staging/src/k8s.io/mount-utils
	k8s.io/pod-security-admission => ./staging/src/k8s.io/pod-security-admission
	k8s.io/sample-apiserver => ./staging/src/k8s.io/sample-apiserver
	k8s.io/sample-cli-plugin => ./staging/src/k8s.io/sample-cli-plugin
	k8s.io/sample-controller => ./staging/src/k8s.io/sample-controller
)
```

如果你是用的 vendor 机制，你会发现 vendor/k8s.io/api 目录也被作了软连接：

```
vendor/k8s.io/api -> ../../staging/src/k8s.io/api
```

### 如何根据 kubernetes v1.30.2 查找 staging 包的版本？

我们基于 Kubernetes v1.30.2 来进行讲解，Kubernetes 会依赖 staging 包，在 kubernetes 包中，对 staging 包的依赖配置如下：

```
$ grep -w k8s.io/api go.mod
k8s.io/api v0.0.0
k8s.io/api => ./staging/src/k8s.io/api
```

可以看到，go.mod 中并没有配置 k8s.io/api 包的版本。那么，我们如何知道 kubernetes v1.30.2 所依赖 k8s.io/api 包的具体发布版本呢？

了解版本映射很重要，因为如果第三方项目需要引用 kubernetes 包及 staging 包，是需要指明版本的，如果 kubernetes 包、staging 包之间的版本不匹配，会带来很多兼容性问题。

