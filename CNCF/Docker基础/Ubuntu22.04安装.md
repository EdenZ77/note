# 安装

具体安装细节直接网上搜就可以了，比较简单

> https://blog.csdn.net/qq_44490498/article/details/138259678



## 换源操作

在第六步换源操作时，使用下面的操作即可，需要禁用CD/DVD源

```shell
# 1. 编辑 sources.list
sudo vi /etc/apt/sources.list

# 2. 在 CD/DVD 源行前添加 # 注释掉它
# 修改前：
# deb cdrom:[Debian GNU/Linux...]/ bookworm contrib main non-free-firmware
#
# 修改后：
# # deb cdrom:[Debian GNU/Linux...]/ bookworm contrib main non-free-firmware

# 3. 保存并退出 vi（按 Esc，然后输入 :wq）

# 4. 更新软件包列表
sudo apt update

# 5. 安装 vim
sudo apt install -y vim
```

额外建议：优化您的 sources.list；修改后的完整 `sources.list` 建议：

```shell
主要改进：
1、添加了 contrib 和 non-free 仓库：包含更多软件
2、移除了 CD/DVD 源：避免安装时要求插入光盘
3、统一了仓库结构：每个源都包含所有组件

# 备份现有源配置（推荐）
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak


# 写入新配置
sudo sh -c 'cat > /etc/apt/sources.list << EOF
# 清华大学主源
deb http://mirrors.tuna.tsinghua.edu.cn/debian/ bookworm main contrib non-free non-free-firmware
deb-src http://mirrors.tuna.tsinghua.edu.cn/debian/ bookworm main contrib non-free non-free-firmware

# 阿里云镜像源（备份）
deb http://mirrors.aliyun.com/debian/ bookworm main contrib non-free non-free-firmware
deb-src http://mirrors.aliyun.com/debian/ bookworm main contrib non-free non-free-firmware

# 安全更新
deb http://security.debian.org/debian-security bookworm-security main contrib non-free non-free-firmware
deb-src http://security.debian.org/debian-security bookworm-security main contrib non-free non-free-firmware

# 软件更新
deb http://mirrors.tuna.tsinghua.edu.cn/debian/ bookworm-updates main contrib non-free non-free-firmware
deb-src http://mirrors.tuna.tsinghua.edu.cn/debian/ bookworm-updates main contrib non-free non-free-firmware
EOF'


# 更新软件包列表
sudo apt update

# 验证配置
cat /etc/apt/sources.list
apt-cache policy

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
