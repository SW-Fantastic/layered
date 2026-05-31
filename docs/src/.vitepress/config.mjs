import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Layered",
  description: "Native library runtime",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: '首页', link: '/' },
      { text: '概述', link: '/summary/abstract' }
    ],

    sidebar: [
      {
        text: '概述',
        items: [
          {
            text: 'Layered是什么项目',
            link: "/summary/abstract",
            items: [
              {text: "基本概念 - ABI", link: "/summary/WhatIsABI"},
              {text: "项目设计", link: "/summary/BasicDesign"}
            ]
          },
          { text: '构建本项目', link: '/getstart/abstract.md' },
          { text: '使用方法', link: '/getstart/howToUse.md' },
          { text: 'Runtime API Examples', link: '/api-examples' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/vuejs/vitepress' }
    ]
  }
})
