package com.example.unitexml

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.unitexml.databinding.ActivityMain3Binding
import java.util.Stack
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity3 : AppCompatActivity() {

    private lateinit var binding : ActivityMain3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMain3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initButtons()
    }

    private fun initButtons() {
        val commonButtons = listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9, binding.buttonPoint,
            binding.buttonPlus, binding.buttonMinus, binding.buttonMultiply,
            binding.buttonDivide, binding.buttonLeftParen, binding.buttonRightParen,
            binding.buttonPercent, binding.buttonSqu, binding.buttonFac
        )

        commonButtons.forEach { btn ->
            btn.setOnClickListener {
                // 将按钮文字追加到主屏幕
                appendToMainDisplay(btn.text.toString())
            }
        }

        binding.buttonClear.setOnClickListener {
            binding.tvMain.text = "0"
            binding.tvSecondary.text = ""
        }

        binding.backspace.setOnClickListener {
            val current = binding.tvMain.text.toString()
            if (current == "Error") {
                binding.tvMain.text = "0"
            }
            else if (current.length > 1) {
                binding.tvMain.text = current.dropLast(1)
            } else {
                binding.tvMain.text = "0"
            }
            tryLivePreview()
        }

        // 等号键 (计算结果)
        binding.equal.setOnClickListener {
            calculateResult()
        }
    }

    private fun appendToMainDisplay(str: String) {
        var current = binding.tvMain.text.toString()

        // 1. 清洗与重置
        if (current == "Error") {
            current = "0"
        }
        // 如果当前是纯科学计数法结果（且没开始输入运算符），输入数字则重置
        if (current.contains("E") && !current.any { "+-×÷".contains(it) && current.indexOf(it) > current.indexOf('E') } && str.matches("[0-9]".toRegex())) {
            current = "0"
        }

        val lastChar = if (current.isNotEmpty()) current.last().toString() else ""
        val isBinaryOp = "[+\\-×÷]".toRegex()

        // 【关键修改】：判断末尾是否为“真正的”运算符（排除 E+ 或 E-）
        val isRealOperatorAtEnd = lastChar.matches(isBinaryOp) &&
                (current.length < 2 || current[current.length - 2].uppercaseChar() != 'E')

        // 2. 小数点拦截：针对科学计数法特殊处理，不使用 split
        if (str == ".") {
            // 如果当前数字已经是科学计数法（E后面），通常不允许再点小数点
            if (current.contains("E")) {
                val afterE = current.substringAfterLast("E")
                // 如果 E 之后还没有出现新的运算符，说明还在指数部分，拦截小数点
                if (!afterE.any { "+-×÷".contains(it) && it != '+' && it != '-' }) return
            }
            // 普通小数点逻辑
            val lastPart = current.split("[+\\-×÷()√!]".toRegex()).last()
            if (lastPart.contains(".") || lastChar.matches("[!%]".toRegex())) return
        }

        // 3. 核心逻辑处理
        when {
            current == "0" -> {
                when {
                    str == "√" -> binding.tvMain.text = "√"
                    str == "-" -> binding.tvMain.text = "-"
                    str == "." -> binding.tvMain.text = "0."
                    str.matches(isBinaryOp) || str.matches("[!%]".toRegex()) -> return
                    else -> binding.tvMain.text = str
                }
            }

            // 处理减号
            str == "-" -> {
                if (lastChar == "(" || lastChar == "×" || lastChar == "÷") {
                    binding.tvMain.text = current + str
                } else if (isRealOperatorAtEnd) {
                    // 只有末尾是真正的运算符才替换
                    binding.tvMain.text = current.dropLast(1) + str
                } else {
                    binding.tvMain.text = current + str
                }
            }

            // 处理加、乘、除
            str.matches(isBinaryOp) -> {
                if (isRealOperatorAtEnd) {
                    binding.tvMain.text = current.dropLast(1) + str
                } else if (lastChar == "." || lastChar == "(" || lastChar == "√") {
                    return
                } else {
                    binding.tvMain.text = current + str
                }
            }

            str == "√" -> {
                if (lastChar == ".") return
                binding.tvMain.text = current + str
            }

            // 场景 E：普通追加 (数字、阶乘、括号等)
            else -> {
                // 1. 处理 % 后的零
                if (current.endsWith("%0")) {
                    if (str == "0") return // 禁止 5%00
                    if (str.matches("[1-9]".toRegex())) {
                        binding.tvMain.text = current.dropLast(1) + str // 5%0 -> 5%2
                        tryLivePreview()
                        return
                    }
                }

                // 2. 处理 ! 后的零 (如果你也想限制 5!00 的话，可以加这一段)
                if (current.endsWith("!0")) {
                    if (str == "0") return
                    if (str.matches("[1-9]".toRegex())) {
                        binding.tvMain.text = current.dropLast(1) + str
                        tryLivePreview()
                        return
                    }
                }

                // 3. 正常追加
                binding.tvMain.text = current + str
            }
        }

        tryLivePreview()
        binding.tvMain.requestLayout()
    }

    private fun calculateResult() {
        val expression = binding.tvMain.text.toString()
        if (expression == "0" || expression.isEmpty()) return


        try {
            if (expression.last() == '√') {
                throw ArithmeticException("结尾不能是根号")
            }

            val result = evaluate(expression)

            // 更新 UI
            binding.tvSecondary.text = ""
            binding.tvMain.text = formatResult(result)
        } catch (e: Exception) {
            // 如果计算出错（比如除以0，或者括号不匹配）
            binding.tvMain.text = "Error"
            binding.tvSecondary.text = ""
        }
    }

    private fun evaluate(expression: String): Double {
        if (expression.contains("""\(\)""".toRegex())) {
            throw ArithmeticException("不允许出现空括号")
        }

        if (expression.all { it == '(' || it == ')' || it.isWhitespace() }) {
            throw ArithmeticException("不能只有括号")
        }


        // 1. 自动补全括号
        // 1. 先补全括号 (这是 6(9 变成 54 的关键第一步)
        var fixedExpression = expression
        val leftCount = fixedExpression.count { it == '(' }
        val rightCount = fixedExpression.count { it == ')' }
        if (leftCount > rightCount) {
            fixedExpression += ")".repeat(leftCount - rightCount)
        }

        // 2. 【核心修改】暂时隐藏科学计数法中的 E+ 和 E-，防止被 replace 误伤
        // 把 "E+" 换成一个特殊占位符 "###"，把 "E-" 换成 "@@@"
        var protectedExp = fixedExpression
            .replace("E+", "###")
            .replace("E-", "@@@")

        // 3. 执行你原有的各种 replace (现在它们不会碰到科学计数法的符号了)
        var cleanedExp = protectedExp
            .replace("(-", "(0-")
            .replace("(+", "(0+")
            .replace("×-", "×(0-1)×")
            .replace("÷-", "÷(0-1)×")
            .replace("+-", "+(0-1)×")
            .replace("--", "-(0-1)×")

        if (cleanedExp.startsWith("-")) {
            cleanedExp = "(0-1)×" + cleanedExp.substring(1)
        }

        // 补全隐式乘法
        cleanedExp = cleanedExp
            .replace("""(?<=\d)\(""".toRegex(), "×(")
            .replace("""\)(?=\d)""".toRegex(), ")×")
            .replace("""\)\(""".toRegex(), ")×(")
            .replace("""(?<=\d)√""".toRegex(), "×√")
            .replace("""\)√""".toRegex(), ")×√")
            .replace("""!(?=\d)""".toRegex(), "!×")
            .replace("""%(?=\d)""".toRegex(), "%×")

        // 4. 【还原】把科学计数法换回来
        cleanedExp = cleanedExp
            .replace("###", "E+")
            .replace("@@@", "E-")
        // 3. 双栈运算准备
        val numbers = Stack<Double>()
        val operators = Stack<Char>()

        val tokens = mutableListOf<String>()
        // 这里的正则要支持 E+ 和 E-
        val pattern = """(\d+\.?\d*([eE][+-]?\d+)?)|[+\-×÷()√!%]""".toRegex()
        pattern.findAll(cleanedExp).forEach { tokens.add(it.value) }

        for (t in tokens) {
            when {
                // 直接用 toDoubleOrNull 判断，它原生支持 2.13E18 这种格式
                t.toDoubleOrNull() != null -> {
                    numbers.push(t.toDouble())
                }

                t == "√" -> operators.push('√')
                t == "%" -> if (numbers.isNotEmpty()) numbers.push(numbers.pop() / 100.0)
                t == "!" -> if (numbers.isNotEmpty()) numbers.push(factorial(numbers.pop()))
                t == "(" -> operators.push('(')

                t == ")" -> {
                    if (!operators.contains('(')) throw ArithmeticException("Error")
                    while (operators.isNotEmpty() && operators.peek() != '(') {
                        numbers.push(applyOp(operators.pop(), numbers))
                    }
                    if (operators.isNotEmpty()) operators.pop()
                    if (operators.isNotEmpty() && operators.peek() == '√') {
                        numbers.push(applyOp(operators.pop(), numbers))
                    }
                }

                isOperator(t[0]) -> {
                    while (operators.isNotEmpty() && hasPrecedence(t[0], operators.peek())) {
                        numbers.push(applyOp(operators.pop(), numbers))
                    }
                    operators.push(t[0])
                }
            }
        }

        while (operators.isNotEmpty()) {
            numbers.push(applyOp(operators.pop(), numbers))
        }

        return if (numbers.isNotEmpty()) numbers.pop() else 0.0
    }

    private fun tryLivePreview() {
        val expression = binding.tvMain.text.toString()

        if (expression.isEmpty() || expression == "0") {
            binding.tvSecondary.text = ""
            return
        }

        try {
            // 1. 准备一个待计算的变量
            var calcExpression = expression

            // 2. 正则：检查是否以运算符 [+\-×÷(] 结尾
            val operatorAtEndRegex = "[+\\-×÷(]$".toRegex()

            // 如果结尾是根号，直接报错
            if (expression.last() == '√') {
                throw ArithmeticException("末尾不能为根号")
            }

            // 3. 如果末尾确实是符号，我们就截取掉最后一位再算f
            // 比如 "8-6+" 变成 "8-6" 参与计算
            if (expression.contains(operatorAtEndRegex)) {
                calcExpression = expression.dropLast(1)
            }

            // 4. 如果截取后变空了（比如只输入了一个“-”），就清空预览
            if (calcExpression.isEmpty() || calcExpression.matches(operatorAtEndRegex)) {
                binding.tvSecondary.text = ""
                return
            }

            // 5. 执行计算并显示
            val result = evaluate(calcExpression)
            binding.tvSecondary.text = formatResult(result)

        } catch (e: Exception) {
            // 遇到括号未闭合等情况，保持预览区为空
            binding.tvSecondary.text = "Error"
        }
    }

    // 直接判断你的自定义符号
    private fun isOperator(c: Char) = c == '+' || c == '-' || c == '×' || c == '÷'

    // 判断优先级逻辑：如果 op2 优先级 >= op1，则返回 true
    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        if (op2 == '(' || op2 == ')') return false
        return getPriority(op2) >= getPriority(op1)
    }

    // 优先级定义：√ 最高，乘除次之，加减最后
    private fun getPriority(op: Char): Int {
        return when (op) {
            '√' -> 3
            '×', '÷' -> 2
            '+', '-' -> 1
            else -> 0
        }
    }

    // 核心运算逻辑
    private fun applyOp(op: Char, numbers: Stack<Double>): Double {
        // 1. 基础校验：栈空直接抛异常，交给外层 try-catch 处理成 Error
        if (numbers.isEmpty()) throw ArithmeticException("Invalid")

        // 2. 根号处理 (一元运算)
        if (op == '√') {
            val a = numbers.pop()
            if (a < 0) throw ArithmeticException("Math Error")
            return sqrt(a)
        }

        // 3. 【核心修改】二元运算 (加减乘除)
        // 必须确保栈里至少有 2 个数字，否则说明算式残缺（如 "-6-("）
        if (numbers.size < 2) {
            throw ArithmeticException("Invalid Expression")
        }

        val b = numbers.pop() // 第二个操作数
        val a = numbers.pop() // 第一个操作数

        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '×' -> a * b
            '÷' -> {
                if (b == 0.0) throw ArithmeticException("Div 0")
                a / b
            }
            else -> 0.0
        }
    }

    // 阶乘算法
    private fun factorial(n: Double): Double {
        if (n < 0) throw ArithmeticException("阶乘数目不可为负数")
        else if (n == 0.0) return 1.0
        else if (n > 170) throw ArithmeticException("数字太大，无法计算")
        else if (n % 1 != 0.0) throw ArithmeticException("阶乘不可为小数")
        var res = 1.0
        for (i in 1..n.toInt()) res *= i
        return res
    }

    // 格式化结果：如果是整数就不显示 .0
    private fun formatResult(result: Double): String {
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