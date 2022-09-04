#!/bin/sh

if [ $# -ne 1 ]; then
	echo "Usage: mkcsr <name>"
	echo
	exit 1
fi

base_dir=/root/SSLs/mkssl
export OPENSSL_CONF=$base_dir/openssl.cnf

server=${1}
keylen=2048

openssl genrsa -out $server.key $keylen
chmod 400 $server.key

openssl req -new -nodes -key $server.key -out $server.csr


