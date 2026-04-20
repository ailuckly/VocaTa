export interface VoiceTranscriptItem {
  speaker: 'user' | 'ai'
  text: string
  timestamp: number
}
