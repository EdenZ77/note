# 04-安装部署GitLab服务

## rpm方式

源地址：https://mirrors.tuna.tsinghua.edu.cn/gitlab-ce/yum/el7/

```shell
wget https://mirrors.tuna.tsinghua.edu.cn/gitlab-ce/yum/el7/gitlab-ce-12.9.0-ce.0.el7.x86_64.rpm


rpm -ivh gitlab-ce-12.9.0-ce.0.el7.x86_64.rpm

vim /etc/gitlab.rb   # 编辑站点地址
gitlab-ctl reconfigure  # 配置


#服务控制
gitlab-ctl start 
gitlab-ctl status
gitlab-ctl stop 
```



## Docker方式

```shell
mkdir -p ~/data/gitlab/config ~/data/gitlab/logs ~/data/gitlab/data
docker pull gitlab/gitlab-ce:12.9.0-ce.0

# 启动
docker run -d  -p 443:443 -p 80:80 -p 222:22 --name gitlab --restart always -v /root/data/gitlab/config:/etc/gitlab -v /root/data/gitlab/logs:/var/log/gitlab -v /root/data/gitlab/data:/var/opt/gitlab gitlab/gitlab-ce:12.9.0-ce.0

# 编辑gitlab配置文件
 cd /root/data/gitlab/config       #进入配置文件所在目录下
 cp gitlab.rb gitlab.rb.bak        #修改配置文件之前先备份
 vim gitlab.rb                      #下列显示的都是编辑器中内容
 # external_url 'GENERATED_EXTERNAL_URL'           #找到这一行,修改为下面这一行
  external_url 'http://192.168.58.143'           #后面的地址改为gitlab地址
# gitlab_rails['gitlab_shell_ssh_port'] = 22      #找到这一行,修改为下面一行
  gitlab_rails['gitlab_shell_ssh_port'] = 222    #开启gitlab的ssh功能并且端口改为222;

#服务控制
docker restart gitlab
docker start gitlab
docker stop gitlab
docker rm gitlab

# 如果忘记root密码
docker exec -it gitlab   bash # 进入容器
root@897b598a109d:/# gitlab-rails console production
# 如果执行上面的指令提示
Traceback (most recent call last):
        8: from bin/rails:4:in `<main>'
        7: from bin/rails:4:in `require'
        6: from /opt/gitlab/embedded/lib/ruby/gems/2.6.0/gems/railties-6.0.2/lib/rails/commands.rb:18:in `<top (required)>'
        5: from /opt/gitlab/embedded/lib/ruby/gems/2.6.0/gems/railties-6.0.2/lib/rails/command.rb:46:in `invoke'
        4: from /opt/gitlab/embedded/lib/ruby/gems/2.6.0/gems/railties-6.0.2/lib/rails/command/base.rb:69:in `perform'
# 则可能是Gitlab版本不一样，然后参数方式不一样，需要用如下方式：
root@897b598a109d:/# gitlab-rails console -e production
--------------------------------------------------------------------------------
 GitLab:       12.9.0 (9a382ff2c82) FOSS
 GitLab Shell: 12.0.0
 PostgreSQL:   10.12
--------------------------------------------------------------------------------

Loading production environment (Rails 6.0.2)
irb(main):001:0>
# 查询用户
irb(main):002:0>  user = User.where(username:"root").first
=> #<User id:1 @root>
# 修改密码
irb(main):003:0> user.password = "12345678"
=> "12345678"
# 保存
irb(main):004:0> user.save!
Enqueued ActionMailer::DeliveryJob (Job ID: 018fdb35-d12e-40d7-8fa7-9682a2cbd70f) to Sidekiq(mailers) with arguments: "DeviseMailer", "password_change", "deliver_now", #<GlobalID:0x00007f4bf2ea94e0 @uri=#<URI::GID gid://gitlab/User/1>>
=> true
irb(main):005:0>

# 浏览器访问
http://192.168.58.143/       root/12345678
```



## Kubernetes部署

文件地址： https://github.com/zeyangli/devops-on-k8s/blob/master/devops/gitlab.yaml

```yaml
---
kind: Deployment
apiVersion: apps/v1
metadata:
  labels:
    k8s-app: gitlab
  name: gitlab
  namespace: devops
spec:
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      k8s-app: gitlab
  template:
    metadata:
      labels:
        k8s-app: gitlab
      namespace: devops
      name: gitlab
    spec:
      containers:
        - name: gitlab
          image: gitlab/gitlab-ce:12.6.0-ce.0
          imagePullPolicy: Always
          ports:
            - containerPort: 30088
              name: web
              protocol: TCP
            - containerPort: 22
              name: agent
              protocol: TCP
          resources:
            limits:
              cpu: 1000m
              memory: 4Gi
            requests:
              cpu: 500m
              memory: 512Mi
          livenessProbe:
            httpGet:
              path: /users/sign_in
              port: 30088
            initialDelaySeconds: 60
            timeoutSeconds: 5
            failureThreshold: 12
          readinessProbe:
            httpGet:
              path: /users/sign_in
              port: 30088
            initialDelaySeconds: 60
            timeoutSeconds: 5
            failureThreshold: 12
          volumeMounts:
            - name: gitlab-conf
              mountPath: /etc/gitlab
            - name: gitlab-log
              mountPath: /var/log/gitlab
            - name: gitlab-data
              mountPath: /var/opt/gitlab
          env:
            - name: gitlab_HOME
              value: /var/lib/gitlab
      volumes:
        - name: gitlab-conf
          hostPath: 
            path: /data/devops/gitlab/config
            type: Directory
        - name: gitlab-log
          hostPath: 
            path: /data/devops/gitlab/logs
            type: Directory
        - name: gitlab-data
          hostPath: 
            path: /data/devops/gitlab/data
            type: Directory
      serviceAccountName: gitlab
---
apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    k8s-app: gitlab
  name: gitlab
  namespace: devops
---
kind: Service
apiVersion: v1
metadata:
  labels:
    k8s-app: gitlab
  name: gitlab
  namespace: devops
spec:
  type: NodePort
  ports:
    - name: web
      port: 30088
      targetPort: 30088
      nodePort: 30088
    - name: slave
      port: 22
      targetPort: 22
      nodePort: 30022
  selector:
    k8s-app: gitlab
---
kind: Role
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
 name: gitlab
 namespace: devops
rules:
 - apiGroups: [""]
   resources: ["pods"]
   verbs: ["create","delete","get","list","patch","update","watch"]
 - apiGroups: [""]
   resources: ["pods/exec"]
   verbs: ["create","delete","get","list","patch","update","watch"]
 - apiGroups: [""]
   resources: ["pods/log"]
   verbs: ["get","list","watch"]
 - apiGroups: [""]
   resources: ["secrets"]
   verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: RoleBinding
metadata:
 name: gitlab
 namespace: devops
roleRef:
 apiGroup: rbac.authorization.k8s.io
 kind: Role
 name: gitlab
subjects:
 - kind: ServiceAccount
   name: gitlab
   namespace: devops

```



```
kubectl create -f gitlab.yaml
kubectl delete -f gitlab.yaml
```



# 总结

本次我尝试了使用Docker方式安装GitLab，很顺利，详细步骤写在了相应小节。对于K8S的方式，后期再来看一下如何安装，目前就是先简单使用起来。