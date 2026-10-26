import {mount,flushPromises} from '@vue/test-utils'
import {beforeEach,vi,expect,test} from 'vitest'
import App from './App.vue'

beforeEach(()=>vi.restoreAllMocks())

test('reopens a template with real merge, row height and column width layout',async()=>{
 const template={id:'r1',name:'月度销售',definition:{rows:2,columns:3,cells:[{row:0,column:0,value:'标题'}],merges:[{startRow:0,startColumn:0,endRow:0,endColumn:1}],rowHeights:{0:58},columnWidths:{0:180,1:90}}}
 vi.stubGlobal('fetch',vi.fn().mockResolvedValueOnce({ok:true,json:async()=>[{id:'1',name:'销售概览',sql:'select 1'}]}).mockResolvedValueOnce({ok:true,json:async()=>[template]}))
 const wrapper=mount(App);await flushPromises();await wrapper.find('.template').trigger('click')
 const sheet=wrapper.get('[data-testid="design-sheet"]');expect(sheet.attributes('style')).toContain('180px 90px');expect(sheet.attributes('style')).toContain('58px')
 const merged=wrapper.findAll('.cell.merged').filter(cell=>cell.isVisible());expect(merged).toHaveLength(1);expect(merged[0].attributes('style')).toContain('span 2');expect(wrapper.text()).toContain('标题')
})

test('saves regionId and edited dimensions',async()=>{
 const fetchMock=vi.fn().mockResolvedValueOnce({ok:true,json:async()=>[{id:'d1',name:'销售',sql:'select 1'}]}).mockResolvedValueOnce({ok:true,json:async()=>[]}).mockResolvedValueOnce({ok:true,json:async()=>({id:'r2'})}).mockResolvedValueOnce({ok:true,json:async()=>[{id:'d1',name:'销售'}]}).mockResolvedValueOnce({ok:true,json:async()=>[]})
 vi.stubGlobal('fetch',fetchMock);const wrapper=mount(App);await flushPromises();
 await wrapper.find('.inspector select').setValue('d1');await wrapper.get('input[placeholder="region"]').setValue('amount');await wrapper.findAll('.inspector select')[1].setValue('DOWN');await wrapper.vm.$nextTick();await wrapper.get('input[placeholder^="detail"]').setValue('sales-detail');
 const numbers=wrapper.findAll('input[type="number"]');await numbers[0].setValue('52');await numbers[1].setValue('160');await wrapper.find('.inspector button.wide:last-child').trigger('click');await wrapper.find('nav button:last-child').trigger('click');await flushPromises();
 const body=JSON.parse(String(fetchMock.mock.calls[2][1].body));expect(body.definition.cells[0].binding.regionId).toBe('sales-detail');expect(body.definition.rowHeights['0']).toBe(52);expect(body.definition.columnWidths['0']).toBe(160)
})
