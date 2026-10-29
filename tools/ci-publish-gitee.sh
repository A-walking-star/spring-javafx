#!/usr/bin/env bash
# 在 Gitee Go 流水线里给指定 tag 的 Gitee 发行版补齐三个平台的 demo jar。
#
# 设计成幂等：已经有全部附件时直接退出（定时触发时不会白跑构建）。
# 依赖：git / mvn / curl（Gitee Go 的 build@maven 环境都有）。
#
# 需要的环境变量：
#   GITEE_TOKEN  有 projects 权限的私人令牌（在 Gitee Go 流水线里配成变量）
# 可选：
#   TAG          指定 tag，默认取 git describe 的最近 tag
set -euo pipefail

ASSETS=(spring-javafx-demo-win-x64.jar spring-javafx-demo-linux-x64.jar spring-javafx-demo-mac-x64.jar)
PLATFORMS=(win linux mac)

if [ -z "${GITEE_TOKEN:-}" ] || [ "${GITEE_TOKEN:0:2}" = '${' ]; then
  echo "GITEE_TOKEN 未配置（在 Gitee Go 流水线变量里添加一个私密变量 GITEE_TOKEN），跳过"
  exit 0
fi

TAG="${TAG:-}"
if [ -z "$TAG" ]; then
  TAG="$(git describe --tags --abbrev=0 2>/dev/null || true)"
fi
if [ -z "$TAG" ]; then
  # 浅克隆里没有本地 tag 时，从远端取最大的 v* tag
  TAG="$(git ls-remote --tags origin 2>/dev/null | grep -o 'refs/tags/v[0-9][0-9.]*$' \
    | sed 's#refs/tags/##' | sort -V | tail -1 || true)"
fi
if [ -z "$TAG" ]; then
  echo "没有找到 tag，跳过"
  exit 0
fi

# 从 origin 解析 owner/repo，避免把用户名写死
REMOTE="$(git remote get-url origin)"
OWNER="$(echo "$REMOTE" | sed -E 's#.*[:/]([^/]+)/[^/]+(\.git)?$#\1#')"
REPO="$(echo "$REMOTE" | sed -E 's#.*[:/][^/]+/([^/]+?)(\.git)?$#\1#')"
API="https://gitee.com/api/v5/repos/${OWNER}/${REPO}/releases"
echo "仓库：${OWNER}/${REPO}   tag：${TAG}"

DETAIL="$(curl -sS --connect-timeout 20 --max-time 120 --retry 3 --retry-delay 5 \
  "${API}/tags/${TAG}?access_token=${GITEE_TOKEN}" || true)"
ID="$(echo "$DETAIL" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2 || true)"
if [ -z "$ID" ]; then
  echo "Gitee 上还没有 ${TAG} 的发行版（由 GitHub Actions 创建），本次跳过"
  exit 0
fi
echo "release id=${ID}"

have_asset() {
  echo "$DETAIL" | grep -q "\"name\":\"$1\""
}

missing=0
for a in "${ASSETS[@]}"; do
  if have_asset "$a"; then
    echo "已存在：$a"
  else
    missing=$((missing + 1))
  fi
done
if [ "$missing" -eq 0 ]; then
  echo "三个平台的附件都齐了，无需处理"
  exit 0
fi

mkdir -p dist
for i in "${!PLATFORMS[@]}"; do
  p="${PLATFORMS[$i]}"
  jar="${ASSETS[$i]}"
  if have_asset "$jar"; then
    continue
  fi
  echo "==> 构建 ${p} ..."
  mvn -B -ntp -DskipTests "-Djavafx.platform=${p}" package
  cp demo/target/spring-javafx-demo.jar "dist/${jar}"

  echo "==> 上传 ${jar} ..."
  curl -sS --connect-timeout 30 --max-time 1800 --retry 2 --retry-delay 10 \
    -X POST "${API}/${ID}/attach_files?access_token=${GITEE_TOKEN}" \
    -F "file=@dist/${jar}" | head -c 200
  echo
done

echo "完成：https://gitee.com/${OWNER}/${REPO}/releases/tag/${TAG}"
