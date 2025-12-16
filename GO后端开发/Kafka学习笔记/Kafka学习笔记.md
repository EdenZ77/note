# 架构

<img src="image/image-20251215082422811.png" alt="image-20251215082422811" style="zoom:50%;" />

Consumer Group（CG）： 消费者组，由多个 consumer 组成。 消费者组内每个消费者负责消费同一topic的不同分区的数据，一个分区只能由一个组内消费者消费；消费者组之间互不影响。 所有的消费者都属于某个消费者组，即消费者组是逻辑上的一个订阅者。 

Broker： 一台 Kafka 服务器就是一个 broker。一个集群由多个 broker 组成。一个 broker 可以容纳多个 topic。  

Topic： 可以理解为一个队列， 生产者和消费者面向的都是一个 topic。

Partition： 为了实现扩展性，一个非常大的 topic 可以分布到多个 broker（即服务器）上， 一个 topic 可以分为多个 partition，每个 partition 是一个有序的队列。  

Replica： 副本。 一个 topic 的每个分区都有若干个副本，一个 Leader 和若干个Follower。  

Leader： 每个分区多个副本的“主”，生产者发送数据的对象，以及消费者消费数据的对象都是 Leader。  

Follower： 每个分区多个副本中的“从”，实时从 Leader 中同步数据，保持和Leader 数据的同步。 Leader 发生故障时，某个 Follower 会成为新的 Leader。  

# 生产者

在消息发送的过程中，涉及到了两个线程——main 线程和 sender 线程。在 main 线程中创建了一个双端队列 RecordAccumulator。 main 线程将消息发送给 RecordAccumulator，sender 线程不断从 RecordAccumulator 中拉取消息发送到 Kafka Broker。  

![image-20251215203621415](image/image-20251215203621415.png)

## 生产者分区

1） 便于合理使用存储资源， 每个Partition在一个Broker上存储， 可以把海量的数据按照分区切割成一块一块数据存储在多台Broker上。

2） 提高并行度， 生产者可以以分区为单位发送数据；消费者可以以分区为单位进行消费数据。  

<img src="image/image-20251215202815356.png" alt="image-20251215202815356" style="zoom:50%;" />

生产者发送消息的分区策略：默认的分区器 DefaultPartitioner

![image-20251215203412869](image/image-20251215203412869.png)



## 生产经验

### 生产者如何提高吞吐量

<img src="image/image-20251215204458805.png" alt="image-20251215204458805" style="zoom:50%;" />

### 数据可靠性

<img src="image/image-20251215205006967.png" alt="image-20251215205006967" style="zoom:50%;" />

<img src="image/image-20251215210256241.png" alt="image-20251215210256241" style="zoom:50%;" />

<img src="image/image-20251215210522311.png" alt="image-20251215210522311" style="zoom:50%;" />





### 数据去重、幂等、事务

<img src="image/image-20251215213647406.png" alt="image-20251215213647406" style="zoom:50%;" />

<img src="image/image-20251215214050630.png" alt="image-20251215214050630" style="zoom:50%;" />

**PID（Producer ID）**

- 是什么：Broker为每个幂等生产者实例分配的全局唯一标识符。
- 关键限制：PID与生产者会话绑定。如果生产者客户端重启，通常会获取到一个新的PID（除非启用事务并配置了`transactional.id`）。
- 作用：标识消息的来源生产者。

**Partition（分区号）** 

- 是什么：消息要发送到的目标Topic的分区编号。 
- 作用：将消息的幂等性判断限定在同一个分区内。同一个生产者（PID）发送到不同分区的消息，其序列号是独立计算的。

**Sequence Number（序列号，SN）** 

- 是什么：生产者针对每个<PID, Partition> 单调递增的序号。从0开始，每条消息发送时+1。 
- 作用：这是去重判断的核心。Broker会为每个<PID, Partition>对在内存中维护一个已收到的最新序列号。

<img src="image/image-20251215214117367.png" alt="image-20251215214117367" style="zoom:50%;" />

Kafka 事务是在幂等性基础上构建的，提供了跨分区、跨会话的原子性写入能力，实现了 "精确一次"（Exactly-Once） 语义。







### 数据有序

<img src="image/image-20251216061826309.png" alt="image-20251216061826309" style="zoom:50%;" />



### 数据乱序

<img src="image/image-20251216061907798.png" alt="image-20251216061907798" style="zoom:50%;" />

# Broker



## 高效读写数据

1）Kafka 本身是分布式集群，可以采用分区技术，并行度高

2）读数据采用稀疏索引， 可以快速定位要消费的数据

3）顺序写磁盘
Kafka 的 producer 生产数据，要写入到 log 文件中，写的过程是一直追加到文件末端，为顺序写。 官网有数据表明，同样的磁盘，顺序写能到 600M/s，而随机写只有 100K/s。这与磁盘的机械机构有关，顺序写之所以快，是因为其省去了大量磁头寻址的时间。 

4）页缓存 + 零拷贝技术

<img src="image/image-20251216064510743.png" alt="image-20251216064510743" style="zoom:50%;" />

# 消费者

## Kafka消费方式

<img src="image/image-20251216065109690.png" alt="image-20251216065109690" style="zoom:50%;" />

## Kafka消费者工作流程

<img src="image/image-20251216065157178.png" alt="image-20251216065157178" style="zoom:50%;" />

<img src="image/image-20251216093427327.png" alt="image-20251216093427327" style="zoom:50%;" />

<img src="image/image-20251216093534860.png" alt="image-20251216093534860" style="zoom:50%;" />



## 分区的分配以及再平衡

<img src="image/image-20251216070723367.png" alt="image-20251216070723367" style="zoom: 50%;" />

### Range

<img src="image/image-20251216070817780.png" alt="image-20251216070817780" style="zoom:50%;" />

### RoundRobin

<img src="image/image-20251216071033513.png" alt="image-20251216071033513" style="zoom:50%;" />



## offset位移

<img src="image/image-20251216071339255.png" alt="image-20251216071339255" style="zoom:50%;" />

__consumer_offsets 主题里面采用 key 和 value 的方式存储数据。 key 是 group.id+topic+分区号， value 就是当前 offset 的值。 每隔一段时间， kafka 内部会对这个 topic 进行compact，也就是每个 group.id+topic+分区号就保留最新数据。  

思想： __consumer_offsets 为 Kafka 中的 topic，那就可以通过消费者进行消费。

在配置文件 config/consumer.properties 中添加配置 exclude.internal.topics=false，默认是 true，表示不能消费系统主题。为了查看该系统主题数据，所以该参数修改为 false。  

<img src="image/image-20251216071918901.png" alt="image-20251216071918901" style="zoom:50%;" />

<img src="image/image-20251216072248717.png" alt="image-20251216072248717" style="zoom:50%;" />

重复消费： 已经消费了数据，但是 offset 没提交。

漏消费： 先提交 offset 后消费，有可能会造成数据的漏消费。

<img src="image/image-20251216101420305.png" alt="image-20251216101420305" style="zoom:50%;" />

思考：怎么能做到既不漏消费也不重复消费呢？ 详看消费者事务。

## 消费者事务

<img src="image/image-20251216101547302.png" alt="image-20251216101547302" style="zoom:50%;" />

## 数据积压

<img src="image/image-20251216101626873.png" alt="image-20251216101626873" style="zoom:50%;" />



# Kraft模式





# 调优