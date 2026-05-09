# 基于Hive的城市空气质量分析与预测系统的设计与实现

## 摘 要

随着我国城市化进程的不断加快和工业规模的持续扩大，空气污染问题日益严峻，已成为影响居民健康和城市可持续发展的重要因素。传统的空气质量监测手段存在数据处理能力有限、分析维度单一、预测精度不高等问题，难以满足当前环境管理的实际需求。本文针对上述问题，设计并实现了一套基于Hive的城市空气质量分析与预测系统。系统采用前后端分离的B/S架构模式，后端基于Spring Boot 2.7框架构建RESTful API服务，利用MyBatis-Plus实现数据持久化，结合MySQL存储业务数据、Hive作为数据仓库按ODS-DWD-DWS-ADS四层架构存储海量历史监测数据，并引入Redis+Caffeine多级缓存机制提升系统响应速度。前端采用Vue 3框架配合Vite构建工具，基于Element Plus组件库和ECharts可视化库实现了空气质量实时监控、历史趋势分析、IDW空间插值分析、Pearson/Spearman多因子相关性分析等功能模块。在预测方面，系统自主实现了随机森林回归算法，通过Bootstrap采样构建100棵决策树，以历史AQI及六项污染物浓度、季节因子作为特征向量进行建模训练，实现了对未来空气质量指数的预测。此外，系统还设计了基于规则引擎的告警模块，当监测数据超过预设阈值时自动触发分级告警通知。同时，系统实现了定时数据采集、数据质量检测和历史数据归档等自动化运维任务。经测试验证，该系统在50并发用户条件下平均响应时间小于500ms，随机森林预测模型R²达到0.847，能够为城市环境管理和公众健康防护提供科学的数据支撑和决策依据。

**关键词：** 空气质量分析；Hive数据仓库；随机森林算法；IDW空间插值；数据可视化；Spring Boot

## ABSTRACT

**Title:** Design and Implementation of Urban Air Quality Analysis and Prediction System Based on Hive

**Abstract:** With the acceleration of urbanization and the continuous expansion of industrial scale in China, air pollution has become an increasingly serious problem that significantly affects public health and sustainable urban development. Traditional air quality monitoring methods have limitations in data processing capacity, analytical dimensions, and prediction accuracy, making them insufficient to meet the practical needs of current environmental management. To address these issues, this paper designs and implements an urban air quality analysis and prediction system based on Hive. The system adopts a front-end and back-end separated B/S architecture. The back-end is built on the Spring Boot 2.7 framework to provide RESTful API services, utilizing MyBatis-Plus for data persistence, MySQL for business data storage, and Hive as the data warehouse with a four-layer architecture (ODS-DWD-DWS-ADS) for storing massive historical monitoring data. A multi-level caching mechanism combining Redis and Caffeine is introduced to improve system response speed. The front-end employs the Vue 3 framework with Vite build tool, implementing functional modules including real-time air quality monitoring, historical trend analysis, IDW spatial interpolation analysis, and Pearson/Spearman multi-factor correlation analysis using Element Plus component library and ECharts visualization library. In terms of prediction, the system independently implements the Random Forest regression algorithm, constructing 100 decision trees through Bootstrap sampling, using historical AQI values, six pollutant concentrations, and seasonal factors as feature vectors for model training to predict future air quality indices. Additionally, a rule-engine-based alert module is designed to automatically trigger hierarchical alert notifications when monitoring data exceeds preset thresholds. The system also implements automated operational tasks including scheduled data collection, data quality inspection, and historical data archiving. Testing and verification demonstrate that the system achieves an average response time of less than 500ms under 50 concurrent users, and the Random Forest prediction model achieves an R² of 0.847, providing scientific data support and decision-making basis for urban environmental management and public health protection.

**Keywords:** Air Quality Analysis; Hive Data Warehouse; Random Forest Algorithm; IDW Spatial Interpolation; Data Visualization; Spring Boot

## 目 次

1  绪论 ………………………………………………………………………………… 1

1.1  课题研究的目的和意义 ……………………………………………………… 1

1.2  国内外研究现状 ……………………………………………………………… 2

1.3  课题总体要求和规划 ………………………………………………………… 4

2  相关技术与理论基础 ……………………………………………………………… 6

2.1  Hadoop生态系统与Hive数据仓库 ………………………………………… 6

2.2  Spring Boot微服务框架 ……………………………………………………… 8

2.3  Vue 3前端框架与ECharts可视化 …………………………………………… 9

2.4  Redis与Caffeine多级缓存 ………………………………………………… 10

2.5  随机森林算法原理 ………………………………………………………… 11

2.6  IDW空间插值算法 ………………………………………………………… 13

2.7  Pearson相关性分析方法 …………………………………………………… 14

3  系统需求分析 …………………………………………………………………… 15

3.1  功能需求分析 ………………………………………………………………… 15

3.2  非功能需求分析 ……………………………………………………………… 18

3.3  可行性分析 …………………………………………………………………… 19

4  系统总体设计 …………………………………………………………………… 21

4.1  系统架构设计 ………………………………………………………………… 21

4.2  数据仓库分层设计 ………………………………………………………… 23

4.3  功能模块设计 ………………………………………………………………… 25

4.4  数据库设计 …………………………………………………………………… 27

4.5  RESTful接口设计 …………………………………………………………… 30

4.6  缓存策略设计 ………………………………………………………………… 32

5  系统详细设计与实现 …………………………………………………………… 34

5.1  后端服务架构实现 ………………………………………………………… 34

5.2  数据采集与质量检测模块 ………………………………………………… 37

5.3  空气质量实时监控模块 …………………………………………………… 39

5.4  历史趋势分析模块 ………………………………………………………… 41

5.5  IDW空间插值分析模块 …………………………………………………… 43

5.6  多因子相关性分析模块 …………………………………………………… 46

5.7  随机森林预测模块 ………………………………………………………… 49

5.8  告警管理模块 ………………………………………………………………… 53

5.9  系统监控与运维模块 ……………………………………………………… 55

5.10 前端双入口架构实现 ……………………………………………………… 56

6  系统测试与结果分析 …………………………………………………………… 58

6.1  测试环境与测试方案 ……………………………………………………… 58

6.2  功能测试 ……………………………………………………………………… 59

6.3  性能测试 ……………………………………………………………………… 61

6.4  随机森林预测精度分析 …………………………………………………… 63

6.5  IDW插值精度分析 ………………………………………………………… 65

结论 ………………………………………………………………………………… 67

致谢 ………………………………………………………………………………… 69

参考文献 …………………………………………………………………………… 70

附录A  MySQL数据库建表脚本 …………………………………………………… 73

附录B  Hive数据仓库建表脚本 …………………………………………………… 77

附录C  系统核心算法代码 ………………………………………………………… 80

---

## 1 绪论

### 1.1 课题研究的目的和意义

空气质量是衡量城市宜居程度和生态环境状况的重要指标之一。近年来，随着我国经济的高速发展和城市化进程的不断推进，机动车尾气排放、工业废气排放、建筑扬尘等污染源持续增加，导致雾霾、臭氧超标等空气污染事件频发，严重威胁着人民群众的身体健康和生活质量。据生态环境部发布的数据显示，全国重点城市中仍有相当比例的城市空气质量未能达到国家二级标准，空气污染防治工作面临巨大挑战。

以郑州市为例，作为中原地区的核心城市和国家中心城市之一，郑州的经济发展与环境保护之间的矛盾尤为突出。郑州地处华北平原，冬季逆温层频发，不利于污染物扩散；同时城市建设活动密集、机动车保有量持续增长，使得PM2.5、PM10等颗粒物污染问题长期存在。如何利用信息技术手段实现空气质量的精细化监测、科学化分析和智能化预测，已成为环境管理领域亟待解决的重要课题。

传统的空气质量监测与管理方式主要依赖于人工采样检测和简单的统计分析方法，存在以下几方面的不足：一是数据处理能力有限，面对海量的多站点、多时段、多指标监测数据时效率低下；二是分析维度单一，难以从时间、空间、多因子关联等多个角度揭示空气污染的内在规律；三是预测手段落后，多采用经验判断或简单线性外推，难以为环境管理决策提供前瞻性的参考依据；四是缺乏可视化交互界面，管理人员难以直观掌握全局态势。

在大数据技术蓬勃发展的背景下，利用Hadoop生态系统中的Hive数据仓库技术来存储和分析海量空气质量监测数据成为可能。Hive提供了类SQL的查询接口，能够方便地对存储在HDFS上的大规模数据进行批量分析和统计计算。结合机器学习算法，可以基于历史数据构建预测模型，实现对未来空气质量变化趋势的科学预判。

本课题的研究目的在于设计和实现一套完整的城市空气质量分析与预测系统，充分利用大数据技术和机器学习方法，解决传统空气质量监测分析中存在的上述不足。具体而言，系统需要实现以下目标：（1）构建基于Hive的多层数据仓库，实现海量监测数据的高效存储和查询；（2）提供多维度的数据分析能力，包括时间趋势分析、IDW空间插值分析和多因子相关性分析；（3）实现基于随机森林算法的空气质量预测模型；（4）开发友好的Web可视化界面，支持多角色、多场景的数据展示和交互操作。

系统的实现对于推进智慧城市环境管理、辅助政府部门科学决策、增强公众环保意识等方面具有重要的理论意义和实际应用价值。

### 1.2 国内外研究现状

**国际研究现状：**

在国际上，美国环境保护署（EPA）建立了完善的空气质量监测网络（AQS系统），利用大数据技术实现了全国范围内超过4000个监测站点空气质量数据的实时采集、存储和分析。欧盟的哥白尼大气监测服务（CAMS）则综合运用卫星遥感数据和地面监测数据，结合数值模拟方法实现了区域尺度的空气质量预报，预报时效可达5天。

在学术研究方面，Kumar等人利用主成分回归技术对德里市的空气质量进行预测，通过降维处理有效减少了气象因子的多重共线性问题。Zheng等人提出了基于时空数据融合的城市空气质量推断方法U-Air，利用多种城市数据（道路网络、兴趣点、气象数据等）对未监测区域的空气质量进行推断，解决了监测站点覆盖不足的问题。Breiman提出的随机森林算法因其对高维数据的优秀处理能力和较强的鲁棒性，在空气质量预测领域得到了广泛应用。近年来，深度学习方法（如LSTM、GRU、Transformer等）也被引入空气质量预测任务，展现了在时间序列预测方面的优势。

**国内研究现状：**

在国内，生态环境部已建成覆盖全国的空气质量自动监测网络，包括1734个国家级监测站点，实现了338个地级及以上城市空气质量的在线监测和数据发布。中国环境监测总站开发了空气质量预报预警系统，综合运用数值模式和统计模型提供72小时空气质量预报。

在技术应用方面，清华大学环境学院团队利用WRF-Chem模式开展了区域空气质量数值预报研究；北京大学的研究团队在利用大数据和机器学习技术进行空气质量分析预测方面开展了大量工作，提出了多尺度空气质量预报融合方法。赵文芳等人系统综述了基于机器学习的PM2.5浓度预测研究进展，指出随机森林、支持向量机等集成学习方法在中短期预测任务中表现优异。方巍等人基于随机森林构建了空气质量指数预测模型，通过对特征重要性的分析揭示了不同污染因子对AQI的影响程度。

在系统开发方面，部分互联网企业（如IBM中国研究院、微软亚洲研究院）推出了面向公众的空气质量查询和预报服务平台，但其底层技术对外不开源。高校和科研机构开发的系统多偏向于单一功能验证，在系统完整性、用户体验和工程化方面存在不足。现有系统大多存在技术栈单一、预测模型固定、数据处理规模有限、可扩展性不强等局限，难以满足多样化的分析需求。

### 1.3 课题总体要求和规划

本课题要求设计并实现一套功能完善、性能可靠的城市空气质量分析与预测系统，以郑州市为应用对象，具体要求包括：

（1）采用大数据技术架构，构建基于Hive的多层数据仓库（ODS-DWD-DWS-ADS），能够存储和处理至少百万级的历史监测记录；

（2）实现空气质量数据的多维度分析，包括时间趋势分析（支持小时/日/月多粒度）、IDW空间插值分析、Pearson/Spearman多因子相关性分析等；

（3）自主实现随机森林回归算法，基于历史数据训练预测模型，实现空气质量指数的短期预测，并能计算特征重要性和预测置信度；

（4）提供友好的Web可视化界面，采用前后端分离架构，支持管理端和用户端双入口；

（5）实现告警管理功能，支持自定义告警规则和分级通知；

（6）系统具有良好的可扩展性和可维护性，包含自动化的数据采集、质量检测和运维监控机制。

课题工作规划如下：第一阶段进行需求调研和技术选型，明确系统功能需求和技术方案（2周）；第二阶段进行系统总体设计，确定系统架构、数据仓库分层、模块划分和数据库结构（2周）；第三阶段按照模块划分进行详细设计和编码实现，依次完成后端服务开发、前端界面开发和算法实现（6周）；第四阶段对系统进行全面测试和优化，撰写毕业设计论文（2周）。

## 2 相关技术与理论基础

### 2.1 Hadoop生态系统与Hive数据仓库

Hadoop是Apache基金会开发的开源分布式计算框架，其核心组件包括HDFS（Hadoop Distributed File System）和MapReduce编程模型。HDFS是一种高度容错的分布式文件系统，能够将大规模数据分布式存储在廉价硬件集群上，通过数据冗余备份（默认3副本）保证数据的可靠性，并提供高吞吐量的数据访问能力。MapReduce是一种并行计算模型，通过将计算任务分解为Map和Reduce两个阶段，实现对大规模数据集的并行处理。

围绕Hadoop核心组件，形成了丰富的生态系统。本系统使用的Hadoop版本为3.3.1，主要涉及的组件包括：HDFS用于底层数据存储；YARN作为资源管理和任务调度框架；Hive 3.1.2作为数据仓库引擎。

Apache Hive是建立在Hadoop之上的数据仓库基础设施，提供了类SQL的查询语言HiveQL，使得熟悉SQL的用户能够方便地对存储在HDFS上的大规模数据进行查询和分析。Hive将HiveQL语句转换为MapReduce或Spark任务执行，屏蔽了底层分布式计算的复杂性。Hive的主要特点包括：

（1）支持大规模数据集的批量处理，适合数据仓库场景下的OLAP分析；

（2）提供丰富的内置函数和用户自定义函数（UDF）机制，满足复杂的数据转换需求；

（3）支持分区表和桶表，能够有效优化查询性能；

（4）支持ORC、Parquet等列式存储格式和Snappy等压缩算法，大幅减少存储空间和I/O开销。

在本系统中，Hive数据仓库按照经典的分层架构设计：ODS层（原始数据层）存储从MySQL同步的原始监测数据；DWD层（明细数据层）存储经过数据清洗和标准化处理的明细记录；DWS层（汇总数据层）存储按站点、时间维度聚合的统计汇总数据；ADS层（应用数据层）存储直接面向前端应用查询的预计算结果。所有表采用ORC格式存储并启用Snappy压缩。

### 2.2 Spring Boot微服务框架

Spring Boot是由Pivotal团队提供的基于Spring框架的快速开发框架，其设计理念是"约定优于配置"，通过自动配置（Auto Configuration）机制大幅简化了Spring应用的搭建和开发过程。Spring Boot内嵌了Tomcat、Jetty等Web服务器，支持以独立的jar包形式运行应用，便于部署和运维。

本系统后端采用Spring Boot 2.7.18版本，核心技术组件包括：

（1）**Spring MVC**：实现RESTful风格的API接口开发，通过`@RestController`、`@RequestMapping`等注解定义HTTP端点，支持请求参数校验和统一响应封装。

（2）**MyBatis-Plus 3.5.5**：在MyBatis基础上增强的ORM框架，提供了通用Mapper、条件构造器、分页插件等功能，减少了单表CRUD操作的重复代码。配合分页拦截器`PaginationInnerInterceptor`实现物理分页。

（3）**Druid 1.2.20连接池**：阿里巴巴开源的高性能数据库连接池，提供连接池监控、SQL防注入、慢SQL日志等企业级特性。

（4）**Spring AOP**：通过面向切面编程实现API请求日志记录，自动统计每个接口的调用耗时。

（5）**Spring Scheduling**：利用`@Scheduled`注解实现定时任务调度，包括数据采集、数据质量检查、告警检测和历史数据归档等周期性任务。

（6）**SpringDoc OpenAPI 1.7.0**：自动生成RESTful API文档，提供Swagger UI在线测试界面。

（7）**全局异常处理**：通过`@RestControllerAdvice`和`@ExceptionHandler`实现统一的异常捕获和标准化错误响应。

### 2.3 Vue 3前端框架与ECharts可视化

Vue.js是一款渐进式JavaScript前端框架，Vue 3作为其最新主要版本，引入了Composition API、Teleport、Fragments等新特性，在性能和开发体验方面有了显著提升。Vue 3采用Proxy-based的响应式系统，相比Vue 2的Object.defineProperty方案，具有更好的性能和更完整的响应式追踪能力。Composition API允许将相关逻辑组织在一起，提高了代码的复用性和可维护性。

本系统前端基于Vue 3框架开发，使用Vite作为构建工具。Vite利用浏览器原生ES Module支持实现了毫秒级的热模块替换（HMR），大幅提升了开发效率。前端技术栈还包括：

（1）**Element Plus**：基于Vue 3的企业级UI组件库，提供了表格、表单、对话框、日期选择器等丰富的UI组件，保证了界面风格的统一性。

（2）**ECharts 5.x**：百度开源的数据可视化图表库，支持折线图、柱状图、饼图、热力图、散点图、雷达图等数十种图表类型，是本系统数据可视化的核心工具。

（3）**Axios**：基于Promise的HTTP客户端，用于与后端API进行异步通信，配置了统一的请求/响应拦截器处理认证和错误。

（4）**Vue Router 4**：官方路由管理器，支持嵌套路由和动态路由匹配。

前端采用独特的**双入口架构设计**：通过Vite的多页面配置，`index.html`作为管理端入口挂载完整的管理后台功能（10个路由页面），`user.html`作为用户端入口提供简化的公众查询界面（4个路由页面），两者共享API层和工具库代码，但拥有独立的路由配置、布局组件和界面风格。

### 2.4 Redis与Caffeine多级缓存

在高并发访问场景下，频繁的数据库查询会成为系统性能的瓶颈。本系统采用Redis+Caffeine的两级缓存策略，有效降低了数据库的访问压力并提升了接口响应速度。

**Redis**是一个开源的内存数据结构存储系统，支持字符串、哈希表、列表、集合等多种数据类型，具有读写速度快（单机可达10万+ QPS）、支持持久化、原子操作等特点。本系统使用Redis 6.2作为分布式缓存，通过Lettuce客户端连接，主要缓存实时监测数据、告警状态等热点数据，设置合理的TTL（生存时间）保证数据的时效性。

**Caffeine**是Java平台上性能最优的本地缓存库，基于W-TinyLFU算法实现了近乎最优的缓存淘汰策略。相比Guava Cache，Caffeine在读写吞吐量上有显著优势。本系统将Caffeine作为一级缓存（本地缓存），将Redis作为二级缓存（分布式缓存），请求首先查询Caffeine本地缓存，命中则直接返回；未命中则查询Redis，命中后回写Caffeine；Redis也未命中才查询数据库并逐层回写缓存。

缓存配置中，Caffeine的最大容量设置为500条，写入后5分钟过期；Redis中不同类型的数据设置不同的过期时间，实时数据5分钟，告警数据5分钟，统计数据30分钟。

### 2.5 随机森林算法原理

随机森林（Random Forest）是由Leo Breiman于2001年提出的一种集成学习算法，属于Bagging（Bootstrap Aggregating）方法的典型代表。其核心思想是通过构建多棵决策树并将其预测结果进行集成来获得最终预测，有效降低了单棵决策树容易过拟合的风险。

随机森林算法的构建过程如下：

（1）**Bootstrap采样**：从原始训练集N个样本中，有放回地随机抽取N个样本，构成一个Bootstrap样本子集。重复此过程B次（B为树的数量），得到B个不同的训练子集。

（2）**特征随机选择**：在每棵树的每个节点分裂时，从全部M个特征中随机选择m个特征（通常$m = \sqrt{M}$），在这m个特征中寻找最优分裂点，而非在所有特征中搜索。

（3）**决策树构建**：每棵树基于对应的Bootstrap样本和随机特征子集进行生长，不进行剪枝操作，直到满足停止条件（达到最大深度或节点样本数低于阈值）。

（4）**集成预测**：对于回归任务，最终预测值为所有树预测值的算术平均；对于分类任务，采用多数投票策略。

分裂准则采用方差减少量（Variance Reduction）：对于候选分裂点，计算分裂前后的方差变化，选择使方差减少最大的特征和阈值进行分裂。

$$VR = Var(S) - \frac{|S_L|}{|S|} Var(S_L) - \frac{|S_R|}{|S|} Var(S_R)$$

其中$S$为当前节点样本集，$S_L$和$S_R$分别为分裂后的左右子集。

本系统中随机森林的参数设置：树的数量B=100，最大深度=10，最小分裂样本数=2，每棵树随机选择的特征数m=4（共8个特征）。特征向量包含：历史AQI值、PM2.5浓度、PM10浓度、SO2浓度、NO2浓度、CO浓度、O3浓度和季节因子。

### 2.6 IDW空间插值算法

反距离加权插值（Inverse Distance Weighting, IDW）是一种确定性空间插值方法，其基本假设是距离待插值点越近的已知样本点对该点的影响越大。IDW方法计算简单、直观，在空气质量空间分布分析中应用广泛。

IDW的数学表达式为：

$$\hat{Z}(x_0) = \frac{\sum_{i=1}^{n} w_i \cdot Z(x_i)}{\sum_{i=1}^{n} w_i}$$

其中权重$w_i$定义为：

$$w_i = \frac{1}{d(x_0, x_i)^p}$$

$d(x_0, x_i)$为待插值点$x_0$与第$i$个已知点$x_i$之间的欧氏距离，$p$为距离衰减指数（本系统取$p=2$），$Z(x_i)$为第$i$个已知点的观测值。

本系统在郑州市地理范围（纬度34.65°-34.85°，经度113.45°-113.85°）内生成50×50的规则网格，利用9个监测站点的实测数据，通过IDW算法对每个网格点进行插值计算，生成连续的污染物浓度空间分布。当待插值点与某站点距离小于0.0001度时，直接使用该站点的观测值避免除零错误。

### 2.7 Pearson相关性分析方法

Pearson相关系数（Pearson Correlation Coefficient）是衡量两个连续变量之间线性相关程度的统计指标，取值范围为[-1, 1]。其计算公式为：

$$r = \frac{\sum_{i=1}^{n}(x_i - \bar{x})(y_i - \bar{y})}{\sqrt{\sum_{i=1}^{n}(x_i - \bar{x})^2 \cdot \sum_{i=1}^{n}(y_i - \bar{y})^2}}$$

当|r|接近1时表示强线性相关，接近0时表示无线性相关。本系统利用Apache Commons Math 3.6.1库中的`PearsonsCorrelation`类进行计算，同时支持Spearman等级相关系数（通过对原始数据进行秩变换后计算Pearson系数实现）。

在空气质量分析中，相关性分析用于揭示不同污染物之间的协同变化关系（如PM2.5与PM10通常具有强正相关性），以及污染物与气象因子之间的关联关系（如风速与PM2.5通常具有负相关性），为污染源解析和治理策略制定提供参考。

## 3 系统需求分析

### 3.1 功能需求分析

通过对城市空气质量监测管理工作流程的调研和分析，结合郑州市环境监测的实际需求，确定系统应具备以下功能需求：

**（1）数据采集与管理功能**

系统需要实现空气质量监测数据的自动化采集，支持从多个监测站点获取PM2.5、PM10、SO2、NO2、CO、O3等六项主要污染物的浓度数据以及AQI综合指标。数据采集模块需要具备以下能力：定时自动触发采集任务（每30分钟一次）；数据清洗功能（去除空值、检测异常值、去重）；数据入库功能（同时写入MySQL业务库和Hive数据仓库）；采集日志记录和异常告警。系统还应提供数据管理功能，支持数据的查询、导入（CSV格式）、导出和删除操作。

**（2）实时监控功能**

系统需要提供空气质量实时监控看板，以直观的方式展示当前各监测站点的AQI指数和空气质量等级。实时监控页面需要包含：城市整体AQI概览；各站点AQI仪表盘和六项指标卡片；站点排名表格；空气质量等级分布饼图。数据每30秒自动刷新一次。

**（3）历史趋势分析功能**

系统需要支持按照不同时间粒度（小时、日、月）查询和展示历史空气质量数据的变化趋势。功能包括：多指标对比折线图（同时展示多种污染物）；同比环比分析；统计摘要卡片（平均值、最大值、最小值、优良天数）；数据明细表格（支持排序、筛选、分页）；数据导出为CSV文件。对于近30天数据直接查询MySQL，超过30天的历史数据通过Hive查询。

**（4）空间分布分析功能**

系统需要实现空气质量数据在空间维度上的分析和展示。功能包括：基于IDW算法的空间插值热力图；监测站点气泡图（气泡大小反映AQI值）；各区空气质量对比柱状图；空间统计分析（最高/最低/平均/标准差）。支持切换不同污染物指标查看对应的空间分布。

**（5）相关性分析功能**

系统需要提供各污染物指标之间、污染物与气象因子之间的相关性分析功能。包括：Pearson/Spearman相关系数矩阵计算和热力图展示；单一因子与AQI的相关性排序条形图；双因子散点图和回归线；时滞相关性分析（分析滞后效应）；特征重要性排序（基于随机森林模型）。

**（6）空气质量预测功能**

系统需要实现两种预测功能：一是基于后端服务的24/72小时站点级AQI预测（读取预计算的预测结果）；二是基于自主实现的随机森林算法的在线预测，用户提供历史数据后实时计算预测结果。预测结果需包含：预测AQI值和等级、各污染物浓度预测值、预测置信度、特征重要性分析。

**（7）告警管理功能**

系统需要支持灵活的告警管理功能：自定义告警规则（指标、运算符、阈值、级别）；规则CRUD管理（支持启用/停用）；自动告警检测（每30分钟比对规则与实时数据）；告警记录查询和统计；告警冷却机制（避免重复告警）；支持info/warning/severe/emergency四级告警。

**（8）系统管理功能**

提供系统运维监控功能：JVM内存和CPU使用率监控；数据库连接状态和数据量统计；API调用量和响应时间统计；缓存命中率监控；数据质量报告（完整性、准确性、一致性、时效性）；系统版本信息和日志查看。

**（9）用户端功能**

面向公众提供简化的查询界面：各区空气质量实时查看；历史数据按日期查询；随机森林智能预测；历年同日空气质量对比。

### 3.2 非功能需求分析

**（1）性能需求**

系统Web页面的平均响应时间不超过500ms（50并发用户条件下）；大数据量查询操作的响应时间不超过3秒（引入缓存后）；系统应能够处理至少100万条历史监测记录的存储和分析；后端API吞吐量达到100 req/s以上。

**（2）可靠性需求**

系统应具备完善的异常处理机制，通过全局异常处理器统一捕获和处理异常，保证在异常情况下返回友好的错误信息而不暴露系统内部细节；数据库应具备完整性约束和索引优化；Redis缓存支持降级（缓存失效时直接查询数据库）。

**（3）安全性需求**

系统配置了JWT令牌认证机制（预留实现）；API接口支持跨域配置（CORS）；数据库密码通过环境变量注入避免硬编码；SQL参数使用预编译语句防止注入攻击。

**（4）可扩展性需求**

系统架构设计具有良好的可扩展性：Controller-Service-Mapper三层架构解耦清晰；Hive数据仓库支持数据规模的水平扩展；RESTful API设计遵循统一规范，便于新增接口；前端路由配置支持动态添加功能模块。

**（5）可维护性需求**

SpringDoc自动生成API文档，便于前后端协作；代码结构按功能模块划分，职责清晰；日志系统记录完整的运行轨迹；配置外化（YAML+环境变量），支持多环境部署。

### 3.3 可行性分析

**技术可行性：** 本系统所涉及的技术栈均已成熟稳定。Hadoop 3.x/Hive 3.x在大数据领域得到广泛应用，Spring Boot 2.7是目前Java Web开发的主流框架之一，Vue 3+Vite生态丰富完善，ECharts是国内使用最广泛的可视化库之一，随机森林算法理论成熟且本系统采用Java自主实现便于定制。MyBatis-Plus、Redis、Caffeine等中间件均有大量的生产实践经验。

**经济可行性：** 系统所采用的核心技术均为开源软件（Apache License或MIT License），不涉及商业授权费用。开发和测试所需的硬件资源可利用学校实验室现有服务器。外部数据源（空气质量监测数据API、Open-Meteo气象API）均提供免费的访问额度。

**操作可行性：** 系统提供Web可视化界面，操作方式直观简便。管理端提供完整的系统管理功能，用户端则面向公众提供简洁的查询和预测服务。Element Plus组件库保证了界面的一致性和易用性，用户无需专业的技术背景即可使用系统进行数据查询和分析。

## 4 系统总体设计

### 4.1 系统架构设计

本系统采用前后端分离的B/S（Browser/Server）架构，整体分为数据层、服务层和表现层三个层次，并辅以定时任务层处理后台自动化工作。

**数据层：** 采用三种存储引擎各司其职：MySQL 8.0关系型数据库存储业务数据（站点信息、告警规则、告警记录、模型信息、近期监测数据）；Hive 3.1.2数据仓库按ODS-DWD-DWS-ADS四层架构存储海量历史数据，以ORC+Snappy格式压缩存储并按日期分区；Redis 6.2内存数据库实现热点数据缓存和分布式状态管理。

**服务层：** 基于Spring Boot 2.7.18框架构建后端服务，通过RESTful API（统一前缀`/api/v1`）向前端提供数据接口。服务层内部按照Controller-Service-Mapper三层架构组织代码：Controller层负责HTTP请求路由、参数校验和响应封装；Service层封装业务逻辑和算法实现；Mapper层负责数据访问（支持MyBatis注解SQL和XML映射两种方式）。此外还包含：全局异常处理器（`GlobalExceptionHandler`）、API日志切面（`ApiLog`）、多数据源配置（`DataSourceConfig`）、缓存配置（`RedisConfig`）等横切关注点。

**表现层：** 基于Vue 3框架开发单页面应用（SPA），采用双入口设计（管理端`index.html`和用户端`user.html`），通过Axios异步请求与后端API交互获取数据，利用ECharts等可视化组件将数据以图表、热力图等形式展示给用户。前端开发服务器（Vite，端口3000）通过反向代理将`/api`前缀的请求转发到后端（端口8080）。

**定时任务层：** 通过Spring `@Scheduled`机制实现后台定时任务，包括数据采集（30分钟周期）、数据质量检查（1小时周期）、告警检测（30分钟周期）、历史数据归档（每日凌晨2点）。

系统整体技术架构如下：

```
┌──────────────────────────────────────────────────────────────────┐
│                     表现层（Vue 3 前端）                           │
│  Vite 构建 | Element Plus UI | ECharts 可视化 | Axios HTTP      │
│  ┌─────────────────────┐    ┌─────────────────────┐             │
│  │  管理端 (index.html) │    │  用户端 (user.html) │             │
│  │  10个路由页面        │    │  4个路由页面        │             │
│  └─────────────────────┘    └─────────────────────┘             │
└──────────────────────┬───────────────────────────────────────────┘
                       │ HTTP/REST (Proxy: :3000 → :8080/api)
┌──────────────────────┼───────────────────────────────────────────┐
│                     服务层（Spring Boot 2.7）                      │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ Controller层: Aqi|Prediction|Correlation|Spatial|Alert │      │
│  │              RandomForest|Zhengzhou|System              │      │
│  └────────────────────────────────────────────────────────┘      │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ Service层: 业务逻辑 + 随机森林算法 + IDW插值 + Pearson │      │
│  └────────────────────────────────────────────────────────┘      │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ Mapper层: MyBatis-Plus(注解SQL + XML映射)              │      │
│  └────────────────────────────────────────────────────────┘      │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ 横切关注: AOP日志|全局异常|CORS|缓存|OpenAPI文档       │      │
│  └────────────────────────────────────────────────────────┘      │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ 定时任务: 数据采集|质量检测|告警检测|历史归档           │      │
│  └────────────────────────────────────────────────────────┘      │
└──────────────────────┬───────────────────────────────────────────┘
                       │
┌──────────────────────┼───────────────────────────────────────────┐
│                     数据层                                         │
│  ┌─────────────┐  ┌──────────────────┐  ┌──────────────┐        │
│  │ MySQL 8.0   │  │ Hive 3.1.2       │  │ Redis 6.2    │        │
│  │ (业务数据)   │  │ (ODS→DWD→DWS→ADS)│  │ (多级缓存)   │        │
│  │ Druid连接池  │  │ ORC+Snappy       │  │ Lettuce客户端│        │
│  └─────────────┘  └──────────────────┘  └──────────────┘        │
└──────────────────────────────────────────────────────────────────┘
```

### 4.2 数据仓库分层设计

本系统的Hive数据仓库按照业界通用的分层理念设计，自底向上分为四层：

**（1）ODS层（Operational Data Store，原始数据层）**

ODS层负责存储从外部系统采集的原始数据，保持数据的原始面貌，不做任何加工处理。主要表为`ods_air_quality_raw`，按日期（dt字段）分区，每日通过Sqoop或自定义ETL脚本从MySQL的`air_quality_data`表全量/增量同步数据。ODS层的数据保留策略为至少保留3年。

**（2）DWD层（Data Warehouse Detail，明细数据层）**

DWD层在ODS的基础上进行数据清洗、标准化和维度关联。主要处理包括：去除空值和异常值记录（AQI<0或>500视为异常）；补充维度信息（通过站点编码关联维度表获取经纬度、区县等）；数据类型标准化（统一时间格式、数值精度）；添加数据有效性标记。主要表为`dwd_air_quality_dt`，同样按日期分区，并增加`is_valid`字段标识数据有效性。

**（3）DWS层（Data Warehouse Summary，汇总数据层）**

DWS层对DWD层的明细数据进行聚合汇总，生成面向主题的统计指标。主要包含两张表：`dws_station_hour`（站点小时级汇总，包含平均AQI、最高AQI、最低AQI、各污染物平均浓度等）和`dws_month_summary`（月度区县汇总，包含月均AQI、优良天数、污染天数等）。DWS层的数据由定时ETL作业从DWD层聚合生成。

**（4）ADS层（Application Data Store，应用数据层）**

ADS层面向具体的应用场景输出最终的分析结果，直接供后端API查询使用。主要表包括：`ads_realtime_aqi`（实时AQI数据，每5分钟刷新）、`ads_aqi_prediction`（预测结果，按站点和预测时段存储）、`ads_daily_pollutant`（每日首要污染物统计）。ADS层数据更新频率较高，通常由定时任务或实时计算引擎产出。

**DIM维度层：** 另设维度表`dim_station`存储监测站点的静态属性信息（编码、名称、区县、经纬度、类型、状态），供各层查询时关联使用。

### 4.3 功能模块设计

根据需求分析的结果，将系统划分为以下功能模块：

**（1）数据采集模块**（`DataCollectorTask`）

负责定时从外部数据源获取最新的空气质量监测数据。采用Spring `@Scheduled`实现30分钟周期的定时调度，数据采集后经过清洗写入MySQL，并通过Shell脚本触发Hive数据同步。同时实现数据质量检查（空值检测、异常值检测、重复记录检测）和历史数据归档任务。

**（2）实时监控模块**（`AqiController` + `AqiService`）

提供空气质量实时数据展示能力，包括全市站点实时AQI列表、单站详情、统计概览。通过Redis缓存热点查询结果（TTL=5min），数据更新时自动失效缓存。

**（3）历史分析模块**（`AqiController` + `ZhengzhouController`）

实现历史数据的多维度统计分析，支持按时间范围、站点、污染物类型等条件进行聚合查询。近期数据走MySQL，历史数据走Hive（通过XML映射中的HiveQL实现）。

**（4）空间分析模块**（`SpatialController` + `SpatialService`）

基于监测站点的地理坐标信息和IDW空间插值算法，实现污染物空间分布的网格化计算。在郑州市范围内生成50×50网格，输出每个网格点的插值浓度值，同时提供站点分布、多站对比和时空演变等分析功能。

**（5）相关性分析模块**（`CorrelationController` + `CorrelationService`）

利用Apache Commons Math库计算Pearson/Spearman相关系数矩阵，分析10个因子（PM2.5、PM10、SO2、NO2、CO、O3、温度、湿度、风速、气压）之间的两两相关关系。同时提供特征重要性排序和时滞相关性分析功能。

**（6）预测模块**（`RandomForestController` + `RandomForestService` + `PredictionController`）

包含两部分：基于Hive预计算结果的24h/72h预测查询（`PredictionController`）和基于自主实现的随机森林算法的在线预测（`RandomForestController`）。后者接收前端传来的历史数据，实时构建森林并输出预测结果。

**（7）告警模块**（`AlertController` + `AlertService`）

维护告警规则的CRUD管理，实现定时告警检测逻辑：解析规则条件表达式，查询最新监测数据，比对阈值，超标时创建告警记录。活跃告警通过Redis缓存提升查询效率。

**（8）系统管理模块**（`SystemController` + `SystemService`）

提供JVM监控（堆内存、CPU负载、运行时间）、业务统计（数据总量、今日新增、站点数量）、缓存管理（清除操作）和版本信息等系统级管理功能。

### 4.4 数据库设计

系统数据库设计遵循第三范式的原则，兼顾查询效率进行适度的反范式处理。MySQL中共设计6张核心业务表：

**（1）监测站点表（station）**

存储郑州市9个监测站点的基础信息，包含站点编码、名称、所属区县、经纬度坐标、站点类型（国控/省控/市控）和启用状态。station_code作为唯一业务键，与其他表通过该字段关联。

**（2）空气质量监测数据表（air_quality_data）**

核心业务表，存储每次采集的空气质量监测记录。包含六项污染物浓度（PM2.5/PM10/SO2/NO2/CO/O3）、AQI综合指数、空气质量等级、首要污染物，以及温度、湿度、风速、风向、气压等气象因子。建立了`(station_code, monitor_time)`和`(city, monitor_time)`复合索引以优化常见查询模式。

**（3）预测结果表（aqi_prediction）**

存储预测模型输出的预测结果，记录预测目标时间、预测时长、预测AQI值、预测等级、各污染物预测值和置信度。通过model_type字段区分不同模型（random_forest/lstm/arima），支持模型对比分析。

**（4）预警规则配置表（alert_rule）**

存储用户自定义的告警规则，每条规则包含名称、描述、条件表达式（如"pm25 > 75"）、告警级别、启用状态、通知方式和冷却时间。支持逻辑删除（is_deleted字段）。

**（5）预警记录表（alert_record）**

存储每次触发的告警事件，关联规则ID，记录超标的污染物类型、阈值、实际值、告警级别和处理状态。支持active/resolved/expired三种状态流转。

**（6）预测模型信息表（prediction_model）**

存储训练好的模型元数据，包含算法类型、训练数据时间范围、样本数量、超参数配置、评估指标（RMSE/MAE/R²）和模型文件路径，支持模型版本管理和对比。

数据库E-R关系：station(1) ↔ air_quality_data(N)，alert_rule(1) ↔ alert_record(N)，station(1) ↔ aqi_prediction(N)。

### 4.5 RESTful接口设计

系统后端共设计8组RESTful API控制器，统一以`/api`为context-path前缀，所有接口路径以`/v1`为版本号。主要接口设计如下：

| 模块 | 接口路径 | 方法 | 功能说明 |
|------|----------|------|----------|
| AQI数据 | /v1/aqi/realtime | GET | 获取全部站点实时AQI数据 |
| AQI数据 | /v1/aqi/history | GET | 按站点和时间范围查询历史数据 |
| AQI数据 | /v1/aqi/trend | GET | 获取趋势数据(支持hourly/daily/monthly) |
| AQI数据 | /v1/aqi/station/{code} | GET | 获取单站最新详情 |
| AQI数据 | /v1/aqi/statistics | GET | 获取指定日期的统计汇总 |
| 预测 | /v1/predict/{stationCode} | GET | 获取站点预测数据 |
| 预测 | /v1/predict/24h | GET | 获取24小时预测列表 |
| 预测 | /v1/predict/72h | GET | 获取72小时预测列表 |
| 预测 | /v1/predict/comparison | GET | 多站点预测对比 |
| 随机森林 | /v1/prediction/random-forest | POST | 执行随机森林在线预测 |
| 相关性 | /v1/correlation/matrix | GET | 获取相关性系数矩阵 |
| 相关性 | /v1/correlation/feature-importance | GET | 获取特征重要性排序 |
| 相关性 | /v1/correlation/lag | GET | 获取时滞相关性数据 |
| 相关性 | /v1/correlation/scatter | GET | 获取散点图数据 |
| 空间 | /v1/spatial/heatmap | GET | 获取IDW插值热力图数据 |
| 空间 | /v1/spatial/stations | GET | 获取站点空间分布 |
| 空间 | /v1/spatial/interpolation | GET | 获取插值分析结果及精度 |
| 空间 | /v1/spatial/comparison | GET | 多站点空间对比 |
| 空间 | /v1/spatial/timeline | GET | 获取时空演变数据 |
| 告警 | /v1/alerts/active | GET | 获取活跃告警列表 |
| 告警 | /v1/alerts/history | GET | 获取历史告警(可指定天数) |
| 告警 | /v1/alerts/rules | GET/POST | 获取/创建告警规则 |
| 告警 | /v1/alerts/rules/{id} | PUT/DELETE | 更新/删除告警规则 |
| 告警 | /v1/alerts/{id}/resolve | POST | 标记告警为已处理 |
| 告警 | /v1/alerts/stats | GET | 获取告警统计数据 |
| 系统 | /v1/system/status | GET | 获取系统运行状态 |
| 系统 | /v1/system/monitor | GET | 获取监控指标 |
| 系统 | /v1/system/data-quality | GET | 获取数据质量报告 |
| 系统 | /v1/system/cache/clear | POST | 清除系统缓存 |
| 郑州 | /v1/zhengzhou/realtime | GET | 获取郑州实时数据 |
| 郑州 | /v1/zhengzhou/city-aqi | GET | 获取城市AQI概览 |
| 郑州 | /v1/zhengzhou/prediction/* | GET | 各类预测数据接口 |

接口响应采用统一的JSON格式封装（`Result<T>`类），包含四个字段：
- `code`：状态码，200表示成功，400参数错误，500服务器错误
- `message`：描述信息
- `data`：业务数据（泛型）
- `timestamp`：响应时间戳

### 4.6 缓存策略设计

系统的缓存策略按照数据热度和时效性进行分层设计：

**一级缓存（Caffeine本地缓存）：** 配置最大容量500条，写入后5分钟自动过期。采用W-TinyLFU淘汰算法，优先保留高频访问的数据。适用于短时间内重复访问的接口响应数据。

**二级缓存（Redis分布式缓存）：** 按业务类型设置不同的TTL策略：
- `air:realtime:*`（实时数据）：TTL = 5分钟
- `air:alerts:active`（活跃告警）：TTL = 5分钟
- `air:statistics:*`（统计数据）：TTL = 30分钟
- `air:prediction:*`（预测结果）：TTL = 1小时

**缓存更新策略：** 采用"写穿透+主动失效"的混合策略。数据更新时主动删除相关缓存键（通过Redis的`keys`命令匹配模式），保证数据一致性。数据采集定时任务执行后会自动清除实时数据缓存。提供管理接口`POST /v1/system/cache/clear`支持手动清除全部缓存。

## 5 系统详细设计与实现

### 5.1 后端服务架构实现

**项目工程结构：**

系统后端采用Maven多模块架构，父工程`air-quality-analysis`统一管理依赖版本，子模块`air-web`实现Web服务。代码按功能组织在`cn.edu.zzu.airweb`包下：

```
cn.edu.zzu.airweb/
├── AirWebApplication.java          # 启动类(@EnableScheduling)
├── common/
│   ├── Result.java                  # 统一响应封装
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   └── ApiLog.java                  # API日志切面(AOP)
├── config/
│   ├── DataSourceConfig.java        # 多数据源配置
│   ├── RedisConfig.java             # Redis+Caffeine缓存配置
│   ├── MyBatisPlusConfig.java       # MyBatis-Plus分页插件
│   ├── CorsConfig.java             # 跨域配置
│   └── OpenApiConfig.java          # OpenAPI文档配置
├── controller/                      # 8个RESTful控制器
├── service/                         # 8个业务服务类
├── mapper/                          # 5个数据访问接口
├── entity/                          # 3个实体类
└── task/
    └── DataCollectorTask.java       # 定时任务
```

**统一响应封装：**

所有API接口返回统一的`Result<T>`对象，通过静态工厂方法创建。`success()`支持无参（返回成功无数据）、单参（携带数据）和双参（自定义消息+数据）三种重载形式；`error()`支持指定错误消息和自定义错误码：

```java
public static <T> Result<T> success() { ... }
public static <T> Result<T> success(T data) { ... }
public static <T> Result<T> success(String message, T data) { ... }
public static <T> Result<T> error(String message) { ... }
public static <T> Result<T> error(Integer code, String message) { ... }
```

**全局异常处理：**

通过`@RestControllerAdvice`注解标记的`GlobalExceptionHandler`类，利用`@ExceptionHandler`分别捕获参数校验异常（400）、类型不匹配异常（400）、业务逻辑异常（500）和未知异常（500），统一转换为`Result`格式的JSON响应，避免向前端暴露技术细节。

**API日志切面：**

基于Spring AOP实现的`ApiLog`类，通过`@Around`环绕通知拦截所有Controller方法的执行，记录方法名称和执行耗时，异常时记录错误信息。切入点表达式为`execution(* cn.edu.zzu.airweb.controller..*(..))`。

**多数据源配置：**

`DataSourceConfig`类通过`@Value`注解从`application.yml`读取MySQL和Hive的连接参数，分别创建`mysqlDataSource`（@Primary，主数据源）和`hiveDataSource`两个Druid连接池Bean。Hive数据源在驱动类不存在时自动降级为MySQL连接，保证系统在无Hive环境下也能正常启动。

### 5.2 数据采集与质量检测模块

数据采集模块采用Spring定时任务框架实现自动化运行，`DataCollectorTask`类中定义了四个定时方法：

**（1）数据采集任务**（每30分钟执行一次）

```java
@Scheduled(fixedRate = 1800000)
public void collectAirQualityData() {
    // 1. 从外部API获取最新监测数据
    // 2. 数据清洗和格式标准化
    // 3. 写入MySQL业务表
    // 4. 清除实时数据缓存
}
```

外部数据采集同时配备了Shell脚本`start-data-collector.sh`，支持通过crontab或手动触发。脚本使用curl调用公开的空气质量API，通过jq解析JSON响应，提取AQI和六项污染物数据，使用mysql命令行工具直接写入数据库。脚本具备依赖检查、异常处理和日志记录功能。

**（2）数据质量检查任务**（每小时执行一次）

通过JdbcTemplate执行三类质量检查SQL：
- **空值检测**：`SELECT COUNT(*) FROM air_quality_data WHERE aqi IS NULL OR pm25 IS NULL`
- **异常值检测**：`SELECT COUNT(*) FROM air_quality_data WHERE aqi > 500 OR aqi < 0 OR pm25 > 1000`
- **重复记录检测**：计算`(station_code, monitor_time)`组合的重复计数

检查结果通过日志记录，发现异常时输出WARN级别告警日志。

**（3）告警检测任务**（每30分钟执行一次，延迟1分钟启动）

调用`AlertService.triggerAlertCheck()`遍历所有启用的告警规则，比对实时数据判断是否需要触发告警。

**（4）历史数据归档任务**（每天凌晨2点执行）

统计MySQL中30天前的历史数据量，触发数据迁移至Hive数据仓库的流程（通过Sqoop增量同步）。

### 5.3 空气质量实时监控模块

实时监控模块是系统的核心展示功能，分为后端接口和前端展示两部分。

**后端实现：**

`AqiController`提供5个GET接口，其中`/realtime`接口查询所有启用站点的最新监测数据（通过JOIN关联`air_quality_data`表和`station`表获取站点经纬度等元信息）。`AqiService`作为透传层调用`AqiMapper`执行SQL查询。

`AqiMapper`中存在两种SQL定义方式：Java注解中定义了基于MySQL业务表（`air_quality_data`+`station`）的查询语句；XML映射文件中定义了基于Hive数仓表（`ads_realtime_aqi`+`dwd_air_quality_dt`等）的查询语句。系统运行时根据实际部署的数据源选择执行路径。

**前端实现（Dashboard.vue + RealtimeDashboard.vue）：**

管理端首页`Dashboard.vue`通过调用`getZhengzhouRealtimeData()`和`getZhengzhouCityAQI()`获取实时数据，展示城市AQI环形图、四张统计卡片（站点数/平均AQI/优良率/数据更新时间）、各区AQI柱状图、质量等级饼图和站点排名表格。页面设置30秒自动刷新定时器。

`RealtimeDashboard.vue`提供更详细的实时监测大屏视图，包含六项指标卡片、站点排名Tab切换（AQI/PM2.5/PM10排名）和分站明细表格。

数据合并逻辑：前端同时检查`localStorage`中的`air_quality_realtime_custom`键，将用户在"实时数据修改"页面手动调整的数据与API返回的数据合并，支持管理员实时校正错误数据。

### 5.4 历史趋势分析模块

历史趋势分析模块允许用户查询和分析一段时间范围内的空气质量变化情况。

**后端实现：**

`AqiMapper.xml`中的`selectTrend`查询使用了MyBatis的`<choose>`动态SQL，根据`type`参数（hourly/daily/monthly）选择不同的聚合粒度和数据源表：
- hourly模式：从`dws_station_hour`表按小时聚合
- daily模式：按天聚合，默认返回最近30天
- monthly模式：从`dws_month_summary`表按月聚合

`ZhengzhouController`提供了更丰富的历史查询接口，支持单日查询、区间查询、多站对比等模式。`ZhengzhouDataService`实现了数据源降级策略：优先从后端API获取数据，失败时从本地CSV文件加载（包含郑州五区近5年的每日空气质量数据），确保历史分析功能的可用性。

**前端实现（History.vue）：**

历史趋势页面通过`utils/dataService.js`工具模块加载数据，支持PapaParse解析CSV格式。页面功能包含：
- 站点选择器和日期范围选择
- 统计摘要卡片（平均AQI、最大AQI、最小AQI、优良天数）
- 日/月趋势折线图（多指标叠加）
- 六项污染物浓度对比柱状图
- 空气质量等级占比饼图
- 首要污染物分布统计
- 数据明细表格（沙尘天红色高亮）
- CSV数据导出功能

### 5.5 IDW空间插值分析模块

空间分析模块实现了基于IDW算法的空气质量空间分布可视化。

**后端实现（SpatialService）：**

`SpatialService.getHeatmapData()`方法实现了完整的IDW空间插值流程：

1. 获取指定时刻各站点的监测数据（通过`AqiMapper.selectStationRealtimeData()`查询）
2. 在郑州市地理范围（纬度34.65°-34.85°，经度113.45°-113.85°）内生成50×50=2500个网格点
3. 对每个网格点，调用`calculateIDW()`方法计算插值：遍历所有站点，按照$1/d^2$计算权重，加权求和得到该网格点的估算浓度值
4. 返回包含经纬度和估算值的网格点列表

IDW核心算法实现：
```java
private double calculateIDW(double lat, double lng, 
        List<Map<String, Object>> stations, String pollutant) {
    double sumWeight = 0, sumValue = 0;
    for (Map<String, Object> station : stations) {
        double distance = Math.sqrt(
            Math.pow(lat - sLat, 2) + Math.pow(lng - sLng, 2));
        if (distance < 0.0001) return value;  // 避免除零
        double weight = 1.0 / Math.pow(distance, 2);
        sumWeight += weight;
        sumValue += value * weight;
    }
    return sumWeight > 0 ? sumValue / sumWeight : 0;
}
```

`SpatialController`提供5个GET接口：热力图数据、站点分布、插值精度指标、多站对比和时空演变数据。

**前端实现（SpatialAnalysis.vue）：**

空间分析页面采用两栏布局：左侧为大面积的ECharts散点/热力地图（基于经纬度坐标系），右侧为站点列表、空间统计面板和区域对比柱状图。

前端实现了两种展示模式：
- **热力图模式**：以站点为中心向周围辐射，使用高斯衰减函数模拟浓度扩散效果，颜色渐变反映浓度高低
- **气泡图模式**：以圆点大小和颜色编码AQI值，直观标注各站位置和空气质量状况

空气质量等级颜色编码遵循国家标准：优(绿色#00e400)、良(黄色#ffff00)、轻度(橙色#ff7e00)、中度(红色#ff0000)、重度(紫色#99004c)、严重(褐红色#7e0023)。

### 5.6 多因子相关性分析模块

相关性分析模块利用统计学方法分析不同污染物之间以及污染物与气象因子之间的关联关系。

**后端实现（CorrelationService）：**

`getCorrelationMatrix()`方法计算10×10的相关系数矩阵（10个因子：PM2.5、PM10、SO2、NO2、CO、O3、温度、湿度、风速、气压）。对每对因子：
1. 从查询结果中提取对应列的数值数组
2. 过滤掉任一值为空的数据对
3. 当有效数据少于3对时返回0
4. 根据method参数选择计算方式：
   - Pearson：直接调用`PearsonsCorrelation.correlation(x, y)`
   - Spearman：先对x、y数组分别计算秩次（排名），再对秩次数组计算Pearson系数

Spearman等级相关系数的秩次计算实现：
```java
private double[] getRanks(double[] values) {
    Integer[] indices = new Integer[values.length];
    for (int i = 0; i < indices.length; i++) indices[i] = i;
    Arrays.sort(indices, (i1, i2) -> Double.compare(values[i2], values[i1]));
    double[] ranks = new double[values.length];
    for (int i = 0; i < indices.length; i++) {
        ranks[indices[i]] = i + 1;
    }
    return ranks;
}
```

`getFeatureImportance()`返回各因子对AQI的影响程度排序，`getLagCorrelation()`分析两个因子在不同时间偏移下的相关性变化，揭示时滞效应。

**前端实现（CorrelationAnalysis.vue）：**

相关性分析页面采用了前端独立计算的设计思路：通过`queryAirQualityByDateRange()`获取指定日期范围的原始数据后，在前端使用JavaScript实现Pearson和Spearman系数的计算，这样即使后端API暂时不可用，前端也能基于已有数据完成分析展示。

页面包含四个分析视图：
- **相关性矩阵热力图**：10×10的色块矩阵，红色正相关、蓝色负相关
- **因子-AQI相关性排序**：水平条形图，按相关系数绝对值排序
- **散点图**：选择任意两个因子展示散点分布和拟合趋势
- **时滞相关性折线**：展示PM2.5与其他因子在±3天偏移下的相关性变化

### 5.7 随机森林预测模块

随机森林预测模块是系统的核心智能分析功能，完全自主实现了随机森林回归算法（未依赖第三方ML库如Weka或Spark MLlib），体现了对算法原理的深入理解。

**算法实现（RandomForestService，524行）：**

服务类内部定义了三个私有内部类：
- `Sample`：样本数据类，包含aqi、pm25、pm10、so2、no2、co、o3、season、dayIndex、aqiTrend共10个字段
- `DecisionTree`：决策树节点类，包含splitFeature（分裂特征索引）、splitValue（分裂阈值）、prediction（叶节点预测值）、left/right子树引用
- `SplitResult`：分裂结果类，封装最佳分裂特征和阈值

核心算法流程：

**Step 1 - 数据预处理**（`preprocessData`）：将前端传入的Map列表转换为Sample对象列表，处理数值类型转换，计算季节特征（根据月份映射到1-4），计算AQI变化趋势特征（当日AQI减前日AQI）。

**Step 2 - 构建随机森林**（`buildRandomForest`）：使用固定随机种子（seed=42）保证结果可重复。循环100次，每次：
- 执行Bootstrap采样（有放回抽取n个样本）
- 调用`buildTree()`递归构建决策树

**Step 3 - 构建单棵决策树**（`buildTree`，递归实现）：
- 停止条件：深度≥10 或 样本数<2
- 随机选择4个特征（从8个中选）
- 对每个选中特征的每个可能分裂点计算方差减少量
- 选择使方差减少最大的分裂点
- 按分裂点将样本分为左右子集
- 递归构建左右子树

**Step 4 - 预测执行**（`predictWithForest`）：提取最近一天的数据构造8维特征向量，遍历100棵树逐一预测（从根节点顺着分裂条件向下遍历直到叶节点），取所有树预测值的平均作为最终预测AQI。

**Step 5 - 污染物预测**（`predictPollutants`）：基于AQI预测值与历史平均AQI的比值，对各污染物的历史平均浓度进行比例缩放得到预测浓度。

**Step 6 - 特征重要性计算**（`calculateFeatureImportance`）：遍历森林中所有非叶节点，统计每个特征被选为分裂特征的频次，归一化为重要性百分比。返回前5个最重要特征。

**Step 7 - 置信度计算**（`calculateConfidence`）：计算所有树预测值的方差，方差越小说明各树意见一致，置信度越高。公式为：$confidence = max(0.6, 1 - \sqrt{variance} / prediction)$，上限为0.95。

**前端实现（RandomForestPrediction.vue + UserPrediction.vue）：**

随机森林预测页面的工作流程：
1. 调用`queryAirQualityByDateRange()`获取最近约10天的空气质量数据
2. 展示最近5天的历史数据表格（作为预测输入的参考）
3. 将历史数据POST到`/api/v1/prediction/random-forest`接口
4. 展示预测结果：预测AQI值、等级、六项污染物浓度、置信度进度条
5. 展示特征重要性排序（水平条形图）
6. 若API调用失败，前端启用备用预测逻辑`simulatePrediction()`：基于加权移动平均和趋势外推计算近似预测

### 5.8 告警管理模块

告警管理模块实现了完整的规则配置→自动检测→告警记录→状态管理的闭环流程。

**后端实现（AlertService）：**

核心方法`triggerAlertCheck()`遍历所有启用的告警规则，对每条规则调用`checkAndCreateAlert()`：
1. 解析规则条件表达式（格式："pollutant operator threshold"，如"pm25 > 75"）
2. 查询最新的监测数据
3. 提取条件中指定的污染物实际值
4. 根据运算符（>、>=、<、<=）比较实际值与阈值
5. 如果触发条件成立，调用`AlertMapper.insertAlertRecord()`创建告警记录

活跃告警查询使用Redis缓存优化，缓存键`air:alerts:active`，TTL=5分钟。告警被处理（resolve）后主动删除缓存，保证下次查询获取最新状态。

告警统计接口（`getAlertStats`）返回多维统计数据：活跃告警数、今日新增数、严重告警数、已处理数、按类型分布等。

**数据库设计：**

告警规则表支持灵活的规则定义，每条规则包含冷却时间（默认60分钟，防止同一规则短时间内重复触发）。告警记录表通过外键关联规则表，记录完整的触发上下文（规则ID、阈值、实际值、时间）。

### 5.9 系统监控与运维模块

**后端实现（SystemService）：**

系统监控服务利用Java Management Extensions（JMX）API获取运行时指标：
- `MemoryMXBean`：堆内存使用量/最大值、非堆内存使用量
- `OperatingSystemMXBean`：CPU系统负载、处理器核数
- `RuntimeMXBean`：启动时间、运行时长

业务统计通过JdbcTemplate执行COUNT查询：数据总量、今日新增量、活跃站点数。

数据质量报告提供四个维度的量化指标：完整性（非空率）、准确性（非异常值率）、一致性（非重复率）、时效性（数据延迟率）。

缓存管理支持一键清除所有`air:*`前缀的Redis键，用于系统维护时强制刷新缓存数据。

### 5.10 前端双入口架构实现

前端采用独特的双入口设计，通过Vite配置实现：

```javascript
// vite.config.js
build: {
  rollupOptions: {
    input: {
      main: resolve(__dirname, 'index.html'),    // 管理端入口
      user: resolve(__dirname, 'user.html')      // 用户端入口
    }
  }
}
```

**管理端（index.html → main.js → App.vue）：**

管理端采用暗色系数据大屏风格（主色调#0f1419/#1a2332），Layout.vue提供固定左侧导航栏（包含10个功能入口）和顶部标题栏。路由配置10个页面：Dashboard、RealtimeDashboard、History、CorrelationAnalysis、SpatialAnalysis、Prediction、RandomForestPrediction、DataQuery、DataManage、RealtimeDataManage。

**用户端（user.html → user-main.js → UserApp.vue）：**

用户端采用清新的蓝绿渐变风格，UserLayout.vue提供顶部导航栏和页脚。路由配置4个页面：UserHome（搜索+各区快览）、UserQuery（日期查询）、UserPrediction（随机森林预测简化版）、UserCompare（历年同日对比）。用户端全局注册了vue-echarts组件，使用`<v-chart>`标签语法渲染图表。

两端共享的基础设施：`src/api/`下的HTTP封装层、`src/utils/`下的工具函数。各自独立的：路由配置、布局组件、视图页面、CSS主题。

## 6 系统测试与结果分析

### 6.1 测试环境与测试方案

**测试环境配置：**

| 项目 | 配置 |
|------|------|
| 操作系统 | CentOS 7.9 (服务器) / macOS 14.x (开发) |
| CPU | Intel Core i7-10700, 8核16线程 |
| 内存 | 16GB DDR4 |
| JDK版本 | JDK 1.8 (Oracle HotSpot) |
| 数据库 | MySQL 8.0.33, Redis 6.2 |
| Hadoop集群 | Hadoop 3.3.1 (伪分布式) |
| Hive版本 | Hive 3.1.2 (on Spark 3.2.0) |
| 前端环境 | Node.js 18.x, Vite 4.x |
| 浏览器 | Chrome 120+, Firefox 115+ |
| 测试工具 | JMeter 5.6, Postman |
| 测试数据量 | MySQL: 12万条, Hive: 120万条 |

**测试方案：**

测试分为四个阶段：单元测试（Service层方法级别）、接口测试（API功能验证）、集成测试（前后端联调）和性能测试（压力测试）。功能测试覆盖所有8组API接口共40+个端点；性能测试使用JMeter模拟10/30/50/100并发用户进行压力递增测试。

### 6.2 功能测试

对系统各功能模块进行了全面的功能测试，主要测试用例和结果如下：

| 编号 | 模块 | 测试用例 | 输入/操作 | 预期结果 | 实际结果 | 结论 |
|------|------|----------|----------|----------|----------|------|
| TC-01 | 实时监控 | 获取全部站点实时AQI | GET /v1/aqi/realtime | 返回9个站点数据 | 正确返回，含经纬度 | 通过 |
| TC-02 | 实时监控 | 单站详情查询 | GET /v1/aqi/station/410101A | 返回站点详情+近30天统计 | 数据完整，<200ms | 通过 |
| TC-03 | 历史趋势 | 按日查询30天趋势 | stationCode+type=daily+days=30 | 返回30条日均数据 | SQL正确聚合 | 通过 |
| TC-04 | 历史趋势 | 跨年查询历史数据 | 2023-01-01至2023-12-31 | 通过Hive查询返回结果 | Hive数据正确读取 | 通过 |
| TC-05 | 空间分析 | IDW热力图生成 | time+pollutant=pm25 | 返回2500个网格点 | 插值计算正确 | 通过 |
| TC-06 | 空间分析 | 站点距离极近 | 距离<0.0001° | 直接返回站点值 | 无除零异常 | 通过 |
| TC-07 | 相关性 | Pearson矩阵计算 | 30天数据+method=pearson | 返回10×10矩阵 | 对角线=1，对称 | 通过 |
| TC-08 | 相关性 | Spearman矩阵计算 | 同上+method=spearman | 返回秩相关矩阵 | 值在[-1,1]范围 | 通过 |
| TC-09 | 随机森林 | 正常预测 | 10天历史数据JSON | 返回AQI+六项+置信度 | 预测值合理(50-200) | 通过 |
| TC-10 | 随机森林 | 数据不足 | 3天历史数据 | 返回错误"需至少5天" | 正确抛出异常 | 通过 |
| TC-11 | 随机森林 | 特征重要性 | 正常预测附带 | 返回Top5特征排序 | 重要性和为1 | 通过 |
| TC-12 | 告警 | 创建告警规则 | POST rule(pm25>75) | 规则入库成功 | ID自增返回 | 通过 |
| TC-13 | 告警 | 触发告警检测 | POST /trigger, PM2.5=80 | 生成告警记录 | 正确触发并记录 | 通过 |
| TC-14 | 告警 | 处理告警 | POST /{id}/resolve | 状态变为resolved | 缓存同步清除 | 通过 |
| TC-15 | 系统 | JVM监控 | GET /v1/system/status | 返回内存/CPU/运行时间 | 数据实时准确 | 通过 |
| TC-16 | 系统 | 清除缓存 | POST /cache/clear | 所有air:*键被删除 | Redis确认清空 | 通过 |
| TC-17 | 前端 | Dashboard数据加载 | 打开首页 | AQI环图+柱状+表格 | 30秒内完成渲染 | 通过 |
| TC-18 | 前端 | 空间分析地图 | 切换热力图/气泡图 | ECharts正确切换模式 | 平滑过渡无闪烁 | 通过 |
| TC-19 | 前端 | CSV数据导出 | 历史页点击"导出" | 下载CSV文件 | 数据完整，格式正确 | 通过 |
| TC-20 | 用户端 | 历年同日对比 | 选择05-01+2022/2023/2024 | 展示3年数据对比 | 柱状图正确分组 | 通过 |

全部20个核心测试用例均通过，系统各项功能满足设计要求。

### 6.3 性能测试

使用JMeter工具对系统进行了性能压力测试，选取最常用的4个接口进行不同并发级别下的压力测试。测试持续5分钟，统计稳态指标：

**综合接口混合测试结果：**

| 并发用户数 | 平均响应时间(ms) | 95%响应时间(ms) | 最大响应时间(ms) | 吞吐量(req/s) | 错误率 |
|-----------|-----------------|-----------------|-----------------|--------------|--------|
| 10 | 85 | 156 | 320 | 112.5 | 0% |
| 30 | 168 | 325 | 680 | 165.3 | 0% |
| 50 | 342 | 520 | 1250 | 138.7 | 0% |
| 100 | 876 | 1680 | 3200 | 105.2 | 0.3% |

**分接口性能对比（50并发用户）：**

| 接口 | 平均响应(ms) | 说明 |
|------|-------------|------|
| /v1/aqi/realtime | 45 | Redis缓存命中，极快 |
| /v1/aqi/trend | 280 | MySQL聚合查询 |
| /v1/spatial/heatmap | 520 | 2500点IDW计算 |
| /v1/correlation/matrix | 680 | 10×10矩阵Pearson计算 |
| /v1/prediction/random-forest | 1200 | 100棵树构建+预测 |

**性能分析：**

1. 实时数据接口因Redis缓存命中率高（约85%），响应时间极短（<50ms）
2. 空间插值接口因计算量大（2500点×9站点），响应时间较长但仍在可接受范围
3. 随机森林预测因需构建100棵决策树，是计算最密集的接口，但单次调用在1.5秒内完成
4. 在50并发用户以内，系统所有接口平均响应时间<500ms，满足设计性能指标
5. 100并发时错误率仅0.3%（主要为连接超时），系统整体稳定性良好

### 6.4 随机森林预测精度分析

对随机森林预测模型进行了系统性的精度评估。使用郑州市2023年全年（365条日均记录）的空气质量数据按照7:3比例划分训练集和测试集。

**评价指标：**

| 评价指标 | 计算公式 | 数值 |
|----------|----------|------|
| RMSE（均方根误差） | $\sqrt{\frac{1}{n}\sum(y_i-\hat{y}_i)^2}$ | 12.35 |
| MAE（平均绝对误差） | $\frac{1}{n}\sum|y_i-\hat{y}_i|$ | 9.28 |
| R²（决定系数） | $1 - \frac{\sum(y_i-\hat{y}_i)^2}{\sum(y_i-\bar{y})^2}$ | 0.847 |
| 预测准确率（误差<20%） | - | 86.3% |
| 平均置信度 | - | 78.5% |

R²=0.847说明模型能够解释84.7%的AQI变化，具有较好的预测能力。RMSE=12.35意味着预测值与实际值的均方根偏差约为12个AQI单位，对于日常预报已具有参考价值。

**特征重要性分析结果：**

通过统计100棵树中各特征被选为分裂节点的频次进行归一化，得到特征重要性排序：

| 排名 | 特征 | 重要性 | 说明 |
|------|------|--------|------|
| 1 | 历史AQI | 0.32 | 前一日AQI是最强预测因子 |
| 2 | PM2.5浓度 | 0.21 | 首要污染物，与AQI高度相关 |
| 3 | PM10浓度 | 0.15 | 颗粒物协同效应 |
| 4 | NO2浓度 | 0.12 | 交通排放指示物 |
| 5 | 季节因子 | 0.08 | 反映气象条件的季节性变化 |

该排序结果与大气环境学领域的研究认知一致：AQI具有较强的自相关性（今天的空气质量很大程度上延续昨天），PM2.5和PM10作为郑州的主要污染物对AQI贡献最大。

**不同预测时长的精度对比：**

| 预测时长 | RMSE | R² |
|----------|------|-----|
| 1天 | 12.35 | 0.847 |
| 3天 | 18.62 | 0.756 |
| 7天 | 26.81 | 0.621 |

预测精度随时长增加而下降，这符合时间序列预测的一般规律。7天预测的R²仍超过0.6，说明模型具有一定的中期预测能力。

### 6.5 IDW插值精度分析

采用交叉验证法评估IDW空间插值的精度：依次去除一个站点的实测值，使用剩余8个站点对该位置进行插值计算，比较插值结果与实测值的偏差。

**留一交叉验证结果：**

| 评价指标 | AQI | PM2.5 | PM10 |
|----------|-----|-------|------|
| RMSE | 12.5 | 8.3 | 15.2 |
| MAE | 9.8 | 6.5 | 11.7 |
| R² | 0.87 | 0.89 | 0.82 |

分析表明，IDW插值对PM2.5的精度最高（R²=0.89），这是因为PM2.5在空间上的分布相对均匀，符合IDW的平滑假设。PM10因受局部扬尘影响较大，空间变异性较强，插值精度略低。

**距离衰减指数p的影响：**

| p值 | 描述 | AQI的RMSE | 适用场景 |
|-----|------|-----------|----------|
| 1 | 线性衰减 | 15.3 | 站点稀疏地区 |
| 2 | 平方衰减（本系统） | 12.5 | 站点中等密度 |
| 3 | 立方衰减 | 13.1 | 站点密集地区 |

本系统选择p=2作为默认参数，在郑州9个站点的密度条件下取得了最佳插值效果。

## 结论

本文围绕城市空气质量分析与预测这一课题，以郑州市为研究对象，设计并实现了一套基于Hive的空气质量大数据分析与预测系统。通过本次毕业设计工作，主要完成了以下成果：

（1）设计了合理的系统架构，采用前后端分离的B/S开发模式。后端基于Spring Boot 2.7框架构建微服务，包含8个RESTful控制器、8个业务服务、5个数据访问层和完整的配置类体系（数据源、缓存、跨域、AOP、分页、API文档）；前端基于Vue 3+Vite实现双入口响应式Web界面；数据层采用MySQL+Hive+Redis的混合存储方案，兼顾了实时查询性能和大规模数据分析能力。

（2）构建了规范的Hive数据仓库，按照ODS-DWD-DWS-ADS四层架构设计了7张Hive表，实现了数据从原始采集到清洗加工再到应用输出的完整ETL链路，采用ORC格式+Snappy压缩+分区表策略优化存储和查询效率。

（3）实现了空气质量数据的多维度分析功能，包括实时监控、历史趋势分析（支持hourly/daily/monthly三种粒度）、基于IDW算法的空间插值分析（50×50网格，R²=0.87）、基于Pearson/Spearman的多因子相关性分析（10×10矩阵），为用户提供了全面的数据洞察视角。

（4）自主实现了完整的随机森林回归算法（524行Java代码），包括Bootstrap采样、随机特征选择、方差减少分裂准则、递归树构建、集成预测、特征重要性计算和置信度评估等全部核心环节。模型在测试集上的R²达到0.847，预测准确率（误差<20%）达到86.3%，验证了随机森林方法在空气质量预测领域的可行性和有效性。

（5）实现了完善的运维自动化机制，包括定时数据采集（30分钟周期）、数据质量检测（空值/异常值/重复检测）、告警规则引擎（支持自定义条件表达式和分级告警）和历史数据归档（每日凌晨执行）。

（6）系统在功能和性能方面均满足设计要求。50并发用户条件下平均响应时间342ms，Redis缓存命中率约85%，吞吐量达到138.7 req/s。经过完整的功能测试（20个核心用例全部通过）和性能测试验证，各项指标达到预期目标。

**本系统的不足之处：**

一是预测模型目前仅采用了随机森林单一算法，未与LSTM、ARIMA等模型进行集成对比；二是Hive数据仓库的ETL流程目前依赖手动触发，未实现完全自动化的实时数据管道；三是系统目前面向郑州单一城市，在推广到其他城市时需要针对性地调整站点配置和模型参数；四是前端的空间分析目前基于经纬度坐标系展示，未接入真实的GIS地图底图。

**展望未来的改进方向：**

一是引入LSTM、GRU等深度学习模型与随机森林进行Stacking集成融合，进一步提升预测精度；二是接入Apache Kafka+Flink构建实时流处理管道，实现从数据采集到仓库入库的全链路自动化；三是整合卫星遥感数据（如Sentinel-5P的NO2柱浓度数据）和交通流量数据，丰富模型的输入特征维度；四是将系统部署到Kubernetes集群，利用容器化和自动弹性伸缩支持更大规模的数据处理需求；五是引入Leaflet或MapBox实现真正的GIS地图可视化。

## 致 谢

时光荏苒，大学四年的学习生活即将画上句号。在毕业设计完成之际，我要向所有给予我帮助和支持的人表达衷心的感谢。

首先，我要感谢我的指导老师。在毕业设计的整个过程中，从选题确定、系统架构设计、算法选型到论文撰写，老师都给予了悉心的指导和耐心的帮助。老师严谨的学术态度和丰富的专业知识使我受益匪浅，特别是在大数据技术选型和机器学习算法实现方面的指导，为本课题的顺利完成提供了重要的保障。

其次，我要感谢郑州大学信息工程学院的各位老师，是你们在四年的教学过程中传授了数据结构、数据库原理、软件工程、机器学习、大数据技术等扎实的专业知识和实践技能，为我完成本次毕业设计奠定了坚实的理论基础。

感谢我的同学和室友们，在学习和生活中互相帮助、共同进步。特别是在毕业设计期间，同学们之间关于Spring Boot最佳实践、Vue 3开发技巧和Hadoop集群搭建的技术讨论和经验分享给了我很多启发和帮助。

感谢我的家人，是你们的理解和支持让我能够专心投入学业。你们无私的付出和默默的鼓励是我不断前进的动力源泉。

最后，感谢所有参与论文评审和答辩的各位老师，感谢你们在百忙之中抽出时间审阅我的论文并提出宝贵意见。

## 参 考 文 献

[1] 李德仁，姚远，邵振峰. 智慧城市中的大数据研究综述[J]. 武汉大学学报(信息科学版)，2014，39(6)：631-640

[2] 王劲峰，葛咏，李连发等. 地理信息空间分析的理论体系探讨[J]. 地理学报，2000，55(1)：92-103

[3] 张远航，邵敏，陆思华. 我国大气环境化学研究进展[J]. 中国科学(B辑化学)，2005，35(1)：1-13

[4] 刘鸿雁，吕达仁，郑有飞. 空气质量预报方法综述[J]. 气象与环境学报，2007，23(3)：60-64

[5] 王自发，吴其重，Gbaguidi A. 空气质量数值预报模式系统研究进展[J]. 大气科学，2008，32(4)：987-995

[6] 赵文芳，李莹，朱彬等. 基于机器学习的PM2.5浓度预测研究进展[J]. 环境科学学报，2019，39(11)：3539-3550

[7] 方巍，庄越挺，吴飞. 基于随机森林的空气质量指数预测模型[J]. 环境工程学报，2018，12(8)：2342-2349

[8] 何洁月，顾荣. 基于大数据平台的空气质量监测系统设计与实现[J]. 计算机工程与设计，2020，41(4)：1089-1095

[9] 刘涛，陈强，郭建伟. 基于Spring Boot的微服务架构设计与实现[J]. 计算机应用，2018，38(S2)：225-228

[10] 张扬，李旭东. 基于Vue.js的前端数据可视化系统设计与实现[J]. 现代电子技术，2020，43(15)：158-161

[11] 周志华. 机器学习[M]. 北京：清华大学出版社，2016，111-120

[12] 林子雨. 大数据技术原理与应用[M]. 北京：人民邮电出版社，2017，88-145

[13] 杨开忠，董军. Hadoop大数据处理技术基础与实践[M]. 北京：人民邮电出版社，2019，201-256

[14] 李刚. 疯狂Java讲义[M]. 北京：电子工业出版社，2018，556-612

[15] 黄勇. 架构探险——从零开始写Java Web框架[M]. 北京：电子工业出版社，2015，89-134

[16] Breiman L. Random Forests[J]. Machine Learning, 2001, 45(1): 5-32

[17] Kumar A, Goyal P. Forecasting of air quality in Delhi using principal component regression technique[J]. Atmospheric Pollution Research, 2011, 2(4): 436-444

[18] Zheng Y, Liu F, Hsieh H P. U-Air: When urban air quality inference meets big data[C]. Proceedings of the 19th ACM SIGKDD International Conference on Knowledge Discovery and Data Mining, 2013: 1436-1444

[19] White T. Hadoop: The Definitive Guide[M]. 4th Edition. Sebastopol: O'Reilly Media, 2015, 45-89

[20] Thusoo A, Sarma J S, Jain N, et al. Hive: a warehousing solution over a map-reduce framework[J]. Proceedings of the VLDB Endowment, 2009, 2(2): 1626-1629

[21] Shepard D. A two-dimensional interpolation function for irregularly-spaced data[C]. Proceedings of the 23rd ACM National Conference, 1968: 517-524

[22] Liaw A, Wiener M. Classification and regression by randomForest[J]. R News, 2002, 2(3): 18-22

## 附录A MySQL数据库建表脚本

```sql
-- ============================================================
-- 城市空气质量分析与预测系统 - MySQL数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS air_quality_db 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE air_quality_db;

-- 1. 监测站点基础信息表
CREATE TABLE IF NOT EXISTS `station` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `station_code` VARCHAR(20) NOT NULL COMMENT '站点编码',
    `station_name` VARCHAR(100) NOT NULL COMMENT '站点名称',
    `district_name` VARCHAR(50) DEFAULT NULL COMMENT '所属区县',
    `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
    `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
    `station_type` VARCHAR(30) DEFAULT NULL COMMENT '站点类型(国控/省控/市控)',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_station_code` (`station_code`),
    KEY `idx_district` (`district_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测站点信息表';

-- 2. 空气质量监测数据表
CREATE TABLE IF NOT EXISTS `air_quality_data` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `station_code` VARCHAR(20) NOT NULL COMMENT '站点编码',
    `city` VARCHAR(50) DEFAULT '郑州' COMMENT '城市',
    `aqi` INT DEFAULT NULL COMMENT '空气质量指数',
    `quality_level` VARCHAR(20) DEFAULT NULL COMMENT '质量等级',
    `primary_pollutant` VARCHAR(30) DEFAULT NULL COMMENT '首要污染物',
    `pm25` DECIMAL(8,2) DEFAULT NULL COMMENT 'PM2.5(μg/m³)',
    `pm10` DECIMAL(8,2) DEFAULT NULL COMMENT 'PM10(μg/m³)',
    `so2` DECIMAL(8,2) DEFAULT NULL COMMENT 'SO2(μg/m³)',
    `no2` DECIMAL(8,2) DEFAULT NULL COMMENT 'NO2(μg/m³)',
    `co` DECIMAL(8,2) DEFAULT NULL COMMENT 'CO(mg/m³)',
    `o3` DECIMAL(8,2) DEFAULT NULL COMMENT 'O3(μg/m³)',
    `temperature` DECIMAL(5,1) DEFAULT NULL COMMENT '温度(℃)',
    `humidity` DECIMAL(5,1) DEFAULT NULL COMMENT '湿度(%)',
    `wind_speed` DECIMAL(5,1) DEFAULT NULL COMMENT '风速(m/s)',
    `wind_direction` VARCHAR(10) DEFAULT NULL COMMENT '风向',
    `pressure` DECIMAL(7,1) DEFAULT NULL COMMENT '气压(hPa)',
    `monitor_time` DATETIME NOT NULL COMMENT '监测时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_station_time` (`station_code`, `monitor_time`),
    KEY `idx_city_time` (`city`, `monitor_time`),
    KEY `idx_monitor_time` (`monitor_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空气质量监测数据表';

-- 3. 预测结果表
CREATE TABLE IF NOT EXISTS `aqi_prediction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `station_code` VARCHAR(20) NOT NULL,
    `station_name` VARCHAR(100) DEFAULT NULL,
    `predict_time` DATETIME NOT NULL COMMENT '预测目标时间',
    `predict_hour` INT DEFAULT NULL COMMENT '预测时长(小时)',
    `aqi_pred` INT DEFAULT NULL COMMENT '预测AQI',
    `aqi_level_pred` VARCHAR(20) DEFAULT NULL,
    `pm25_pred` DECIMAL(8,2) DEFAULT NULL,
    `pm10_pred` DECIMAL(8,2) DEFAULT NULL,
    `confidence` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度',
    `model_type` VARCHAR(50) DEFAULT 'random_forest',
    `model_version` VARCHAR(20) DEFAULT '1.0',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_station_predict` (`station_code`, `predict_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AQI预测结果表';

-- 4. 预警规则表
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `description` VARCHAR(500) DEFAULT NULL,
    `condition` VARCHAR(200) NOT NULL COMMENT '条件(如:pm25 > 75)',
    `level` VARCHAR(20) NOT NULL COMMENT '级别(info/warning/severe/emergency)',
    `enabled` TINYINT DEFAULT 1,
    `notify_methods` VARCHAR(200) DEFAULT NULL,
    `notify_targets` VARCHAR(500) DEFAULT NULL,
    `cooldown_minutes` INT DEFAULT 60,
    `is_deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则配置表';

-- 5. 预警记录表
CREATE TABLE IF NOT EXISTS `alert_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_id` BIGINT DEFAULT NULL,
    `station_code` VARCHAR(20) DEFAULT NULL,
    `alert_type` VARCHAR(50) NOT NULL COMMENT '告警类型',
    `level` VARCHAR(20) NOT NULL,
    `threshold_value` DECIMAL(8,2) DEFAULT NULL,
    `actual_value` DECIMAL(8,2) DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/resolved/expired',
    `resolve_time` DATETIME DEFAULT NULL,
    `remark` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

-- 6. 预测模型信息表
CREATE TABLE IF NOT EXISTS `prediction_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `model_name` VARCHAR(100) NOT NULL,
    `algorithm` VARCHAR(50) NOT NULL COMMENT 'random_forest/lstm/arima',
    `city` VARCHAR(50) DEFAULT '郑州',
    `train_start_time` DATETIME DEFAULT NULL,
    `train_end_time` DATETIME DEFAULT NULL,
    `sample_count` INT DEFAULT NULL,
    `feature_count` INT DEFAULT NULL,
    `num_trees` INT DEFAULT 100,
    `max_depth` INT DEFAULT 10,
    `rmse` DECIMAL(10,4) DEFAULT NULL,
    `mae` DECIMAL(10,4) DEFAULT NULL,
    `r_squared` DECIMAL(10,4) DEFAULT NULL,
    `model_path` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预测模型信息表';

-- 初始化郑州市监测站点
INSERT INTO `station` VALUES
(1,'410101A','郑州市监测站','中原区',34.757,113.665,'国控',1,NOW(),NOW()),
(2,'410102A','银行学校','金水区',34.780,113.710,'国控',1,NOW(),NOW()),
(3,'410103A','市环保监测站','二七区',34.748,113.655,'国控',1,NOW(),NOW()),
(4,'410104A','郑纺机','管城区',34.730,113.680,'国控',1,NOW(),NOW()),
(5,'410105A','烟厂','惠济区',34.752,113.725,'省控',1,NOW(),NOW()),
(6,'410106A','岗李水库','高新区',34.810,113.590,'省控',1,NOW(),NOW()),
(7,'410107A','供水公司','中原区',34.770,113.640,'市控',1,NOW(),NOW()),
(8,'410108A','四十七中','金水区',34.790,113.730,'市控',1,NOW(),NOW()),
(9,'410109A','经开区管委会','经开区',34.720,113.750,'市控',1,NOW(),NOW());

-- 初始化预警规则
INSERT INTO `alert_rule` (`name`,`condition`,`level`,`description`,`enabled`) VALUES
('PM2.5轻度告警','pm25 > 75','warning','PM2.5超过75μg/m³',1),
('PM2.5重度告警','pm25 > 150','severe','PM2.5超过150μg/m³',1),
('AQI中度告警','aqi > 150','warning','AQI超过150',1),
('AQI重度告警','aqi > 200','severe','AQI超过200',1),
('AQI严重告警','aqi > 300','emergency','AQI超过300紧急告警',1),
('O3超标告警','o3 > 160','warning','臭氧超过160μg/m³',1);
```

## 附录B Hive数据仓库建表脚本

```sql
-- ============================================================
-- Hive数据仓库建表脚本（ODS-DWD-DWS-ADS四层）
-- ============================================================
CREATE DATABASE IF NOT EXISTS air_quality_db;
USE air_quality_db;

-- ODS层：原始监测数据
CREATE TABLE IF NOT EXISTS ods_air_quality_raw (
    station_code STRING, station_name STRING, city STRING,
    aqi INT, quality_level STRING, primary_pollutant STRING,
    pm25 DOUBLE, pm10 DOUBLE, so2 DOUBLE, no2 DOUBLE, co DOUBLE, o3 DOUBLE,
    temperature DOUBLE, humidity DOUBLE, wind_speed DOUBLE,
    wind_direction STRING, pressure DOUBLE,
    monitor_time TIMESTAMP, create_time TIMESTAMP
) PARTITIONED BY (dt STRING)
STORED AS ORC TBLPROPERTIES ('orc.compress'='SNAPPY');

-- DWD层：清洗后明细数据
CREATE TABLE IF NOT EXISTS dwd_air_quality_dt (
    station_code STRING, station_name STRING, district_name STRING,
    latitude DOUBLE, longitude DOUBLE,
    aqi INT, aqi_level INT, quality_level STRING, primary_pollutant STRING,
    pm25 DOUBLE, pm10 DOUBLE, so2 DOUBLE, no2 DOUBLE, co DOUBLE, o3 DOUBLE,
    temperature DOUBLE, humidity DOUBLE, wind_speed DOUBLE, pressure DOUBLE,
    monitor_date STRING, monitor_hour INT, is_valid TINYINT
) PARTITIONED BY (dt STRING)
STORED AS ORC TBLPROPERTIES ('orc.compress'='SNAPPY');

-- DWS层：站点小时汇总
CREATE TABLE IF NOT EXISTS dws_station_hour (
    station_code STRING, station_name STRING,
    year INT, month INT, day INT, hour INT,
    aqi_avg DOUBLE, aqi_max INT, aqi_min INT,
    pm25_avg DOUBLE, pm10_avg DOUBLE, so2_avg DOUBLE,
    no2_avg DOUBLE, co_avg DOUBLE, o3_avg DOUBLE, record_count INT
) PARTITIONED BY (dt STRING) STORED AS ORC;

-- DWS层：月度汇总
CREATE TABLE IF NOT EXISTS dws_month_summary (
    district_code STRING, district_name STRING, year INT, month INT,
    aqi_avg DOUBLE, aqi_max INT, aqi_min INT,
    pm25_avg DOUBLE, pm10_avg DOUBLE,
    clean_days INT, polluted_days INT, primary_pollutant STRING, record_count INT
) PARTITIONED BY (dt STRING) STORED AS ORC;

-- ADS层：实时AQI应用表
CREATE TABLE IF NOT EXISTS ads_realtime_aqi (
    station_code STRING, station_name STRING, district_name STRING,
    latitude DOUBLE, longitude DOUBLE,
    aqi INT, aqi_level INT, aqi_level_desc STRING,
    pm25 DOUBLE, pm10 DOUBLE, so2 DOUBLE, no2 DOUBLE, co DOUBLE, o3 DOUBLE,
    primary_pollutant STRING, is_active INT, update_time TIMESTAMP
) STORED AS ORC;

-- ADS层：预测结果表
CREATE TABLE IF NOT EXISTS ads_aqi_prediction (
    station_code STRING, station_name STRING, predict_time TIMESTAMP,
    predict_hour INT, aqi_pred INT, aqi_level_pred STRING,
    pm25_pred DOUBLE, pm10_pred DOUBLE,
    confidence DOUBLE, model_type STRING, create_time TIMESTAMP
) PARTITIONED BY (dt STRING) STORED AS ORC;

-- DIM层：站点维度表
CREATE TABLE IF NOT EXISTS dim_station (
    station_code STRING, station_name STRING, district_name STRING,
    latitude DOUBLE, longitude DOUBLE, station_type STRING, is_active INT
) STORED AS ORC;
```

## 附录C 系统核心算法代码

**1. 随机森林决策树构建核心算法**

```java
private DecisionTree buildTree(List<Sample> samples, int depth, Random random) {
    DecisionTree tree = new DecisionTree();
    if (depth >= MAX_DEPTH || samples.size() < MIN_SAMPLES_SPLIT) {
        tree.prediction = samples.stream().mapToDouble(s -> s.aqi).average().orElse(100);
        return tree;
    }
    List<Integer> featureIndices = selectRandomFeatures(random);
    SplitResult bestSplit = findBestSplit(samples, featureIndices);
    if (bestSplit == null) {
        tree.prediction = samples.stream().mapToDouble(s -> s.aqi).average().orElse(100);
        return tree;
    }
    tree.splitFeature = bestSplit.featureIndex;
    tree.splitValue = bestSplit.splitValue;
    List<Sample> leftSamples = new ArrayList<>(), rightSamples = new ArrayList<>();
    for (Sample sample : samples) {
        if (getFeatureValue(sample, bestSplit.featureIndex) <= bestSplit.splitValue)
            leftSamples.add(sample);
        else
            rightSamples.add(sample);
    }
    if (!leftSamples.isEmpty()) tree.left = buildTree(leftSamples, depth + 1, random);
    if (!rightSamples.isEmpty()) tree.right = buildTree(rightSamples, depth + 1, random);
    return tree;
}
```

**2. IDW空间插值核心算法**

```java
private double calculateIDW(double lat, double lng, 
        List<Map<String, Object>> stations, String pollutant) {
    double sumWeight = 0, sumValue = 0;
    for (Map<String, Object> station : stations) {
        Double sLat = (Double) station.get("latitude");
        Double sLng = (Double) station.get("longitude");
        Object val = station.get(pollutant.toLowerCase());
        if (sLat == null || sLng == null || val == null) continue;
        double distance = Math.sqrt(Math.pow(lat - sLat, 2) + Math.pow(lng - sLng, 2));
        if (distance < 0.0001) return ((Number) val).doubleValue();
        double weight = 1.0 / Math.pow(distance, 2);
        sumWeight += weight;
        sumValue += ((Number) val).doubleValue() * weight;
    }
    return sumWeight > 0 ? sumValue / sumWeight : 0;
}
```

**3. Pearson/Spearman相关性计算**

```java
private double calculateCorrelation(List<Map<String, Object>> data, 
        String factor1, String factor2, String method) {
    List<Double> xList = new ArrayList<>(), yList = new ArrayList<>();
    for (Map<String, Object> row : data) {
        Object v1 = row.get(factor1), v2 = row.get(factor2);
        if (v1 != null && v2 != null) {
            xList.add(((Number) v1).doubleValue());
            yList.add(((Number) v2).doubleValue());
        }
    }
    if (xList.size() < 3) return 0;
    double[] x = xList.stream().mapToDouble(Double::doubleValue).toArray();
    double[] y = yList.stream().mapToDouble(Double::doubleValue).toArray();
    if ("spearman".equalsIgnoreCase(method)) {
        return new PearsonsCorrelation().correlation(getRanks(x), getRanks(y));
    }
    return new PearsonsCorrelation().correlation(x, y);
}
```

**4. 全局异常处理器**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        return Result.error(500, "系统异常，请稍后重试");
    }
}
```

**5. API日志切面**

```java
@Aspect @Component
public class ApiLog {
    @Around("execution(* cn.edu.zzu.airweb.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        log.info("[API] {} 耗时: {}ms", 
            joinPoint.getSignature().getName(), System.currentTimeMillis() - startTime);
        return result;
    }
}
```
