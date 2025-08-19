package com.xxmicloxx.NoteBlockAPI.nukkit;

import cn.nukkit.Player;
import cn.nukkit.network.protocol.ProtocolInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClientTypeDetector {
    // 定义关键版本字符串
    private static final String VERSION_1_8_0_11 = "1.8.0.11";
    private static final String VERSION_1_13_0 = "1.13.0";
    private static final String VERSION_1_21_50 = "1.21.50";
    private static final String VERSION_1_21_70 = "1.21.70";

    /**
     * 获取客户端类型（1-5）
     * 类型划分：
     * 1: 低于1.8.0.11（协议<312）
     * 2: 1.8.0.11(含) 到 1.13.0(不含)（312<=协议<388）
     * 3: 1.13.0(含) 到 1.21.50(不含)（388<=协议<766）
     * 4: 1.21.50(含) 到 1.21.70(不含)（766<=协议<786）
     * 5: 1.21.70(含)及以上（协议>=786）
     */
    public static int getClientType(Player player) {
        // 优先级1：检查Nukkit-MOT的protocol字段
        Integer motProtocol = getMOTProtocol(player);
        if (motProtocol != null) {
            return getTypeFromProtocol(motProtocol);
        }

        // 优先级2：尝试解析GameVersion
        String gameVersion = getGameVersion(player);
        if (gameVersion != null) {
            try {
                return getTypeFromGameVersion(gameVersion);
            } catch (Exception e) {
                // 解析失败，降级使用CURRENT_PROTOCOL
            }
        }

        // 优先级3：使用服务器当前协议版本
        return getTypeFromProtocol(ProtocolInfo.CURRENT_PROTOCOL);
    }

    /**
     * 从Nukkit-MOT的Player类中获取protocol字段（反射实现兼容）
     */
    private static Integer getMOTProtocol(Player player) {
        try {
            Field protocolField = Player.class.getField("protocol");
            protocolField.setAccessible(true);
            return (Integer) protocolField.get(player);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null; // 非MOT分支
        }
    }

    /**
     * 从GameVersion字符串解析客户端类型
     */
    private static int getTypeFromGameVersion(String gameVersion) {
        if (compareVersions(gameVersion, VERSION_1_8_0_11) < 0) {
            return 1;
        } else if (compareVersions(gameVersion, VERSION_1_13_0) < 0) {
            return 2;
        } else if (compareVersions(gameVersion, VERSION_1_21_50) < 0) {
            return 3;
        } else if (compareVersions(gameVersion, VERSION_1_21_70) < 0) {
            return 4;
        } else {
            return 5;
        }
    }

    /**
     * 从协议号解析客户端类型
     */
    private static int getTypeFromProtocol(int protocol) {
        if (protocol < 312) {
            return 1;
        } else if (protocol < 388) {
            return 2;
        } else if (protocol < 766) {
            return 3;
        } else if (protocol < 786) {
            return 4;
        } else {
            return 5;
        }
    }

    /**
     * 获取玩家的GameVersion（反射实现兼容）
     */
    private static String getGameVersion(Player player) {
        try {
            Method getGameVersion = Player.class.getMethod("getGameVersion");
            return (String) getGameVersion.invoke(player);
        } catch (Exception e) {
            return null; // 无法获取GameVersion
        }
    }

    /**
     * 版本字符串比较（逐位对比）
     * @return -1: v1 < v2; 0: 相等; 1: v1 > v2
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            int num1 = parseVersionPart(parts1, i);
            int num2 = parseVersionPart(parts2, i);
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    /**
     * 解析版本片段（处理位数不足和非数字情况）
     */
    private static int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}