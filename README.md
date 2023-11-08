# mergeMine

> github项目地址：https://github.com/HideInBlack/mergeMine
>
> 飞书README地址：[科研-研究](https://ovxmsaoguz.feishu.cn/docx/IosmdqIxzoAOKuxoDehcyALznBg) 

# 一.科研计划

1. 验证思路的正确性（通过真实例子体现）[1.验证上下文的有效性](https://ovxmsaoguz.feishu.cn/docx/HYMydWpYponBVxxMUJzcrhsonlf) 
2. 分析所提出方法的可行性（关键信息的补充）
3. 所提方法的关键挑战
   1. ①什么信息才是关键的上下文信息？ 
   2. ②如何才可以搜寻到关键上下文信息？ 
   3. ③搜寻到关键信息后，如何嵌入到合并冲突信息的输入中？ 
   4. ④如何证明其的有效性？
4. 数据集的预处理
5. 大型预训练代码模型的选择
6. 微调...
7. 小模型的效果评估
8. 大模型的训练成本调研

## 1.验证思路的正确性

（1）初步调研

> - 实现 token-level Diff3 算法，收集token粒度冲突（单文件）
> - 实现 token-level Diff3 算法，批量收集token粒度冲突（基于文件夹dir批量收集）
> - 基于已收集的token粒度冲突，做BM2.5算法的关键信息定位与验证
> - 基于已使用BM2.5算法的关键信息，验证关键信息辅助解决冲突的效果
> - 调研WALA切片、考虑使用数据流图来限制搜索范围。
> - 集成使用WALA、BM2.5(或其他搜索算法)来提高搜索关键信息准确度。
> - 基于已收集的token粒度冲突，做CodeBERT Vector的关键信息定位与验证
> - 基于已使用CodeBERT Vector的关键信息，验证关键信息辅助解决冲突的效果

（2）其他

> - Java 语义的上下文信息
> - Java切片 数据流图来限制搜索范围 （WALA、Soot）
> - 消解冲突块的策略的顺序 （文件依赖关系 文件内冲突依赖关系）以项目为单位

## 注意

> 1.并不是所有的冲突块都适合做token-level的解决 有合适的有不合适的。
>
> - 合适是：A、O、B三个版本中行数相近
> - 不合适的：三版本行数相差太多