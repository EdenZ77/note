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



## 桌面版关闭GUI



```shell
# 永久关闭
sudo systemctl set-default multi-user.target

# 永久开启
sudo systemctl set-default graphical.target

# 修改后重启
reboot
```





## 静态IP

你有可能想固定机器的IP地址，编辑`/etc/netplan`下的`xxx.yaml`文件，如果只有一张网卡，就只有一个文件

```shell
root@master:/etc/netplan# ll
总计 20
drwxr-xr-x   2 root root  4096  6月 27 13:36 ./
drwxr-xr-x 130 root root 12288  6月 27 11:05 ../
-rw-------   1 root root   537  6月 27 13:36 01-network-manager-all.yaml

# 原始内容如下：
root@node2:/etc/netplan# cat 01-network-manager-all.yaml
# Let NetworkManager manage all devices on this system
network:
  version: 2
  renderer: NetworkManager


# 修改为如下内容：
network:
  version: 2
  renderer: NetworkManager  # 桌面版必须保留此行
  ethernets:
    ens33: # 要固定的网卡名
      dhcp4: false # false是关闭自动获取地址，true是开启
      addresses:
        - 192.168.220.152/24 # 你要固定的IP地址
      routes:                   
        - to: default           # 表示默认路由
          via: 192.168.220.2    # 网关地址
      nameservers:
        addresses: # DNS地址，可以有多个
          - 223.5.5.5 # 阿里DNS
          - 8.8.8.8 # 谷歌DNS
          
# 改完文件后应用更改：
netplan apply

# 有可能有如下警告：
root@master:/etc/netplan# netplan apply

** (generate:12063): WARNING **: 13:38:06.306: Permissions for /etc/netplan/01-network-manager-all.yaml are too open. Netplan configuration should NOT be accessible by others.
# Netplan 要求配置文件权限必须为 600（仅 root 可读写），禁止其他用户访问。若权限设置为 644（其他用户可读）或 777（完全开放），则会触发警告

# 修改配置文件权限
sudo chmod 600 /etc/netplan/01-network-manager-all.yaml  # 仅 root 可读写

```

