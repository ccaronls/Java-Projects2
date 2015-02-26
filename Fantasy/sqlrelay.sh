#! /bin/sh

sqlr-stop

sudo cp conf/sqlrelay.conf /usr/local/etc/ || exit 1

sqlr-start -id dbtest || exit 1
