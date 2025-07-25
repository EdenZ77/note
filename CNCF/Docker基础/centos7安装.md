

# 1. VMware安装

双击安装即可 VMware-workstation-full-16.1.2-17966106.exe，然后在网上随便搜一个许可证就可以使用了。

![image-20211117180049098](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117180049098.png)

# 2. centos7安装

## 2.1 安装虚拟机

1、打开文件，点击新建虚拟机

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117180206368.png" alt="image-20211117180206368" style="zoom:67%;" />

2、直接下一步

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117180254041.png" alt="image-20211117180254041" style="zoom: 67%;" />

3、稍后安装操作系统

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117180337023.png" alt="image-20211117180337023" style="zoom:67%;" />

4、选择Linux操作系统和对应的版本

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117180446159.png" alt="image-20211117180446159" style="zoom:67%;" />

5、选择虚拟机安装的位置并命名

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117180631947.png" alt="image-20211117180631947" style="zoom:67%;" />

6、默认即可

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117181005822.png" alt="image-20211117181005822" style="zoom:67%;" />

7、我们适当调整一下硬件配置，然后点“完成”即可

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117181310381.png" alt="image-20211117181310381" style="zoom:67%;" />

调整内存和处理器。

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117181556031.png" alt="image-20211117181556031" style="zoom:67%;" />

8、选择我们centos7镜像所在位置

![image-20211117182130741](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117182130741.png)

## 2.2 启动虚拟机

9、开启虚拟机

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117182310694.png" alt="image-20211117182310694" style="zoom:67%;" />

10、选择语言

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117212758175.png" alt="image-20211117212758175" style="zoom:67%;" />

11、点击进入，再点击“Done”，然后开始安装。

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117212930260.png" alt="image-20211117212930260" style="zoom:67%;" />

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117213210760.png" alt="image-20211117213210760" style="zoom:67%;" />

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117213343346.png" alt="image-20211117213343346" style="zoom:67%;" />

12、设置密码：123456

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117213439343.png" alt="image-20211117213439343" style="zoom:67%;" />

## 2.3 网络配置

13、重启进入，无法访问外网。

![image-20211117214123218](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117214123218.png)

可以看到并没有IP地址信息

![image-20211118110041106](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211118110041106.png)

14、获取IP地址

这时候，可以输入命令dhclient，可以自动获取一个IP地址，再用命令ip addr查看IP。不过这时候获取的IP是动态的。

![image-20211118110229338](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211118110229338.png)

15、修改网络配置文件

注意：这里的网卡名字”ens33”可能每个人不一样，需要通过ip addr命令进行查看

编辑网络相关配置文件： vi /etc/sysconfig/network-scripts/ifcfg-ens33(这个ens33根据每个人的网卡名填写，例如我的就是ens33)

![image-20211117214904452](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117214904452.png)

这里我们还没有静态化，其实没有前面的dhclient命令，仅仅将这里的no改为yes也是可以的，也会有ip地址了。修改为yes之后重启网络

重启网络：`service network restart`

### 静态化

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20220331210103082.png" alt="image-20220331210103082" style="zoom:50%;" />



## 2.4 关闭防火墙和selinux

查看防火墙状态：systemctl status firewalld   或者  firewall-cmd  --state

停止firewall：systemctl stop firewalld.service

启用：systemctl start firewalld

禁止firewall开机启动：systemctl disable firewalld.service

开机启用：systemctl enable firewalld

vi  /etc/selinux/config
将SELINUX=enforcing改为SELINUX=disabled

## 2.5 配置yum

```
yum clean all
yum makecache（将服务器上的软件包信息 现在本地缓存,以提高搜索安装软件的速度）
yum install -y wget
yum install -y gcc
```

使用yum命令报错Could not retrieve mirrorlist

**验证配置resolv.conf是否配置**

```
vi /etc/resolv.conf
```

添加：

```
// 原来文件
# Generated by NetworkManager
nameserver fe80::1%enp0s3
// 修改为下面两行
nameserver 8.8.8.8
search localdomain
```

同时，切换到国内镜像源（推荐）

```
# 备份原有源配置
sudo mv /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.bak

# 下载阿里云镜像源（CentOS 7）
sudo curl -o /etc/yum.repos.d/CentOS-Base.repo http://mirrors.aliyun.com/repo/Centos-7.repo

# 清理缓存并重建
sudo yum clean all
sudo yum makecache  
```



# 3. VMware克隆与快照

## 3.1 克隆

注意，先要关机

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117215917373.png" alt="image-20211117215917373" style="zoom:67%;" />

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117220015662.png" alt="image-20211117220015662" style="zoom:67%;" />

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117220036800.png" alt="image-20211117220036800" style="zoom:67%;" />

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211117220128693.png" alt="image-20211117220128693" style="zoom:67%;" />

看看克隆的虚拟机，IP地址进行了自动分配，防火墙也是关闭的。

![image-20211118112628471](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211118112628471.png)



## 3.2 快照

进行拍摄快照

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211118113424316.png" alt="image-20211118113424316" style="zoom:67%;" />

通过管理快照可以看到结构，接下来创建一个文件，然后还原到v1版本，看看存不存在创建的文件。

<img src="https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211118113625167.png" alt="image-20211118113625167" style="zoom:67%;" />

![image-20211118114016361](https://eden-typora-picture.oss-cn-hangzhou.aliyuncs.com/img/image-20211118114016361.png)





