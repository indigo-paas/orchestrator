# Replace env variables with ${VAR} format

compgen -e | xargs -I @ sh -c 'printf "s|\${%q}|%q|g\n" "@" "$@"' | sed -f /dev/stdin /docker-entrypoint-initdb.d/init.sql
