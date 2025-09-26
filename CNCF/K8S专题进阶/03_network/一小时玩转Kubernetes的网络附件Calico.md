# Kubernetes 网络附件 Calico

# 主要内容

- Kubernetes 的网络模型- Kubernetes CNI- Underlay Network 和 Overlay Network- Kubernetes 常见网络附件- Calico 网络工作机制- Calico 的 Pod 网络特性- Calico 网络模型- Calico 软件组件- Calico 部署和各种网络模型案例

# 1 Kubernetes 网络机制

# 1.1 Kubernetes 的网络模型

Kubernetes 集群内一般有三种网络：节点网络, Service 网络, Pod 网络

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/a33c452f4a997247530cf7b958c8df457c1df0f57b8e76677cca1d1f72296fb8.jpg)

因此有如下的四类通信

- 同一Pod内容器间的通信（Container to Container）：Pod内所有容器共享同一个网络命名空间，容器间通过loopback直接通信- Pod间的通信（Pod to Pod）：network plugin is configured to assign IP addresses to Pods.- Service到Pod间的通信（Service to Pod）：依赖Kube-proxy的所管理的Service 在内核实现，比如：iptables或IPVS策略实现- 集群外部与Service之间的通信（external to Service）：依赖于不同的Service 类型，比如：NodePort 或者LoadBalancer等Kube Proxy功能

KubeProxy功能

- 指供service网络通信- iptables或IPVS规则- 实现服务发现

# 1.2 Kubernetes CNI

https://www.cni.dev/https://github.com/containernetworking/cni

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/76674918c0c268df2d0681394fe87f7ca0dd15b5ff1e7d465f0728c25b133647.jpg)

Kubernetes中并没有提供一个核心组件完成Pod之间的网络通信功能，而是Kubernetes定义了CNI（Container Network Interface）接口标准

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/4a6455f221450f4909af4b916dc0838c3e8b4d8f849f89fce3fdb13987710aa8.jpg)

Kubernetes将Pod间的网络通信功能通过CNI委托给了第三方的外部的插件

只要由第三方的网络插件遵守CNI规范，即可支持Kubernetes的Pod通信

在创建kubernetes集群时，需要安装CNI兼容的网络插件，否则多个节点间无法正常通信，每个节点都会处于NotReady状态。

CNI容器网络接口是CNCF的项目，是由google和CoreOS主导制定的容器网络标准，但自身并非负责具体实现，只是一种协议和标准

CNI是容器引擎与遵循该规范网络插件的中间层，专用于为容器配置网络子系统

使用CNI插件编排网络，Pod初始化或删除时，kubelet会调用默认CNI插件，创建虚拟设备接口附加到相关的底层网络，设置IP、路由并映射到Pod对象网络名称空间。

CNI基本思想：创建容器时，先创建好网络名称空间，然后调用CNI插件配置这个网络，而后启动容器内的进程

CNI网络插件主要功能

- 创建Pod的网络设备- IPAM IP Address Management，用于给Pod分配IP地址    
- flannel: 10.244.0.0/16    
- calico: 192.168.0.0/16    
- cilium: 192.168.0.0/16- 同一个节点Pod间通信- 不同节点的Pod间通信- 也可以支持额外功能，比如：流量控制

CNI将Pod接入Pod网络流程

- 创建Pod网络- 创建Pod时为每个Pod创建对应虚拟网络接口- 将新创建的Pod的虚拟网络接口连接至Pod网络中- 给启动的Pod的虚拟网络接口分配IP地址

# CNI插件可以分为三部分：

main、meta、ipam（按照源代码中的存放目录可以看出）

https://github.com/containernetworking/plugins/tree/main/pluginshttps://www.cni.dev/plugins/current/

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/76334bc5e754c2399478e5c802780155b0333d8bfaefd6b543d594ee9c954d06.jpg)

- Main

主要负责创建和删除网络以及向络中添加删除容器

专注于连通容器与容器之间以及容器与宿主机之间的通信

该类插件还负责创建和容器相关网络设备，例如：bridge、ipvlan、macvlan、loopback、ptp、veth以及vlan等虚拟设备

- IPAM:

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/b67343fc1c269e088eaa16def5b4d7239b6b0c0e9f11739af2bb5dc7308f2c6a.jpg)

IP Address Management，仅用于给当前节点的Pod分配IP地址，不提供网络实现

IPAM Plugin 作为 CNI NetPlugin的一部分，与 CNI Plugin 一起工作

该类插件API负责创建/删除地址池以及分配/回收容器的IP地址

目前，该类型插件的实现主要有dhcp，host- local和static

- DHCP: 通过上节点运行dhcp服务进程通过DHCP协议获取地址

- host-local: 基于预置的本地地址范围库进行地址分配

- Static: 静态IP

- Meta:

其它的plugins，包括如下

- tuning: Changes sysctl parameters of an existing interface- portmap: An iptables-based portmapping plugin. Maps ports from the host's address space to the container- bandwidth: Allows bandwidth-limiting through use of traffic control tbf (ingress/egress)- sbr: A plugin that configures source based routing for an interface (from which it is chained)- firewall: A firewall plugin which uses iptables or firewalld to add rules to allow traffic to/from the container

# 1.3 Underlay Network 和 Overlay Network

Kubernetes 的NetPlugin目前常用的实现Pod网络的方案有承载网络（Underlay Network）和叠加网络（Overlay Network）两类

# - Underlay Network 高层(承载)网络

即实际的物理网络，包括所有的物理设备（如路由器，交换机，电缆等），以及它们之间的网线连接。

在大多数情况下，Underlay网络负责数据包的物理传输，包括IP路由，网络拓扑，带宽等。

基础网络通常由网络管理员直接管理，并使用传统的网络协议和技术（如IP，Ethernet，MPLS等）

# 底层（承载）网络支持两种模型：

# 路由模型

承载网络通常使用direct routing（直接路由，即将节点做为路由器）技术在Pod的各子网间路由Pod的IP报文把节点当路由器，每个节点背后都是一个末端网络，即持有的Pod子网(PodCIDR)每个节点的路由表通过查询中央数据库(如：ETCD)或BGP协议生成到由其它节点上的Pod目标子网的路由记录

# 二层网络

使用Bridge，MACVLAN或IPVLAN等技术直接将容器暴露至外部网络中承载网络的解决方案就是一类非借助于隧道协议而构建的容器通信网络容器网络中的承载网络是指借助驱动程序将宿主机的底层网络接口直接暴露给容器使用的一种网络构建技术

# 此方式不支持节点间的跨网段通信

# OverlayNetwork叠加网络

即虚拟网络

在UnderlayNetwork之上创建的虚拟网络被称为OverlayNetwork叠加网络。叠加网络使用软件来抽象UnderlayNetwork的硬件细节，从而创建虚拟访问网络设备和连接。叠加网络可以用来创建复杂的网络拓扑，实现网络虚拟化，实现多租户隔离，以及满足特定应用程序的特殊网络需求。叠加网络使用各种封装隧道协议（如VXLAN，IPIP，GRE，STT等）在UnderlayNetwork上封装和传输数据包。

通过隧道协议报文封装Pod间的通信报文（IP报文或以太网帧）来构建虚拟网络，相较于承载网络，叠加网络由于存在额外的隧道报文封装，会存在一定程度的性能开销

叠加网络的底层网络也就是承载网络

但希望创建跨越多个L2或L3的逻辑网络子网时，只能借助于叠加封装协议实现节点间跨网段通信此方式生产较为常见

总的来说，UnderlayNetwork和OverlayNetwork的主要区别在于：

UnderlayNetwork是物理的，实际的，负责数据包的实际传输OverlayNetwork是虚拟的，由软件实现，并且提供了对网络资源的更灵活的管理和使用。

# 1.4 Kubernetes常见网络插件

https://github.com/containernetworking/cni https://kubernetes.io/zh- cn/docs/concepts/cluster- administration/addons/

# 联网和网络策略

联网和网络策略- ACI 通过 Cisco ACI 提供集成的容器网络和安全网络。- Antrea 在第 3/4 层执行操作，为 Kubernetes 提供网络连接和安全服务。Antrea 利用 Open vSwitch 作为网络的数据面。- Calico 是一个联网和网络策略供应商。Calico 支持一套灵活的网络选项，因此你可以根据自己的情况选择最有效的选项，包括非覆盖和覆盖网络，带或不带 BGP。Calico 使用相同的引擎为主机、Pod 和（如果使用 Istio 和 Envoy）应用程序在服务网格层执行网络策略。- Canal 结合 Flannel 和 Calico，提供联网和网络策略。- Cilium 是一种网络、可观察性和安全解决方案，具有基于 eBPF 的数据平面。Cilium 提供了简单的 3 层扁平网络，能够以原生路由（routing）和覆盖/封装（overlay/encapsulation）模式跨越多个集群，并且可以使用与网络寻址分离的基于身份的安全模型在 L3 至 L7 上实施网络策略。Cilium 可以作为 kube- proxy 的替代品；它还提供额外的、可选的可观察性和安全功能。- CNI- Genie 使 Kubernetes 无缝连接到 Calico、Canal、Flannel 或 Weave 等其中一种 CNI 插件。- Contiv 为各种用例和丰富的策略提供访问配置的网络（带 BGP 的原生 L3、带 vxlan 的覆盖、标准 L2 和 Cisco- SDN/ACI）。Contiv 项目完全开源。其安装程序提供了基于 kubeadm 和非 kubeadm 的安装选项。- Contrail 基于 Tungsten Fabric，是一个开源的多云网络虚拟化和策略管理平台。Contrail 和 Tungsten Fabric 与业务流程系统（例如 Kubernetes、OpenShift、OpenStack 和 Mesos）集成在一起，为虚拟机、容器或 Pod 以及裸机工作负载提供了隔离模式。- Flannel 是一个可以用于 Kubernetes 的 overlay 网络提供者。- Knitter 是在一个 Kubernetes Pod 中支持多个网络接口的插件。- Multus 是一个多插件，可在 Kubernetes 中提供多种网络支持，以支持所有 CNI 插件（例如 Calico、Cilium、Contiv、Flannel），而且包含了在 Kubernetes 中基于 GPIO、DPDK、OVS- DPDK 和 VPP 的工作负载。- OVN- Kubernetes 是一个 Kubernetes 网络驱动，基于 OVN（Open Virtual Network）实现，是从 Open vSwitch（OVS）项目衍生出来的虚拟网络实现。OVN- Kubernetes 为 Kubernetes 提供基于覆盖网络的网络实现，包括一个基于 OVS 实现的负载均衡器和网络策略。- Nodus 是一个基于 OVN 的 CNI 控制器插件，提供基于云原生的服务功能链（SFC）。- NSX- T 容器插件（NCP）提供了 VMware NSX- T 与容器协调器（例如 Kubernetes）之间的集成，以及 NSX- T 与基于容器的 CaaS/PaaS 平台（例如关键容器服务（PKS）和 OpenShift）之间的集成。- Nuage 是一个 SDN 平台，可在 Kubernetes Pods 和非 Kubernetes 环境之间提供基于策略的联网，并具有可视化和安全监控。- Romana 是一个 Pod 网络的第三层解决方案，并支持 NetworkPolicy API。- Weave Net 提供在网络分组两端参与工作的联网和网络策略，并且不需要额外的数据库。

# 网络插件主要功能：

Pod间的网络通信，最为核心的功能

构建一个网络将pod接入到这个网络中实时维护所有节点上的路由信息，实现所有节点上Pod之间的通信

网络策略，实现网络通信的安全控制

·通信加密，默认Pod间的通信是明文的，但安全要求较高时，也可以实现tls的安全加密通信

注意：这些网络插件可以完成所有的功能，也可以完成部分的功能，再借助于其他方案实现完整的方案但是需要注意的是：不要同时部署多个插件来做同一件事情，因为对于CNI来说，只会有一个生效。

常见的CNI解决方案有：

- Flannel

是CoreOS团队开源的最早的支持kubernetes的网络插件

用于让集群中不同节点创建的容器都具有集群内全局唯一的网络（集群外无法感知），也是当前Kubernetes开源方案中比较成熟的方案

支持HostGW和VXLAN模式，基于linuxTUN/TAP，使用UDP封装IP报文来创建叠加网络，并借助etcd维护网络分配情况

功能简单，特性较少，无法实现网络流量的控制等高级功能

Calico

纯3层的数据中心网络方案，支持IPIP和BGP模式，后者可以无缝集成像OpenStack这种laaS云架构，能够提供可控的VM、容器、裸机之间的IP通信。

但是，需要网络设备对BGP的支持，比如阿里云vpc子网内是不支持BGP的

在每台机器上运行一个vRouter，利用内核转发数据包

性能优秀，而且借助iptables实现网络策略控制等功能

# 不支持多播

- Canal由Flannel和Calico联合发布的一个统一网络插件，相当于两者的合体即集成了flannel和Calico的特性，也支持网络策略当前活跃度较低

- cilium

https://github.com/cilium/cilium

Cilium是一款基于eBPF技术的KubernetesCNI插件

Cilium在其官网上对产品的定位为“eBPF- based Networking, Observability, Security”，致力于为容器工作负载提供基于eBPF的网络、可观察性和安全性的一系列解决方案。

Cilium通过使用eBPF技术在Linux内部动态插入一些控制逻辑，可以在不修改应用程序代码或容器配置的情况下进行应用和更新，从而实现网络、可观察性和安全性相关的功能。

Cilium最初由Isovalent创建，于2021年10月成为CNCF孵化项目，现在有来自7家不同公司的维护者和800多名个人贡献者。就提交数量而言，Cilium在CNCF项目的活跃度中排名第二，仅次于Kubernetes。

2023年10月11日，CNCF发文宣布容器网络开源项目Cilium正式毕业。

Cilium使用eBPF（Extended Berkeley Packet Filter）技术，它可以在Linux内核中运行高性能的程序来处理网络数据包，从而实现高效的网络功能。Cilium通过eBPF程序来管理网络流量，执行负载均衡、安全策略、服务发现等任务

当你选择使用Cilium作为网络插件时，Cilium会接管网络功能，包括服务发现和负载均衡，Cilium会替代kube- proxy来提供更强大和高效的网络功能。因此不再依赖于kube- proxy，不再需要安装或配置kube- proxy。

支持多集群和安装多个CNI插件来增强功能

此插件相对复杂，如果想实现BGP还需要安装额外的插件，比如：kube- router

- Terway

Terway是阿里云开源的基于VPC网络的CNI插件，支持VPC和ENI(弹性网卡)模式，后者可实现容器网络使用VPC子网网络

Pod会通过弹性网卡资源直接被分配使用VPC中的IP地址，而不需要额外指定虚拟Pod网段。

Pod地址即为VPC中地址，无NAT损耗支持独占ENI模式，几乎无损。

支持基于Kubernetes标准的网络策略(Network Policy)来定义容器间的访问策略，并兼容Calico的网络策略。

支持对单个容器做带宽的限流。

- Contiv

思科开源的用于跨虚拟机、裸机、公有云或私有云的异构容器部署的开源容器网络架构方案直接提供多租户网络，支持L2(VLAN)、L3(BGP)、Overlay(VXLAN)

- kube-router

K8s网络一体化解决方案，可取代kube- proxy实现基于ipvs的Service，支持网络策略、完美兼容BGP的高级特性

- Weave Net

由weaveworks公司开发，当前此公司已倒闭

WeaveNet最初是为容器开发的，但后来演变成了Kubernetes网络插件

# 2 Calico

# 2.1 Calico 网络机制

# 2.1.1 Calico 介绍

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/40432a4196772263c4fd27cea1b63a15390614bbc259d887abe0a6acc2d444c3.jpg)

Calico (斑点猫)是一个开源的虚拟化网络方案，用于为云原生应用实现互联及策略控制

相较于 Flannel 来说，Calico 的优势是支持网络策略 network policy

Calico 允许用户动态定义 ACL 规则控制进出容器的数据报文，实现为 Pod 间的通信按需施加安全策略

Calico 还可以整合进大多数具备编排能力的各种平台，包括 Kubernetes、OpenShift、Docker EE、OpenStack 等，为虚机和容器提供多主机间通信的功能。

所以Calico可以认为是Flannel的增强版

官方地址：https://www.tigera.io/project- calico/

# 2.1.2 Calico 工作机制

# 2.1.2.1 Calico 工作机制

Calico 是三层的虚拟网络方案，而不需要像 flannel 的 cni0 网桥设备- 它把每个节点都当作虚拟路由器（vRouter），把每个节点上的 Pod 都当作是“节点路由器”后的一个终端设备并为其分配一个 IP 地址- 各节点路由器通过 BGP（Border Gateway Protocol 边界网关协议）协议学习生成路由规则从而实现不同节点上 Pod 间的互联互通

- Calico 方案其实是一个纯三层的解决方案，通过每个节点协议栈的三层（网络层）确保容器之间的连通性，即所有容器（包括同一个节点的容器间）都需要通过路由实现通信，这摆脱了 flannel host-gw 类型的所有节点必须位于同一二层网络的限制，从而极大地扩展了网络规模和网络边界。- Calico 在每一个节点利用 Linux 内核实现了一个高效的 vRouter（虚拟路由器）进行报文转发，而每个 vRouter 通过 BGP 协议负责把自身所属的节点上运行的 Pod 资源的 IP 地址信息基于节点的 agent 程序（Felix）直接由 vRouter 生成路由规则向整个 Calico 网络内传播- Calico 把 Kubernetes 集群环境中的每个节点上的 Pod 所组成的网络视为一个自治系统，各节点也就是各自治系统的边界网关，它们彼此间通过 BGP 协议交换路由信息生成路由规则- 由于不是所有网络（比如公有云）都能支持 BGP，以及 BGP 路由模型要求所有节点必须要位于同一个二层网络，Calico 还支持基于 IPIP 或者 VXLAN 的叠加网络模型- 类似于 Flannel 在 VXLAN 后端中启用 DirectRouting 时的网络模型，Calico 也支持混合使用路由和叠加网络模型，BGP 路由模型用于二层网络的高性能通信，IPIP 或 VXLAN 用于跨子网的节点间（Cross-Subnet）报文转发- Calico 不使用隧道或者 NAT 来实现转发，而是巧妙的把所有二三层流量转换成三层流量，并通过 host 上路由配置完成跨 host 转发。- 默认使用 192.168.0.0/16 的网段，并给每个节点划分子网 192.168.0.0/26，即支持 2^10 个子网，每个节点的子网支持 2^6-2=62 个 Pod

# 2.1.2.2 Calico 的 Pod 网络

# 2.1.2.2.1 Calico 的 Pod 网络工作机制

Calico 的每个 Pod 也会生成一对 veth 虚拟网卡，一半在 Pod 内，另一半在节点主机表现为 calixXXXXXXX@ifN 形式的虚拟卡

其中 11 个 X 表示函数自动生成，N 为 Pod 的网卡的编号

但和 Flannel 不同的是，Calico 并不使用一个虚拟网桥连接同一个节点主机上的多个不同 Pod

# Proxy ARP 介绍

当局域网内部主机发起跨网段的 ARP 请求时，出口路由器/网关设备将自身 mac 地址回复给这个请求这个过程称为 Proxy ARP

Proxy ARP（代理 ARP）是一种网络功能，它允许路由器代替目标主机回应 ARP 请求。

通常情况下，ARP（地址解析协议）用于将 IP 地址解析为 MAC 地址，以便在同一个物理网络上的主机之间进行通信。然而，当主机不在同一个物理网络上时，proxy ARP 可以让路由器模拟（代替）目标设备的 MAC 地址响应 ARP 请求，从而使得发送方主机认为目标主机与自己在同一个物理网络上，实现位于不同物理网络的主机之间建立通信。

# Proxy ARP 的工作原理

1. ARP 请求：当一台主机（如 Host_A）需要与同一 IP 网段内的另一台主机（如 Host_B）通信时，它首先会发送 ARP 请求报文以获取 Host_B 的 MAC 地址。如果 Host_A 和 Host_B 位于不同的物理网络，Host_B 将无法收到这个 ARP 请求。

2. 路由器回应：如果连接这两个网络的设备（如路由器或交换机）启用了 Proxy ARP 功能，它将会接收到这个 ARP 请求。该设备会查找其路由表或 ARP 表，以确认是否存在到 Host_B 的路由或 ARP 表项。

3. MAC 地址代理：如果设备能够确认到 Host_B 的路由或 ARP 表项存在，它将使用自己的 MAC 地址作为响应，发送给 Host_A。这样，Host_A 就会认为它已经找到了 Host_B 的 MAC 地址，并开始使用这个 MAC 地址进行数据转发

4. 数据包转发：主机 Host_A 会将数据包发送到路由器，路由器再将数据包转发给真正的目标主机 Host_B

Proxy ARP 的应用场景

1. 跨物理网络通信：当两台主机在同一个逻辑网络（子网）上，但不在同一个物理网络上时，在没有配置默认网关或静态路由的情况下，proxy ARP 可以用来实现跨物理网络的通信。

2. 简化网络配置：在某些情况下，使用 proxy ARP 可以简化网络配置，避免复杂的 NAT（网络地址转换）和路由配置。

# Proxy ARP 配置

Proxy ARP 是在网卡上启用的，可通过下面文件查看是否启用 Proxy ARP

```bash/proc/sys/net/ipv4/conf/oEv>/proxy ARP#值为0则未开启，此为默认值#值为1则已经开启```

节点为每个cali 接口都开启了 ARP Proxy 功能，从而让宿主机扮演网关设备，并以自己的 MAC 地址代为应答对端 Pod 中发来的 ARP 请求

同一节点上的 Pod 间通信依赖于节点主机上为每个 Pod 单独配置的路由规则，即同一个节点的 Pod 间通信也是通过三层路由实现的

Pod 内生成的路由如下

[root@myapp- 6b59f8f86- mfdn8 ]# route - n

Kernel IP routing table  Destination Gateway Genmask Flags Metric Ref Use Iface  0.0.0.0 169.254.1.1 0.0.0.0 UG 0 0 0 eth0  169.254.1.1 0.0.0.0 255.255.255.255 UH 0 0 0 eth0

节点主机上生成的路由如下

[root@node1 ~]#route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface  0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0  10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0  10.244.36.0 10.0.0.202 255.255.255.192 UG 0 0 0 tun10  10.244.53.64 10.0.0.200 255.255.255.192 UG 0 0 0 tun10  10.244.101.128 0.0.0.0 255.255.255.192 U 0 0 0 10.244.101.129 0.0.0.0 255.255.255.255 UH 0 0 0 cali2ad752e56a1 #对应Pod的veth网卡

10.244.101.130 0.0.0.0 255.255.255.255 UH 0 0 0 cali47ddc65abff

开启 ARP Proxy

[root@node1 ~]#ls /proc/sys/net/ipv4/conf/ all cali13251176854 cali3d321aef9c7 cali7f09440efda cali8e36c4d65e6 default eth0 tun10 cali0e6b75291c2 cali25e96b97a8c cali72c6e46c423 cali7f22771d9d2 calid568e889c3c docker0 lo

1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1

不同节点上的Pod间通信，则由Calico的网络模式决定

# 2.1.2.2.2范例：Calico同一个节点不同容器间通信

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/b38cbffe14aecb9710b1988bcae2db9fd863b7d660ac333cbba766479a86ae24.jpg)

# #1.创建ns

[root@ubuntu2204 \~]#ip netns add ns1 [root@ubuntu2204 \~]#ip netns add ns2 [root@ubuntu2204 \~]#ip netns ns2 ns1

# #2.创建一对veth peer

[root@ubuntu2204 \~]#ip link add veth1 type veth peer name veth1- ns1 [root@ubuntu2204 \~]#ip link add veth2 type veth peer name veth2- ns2

[root@ubuntu2204 \~]#ipa

3: veth1- ns1@veth1: <BROADCAST,MULTICAST,M- DOWN> mtu 1500 qdisc noop state DOWN group default qlen 1000 link/ether 96:ab:a3:21:3d:c3 brd ff:ff:ff:ff:ff:ff

4: veth1@veth1- ns1: <BROADCAST,MULTICAST,M- DOWN> mtu 1500 qdisc noop state DOWN group default qlen 1000

link/ether a6:55:12:8c:96:c6 brd ff:ff:ff:ff:ff:ff

5: veth2- ns2@veth2: <BROADCAST,MULTICAST,M- DOWN> mtu 1500 qdisc noop state DOWN group default qlen 1000

link/ether c6:a3:df:ea:2f:e3 brd ff:ff:ff:ff:ff:ff

6: veth2@veth2- ns2: <BROADCAST,MULTICAST,M- DOWN> mtu 1500 qdisc noop state DOWN group default qlen 1000

link/ether 9e:fd:86:30:9c:10 brd ff:ff:ff:ff:ff:ff

# #3.把veth1-ns1/veth2-ns2分别放在ns1/ns2名称空间中

[root@ubuntu2204 \~]#ip link set veth1- ns1 netns ns1 [root@ubuntu2204 \~]#ip link set veth2- ns2 netns ns2

# [root@ubuntu2204 ~]#ip netns

ns2 (id: 1)

ns1 (id: 0)

[root@ubuntu2204 ~]#ip a

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000

link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00 inet 127.0.0.1/8 scope host lo valid_lft forever preferred_lft forever inet6 :1/128 scope host valid_lft forever preferred_lft forever

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_code1 state UP group default qlen 1000

link/ether 00:0c:29.8d:9a2e brd ff:ff:ff:ff:ff:ff:ff:altname enp2s1 altname ens33 inet 10.0.0.100/24 brd 10.0.0.255 scope global eth0 valid_lft forever preferred_lft forever inet6 fe80::20c:29ff:fe8d:9a2e/64 scope link valid_lft forever preferred_lft forever

4: veth1@if3: <BROADCAST,MULTICAST> mtu 1500 qdisc noop state DOWN group default qlen 1000

link/ether a6:55:12:8c:96:c6 brd ff:ff:ff:ff:ff:ff:link- netns ns1

6: veth2@if5: <BROADCAST,MULTICAST> mtu 1500 qdisc noop state DOWN group default qlen 1000

link/ether 9e:fd:86:30:9c:10 brd ff:ff:ff:ff:ff:ff:link- netns ns2

4. 设置ns1和ns2的网卡ip地址

配置ns1名称空间的网卡IP

[root@ubuntu2204 ~]#ip netns exec ns1 ip addr add 192.168.100.101/32 dev veth1- ns1 [root@ubuntu2204 ~]#ip netns exec ns1 ip link set veth1- ns1 up

[root@ubuntu2204 ~]#ip netns exec ns1 ip addr

1: lo: <LOOPBACK> mtu 65536 qdisc noop state DOWN group default qlen 1000 link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00

3: veth1- ns1@if4: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default qlen 1000

link/ether 96:ab:aa:21:3d:c3 brd ff:ff:ff:ff:ff:ff:link- netnsid 0 inet 192.168.100.101/32 scope global veth1- ns1 valid_lft forever preferred_lft forever inet6 fe80::94ab:aa3ff:fe21:3dc3/64 scope link valid_lft forever preferred_lft forever

默认无法访问自身的IP

[root@ubuntu2204 ~]#ip netns exec ns1 ping - c1 192.168.100.101 PING 192.168.100.101 (192.168.100.101) 56(84) bytes of data.

192.168.100.101 ping statistics - - - 1 packets transmitted, 0 received, 100% packet loss, time Oms

启用lo网卡即可

[root@ubuntu2204 ~]#ip netns exec ns1 ip link set lo up [root@ubuntu2204 ~]#ip netns exec ns1 ping - c1 192.168.100.101 PING 192.168.100.101 (192.168.100.101) 56(84) bytes of data. 64 bytes from 192.168.100.101: icmp_seq=1 ttl=64 time=0.018 ms

192.168.100.101 ping statistics - - -

1 packets transmitted, 1 received,  $0\%$  packet loss, time 0ms  rtt min/avg/max/mdev = 0.018/0.018/0.018/0.000 ms

# #配置ns2名称空间的网卡IP

[root@ubuntu2204 ~]#ip netns exec ns2 ip addr add 192.168.100.102/32 dev veth2- ns2  [root@ubuntu2204 ~]#ip netns exec ns2 ip link set veth2- ns2 up  [root@ubuntu2204 ~]#ip netns exec ns2 ip addr  1: lo: <LOOPBACK> mtu 655336 qdisc noop state DOWN group default qlen 1000  link/loopback 00:00:00:00:00 brd 00:00:00:00:00:00  5: veth2- ns2@if6: <BR0ADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default qlen 1000  link/ether co.a3. ifr.ea:2fe3 brd ff:ff:ff:ff:ff:link- netnsld 0  inet 192.168.100.102/32 scope global veth2- ns2  valid_lft forever preferred_lft forever  inet6 fe80::c4a3:dfff:feea:2fe3/64 scope link  valid_lft forever preferred_lft forever

# #ns1和ns2无法通信

[root@ubuntu2204 ~]#ip netns exec ns1 ping - c1 192.168.100.102  ping: connect: Network is unreachable

# #5.设置route

[root@ubuntu2204 ~]#ip netns exec ns1 ip route add 169.254.1.1 dev veth1- ns1 scope link

[root@ubuntu2204 ~]#ip netns exec ns1 ip route add default via 169.254.1.1 dev veth1- ns1

[root@ubuntu2204 ~]#ip netns exec ns1 route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 169.254.1.1 0.0.0.0 UG 0 0 0 veth1- ns1 169.254.1.1 0.0.0.0 255.255.255.255 UH 0 0 0 veth1- ns1

[root@ubuntu2204 ~]#ip netns exec ns2 ip route add 169.254.1.1 dev veth2- ns2 scope link

[root@ubuntu2204 ~]#ip netns exec ns2 ip route add default via 169.254.1.1 dev veth2- ns2

[root@ubuntu2204 ~]#ip netns exec ns2 route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 169.254.1.1 0.0.0.0 UG 0 0 0 veth2- ns2

169.254.1.1 0.0.0.0 255.255.255.255 UH 0 0 0 veth2- ns2

# #6.把veth设备up

[root@ubuntu2204 ~]#ip link set veth1 up  [root@ubuntu2204 ~]#ip link set veth2 up

# #7.开启路由

[root@ubuntu2204 ~]#sysctl - w net.ipv4. ip_forward=1

# #8.在hosts上设置回复路由

[root@ubuntu2204 ~]#ip route add 192.168.100.101/32 dev veth1

[root@ubuntu2204 ~]#ip route add 192.168.100.102/32 dev veth2 [root@ubuntu2204 ~]#route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 10.0.0.2 0.0.0.0 U 0 0 0 eth0 10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0 192.168.100.101 0.0.0.0 255.255.255.255 UH 0 0 0 veth1 192.168.100.102 0.0.0.0 255.255.255.255 UH 0 0 0 veth2

# 无法访问外网

[root@ubuntu2204 ~]#ip netns exec ns1 ping - c1 192.168.100.102 PING 192.168.100.102 (192.168.100.102) 56(84) bytes of data. FROM 192.168.100.101 icmp_seq=1 destination host unreachable

192.168.100.102 ping statistics - - - 1 packets transmitted, 0 received, +1 errors, 100% packet loss, time Oms

9. 为veth网卡开启proxy arp

[root@ubuntu2204 ~]#cat /proc/sys/net/ipv4/conf/veth1/proxy_arp 0 [root@ubuntu2204 ~]#cat /proc/sys/net/ipv4/conf/veth2/proxy_arp 0 [root@ubuntu2204 ~]#echo 1 > /proc/sys/net/ipv4/conf/veth1/proxy_arp [root@ubuntu2204 ~]#echo 1 > /proc/sys/net/ipv4/conf/veth2/proxy_arp

# 10.测试相互访问

[root@ubuntu2204 ~]#ip netns exec ns1 ping - c1 192.168.100.102 PING 192.168.100.102 (192.168.100.102) 56(84) bytes of data. 64 bytes from 192.168.100.102: icmp_seq=1 ttl=63 time=0.051 ms

192.168.100.102 ping statistics - - - 1 packets transmitted, 1 received, 0% packet loss, time Oms rtt min/avg/max/mdev = 0.051/0.051/0.051/0.000 ms

# 查看MAC，看到网关对应veth网卡的MAC

[root@ubuntu2204 ~]#ip netns exec ns1 arp - n Address HWtype HWaddress Flags Mask Iface 169.254.1.1 ether a6:55:12:8c:96:c6 C veth1- ns1 10.0.0.100 ether a6:55:12:8c:96:c6 C veth1- ns1

[root@ubuntu2204 ~]#ip netns exec ns2 ping - c1 192.168.100.101 PING 192.168.100.101 (192.168.100.101) 56(84) bytes of data. 64 bytes from 192.168.100.101: icmp_seq=1 ttl=63 time=0.036 ms

192.168.100.101 ping statistics - - - 1 packets transmitted, 1 received, 0% packet loss, time Oms rtt min/avg/max/mdev = 0.036/0.036/0.036/0.000 ms

# 查看MAC，看到网关对应veth网卡的MAC

[root@ubuntu2204 ~]#ip netns exec ns2 arp - n Address HWtype HWaddress Flags Mask Iface 10.0.0.100 ether 9e:fd:86:30:9c:10 C veth2- ns2

# 2.1.2.2.3范例：Calico跨不同节点的不同容器间通信

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/5d6476a3ee9b639d45de5596b9b8e397e0d43c559889ab4ec6bac99c652bdf14.jpg)

# 节点1上执行

节点1上执行[root@ubuntu2204 ~]#ip netns add ns1[root@ubuntu2204 ~]#ip link add veth0 type veth peer name eth0- ns1[root@ubuntu2204 ~]#ip link set veth0 up

[root@ubuntu2204 ~]#ip link set eth0- ns1 netns ns1[root@ubuntu2204 ~]#ip netns exec ns1 ip addr add 192.168.200.101/32 dev eth0- ns1[root@ubuntu2204 ~]#ip netns exec ns1 ip link set eth0- ns1 up

# 添加ns1名称空间的路由

[root@ubuntu2204 ~]#ip netns exec ns1 ip route add 169.254.1.1 dev eth0- ns1 scope link[root@ubuntu2204 ~]#ip netns exec ns1 ip route add default via 169.254.1.1 dev eth0- ns1

[root@ubuntu2204 ~]#ip netns exec ns1 route - nKernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface0.0.0.0 169.254.1.1 0.0.0.0 UG 0 0 0 eth0- ns1169.254.1.1 0.0.0.0 255.255.255.255 UH 0 0 0 eth0- ns1

# 添加宿主机的路由

添加宿主机的路由[root@ubuntu2204 ~]#ip route add 192.168.200.101 dev veth0 scope link[root@ubuntu2204 ~]#ip route add 192.168.200.102 via 10.0.0.102 dev eth0[root@ubuntu2204 ~]#route - n

Kernel IP routing table 192.168.200.101 0.0.0.0 255.255.255.255 UH 0 0 0 veth0 192.168.200.102 10.0.0.102 255.255.255.255 UGH 0 0 0 eth0

开启路由

[root@ubuntu2204 ~]#sysctl - w net.ipv4. ip_forward=1

开启路由

[root@ubuntu2204 ~]#echo 1 > /proc/sys/net/ipv4/conf/veth0/proxy_arp

节点2上执行

[root@ubuntu2204 ~]#ip netns add ns2  [root@ubuntu2204 ~]#ip link add veth0 type veth peer name eth0- ns2  [root@ubuntu2204 ~]#ip link set veth0 up

[root@ubuntu2204 ~]#ip link set eth0- ns2 netns ns2  [root@ubuntu2204 ~]#ip netns exec ns2 ip addr add 192.168.200.102/32 dev eth0- ns2  [root@ubuntu2204 ~]#ip netns exec ns2 ip link set eth0- ns2 up

添加ns2名称空间的路由

[root@ubuntu2204 ~]#ip netns exec ns2 ip route add 169.254.1.1 dev eth0- ns2 scope link  [root@ubuntu2204 ~]#ip netns exec ns2 ip route add default via 169.254.1.1 dev eth0- ns2

添加宿主机的路由

[root@ubuntu2204 ~]#ip route add 192.168.200.102 dev veth0 scope link  [root@ubuntu2204 ~]#ip route add 192.168.200.101 via 10.0.0.101 dev eth0

开启路由

[root@ubuntu2204 ~]#sysctl - w net.ipv4. ip_forward=1

开启proxy ARP

[root@ubuntu2204 ~]#echo 1 > /proc/sys/net/ipv4/conf/veth0/proxy_arp

测试访问

在第1个节点上执行

[root@ubuntu2204 ~]#ip netns exec ns1 ping - c1 192.168.200.102  PING 192.168.200.102 (192.168.200.102) 56(84) bytes of data.  64 bytes from 192.168.200.102: icmp_seq=1 ttl=62 time=1.44 ms

- -- 192.168.200.102 ping statistics

1 packets transmitted, 1 received, 0% packet loss, time 0ms  rtt min/avg/max/mdev = 1.442/1.442/1.442/0.000 ms

查看网关的MAC为veth的网卡MAC

[root@ubuntu2204 ~]#ip netns exec ns1 arp - n

Address Hwtype HWaddress Flags Mask Iface 10.0.0.101 ether 1a:1d:32:3f:e9:06 C eth0- ns1 169.254.1.1 ether 1a:1d:32:3f:e9:06 C eth0- ns1

在第2个节点上执行

[root@ubuntu2204 ~]#ip netns exec ns2 ping - c1 192.168.200.101  PING 192.168.200.101 (192.168.200.101) 56(84) bytes of data.  64 bytes from 192.168.200.101: icmp_seq=1 ttl=62 time=0.608 ms

- -192.168.200.101 ping statistics 
---

1 packets transmitted, 1 received,  $0\%$  packet loss, time Oms rtt min/avg/max/mdev  $=$  0.608/0.608/0.608/0.000 ms

[root@ubuntu2204 \~]#ip netns exec ns2 arp - n

Address Hwtype HWaddress Flags Mask Iface 169.254.1.1 ether 1a:1d:32:3f:e9:06 C eth0- ns2 10.0.0.102 ether 1a:1d:32:3f:e9:06 C eth0- ns2

# 2.1.3Calico网络模型

Calico为了实现更广层次的虚拟网络的应用场景，它支持多种网络模型来满足需求。

- underlay network: BGP- overlay network: IPIP(默认模式)、VXLAN

# 2.1.3.1BGPNativeRouting

BGP(Border Gateway Protocol- 边界网关协议），这是一种三层虚拟网络解决方案，也是Calico广为人知的一种网络模型。

BGP是互联网上一个核心的去中心化自治路由协议

- iBGP:Interior Border Gateway Protocol，负责在同一AS内的BGP路由器间传播路由，它通过递归方式进行路径选择- eBGP:Exterior Border Gateway Protocol，用于在不同AS间传播BGP路由，它基于hop-by-hop机制进行路径选择

BGP是一种纯粹的三层路由解决方案，并没有叠加，而是一种承载物理网络。

BGP要求所有的节点处于一个二层网络中，同时所有的主机也需要支持BGP协议。

但是如果要构建大规模的网络集群，比如是一个B类网站，放了很多的节点主机，此时由于网络报文无法被隔离，可能会导致广播风暴，对网络产生很大的影响。

所以，正常情况下，一般不推荐在一个大的网络内创建大量的节点主机。而且一旦节点跨子网，BGP就不支持了。

BGP有点类似于flannel的host- gw模型，但是与host- gw的区别在于，host- gw是由flanneld通过kube- apiserver来操作Fetct内部的各种网络配置，进而实时更新到当前节点上的路由表中。

BGP方案中，各节点上的vrouter通过BGP协议学习生成路由表，基于路径、网络策略或规则集来动态生成路由规则

CalicoBGP工作机制

Calico BGP 工作机制- 每个工作节点都是一个网络主机边缘节点。由一个虚拟路由vrouter和一系列其他节点组成的一个自治系统AS (Autonomous system)。每个工作节点都有自己的独立的网段，负责给该节点的Pod分配置IP- 各个节点上的vrouter基于BGP协议，通过互相学习的方式，动态生成路由规则，实现各节点之间的网络互通性BGP不使用传统的内部网关协议(IGP)的指标，而使用基于路径、网络策略或规则集来决定路由规则它属于矢量路由协议。

简单来说，BGP就是通过动态生成的路由表的方式，以类似flannel的host:gw方式，来完成pod之间报文的直接路由，通信效率较高。

它要求所有的节点处于一个二层网络中，同时所有的主机也需要支持BGP协议。

Calico Node能够基于BGP协议将物理路由器作为BGP Peer，每个路由器都存在一到多个对等端BGP Peer

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/a4ec2b90d7a6fb33e6f52725c0f8fd775200ea37d6d354b2f26a3885b954afda.jpg)  
BGP Pod通信过程示例

A POD eth0 10.121.74.181 - - - - - - - - A POD 网关 ARP - - - - - - - - A宿主机 cali3e7996a24a7网卡 - - - - - - - - 默认路由0.0.0.0 - - - - - - - A eth0 10.120.129.9 - - - - - - - - 达到 B NODE eth0 10.120.128.7 - - - - - - - - 通过路由表 ip为10.121.46.65到达B宿主机 cali558def1712b网卡通过iptables转发 - - - - - - - - B POD eth0 10.121.46.65

# 有bug

A POD eth0 10.121.74.181 - - - - - - - - A POD 网关 ARP - - - - - - - - A宿主机 cali3e7996a24a7网卡 - - - - - - - - 默认路由0.0.0.0 - - - - - - - A eth0 10.120. 129.9 - - - - - - - A node 网关 10.120.129.254 - - - - - - - - B Node 网关 10.120.128.254 - - - - - - - - 达到 B NODE eth0 10.120.128.7 - - - - - - - - 通过路由表 ip为10.121.46.65到达B宿主机 cali558def1712b网卡通过iptables转发 - - - - - - - - B POD eth0 10.121.46.65不支持BGP的场景：

# 不支持BGP的场景：

- 节点是跨网段，会因为目标地址找不到，而无法发送数据包- 阻止BGP报文的网络环境，比如：很多公有云不支持BGP- 对入站数据包强制执行源地址和目标地址的校验

# 2.1.3.2 IPIP

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/5c594ab3166f91f95509ba92d411f88fabd539e1f1e79c0291c84571e1a4a28f.jpg)

IPIP是Calico的默认模式，生产使用较多

通过把一个IP数据包又套在另一个IP包里，即把IP层封装到IP层的一个tunnel，实现两层IP头

IPIP模式仍然还需要使用BGP协议，实现生成当前节点到达其它节点的Pod所在子网的路由信息

在每个节点的路由表中每条目标地址Pod所在子网的路由记录的网关指向Pod所在节点的IP，接口为隧道接口tunl0设备

此模式因为报文结构更简洁，MTU默认为1480（通过configmap/calico- config配置），因此相对VXLAN模式性能更好，

当CALICO_IPV4POOL_IPIP配置为Cross- Subnet，表示同一网络使用BGP，跨网段时才启用IPIP

# Pod通信流程示例

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/10028fb1da73c7101ba56118e9745fce9c6d4f6c4c070dd654daab246dd56546.jpg)

A POD eth0 10.7.75.132 - - - - - - > A POD 网关 - - - - - - > A 宿主机 cal176174826315网卡 - - - - - - > A tun10 10.7.75.128 其装 - - - - - - > Aeth0 10.120.181.20 - - - - - - > 通过网关 10.120.181.254 - - - - - - > 下一跳 B NODE eth0 10.120.179.8 解封装 - - - - - - > B tun10 10.5.34.192 - - - - - - > 路由 B 宿主机 calie83684F4735 - - - - - - > B POD eth0 10.5.34.192

# 不支持IPIP的场景：

- 禁止使用IPIP协议的网络，比如：Azure- 阻止BGP报文的网络环境

# 2.1.3.3 IPIP with BGP

IPIP 还实现了这样一种类似于 flannel的 VXLAN with directrouting的机制，支持一种 IPIP With BGP的方式。

即当CALICO_IPV4POOL_VXLAN配置为Cross- Subnet时

如果是同一网段，就使用BGP- 如果不是同一网段，就用IPIP机制

# 2.1.3.4 VXLAN

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/3a6b3d9b208da3a65417a173d5b45d14e0a8a2a5daeb87e014a7fe87a19e86ef.jpg)

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/b369310a71e3f8520732350dfadca73de95182de7ac02ca23abca5be57be7ae0.jpg)

VXLAN 把数据包封装在UDP(4789端口)中，并使用物理网络的IP/MAC作为outer- header进行封装标识然后在物理IP网上传输，根据标识到达目的地后，由隧道终点对报文进行解封，最终将数据发送给目标。

VXLAN不依赖于BGP协议，功能强大

Calico的VXLAN模式默认MTU设置为1450字节（通过configmap/calico- config配置），这是因为Calico在计算VXLAN封装的开销时，预留了更多的字节，以确保封装后的数据包不会超过底层网络的标准MTU。封装的报文头较大和复杂，因此性能较差，但功能更多

具体计算如下：

- 标准以太网的MTU为1500字节。- VXLAN封装通常会增加50字节的开销。- Calico默认预留90字节的封装开销，以适应不同的网络环境和可能的附加协议开销。

Calico的VXLAN模式不需要BGP生成路由表，而使用VXLAN隧道自身来维护的路由表实现。

VXLAN（Virtual Extensible LAN）是一种覆盖网络技术，它在现有的三层网络之上构建了一个虚拟的二层网络，使得连接在这个VXLAN网络上的主机（无论是虚拟机还是容器）可以像在同一个局域网内一样自由通信。在VXLAN模式下，Calico通过在源节点上的VTEP（VXLAN Tunnel Endpoint）将Pod发出的以太网帧封装在VXLAN报文中，并通过三层网络传输到目标节点的VTEP，目标节点的VTEP解封报文并将原始的以太网帧转发给目标Pod。

VXLAN模式不依赖于BGP协议来传播路由信息，因为它使用VXLAN隧道来封装和传输数据包，而不是通过BGP协议来控制路由的传播。因此，即使在没有BGP的环境中，VXLAN模式也可以工作。VXLAN模式适用于需要跨越不同三层网络（如不同的子网或VLAN）实现Pod之间通信的场景。在VXLAN模式下，Calico会为每个节点创建一个vxlan.calico设备，用于VXLAN的封装和解封装工作，而不需要BGP的参与

# 每个节点的路由记录了到达其它节点的Pod所在网段的路由信息

网关指向Pod网络所在节点的vxlan.calico的接口地址，接口为当前本机的vxlan.calico接口

# 2.1.3.5 VXLAN With BGP

VXLAN 还实现了这样一种类似于 flannel 的 VXLAN with directrouting 的机制，支持一种 Vxlan With BGP 的方式。

即当CALICO_IPV4POOL_VXLAN配置为Cross- Subnet时

如果是同一网段，就使用BGP- 如果不是同一网段，就用VXLAN机制

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/b2b3cc063fb6d05632bb709b14b607ba3689d80b6fd1a306c163e6fc7e900174.jpg)

# 2.1.4 Calico 软件架构

参考资料：

https://docs.projectcalico.org/reference/architecture/overview

由于Calico是一种纯三层的实现，因此可以避免与二层方案相关的数据包封装的操作，中间没有任何的NAT，没有任何的overlay，所以它的转发效率可能是所有方案中最高的，因为它的包直接走原生TCP/IP的协议栈，它的隔离也因为这个栈而变得容易实现。因为TCP/IP的协议栈提供了一整套的防火墙的规则，所以它可以通过IPTABLES的规则达到比较复杂的隔离逻辑。

# 2.1.4.1 Calico 组件

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/d5827226e567ed95b5b28d0990aacf6337ed83f1ac672647f5b460e0151bd640.jpg)

<table><tr><td>组件</td><td>解析</td></tr><tr><td>Felix</td><td>Calico Agent，以Pod形式运行于每个节点，主要负责维护虚拟接口设备、路由信息、ACL并向etcd宣告状态等
由kube-system名称空间的daemonset的calico-node提供,表现为节点的一个独立进程</td></tr><tr><td>BIRD</td><td>负责分发路由信息BGP客户端，以Pod形式在每个节点，负责把Felix写入Kermel路由信息分发到整个Calico网络
由kube-system名称空间的daemonset的calico-node提供,表现为节点的一个独立进程</td></tr><tr><td>etcd</td><td>持久存储Calico数据(节点,网段,IP等)的存储系统，可以结合kube-apiserver来工作</td></tr><tr><td>Route Reflector</td><td>BGP路由反射器(中央路由器,类似于HUB)，可选组件，可以集中式的动态生成所有主机的路由表，用于较大规模的网络场景</td></tr><tr><td>Calico编排系统插件 Orchestrator Plugin</td><td>支持不同的编排系统（例如Kubernetes、OpenStack等）的插件，用于将Calico整合进行不同的系统中,例如Kubernetes的提供了CNI,实现更广范围的虚拟网络解决方案。</td></tr></table>

# Calico网络模型主要工作组件

- Felix

运行在每一台host的Calico的agent进程，主要负责网络接口管理和监听、路由规划、ARP管理、ACL管理和同步、状态报告等。

Felix会监听Etcd中心的存储，从它获取事件，比如说用户在这台机器上加了一个IP，或者是创建了一个容器等，用户创建Pod后，Felix负责将其网卡、IP、MAC都设置好，然后在内核的路由表里面写一条，注明这个IP应该到这张网卡。

用户如果制定了隔离策略，Felix同样会将该策略创建到ACL中，以实现隔离。

注意：这里的网络策略规则是通过内核的iptables方式实现的

- Etcd

分布式键值存储，主要负责网络元数据一致性，确保Calico网络状态的准确性，可以与Kubernetes共用。

ETCD相当于所有calico节点的通信总线，也是calico对接到到其他编排系统的通信总线。

少于50节点可以结合kube- apiserver来实现数据的存储

多于50节点可以使用独立的ETCD集群来进行处理，但无法使用RBAC实现安全控制。

- BIRD

Calico为每一台host部署一个BGP Client，使用BIRD实现

BIRD是一个单独的持续发展的项目，实现了众多动态路由协议比如：BGP、OSPF、RIP等。

BIRD监听Host上由Felix注入的路由信息，然后通过BGP协议广播告诉剩余Host节点，从而实现网络互通。

BIRD是一个标准的路由程序，它会从内核里面获取哪一些IP的路由发生了变化，然后通过标准BGP的路由协议扩散到整个其他的宿主机上，让外界都知道这个IP网段在这主机，你们路由的时候得到这里来。

- Route Reflector

在大型网络规模中，如果仅仅使用BGP Client形成mesh全网互联的方案就会导致规模限制，因为所有节点之间两两互连，需要N^2个连接，为了解决这个规模问题，可以采用BGP的Route Reflector的方法，使所有BGP Client仅与特定RR节点互连并做路由同步，从而大大减少连接数。

注意：以下三点不是calico的组成部分

- 每个灰色的背景就是一个边缘自治worker节点，里面有很多工作中的pod对象- 每个pod对象不像flannel通过一对虚拟网卡连接到cni0，而是直接连接到内核中- 借助于内核中的iptables和routers路由表来完成转发的功能

# 2.1.4.2 Calico 软件

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/076da2fa7f02cdf0644b94b6727547e22761bef8602e8516ba746b24714f7359.jpg)

- calico-kube-controller

控制器，实现网络策略功能，支持calico相关的CRD资源

- calico Datastore

存储的Calico配置、路由、策略及其它信息，它们通常表现为Calico CRD资源对象

支持的CRD包括BGPConfiguration、BGPFilter、BGPPeer、BlockAffinity、CalicoNodeStatus、ClusterInformation、FelixConfiguration、GlobalNetworkPolicy、GlobalNetworkSet、HostEndpoint、IPAMBlock、IPAMConfig、IPAMHandle、IPPool、IPReservation、NetworkPolicy、NetworkSet和KubeControllersConfiguration等

几个常用的CRD功能说明

- BGPConfiguration：全局BGP配置，用于设定AS(自治系统)编号、node mesh，以及用于通告CusterIP的设置- FelixConfiguration：Felix相关的低级别配置，包括iptables、MTU和路由协议等- GlobalNetworkPolicy：全局网络策略，生效于整个集群级别- GlobalNetworkSet：全局网络集，是指可由GlobalNetworkPolicy引用的外部网络IP列表或CIDR列表- IPPool：Pod所在IP地址池及相关选项，包括要使用的路由协议(IPIP、VXLAN或Native)；一个集群支持使用多个Pool

虽然支持将数据保存在集群外独立的ETCD来减少APIServer压力，但因为其复杂和安全性（不支持RBAC）并不建议此方式

而Calico一般将数据保存在Kube- apiserver中，但是可以利用typha减轻访问压力

calico- node:

以daemonset类型的pod在每个主机上运行，提供Felix和BIRD组件，同时维护iptables的相关规则

Typha:可选

各calico- node实例同CalicoDatastore通信的中间层，由其负责将CalicoDatastore中生成的更改信息分发给各calico- node，以减轻较大规模集群中的CalicoDatastore的负载，具有缓存功能，且能够通过删除重复事件，降低系统负载

如果没有Calico- Typha，每个calico- node都必须向API服务器注册自己的监视，并且随着节点数量的增加，API服务器上的负载会成倍增加。有了Typha，所有监视事件都会由Typha实现，并且只从API服务器读取一次。

因此，如果集群规模超过50个节点以上，建议可以使用Typha

Typha以deployment方式运行，replicas数量根据节点数决定，一般每200个节点对应一个Typha，但Typha副本最多不超过20个

# 2.2Calico部署和管理

# 2.2.1部署流程说明

参考资料：

https://docs.projectcalico.org/getting- started/kubernetes/quickstart

注意：如果要使用calico，必须要提前将flannel删除，然后再安装calico。因为对于k8s的cni同时只支持一个网络插件。

对于calico在k8s集群上的部署上面提到的四个组件，这里会涉及到两个组件

<table><tr><td>组件名</td><td>组件作用</td></tr><tr><td>calico-node</td><td>需要部署到所有集群节点上的代理守护进程，提供封装好的Felix和BIRD</td></tr><tr><td>calico-kube-controller</td><td>专用于k8s上对calico所有节点管理的中央控制器。负责calico与k8s集群的协同及calico核心功能实现。</td></tr></table>

部署思路

calico的官方对于k8s集群上的环境部署，提供了三种方式：

小于50节点大于50节点专属的etcd节点

部署方法

Calico operatorCalico由operator安装，该operator管理Calico集群的安装、升级和一般生命周期。operator作为部署直接安装在集群上，并通过一个或多个自定义Kubernetes API资源进行配置。

Calico manifests

Calico也可以使用原始清单manifests作为操作器的替代方案进行安装。

manifests包含在Kubernetes集群中每个节点上安装Calico所需的资源。

不建议使用manifests清单，因为它们无法像操作器那样自动管理Calico的生命周期。

但是，manifests清单对于需要对底层Kubernetes资源进行高度特定修改的集群可能很有用参考资料：

https://docs.projectcalico.org/getting- started/kubernetes/self- managed- onprem/onpremises https://projectcalico.docs.tigera.io/getting- started/kubernetes/self- managed- onprem/onpremises

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/b58edb8ca922478b4014733cd9dd19bce7ac3797cc33eee1aaf86afab923a6bb.jpg)

这里采用第一种，小于50节点的场景。

# 部署步骤

清理无关的网络其他插件

获取资源配置文件

从calico官网获取相关的配置信息

定制CIDR配置

定制calico自身对于pod网段的配置信息

定制pod的manifest文件分配网络配置

默认的k8s集群在启动的时候，会有一个cidr的配置，有可能与calico进行冲突，需要进行修改

应用资源配置文件

注意事项：

对于calico来说，它自己会生成自己的路由表，如果路由表中存在响应的记录，默认情况下会直接使用，而不是覆盖掉当前主机的路由表

所以如果我们在部署calico之前，曾经使用过flannel，尤其是flannel的host- gw模式的话，一定要注意，在使用calico之前，将之前所有的路由表信息清空，否则无法看到calico的runn的封装效果

# 2.2.2部署Calico案例：使用默认配置IPIP模式

# 2.2.2.1清理之前的网络插件（可选）

如果之前部署过其它网络插件，需要清理干净，Kubernetes不允许同时多个网络插件存在

清理之前的flanne1插件[root@masterl \~]#kubectl delete - f kube- flanne1. yml

确认flanne1的相关Pod被删除[root@masterl \~]#kubectl get pod - n kube- system | grep flanne1

先清除旧网卡，然后重启一下主机，直接清空所有的路由表信息[root@masterl \~]#reboot

# 重启后，查看网络效果

[root@masterl \~]#ip a

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000

link/loopback 00:00:00:00:00 brd 00:00:00:00:00 inet 127.0.0.1/8 scope host lo valid_lft forever preferred_lft forever inet6 ::1/128 scope host valid_lft forever preferred_lft forever

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP

group default qlen 1000

link/ether 00:0c:29:56:15:a2 brd ff:ff:ff:ff:ff inet 10.0.0.101/24 brd 10.0.0.255 scope global eth0 valid_lft forever preferred_lft forever inet6 fe80::20c:29ff:fe56:15a2/64 scope link valid_lft forever preferred_lft forever

3: docker0: <NO- CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN group default

link/ether 02:42:13:00:f2:cd brd ff:ff:ff:ff:ff inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0 valid_lft forever preferred_lft forever

# 2.2.2.2下载和使用calico清单文件

查看calico版本的k8s版本的兼容性

https://projectcalico.docs.tigera.io/getting- started/kubernetes/requirements

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/e76f77c2cb96f0fc7e3b45e3591ebd59537666acf0fc242dfd58fb657f399bc2.jpg)

# 查看安装方法

https://docs.tigera.io/calico/latest/getting- started/kubernetes/self- managed- onprem/onpremises

# Install Calico

Operator Manifest

Based on your datastore and number of nodes, select a link below to install Calico.

Note: The option, Kubernetes API datastore, more than 50 nodes provides scaling using Typha daemon. Typha is not included for etcd because etcd already handles many clients so using Typha is redundant and not recommended.

- Install Calico with Kubernetes API datastore, 50 nodes or less- Install Calico with Kubernetes API datastore, more than 50 nodes- Install Calico with etcd datastore

# Install Calico with Kubernetes API datastore, 50 nodes or less

1. Download the Calico networking manifest for the Kubernetes API datastore.

\$ curl https://raw.github.com/content.com/projectcalico/calico/v3.24.1/manifests/calico.yaml - 0

2. If you are using pod CIDR 192.168.0.0/16, skip to the next step. If you are using a different pod CIDR with kubeadm, no changes are required - Calico will automatically detect the CIDR based on the running configuration. For other platforms, make sure you uncomment the CALICO_IPV4POOL_CIDR variable in the manifest and set it to the same value as your chosen pod CIDR.

3. Customize the manifest as necessary. 
4. Apply the manifest using the following command.

\$ kubectl apply - f calico.yaml

The geeky details of what you get:

<table><tr><td>Policy</td><td>IPAM</td><td>CNI</td><td>Overlay</td><td>Routing</td><td>Datastore</td></tr><tr><td>Calico</td><td>Calico</td><td>Calico</td><td>IPIP</td><td>BGP</td><td>Kubernetes</td></tr></table>

Install Calico with Kubernetes API datastore, more than 50 nodes

范例: 选择少于50个节点以内的Manifest 清单部署方式使用官方默认的配置文件

# 下载最新版本的yaml文件

[root@master1 ~]#curl https://raw.githubusercontent.com/projectcalico/calico/master/manifests/calico.yaml - 0 [root@master1 ~]#wget https://docs.projectcalico.org/manifests/calico.yaml

# 指定下载版本

CALICO_VERSION=v3.29.1 #CALICO_VERSION=v3.28.0 #CALICO_VERSION=v3.27.3 #CALICO_VERSION=v3.26.1 #CALICO_VERSION=v3.24.1

[root@master1 ~]#curl

https://raw.githubusercontent.com/projectcalico/calico/\${CALICO_VERSION}=7/manifests/calico.yaml - 0

可以直接使用原文件，如果修改可以先备份原文件（可选）

[root@master1 ~]#cp calico.yaml calico.yaml.bak

可以直接使用原来的flannel的网络配置，测试环境也可以做下面配置修改CIDR，（可选）

[root@master1 ~]# vim calico.yaml

官网推荐的修改内容（基于kubeadm安装可不做修改，其它安装方式，需要修改，可选）

1)指定Pod的网段，如果使用当前默认配置，即保留当前注释，Pod的IP将使用安装k8s时的--pod-network-cidr选项指定IP网段（可选）

4223 - name: CALICO_IPV4POOL_CIDR #删除注释可以覆盖安装k8s时的- - pod- network- cidr选项，使用指定pod网段4224 value: "192.168.0.0/16" #默认值为value: "192.168.0.0/16"

2)开放默认注释的CALICO_IPV4POOL_CIDR变量，此处改为24，生产环境可不用修改，当前为测试环境建议修改

默认没有下面内容，手动加下面两行，调大单个节点上的Pod所在网段，默认使用/26，即255.255.255.192的子网掩码，每个节点只有62个Pod的地址

4225 - name: CALICO_IPV4POOL_BLOCK_SIZE4226 value: "24"

3)是否启用IPIP或VXLAN，注意，两者互斥，不能同时启用，默认启用IPIP（可选）

value可以支持Always（启用），Never（禁用），Cross- Subnet（表示同一网络使用BGP，跨网段时才启用IPIP或VXLAN）

当IPIP和VXLAN都为Never时，表示使用BGP，要求所有节点不能跨网段，才能使用此方式

Enable IPIP

name: CALICO_IPV4POOL_IPIP

value: "Always" #此项修改为"Cross- Subnet"，表示同一网络使用BGP，跨网段时才启用IPIP，但VXLAN必须为Never

Enable or Disable VXLAN on the default IP pool.

name: CALICO_IPV4POOL_VXLAN

value: "Never" #此项修改为"Cross- Subnet"，表示同一网络使用BGP，跨网段时才启用VXLAN，但IPIP必须为Never

4)BGP使用地址自动探测，默认值不用修改，如何有多块网卡，需要指定哪个网卡的IP用于BGP通信（可选）

# containers

env

Auto- detect the BGP IP address. - name: IP value: "autodetect"

# 2.2.2.3定制Docker镜像地址

从2024年6月开始，dockerhub官网从国内无法访问，因此修改镜像地址

# 查看默认的镜像文件

[root@master1 \~]#grep image: calico.yaml image: docker.io/calico/cni:v3.29.1 image: docker.io/calico/cni:v3.29.1 image: docker.io/calico/node:v3.29.1 image: docker.io/calico/node:v3.29.1 image: docker.io/calico/kube- controllers:v3.29.1

# [root@master1 \~]#grep docker.io calico.yaml

image: docker.io/calico/cni:v3.22.0 image: docker.io/calico/cni:v3.22.0 image: docker.io/calico/pod2daemon- flexvol:v3.22.0 image: docker.io/calico/node:v3.22.0 image: docker.io/calico/kube- controllers:v3.22.0

# 修改为定制的镜像

# 新版docker

[root@master1 \~]#sed - i 's@docker.io/calico/@registry.cn- beijing.aliyuncs.com/wangxiaochun/@' calico- v3.29.1. yaml [root@master1 \~]#sed - i 's@docker.io/calico/@registry.cn- beijing.aliyuncs.com/wangxiaochun/@' calico- v3.28.0. yaml

# 旧版

[root@master1 \~]#sed - i 's@docker.io/calico#harbor.wang.org/google_containers@g' calico.yaml

# 查看效果

# 新版

[root@master1 \~]#grep image: calico- v3.29.1. yaml

image: registry.cn- beijing.aliyuncs.com/wangxiaochun/cni:v3.29.1 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/cni:v3.29.1 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/node:v3.29.1 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/node:v3.29.1 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/kube- controllers:v3.29.1

controllers:v3.29.1

# [root@master1 \~]#grep image: calico-v3.28.0.yaml

image: registry.cn- beijing.aliyuncs.com/wangxiaochun/cni:v3.28.0 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/cni:v3.28.0 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/node:v3.28.0 image: registry.cn- beijing.aliyuncs.com/wangxiaochun/kube- controllers:v3.28.0

controllers:v3.28.0

# 旧版

# [root@master1 \~]## grep google calico.yaml

image: harbor.wang.org/google_containers/cni:v3.22.0 image: harbor.wang.org/google_containers/cni:v3.22.0 image: harbor.wang.org/google_containers/pod2daemon- flexvol:v3.22.0 image: harbor.wang.org/google_containers/node:v3.22.0 image: harbor.wang.org/google_containers/kube- controllers:v3.22.0

# 2.2.2.4应用官方默认清单文件

范例：使用官方默认文件的网络配置不做修改

# 应用calico插件

[root@master1 \~]#kubectl apply - f calico.yaml

# 查看master和worker节点的镜像有三个

[root@master1 \~]#docker images |grep calico|awk '{print \ $1,$ 2,\$NF}' calico/cni v3.22.0 236MB calico/pod2daemon- flexvol v3.22.0 21.4MB calico/node v3.22.0 213MB

# 只有一个节点有kube-controllers镜像，其它节点都只有三个

[root@node1 \~]#docker images |grep calico|awk '{print \ $1,$ 2,\$NF}' calico/kube- controllers v3.22.0 132MB calico/cni v3.22.0 236MB calico/pod2daemon- flexvol v3.22.0 21.4MB calico/node v3.22.0 213MB

# 环境部署完毕后，查看效果

[root@master1 \~]#kubectl get pod - n kube- system | egrep 'NAME|calico' NAME READY STATUS RESTARTS AGE calico- kube- controllers- 6fd7b9848d- 7wtvh 1/1 Running 0 11m calico- node- 5rfr1 1/1 Running 0 11m calico- node- 8624w 1/1 Running 0 11m calico- node- g4tcn 1/1 Running 0 11m calico- node- hgwd9 1/1 Running 0 11m calico- node- msjqp 1/1 Running 0 11m calico- node- tbh9k 1/1 Running 0 11m

自动生成一个api版本信息。calico部署完毕后，会生成一系列的自定义配置属性信息[root@master1 \~]#kubectl api- versions | grep crdcrd.projectcalico.org/v1

# [root@master1 \~]#kubectl get crds

NAME bgpconfigurations.crd.projectcalico.org bgppeers.crd.projectcalico.org blockaffinities.crd.projectcalico.org calicondestatuses.crd.projectcalico.org clusterinformations.crd.projectcalico.org felixconfigurations.crd.projectcalico.org globalnetworkpolicies.crd.projectcalico.org globalnetworksets.crd.projectcalico.org hostendpoints.crd.projectcalico.org ipamblocks.crd.projectcalico.org ipamconfigs.crd.projectcalico.org ipamhandles.crd.projectcalico.org

CREATED AT 2021- 09- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 109- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 09- 17T12:20:14Z 2021- 09- 17T2:20:15Z 2021- 09- 17T12:20:15Z

ippools.crd.projectcalico.org 2021- 09- 17T12:20:15Z  ipreservations.crd.projectcalico.org 2021- 09- 17T12:20:15Z  kubecontrollersconfigurations.crd.projectcalico.org 2021- 09- 17T12:20:15Z  networkpolicies.crd.projectcalico.org 2021- 09- 17T12:20:15Z  networksets.crd.projectcalico.org 2021- 09- 17T12:20:15Z

# #该api版本信息中有大量的配置属性

[root@master1 ~]#kubectl api- resources - - api- group=crd.projectcalico.org  NAME SHORTNAMES APIVERSION  NAMESPACED KIND  bgpconfigurations crd.projectcalico.org/v1 false  BGPConfiguration  bgppeers crd.projectcalico.org/v1 false  bgppeer  blockaffinities crd.projectcalico.org/v1 false  BlockAffinity  calicondestatuses crd.projectcalico.org/v1 false  Calicondestatus  clusterinformations crd.projectcalico.org/v1 false  ClusterInformation  felixconfigurations crd.projectcalico.org/v1 false  FelixConfiguration  globalnetworkpolicies crd.projectcalico.org/v1 false  GlobalNetworkPolicy  globalnetworksets crd.projectcalico.org/v1 false  GlobalNetworkSet  hostendpoints crd.projectcalico.org/v1 false  HostEndpoint  ipamblocks crd.projectcalico.org/v1 false  IPAMBlock  ipamconfigs crd.projectcalico.org/v1 false  IPAMConfig  ipamhandles crd.projectcalico.org/v1 false  IPAMHandle  ippools crd.projectcalico.org/v1 false  IPPool  ipreservations crd.projectcalico.org/v1 false  IPReservation  kubecontrollersconfigurations crd.projectcalico.org/v1 false  KubeControllersConfiguration  networkpolicies crd.projectcalico.org/v1 true  NetworkPolicy  networksets crd.projectcalico.org/v1 true  NetworkSet  networkSet

# #上面自定义资源，如下面的控制器进行控制

[root@master1 ~]#kubectl get pod - n kube- system |grep calico calico- kube- controllers- 5d6f95db4f- czg7z 1/1 Running 0 5h48m calico- node- 687qb 1/1 Running 0 5h48m calico- node- ddp56 1/1 Running 0 5h48m calico- node- strl2 1/1 Running 0 5h48m calico- node- wdh75 1/1 Running 0 5h48m

[root@master1 ~]#kubectl get ippoolsNAME AGEdefault- ipv4- ippool 10m

[root@master1 ~]#kubectl get ippools.crd.projectcalico.org default- ipv4- ippool - o yaml

apiversion: crd.projectcalico.org/v1 kind: IPPool

metadata:

annotations: projectcalico.org/metadata:{"uid":"d756dce9- 6384- 427d- bfa6- 35ffa4625640","creationTimestamp":"2023- 07- 11T10:05:42z"}

creationTimestamp: ""

generation: 1

name: default- ipv4- ippool

resourceversion: "4403"

uid: fa62275f- ccc6- 4a61- 97d9- 809605c5a89d

spec:

allowedUses:

- workload

- Tunnel

blockSize: 26

默认24位

cidr: 10.244.0.0/16 #Pod默认使用集群初始化时- - pod- network- - idr选项指定的网段

ipipMode: Always

natOutgoing: true

nodeSelector: all()

vxlanMode: Never

# 2.2.2.5 创建Pod测试效果

范例：查看网卡和路由的信息

查看每个Node的Pod的网段

[root@master1 calico]#kubectl get nodes - o yaml | grep - A3 spec

spec: podCIDR: 10.244.0.0/24 podCIDRs: - 10.244.0.0/24

spec:

podCIDR: 10.244.2.0/24

podCIDRs:

- 10.244.2.0/24

- -

spec:

podCIDR: 10.244.1.0/24

podCIDRs:

- 10.244.1.0/24

- -

spec:

podCIDR: 10.244.3.0/24

podCIDRs:

- 10.244.3.0/24

# 创建Pod，观察结果

[root@master1 ~]#kubectl create deployment myapp - - image wangxiaochun/pod- test:v0.1 - - replicas 6

# 观察到同一个节点的Pod使用相同的网段IP

[root@master1 ~]#kubectl get pod - o wide |awk '{print $7,\$6}' |sort  node1. wang.org 10.244.101.129  node1. wang.org 10.244.101.130  node2. wang.org 10.244.36.1  node2. wang.org 10.244.36.2  node3. wang.org 10.244.168.196  node3. wang.org 10.244.168.197

# 发现Calico并不使用Node节点指令的网段，而是自主分配

[root@master1 ~]#kubectl get pod - o wide

NAME READY STATUS RESTARTS AGE IP NODE NOMINATED NODE READINESS GATES myapp- 6b59f8f86- 2t2bn 1/1 Running 0 65s 10.244.36.1 node2. wang.org <none> <none> myapp- 6b59f8f86- 7q7k4 1/1 Running 0 65s 10.244.168.197 node3. wang.org <none> <none> myapp- 6b59f8f86- dtcq1 1/1 Running 0 65s 10.244.36.2 node2. wang.org <none> <none> myapp- 6b59f8f86- 1vsx9 1/1 Running 0 65s 10.244.168.196 node3. wang.org <none> <none> myapp- 6b59f8f86- mfdn8 1/1 Running 0 65s 10.244.101.129 node1. wang.org <none> <none> myapp- 6b59f8f86- w6g11 1/1 Running 0 65s 10.244.101.130 node1. wang.org <none> <none>

每个Pod所在节点自动生成对应的calixxxx的网卡，且此网卡的MAC都为ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee:ee

[root@node1 ~]#ip a

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000

link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00:00 inet 127.0.0.1/8 scope host lo valid_lft forever preferred_lft forever inet6 :1/128 scope host valid_lft forever preferred_lft forever

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_code1 state UP group default qlen 1000

link/ether 00:0c:29:2e:29:f3 brd ff:ff:ff:ff:ff:ff

altname enps31

altname ens33

inet 10.0.0.201/24 brd 10.0.0.255 scope global eth0 valid_lft forever preferred_lft forever inet6 fe80::20c:29ff:fe2e:29f3/64 scope link valid_lft forever preferred_lft forever

3: docker0: <NO- CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN group default

inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0 valid_lft forever preferred_lft forever

4: tun10@NONE: <NOARP,UP,LOWER_UP> mtu 1480 qdisc noqueue state UNKNOWN group default qlen 1000

link/ipip 0.0.0.0 brd 0.0.0.0

inet 10 2 scope global tunl0valid_lft forever preferred_lft forever7: cali2ad752e56a1@if4: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1480 qdisc noqueuestate UP group default qlen 1000link/ether ee:ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 0inet6 fe80::ecee:eeff:fee:eeee/64 scope linkvalid_lft forever preferred_lft forever8: cali47ddc65abff@if4: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1480 qdisc noqueuestate UP group default qlen 1000link/ether ee:ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 1inet6 fe80::ecee:eeff:fee:eeee/64 scope linkvalid_lft forever preferred_lft forever

# 每个Pod在宿主机上生成veth的网卡对

[root@node1 ~]#ethtool - i cali2ad752e56a1driver: vethversion: 1.0firmware- version:expansion- rom- version:bus- info:supports- statistics: yessupports- test: nosupports- eeprom- access: nosupports- register- dump: nosupports- priv- flags: no

# 查看和Pod的对应编号

[root@node1 ~]#ethtool - s cali13251176854

NIC statistics:

peer_ifindex: 4 #对应Pod网卡编号rx_queue_0_xdp_packets: 0rx_queue_0_xdp_bytes: 0rx_queue_0_drops: 0rx_queue_0_xdp_redirect: 0rx_queue_0_xdp_drops: 0rx_queue_0_xdp_tx: 0rx_queue_0_xdp_tx_errors: 0tx_queue_0_xdp_xmit: 0tx_queue_0_xdp_xmit_errors: 0

# 和flannel不同，calico不会生成新的网桥

[root@node1 ~]#brctl showbridge name bridge id STP enabled interfacesdocker0 8000.0242fd8a2fc8 no

# 当前主机上的每个Pod对应一条/32的路由

[root@node1 ~]#route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0 10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0 10.244.36.0 10.0.0.202 255.255.255.192 UG 0 0 0 tun10 10.244.53.64 10.0.0.200 255.255.255.192 UG 0 0 0 tun10 10.244.101.128 0.0.0.0 255.255.255.192 U 0 0 0 \* 10.244.101.129 0.0.0.0 255.255.255.255 UH 0 0 0

cali2ad752e56a1

10.244.101.130 0.0.0.0 255.255.255.255 UH 0 0 0 cali47ddc65abff 10.244.168.192 10.0.0.203 255.255.255.192 UG 0 0 0 tun10 172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0

# #查看节点上calixxxx的网卡信息

[root@node1 ~]#ethtool - i cali2ad752e56a1

driver:veth

version:1.0

firmware- version:

expansion- rom- version:

bus- info:

supports- statistics: yes

supports- test: no

supports- eeprom- access: no

supports- register- dump: no

supports- priv- flags: no

[root@node1 ~]#ethtool - s cali2ad752e56a1

NIC statistics:

peer_ifindex: 4 #对应Pod的4号网卡rx_queue_0_xdp_packets: 0rx_queue_0_xdp_bytes: 0rx_queue_0_drops: 0rx_queue_0_xdp_redirect: 0rx_queue_0_xdp_errors: 0rx_queue_0_xdp_tx: 0rx_queue_0_xdp_tx_errors: 0tx_queue_0_xdp_xmit: 0tx_queue_0_xdp_xmit_errors: 0

进入Pod查看网卡信息

[root@master1 ~]#kubectl exec - it myapp- 6b59f8f86- mfdn8 - - sh

安装ethtool工具

[root@myapp- 6b59f8f86- mfdn8 /]# apk add ethtool

查看网卡信息

[root@myapp- 6b59f8f86- mfdn8 /]# ip a

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000

link/loopback 00:00:00:00:00 brd 00:00:00:00:00

inet 127.0.0.1/8 scope host lo valid_lft forever preferred_lft forever

2: tun10@NONE: <NOARP> mtu 1480 qdisc noop state DOWN group default qlen 1000 link/qlip 0.0.0.0 brd 0.0.0.0

Pod内的4号网卡对应节点的7号网卡

4: eth0@if7: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1480 qdisc noqueue state UP group default qlen 1000 link/ether 4e:a9:cf:b0:59:f9 brd ff:ff:ff:ff:ff:ff link- netnsid 0 inet 10.244.101.129/32 scope global eth0 valid_lft forever preferred_lft forever

[root@myapp- 6b59f8f86- mfdn8 /]# ethtool - i eth0

driver:veth version: 1.0 firmware- version: expansion- rom- version: bus- info:

supports- statistics: yes supports- test: no supports- eeprom- access: no supports- register- dump: no supports- priv- flags: no

[root@myapp- 6b59f8f86- mfdn8 /]# ethtool - s eth0

NIC statistics:

peer_ifindex:7 #对应节点的7号网卡 rx_queue_0_xdp_packets:0 rx_queue_0_xdp_bytes:0 rx_queue_0_drops:0 rx_queue_0_xdp_redirect:0 rx_queue_0_xdp_rops:0 rx_queue_0_xdp_tx:0 rx_queue_0_xdp_tx_errors:0 tx_queue_0_xdp_xmit:0 tx_queue_0_xdp_xmit_errors:0

[root@myapp- 6b59f8f86- mfdn8 /]# route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 169.254.1.1 0.0.0.0 UG 0 0 0 eth0 169.254.1.1 0.0.0.0 255.255.255.255 UH 0 0 0 eth0

# 2.2.2.6Pod网络通信分析

范例：tcpdump抓包分析

由于在calico的默认网络模型是IPIP，所以在进行Pod之间的数据包测试的时候，可以通过直接抓取宿主机数据包，来发现双层ip效果[root@master1 ~]#kubectl run pod- client - - image=registry.cn- - beijing.aliyuncs.com/wangxiaochun/admin- box:v0.1 - it - - rm - - command - - /bin/bash

如果在同一个节点的Pod间通信，也需要路由一跳通信，说明同一个节点的Pod间通信也是需要跨路由的，即每一个Pod都需要有一条路由记录[root@myapp- 547df679bp- 7gjlh /]# ping - c1 10.244.163.231PING 10.244.163.231 (10.244.163.231): 56 data bytes64 bytes from 10.244.163.231: seq=0 ttl=63 time=0.416 ms

- --- 10.244.163.231 ping statistics 
- - 1 packets transmitted, 1 packets received, 0% packet loss round-trip min/avg/max = 0.416/0.416/0.416 ms

如果在不同个节点的Pod间通信，路由需要2跳

[root@myapp- 547df679bp- 7gjlh /]# ping - c1 10.244.23.45PING 10.244.23.45 (10.244.23.45): 56 data bytes64 bytes from 10.244.23.45: seq=0 ttl=62 time=0.643 ms

- --- 10.244.23.45 ping statistics 
- - 1 packets transmitted, 1 packets received, 0% packet loss round-trip min/avg/max = 0.643/0.643/0.643 ms

root@pod- client /# ping 192.168.144.4

或者使用之前的Pod

[root@master1 ~]#kubectl exec - it myapp- 7c47997446- 8h99v - /bin/sh [root@myapp- 7c47997446- 8h99v ]# ping 192.168.144.4

[root@master1 ~]#kubectl get pod - o wide |awk '{print  $1,$ 2, $6,$ 7}' NAME READY IP NODE myapp- 7c47997446- 8h99v 1/1 192.168.23.4 node1. wang.org myapp- 7c47997446- p9tn8 1/1 192.168.144.4 node2. wang.org myapp- 7c47997446- zx2rd 1/1 192.168.163.5 node3. wang.org

# #抓包观察

[root@node2 ~]#tcpdump - i eth0 - nn - vvv ip host 10.0.0.104 and host 10.0.0.105 tcpdump: verbose output suppressed, use - v or - vv for full protocol decode listening on eth0, link- type EN10MB (Ethernet), capture size 262144 bytes 12:16:35.240634 IP 10.0.0.104 > 10.0.0.105: IP 192.168.23.4 > 192.168.144.4: ICMP echo request, id 6656, seq 0, length 64 (ipip- proto- 4) 12:16:35.240844 IP 10.0.0.105 > 10.0.0.104: IP 192.168.144.4 > 192.168.23.4: ICMP echo reply, id 6656, seq 0, length 64 (ipip- proto- 4) 12:16:36.240975 IP 10.0.0.104 > 10.0.0.105: IP 192.168.23.4 > 192.168.144.4: ICMP echo request, id 6656, seq 1, length 64 (ipip- proto- 4) 12:16:36.241130 IP 10.0.0.105 > 10.0.0.104: IP 192.168.144.4 > 192.168.23.4: ICMP echo reply, id 6656, seq 1, length 64 (ipip- proto- 4) 12:16:37.241220 IP 10.0.0.104 > 10.0.0.105: IP 192.168.23.4 > 192.168.144.4: ICMP echo request, id 6656, seq 2, length 64 (ipip- proto- 4) 12:16:37.241349 IP 10.0.0.105 > 10.0.0.104: IP 192.168.144.4 > 192.168.23.4: ICMP echo reply, id 6656, seq 2, length 64 (ipip- proto- 4) 12:16:38.241473 IP 10.0.0.104 > 10.0.0.105: IP 192.168.23.4 > 192.168.144.4: ICMP echo request, id 6656, seq 3, length 64 (ipip- proto- 4) 12:16:38.241595 IP 10.0.0.105 > 10.0.0.104: IP 192.168.144.4 > 192.168.23.4: ICMP echo reply, id 6656, seq 3, length 64 (ipip- proto- 4)

# #结果显示：

每个数据包都是基于双层ip取套的方式来进行传输，而且协议是ipip- proto- 4结合路由的分发详情，可以看到具体的操作效果。具体效果：192.168.23.4 - > 10.0.0.104 - > 10.0.0.105 - > 192.168.144.4

[root@node1 ~]#tcpdump - i eth0 - nn - vvv ip host 10.0.0.203 and host 10.0.0.202 tcpdump: listening on eth0, link- type EN10MB (Ethernet), snapshot length 262144 bytes 14:45:07.745422 IP (tos 0x0, ttl 63, id 5475, offset 0, flags [DF], proto IPIP (4), length 80) 10.0.0.201 > 10.0.0.202: IP (tos 0x0, ttl 63, id 31692, offset 0, flags [DF], proto TCP (6), length 60) 192.168.23.2.52676 > 192.168.144.3.80: Flags [S], cksum 0xab9c (correct), seq 2731853996, win 64800, options [mss 1440, sack0K,TS val 372943032 ecr 0, nop,wscale 7], length 0 14:45:07.746048 IP (tos 0x0, ttl 63, id 6018, offset 0, flags [DF], proto IPIP (4), length 80) 10.0.0.202 > 10.0.0.201: IP (tos 0x0, ttl 63, id 0, offset 0, flags [DF], proto TCP (6), length 60) 192.168.144.3.80 > 192.168.23.2.52676: Flags [S.], cksum 0xb241 (correct), seq 3643199396, ack 2731853997, win 64260, options [mss 1440, sack0K,TS val 1306330301 ecr 3752948032, nop,wscale 7], length 0 14:45:07.746158 IP (tos 0x0, ttl 63, id 5476, offset 0, flags [DF], proto IPIP (4), length 72) 10.0.0.201 > 10.0.0.202: IP (tos 0x0, ttl 63, id 31693, offset 0, flags [DF], proto TCP (6), length 52)

192.168.23.2.52676 > 192.168.144.3.80: Flags [., cksum 0xda02 (correct), seq 1, ack 1, win 507, options [nop,nop,TS val 3752943033 ecr 1306330301], length 0 14:45:07.746276 IP (tos 0x0, ttl 63, id 5477, offset 0, flags [DF], proto IPIP (4), length 149) 10.0.0.201 > 10.0.0.202: IP (tos 0x0, ttl 63, id 31694, offset 0, flags [DF], proto TCP (6), length 129) 192.168.23.2.52676 > 192.168.144.3.80: Flags [P.], cksum 0x3f94 (correct), seq 1:78, ack 1, win 507, options [nop,nop,TS val 3752943033 ecr 1306330301], length 77: HTTP, length: 77 GET / HTTP/1.1 Host: 192.168.144.3 User- Agent: curl/7.67.0 Accept: "/" 14:45:07.746607 IP (tos 0x0, ttl 63, id 6019, offset 0, flags [DF], proto IPIP (4), length 72) 10.0.0.202 > 10.0.0.201: IP (tos 0x0, ttl 63, id 2398, offset 0, flags [DF], proto TCP (6), length 52) 192.168.144.3.80 > 192.168.23.2.52676: Flags [., cksum 0xd9ba (correct), seq 1, ack 78, win 502, options [nop,nop,TS val 1306330301 ecr 3752943033], length 0 14:45:07.749036 IP (tos 0x0, ttl 63, id 6020, offset 0, flags [DF], proto IPIP (4), length 89) 10.0.0.202 > 10.0.0.201: IP (tos 0x0, ttl 63, id 2399, offset 0, flags [DF], proto TCP (6), length 69) 192.168.144.3.80 > 192.168.23.2.52676: Flags [P.], cksum 0x19da (correct), seq 1:18, ack 78, win 502, options [nop,nop,TS val 1306330304 ecr 3752943033], length 17: HTTP, length: 17 HTTP/1.0 200 OK

范例：wireshark 抓包查看通信过程

# 创建Pod

[root@master1 ~]#kubectl create deployment myapp - - image wangxiaochun/pod- test:v0.1 - - replicas 6

[root@master1 ~]#kubectl get pod - o wide

NAME READY STATUS RESTARTS AGE IP NODE NOMINATED NODE READINESS GATES myapp- 6b59f8f86- 2t2bn 1/1 Running 0 65s 10.244.36.1 node2. wang.org <none> <none> myapp- 6b59f8f86- 7q7k4 1/1 Running 0 65s 10.244.168.197 node3. wang.org <none> <none> myapp- 6b59f8f86- dtcq1 1/1 Running 0 65s 10.244.36.2 node2. wang.org <none> <none> myapp- 6b59f8f86- 1vsx9 1/1 Running 0 65s 10.244.168.196 node3. wang.org <none> <none> myapp- 6b59f8f86- mfdn8 1/1 Running 0 65s 10.244.101.129 node1. wang.org <none> <none> myapp- 6b59f8f86- w6g11 1/1 Running 0 65s 10.244.101.130 node1. wang.org <none> <none>

在节点网卡上抓包，可以观察跨节点Pod间的通信过程

[root@master1 ~]#kubectl exec - it myapp- 6b59f8f86- mfdn8 - - sh [root@myapp- 6b59f8f86- mfdn8 /]# curl 10.244.36.1 kubernetes pod- test v0.1!! ClientIP: 10.244.101.129, ServerName: myapp- 6b59f8f86- 2t2bn, ServerIP: 10.244.36.1!

访问同一个节点的其它Pod，由于在calixxxx网卡直接通信，不走节点网卡，所以在节点网卡抓不到包[root@myapp- 6b59f8f86- mfdn8 /]# curl 10.244.101.130kubernetes pod- test v0.1!! ClientIP: 10.244.101.129, ServerName: myapp- 6b59f8f86- w6g11, ServerIP: 10.244.101.130!

# 观察如下显示结果

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/77418229db911e2f890580c6611ba9a1645a33f23e8e225d75fa31c224c764bc.jpg)

# 2.2.3部署Calico案例：使用IPIPCross-Subnet配置

默认总是使用IPIP叠加网络，性能不佳，可修改为Cross- Subnet，实现Pod通信时同一网段用BGP，不同网段才使用IPIP

方法1

删除原有的网络

[root@master1 \~]# kubectl delete - f calico.yaml

修改配置

[root@master1 \~]# vim calico.yaml

Enable IPIP

- name: CALICO_IPV4POOL_IPIP

value:"Always"

value:"Cross- Subnet" #此项修改为Cross- Subnet或者Cross- Subnet，表示Pod在同一网段使用BGP，跨网段时才启用IPIP，但不能VxLAN同时启用

Enable or Disable VXLAN on the default IP pool.- name: CALICO_IPV4POOL_VXLANvalue: "Never" #此项修改为"Cross- Subnet", 表示Pool在同一网段使用BGP, 跨网段时才启用VXLAN, 但不能IPIP同时启用

# 应用配置

[root@master1 ~]#kubectl apply - f calico.yaml

查看可能没有成功

[root@master1 ~]#kubectl get ippools default- ipv4- ippool - o yaml

# 方法2

[root@master1 ~]#kubectl edit ippools.crd.projectcalico.org default- ipv4- ippool

方法3：如果第1种方式有问题，可以使用此方式

查看配置生成配置文件

[root@master1 ~]#kubectl get ippools default- ipv4- ippool.yaml - o yaml > default- ipv4- ippool.yaml

# 修改

[root@master1 ~]#vim default- ipv4- ippool.yaml

删除timestamp, generation, uid信息

修改ipipMode:

ipipMode: CrossSubnet #支持Always|CrossSubnet|Never三个值

# 应用配置生效

[root@master1 ~]#kubectl apply - f default- ipv4- ippool.yaml

查看路由表，注意接口为eth0，说明直接路由

[root@master1 ~]#route - n

内核IP路由表

目标 网关 子网掩码 标志 跃点 引用 使用 接口 0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0 10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0 172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0 192.168.23.0 10.0.0.104 255.255.255.0 UG 0 0 0 eth0 192.168.25.0 10.0.0.102 255.255.255.0 UG 0 0 0 eth0 192.168.144.0 10.0.0.105 255.255.255.0 UG 0 0 0 eth0 192.168.163.0 10.0.0.106 255.255.255.0 UG 0 0 0 eth0 192.168.213.0 0.0.0.0 255.255.255.0 U 0 0 0 192.168.216.0 10.0.0.103 255.255.255.0 UG 0 0 0 eth0

[root@master1 ~]#kubectl delete deployments.apps myapp[root@master1 ~]#kubectl create deployment myapp - - image=wangxiaochun/pod- test:v0.1 - - replicas=3

[root@master1 ~]#kubectl get pod - o wide

NAME READY STATUS RESTARTS AGE IP NODE NOMINATED NODE READINESS GATES myapp- 7c47997446- jrbw1 1/1 Running 0 32s 192.168.23.1 node1. wang.org <none> <none> myapp- 7c47997446- vq6vt 1/1 Running 0 32s 192.168.163.1 node3. wang.org <none> <none> myapp- 7c47997446- wmj6n 1/1 Running 0 32s 192.168.144.2 node2. wang.org <none> <none> [root@master1 ~]#kubectl exec - it myapp- 7c47997446- jrbw1 - - sh [root@myapp- 7c47997446- jrbw1 /]# ping 192.168.163.1

PING 192.168.163.1 (192.168.163.1): 56 data bytes 64 bytes from 192.168.163.1: seq=0 ttl=62 time=0.480 ms [root@myapp- 7c47997446- jrbw1 /]# curl 192.168.163.1

# #抓包观察

[root@node2 ~]#tcpdump - i eth0 - nn icmp

tcpdump: verbose output suppressed, use - v or - vv for full protocol decode listening on eth0, link- type EN10MB (Ethernet), capture size 262144 bytes 12:25:53.953736 IP 192.168.23.1 > 192.168.163.1: ICMP echo request, id 3072, seq 26, length 64 12:25:53.953888 IP 192.168.163.1 > 192.168.23.1: ICMP echo reply, id 3072, seq 26, length 64 12:25:54.954269 IP 192.168.23.1 > 192.168.163.1: ICMP echo request, id 3072, seq 27, length 64

[root@node2 ~]#tcpdump - i eth0 - nn - vvv tcp port 80

tcpdump: listening on eth0, link- type EN10MB (Ethernet), snapshot length 262144 bytes

15:07:39.634858 IP (tos 0x0, ttl 63, id 6063, offset 0, flags [DF], proto TCP (6), length 60)

192.168.23.1.52476 > 192.168.163.1.80: Flags [S], cksum 0x2885 (incorrect - > 0xc61b), seq 822640604, win 64800, options [mss 1440, sackOK,TS val 3754294921 ecr 0, nop,wscale 7], length 0

15:07:39.635665 IP (tos 0x0, ttl 63, id 0, offset 0, flags [DF], proto TCP (6), length 60)

192.168.163.1.80 > 192.168.23.12.52476: Flags [S.], cksum 0x535d (correct), seq 3257646885, ack 822640605, win 64260, options [mss 1440, sackOK,TS val 1307682182 ecr 3754294921, nop,wscale 7], length 0

15:07:39.635732 IP (tos 0x0, ttl 63, id 6064, offset 0, flags [DF], proto TCP (6), length 52)

192.168.23.1.52476 > 192.168.163.1.80: Flags [.], cksum 0x287d (incorrect - > 0x7b1e), seq 1, ack 1, win 507, options [nop, nop, TS val 3754294922 ecr 1307682182], length 0

15:07:39.635815 IP (tos 0x0, ttl 63, id 6065, offset 0, flags [DF], proto TCP (6), length 129)

192.168.23.1.52476 > 192.168.163.1.80: Flags [P.], cksum 0x28ca (incorrect - > 0xe0af), seq 1:78, ack 1, win 507, options [nop, nop, TS val 3754294922 ecr 1307682182], length 77: HTTP, length: 77

GET / HTTP/1.1  Host: 192.168.163.1  User- Agent: curl/7.07.0  Accept: */*

# #在worker节点查看路由表

[root@node1 ~]#route - n 内核 IP 路由表

目标 网关 子网掩码 标志 跃点 引用 使用 接口 0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0 10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0 172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0 192.168.23.0 0.0.0.0 255.255.255.0 U 0 0 0 \* 192.168.23.1 0.0.0.0 255.255.255.255 UH 0 0 0 cali01aad30a3bf 192.168.23.2 0.0.0.0 255.255.255.255 UH 0 0 0 cali545684cf79f 192.168.25.0 10.0.0.102 255.255.255.0 UG 0 0 0 eth0

<table><tr><td>192.168.144.0</td><td>10.0.0.105</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.163.0</td><td>10.0.0.106</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.213.0</td><td>10.0.0.101</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.216.0</td><td>10.0.0.103</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr></table>

# [root@node2 \~]#route -n

内核IP路由表

<table><tr><td>目标</td><td>网关</td><td>子网掩码</td><td>标志</td><td>跃点</td><td>引用</td><td>使用</td><td>接口</td></tr><tr><td>0.0.0.0</td><td>10.0.0.2</td><td>0.0.0.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>10.0.0.0</td><td>0.0.0.0</td><td>255.255.255.0</td><td>U</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>172.17.0.0</td><td>0.0.0.0</td><td>255.255.0.0</td><td>U</td><td>0</td><td>0</td><td>0</td><td>docker0</td></tr><tr><td>192.168.23.0</td><td>10.0.0.104</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.25.0</td><td>10.0.0.102</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.144.0</td><td>0.0.0.0</td><td>255.255.255.0</td><td>U</td><td>0</td><td>0</td><td>0</td><td>*</td></tr><tr><td>192.168.144.1</td><td>0.0.0.0</td><td>255.255.255.255</td><td>UH</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>cal1426e6b2189e</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>192.168.144.2</td><td>0.0.0.0</td><td>255.255.255.255</td><td>UH</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>cal19bab2670bc</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>192.168.144.3</td><td>0.0.0.0</td><td>255.255.255.255</td><td>UH</td><td>0</td><td>0</td><td>0</td><td>0</td></tr><tr><td>cal16bbe530865b</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>192.168.163.0</td><td>10.0.0.106</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.213.0</td><td>10.0.0.103</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr><tr><td>192.168.216.0</td><td>10.0.0.103</td><td>255.255.255.0</td><td>UG</td><td>0</td><td>0</td><td>*</td><td>eth0</td></tr></table>

# [root@node3 \~]#route -n

内核IP路由表

<table><tr><td>目标</td><td>网关</td><td>子网掩码</td><td>标志</td><td>跃点</td><td>引用</td><td>使用</td><td>接口</td></tr><tr><td>0.0.0.0</td><td>10.0.0.2</td><td>0.0.0.0</td><td>UG</td><td>0</td><td>0</td><td>0</td><td>eth0</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

<table><tr><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;fcel&gt;</td><td>&lt;nl&gt;</td></tr></table>

# 注意：Node1上的两个Pod对应上面的两个calixxx接口

[root@master1 \~]#kubectl get pod - A - o wide |grep node1

default myapp- 7c47997446- jrbw1 1/1 Running 0

7m29s 192.168.23.1 node1. wang.org <none> <none>

kube- system coredns- 8468f7cd6- bdpf7 1/1 Running

1 (23m ago) 20d 192.168.23.2 node1. wang.org <none>

<none>

# 2.2.4部署Calico案例：使用BGP配置

# #修改为BGP模式

[root@master1 \~]#kubectl edit ippools.crd.projectcalico.org default- ipv4- ippoo1 apiversion: crd.projectcalico.org/v1 kind:IPPool

metadata:

annotations:

generation: 3name: default- ipv4- ippoolresourceversion: "2428730"uid: 304c761c- d33e- 4c22- a2f1- 60fd629943a8

uid:304c761c- d33e- 4c22- a2f1- 60fd629943a8

spec:

alloweduses: - workload - Tunnel blockSize: 24 cidr: 10.244.0.0/16

#ipipMode:Always

ipipMode:Never #修改此行

natOutgoing: true

nodeSelector: a11()

vxlanMode:Never

# #查看路由表

[root@node1 \~]#route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 10.0.0.2 0.0.0.0 U 0 0 eth0 10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 eth0 10.244.23.0 0.0.0.0 255.255.255.0 U 0 0 0 \* 10.244.23.90 0.0.0.0 255.255.255.255 UH 0 0 0

cali3d321aef9c7

10.244.23.91 0.0.0.0 255.255.255.255 UH 0 0 0

cali0e6b75291c2

10.244.23.92 0.0.0.0 255.255.255.255 UH 0 0 0

cali7f22771d9d2

10.244.23.93 0.0.0.0 255.255.255.255 UH 0 0 0

cali7f09440efda

10.244.23.94 0.0.0.0 255.255.255.255 UH 0 0 0

calid568e889c3c

10.244.23.95 0.0.0.0 255.255.255.255 UH 0 0 0

cali13251176854

10.244.23.101 0.0.0.0 255.255.255.255 UH 0 0 0

cali25e96b97a8c

10.244.23.102 0.0.0.0 255.255.255.255 UH 0 0 0

cali8e36c4d65e6

10.244.23.103 0.0.0.0 255.255.255.255 UH 0 0 0

calid034cb34a92

10.244.144.0 10.0.0.202 255.255.255.0 UG 0 0 0 eth0

10.244.163.0 10.0.0.203 255.255.255.0 UG 0 0 0 eth0

10.244.213.0 10.0.0.200 255.255.255.0 UG 0 0 0 eth0

172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0

# #查看Pod

[root@master1 \~]#kubectl get pod - o wide

NAME READY STATUS RESTARTS AGE IP NODE

NOMINATED NODE READINESS GATES

myapp- 547df679bb- 2rtx6 1/1 Running 0 17m 10.244.163.26

node3. wang.org <none> <none>

myapp- 547df679bb- kvj1t 1/1 Running 0 17m 10.244.144.1

node2. wang.org <none> <none>

myapp- 547df679bb- phftk 1/1 Running 0 17m 10.244.23.103

node1. wang.org <none>

# #跨主机通信

[root@master1 ~]#kubectl exec - it myapp- 547df679bb- phftk - - sh [root@myapp- 547df679bb- phftk ]# curl 10.244.144.1 kubernetes pod- test v0.1!! ClientIP: 10.244.23.103, ServerName: myapp- 547df679bb- kvjlt, ServerIP: 10.244.144.1! [root@myapp- 547df679bb- phftk ]#

# #抓包分析，可以看到直接的路由通信

[root@node1 ~]#tcpdump - i eth0 - nn port 80tcpdump: verbose output suppressed, use - v[v]... for full protocol decode listening on eth0, link- type EN10MB (Ethernet), snapshot length 262144 bytes 17:43:42.252593 IP 10.244.23.103.49260 > 10.244.144.1.80: flags [S], seq 2963939714, win 64800, options [mss 1440, sackok,TS val 2172510891 ecr 0, nop,wscale 7], length 0 17:43:42.253024 IP 10.244.144.1.80 > 10.244.23.103.49260: flags [S.], seq 114478910, ack 2963939715, win 64260, options [mss 1440, sackok,TS val 3762915002 ecr 2172510891, nop,wscale 7], length 0 17:43:42.253102 IP 10.244.23.103.49260 > 10.244.144.1.80: flags [.], ack 1, win 507, options [nop,nop,TS val 2172510892 ecr 3762915002], length 0 17:43:42.254686 IP 10.244.23.103.49260 > 10.244.144.1.80: flags [P.], seq 1:77, ack 1, win 507, options [nop,nop,TS val 2172510893 ecr 3762915002], length 76: HTTP: GET / HTTP/1.1 17:43:42.255457 IP 10.244.144.1.80 > 10.244.23.103.49260: flags [.], ack 77, win 502, options [nop,nop,TS val 3762915005 ecr 2172510893], length 0 17:43:42.256642 IP 10.244.144.1.80 > 10.244.23.103.49260: flags [P.], seq 1:18, ack 77, win 502, options [nop,nop,TS val 3762915006 ecr 2172510893], length 17: HTTP: HTTP/1.0 200 OK 17:43:42.256746 IP 10.244.23.103.49260 > 10.244.144.1.80: flags [.], ack 18, win 507, options [nop,nop,TS val 2172510896 ecr 3762915006], length 0 17:43:42.256968 IP 10.244.144.1.80 > 10.244.23.103.49260: flags [FP.], seq 18:267, ack 77, win 502, options [nop,nop,TS val 3762915006 ecr 2172510893], length 249: HTTP 17:43:42.257094 IP 10.244.23.103.49260 > 10.244.144.1.80: flags [F.], seq 77, ack 268, win 506, options [nop,nop,TS val 2172510896 ecr 3762915006], length 0 17:43:42.257337 IP 10.244.144.1.80 > 10.244.23.103.49260: flags [.], ack 78, win 502, options [nop,nop,TS val 3762915007 ecr 2172510896], length 0

# 2.2.5部署Calico案例：使用VXLAN模式

# 方法1

[root@master1 ~]#kubectl edit ippools.crd.projectcalico.org default- ipv4- ippool

spec:

alloweduses: - workload - Tunnel blocksize: 24 cidr: 192.168.0.0/16 ipipMode: Never #此处改为Never natOutgoing: true nodeSelector: all() vxlanMode: Always #此处改为A1ways，表示总是使用VXLAN，也可以为Cross- Subnet，实现Pod通信时同一网段用BGP，不同网段才使用VXLAN，支持A1ways|CrossSubnet|Never三个值

# 方法2

[root@master1 ~]# vim calico.yaml

# Enable IPIP

- name: CALICO_IPV4POOL_IPIP

value:"Never"#此项修改为Never，表示Pod启用VXLAN

Enable or Disable VXLAN on the default IP pool.

- name: CALICO_IPV4POOL_VXLAN

value:"Always"#此项修改为"Always"，表示Pod启用VXLAN

应用calico插件

[root@master1 ~]# kubectl apply - f calico.yaml

修改后立即生效，查看路由表如下

[root@node1 ~]# ip a

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000

link/loopback 00:00:00:00:00 brd 00:00:00:00:00:00

inet 127.0.0.1/8 scope host 10

valid_lft forever preferred_lft forever

inet6 :1/128 scope host

valid_lft forever preferred_lft forever

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_code1 state UP group default qlen 1000

link/ether 00:0c:29:a3:42:3c brd ff:ff:ff:ff:ff:ff

altname enp2s1

altname ens33

inet 10.0.0.201/24 brd 10.0.0.255 scope global eth0

valid_lft forever preferred_lft forever

inet6 fe80::20c:29ff:fea3:423c/64 scope link

valid_lft forever preferred_lft forever

3: docker0: <NO- CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state

DOWN group default

link/ether 02:42:20:dd:f9:35 brd ff:ff:ff:ff:ff:ff

inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0

valid_lft forever preferred_lft forever

4: cali25e96b97a8c@if2: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 0

inet6 fe80::ecee:eeff:fee:eeee/64 scope link

valid_lft forever preferred_lft forever

5: cali7f09440efda@if2: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 1

inet6 fe80::ecee:eeff:fee:eeee/64 scope link

valid_lft forever preferred_lft forever

6: calid568e889c3c@if2: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 2inet6 fe80::ecee:eeff:fee:eeee/64 scope linkvalid_lft forever preferred_lft forever

7: cali7f22771d9d2@if2: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 3inet6 fe80::ecee:eeff:fee:eeee/64 scope linkvalid_lft forever preferred_lft forever

8: cali3d321aef9c7@if2: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:ee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 4

inet6 fe80::ecee:eeff:fee:eeee/64 scope linkvalid_lft forever preferred_lft forever

valid_lft forever preferred_lft forever

9: vxlan.calico: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state

UNKNOWN group default qlen 1000

link/ether 66:69:94:f0:87:f7 brd ff:ff:ff:ff:ff:ff inet 10.244.23.66/32 scope global vxlan.calico valid_lft forever preferred_lft forever inet6 fe80::6469:94ff:fef0:87f7/64 scope link valid_lft forever preferred_lft forever

# 隧道接口

[root@node1 \~]#ethtool - i vxlan.calico

driver: vxlan

version:0.1

firmware- version:

expansion- run- version:

bus- info:

supports- statistics: no

supports- test: no

supports- eeprom- access: no

supports- register- dump: no

supports- priv- flags: no

[root@master1 \~]#route - n

内核IP路由表

目标 网关 子网掩码 标志 跃点 引用 使用 接口

0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0

10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0

172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0

192.168.23.0 192.168.23.3 255.255.255.0 UG 0 0 0

vxlan.calico

192.168.144.0 192.168.144.4 255.255.255.0 UG 0 0 0

vxlan.calico

192.168.163.0 192.168.163.4 255.255.255.0 UG 0 0 0

vxlan.calico

192.168.213.0 0.0.0.0 255.255.255.0 U 0 0 0 \*

[root@node1 \~]#route - n

内核IP路由表

目标 网关 子网掩码 标志 跃点 引用 使用 接口

0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0

10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0

172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0

192.168.23.0 0.0.0.0 255.255.255.0 U 0 0 0 \*

192.168.23.2 0.0.0.0 255.255.255.255 UH 0 0 0

calia6b94e324d6

192.168.144.0 192.168.144.4 255.255.255.0 UG 0 0 0

vxlan.calico

192.168.163.0 192.168.163.4 255.255.255.0 UG 0 0 0

vxlan.calico

192.168.213.0 192.168.213.1 255.255.255.0 UG 0 0 0

vxlan.calico

创建Pod，相互通信

[root@master1 \~]#kubectl create deployment myapp - - image wangxiaochun/pod- test:v0.1 - - replicas 3

[root@master1 \~]#kubectl get pod - o wide

NAME READY STATUS RESTARTS AGE IP NODE

NOMINATED NODE READINESS GATES

myapp- 7dbd45d496- gcmzt 1/1 Running 0 81m 192.168.23.2 node1. wang.org <none> <none> myapp- 7dbd45d496- jbwfb 1/1 Running 0 81m 192.168.144.3 node2. wang.org <none> <none> myapp- 7dbd45d496- twzb1 1/1 Running 0 81m 192.168.163.3 node3. wang.org <none> <none> [root@master1 ~]#kubect1 exec - it myapp- 7dbd45d496- gcmzt - - sh [root@myapp- 7dbd45d496- gcmzt /]# cur1 192.168.144.3 kubernetes pod- test v0.1!! ClientIP: 192.168.23.2, ServerName: myapp- 7dbd45d496- jbwfb, ServerIP: 192.168.144.3!

# #抓包分析

[root@node1 ~]#tcpdump - i eth0 - nn host 10.0.0.201 and host 10.0.0.202 and udp port 4789 tcpdump: verbose output suppressed, use - v[v]... for full protocol decode listening on eth0, link- type EN10MB (Ethernet), snapshot length 262144 bytes 16:36:13.529708 IP 10.0.0.201.55745 > 10.0.0.202.4789: VXLAN, flags [I] (0x08), vni 4096 IP 192.168.23.2.45814 > 192.168.144.3.80: Flags [S], seq 3732184155, win 64800, options [mss 1440, sackOK,TS val 3759608816 ecr 0,nop,wscale 7], length 0 16:36:13.530431 IP 10.0.0.202.33406 > 10.0.0.201.4789: VXLAN, flags [I] (0x08), vni 4096 IP 192.168.144.3.80 > 192.168.23.2.45814: Flags [S.], seq 2718718049, ack 3332184156, win 64260, options [mss 1440, sackOK,TS val 1312996078 ecr 3759608816,nop,wscale 7], length 0 16:36:13.530493 IP 10.0.0.201.55745 > 10.0.0.202.4789: VXLAN, flags [I] (0x08), vni 4096 IP 192.168.23.2.45814 > 192.168.144.3.80: Flags [.], ack 1, win 507, options [nop,nop,TS val 3759608817 ecr 1312996078], length 0 16:36:13.530566 IP 10.0.0.201.55745 > 10.0.0.202.4789: VXLAN, flags [I] (0x08), vni 4096 IP 192.168.23.2.45814 > 192.168.144.3.80: Flags [P.], seq 1:78, ack 1, win 507, options [nop,nop,TS val 3759608817 ecr 1312996078], length 77: HTTP: GET / HTTP/1.1

# #通过wireshark抓包可以看到如下显示

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/570e308dd81fa7568f3f050aa8c46d983c25f9e79d78d5dd483f8fd2a5f6729a.jpg)

# 2.3.6部署Calico案例：使用VXLANCross-Subnet配置

对于现有的calico环境，如果需要使用BGP环境，可以直接使用一个配置清单来进行修改calico环境即可。

这里先来使用calicoctl修改配置属性。

#

获取当前的配置属性[root@master1 \~]#kubectl calico get ipPoolsNAME CEDR SELECTORdefault- ipv4- ippool 10.244.0.0/16 all)#当前路由表[root@master1 \~]#route - nKernel IP routing tableDestination Gateway Genmask Flags Metric Ref Use Iface0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth010.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth010.244.36.0 10.0.0.202 255.255.255.192 UG 0 0 0 tun1010.244.53.64 0.0.0.0 255.255.255.192 U 0 0 0 \*10.244.101.128 10.0.0.201 255.255.255.192 UG 0 0 0 tun1010.244.168.192 10.0.0.203 255.255.255.192 UG 0 0 0 tun10172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0[root@node1 \~]#route - nKernel IP routing tableDestination Gateway Genmask Flags Metric Ref Use Iface0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth010.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth010.2  $44.36.0$  10.0.0.202 255.255.255.192 UG 0 0 0 tun1010.244.53.64 10.0.0.200 255.255.255.192 UG 0 0 0 tun1010.244.101.128 0.0.0.0 255.255.255.192 U 0 0 0 \*10.244.101.129 0.0.0.0 255.255.255.255 UH 0 0 0 cali2ad752e56a110.244.101.130 0.0.0.0 255.255.255.255 UH 0 0 0 cali47ddc65abff10.244.168.192 10.0.0.203 255.255.255.192 UG 0 0 0 tun10172.17.0.0 0.0.0.0 255.255.0.0 U 0 0 0 docker0

# 查看配置

[root@master1 \~]#kubectl get ipPools default- ipv4- ippool - o yamlapiversion: crd.projectcalico.org/v1kind: IPPoolmetadata:  annotations:    projectcalico.org/metadata: '{"uid":"36bea103- 3b2a- 4c98- a7f7- 16d8705c3ea1","creationTimestamp":"2023- 07- 16T00:16:43z"}'  creationTimestamp: "2021- 03- 29T01:56:56z"  generation: 3  name: default- ipv4- ippool  resourceVersion: "66588"  uid: b65e5f4f- cba5- 4d55- b16a- d2084257c500  spec:  alloweduses:  - workload  - Tunnel  blocksize: 26

cidr: 10.244.0.0/16  ipipMode: Always  natOutgoing: true  nodeSelector: all()  vxlanMode: Never

查看配置，此方式需要提前安装caliactl工具  [root@master1 ~]#kubect1 calico get ipPools default- ipv4- ippool - o yaml  apiVersion: projectcalico.org/v3  kind: IPPool  metadata:  creationTimestamp: "2021- 03- 29T01:56:56Z"  name: default- ipv4- ippool  resourceversion: "053278"  uid: 494e0fd3- 1704- 4cbf- bd77- 36c2b675e7ca

spec:

blockSize: 24  cidr: 10.244.0.0/16  ipipMode: Always  natOutgoing: true  nodeSelector: all()  vxlanMode: Never

修改配置实现VXLAN with BGP

方法1：直接修改ipPool配置

[root@master1 ~]#kubect1 edit ipPools default- ipv4- ippool

spec:

alloweduses:  - workload

- Tunnel

blockSize: 26

cidr: 10.244.0.0/16

ipipMode: Never  #修改此行为Never

natOutgoing: true

nodeSelector: all()

vxlanMode: CrossSubnet  #修改此行

方法2：定制资源配置文件，注意：此方式需要安装caliactl工具

[root@master1 ~]#kubect1 calico get ipPools default- ipv4- ippool - o yaml > default- ipv4- ippool.yaml

修改配置文件

[root@master1 ~]#vim default- ipv4- ippool.yaml

[root@master1 ~]#cat default- ipv4- ippool.yaml

apiVersion: projectcalico.org/v3

kind: IPPool

metadata:

name: default- ipv4- ippool

spec:

blockSize: 26

cidr: 10.244.0.0/16

#ipipMode: Always

ipipMode: Never  #修改此行Never，禁用IPIP

natOutgoing: true

nodeSelector: all()

#vxlanMode: Never

vxlanMode: Never  vxlanMode: Cross- Subnet  #修改此行为Cross- Subnet或者CrossSubnet

配置说明：

禁用IPIP，将原来的Aways改为Never

#vlanMode 将原来的Never更改成Cross- Subnet（节点间跨网段）模式，即可以实现vlan with BGP的效果

# 应用资源配置文件

[root@master1 ~]#kubectl calico apply - f default- ipv4- ippool.yaml Successfully applied 1 'IPPool' resource(s)

# 查看节点的IP信息，tunl0网卡没有IP

[root@node1 ~]#ip a

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000

link/loopback 00:00:00:00:00 brd 00:00:00:00:00:00

inet 127.0.0.1/8 scope host 10

valid_lft forever preferred_lft forever

inet6 :1/128 scope host

valid_lft forever preferred_lft forever

2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_code1 state UP group default qlen 1000

link/ether 00:0c:29:2e:29:f3 brd ff:ff:ff:ff:ff:ff altname enp2s1 altname ens33 inet 10.0.0.201/24 brd 10.0.0.255 scope global eth0 valid_lft forever preferred_lft forever inet6 fe80::20c:29ff:fe2e:29f3/64 scope link valid_lft forever preferred_lft forever

3: docker0: <NO- CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN group default

link/ether 02:42:6e:5b:1d:3e brd ff:ff:ff:ff:ff:ff inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0 valid_lft forever preferred_lft forever

4: tunl0@NONE: <NOARP,UP,LOWER_UP> mtu 1480 qdisc noqueue state UNKNOWN group default qlen 1000

link/ipip 0.0.0.0 brd 0.0.0.0

7: cali2ad752e56a1@if4: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1480 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:eee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 0 inet6 fe80:ecee:eeff:fee:eeee/64 scope link valid_lft forever preferred_lft forever

8: cali47ddc65abff@if4: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1480 qdisc noqueue state UP group default qlen 1000

link/ether ee:ee:eee:ee:ee:ee brd ff:ff:ff:ff:ff:ff link- netnsid 1 inet6 fe80:ecee:eeff:fee:eeee/64 scope link valid_lft forever preferred_lft forever

11: vxlan.calico: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1450 qdisc noqueue state UNKNOWN group default qlen 1000

link/ether 66:69.94:f0:87:f7 brd ff:ff:ff:ff:ff:ff inet 10.244.101.181/32 scope global vxlan.calico valid_lft forever preferred_lft forever inet6 fe80::6469:94ff:fef0:87f7/64 scope link valid_lft forever preferred_lft forever

# 验证路由表，不再显示tunl0路由信息

[root@node1 ~]#route - n

Kernel IP routing table

Destination Gateway Genmask Flags Metric Ref Use Iface 0.0.0.0 10.0.0.2 0.0.0.0 UG 0 0 0 eth0 10.0.0.0 0.0.0.0 255.255.255.0 U 0 0 0 eth0

<table><tr><td>10.244.36.0</td><td>10.0.0.202</td><td>255.255.255.192 UG</td><td>0</td><td>0</td><td>0 eth0</td></tr><tr><td>10.244.53.64</td><td>10.0.0.200</td><td>255.255.255.192 UG</td><td>0</td><td>0</td><td>0 eth0</td></tr><tr><td>10.244.101.128</td><td>0.0.0.0</td><td>255.255.255.192 U</td><td>0</td><td>0</td><td>0 *</td></tr><tr><td>10.244.101.129</td><td>0.0.0.0</td><td>255.255.255.255 UH</td><td>0</td><td>0</td><td>0</td></tr><tr><td>cal12ad752e56a1</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>10.244.101.130</td><td>0.0.0.0</td><td>255.255.255.255 UH</td><td>0</td><td>0</td><td>0</td></tr><tr><td>cal147ddc65abff</td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>10.244.168.192</td><td>10.0.0.203</td><td>255.255.255.192 UG</td><td>0</td><td>0</td><td>0 eth0</td></tr><tr><td>172.17.0.0</td><td>0.0.0.0</td><td>255.255.0.0</td><td>U</td><td>0</td><td>0 docker0</td></tr></table>

# #再次进行Pod跨节点网络通信

[root@master1 ~]#kubectl get pod - o wide | awk '{print  $1,$ 2, $6,$ 7}'

NAME READY IP NODE

myapp- 6b59f8f86- 2t2bn 1/1 10.244.36.1 node2. wang.org myapp- 6b59f8f86- 7q7k4 1/1 10.244.168.197 node3. wang.org myapp- 6b59f8f86- dtcq1 1/1 10.244.36.2 node2. wang.org myapp- 6b59f8f86- 1vsx9 1/1 10.244.168.196 node3. wang.org myapp- 6b59f8f86- mfdn8 1/1 10.244.101.129 node1. wang.org myapp- 6b59f8f86- w6g11 1/1 10.244.101.130 node1. wang.org

# #从Node1节点的Pod访问Node2节点的Pod

[root@master1 ~]#kubectl exec - it myapp- 6b59f8f86- mfdn8 - - sh [root@myapp- 6b59f8f86- mfdn8 ]# curl 10.244.36.1 kubernetes pod- test v0.1!!! clientIP: 10.244.101.129, ServerName: myapp- 6b59f8f86- 2t2bn, ServerIP: 10.244.36.1!

在node1节点抓包，由于这次直接通过节点的BGP路由进行转发，所以在抓包时，直接通过Pod的IP抓取即可。

[root@node1 ~]#tcpdump - i eth0 - vv - nn tcp port 80

[root@node1 ~]#tcpdump - i eth0 - vv - nn ip host 10.244.36.1

09:49:24.430615 IP (tos 0x0, ttl 63, id 36548, offset 0, Flags [DF], proto TCP (6), length 60)

10.244.101.129.50984 > 10.244.36.1.80: Flags [S], cksum 0x9f98 (incorrect - > 0x57c5), seq 119959596, win 64800, options [mss 1440, sackOK,TS val 2736419294 ecr 0, nop, wscale 7], length 0

09:49:24.431332 IP (tos 0x0, ttl 63, id 0, offset 0, flags [DF], proto TCP (6), length 60)

10.244.36.1.80 > 10.244.101.129.50984: Flags [S.], cksum 0x763f (correct), seq 1816929306, ack 119959597, win 64260, options [mss 1440, sackOK,TS val 3272380445 ecr 2736419294, nop, wscale 7], length 0

09:49:24.431400 IP (tos 0x0, ttl 63, id 36549, offset 0, flags [DF], proto TCP (6), length 52)

10.244.101.129.50984 > 10.244.36.1.80: Flags [., cksum 0x9f90 (incorrect - > 0x9c00), seq 1, ack 1, win 507, options [nop, nop, TS val 2736419295 ecr 3272380445], length 0

09:49:24.431480 IP (tos 0x0, ttl 63, id 36550, offset 0, flags [DF], proto TCP (6), length 127)

10.244.101.129.50984 > 10.244.36.1.80: Flags [P.], cksum 0x9fdb (incorrect - > 0x3fca), seq 1:76, ack 1, win 507, options [nop, nop, TS val 2736419295 ecr 3272380445], length 75: HTTP, length: 75

GET / HTTP/1.1  Host: 10.244.36.1  User- Agent: curl/7.67.0  Accept: */*

同时用wireshark抓包，显示如下

![](https://cdn-mineru.openxlab.org.cn/result/2025-09-26/f8e1a47b-b9b5-4279-9e83-fb2ed568831d/17e84331ee496da23ee67f89565a420f79a53c6979e98e4ef0bc0a45a05b2476.jpg)