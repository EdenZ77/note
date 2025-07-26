如果你想学习 Kubernetes 相关技术，例如：Kubernetes 运维、Kubernetes 开发、云原生技术，基本上都需要一个 Kubernetes 集群。因为 Kubernetes 集群组件多，部署复杂，所以社区当前也有不少部署 Kubernetes 集群的方法来简化部署 Kubernetes 的复杂度，降低部署难度。

那么社区当前有哪些部署 Kubernetes 集群的方法，这些方法又分别适用什么场景呢？本节课，我就来详细给你介绍下，使你在未来能够选择合适的方法，快速搭建满足需求的 Kubernetes 集群。

## Kubernetes 集群部署方法介绍

社区当前有很多部署 Kubernetes 的方案和工具，每种方案和工具都有其自身的特点、优缺点和适用场景。我们可以根据需要选择合适的方式来部署 Kubernetes 集群。当前社区常用的部署工具有：minikube、kind、kubeadam、k3d、microk8s、二进制部署、Kops、Kubespray、Terraform 等。其中，用的最多的是minikube、kind、kubeadam 和二进制部署。当然，如果你舍得花钱，也可以直接在云服务提供商一键创建一个生产级的 Kubernetes 集群。

本节课，我就来简单介绍下以下 4 种部署 Kubernetes 集群的方法。

### Kind

[Kind](https://github.com/kubernetes-sigs/kind)（**K**ubernetes **In** **D**ocker） 是一个 Kubernetes SIG 项目，用来快速在本地创建一个开发、测试用的 Kubernetes 集群。更多关于 kind 的介绍，可阅读其官方文档：https://kind.sigs.k8s.io/。

Kind 的核心特性如下：

1. 支持集群 HA，其实就是创建多个 control-plane 类型的 Node 节点；
2. 创建的 Kubernetes 集群经过 Kubernetes 一致性认证；
3. 支持 Linux、macOS 和 Windows；
4. 因为 Kind 足够轻量，所以 Kind 也大量使用于 CI 流程中的集群创建。

更多 Kind 的介绍和使用，见下节课：**项目部署（2）：如何配置和创建一个 Kind 集群？**

> 说明：早期的 minikube 不支持以容器的方式部署 Kubernetes 集群，这直接导致了 Kind 项目的诞生。



### Minikube

[Minikube](https://github.com/kubernetes/minikube) 是一个 Kubernetes SIG 项目，用来在 macos、linux、windows 上快速部署本地的 kubernetes 集群。因为 minikube 可以很方便的快速部署一个 kubernetes 集群，所以很适合用来创建开发、测试用的 kubernetes 集群。

minikube 的核心特性如下：

1. 支持最新的 Kubernetes 版本；
2. 跨平台，支持 Linux，macOS，Windows；
3. 支持以虚拟机、容器或裸金属的方式部署 Kubernetes 集群；
4. 支持 CRI-O，containerd，docker 容器运行时；
5. 支持一些高级功能，如负载均衡器、文件系统挂载、FeatureGates 和网络策略；
6. minikube 以 AddOn 的方式，支持很多应用插件，例如：efk、gvisor、istio、kubevirt、registry、ingress 等；
7. 支持常用的 CI 环境，例如：[Travis CI](https://travis-ci.com/)、[Github](https://help.github.com/en/actions/automating-your-workflow-with-github-actions/about-continuous-integration)、[CircleCI](https://circleci.com/)、[Gitlab ](https://about.gitlab.com/product/continuous-integration/)等。

minikube 当前仍处在活跃的更新阶段，更多关于 minikube 的介绍可阅读其官方文档：[https://minikube.sigs.k8s.io/docs/](https://minikube.sigs.k8s.io/docs)



### 二进制部署

二进制部署，就是直接基于 Kubernetes 官方提供的发布版的二进制文件（例如 [v1.29.2 版本 Kubernetes 二进制文件](https://github.com/kubernetes/kubernetes/blob/master/CHANGELOG/CHANGELOG-1.29.md#v1292)）进行部署。在部署过程中，需要下载这些二进制文件，并根据集群配置创建 Systemd Unit 文件，创建完成后，按照预定的顺序启动 Systemd 服务，完成 Kubernetes 集群的部署。

二进制部署是最复杂、开发工作量最大的部署方式，一般只有在大公司、大团队，或者对部署集群有特殊需求的团队中才会采用这种方式来部署。早期社区没有太多 Kubernetes 集群部署工具，很多团队，都采用这种方式来部署集群。但是，随着社区 Kubernetes 部署工具的成熟，例如：kubeadm、cluster-api 等，这些团队逐渐的放弃了原始的二进制部署方式，而改为基于 kubeadm 或 cluster-api 来部署。



### kubeadam

[Kubeadm](https://github.com/kubernetes/kubeadm) 是 Kubernetes 官方提供的一个工具，用来便捷的创建生产级可用 Kubernetes 集群。Kubeadm 提供了一种简单快捷的方式，来创建一个最小可行的、安全的 Kubernetes 集群。

常用的 kubeadm 命令如下：

1. kubeadm init：初始化 Kubernetes 控制面节点；
2. kubeadm join：将控制面组件或工作节点添加到已有的 Kubernetes 集群中；
3. kubeadm upgrade：升级 Kubernetes 到指定的版本；
4. kubeadm reset：撤销 kubeadm init 或 kubeadm join 对机器做的所有更改。



### cluster-api

[cluster-api](https://github.com/kubernetes-sigs/cluster-api)（capi） 是一个 Kubernetes 项目，使用声明式 API 的方式来创建、配置和管理 Kubernetes 集群。cluster-api 是更为高阶的集群部署方式，其复杂度也是很高的，通常只有具有很强研发能力的团队才会选择使用 cluster-api 的方式来部署。

现在很多公有云厂商，例如火山引擎、腾讯云、阿里云等都逐渐将集群部署方式迁移为 cluster-api 的部署方式。火山引擎当前已经是使用 cluster-api 来部署。

以下是 Cluster API 的一些主要特点和功能：

1. **声明式 API：**Cluster API 提供了一组自定义资源（Custom Resources）和控制器，允许用户通过声明式 API 对集群进行管理。用户可以定义集群的规格（Spec）和状态（Status），控制器负责根据规格来实现状态。
2. **可扩展性：**Cluster API 是一个可扩展的框架，可以轻松地集成到现有的 Kubernetes 生态系统中。用户可以编写自定义的 Provider 来支持不同的基础设施平台，如 AWS、Azure、GCP 等。
3. **多集群管理：**Cluster API 支持多集群管理，允许用户在一个 Kubernetes 集群中管理多个集群实例。这使得跨多个环境和地理位置的集群管理变得更加简单和统一。
4. **版本控制：**Cluster API 提供了版本控制机制，可以确保集群配置的一致性和可追踪性。用户可以轻松地管理和追踪集群配置的变更历史。
5. **自动化操作：**Cluster API 提供了自动化的集群生命周期管理功能，包括集群的创建、扩展、缩减和删除等操作。用户可以通过简单的 API 调用来执行这些操作。

cluster-api 比较复杂，明年规划的课程 **Kubernetes 开发高阶实战课** 中，会详细介绍如何使用 cluster-api 来进行集群管理（集群的增删改查），敬请期待。



### Minikube VS Kind

二者不是一个替代与被替代的关系，如果你对创建 kubernetes 没有特别的需求，并且 minikube 选择 Docker 驱动时，二者其实没有太多区别。但如果你一定想选择一个最合适的，那么你可以通过二者的对比优点，来自行选择。

如果你只想快速部署一个测试用的 Kubernetes 集群，没有其他特别需求，例如：期望在虚拟机上部署、期望集群中部署一些插件（例如：efk、gvisor、istio、kubevirt、registry、ingress）以增强集群的功能，那么 kind 是一个好的选择，因为其足够轻量。如果你想创建一个 HA 集群，当前只能选择 kind，因为 minikube 暂时还不支持。

如果你想创建一个稍微复杂点的测试集群，并且期望能对集群做更多的管控，可以使用 minikube 来创建。

本实战营未来会主要选择使用 minikube 来创建测试集群。因为，比较喜欢 minikube 强大的功能、功能丰富的 Kubernetes 集群。minikube 支持了 docker 引擎之后，个人感觉 kind 工具会显得有点多余。

## 如何选择合适的部署方式？

社区提供了这么多部署 Kubernetes 集群的方式，那么到底该如何选择呢？选择何种方式搭建 kubernetes 集群，很多时候取决于你使用 Kubernetes 集群的场景。这里，给你介绍下我的选择方式。



### Kubernetes 源码开发

如果你是一个 Kubernetes 开发者，例如：需要修改 kubernetes 源码，来添加新的功能或者修复已知 bug，那么你可能需要频繁的修改 kubernetes 代码，编译 kubernetes 组件，并部署。这时候，你需要一种快捷的方式，来直接更新修改过的 kubernetes 二进制文件。因为是开发测试场景，最好要是轻量的，因为如果能够快速更新和启动 kubernetes 组件，在一个高频的场景下会极大提高你的开发效率。这时候，最好的选择是使用 kubernetes 源码仓库下提供的hack/[local-up-cluster.sh](http://local-up-cluster.sh/)脚本。

另外，使用 hack/[local-up-cluster.sh](http://local-up-cluster.sh/) 脚本来创建、并测试 Kubernetes 集群还有一个好处，就是当代码合并到 Kubernetes 仓库时，至少通过了该脚本的测试。

当然，如果你不介意初次部署 kubernetes 集群的复杂操作，你也可以花一点时间使用 kubeadam 或二进制部署的方式，搭建一个 kubernetes 集群，专门用作长期的 kubernetes 开发测试用，之后每次更新，只更新目标组件即可。但这种方式有个弊端，就是如果 Kuberntes 集群变得不可用了，你可能要重新使用 kubeadam 或二进制部署的方式来搭建一个 Kubernetes 集群，而这个工作量不小。



### 云原生开发

如果你是一个云原生开发者，需要围绕 Kubernetres 来构建自己的应用，在开发应用的过程中，需要一个 Kubernetes 集群来部署你的应用。这时候，你需要一种可以快速搭建一个测试用的 Kubernetes 集群的方式。这种情况下，你可以选择 minikube 或 kind。

云原生实战营课程开发过程中，用的就是这种方式。云原生实战营课程同时选择了 kind 和 minikube，原因如下：

1. 课程的前半部分，为了使你能够更快速创建测试用的 Kubernetes 集群，并且减少学习过程中的障碍，我会选择更轻量的 kind 工具来创建测试集群；
2. 课程的后半部分会选择使用 minikube 来创建一个功能更加强大的 Kubernetes 集群，例如集成了 EKF 插件；
3. 同时选择使用 kind 和 minikube 还有一个重要目的，是想让你同时掌握这 2 个工具的使用方法，提供实战机会，丰富你的云原生知识。



### 部署生产级应用

如果你想在集群中部署生产环境使用的应用，那么你需要创建一个生产级可用的 Kubernetes 集群。搭建生产环境用的  kubernetes 集群，当前用的比较多的是使用 kubeadm 工具来安装部署。很多公有云厂商会有自己的一套 Kubernetes 集群部署系统，这些系统通常情况都是基于 kubeadm 工具封装的，例如可以使用 Ansiable、ArgoCD 等来封装 kubeadm。因为 kubeadm 工具已经提供了功能强大的集群部署方式，所以，企业只需要进行简单的封装即可。

另外，一些企业或团队，期望能够对 Kubernetes 集群的部署方式进行完全的掌控，或者觉得 kubeadm 不能满足企业部署 Kubernetes 集群的某些需求，这些企业也可以直接使用 Kubernetes 二进制文件，并自定义启动参数来部署 Kubernetes 集群。

将集群部署在物理机或虚拟机上，可以获得更好的性能和隔离性，但是不利于运维和管理，并且资源利用率不高。所以，当前越来越多的企业选择将 Kubernetes 集群部署在 Kubernetes 集群中（也即：Kubernetes In Kubernetes）。

当然，如果你是初创公司，没有人力来维护一套复杂的集群管理系统，或者你愿意付费，以减小集群创建和运维成本。你也可以选择在公有云厂商付费一键购买一个功能完备的、生产可用的 Kubernetes 集群。