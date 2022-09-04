#!/usr/local/bin/python3
import getpass
import json
import logging
import os
import platform
import socket
import ssl
import subprocess
import sys
from logging.handlers import RotatingFileHandler
from threading import Lock, Thread

POOFF_HOST = '0.0.0.0'
POOFF_PORT = 13013
POOFF_BACKLOG = 5
POOFF_RECV_SIZE = 512
POOFF_CURRENT_OS = platform.system().lower()
POOFF_SUPPORTED_OS = ['linux', 'darwin', 'windows']
POOFF_LOGGER_NAME = 'pooff_logger'
POOFF_BUF_LEN = 1024
pooff_logger_lock = Lock()


def getenv_path(name, default=None):
    value = os.getenv(name, default)
    if value and value.startswith('./'):
        return os.path.join(os.getcwd(), value[2:])
    return value


if 'windows' == POOFF_CURRENT_OS:
    POOFF_PARENT = os.getenv('POOFF_PARENT', 'C:\\Users\\' + getpass.getuser() + '\\Desktop\\poweronoffd')
else:
    POOFF_PARENT = getenv_path('POOFF_PARENT', './')
CERT_DIR = 'cert'
POOFF_CERT_FILE = os.path.join(POOFF_PARENT, CERT_DIR, 'server.crt')
POOFF_CERT_KEY = os.path.join(POOFF_PARENT, CERT_DIR,  'server.key')
POOFF_CA_FILE = os.path.join(POOFF_PARENT, CERT_DIR, 'ca.crt')


class Logger:
    logger = None

    @staticmethod
    def get_logger(path=getenv_path('POOFF_LOG_PATH', './pooff.log'), level=logging.INFO, max_bytes=204800,
                   backup_count=4):
        if Logger.logger is None:
            Logger.logger = logging.getLogger(POOFF_LOGGER_NAME)
            Logger.logger.setLevel(level)
            handler = RotatingFileHandler(path, maxBytes=max_bytes, backupCount=backup_count)
            handler.setFormatter(logging.Formatter(
                '%(asctime)s|%(levelname)s|%(lineno)d|%(message)s', '%Y-%m-%d %H:%M:%S'))
            Logger.logger.addHandler(handler)
            # logging.getLogger("asyncio").addHandler(handler)
        return Logger.logger


def error(log_item):
    with pooff_logger_lock:
        Logger.get_logger().error(log_item)


def info(log_item):
    with pooff_logger_lock:
        Logger.get_logger().info(log_item)


class Runner(Thread):
    def __init__(self, commands_with_args):
        Thread.__init__(self)
        self.commands = commands_with_args

    @staticmethod
    def read_output(process, output, log_fun):
        line = b''
        while True:
            buf = os.read(output.fileno(), POOFF_BUF_LEN)
            if buf == b'' and process.poll() is not None:
                break
            i = buf.find(b'\n')
            if i >= 0:
                line_with_buf = line + buf[:i+1]
                log_buf = line_with_buf.decode('utf8').strip()
                if log_buf:
                    log_fun(log_buf)
                line = buf[i+1:]
            else:
                j = buf.find(b'\r')
                if j >= 0:
                    time_bytes = line + buf[:j+1]
                    log_buf = time_bytes.decode('utf8').strip()
                    if log_buf:
                        log_fun(log_buf)
                    line = buf[j+1:]
                else:
                    line += buf

    def run(self):
        info(self.commands)
        for cmd in self.commands:
            try:
                with subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE) as p:
                    Runner.read_output(p, p.stdout, info)
                    Runner.read_output(p, p.stderr, error)
                    p.communicate()

                # subprocess.call(cmd)
            except Exception as e:
                error(e.strerror)


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
            Runner([['/sbin/shutdown', '-c'], ['/sbin/shutdownx', '-h', '-P', str(time_int)]]).start()
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
    info(data)
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
    info(platform.system())
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((POOFF_HOST, POOFF_PORT))
    s.listen(POOFF_BACKLOG)
    while True:
        client, address = s.accept()
        info('accepted: ' + str(address))
        try:
            ssl_client = ssl.wrap_socket(client, server_side=True, certfile=POOFF_CERT_FILE, keyfile=POOFF_CERT_KEY,
                                         ssl_version=ssl.PROTOCOL_TLSv1_2, cert_reqs=ssl.CERT_OPTIONAL, ca_certs=POOFF_CA_FILE)
        except ssl.SSLError as e:
            error('no client cert: ' + e.strerror)
            client.close()
            continue
        except ConnectionResetError as e:
            error('conn reset: ' + e.strerror)
            client.close()
            continue
        client = None
        data = ssl_client.recv(POOFF_RECV_SIZE)
        while data is not None and len(data) > 0:
            response = process_request(data)
            if response is not None:
                info(response)
                ssl_client.sendall(response.encode('utf8'))
            data = ssl_client.recv(POOFF_RECV_SIZE)


run()
