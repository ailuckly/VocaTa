/**
 * VocaTa AI对话系统 - WebSocket客户端和音频管理器
 * 基于文档 VocaTa-AI对话完整对接文档.md 实现
 *
 * Realtime voice control/STT subset:
 * - Client -> server:
 *   - audio_start: open a new voice stream session for incremental STT
 *   - binary audio frame: one audio chunk for the active session
 *   - audio_end: finish the current audio stream
 *   - audio_cancel: abort the current audio stream and discard partial state
 *   - ping: keepalive control message
 * - Server -> client:
 *   - stt_result: incremental transcript update
 *   - status: lifecycle notice
 *   - error: protocol or processing failure
 */

import { getToken } from './token'

type EventCallback = (data?: unknown) => void

interface WindowWithWebkitAudio extends Window {
  webkitAudioContext?: typeof AudioContext
}

// WebSocket消息类型定义
export interface WebSocketMessage {
  type: string
  [key: string]: unknown
}

export type ClientControlMessage =
  | { type: 'audio_start'; format: string; mimeType?: string; sampleRate?: number }
  | { type: 'audio_end' }
  | { type: 'audio_cancel' }
  | { type: 'ping' }

export interface ServerSttMessage extends WebSocketMessage {
  type: 'stt_result'
  text: string
  isFinal: boolean
  confidence: number
  timestamp: number
}

export interface ServerErrorMessage extends WebSocketMessage {
  type: 'error'
  error: string
  timestamp: number
}

interface LLMTextStreamMessage extends WebSocketMessage {
  type: 'llm_text_stream'
  text: string
  characterName: string
  isComplete: boolean
  timestamp: number
}

interface TTSAudioMetaMessage extends WebSocketMessage {
  type: 'tts_audio_meta'
  audioSize: number
  format: string
  sampleRate: number
  channels: number
  bitDepth: number
  timestamp: number
}

interface TTSResultMessage extends WebSocketMessage {
  type: 'tts_result'
  text: string
  format: string
  sampleRate: number
  voiceId?: string
  timestamp: number
}

interface CompleteMessage extends WebSocketMessage {
  type: 'complete'
  message: string
  timestamp: number
}

// WebSocket客户端类
export class VocaTaWebSocketClient {
  private ws: WebSocket | null = null
  private conversationUuid: string
  private reconnectAttempts = 0
  private readonly maxReconnectAttempts = 5
  private callbacks: Map<string, EventCallback[]> = new Map()
  private manualClose = false

  constructor(conversationUuid: string) {
    this.conversationUuid = conversationUuid
  }

  connect(): void {
    console.log('🔄 开始建立WebSocket连接，conversationUuid:', this.conversationUuid)

    const token = getToken()
    if (!token) {
      console.error('❌ 未找到认证令牌，无法建立WebSocket连接')
      this.emit('error', new Error('认证令牌未找到'))
      return
    }

    const appUrl = import.meta.env.VITE_APP_URL || window.location.origin
    const isSecure = appUrl.startsWith('https')
    const wsProtocol = isSecure ? 'wss' : 'ws'
    const host = appUrl.replace(/^https?:\/\//, '')
    const wsUrl = `${wsProtocol}://${host}/ws/chat/${this.conversationUuid}?token=${encodeURIComponent(token)}`
    console.log('🔌 尝试连接WebSocket:', wsUrl)
    console.log('🔐 使用Token:', token.substring(0, 20) + '...')

    try {
      this.manualClose = false
      this.ws = new WebSocket(wsUrl)
      this.ws.binaryType = 'arraybuffer'
      this.setupEventHandlers()
    } catch (error) {
      console.error('❌ WebSocket连接创建失败:', error)
      this.emit('error', error)
    }
  }

  private setupEventHandlers(): void {
    if (!this.ws) return

    this.ws.onopen = (event) => {
      console.log('✅ WebSocket连接已建立')
      console.log('🔍 WebSocket状态检查:', {
        readyState: this.ws?.readyState,
        isOpen: this.ws?.readyState === WebSocket.OPEN,
        WebSocketOPEN: WebSocket.OPEN
      })
      this.reconnectAttempts = 0
      this.emit('connected', event)
    }

    this.ws.onmessage = (event) => {
      // 检查是否为二进制音频数据
      if (event.data instanceof ArrayBuffer) {
        console.log(`📦 收到音频数据(ArrayBuffer): ${event.data.byteLength} bytes`)
        this.emit('audioData', event.data)
        return
      }

      // 检查是否为Blob音频数据
      if (event.data instanceof Blob) {
        console.log(`📦 收到音频数据(Blob): ${event.data.size} bytes`)
        // 将Blob转换为ArrayBuffer
        event.data.arrayBuffer().then(arrayBuffer => {
          this.emit('audioData', arrayBuffer)
        }).catch(error => {
          console.error('❌ Blob转ArrayBuffer失败:', error)
        })
        return
      }

      // 否则按JSON消息处理
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        console.log(`📨 收到消息:`, message)
        this.emit('message', message)
      } catch {
        console.error('❌ 解析消息失败:', event.data)
      }
    }

    this.ws.onclose = (event) => {
      console.log(`🔌 WebSocket连接关闭: code=${event.code}, reason="${event.reason}", wasClean=${event.wasClean}`)
      this.emit('disconnected', { event, manual: this.manualClose })
      const shouldReconnect = !this.manualClose
      this.ws = null
      if (shouldReconnect) {
        this.attemptReconnect()
      } else {
        this.reconnectAttempts = 0
      }
    }

    this.ws.onerror = (error) => {
      console.error('❌ WebSocket错误:', error)
      console.error('WebSocket readyState:', this.ws?.readyState)
      this.emit('error', error)
    }
  }

  // 发送文字消息
  sendTextMessage(text: string): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket未连接')
      return
    }

    const message = {
      type: 'text_message',
      data: { message: text }
    }

    console.log('📤 发送文字消息:', text)
    this.ws.send(JSON.stringify(message))
  }

  // 发送音频数据
  sendAudioData(audioBuffer: ArrayBuffer): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    // Binary audio frame: one chunk for the current active voice session.
    this.ws.send(audioBuffer)
  }

  // 音频录制控制
  startAudioRecording(): void {
    this.sendControlMessage({
      type: 'audio_start',
      format: 'pcm',
      sampleRate: 16000
    })
  }

  stopAudioRecording(): void {
    this.sendControlMessage({ type: 'audio_end' })
  }

  sendControlMessage(message: ClientControlMessage): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    console.log(`📡 发送控制指令:`, message)
    this.ws.send(JSON.stringify(message))
  }

  // 发送心跳
  sendPing(): void {
    this.sendControlMessage({ type: 'ping' })
  }

  // 事件监听器
  on<T = unknown>(event: string, callback: (data: T) => void): void {
    if (!this.callbacks.has(event)) {
      this.callbacks.set(event, [])
    }
    this.callbacks.get(event)?.push(callback as EventCallback)
  }

  private emit(event: string, data?: unknown): void {
    const callbacks = this.callbacks.get(event)
    if (callbacks) {
      callbacks.forEach(callback => callback(data))
    }
  }

  // 自动重连
  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      const delay = Math.pow(2, this.reconnectAttempts) * 1000
      console.log(`🔄 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts}) - ${delay}ms后`)

      setTimeout(() => {
        this.connect()
      }, delay)
    } else {
      console.error('❌ 重连次数已达上限')
      this.emit('reconnectFailed')
    }
  }

  disconnect(): void {
    if (this.ws) {
      this.manualClose = true
      try {
        this.ws.close(1000, 'client_closed')
      } finally {
        this.ws = null
      }
    }
  }

  // 获取连接状态
  get readyState(): number {
    return this.ws?.readyState || WebSocket.CLOSED
  }

  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// VAD 常量（ScriptProcessorNode 2048 帧 @ 16kHz = 128ms/帧）
const PROC_BUFFER = 2048
const SPEECH_THRESHOLD = 0.015        // RMS 超过此值 → 识别为说话
const SILENCE_THRESHOLD = 0.010       // RMS 低于此值 → 识别为静音
const MIN_SPEECH_FRAMES = 2           // 至少 2 帧真实语音才允许 VAD 触发（防误触）
const SILENCE_FRAMES_REQUIRED = 6     // 6 × 128ms ≈ 0.8s 静音后自动停止

// 音频管理器类 - PCM 实时录音模式（ScriptProcessorNode → 16kHz Int16 PCM）
export class AudioManager {
  private audioContext: AudioContext | null = null
  private recordingContext: AudioContext | null = null
  private scriptProcessor: ScriptProcessorNode | null = null
  private audioSourceNode: MediaStreamAudioSourceNode | null = null
  private audioQueue: ArrayBuffer[] = []
  private isPlaying = false
  private isRecording = false
  private audioStream: MediaStream | null = null

  private currentWsClient: VocaTaWebSocketClient | null = null
  private pendingChunkSends: Set<Promise<void>> = new Set()
  private chunkSendFailureCount = 0
  private lifecycleQueue: Promise<void> = Promise.resolve()
  private sessionCounter = 0
  private activeSessionId = 0
  private recordingState: 'idle' | 'starting' | 'recording' | 'stopping' = 'idle'
  private stopRecordingPromise: Promise<void> | null = null
  private stopRecordingResolve?: () => void
  private stopRecordingReject?: (reason?: unknown) => void
  private playbackStateListener?: (isPlaying: boolean) => void

  // VAD 状态
  private isMuted = false
  private isAISpeaking = false
  private hasSpeechStarted = false
  private speechFrameCount = 0
  private silenceFrameCount = 0
  private bargeInTriggered = false
  private onVADSilenceCallback?: () => void
  private onBargeInCallback?: () => void

  async initialize(): Promise<void> {
    try {
      console.log('🎵 音频管理器初始化完成（延迟初始化AudioContext）')
      // 不再在初始化时立即创建AudioContext，而是在需要时才创建
      // 这样避免了浏览器的安全策略限制
    } catch (error) {
      console.error('❌ 音频管理器初始化失败:', error)
      throw error
    }
  }

  async preparePlayback(): Promise<void> {
    try {
      await this.ensureAudioContext()
    } catch (error) {
      console.warn('⚠️ 准备音频播放失败:', error)
    }
  }

  private enqueueLifecycleOperation<T>(operation: () => Promise<T>): Promise<T> {
    const run = this.lifecycleQueue.then(operation, operation)
    this.lifecycleQueue = run.then(
      () => undefined,
      () => undefined
    )
    return run
  }

  private resetRecordingState(options: { stopTracks?: boolean } = {}): void {
    if (this.scriptProcessor) {
      try { this.scriptProcessor.disconnect() } catch { /* ignore */ }
      this.scriptProcessor = null
    }
    if (this.audioSourceNode) {
      try { this.audioSourceNode.disconnect() } catch { /* ignore */ }
      this.audioSourceNode = null
    }
    if (options.stopTracks && this.audioStream) {
      this.audioStream.getTracks().forEach(track => track.stop())
    }
    if (this.recordingContext) {
      this.recordingContext.close().catch(() => undefined)
      this.recordingContext = null
    }

    this.audioStream = null
    this.currentWsClient = null
    this.pendingChunkSends = new Set()
    this.chunkSendFailureCount = 0
    this.stopRecordingPromise = null
    this.stopRecordingResolve = undefined
    this.stopRecordingReject = undefined
    this.recordingState = 'idle'
    this.isRecording = false
    // VAD 状态重置（isMuted 跨轮次保持，不在这里重置）
    this.hasSpeechStarted = false
    this.speechFrameCount = 0
    this.silenceFrameCount = 0
    this.bargeInTriggered = false
  }

  // 延迟初始化AudioContext，在用户交互后调用
  private async ensureAudioContext(): Promise<void> {
    if (!this.audioContext) {
      console.log('🎵 延迟初始化音频上下文...')
      const AudioContextConstructor =
        window.AudioContext || (window as WindowWithWebkitAudio).webkitAudioContext
      if (!AudioContextConstructor) {
        throw new Error('当前浏览器不支持 AudioContext')
      }
      this.audioContext = new AudioContextConstructor()

      // 检查音频上下文状态
      if (this.audioContext.state === 'suspended') {
        await this.audioContext.resume()
      }

      console.log('✅ 音频上下文初始化成功')
    } else if (this.audioContext.state === 'suspended') {
      console.log('🔄 音频上下文处于挂起状态，尝试恢复...')
      await this.audioContext.resume()
    }
  }

  async startRecording(
    wsClient: VocaTaWebSocketClient,
    shouldAbort?: () => boolean
  ): Promise<boolean> {
    return this.enqueueLifecycleOperation(async () => {
      const abortAndCleanup = (stopTracks = true): false => {
        this.resetRecordingState({ stopTracks })
        return false
      }

      try {
        if (this.recordingState !== 'idle') {
          throw new Error('录音会话已在进行中')
        }

        console.log('🎤 开始实时分块录音模式...')
        this.currentWsClient = wsClient
        this.pendingChunkSends = new Set()
        this.chunkSendFailureCount = 0
        this.stopRecordingPromise = null
        this.stopRecordingResolve = undefined
        this.stopRecordingReject = undefined
        this.recordingState = 'starting'

        const sessionId = ++this.sessionCounter
        this.activeSessionId = sessionId

        // 确保AudioContext已初始化
        if (shouldAbort?.()) {
          return abortAndCleanup()
        }

        await this.ensureAudioContext()
        if (shouldAbort?.()) {
          return abortAndCleanup()
        }

        console.log('🎤 请求麦克风权限...')

        // 检查安全上下文（getUserMedia 仅在 HTTPS 或 localhost 下可用）
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
          throw new Error('当前环境不支持语音功能，请使用 HTTPS 访问或在 localhost 上测试')
        }

        // 直接获取麦克风权限
        this.audioStream = await navigator.mediaDevices.getUserMedia({
          audio: {
            channelCount: 1,
            sampleRate: 16000,
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true
          }
        })
        if (shouldAbort?.()) {
          return abortAndCleanup()
        }

        // 验证音频流
        const tracks = this.audioStream.getTracks()
        const audioTracks = tracks.filter(track => track.kind === 'audio')

        console.log('🔍 音频流详细信息:', {
          tracks: this.audioStream.getTracks().length,
          audioTracks: audioTracks.length,
          active: this.audioStream.active
        })

        if (audioTracks.length === 0 || !this.audioStream.active) {
          throw new Error('未能获取有效的音频轨道')
        }
        if (shouldAbort?.()) {
          return abortAndCleanup()
        }

        // 创建 16kHz AudioContext 用于 PCM 采集（讯飞 IAT 要求 16kHz 单声道 PCM）
        const AudioContextCtor =
          window.AudioContext || (window as WindowWithWebkitAudio).webkitAudioContext
        this.recordingContext = new AudioContextCtor({ sampleRate: 16000 })
        if (this.recordingContext.state === 'suspended') {
          await this.recordingContext.resume()
        }

        this.audioSourceNode = this.recordingContext.createMediaStreamSource(this.audioStream)

        // ScriptProcessorNode: 2048 帧 @ 16kHz = 128ms/帧，比 4096 更细粒度，利于 VAD
        this.scriptProcessor = this.recordingContext.createScriptProcessor(PROC_BUFFER, 1, 1)
        this.audioSourceNode.connect(this.scriptProcessor)
        this.scriptProcessor.connect(this.recordingContext.destination)

        this.scriptProcessor.onaudioprocess = (event) => {
          if (this.activeSessionId !== sessionId || this.recordingState !== 'recording') {
            return
          }
          if (this.isMuted) return  // 静音：跳过发送和 VAD

          const float32 = event.inputBuffer.getChannelData(0)

          // RMS 计算
          let sumSq = 0
          for (let i = 0; i < float32.length; i++) sumSq += float32[i] * float32[i]
          const rms = Math.sqrt(sumSq / float32.length)

          // VAD + Barge-in 状态更新
          if (rms > SPEECH_THRESHOLD) {
            this.hasSpeechStarted = true
            this.speechFrameCount++
            this.silenceFrameCount = 0
            // Barge-in：AI 说话时用户插话
            if (this.isAISpeaking && !this.bargeInTriggered) {
              this.bargeInTriggered = true
              this.onBargeInCallback?.()
            }
          } else if (this.hasSpeechStarted && this.speechFrameCount >= MIN_SPEECH_FRAMES) {
            if (rms < SILENCE_THRESHOLD) {
              this.silenceFrameCount++
              if (this.silenceFrameCount >= SILENCE_FRAMES_REQUIRED) {
                console.log('🔇 VAD: silence detected, auto-stopping')
                this.hasSpeechStarted = false
                this.silenceFrameCount = 0
                this.speechFrameCount = 0
                this.onVADSilenceCallback?.()
                return  // 触发后本帧不发送
              }
            } else {
              this.silenceFrameCount = 0
            }
          }

          // Float32 [-1,1] → Int16 PCM
          const int16 = new Int16Array(float32.length)
          for (let i = 0; i < float32.length; i++) {
            const s = Math.max(-1, Math.min(1, float32[i]))
            int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff
          }
          if (this.currentWsClient?.isConnected) {
            this.currentWsClient.sendAudioData(int16.buffer.slice(0))
          } else {
            this.chunkSendFailureCount++
          }
        }

        if (shouldAbort?.()) {
          return abortAndCleanup()
        }

        this.recordingState = 'recording'
        this.isRecording = true
        console.log('✅ 开始 PCM 实时录音 (16kHz, mono, Int16, VAD enabled)')
        return true
      } catch (error) {
        this.resetRecordingState({ stopTracks: true })
        console.error('❌ 录音启动失败:', error)
        throw error
      }
    })
  }

  async stopRecording(): Promise<boolean> {
    return this.enqueueLifecycleOperation(async () => {
      if (this.recordingState !== 'recording') {
        return false
      }

      const sessionId = this.activeSessionId

      if (!this.stopRecordingPromise) {
        this.recordingState = 'stopping'
        this.isRecording = false

        this.stopRecordingPromise = new Promise<void>((resolve, reject) => {
          this.stopRecordingResolve = resolve
          this.stopRecordingReject = reject

          try {
            // 断开 PCM 采集节点
            if (this.scriptProcessor) {
              this.scriptProcessor.disconnect()
              this.scriptProcessor = null
            }
            if (this.audioSourceNode) {
              this.audioSourceNode.disconnect()
              this.audioSourceNode = null
            }
            if (this.audioStream) {
              this.audioStream.getTracks().forEach(t => t.stop())
              this.audioStream = null
            }
            if (this.recordingContext) {
              this.recordingContext.close().catch(() => undefined)
              this.recordingContext = null
            }
            console.log('⏹️ PCM 录音已停止')
            resolve()
          } catch (error) {
            console.error('❌ 停止录音失败:', error)
            this.stopRecordingResolve = undefined
            this.stopRecordingReject = undefined
            this.stopRecordingPromise = null
            this.recordingState = 'idle'
            reject(error)
          }
        })
      }

      try {
        await this.stopRecordingPromise
        return sessionId === this.activeSessionId
      } finally {
        this.stopRecordingPromise = null
        this.stopRecordingResolve = undefined
        this.stopRecordingReject = undefined
        this.currentWsClient = null
        this.recordingState = 'idle'
      }
    })
  }

  // VAD / 静音 / Barge-in 控制
  setVADSilenceCallback(cb: (() => void) | undefined): void {
    this.onVADSilenceCallback = cb
  }

  setBargeInCallback(cb: (() => void) | undefined): void {
    this.onBargeInCallback = cb
  }

  setAISpeaking(speaking: boolean): void {
    this.isAISpeaking = speaking
    if (!speaking) {
      // AI 停止说话后重置 barge-in，允许下一轮检测
      this.bargeInTriggered = false
    }
  }

  setMuted(muted: boolean): void {
    this.isMuted = muted
    if (muted) {
      this.hasSpeechStarted = false
      this.silenceFrameCount = 0
      this.speechFrameCount = 0
    }
  }

  get muted(): boolean {
    return this.isMuted
  }

  async playAudio(audioBuffer: ArrayBuffer): Promise<void> {    try {
      if (!this.audioContext) {
        await this.initialize()
      }

      const audioData = await this.audioContext!.decodeAudioData(audioBuffer.slice())
      const source = this.audioContext!.createBufferSource()
      source.buffer = audioData

      // 添加音量控制
      const gainNode = this.audioContext!.createGain()
      source.connect(gainNode)
      gainNode.connect(this.audioContext!.destination)

      source.start()
      console.log(`🔊 播放音频: 时长${audioData.duration.toFixed(2)}秒`)

      return new Promise((resolve) => {
        source.onended = () => resolve()
      })
    } catch (error) {
      console.error('❌ 音频播放失败:', error)
    }
  }

  // 音频队列管理
  addToQueue(audioBuffer: ArrayBuffer): void {
    this.audioQueue.push(audioBuffer)
    if (!this.isPlaying) {
      this.playQueue()
    }
  }

  private async playQueue(): Promise<void> {
    if (this.audioQueue.length === 0) {
      this.isPlaying = false
      this.notifyPlaybackState(false)
      return
    }

    this.isPlaying = true
    this.notifyPlaybackState(true)

    try {
      // 确保AudioContext已初始化
      await this.ensureAudioContext()
    } catch (error) {
      console.warn('⚠️ AudioContext初始化失败，跳过音频播放:', error)
      this.isPlaying = false
      return
    }

    const audioBuffer = this.audioQueue.shift()!

    try {
      await this.playAudio(audioBuffer)
    } catch (error) {
      console.error('❌ 队列音频播放失败:', error)
    }

    // 播放下一个
    this.playQueue()
  }

  clearQueue(): void {
    this.audioQueue = []
    this.isPlaying = false
    this.notifyPlaybackState(false)
    console.log('🗑️ 清除音频队列')
  }

  // 获取音量级别（用于可视化）
  getVolumeAnalyzer(): (() => number) | null {
    if (!this.audioStream || !this.audioContext) {
      return null
    }

    const analyser = this.audioContext.createAnalyser()
    const microphone = this.audioContext.createMediaStreamSource(this.audioStream)
    const dataArray = new Uint8Array(analyser.frequencyBinCount)

    microphone.connect(analyser)
    analyser.fftSize = 256

    return () => {
      analyser.getByteFrequencyData(dataArray)
      const average = dataArray.reduce((sum, value) => sum + value, 0) / dataArray.length
      return average / 255 // 标准化到0-1
    }
  }

  // 检查麦克风权限
  async checkMicrophonePermission(): Promise<PermissionState> {
    try {
      const result = await navigator.permissions.query({ name: 'microphone' as PermissionName })
      return result.state
    } catch {
      return 'prompt'
    }
  }

  get recording(): boolean {
    return this.isRecording
  }

  get playing(): boolean {
    return this.isPlaying
  }

  setPlaybackStateListener(listener: (isPlaying: boolean) => void): void {
    this.playbackStateListener = listener
  }

  private notifyPlaybackState(isPlaying: boolean): void {
    this.playbackStateListener?.(isPlaying)
  }
}

// 实时AI对话管理器
export class VocaTaAIChat {
  private wsClient: VocaTaWebSocketClient | null = null
  private audioManager: AudioManager
  private isAudioCallActive = false
  private isContinuousModeActive = false
  private conversationUuid: string | null = null
  private connectingPromise: Promise<void> | null = null
  private voiceState: 'idle' | 'starting' | 'recording' | 'stopping' = 'idle'
  private pendingStartPromise: Promise<boolean> | null = null
  private pendingStopPromise: Promise<void> | null = null
  private stopRequested = false

  // 临时消息存储，用于流式显示
  private currentLLMResponse = ''
  private currentSTTText = ''

  // 回调函数
  private onMessageCallback?: (message: WebSocketMessage) => void
  private onSTTResultCallback?: (text: string, isFinal: boolean) => void
  private onLLMStreamCallback?: (text: string, isComplete: boolean, characterName?: string) => void
  private onAudioPlayCallback?: (isPlaying: boolean) => void
  private onConnectionStatusCallback?: (status: 'connected' | 'disconnected' | 'error', message?: string) => void

  constructor() {
    this.audioManager = new AudioManager()
    this.audioManager.setPlaybackStateListener(isPlaying => {
      this.onAudioPlayCallback?.(isPlaying)
      // 同步 AI 说话状态给 AudioManager（用于 barge-in 检测）
      this.audioManager.setAISpeaking(isPlaying)
      // 持续模式：TTS 播完后自动开始下一轮聆听
      if (!isPlaying && this.isAudioCallActive && this.isContinuousModeActive) {
        setTimeout(() => {
          if (this.isAudioCallActive && this.voiceState === 'idle') {
            this.startRecording().catch(err => console.error('❌ 自动重启录音失败:', err))
          }
        }, 300)
      }
    })
  }

  async initialize(conversationUuid: string): Promise<void> {
    try {
      console.log('🚀 初始化AI对话系统...')

      // 初始化音频管理器
      await this.audioManager.initialize()

      // 建立WebSocket连接并等待连接成功
      await this.connectWebSocket(conversationUuid)
      this.conversationUuid = conversationUuid

      console.log('✅ AI对话系统初始化完成')
    } catch (error) {
      console.error('❌ AI对话系统初始化失败:', error)
      throw error
    }
  }

  private connectWebSocket(conversationUuid: string): Promise<void> {
    if (this.connectingPromise) {
      return this.connectingPromise
    }

    this.connectingPromise = new Promise((resolve, reject) => {
      let connectionResolved = false // 防止重复resolve

      const finalize = () => {
        this.connectingPromise = null
      }

      try {
        this.wsClient = new VocaTaWebSocketClient(conversationUuid)
      } catch (creationError) {
        finalize()
        reject(creationError)
        return
      }

      // 设置事件监听器
      this.wsClient.on('connected', () => {
        console.log('🎉 WebSocket连接成功，等待服务器确认...')
        // 不在这里resolve，等待服务器状态消息
      })

      this.wsClient.on('message', (message: WebSocketMessage) => {
        this.handleWebSocketMessage(message)

        // 如果收到状态消息表示连接已建立，则resolve
        const statusMessage = typeof message.message === 'string' ? message.message : ''
        if (
          !connectionResolved &&
          message.type === 'status' &&
          (statusMessage.includes('连接已建立') || statusMessage.includes('WebSocket连接已建立'))
        ) {
          console.log('🎉 收到服务器连接确认，连接完全建立')
          connectionResolved = true
          this.onConnectionStatusCallback?.('connected', 'WebSocket连接已建立')
          resolve()
          finalize()
        }

        // 如果还没有连接确认，但收到了任何其他消息（AI回复等），也认为连接成功
        if (!connectionResolved && (message.type === 'llm_text_stream' || message.type === 'text_message')) {
          console.log('🎯 收到AI消息，连接确认成功')
          connectionResolved = true
          this.onConnectionStatusCallback?.('connected', 'AI系统连接成功')
          resolve()
          finalize()
        }
      })

      this.wsClient.on('audioData', (audioBuffer: ArrayBuffer) => {
        this.handleAudioData(audioBuffer)
      })

      this.wsClient.on('error', (error) => {
        console.error('❌ WebSocket错误:', error)
        this.onConnectionStatusCallback?.('error', 'WebSocket连接错误')
        if (!connectionResolved) {
          connectionResolved = true
          reject(error instanceof Error ? error : new Error('WebSocket连接错误'))
          finalize()
        }
      })

      this.wsClient.on('disconnected', (payload: { event: CloseEvent, manual: boolean }) => {
        if (!payload?.manual) {
          console.log('📡 WebSocket连接断开，正在重连...')
          this.onConnectionStatusCallback?.('disconnected', '语音连接已断开，正在重连...')
        } else {
          console.log('📡 WebSocket连接已手动关闭')
        }
      })

      this.wsClient.on('reconnectFailed', () => {
        console.error('❌ WebSocket重连失败')
        this.onConnectionStatusCallback?.('error', '连接失败，请刷新页面重试')
      })

      // 启动连接
      this.wsClient.connect()

      // 设置超时，如果10秒内没有连接成功，则reject
      setTimeout(() => {
        if (!connectionResolved) {
          console.error('❌ WebSocket连接超时')
          connectionResolved = true
          reject(new Error('WebSocket连接超时'))
          finalize()
        }
      }, 10000)
    })

    return this.connectingPromise
  }

  private handleWebSocketMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case 'stt_result':
        this.handleSTTResult(message as ServerSttMessage)
        break

      case 'llm_text_stream':
        this.handleLLMTextStream(message as LLMTextStreamMessage)
        break

      case 'tts_result':
        this.handleTTSResult(message as TTSResultMessage)
        break

      case 'tts_audio_meta':
        this.handleTTSAudioMeta(message as TTSAudioMetaMessage)
        break

      case 'complete':
        this.handleProcessComplete(message as CompleteMessage)
        break

      case 'error':
        this.handleError(message as ServerErrorMessage)
        break

      default:
        console.log('🔄 收到其他类型消息:', message)
    }

    // 触发通用消息回调
    this.onMessageCallback?.(message)
  }

  private handleSTTResult(message: ServerSttMessage): void {
    console.log(`🎤 STT识别: ${message.text} (${message.isFinal ? '最终' : '临时'})`)

    this.currentSTTText = message.text
    this.onSTTResultCallback?.(message.text, message.isFinal)
  }

  private handleLLMTextStream(message: LLMTextStreamMessage): void {
    console.log(`🤖 LLM响应: ${message.text} (${message.isComplete ? '完成' : '流式'})`)

    // 修复：始终累积文本，无论是否完成
    // 流式渲染应该累积所有收到的文本片段
    this.currentLLMResponse += message.text

    console.log(`🔍 当前累积文本长度: ${this.currentLLMResponse.length}`)

    this.onLLMStreamCallback?.(this.currentLLMResponse, message.isComplete, message.characterName)

    if (message.isComplete) {
      this.currentLLMResponse = '' // 重置
    }
  }

  private handleTTSResult(message: TTSResultMessage): void {
    console.log(`🗣️ TTS最终文字: ${message.text} (格式: ${message.format}, 采样率: ${message.sampleRate})`)

    if (message.text) {
      this.onLLMStreamCallback?.(message.text, true, message.voiceId)
    }
  }

  private handleTTSAudioMeta(message: TTSAudioMetaMessage): void {
    console.log(`🔊 TTS音频元数据: ${message.audioSize} bytes, ${message.format}`)
  }

  private handleAudioData(audioBuffer: ArrayBuffer): void {
    console.log(`🔊 播放音频数据: ${audioBuffer.byteLength} bytes`)
    this.audioManager.addToQueue(audioBuffer)
  }

  private handleProcessComplete(message: CompleteMessage): void {
    console.log('✅ 处理完成:', message.message)
    // STT 无结果时 TTS 不播放，不会触发 onAudioPlay(false) → 手动触发下一轮
    if (this.isAudioCallActive && this.isContinuousModeActive && !this.audioManager.playing) {
      setTimeout(() => {
        if (this.isAudioCallActive && this.voiceState === 'idle') {
          this.startRecording().catch(err => console.error('❌ complete后自动重启失败:', err))
        }
      }, 300)
    }
  }

  private handleError(message: ServerErrorMessage): void {
    console.error('❌ 服务器错误:', message.error)
    // 如果正在录音期间收到服务器错误，停止录音避免卡死
    if (this.voiceState !== 'idle') {
      this.stopRecording().catch(() => undefined)
    }
  }

  // 公开方法
  sendTextMessage(text: string): void {
    if (!this.wsClient) {
      console.error('❌ WebSocket客户端未初始化')
      return
    }

    this.wsClient.sendTextMessage(text)
  }

  // 开始录音
  async startRecording(): Promise<void> {
    if (!this.audioCallActive) {
      throw new Error('音频通话未激活')
    }

    if (this.voiceState === 'starting') {
      return this.pendingStartPromise ? this.pendingStartPromise.then(() => undefined) : undefined
    }

    if (this.voiceState === 'recording' || this.voiceState === 'stopping') {
      return
    }

    this.voiceState = 'starting'
    this.stopRequested = false

    this.pendingStartPromise = (async () => {
      let started = false
      let shouldCancel = false
      try {
        console.log('📞 开始实时分块录音')

        await this.ensureWebSocketConnection()
        if (!this.wsClient) {
          throw new Error('WebSocket客户端未初始化')
        }

        this.wsClient.startAudioRecording()

        started = await this.audioManager.startRecording(this.wsClient, () => this.stopRequested)
        shouldCancel = this.stopRequested || !started

        if (shouldCancel) {
          if (started) {
            await this.audioManager.stopRecording()
          }
          this.wsClient.sendControlMessage({ type: 'audio_cancel' })
          this.voiceState = 'idle'
          return false
        }

        this.voiceState = 'recording'
        return true
      } catch (error) {
        console.error('❌ 无法启动录音:', error)
        const errorMessage = error instanceof Error ? error.message : '无法启动录音'
        const errorName = error instanceof DOMException ? error.name : ''
        if (errorName === 'NotAllowedError' || errorName === 'SecurityError') {
          this.onConnectionStatusCallback?.('error', '麦克风权限失败')
        } else {
          this.onConnectionStatusCallback?.('error', errorMessage)
        }
        if (this.wsClient) {
          this.wsClient.sendControlMessage({ type: 'audio_cancel' })
        }
        this.voiceState = 'idle'
        throw error
      } finally {
        this.pendingStartPromise = null
      }
    })()

    return this.pendingStartPromise.then(() => undefined)
  }

  async prepareAudioPlayback(): Promise<void> {
    try {
      await this.audioManager.preparePlayback()
    } catch (error) {
      console.warn('⚠️ 准备音频上下文失败:', error)
    }
  }

  // 停止录音
  async stopRecording(): Promise<void> {
    console.log('📞 停止录音并完成音频流刷新')

    if (this.voiceState === 'idle') {
      return
    }

    if (this.voiceState === 'starting') {
      this.stopRequested = true
      if (this.pendingStartPromise) {
        await this.pendingStartPromise
      }
      return
    }

    if (this.voiceState === 'stopping') {
      if (this.pendingStopPromise) {
        await this.pendingStopPromise
      }
      return
    }

    this.voiceState = 'stopping'
    this.pendingStopPromise = (async () => {
      try {
        const stoppedActiveSession = await this.audioManager.stopRecording()
        if (stoppedActiveSession) {
          this.wsClient?.stopAudioRecording()
        }
      } finally {
        this.voiceState = 'idle'
        this.pendingStopPromise = null
        this.stopRequested = false
      }
    })()

    await this.pendingStopPromise
  }

  // 兼容旧的音频通话方法
  async startAudioCall(): Promise<void> {
    await this.ensureWebSocketConnection()
    if (!this.wsClient || !this.wsClient.isConnected) {
      throw new Error('WebSocket未连接，无法启动音频通话')
    }

    this.isAudioCallActive = true
    this.isContinuousModeActive = true

    // 清空残留的播放队列
    this.audioManager.clearQueue()
    this.onAudioPlayCallback?.(false)

    // 注册 VAD 静音回调：静音 ~0.8s 后自动提交
    this.audioManager.setVADSilenceCallback(() => {
      if (this.voiceState === 'recording') {
        this.stopRecording().catch(err => console.error('❌ VAD 自动停止失败:', err))
      }
    })

    // 注册 Barge-in 回调：AI 说话时用户插话
    this.audioManager.setBargeInCallback(() => {
      console.log('🎤 Barge-in：用户插话，打断 AI')
      this.audioManager.clearQueue()
      // 发送 audio_start → 服务端 SPEAKING 状态时触发 handleBargeIn
      this.wsClient?.startAudioRecording()
    })

    // 立即开始聆听（GPT Voice 体验）
    console.log('📞 音频通话已激活，立即开始聆听')
    await this.startRecording()
  }

  async stopAudioCall(): Promise<void> {
    this.isContinuousModeActive = false
    this.audioManager.setVADSilenceCallback(undefined)
    this.audioManager.setBargeInCallback(undefined)
    this.audioManager.setMuted(false)

    if (this.voiceState !== 'idle') {
      await this.stopRecording()
    }
    this.isAudioCallActive = false
    this.audioManager.clearQueue()
    this.onAudioPlayCallback?.(false)

    if (this.wsClient) {
      const client = this.wsClient
      if (client.isConnected) {
        setTimeout(() => {
          client.disconnect()
        }, 100)
      } else {
        client.disconnect()
      }
      this.wsClient = null
    }
  }

  // 设置回调函数
  onMessage(callback: (message: WebSocketMessage) => void): void {
    this.onMessageCallback = callback
  }

  onSTTResult(callback: (text: string, isFinal: boolean) => void): void {
    this.onSTTResultCallback = callback
  }

  onLLMStream(callback: (text: string, isComplete: boolean, characterName?: string) => void): void {
    this.onLLMStreamCallback = callback
  }

  onAudioPlay(callback: (isPlaying: boolean) => void): void {
    this.onAudioPlayCallback = callback
  }

  onConnectionStatus(callback: (status: 'connected' | 'disconnected' | 'error', message?: string) => void): void {
    this.onConnectionStatusCallback = callback
  }

  // 获取状态
  get connected(): boolean {
    const isConnected = this.wsClient?.isConnected || false
    console.log('🔍 检查连接状态:', {
      wsClient: !!this.wsClient,
      readyState: this.wsClient?.readyState,
      isConnected: isConnected,
      expectedReadyState: WebSocket.OPEN
    })
    return isConnected
  }

  get audioCallActive(): boolean {
    return this.isAudioCallActive
  }

  get recording(): boolean {
    return this.audioManager.recording
  }

  get playing(): boolean {
    return this.audioManager.playing
  }

  get voiceActive(): boolean {
    return this.audioManager.recording
  }

  get micMuted(): boolean {
    return this.audioManager.muted
  }

  muteMic(): void {
    this.audioManager.setMuted(true)
  }

  unmuteMic(): void {
    this.audioManager.setMuted(false)
  }

  // 清理资源
  destroy(): void {
    console.log('🧹 清理AI对话系统资源')

    void this.stopAudioCall()
    this.wsClient?.disconnect()
    this.audioManager.clearQueue()

    this.wsClient = null
    this.onMessageCallback = undefined
    this.onSTTResultCallback = undefined
    this.onLLMStreamCallback = undefined
    this.onAudioPlayCallback = undefined
    this.onConnectionStatusCallback = undefined
  }

  private async ensureWebSocketConnection(): Promise<void> {
    if (this.wsClient && this.wsClient.isConnected) {
      return
    }

    if (!this.conversationUuid) {
      throw new Error('缺少会话标识，无法建立语音连接')
    }

    await this.connectWebSocket(this.conversationUuid)
  }
}
