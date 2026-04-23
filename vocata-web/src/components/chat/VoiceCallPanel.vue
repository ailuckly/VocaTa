<template>
  <Transition name="voice-panel">
    <div v-if="active" class="immersive-voice-mode">
      <!-- Blurred Background -->
      <div 
        class="immersive-bg"
        :style="{ backgroundImage: `url(${characterAvatar || '/default-bg.png'})` }"
      ></div>

      <!-- Main Visual Array -->
      <div class="voice-center">
        <!-- Avatar Ring that pulses -->
        <div class="avatar-container" :class="{ 'is-speaking': speaking, 'is-muted': muted }">
          <div class="pulse-ring"></div>
          <div class="pulse-ring delay-1"></div>
          <img :src="characterAvatar" class="character-avatar" alt="Character Avatar" />
        </div>
        
        <!-- Status Text -->
        <h2 class="voice-status">{{ status }}</h2>
        <p class="voice-name">{{ characterName }}</p>
      </div>

      <!-- Subtitles Area -->
      <Transition name="fade">
        <div v-if="showSubtitles" class="subtitles-container">
          <div
            v-for="entry in entries.slice(-3)"
            :key="entry.timestamp"
            class="subtitle-row"
            :class="entry.speaker === 'user' ? 'is-user' : 'is-ai'"
          >
            <span class="subtitle-speaker">{{ entry.speaker === 'user' ? '我' : characterName }}:</span>
            <span class="subtitle-text">{{ entry.text }}</span>
          </div>
        </div>
      </Transition>

      <!-- Bottom Controls -->
      <div class="voice-controls">
        <button
          class="control-btn cc-btn"
          :class="{ 'is-active': showSubtitles }"
          @click="showSubtitles = !showSubtitles"
          title="切换字幕"
        >
          <span class="cc-text">CC</span>
        </button>

        <button
          class="control-btn mute-btn"
          :class="{ 'is-muted': muted }"
          @click="$emit('mute')"
          :title="muted ? '取消静音' : '静音'"
        >
          <el-icon v-if="muted"><MicrophoneOff /></el-icon>
          <el-icon v-else><Microphone /></el-icon>
        </button>

        <button
          class="control-btn hangup-btn"
          @click="$emit('hangup')"
          title="结束通话"
        >
          <el-icon><PhoneFilled /></el-icon>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { VoiceTranscriptItem } from '@/types/ui'

defineProps<{
  active: boolean
  entries: VoiceTranscriptItem[]
  speaking: boolean
  muted: boolean
  status: string
  characterName: string
  characterAvatar?: string
}>()

defineEmits<{
  mute: []
  hangup: []
}>()

const showSubtitles = ref(true)
</script>

<style scoped lang="scss">
.immersive-voice-mode {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #111;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  color: white;
}

.immersive-bg {
  position: absolute;
  inset: -10%;
  width: 120%;
  height: 120%;
  background-size: cover;
  background-position: center;
  filter: blur(40px) brightness(0.3);
  z-index: -1;
  pointer-events: none;
}

/* Center visual */
.voice-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.avatar-container {
  position: relative;
  width: 160px;
  height: 160px;
  margin-bottom: 20px;

  &.is-speaking .pulse-ring {
    opacity: 1;
    animation: pulse-out 2s cubic-bezier(0.2, 0.8, 0.2, 1) infinite;
  }
  
  &.is-speaking .pulse-ring.delay-1 {
    animation-delay: 1s;
  }
}

.character-avatar {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  z-index: 2;
  border: 4px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
}

.pulse-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: color-mix(in srgb, var(--vt-brand, #3b82f6) 70%, transparent);
  z-index: 1;
  opacity: 0;
}

@keyframes pulse-out {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.8); opacity: 0; }
}

.voice-status {
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 2px;
  opacity: 0.9;
  margin: 0;
}

.voice-name {
  font-size: 15px;
  font-weight: 300;
  opacity: 0.6;
  margin: 0;
}

/* Subtitles */
.subtitles-container {
  width: 100%;
  max-width: 700px;
  margin: 0 auto 30px;
  padding: 20px 30px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 120px;
  justify-content: flex-end;
}

.subtitle-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 16px;
  line-height: 1.6;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.6);
  
  &.is-user {
    color: rgba(255, 255, 255, 0.7);
    text-align: right;
    .subtitle-speaker {
      display: none;
    }
    .subtitle-text {
      background: rgba(255, 255, 255, 0.1);
      padding: 10px 16px;
      border-radius: 18px 18px 0 18px;
      display: inline-block;
      align-self: flex-end;
      max-width: 80%;
    }
  }
  &.is-ai {
    color: #fff;
    font-size: 18px;
    font-weight: 400;
    
    .subtitle-speaker {
      font-size: 14px;
      opacity: 0.7;
      margin-bottom: 2px;
    }
  }
}

.subtitle-speaker {
  opacity: 0.8;
  font-weight: 600;
}

/* Bottom Controls */
.voice-controls {
  padding: 0 0 60px 0;
  display: flex;
  gap: 36px;
  align-items: center;
}

.control-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  color: white;
  font-size: 24px;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-2px) scale(1.05);
  }

  &.mute-btn.is-muted {
    background: rgba(255, 255, 255, 0.9);
    color: var(--vt-text, #111);
  }

  &.hangup-btn {
    background: #ff4b4b;
    border-color: #ff4b4b;
    width: 76px;
    height: 76px;
    font-size: 32px;
    transform: rotate(135deg);
    
    &:hover {
      background: #ff3333;
      transform: rotate(135deg) scale(1.1);
      box-shadow: 0 0 20px rgba(255, 75, 75, 0.4);
    }
  }

  &.cc-btn {
    width: 52px;
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    
    &.is-active {
      background: rgba(255, 255, 255, 0.9);
      color: var(--vt-text, #111);
    }
  }
}

/* Transitions */
.voice-panel-enter-active,
.voice-panel-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}
.voice-panel-enter-from,
.voice-panel-leave-to {
  opacity: 0;
  transform: translateY(30px) scale(0.98);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
