# 安装

具体安装细节直接网上搜就可以了，比较简单

> https://blog.csdn.net/qq_44490498/article/details/138259678



## 换源操作

```shell

# 备份现有源配置（推荐）
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak


sudo sh -c 'cat << "EOF" > /etc/apt/sources.list
deb http://mirrors.aliyun.com/ubuntu/ jammy main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ jammy-security main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ jammy-updates main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ jammy-proposed main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ jammy-backports main restricted universe multiverse
EOF'


# 更新软件包列表
sudo apt update


sudo apt install -y vim
vim --version
```



## root用户ssh登录

```shell
# 切换成root用户
sudo su
# 输入密码，切换成功之后

# 修改root的密码
passwd root
# 输入新密码


sudo apt update
# 1.1安装ssh安装ssh-server
sudo apt install openssh-server
# 1.2启动ssh
sudo systemctl start ssh
# 1.3启用ssh
sudo systemctl enable ssh
# 1.4查看ssh状态
sudo systemctl status ssh
```

2、打开 root 远程登录

```shell
# 修改 /etc/ssh/sshd_config 文件

1、找到 #PermitRootLogin 一行 改成 PermitRootLogin yes ，也就是删掉前端的注释并做改后面的值为yes
2、删掉#PasswordAuthentication yes 前面的 #

# 重启 ssh 服务
sudo service ssh restart
```



## 安装curl

```shell
# 安装 curl（默认版本）
sudo apt install curl -y

# 验证安装
curl --version
```
