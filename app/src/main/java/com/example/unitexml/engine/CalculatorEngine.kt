package com.example.unitexml.engine

import java.util.Stack
import kotlin.math.sqrt

object CalculatorEngine {
    fun evaluate(expression: String): Double {
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
        val protectedExp = fixedExpression
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
            .replace("""!(?=[.\d])""".toRegex(), "!×")
            .replace("""%(?=[.\d])""".toRegex(), "%×")

        // 4. 【还原】把科学计数法换回来
        cleanedExp = cleanedExp
            .replace("###", "E+")
            .replace("@@@", "E-")
        // 3. 双栈运算准备
        val numbers = Stack<Double>()
        val operators = Stack<Char>()

        var finalExp = cleanedExp.trim()
        // 使用正则：如果结尾是 + - × ÷ ( 之一，就把它删掉
        while (finalExp.isNotEmpty() && "[+×÷\\-(]$".toRegex().containsMatchIn(finalExp)) {
            finalExp = finalExp.dropLast(1).trim()
        }

        // 检查删完后是否为空
        if (finalExp.isEmpty()) return 0.0

        val tokens = mutableListOf<String>()
        // 这里的正则要支持 E+ 和 E-
        val pattern = """(\d*\.?\d+(E[+-]?\d+)?)|[+\-×÷()√!%]""".toRegex()
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

        // --- 如果栈里只有一个数，却遇到了二元运算符 ---
        if (numbers.size < 2) {
            // 比如只有 6，遇到了 ×，那就把 6 还给栈，不进行运算
            return if (numbers.isNotEmpty()) numbers.pop() else 0.0
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
}