上一节课，我在介绍资源对象定义的过程中，提到了一些对象，例如：metav1.TypeMeta、metav1.ObjectMeta等。除了这些对象之外，还有一些其他对象，为方便你统一理解，本小节，我来统一介绍下这些对象。

## Kubernetes 对象概览

![img](image/Fln9Zif6DWp50Ea3GbK5dKYeo3_b)

Kubernetes 对象都实现了 runtime.Object 接口，我们可以用 runtime.Object 指代所有的 Kubernetes 对象。runtime.Object 是个接口类型，可以有多个实现。在 Kubernetes 中，你可以根据需要实现多种不同的 Kubernetes 对象。当前 Kubernetes 中有以下 2 种对象类型：

1. 单个对象，例如 Pod；
2. 列表对象，例如：PodList。

每种对象类型，又包含了对象类型和对象属性。

上图，包含了 Kubernetes 对象构建体系中的多个核心概念，例如：schema.ObjectKind、metav1.TypeMeta、metav1.ObjectMeta、metav1.Object 等，接下来，我一个一个给你讲解。

metav1、runtime、schema 包的导入路径如下：

```
metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
"k8s.io/apimachinery/pkg/runtime"
"k8s.io/apimachinery/pkg/runtime/schema"
```

## Kubernetes 对象概念区分

在 Kubernetes 相关的开发文章、书籍或者课程中，你可能会经常听到 Kubernetes 资源对象、Kubernetes 对象、Kubernetes 对象属性、Kubernetes 资源元数据这些概念。未来避免你混淆，本小节来统一说明。

### 资源对象（Resource Object）

Kubernetes 中“一条记录”就是一个资源对象，代表集群期望或当前的某个实体状态：

1. 会持久化到 etcd；
2. API Server 为它暴露 REST 端点；
3. 典型例子：Pod、Deployment、Service、ConfigMap、CustomResource 等。

例如 Deployment 资源对象定义如下：

```go
type Deployment struct {  
    metav1.TypeMeta   `json:",inline"` // apiVersion / kind  
    metav1.ObjectMeta `json:"metadata,omitempty"` // 名称、标签等  
    Spec   appsv1.DeploymentSpec   `json:"spec,omitempty"`   // 期望  
    Status appsv1.DeploymentStatus `json:"status,omitempty"` // 当前  
}
```

### 对象（Object）

 Kubernetes 对象与 Kubernetes资源对象基本等价，多指单个 API 资源实例：

1. “对象”更侧重抽象概念，“资源对象”强调它通过 API 暴露且可持久化；
2. 在代码层，二者都映射为某个 Go 结构体。

### 对象属性（Object Field / Attribute）

对象属性即对象结构体里的 字段：

1. 最顶层常见字段：metadata、spec.replicas、status.phase 等。
2. 字段既可读也可写，部分字段只由控制器写（如 Status）。

示例如下：

```
spec.replicas           # 属性：副本数  
spec.template.spec.containers[0].image  # 属性：容器镜像  
status.observedGeneration              # 属性：已观测版本  
```

### 资源元数据（Resource Metadata）

指所有描述“这个对象是谁”的信息，主要由 TypeMeta 与 ObjectMeta 组成。

1） TypeMeta（类型元数据）

```
apiVersion: apps/v1  
kind: Deployment  
```

告诉 API Server 这是哪个组/版本里的哪种资源。

2）ObjectMeta（对象元数据）

介绍资源对象有哪些元数据的数据结构，典型字段如下：

```
metadata:  
  name: web-deploy  
  namespace: prod  
  labels:  
    app: web  
  annotations:  
    description: "demo"  
  uid: 3c6…      # 系统生成  
  resourceVersion: "74291"  
  generation: 4  
  creationTimestamp: "2024-04-22T10:00:00Z"  
  finalizers: [...]   
  ownerReferences: [...]  
```

1. name/namespace：唯一定位；
2. labels/annotations：查询与附加信息；
3. uid/resourceVersion/generation：用于一致性与并发控制；
4. finalizers/ownerReferences：级联删除、控制器关联等。

### 小结对比

![img](image/FvkOvDj05bme0Kd3kv1aiA8oJrwu)

## 核心结构源码位置

Kubernetes 对象体系中涉及到多个核心结构，这些核心结构的源码位置和功能介绍如下表所示：

> 提示：文件位置父路径统一为 k8s.io/apimachinery/pkg。

![img](image/FtnL2CvnH61sKHvoXGPuuzxnIvUG)

接下来，我自顶向下的来给你介绍下涉及到的核心结构的功能和实现。

## runtime.Object

在 Kubernetes 代码中，统一使用 [runtime.Object](https://github.com/kubernetes/kubernetes/blob/v1.30.4/staging/src/k8s.io/apimachinery/pkg/runtime/interfaces.go#L323) 接口来代表 Kubernetes 对象。runtime.Object 接口是一个非常重要的接口，用于表示 Kubernetes API 对象的通用类型。这个接口定义了一些方法，使得所有 Kubernetes API 对象都能够遵循统一的规范，这有助于简化开发人员对 Kubernetes 对象的处理和管理。

### runtime.Object接口定义

下面是 runtime.Object 接口的定义：

```
// Object interface must be supported by all API types registered with Scheme. Since objects in a scheme are
// expected to be serialized to the wire, the interface an Object must provide to the Scheme allows
// serializers to set the kind, version, and group the object is represented as. An Object may choose
// to return a no-op ObjectKindAccessor in cases where it is not expected to be serialized.
type Object interface {
    GetObjectKind() schema.ObjectKind
    DeepCopyObject() Object
}
```

runtime.Object 接口中包含了两个方法：

1. GetObjectKind() schema.ObjectKind：此方法返回对象的类型信息。schema.ObjectKind 是一个接口，用于描述 Kubernetes API 对象的类型和版本信息。
2. DeepCopyObject() Object：此方法用于创建对象的深层副本。在 Kubernetes 中，对象的深层副本是一种常见的操作，用于确保对象的不可变性和避免引用共享。

runtime.Object 位于 runtime 包中，说明 runtime.Object 是一个非常基础的接口。事实上，所有的 Kubernetes 对象都属于 runtime.Object。

runtime.Object 是一个接口类型，意味着，它可以有多个实现。在 Kubernetes 中，有 2 种类型的 runtime.Object：单个对象和列表对象。

[schema.ObjectKind](https://github.com/kubernetes/kubernetes/blob/v1.30.4/staging/src/k8s.io/apimachinery/pkg/runtime/schema/interfaces.go#L22) 接口类型定义如下：

```
// 代码位于：staging/src/k8s.io/apimachinery/pkg/runtime/schema/interfaces.go

// All objects that are serialized from a Scheme encode their type information. This interface is used
// by serialization to set type information from the Scheme onto the serialized version of an object.
// For objects that cannot be serialized or have unique requirements, this interface may be a no-op.
type ObjectKind interface {
    // SetGroupVersionKind sets or clears the intended serialized kind of an object. Passing kind nil
    // should clear the current setting.
    SetGroupVersionKind(kind GroupVersionKind)
    // GroupVersionKind returns the stored group, version, and kind of an object, or an empty struct
    // if the object does not expose or provide these fields.
    GroupVersionKind() GroupVersionKind
}

// GroupVersionKind unambiguously identifies a kind.  It doesn't anonymously include GroupVersion
// to avoid automatic coercion.  It doesn't use a GroupVersion to avoid custom marshalling
type GroupVersionKind struct {
    Group   string // 资源组
    Version string // 资源版本
    Kind    string // 资源类型
}
```

schema.ObjectKind 接口提供了 2 个核心方法用来设置和获取资源的核心信息：

1. SetGroupVersionKind()：设置资源组、资源版本、资源类型。3 个核心信息以字符串的形式包含在 GroupVersionKind 结构体中；
2. GroupVersionKind()：获取资源组、资源版本、资源类型。3 个核心信息以字符串的形式包含在GroupVersionKind 结构体中。

也就是说，所有的 Kubernetes 对象，都可以使用 SetGroupVersionKind 方法设置 GKV 信息、使用GroupVersionKind 方法获取 GVK 信息，使用 DeepCopyObject 方法深拷贝该对象。

在 runtime.Object 的接口中，包含了一个函数 DeepCopyObject，用来实现 Kubernetes 对象的深度拷贝。那么， 为什么一定要生成 API 定义的深度拷贝方法呢？在 Kubernetes 中，深拷贝函数的存在是为了确保在处理 API 对象时能够正确地进行对象的复制，避免因浅拷贝而导致的数据共享和引用问题。以下是一些原因说明为什么 Kubernetes 需要深拷贝函数：

1. 避免数据共享问题：在 Kubernetes 中，API 对象通常包含复杂的嵌套结构和引用关系。如果使用浅拷贝来复制这些对象，可能会导致不同对象之间共享同一份数据，一个对象的修改会影响到其他对象，从而造成意外的行为；
2. 保持对象的一致性：在 Kubernetes 中，各种控制器和操作都可能涉及到对 API 对象的操作和处理。通过使用深拷贝函数，可以确保在处理对象时不会意外地修改原对象，从而保持对象的一致性和正确性。

所以，总结如上，我们可以知道一个 Kubernetes 一定实现了 runtime.Object 接口，一定具有以下 3 个核心方法：

1. SetGroupVersionKind()：设置对象的 GVK（**G**roup、**V**ersion、**K**ind）；
2. GroupVersionKind()：获取对象的 GVK；
3. DeepCopyObject()：深度拷贝当前资源对象。

通过上面的介绍，你应该已经明白 runtime.Object 的实现和功能了。runtime.Object 代表任何 Kubernetes 对象，可能是一个 Kubernetes 资源对象，也可能只是一个 metav1.TypeMeta 结构体对象，但是无论如何，能直接从它那里得到的东西很少，因为它只有一个方法定义而已，所以拿到 runtime.Object 之后，还需要通过转换和判断，获取原始类型对象，从而获取更多的信息，执行更多的操作。

### runtime.Object 接口实现（metav1.TypeMeta）

在前面的小结中，我介绍了 Kubernetes 对象定义中，都要内嵌一个 metav1.TypeMeta 结构体类型，metav1.TypeMeta 就是 runtime.Object 接口的具体实现。

metav1.TypeMeta 结构体定义如下：

```
// 位于 k8s.io/apimachinery/pkg/apis/meta/v1/types.go 文件中

// TypeMeta describes an individual object in an API response or request
// with strings representing the type of the object and its API schema version.
// Structures that are versioned or persisted should inline TypeMeta.
//
// +k8s:deepcopy-gen=false
type TypeMeta struct {
    // Kind is a string value representing the REST resource this object represents.
    // Servers may infer this from the endpoint the client submits requests to.
    // Cannot be updated.
    // In CamelCase.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds
    // +optional
    Kind string `json:"kind,omitempty" protobuf:"bytes,1,opt,name=kind"`

    // APIVersion defines the versioned schema of this representation of an object.
    // Servers should convert recognized schemas to the latest internal value, and
    // may reject unrecognized values.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources
    // +optional
    APIVersion string `json:"apiVersion,omitempty" protobuf:"bytes,2,opt,name=apiVersion"`
}
```

metav1.TypeMeta 结构体具有以下方法（[metav1.TypeMeta 方法列表](https://github.com/kubernetes/kubernetes/blob/v1.30.4/staging/src/k8s.io/apimachinery/pkg/apis/meta/v1/meta.go#L117)）：

```
func (obj *TypeMeta) GetObjectKind() schema.ObjectKind { return obj }

// SetGroupVersionKind satisfies the ObjectKind interface for all objects that embed TypeMeta
func (obj *TypeMeta) SetGroupVersionKind(gvk schema.GroupVersionKind) {
    obj.APIVersion, obj.Kind = gvk.ToAPIVersionAndKind()
}

// GroupVersionKind satisfies the ObjectKind interface for all objects that embed TypeMeta
func (obj *TypeMeta) GroupVersionKind() schema.GroupVersionKind {
    return schema.FromAPIVersionAndKind(obj.APIVersion, obj.Kind)
}
```

可以看到 Kubernetes 对象通过内嵌 metav1.TypeMeta 结构体实现了 GetObjectKind() 方法。



那么 DeepCopyObject() Object 方法又是如何实现的呢？这里，我们来看下 Deployment 的具体定义：

```
// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +k8s:prerelease-lifecycle-gen:introduced=1.6
// +k8s:prerelease-lifecycle-gen:deprecated=1.8
// +k8s:prerelease-lifecycle-gen:removed=1.16
// +k8s:prerelease-lifecycle-gen:replacement=apps,v1,Deployment

// DEPRECATED - This group version of Deployment is deprecated by apps/v1beta2/Deployment. See the release notes for
// more information.
// Deployment enables declarative updates for Pods and ReplicaSets.
type Deployment struct {
    metav1.TypeMeta `json:",inline"`
    // Standard object metadata.
    // +optional
    metav1.ObjectMeta `json:"metadata,omitempty" protobuf:"bytes,1,opt,name=metadata"`

    // Specification of the desired behavior of the Deployment.
    // +optional
    Spec DeploymentSpec `json:"spec,omitempty" protobuf:"bytes,2,opt,name=spec"`

    // Most recently observed status of the Deployment.
    // +optional
    Status DeploymentStatus `json:"status,omitempty" protobuf:"bytes,3,opt,name=status"`
}
```

在 Deployment 结构体上面有一行 // +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object 注释，只是 Kubernetes 源码文件中，一种特殊的注释，Kubernetes 的 deepcopy-gen 代码生成工具会根据该注释生成资源结构体的 DeepCopyObject 方法（生成的代码保存在 [zz_generated.deepcopy.go](https://github.com/kubernetes/kubernetes/blob/v1.29.2/staging/src/k8s.io/api/apps/v1beta1/zz_generated.deepcopy.go#L112) 文件中）：

```
// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *Deployment) DeepCopyObject() runtime.Object {
    if c := in.DeepCopy(); c != nil {
        return c
    }
    return nil
}
```

所以，在 Kubernetes 我们如果想实现一个 Kubernetes 对象，就要做以下 2 步：

1. 定义的资源对象内嵌 metav1.TypeMeta 结构体；
2. 结构体定义前面，添加 // +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object 注释，并调用 deepcopy-gen 工具生成 DeepCopyObject 方法。

Kubernetes 对象定义中，也会固定内嵌一个 metav1.ObjectMeta 结构体，那么该结构体的功能是什么呢？接下来，我再来给你介绍下。

### DeepCopyObject 是如何生成的？

https://cloud.tencent.com/developer/article/2064836

上面，我多次提到过 DeepCopyObject 方法，那么 DeepCopyObject 方法具体是如何实现的呢？在 Kubernetes 中，DeepCopyObject方法是通过 deepcopy-gen工具生成的。

首先，在资源对象目录下创建一个 doc.go文件（如果没有的话），并在 doc.go文件中添加 / +k8s:deepcopy-gen=package注释：

```
// Copyright 2022 Lingfei Kong <colin404@foxmail.com>. All rights reserved.
// Use of this source code is governed by a MIT style
// license that can be found in the LICENSE file. The original repo for
// this file is https://github.com/superproj/onex.
//

// +k8s:deepcopy-gen=package

// Package v1beta1 is the v1beta1 version of the API.
package v1beta1
```

这里一定要注意 // +k8s:deepcopy-gen=package注释，要在 package v1beta1之上。

接着，在你要生成的资源对象结构体定义之上添加 // +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object注释，例如：

```go
// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// XXX is an example definition of a Kubernetes resource object.
type XXX struct {
    metav1.TypeMeta `json:",inline"`
    // Standard object's metadata.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata
    // +optional
    metav1.ObjectMeta `json:"metadata,omitempty" protobuf:"bytes,1,opt,name=metadata"`

    // Spec defines the behavior of the XXX.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status
    // +optional
    Spec XXXSpec `json:"spec,omitempty" protobuf:"bytes,2,opt,name=spec"`

    // Status describes the current status of a XXX.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status
    // +optional
    Status XXXStatus `json:"status,omitempty" protobuf:"bytes,3,opt,name=status"`
}
```

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object注释告诉 deepcopy-gen工具生成 runtime.Object的接口实现。

## metav1.ObjectMeta 实现

在前面的小节中，介绍了所有的 Kubernetes 资源对象，都内嵌了一个 metav1.ObjectMeta 类型的结构体。本小节，来介绍下该结构体的实现和功能。

metav1.ObjectMeta 是 Kubernetes 资源的元数据。所有 Kubernetes 资源对象均具有统一的元数据。资源具有统一的元数据，会带来很多好处：

1. **提高代码复用度：**所有资源对象，用同一个元数据定义，可以复用跟元数据相关的方法、函数等；
2. **提高代码可读性：**所有资源具有统一的元数据，也可以提高代码的可读性，开发者理解了一个资源的元数据定义，相当于理解了所有资源对象的元数据定义。

> 提示：在实际的业务开发中，也建议业务的 REST 资源对象也具有统一的元数据。

[metav1.ObjectMeta](https://github.com/kubernetes/kubernetes/blob/v1.29.2/staging/src/k8s.io/apimachinery/pkg/apis/meta/v1/types.go#L111) 的具体定义及字段释义如下：

```go
// Kubernetes 所有的资源对象定义中都会内嵌该字段，作为资源对象的元数据（资源List对象除外）。
type ObjectMeta struct {
	// 资源对象的名字，作为资源的唯一标识。
	// 如果资源是集群维度的，那需要集群维度唯一。如果资源是命名空间维度的，那需要命名空间唯一。
	// 如果创建资源时，没有指定 GenerateName 字段，那么必须要设置 Name 字段。
	// Name 字段不能被更新。
	// 更多信息：https://kubernetes.io/docs/concepts/overview/working-with-objects/names#names  
	Name string `json:"name,omitempty" protobuf:"bytes,1,opt,name=name"`

	// GenerateName 是一个可选的字段，用来告诉 Kubernets 要以 GenerateName 字段为前缀自动生成资源名
	// 例如：GenerateName: agent-，当 Name 字段为空时，Kubernetes 会为该字段生成 agent-xxxxx 的资源名。
	// 当 Name 字段不为空时，使用 Name 字段作为资源名，忽略 GenerateName 字段。
	// 如果指定该字段且生成的名称已存在，服务器将返回 409。
	// 更多信息：https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#idempotency
	GenerateName string `json:"generateName,omitempty" protobuf:"bytes,2,opt,name=generateName"`

	// 必须是 DNS_LABEL。
	// Namespace 定义了资源所在的命令空间。这里要注意，并非所有的对象都需要命名空间。
	// 在 Kubernetes 中有 2 大类资源：
	//   - 命名空间维度的资源，例如：Pod、Service、Secret等，绝大部分资源都有 Namespace 属性；
	//   - 集群维度的资源，例如：Node、ClusterRole、PV 等。
	// Namespace 字段不能被更新。
	// 更多信息：https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces
	Namespace string `json:"namespace,omitempty" protobuf:"bytes,3,opt,name=namespace"`

	// 已废弃：selfLink 是一个遗留只读字段，系统不再填充该字段。
	SelfLink string `json:"selfLink,omitempty" protobuf:"bytes,4,opt,name=selfLink"`

	// UID 是该资源对象在时间和空间中唯一的值。通常在资源成功创建时由服务器生成，且不允许在 PUT 操作中更改。
	// UID 字段由系统填充，且只读。
	// 更多信息：https://kubernetes.io/docs/concepts/overview/working-with-objects/names#uids
	UID types.UID `json:"uid,omitempty" protobuf:"bytes,5,opt,name=uid,casttype=k8s.io/kubernetes/pkg/types.UID"`

	// ResourceVersion 表示该对象的内部版本，可被客户端用于确定对象何时发生变化。
	// 可用于乐观并发、变更检测和资源或资源集的观察操作。
	// 客户端必须将这些值视为不透明并未修改地传回服务器。
	// 仅对特定资源或资源集有效。
	// ResourceVersion 字段由系统填充，且只读。
	// 更多信息：https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#concurrency-control-and-consistency
	ResourceVersion string `json:"resourceVersion,omitempty" protobuf:"bytes,6,opt,name=resourceVersion"`

	// Generation 代表特定状态的序列号。
	// Generation 由系统填充，且只读。
	Generation int64 `json:"generation,omitempty" protobuf:"varint,7,opt,name=generation"`

	// CreationTimestamp 表示该对象创建时的服务器时间戳。
	// 客户端无法设置此值。其表示格式为 RFC3339，并为 UTC。
	// CreationTimestamp 由系统填充，且只读。
	// 对于资源列表为 Null。
	// 更多信息：https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata
	CreationTimestamp Time `json:"creationTimestamp,omitempty" protobuf:"bytes,8,opt,name=creationTimestamp"`

	// DeletionTimestamp 表示该资源将被删除的 RFC 3339 日期和时间。
	// 当用户请求优雅删除时，服务器设置该字段，客户端无法直接设置。
	// 预期在此字段中的时间之后，资源将被删除（不再可见且无法通过名称访问）。
	// 只要 finalizers 列表中有元素，删除就会被阻止。
	// 一旦设置了 deletionTimestamp，该值不可取消或设置为将来的时间，但可缩短。
	// 例如，用户可能请求在 30 秒后删除 Pod，Kubelet 会向 Pod 中的容器发送优雅终止信号。
	// 30 秒后，Kubelet 会发送强制终止信号（SIGKILL）并在清理后从 API 中移除 Pod。
	// 如果未设置，则表示未请求对象的优雅删除。
	//
	// DeletionTimestamp 由系统在请求优雅删除时填充，且只读。
	// 更多信息：https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata
	DeletionTimestamp *Time `json:"deletionTimestamp,omitempty" protobuf:"bytes,9,opt,name=deletionTimestamp"`

	// 允许该对象优雅终止的秒数，在此之后将从系统中删除。
	// 仅在 deletionTimestamp 设置时才会设置。
	// 该字段只能缩短，且只读。
	DeletionGracePeriodSeconds *int64 `json:"deletionGracePeriodSeconds,omitempty" protobuf:"varint,10,opt,name=deletionGracePeriodSeconds"`

	// Labels（标签）是资源对象非常重要的一个属性，可用于组织和分类（范围和选择）对象。
	// 在 Kubernetes 中，我们可以基于标签来查询资源，也即指定 labelSelector。
	// 更多信息：https://kubernetes.io/docs/concepts/overview/working-with-objects/labels
	Labels map[string]string `json:"labels,omitempty" protobuf:"bytes,11,rep,name=labels"`

	// Annotations（注解）是资源对象非常重要的一个属性，用来给资源对象添加额外的字段属性。
	// 外部工具可根据需要设置任意元数据。
	// 你可以将 Annotations 理解为实验性质的 Spec 定义。如果 Annotations 某个键，
	// 后来证明是资源刚需的字段，那么可以靠谱将键以 Spec 字段的方式在资源对象中定义
	// 该字段不能被查询。
	// 更多信息：https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations
	Annotations map[string]string `json:"annotations,omitempty" protobuf:"bytes,12,rep,name=annotations"`

	// 此对象依赖的对象列表。如果列表中的所有对象都被删除， 则该对象将被垃圾回收。
	// 如果该对象由控制器管理，则列表中的一个条目将指向该控制器，且该控制器字段设置为 true。
	// 不能有多个管理控制器。
	OwnerReferences []OwnerReference `json:"ownerReferences,omitempty" patchStrategy:"merge" patchMergeKey:"uid" protobuf:"bytes,13,rep,name=ownerReferences"`

	// 在 Kubernetes 中，Finalizers（最终器）是一种机制，用于在资源被删除之前执行特定的清理操作。
	// 它们允许开发者确保在 Kubernetes 资源的删除过程中，执行一些必要的步骤或逻辑，以便妥善处理依赖关系或释放资源。
	// 如果对象的 deletionTimestamp 非空，则此列表中的条目只能被移除。
	// Finalizers 可以以任何顺序处理和移除。未强制执行顺序，因为这会引入严重的风险，导致最终器阻塞。
	// finalizers 是共享字段，任何拥有权限的参与者都可以重新排序。
	// 如果按顺序处理 finalizer 列表，则可能导致第一个最终器负责的组件在等待某个信号时阻塞，
	// 该信号由负责列表中后一个 finalizer 的组件产生，导致死锁。
	// 在未强制顺序的情况下，finalizers 可以自行排序并不会受到列表顺序变化的影响。
	Finalizers []string `json:"finalizers,omitempty" patchStrategy:"merge" protobuf:"bytes,14,rep,name=finalizers"`

	// Tombstone：ClusterName 是一个遗留字段，系统会始终将其清除且从未使用。
	// ClusterName string `json:"clusterName,omitempty" protobuf:"bytes,15,opt,name=clusterName"`

	// 在 Kubernetes 中，ManagedFields（管理字段）是一种机制，用于追踪特定字段的更新和管理状态。
	// 这一机制主要用于为对象的不同字段提供版本控制和管理信息，主要用于内部管理，
	// 用户通常不需要设置或理解该字段。  
	ManagedFields []ManagedFieldsEntry `json:"managedFields,omitempty" protobuf:"bytes,17,rep,name=managedFields"`
}
```

metav1.ObjectMeta 包含了很多资源对象的元数据信息，这些元数据信息在 Kubernetes 中经常被用到，例如：通过 Annotations 来给 Kubernetes 资源打上标注，Controller 会根据这些标注实现自定义的逻辑。又比如，我们删除资源时，需要指定资源的 Namespace 和 Name。

### metav1.ObjectMeta 字段详细介绍

[metav1.ObjectMeta](https://github.com/kubernetes/kubernetes/blob/release-1.33/staging/src/k8s.io/apimachinery/pkg/apis/meta/v1/types.go#L111) 中包含了资源对象的标准元数据信息。以下是 metav1.ObjectMeta结构体中包含的元数据信息的解释：

```go
// ObjectMeta 保存所有持久化资源必须包含的元数据。  
// 下面对每个字段含义逐一说明（已去掉 json / protobuf 等标签）。  
type ObjectMeta struct {  
	// Name 是对象在同一命名空间内唯一的名称。  
	// 创建时通常必填，用于幂等与配置声明；创建后不可修改。  
	Name string  

	// GenerateName 是服务器在 Name 未指定时生成唯一名称的前缀。  
	// 返回给客户端的最终名称会在该前缀后附加随机后缀；若生成后的名称冲突将返回 409。  
	GenerateName string  

	// Namespace 决定名称的作用域。不填写等同于 "default" 命名空间。  
	// 并非所有资源都需要归属命名空间；集群级资源该字段为空。  
	Namespace string  

	// SelfLink（已废弃）曾用于存放对象的完整访问路径，现已停止填充。  
	SelfLink string  

	// UID 是对象在时间与空间上的唯一标识，成功创建后由服务器分配；PUT 不可变。  
	UID types.UID  

	// ResourceVersion 表示对象的内部版本号，用于并发控制、变更检测及 watch。  
	// 客户端需视为不透明字符串，原样回传。  
	ResourceVersion string  

	// Generation 表示期望状态（Spec）的版本号，每次用户修改 Spec 时自动递增。  
	Generation int64  

	// CreationTimestamp 表示对象在服务器上的创建时间（UTC，RFC3339 格式）。  
	// 由系统填充，客户端只读；列表对象中为 null。  
	CreationTimestamp Time  

	// DeletionTimestamp 指定资源将被删除的时间（优雅删除场景）。  
	// 设置后需等待 Finalizers 清空才真正删除；若为空代表未请求删除。  
	DeletionTimestamp *Time  

	// DeletionGracePeriodSeconds 与 DeletionTimestamp 配合，表示优雅终止的宽限秒数。  
	// 仅在 DeletionTimestamp 非空时设置，只允许缩短。  
	DeletionGracePeriodSeconds *int64  

	// Labels 是键值对，用于对象的分组与选择（label 选择器）。  
	Labels map[string]string  

	// Annotations 也是键值对，用于存放非查询型的附加信息，由外部工具自由读写。  
	Annotations map[string]string  

	// OwnerReferences 记录该对象依赖的父对象列表，全部父对象删除后本对象可被垃圾回收。  
	// 若由控制器管理，其中一个引用的 controller 字段会为 true；同一对象最多一个控制器。  
	OwnerReferences []OwnerReference  

	// Finalizers 在删除前必须被清空的标记列表，用于实现外部/级联清理逻辑。  
	// 条目顺序不保证，任何拥有权限的主体都可增删重排。  
	Finalizers []string  

	// ManagedFields 记录不同「工作流」对对象字段的管理关系，  
	// 用于服务器端 Apply 等变更合并逻辑。一般用户无需关心。  
	ManagedFields []ManagedFieldsEntry  
}
```

### metav1.ObjectType 的接口实现

metav1.ObjectType 是 [metav1.Object](https://github.com/kubernetes/kubernetes/blob/v1.29.2/staging/src/k8s.io/apimachinery/pkg/apis/meta/v1/meta.go#L33) 接口的具体实现。metav1.Object 接口定义如下：

```go
type Object interface {
    GetNamespace() string
    SetNamespace(namespace string)
    GetName() string 
    SetName(name string)
    GetGenerateName() string
    SetGenerateName(name string)
    GetUID() types.UID
    SetUID(uid types.UID)
    GetResourceVersion() string
    SetResourceVersion(version string)
    GetGeneration() int64
    SetGeneration(generation int64)
    GetSelfLink() string
    SetSelfLink(selfLink string)
    GetCreationTimestamp() Time
    SetCreationTimestamp(timestamp Time)
    GetDeletionTimestamp() *Time
    SetDeletionTimestamp(timestamp *Time)
    GetDeletionGracePeriodSeconds() *int64
    SetDeletionGracePeriodSeconds(*int64)
    GetLabels() map[string]string
    SetLabels(labels map[string]string)
    GetAnnotations() map[string]string
    SetAnnotations(annotations map[string]string)
    GetFinalizers() []string
    SetFinalizers(finalizers []string)
    GetOwnerReferences() []OwnerReference
    SetOwnerReferences([]OwnerReference)
    GetManagedFields() []ManagedFieldsEntry
    SetManagedFields(managedFields []ManagedFieldsEntry)
}
```

在 Kubernetes 开发中，我们经常需要在代码中获取资源对象的 Namespace、Name、Labels、Annotations 等属性，如果操作对象是一个 Deployment/*Deployment 类型的对象，我们可以直接引用其字段值即可，例如：

```
obj := &Deployment{}
namespace := obj.ObjectMeta.Namespace
name := obj.ObjectMeta.Name
```

但， 如果操作对象是 runtime.Object 类型的类型呢？在 Kubernetes 开发中，我们可以使用 [meta.Accessor](https://github.com/kubernetes/kubernetes/blob/v1.29.2/staging/src/k8s.io/apimachinery/pkg/api/meta/meta.go#L96) 方法来从 runtime.Object 类型对象中，获取 metav1.Object 接口对象，例如：

```
deploy := &Deployment{}
var obj runtime.Object = deploy

accessor, _ := meta.Accessor(obj)
namespace := accessor.GetNamespace()
name := accessor.GetName()
```

[meta.Accessor](https://github.com/kubernetes/apimachinery/blob/v0.30.4/pkg/api/meta/meta.go#L96) 方法，在 Kubernetes 源码中，也经常被使用到，是一个非常实用和常用的方法。meta.Accessor 函数实现如下：

```go
func Accessor(obj interface{}) (metav1.Object, error) {
    switch t := obj.(type) {
    case metav1.Object:
        return t, nil
    case metav1.ObjectMetaAccessor:
        if m := t.GetObjectMeta(); m != nil {
            return m, nil
        }   
        return nil, errNotObject
    default:    
        return nil, errNotObject
    }           
}
```

可以 meta.Accessor 函数其实是借助了 Go 的类型断言来获取具体类型的。

提示：可以看到很多 Kubernetes 的核心结构都是在 k8s.io/apimachinery 定义的，所以，我们也可以称apimachinery 包为“API 类型构建的基础设施”

## 总结

本节课，详细介绍了 Kubernetes 中的核心对象，及源码实现。Kubernets 资源对象格式如下：

<img src="image/Fhqb99-K2Xz7JuScDgqLrzLRuEM7" alt="img" style="zoom: 50%;" />

在 Kuberentes 中有很多内置资源和自定义资源，这些资源有一些通用的处理逻辑，为了能够使用同一份代码，对不同资源执行相同的程序逻辑。Kubernetes 还定义了不同的 Go 接口，用来处理资源对象中的不同部分：

1. runtime.Object：用来操作 metav1.TypeMeta域及资源对象的复制；
2. schema.ObjectKind：用来操作metav1.TypeMeta域；
3. metav1.Object：用来操作 metav1.ObjectMeta域。

这些对象之间的关系如下：

![img](image/FgC5bXx-9L_NqykqCNW3_RiVyTaT)