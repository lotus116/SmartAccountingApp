## 📚 SmartAccountingApp 智能记账本应用

### 简介 (Introduction)

`SmartAccountingApp` 是一款简洁、高效的移动记账应用，旨在帮助用户轻松记录日常收支，并通过直观的图表分析和灵活的筛选排序功能，更好地管理个人财务。该项目基于原生 Android 开发，使用了 SQLite 数据库进行本地数据持久化，并集成了知名的 MPAndroidChart 库进行数据可视化。

### 核心功能 (Features)

  * **用户认证：** 支持用户注册和本地登录，保护数据私密性。
  * **收支记录 (CRUD)：**
      * 快速添加收入和支出记录。
      * 点击列表项进入编辑/修改页面。
      * 在主界面直接删除记录。
  * **数据筛选与排序：**
      * **灵活筛选：** 可根据类型（收入/支出）、类别和日期范围进行组合筛选。
      * **自定义排序：** 支持按日期（最新/最早）和金额（最高/最低）排序，优化列表查看体验。
  * **图表分析：** 提供可视化界面（饼图和折线图），分析指定时间范围内的支出结构和收支趋势。
  * **数据管理：** 支持一键导出所有记账数据为 JSON 备份文件，并支持清空数据后导入备份文件。
  * **UI 优化：** 列表显示类别图标、备注、金额、日期和删除按钮。

### 技术栈 (Tech Stack)

| 方面 | 技术/库 | 用途 |
| :--- | :--- | :--- |
| **平台** | Android Native (Java) | 核心应用逻辑开发 |
| **数据库** | SQLite | 本地数据持久化和存储 |
| **UI/UX** | Material Components (AppCompat) | 标准化界面组件和浅色主题 |
| **图表** | MPAndroidChart | 高级数据可视化（饼图、折线图） |
| **数据序列化**| Gson | 导出/导入数据时的 JSON 格式处理 |
| **版本控制**| Git / GitHub | 项目版本管理和跨设备同步 |

### 项目结构 (Project Structure)

| 目录/文件 | 描述 |
| :--- | :--- |
| `java/activity` | 包含所有 Activity 文件：`MainActivity`, `AddAccountActivity`, `ChartActivity` 等。 |
| `java/model` | 数据模型层：`Account.java` (已实现 `Serializable`)。 |
| `java/adapter` | `AccountAdapter.java`：用于 `RecyclerView` 的列表项展示和事件处理。 |
| `java/util` | 实用工具类：`DBHelper.java` (数据库操作核心), `FileUtil.java` (导入/导出)。 |
| `res/layout` | 所有界面布局文件，包括 `activity_main.xml` 和 `item_account.xml`。 |
| `res/menu` | 顶部工具栏菜单定义：`main_menu.xml`。 |
| `res/values` | 资源文件：`themes.xml` (浅色主题设置), `strings.xml` (包含排序、筛选数组)。 |

### 安装与运行 (Installation Guide)

#### 1\. 克隆仓库

首先，将项目仓库克隆到本地计算机：

```bash
git clone [您的 GitHub 仓库地址]
cd SmartAccountingApp
```

#### 2\. 环境要求

  * Android Studio Arctic Fox 或更高版本。
  * Android SDK 21 (Lollipop) 或更高版本。

#### 3\. 运行项目

1.  打开 Android Studio。
2.  选择 **File -\> Open**，导航到您克隆的 `SmartAccountingApp` 目录。
3.  等待 Gradle 同步完成。
4.  选择一个模拟器或连接一个物理设备，点击 **Run** 按钮。

### 使用指南 (Usage)

1.  **登录/注册：** 首次使用需在登录界面进行注册。
2.  **添加记录：** 点击右下角的绿色浮动按钮 (`FAB`) 进入添加页面。
3.  **编辑记录：** 在主列表点击任一记账记录，即可跳转到编辑页进行修改或删除。
4.  **筛选数据：** 点击顶部工具栏的**漏斗图标**，可以设置日期、类型和类别进行筛选。
5.  **排序数据：** 主列表上方或工具栏上方的**下拉框**允许您切换排序方式（如：按金额，按时间）。
6.  **数据备份：** 点击右上角**菜单 (⋮)** -\> **一键导出**，文件将保存在应用的内部存储中。

### 未来改进方向 (Future Work)

  * 添加云同步功能（如 Firebase 或自定义后端）。
  * 增加月度预算设定和提醒功能。
  * 优化日期选择和金额输入的用户体验。
  * 支持多种货币或汇率转换。
