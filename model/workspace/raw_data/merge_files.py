import pandas as pd
import glob

# 統合するCSVファイルのパスを取得
file_paths = glob.glob('./wthor_*.csv')

# 空のデータフレームを作成
merged_df = pd.DataFrame()

# 各ファイルを読み込み、ヘッダー以外のデータを追加
for i, file_path in enumerate(file_paths):
    df = pd.read_csv(file_path)
    if i == 0:
        merged_df = df
    else:
        merged_df = pd.concat([merged_df, df[1:]], ignore_index=True)

# 統合されたデータを新しいCSVファイルに保存
merged_df.to_csv('./wthor.csv', index=False)