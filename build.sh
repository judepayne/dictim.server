#!/bin/bash

containerprog=${1:-docker}

portfile="resources/PORT"

port=$(cat "$portfile")

$containerprog build --build-arg="PORT=$port" -t dictim-server .
