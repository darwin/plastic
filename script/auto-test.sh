#!/usr/bin/env bash
set -ex

# http://stackoverflow.com/a/32566506/84283
kill_childs() {
        local pid="${1}"
        local self="${2:-false}"

        if children="$(pgrep -P "$pid")"; then
                for child in $children; do
                        kill_childs "$child" true
                done
        fi

        if [ "$self" == true ]; then
                kill -s SIGTERM "$pid" || (sleep 10 && kill -9 "$pid" &)
        fi
}

# http://superuser.com/a/917073/8244
wait_file() {
  local file="$1"; shift
  local wait_seconds="${1:-10}"; shift # 10 seconds as default timeout

  until test $((wait_seconds--)) -eq 0 -o -f "$file" ; do sleep 1; done

  ((++wait_seconds))
}

echo "THIS SCRIPT DOES NOT WORK RELIABLY"
echo "karmas's web server does not wait for cljs compiler to finish writing of files, corrupts its own cache and starts serving corrupted files."


trap 'echo "killing child processes: $(jobs -p)..." && kill_childs $(jobs -p)' EXIT

# ensure we start in project root
cd "$(dirname "${BASH_SOURCE[0]}")"; cd ..

lein clean
lein cljsbuild auto test &

compiler_output="resources/test/.build/test/plastic.js"

wait_file $compiler_output 120 || {
  echo "compiler output file missing after waiting for $? seconds: '$$compiler_output'"
  echo "check for errors/misconfiguration of: lein cljsbuild auto test"
  exit 1
}

../node_modules/karma/bin/karma start ../karma/dev.conf.js