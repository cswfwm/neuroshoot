package com.neuroshoot;

/**
 * 版本信息：应用内展示与更新检查共用同一份定义。
 *
 * 发版时只需改这里的两行，构建脚本会同步读取并写入 Gradle 的
 * versionCode / versionName，避免两处不一致。
 */
public final class Version {
    /** 构建号，每次发版必须递增，Android 用它判断能否覆盖安装。 */
    public static final int CODE = 2;
    /** 展示给用户的版本号。 */
    public static final String NAME = "1.1";

    /** 更新检查读取的仓库。 */
    public static final String REPO = "cswfwm/neuroshoot";

    private Version() {}

    /** 拼给用户看的完整版本串。 */
    public static String display() {
        return "v" + NAME + " (" + CODE + ")";
    }
}
