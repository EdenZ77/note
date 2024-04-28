# 进程控制：进程的一生

进程是操作系统的一个核心概念。每个进程都有自己唯一的标识：进程ID，也有自己的生命周期。

## 进程ID

Linux下每个进程都会有一个非负整数表示的唯一进程ID，简称pid。Linux提供了getpid函数来获取进程的pid，同时还提供了getppid函数来获取父进程的pid，相关接口定义如下：

```c
#include <sys/types.h>
#include <unistd.h>
pid_t getpid(void);
pid_t getppid(void);
```

每个进程都有自己的父进程，父进程又会有自己的父进程，最终都会追溯到1号进程即init进程。这就决定了操作系统上所有的进程必然会组成树状结构，就像一个家族的家谱一样。可以通过pstree的命令来查看进程的家族树。

procfs文件系统会在/proc下为每个进程创建一个目录，名字是该进程的pid。目录下有很多文件，用于记录进程的运行情况和统计信息等，如下所示：

```shell
root@LVS-OPS-172-22-175-192:/proc# ll /proc
total 4
dr-xr-xr-x 230 root             root                           0 Sep  4  2023 ./
drwxr-xr-x  20 root             root                        4096 Mar  6 14:59 ../
dr-xr-xr-x   9 root             root                           0 Sep  4  2023 1/
dr-xr-xr-x   9 root             root                           0 Apr 26 14:05 10/
dr-xr-xr-x   9 root             root                           0 Apr 26 14:05 1016/
dr-xr-xr-x   9 root             root                           0 Apr 26 14:05 102/
dr-xr-xr-x   9 root             root                           0 Apr 26 14:05 103/
dr-xr-xr-x   9 root             root                           0 Apr 26 14:05 105/
...
```

因为进程有创建，也有终止，所以/proc/下记录进程信息的目录（以及目录下的文件）也会发生变化。

操作系统必须保证在任意时刻都不能出现两个进程有相同pid的情况。虽然进程ID是唯一的，但是进程ID可以重用。进程退出以后，其进程ID还可以再次分配给其他的进程使用。那么问题就来了，内核是如何分配进程ID的？

Linux分配进程ID的算法不同于给进程分配文件描述符的最小可用算法，它采用了延迟重用的算法，即分配给新创建进程的ID尽量不与最近终止进程的ID重复，这样就可以防止将新创建的进程误判为使用相同进程ID的已经退出的进程。

那么如何实现延迟重用呢？内核采用的方法如下：

1）位图记录进程ID的分配情况（0为可用，1为已占用）。
2）将上次分配的进程ID记录到last_pid中，分配进程ID时，从last_pid+1开始找起，从位图中寻找可用的ID。
3）如果找到位图集合的最后一位仍不可用，则回滚到位图集合的起始位置，从头开始找。

既然是位图记录进程ID的分配情况，那么位图的大小就必须要考虑周全。位图的大小直接决定了系统允许同时存在的进程的最大个数，这个最大个数在系统中称为pid_max。

上面的第3步提到，回绕到位图集合的起始位置，从头寻找可用的进程ID。事实上，严格说来，这种说法并不正确，回绕时并不是从0开始找起，而是从300开始找起。内核在 kernel/pid.c 文件中定义了RESERVED_PIDS，其值是300，300以下的pid会被系统占用，而不能分配给用户进程：

```c
define RESERVED_PIDS       300
int pid_max = PID_MAX_DEFAULT;
```

Linux系统下可以通过 procfs 或 sysctl 命令来查看pid_max的值：

```shell
manu@manu-rush:~$ cat /proc/sys/kernel/pid_max
131072
manu@manu-rush:~$ sysctl kernel.pid_max
kernel.pid_max = 131072
```

其实，此上限值是可以调整的，系统管理员可以通过如下方法来修改此上限值：

```shell
root@manu-rush:~# sysctl -w kernel.pid_max=4194304
kernel.pid_max = 4194304
```

但是内核自己也设置了硬上限，如果尝试将pid_max的值设成一个大于硬上限的值就会失败，如下所示：

```shell
root@manu-rush:~# sysctl -w kernel.pid_max=4194305
error: "Invalid argument" setting key "kernel.pid_max"
```

从上面的操作可以看出，Linux系统将系统进程数的硬上限设置为4194304（4M）。内核又是如何决定系统进程个数的硬上限的呢？对此，内核定义了如下的宏：

```c
#define PID_MAX_LIMIT (CONFIG_BASE_SMALL ? PAGE_SIZE * 8 : \
    (sizeof(long) > 4 ? 4 * 1024 * 1024 :PID_MAX_DEFAULT))
```

从上面代码中可以看出决定系统进程个数硬上限的逻辑为：

- 如果选择了CONFIG_BASE_SMALL编译选项，则为页面（PAGE_SIZE）的位数。
- 如果选择了CONFIG_BASE_FULL编译选项，那么：

  - 对于32位系统，系统进程个数硬上限为32768（即32K）。
  - 对于64位系统，系统进程个数硬上限为4194304（即4M）。

通过上面的讨论可以看出，在64位系统中，系统容许创建的进程的个数超过了400万，这个数字是相当庞大的，足够应用层使用。

对于单线程的程序，进程ID比较好理解，就是唯一标识进程的数字。对于多线程的程序，每一个线程调用getpid函数，其返回值都是一样的，即进程的ID。

## 进程的层次

每个进程都有父进程，父进程也有父进程，这就形成了一个以 init 进程为根的家族树。除此以外，进程还有其他层次关系：进程、进程组和会话。

进程组和会话在进程之间形成了两级的层次：进程组是一组相关进程的集合，会话是一组相关进程组的集合。用人来打比方，会话如同一个公司，进程组如同公司里的部门，进程则如同部门里的员工。尽管每个员工都有父亲，但是不影响员工同时属于某个公司中的某个部门。

这样说来，一个进程会有如下ID：

- PID：进程的唯一标识。对于多线程的进程而言，所有线程调用getpid函数会返回相同的值。
- PGID：进程组ID。每个进程都会有进程组ID，表示该进程所属的进程组。默认情况下新创建的进程会继承父进程的进程组ID。
- SID：会话ID。每个进程也都有会话ID。默认情况下，新创建的进程会继承父进程的会话ID。

可以调用如下指令来查看所有进程的层次关系：

```shell
ps -ejH
ps axjf
```

对于进程而言，可以通过如下函数调用来获取其进程组ID和会话ID。

```c
#include <unistd.h>
pid_t getpgrp(void);
pid_t getsid(pid_t pid);
```

前面提到过，新进程默认继承父进程的进程组ID和会话ID，如果都是默认情况的话，那么追根溯源可知，所有的进程应该有共同的进程组ID和会话ID。但是调用 `ps axjf` 可以看到，实际情况并非如此，系统中存在很多不同的会话，每个会话下也有不同的进程组。

为何会如此呢？

就像家族企业一样，如果从创业之初，所有家族成员都墨守成规，循规蹈矩，默认情况下，就只会有一个公司、一个部门。但是也有些“叛逆”的子弟，愿意为家族公司开疆拓土，愿意成立新的部门。这些新的部门就是新创建的进程组。如果有子弟“离经叛道”，甚至不愿意呆在家族公司里，他别开天地，另创了一个公司，那这个新公司就是新创建的会话组。由此可见，系统必须要有改变和设置进程组ID和会话ID的函数接口，否则，系统中只会存在一个会话、一个进程组。

进程组和会话是为了支持shell作业控制而引入的概念。当有新的用户登录Linux时，登录进程会为这个用户创建一个会话。用户的登录shell就是会话的首进程。会话的首进程ID会作为整个会话的ID。会话是一个或多个进程组的集合，囊括了登录用户的所有活动。

在登录shell时，用户可能会使用管道，让多个进程互相配合完成一项工作，这一组进程属于同一个进程组。当用户通过SSH客户端工具（putty、xshell等）连入Linux时，与上述登录的情景是类似的。

### 进程组

修改进程组ID的接口如下：

```c
#include <unistd.h>
int setpgid(pid_t pid, pid_t pgid);
```

这个函数的含义是，找到进程ID为pid的进程，将其进程组ID修改为pgid，如果pid的值为0，则表示要修改调用进程的进程组ID。该接口一般用来创建一个新的进程组。如果pgid的值为0，则表示使用pid参数的值作为新的进程组ID。

下面三个接口含义一致，都是创立新的进程组，并且指定的进程会成为进程组的首进程。如果参数pid和pgid的值不匹配，那么setpgid函数会将一个进程从原来所属的进程组迁移到pgid对应的进程组。

```c
setpgid(0,0)
setpgid(getpid(),0)
setpgid(getpid(),getpid())
```

`setpgid`函数有很多限制：

- pid参数必须指定为调用 `setpgid`函数的进程或其子进程，不能随意修改不相关进程的进程组ID，如果违反这条规则，则返回-1，并置errno为ESRCH。
- pid参数可以指定调用进程的子进程，但是子进程如果已经执行了 `exec`函数，则不能修改子进程的进程组ID。如果违反这条规则，则返回-1，并置errno为EACCESS。
- 在进程组间移动，调用进程，pid指定的进程及目标进程组必须在同一个会话之内。这个比较好理解，不加入公司（会话），就无法加入公司下属的部门（进程组），否则就是部门要造反的节奏。如果违反这条规则，则返回-1，并置errno为EPERM。
- pid指定的进程，不能是会话首进程。如果违反这条规则，则返回-1，并置errno为EPERM。

有了创建进程组的接口，新创建的进程组就不必继承父进程的进程组ID了。最常见的创建进程组的场景就是在shell中执行管道命令，代码如下：

```shell
cmd1 | cmd2 | cmd3
```

下面用一个最简单的命令来说明，其进程之间的关系如图所示。

```shell
ps ax|grep nfsd
```
![](image/2024-04-26-18-22-07.png)

ps进程和grep进程都是bash创建的子进程，两者通过管道协同完成一项工作，它们隶属于同一个进程组，其中ps进程是进程组的组长。

进程组的概念并不难理解，可以将人与人之间的关系做类比。一起工作的同事，自然比毫不相干的路人更加亲近。shell中协同工作的进程属于同一个进程组，就如同协同工作的人属于同一个部门一样。

引入了进程组的概念，可以更方便地管理这一组进程了。比如这项工作放弃了，不必向每个进程一一发送信号，可以直接将信号发送给进程组，进程组内的所有进程都会收到该信号。

前文曾提到过，子进程一旦执行exec，父进程就无法调用setpgid函数来设置子进程的进程组ID了，这条规则会影响shell的作业控制。出于保险的考虑，一般父进程在调用fork创建子进程后，会调用setpgid函数设置子进程的进程组ID，同时子进程也要调用setpgid函数来设置自身的进程组ID。这两次调用有一次是多余的，但是这样做能够保证无论是父进程先执行，还是子进程先执行，子进程一定已经进入了指定的进程组中。由于fork之后，父子进程的执行顺序是不确定的，因此如果不这样做，就会造成在一定的时间窗口内，无法确定子进程是否进入了相应的进程组。

可以通过跟踪bash进程的系统调用来证明这一点，下面的2258进程是bash，我们在该bash上执行sleep 200，在执行之前，在另一个终端用strace跟踪bash的系统调用，可以看到，父进程和子进程都执行了一遍setpgid函数，代码如下所示：

```shell
manu@manu-hacks:~$ sudo strace -f -p 2258
Process 2258 attached
    ．．．
/*父进程调用setpgid函数*/
[pid  2258] setpgid(2509, 2509 <unfinished ...>．．．
/*子进程调用setpgid函数*/
[pid  2509] setpgid(2509, 2509 <unfinished ...>．．．
/*子进程执行execve*/
[pid  2509] execve("/bin/sleep", ["sleep", "200"], [/* 31 vars */]) = 0．．．
```

strace工具说明：`strace` 是 Linux 系统中一个非常有用的命令行工具，它可以跟踪系统调用和信号。

- `-f` 参数告诉 `strace` 跟踪指定进程及其所有的子进程（forked processes）。
- `-p 2258` 参数告诉 `strace` 要附加（attach）到进程号为 2258 的进程，并开始跟踪它的系统调用。

所以，`sudo strace -f -p 2258` 命令的含义是以超级用户权限（sudo）启动 `strace`，并附加到进程号为 2258 的进程，同时监视该进程及其所有子进程的所有系统调用。使用 `strace` 可以帮助你理解进程在运行时与操作系统如何交互，包括它打开的文件、它发出的网络请求、它如何分配内存等等。这是一种强大的方式来诊断程序中的问题或了解程序的行为。

用户在shell中可以同时执行多个命令。对于耗时很久的命令（如编译大型工程），用户不必傻傻等待命令运行完毕才执行下一个命令。用户在执行命令时，可以在命令的结尾添加“&”符号，表示将命令放入后台执行。这样该命令对应的进程组即为后台进程组。在任意时刻，可能同时存在多个后台进程组，但是不管什么时候都只能有一个前台进程组。只有在前台进程组中进程才能在控制终端读取输入。当用户在终端输入信号生成终端字符（如ctrl+c、ctrl+z、ctr+\等）时，对应的信号只会发送给前台进程组。

shell中可以存在多个进程组，无论是前台进程组还是后台进程组，它们或多或少存在一定的联系，为了更好地控制这些进程组（或者称为作业），系统引入了会话的概念。会话的意义在于将很多的工作囊括在一个终端，选取其中一个作为前台来直接接收终端的输入及信号，其他的工作则放在后台执行。

### 会话

会话是一个或多个进程组的集合，以用户登录系统为例，可能存在如图所示的情况。
![](image/2024-04-28-06-23-54.png)

系统提供setsid函数来创建会话，其接口定义如下：

```c
#include <unistd.h>
pid_t setsid(void);
```

如果这个函数的调用进程不是进程组组长，那么调用该函数会发生以下事情：
1）创建一个新会话，会话ID等于进程ID，调用进程成为会话的首进程。
2）创建一个进程组，进程组ID等于进程ID，调用进程成为进程组的组长。
3）该进程没有控制终端，如果调用setsid前，该进程有控制终端，这种联系就会断掉。

调用setsid函数的进程不能是进程组的组长，否则调用会失败，返回-1，并置errno为EPERM。

这个限制是比较合理的。如果允许进程组组长迁移到新的会话，而进程组的其他成员仍然在老的会话中，那么，就会出现同一个进程组的进程分属不同的会话之中的情况，这就破坏了进程组和会话的严格的层次关系了。

Linux提供了setsid命令，可以在新的会话中执行命令，通过该命令可以很容易地验证上面提到的三点：

```shell
manu@manu-hacks:~$ setsid sleep 100
manu@manu-hacks:~$ ps ajxf
PPID   PID  PGID   SID TTY      TPGID STAT   UID   TIME COMMAND…
1     4469  4469  4469 ?           -1   Ss   1000   0:00 sleep 100
```

从输出中可以看出，系统创建了新的会话4469，新的会话下又创建了新的进程组，会话ID和进程组ID都等于进程ID，而该进程已经不再拥有任何控制终端了（TTY对应的值为“？”表示进程没有控制终端）。

常用的调用setsid函数的场景是login和shell。除此以外创建daemon进程也要调用setsid函数。

## 进程的创建之fork()

Linux系统下，进程可以调用fork函数来创建新的进程。调用进程为父进程，被创建的进程为子进程。fork函数的接口定义如下：

```c
#include <unistd.h>
pid_t fork(void);
```

与普通函数不同，fork函数会返回两次。一般说来，创建两个完全相同的进程并没有太多的价值。大部分情况下，父子进程会执行不同的代码分支。fork函数的返回值就成了区分父子进程的关键。fork函数向子进程返回0，并将子进程的进程ID返给父进程。当然了，如果fork失败，该函数则返回-1，并设置errno。

常见的出错情景如表所示。
![](image/2024-04-28-06-46-08.png)

所以一般而言，调用fork的程序，大多会如此处理：

```c
ret = fork();
if(ret == 0)
{
    …//此处是子进程的代码分支
}
else if(ret > 0)
{
    …//此处是父进程的代码分支
}
else
{
     …// fork失败，执行error handle
}
```

注意　fork可能失败。检查返回值进行正确的出错处理，是一个非常重要的习惯。设想如果fork返回-1，而程序没有判断返回值，直接将-1当成子进程的进程号，那么后面的代码执行`kill（child_pid，9）`就相当于执行`kill（-1，9）`。这会发生什么？后果是惨重的，它将杀死除了init以外的所有进程，只要它有权限。读者可以通过`man 2 kill`来查看`kill（-1，9）`的含义。

fork之后，对于父子进程，谁先获得CPU资源，而率先运行呢？从内核2.6.32开始，在默认情况下，父进程将成为fork之后优先调度的对象。采取这种策略的原因是：fork之后，父进程在CPU中处于活跃的状态，并且其内存管理信息也被置于硬件内存管理单元的转译后备缓冲器（TLB），所以先调度父进程能提升性能。从2.6.24起，Linux采用完全公平调度（Completely Fair Scheduler，CFS）。用户创建的普通进程，都采用CFS调度策略。对于CFS调度策略，procfs提供了如下控制选项：

```shell
/proc/sys/kernel/sched_child_runs_first
```

该值默认是0，表示父进程优先获得调度。如果将该值改成1，那么子进程会优先获得调度。POSIX标准和Linux都没有保证会优先调度父进程。因此在应用中，决不能对父子进程的执行顺序做任何的假设。如果确实需要某一特定执行的顺序，那么需要使用进程间同步的手段。

### fork之后父子进程的内存关系

fork之后的子进程完全拷贝了父进程的地址空间，包括栈、堆、代码段等。通过下面的示例代码，我们一起来查看父子进程的内存关系：

```c
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <wait.h>
int g_int = 1;
int main()
{
    int local_int = 1;
    int *malloc_int = malloc(sizeof(int));
    *malloc_int = 1;
    pid_t pid = fork();
    if(pid == 0) /*子进程*/
    {
        local_int = 0;
        g_int = 0;
        *malloc_int = 0;
        fprintf(stderr,"[CHILD ] child change local global malloc value to 0\n");
        free(malloc_int);
        sleep(10);
        fprintf(stderr,"[CHILD ] child exit\n");
        exit(0);
    }
    else if(pid < 0)
    {
        printf("fork failed (%s)",strerror(errno));
        return 1;
    }
    fprintf(stderr,"[PARENT] wait child exit\n");
    waitpid(pid,NULL,0);
    fprintf(stderr,"[PARENT] child have exit\n");
    printf("[PARENT] g_int = %d\n",g_int);
    printf("[PARENT] local_int = %d\n",local_int);
    printf("[PARENT] malloc_int = %d\n",malloc_int);
    free(malloc_int);
    return 0;
}
```

这里刻意定义了三个变量，一个是位于数据段的全局变量，一个是位于栈上的局部变量，还有一个是通过malloc动态分配位于堆上的变量，三者的初始值都是1。然后调用fork创建子进程，子进程将三个变量的值都改成了0。

按照fork的语义，子进程完全拷贝了父进程的数据段、栈和堆上的内存，如果父子进程对相应的数据进行修改，那么两个进程是并行不悖、互不影响的。因此，在上面示例代码中，尽管子进程将三个变量的值都改成了0，对父进程而言这三个值都没有变化，仍然是1，代码的输出也证实了这一点。

```shell
[PARENT] wait child exit
[CHILD ] child change local global malloc value to 0
[CHILD ] child exit
[PARENT] child have exit
[PARENT] g_int = 1
[PARENT] local_int = 1
[PARENT] malloc_int = 1
```

前文提到过，子进程和父进程执行一模一样的代码的情形比较少见。Linux提供了execve系统调用，构建在该系统调用之上，glibc提供了exec系列函数。这个系列函数会丢弃现存的程序代码段，并构建新的数据段、栈及堆。调用fork之后，子进程几乎总是通过调用exec系列函数，来执行新的程序。

在这种背景下，fork时子进程完全拷贝父进程的数据段、栈和堆的做法是不明智的，因为接下来的exec系列函数会毫不留情地抛弃刚刚辛苦拷贝的内存。为了解决这个问题，Linux引入了写时拷贝（copy-on-write）的技术。

写时拷贝是指子进程的页表项指向与父进程相同的物理内存页，这样只拷贝父进程的页表项就可以了，当然要把这些页面标记成只读（如图4-4所示）。如果父子进程都不修改内存的内容，大家便相安无事，共用一份物理内存页。但是一旦父子进程中有任何一方尝试修改，就会引发缺页异常（page fault）。此时，内核会尝试为该页面创建一个新的物理页面，并将内容真正地复制到新的物理页面中，让父子进程真正地各自拥有自己的物理内存页，然后将页表中相应的表项标记为可写。

<img src="image/2024-04-28-21-52-43.png" style="zoom:67%;" />

从上面的描述可以看出，对于没有修改的页面，内核并没有真正地复制物理内存页，仅仅是复制了父进程的页表。这种机制的引入提升了fork的性能，从而使内核可以快速地创建一个新的进程。

从内核代码层面来讲，其调用关系如图4-5所示。

![](image/2024-04-28-21-53-04.png)

Linux的内存管理使用的是四级页表，如图4-6所示，看了四级页表的名字，也就不难推测图4-5中那些函数的作用了。

![](image/2024-04-28-21-53-24.png)

在最后的copy_one_pte函数中有如下代码：

```c
   /*如果是写时拷贝，那么无论是初始页表，还是拷贝的页表，都设置了写保护
    *后面无论父子进程，修改页表对应位置的内存时，都会触发page fault
    */
    if (is_cow_mapping(vm_flags)) {
        ptep_set_wrprotect(src_mm, addr, src_pte);
        pte = pte_wrprotect(pte);
    }
```

该代码将页表设置成写保护，父子进程中任意一个进程尝试修改写保护的页面时，都会引发缺页中断，内核会走向do_wp_page函数，该函数会负责创建副本，即真正的拷贝。

写时拷贝技术极大地提升了fork的性能，在一定程度上让vfork成为了鸡肋。

### fork之后父子进程与文件的关系

执行fork函数，内核会复制父进程所有的文件描述符。对于父进程打开的所有文件，子进程也是可以操作的。那么父子进程同时操作同一个文件是并行不悖的，还是互相影响的呢？

下面通过对一个例子的讨论来说明这个问题。read函数并没有将偏移量作为参数传入，但是每次调用read函数或write函数时，却能够接着上次读写的位置继续读写。原因是内核已经将偏移量的信息记录在与文件描述符相关的数据结构里了。那么问题来了，父子进程是共用一个文件偏移量还是各有各的文件偏移量呢？

```c
/*read 和write 都没有将pos信息作为入参*/
ssize_t read(int fd, void *buf, size_t count);
ssize_t write(int fd, const void *buf, size_t count);
```

我们用事实说话，请看下面的例子：

```c
#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#define INFILE "./in.txt"
#define OUTFILE "./out.txt"
#define MODE  S_IRUSR |S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH
int main(void)
{
    int fd_in,fd_out;
    char buf[1024];
    memset(buf, 0, 1024);
    fd_in = open(INFILE, O_RDONLY);
    if(fd_in < 0 )
    {
        fprintf(stderr,"failed to open %s, reason(%s)\n",
INFILE,strerror(errno));
        return 1;
    }
    fd_out = open(OUTFILE,O_WRONLY|O_CREAT|O_TRUNC,MODE);
    if(fd_out < 0)
    {
        fprintf(stderr,"failed to open %s, reason(%s)\n", OUTFILE,strerror(errno));
        return 1;
    }
    fork();/*此处忽略错误检查*/
    while(read(fd_in, buf, 2) > 0)
    {
        printf("%d: %s",getpid(),buf);
        sprintf(buf, "%d Hello,World!\n",getpid());
        write(fd_out,buf,strlen(buf));
        sleep(1);
        memset(buf, 0, 1024);
    }
}
```

INFILE的内容是：

```
1
2
3
4
5
6
```

上面的程序中，父子进程都会去读INFILE，如果父子进程各维护各的文件偏移量，那么父子进程都会打印出1~6。

事实如何呢？请看输出内容：

```shell
manu@manu-hacks:~/code/self/c/fork$ ./fork_file
6602: 1
6603: 2
6602: 3
6603: 4
6602: 5
6603: 6
```

当然，有时候输出是这样的：

```shell
manu@manu-hacks:~/code/self/c/fork$ ./fork_file
6610: 1
6611: 2
6610: 3
6611: 4
6610: 5
6611: 5
6610: 6
```

如果父子进程各自维护自己的文件偏移量，那么一定是打印出两套1~6，但是事实并非如此。无论父进程还是子进程调用read函数导致文件偏移量后移都会被对方获知，这表明父子进程共用了一套文件偏移量。

对于第二个输出，为什么父子进程都打印5呢？这是因为我的机器是多核的，父子进程同时执行，发现当前文件偏移量是4*2，然后各自去读了第8和第9字节，也就是“5\n”。

写文件也是一样，如果fork之前打开了某文件，之后父子进程写入同一个文件描述符而又不采取任何同步的手段，那么就会因为共享文件偏移量而使输出相互混合，不可阅读。

文件描述符还有一个文件描述符标志（file descriptor flag）。目前只定义了一个标志位：FD_CLOSEXEC。细心阅读open函数手册也会发现，open函数也有一个类似的标志位，即O_CLOSEXEC，该标志位也是用于设置文件描述符标志的。

那么这个标志位到底有什么作用呢？如果文件描述符中将这个标志位置位，那么调用exec时会自动关闭对应的文件。

可是为什么需要这个标志位呢？主要是出于安全的考虑。默认情况下，执行 `execve` (或其它 `exec` 系列函数) 后，所有打开的文件描述符都会被新程序继承。

对于fork之后子进程执行exec这种场景，如果子进程可以操作父进程打开的文件，就会带来严重的安全隐患。一般来讲，调用exec的子进程时，因为它会另起炉灶，因此父进程打开的文件描述符也应该一并关闭，但事实上内核并没有主动这样做。例如，如果你在父进程中打开了一个用于日志记录的文件，并且你不希望在通过 `execve` 调用启动的新程序中继续使用这个文件描述符（可能因为安全或资源管理的考虑），你可以设置 `FD_CLOSEXEC` 标志。这样，在 `execve` 执行时，这个文件描述符会被自动关闭，新程序就不会"继承"这个文件描述符。

为了解决这个问题，Linux引入了close on exec机制。设置了FD_CLOSEXEC标志位的文件，在子进程调用exec家族函数时会将相应的文件关闭。而设置该标志位的方法有两种：

- open时，带上O_CLOSEXEC标志位。
- open时如果未设置，那就在后面调用fcntl函数的F_SETFD操作来设置。

建议使用第一种方法。原因是第二种方法在某些时序条件下并不那么绝对的安全。考虑图4-7的场景：Thread 1还没来得及将FD_CLOSEXEC置位，由于Thread 2已经执行过fork，这时候fork出来的子进程就不会关闭相应的文件。尽管Thread1后来调用了fcntl的F_SETFD操作，但是为时已晚，文件已经泄露了。
<img src="image/2024-04-28-22-24-23.png" style="zoom:50%;" />

前面提到，执行fork时，子进程不仅会获取父进程所有文件描述符的副本，而且测试结果表明，父子进程共享了文件的很多属性。这到底是怎么回事？让我们深入内核一探究竟。

### 文件描述符复制的内核实现

此小节暂时用不着，先跳过。。。

## 进程的创建之vfork()

在早期的实现中，fork没有实现写时拷贝机制，而是直接对父进程的数据段、堆和栈进行完全拷贝，效率十分低下。很多程序在fork一个子进程后，会紧接着执行exec家族函数，这更是一种浪费。所以BSD引入了vfork。既然fork之后会执行exec函数，拷贝父进程的内存数据就变成了一种无意义的行为，所以引入的vfork压根就不会拷贝父进程的内存数据，而是直接共享。再后来Linux引入了写时拷贝的机制，其效率提高了很多，这样一来，vfork其实就可以退出历史舞台了。除了一些需要将性能优化到极致的场景，大部分情况下不需要再使用vfork函数了。

此小节暂时用不着，先跳过。。。



## daemon进程的创建

daemon进程又被称为守护进程，一般来说它有以下两个特点：

- 生命周期很长，一旦启动，正常情况下不会终止，一直运行到系统退出。但凡事无绝对：daemon进程其实也是可以停止的，如很多daemon提供了stop命令，执行stop命令就可以终止daemon，或者通过发送信号将其杀死，又或者因为daemon进程代码存在bug而异常退出。这些退出一般都是由手工操作或因异常引发的。
- 在后台执行，并且不与任何控制终端相关联。即使daemon进程是从终端命令行启动的，终端相关的信号如SIGINT、SIGQUIT和SIGTSTP，以及关闭终端，都不会影响到daemon进程的继续执行。

习惯上daemon进程的名字通常以d结尾，如sshd、rsyslogd等。但这仅仅是习惯，并非一定要如此。如何使一个进程变成daemon进程，或者说编写daemon进程，需要遵循哪些规则或步骤呢？一般来讲，创建一个daemon进程的步骤被概括地称为double-fork magic。细细说来，需要以下步骤。

（1）执行`fork()`函数，父进程退出，子进程继续

执行这一步，原因有二：·

- 父进程有可能是进程组的组长（在命令行启动的情况下），从而不能够执行后面要执行的setsid函数，子进程继承了父进程的进程组ID，并且拥有自己的进程ID，一定不会是进程组的组长，所以子进程一定可以执行后面要执行的setsid函数。
- 如果daemon是从终端命令行启动的，那么父进程退出会被shell检测到，shell会显示shell提示符，让子进程在后台执行。

（2）子进程执行如下三个步骤，以摆脱与环境的关系

1）修改进程的当前目录为根目录（/）。

这样做是有原因的，因为daemon一直在运行，如果当前工作路径上包含有根文件系统以外的其他文件系统，那么这些文件系统将无法卸载。因此，常规是将当前工作目录切换成根目录，当然也可以是其他目录，只要确保该目录所在的文件系统不会被卸载即可。

```c
chdir("/")
```

2）调用setsid函数。这个函数的目的是切断与控制终端的所有关系，并且创建一个新的会话。

这一步比较关键，因为这一步确保了子进程不再归属于控制终端所关联的会话。因此无论终端是否发送SIGINT、SIGQUIT或SIGTSTP信号，也无论终端是否断开，都与要创建的daemon进程无关，不会影响到daemon进程的继续执行。

3）设置文件模式创建掩码为0。

```c
umask(0)
```

这一步的目的是让daemon进程创建文件的权限属性与shell脱离关系。因为默认情况下，进程的umask来源于父进程shell的umask。如果不执行umask（0），那么父进程shell的umask就会影响到daemon进程的umask。如果用户改变了shell的umask，那么也就相当于改变了daemon的umask，就会造成daemon进程每次执行的umask信息可能会不一致。

（3）再次执行fork，父进程退出，子进程继续执行完前面两步之后，可以说已经比较圆满了：新建会话，进程是会话的首进程，也是进程组的首进程。进程ID、进程组ID和会话ID，三者的值相同，进程和终端无关联。那么这里为何还要再执行一次fork函数呢？

原因是，daemon进程有可能会打开一个终端设备，即daemon进程可能会根据需要，执行类似如下的代码：

```c
int fd = open("/dev/console", O_RDWR);
```

这个打开的终端设备是否会成为daemon进程的控制终端，取决于两点：

- daemon进程是不是会话的首进程。
- 系统实现。（BSD风格的实现不会成为daemon进程的控制终端，但是POSIX标准说这由具体实现来决定）。

既然如此，为了确保万无一失，只有确保daemon进程不是会话的首进程，才能保证打开的终端设备不会自动成为控制终端。因此，不得不执行第二次fork，fork之后，父进程退出，子进程继续。这时，子进程不再是会话的首进程，也不是进程组的首进程了。

（4）关闭标准输入（stdin）、标准输出（stdout）和标准错误（stderr）

因为文件描述符0、1和2指向的就是控制终端。daemon进程已经不再与任意控制终端相关联，因此这三者都没有意义。一般来讲，关闭了之后，会打开/dev/null，并执行dup2函数，将0、1和2重定向到/dev/null。这个重定向是有意义的，防止了后面的程序在文件描述符0、1和2上执行I/O库函数而导致报错。

至此，即完成了daemon进程的创建，进程可以开始自己真正的工作了。

上述步骤比较繁琐，对于C语言而言，glibc提供了daemon函数，从而帮我们将程序转化成daemon进程。

```c
#include <unistd.h>
int daemon(int nochdir, int noclose);
```

该函数有两个入参，分别控制一种行为，具体如下。

其中的nochdir，用来控制是否将当前工作目录切换到根目录。

- 0：将当前工作目录切换到/。
- 1：保持当前工作目录不变。

而noclose，用来控制是否将标准输入、标准输出和标准错误重定向到/dev/null。

- 0：将标准输入、标准输出和标准错误重定向到/dev/null。
- 1：保持标准输入、标准输出和标准错误不变。

一般情况下，这两个入参都要为0。

```c
ret = daemon(0,0)
```

成功时，daemon函数返回0；失败时，返回-1，并置errno。因为daemon函数内部会调用fork函数和setsid函数，所以出错时errno可以查看fork函数和setsid函数的出错情形。

glibc的daemon函数做的事情，和前面讨论的大体一致，但是做得并不彻底，没有执行第二次的fork。

此小节暂时用不着，先跳过。。。

## 进程的终止

在不考虑线程的情况下，进程的退出有以下5种方式。

正常退出有3种：

- 从main函数return返回
- 调用exit
- 调用_exit

异常退出有两种：

- 调用abort
- 接收到信号，由信号终止

### _exit函数

_exit函数的接口定义如下：

```c
#include <unistd.h>
void _exit(int status);
```

_exit函数中status参数定义了进程的终止状态，父进程可以通过`wait()`来获取该状态值。

需要注意的是返回值，尽管 `status` 参数是一个 `int` 类型，但是操作系统不会完整地传递这个 `int` 的所有位。实际上，只有低8位（即0-255的范围）会被传递。这是由于历史和兼容性的原因，确保在不同的系统和环境中具有相同的行为。所以写`exit(-1)`结束进程时，在终端执行`$?`会发现返回值是255。（当你使用 `_exit(-1)` 时，这个值会被转换成一个无符号的8位整数，因此 `-1` 实际上会变成 `255`（在二进制表示中，-1 的所有位都是1，截取低8位后即为 `11111111`，对应的十进制值是255）。这就解释了为什么在终端执行 `$?` 后会看到返回值是255。）

如果是shell相关的编程，shell可能需要获取进程的退出值，那么退出值最好不要大于128。如果退出值大于128，会给shell带来困扰。POSIX标准规定了退出状态及其含义如表4-2所示。
![](image/2024-04-29-07-25-29.png)

下面的命令被SIGINT信号（signo=2）中断，返回了130。如程序通过exit返回130，与其配合工作的shell就可能会误判为收到信号而退出。

```c
manu@manu-hacks:~/code/me/exit$ sleep 10000
^C
manu@manu-hacks:~/code/me/exit$ $?
130：未找到命令
```

用户调用_exit函数，本质上是调用exit_group系统调用。这点在前面已经详细介绍过，在此就不再赘述了。

### exit函数

![](image/2024-04-29-07-25-56.png)


### return退出

## 等待子进程

### 僵尸进程

### 等待子进程之wait()

### 等待子进程之waitpid()

### 等待子进程之等待状态值

### 等待子进程之waitid()

### 进程退出和等待的内核实现

## exec家族

前面讨论了进程的创建和退出，exec家族函数在其中犹抱琵琶半遮面，现在是时候让exec家族函数登台亮相了。

整个exec家族有6个函数，这些函数都是构建在execve系统调用之上的。该系统调用的作用是，将新程序加载到进程的地址空间，丢弃旧有的程序，进程的栈、数据段、堆栈等会被新程序替换。

基于execve系统调用的6个exec函数，接口虽然各异，实现的功能却是相同的，首先我们来讲述与系统调用同名的execve函数。

### execve函数

execve函数的接口定义如下：

```c
 #include <unistd.h>
 int execve(const char *filename, char *const argv[], char *const envp[]);
```

其中，参数filename是准备执行的新程序的路径名，可以是绝对路径，也可以是相对于当前工作目录的相对路径。

后面的第二个参数很容易让我们联想到C语言的`main()`函数的第二个参数，事实上格式也是一样的：字符串指针组成的数组，以NULL结束。argv[0]一般对应可执行文件的文件名，也就是filename中的basename（路径名最后一个/后面的部分）。当然如果argv[0]不遵循这个约定也无妨，因为execve可以从第一个参数获取到要执行文件的路径，只要不是NULL即可。

第三个参数与C语言的main函数中的第三个参数envp一样，也是字符串指针数组，以NULL结束，指针指向的字符串的格式为name=value。

在使用 `execve()` 函数时，程序的 PID（进程标识符）**不会改变**。这是因为 `execve()` 并不创建一个新的进程，而是在当前进程的上下文中替换掉现有的程序映像、数据、堆栈和其他进程相关的属性，以新的程序内容进行替换。

此特性是 `execve()` 与 `fork()` 的主要区别之一：

- **`fork()`** 函数用于创建一个新的子进程，这个子进程是父进程的一个副本，并且获得一个新的、唯一的 PID。
- **`execve()`** 函数则是在当前进程的上下文中加载一个新的程序。因此，执行了 `execve()` 后，尽管正在运行的程序已经更改，但进程的标识（如 PID）保持不变。

这就意味着，如果你在一个进程中直接调用 `execve()` 而不先调用 `fork()`，那么当前进程将直接变为新的程序，但进程的 PID 等属性不发生变化。这通常用在需要替换当前执行内容但又不需产生新进程的场景中。

在实际应用中，通常见到的模式是先 `fork()` 然后在子进程中调用 `execve()`。这样，父进程可以继续执行原来的程序，而子进程则加载并执行新的程序，各自拥有不同的 PID。这种方式在实现守护进程、启动新服务等场景中非常常见。

但是也可以不执行fork，单独调用execve函数：

```c
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
int main(void)
{
    char *args[] = {"/bin/ls", "-l",NULL};
    if(execve("/bin/ls",args, NULL) == -1) {
        perror("execve");
        exit(EXIT_FAILURE);
    }
    puts("Never get here");
    exit(EXIT_SUCCESS);
}
```

本着“贵在折腾”的原则，上面写了一个不fork直接调用execve的程序。调用execve后，程序就变成了`/bin/sh -l`。这个程序的输出如下：

```shell
total 16
-rwxr-xr-x 1 root root 8672 Dec 27 20:40 exec_no_fork
-rw-r--r-- 1 root root  288 Dec 27 20:40 exec_no_fork.c
```

我们可以看到，代码段最后的Never get here没有被打印出来，这是因为execve函数的返回是特殊的。如果失败，则会返回-1，但是如果成功，则永不返回，这是可以理解的。execve做的就是斩断过去，奔向新生活的事情，如果成功，自然不可能再返回来，再次执行老程序的代码。所以无须检查execve的返回值，只要返回，就必然是-1。可以从errno判断出出错的原因。出错的可能性非常多，手册提供了19种不同的errno，罗列了22种失败的情景。很难记住，好在大部分都不常见，常见的情况有以下几种：

- EACCESS：这个是我们最容易想到的，就是第一个参数filename，不是个普通文件，或者该文件没有赋予可执行的权限，或者目录结构中某一级目录不可搜索，或者文件所在的文件系统是以MS_NOEXEC标志挂载的。
- ENOENT：文件不存在。
- ETXTBSY：存在其他进程尝试修改filename所指代的文件。
- ENOEXEC：这个错误其实是比较高端的一种错误了，文件存在，也可以执行，但是无法执行，比如说，Windows下的可执行程序，拿到Linux下，调用execve来执行，文件的格式不对，就会返回这种错误。

上面提到的ENOEXEC错误码，其实已经触及了execve函数的核心，即哪些文件是可以执行的，execve系统调用又是如何执行的呢？这些会在execve系统调用的内核实现中详细介绍。

### exec家族

从内核的角度来说，提供execve系统调用就足够了，但是从应用层编程的角度来讲，execve函数就并不那么好使了：

- 第一个参数必须是绝对路径或是相对于当前工作目录的相对路径。习惯在shell下工作的用户会觉得不太方便，因为日常工作都是写ls和mkdir之类命令的，没有人会写/bin/ls或/bin/mkdir。shell提供了环境变量PATH，即可执行程序的查找路径，对于位于查找路径里的可执行程序，我们不必写出完整的路径，很方便，而execve函数享受不到这个福利，因此使用不便。
- execve函数的第三个参数是环境变量指针数组，用户使用execve编程时不得不自己负责环境变量，书写大量的“key=value”，但大部分情况下并不需要定制环境变量，只需要使用当前的环境变量即可。

正是为了提供相应的便利，所以用户层提供了6个函数，当然，这些函数本质上都是调用execve系统调用，只是使用的方法略有不同，代码如下：

```c
#include <unistd.h>
extern char **environ;
int execl(const char *path, const char *arg, ...);
int execlp(const char *file, const char *arg, ...);
int execle(const char *path, const char *arg,
                  ..., char * const envp[]);
int execv(const char *path, char *const argv[]);
int execvp(const char *file, char *const argv[]);
int execve(const char *path, char *const argv[], char *const envp[]);
```

上述6个函数分成上下两个半区。分类的依据是参数采用列表（l，表示list）还是数组（v，表示vector）。上半区采用列表，它们会罗列所有的参数，下半区采用数组。在每个半区之中，带p的表示可以使用环境变量PATH，带e的表示必须要自己维护环境变量，而不使用当前环境变量，具体见表4-10。
![](image/2024-04-28-22-50-54.png)

举个例子来加深记忆：

```c
#include <unistd.h>
char *const ps_argv[] = {"ps","-ax",NULL};
char *const ps_envp[] = {"PATH=/bin:/usr/bin","TERM=console",NULL};
execl("/bin/ps","ps","-ax",NULL);
/*带p的，可以使用环境变量PATH，无须写全路径*/
execlp("ps","ps","-ax",NULL);
/*带e的需要自己组拼环境变量*/
execle("/bin/ps","ps","-ax",NULL,ps_envp);
execv("/bin/ps",ps_argv);
/*带p的，可以使用环境变量PATH，无须写全路径*/
execvp("ps",ps_argv);
/*带e的需要自己组拼环境变量*/
execve("/bin/ps",ps_argv,ps_envp);
```

### execve系统调用的内核实现

前面提到的ENOEXEC错误表示内核不知道如何执行对应的可执行文件。Linux支持很多种可执行文件的格式，有渐渐退出历史舞台的a.out格式，有比较通用的ELF格式的文件，还有shell脚本文件、python脚本、java文件、php文件等。对于这些形形色色的可执行文件，内核该如何正确地执行呢？直接将Windows平台上的可执行文件拷贝到Linux下，Linux为什么不能执行（假设没有wine这个执行Windows程序的工具）？这是本节需要解决问题。要解决上述问题，首先还是需要深入内核。

execve是平台相关的系统调用，刨去我们不太关心的平台差异，内核都会走到do_execve_common函数这一步。

```c
static int do_execve_common(const char *filename,
        struct user_arg_ptr argv,
        struct user_arg_ptr envp,
        struct pt_regs *regs)
{
    struct linux_binprm *bprm;
    struct file *file;
    struct files_struct *displaced;
    bool clear_in_exec;
    int retval;
    const struct cred *cred = current_cred();
    if ((current->flags & PF_NPROC_EXCEEDED) &&
            atomic_read(&cred->user->processes) > rlimit(RLIMIT_NPROC)) {
        retval = -EAGAIN;
        goto out_ret;
    }
    /* We're below the limit (still or again), so we don't want to make
     * further execve() calls fail. */
    current->flags &= ~PF_NPROC_EXCEEDED;
    retval = unshare_files(&displaced);
    if (retval)
        goto out_ret;
    retval = -ENOMEM;
    bprm = kzalloc(sizeof(*bprm), GFP_KERNEL);
    if (!bprm)
        goto out_files;
    retval = prepare_bprm_creds(bprm);
    if (retval)
        goto out_free;
    retval = check_unsafe_exec(bprm);
    if (retval < 0)
        goto out_free;
    clear_in_exec = retval;
    current->in_execve = 1;
    /*读取可执行文件*/
    file = open_exec(filename);
    retval = PTR_ERR(file);
    if (IS_ERR(file))
        goto out_unmark;
    /*选择负载最小的CPU来执行新程序*/
    sched_exec();
    bprm->file = file;
    bprm->filename = filename;
    bprm->interp = filename;
    retval = bprm_mm_init(bprm);
    if (retval)
        goto out_file;
    bprm->argc = count(argv, MAX_ARG_STRINGS);
    if ((retval = bprm->argc) < 0)
        goto out;
    bprm->envc = count(envp, MAX_ARG_STRINGS);
    if ((retval = bprm->envc) < 0)
        goto out;/*填充linux_binprm数据结构*/
    retval = prepare_binprm(bprm);
    if (retval < 0)
        goto out;
    /*接下来的3个copy用来拷贝文件名、命令行参数和环境变量*/
    retval = copy_strings_kernel(1, &bprm->filename, bprm);
    if (retval < 0)
        goto out;
    bprm->exec = bprm->p;
    retval = copy_strings(bprm->envc, envp, bprm);
    if (retval < 0)
        goto out;
    retval = copy_strings(bprm->argc, argv, bprm);
    if (retval < 0)
        goto out;
    /*核心部分，遍历formats链表，尝试每个load_binary函数*/retval = search_binary_handler(bprm,regs);
    if (retval < 0)
        goto out;
    /* execve succeeded */
    current->fs->in_exec = 0;
    current->in_execve = 0;
    acct_update_integrals(current);
    free_bprm(bprm);
    if (displaced)
        put_files_struct(displaced);
    return retval;
out:
    if (bprm->mm) {
        acct_arg_size(bprm, 0);
        mmput(bprm->mm);
    }
out_file:
    if (bprm->file) {
        allow_write_access(bprm->file);
        fput(bprm->file);
    }
out_unmark:
    if (clear_in_exec)
        current->fs->in_exec = 0;
    current->in_execve = 0;
out_free:
    free_bprm(bprm);
out_files:
    if (displaced)
        reset_files_struct(displaced);
out_ret:
    return retval;
}
```

其中，linux_binprm是重要的结构体，它与稍后提到的linux_binfmt联手，支持了Linux下多种可执行文件的格式。首先，内核会将程序运行需要的参数argv和环境变量搜集到linux_binprm结构体中，比较关键的一步是：

```c
 retval = prepare_binprm(bprm);
```

在prepare_binprm函数中读取可执行文件的头128个字节，存放在linux_binprm结构体的`buf[BINPRM_BUF_SIZE]`中。我们知道日常写shell脚本、python脚本的时候，总是会在第一行写下如下语句：

```shell
#！/bin/bash
#! /usr/bin/python
#！/usr/bin/env python
```

开头的#！被称为shebang，又被称为sha-bang、hashbang等，指的就是脚本中开始的字符。在类Unix操作系统中，运行这种程序，需要相应的解释器。使用哪种解释器，取决于shebang后面的路径。`#！`后面跟随的一般是解释器的绝对路径，或者是相对于当前工作目录的相对路径。格式如下所示：

```sh
#! interpreter [optional-arg]
```

解释器是绝对路径或是相对于当前工作目录的相对路径，这就给脚本的可移植性带来了挑战。以python的解释器为例，python可能位于/usr/bin/python，也可能位于/usr/local/bin/python，甚至有的还位于/home/username/bin/python。这样编写的脚本在新的环境里面运行时，用户就不得不修改脚本了，当大量的脚本移植到新环境中运行时，修改量是巨大的。为了解决这个问题，系统又引入了如下格式：

```sh
#！/usr/bin/env python
```

在执行时，这种格式会从环境变量$PATH中查找python解释器。如果存在多个版本的解释器，则会按照$PATH中查找路径的顺序来查找。

```shell
manu@manu-hacks：~$ echo $PATH
/home/manu/bin:/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games
```

如果执行方式是./python_script的方式，就会优先查找/home/manu/bin/python，/usr/local/bin/python次之……如下所示：

```c
execve("/home/manu/bin/python", ["python", "./hello.py"], [/* 25 vars */]) = -1 ENOENT (No such file or directory)
execve("/usr/local/bin/python", ["python", "./hello.py"], [/* 25 vars */]) = -1 ENOENT (No such file or directory)
execve("/usr/local/sbin/python", ["python", "./hello.py"], [/* 25 vars */]) = -1 ENOENT (No such file or directory)
execve("/usr/local/bin/python", ["python", "./hello.py"], [/* 25 vars */]) = -1 ENOENT (No such file or directory)
execve("/usr/sbin/python", ["python", "./hello.py"], [/* 25 vars */]) = -1 ENOENT (No such file or directory)
execve("/usr/bin/python", ["python", "./hello.py"], [/* 25 vars */]) = 0
```

上面提到的是脚本文件，除此以外，还有其他格式的文件。Linux平台上最主要的可执行文件格式是ELF格式，当然还有出现较早，逐渐退出历史舞台的的a.out格式，这些文件的特点是最初的128字节中都包含了可执行文件的属性的重要信息。比如图4-14中ELF格式的可执行文件，开头4字节为7F 45（E）4C（L）46（F）。

```shell
manu@manu-hacks：~$  file hello
hello: ELF 64-bit LSB  executable, x86-64, version 1 (SYSV), dynamically linked 
    (uses shared libs), for GNU/Linux 2.6.24, BuildID[sha1]=657d5ef3eab6741481bb219ef6c2fb21f8e91b51, not stripped
```

此小节暂时用不着，先跳过。。。



### exec与信号

exec系列函数，会将现有进程的所有文本段抛弃，直接奔向新生活。调用exec之前，进程可能执行过signal或sigaction，为某些信号注册了新的信号处理函数。一旦决裂，这些新的信号处理函数就无处可寻了。所以内核会为那些曾经改变信号处理函数的信号负责，将它们的处理函数重新设置为SIG_DFL。

这里有一个特例，就是将处理函数设置为忽略（SIG_IGN）的SIGCHLD信号。调用exec之后，SIGCHLD的信号处理函数是保持为SIG_IGN还是重置成SIG_DFL，SUSv3语焉不详，这点要取决于操作系统。对于Linux系统而言，采用的是前者：保持为SIG_IGN。

### 执行exec之后进程继承的属性

执行exec的进程，其个性虽然叛逆，与过去做了决裂，但是也继承了过去的一些属性。exec运行之后，与进程相关的ID都保持不变。如果进程在执行exec之前，设置了告警（如调用了alarm函数），那么在告警时间到时，它仍然会产生一个信号。在执行exec后，挂起信号依然保留。创建文件时，掩码umask和执行exec之前一样。表4-11给出了执行exec之后进程继承的属性。
![](image/2024-04-28-22-53-04.png)

通过fork创建的子进程继承的属性和执行exec之后进程保持的属性，两相比较，差异不小。对于fork而言：

- 告警剩余时间：不仅仅是告警剩余时间，还有其他定时器（setitimer、timer_create等），fork创建的子进程都不继承。
- 进程挂起信号：子进程会将挂起信号初始化为空。
- 信号量调整值semadj：子进程不继承父进程的该值，详情请见进程间通信的相关章节。
- 记录锁（fcntl）：子进程不继承父进程的记录锁。比较有意思的地方是文件锁flock子进程是继承的。
- 已用的时间times：子进程将该值初始化成0。






## system函数

前面提到了fork函数、exec系列函数、wait系列函数。库将这些接口糅合在一起，提供了一个system函数。程序可以通过调用system函数，来执行任意的shell命令。相信很多程序员都用过system函数，因为它起到了一个粘合剂的作用，可以让C程序很方便地调用其他语言编写的程序。同时，相信有很多程序员被system函数折磨过，当出现错误时，如何根据system函数的返回值，定位失败的原因是个比较头疼的问题。下面我们来细细展开。

### system函数接口

system函数的接口定义如下：

```c
#include <stdlib.h>
int system(const char *command);
```

这里将需要执行的命令作为command参数，传给system函数，该函数就帮你执行该命令。这样看来system最大的好处就在于使用方便。不需要自己来调用fork、exec和waitpid，也不需要自己处理错误，处理信号，方便省心。

但是system函数的缺点也是很明显的。首先是效率，使用system运行命令时，一般要创建两个进程，一个是shell进程，另外一个或多个是用于shell所执行的命令。如果对效率要求比较高，最好是自己直接调用fork和exec来执行既定的程序。

从进程的角度来看，调用system的函数，首先会创建一个子进程shell，然后shell会创建子进程来执行command，如图4-15所示。
<img src="image/2024-04-28-22-56-05.png" style="zoom:50%;" />

调用system函数后，命令是否运行成功是我们最关心的事情。但是system的返回值比较复杂，下面通过一个简化的不完备（没有处理信号）的system实现来讲述system函数的返回值，代码如下：

```c
#include<unistd.h>
#include<sys/wait.h>
#include<sys/types.h>
int system(char* command)
{
    int status ;
    pid_t child;
    switch(child = fork())
    {
        case -1:
            return -1;
        case 0:
            execl("/bin/sh),"sh","-c",command,NULL);
            _exit(127);
        default:
            while(waitpid(child,&status,0) < 0)
            {
                /*如果系统调用被中断，则重启系统调用*/
                if(errno != EINTR)
                {
                        status = -1;
                        break;}
            }
            else
                return status;
    }
}
```

下面我们来分别讲述system函数的返回值。

（1）当command为NULL时，返回0或1

正常情况下，不会这样用system。但是command为NULL是有用的，用户可以通过调用system（NULL）来探测shell是否可用。如果shell存在并且可用，则返回1，如果系统里面压根就没有shell，这种情况下，shell就是不可用的，返回0。那么何种情况下shell不可用呢？比如system函数运行在非Unix系统上，再比如程序调用system之前，执行过了chroot，这些情况下shell都可能无法使用。

command为NULL的情况从简化版的代码段中看不出来，但是从glibc的system函数源码中可以看出端倪：

```c
glibc-2.17/sysdeps/posix/system.c
----------------------------------
int
__libc_system (const char *line)
{
  if (line == NULL)
    return do_system ("exit 0") == 0;
    ……
}
weak_alias (__libc_system, system)
```

（2）创建进程（fork）失败，或者获取子进程终止状态（waitpid）失败，则返回-1

创建进程失败的情况比较少见，比较容易想到的也就是创建了太多的进程，超出了系统的限制。但是等待子进程终止状态失败，是比较容易造出来的。

前面讲过，子进程退出的时候，如果SIGCHLD的信号处理函数是SIG_IGN或用户设置了SA_NOCLDWAIT标志位，那么子进程就不进入僵尸状态等待父进程wait了，直接自行了断，灰飞烟灭。但是system函数的内部实现会调用waitpid来获取子进程的退出状态。这就是父子之前没有协调好造成的错误。这种情况下，system返回-1，errno为ECHLD。

这种错误的示范代码如下：

```c
signal(SIGCHLD,SIG_IGN);/*返回-1的根源在于此处*/
if((status = system(command) )<0)
{
    fprintf(stderr,"system return %d (%s)\n",
status,strerror(errno));
    return -2;
}
```

这种情况下，总是返回-1，错误码是ECHLD，如下所示：

```c
manu@manu-hacks:~$ ./t_sys_err "ls"
system_return.c  t_sys    t_sys_err  t_sys_null  t_system.c  t_system_null.c
system return -1 (No child processes)
```








### system函数与信号







## 总结

# 进程控制：状态、调度和优先级

## 进程的状态

## 进程调度概述

## 普通进程的优先级

## 完全公平调度的实现

## 普通进程的组调度

## 实时进程

## CPU的亲和力

# 信号

## 信号的完整生命周期

## 信号的产生

### 硬件异常

### 终端相关的信号

### 软件事件相关的信号

## 信号的默认处理函数

## 信号的分类

## 传统信号的特点

## 信号的可靠性

## 信号的安装

## 信号的发送

## 信号与线程的关系

## 等待信号

## 通过文件描述符来获取信号

## 信号递送的顺序

## 异步信号安全

## 总结
