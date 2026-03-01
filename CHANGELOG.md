# Changelog

すべての重要な変更は、このファイルに記録されます。

## [Unreleased]

### 修正 (Fixed)
- `MusicTrack.artworkUri` が音声ファイルURIを返していた問題を修正。MediaStoreのアルバムアートURIを使用するように変更。
- `MediaController` のgetter が `ExecutionException` でクラッシュする可能性があった問題を修正。
- `Visualizer` の初期化・解放にスレッド安全性がなかった問題を `@Synchronized` で修正。
- `loadMusic()` の `ContentResolver.query()` がメインスレッドで実行されていた問題を `Dispatchers.IO` に移動。
- `SortOrder.DATE_ADDED` がリストを反転するだけだった問題を修正。実際の `DATE_ADDED` フィールドでソートするように変更。
- `TrackItem` の `Modifier` 順序を修正し、選択時の背景色がパディングを含む全体に適用されるように修正。
- `Theme.kt` の Dynamic Color をデフォルトで無効化し、アプリ定義のカラースキームが一貫して使用されるように変更。

### 追加 (Added)
- `PlaybackService.onTaskRemoved()` を実装。アプリがスワイプ終了された際にサービスが適切に停止するように。
- `MusicTrack` に `dateAdded` フィールドを追加。
- 全てのハードコード英語文字列を `strings.xml` に移動し、日本語翻訳を追加。
  - Light/Dark Mode, Visualizer Settings, Zoom on Kick, Background Mode, Select Custom Image, Opacity, All Songs, Select Visualizer
- 75MBを超える大きなファイルが誤ってGitにコミットされるのを防ぐための `ignore_large_files.ps1` スクリプトを追加。
- `.gitignore` を更新し、大容量メディアファイル（`.apk`, `.aab`, `.mp4`, `.mkv`, `.avi`）を除外。
- GitHub向けの日本語 `README.md` を作成。
