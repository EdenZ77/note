上一节课，我介绍了 Kuberentes 的应用构建模型，Kubernetes 中的核心组件基本都是基于上一节课的应用构建方式来构建的。所以，后面组件不会再详细讲解 Kubernetes 组件的应用构建实现。



kube-apiserver 是 Kubernetres 最核心的组件，也是其他 Kubernetes 组件运行的依赖。所以，本节课，我先来介绍下 kube-apiserver 的源码。



因为 kube-apiserver 是 Kubernetes 中最核心、实现最复杂的组件。所以，本套课程会花一些篇幅详细介绍 kube-apiserver 的实现，先给你打个预防针。



## 如何学习 kube-apiserver 源码？



kube-apiserver 源码量很大，涉及到的概念也非常多，很难面面俱到的全部讲清楚。所以，在讲 kube-apiserver 的时候，我进行了一些取舍。首先，kube-apiserver 本质上是一个标准的 RESTful API 服务器，所以，我会按照 RESTful API 服务器的开发流程去给你讲解 kube-apiserver 具体是如何去实现一个 REST 服务器的。另外，kube-apiserver 中有很多核心的功能和概念，这些功能和概念，我会根据自己的理解，来选择性讲解。



本套课程可能不会一下就把所有的核心功能都讲解完，但是 kube-apiserver 中的核心流程，在课程上线后，仍然会不断补充、更新的。



kube-apiserver 本质上是一个提供了 REST 接口的 Web 服务，在开发 Web 服务时，通常我们的开发步骤如下：

![img](image/Fk-e3-gEnk4BDau6lXgmQ7_FJ3pA)