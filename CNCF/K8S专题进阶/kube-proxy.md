一个 Service 对应的“后端”由 Pod 的 IP 地址和容器端口号组成，即一个完整的“IP:Port”访问地址，它在 Kubernetes 系统中被称作 Endpoint(端点)。通过查看Service的详细信息，可以看到其后端Endpoints列表：

```shell
root@master:~/yamlDir/K8sDefinitiveGuide-V6-A/Chapter05# kubectl describe svc webapp
Name:              webapp
Namespace:         default
Labels:            <none>
Annotations:       <none>
Selector:          app=webapp
Type:              ClusterIP
IP Family Policy:  SingleStack
IP Families:       IPv4
IP:                10.50.196.202
IPs:               10.50.196.202
Port:              <unset>  8080/TCP
TargetPort:        8080/TCP
Endpoints:         10.60.104.46:8080,10.60.166.160:8080
Session Affinity:  None
Events:            <none>
```

实际上，Kubernetes 自动创建了与 Service 关联的 Endpoint 资源对象，这可以通过查询 endpoints 对象进行查看：

```shell
root@master:~/yamlDir/K8sDefinitiveGuide-V6-A/Chapter05# kubectl get endpoints
NAME         ENDPOINTS                              AGE
webapp       10.60.104.46:8080,10.60.166.160:8080   118m
```

从 Kubernetes v1.21 版本开始，Kubernetes 系统也会默认创建 endpointslice (端点分片)资源对象

```shell
root@master:~/yamlDir/K8sDefinitiveGuide-V6-A/Chapter05# kubectl get endpointslice
NAME           ADDRESSTYPE   PORTS   ENDPOINTS                    AGE
webapp-wd6db   IPv4          8080    10.60.166.160,10.60.104.46   119m
```

当一个 Service 对象在 Kubernetes 集群中被定义出来时，集群中的客户端应用就可以通过服务 IP 地址访问具体的 Pod 容器提供的服务了。从 Master 中获取 Service 和 Endpoint 的变更，以及在节点上设置 Service 到后端的多个 Endpoint (图中简写为EP)的负载均衡策略，则是由每个 Node 上的 kube-proxy 负责实现的，如图5.1所示。  

![image-20250718162041706](image/image-20250718162041706.png)

## kube-proxy的代理模式

kube-proxy 目前提供了以下几种代理模式(通过启动参数 --proxy-mode 设置)。

(1) iptables 模式(仅适用于Linux操作系统)

在 iptables 模式下，kube-proxy 通过设置 Linux Kernel 的 iptables 规则，实现了从 Service 到后端 Endpoints 列表的负载分发规则。由于使用的是 Linux 操作系统内核的 Netfilter 机制，所以流量转发效率很高，也很稳定。

每次新建的 Service 或者 Endpoint 发生变化时，kube-proxy 都会刷新本 Node 的全部 iptables 规则，在大规模集群(如 Service 和 Endpoint 的数量达到数万个)中这会导致刷新时间过长，并进一步导致系统性能下降，这时可以在 kube-proxy 的配置资源对象 kube-proxy 中通过以下参数调整 iptables 规则的同步行为：

```yaml
    iptables:
      localhostNodePorts: null
      masqueradeAll: false
      masqueradeBit: null
      minSyncPeriod: 0s
      syncPeriod: 0s
```

**syncPeriod（同步周期）**

- 作用：控制全量 iptables 规则同步的间隔时间
- 工作机制：
  - kube-proxy 每隔 `syncPeriod`时间执行一次全量规则刷新
  - 期间发生的变更会累积，在下一个周期统一处理
- 设置 iptables 规则的同步时间间隔，用于与 Service 或 EndpointSlice 变化无关的 iptables 规则的同步(有时候其他系统可能会干扰 kube-proxy 设置的 iptables 规则)，以及用于定时清理 iptables 规则，单位为 s。

**minSyncPeriod（最小同步间隔）**

- 作用：限制两次全量刷新之间的最小时间间隔
- 关键机制：
  - 当 Service/Endpoint 变更时，触发异步刷新请求
  - 若距上次刷新不足 `minSyncPeriod`，则延迟执行
  - 保证刷新间隔 ≥ `minSyncPeriod`
- 该属性值被设置为 0 时表示只要有 Service 或 Endpoint 发生变化，kube-proxy 就会立刻同步所有 iptables 规则。
  

(2) ipvs 模式(仅适用于Linux操作系统)

在 ipvs 模式下，kube-proxy 通过 Linux Kernel 的 netlink 接口来设置 ipvs 规则。ipvs 模式基于 Linux 操作系统内核的netfilter 钩子函数(hook)，类似于 iptables 模式，但使用了散列表作为底层数据结构，并且工作在内核空间，这使得 ipvs 模式比 iptables 模式的转发性能更高、延迟更低，同步 Service 和 Endpoint 规则的效率也更高，还支持更高的网络吞吐量。

ipvs 模式要求 Linux Kernel 启用 IPVS 内核模块，如果 kube-proxy 在 Linux 操作系统中未检测到 IPVS 内核模块，kube-proxy 会自动切换至 iptables 模式。