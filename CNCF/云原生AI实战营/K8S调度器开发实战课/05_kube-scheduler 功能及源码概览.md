Kubernetes 中有很多核心组件，其中一个非常重要的组件是 kube-scheduler。kube-scheduler 负责将新创建的 Pod 调度到集群中的合适节点上运行，如果没有 kube-scheduler，我们创建的 Pod 就无法调度到合适的 Node 上，也就没办法运行我们的业务。

<img src="image/FpxEQ5rjDDD1F5k_L3f_wmB5HnFm" alt="img" style="zoom:40%;" />

在企业使用 Kubernetes 的过程中，需要改造 Kubernetes 的地方不多，在这些需要改造的部分中，调度器占了很大一部分比例。

本篇文章，我就来通过剖析 v1.31.1 版本的 kube-scheduler 的源码，来让你深入学习 Kubernetes 的调度原理和实现。

因为 kube-scheduler 实现代码量大、逻辑复杂，所以本篇文章，难免出现一些逻辑、语义的错误，但这些错误，还不至于严重误导对 kube-scheduler 源码的学习。所以，大家可以放心阅读，也希望能包容其中的一些错误。后面会不断细化文章逻辑，以及调度细节。争取，能够清晰、准确的介绍 kube-shceduler 实现的方方面面。

## 如何学习 kube-scheduler 源码？

因为kube-scheduler源码很多，为了能够让你高效的学习kube-scheduler源码。本小节，我来介绍下如何学习kube-scheduler源码。

v1.31.1版本的kube-scheduler源码至少 24062 行代码，规模庞大，需要有一个有效的学习方法。首先，我们应该从全局角度去了解kube-scheduler的功能及源码构成。接着，我们可以深入到kube-sheduler源码内部，去阅读、学习kube-scheduler具体是如何实现调度功能的，这包括以下几方面：

1. kube-scheduler应用配置与构建；
2. kube-scheduler调度原理；
3. Pod的调度流程；
4. kube-scheduler的核心功能实现；
5. kube-scheduler调度插件实现。

![img](image/FuRneMDa63Z57DHnKwwKy_skjxuC)

## kube-scheduler 调度Pod流程

kube-scheduler最核心的功能是Pod调度。这里，我们通过Pod的调度流程，来了解kube-scheduler的功能。

### Kubernetes视角看Pod调度

kube-scheduler 在整个系统中承担了“承上启下”的重要功能。“承上”是指它负责接受kube-controller-manager创建的新 Pod，为其安排 Node。“启下”是指安置工作完成后，目标 Node 上的 kubelet 服务进程接管后续工作。Pod 是 Kubernetes 中最小的调度单元，Pod 被创建出来的工作流程如图所示：

<img src="image/FuWvEdf9b6ye0DM5_uKHfyNAcrHd" alt="img" style="zoom:70%;" />

在上面这张图中

- 第一步通过kube-apiserver的 REST API 创建一个 Pod；
- 然后kube-apiserver接收到数据后将数据写入到 Etcd 中；
- kube-scheduler在启动后，会通过List-Watch机制监听kube-apiserver中Pod资源的变化。当新Pod被保存在Etcd中，kube-scheduler会收到Pod新建的事件。新建的Pod没有和任何Node绑定，也即 Pod的 .spec.nodeName 字段值是空的。这时候，kube-scheduler就会将Pod加入到自己的待调度队列中，对该Pod进行调度。kube-scheduler会选择一个合适的Node，并将Node与该Pod进行绑定，也即设置Pod的.spec.nodeName 为目标Node，并将Pod更新到Etcd中；
- kubelet在启动后，通过List-Watch机制监听kube-apiserver中Node资源的变化。当发现本节点有一个新的Pod需要创建时，就会调用容器运行时，在节点上启动并运行Pod；
- 节点上的kubelet 还会通过容器运行时获取 Pod 的状态，然后更新到kube-apiserver中，当然最后也是写入到 Etcd 中去的。

### 调度器视角看Pod调度

提示：本小节，上一节课已经介绍下。为了内容的完备性，这里再介绍下。

下图展示了调度器调度 Pod 的总体流程：

<img src="image/FnCpAKc0qoEZKy918J6pPkv3jqxJ" alt="img" style="zoom:50%;" />

kube-scheduler 在启动后会通过 List-Watch 的方式，监听来自 kube-apiseraver 的 Pod、Node、PV、PVC 等资源的变更事件。

会将 Pod 变更事件放在 Scheduling Queue（调度队列）中。 Scheduling Cycle（调度循环）会不断地从Scheduling Queue 中 POP 带调度的 Pod，执行调度流程。

会将 Node、Pod、PV、PVC 等资源，缓存在 Cache 中，缓存在 Cache 中的信息是经过加工的信息。在Scheduling Cycle 中会被直接使用，以提高调度性能。

主循环从队列 POP 出一个 Pod 进入 Scheduling Cycle，在这里依次执行预过滤、打分等插件逻辑，结合 Cache 中的资源快照挑选可行节点。如果没有节点满足需求，则走 Preemption 分支尝试抢占低优先级 Pod。若仍失败，则把当前 Pod 标记为不可调度并回到队列等待下次机会。

一旦确认“Schedulable”，调度器进入 Binding Cycle，将所选节点写回 Pod.spec.nodeName 并调用 Bind 扩展或直接向 apiserver 发起 Bind 请求。绑定成功后，调度器的任务告一段落，Pod 对象被标记为已绑定，接下来由目标节点上的 kubelet 接管，完成镜像拉取、容器创建等运行阶段。

整个流程依靠事件驱动、缓存快照和多阶段插件体系实现高吞吐与实时性，并通过重回队列、抢占等机制保证在资源紧张时仍能尽量满足调度需求。

## Kubernetes 调度器源码概览

Kubernetesv1.31.1 版本的源码仓库中，跟 kube-scheduler 相关的源码如下：

```shell
├── cmd/ # 存放组件main文件的目录。将应用下的所有组件统一放在一个目录中，便于维护
│   └── kube-scheduler/ # scheduler应用层代码，主要包括应用配置、应用构建和启动代码
│       ├── app/
│       │   ├── config/
│       │   │   └── config.go # 保存了应用配置相关代码。配置内容通过命令行选项或者配置文件构建而来
│       │   ├── options/
│       │   │   ├── configfile.go # 配置文件读取或写入
│       │   │   ├── deprecated.go # 包含已弃用命令行 Flag
│       │   │   └── options.go # 用来给应用设置命令行选项，并对这些选项进行补全和校验
│       │   ├── server.go # 包含了创建并启动调度器的代码
│       └── scheduler.go # scheduler main 入口
├── hack/
│   └── local-up-cluster.sh* # 用于本地测试集群的部署脚本，包括 kube-scheduler 的部署
├── Makefile -> build/root/Makefile # 用于编译 kube-scheduler 二进制文件的 Makefile，编译命令为：make all WHAT=cmd/kube-scheduler。
├── _output/
│   └── bin/kube-sheduler # 编译生成的 kube-scheduler 二进制文件
├── pkg/ # 目录中保存了调度器的核心实现代码
│   ├── features/
│   │   └── kube_features.go # 定义了一些与 kube-scheduler 相关的功能门控
│   └── scheduler/ # 调度器的核心实现
│       ├── apis/ # 调度器组件配置 KubeSchedulerConfiguration 定义
│       │   └── config/
│       │       ├── latest/
│       │       │   └── latest.go # 包含了用来获取最新KubeSchedulerConfiguration定义的 Default() 函数
│       │       ├── register.go # 包含了用于注册调度相关联的资源对象的内容版本的代码
│       │       ├── scheme/
│       │       │   └── scheme.go # 用于将资源对象注册到全局的资源注册表中，会同时注册内部版本资源对象和版本化的资源对象
│       │       ├── testing/ # 定义了一些 方法、变量用来参与调度器的单元测试。例如：包含了参与测试的调度器插件的配置。
│       │       │   ├── config.go
│       │       │   └── defaults/
│       │       │       └── defaults.go
│       │       ├── types.go # 包含了 KubeSchedulerConfiguration 内部版本定义
│       │       ├── types_pluginargs.go # 包含了调度器插件配置资源对象的内部版本定义
│       │       └── zz_generated.deepcopy.go # 使用 deepcopy-gen 工具生成的代码（内部版本的深拷贝）
│       │       ├── v1/ # 包含了调度相关资源对象 v1 版本定义（外部版本）
│       │       │   ├── conversion.go # 包含了内部版本和 v1 版本自定义转换规则
│       │       │   ├── default_plugins.go # 包含了调度器插件配置资源对象默认值设置代码
│       │       │   ├── defaults.go # 包含了KubeSchedulerConfiguration资源对象的默认值设置代码
│       │       │   ├── register.go # 包含了用于注册调度相关联的资源对象的 v1 版本的代码
│       │       │   ├── zz_generated.conversion.go # 使用 conversion-gen 工具生成的代码
│       │       │   ├── zz_generated.deepcopy.go # 使用 deepcopy-gen 工具生成的代码（v1 版本的深拷贝）
│       │       │   └── zz_generated.defaults.go # 使用 defaulter-gen 工具生成的代码
│       │       ├── validation/ # 包含了调度器相关资源对象的验证代码
│       │       │   ├── validation.go # 主要验证 KubeSchedulerConfiguration
│       │       │   └── validation_pluginargs.go # 主要验证各类调度器插件资源配置对象
│       ├── eventhandlers.go # 包含了对 Pod 待调度队列和 Node 缓存操作的方法
│       ├── extender.go # 包含了 scheduler extender（调度器扩展器）相关的代码
│       ├── framework/ # 包含了 Scheduling Framework（调度框架） 相关的代码
│       │   ├── autoscaler_contract/
│       │   ├── cycle_state.go
│       │   ├── events.go
│       │   ├── extender.go
│       │   ├── interface.go
│       │   ├── listers.go
│       │   ├── parallelize/
│       │   │   ├── error_channel.go
│       │   │   └── parallelism.go
│       │   ├── plugins/ # 包含了各类内置的调度插件实现
│       │   │   ├── defaultbinder/ # 默认的绑定器，负责将 Pod 绑定到节点上执行
│       │   │   ├── defaultpreemption/ # 在资源不足时，负责预先终止低优先级的 Pod，以腾出资源给高优先级的 Pod
│       │   │   ├── dynamicresources/ # 动态资源调度插件，用于根据实时资源需求动态调整 Pod 的调度
│       │   │   ├── examples/ # 不同类型调度器的实现 Demo
│       │   │   │   ├── multipoint/
│       │   │   │   │   └── multipoint.go # 多点插件示例
│       │   │   │   ├── prebind/
│       │   │   │   │   └── prebind.go # 预绑定插件示例
│       │   │   │   └── stateful/
│       │   │   │       └── stateful.go # 有状态插件示例
│       │   │   ├── feature/ # 定义 Features 类型的结构体，用于存储被调度器插件用到的 Feature Gate 的值。使用该包可以避免直接依赖 Kubernetes 内部的 features 包
│       │   │   ├── helper/ # 包含了一些工具或者帮助类的函数
│       │   │   ├── imagelocality/ # 选择已经存在 Pod 运行所需容器镜像的节点。实现的扩展点：Score
│       │   │   ├── interpodaffinity/ # 根据 Pod 间的亲和性和反亲和性调度 Pod
│       │   │   ├── names/ # 统一定义了内置调度器插件的名字
│       │   │   ├── nodeaffinity/ # 根据节点的属性或标签，调度 Pod 到具有特定属性的节点上
│       │   │   ├── nodename/ # 检查 Pod 指定的节点名称与当前节点是否匹配，也即根据节点名调度
│       │   │   ├── nodeports/ # 根据节点上的端口分配情况，调度 Pod 到端口可用的节点上
│       │   │   ├── noderesources/ # 包含了一些根据节点资源和 Pod 资源请求来调度 Pod 的算法，是调度器中非常核心的调度插件实现
│       │   │   │   ├── balanced_allocation.go # 调度 Pod 时，选择资源使用更为均衡的节点
│       │   │   │   ├── fit.go # 检查节点是否拥有 Pod 请求的所有资源，只有满足资源需求的节点才被允许调度
│       │   │   │   ├── least_allocated.go # 选择资源分配较少的节点
│       │   │   │   ├── most_allocated.go # 选择已分配资源多的节点（常用于降本场景下）
│       │   │   │   ├── requested_to_capacity_ratio.go
│       │   │   │   ├── resource_allocation.go
│       │   │   ├── nodeunschedulable/ # 过滤掉 .spec.unschedulable 值为 true 的节点 
│       │   │   ├── nodevolumelimits/ # 根据节点的存储容量限制，调度 Pod 到合适的节点上
│       │   │   ├── podtopologyspread/ # 根据 Pod 的拓扑分布要求，调度 Pod 到不同的区域或节点上
│       │   │   ├── queuesort/ # 根据 Pod 的优先级对待调度的 Pod 进行排序，优先级高的 Pod 会被优先调度
│       │   │   ├── registry.go
│       │   │   ├── schedulinggates/ # 根据调度门控条件，控制 Pod 的调度行为
│       │   │   ├── tainttoleration/ # 根据节点的 Taint 和 Pod 的 Tolerations，以确定 Pod 是否可以调度到节点上
│       │   │   ├── testing/ # 包含了一些用于调度器单元测试的函数
│       │   │   ├── volumebinding/ # 检查节点是否有请求的卷，或是否可以绑定请求的卷，类似的有 VolumeRestrictions/VolumeZone/NodeVolumeLimits/EBSLimits/GCEPDLimits/AzureDiskLimits/CinderVolume
│       │   │   ├── volumerestrictions/ # 根据卷的限制条件，调度 Pod 到符合条件的节点上
│       │   │   └── volumezone/ # 根据卷的存储区域要求，调度 Pod 到具有相应存储区域的节点上
│       │   ├── preemption/ # 包含了实现抢占逻辑的相关代码
│       │   ├── runtime/ # Scheduling Framework 的运行时实现（最核心的代码逻辑）
│       │   │   ├── framework.go
│       │   │   ├── instrumented_plugins.go
│       │   │   ├── registry.go
│       │   │   └── waiting_pods_map.go
│       │   └── types.go # 包含了 Scheduling Framework 用到的类型定义及方法实现
│       ├── internal/ # 调度器内部包（仅用于调度器实现）
│       │   ├── cache/ # 调度器缓存实现
│       │   │   ├── cache.go
│       │   │   ├── debugger/ # 包含了一些 Debug 方法，用来检查和更新缓存（用于 debug 目的）
│       │   │   ├── interface.go # 定义了实现 Cache 接口的方法列表
│       │   │   ├── node_tree.go # 包含了 nodeTree 类型的结构体定义，于存储每个区域中的节点名称
│       │   │   └── snapshot.go # 包含了创建和管理缓存快照的代码实现
│       │   ├── heap/ # 包含了一个标准的堆实现，以及对堆进行操作的方法
│       │   └── queue/ # 定义 SchedulingQueue 类型的接口，接口定义了操作待调度 Pod 队列需要实现的方法。目录中也包含了一个具体的 SchedulingQueue 实现：优先级队列。
│       ├── metrics/ # 包含了调度器指标记录相关实现（变量、方法、函数等）
│       ├── profile/ # 包含了根据配置创建 framework.Framework 类型实例的 NewMap 函数
│       ├── schedule_one.go # 处理单个调度循环的实现
│       ├── scheduler.go # 调度器实现的核心逻辑
│       ├── testing/ # 包含了测试 Scheduling Framework 的代码实现
│       └── util/ # 包含了一些工具类的函数
└── staging/
    └── src/
        └── k8s.io/
            └── kube-scheduler/
                ├── code-of-conduct.md
                ├── config/
                │   └── v1/
                │       ├── register.go # 包含了 v1 版本资源对象注册逻辑
                │       ├── types.go # 包含 v1 版本 KubeSchedulerConfiguration 的定义
                │       ├── types_pluginargs.go # 包含了 v1 版本调度器插件相关资源对象的定义
                │       └── zz_generated.deepcopy.go # 使用 deepcopy-gen 工具生生成的代码用来深拷贝 v1 版本的资源对象
                ├── extender/ # 包含了 scheduler extender 相关的类型定义
                │   └── v1/
                │       ├── types.go # v1 版本的类型定义，例如：ExtenderPreemptionArgs、ExtenderPreemptionResult、ExtenderBindingArgs、ExtenderBindingResult 等
                │       └── zz_generated.deepcopy.go # deepcopy-gen 工具生成的代码
```

上面的 kube-scheduler 源码树中，列出了几乎所有跟 kube-scheduler 相关目录及文件。

这里需要注意，考虑到代码的复用性，对外的 v1版本的资源对象定义存放在 staging/src/k8s.io/kube-scheduler/config/v1/types.go文件中，内部版本资源对象定义，因为不需要对外，所以存放在 pkg/scheduler/apis/config/types.go文件中。

另外外部不需要感知的 v1版本资源对象的具体实现，例如：默认值设置，内外部版本转换、资源定义校验等实现放在 pkg/scheduler/apis/config/v1 目录下。