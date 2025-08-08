Kubernetes 中有很多组件，例如：kube-apiserver、kube-controller-manager、kube-scheduler、kubelet 等。这些组件的功能都是通过应用的启动框架去初始化、启动和运行的，而且应用的启动框架也是学习组件的一部分。

在 Kubernetes 中，应用的启动框架构建方式是类似的，或者说具有一定的标准化程度。接下来的 2 节课，我会尝试将这些组件的应用构建方式进行归一化，并抽象出一个应用构建模型，通过这种方式，让你了解 Kubernetes 中应用的构建方式。通过学习 kubernetes 的应用构建方式，我们可以学习并掌握如何优雅的构建功能不同的大型企业级组件。站在巨人的肩膀上，我们会看的更高、看的更远。

## Kubernetes 应用架构演进史

本节课，我们从 Kubernetes 的应用架构演进的角度，来看下 Kubernetes 项目在构建应用时的一些变化和实现，进而探索出一个优雅的应用构建方式。

Kubernetes 应用构建的方式大概经历了以下几个阶段：

1. 阶段 1：原始阶段（v0.2～v0.9.3）；
2. 阶段 2：精简 main 文件（v0.10.0～v0.11.0）；
3. 阶段 3：应用代码分离（v0.12.0～v1.1.8）；
4. 阶段 4：命令行参数剥离（v1.2.0～v1.9.11）；
5. 阶段 5：应用构建框架化（v1.10.0～v1.28.3）；

## 阶段 1：原始阶段（v0.2～ v0.9.3）

这是 Kubernetes 应用构建的第一个阶段，应用构建采用了最简单的大一统模式。该阶段具有以下特性：

- 命令行参数设置、命令行程序构建、程序初始化等代码，均在 `cmd/apiserver/apiserver.go` 文件中实现，main 文件中，集成了组件启动的所有核心逻辑，这导致 main 文件即使在 v0.2 版本也有 143 行。在 v0.9.3 版本更是多达 285 行。随着 main 文件中集成功能的增多，代码行数的变多，维护 main 文件已经变得越来越困难；
- v0.2 版本 ～ v0.4.4 版本中，`cmd/apiserver/apiserver.go` 和 `cmd/controller-manager/controller-manager.go` 2 个组件的实现方式不统一，前者将服务启动的核心代码都集成在 main 文件中，而后者则将应用启动的核心代码放在 `pkg/` 目录下。这种不一致，也会带来一些维护成本；
- v0.5.1 版本 ～ v0.9.3 版本，这个版本区间 Kubernetes 主要做了以下变更：
  - `cmd/` 目录下各组件的构建方式保持了统一。这里，其实也展现了 Kubernetes 项目对规范和统一的看重程度。如果不规范、不统一，对于 Kubernetes 这个量级的项目来说，维护成本会非常的高；
  - 组件名的变化：`cmd/apiserver -> cmd/kube-apiserver`，`cmd/controller-manager -> cmd/kube-controller-manager`，`cmd/proxy -> cmd/kube-proxy` 新的名称一直沿用至今。猜测更改名字，是为了从组件名能够知道该组件的功能类别，便于理解和维护。
- v0.9.0 版本中，新增了以下 2 个变化：
  - 使用 pflag 包替代了标准库的 flag 包。pflag 包提供了更强大的命令行参数管理能力；
  - 首次代码生成技术。在 v0.9.0 版本中，新增了 `cmd/gendocs` 组件，用来自动生成 `kubectl` 命令行工具的使用文档。

这一阶段最大的特点是应用构建比较原始，没有采用任何应用构建框架、应用构建也没有层次、各组件应用构建方式并不统一。这种方式，在代码量较小时，问题不大。但是，当功能增多、代码量激增时，会带来很大的维护成本。

## 阶段 2：精简 main 文件（v0.10.0～v0.11.0）

这个阶段，核心应用均采用了统一的构建方式。将命令行参数设置、命令行程序构建、程序初始化等代码分别剥离到以下目录中：

- `pkg/util`：日志标志的添加、日志包的初始化；
- `pkg/version/verflag`：版本标志的添加和版本号的打印；
- `pkg/master/server`、`pkg/controllermanager`、`pkg/proxy/server` 等目录：应用初始化、启动等核心代码。

通过将一些功能实现细节剥离出 main 文件，保持了 main 文件的简单，剥离后的 main 文件只有 40 行（[cmd/kube-apiserver/apiserver.go](https://github.com/kubernetes/kubernetes/blob/v0.10.0/cmd/kube-apiserver/apiserver.go)）。

其实通过 Kubernetes 应用构建的重构可以猜测到：在 Kubernetes 项目看来，一个应用除了具有应用初始化和启动等基本功能外，还应该具有统一的日志和版本功能。

这里 Kubernetes 其实也采用了服务启动的最常见代码开发模式（先创建一个服务实例，再调用其 Run/Serve/Start 方法）：

```go
s := server.NewAPIServer()
// ...
s.Run(pflag.CommandLine.Args())
```

这个阶段，最大的变化在于，将很多实现细节从 main 文件中剥离出，并继续保证各组件（存量+新增）的构建范式是一致的，进一步提升了代码的可维护性。

## 阶段 3：应用代码分离（v0.12.0～v1.1.8）

这个阶段应用构建一个最大的变化是，在 `cmd/` 目录下新增了 `app` 目录：

```shell
cmd/
├── kube-apiserver/
│   ├── apiserver.go
│   └── app/
│       ├── plugins.go
│       └── server.go
```

将之前在 `pkg/master/server`、`pkg/controllermanager`、`pkg/proxy/server` 实现的应用初始化、启动等核心代码迁移到了 `cmd/app/` 目录下。

从这里开始，Kubernetes 的应用构建出现了分层的思想，即 Kubernetes 在构建应用时，采用了以下分层设计：

<img src="image/FrKXOb-BxN49KEVwdyqCerxeUm-q" alt="img" style="zoom:40%;" />

上述分层的每一层的功能解释如下：

- main 入口层：组件的 main 文件，代码量很少，可以很快的找到组件的代码入口，方便我们进行代码走读、开发等；
- 应用框架层：主要用负责应用的命令行参数设置、应用初始化和启动等，不会涉及具体的功能代码；
- 业务层：应用功能的实现代码。

在构建应用时，将整个组件初始化、启动相关的代码放在了应用框架层，将具体的业务逻辑实现放在了业务层。可以看到，在之前的版本中，Kubernetes 主要是物理位置上进行隔离、划分以降低代码的学习难度和维护复杂度。在这个阶段，Kubernetes 项目已经开始尝试从功能上进行剥离，以进一步提高代码的可维护性。

通过将应用框架和业务实现进行物理隔离，也可以提高代码的健壮度。如果你修改应用框架层的代码，引入一个 Bug，这个 Bug 大概率会在启动阶段被发现，并不会影响实际的功能。

另外，在 v1.1.1 版本中，还引入了 `github.com/spf13/cobra` 包。cobra 是一个非常强大的命令行框架。这个阶段，cobra 包还没有参与组件的应用构建中去，主要是使用 cobra 包创建了一个 `*cobra.Command` 类型实例，并使用实例提供的 GenMarkdownTree 方法，来自动生成命令行工具的使用文档。

这个阶段，Kubernetes 引入了越来越多的自动代码生成功能：

```shell
$ ls -d cmd/gen*| pr -T -4
cmd/genbashcomp          cmd/gendeepcopy   cmd/genkubedocs   cmd/genswaggertyp
cmd/genconversion cmd/gendocs            cmd/genman              cmd/genutils
```

在这个阶段，还有一个变化就是：

- v0.18.0 版本之前：Kubernetes 的生成工具主要用来自动生成一些文档；
- v0.18.0 及之后的版本：Kubernetes 的生成工具会自动生成一些非文档类的代码。这些代码会直接作为 Kubernetes 的核心源码保存在 Kubernetes 项目仓库中。

## 阶段4：命令行参数剥离（v1.2.0～v1.9.11）

这个版本阶段，Kubernetes 应用构建主要带来以下 2 个大的变化：

1. 剥离命令行参数设置代码到 `cmd/xxx/app/options/` 目录下；
2. 引入动态配置功能；

### 命令行参数代码剥离

该阶段，Kubernetes 项目将命令行参数设置相关的代码进一步剥离到 `cmd/kube-apiserver/app/options` 目录下。具体命令行选项处理模式如下（[cmd/kube-apiserver/app/options/options.go](https://github.com/kubernetes/kubernetes/blob/v1.9.11/cmd/kube-apiserver/app/options/options.go)）。

模式解读：

1. 创建一个 `XXXOptions` 结构体，用来作为命令行选项的操作对象；
2. 创建一个 `NewXXXOptions` 函数，用来创建一个带默认值的 `XXXOptions` 实例；
3. 给 `XXXOptions` 结构体添加一个 `AddFlags` 方法，在该方法中设置命令行参数，并将命令行参数的值绑定到结构体中的字段中。

在将命令行参数相关的代码剥离到 `cmd/kube-apiserver/app/options/` 目录时，Kubernetes 也将代码按功能分别存放在不同的源码文件下：

- 命令行参数的创建与设置，放在 `options.go` 文件中；
- 命令行选项值验证放在了 `validation.go` 文件中。

通过按功能隔离，也能一定程度上减小误改代码的概率，提高代码健壮性，并能够提高代码的可维护性。

通过将命令行参数进一步隔离，Kubernetes 应用构建层级中的应用框架层，又包含了命令行参数设置这一大类功能

<img src="image/FjhpallR0nyjHR-V8l8NzLL4gq-y" alt="img" style="zoom:40%;" />

### 动态配置

在  v1.2.0 之前的版本中，Kubernetes 各组件都是通过命令行参数来进行配置的。这种配置方式在命令行参数不多的情况下问题不大，但是当命令行参数变的越来越多的时候，会有以下问题：

- 命令行参数太多，启动命令太长，难以阅读；
- 当命令行参数很多时，相比于配置文件的方式，管理和分发成本会更高；

所以当命令行参数很多的时候，比较建议使用配置文件，可以简化配置和部署的复杂度。

对于 kubelet 组件，之前的命令行参数配置形式还额外有以下问题：kubelet 是部署在每个 Node 节点上的 agent，在更新 kubelet 配置时，往往需要集群管理员，通过 SSH（手动或通过脚本）登录到节点上，然后配置 kubelet 命令行参数，并重启 kubelet。这种方式更新 kubelet 的配置成本比较高。

所以 kubelet 组件的配置更新，需要一种类似配置中心的配置机制，能够通过一个配置中心，触发配置文件的变更和 kubelet 组件的重启。要实现配置文件下发，你可以引入第三方配置中心组件，例如：Apollo、Nacos、Consul、Etcd 等。当然，也可以直接复用现有的 kubernetes 机制，增加一个配置类型的 API 资源，并 watch kube-apiserver，从而感知 API 资源的变更。kubelet 直接复用了 kubernetes 现有的机制。

为此，kubernetes v1.2.0 版本，新增了一个 KubeletConfiguration 类型的 API 定义，见文件 [pkg/apis/componentconfig/types.go](https://github.com/kubernetes/kubernetes/blob/v1.2.0/pkg/apis/componentconfig/types.go#L100)，例如：

```yaml
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
address: "192.168.0.8"
port: 20250
serializeImagePulls: false
evictionHard:
    memory.available:  "100Mi"
    nodefs.available:  "10%"
    nodefs.inodesFree: "5%"
    imagefs.available: "15%"
```

上述配置会保存在 ConfigMap 资源的 data 字段中。kubelet 会 watch ConfigMap，当 ConfigMap 有变更时，kubelet 会将 ConfigMap data 字段的配置内容写到本地磁盘上，然后退出进程，之后由操作系统级别的进程管理服务自动重新拉起 kubelet 进程。kubelet watch 哪个 ConfigMap 是在 Node.Spec.ConfigSource.ConfigMap 中指定的。

更对关于 kubelet 动态配置的介绍，你可以参考以下 2 篇文章：

1. [Dynamic Kubelet Configuration](https://kubernetes.io/blog/2018/07/11/dynamic-kubelet-configuration/); 
2. [Set Kubelet Parameters Via A Configuration File](https://kubernetes.io/docs/tasks/administer-cluster/kubelet-config-file/). 

将组件配置以 Kubernetes API 资源的形式进行加载，还会带来以下好处：

1. 配置版本化转换：Kubernetes API 资源是有资源的，配置版本化，可以使得后续的版本转换成为可能，通过版本转换可以使配置兼容以前的版本；
2. 默认值：可以复用 Kubernetes 给资源设置默认值的机制，给配置资源设置默认值；
3. 配置校验：可以直接复用 Kubernetes 资源的校验机制，对各个配置项进行校验。

之后的 Kubernetes 版本中，kube-controller-manager、kube-proxy、kube-scheduler等组件，也将配置文件已 Kubernetes 资源的形式进行加载和设置。

## 阶段5：应用构建框架化（v1.10.0 ～ v1.28.3）

这个阶段，最大的变化就是直接复用了 cobra 框架的能力，通过 cobra 框架来构建并启动应用。

## Kubernetes 应用架构演进图

为了，让你更清晰的观察到 Kubernetes 应用构建的演进方式，我将各个演进阶段以图示的方式展示给你，如下图所示：

![img](image/FkDabIhad6fL4D8ZEnMuYHfmnXsI)

可以看到，Kubernetes 的应用构建方式具有以下几个特点：

- 代码功能越来越隔离；
- 代码风格越来越统一；
- 代码位置越来越清晰；
- 应用构建越来越高效；
- 代码结构越来越稳定。

Kubernetes 最后通过按功能分层的方式，将每个功能的代码实现分别保存在不同的位置和文件，以此来进行代码隔离。代码隔离可以带来以下好处：

- 易于阅读和维护；
- 减少引入 Bug 的概率，提高程序的健壮性。

## 总结

Kubernetes 的应用架构历经多个阶段，从最初的大一统模式逐步演变为分层、模块化和框架化的结构。这一过程不仅提高了代码的可维护性和健壮性，也为开发者提供了一个更优雅的应用构建方式，更好地适应了 Kubernetes 项目的发展需求。长期以来，Kubernetes 项目的成功也体现了规范化和统一化在大型开源项目中的重要性。