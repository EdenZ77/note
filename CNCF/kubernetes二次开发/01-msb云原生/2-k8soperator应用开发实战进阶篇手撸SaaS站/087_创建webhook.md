# 创建WebHook

# 1 命令：

```
kubebuilder create webhook --group <group> --version <version> --kind <kind> --defaulting --programmatic-validation
```

## 1.1 我们项目的命令

```
kubebuilder create webhook --group apps --version v1 --kind MsbDeployment --defaulting --programmatic-validation
```

## 1.2 创建的内容

* main文件中的webhook注册
* api中default相关的代码
* api中validate相关的代码
* config中webhook相关的配置

# 2 其他设置

## 2.1 安装cert-manager

命令为

```
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.2/cert-manager.yaml
```

也可以加到test里面（config.yaml中增加一步）

## 2.2 打开webhook及cert-manager相关的配置

路径为config/default/kustomization.yaml（包括下面的patchesStrategicMerge和vars中相关的部分）
