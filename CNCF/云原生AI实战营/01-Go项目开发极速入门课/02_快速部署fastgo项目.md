## 部署环境

本项目的部署环境要求如下：

- **Go版本：** >= 1.24.0；
- **操作系统：** 建议使用 Linux 操作系统。本课程的部署环境为 Debian 12。如有条件，建议直接使用 Debian 12 系统，这样可以避免因操作系统差异带来的安装问题。若非 Debian 12 系统，出现部署问题，需要你自己根据错误日志，解决问题。

部署 fastgo 项目分为以下 4 步：

1. Go 编译环境安装和配置；
2. 安装和配置 MariaDB 数据库；
3. 初始化 `fastgo` 数据库；
4. 安装和配置 fastgo 项目。

## 步骤 1：Go 编译环境安装和配置

```
有关debian12安装、go安装的文档在 CNCF\Docker基础 文件夹中
```

## 步骤 2： 安装和配置 MariaDB 数据库

本课程在 master 分支下提供了一个名为 [configs/fastgo.sql](https://github.com/onexstack/fastgo/blob/master/configs/fastgo.sql) 的文件，其中保存了数据库初始化的 SQL 语句。

在生产环境中，fastgo 项目需要使用 MariaDB 数据库来存储数据，因此需要先安装 MariaDB 数据库。安装和配置 MariaDB 的具体步骤如下。

1、安装 MariaDB 服务端和 MariaDB 客户端

安装命令如下：

```shell
$ sudo apt install -y mariadb-server mariadb-client
```

2、启动 MariaDB，并设置开机启动

启动命令如下：

```shell
$ sudo systemctl enable mariadb
$ sudo systemctl start mariadb
$ sudo systemctl status mariadb
```

3、设置 root 初始密码

初始化命令如下：

```shell
$ sudo mysqladmin -uroot password 'fastgo1234'
```

> 提示： 执行 mysqladmin 命令时，必须具有 root 权限，否则可能会出现错误：mysqladmin: connect to server at 'localhost' failed。

## 步骤 3：初始化 fastgo 数据库

1、登录数据库并创建 `fastgo` 用户

创建命令如下：

```shell
$ mysql -h127.0.0.1 -P3306 -uroot -p'fastgo1234'
> grant all on fastgo.* TO fastgo@127.0.0.1 identified by 'fastgo1234';
> flush privileges;
> exit;
```

2、创建 `fastgo` 数据库

使用 `fastgo` 用户登录 MariaDB，并创建 `fastgo` 数据库，创建命令如下：

```shell
$ mkdir -p  $HOME/golang/src/github.com/onexstack/
$ cd $HOME/golang/src/github.com/onexstack/
$ git clone https://github.com/onexstack/fastgo
$ cd $HOME/golang/src/github.com/onexstack/fastgo
$ mysql -h127.0.0.1 -P3306 -u fastgo -p'fastgo1234'
> source configs/fastgo.sql;
> use fastgo;
Database changed
> show tables;
+--------------------+
| Tables_in_fastgo |
+--------------------+
| post               |
| user               |
+--------------------+
3 rows in set (0.000 sec)
```

## 步骤 4： 安装和配置 fastgo 项目

安装和配置 fastgo 项目步骤如下。

1、在配置文件中添加数据库配置

fastgo 项目启动需要连接数据库，所以需要在配置文件 configs/fg-apiserver.yaml 中配置数据库的 IP、端口、用户名、密码和数据库名信息。configs/fg-apiserver.yaml 配置文件内容如下：

```yaml
# 通用配置
#

# JWT 签发密钥
jwt-key: Rtg8BPKNEf2mB4mgvKONGPZZQSaJWNLijxR42qRgq0iBb5
# JWT Token 过期时间
expiration: 1000h

# MySQL 数据库相关配置
mysql:
  # MySQL 机器 IP 和端口，默认 127.0.0.1:3306
  addr: 127.0.0.1:3306
  # MySQL 用户名(建议授权最小权限集)
  username: fastgo
  # MySQL 用户密码
  password: fastgo1234
  # fastgo 系统所用的数据库名
  database: fastgo
  # MySQL 最大空闲连接数，默认 100
  max-idle-connections: 100
  # MySQL 最大打开的连接数，默认 100
  max-open-connections: 100
  # 空闲连接最大存活时间，默认 10s
  max-connection-life-time: 10s

log:
  format: text
  level: info
  output: stdout
```

2、启动 fg-apiserver 组件

执行以下命令编译并启动 fg-apiserver 组件：

```shell
$ cd $HOME/golang/src/github.com/onexstack/fastgo
$ ./build.sh
$ _output/fg-apiserver -c configs/fg-apiserver.yaml 
...
time=2025-03-10T19:24:23.440+08:00 level=INFO msg="Start to listening the incoming requests on http address" addr=0.0.0.0:6666
```

启动的 API 服务器监听地址为：`0.0.0.0:6666`。

3、测试 fg-apiserver 组件功能是否正常

打开一个新的 Linux 终端，执行以下命令测试 fg-apiserver 中的 API 接口是否正常工作：

```shell
$ cd $HOME/golang/src/github.com/onexstack/fastgo
$ ./scripts/test.sh
```

​	执行结果如下所示：

```shell
root@debian:~/golang/src/github.com/onexstack/fastgo# ./scripts/test.sh
{"userID":"user-a41s36"}
1. 成功创建 fastgo1749599269 用户
{"totalCount":2,"users":[{"userID":"user-a41s36","username":"fastgo1749599269","nickname":"fastgo","email":"colin404@foxmail.com","phone":"1749599269","postCount":0,"createdAt":"2025-06-11T07:47:49+08:00","updatedAt":"2025-06-11T07:47:49+08:00"},{"userID":"user-000000","username":"root","nickname":"colin404","email":"colin404@foxmail.com","phone":"18110000000","postCount":0,"createdAt":"2024-12-12T03:55:25+08:00","updatedAt":"2024-12-12T03:55:25+08:00"}]}
2. 成功列出所有用户
{"user":{"userID":"user-a41s36","username":"fastgo1749599269","nickname":"fastgo","email":"colin404@foxmail.com","phone":"1749599269","postCount":0,"createdAt":"2025-06-11T07:47:49+08:00","updatedAt":"2025-06-11T07:47:49+08:00"}}
3. 成功获取 fastgo1749599269 用户详细信息
{}
4. 成功修改 fastgo1749599269 用户信息
{}
5. 成功删除 fastgo1749599269 用户
==> 所有用户接口测试成功
{"userID":"user-rgw81m"}
1. 成功创建测试用户: fastgo1749599269
2. 成功创建博客: post-yfosdj
{"total_count":1,"posts":[{"postID":"post-yfosdj","userID":"user-rgw81m","title":"installation","content":"installation.","createdAt":"2025-06-11T07:47:50+08:00","updatedAt":"2025-06-11T07:47:50+08:00"}]}
3. 成功列出所有博客
{"post":{"postID":"post-yfosdj","userID":"user-rgw81m","title":"installation","content":"installation.","createdAt":"2025-06-11T07:47:50+08:00","updatedAt":"2025-06-11T07:47:50+08:00"}}
4. 成功获取博客 post-yfosdj 详细信息
{}
5. 成功更新博客 post-yfosdj 信息
{}
6. 成功删除博客 post-yfosdj
{}
7. 成功删除测试用户：fastgo1749599269
==> 所有博客接口测试成功
==> 所有 fastgo 接口测试成功
```

可以看到，fg-apiserver 中的接口，都测试通过。