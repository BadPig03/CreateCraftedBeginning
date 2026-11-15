# Create Crafted Beginning Wiki

中英文公开文档站，使用 VitePress 1.6.4。默认中文，英文位于 `/en/`。与本地维护文档 `docs/` 分开，网站只收集 `wiki/src/` 下的内容，不参与 Gradle 或模组打包。

网站地址：<https://badpig03.github.io/CreateCraftedBeginning/>

## 本地预览

安装 Node.js 22 或更新版本，在仓库根目录运行：

```powershell
cd wiki
npm ci
npm run dev
```

打开终端显示的地址，路径包含 `/CreateCraftedBeginning/`。修改 Markdown 后会自动刷新。

检查发布版本：

```powershell
npm run build
npm run preview
```

## 添加文章

1. 在 `src/guide/` 或 `src/systems/` 新建中文 Markdown 文件。
2. 在 `src/en/` 下的相同相对路径添加英文版本。保持文件名一致，语言菜单才能切换到对应文章；给中英文对应标题使用相同的显式锚点，例如 `## 气体网络 {#gas-networks}`，以便切换语言时保留阅读位置。
3. 在 `.vitepress/config.mts` 的 `sidebar()` 中添加中英文目录标题和链接。
4. 共用图片放在 `src/public/assets/`，文章中使用 `/assets/文件名` 引用。
5. 运行 `npm run build` 检查站内链接，然后在本地预览中检查文章与搜索。

首页是 `src/index.md` 与 `src/en/index.md`，配色在 `.vitepress/theme/style.css`。搜索使用本地索引，包含中英文分词，无需 Algolia 密钥。

`cleanUrls` 保持关闭，以便 GitHub Pages 直接提供 `.html` 页面，文章深链接与刷新不依赖服务器重写。

## GitHub Pages 发布

首次在仓库 **Settings → Pages → Build and deployment → Source** 选择 **GitHub Actions**，无需自定义域名。

`.github/workflows/wiki-pages.yml` 在 `main` 分支的 Wiki 文件变动后自动构建、发布；PR 只构建，不发布。也可以在 Actions 中手动运行 **Wiki / GitHub Pages**。工作流使用 Node.js 24 与 `npm ci`，依赖版本记录在 `package-lock.json` 中。

输出目录为 `wiki/.vitepress/dist/`。不要提交 `node_modules/`、构建输出或缓存。现有 Gradle 构建工作流独立保留。

构建依赖通过 `overrides` 使用 Vite 6.4.6，替换 VitePress 1 默认的旧版 Vite 5，修复其开发服务器依赖的已知安全问题。升级 VitePress 时应同时检查此覆盖项。

## Assets

- `ccb-mark.svg` and `factory-core.svg` are original vector artwork for this wiki.
- `gas-canister.png` and `amethyst-crystal-sheet.png` are copied from this project's item textures. When replacing those textures, update the wiki copies as needed.
- The wiki follows this repository's MIT license. No text, images or theme overrides were copied from the Create wiki.
