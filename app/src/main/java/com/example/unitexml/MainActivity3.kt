package com.example.unitexml

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.unitexml.databinding.ActivityMain3Binding
import java.util.Stack
import kotlin.math.log
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
            // 先占个位，下一步我们写计算逻辑
            calculateResult()
        }
    }

    private fun appendToMainDisplay(str: String) {
        val current = binding.tvMain.text.toString()

        // 1. 普通二元运算符（+ - × ÷）
        val isBinaryOp = "[+\\-×÷]".toRegex()
        // 2. 所有符号（包含小数点、根号等）
        val isSpecial = "[+\\-×÷.√!%]".toRegex()

        val lastChar = current.last().toString()

        if (current == "0" || current == "Error") {
            when (str) {
                "√" -> {
                    binding.tvMain.text = "√" // 0 变 √
                }
                "." -> {
                    binding.tvMain.text = "0."
                }
                "%","+","!","×","÷","-" -> {
                    return
                }
                else -> {
                    binding.tvMain.text = str
                }
            }
        }
        // 情况：如果最后一位是 [+ - × ÷]，现在又按了一个 [+ - × ÷]
        else if (lastChar.matches(isSpecial) && str.matches(isSpecial)) {
            binding.tvMain.text = current.dropLast(1) + str // 替换旧符号
        }
        // 情况：防止 .√ 或 √. 这种错误
        else if (lastChar == "." && str == "√") {
            return // 不允许操作
        }
        else {
            binding.tvMain.text = current + str
        }

        // 每次点击都尝试即时预览
        tryLivePreview()
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

        var fixedExpression = expression

        // 1. 统计左右括号数量差
        val leftCount = fixedExpression.count { it == '(' }
        val rightCount = fixedExpression.count { it == ')' }

        // 2. 如果左括号多于右括号，在末尾补全
        if (leftCount > rightCount) {
            fixedExpression += ")".repeat(leftCount - rightCount)
        }

        val cleanedExp = fixedExpression
            .replace("(?<=\\d)√".toRegex(), "×√")
            .replace("(?<=\\d)\\(".toRegex(), "×(")
            .replace("\\)(?=\\d)".toRegex(), ")×")
            .replace("\\)√".toRegex(), ")×√")
            .replace("!(?=\\d)".toRegex(), "!×")
            .replace("%(?=\\d)".toRegex(), "%×")

        val numbers = Stack<Double>()
        val operators = Stack<Char>()

        // 重新调整后的正则拆分，确保所有符号都被识别
        val tokens = cleanedExp.split("(?<=[+\\-×÷()√!%])|(?=[+\\-×÷()√!%])".toRegex())

        for (token in tokens) {
            val t = token.trim()
            if (t.isEmpty()) continue

            when {
                // 1. 处理数字
                t[0].isDigit() || (t.length > 1 && t[0] == '.') -> {
                    numbers.push(t.toDouble())
                }

                // 2. 处理根号 (前置符号：优先级极高)
                t == "√" -> operators.push('√')

                // 3. 处理百分号和阶乘 (后置符号：立即作用于栈顶数字)
                t == "%" -> if (numbers.isNotEmpty()) numbers.push(numbers.pop() / 100.0)
                t == "!" -> if (numbers.isNotEmpty()) numbers.push(factorial(numbers.pop()))

                t == "(" -> operators.push('(')
                t == ")" -> {
                    while (operators.peek() != '(') {
                        numbers.push(applyOp(operators.pop(), numbers))
                    }
                    operators.pop()
                    // 括号算完后，看看前面有没有紧跟根号
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
        return numbers.pop()
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
        // 根号是一元运算
        if (op == '√') {
            val a = numbers.pop()
            return sqrt(a)
        }

        // 加减乘除是二元运算
        if (numbers.size < 2) return if (numbers.isNotEmpty()) numbers.pop() else 0.0
        val b = numbers.pop() // 第二个操作数
        val a = numbers.pop() // 第一个操作数

        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '×' -> a * b
            '÷' -> {
                if (b == 0.0) {
                    throw ArithmeticException("除数不能为零") // 这里抛异常
                } else {
                    a / b
                }
            }
            else -> 0.0
        }
    }

    // 阶乘算法
    private fun factorial(n: Double): Double {
        if (n < 0) return 0.0
        if (n == 0.0) return 1.0
        var res = 1.0
        for (i in 1..n.toInt()) res *= i
        return res
    }

    // 格式化结果：如果是整数就不显示 .0
    private fun formatResult(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.6f", result).trimEnd('0').trimEnd('.')
        }
    }

}