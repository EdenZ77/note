上一节课，我介绍了 Kubernets 设计功能门控的目的、使用方式、生命周期，以及如何在代码中使用功能门控。看完之后，其实你还不知道具体如何定义并使用一个功能门控。本小节，我就通过一个实战示例，来给你展示具体如何定义一个功能门控，并在代码中使用这个功能门控。并给你介绍 Kubernetes 中功能门控的使用方式。

另外，OneX 项目也借鉴了 Kubernetes 的 Feature Gates 设计，课程的最后，也会介绍下 OneX 项目中是如何使用 Feature Gates 特性的。

## 定义并使用 Feature Gates

我们可以通过以下几步来定义并使用一个新的功能门控：

1. 定义功能门控；
2. 在代码中使用功能门控；

本小节，示例源码为：https://github.com/superproj/k8sdemo/tree/master/featuregates。

### 步骤 1：定义功能门控

定义功能门控，又分为以下几步：

1. 添加自定义功能门控列表；
2. 新增功能门控；
3. 注册功能门控。

#### 1. 创建自定义功能门控列表

新建文件  featuregates/feature/feature_gate.go，内容如下：

```go
package feature


import (
    "k8s.io/component-base/featuregate"
)


var (
    // DefaultMutableFeatureGate is a mutable version of DefaultFeatureGate.
    // Only top-level commands/options setup and the k8s.io/component-base/featuregate/testing package should make use of this.
    // Tests that need to modify feature gates for the duration of their test should use:
    //   defer featuregatetesting.SetFeatureGateDuringTest(t, utilfeature.DefaultFeatureGate, features.<FeatureName>, <value>)()
    DefaultMutableFeatureGate featuregate.MutableFeatureGate = featuregate.NewFeatureGate()


    // DefaultFeatureGate is a shared global FeatureGate.
    // Top-level commands/options setup that needs to modify this feature gate should use DefaultMutableFeatureGate.
    DefaultFeatureGate featuregate.FeatureGate = DefaultMutableFeatureGate
)
```

上述代码，创建了 2 个包级别的变量：

1. DefaultMutableFeatureGate：可写的 Feature Gates，用来在应用启动时，设置 Feature Gates。例如：读取命令行 Flag，并将 Flag 传入的 Feature Gate 的开启状态设置到 DefaultMutableFeatureGate全局变量中；
2. DefaultFeatureGate：只读的 Feature Gates，用来读取 Feature Gates 中注册的功能列表，并根据是否开启，进行功能开启/关闭。这里需要注意，DefaultFeatureGate是 DefaultMutableFeatureGate的一个子集，只暴露了读方法。

#### 2. 新增功能门控

新建文件 featuregates/feature/feature.go，内容如下：

```go
package feature


import (
    "k8s.io/apimachinery/pkg/util/runtime"
    "k8s.io/component-base/featuregate"
)


// Define a new feature gate.
const MyNewFeature featuregate.Feature = "MyNewFeature"


func init() {
    // runtime.Must(utilfeature.DefaultMutableFeatureGate.Add(defaultFeatureGates))
    runtime.Must(DefaultMutableFeatureGate.Add(defaultFeatureGates))
}


// defaultFeatureGates consists of all known specific feature keys.
// To add a new feature, define a key for it above and add it here.
var defaultFeatureGates = map[featuregate.Feature]featuregate.FeatureSpec{
    // owner: @colin404
    // Deprecated: v1.31
    //
    // An example feature gate.
    MyNewFeature: {Default: false, PreRelease: featuregate.Alpha},
}
```

k8s.io/component-base/featuregate 包提供了功能门控的核心实现和接口，允许开发者定义和管理功能门控。在上述代码中，我们使用了 k8s.io/component-base/featuregate 包中定义的 featuregate.Feature类型作为 map 的 key，featuregate.FeatureSpec 类型作为 map 的 value。featuregate.Feature类型的底层数据类型为 string。

上述代码，我们定义并注册了一个名为MyNewFeature的功能，其设置为：默认关闭（Default: false）、特性的成熟度为 Alpha（PreRelease: featuregate.Alpha）。

#### 3. 注册功能门控

上面，我们新增了一个功能门控，接下来，我们还需要将此功能门控添加到全局的可变功能门控中。

在 init()函数中，通过DefaultMutableFeatureGate.Add(defaultFeatureGates)方法调用，将我们新增的功能门控，添加到全局的可变功能门控中。defaultFeatureGates是我们自定义的功能门控列表。defaultFeatureGates是一个 map 类型的结构，其中 key 是功能门控名字，value 是功能门控的定义，在定义中指定了功能门控的默认值、所处的发布状态。

### 步骤 2：在代码中使用功能门控

上面，我们定义了一个新的功能门控。这里，我们再来看下具体如何使用新增的功能门控。添加 featuregates/main.go文件，内容如下：

```go
package main


import (
    "fmt"
    "os"


    "github.com/spf13/pflag"
    "github.com/superproj/k8sdemo/featuregates/feature"
)


func main() {
    // Create a new FlagSet for managing command-line flags
    fs := pflag.NewFlagSet("feature", pflag.ExitOnError)


    // Set the usage function to provide a custom help message
    fs.Usage = func() {
        fmt.Fprintf(os.Stderr, "Usage of %s:\n", os.Args[0])
        fs.PrintDefaults()
    }


    // Define a boolean flag for displaying help
    help := fs.BoolP("help", "h", false, "Show this help message.")


    // Add the feature gates to the flag set
    feature.DefaultMutableFeatureGate.AddFlag(fs)


    // Parse the command-line flags
    fs.Parse(os.Args[1:])


    // Display help message if the help flag is set
    if *help {
        fs.Usage()
        return
    }


    // Check if the MyNewFeature feature gate is enabled
    if feature.DefaultFeatureGate.Enabled(feature.MyNewFeature) {
        // Logic when the new feature is enabled
        fmt.Println("Feature Gates: MyNewFeature is opened")
    } else {
        // Logic when the new feature is disabled
        fmt.Println("Feature Gates: MyNewFeature is closed")
    }
}
```

上述代码，内容比较简单、易懂，我这里就不过多介绍，看注释不难理解。

运行上述命令输出如下：

```shell
$ go run main.go -h
Usage of /tmp/go-build224395384/b001/exe/main:
      --feature-gates mapStringBool   A set of key=value pairs that describe feature gates for alpha/experimental features. Options are:
                                      AllAlpha=true|false (ALPHA - default=false)
                                      AllBeta=true|false (BETA - default=false)
                                      MyNewFeature=true|false (ALPHA - default=false)
  -h, --help                          Show this help message.
$ go run main.go  --feature-gates=MyNewFeature=false
Feature Gates: MyNewFeature is closed
$ go run main.go  --feature-gates=MyNewFeature=true
Feature Gates: MyNewFeature is opened
```

可以看到，我们可以通过  --feature-gates来控制某个功能是否开启，非常方便。并且在执行 -h时，会输出当前注册的功能列表，开启状态及成熟度。

上面，我介绍了如何自定义 Feature Gates，并使用这些 Feature Gate，以此加深你对 Feature Gates 的了解。接下来，我们来看下 Kubernetes 项目是如何使用 Feature Gates 的。

## Kubernetes 中的功能门控实现

Kubernetes 中使用 Feature Gates 的方式和上述介绍的定义及使用方式大同小异。一些区别如下：

1. Kubernetes 中的 Feature Gates 列表更多。Kubernetes 支持的 Feature Gates 列表见：[pkg/features/kube_features.go](https://github.com/kubernetes/kubernetes/blob/master/pkg/features/kube_features.go#L28)。当然，你也可以执行 kube-apiserver -h查看 --feature-gates命令行 Flag 的描述，描述中详细列出了 kube-apiserver 支持的 Feature Gate；
2. kube-apiserver --feature-gates 描述中的 Feature Gate 列表，展示的 Feature Gate 名字，通过 :符合，进一步划分了 Feature Gate 的类别，例如： kube:APIResponseCompression=true|false (BETA - default=true)；
3. Kubernetes 中的 Feature Gate 在定义时，有严格的注释规范，例如：

```
    // owner: @thockin
    // deprecated: v1.29
    //
    // Enables Service.status.ingress.loadBanace to be set on
    // services of types other than LoadBalancer.
    AllowServiceLBStatusOnNonLB featuregate.Feature = "AllowServiceLBStatusOnNonLB"


    // owner: @bswartz
    // alpha: v1.18
    // beta: v1.24
    //
    // Enables usage of any object for volume data source in PVCs
    AnyVolumeDataSource featuregate.Feature = "AnyVolumeDataSource"
```

在 Feature Gate 定义时，通过注释说明了 Feature Gate 的功能、负责人、废弃版本、每个阶段所在的版本等信息。

### Kubernetes 中功能门控定义

Kubernetes 中有 2 个地方定义了功能门控：

1. staging/src/k8s.io/apiserver/pkg/features/kube_features.go：定义了一些通用的功能门控，这些功能会被多个 Kubernetes 组件共同使用；
2. pkg/features/kube_features.go：组件级别的功能门控，这些功能被某个组件使用，其他组件无法使用。组件级别的功能门控， 可以通过以下方式（可选的）来引用通用的功能门控：

```go
package features


import (
    apiextensionsfeatures "k8s.io/apiextensions-apiserver/pkg/features"
    "k8s.io/apimachinery/pkg/util/runtime"
    genericfeatures "k8s.io/apiserver/pkg/features"
    utilfeature "k8s.io/apiserver/pkg/util/feature"
    clientfeatures "k8s.io/client-go/features"
    "k8s.io/component-base/featuregate"
)




func init() {
    runtime.Must(utilfeature.DefaultMutableFeatureGate.Add(defaultKubernetesFeatureGates))
    runtime.Must(utilfeature.DefaultMutableFeatureGate.AddVersioned(defaultVersionedKubernetesFeatureGates))


    // Register all client-go features with kube's feature gate instance and make all client-go
    // feature checks use kube's instance. The effect is that for kube binaries, client-go
    // features are wired to the existing --feature-gates flag just as all other features
    // are. Further, client-go features automatically support the existing mechanisms for
    // feature enablement metrics and test overrides.
    ca := &clientAdapter{utilfeature.DefaultMutableFeatureGate}
    runtime.Must(clientfeatures.AddFeaturesToExistingFeatureGates(ca))
    clientfeatures.ReplaceFeatureGates(ca)
}


// defaultKubernetesFeatureGates consists of all known Kubernetes-specific feature keys.
// To add a new feature, define a key for it above and add it here. The features will be
// available throughout Kubernetes binaries.
//
// Entries are separated from each other with blank lines to avoid sweeping gofmt changes
// when adding or removing one entry.
var defaultKubernetesFeatureGates = map[featuregate.Feature]featuregate.FeatureSpec{
    ...
    genericfeatures.AdmissionWebhookMatchConditions: {Default: true, PreRelease: featuregate.GA, LockToDefault: true}, // remove in 1.33


    genericfeatures.AggregatedDiscoveryEndpoint: {Default: true, PreRelease: featuregate.GA, LockToDefault: true}, // remove in 1.33


    genericfeatures.AnonymousAuthConfigurableEndpoints: {Default: false, PreRelease: featuregate.Alpha},
    ...
}
```

在 Kubernetes 源码中，在导入包的时候，你经常可以发现有以下包重命名方式：genericxxx，generic关键子说明，这个包中的功能是通用的。很多时候也意味着，还有一些组件维度的，或者非通用的具有类似功能的包。

### Kubernetes 中功能门控注册

在 k8s.io/apiserver/pkg/util/feature包中，定义了全局的DefaultMutableFeatureGate和DefaultFeatureGate 2 个 Feature Gate。在程序启动的时候，会将预定义的 Feature Gate 注册到这 2 个全局的 Feature Gates 中，供程序使用。例如：在 kube-apiserver 组件启动时，通过以下代码，来向上述的 Feature Gates 注册预定义的 Feature Gate：

```go
// 位于 cmd/kube-apiserver/app/server.go 文件中
package app


import (
    ...
    utilfeature "k8s.io/apiserver/pkg/util/feature"
    ...
    "k8s.io/kubernetes/cmd/kube-apiserver/app/options"
    ...
)


func init() {
    utilruntime.Must(logsapi.AddFeatureGates(utilfeature.DefaultMutableFeatureGate))
}


...


// 位于 cmd/kube-apiserver/app/options/options.go 文件中
package options


import (
    ...
    _ "k8s.io/kubernetes/pkg/features" // add the kubernetes feature gates
    ...
)


// 位于 pkg/features/kube_features.go 文件中
package features


import (
    apiextensionsfeatures "k8s.io/apiextensions-apiserver/pkg/features"
    "k8s.io/apimachinery/pkg/util/runtime"
    genericfeatures "k8s.io/apiserver/pkg/features"
    utilfeature "k8s.io/apiserver/pkg/util/feature"
    clientfeatures "k8s.io/client-go/features"
    "k8s.io/component-base/featuregate"
)


...


func init() {
    runtime.Must(utilfeature.DefaultMutableFeatureGate.Add(defaultKubernetesFeatureGates))
    runtime.Must(utilfeature.DefaultMutableFeatureGate.AddVersioned(defaultVersionedKubernetesFeatureGates))


    // Register all client-go features with kube's feature gate instance and make all client-go
    // feature checks use kube's instance. The effect is that for kube binaries, client-go
    // features are wired to the existing --feature-gates flag just as all other features
    // are. Further, client-go features automatically support the existing mechanisms for
    // feature enablement metrics and test overrides.
    ca := &clientAdapter{utilfeature.DefaultMutableFeatureGate}
    runtime.Must(clientfeatures.AddFeaturesToExistingFeatureGates(ca))
    clientfeatures.ReplaceFeatureGates(ca)
}
```

这里，我来给你顺下 kube-apiserver 在启动的时候，注册 Feature Gate 的流程：

1. 在 k8s.io/kubernetes/pkg/features包中，定义了一些预定于的 Feature Gate，这些包在被加载时，会调用  init()函数，在函数中，会将这些预定义的 Feature Gate 通过 utilfeature.DefaultMutableFeatureGate.Add方法调用 注册到 utilfeature（k8s.io/apiserver/pkg/util/feature包别名）包的 DefaultMutableFeatureGateFeature Gates 中；
2. 在 kube-apiserver 启动时，会首先导入 k8s.io/kubernetes/cmd/kube-apiserver/app/options包，在 k8s.io/kubernetes/cmd/kube-apiserver/app/options包中匿名导入 k8s.io/kubernetes/pkg/features

通过以上 2 步，将 kube-apiserver 预定义的 Feature Gate 注册到 k8s.io/apiserver/pkg/util/feature包的 DefaultMutableFeatureGate全局变量中。之后，便可以在 kube-apiserver 代码中，使用 DefaultMutableFeatureGate中注册的 Feature Gate。

### Kubernetes 中功能门控使用

这里，我们来看下 Kubernetes 代码中，是如何使用 CSIVolumeHealth功能门控的。使用代码见文件 [pkg/volume/csi/csi_client.go](https://github.com/kubernetes/kubernetes/blob/master/pkg/volume/csi/csi_client.go#L626)，代码内容如下：

```go
    if utilfeature.DefaultFeatureGate.Enabled(features.CSIVolumeHealth) {
        isSupportNodeVolumeCondition, err := c.nodeSupportsVolumeCondition(ctx)
        if err != nil {
            return nil, err
        }


        if isSupportNodeVolumeCondition {
            abnormal, message := resp.VolumeCondition.GetAbnormal(), resp.VolumeCondition.GetMessage()
            metrics.Abnormal, metrics.Message = &abnormal, &message
        }
    }
```

通过上述代码，我们不难发现，使用方式其实就是调用 Feature Gate 的 Enabled方法，来判断功能门控是否开启，如果开启了做什么操作，如果没有开启又做什么操作。Feature Gate 来自于 k8s.io/apiserver/pkg/util/feature包中的全局变量 DefaultMutableFeatureGate。

另外，在启动 kube-apiserver 时，我们也可以动态的设置某个功能是否开启，例如：

```
$ _output/bin/kube-apiserver --feature-gates=CSIVolumeHealth=true
```

### Feature Gates 源码剖析

Kubernetes 定义 Feature Gates、使用 Feature Gates 都是通过 k8s.io/component-base/featuregate包中定义的结构体、函数、方法来实现的。本小节，我来给你详细介绍下 k8s.io/component-base/featuregate包的具体实现，也即 Kubernetes Feature Gates 机制的具体实现方式。

Kubernetes Feature Gates 机制的核心实现位于 [staging/src/k8s.io/component-base/featuregate/feature_gate.go](https://github.com/kubernetes/kubernetes/blob/v1.31.1/staging/src/k8s.io/component-base/featuregate/feature_gate.go) 文件中。

k8s.io/component-base/featuregate包定义了一个 featureGate结构体类型，该结构体类型，其实就代表一个 Feature Gates。featureGate结构体定义如下：

```go
// featureGate implements FeatureGate as well as pflag.Value for flag parsing.
type featureGate struct {
    // Feature Gates 的名称，用来表示一个 Feature Gates.
    featureGateName string


    // Feature Gates 的处理函数映射，用于定义如何处理相关的特性逻辑，如解析和启用特性。
    special map[Feature]func(map[Feature]VersionedSpecs, map[Feature]bool, bool, *version.Version)


    // 互斥锁，用于保护对以下字段的写访问，确保线程安全。
    lock sync.Mutex
    // 存储已知特性及其相关定义的映射（map[Feature]FeatureSpec）。
    known atomic.Value
    // 存储每个特性是否被启用的映射（map[Feature]bool）。
    enabled atomic.Value
    // 存储解析后的原始标志值的映射（map[string]bool），保留特殊特性的初始值。
    enabledRaw atomic.Value
    // 在调用 AddFlag 时设置为 true，以防止后续的 Add 方法调用。
    closed bool
    // 存储通过 Enabled 接口查询的特性，当调用 SetEmulationVersion 时，这些特性重置。
    queriedFeatures atomic.Value
    // 指向当前模拟版本的指针，用于特性门控的逻辑处理。
    emulationVersion atomic.Pointer[version.Version]
}
```

通过 featureGate结构体中包含了一些字段，分别用来完成不同的功能。featureGate结构体具有一些方法，这些方法完成跟功能门控相关的各类功能。核心方法列表如下：

1. NewFeatureGate()：创建并返回一个新的 featureGate 实例；
2. Set(value string) error：设置特性的值。根据传入的字符串值解析并更新功能门控的状态；
3. SetFromMap(m map[string]bool) error：根据给定的映射设置特性的启用状态。这是一个公共方法，用于根据传入的键值对更新特性；
4. Add(features map[Feature]FeatureSpec) error：向功能门控添加新的特性及其规格。用于扩展功能门控的能力以支持更多特性；
5. SetEmulationVersion(emulationVersion version.Version) error：设置模拟版本，以便在特性逻辑中使用。此版本会影响特性启用的逻辑；
6. AddFlag(fs *pflag.FlagSet)：将功能门控的标志添加到提供的标志集，便于通过命令行配置特性；
7. KnownFeatures() []string：返回所有已知特性的名称列表，提供特性的可用性信息；

还有一些其他有用的方法，你可以阅读 [staging/src/k8s.io/component-base/featuregate/feature_gate.go](https://github.com/kubernetes/kubernetes/blob/v1.31.1/staging/src/k8s.io/component-base/featuregate/feature_gate.go) 文件，学习并掌握。

## OneX 中 Feature Gates 的使用

OneX 项目在开发的时候，也借鉴了 Kuberentes Feature Gates 的特性，并复用了相关包。这里，我仅列出使用到的位置，具体使用方法，你可以阅读 OneX 中的相关源码（下面列出的这些文件）：

1. Feature Gates 定义：位于 [internal/pkg/feature/features.go](https://github.com/superproj/onex/blob/v0.1.1/internal/pkg/feature/features.go) 文件中；
2. Feature Gates 使用：位于 [internal/controller/miner/controller.go](https://github.com/superproj/onex/blob/v0.1.1/internal/controller/miner/controller.go#L106) 文件中。

## 总结

本节课详细介绍了如何定义并使用 Feature Gates，给出了详细、可实操的示例。另外，本节课，也详细介绍了 Kubernetes 项目是如何定义并使用 Feature Gates。通过学习 Kubernetes Feature Gates 功能的实现，在你的业务开发中，也可以复用 Kubernetes 的 Feature Gates 的设计及包。