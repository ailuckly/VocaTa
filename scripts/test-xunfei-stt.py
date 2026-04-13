#!/usr/bin/env python3
"""
测试讯飞语音听写 WebSocket STT API
用法: python3 test-xunfei-stt.py <APP_ID> <API_KEY> <API_SECRET> [audio_file.pcm]

如果不指定音频文件，会自动生成一段 1.5 秒的 440Hz 测试音（16kHz 16bit mono PCM）
"""

import sys
import hmac
import hashlib
import base64
import json
import time
import struct
import math
import websocket
from datetime import datetime, timezone
from urllib.parse import urlencode
from email.utils import formatdate

def build_url(app_id, api_key, api_secret):
    host = "iat-api.xfyun.cn"
    path = "/v2/iat"
    date = formatdate(usegmt=True)

    signature_origin = f"host: {host}\ndate: {date}\nGET {path} HTTP/1.1"
    signature = base64.b64encode(
        hmac.new(api_secret.encode(), signature_origin.encode(), hashlib.sha256).digest()
    ).decode()

    authorization_origin = (
        f'api_key="{api_key}", algorithm="hmac-sha256", '
        f'headers="host date request-line", signature="{signature}"'
    )
    authorization = base64.b64encode(authorization_origin.encode()).decode()

    params = urlencode({
        "authorization": authorization,
        "date": date,
        "host": host,
    })
    return f"wss://{host}{path}?{params}"


def generate_test_pcm(duration_sec=1.5, sample_rate=16000, freq=440):
    """生成一段正弦波 PCM 测试音频（16bit mono）"""
    n = int(duration_sec * sample_rate)
    samples = []
    for i in range(n):
        v = math.sin(2 * math.pi * freq * i / sample_rate)
        samples.append(int(v * 16383))  # 50% 音量
    return struct.pack(f"<{n}h", *samples)


def send_audio(ws, app_id, pcm_data, chunk_ms=40):
    """按帧发送 PCM 音频数据"""
    chunk_size = int(16000 * 2 * chunk_ms / 1000)  # 16kHz * 16bit * chunk_ms
    total = len(pcm_data)
    offset = 0
    frame_idx = 0

    while offset < total:
        chunk = pcm_data[offset:offset + chunk_size]
        offset += chunk_size
        is_last = offset >= total
        status = 2 if is_last else (1 if frame_idx > 0 else 0)

        frame = {"data": {
            "status": status,
            "format": "audio/L16;rate=16000",
            "encoding": "raw",
            "audio": base64.b64encode(chunk).decode(),
        }}

        if frame_idx == 0:
            frame["common"] = {"app_id": app_id}
            frame["business"] = {
                "language": "zh_cn",
                "domain": "iat",
                "accent": "mandarin",
                "vad_eos": 3000,
                "dwa": "wpgs",
            }

        ws.send(json.dumps(frame))
        frame_idx += 1
        time.sleep(chunk_ms / 1000.0)

    print(f"[发送完毕] 共 {frame_idx} 帧，{total} 字节")


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(1)

    app_id = sys.argv[1]
    api_key = sys.argv[2]
    api_secret = sys.argv[3]
    audio_file = sys.argv[4] if len(sys.argv) > 4 else None

    if audio_file:
        with open(audio_file, "rb") as f:
            pcm_data = f.read()
        print(f"[音频] 读取文件 {audio_file}，大小 {len(pcm_data)} 字节")
    else:
        pcm_data = generate_test_pcm(duration_sec=1.5)
        print(f"[音频] 生成 1.5 秒 440Hz 正弦波测试音，大小 {len(pcm_data)} 字节")

    url = build_url(app_id, api_key, api_secret)
    print(f"[连接] {url[:80]}...")

    results = []
    errors = []
    connected = [False]

    def on_open(ws):
        connected[0] = True
        print("[WebSocket] 连接成功，开始发送音频...")
        import threading
        t = threading.Thread(target=send_audio, args=(ws, app_id, pcm_data))
        t.daemon = True
        t.start()

    def on_message(ws, msg):
        try:
            data = json.loads(msg)
            code = data.get("code", -1)
            if code != 0:
                err = f"API错误 code={code}: {data.get('message', '')}"
                print(f"[错误] {err}")
                errors.append(err)
                return

            ws_items = data.get("data", {}).get("result", {}).get("ws", [])
            text = "".join(
                cw.get("w", "")
                for item in ws_items
                for cw in item.get("cw", [])
            )
            status = data.get("data", {}).get("status", -1)
            if text:
                results.append(text)
                final = "【最终】" if status == 2 else "【中间】"
                print(f"[识别] {final} {text}")

            if status == 2:
                print("[完成] 讯飞 STT 服务正常，识别完毕")
                ws.close()
        except Exception as e:
            print(f"[解析错误] {e}: {msg}")

    def on_error(ws, err):
        print(f"[WebSocket错误] {err}")
        errors.append(str(err))

    def on_close(ws, code, reason):
        print(f"[关闭] code={code} reason={reason}")

    ws = websocket.WebSocketApp(
        url,
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close,
    )

    ws.run_forever(ping_interval=0)

    print("\n===== 测试结果 =====")
    if errors:
        print(f"❌ 失败: {errors}")
    elif not connected[0]:
        print("❌ 连接失败（URL 认证错误或网络问题）")
    elif results:
        print(f"✅ 成功，识别文本: {''.join(results)}")
        print("   （测试音是正弦波，无实际文字，结果为空也属正常）")
    else:
        print("✅ 连接和认证成功（测试音无文字内容，识别结果为空）")


if __name__ == "__main__":
    main()
