-- ============================================================================
-- NEW-045 汽车维修工单与配件领用管理系统 · 数据库初始化脚本
-- MySQL 8 / utf8mb4；执行：mysql -uroot -p123456 < sql/init.sql
-- 生成器：gen/make_sql.py（勿手改本文件，改生成器后重跑）
-- ============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS new_045;
CREATE DATABASE new_045 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE new_045;

-- ============================================================================
-- NEW-045 汽车维修工单与配件领用管理系统 · 业务表结构
-- 字符集 utf8mb4；主数据带逻辑删除/版本号/审计字段；过程单据带状态机字段
-- ============================================================================

-- 1 客户
CREATE TABLE ar_customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  customer_no VARCHAR(30) NOT NULL COMMENT '客户编号',
  name VARCHAR(50) NOT NULL COMMENT '客户姓名',
  phone VARCHAR(20) NOT NULL COMMENT '手机号',
  customer_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '客户类型 MEMBER/NORMAL',
  source VARCHAR(40) DEFAULT NULL COMMENT '客户来源',
  visit_count INT NOT NULL DEFAULT 0 COMMENT '进厂次数',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计消费',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_by VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  UNIQUE KEY uk_customer_no (customer_no),
  KEY idx_customer_phone (phone)
) ENGINE=InnoDB COMMENT='客户档案';

-- 2 维修车辆（系统核心主数据）
CREATE TABLE ar_vehicle (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  plate_no VARCHAR(50) NOT NULL COMMENT '车牌号',
  customer_id BIGINT NOT NULL COMMENT '所属客户',
  brand VARCHAR(40) NOT NULL COMMENT '车辆品牌',
  model VARCHAR(60) NOT NULL COMMENT '车型',
  vin VARCHAR(40) NOT NULL COMMENT '车架号',
  engine_no VARCHAR(40) DEFAULT NULL COMMENT '发动机号',
  mileage INT NOT NULL DEFAULT 0 COMMENT '当前里程 km',
  buy_date DATE DEFAULT NULL COMMENT '购车日期',
  vehicle_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '车辆状态 NORMAL/IN_REPAIR/WAIT_SETTLE',
  photo VARCHAR(200) DEFAULT NULL COMMENT '车辆照片',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_by VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  UNIQUE KEY uk_plate_no (plate_no),
  KEY idx_vehicle_customer (customer_id),
  KEY idx_vehicle_vin (vin)
) ENGINE=InnoDB COMMENT='维修车辆主档';

-- 3 预约
CREATE TABLE ar_appointment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  appoint_no VARCHAR(30) NOT NULL COMMENT '预约号',
  customer_id BIGINT NOT NULL COMMENT '客户',
  vehicle_id BIGINT NOT NULL COMMENT '车辆',
  appoint_type VARCHAR(20) NOT NULL COMMENT '预约类型 MAINTAIN/REPAIR/CHECK/SHEET_METAL',
  expect_time DATETIME NOT NULL COMMENT '期望时间',
  problem_type VARCHAR(60) DEFAULT NULL COMMENT '问题类型',
  status VARCHAR(20) NOT NULL DEFAULT 'WAIT_RECEIVE' COMMENT 'WAIT_RECEIVE/RECEIVED/CANCELED/MISSED',
  advisor_id BIGINT DEFAULT NULL COMMENT '服务顾问',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_appoint_no (appoint_no),
  KEY idx_appoint_time (expect_time),
  KEY idx_appoint_status (status)
) ENGINE=InnoDB COMMENT='维修预约';

-- 4 接车检查
CREATE TABLE ar_reception (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  mileage INT NOT NULL COMMENT '进厂里程',
  fuel_level VARCHAR(20) DEFAULT NULL COMMENT '油量',
  scratch_desc VARCHAR(300) DEFAULT NULL COMMENT '外观划痕',
  items_desc VARCHAR(300) DEFAULT NULL COMMENT '随车物品',
  photos VARCHAR(500) DEFAULT NULL COMMENT '现场照片，逗号分隔相对路径',
  receive_time DATETIME NOT NULL COMMENT '接车时间',
  advisor_id BIGINT DEFAULT NULL COMMENT '服务顾问',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_reception_order (order_id)
) ENGINE=InnoDB COMMENT='接车检查记录';

-- 5 维修工单（核心过程单据）
CREATE TABLE ar_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_no VARCHAR(30) NOT NULL COMMENT '工单号（后端生成）',
  vehicle_id BIGINT NOT NULL COMMENT '车辆',
  customer_id BIGINT NOT NULL COMMENT '客户',
  order_type VARCHAR(20) NOT NULL COMMENT '工单类型',
  status VARCHAR(30) NOT NULL DEFAULT 'RECEIVE' COMMENT '接车登记/故障诊断/报价确认/维修领料/质量检验/交车结算 + REWORK/CANCELED',
  advisor_id BIGINT DEFAULT NULL COMMENT '服务顾问',
  tech_id BIGINT DEFAULT NULL COMMENT '主责技师',
  receive_time DATETIME NOT NULL COMMENT '接车时间',
  expect_finish DATETIME DEFAULT NULL COMMENT '预计完工',
  finish_time DATETIME DEFAULT NULL COMMENT '实际完工',
  fault_desc VARCHAR(100) DEFAULT NULL COMMENT '故障描述（≤100 字）',
  urgency VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT '紧急程度',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应收金额',
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  discount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  rework_times INT NOT NULL DEFAULT 0 COMMENT '返工次数',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_by VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_status (status),
  KEY idx_order_vehicle (vehicle_id),
  KEY idx_order_time (create_time)
) ENGINE=InnoDB COMMENT='汽车维修工单';

-- 6 检测记录
CREATE TABLE ar_inspection (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  check_item VARCHAR(60) NOT NULL COMMENT '检查项',
  check_result VARCHAR(20) NOT NULL COMMENT 'NORMAL/OBSERVE/REPAIR',
  fault_desc VARCHAR(200) DEFAULT NULL COMMENT '故障描述',
  severity VARCHAR(10) DEFAULT NULL COMMENT 'HIGH/MID/LOW',
  suggestion VARCHAR(200) DEFAULT NULL COMMENT '建议',
  tech_id BIGINT DEFAULT NULL COMMENT '技师',
  check_time DATETIME NOT NULL COMMENT '检测时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_inspection_order (order_id)
) ENGINE=InnoDB COMMENT='故障检测记录';

-- 7 标准维修项目库
CREATE TABLE ar_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  project_code VARCHAR(20) NOT NULL COMMENT '项目编码',
  project_name VARCHAR(60) NOT NULL COMMENT '项目名称',
  work_type VARCHAR(20) NOT NULL COMMENT '工种 MECHANIC/ELECTRIC/SHEET_METAL/PAINT',
  std_hours DECIMAL(6,2) NOT NULL COMMENT '标准工时',
  labor_fee DECIMAL(10,2) NOT NULL COMMENT '工时费',
  apply_models VARCHAR(100) DEFAULT NULL COMMENT '适用车型',
  common_parts VARCHAR(200) DEFAULT NULL COMMENT '常用配件',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_project_code (project_code)
) ENGINE=InnoDB COMMENT='维修项目库';

-- 8 工单项目
CREATE TABLE ar_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  project_id BIGINT DEFAULT NULL COMMENT '项目库',
  project_name VARCHAR(60) NOT NULL COMMENT '项目名称',
  work_type VARCHAR(20) DEFAULT NULL COMMENT '工种',
  hours DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '工时',
  labor_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '工时费',
  tech_id BIGINT DEFAULT NULL COMMENT '技师',
  item_status VARCHAR(20) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/WORKING/DONE/REJECTED/REWORK',
  confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '客户是否确认 1/0',
  quote_id BIGINT DEFAULT NULL COMMENT '所属报价版本',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_item_order (order_id)
) ENGINE=InnoDB COMMENT='工单维修项目';

-- 9 报价单（版本化，已确认版本不可覆盖）
CREATE TABLE ar_quote (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  quote_no VARCHAR(30) NOT NULL COMMENT '报价单号',
  order_id BIGINT NOT NULL COMMENT '工单',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  total_labor DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '工时费合计',
  total_part DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '配件费合计',
  discount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '优惠',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '预计总额',
  expect_days INT NOT NULL DEFAULT 1 COMMENT '预计工期（天）',
  status VARCHAR(20) NOT NULL DEFAULT 'WAIT_CONFIRM' COMMENT 'WAIT_CONFIRM/CONFIRMED/REJECTED/EXPIRED',
  create_by VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_quote_no (quote_no),
  KEY idx_quote_order (order_id, version_no)
) ENGINE=InnoDB COMMENT='报价单';

-- 10 报价明细
CREATE TABLE ar_quote_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  quote_id BIGINT NOT NULL COMMENT '报价单',
  item_type VARCHAR(10) NOT NULL COMMENT 'LABOR/PART',
  ref_id BIGINT DEFAULT NULL COMMENT '对应工单项目或配件',
  item_name VARCHAR(80) NOT NULL COMMENT '名称',
  qty DECIMAL(8,2) NOT NULL DEFAULT 1 COMMENT '数量',
  unit VARCHAR(10) DEFAULT NULL COMMENT '单位',
  unit_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '小计',
  confirm_status VARCHAR(20) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/AGREE/REJECT',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_qitem_quote (quote_id)
) ENGINE=InnoDB COMMENT='报价明细';

-- 11 客户确认
CREATE TABLE ar_confirm (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  quote_id BIGINT NOT NULL COMMENT '报价单',
  order_id BIGINT NOT NULL COMMENT '工单',
  version_no INT NOT NULL COMMENT '报价版本',
  confirm_type VARCHAR(20) NOT NULL COMMENT 'PHONE/SMS/ONSITE/APP',
  confirm_result VARCHAR(20) NOT NULL COMMENT 'AGREE/PART/REJECT',
  confirm_time DATETIME NOT NULL COMMENT '确认时间',
  confirm_by VARCHAR(50) DEFAULT NULL COMMENT '确认人（客户或代登记人）',
  remark VARCHAR(300) DEFAULT NULL COMMENT '说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_confirm_order (order_id)
) ENGINE=InnoDB COMMENT='客户报价确认';

-- 12 派工
CREATE TABLE ar_dispatch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  item_id BIGINT NOT NULL COMMENT '工单项目',
  tech_id BIGINT NOT NULL COMMENT '技师',
  work_type VARCHAR(20) DEFAULT NULL COMMENT '工种',
  plan_hours DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '计划工时',
  actual_hours DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '实际工时',
  start_time DATETIME DEFAULT NULL COMMENT '开始时间',
  pause_time DATETIME DEFAULT NULL COMMENT '暂停时间',
  end_time DATETIME DEFAULT NULL COMMENT '完成时间',
  status VARCHAR(20) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/WORKING/PAUSED/DONE',
  remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_dispatch_order (order_id),
  KEY idx_dispatch_tech (tech_id)
) ENGINE=InnoDB COMMENT='派工单';

-- 13 工时记录
CREATE TABLE ar_worklog (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  dispatch_id BIGINT NOT NULL COMMENT '派工单',
  order_id BIGINT NOT NULL COMMENT '工单',
  tech_id BIGINT NOT NULL COMMENT '技师',
  action VARCHAR(20) NOT NULL COMMENT 'START/PAUSE/RESUME/FINISH',
  log_time DATETIME NOT NULL COMMENT '操作时间',
  hours DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '本次工时',
  remark VARCHAR(200) DEFAULT NULL COMMENT '维修说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_worklog_dispatch (dispatch_id)
) ENGINE=InnoDB COMMENT='维修工时记录';

-- 14 配件档案
CREATE TABLE ar_part (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  part_code VARCHAR(20) NOT NULL COMMENT '配件编码',
  part_name VARCHAR(60) NOT NULL COMMENT '配件名称',
  apply_models VARCHAR(100) DEFAULT NULL COMMENT '适用车型',
  unit VARCHAR(10) NOT NULL DEFAULT '个' COMMENT '单位',
  purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '采购价',
  sale_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '销售价',
  stock_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当前库存',
  min_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存下限',
  max_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存上限',
  image VARCHAR(200) DEFAULT NULL COMMENT '配件图片',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_part_code (part_code)
) ENGINE=InnoDB COMMENT='配件档案';

-- 15 库存批次
CREATE TABLE ar_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  part_id BIGINT NOT NULL COMMENT '配件',
  batch_no VARCHAR(30) NOT NULL COMMENT '批次号',
  warehouse VARCHAR(40) NOT NULL DEFAULT '主仓' COMMENT '仓库',
  in_time DATETIME NOT NULL COMMENT '入库时间',
  in_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '入库数量',
  left_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '剩余数量',
  purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '采购价',
  supplier VARCHAR(60) DEFAULT NULL COMMENT '供应商',
  expire_date DATE DEFAULT NULL COMMENT '有效期',
  status VARCHAR(20) NOT NULL DEFAULT 'IN_STOCK' COMMENT 'IN_STOCK/LOW/EXPIRE/OUT',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_batch_no (batch_no),
  KEY idx_batch_part (part_id)
) ENGINE=InnoDB COMMENT='库存批次';

-- 16 配件需求（由预检或人工产生）
CREATE TABLE ar_demand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  part_id BIGINT NOT NULL COMMENT '配件',
  demand_qty DECIMAL(10,2) NOT NULL COMMENT '需求数量',
  stock_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '计算时库存',
  gap_qty DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '缺口数量',
  source VARCHAR(20) NOT NULL DEFAULT 'PRECHECK' COMMENT 'PRECHECK/MANUAL',
  score_id BIGINT DEFAULT NULL COMMENT '预检计算快照',
  status VARCHAR(20) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/RESERVED/CLAIMED/CANCELED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_demand_order (order_id)
) ENGINE=InnoDB COMMENT='配件需求';

-- 17 领退记录
CREATE TABLE ar_part_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  flow_no VARCHAR(30) NOT NULL COMMENT '领用单号',
  order_id BIGINT NOT NULL COMMENT '关联工单（必填）',
  part_id BIGINT NOT NULL COMMENT '配件',
  batch_id BIGINT DEFAULT NULL COMMENT '批次',
  qty DECIMAL(10,2) NOT NULL COMMENT '数量（正数）',
  flow_type VARCHAR(20) NOT NULL COMMENT 'CLAIM/EXTRA/RETURN',
  tech_id BIGINT DEFAULT NULL COMMENT '领用技师',
  old_part_handle VARCHAR(40) DEFAULT NULL COMMENT '旧件处理方式',
  operator_id BIGINT DEFAULT NULL COMMENT '仓库操作人',
  flow_time DATETIME NOT NULL COMMENT '业务时间',
  status VARCHAR(20) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/ISSUED/REJECTED/RETURNED',
  remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_flow_order (order_id),
  KEY idx_flow_part (part_id)
) ENGINE=InnoDB COMMENT='配件领退记录';

-- 18 质检
CREATE TABLE ar_qc (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  item_id BIGINT DEFAULT NULL COMMENT '工单项目',
  qc_result VARCHAR(20) NOT NULL COMMENT 'PASS/FAIL',
  qc_desc VARCHAR(300) DEFAULT NULL COMMENT '检验说明',
  inspector_id BIGINT DEFAULT NULL COMMENT '质检人员',
  qc_time DATETIME NOT NULL COMMENT '检验时间',
  status VARCHAR(20) NOT NULL DEFAULT 'DONE' COMMENT 'DONE/REWORK',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_qc_order (order_id)
) ENGINE=InnoDB COMMENT='质量检验记录';

-- 19 返工记录
CREATE TABLE ar_rework_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  qc_id BIGINT DEFAULT NULL COMMENT '质检记录',
  item_id BIGINT DEFAULT NULL COMMENT '工单项目',
  tech_id BIGINT DEFAULT NULL COMMENT '责任技师',
  reason VARCHAR(300) NOT NULL COMMENT '返工原因',
  times INT NOT NULL DEFAULT 1 COMMENT '第几次返工',
  recheck_result VARCHAR(20) DEFAULT NULL COMMENT '复检结果',
  recheck_time DATETIME DEFAULT NULL COMMENT '复检时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_rework_order (order_id)
) ENGINE=InnoDB COMMENT='返工记录';

-- 20 费用明细
CREATE TABLE ar_fee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  fee_type VARCHAR(20) NOT NULL COMMENT 'LABOR/PART/EXTRA/DISCOUNT',
  fee_desc VARCHAR(120) NOT NULL COMMENT '费用说明',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '金额',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_fee_order (order_id)
) ENGINE=InnoDB COMMENT='费用明细';

-- 21 收款记录（线下收款，不接支付接口）
CREATE TABLE ar_payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  pay_type VARCHAR(20) NOT NULL COMMENT 'CASH/WECHAT/ALIPAY/CARD',
  amount DECIMAL(10,2) NOT NULL COMMENT '收款金额',
  pay_time DATETIME NOT NULL COMMENT '收款时间',
  operator_id BIGINT DEFAULT NULL COMMENT '收款人',
  remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_payment_order (order_id)
) ENGINE=InnoDB COMMENT='收款记录';

-- 22 交车记录
CREATE TABLE ar_delivery (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  mileage INT NOT NULL COMMENT '交车里程',
  accept_result VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED' COMMENT '客户验收结果',
  old_part_taken VARCHAR(20) DEFAULT NULL COMMENT '旧件是否领取',
  voucher VARCHAR(200) DEFAULT NULL COMMENT '交车凭证相对路径',
  delivery_time DATETIME NOT NULL COMMENT '交车时间',
  operator_id BIGINT DEFAULT NULL COMMENT '经办人',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_delivery_order (order_id)
) ENGINE=InnoDB COMMENT='交车记录';

-- 23 质保台账
CREATE TABLE ar_warranty (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  warranty_no VARCHAR(30) NOT NULL COMMENT '质保编号',
  order_id BIGINT NOT NULL COMMENT '原工单',
  item_id BIGINT DEFAULT NULL COMMENT '原工单项目',
  vehicle_id BIGINT NOT NULL COMMENT '车辆',
  project_name VARCHAR(60) NOT NULL COMMENT '维修项目',
  warranty_days INT NOT NULL DEFAULT 180 COMMENT '质保天数',
  warranty_km INT NOT NULL DEFAULT 10000 COMMENT '质保里程',
  start_date DATE NOT NULL COMMENT '起算日期',
  end_date DATE NOT NULL COMMENT '到期日期',
  status VARCHAR(20) NOT NULL DEFAULT 'VALID' COMMENT 'VALID/EXPIRE/USED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_warranty_no (warranty_no),
  KEY idx_warranty_vehicle (vehicle_id)
) ENGINE=InnoDB COMMENT='质保台账';

-- 24 返修单
CREATE TABLE ar_return (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  return_no VARCHAR(30) NOT NULL COMMENT '返修单号',
  order_id BIGINT NOT NULL COMMENT '原工单',
  warranty_id BIGINT DEFAULT NULL COMMENT '关联质保',
  customer_id BIGINT NOT NULL COMMENT '客户',
  vehicle_id BIGINT NOT NULL COMMENT '车辆',
  item_name VARCHAR(60) NOT NULL COMMENT '原维修项目',
  problem_desc VARCHAR(300) NOT NULL COMMENT '问题描述',
  duty_result VARCHAR(20) DEFAULT NULL COMMENT 'STORE/PART/CUSTOMER/WAIT',
  status VARCHAR(20) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT/REPAIRING/DONE/CANCELED',
  rework_times INT NOT NULL DEFAULT 1 COMMENT '返修次数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_return_no (return_no),
  KEY idx_return_order (order_id)
) ENGINE=InnoDB COMMENT='返修单';

-- 25 评价
CREATE TABLE ar_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '工单',
  customer_id BIGINT NOT NULL COMMENT '客户',
  score INT NOT NULL DEFAULT 5 COMMENT '评分 1-5',
  content VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  reply VARCHAR(500) DEFAULT NULL COMMENT '门店回复',
  review_time DATETIME NOT NULL COMMENT '评价时间',
  reply_time DATETIME DEFAULT NULL COMMENT '回复时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_review_order (order_id)
) ENGINE=InnoDB COMMENT='服务评价';

-- 26 附件（图片/凭证，不存外链）
CREATE TABLE ar_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  biz_type VARCHAR(40) NOT NULL COMMENT '业务类型 vehicle/reception/inspection/voucher/avatar/part',
  biz_id BIGINT DEFAULT NULL COMMENT '业务主键',
  file_name VARCHAR(160) NOT NULL COMMENT '原始文件名',
  file_path VARCHAR(200) NOT NULL COMMENT '相对路径',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT '大小（字节）',
  upload_by VARCHAR(50) DEFAULT NULL COMMENT '上传人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_att_biz (biz_type, biz_id)
) ENGINE=InnoDB COMMENT='附件元数据';

-- 27 预检规则（创新点：故障项目与配件需求预检）
CREATE TABLE ar_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  rule_version VARCHAR(20) NOT NULL COMMENT '版本号',
  w_model DECIMAL(5,2) NOT NULL COMMENT '车型权重',
  w_fault DECIMAL(5,2) NOT NULL COMMENT '故障标签权重',
  w_project DECIMAL(5,2) NOT NULL COMMENT '诊断项目权重',
  w_hours DECIMAL(5,2) NOT NULL COMMENT '历史工时权重',
  w_part DECIMAL(5,2) NOT NULL COMMENT '常用配件权重',
  w_stock DECIMAL(5,2) NOT NULL COMMENT '当前库存权重',
  threshold_high DECIMAL(5,2) NOT NULL DEFAULT 80 COMMENT '推荐阈值',
  threshold_low DECIMAL(5,2) NOT NULL DEFAULT 60 COMMENT '不推荐阈值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 生效 0 历史',
  effect_time DATETIME NOT NULL COMMENT '生效时间',
  create_by VARCHAR(50) DEFAULT NULL COMMENT '修改人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_rule_version (rule_version)
) ENGINE=InnoDB COMMENT='预检规则配置';

-- 28 预检计算留痕
CREATE TABLE ar_score (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  score_no VARCHAR(30) NOT NULL COMMENT '快照编号',
  order_id BIGINT NOT NULL COMMENT '工单',
  rule_id BIGINT NOT NULL COMMENT '规则',
  rule_version VARCHAR(20) NOT NULL COMMENT '规则版本',
  input_snapshot VARCHAR(800) NOT NULL COMMENT '输入快照 JSON',
  score_model DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '车型得分',
  score_fault DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '故障标签得分',
  score_project DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '诊断项目得分',
  score_hours DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '历史工时得分',
  score_part DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '常用配件得分',
  score_stock DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '当前库存得分',
  total_score DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '总分',
  result_level VARCHAR(20) NOT NULL COMMENT 'RECOMMEND/CAUTION/REJECT',
  explain_text VARCHAR(600) DEFAULT NULL COMMENT '结果解释',
  miss_text VARCHAR(400) DEFAULT NULL COMMENT '未满足条件',
  improve_text VARCHAR(400) DEFAULT NULL COMMENT '可改进项',
  manual_result VARCHAR(20) DEFAULT NULL COMMENT '人工修正结果',
  manual_reason VARCHAR(300) DEFAULT NULL COMMENT '修正原因（≥5 字）',
  manual_by VARCHAR(50) DEFAULT NULL COMMENT '修正人',
  manual_time DATETIME DEFAULT NULL COMMENT '修正时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  UNIQUE KEY uk_score_no (score_no),
  KEY idx_score_order (order_id)
) ENGINE=InnoDB COMMENT='预检计算留痕';

-- 29 公告
CREATE TABLE ar_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  title VARCHAR(120) NOT NULL COMMENT '标题',
  content VARCHAR(1000) DEFAULT NULL COMMENT '内容',
  publisher VARCHAR(50) DEFAULT NULL COMMENT '发布人',
  publish_time DATETIME NOT NULL COMMENT '发布时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 发布 0 下线',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='门店公告';


-- ===== 系统表 =====
CREATE TABLE sys_org (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  org_name VARCHAR(64) NOT NULL COMMENT '门店名称',
  org_code VARCHAR(32) NOT NULL COMMENT '门店编码',
  city VARCHAR(32) DEFAULT NULL COMMENT '城市',
  address VARCHAR(200) DEFAULT NULL COMMENT '地址',
  phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  open_time VARCHAR(16) DEFAULT NULL COMMENT '营业开始',
  close_time VARCHAR(16) DEFAULT NULL COMMENT '营业结束',
  traffic VARCHAR(300) DEFAULT NULL COMMENT '交通指引',
  notice VARCHAR(500) DEFAULT NULL COMMENT '服务须知',
  cover VARCHAR(200) DEFAULT NULL COMMENT '门店图片',
  manager VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '1 营业 0 停业',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_org_code (org_code)
) ENGINE=InnoDB COMMENT='门店/组织';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_code VARCHAR(40) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(60) NOT NULL COMMENT '角色名称',
  data_scope VARCHAR(20) DEFAULT NULL COMMENT 'all/store/self',
  remark VARCHAR(300) DEFAULT NULL COMMENT '说明',
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色';

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  real_name VARCHAR(50) NOT NULL COMMENT '姓名',
  phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  email VARCHAR(80) DEFAULT NULL COMMENT '邮箱',
  avatar VARCHAR(200) DEFAULT NULL COMMENT '头像相对路径',
  org_id BIGINT DEFAULT NULL COMMENT '所属门店',
  org_name VARCHAR(64) NOT NULL COMMENT '门店名称',
  gender VARCHAR(4) DEFAULT NULL COMMENT '性别',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '1 正常 0 停用',
  login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续失败次数',
  lock_until DATETIME DEFAULT NULL COMMENT '锁定截止',
  last_login DATETIME DEFAULT NULL COMMENT '最近登录',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_username (username),
  KEY idx_user_org (org_id)
) ENGINE=InnoDB COMMENT='系统用户';

CREATE TABLE sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT DEFAULT NULL COMMENT '角色',
  menu_id BIGINT DEFAULT NULL COMMENT '菜单',
  KEY idx_rm_role (role_id)
) ENGINE=InnoDB COMMENT='角色菜单';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT DEFAULT NULL COMMENT '用户',
  role_id BIGINT DEFAULT NULL COMMENT '角色',
  KEY idx_ur_user (user_id)
) ENGINE=InnoDB COMMENT='用户角色';

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  parent_id BIGINT DEFAULT NULL COMMENT '父菜单',
  menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
  path VARCHAR(120) DEFAULT NULL COMMENT '前端路由',
  icon VARCHAR(40) DEFAULT NULL COMMENT '图标',
  menu_type VARCHAR(20) DEFAULT NULL COMMENT 'catalog/menu/button',
  perm VARCHAR(80) NOT NULL COMMENT '权限标识',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  visible TINYINT NOT NULL DEFAULT 0 COMMENT '是否显示'
) ENGINE=InnoDB COMMENT='菜单';

CREATE TABLE sys_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  dict_type VARCHAR(50) NOT NULL COMMENT '字典类型',
  dict_label VARCHAR(50) NOT NULL COMMENT '显示值',
  dict_value VARCHAR(50) NOT NULL COMMENT '存储值',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB COMMENT='数据字典';

CREATE TABLE sys_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  receiver_id BIGINT DEFAULT NULL COMMENT '接收人',
  msg_type VARCHAR(100) DEFAULT NULL COMMENT '消息类型',
  title VARCHAR(120) NOT NULL COMMENT '标题',
  content VARCHAR(500) DEFAULT NULL COMMENT '内容',
  biz_no VARCHAR(50) DEFAULT NULL COMMENT '业务编号',
  link VARCHAR(200) DEFAULT NULL COMMENT '跳转地址',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_msg_receiver (receiver_id, is_read)
) ENGINE=InnoDB COMMENT='站内消息';

CREATE TABLE sys_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(50) NOT NULL COMMENT '操作人',
  action VARCHAR(50) NOT NULL COMMENT '操作',
  target VARCHAR(120) DEFAULT NULL COMMENT '操作对象',
  detail VARCHAR(300) DEFAULT NULL COMMENT '明细',
  ip VARCHAR(100) DEFAULT NULL COMMENT '来源 IP',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  KEY idx_log_time (create_time)
) ENGINE=InnoDB COMMENT='操作日志';

INSERT INTO sys_org (id, org_name, org_code, city, address, phone, open_time, close_time, traffic, notice, cover, manager, status, create_time) VALUES
(1, '车维通 · 南山旗舰店', 'CN001', '深圳', '南山区科技园南区科苑路 15 号', '0755-8600 1234', '08:30', '19:00', '地铁 1 号线高新园站 B 出口步行 400 米', '进厂请携带行驶证；保养客户可享免费 32 项检测', '/uploads/seed/shop/01.jpg', '王琳', 1, '2026-01-08 09:00:00'),
(2, '车维通 · 宝安中心店', 'CN002', '深圳', '宝安区新湖路 88 号', '0755-2300 8899', '09:00', '18:30', '地铁 5 号线宝华站 C 出口', '钣喷业务需提前 1 天预约', '/uploads/seed/shop/02.jpg', '何敏', 1, '2026-02-16 09:00:00');

INSERT INTO sys_role (id, role_code, role_name, data_scope, remark) VALUES
(1, 'ROLE_MANAGER', '门店经理', 'all', '查看全店数据、审批异常、系统配置'),
(2, 'ROLE_SERVICE', '服务顾问', 'self', '接车建单、检测登记、报价与客户确认、交车'),
(3, 'ROLE_TECH', '维修技师', 'self', '仅本人任务：领取派工、记录工时、完工提交'),
(4, 'ROLE_STORE', '仓库人员', 'store', '配件档案、入库、领退、盘点与预警'),
(5, 'ROLE_QC', '质检', 'all', '质量检验、返工退回、复检'),
(6, 'ROLE_OWNER', '车主客户', 'self', '本人车辆、预约、报价确认、返修与评价');

INSERT INTO sys_user (id, username, password, real_name, phone, email, org_id, org_name, avatar, gender, status, login_fail_count, lock_until, last_login, deleted, create_time, update_time) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '王琳', '18900008877', 'manager@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/01.jpg', '女', 1, 0, NULL, '2026-11-01 08:13:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(2, 'manager02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '梁海', '18900008878', 'lianghai@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/02.jpg', '男', 1, 0, NULL, '2026-11-01 08:14:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(3, 'service01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '陈立', '13800001122', 'chenli@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/03.jpg', '男', 1, 0, NULL, '2026-11-01 08:15:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(4, 'service02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '周敏', '13900003344', 'zhoumin@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/04.jpg', '女', 1, 0, NULL, '2026-11-01 08:16:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(5, 'tech01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '刘洋', '13700005566', 'liuyang@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/05.jpg', '男', 1, 0, NULL, '2026-11-01 08:17:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(6, 'tech02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '王磊', '13600007788', 'wanglei@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/06.jpg', '男', 1, 0, NULL, '2026-11-01 08:18:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(7, 'tech03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '赵鹏', '13500009900', 'zhaopeng@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/07.jpg', '男', 1, 0, NULL, '2026-11-01 08:19:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(8, 'tech04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '周涛', '18800002211', 'zhoutao@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/08.jpg', '男', 1, 0, NULL, '2026-11-01 08:20:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(9, 'tech05', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '孙宇', '15000004433', 'sunyu@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/01.jpg', '男', 1, 0, NULL, '2026-11-01 08:21:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(10, 'tech06', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '李强', '13300001100', 'liqiang@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/02.jpg', '男', 1, 0, NULL, '2026-11-01 08:22:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(11, 'store01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '孙伟', '18600006655', 'sunwei@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/03.jpg', '男', 1, 0, NULL, '2026-11-01 08:23:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(12, 'store02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '钱芳', '18600006656', 'qianfang@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/04.jpg', '女', 1, 0, NULL, '2026-11-01 08:24:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(13, 'qc01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '王琳', '18900008877', 'qc@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/05.jpg', '女', 1, 0, NULL, '2026-11-01 08:25:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(14, 'qc02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '许峰', '18900008879', 'xufeng@cheweito.com', 1, '车维通 · 南山旗舰店', '/uploads/seed/avatar/06.jpg', '男', 1, 0, NULL, '2026-11-01 08:26:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(15, 'owner01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '张三丰', '13800006621', 'owner01@qq.com', 2, '车维通 · 宝安中心店', '/uploads/seed/avatar/07.jpg', '男', 1, 0, NULL, '2026-11-01 08:27:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(16, 'owner02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '王振国', '13700009915', 'owner02@qq.com', 2, '车维通 · 宝安中心店', '/uploads/seed/avatar/08.jpg', '男', 1, 0, NULL, '2026-11-01 08:28:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(17, 'owner03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '赵国强', '13600002233', 'owner03@qq.com', 2, '车维通 · 宝安中心店', '/uploads/seed/avatar/01.jpg', '男', 1, 0, NULL, '2026-11-01 08:29:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00'),
(18, 'owner04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '何静', '18600001174', 'owner04@qq.com', 2, '车维通 · 宝安中心店', '/uploads/seed/avatar/02.jpg', '女', 1, 0, NULL, '2026-11-01 08:30:00', 0, '2026-01-10 09:00:00', '2026-10-30 18:00:00');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 3, 2),
(4, 4, 2),
(5, 5, 3),
(6, 6, 3),
(7, 7, 3),
(8, 8, 3),
(9, 9, 3),
(10, 10, 3),
(11, 11, 4),
(12, 12, 4),
(13, 13, 5),
(14, 14, 5),
(15, 15, 6),
(16, 16, 6),
(17, 17, 6),
(18, 18, 6);

INSERT INTO sys_menu (id, parent_id, menu_name, path, icon, menu_type, perm, sort, visible) VALUES
(1, 0, '工作台', '/dashboard', 'chart', 'catalog', 'dashboard', 1, 1),
(2, 1, '维修经营分析', '/dashboard/index', 'chart', 'menu', 'dashboard:view', 1, 1),
(3, 1, '我的待办', '/todo', 'clip', 'menu', 'todo:view', 2, 1),
(4, 0, '接车与预约', '/reception', 'cal', 'catalog', 'reception', 2, 1),
(5, 4, '预约接车台', '/appointments', 'cal', 'menu', 'appointment:view', 1, 1),
(6, 4, '接车登记', '/reception/new', 'car', 'menu', 'reception:create', 2, 1),
(7, 6, '提交建单', '', '', 'button', 'order:create', 1, 1),
(8, 0, '车辆档案', '/vehicle', 'car', 'catalog', 'vehicle', 3, 1),
(9, 8, '客户档案', '/customers', 'user', 'menu', 'customer:view', 1, 1),
(10, 8, '维修车辆', '/vehicles', 'car', 'menu', 'vehicle:view', 2, 1),
(11, 10, '车辆详情', '/vehicles/:id', 'clip', 'menu', 'vehicle:detail', 3, 1),
(12, 0, '维修工单', '/order', 'clip', 'catalog', 'order', 4, 1),
(13, 12, '维修工单', '/orders', 'clip', 'menu', 'order:view', 1, 1),
(14, 12, '工单详情', '/orders/:id', 'clip', 'menu', 'order:detail', 2, 1),
(15, 12, '故障诊断与检测', '/diagnosis/:id', 'wrench', 'menu', 'inspection:view', 3, 1),
(16, 12, '报价与客户确认', '/quote/:id', 'cash', 'menu', 'quote:view', 4, 1),
(17, 12, '派工看板', '/dispatch', 'menu', 'menu', 'dispatch:view', 5, 1),
(18, 12, '质量检验', '/inspection/:id', 'check', 'menu', 'qc:view', 6, 1),
(19, 12, '交车结算', '/settlement/:id', 'cash', 'menu', 'settle:view', 7, 1),
(20, 0, '配件库存', '/part', 'box', 'catalog', 'part', 5, 1),
(21, 20, '配件档案', '/parts', 'box', 'menu', 'part:view', 1, 1),
(22, 20, '领用与预检', '/part/claim/:id', 'box', 'menu', 'part:claim', 2, 1),
(23, 20, '领退记录', '/part/flows', 'clip', 'menu', 'part:flow', 3, 1),
(24, 20, '库存批次', '/part/batches', 'box', 'menu', 'part:batch', 4, 1),
(25, 0, '质保与返修', '/warranty', 'shield', 'catalog', 'warranty', 6, 1),
(26, 25, '质保台账', '/warranties', 'shield', 'menu', 'warranty:view', 1, 1),
(27, 25, '返修记录', '/returns', 'wrench', 'menu', 'return:view', 2, 1),
(28, 0, '统计与配置', '/stat', 'chart', 'catalog', 'stat', 7, 1),
(29, 28, '经营分析报表', '/analysis', 'chart', 'menu', 'analysis:view', 1, 1),
(30, 28, '预检规则配置', '/rules', 'gear', 'menu', 'rule:config', 2, 1),
(31, 28, '维修项目库', '/projects', 'wrench', 'menu', 'project:view', 3, 1),
(32, 0, '系统管理', '/system', 'gear', 'catalog', 'system', 9, 1),
(33, 32, '员工管理', '/system/staff', 'user', 'menu', 'staff:view', 1, 1),
(34, 32, '用户账号', '/system/users', 'user', 'menu', 'user:view', 2, 1),
(35, 32, '角色权限', '/system/roles', 'lock', 'menu', 'role:view', 3, 1),
(36, 32, '菜单管理', '/system/menus', 'menu', 'menu', 'menu:view', 4, 0),
(37, 32, '消息中心', '/messages', 'bell', 'menu', 'message:view', 5, 1),
(38, 32, '个人中心', '/profile', 'user', 'menu', 'profile:view', 6, 1),
(39, 0, '我的爱车', '/portal', 'car', 'catalog', 'portal', 10, 1),
(40, 39, '我的车辆', '/portal/home', 'car', 'menu', 'portal:vehicle', 1, 1),
(41, 39, '维修进度', '/portal/orders', 'clip', 'menu', 'portal:order', 2, 1),
(42, 39, '报价确认', '/portal/quotes', 'cash', 'menu', 'portal:quote', 3, 1),
(43, 39, '返修申请', '/portal/returns', 'wrench', 'menu', 'portal:return', 4, 1);

INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4),
(5, 1, 5),
(6, 1, 6),
(7, 1, 7),
(8, 1, 8),
(9, 1, 9),
(10, 1, 10),
(11, 1, 11),
(12, 1, 12),
(13, 1, 13),
(14, 1, 14),
(15, 1, 15),
(16, 1, 16),
(17, 1, 17),
(18, 1, 18),
(19, 1, 19),
(20, 1, 20),
(21, 1, 21),
(22, 1, 22),
(23, 1, 23),
(24, 1, 24),
(25, 1, 25),
(26, 1, 26),
(27, 1, 27),
(28, 1, 28),
(29, 1, 29),
(30, 1, 30),
(31, 1, 31),
(32, 1, 32),
(33, 1, 33),
(34, 1, 34),
(35, 1, 35),
(36, 1, 36),
(37, 1, 37),
(38, 1, 38),
(39, 1, 39),
(40, 1, 40),
(41, 1, 41),
(42, 1, 42),
(43, 1, 43),
(44, 2, 1),
(45, 2, 2),
(46, 2, 3),
(47, 2, 4),
(48, 2, 5),
(49, 2, 6),
(50, 2, 7),
(51, 2, 8),
(52, 2, 9),
(53, 2, 10),
(54, 2, 11),
(55, 2, 12),
(56, 2, 13),
(57, 2, 14),
(58, 2, 15),
(59, 2, 16),
(60, 2, 17),
(61, 2, 18),
(62, 2, 19),
(63, 2, 20),
(64, 2, 21),
(65, 2, 22),
(66, 2, 23),
(67, 2, 24),
(68, 2, 25),
(69, 2, 26),
(70, 2, 27),
(71, 2, 37),
(72, 2, 38),
(73, 3, 1),
(74, 3, 2),
(75, 3, 3),
(76, 3, 12),
(77, 3, 13),
(78, 3, 14),
(79, 3, 15),
(80, 3, 17),
(81, 3, 20),
(82, 3, 21),
(83, 3, 22),
(84, 3, 37),
(85, 3, 38),
(86, 4, 1),
(87, 4, 2),
(88, 4, 3),
(89, 4, 20),
(90, 4, 21),
(91, 4, 22),
(92, 4, 23),
(93, 4, 24),
(94, 4, 37),
(95, 4, 38),
(96, 5, 1),
(97, 5, 2),
(98, 5, 3),
(99, 5, 4),
(100, 5, 5),
(101, 5, 8),
(102, 5, 9),
(103, 5, 10),
(104, 5, 11),
(105, 5, 12),
(106, 5, 13),
(107, 5, 14),
(108, 5, 15),
(109, 5, 16),
(110, 5, 18),
(111, 5, 19),
(112, 5, 25),
(113, 5, 26),
(114, 5, 27),
(115, 5, 28),
(116, 5, 29),
(117, 5, 30),
(118, 5, 37),
(119, 5, 38),
(120, 6, 39),
(121, 6, 40),
(122, 6, 41),
(123, 6, 42),
(124, 6, 43),
(125, 6, 38);

INSERT INTO sys_dict (id, dict_type, dict_label, dict_value, sort) VALUES
(1, 'order_status', '接车登记', 'RECEIVE', 1),
(2, 'order_status', '故障诊断', 'DIAGNOSIS', 2),
(3, 'order_status', '报价确认', 'QUOTE_CONFIRM', 3),
(4, 'order_status', '维修领料', 'REPAIR_PART', 4),
(5, 'order_status', '质量检验', 'QC', 5),
(6, 'order_status', '交车结算', 'SETTLE', 6),
(7, 'order_status', '返工中', 'REWORK', 7),
(8, 'order_status', '已取消', 'CANCELED', 8),
(9, 'order_type', '保养', 'MAINTAIN', 9),
(10, 'order_type', '一般维修', 'REPAIR', 10),
(11, 'order_type', '事故维修', 'ACCIDENT', 11),
(12, 'order_type', '检测', 'CHECK', 12),
(13, 'order_type', '钣喷', 'SHEET_METAL', 13),
(14, 'order_type', '电控检测', 'ELECTRIC', 14),
(15, 'part_flow_type', '领用', 'CLAIM', 15),
(16, 'part_flow_type', '补领', 'EXTRA', 16),
(17, 'part_flow_type', '退回', 'RETURN', 17),
(18, 'vehicle_status', '正常', 'NORMAL', 18),
(19, 'vehicle_status', '在修', 'IN_REPAIR', 19),
(20, 'vehicle_status', '待结算', 'WAIT_SETTLE', 20),
(21, 'severity', '高', 'HIGH', 21),
(22, 'severity', '中', 'MID', 22),
(23, 'severity', '低', 'LOW', 23),
(24, 'check_result', '正常', 'NORMAL', 24),
(25, 'check_result', '建议观察', 'OBSERVE', 25),
(26, 'check_result', '建议维修', 'REPAIR', 26),
(27, 'qc_result', '合格', 'PASS', 27),
(28, 'qc_result', '不合格', 'FAIL', 28),
(29, 'pay_type', '现金', 'CASH', 29),
(30, 'pay_type', '微信', 'WECHAT', 30),
(31, 'pay_type', '支付宝', 'ALIPAY', 31),
(32, 'pay_type', '刷卡', 'CARD', 32),
(33, 'duty_result', '门店责任', 'STORE', 33),
(34, 'duty_result', '配件质量', 'PART', 34),
(35, 'duty_result', '客户使用', 'CUSTOMER', 35),
(36, 'duty_result', '待判定', 'WAIT', 36),
(37, 'confirm_type', '电话确认', 'PHONE', 37),
(38, 'confirm_type', '短信链接', 'SMS', 38),
(39, 'confirm_type', '到店确认', 'ONSITE', 39),
(40, 'confirm_type', 'APP 确认', 'APP', 40),
(41, 'work_type', '机修', 'MECHANIC', 41),
(42, 'work_type', '电控', 'ELECTRIC', 42),
(43, 'work_type', '钣金', 'SHEET_METAL', 43),
(44, 'work_type', '喷漆', 'PAINT', 44),
(45, 'customer_type', '会员客户', 'MEMBER', 45),
(46, 'customer_type', '普通客户', 'NORMAL', 46);

INSERT INTO ar_customer (id, customer_no, name, phone, customer_type, source, visit_count, total_amount, remark, deleted, create_by, create_time, update_time, version) VALUES
(1, 'CUS-0001', '张三丰', '13800006621', 'MEMBER', '老客户转介绍', 3, 22007.38, '长期合作客户，介意等待时间', 0, 'admin', '2026-05-10 09:20:00', '2026-10-28 16:40:00', 0),
(2, 'CUS-0002', '李慧', '13900003082', 'NORMAL', '门店到店', 1, 22529.41, NULL, 0, 'admin', '2026-05-14 09:20:00', '2026-10-28 16:40:00', 0),
(3, 'CUS-0003', '王振国', '13700009915', 'MEMBER', '保险公司合作', 4, 22960.81, '长期合作客户，介意等待时间', 0, 'admin', '2026-05-18 09:20:00', '2026-10-28 16:40:00', 0),
(4, 'CUS-0004', '陈晓雯', '13500004407', 'NORMAL', '线上咨询', 1, 21301.83, NULL, 0, 'admin', '2026-05-22 09:20:00', '2026-10-28 16:40:00', 0),
(5, 'CUS-0005', '赵国强', '13600002233', 'MEMBER', '老客户转介绍', 5, 16978.82, '长期合作客户，介意等待时间', 0, 'admin', '2026-05-26 09:20:00', '2026-10-28 16:40:00', 0),
(6, 'CUS-0006', '刘佳', '18800007712', 'NORMAL', '门店到店', 2, 21004.31, NULL, 0, 'admin', '2026-05-30 09:20:00', '2026-10-28 16:40:00', 0),
(7, 'CUS-0007', '孙浩', '15000008829', 'NORMAL', '线上咨询', 1, 11592.99, NULL, 0, 'admin', '2026-06-03 09:20:00', '2026-10-28 16:40:00', 0),
(8, 'CUS-0008', '何静', '18600001174', 'MEMBER', '保险公司合作', 3, 14047.4, '长期合作客户，介意等待时间', 0, 'admin', '2026-06-07 09:20:00', '2026-10-28 16:40:00', 0),
(9, 'CUS-0009', '郑凯', '13100003345', 'NORMAL', '门店到店', 2, 9676.11, NULL, 0, 'admin', '2026-06-11 09:20:00', '2026-10-28 16:40:00', 0),
(10, 'CUS-0010', '吴敏', '13300005571', 'NORMAL', '线上咨询', 1, 18410.6, NULL, 0, 'admin', '2026-06-15 09:20:00', '2026-10-28 16:40:00', 0),
(11, 'CUS-0011', '周文', '15800002288', 'MEMBER', '老客户转介绍', 4, 8184.45, '长期合作客户，介意等待时间', 0, 'admin', '2026-06-19 09:20:00', '2026-10-28 16:40:00', 0),
(12, 'CUS-0012', '黄海', '15900003399', 'NORMAL', '门店到店', 2, 3905.48, NULL, 0, 'admin', '2026-06-23 09:20:00', '2026-10-28 16:40:00', 0),
(13, 'CUS-0013', '林芳', '18000004466', 'MEMBER', '线上咨询', 3, 9113.18, '长期合作客户，介意等待时间', 0, 'admin', '2026-06-27 09:20:00', '2026-10-28 16:40:00', 0),
(14, 'CUS-0014', '徐亮', '18100005577', 'NORMAL', '门店到店', 1, 7330.28, NULL, 0, 'admin', '2026-07-01 09:20:00', '2026-10-28 16:40:00', 0),
(15, 'CUS-0015', '马超', '18200006688', 'NORMAL', '保险公司合作', 2, 2895.78, NULL, 0, 'admin', '2026-07-05 09:20:00', '2026-10-28 16:40:00', 0),
(16, 'CUS-0016', '高远', '18300007799', 'MEMBER', '老客户转介绍', 3, 9973.04, '长期合作客户，介意等待时间', 0, 'admin', '2026-07-09 09:20:00', '2026-10-28 16:40:00', 0),
(17, 'CUS-0017', '罗静', '18500008811', 'NORMAL', '线上咨询', 1, 7046.12, NULL, 0, 'admin', '2026-07-13 09:20:00', '2026-10-28 16:40:00', 0),
(18, 'CUS-0018', '谢涛', '18700009922', 'NORMAL', '门店到店', 2, 13706.71, NULL, 0, 'admin', '2026-07-17 09:20:00', '2026-10-28 16:40:00', 0),
(19, 'CUS-0019', '韩雪', '18900001133', 'MEMBER', '老客户转介绍', 4, 15745.77, '长期合作客户，介意等待时间', 0, 'admin', '2026-07-21 09:20:00', '2026-10-28 16:40:00', 0),
(20, 'CUS-0020', '邓强', '19000002244', 'NORMAL', '门店到店', 1, 24407.25, NULL, 0, 'admin', '2026-07-25 09:20:00', '2026-10-28 16:40:00', 0);

INSERT INTO ar_vehicle (id, plate_no, customer_id, brand, model, vin, engine_no, mileage, buy_date, vehicle_status, photo, remark, deleted, create_by, create_time, update_time, version) VALUES
(1, '粤B·8K2L9', 1, '大众', '迈腾 330TSI', 'LFV3A23C8K3124587', 'ENG00100137', 24752, '2020-02-04', 'NORMAL', '/uploads/seed/vehicle/01.jpg', NULL, 0, 'service01', '2026-05-13 10:15:00', '2026-10-31 17:30:00', 0),
(2, '粤B·5D7F1', 2, '丰田', '卡罗拉 1.2T', 'LFMAP22C1H0123456', 'ENG00100274', 54252, '2021-03-07', 'NORMAL', '/uploads/seed/vehicle/02.jpg', NULL, 0, 'service01', '2026-05-18 10:15:00', '2026-10-31 17:30:00', 0),
(3, '粤B·9M3X2', 3, '本田', 'CR-V 240TURBO', 'LVHRW1856K5012345', 'ENG00100411', 61264, '2022-04-10', 'NORMAL', '/uploads/seed/vehicle/03.jpg', NULL, 0, 'service01', '2026-05-23 10:15:00', '2026-10-31 17:30:00', 0),
(4, '粤B·2T8C6', 4, '日产', '轩逸 1.6L', 'LGBH52E03HY123456', 'ENG00100548', 125995, '2023-05-13', 'NORMAL', '/uploads/seed/vehicle/04.jpg', NULL, 0, 'service01', '2026-05-28 10:15:00', '2026-10-31 17:30:00', 0),
(5, '粤B·6H1P4', 5, '别克', '君越 28T', 'LSGKB54E8JA123456', 'ENG00100685', 97696, '2024-06-16', 'NORMAL', '/uploads/seed/vehicle/05.jpg', NULL, 0, 'service01', '2026-06-02 10:15:00', '2026-10-31 17:30:00', 0),
(6, '粤B·4R7Y8', 6, '奥迪', 'A4L 40TFSI', 'LFV3B28R8J3123456', 'ENG00100822', 109068, '2019-07-19', 'NORMAL', '/uploads/seed/vehicle/06.jpg', NULL, 0, 'service01', '2026-06-07 10:15:00', '2026-10-31 17:30:00', 0),
(7, '粤B·1Q5Z3', 7, '特斯拉', 'Model 3 后驱', 'LRW3E7FA6LC012345', 'ENG00100959', 62888, '2020-08-22', 'NORMAL', '/uploads/seed/vehicle/07.jpg', NULL, 0, 'service01', '2026-06-12 10:15:00', '2026-10-31 17:30:00', 0),
(8, '粤B·3A9S1', 8, '宝马', '320Li M运动', 'LBV8W3105JMP12345', 'ENG00101096', 112351, '2021-09-25', 'NORMAL', '/uploads/seed/vehicle/08.jpg', NULL, 0, 'service01', '2026-06-17 10:15:00', '2026-10-31 17:30:00', 0),
(9, '粤B·7W2E5', 9, '马自达', '昂克赛拉 2.0L', 'LFPBM24A6L0123456', 'ENG00101233', 51753, '2022-10-28', 'NORMAL', '/uploads/seed/vehicle/09.jpg', NULL, 0, 'service01', '2026-06-22 10:15:00', '2026-10-31 17:30:00', 0),
(10, '粤B·0L4G7', 10, '现代', '途胜 1.5T', 'LBECUB2B1M0123456', 'ENG00101370', 136700, '2023-11-03', 'NORMAL', '/uploads/seed/vehicle/10.jpg', NULL, 0, 'service01', '2026-06-27 10:15:00', '2026-10-31 17:30:00', 0),
(11, '粤B·8N3B6', 11, '奔驰', 'C260L 运动版', 'LE4ZG4CB6LL123456', 'ENG00101507', 111379, '2024-12-06', 'NORMAL', '/uploads/seed/vehicle/11.jpg', NULL, 0, 'service01', '2026-07-02 10:15:00', '2026-10-31 17:30:00', 0),
(12, '粤B·2J5V7', 12, '斯柯达', '明锐 1.4T', 'LSVAB2BM8K0123456', 'ENG00101644', 41742, '2019-01-09', 'NORMAL', '/uploads/seed/vehicle/12.jpg', NULL, 0, 'service01', '2026-07-07 10:15:00', '2026-10-31 17:30:00', 0),
(13, '粤B·9C1M4', 13, '吉利', '星越L 2.0T', 'LB37622Z6NX123456', 'ENG00101781', 131580, '2020-02-12', 'NORMAL', '/uploads/seed/vehicle/01.jpg', NULL, 0, 'service01', '2026-07-12 10:15:00', '2026-10-31 17:30:00', 0),
(14, '粤B·6F8K2', 14, '比亚迪', '宋 PLUS DM-i', 'LGXC76C4XN0123456', 'ENG00101918', 68028, '2021-03-15', 'NORMAL', '/uploads/seed/vehicle/02.jpg', NULL, 0, 'service01', '2026-07-17 10:15:00', '2026-10-31 17:30:00', 0),
(15, '粤B·5G3H1', 15, '福特', '蒙迪欧 2.0T', 'LVSFCFBB6NA123456', 'ENG00102055', 31511, '2022-04-18', 'NORMAL', '/uploads/seed/vehicle/03.jpg', NULL, 0, 'service01', '2026-07-22 10:15:00', '2026-10-31 17:30:00', 0),
(16, '粤B·4L7P9', 16, '雪佛兰', '科鲁泽 1.3T', 'LSGBB44E0LA123456', 'ENG00102192', 83093, '2023-05-21', 'NORMAL', '/uploads/seed/vehicle/04.jpg', NULL, 0, 'service01', '2026-07-27 10:15:00', '2026-10-31 17:30:00', 0),
(17, '粤B·3Z2X8', 17, '沃尔沃', 'XC60 B5', 'LYVBR11D6NB123456', 'ENG00102329', 47844, '2024-06-24', 'NORMAL', '/uploads/seed/vehicle/05.jpg', NULL, 0, 'service01', '2026-08-01 10:15:00', '2026-10-31 17:30:00', 0),
(18, '粤B·7B4N5', 18, '雷克萨斯', 'ES200', 'JTJBARBZ9N2123456', 'ENG00102466', 114572, '2019-07-27', 'NORMAL', '/uploads/seed/vehicle/06.jpg', NULL, 0, 'service01', '2026-08-06 10:15:00', '2026-10-31 17:30:00', 0),
(19, '粤B·1D6Q3', 19, '凯迪拉克', 'XT5 28T', 'LSGNB83L8NA123456', 'ENG00102603', 34105, '2020-08-02', 'NORMAL', '/uploads/seed/vehicle/07.jpg', NULL, 0, 'service01', '2026-08-11 10:15:00', '2026-10-31 17:30:00', 0),
(20, '粤B·8R5T2', 20, '保时捷', 'Macan 2.0T', 'WP1ZZZ95ZNLB12345', 'ENG00102740', 144876, '2021-09-05', 'NORMAL', '/uploads/seed/vehicle/08.jpg', NULL, 0, 'service01', '2026-08-16 10:15:00', '2026-10-31 17:30:00', 0),
(21, '粤B·0V9W4', 1, '大众', '帕萨特 330TSI', 'LSVAM2BM6N0120021', 'ENG00102877', 56645, '2022-10-08', 'NORMAL', '/uploads/seed/vehicle/09.jpg', NULL, 0, 'service01', '2026-08-21 10:15:00', '2026-10-31 17:30:00', 0),
(22, '粤B·2Y7U1', 2, '丰田', '汉兰达 2.0T', 'LVGDA46A1NG120022', 'ENG00103014', 26548, '2023-11-11', 'NORMAL', '/uploads/seed/vehicle/10.jpg', NULL, 0, 'service01', '2026-08-26 10:15:00', '2026-10-31 17:30:00', 0),
(23, '粤B·5A8S6', 3, '本田', '思域 240TURBO', 'LVHFB26B8N0120023', 'ENG00103151', 51471, '2024-12-14', 'NORMAL', '/uploads/seed/vehicle/11.jpg', NULL, 0, 'service01', '2026-08-31 10:15:00', '2026-10-31 17:30:00', 0),
(24, '粤B·9E3J7', 4, '长安', 'CS75 PLUS', 'LS5A3DKR6NA120024', 'ENG00103288', 142391, '2019-01-17', 'NORMAL', '/uploads/seed/vehicle/12.jpg', NULL, 0, 'service01', '2026-09-05 10:15:00', '2026-10-31 17:30:00', 0);

INSERT INTO ar_project (id, project_code, project_name, work_type, std_hours, labor_fee, apply_models, common_parts, status, remark, create_time) VALUES
(1, 'PJ-001', '更换机油机滤', 'MECHANIC', 0.5, 80, '大众 / 奥迪 / 斯柯达', '机油、机滤', 1, NULL, '2026-05-02 09:00:00'),
(2, 'PJ-002', '更换空调滤芯', 'MECHANIC', 0.3, 50, '通用', '空调滤芯', 1, NULL, '2026-05-05 09:00:00'),
(3, 'PJ-003', '清洗节气门', 'ELECTRIC', 0.8, 160, '通用', '节气门清洗剂', 1, NULL, '2026-05-08 09:00:00'),
(4, 'PJ-004', '更换前刹车片', 'MECHANIC', 1.2, 220, '通用', '前刹车片', 1, NULL, '2026-05-11 09:00:00'),
(5, 'PJ-005', '四轮定位', 'MECHANIC', 0.6, 120, '通用', NULL, 1, NULL, '2026-05-14 09:00:00'),
(6, 'PJ-006', '更换制动液', 'MECHANIC', 0.5, 90, '通用', '制动液 DOT4', 1, NULL, '2026-05-17 09:00:00'),
(7, 'PJ-007', '钣金修复（单幅）', 'SHEET_METAL', 3.5, 480, '通用', '原子灰、砂纸', 1, NULL, '2026-05-20 09:00:00'),
(8, 'PJ-008', '喷漆（单幅）', 'PAINT', 2.0, 360, '通用', '面漆、清漆', 1, NULL, '2026-05-23 09:00:00'),
(9, 'PJ-009', '更换点火线圈', 'ELECTRIC', 1.0, 180, '大众 / 本田', '点火线圈', 1, NULL, '2026-05-26 09:00:00'),
(10, 'PJ-010', '更换火花塞（4 支）', 'MECHANIC', 0.9, 150, '通用', '火花塞', 1, NULL, '2026-05-29 09:00:00'),
(11, 'PJ-011', '更换空气滤芯', 'MECHANIC', 0.3, 45, '通用', '空气滤芯', 1, NULL, '2026-06-01 09:00:00'),
(12, 'PJ-012', '更换雨刮片', 'MECHANIC', 0.2, 30, '通用', '雨刮片', 1, NULL, '2026-06-04 09:00:00'),
(13, 'PJ-013', '更换电瓶', 'ELECTRIC', 0.6, 100, '通用', '蓄电池', 1, NULL, '2026-06-07 09:00:00'),
(14, 'PJ-014', '大保养（含油液）', 'MECHANIC', 2.4, 380, '通用', '机油、机滤、空滤、空调滤', 1, NULL, '2026-06-10 09:00:00'),
(15, 'PJ-015', '底盘异响排查', 'MECHANIC', 0.8, 140, '通用', NULL, 1, NULL, '2026-06-13 09:00:00'),
(16, 'PJ-016', '电控系统检测', 'ELECTRIC', 1.2, 200, '通用', NULL, 1, NULL, '2026-06-16 09:00:00'),
(17, 'PJ-017', '更换正时皮带', 'MECHANIC', 3.0, 520, '大众 / 奥迪', '正时皮带套装', 1, NULL, '2026-06-19 09:00:00'),
(18, 'PJ-018', '轮毂修复', 'SHEET_METAL', 1.5, 260, '通用', '轮毂修复剂', 1, NULL, '2026-06-22 09:00:00'),
(19, 'PJ-019', '更换减震器', 'MECHANIC', 2.0, 340, '通用', '减震器', 0, '停用：等待更新工时标准', '2026-06-25 09:00:00'),
(20, 'PJ-020', '空调系统检漏加氟', 'ELECTRIC', 1.0, 180, '通用', '冷媒', 0, '停用：等待更新工时标准', '2026-06-28 09:00:00');

INSERT INTO ar_part (id, part_code, part_name, apply_models, unit, purchase_price, sale_price, stock_qty, min_qty, max_qty, image, status, remark, deleted, create_time, update_time) VALUES
(1, 'PT-1001', '全合成机油 5W-30', '大众 / 奥迪通用', 'L', 52, 78, 128, 20, 200, '/uploads/seed/part/01.jpg', 1, NULL, 0, '2026-05-03 09:00:00', '2026-10-31 18:00:00'),
(2, 'PT-1002', '机油滤清器', '大众 迈腾', '个', 28, 48, 64, 10, 80, '/uploads/seed/part/02.jpg', 1, NULL, 0, '2026-05-05 09:00:00', '2026-10-31 18:00:00'),
(3, 'PT-1003', '空气滤芯', '大众 迈腾', '个', 34, 58, 6, 10, 60, '/uploads/seed/part/03.jpg', 1, NULL, 0, '2026-05-07 09:00:00', '2026-10-31 18:00:00'),
(4, 'PT-1004', '空调滤芯', '大众 迈腾', '个', 42, 68, 37, 10, 60, '/uploads/seed/part/04.jpg', 1, NULL, 0, '2026-05-09 09:00:00', '2026-10-31 18:00:00'),
(5, 'PT-1005', '前刹车片（陶瓷）', '丰田 卡罗拉', '套', 186, 298, 3, 5, 40, '/uploads/seed/part/05.jpg', 1, NULL, 0, '2026-05-11 09:00:00', '2026-10-31 18:00:00'),
(6, 'PT-1006', '火花塞（铱金）', '本田 CR-V', '支', 46, 78, 48, 12, 96, '/uploads/seed/part/06.jpg', 1, NULL, 0, '2026-05-13 09:00:00', '2026-10-31 18:00:00'),
(7, 'PT-1007', '制动液 DOT4', '通用', '瓶', 38, 62, 22, 8, 50, '/uploads/seed/part/07.jpg', 1, NULL, 0, '2026-05-15 09:00:00', '2026-10-31 18:00:00'),
(8, 'PT-1008', '节气门清洗剂', '通用', '瓶', 18, 32, 41, 10, 60, '/uploads/seed/part/08.jpg', 1, NULL, 0, '2026-05-17 09:00:00', '2026-10-31 18:00:00'),
(9, 'PT-1009', '雨刮片 24 寸', '通用', '支', 26, 45, 0, 6, 40, '/uploads/seed/part/09.jpg', 1, NULL, 0, '2026-05-19 09:00:00', '2026-10-31 18:00:00'),
(10, 'PT-1010', '点火线圈', '大众 / 本田', '个', 168, 268, 14, 4, 30, '/uploads/seed/part/10.jpg', 1, NULL, 0, '2026-05-21 09:00:00', '2026-10-31 18:00:00'),
(11, 'PT-1011', '蓄电池 60Ah', '通用', '个', 420, 620, 8, 3, 20, '/uploads/seed/part/01.jpg', 1, NULL, 0, '2026-05-23 09:00:00', '2026-10-31 18:00:00'),
(12, 'PT-1012', '减震器（前）', '通用', '个', 320, 480, 6, 4, 24, '/uploads/seed/part/02.jpg', 1, NULL, 0, '2026-05-25 09:00:00', '2026-10-31 18:00:00'),
(13, 'PT-1013', '正时皮带套装', '大众 / 奥迪', '套', 560, 820, 5, 2, 16, '/uploads/seed/part/03.jpg', 1, NULL, 0, '2026-05-27 09:00:00', '2026-10-31 18:00:00'),
(14, 'PT-1014', '面漆（素色白）', '通用', 'L', 88, 138, 26, 8, 50, '/uploads/seed/part/04.jpg', 1, NULL, 0, '2026-05-29 09:00:00', '2026-10-31 18:00:00'),
(15, 'PT-1015', '清漆', '通用', 'L', 96, 156, 9, 6, 40, '/uploads/seed/part/05.jpg', 1, NULL, 0, '2026-05-31 09:00:00', '2026-10-31 18:00:00'),
(16, 'PT-1016', '原子灰', '通用', '桶', 68, 108, 11, 5, 30, '/uploads/seed/part/06.jpg', 1, NULL, 0, '2026-06-02 09:00:00', '2026-10-31 18:00:00'),
(17, 'PT-1017', '冷媒 R134a', '通用', '瓶', 42, 72, 18, 8, 50, '/uploads/seed/part/07.jpg', 1, NULL, 0, '2026-06-04 09:00:00', '2026-10-31 18:00:00'),
(18, 'PT-1018', '轮毂修复剂', '通用', '套', 120, 190, 4, 3, 20, '/uploads/seed/part/08.jpg', 1, NULL, 0, '2026-06-06 09:00:00', '2026-10-31 18:00:00'),
(19, 'PT-1019', '玻璃水 -25℃', '通用', '瓶', 12, 22, 76, 20, 120, '/uploads/seed/part/09.jpg', 1, NULL, 0, '2026-06-08 09:00:00', '2026-10-31 18:00:00'),
(20, 'PT-1020', '刹车油管', '通用', '根', 78, 128, 12, 5, 40, '/uploads/seed/part/10.jpg', 0, '停用：改用品号 PT-1013 替代', 0, '2026-06-10 09:00:00', '2026-10-31 18:00:00');

INSERT INTO ar_batch (id, part_id, batch_no, warehouse, in_time, in_qty, left_qty, purchase_price, supplier, expire_date, status, create_time) VALUES
(1, 1, 'B2026040401', '主仓', '2026-04-04 10:00:00', 146, 128, 52, '曼牌华南', '2028-04-03', 'IN_STOCK', '2026-04-04 10:00:00'),
(2, 2, 'B2026050702', '主仓', '2026-05-07 10:00:00', 101, 64, 28, 'PPG', '2027-10-29', 'IN_STOCK', '2026-05-07 10:00:00'),
(3, 3, 'B2026061003', '主仓', '2026-06-10 10:00:00', 15, 0, 34, '长城润滑', '2027-06-10', 'OUT', '2026-06-10 10:00:00'),
(4, 3, 'B2026071104', '主仓', '2026-07-11 10:00:00', 17, 6, 34, 'PPG', '2028-07-10', 'LOW', '2026-07-11 10:00:00'),
(5, 4, 'B2026071305', '主仓', '2026-07-13 10:00:00', 74, 37, 42, '长城润滑', '2027-07-13', 'IN_STOCK', '2026-07-13 10:00:00'),
(6, 5, 'B2026081606', '主仓', '2026-08-16 10:00:00', 33, 3, 186, '粤润石化', '2028-02-07', 'LOW', '2026-08-16 10:00:00'),
(7, 6, 'B2026091907', '主仓', '2026-09-19 10:00:00', 52, 33, 46, '泰明顿', '2027-09-19', 'IN_STOCK', '2026-09-19 10:00:00'),
(8, 6, 'B2026102008', '主仓', '2026-10-20 10:00:00', 30, 15, 46, '泰明顿', '2028-04-12', 'IN_STOCK', '2026-10-20 10:00:00'),
(9, 7, 'B2026102209', '主仓', '2026-10-22 10:00:00', 39, 22, 38, '粤润石化', '2027-10-22', 'IN_STOCK', '2026-10-22 10:00:00'),
(10, 8, 'B2026032510', '主仓', '2026-03-25 10:00:00', 53, 41, 18, 'PPG', '2027-03-25', 'IN_STOCK', '2026-03-25 10:00:00'),
(11, 9, 'B2026040111', '主仓', '2026-04-01 10:00:00', 27, 16, 26, '曼牌华南', '2028-03-31', 'IN_STOCK', '2026-04-01 10:00:00'),
(12, 9, 'B2026050212', '主仓', '2026-05-02 10:00:00', 13, 0, 26, '曼牌华南', '2027-10-24', 'OUT', '2026-05-02 10:00:00'),
(13, 10, 'B2026050413', '主仓', '2026-05-04 10:00:00', 18, 14, 168, 'PPG', '2028-05-03', 'IN_STOCK', '2026-05-04 10:00:00'),
(14, 11, 'B2026060714', '主仓', '2026-06-07 10:00:00', 36, 8, 420, '泰明顿', '2027-11-29', 'IN_STOCK', '2026-06-07 10:00:00'),
(15, 12, 'B2026071015', '主仓', '2026-07-10 10:00:00', 27, 18, 320, '曼牌华南', '2028-07-09', 'IN_STOCK', '2026-07-10 10:00:00'),
(16, 12, 'B2026081116', '主仓', '2026-08-11 10:00:00', 40, 0, 320, '长城润滑', '2028-08-10', 'OUT', '2026-08-11 10:00:00'),
(17, 13, 'B2026081317', '主仓', '2026-08-13 10:00:00', 36, 5, 560, '长城润滑', '2028-08-12', 'IN_STOCK', '2026-08-13 10:00:00'),
(18, 14, 'B2026091618', '主仓', '2026-09-16 10:00:00', 58, 26, 88, '泰明顿', '2027-09-16', 'IN_STOCK', '2026-09-16 10:00:00'),
(19, 15, 'B2026101919', '主仓', '2026-10-19 10:00:00', 41, 23, 96, '粤润石化', '2027-10-19', 'IN_STOCK', '2026-10-19 10:00:00'),
(20, 15, 'B2026032020', '主仓', '2026-03-20 10:00:00', 21, 0, 96, 'PPG', '2027-09-11', 'OUT', '2026-03-20 10:00:00'),
(21, 16, 'B2026032221', '主仓', '2026-03-22 10:00:00', 19, 11, 68, '泰明顿', '2027-03-22', 'IN_STOCK', '2026-03-22 10:00:00'),
(22, 17, 'B2026042522', '主仓', '2026-04-25 10:00:00', 55, 18, 42, '泰明顿', '2028-04-24', 'IN_STOCK', '2026-04-25 10:00:00'),
(23, 18, 'B2026050123', '主仓', '2026-05-01 10:00:00', 39, 37, 120, 'PPG', '2027-10-23', 'IN_STOCK', '2026-05-01 10:00:00'),
(24, 18, 'B2026060224', '主仓', '2026-06-02 10:00:00', 15, 0, 120, '曼牌华南', '2027-06-02', 'OUT', '2026-06-02 10:00:00'),
(25, 19, 'B2026060425', '主仓', '2026-06-04 10:00:00', 116, 76, 12, '博世授权商', '2028-06-03', 'IN_STOCK', '2026-06-04 10:00:00'),
(26, 20, 'B2026070726', '主仓', '2026-07-07 10:00:00', 42, 12, 78, '粤润石化', '2028-07-06', 'IN_STOCK', '2026-07-07 10:00:00');

INSERT INTO ar_appointment (id, appoint_no, customer_id, vehicle_id, appoint_type, expect_time, problem_type, status, advisor_id, remark, create_time, update_time) VALUES
(1, 'AP-20261024-0001', 2, 2, 'REPAIR', '2026-10-24 11:00:00', '事故维修', 'RECEIVED', 4, NULL, '2026-10-23 11:00:00', '2026-10-24 11:00:00'),
(2, 'AP-20261025-0002', 3, 3, 'CHECK', '2026-10-25 13:00:00', '刹车异响', 'RECEIVED', 3, NULL, '2026-10-24 13:00:00', '2026-10-25 13:00:00'),
(3, 'AP-20261025-0003', 4, 4, 'SHEET_METAL', '2026-10-25 15:00:00', '底盘异响', 'RECEIVED', 4, NULL, '2026-10-24 15:00:00', '2026-10-25 15:00:00'),
(4, 'AP-20261026-0004', 5, 5, 'MAINTAIN', '2026-10-26 17:00:00', '事故维修', 'RECEIVED', 3, '客户要求当天取车', '2026-10-25 17:00:00', '2026-10-26 17:00:00'),
(5, 'AP-20261026-0005', 6, 6, 'REPAIR', '2026-10-26 09:00:00', '事故维修', 'RECEIVED', 4, NULL, '2026-10-25 09:00:00', '2026-10-26 09:00:00'),
(6, 'AP-20261027-0006', 7, 7, 'CHECK', '2026-10-27 11:00:00', '怠速抖动', 'RECEIVED', 3, NULL, '2026-10-26 11:00:00', '2026-10-27 11:00:00'),
(7, 'AP-20261027-0007', 8, 8, 'SHEET_METAL', '2026-10-27 13:00:00', '事故维修', 'MISSED', 4, NULL, '2026-10-26 13:00:00', '2026-10-27 13:00:00'),
(8, 'AP-20261028-0008', 9, 9, 'MAINTAIN', '2026-10-28 15:00:00', '事故维修', 'RECEIVED', 3, '客户要求当天取车', '2026-10-27 15:00:00', '2026-10-28 15:00:00'),
(9, 'AP-20261028-0009', 10, 10, 'REPAIR', '2026-10-28 17:00:00', '保养到期', 'CANCELED', 4, NULL, '2026-10-27 17:00:00', '2026-10-28 17:00:00'),
(10, 'AP-20261029-0010', 11, 11, 'CHECK', '2026-10-29 09:00:00', '怠速抖动', 'RECEIVED', 3, NULL, '2026-10-28 09:00:00', '2026-10-29 09:00:00'),
(11, 'AP-20261029-0011', 12, 12, 'SHEET_METAL', '2026-10-29 11:00:00', '刹车异响', 'RECEIVED', 4, NULL, '2026-10-28 11:00:00', '2026-10-29 11:00:00'),
(12, 'AP-20261030-0012', 13, 13, 'MAINTAIN', '2026-10-30 13:00:00', '底盘异响', 'RECEIVED', 3, '客户要求当天取车', '2026-10-29 13:00:00', '2026-10-30 13:00:00'),
(13, 'AP-20261030-0013', 14, 14, 'REPAIR', '2026-10-30 15:00:00', '怠速抖动', 'RECEIVED', 4, NULL, '2026-10-29 15:00:00', '2026-10-30 15:00:00'),
(14, 'AP-20261031-0014', 15, 15, 'CHECK', '2026-10-31 17:00:00', '刹车异响', 'RECEIVED', 3, NULL, '2026-10-30 17:00:00', '2026-10-31 17:00:00'),
(15, 'AP-20261031-0015', 16, 16, 'SHEET_METAL', '2026-10-31 09:00:00', '底盘异响', 'WAIT_RECEIVE', 4, NULL, '2026-10-30 09:00:00', '2026-10-31 09:00:00'),
(16, 'AP-20261101-0016', 17, 17, 'MAINTAIN', '2026-11-01 10:30:00', '保养到期', 'WAIT_RECEIVE', 3, '客户要求当天取车', '2026-10-31 10:30:00', '2026-11-01 10:30:00'),
(17, 'AP-20261101-0017', 18, 18, 'REPAIR', '2026-11-01 12:30:00', '空调不冷', 'WAIT_RECEIVE', 4, NULL, '2026-10-31 12:30:00', '2026-11-01 12:30:00'),
(18, 'AP-20261101-0018', 19, 19, 'CHECK', '2026-11-01 14:30:00', '底盘异响', 'WAIT_RECEIVE', 3, NULL, '2026-10-31 14:30:00', '2026-11-01 14:30:00');

-- 以上为主数据；工单及过程单据见下半部分（由同一生成器输出）