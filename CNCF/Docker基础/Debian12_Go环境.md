# 安装

具体安装细节：image/VMware虚拟机安装Debian12-CSDN博客.pdf

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

1、Debian12配置ssh服务器

```shell
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

## 支持ll命令

```shell
# 并不是说原生debian是不支持ll命令，而是因为ll本身就是别名命令。
# 别名可以在bashrc上追加再应用即可，一行搞定：
echo "alias ll='ls -la --color=auto'" >> ~/.bashrc && source ~/.bashrc
```

## vim支持复制

```shell
# 检查剪贴板支持
vim --version | grep clipboard
# 如果显示 +clipboard 则支持，若显示 -clipboard 需要重装vim
sudo apt install vim-gtk3  # Debian 10+ 使用这个名称
```

## 安装curl

```shell
# 安装 curl（默认版本）
sudo apt install curl -y

# 验证安装
curl --version
```



# 配置环境

## Github全局加速

国内大部分服务器无法访问 github，或者即时能访问也是速度慢，时灵时不灵的。所以需要给ssh增加代理：

```shell
# 如果是虚拟机中增加，则需要使用与虚拟机同一网段的宿主机IP地址
git config --global https.'https://github.com'.proxy "http://192.168.220.1:10809"
git config --global http.'https://github.com'.proxy "http://192.168.220.1:10809"

git config --list
git config --global --list

# 如果是window中增加
git config --global https.'https://github.com'.proxy "http://127.0.0.1:10809"
git config --global http.'https://github.com'.proxy "http://127.0.0.1:10809"
```

### 问题

```shell
ssh: connect to host github.com port 22: Connection timed out
```

网络环境（例如：公司网络、学校网络、公共 WiFi）可能出于安全考虑，防火墙禁用了 SSH 协议的 22 端口。

解决方案一：改用 HTTPS 协议

这是最快、最直接的临时解决方案。你可以将远程仓库的 URL 从 SSH 切换到 HTTPS。

```shell
# 查看当前远程仓库地址
$ git remote -v
origin  git@github.com:EdenZ77/note.git (fetch)
origin  git@github.com:EdenZ77/note.git (push)


# 那么将其更改为 HTTPS 地址
git remote set-url origin https://github.com/EdenZ77/note.git
# 改回去
git remote set-url origin git@github.com:EdenZ77/note.git

$ git remote -v
origin  https://github.com/EdenZ77/note.git (fetch)
origin  https://github.com/EdenZ77/note.git (push)

# 再次执行 pull
$ git pull --tags origin master
```

HTTPS 使用的 443 端口几乎在所有网络环境中都是开放的。每次推送可能需要输入用户名和密码。

解决方案二：使用 SSH over HTTPS 端口

这是一个更优雅的方案。SSH 可以通过 443 端口进行连接，完美绕过对 22 端口的封锁。编辑 `~/.ssh/config`文件（如果不存在就创建），加入以下内容：

```
Host github.com
  Hostname ssh.github.com
  Port 443
  User git
```

保存后，再次尝试你的 `git pull`命令。这个配置会让所有与 `github.com`的 SSH 连接都通过 `ssh.github.com`的 443 端口进行。



## Go安装

1、下载安装包

可以从 Go 语言官方网站下载对应的 Go 安装包和源码包。以下命令将下载 go1.24.0 安装包：

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

# 修改完 $HOME/.bashrc 之后，执行以下命令：
source ~/.bashrc
```



## Git安装与配置

```shell
# 安装 Git（默认版本）
sudo apt install git -y

# 验证安装
git --version
# 应输出类似：git version 2.39.2

# 设置用户名（全局）
git config --global user.name "debian-eden"

# 设置邮箱（全局）
git config --global user.email "2660996862@example.com"

# 查看配置
git config --list

# 生成密钥，总共三次需要输入的，你都直接三次回车就好！
ssh-keygen -t rsa -C "2660996862@example.com"
# 生成后，会在当前用户的目录下，生成一个.ssh隐藏目录，目录中会有【id_rsa】和【id_rsa.pub】两个文件，一个是私钥，一个是公钥

git config --global core.longpaths true # 解决 Git 中 'Filename too long' 的错误
```



## VSCode远程调试

在 VS Code 中通过 **Remote-SSH** 连接到 Debian 12 服务器后调试 Go 程序，需要配置远程调试环境。以下是分步指南：

```shell
# 检查安装
dlv version
# 如果未安装，使用go安装
go install github.com/go-delve/delve/cmd/dlv@latest
```

在服务器启动 Delve 调试器

```shell
# 编译并启动调试（在服务器上执行）
cd ~/your-project
dlv debug ./cmd/fg-apiserver/main.go --headless --listen=:2345 --api-version=2 -- \
  -c configs/fg-apiserver.yaml
  
# debug: 子命令，表示编译并调试指定的 Go 程序
# --headless 无头模式，不启动交互式终端，作为后台服务运行
# --listen=:2345 监听端口，在 2345 端口监听调试连接
# --api-version=2 API 版本，使用 Delve API 版本 2
# 分隔符 -- 表示后续参数是传递给被调试程序的，而不是调试器本身
# -c configs/fg-apiserver.yaml 这是传递给 fg-apiserver 程序的参数
```

配置 VS Code `launch.json`

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Remote Attach",
            "type": "go",
            "request": "attach",
            "mode": "remote",
            "remotePath": "/root/golang/src/github.com/onexstack/fastgo", // 服务器上的项目路径
            "port": 2345,
            "host": "192.168.220.151",
        }
    ]
}
```

在 VS Code 中：

- 选择调试配置 **「Remote Attach」**
- 按 `F5` 开始调试
- 使用断点、变量查看等功能

**常见问题解决**

问题1：连接被拒绝

- 确认服务器防火墙放行端口：

  ```
  sudo ufw allow 2345
  ```

- 检查 Delve 是否正在运行：

  ```
  netstat -tulnp | grep 2345
  ```

### 自动dlv

其实也可以不用上面那种麻烦的方式，操作如下：

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Launch fg-apiserver",
            "type": "go",
            "request": "launch",
            "mode": "debug",
            "program": "${workspaceFolder}/cmd/fg-apiserver/main.go",
            "args": ["-c", "configs/fg-apiserver.yaml"],
            "cwd": "${workspaceFolder}",// Current Working Directory​​（当前工作目录）
            "env": {
                "GIN_MODE": "debug"  // 可选：设置环境变量
            }
        }
    ]
}
```

然后调试时选择该配置名即可，控制台的输出如下，可以看到也是使用了 dlv 的方式进行远程调试。

```shell
Starting: /root/golang/bin/dlv dap --listen=127.0.0.1:45101 --log-dest=3 from /root/golang/src/github.com/onexstack/fastgo/cmd/fg-apiserver
DAP server listening at: 127.0.0.1:45101
Type 'dlv help' for list of commands.
2025/07/09 10:24:46 maxprocs: Leaving GOMAXPROCS=4: CPU quota undefined
命令行参数: [/root/golang/src/github.com/onexstack/fastgo/cmd/fg-apiserver/__debug_bin2379856144 -c configs/fg-apiserver.yaml]
else versionFlag: false
[GIN-debug] [WARNING] Running in "debug" mode. Switch to "release" mode in production.
 - using env:	export GIN_MODE=release
 - using code:	gin.SetMode(gin.ReleaseMode)
```

