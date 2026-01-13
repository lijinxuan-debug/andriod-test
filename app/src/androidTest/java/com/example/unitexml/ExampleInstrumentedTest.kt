package com.example.calcultor

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unitexml.MainActivity3
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, 移除根号和阶乘相关用例
 * 仅验证四则运算、幂运算、百分比、负数、隐式乘法等核心功能
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /**
     * 通用断言方法，输出测试报告并验证结果
     *
     * @param expression 输入表达式
     * @param expected   预期结果
     */
    private fun assertCalc(expression: String, expected: String?) {
        val main = MainActivity3()
        val result: String = main.evaluate(expression).toString()
        // 构建控制台输出报告
        val report = StringBuilder()
        report.append("\n[TEST CASE]")
        report.append("\n   Input:    ").append(expression)
        report.append("\n   Expected: ").append(expected)
        report.append("\n   Actual:   ").append(result)
        report.append("\n----------------------------------")

        println(report.toString())
        Log.i(TAG, report.toString())
        Assert.assertEquals("表达式测试失败: " + expression, expected, result)
    }

    // --- 1. 基础四则运算 (补充边界和特殊值) ---
    @Test
    fun testBasicArithmetic() {
        assertCalc("1+2", "3")
        assertCalc("10-5", "5")
        assertCalc("3x4", "12")
        assertCalc("10÷2", "5")
        assertCalc("2.5+2.5", "5")
        assertCalc("10÷4", "2.5")
        // 补充：结果为0的场景
        assertCalc("5-5", "0")
        assertCalc("0x100", "0")
        assertCalc("0÷5", "0")
        // 补充：小数点边界
        assertCalc(".5+0.5", "1")
        assertCalc("5.+5", "10")
        assertCalc("10.5-0.5", "10")
    }

    // --- 2. 优先级校验 (PEMDAS) 补充嵌套和复杂优先级 ---
    @Test
    fun testPriority() {
        assertCalc("1+2x3", "7") // 乘法优先
        assertCalc("(1+2)x3", "9") // 括号优先
        assertCalc("10-2^3", "2") // 幂运算优先
        assertCalc("2^3x2", "16") // 幂优先于乘
        // 补充：多层嵌套优先级
        assertCalc("10-(2+3)x2^2", "-10")
        assertCalc("(5+3)x(4-2)^3", "64")
    }

    // --- 3. 幂运算与百分比 (补充更多组合和边界) ---
    @Test
    fun testPowerAndPercent() {
        assertCalc("9^(2)%", "0.81") // 验证 % 优先级最低 (先算 9^2)
        assertCalc("9^2%", "0.81") // 简写形式
        assertCalc("100+10%", "100.1") // 100 + 0.1
        assertCalc("(5+5)%", "0.1") // 括号整体百分比
        assertCalc("10%%", "0.001") // 连续百分号 (10 * 0.01 * 0.01)
        // 补充：百分比混合运算
        assertCalc("10%x5", "0.5")
        assertCalc("5+10%x2", "5.2")
        assertCalc("(10+20)%x2", "0.6")
        // 补充：幂运算边界
        assertCalc("2^0", "1") // 任何数的0次幂为1
        assertCalc("0^5", "0") // 0的正数次幂为0
        assertCalc("2^1000", "超出计算范围") // 超大幂次超出范围
        assertCalc("2^0.5", "1.4142135623730951") // 小数次幂
    }

    // --- 4. 隐式乘法 (仅保留不含根号的场景) ---
    @Test
    fun testImplicitMultiplication() {
        assertCalc("3(4+5)", "27") // 数字(
        assertCalc("(2+3)4", "20") // )数字
        assertCalc("(2)(3)", "6") // )(
    }

    // --- 5. 负数处理 (补充嵌套负号场景) ---
    @Test
    fun testNegativeNumbers() {
        assertCalc("-5+3", "-2") // 开头负号
        assertCalc("5x(-2)", "-10") // 括号内负号
        assertCalc("9^-2", "0.012345679012345679") // 运算符后跟负号 (自动加括号)
        assertCalc("2^(-1+3)", "4") // 复杂指数
        assertCalc("-5x-5", "25") // 连续负号简写
        // 补充：嵌套负号
        assertCalc("-(-5)", "5") // 负负得正
        assertCalc("10+-5", "5") // 运算符+负号
        assertCalc("(-3)^2", "9") // 负数的平方
        assertCalc("-3^2", "-9") // 优先级：先算幂再算负号
    }

    // --- 6. 科学计数法与大数值 (补充更多场景) ---
    @Test
    fun testBigNumbersAndFormatting() {
        // 触发科学计数法 (>= 1E20)
        assertCalc("10^20", "1E20")
        // 极小值
        assertCalc("1÷10^8", "1E-8")
        // 精度保留 (34位)
        assertCalc("1÷3", "0.3333333333333333333333333333333333")
        // 科学计数法输入解析
        assertCalc("1.2E2+10", "130")
        // 补充：更多科学计数法场景
        assertCalc("1.234E5", "123400") // 正指数
        assertCalc("-2.5E-3", "-0.0025") // 负指数+负数
        assertCalc("1E-7", "1E-7") // 临界极小值
        assertCalc("1.00000001E-7", "1.00000001E-7") // 接近临界值
        // 补充：超长数字格式处理
        assertCalc("123456789012345678901", "1.2345678901234568E20") // 超长整数
        assertCalc("0.000000009999999", "9.999999E-9") // 超小小数
    }

    // --- 7. 异常输入拦截 (移除根号和阶乘相关异常) ---
    @Test
    fun testErrorHandling() {
        assertCalc("9^+", "错误") // 幂后接非法符号
        assertCalc("5++3", "错误") // 连续二元运算
        assertCalc("÷5", "错误") // 非法开头
        assertCalc("5÷0", "错误") // 除零
        assertCalc("()", "错误") // 空括号
        assertCalc("x", "错误") // 纯符号
        // 补充：更多异常场景
        assertCalc("5..3", "错误") // 连续小数点
        assertCalc(")1+2(", "错误") // 括号顺序错误
        assertCalc("1+(2x3))", "错误") // 右括号过多
        assertCalc("^5", "错误") // 纯幂运算符开头
    }

    // --- 8. 自动补全逻辑 (补充更多场景) ---
    @Test
    fun testAutoCompletion() {
        assertCalc("((1+2)", "3") // 自动补齐右括号
        assertCalc("5+3x", "8") // 自动去掉末尾运算符
        assertCalc("5+", "5") // 末尾单运算符
        // 补充：多层嵌套括号补全
        assertCalc("((((10-5)", "5") // 多层左括号自动补全
        assertCalc("10-2x(3+", "4") // 运算符+括号未闭合
    }

    // --- 9. 空/空白输入处理 ---
    @Test
    fun testEmptyInput() {
        assertCalc("", "0") // 空字符串
        assertCalc("   ", "0") // 纯空白
    }

    // --- 10. 复杂混合运算场景 (移除根号和阶乘) ---
    @Test
    fun testComplexExpressions() {
        // 综合运算：括号+幂+乘+加+百分比
        assertCalc("(5+3)x2^3 - 10%", "63.9")
        // 科学计数法+四则运算
        assertCalc("1.2E3 + 5x10^2 - 2.5E2", "1450")
        // 负数+幂运算+百分比
        assertCalc("-5 + 2^4 + 20%", "11.2")
        // 百分比+隐式乘法
        assertCalc("5(10%) + 2^2", "4.5")
    }

    // --- 12. 百分号与隐式乘法的碰撞 ---
    @Test
    fun testPercentWithImplicit() {
        assertCalc("10%(2+3)", "0.5") // 预期：0.1 x 5 = 0.5
        assertCalc("(5+5)%10", "1") // 预期：0.1 x 10 = 1
        assertCalc("2(50%)", "1") // 预期：2 x 0.5 = 1
    }

    // --- 13. 浮点数格式化边界 ---
    @Test
    fun testFormattingBoundary() {
        assertCalc("0.00000000", "0")
        assertCalc("1.000000", "1")
        assertCalc("0.00000001", "1E-8") // 验证科学计数法触发点
        assertCalc("99999999999999999999", "99999999999999999999") // 20位整数不触发E
        assertCalc("100000000000000000000", "1E20") // 21位整数触发E
    }

    // --- 14. 连续小数点与异常点 ---
    @Test
    fun testPointErrors() {
        assertCalc("5..5", "错误")
        assertCalc(".5.5", "错误")
        assertCalc("5.5.", "错误")
    }

    // --- 15. 负数底数的幂运算 (核心逻辑校验) ---
    @Test
    fun testNegativeBasePower() {
        assertCalc("0-3^2", "-9") // 预期：先算3^2=9，再算0-9 = -9
        assertCalc("(-3)^2", "9") // 预期：(-3)的平方 = 9
        assertCalc("-3^2", "-9") // 预期：同0-3^2
    }

    companion object {
        private const val TAG = "CalculatorInstrumentedTest"
    }
}