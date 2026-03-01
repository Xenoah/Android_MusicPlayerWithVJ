# Changelog

すべての重要な変更は、このファイルに記録されます。

## [Unreleased]
### 追加 (Added)
- 75MBを超える大きなファイルが誤ってGitにコミットされるのを防ぐための `ignore_large_files.ps1` スクリプトを追加。
- `.gitignore` を更新し、標準的なAndroidのビルド出力と、大容量になりがちなメディアファイル（`.apk`, `.aab`, `.mp4`, `.mkv`, `.avi`）を除外するように設定。
- プロジェクトの仕様、必要な権限、使用方法、ファイル構成をドキュメント化した日本語版の `README.md` を作成。

### 変更 (Changed)
- 初期のAndroid Music Player with VJアプリの基本構造を設定（Jetpack Compose UI、Media3バックグラウンド再生、MediaSession連携、VJ視覚化準備）。
