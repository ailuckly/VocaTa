import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ChatComposer from '@/components/chat/ChatComposer.vue'

describe('ChatComposer', () => {
  it('uses the primary action as voice chat when input is empty, and send when input has text', async () => {
    const wrapper = mount(ChatComposer, {
      props: {
        modelValue: '',
        connected: true,
        recording: false,
      },
    })

    const primaryButton = wrapper.find('[data-test="composer-primary"]')
    const micButton = wrapper.find('[data-test="composer-mic"]')

    expect(primaryButton.exists()).toBe(true)
    expect(micButton.exists()).toBe(true)
    expect(primaryButton.attributes('aria-label')).toContain('开始语音聊天')

    await primaryButton.trigger('click')
    expect(wrapper.emitted('toggleCall')).toHaveLength(1)
    expect(wrapper.emitted('send')).toBeUndefined()

    await wrapper.setProps({ modelValue: '你好' })
    expect(primaryButton.attributes('aria-label')).toContain('发送消息')

    await primaryButton.trigger('click')
    expect(wrapper.emitted('send')).toHaveLength(1)

    await micButton.trigger('click')
    expect(wrapper.emitted('toggleCall')).toHaveLength(2)
  })
})
