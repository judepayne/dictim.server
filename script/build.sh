#!/bin/bash

set -e

containerprog=${1:-docker}

portfile="resources/PORT"

port=$(cat "$portfile")

sslportfile="resources/SSLPORT"

sslport=$(cat "$sslportfile")

$containerprog build \
	       --build-arg="PORT=$port" \
	       --build-arg="SSLPORT=$sslport" \
	       -t dictim-server .
