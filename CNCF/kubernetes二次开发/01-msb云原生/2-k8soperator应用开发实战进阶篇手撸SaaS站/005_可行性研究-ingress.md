# 可行性研究

# 1 安装k8s集群

## 1.1 配置kind

创建目录

```
mkdir ~/config
```

创建文件~/config/kind.config

```
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
```

## 1.2 安装集群

```
kind create cluster --config ~/config/kind.config --name ingress --image kindest/node:v1.24.3
```

# 2 ingress-nginx

## 2.1 安装ingress-nginx

```
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.3.0/deploy/static/provider/cloud/deploy.yaml
```

PS:
镜像为：
`registry.k8s.io/ingress-nginx/kube-webhook-certgen:v1.1.1@sha256:64d8c73dca984af206adf9d6d7e46aa550362b1d7a01f3a0a91b20cc67868660`
`registry.k8s.io/ingress-nginx/controller:v1.3.0@sha256:d1707ca76d3b044ab8a28277a2466a02100ee9f58a86af1535a3edf9323ea1b5`
可以提前下载
通过以下命令下载镜像

获取kind所在的运行容器id

```
docker ps
```

pull 镜像

```
docker exec -it <容器id> crictl pull <image>
```

## 2.2 依赖创建

启动测试镜像

```
kubectl run my-nginx --image=nginx
```

暴露端口

```
kubectl expose pod my-nginx --port=80
```

## 2.3 启动ingress

创建文件 ~/config/ingress.yaml

```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: minimal-ingress
spec:
  ingressClassName: nginx
  rules:
  - host: www.mashibing-test.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: my-nginx
            port:
              number: 80
```

执行命令启动

```
kubectl create -f ~/config/ingress.yaml
```

# 3 系统配置

配置hosts，添加www.mashibing-test.com到本机（或者浏览器插件安装同样功能的工具）
