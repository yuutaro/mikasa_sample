const fs = require('fs');

// ファイルのパスを指定
const inputFilePath = 'requirements.txt';
const outputFilePath = 'converted_requirements.txt';

// ファイルを読み込んで処理する関数
fs.readFile(inputFilePath, 'utf8', (err, data) => {
  if (err) {
    console.error(err);
    return;
  }

  // 各行を配列に分割
  const lines = data.trim().split('\n');

  // 変換後の文字列を格納する配列
  const convertedLines = [];

  // 各行を処理して変換
  lines.forEach(line => {
    // 行をスペースで分割して、ライブラリ名とバージョンを取得
    const [library, version] = line.split(/\s+/);

    // 変換後の形式で配列に追加
    convertedLines.push(`${library}==${version}`);
  });

  // 変換後の配列をファイルに書き込む
  fs.writeFile(outputFilePath, convertedLines.join('\n'), 'utf8', (err) => {
    if (err) {
      console.error(err);
      return;
    }
    console.log(`変換後の内容が ${outputFilePath} に書き込まれました。`);
  });
});