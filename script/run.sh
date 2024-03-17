#!/bin/bash

set -e

containerprog=${1:-docker}

portfile="resources/PORT"

port=$(cat "$portfile")

$containerprog run --env-file docker/tala.env -dp 127.0.0.1:$port:$port dictim-server
