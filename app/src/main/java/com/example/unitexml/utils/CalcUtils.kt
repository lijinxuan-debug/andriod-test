package com.example.unitexml.utils

import kotlin.math.abs

object CalcUtils {
    // 格式化结果：如果是整数就不显示 .0
    fun formatResult(result: Double): String {
        // 1. 处理特殊情况：无穷大或非法数字
        if (result.isInfinite() || result.isNaN()) return "Error"

        // 2. 获取绝对值，用于判断是否需要进入“E”模式
        val absValue = abs(result)

        return when {
            // 3. 科学计数法：数字太大（10亿以上）或太小（0.0000001以下）
            // 这里的 1e9 表示 10的9次方，1e-7 表示 10的-7次方
            absValue >= 1e9 || (absValue < 1e-7 && absValue > 0) -> {
                // %.6E 会格式化为：1.234567E+12
                String.format("%.6E", result)
            }

            // 4. 整数处理：如果 Double 等于它的 Long 值，说明是纯整数
            result == result.toLong().toDouble() -> {
                result.toLong().toString()
            }

            // 5. 普通小数处理：保留 10 位有效小数并去除末尾的 0
            else -> {
                // 使用 %.10f 保证高精度，避免 1.1 变成 1.100000
                String.format("%.10f", result)
                    .replace("0*$".toRegex(), "") // 去掉末尾所有的0
                    .replace("\\.$".toRegex(), "") // 如果最后剩下小数点，也去掉
            }
        }
    }
}