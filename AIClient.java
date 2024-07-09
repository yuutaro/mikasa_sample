import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AIClient {
  final static int BLACK = 1;
  final static int WHITE = -1;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  private Socket pySocket;
  private PrintWriter pyOut;
  private BufferedReader pyIn;

  private int myColor;
  private int currentTurn;
  private int[][] board = new int[8][8];

  Random random = new Random();

  public AIClient(String serverAddress, int serverPort) {
    try {
      // オセロゲームサーバーとの通信用ソケットとIO
      socket = new Socket(serverAddress, serverPort);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out.println("NICK 6322087_Mikasa");
    } catch (Exception e) {
      System.out.println("オセロサーバーに接続できませんでした。");
      System.exit(1);
    }

    try {
      // Pythonプロセス起動
      ProcessBuilder pb = new ProcessBuilder("python3", "test.py");
      Process process = pb.start();

      // Pythonプロセスとの通信用ソケットとIO
      boolean connected = false;
      while (!connected) {
        try {
          pySocket = new Socket("localhost", 12345);
          connected = true;
        } catch (Exception e) {
          System.out.println("Pythonプロセスとの接続に失敗しました。再接続を試みます。");
          Thread.sleep(1000);
        }
      }
      pyOut = new PrintWriter(pySocket.getOutputStream(), true);
      pyIn = new BufferedReader(new InputStreamReader(pySocket.getInputStream()));
      System.out.println("Pythonプロセスとの接続が確立されました。");
    } catch (Exception e) {
      System.out.println("Pythonプロセス立ち上げ失敗");
      System.exit(1);
    }

    // サーバーからのオセロ対戦通信受信部分追加
    new Thread(new Runnable() {
      public void run() {
        try {
          String line;
          while ((line = in.readLine()) != null) {
            handleServerMessage(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();

    new Thread(new Runnable() {
      public void run() {
        try {
          String line;
          while ((line = pyIn.readLine()) != null) {
            handlePyServerMessage(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();

    // 定期的にpythonプロセスに"hello"を送信
    // new Thread(new Runnable() {
    //   public void run() {
    //     while (true) {
    //       try {
    //         Thread.sleep(1000);
    //         sayHello();
    //       } catch (InterruptedException e) {
    //         e.printStackTrace();
    //       }
    //     }
    //   }
    // }).start();

  }

  private void handleServerMessage(String message) {
    String command = message.split(" ")[0];

    switch (command) {
      case "START":
        String[] startTokens = message.split(" ");
        myColor = Integer.parseInt(startTokens[1]);
        System.out.println("自分の色: " + (myColor == BLACK ? "BLACK" : "WHITE") + "\n");
        break;

      case "BOARD":
        String[] boardTokens = message.split(" ");
        for (int i = 1; i < boardTokens.length; i++) {
          board[(i - 1) / 8][(i - 1) % 8] = Byte.parseByte(boardTokens[i]);
        }
        System.out.println("現在の盤面\n");
        for (int i = 0; i < 8; i++) {
          for (int j = 0; j < 8; j++) {
            if (board[i][j] == BLACK) {
              System.out.print("○ ");
            } else if (board[i][j] == WHITE) {
              System.out.print("● ");
            } else {
              System.out.print("- ");
            }
          }
          System.out.print("\n");
        }
        System.out.println();
        break;

      case "TURN":
        int turn = Integer.parseInt(message.split(" ")[1]);
        currentTurn = turn;
        System.out.println("現在のターン: " + (currentTurn == BLACK ? "BLACK" : "WHITE") + "\n");

        // 自分のターンのときに石を置く処理を以下に記載
        if (currentTurn == myColor) {
          sendBoardToPython(board);
          // int[] selectedMove = randomPut(board, myColor);
          // if (selectedMove != null) {
          //   out.println("PUT " + selectedMove[0] + " " + selectedMove[1]);
          // }
        }
        break;

      case "SAY":
        System.out.println("メッセージ" + message.substring(4) + "\n");
        break;

      case "ERROR":
        String errorCode = message.split(" ")[1];
        switch (errorCode) {
          case "1":
            System.out.println("書式が間違っています。\n");
            break;
          case "2":
            System.out.println("選択した番目には自身の色の石は置けません。\n");
            break;
          case "3":
            System.out.println("相手のターンです。\n");
            break;
          case "4":
            System.out.println("処理できない命令です。\n");
            break;
          default:
            System.out.println("未知のエラーが発生しました。\n");
            break;
        }
        break;

      case "NICK":
        String nickname = message.substring(5);
        System.out.println("ニックネームが" + nickname + "に設定されました。\n");
        break;

      case "END":
        System.out.println("ゲーム終了: " + message.substring(4) + "\n");
        System.exit(0);
        break;

      case "CLOSE":
        System.out.println("対戦相手とサーバーとの接続が切断されました。\n");
        break;

      default:
        System.out.println("不明なメッセージが送られてきました。\n");
        System.out.println(message + "\n");
        break;
    }
  }

  private void handlePyServerMessage(String message) {
    if (message == null) {
      System.out.println("Pythonプロセスからのメッセージがnullです。\n");
      return;
    } else {
      // System.out.println("Python Process: " + message + "\n");
      int[][] probabilityBoard = stringToBoard(message);
      // System.out.println(probabilityBoard);
      int[] bestMove = getBestMove(probabilityBoard, board);
      if (bestMove != null) {
        System.out.println("選択された位置: (" + bestMove[0] + ", " + bestMove[1] + ")");
        out.println("PUT " + bestMove[0] + " " + bestMove[1]);
      }

    }
  }

  // 石が置けるかどうかを判定する関数
  private boolean isValidMove(int[][] board, int x, int y, int color) {
    // すでに石が置かれている場所には置けない
    if (board[x][y] != 0) {
      return false;
    }

    int opponent = -color;

    // 8方向をチェック
    int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

    for (int[] dir : directions) {
      int dx = dir[0];
      int dy = dir[1];
      int nx = x + dx;
      int ny = y + dy;

      // 隣が相手の石かチェック
      if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8 && board[nx][ny] == opponent) {
        nx += dx;
        ny += dy;
        // その方向にさらに進む
        while (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
          if (board[nx][ny] == color) {
            // 自分の石で挟めていればtrue
            return true;
          } else if (board[nx][ny] == 0) {
            // 空白マスに到達したらこの方向は無効
            break;
          }
          nx += dx;
          ny += dy;
        }
      }
    }

    // どの方向でも挟めなかった
    return false;
  }

  // 全マスをチェックして置ける場所を二次元配列で返す関数
  private int[][] getValidMoves(int[][] board, int color) {
    int[][] validMoves = new int[8][8];

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        if (isValidMove(board, i, j, color)) {
          validMoves[i][j] = 1;
        }
      }
    }

    return validMoves;
  }

  // とりあえずランダムに石を置く関数
  private int[] randomPut(int[][] board, int color) {
    int[][] validMoves = getValidMoves(board, color);
    ArrayList<int[]> movesList = new ArrayList<>();

    // デバッグ用の表示と有効な手のリスト作成
    System.out.println("置ける位置:");
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        System.out.print(validMoves[i][j] + " ");
        if (validMoves[i][j] == 1) {
          movesList.add(new int[] { i, j });
        }
      }
      System.out.println();
    }
    System.out.println();

    // 有効な手がない場合
    if (movesList.isEmpty()) {
      System.out.println("置ける位置がありません。");
      return null;
    }

    // ランダムに選択
    Random random = new Random();
    int randomIndex = random.nextInt(movesList.size());
    int[] selectedMove = movesList.get(randomIndex);

    System.out.println("選択された位置: (" + selectedMove[0] + ", " + selectedMove[1] + ")");

    return selectedMove;
  }

  // int [8][8] -> "[[0,0,0,0,0,0,0,0],[1,1,1,1,1,1,1,1], ... , [7,7,7,7,7,7,7,7]]"
  private String boardToString(int[][] board) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < 8; i++) {
      sb.append("[");
      for (int j = 0; j < 8; j++) {
        sb.append(board[i][j]);
        if (j != 7) {
          sb.append(",");
        }
      }
      sb.append("]");
      if (i != 7) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  // "[[0,0,0,0,0,0,0,0],[1,1,1,1,1,1,1,1], ... , [7,7,7,7,7,7,7,7]]" -> int [8][8]
  private int[][] stringToBoard(String boardString) {
    int[][] board = new int[8][8];
    
    // 外側の角括弧を削除
    boardString = boardString.substring(2, boardString.length() - 2);
    
    // 各行を分割
    String[] rows = boardString.split("\\],\\[");
    
    for (int i = 0; i < 8; i++) {
        // 各行の要素をカンマで分割
        String[] cells = rows[i].split(",");
        for (int j = 0; j < 8; j++) {
            board[i][j] = Integer.parseInt(cells[j].trim());
        }
    }
    
    return board;
  }

  // Pythonプロセスに盤面を送信する関数
  private void sendBoardToPython(int[][] board) {
    pyOut.println(boardToString(board));
  }

  // 確率配列と盤面を受け取り、置ける場所の中から最も高い確率のマスを返す関数（数字が小さいほど確率が高い）
  private int[] getBestMove(int[][] probabilityBoard, int[][] board) {
    int[] bestMove = null;
    int bestProbability = Integer.MAX_VALUE;

    int[][] validMoves = getValidMoves(board, myColor);

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        if (validMoves[i][j] == 1 && probabilityBoard[i][j] < bestProbability) {
          bestMove = new int[] { i, j };
          bestProbability = probabilityBoard[i][j];
        }
      }
    }

    return bestMove;
  }

  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Usage: java AIClient <server address> <server port>");
      System.exit(1);
    }
    new AIClient(args[0], Integer.parseInt(args[1]));
  }
}
