# mergeMine

# 一.运行环境

> - JDK 18.0.1

# 二.内容介绍

![img](https://ovxmsaoguz.feishu.cn/space/api/box/stream/download/asynccode/?code=NmUzOTgwNTU3NjllOTgyY2NkMGVlMTk0ZTgxMjU2MzRfb25BQ2hXSzFxRUs1UVZzSDJsc0tGT2RoUERYQks0UlNfVG9rZW46SVZQdmI4dFo2b1IxRVp4Z2hITGMxUHE5bkVlXzE3MzIwMjQxNzc6MTczMjAyNzc3N19WNA)

> ----文件----
>
> - DatasetCollector:
>   - 基于*MergeBERT数据集收集了所需要格式的80056*个冲突块（a、o、b、resolution四元组）、token级别的diff3、提取相关上下、BLEU-4与完美匹配结果分析等
> - Dataset50Collector:
>   - 基于*Top-50-repo数据集收集了所需要格式的*73779个冲突块
> - KeyInformationCollector:
>   - 抽取token级别diff3后的核心冲突词元内容
> - KeyContextCollector:
>   - 基于BM25相似算法抽取对应的文本相似上下文
> - utils：
>   - 常规工具类方法

# 1.MergeBERT数据集中resolution分析

## （1）**Line-level 中 resolution label 统计结果展示**

> - 目的：查看MergeBERT各resolution标签占比，为了合理选择出100个冲突示例（以备验证上下文代码信息有效性的数据） 结果：因此选择比例结果如表的最后一列，最大真实贴合原数据。
> - 各数据的饼图占比展示：file:///G:/now/2024merge/backup/ECharts/resolution-label-MergeBERT.html
> - 各数据的表格占比展示：如下

| Resolution Label | 个数统计  | 基于全部占比 | 基于有效label占比 | 准备选择示例个数 |
| :--------------- | :-------- | :----------- | :---------------- | :--------------- |
| A                | 11939     | 14.91%       | 18.37%            | 18               |
| B                | 12241     | 15.29%       | 18.83%            | 19               |
| BASE             | 384       | 0.48%        | 0.59%             | 1                |
| AB               | 5396      | 6.74%        | 8.30%             | 8                |
| BA               | 2261      | 2.82%        | 3.48%             | 4                |
| OTHER            | 20429     | 25.52%       | 31.43%            | 31               |
| REM-BASE-AB      | 4441      | 5.55%        | 6.83%             | 7                |
| REM-BASE-BA      | 4093      | 5.11%        | 6.30%             | 6                |
| REM-BASE-A       | 2132      | 2.66%        | 3.28%             | 3                |
| REM-BASE-B       | 1678      | 2.10%        | 2.58%             | 3                |
| RES_EMPTY        | 292       | 0.36%        | --                | --               |
| RES_FILE_EMPTY   | 456       | 0.57%        | --                | --               |
| null             | 14314     | 17.88%       | --                | --               |
|                  | **80056** |              | **64994**         | 100              |

# 2.Top-50-repo dataset with most conflicts among 2731 Java repos 

## （1）数据集分析

> 1.收集结果：
>
> ```
> {line_allCount=73779, merge_correct=18154, merge_succeed=24074, fit_merge=73779}
> ```
>
> 2.label统计结果：
>
> ```
> {CC21=482, A=29171, B=13509, null=12613, NC=10062, CC12=1289, CB=6653}
> ```
>
>  file:///G:/now/2024merge/backup/ECharts/resolution-label.html
>
> 3.有效（**61166**）resolution里面的perfect：
>
> ```
> {``all_succeeded``=22525, ``all_correct``=18154}
> ```
>
> 3.各项目内的perfect统计结果：[token merge accuracy of 50Repo](https://ovxmsaoguz.feishu.cn/sheets/Oy59sqYhChFpTatMVGRc1axcnBf) 
>
> 4.50Repo单个行粒度冲突有n个token粒度冲突（只关注20以内）：
>
> ```
> {``0=24074, 1=36953, 2=8330, 3=1345``, 4=1188, 5=488, 6=291, 7=162, 8=154, 9=68, 10=90, 11=73, 12=64, 13=45, 14=52, 15=32, 16=37, 17=11, 18=14, 19=9, 20=31}
> ```
>
> 对比MergeBERT:
>
> ```
> {``0=25661, 1=46566, 2=4934, 3=1380``, 4=605, 5=255, 6=154, 7=157, 8=72, 9=59, 10=32, 11=18, 12=11, 13=23, 14=9, 15=11, 16=7, 17=9, 18=3, 19=10, 20=5}
> ```

Token merge 效果数据：

| All conflicts       | 73779 | --                                 |
| ------------------- | ----- | ---------------------------------- |
| All valid conflicts | 61166 | --                                 |
| Merge succeed       | 22525 | --                                 |
| Merge correct       | 18154 | accuracy=29.68%   precision=80.59% |

100示例的占比选取：

| Label | 个数  | 占比   | 示例分配个数（100） |
| :---- | :---- | :----- | :------------------ |
| A     | 29171 | 47.69% | 47                  |
| B     | 13509 | 22.08% | 23                  |
| CB    | 6653  | 10.88% | 11                  |
| CC12  | 1289  | 2.11%  | 2                   |
| CC21  | 482   | 0.79%  | 1                   |
| NC    | 10062 | 16.45% | 16                  |
| Null  | 12613 | --     | --                  |
|       |       |        | 100                 |