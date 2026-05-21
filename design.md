# Feature Flag 功能开关管理服务 — 设计思路

## 1. 核心问题与设计目标

### 问题定义
Feature Flag（功能开关）允许工程团队在不重新部署代码的情况下，动态地控制功能的开启或关闭。核心需求：

- **是否启用？**（enabled）
- **对谁启用？**（subject / region / subjectGroup）
- **关联哪个版本？**（release）
- **灰度比例？**（rolloutPercentage）

### 设计目标
1. **高并发低延迟**：评估请求是热路径，P99 < 5ms
2. **高可用**：控制面故障不影响数据面，SDK 本地缓存兜底
3. **强一致性快照**：Immutable Snapshot 保证同一版本内的评估行为一致
4. **跨语言可复现**：哈希分桶算法基于 SHA-256，结果与语言无关
5. **可解释性**：每次评估返回 reasonCode + matchedConditions，支持溯源

---

## 2. 两平面分离架构

```
控制面 (Control Plane)          数据面 (Data Plane)
─────────────────────          ──────────────────
web-admin (Vue 3)              frontend-sdk (TS)
    ↓ REST API                 java-sdk (Java)
backend (Spring Boot)              ↓ HTTP GET
    ↓ Publish                  Evaluation API
Snapshot DB                        ↓
    ↓                          SnapshotCache
                                   ↓
                               EvaluationEngine
```

**控制面** 负责：创建 Flag、配置规则条件、环境升级审批、发布 Snapshot。
**数据面** 负责：加载最新 Snapshot、执行评估、返回 enabled/disabled + 原因码。

---

## 3. 不可变快照模型

每次 Publish 生成一份 `Snapshot`，包含该 appKey+environment 下所有 Flag 的完整配置：

```json
{
  "appKey": "vue-demo",
  "environment": "prod",
  "version": 42,
  "checksum": "sha256:...",
  "flags": [
    {
      "flagKey": "new-checkout",
      "type": "boolean",
      "enabled": true,
      "releaseKey": "20260520",
      "rules": [
        {
          "ruleId": "1",
          "priority": 1,
          "conditionJson": {"region": ["Asia"], "subject": ["vip"]},
          "rolloutPercentage": 80
        }
      ]
    }
  ]
}
```

**优点**：
- 评估时不查 DB，只读内存缓存
- Checksum 校验保证完整性
- 版本号单调递增，SDK 可做 ETag 条件请求

---

## 4. 评估引擎设计

### 4.1 评估决策树

```
evaluate(snapshot, flagKey, context)
    │
    ├─ [FLAG_NOT_FOUND] flag 不存在
    ├─ [FLAG_DISABLED]  flag.enabled = false（kill switch）
    │
    └─ 遍历 rules（按 priority 升序）
           │
           ├─ 条件不匹配 → 继续下一条规则
           ├─ 条件匹配 + bucket < rolloutPercentage → [RULE_MATCH] enabled=true
           └─ 条件匹配 + bucket ≥ rolloutPercentage → [ROLLOUT_NOT_INCLUDED] enabled=false
           │
           └─ 所有规则不匹配 → [DEFAULT_VALUE] enabled=false
```

### 4.2 跨语言哈希分桶

```
bucket = SHA-256(flagKey + ":" + subjectKey)[0..3] as uint32 mod 100
```

- 使用 SHA-256 前 4 字节转 Big-endian uint32
- 对 100 取模得到 0-99 的桶号
- Java、TypeScript、Go、Python 实现结果完全一致

### 4.3 条件匹配

`conditionJson` 是 `Map<String, Object>`，支持单值和列表：
- `region: "Asia"` 或 `region: ["Asia", "Europe"]`
- `subject: "vip"` 或 `subject: ["vip", "beta"]`
- 空值/null 视为"不限制"（总是匹配）

---

## 5. 多级缓存策略

```
请求链路（由近到远）：

L1 SDK内存缓存 (TTL: 30s)
    ↓ miss
L2 CDN缓存 (TTL: 60s，ETag条件请求)
    ↓ miss
L3 SnapshotCache (ConcurrentHashMap, JVM内存)
    ↓ miss
L4 Redis 分布式缓存 (TTL: 5min)
    ↓ miss
L5 数据库 (Oracle/H2)
```

**Write-Through**：Publish 操作同时写 DB + 刷新 SnapshotCache + 推送 CDN 失效。
**降级策略**：SDK 本地缓存在服务不可用时继续使用上一个有效快照（Stale-if-Error）。

---

## 6. 数据模型（6张表）

| 表名 | 用途 |
|------|------|
| ff_application | 接入应用注册 |
| ff_flag | Flag 定义（全局，跨环境共享） |
| ff_flag_config | Flag 在特定 app+environment 下的配置 |
| ff_rule | 匹配规则（conditionJson + rolloutPercentage） |
| ff_config_snapshot | 已发布的不可变快照 |
| ff_change_event | 审计日志（所有变更记录） |

---

## 7. 环境升级流程（渐进式发布）

```
local → dev → sit → uat → prod
 ↑       ↑     ↑     ↑     ↑
 直接    直接  直接  直接  需Approve
 Publish Publish Publish Publish 后Publish
```

- **非 prod**：Save Config → Publish（直接）
- **prod**：Save Config → Approve → Publish（两步）
- **Rollout 默认值**：非 prod = 100%，prod = 10%（安全默认）
- **Kill Switch**：setRollout(0) 或 setEnabled(false)

---

## 8. 高可用与灾备设计

### 8.1 单 Region 高可用

```
Internet → 防火墙 → CDN → API Gateway (多实例)
                              ↓
                         K8S 集群
                    ┌─────────────────┐
                    │  Control Plane  │  (web-admin + backend, 2 replicas)
                    │  Data Plane     │  (evaluation-service, HPA 3-10)
                    └─────────────────┘
                              ↓
                    ┌─────────────────┐
                    │ Redis Cluster   │  (3主3从)
                    │ DB Primary      │  (同步复制)
                    │ DB Replica ×2   │  (读分离)
                    └─────────────────┘
```

### 8.2 多地部署（双活 + 灾备）

```
主数据中心 (Active)          灾备中心 (Standby/Active)
─────────────────            ──────────────────────
K8S 集群 A                  K8S 集群 B
DB Primary ──同步复制──→     DB Replica (可提升为Primary)
Redis Cluster A              Redis Cluster B (异步复制)
CDN 节点 (就近回源)          CDN 节点 (就近回源)
        ↑                            ↑
        └──── Global Load Balancer ───┘
                    ↑
               GeoDNS / GSLB
```

**RTO** < 30秒（自动切换），**RPO** < 5秒（同步复制）

---

## 9. K8S 部署规格

| 组件 | Replicas | HPA | Resources |
|------|----------|-----|-----------|
| backend (control) | 2 | — | 1C/1G |
| evaluation-service | 3-10 | CPU>60% | 0.5C/512M |
| web-admin (nginx) | 2 | — | 0.2C/256M |
| redis | 3M+3S | — | 2C/4G |

**ConfigMap**：环境变量、数据库连接  
**Secret**：DB密码、JWT密钥  
**PodDisruptionBudget**：保证滚动更新时至少1个实例可用

---

## 10. 监控与可观测性

### 三大支柱
- **Metrics**：Prometheus + Grafana，关键指标：evaluation QPS、cache hit rate、snapshot version lag
- **Logs**：ELK Stack，结构化日志，TraceId 关联
- **Traces**：Jaeger/Zipkin，分布式链路追踪

### 关键告警
- Evaluation 错误率 > 0.1%
- Cache miss rate > 20%
- Snapshot 版本落后 > 5min
- DB 主从延迟 > 100ms

---

## 11. 安全设计

- **管理端**：OAuth2/OIDC 身份认证，RBAC 权限控制
- **SDK**：AppKey + 签名认证，限速（Rate Limiting）
- **数据**：传输层 TLS 1.3，敏感字段加密存储
- **审计**：所有变更写入 ff_change_event，不可删除

---

## 12. 后续演进方向

1. **Webhook 推送**：Publish 后主动推送 SDK，降低拉取延迟
2. **A/B 测试集成**：在 rollout 分桶基础上增加实验分组
3. **依赖追踪**：自动分析哪些服务依赖了特定 Flag
4. **自动清理**：Flag 长期未使用自动归档，防止配置膨胀
5. **TypeScript SDK 完善**：ETag 条件请求、本地持久化
