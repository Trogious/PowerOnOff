#!/bin/sh

if [ $# -ne 1 ]; then
	echo "Usage: mkssl <name>"
	echo
	exit 1
fi

base_dir=/root/SSLs/mkssl
export OPENSSL_CONF=$base_dir/openssl.cnf

server=$1
openssl x509 -req -in $server.csr -CA $base_dir/ca-swmud.crt -CAkey $base_dir/privkey-ca-swmud.key -out $server.crt -days 3650 -sha256 -extfile $base_dir/v3.ext
