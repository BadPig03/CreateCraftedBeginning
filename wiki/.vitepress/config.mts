import { defineConfig, type DefaultTheme } from 'vitepress'

const base = '/CreateCraftedBeginning/'
const repository = 'https://github.com/BadPig03/CreateCraftedBeginning'

function sidebar(english: boolean): DefaultTheme.SidebarItem[] {
  const prefix = english ? '/en' : ''

  return [
    {
      text: english ? 'Start here' : '从这里开始',
      items: [
        { text: english ? 'Getting started' : '快速开始', link: `${prefix}/guide/getting-started` },
        { text: english ? 'Questions & answers' : '常见问题', link: `${prefix}/guide/faq` }
      ]
    },
    {
      text: english ? 'Explore the systems' : '探索系统',
      items: [
        { text: english ? 'Gas networks' : '气体网络', link: `${prefix}/systems/gas-networks` },
        { text: english ? 'Amethyst optics' : '紫晶光学', link: `${prefix}/systems/amethyst-optics` }
      ]
    }
  ]
}

export default defineConfig({
  title: 'Create Crafted Beginning Wiki',
  description: 'Create Crafted Beginning 的中英文玩法指南与机制参考。',
  base,
  srcDir: 'src',
  cleanUrls: false,
  head: [['link', { rel: 'icon', type: 'image/png', href: `${base}assets/breeze-chamber-cover.png` }]],
  sitemap: { hostname: `https://badpig03.github.io${base}` },
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      themeConfig: {
        nav: [
          { text: '指南', link: '/guide/getting-started', activeMatch: '/guide/' },
          { text: '系统', link: '/systems/gas-networks', activeMatch: '/systems/' }
        ],
        sidebar: sidebar(false),
        outline: { label: '本页目录', level: [2, 3] },
        docFooter: { prev: '上一篇', next: '下一篇' },
        editLink: { pattern: `${repository}/edit/main/wiki/src/:path`, text: '在 GitHub 上编辑此页' },
        sidebarMenuLabel: '目录',
        skipToContentLabel: '跳至正文',
        returnToTopLabel: '返回顶部',
        darkModeSwitchLabel: '外观',
        lightModeSwitchTitle: '切换到浅色模式',
        darkModeSwitchTitle: '切换到深色模式',
        langMenuLabel: '切换语言',
        notFound: {
          title: '没有找到这一页',
          quote: '这条管路还没有接通。回到首页，换一条路线吧。',
          linkLabel: '前往首页',
          linkText: '返回首页'
        },
        footer: { message: 'Create Crafted Beginning · 文档预览', copyright: 'Made for curious engineers.' }
      }
    },
    en: {
      label: 'English',
      lang: 'en',
      description: 'Guides and system notes for Create Crafted Beginning.',
      themeConfig: {
        nav: [
          { text: 'Guide', link: '/en/guide/getting-started', activeMatch: '/en/guide/' },
          { text: 'Systems', link: '/en/systems/gas-networks', activeMatch: '/en/systems/' }
        ],
        sidebar: sidebar(true),
        outline: { label: 'On this page', level: [2, 3] },
        editLink: { pattern: `${repository}/edit/main/wiki/src/:path`, text: 'Edit this page on GitHub' },
        footer: { message: 'Create Crafted Beginning · Wiki preview', copyright: 'Made for curious engineers.' }
      }
    }
  },
  themeConfig: {
    siteTitle: 'CCB Wiki',
    logo: { src: '/assets/breeze-chamber-cover.png', alt: 'Create Crafted Beginning' },
    socialLinks: [{ icon: 'github', link: repository }],
    search: {
      provider: 'local',
      options: {
        miniSearch: {
          options: {
            tokenize: (text) => {
              const segmenter = new Intl.Segmenter('zh-CN', { granularity: 'word' })
              return [...segmenter.segment(text)]
                .filter((part) => part.isWordLike)
                .map((part) => part.segment)
            }
          }
        },
        locales: {
          root: {
            translations: {
              button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
              modal: {
                displayDetails: '显示详细内容',
                resetButtonTitle: '清除搜索',
                backButtonTitle: '关闭搜索',
                noResultsText: '没有找到相关内容',
                footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
              }
            }
          }
        }
      }
    }
  }
})
