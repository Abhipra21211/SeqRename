#!/bin/bash
cd /var/www/html/SeqRename
while true; do
  inotifywait -r -e modify,create,delete,move \
    --exclude '(\.git|android/app/build|node_modules)' \
    www android *.json *.ts README.md 2>/dev/null
  sleep 5
  if [[ -n $(git status --porcelain) ]]; then
    git add .
    git commit -m "auto: $(date '+%Y-%m-%d %H:%M:%S')"
    git push origin main
  fi
done
