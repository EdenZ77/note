## 2026-01-09

PyCharm的安装与破解，方法与IDEA一样，且两个版本都是2025.3.1版本

> 廖雪峰学习教程：https://liaoxuefeng.com/books/python/introduction/index.html



## python基础

### list和tuple

list是一种有序的集合，可以随时添加和删除其中的元素。

```python
>>> classmates = ['Michael', 'Bob', 'Tracy']
>>> classmates
['Michael', 'Bob', 'Tracy']
```

变量`classmates`就是一个list。用`len()`函数可以获得list元素的个数：

```python
>>> len(classmates)
3
```

list是一个可变的有序表，所以，可以往list中追加元素到末尾：

```python
>>> classmates.append('Adam')
>>> classmates
['Michael', 'Bob', 'Tracy', 'Adam']
```

也可以把元素插入到指定的位置，比如索引号为`1`的位置：

```python
>>> classmates.insert(1, 'Jack')
>>> classmates
['Michael', 'Jack', 'Bob', 'Tracy', 'Adam']
```

要删除list末尾的元素，用`pop()`方法：

```python
>>> classmates.pop()
'Adam'
>>> classmates
['Michael', 'Jack', 'Bob', 'Tracy']
```

要删除指定位置的元素，用`pop(i)`方法，其中`i`是索引位置：

```python
>>> classmates.pop(1)
'Jack'
>>> classmates
['Michael', 'Bob', 'Tracy']
```

要把某个元素替换成别的元素，可以直接赋值给对应的索引位置：

```python
>>> classmates[1] = 'Sarah'
>>> classmates
['Michael', 'Sarah', 'Tracy']
```

list里面的元素的数据类型也可以不同，比如：

```python
>>> L = ['Apple', 123, True]
```

list元素也可以是另一个list，比如：

```python
>>> s = ['python', 'java', ['asp', 'php'], 'scheme']
>>> len(s)
4
```



tuple和list非常类似，但是tuple一旦初始化就不能修改，比如同样是列出同学的名字：

```python
>>> classmates = ('Michael', 'Bob', 'Tracy')
```

现在，classmates这个tuple不能变了，它也没有append()，insert()这样的方法。其他获取元素的方法和list是一样的，你可以正常地使用`classmates[0]`，`classmates[-1]`，但不能赋值成另外的元素。

不可变的tuple有什么意义？因为tuple不可变，所以代码更安全。如果可能，能用tuple代替list就尽量用tuple。



### dict和set







## 函数









## 高级特性