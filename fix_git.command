#!/bin/bash
set -e
cd /Users/goran/dev/worklog

echo "=== Git cleanup script ==="

# Ta bort index.lock om den finns
rm -f .git/index.lock && echo "Removed stale index.lock" || true

# Rensa stale worktree-poster
echo "Pruning stale worktrees..."
git worktree prune --verbose

# Ta bort .claude/worktrees/ ur indexet
echo "Removing .claude/worktrees/ from git index..."
git rm -r --cached .claude/worktrees/ 2>/dev/null && echo "Removed from index." || echo "(not tracked, skipping)"

# Stega in .gitignore-ändringen och committa
echo "Committing..."
git add .gitignore
git commit -m "chore: untrack .claude/worktrees/, add to .gitignore"

# Pull --rebase
echo "Pulling with rebase..."
git pull --rebase

# Push
echo "Pushing..."
git push

echo ""
echo "=== All done! ==="
read -rp "Press Enter to close..."
