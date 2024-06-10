import socket
import struct
import signal
import sys

class Request(object):
    def __init__(self, tml, op_code, operand_one, operand_two, request_id, op_name_length, op_name):
        self.tml = tml
        self.op_code = op_code
        self.operand_one = operand_one
        self.operand_two = operand_two
        self.request_id = request_id
        self.op_name_length = op_name_length
        self.op_name = op_name

class Response(object):
    def __init__(self, tml, result, error_code, request_id):
        self.tml = tml
        self.result = result
        self.error_code = error_code
        self.request_id = request_id

def read_request(conn):
    tml = struct.unpack('!B', conn.recv(1))[0]
    op_code = struct.unpack('!B', conn.recv(1))[0]
    operand_one = struct.unpack('!i', conn.recv(4))[0]
    operand_two = struct.unpack('!i', conn.recv(4))[0]
    request_id = struct.unpack('!H', conn.recv(2))[0]
    op_name_length = struct.unpack('!B', conn.recv(1))[0]
    op_name_bytes = conn.recv(op_name_length * 2)
    op_name = op_name_bytes.decode('utf-16-be')  # Changed from utf-16 to utf-16-be

    print("\nRequest ID: {}".format(request_id))
    print("Operation: {}".format(op_name))
    print("Operand 1: {}".format(operand_one))
    print("Operand 2: {}".format(operand_two))

    return Request(tml, op_code, operand_one, operand_two, request_id, op_name_length, op_name)

def process_request(request):
    result = 0
    error = False

    if request.op_code == 0:
        result = request.operand_one + request.operand_two
    elif request.op_code == 1:
        result = request.operand_one - request.operand_two
    elif request.op_code == 2:
        result = request.operand_one | request.operand_two
    elif request.op_code == 3:
        result = request.operand_one & request.operand_two
    elif request.op_code == 4:
        if request.operand_two == 0:
            error = True
        else:
            result = request.operand_one // request.operand_two  # Changed to integer division
    elif request.op_code == 5:
        result = request.operand_one * request.operand_two
    else:
        error = True

    if request.tml != 13 + request.op_name_length:
        error = True

    error_code = 127 if error else 0

    return Response(8, result, error_code, request.request_id)

def send_response(conn, response):
    conn.sendall(struct.pack('!B', response.tml))
    conn.sendall(struct.pack('!i', response.result))
    conn.sendall(struct.pack('!B', response.error_code))
    conn.sendall(struct.pack('!H', response.request_id))

def log_response(response):
    print("\nResponse ID: {}".format(response.request_id))
    print("Result: {}".format(response.result))
    print("Error Code: {}".format("OK" if response.error_code == 0 else "Error"))

def main(port):
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', port))
    server_socket.listen(5)
    print("\nServer running on port: {}".format(port))

    def signal_handler(sig, frame):
        print("\nShutting down server gracefully...")
        server_socket.close()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    while True:
        try:
            conn, addr = server_socket.accept()
            print("\nClient connected: {}".format(addr[0]))
            try:
                request = read_request(conn)
                response = process_request(request)
                send_response(conn, response)
                log_response(response)
            except Exception as e:
                print("Error: {}".format(e))
            finally:
                conn.close()
        except socket.error:
            print("Server socket closed")
            break

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python server.py <port>")
        sys.exit(1)

    port = int(sys.argv[1])
    main(port)
