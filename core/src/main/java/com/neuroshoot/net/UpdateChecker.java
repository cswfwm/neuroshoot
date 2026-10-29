package com.neuroshoot.net;

import com.neuroshoot.Version;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 检查 GitHub Releases 是否有新版本。
 *
 * 用 GitHub 的公开 API 读最新 release，比较 versionCode 决定是否提示。
 * 全程在后台线程执行，失败一律按「无更新」处理，不打断玩家。
 */
public final class UpdateChecker {
    private static final String API =
            "https://api.github.com/repos/" + Version.REPO + "/releases/latest";

    public static final class Result {
        public final boolean hasUpdate;
        public final String latestTag;
        public final int latestCode;
        public final String apkUrl;
        public final String notes;

        Result(boolean hasUpdate, String tag, int code, String url, String notes) {
            this.hasUpdate = hasUpdate;
            this.latestTag = tag;
            this.latestCode = code;
            this.apkUrl = url;
            this.notes = notes;
        }

        static Result none() {
            return new Result(false, "", 0, "", "");
        }
    }

    private UpdateChecker() {}

    /** 同步查询（调用方需放后台线程）。 */
    public static Result check() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "neuroshoot/" + Version.NAME);
            if (conn.getResponseCode() != 200) return Result.none();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            // 不引第三方 JSON 库：直接从响应文本里取需要的几个字段，
            // 减少依赖体积（低内存设备上 dex 也占内存）。
            String body = sb.toString();
            String tag = extract(body, "tag_name");
            String notes = extract(body, "body");
            int latestCode = parseCode(tag);
            if (latestCode <= Version.CODE) return Result.none();

            String apkUrl = extractApkUrl(body);
            return new Result(true, tag, latestCode, apkUrl, notes);
        } catch (Exception e) {
            // 网络异常、解析失败都当作无更新
            return Result.none();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 取 "key": "value" 形式的字符串字段值。
     * GitHub 返回的 JSON 里这些字段不会含转义引号，简单扫描即可。
     */
    public static String extract(String json, String key) {
        String pat = "\"" + key + "\":\"";
        int i = json.indexOf(pat);
        if (i < 0) {
            // 兼容 "key": "value"（冒号后带空格）
            pat = "\"" + key + "\": \"";
            i = json.indexOf(pat);
            if (i < 0) return "";
        }
        int start = i + pat.length();
        StringBuilder sb = new StringBuilder();
        for (int k = start; k < json.length(); k++) {
            char c = json.charAt(k);
            if (c == '\\') {           // 跳过转义对
                if (k + 1 < json.length()) sb.append(json.charAt(++k));
                continue;
            }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    /** 找第一个以 .apk 结尾的资产下载地址。 */
    public static String extractApkUrl(String json) {
        String key = "\"browser_download_url\":\"";
        int idx = 0;
        while (true) {
            int i = json.indexOf(key, idx);
            if (i < 0) return "";
            int start = i + key.length();
            int end = json.indexOf('"', start);
            if (end < 0) return "";
            String url = json.substring(start, end);
            if (url.endsWith(".apk")) return url;
            idx = end;
        }
    }

    /** 从 tag（形如 v1.2 或 v1.2.3）解析出构建号，用于和 versionCode 比较。 */
    public static int parseCode(String tag) {
        if (tag == null) return 0;
        String t = tag.trim().toLowerCase();
        if (t.startsWith("v")) t = t.substring(1);
        // 只比较主版本号：v1.2 → 2，v1.10 → 10
        String[] parts = t.split("\\.");
        try {
            if (parts.length >= 2) return Integer.parseInt(parts[1]);
            if (parts.length == 1) return Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {
            // 非数字 tag 视为无版本信息
        }
        return 0;
    }
}
