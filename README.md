# 🎵 Android Music Player with VJ

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple.svg)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange.svg)](https://developer.android.com)

> Jetpack Compose と Media3 (ExoPlayer) を使用した、**8種のリアルタイムビジュアライザー**を搭載するモダンな Android 音楽プレイヤーです。

---

## ✨ 主な機能

| カテゴリ | 機能 |
|---|---|
| 🎶 **再生** | バックグラウンド再生、シャッフル、リピート（Off / All / One） |
| 📱 **メディアセッション** | ロック画面・通知からの再生コントロール |
| 🎨 **ビジュアライザー** | 8種のVJスタイル（LIQUID, FLOWER, NEON_WAVES, AURA_HEAT, SPEKTRO, ALCHEMY, SPIKE, BARS） |
| 🎛️ **カスタマイズ** | カラーモード（Single / Colorful）、キックでズーム、背景透過度調整 |
| 📂 **ブラウジング** | 全曲 / フォルダ / アルバム / アーティスト別の閲覧、4種のソート |
| 🌗 **テーマ** | ダークモード / ライトモードの切り替え |
| 🖼️ **背景** | アルバムアート表示、カスタム画像設定 |
| 🌐 **多言語** | 日本語 / 英語対応 |

---

## 🎨 VJスタイル一覧

| スタイル | 概要 |
|---|---|
| **LIQUID** | 音量に応じてアートワーク周囲に流体が広がる |
| **FLOWER** | 音声波形で花びら状に収縮、キックでズーム |
| **NEON_WAVES** | ネオンカラーの波形が横に流れる |
| **AURA_HEAT** | FFTの強度でオーラ状のグラデーションが拡散 |
| **SPEKTRO** | 32バンドの周波数スペクトラム表示 |
| **ALCHEMY** | FFTで変化する回転する錬金術模様 |
| **SPIKE** | 中心から放射状にFFTスパイクが伸びる |
| **BARS** | 下部に並ぶ周波数バー |

---

## 🛠️ 技術スタック

| 技術 | 用途 |
|---|---|
| **Kotlin** | メイン言語 |
| **Jetpack Compose** | 宣言型UI |
| **Material 3** | Material Design 3 テーマ |
| **AndroidX Media3 (ExoPlayer)** | オーディオ再生エンジン |
| **MediaSession** | メディアコントロール統合 |
| **Visualizer API** | FFT / 波形データのキャプチャ |
| **Coil** | アルバムアートの非同期画像読み込み |
| **Accompanist Permissions** | ランタイムパーミッション管理 |

---

## 📋 必要な権限

| 権限 | 用途 |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | 端末内の音楽ファイルへのアクセス |
| `RECORD_AUDIO` | Visualizer APIによる音声波形の取得 |
| `FOREGROUND_SERVICE` | バックグラウンド再生の維持 |
| `POST_NOTIFICATIONS` | メディアコントロール通知の表示 |

---

## 🚀 セットアップ

### 前提条件

- **Android Studio** Koala 以降
- **JDK** 11+
- **Android SDK** 35
- Android 7.0 (API 24) 以上の実機またはエミュレータ

### ビルドと実行

```bash
# 1. リポジトリをクローン
git clone https://github.com/Xenoah/Android_MusicPlayerWithVJ.git
cd Android_MusicPlayerWithVJ

# 2. Android Studio で開き、Gradle Sync を実行

# 3. 実機を接続 or エミュレータを起動し、Run (Shift+F10)
```

> **📌 メディアファイル**: `.mp3`, `.wav` などの音楽ファイルは端末のストレージに直接配置してください。

---

## 📁 プロジェクト構成

```text
app/src/main/
├── AndroidManifest.xml                    # 権限・サービス宣言
├── java/com/example/android_musicplayerwithvj/
│   ├── MainActivity.kt                   # メインUI (Jetpack Compose)
│   ├── MusicViewModel.kt                 # 状態管理・再生制御・Visualizer
│   ├── MusicTrack.kt                     # 楽曲データクラス
│   ├── PlaybackService.kt                # Media3 フォアグラウンドサービス
│   ├── VJCanvas.kt                       # 8種のビジュアライザー描画
│   └── ui/theme/
│       ├── Color.kt                      # カラー定義
│       ├── Theme.kt                      # Material 3 テーマ
│       └── Type.kt                       # タイポグラフィ
└── res/
    ├── values/strings.xml                # 英語リソース
    └── values-ja/strings.xml             # 日本語リソース
```

---

## 🔧 大きなファイルの管理

VJ用の大容量メディアファイル（4K背景動画など）を追加する場合、GitHub の 100MB 制限に注意してください。

同梱の PowerShell スクリプトで、75MB を超えるファイルを自動的に `.gitignore` に追加できます：

```powershell
powershell -ExecutionPolicy Bypass -File .\ignore_large_files.ps1

# カスタムサイズ指定も可能
powershell -ExecutionPolicy Bypass -File .\ignore_large_files.ps1 -MaxSizeMB 50
```

---

## 📝 変更履歴

[CHANGELOG.md](CHANGELOG.md) を参照してください。

---

## 📄 ライセンス

このプロジェクトは [MIT License](LICENSE) の下でライセンスされています。

---

## 🤝 コントリビューション

1. このリポジトリを Fork
2. Feature ブランチを作成 (`git checkout -b feature/amazing-feature`)
3. 変更をコミット (`git commit -m 'Add amazing feature'`)
4. ブランチを Push (`git push origin feature/amazing-feature`)
5. Pull Request を作成
