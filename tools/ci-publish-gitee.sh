#!/usr/bin/env bash
# 在 Gitee Go 流水线里给指定 tag 的 Gitee 发行版补齐三个平台的 demo jar。
#
# 幂等：附件齐全时直接退出（定时/重复触发不会白跑构建）。
# 发行版不存在时会自己创建（不依赖 GitHub Actions）。
#
# 环境变量：
#   PUBLISH_TOKEN  有 projects 权限的私人令牌（Gitee Go 流水线变量；不能叫 GITEE_*，那是系统保留前缀）
# 可选：
#   TAG          指定 tag，默认取最近的 v* tag
set -euo pipefail

ASSETS=(spring-javafx-demo-win-x64.jar spring-javafx-demo-linux-x64.jar spring-javafx-demo-mac-x64.jar)
PLATFORMS=(win linux mac)

TOKEN="${PUBLISH_TOKEN:-${GITEE_TOKEN:-}}"
if [ -z "$TOKEN" ] || [ "${TOKEN:0:2}" = '${' ]; then
  echo "PUBLISH_TOKEN 未配置（Gitee Go 流水线变量名不能以 GITEE_ 开头，所以用 PUBLISH_TOKEN），跳过"
  exit 0
fi

TAG="${TAG:-}"
if [ -z "$TAG" ]; then
  TAG="$(git describe --tags --abbrev=0 2>/dev/null || true)"
fi
if [ -z "$TAG" ]; then
  TAG="$(git ls-remote --tags origin 2>/dev/null | grep -o 'refs/tags/v[0-9][0-9.]*$' \
    | sed 's#refs/tags/##' | sort -V | tail -1 || true)"
fi
if [ -z "$TAG" ]; then
  echo "没有找到 v* tag，跳过"
  exit 0
fi

REMOTE="$(git remote get-url origin)"
REPO="$(basename "$REMOTE" .git)"
OWNER="$(echo "$REMOTE" | sed -E 's#.*[:/]([^/]+)/[^/]+$#\1#')"
API="https://gitee.com/api/v5/repos/${OWNER}/${REPO}/releases"
echo "仓库=${OWNER}/${REPO} tag=${TAG}"

CURL=(curl -sS --connect-timeout 30 --max-time 180 --retry 3 --retry-delay 5 --retry-all-errors)

# Gitee 的 JSON 带空格（"id": 123），所以正则里要容忍空白
id_of() {
  echo "$1" | grep -o '"id"[[:space:]]*:[[:space:]]*[0-9]\+' | head -1 | grep -o '[0-9]\+' || true
}
has_asset() {
  echo "$1" | grep -q "\"name\"[[:space:]]*:[[:space:]]*\"$2\"" && echo yes || echo no
}

echo "==> 查询发行版 ..."
DETAIL="$("${CURL[@]}" "${API}/tags/${TAG}?access_token=${TOKEN}" || true)"
ID="$(id_of "$DETAIL")"

BODY='已发布到 Maven Central，无需额外配置仓库：

```xml
<dependency>
    <groupId>io.github.a-walking-star</groupId>
    <artifactId>spring-javafx</artifactId>
    <version>__VERSION__</version>
</dependency>
```

附件是可直接运行的示例程序（内置对应平台的 JavaFX 原生库）：
- `spring-javafx-demo-win-x64.jar`（Windows）
- `spring-javafx-demo-linux-x64.jar`（Linux）
- `spring-javafx-demo-mac-x64.jar`（macOS Intel）

运行：`java -jar <对应的 jar>`（需 JDK 8/11/17；JDK 11+ 的运行时依赖见 README）。'
BODY="${BODY//__VERSION__/${TAG#v}}"
BODY_JSON="$(printf '%s' "$BODY" | sed ':a;N;$!ba;s/\n/\\n/g')"

if [ -z "$ID" ]; then
  echo "接口返回：$(echo "$DETAIL" | head -c 300)"
  echo "==> 发行版不存在，创建 ..."
  PAYLOAD="$(printf '{"access_token":"%s","tag_name":"%s","name":"spring-javafx %s","body":"%s","target_commitish":"main","prerelease":false}' \
    "$TOKEN" "$TAG" "$TAG" "$BODY_JSON")"

  RESP="$("${CURL[@]}" -X POST "$API" -H "Content-Type: application/json" -d "$PAYLOAD" || true)"
  ID="$(id_of "$RESP")"
  if [ -z "$ID" ]; then
    echo "创建失败：$(echo "$RESP" | head -c 300)"
    exit 1
  fi
  DETAIL="$RESP"
  echo "==> 已创建，id=${ID}"
else
  echo "==> 已存在，id=${ID}"
  # 旧说明可能只写了 Windows 一个附件，顺手同步一下
  PATCH="$(printf '{"access_token":"%s","name":"spring-javafx %s","body":"%s"}' \
    "$TOKEN" "$TAG" "$BODY_JSON")"
  "${CURL[@]}" -X PATCH "${API}/${ID}" -H "Content-Type: application/json" -d "$PATCH" >/dev/null 2>&1 \
    && echo "    已更新发行版说明" || echo "    说明更新被忽略"
fi

missing=0
for a in "${ASSETS[@]}"; do
  if [ "$(has_asset "$DETAIL" "$a")" = "yes" ]; then
    echo "    已有 $a"
  else
    echo "    缺少 $a"
    missing=$((missing + 1))
  fi
done
if [ "$missing" -eq 0 ]; then
  echo "三个平台的附件都齐了，无需处理"
  exit 0
fi

# 发行版对应的是 tag，构建也应该用 tag 的代码（流水线默认检出的是分支 HEAD）
if git fetch --depth=1 origin "refs/tags/${TAG}:refs/tags/${TAG}" >/dev/null 2>&1; then
  if git checkout -q "refs/tags/${TAG}" 2>/dev/null; then
    echo "==> 使用 tag ${TAG} 的代码构建"
  fi
else
  echo "==> 拉取 tag ${TAG} 失败，用当前 HEAD 构建（可能与 tag 不一致）"
fi

mkdir -p dist
for i in "${!PLATFORMS[@]}"; do
  p="${PLATFORMS[$i]}"
  jar="${ASSETS[$i]}"
  if [ "$(has_asset "$DETAIL" "$jar")" = "yes" ]; then
    continue
  fi

  echo "==> 构建 ${p} ..."
  mvn -B -ntp -DskipTests "-Djavafx.platform=${p}" package
  cp demo/target/spring-javafx-demo.jar "dist/${jar}"

  echo "==> 上传 ${jar} ($(du -h "dist/${jar}" | cut -f1)) ..."
  HTTP="$("${CURL[@]}" -o /tmp/gitee-upload.json -w '%{http_code}' \
    -X POST "${API}/${ID}/attach_files?access_token=${TOKEN}" \
    -F "file=@dist/${jar}")"
  echo "    HTTP ${HTTP}: $(head -c 200 /tmp/gitee-upload.json)"
  case "$HTTP" in
    2*) ;;
    *) echo "    上传失败"; exit 1 ;;
  esac
done

echo "完成：https://gitee.com/${OWNER}/${REPO}/releases/tag/${TAG}"
