# 安装

具体安装细节：image/VMware虚拟机安装Debian12-CSDN博客.pdf

## 换源操作

在第六步换源操作时，使用下面的操作即可，需要禁用CD/DVD源

```
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

```
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

1、Debian12配置ssh服务器

```
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

```
# 修改 /etc/ssh/sshd_config 文件

找到 #PermitRootLogin 一行 改成 PermitRootLogin yes ，也就是删掉前端的注释并做改后面的值为yes

删掉#PasswordAuthentication yes 前面的 #

# 重启 ssh 服务
sudo service ssh restart
```

## 支持ll命令

```
并不是说原生debian是不支持ll命令，而是因为ll本身就是别名命令。
别名可以在bashrc上追加再应用即可，一行搞定：
echo "alias ll='ls -l --color=auto'" >> ~/.bashrc && source ~/.bashrc
```

支持复制

```
sudo apt install vim-gtk3
```



# 配置环境

## Github全局加速

国内大部分服务器无法访问 github，或者即时能访问也是速度慢，时灵时不灵的。需要给 github 加速一下。

```shell
github地址：https://github.com/WangGithubUser/FastGitHub

wget -c -O /opt/fastgithub_linux-x64.zip https://gitee.com/chcrazy/FastGithub/releases/download/2.1.4/fastgithub_linux-x64.zip
unzip -d /opt /opt/fastgithub_linux-x64.zip
rm /opt/fastgithub_linux-x64.zip


/opt/fastgithub_linux-x64/fastgithub start
/opt/fastgithub_linux-x64/fastgithub stop # 卸载服务

# 手动修改 $HOME/.bashrc 文件，在 $HOME/.bashrc 添加以下代码：
export http_proxy=http://127.0.0.1:38457
export https_proxy=http://127.0.0.1:38457

# 修改完 $HOME/.bashrc 之后，执行以下命令：
source ~/.bashrc
```

## Go安装

1、下载安装包

你可以从 Go 语言官方网站下载对应的 Go 安装包和源码包。以下命令将下载 go1.24.0 安装包：

```shell
$ wget -P /tmp/ https://go.dev/dl/go1.24.0.linux-amd64.tar.gz
```

2、解压并安装

请执行以下命令解压并安装 Go 编译工具及源码：

```shell
$ mkdir -p $HOME/go
$ tar -xvzf /tmp/go1.24.0.linux-amd64.tar.gz -C $HOME/go
$ mv $HOME/go/go $HOME/go/go1.24.0
```

`tar` 命令解析：

- `tar`: 是用于打包和解包文件的工具；
- `-xvzf`: 是 tar 命令的选项，含义如下：
  - `x`: 表示解压缩文件；
  - `v`: 表示显示详细的解压缩过程；
  - `z`: 表示使用gzip解压缩；
  - `f`: 表示后面跟着要解压的文件名。
- `go1.24.0.linux-amd64.tar.gz`: 是待解压缩的压缩文件名。
- `-C $HOME/go`: 表示解压缩后的文件要放到指定目录 `$HOME/go` 目录中。

3、配置 `$HOME/.bashrc` 文件

请按照以下命令将 Go 的相关环境变量追加到 `$HOME/.bashrc` 文件中

```shell
$ tee -a $HOME/.bashrc <<'EOF'
# Go envs
export GOVERSION=${GOVERSION-go1.24.0} # Go 版本设置
export GO_INSTALL_DIR=$HOME/go # Go 安装目录
export GOROOT=$GO_INSTALL_DIR/$GOVERSION # GOROOT 设置
export GOPATH=$HOME/golang # GOPATH 设置
export PATH=$GOROOT/bin:$GOPATH/bin:$PATH # 添加 PATH 路径
export GOPROXY=https://goproxy.cn,direct # 安装 Go 模块时，代理服务器设置
export GOPRIVATE=
export GOSUMDB=off # 关闭校验 Go 依赖包的哈希值
EOF
```

