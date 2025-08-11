想掌握好Kubernetes调度器，除了要掌握kube-sheduler的设计、实现和各类调度器插件之外，还需要了解整个Kubernetes调度器生态。生态是对调度器核心能力的补充和增强，生态中的各类方法、技术、实现和项目都能够帮助我们极大的增强Kubernetes调度器的能力，并解决实际遇到的问题。实际上，生态中的很多项目或者实现，也都是为了解决企业调度器使用过程中面临的核心痛点。

本节课，会给你详细介绍Kubernetes调度器的生态。因为Kubernetes调度器生态庞大，并且正在不断往前发展，所以很难完全介绍。但本节课，会介绍生态中的重点内容。

### SIG Scheduling 兴趣小组简介

介绍Kubernetes生态，首先要介绍 SIG Scheduling 兴趣小组，在Kubernetes生态中占有非常重要的位置。

Kubernetes 兴趣小组（Kubernetes Special Interest Groups，简称 SIGs）是 Kubernetes 社区中的重要组成部分，旨在促进特定领域的协作与发展。每个兴趣小组专注于 Kubernetes 的某个特定方面或功能，负责相关功能的设计、开发和维护。

Kubernetes 调度器是Kubernetes项目中非常核心的功能，SIGs 为了促进Kubernetes调度器的发展，成立了 SIG Scheduling 兴趣小组。

[SIG Scheduling](https://github.com/kubernetes/community/tree/master/sig-scheduling) (Special Interest Group Scheduling) 是 Kubernetes 社区中的一个特别兴趣小组，专注于 Pod 调度相关组件的设计、开发和维护。SIG Scheduling 兴趣小组设计和实现了一系列的功能用来帮助开发者自定义 Pod 调度策略。这些功能包括提高工作负载的可靠性、更高效地使用集群资源和 Pod 调度策略。

### SIG Scheduling 兴趣小组维护的开源项目

SIG Scheduling 兴趣小组开发和维护了一些开源的调度器相关的项目。这些项目在企业中也被大量使用。这里罗列如下：

<img src="image/FtCNPc3v0gqpF5RQx2gFYavbAvhR" alt="img" style="zoom:50%;" />

### Kubernetes社区调度器项目介绍

除了SIG Scheduling兴趣小组维护的 [scheduler-plugins](https://github.com/kubernetes-sigs/scheduler-plugins) 项目和kube-scheduler项目之外，社区也有一些开源的优秀调度器项目。因为调度器及调度插件通常跟公司的技术栈及调度需求结合比较紧密，所以，虽然各大公有云厂商及使用Kubernetes的公司都会有自己的调度器扩展或者自定义调度器。但是，开源的调度器项目并不多。

这里，罗列一些社区优秀的开源调度器项目：

<img src="image/Ft4p_Lhtwc7M3mm-Ef2arKKX8N7I" alt="img" style="zoom:50%;" />

## 总结

Kubernetes 调度器不仅仅是一份 kube-scheduler 的代码，还包含围绕它不断演进的完整生态。为了促成这一生态的共建，社区成立了 SIG Scheduling 兴趣小组，专门负责 Pod 调度相关功能的设计、开发与维护。

SIG Scheduling 在保证 kube-scheduler 主仓库持续演进的同时，还孵化了诸如 cluster-capacity、descheduler、scheduler-plugins、kueue、kwok 以及 Wasm 扩展等子项目，它们分别面向容量评估、负载再平衡、插件复用、作业队列、节点模拟与新技术探索等场景，为企业在使用原生调度器时提供了“即插即用”的增强能力。

除了官方 SIG 推动的项目，社区和云厂商也围绕自身业务需求开源了多种高性能或场景化的调度器实现，例如字节跳动的 Godel、腾讯的 Crane-scheduler、华为的 Volcano 以及阿里云的 Koordinator，它们通过自研算法或共享状态架构进一步提升了在大规模集群、批量作业或混部场景下的吞吐与公平性。

掌握这些生态项目及其定位，可以帮助工程师在面对企业级复杂调度需求时，迅速找到合适的现成方案或借鉴思路，而不必从零开始构建整套能力。