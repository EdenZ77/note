# 读操作为什么要加锁

如果在一个并发系统中，一个协程在对共享数据进行加锁并修改，而另一个协程在没有加锁的情况下访问这个共享数据，那么第二个协程是能够访问到共享数据的。而且，这种访问可能会导致读取到修改过程中的中间状态，导致数据不一致的问题。这种情况是典型的竞争条件（race condition）问题，会导致很多潜在的并发错误。

## 情景分析

假设有以下共享数据和两种不同的协程操作：

- **协程 A**：对共享数据进行加锁并修改。
- **协程 B**：不加锁直接访问共享数据。

## 示例代码

下面是一个简单的示例，展示了这种情况可能发生的竞争条件问题：

```go
package main

import (
	"fmt"
	"sync"
	"time"
)

type SharedData struct {
	mu   sync.Mutex
	data int64
}

func (s *SharedData) Modify() {
	s.mu.Lock()
	defer s.mu.Unlock()
	// 模拟修改数据过程
	for i := int64(0); i < 10000000; i++ {
		s.data += 1
		if i == 500 {
			time.Sleep(1 * time.Millisecond)
		}
	}
	s.data = 42 // 最终修改为42
}

func (s *SharedData) Read() int64 {
	// 不加锁直接访问
	return s.data
}

func main() {
	shared := &SharedData{data: 0}

	var wg sync.WaitGroup
	wg.Add(2)

	// 协程 A：修改数据
	go func() {
		defer wg.Done()
		shared.Modify()
	}()

	// 协程 B：读取数据
	go func() {
		defer wg.Done()
		time.Sleep(1 * time.Millisecond) // 确保协程 A 开始修改
		fmt.Println("Read data:", shared.Read())
	}()

	wg.Wait()
	fmt.Println("Final data:", shared.data)
}
```

## 运行结果

在这个示例中，`协程 A` 会对共享数据进行加锁并修改，而 `协程 B` 会在没有加锁的情况下读取数据。由于 `协程 B` 不加锁直接访问数据，它可能会读取到 `协程 A` 修改过程中的中间状态，导致数据不一致。

```sh
Read data: <中间状态的值>
Final data: 42
```

## 解释

1. **加锁的协程 A**：
   - `协程 A` 在进入临界区修改数据时，使用了互斥锁 `Lock` 和 `Unlock`。
   - 这确保了在 `Modify` 方法执行期间，没有其他加锁的协程能够访问或修改共享数据。

2. **不加锁的协程 B**：
   - `协程 B` 直接读取数据，而不使用互斥锁。
   - 由于没有加锁保护，`协程 B` 可能会在 `协程 A` 修改数据的过程中读取到不一致的中间状态。

## 解决方案

为了避免这种情况，所有对共享数据的访问（包括读和写）都应该使用互斥锁保护。下面是修正后的示例：

```go

func (s *SharedData) Read() int64 {
	// 加锁访问
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.data
}
```

结果：

```sh
Read data: 42
Final data: 42
```

