# MyAndroidLibrary

## 他のGradleプロジェクトからの使用方法

1. このリポジトリをClone

2. プロジェクトの`settings.gradle`にimportするモジュールのパスを指定する
```groovy
include ':widget'
project(':widget').projectDir = new File(settingsDir, '${path2project}/library/widget')
```

3. コードを使用したいモジュールの`build.gradle`に依存を追加する

たいていは`app`モジュール

```groovy
    implementation project(':widget')
```

## モジュールの紹介

### `widget`モジュール

`library/widget`  

カスタムViewを集めた

- HorizontalListView  
  RecyclerViewを拡張した横方向にスクロールするListViewのようにふるまうwidget
- CustomNumberPicker  
  NumberPickerを拡張して負数も表示・選択できるwidget
- FloatPicker  
  小数を表示・選択できるwidget
- ExpandableTextView  
  表示する文字列の長さ・viewの横幅に合わせて`textScaleX`を自動調節するwidget

### `diagram`モジュール

`library/diagram`

平面図形を計算するためのツールキット

- 基本的な図形の計算
- ドロネー分割
- ボロノイ分割