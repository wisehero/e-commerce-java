#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <PR URL 또는 번호>" >&2
  exit 64
fi

pr_ref="$1"

read -r is_draft state url < <(
  gh pr view "$pr_ref" \
    --json isDraft,state,url \
    --jq '[.isDraft, .state, .url] | @tsv'
)

if [[ "$state" != "OPEN" ]]; then
  echo "PR이 OPEN 상태가 아닙니다: state=$state url=$url" >&2
  exit 1
fi

if [[ "$is_draft" == "true" ]]; then
  # 서버 백스톱과 동시에 전환해 한쪽이 먼저 성공해도 최종 상태로 판정한다.
  gh pr ready "$pr_ref" || true
fi

read -r is_draft state url < <(
  gh pr view "$pr_ref" \
    --json isDraft,state,url \
    --jq '[.isDraft, .state, .url] | @tsv'
)

if [[ "$state" != "OPEN" || "$is_draft" != "false" ]]; then
  echo "Ready PR 검증에 실패했습니다: state=$state isDraft=$is_draft url=$url" >&2
  exit 1
fi

printf 'READY\t%s\n' "$url"
