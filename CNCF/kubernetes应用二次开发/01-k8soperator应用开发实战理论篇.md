# 基本概念

**k8s管理资源的方式（声明式）**

- 先定义我要管理成什么样
- 再实现如何达成这个目的

<img src="image/image-20240812220120724.png" alt="image-20240812220120724" style="zoom: 67%;" />

该图片展示了k8s处理资源的经典流程。

**什么是operator**

- k8s扩展的一种方式
- 我们根据自己的需要，在k8s中自定义工作方式
- 需要提供两个东西：1. 什么样的结果（期望）。2. 如何达到这个结果

<img src="image/image-20240812220711600.png" alt="image-20240812220711600" style="zoom: 67%;" />

在CRD注册之后，就可以提交CR了，CR就是根据CRD字段的定义来提交对应的字段值，apiserver则根据不同的CRD找到不同的controller。



**开发的内容**

- 定义这个东西的形态（CRD）
- 制定制造的方法（Controller）
- 把制造的东西暴露出去（通过apiserver暴露）



# 环境安装

## wsl

以管理员模式打开PowerShell

```shell
wsl --set-default-version 2
wsl --update
wsl --install -d Ubuntu
wsl -l -v

# 进入wsl
wsl
```



## docker

```
https://docs.docker.com/desktop/install/windows-install/
```

docker是C/S架构，我们这里安装的docker desktop其实是服务端，会被挂载到WSL中，所以docker desktop需要启用WSL 2，并整合WSL。然后我们需要在WSL 2中安装的Ubuntu中安装docker客户端。

启用WSL

<img src="image/image-20240813065021202.png" alt="image-20240813065021202" style="zoom:67%;" />

整合WSL并关联安装的ubuntu系统

<img src="image/image-20240813065610108.png" alt="image-20240813065610108" style="zoom:67%;" />

然后“Apply & restart”。我们这里安装

接着以管理员模式打开PowerShell，安装docker客户端即可

```shell
# 先执行wsl，进入Ubuntu系统
apt-get update
apt-get install docker.io
```



## golang1.19

我推荐在wsl中使用gvm安装和管理，方便在需要不同版本的时候切换，地址https://github.com/moovweb/gvm

安装gvm命令

```
bash < <(curl -s -S -L https://raw.githubusercontent.com/moovweb/gvm/master/binscripts/gvm-installer)
```



安装golang

```
gvm install go1.4 -B

gvm use go1.4

export GOROOT_BOOTSTRAP=$GOROOT

gvm install go1.19

gvm use go1.19 --default
```

