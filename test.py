import socket
import threading
import ast
import sys
import numpy as np
import random
# from tensorflow.python.keras.models import load_model


def handle_client(client_socket):
    while True:
        try:
            # クライアントからのメッセージを受け取る
            message = client_socket.recv(8192).decode('utf-8')
            if not message:
                break
            elif message == "CLOSE\n":
                # プロセスを終了する
                sys.exit()
            else:
                board = string_to_array(message)
                # print(bord)
                # クライアントにメッセージを送信
                client_socket.send((array_to_string(get_best_move_dummy()) + "\n").encode('utf-8'))
        except:
            break
    # 接続を閉じる
    client_socket.close()

def string_to_array(s):
    array = ast.literal_eval(s)
    return array

# int[8][8] -> "[[0,0,0,0,0,0,0,0],[1,1,1,1,1,1,1,1], ... ,[7,7,7,7,7,7,7,7]"
def array_to_string(a):
    return "[" + ",".join("[" + ",".join(str(cell) for cell in row) + "]" for row in a) + "]"

# 盤面配列を白と黒のそれぞれの盤面のnumpy配列に変換
def get_board_array(board):
    board_array = np.array(board)
    board_array_white = np.where(board_array == 1, 1, 0)
    board_array_black = np.where(board_array == -1, 1, 0)
    
    # 三次元配列に変換する
    board_data = np.array([
        [board_array_white],
        [board_array_black]
    ], dtype=np.int8)
    
    return board_data


# 仮で最も確率が高い手を渡す、8x8の配列に高確率な順に1~64のランダムな整数値を入れる
def get_best_move_dummy():
    # 8x8の配列を作成し、ランダムな1から64の整数で初期化する
    board_data = [[random.randint(1, 64) for _ in range(8)] for _ in range(8)]
    return board_data

# def get_best_move(board):
#     board_data = get_board_array(board)
#     model = load_model("mymodel1")

#     return model.predict(board_data)
    



def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(("localhost", 12345))
    server.listen(5)
    print("Server started on port 12345")

    while True:
        client_socket, addr = server.accept()
        print(f"Connection from {addr}")

        # 新しいスレッドを作成してクライアントを処理
        client_handler = threading.Thread(target=handle_client, args=(client_socket,))
        client_handler.start()

if __name__ == "__main__":
    main()