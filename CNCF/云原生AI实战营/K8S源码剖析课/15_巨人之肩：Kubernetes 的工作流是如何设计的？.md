本节课，我来介绍下 Kubernetes 的源码贡献流程，其实就是标准的 GitHub 工作流。如果我们未来想给 Kubernetes 贡献源码，需要遵照 Kubernetes 的源码贡献流程。另外因为 Kubernetes 的 GitHub 工作流，非常规范且详细，对于未来我们自己设计 GitHub 工作流也是很有参考意义的。

OneX 项目的 GitHub 工作流，就参考了 Kubernetes 的工作流设计和步骤。接下来，我们来详细看下 Kubernetes GitHub 工作流的具体操作。

## Kubernetes 项目工作流设计

Kubernetes 项目遵循标准的 GitHub 工作流，Kuberntes 社区给出了一个流程图，详细的说明了整个流程。流程图如下：

![img](image/FoE8HijQnlbn02HwW5naNYhB1ltJ)

接下来，我们来看一下具体的操作流程。

## 步骤 1：在 GitHub上 Fork Kubernetes 项目

具体操作如下：

1. 访问 https://github.com/kubernetes/kubernetes；
2. 点击右上角的 Fork 按钮，Fork Kubernetes 项目。

## 步骤 2：克隆 Fork 项目到本地

执行以下命令，将 Fork 项目克隆到本地指定的路径：

```shell
$ export working_dir="$(go env GOPATH)/src/github.com/colin404" # 设置 kubernetes 仓库存放的目标目录
$ export user=<your github profile name> # 注意，这里需要设置 user 为你的 GitHub 用户名，例如：colin404
$ mkdir -p $working_dir
$ cd $working_dir
$ git clone https://github.com/$user/kubernetes.git # 克隆 Fork 的 kubernetes 项目到本地
$ cd $working_dir/kubernetes
$ git remote add upstream https://github.com/kubernetes/kubernetes.git # 添加一个名为 upstream 的远程仓库到本地 Git 仓库中
$ git remote set-url --push upstream no_push # 设置永不向 upstream 推送代码变更
$ git remote -v # 确认远程仓库设置正确
```

在 GitHub 中，"上游"（upstream）通常指代原始仓库，而 "origin" 指代你 fork 出来的仓库。通过这种方式，你可以方便地与原始仓库保持同步，同时在自己的仓库中进行修改和开发。

另外，要禁止向 upstream 推送代码，如果想给 upstream 仓库提交代码贡献，需要通过 PR 的方式，这个后面会介绍。

## 步骤 3：同步上游 master 分支最新代码

在创建工作分支之前，我们还需要将本地的主分支更新到最新的状态，更新方法如下：

```shell
$ cd $working_dir/kubernetes # 切换到 kubernetes 项目仓库所在的目录
$ git fetch upstream # 从远程仓库 upstream 中获取最新的更改，但不会自动合并到你的当前分支
$ git checkout master # 切换到本地的 master 分支
$ git rebase upstream/master # 同步 upstream/master 分支的最新变更到本地的 master 分支
```

注意：主分支可能被命名为 main 或 master，具体取决于代码仓库，kubernetes 项目的主分支名为 master

在创建一个新的修复分支或者新功能分支时，要确保基于 upstream 最新的 master 代码来创建，这样可以使你能够基于最新的代码来开发，并且尽最大可能降低未来分支代码合并到上游 master 分支时的代码冲突。

## 步骤 4：创建一个工作分支

基于本地 master 分支，创建本地的工作分支：

```shell
# -b 选项：表示创建新分支
# 创建一个名为 feature-myfeature的新分支，并立即切换到该分支。
$ git checkout -b feature-myfeature
```

之后，我们就可以在 feature-myfeature 分支，开发代码、编译代码、 测试代码。

这里要注意，在基于本地 master 分支创建工作分支之前，要确保执行 **步骤 3**，确保本地 master 分支代码跟上游 master 分支代码是一致的。

另外，这里要注意 Kubernetes 的分支命名规则为：

- feature-xxx：功能分支；
- fix-xxx：修复分支；
- release-xxx：发布分支。

## 步骤 5：在工作分支变更代码

之后，我们就可以基于工作分支来开发 Kubernetes 源码。在开发过程中，我们可以根据需求提交代码变更。这里建议的流程如下：

1. 修改代码；
2. 执行 git add 将工作目录的修改添加到暂存区；
3. 执行 git commit -m "some useful commit message"
4. 可以执行 git commit --amend 命令，它允许你修改最近一次的提交（补充内容、修改提交信息或两者兼具）：

```shell
# 追加新变更：将新修改的内容加入上一次提交（避免创建额外的小提交）
# 修改提交信息：修正拼写错误或优化描述
# 覆写提交：创建新的提交哈希值取代原提交（改变历史）

# 1. 创建原始提交
echo "第一个文件" > file1.txt
git add file1.txt
git commit -m "新增文件1"

# 2. 发现漏了变更：
echo "第二个文件" > file2.txt  # 忘记添加的新文件
echo "文件1补充内容" >> file1.txt  # 对已提交文件的修改

# 3. 添加新变更到暂存区：
git add file2.txt file1.txt

# 4. 执行追加操作（3种场景）：
# 场景1：仅追加变更（保持原提交信息不变）
git commit --amend --no-edit

# 场景2：追加变更并修改提交信息
git commit --amend -m "新增文件1和文件2"

# 场景3：交互模式（修改信息和内容）
git commit --amend  # 会打开编辑器（如Vim）修改信息

# 仍是1个提交（取代原提交），并生成全新的哈希值
```

关于 git commit --amend 的详细介绍可参考：[amend your previous commit](https://www.w3schools.com/git/git_amend.asp)。

**确保分支代码和上游 master 代码的一致性**

我们需要定期确保分支代码和上游的 master 代码的一致性。原因上面其实有介绍，就是确保我们基于最新的代码来开发，这样可以尽最大可能避免未来合并到上游 master 分支时的潜在冲突。

保持同步的方法很简单，就是首先切换到工作分支，然后执行下述命令：

```shell
$ git checkout feature-myfeature
$ git fetch upstream # 获取远程仓库（上游）的所有更新，但不会自动合并到当前分支
$ git rebase upstream/master
```

注意，不要使用 git pull 来代替 git fetch 和 git rebase，因为 git pull 会进行合并，并产生一个合并提交记录，这会使 kubernetes 仓库的提交历史变得混乱，并且这也违反了提交记录应该是整洁和清晰的原则。（ git pull 等同于 git fetch + git merge ）

```shell
#### 如果使用
git checkout feature-myfeature  
git pull upstream master        # 默认 = git fetch + git merge
# 会生成一个合并提交（Merge branch 'master' into feature-myfeature）

#### git pull upstream master 冲突问题
# 当远程分支（upstream/master）和你的本地分支修改了同一文件的同一部分代码时，Git 无法自动合并，会提示冲突：
Auto-merging README.md
CONFLICT (content): Merge conflict in README.md
Automatic merge failed; fix conflicts and then commit the result.
# 冲突解决流程
# 1. 手动编辑冲突文件（文件内会标记 <<<<<<<, =======, >>>>>>>）
vim README.md
# 2. 标记冲突已解决
git add README.md
# 3. 完成合并提交（Git 会自动生成一个合并提交记录）
git commit


#### git rebase upstream/master 冲突问题
# 同样在代码重叠时会发生冲突，但提示方式不同：
Applying: Add new feature
Using index info to reconstruct a base tree...
Falling back to patching base and 3-way merge...
Auto-merging README.md
CONFLICT (content): Merge conflict in README.md
error: Failed to merge in the changes.
Patch failed at 0001 Add new feature
Resolve all conflicts manually, mark them as resolved with "git add", then run "git rebase --continue".
# 冲突解决流程
# 1. 手动解决冲突（编辑文件）
vim README.md
# 2. 标记冲突已解决
git add README.md
# 3. 继续变基（而非提交！）
git rebase --continue
# 如果放弃变基：
git rebase --abort
```

你还可以使用以下 2 种方法来同步上游的 master 代码（2 种方法都是确保 git pull 时使用 rebase 而不是 merge）：

**方法 1：**

```shell
$ git config branch.autoSetupRebase always # 该命令实际上是通过修改 .git/config 配置来改变 git pull 行为的
$ git pull upstream master # 自动转换为 git pull --rebase upstream master
# 相当于：
# git fetch upstream
# git rebase upstream/master
```

**方法 2：**或者直接执行以下命令：

```shell
$ git pull --rebase upstream master
```

## 步骤 6：将变更推送到远端仓库

代码开发完之后，就可以将开发分支推送到远端仓库，命令如下：

```shell
# -f或--force 强制覆盖远程分支
# origin 目标远程仓库名称（通常是你 fork 的仓库）
$ git push -f origin feature-myfeature
```

## 步骤 7：创建一个 Pull Request

可以遵循以下步骤，来创建一个PR（Pull Request）：

1. 访问 Fork kubernetes GitHub 仓库；
2. 点击 **Pull requestss** -> **New pull request**：

![img](image/FqAvsY5j7NNecOeO1Qjd-AsnmLr1)

提交的时候要注意：

1. **base repository** 选择 kubernetes/kubernetes，**base** 选择 maser；
2. **head repository** 选择 \<user>/kubernetes，**compare** 选择 feature-myfeature；
3. 再次检查变更 Diff；
4. 点击 **Create pull request** 创建 PR。

### 进行代码审查

PR 提交后，会被分配给一个或多个代码审阅者（reviewers），这些审阅者会对代码进行彻底的审查，尝试从中找到错误的变更、可以改进的代码、不符合风格的代码、以及任何不符合要求的代码变更。

如果代码审查后，发现你的代码需要重新改进，你可以重新在 feature-myfeature 分支修改代码，并推送到 feature-myfeature 分支。

这里建议每个 PR 不要包含很多代码变更，这样的 PR 很难审查，会导致你的 PR 被合入 master 的进度变得很缓慢。

### 压缩提交

在代码 review 后，我们需要压缩我们的提交记录，以准备最终的 PR 合并。

我们应该确保最终的提交记录是有意义的（要么是一个里程碑，要么是一个单独、完整的提交），通过合理的提交记录，来使我们的变更变得更加清晰。

在合并 PR 之前，以下类型的提交应该被压缩：

- 修复/审查反馈；
- 拼写错误；
- 合并和变基；
- 进行中的工作。

我们应该确保 PR 中的每一个提交都能够独立编译，并通过测试（这不是必须的）。我们还应该确保合并提交记录被删除，因为这些提交无法通过测试。你可以阅读 [interactive rebase](https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History) 来了解更多压缩提交的知识。

压缩提交的具体操作流程如下：

> 参考视频：https://www.bilibili.com/video/BV1Ur4y1q7xB

例如有一个 PR，代码已经开发完成，并且有 24 个提交。你对此进行变基，简化更改为两个提交。这两个提交中的每一个代表一个单一的逻辑更改，并且每个提交消息都总结了更改内容。审阅者看到现在一组更改变得更加易懂，并批准了您的 PR。

## 其它后续操作

上面，我们已经按照 Kubernetes 源码的 GitHub 工作流，完成了 PR 的提交。之后 PR 便交给 Kubernetes 项目维护者进行代码 Review，Review 通过后，PR 会被合并到 Kubernetes master 分支。在这个过程中，还可能需要你合并提交你的代码，你也可能需要撤销你的提交。

### 1.  合并提交

当你收到 Review 和 Approval 的通知后，说明你的提交已经被压缩（squashed），PR 已经准备好被合并到 master 分支了。如果你没有压缩你的提交， kubernetes 项目维护者，可能会在批准 PR 之前要求你压缩提交。

### 2. 撤销提交

在开发过程中，你可能需要撤销提交。你可以执行以下命令来回滚一个提交。

1. 创建一个分支，并同步最新的上游代码

```shell
# 创建一个分组
$ git checkout -b revert-myrevert
# 同步最新的上游代码
$ git fetch upstream
$ git rebase upstream/master
```

2. 如果你想要撤销的提交是一个合并提交，请使用以下命令：

```shell
# SHA 是你希望恢复的合并提交的哈希值
$ git revert -m 1 <SHA>
```

如果这是一个单独的提交，请使用以下命令：

```shell
# SHA 是你希望恢复的单独提交的哈希值
$ git revert <SHA>
```

3. 这将创建一个新的提交来撤销这些更改。将这个新的提交推送到您的远程仓库:

```
$ git push <your_remote_name> revert-myrevert
```

4. 最后，基于 revert-myrevert 分支提交一个PR。