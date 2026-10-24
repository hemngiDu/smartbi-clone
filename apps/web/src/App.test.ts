import { mount, flushPromises } from '@vue/test-utils'
import { vi, expect, test } from 'vitest'
import App from './App.vue'
test('renders datasets and report templates returned by API', async () => {
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce({ok:true,json:async()=>[{id:'1',name:'销售概览',sql:'select 1'}]})
    .mockResolvedValueOnce({ok:true,json:async()=>[{id:'r1',name:'月度销售',definition:{rows:2,columns:2,cells:[],merges:[],rowHeights:{},columnWidths:{}}}]}))
  const wrapper = mount(App)
  await flushPromises()
  expect(wrapper.text()).toContain('销售概览')
  expect(wrapper.text()).toContain('月度销售')
  expect(wrapper.text()).toContain('电子表格报表设计器')
})

