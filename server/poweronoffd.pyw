#!/usr/local/bin/python3
import json
import socket
import ssl
import subprocess
import platform
import sys
from threading import Thread, Lock

POOFF_HOST = '0.0.0.0'
POOFF_PORT = 13013
POOFF_BACKLOG = 5
POOFF_RECV_SIZE = 512
POOFF_PARENT = 'C:\\Users\\User\\Desktop\\poweronoffd'
POOFF_CERT_FILE = POOFF_PARENT + '\\server.pem'
POOFF_CERT_KEY = POOFF_PARENT + '\\server.key'
POOFF_CA_FILE = POOFF_PARENT + '\\ca.pem'
POOFF_CURRENT_OS = platform.system().lower()
POOFF_SUPPORTED_OS = ['linux', 'darwin', 'windows']
pooff_stderr_lock = Lock()
pooff_stderr = sys.stderr


def log(log_item):
    with pooff_stderr_lock:
        return
        if not pooff_stderr.closed:
            pooff_stderr.write(str(log_item) + '\n')
            pooff_stderr.flush()


class Runner(Thread):
    def __init__(self, commands_with_args):
        Thread.__init__(self)
        self.commands = commands_with_args

    def run(self):
        log(self.commands)
        for cmd in self.commands:
            try:
                subprocess.call(cmd)
            except Exception as e:
                log(e.strerror)


def get_json_response(result, request_id):
    json_obj = {}
    json_obj['jsonrpc'] = '2.0'
    json_obj['id'] = request_id
    json_obj['result'] = result
    return json_obj


def get_json_response_poweroff_ok(message, request_id):
    return get_json_response({'msg': message}, request_id)


def execute_poweroff(time_int):
    if 'linux' == POOFF_CURRENT_OS:
        if time_int < 1:
            Runner([['/sbin/shutdown', '-c'], ['/sbin/poweroff']]).start()
        else:
            Runner([['/sbin/shutdown', '-c'], ['/sbin/shutdown', '-h', '-P', str(time_int)]]).start()
    elif 'darwin' == POOFF_CURRENT_OS:
        if time_int < 1:
            Runner([['pkill', 'shutdown'], ['/sbin/shutdown', '-h', 'now']]).start()
        else:
            Runner([['pkill', 'shutdown'], ['/sbin/shutdown', '-h', '+' + str(time_int)]]).start()
    elif 'windows' == POOFF_CURRENT_OS:
        if time_int < 1:
            Runner([['shutdown', '/a'], ['shutdown', '/f', '/s', '/t', '0']]).start()
        else:
            Runner([['shutdown', '/a'], ['shutdown', '/f', '/s', '/t', str(time_int*60)]]).start()


def get_json_response_restart_minidlna_ok(message, request_id):
    return get_json_response({'msg': message}, request_id)


def execute_restart_minidlna():
    Runner([['/etc/init.d/minidlna_restart']]).start()


def prepare_response(response_json):
    response = json.dumps(response_json)
    encodedResponse = response.encode('utf8')
    respLen = len(encodedResponse)
    fullContent = 'Content-Length: ' + str(respLen) + "\r\n\r\n" + response
    return fullContent


def process_request(data):
    fullContent = None
    data = data.decode('utf8')
    log(data)
    req = json.loads(data)
    keys = req.keys()
    if 'jsonrpc' in keys and req['jsonrpc'] == '2.0' and 'method' in keys and 'id' in keys:
        method = req['method']
        if 'poweroff' == method:
            if POOFF_CURRENT_OS not in POOFF_SUPPORTED_OS:
                response_json = get_json_response_poweroff_ok('unsupported OS: ' + POOFF_CURRENT_OS, req['id'])
                fullContent = prepare_response(response_json)
            elif 'params' in keys and 'time' in req['params']:
                time_str = req['params']['time']
                time_int = 5
                try:
                    time_int = int(time_str)
                except:
                    pass
                if time_int in [0, 5, 10, 20, 30, 40, 50, 60, 90, 120, 150, 180]:
                    response_json = get_json_response_poweroff_ok('shutting down in: ' + str(time_int), req['id'])
                    fullContent = prepare_response(response_json)
                    execute_poweroff(time_int)
        elif 'reminidlna' == method:
            response_json = get_json_response_restart_minidlna_ok('restarting minidlna', req['id'])
            fullContent = prepare_response(response_json)
            execute_restart_minidlna()
        elif 'x' == method:
            pass
    return fullContent


def run():
    log(platform.system())
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((POOFF_HOST, POOFF_PORT))
    s.listen(POOFF_BACKLOG)
    while True:
        client, address = s.accept()
        log('accepted: ' + str(address))
        try:
            ssl_client = ssl.wrap_socket(client, server_side=True, certfile=POOFF_CERT_FILE, keyfile=POOFF_CERT_KEY, ssl_version=ssl.PROTOCOL_TLSv1_2, cert_reqs=ssl.CERT_OPTIONAL, ca_certs=POOFF_CA_FILE)
        except ssl.SSLError as e:
            log('no client cert: ' + e.strerror)
            client.close()
            continue
        except ConnectionResetError as e:
            log('conn reset: ' + e.strerror)
            client.close()
            continue
        client = None
        data = ssl_client.recv(POOFF_RECV_SIZE)
        while data is not None and len(data) > 0:
            response = process_request(data)
            if response is not None:
                log(response)
                ssl_client.sendall(response.encode('utf8'))
            data = ssl_client.recv(POOFF_RECV_SIZE)

run()
