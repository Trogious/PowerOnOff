#!/bin/sh

if [ $# -ne 3 ]; then
	echo "Usage: mkjks <cert> <keystore> <password>"
	echo
	exit 1
fi

client=$1
store=$2
pass=$3
rm -i $store
openssl pkcs12 -export -in $client.crt -inkey $client.key -out $client.p12 -name client -CAfile ca.crt -caname root -password pass:$pass
keytool -importkeystore -destkeystore $store -deststorepass $pass -deststoretype pkcs12 -srckeystore $client.p12 -srcstorepass $pass -srcstoretype pkcs12 -alias client
keytool -import -trustcacerts -alias root -file ca.crt -keystore $store -deststoretype pkcs12 -deststorepass $pass
keytool -list -keystore $store -storepass $pass
