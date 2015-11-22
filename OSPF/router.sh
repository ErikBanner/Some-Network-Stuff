#!/bin/bash

usage () {
	echo "Usage: <router_id> <nse_host> <nse_port> <router_port>" >& 2
	exit 1
}

if [ $# -ne 4 ]; then
	usage
fi

java router $1 "$2" $3 $4