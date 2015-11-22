#!/bin/bash
usage () {
	echo "Usage: <routers_host> <nse_port>" >& 2
	exit 1
}

if [ $# -ne 2 ]; then
	usage
fi

./nse-linux386 "$1" $2