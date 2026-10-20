#!/bin/sh
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    gradle "$@"
fi
