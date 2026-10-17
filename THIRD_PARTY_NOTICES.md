# 参考源码说明

参考项目：[1sdv/TripStar](https://github.com/1sdv/TripStar)，本次读取版本 `427261fe0038d9f2d568fe378e50629ea4eb085b`。

本项目的 API 字段、行程结构与记忆管理逻辑参考上述项目。
`utils/MemoryPolicy.java`、`service/impl/MemoryServiceImpl.java`、`service/impl/PreferenceExtractorImpl.java` 是对其
`backend/app/memory/memory_manager.py`、`data_model.py`、`preference_extractor.py` 的 Java 迁移与调整，
存储改为 MySQL/MyBatis，增加独立衰减时间和并发控制，不引入 HelloAgents 依赖。

上游仓库随附 GNU GPL version 2 许可；原始许可副本保存在
[LICENSES/TripStar-GPL-2.0.txt](LICENSES/TripStar-GPL-2.0.txt)。迁移部分保留上游许可要求。
