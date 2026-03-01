# Android Music Player with VJ

Jetpack ComposeとMedia3 (ExoPlayer)を使用して構築された、インタラクティブなVJ (Visual Jockey) 機能を備えたモダンなAndroidバックグラウンド音楽プレイヤーです。

## 仕様と機能
- **技術スタック**: Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer).
- **最小SDK**: 24 (Android 7.0 Nougat)
- **ターゲットSDK**: 35 (Android 15)
- **主な機能**:
  - フォアグラウンドサービスを使用したバックグラウンド音声再生。
  - ロック画面および通知コントロールのためのメディアセッション統合。
  - 音声と同期したVJ（ビジュアルジョッキー）の視覚化機能。
- **必要な権限**:
  - `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO`: ローカルの音楽ファイルにアクセスするため。
  - `RECORD_AUDIO`: 音声の視覚化処理に必要するため。
  - `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: アプリがバックグラウンドにある間も音楽を再生し続けるため。
  - `POST_NOTIFICATIONS`: メディアコントロールの通知を表示するため。

## 使用方法
### 前提条件
- Android Studio (Koala以降を推奨)
- JDK 11以上
- Android 7.0 (API 24) 以上を実行しているAndroidデバイスまたはエミュレータ

### ビルドと実行
1. このリポジトリをクローンします。
   ```bash
   git clone https://github.com/yourusername/Android_MusicPlayerWithVJ.git
   ```
2. Android Studioでプロジェクトを開きます。
3. Gradleの同期と依存関係のダウンロードを許可します。
4. Androidデバイスを接続するか、エミュレータを起動します。
5. Android Studioの**Run**ボタン（Shift + F10）をクリックして、アプリをインストールします。
6. **メディアファイルに関する注意**: ご自身の `.mp3`, `.wav`, または `.mp4` (VJ用) ファイルは、デバイスのストレージに直接配置してください。

### コミット時の巨大ファイルの管理
VJ機能用の大容量4K背景ループなどの高品質なメディアをプロジェクトディレクトリに直接追加して開発する場合、一般的なGitリポジトリのサイズ制限（GitHubでは通常1ファイルあたり100MB）に注意してください。

巨大なファイルが誤ってコミットされるのを防ぐため、コミット前に同梱のヘルパースクリプトを使用してください。
```powershell
powershell -ExecutionPolicy Bypass -File .\ignore_large_files.ps1
```
このスクリプトは、ディレクトリ内の75MBを超えるファイルをスキャンし、自動的に `.gitignore` に追加します。

## プロジェクト構成
```text
C:\GitHub\Android_MusicPlayerWithVJ\app\src\main\
├── AndroidManifest.xml       # アプリの権限とサービスの宣言
├── java/com/example/android_musicplayerwithvj/
│   ├── MainActivity.kt       # メインとなるUIのエントリーポイント (Jetpack Compose)
│   ├── PlaybackService.kt    # Media3 PlayerとSessionを処理するフォアグラウンドサービス
│   └── ui/theme/             # Jetpack Compose Material 3 テーマ定義
└── res/                      # Androidリソース (drawables, values, mipmap)
```

## ライセンス
このプロジェクトはMITライセンスの下でライセンスされています。詳細については、[LICENSE](LICENSE) ファイルを参照してください（または暗黙的にMITとしてライセンスされます）。

