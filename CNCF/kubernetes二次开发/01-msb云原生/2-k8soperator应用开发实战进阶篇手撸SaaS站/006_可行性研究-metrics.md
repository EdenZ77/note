# metrics

使用命令

```
kubectl get pod -ningress-nginx
```

获取controller的名称

使用命令

```
kubectl exec -n ingress-nginx ingress-nginx-controller-<随机id,从上面获取知道的具体值> -- curl 127.0.0.1:10254/metrics
```

确认可以获取metrics信息
