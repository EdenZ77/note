上一节课，我介绍了 Kubernetes 在设计 RESTful API 接口时的一些特点。接下来几节课，我们通过源码来看下 Kubernetes 具体是如何设计 RESTful API 的。

设计 RESTful API 的第一个核心步骤便是定义 REST 资源对象。Kubernetes 这么大的源码量级，能够快速迭代、维护的其中一个核心原因就是 Kubernetes 的很多功能设计是标准化的，包括 REST 资源对象的设计。

本节课，我们来看下 Kubernetes 是如何定义标准化的 Kubernetes 资源对象。

## 定义标准化的 Kubernetes 资源对象

开发 API 接口的第一步就是设计 API 接口。所谓的设计 API 接口，一般包含以下 3 大类工作：

- 定义 API 接口的参数（资源定义）：根据 API 接口的功能，会定义该 API 接口的各种参数，这些参数包括路径参数、查询参数、请求体等；
- 定义 API 接口的请求方法：请求方法一般包括 POST、PUT、GET、DELETE、PATCH 等；
- 定义 API 接口的请求路径：我们需要根据 REST 规范来指定 API 接口的请求路径，例如：`/apis/batch/v1/cronjobs`。

在 Kubernetes 中，开发 API 接口最核心的第一步是资源定义。另外 2 项：API 接口的请求方法、请求路径在 Kubernetes 中都会根据资源定义自动生成。

资源定义是一个开发动作，定义出来的资源在 Kubernetes 也叫资源对象，在 Kubernetes 中其实就是一个 Go 结构体。 ~~这个 Go 结构体对象我们也可以称之为 Kubernetes （资源）对象（其实就是一种编程对象）。~~

本小节，我就通过介绍 Kubernetes 的资源对象，来给你介绍下 Kubernetes 是如何定义标准化的资源对象的，也即如何定义 REST API 接口的。

## Kubernetes 对象的官方定义

这里，我们来看下 Kubernetes 对象的官方定义。在 Kubernetes 系统中，Kubernetes 对象是持久化的实体。Kubernetes 使用这些实体去表示整个集群的状态。

Kubernetes 对象具有以下 3 个特点：

- Kubernetes 对象是 “意向表达”： 一旦创建对象，Kubernetes 系统将持续工作以确保对象存在。通过创建对象，本质上是在告知 Kubernetes 系统，所需要的工作负载看起来是什么样子， 这就是 Kubernetes 集群的期望状态（Desired State）；
- 需要使用 Kubernetes API 操作 Kubernetes 对象：无论是创建、修改或者删除，都需要使用 Kubernetes API。比如，当使用 kubectl 命令行接口（CLI）时，CLI 会调用必要的 Kubernetes API；也可以在程序中使用客户端库（client-go），来直接调用 Kubernetes API；
- 对象具有一定的格式：Kubernetes 中的所有资源对象都具有固定的格式，后面会详细介绍。

## Kubernetes 资源对象的标准格式

Kubernetes 中，几乎所有的 Kubernetes 对象都具有固定的格式，都包含以下 4 类信息：类型元数据、资源元数据、资源期望状态定义、资源当前状态定义。例如，Kubernetes 的核心资源对象 Deployment 的资源定义如下：

```go
// staging/src/k8s.io/api/apps/v1/types.go
import (
    metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +genclient
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

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

// DeploymentSpec is the specification of the desired behavior of the Deployment.
type DeploymentSpec struct {
    // replicas is the number of desired pods. This is a pointer to distinguish between explicit
    // zero and not specified. Defaults to 1.
    // +optional
    Replicas *int32 `json:"replicas,omitempty" protobuf:"varint,1,opt,name=replicas"`

    // selector is the label selector for pods. Existing ReplicaSets whose pods are
    // selected by this will be the ones affected by this deployment.
    // +optional
    Selector *metav1.LabelSelector `json:"selector,omitempty" protobuf:"bytes,2,opt,name=selector"`

    // Template describes the pods that will be created.
    // The only allowed template.spec.restartPolicy value is "Always".
    Template v1.PodTemplateSpec `json:"template" protobuf:"bytes,3,opt,name=template"`
    ...
}

// DeploymentStatus is the most recently observed status of the Deployment.
type DeploymentStatus struct {
    // observedGeneration is the generation observed by the deployment controller.
    // +optional
    ObservedGeneration int64 `json:"observedGeneration,omitempty" protobuf:"varint,1,opt,name=observedGeneration"`

    // replicas is the total number of non-terminated pods targeted by this deployment (their labels match the selector).
    // +optional
    Replicas int32 `json:"replicas,omitempty" protobuf:"varint,2,opt,name=replicas"`

    // updatedReplicas is the total number of non-terminated pods targeted by this deployment that have the desired template spec.
    // +optional
    UpdatedReplicas int32 `json:"updatedReplicas,omitempty" protobuf:"varint,3,opt,name=updatedReplicas"`

    // readyReplicas is the number of pods targeted by this Deployment controller with a Ready Condition.
    // +optional
    ReadyReplicas int32 `json:"readyReplicas,omitempty" protobuf:"varint,7,opt,name=readyReplicas"`
    ...
}
```

在有些资源对象中，没有 Status 字段。上述 Deployment 资源定义其实也反应了 Kubernetes 资源对象的基本格式，其定义格式为：

```go
// 参考：staging/src/k8s.io/api/core/v1/types.go
package resourceobject

import (
    metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +enum
type XXXPhase string

// These are the valid phases of a xxx.
const (
    // XXXRunning means the xxx is in the running state.
    XXXRunning XXXPhase = "Running"
    // XXXPending means the xxx is in the pending state.
    XXXPending XXXPhase = "Pending"
)

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

// XXXSpec describes the attributes on a XXX.
type XXXSpec struct {
    // DisplayName is the display name of the XXX resource.
    DisplayName string `json:"displayName" protobuf:"bytes,1,opt,name=displayName"`

    // Description provides a brief summary of the XXX's purpose or functionality.
    // It helps users understand what the XXX is intended for.
    // +optional
    Description string `json:"description,omitempty" protobuf:"bytes,2,opt,name=description"`

    // You can add more status fields as needed.
    // +optional
}

// XXXStatus is information about the current status of a XXX.
type XXXStatus struct {
    // Phase is the current lifecycle phase of the xxx.
    // +optional
    Phase XXXPhase `json:"phase,omitempty" protobuf:"bytes,1,opt,name=phase,casttype=NamespacePhase"`

    // ObservedGeneration reflects the generation of the most recently observed XXX.
    // +optional
    ObservedGeneration int64 `json:"observedGeneration,omitempty" protobuf:"varint,2,opt,name=observedGeneration"`

    // Represents the latest available observations of a xxx's current state.
    // +optional
    // +patchMergeKey=type
    // +patchStrategy=merge
    // +listType=map
    // +listMapKey=type
    Conditions []metav1.Condition `json:"conditions,omitempty" patchStrategy:"merge" patchMergeKey:"type" protobuf:"bytes,3,rep,name=conditions"`

    // You can add more status fields as needed.
    // +optional
}

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// XXXList is a list of XXX objects.
type XXXList struct {
    metav1.TypeMeta `json:",inline"`
    // Standard list metadata.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata
    // +optional
    metav1.ListMeta `json:"metadata,omitempty" protobuf:"bytes,1,opt,name=metadata"`

    // Items is a list of schema objects.
    Items []XXX `json:"items" protobuf:"bytes,2,rep,name=items"`
}
```

> 提示：如果你想开发一个 Kuberentes 资源，可以参考上述格式。直接复制，替换掉 XXX 即可。
>

上述资源定义代码中，XXX 是资源名，通过 `type XXX struct` 语法定义了资源拥有的属性。Kubernetes 资源定义中，有以下 4 个标准属性：

- metav1.TypeMeta（必须）：内嵌字段，定义了资源使用的资源组、资源版本（apiVersion）和资源类型（kind）；
- metav1.ObjectMeta（必须）：资源的元数据，包括名称、命名空间、标签和注释等；
- Spec（必须）： 对象规约，定义对象的期望状态（Desired State），该字段描述了对象的配置和期望状态，其中的字段可以根据需要制定；
- Status（可选）：对象状态，用来描述对象的当前状态（Current State），Status 字段通常由 Kubernetes 的各种 Controller 根据对象的当前状态来设置并更新，用户不需要手动设置。

TypeMeta、ObjectMeta、Spec、Status 字段的名字是固定的。Spec 对应的结构体类型名为格式为 `<资源名>Spec`，Status 对应的结构体类型名格式为 `<资源名>Status`。

XXXStatus 通常包含以下字段：

- Phase：指示资源的当前生命周期阶段，例如 Pending、Running、Succeeded、Failed、Unknown。Phase 字段很常用，但也有很多资源的 XXXStatus 中不需要 Phase 字段。Phase 字段的类型通常可以为 string，但更多的时候，是自定义类型，例如 XXXPhase；
- ObservedGeneration：让 `控制器 (Controller)`能够清晰地标示出：当前资源的 `.status`中所描述的 `实际状态 (Observed State / Current State)`是针对哪一个 `期望状态 (Desired State / Spec)`的版本（Generation）计算和处理出来的。简单说，它就是控制器对资源 Spec 变更的“响应确认标记”。
- Conditions：用于提供资源当前状态的详细信息，让开发者能够更好地理解和监控资源的健康状况。

在定义 XXXStatus 结构体中的 Conditions 字段是一个数组字段，在执行 PATCH 操作时，通常的 Patch 策略为 Merge，也即合并新旧资源的 Conditions 数组中的元素。为了告诉 kube-apiserver 合并 Condition 数组，我们打上指定的结构体标签和注释，例如：

```go
    // Represents the latest available observations of a xxx's current state.
    // +optional
    // +patchMergeKey=type
    // +patchStrategy=merge
    // +listType=map
    // +listMapKey=type
    Conditions []metav1.Condition `json:"conditions,omitempty" patchStrategy:"merge" patchMergeKey:"type" protobuf:"bytes,3,rep,name=conditions"`
```

Kubernetes 中通过各类 `// +` 注释和结构体标签，来控制代码生成器的行为。详见下面小节。

在上面的资源定义中，你还可以看到资源定义注释上面会有一些 `// +k8s:` 注释，这些注释是 Kubernetes code-generator 组件用来生成代码时用的，后面会详细介绍。

上述 XXX资源 JSON 格式的定义示例如下：

```json
{
  "apiVersion": "example.com/v1",
  "kind": "XXX",
  "metadata": {
    "resourceVersion": "12345",
    "name": "example-xxx",
    "namespace": "default",
    "uid": "abc123-xyz456",
    "creationTimestamp": "2023-10-01T00:00:00Z"
  },
  "spec": {
    "displayName": "Example XXX",
    "description": "This is an example definition of a Kubernetes resource object."
  },
  "status": {
    "phase": "Running",
    "observedGeneration": 1,
    "conditions": [
      {
        "type": "Ready",
        "status": "True",
        "lastProbeTime": "2023-10-01T00:00:00Z",
        "lastTransitionTime": "2023-10-01T00:00:00Z",
        "reason": "Initialized",
        "message": "The resource is ready."
      }
    ]
  }
}
```

最后，在定义好资源对象之后，还需要在定义一个资源的列表对象，用来表示资源的列表。列表对象定义的固定格式如下：

```go
// XXXList is a list of XXX objects.
type XXXList struct {
    metav1.TypeMeta `json:",inline"`
    // Standard list metadata.
    // More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#metadata
    // +optional
    metav1.ListMeta `json:"metadata,omitempty" protobuf:"bytes,1,opt,name=metadata"`

    // Items is a list of schema objects.
    Items []XXX `json:"items" protobuf:"bytes,2,rep,name=items"`
}
```

XXXList JSON 格式表示见文件：[xxxlist.json](https://github.com/onexstack/kubernetes-examples/blob/master/resourcedefinition/xxxlist.json)。

最后，值得注意的是，在 Kubernetes 中 XXX、XXXList、TypeMeta、ObjectMeta 等结构体或字段的注释格式也是相似的。在你定义资源的时候，注释可以参考其它资源对象的注释来写。

## 注释与结构体标签详解

### 注释

- `// +patchMergeKey=type`

  解释：这是关于如何合并该字段的标记。在Kubernetes中，当使用 JSON Patch（如 kubectl apply ）更新资源时，需要知道如何合并数组字段。patchMergeKey 指定了数组中对象的主键。这里设置为`type`，意味着 Conditions 数组中每个条件（Condition）通过`type`字段来唯一标识。也就是说，当合并时，会根据条件类型（type）来匹配同一个条件，然后进行合并。

- `// +patchStrategy=merge`

  解释：这个标记指定了在合并数组时使用的策略。`merge`表示对于数组中的元素，按照指定的 merge key（即上面的 type）进行匹配并合并。如果找到了匹配的元素（即相同 type），则合并这两个元素的字段；如果没有找到，则添加新的元素。这区别于`replace`策略。

**步骤 1：初始状态 (存储在 etcd 中的当前状态)**

```yaml
apiVersion: example.com/v1
kind: MyApp
metadata:
  name: my-app
  generation: 1 # 初始生成版本
status:
  conditions:
    - type: Ready               # 条件类型1：整体就绪
      status: "True"            # 状态：就绪
      reason: AllComponentsUp   # 原因
      message: Application is fully operational
      lastTransitionTime: "2023-10-25T10:00:00Z"
    - type: DatabaseConnected   # 条件类型2：数据库连接
      status: "True"            # 状态：已连接
      reason: PingSuccess       # 原因
      message: Successfully connected to primary DB
      lastTransitionTime: "2023-10-25T10:00:00Z"
```

**步骤 2：用户提交更新 (期望状态变更)**

用户发现数据库连接有问题，决定增加一个连接池大小的配置（修改了 `spec`）。用户使用 `kubectl apply -f new-config.yaml`提交更新。

```yaml
apiVersion: example.com/v1
kind: MyApp
metadata:
  name: my-app
spec: # 用户修改了 spec 的某些字段，比如增加了 connectionPoolSize
  connectionPoolSize: 20
  # ... 其他 spec 配置 ...
# status: # 用户通常不会在 apply 时提交 status，status 由控制器管理
```

Kubernetes API Server 处理：

1. 检测到 `spec`变更，将 `metadata.generation`增加到 `2`。
2. 触发控制器进行调和 (`Reconcile`)。

**步骤 3：控制器调和过程 (更新 status)**

控制器开始工作：

1. 检测到 `metadata.generation=2`> `status.observedGeneration=1`，知道需要调和。
2. 执行逻辑：尝试连接到数据库。
3. 发现数据库连接失败！
4. 控制器需要更新 `status.conditions`来反映这个新状态。

控制器计算出的新 `status.conditions`应该是：

```yaml
status:
  conditions:
    - type: Ready               # 整体就绪状态应该变为 False，因为数据库挂了
      status: "False"           # 状态：不就绪
      reason: DatabaseDown      # 新原因
      message: Application unavailable due to database connection failure
      lastTransitionTime: "2023-10-25T10:05:00Z" # 状态转变时间更新
    - type: DatabaseConnected   # 数据库连接状态变为 False
      status: "False"           # 状态：未连接
      reason: ConnectionTimeout # 新原因
      message: Failed to connect to database within timeout
      lastTransitionTime: "2023-10-25T10:05:00Z" # 状态转变时间更新
  observedGeneration: 2        # 标记此状态是针对 generation=2 的 spec
```

**关键点：控制器如何更新 `status.conditions`？**

控制器不会直接发送整个 `MyApp`对象的新定义给 API Server。它发送的是一个 `PATCH`请求，只包含需要更新的部分（`status`）。这个 `PATCH`请求的负载大致如下：

```json
{
  "status": {
    "conditions": [
      {
        "type": "Ready", // patchMergeKey=type 用于匹配
        "status": "False",
        "reason": "DatabaseDown",
        "message": "Application unavailable due to database connection failure",
        "lastTransitionTime": "2023-10-25T10:05:00Z"
      },
      {
        "type": "DatabaseConnected", // patchMergeKey=type 用于匹配
        "status": "False",
        "reason": "ConnectionTimeout",
        "message": "Failed to connect to database within timeout",
        "lastTransitionTime": "2023-10-25T10:05:00Z"
      }
    ],
    "observedGeneration": 2
  }
}
```

**API Server 应用 `PATCH`时的合并过程 (`patchStrategy=merge`+ `patchMergeKey=type`)：**

1. API Server 读取当前存储在 etcd 中的 `MyApp`对象（步骤 1 的状态）。
2. 它定位到要更新的字段：`status.conditions`。
3. `patchStrategy=merge`告诉它要合并数组。
4. `patchMergeKey=type`告诉它使用 `type`字段作为匹配键。
5. 对于 `PATCH`负载中的每个条件对象：
   - 查找匹配项： 在现有的 `status.conditions`数组中查找具有相同 `type`的条目。
     - 找到 `type: Ready`的现有条目。
     - 找到 `type: DatabaseConnected`的现有条目。
   - 合并： 用 `PATCH`负载中对应条目的字段值 **更新** 找到的现有条目的字段值。
     - 更新 `Ready`条目的 `status`, `reason`, `message`, `lastTransitionTime`。
     - 更新 `DatabaseConnected`条目的 `status`, `reason`, `message`, `lastTransitionTime`。
6. 最终合并后的 `status.conditions`就是步骤 3 中控制器计算出的新状态。
7. 同时更新 `status.observedGeneration`为 `2`。

**步骤 4：另一个控制器更新部分状态 (更复杂的合并)**

假设还有一个独立的 `DatabaseMonitor`控制器，它专门监控数据库连接状态，并负责更新 `DatabaseConnected`条件。它检测到数据库恢复了！

`DatabaseMonitor`控制器计算出的 `status.conditions`更新 (它只关心数据库状态)：

```yaml
status:
  conditions:
    - type: DatabaseConnected   # 只更新这个条件
      status: "True"            # 状态：恢复连接
      reason: Reconnected       # 新原因
      message: Database connection re-established
      lastTransitionTime: "2023-10-25T10:10:00Z" # 状态转变时间更新
```

`DatabaseMonitor`发送的 `PATCH`请求负载：

```json
{
  "status": {
    "conditions": [
      {
        "type": "DatabaseConnected", // 只包含要更新的条件
        "status": "True",
        "reason": "Reconnected",
        "message": "Database connection re-established",
        "lastTransitionTime": "2023-10-25T10:10:00Z"
      }
    ]
    // 它可能不更新 observedGeneration，因为不是主控制器
  }
}
```

**API Server 应用此 `PATCH`时的合并过程：**

1. 读取当前状态 (步骤 3 更新后的状态，其中 `Ready=False`, `DatabaseConnected=False`)。

2. 定位 `status.conditions`。

3. 使用 `patchStrategy=merge`和 `patchMergeKey=type`。

4. 在 `PATCH`负载中找到 `type: DatabaseConnected`。

5. 在现有 `conditions`数组中找到匹配的 `type: DatabaseConnected`条目。

6. 仅更新该匹配条目的字段 (`status`, `reason`, `message`, `lastTransitionTime`) 为 `PATCH`负载中的新值。

7. `Ready`条件 (`type: Ready`) 没有被包含在 `PATCH`负载中，因此它保持不变！

8. 最终状态变为：

   ```yaml
   status:
     conditions:
       - type: Ready
         status: "False"           # 未改变！主控制器还没更新它
         reason: DatabaseDown
         message: Application unavailable due to database connection failure
         lastTransitionTime: "2023-10-25T10:05:00Z"
       - type: DatabaseConnected   # 已更新
         status: "True"
         reason: Reconnected
         message: Database connection re-established
         lastTransitionTime: "2023-10-25T10:10:00Z"
     observedGeneration: 2          # 未改变
   ```

**如果没有 `patchStrategy=merge`会怎样？**

如果注释是 `// +patchStrategy=replace`（或者没有明确指定，某些情况下默认可能是 replace）：

- 当控制器在步骤 3 发送包含两个条件的 `PATCH`时，它会 **完全替换** 整个 `status.conditions`数组。结果是预期的。

- 但当 `DatabaseMonitor`在步骤 4 发送只包含 `DatabaseConnected`条件的 `PATCH`时：

  - `replace`策略会用 `PATCH`负载中的新数组完全替换现有的 `status.conditions`。

  - 新数组只包含 `DatabaseConnected`条件。

  - **结果：`Ready`条件丢失了！** 状态变为：

    ```yaml
    status:
      conditions: # 只剩下 DatabaseConnected 了！
        - type: DatabaseConnected
          status: "True"
          ...
      observedGeneration: 2 # 可能也被覆盖或丢失
    ```

  - 这显然是错误的，丢失了重要的状态信息。



我们来深入解释 `// +listType=map` 和 `// +listMapKey=type` 这两个标记的作用、意义以及它们带来的约束。

这两个标记是 Kubernetes API 机器生成（Kubernetes API Machinery）和模式验证（Schema Validation）层的关键指令。它们共同作用于对象的数组（`slice`/`array`）字段（如 `status.conditions`），目的是：

1. **`// +listType=map`：**
   - 声明底层存储结构： 它告诉 Kubernetes API 服务器和验证逻辑，这个数组字段在逻辑上和操作上应该被视为一个映射（Map），而不仅仅是一个简单的列表。
   - 强调键的唯一性： 这意味着数组中的元素需要通过某个特定的字段**唯一标识**。这个字段就是由 `listMapKey`指定的。在同一个数组中，不允许出现具有相同键值的两个元素。这是与普通列表的关键区别。
2. **`// +listMapKey=type`：**
   - 指定唯一键字段： 它明确指出使用数组元素结构体中的哪个字段作为这个逻辑映射的键（Key）。在这个例子中，指定的是 `type`字段。
   - 强制唯一性约束： 这是实现 `listType=map`语义的关键。它强制要求在同一个 `status.conditions`数组中，所有元素 `type`字段的值必须互不相同。如果有两个元素的 `type`相同，无论是通过 `CREATE`还是 `UPDATE`（包括 `PATCH`和 `PUT`）操作尝试写入，Kubernetes API 服务器都会拒绝该请求，并返回一个验证错误。



### 结构体标签

|                   标签                   |                      作用                       |
| :--------------------------------------: | :---------------------------------------------: |
|      `json:"conditions,omitempty"`       |          JSON序列化字段名，空值时隐藏           |
| `protobuf:"bytes,3,rep,name=conditions"` | ProtoBuf编码设置： - 字段ID: 3 - 类型: 字节数组 |
|         `patchStrategy:"merge"`          |           与注释 `+patchStrategy`一致           |
|          `patchMergeKey:"type"`          |           与注释 `+patchMergeKey`一致           |



### 两者对比

在 Kubernetes API 定义代码中，**注释 (`// +...`)** 和 **结构体标签 (`patchStrategy:"merge"`)** 看起来是重复的。这并非冗余，而是因为它们服务于 **不同阶段** 和 **不同组件** 的 Kubernetes API 处理流程。它们相辅相成，缺一不可。

1. **注释 (`// +patchStrategy=merge`, `// +patchMergeKey=type`):**
   - 受众/作用对象： Kubernetes 的代码生成器工具链，例如 `deepcopy-gen`, `conversion-gen`, `defaulter-gen`, `openapi-gen`, `client-gen`, `lister-gen`, `informer-gen`等。
   - 作用阶段： 代码生成期间 (Build-time)。
   - 目的： 告诉代码生成器如何生成 API 对象的辅助代码、类型定义、OpenAPI/Swagger 文档。
2. **结构体标签 (`patchStrategy:"merge"`, `patchMergeKey:"type"`):**
   - 受众/作用对象： Kubernetes API Machinery 库 (APIMachinery)，特别是负责序列化/反序列化（处理 JSON/YAML）和 `PATCH`操作的运行时逻辑。
   - 作用阶段： 运行时 (Runtime)。
   - 目的： 当 API Server 的 HTTP 层接收到一个请求（尤其是 `PATCH`请求如 `application/merge-patch+json`）时，或者在将收到的 JSON/YAML 数据解码成 Go 结构体时：
     - 序列化库（如 `encoding/json`或 Kubernetes 增强版本）会通过反射读取这些结构体标签。
     - APIMachinery 依赖这些标签来决定如何处理 `Conditions`字段的合并。



## 总结

借助 `// +k8s:` 及 patch 注解，code‑generator 能自动为这些结构体生成 DeepCopy、客户端、合并策略等配套代码，同时据此派生 API 路径和请求方法。每个资源还需提供对应的 XXXList 列表对象（TypeMeta+ListMeta+Items）。正是这套标准化模型，保证了 Kubernetes API 的一致性、可自动化生成及快速迭代。