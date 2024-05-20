# GitLab Runner安装

[TOC]



## 1. 使用GItLab官方仓库安装

- 重点掌握CentOS系统的安装方式
- 重点掌握Ubuntu系统的安装方式

我们提供Debian，Ubuntu，Mint，RHEL，Fedora和CentOS当前受支持版本的软件包。

| Distribution | Version | End of Life date      |
| :----------- | :------ | :-------------------- |
| Debian       | stretch | approx. 2022          |
| Debian       | jessie  | June 2020             |
| Ubuntu       | bionic  | April 2023            |
| Ubuntu       | xenial  | April 2021            |
| Mint         | sonya   | approx. 2021          |
| Mint         | serena  | approx. 2021          |
| Mint         | sarah   | approx. 2021          |
| RHEL/CentOS  | 7       | June 2024             |
| RHEL/CentOS  | 6       | November 2020         |
| Fedora       | 29      | approx. November 2019 |



Add GitLab’s official repository:  添加官方仓库

```shell
# For Debian/Ubuntu/Mint
curl -L https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh | sudo bash

# For RHEL/CentOS/Fedora
curl -L https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.rpm.sh | sudo bash
```

Install the latest version of GitLab Runner: 安装最新版本

```shell
# For Debian/Ubuntu/Mint
sudo apt-get install gitlab-runner

# For RHEL/CentOS/Fedora
sudo yum install gitlab-runner
```

To install a specific version of GitLab Runner: 安装指定版本

```shell
# for DEB based systems
apt-cache madison gitlab-runner
sudo apt-get install gitlab-runner=10.0.0

# for RPM based systems
yum list gitlab-runner --showduplicates | sort -r
sudo yum install gitlab-runner-10.0.0-1
```

更新runner

```shell
# For Debian/Ubuntu/Mint
sudo apt-get update
sudo apt-get install gitlab-runner

# For RHEL/CentOS/Fedora
sudo yum update
sudo yum install gitlab-runner
```



## 2. 在GNU / Linux上手动安装GitLab Runner

如果您不能使用[deb / rpm存储库](https://docs.gitlab.com/12.6/runner/install/linux-repository.html)安装GitLab Runner，或者您的GNU / Linux操作系统不在支持的版本中，则可以使用以下一种方法手动安装它，这是最后的选择。

### 通过`deb`或`rpm`软件包

**下载软件包**

1. 在[https://gitlab-runner-downloads.s3.amazonaws.com/latest/index.html上](https://gitlab-runner-downloads.s3.amazonaws.com/latest/index.html)找到最新的文件名和选项 。
2. 选择一个版本并下载二进制文件，如文档所述，该文件用于[下载任何其他标记的](https://docs.gitlab.com/12.6/runner/install/bleeding-edge.html#download-any-other-tagged-release) GitLab Runner发行版。

例如，对于Debian或Ubuntu：

```shell
curl -LJO https://gitlab-runner-downloads.s3.amazonaws.com/latest/deb/gitlab-runner_<arch>.deb

dpkg -i gitlab-runner_<arch>.deb

dpkg -i gitlab-runner_<arch>.deb
```



例如，对于CentOS或Red Hat Enterprise Linux：

```shell
curl -LJO https://gitlab-runner-downloads.s3.amazonaws.com/latest/rpm/gitlab-runner_<arch>.rpm

rpm -i gitlab-runner_<arch>.rpm

rpm -Uvh gitlab-runner_<arch>.rpm
```



### 使用二进制文件

参考地址： https://docs.gitlab.com/12.6/runner/install/bleeding-edge.html#download-any-other-tagged-release

下载指定版本： 将上面URL中的latest切换为 v12.6。

```shell
# Linux x86-64
sudo curl -L --output /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-amd64

# Linux x86
sudo curl -L --output /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-386

# Linux arm
sudo curl -L --output /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-arm

# Linux arm64
sudo curl -L --output /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-arm64
```

添加执行权限

```shell
sudo chmod +x /usr/local/bin/gitlab-runner
```

创建一个gitlab用户

```shell
sudo useradd --comment 'GitLab Runner' --create-home gitlab-runner --shell /bin/bash
```

安装并作为服务运行

```shell
sudo gitlab-runner install --user=gitlab-runner --working-directory=/home/gitlab-runner
sudo gitlab-runner start
```

更新

```bash
#停止服务
sudo gitlab-runner stop

#下载新版本二进制包
sudo curl -L --output /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-linux-amd64

#赋予执行权限
sudo chmod +x /usr/local/bin/gitlab-runner

#启动服务
sudo gitlab-runner start
```



## 在MacOS中安装

在macOS上安装GitLab Runner有两种方法：

- [手动安装](https://docs.gitlab.com/12.6/runner/install/osx.html#manual-installation-official)。GitLab正式支持和推荐此方法。
- [自制安装](https://docs.gitlab.com/12.6/runner/install/osx.html#homebrew-installation-alternative)。使用[Homebrew进行](https://brew.sh/)安装，以替代手动安装。

### 手动安装

下载二进制包

```
sudo curl --output /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/v12.6/binaries/gitlab-runner-darwin-amd64
```

授予其执行权限：

```
sudo chmod +x /usr/local/bin/gitlab-runner
```

将Runner作为服务安装并启动它：

```
cd ~
gitlab-runner install
gitlab-runner start
```

### 自动安装

安装，启动

```
brew install gitlab-runner
brew services start gitlab-runner
```

### 更新

```
gitlab-runner stop

sudo curl -o /usr/local/bin/gitlab-runner https://gitlab-runner-downloads.s3.amazonaws.com/latest/binaries/gitlab-runner-darwin-amd64

sudo chmod +x /usr/local/bin/gitlab-runner
gitlab-runner start
```



## 4. 在容器中运行GitLab Runner

```shell
# --rm 这个选项表示当容器停止运行后自动删除容器。
# -t 这个选项分配一个伪TTY终端，通常与 -i 选项结合使用。
# -i 表示以交互模式运行容器，即使没有连接到标准输入也保持打开。
# -d 表示以分离模式（detached mode）运行容器，使容器在后台运行。

mkdir -p ~/data/gitlab-runner/config
docker run --rm -t -id -v ~/data/gitlab-runner/config:/etc/gitlab-runner --name gitlab-runner  gitlab/gitlab-runner:v12.6.0
docker exec -it gitlab-runner   bash
root@75ab6ebd177b:/# gitlab-runner -v
Version:      12.6.0
Git revision: ac8e767a
Git branch:   12-6-stable
GO version:   go1.13.4
Built:        2019-12-22T11:55:34+0000
OS/Arch:      linux/amd64
```



# 总结

对于软件的安装，可以体系学习一下鸟哥私房菜里面关于软件安装的部分，当然，目前我还没来得及看。这里我使用docker的方式安装GitLab Runner。