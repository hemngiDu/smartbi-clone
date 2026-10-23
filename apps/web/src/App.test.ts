import { mount, flushPromises } from '@vue/test-utils'
import { vi, expect, test } from 'vitest'
import App from './App.vue'
test('renders data sources and datasets returned by API', async () => {
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce({ok:true,json:async()=>[{id:'source-1',name:'分析库',type:'POSTGRESQL',jdbcUrl:'jdbc:postgresql://db/a',username:'reader'}]})
    .mockResolvedValueOnce({ok:true,json:async()=>[{id:'1',name:'销售概览',sql:'select 1',dataSourceId:'source-1'}]}))
  const wrapper = mount(App)
  await flushPromises()
  expect(wrapper.text()).toContain('销售概览')
  expect(wrapper.text()).toContain('分析库')
  expect(wrapper.text()).toContain('参数化预览')
})

