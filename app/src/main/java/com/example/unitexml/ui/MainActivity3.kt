package com.example.unitexml.ui

import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.ObjectAnimator
import com.example.unitexml.databinding.ActivityMain3Binding
import com.example.unitexml.engine.CalculatorEngine
import com.example.unitexml.utils.CalcUtils

class MainActivity3 : AppCompatActivity() {

    private lateinit var binding : ActivityMain3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMain3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initButtons()
    }

    // 1. 销毁前把数据存进“小盒”里
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("saved_text", binding.tvMain.text.toString())
    }

    // 2. 重新创建后把数据从“小盒”里取出来
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val myText = savedInstanceState.getString("saved_text")
        binding.tvMain.text = myText
        // 反转过来之后还需要重新计算
        tryLivePreview()
    }

    private fun initButtons() {
        // 先获取到了所有的按钮
        val commonButtons = listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9, binding.buttonPoint,
            binding.buttonPlus, binding.buttonMinus, binding.buttonMultiply,
            binding.buttonDivide, binding.buttonLeftParen, binding.buttonRightParen,
            binding.buttonPercent, binding.buttonSqu, binding.buttonFac
        )

//        binding.btnRotate.setOnClickListener {
//            // 如果说resources.configuration用来读，那么ActivityInfo就是用来写。
//            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
//                // 如果是竖屏，就转成横屏
//                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//            } else {
//                // 如果是横屏，就转回竖屏
//                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//            }
//        }

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

        binding.tvMain.movementMethod = ScrollingMovementMethod()
    }

    private fun showCardToast(message: String = "数字长度最长15位！") {
        binding.tvTopToast.text = message
        binding.cardToast.visibility = View.VISIBLE

        // 初始位置：完全藏在屏幕上方
        binding.cardToast.translationY = -300f
        binding.cardToast.alpha = 0f

        // 弹出动画
        binding.cardToast.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            // 使用回弹插值器，让卡片落地时晃动一下，很有质感
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                // 2.5秒后自动消失
                binding.cardToast.postDelayed({
                    binding.cardToast.animate()
                        .translationY(-300f)
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction { binding.cardToast.visibility = View.GONE }
                        .start()
                }, 2500)
            }
            .start()
    }

    // 简单的抖动动画函数
    private fun shakeView(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        anim.duration = 500
        anim.start()
    }

    private fun appendToMainDisplay(str: String) {
        var current = binding.tvMain.text.toString()

        // 1. 清洗与重置
        if (current == "Error") {
            current = "0"
        }

        // 如果输入的是数字或小数点，我们需要检查当前数字片段的长度
        if (str.matches("[0-9.]".toRegex())) {
            // 使用正则拆分，获取最后一段数字片段
            val lastNumberPart = current.split("[+×÷\\-()√!%]".toRegex()).last()

            if (lastNumberPart.length >= 15) {
                if (binding.cardToast.visibility == View.VISIBLE) {
                    // 如果卡片正在显示，就抖两下
                    shakeView(binding.cardToast)
                    // 同时来个震动效果，让用户有所感应
                    val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val vibrator = vibratorManager.defaultVibrator

                    val vibrationEffect =
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)

                    vibrator.vibrate(vibrationEffect)
                } else {
                    showCardToast()
                }
                return
            }
        }

        val lastChar = if (current.isNotEmpty()) current.last().toString().trim() else ""
        val isBinaryOp = "[+×÷-]".toRegex()
        val isRealOperatorAtEnd = lastChar.matches(isBinaryOp) && !(current.uppercase().endsWith("E+") || current.uppercase().endsWith("E-"))

        // 2. 小数点拦截：针对科学计数法特殊处理，不使用 split
        if (str == ".") {
            // 如果当前数字已经是科学计数法（E后面），通常不允许再点小数点
            if (current.contains("E")) {
                val afterE = current.substringAfterLast("E")
                // 如果 E 之后还没有出现新的运算符，说明还在指数部分，拦截小数点
                if (!afterE.any { "+-×÷".contains(it) && it != '+' && it != '-' }) return
            }
            // 普通小数点逻辑
            val lastPart = current.split("[+\\-×÷()√!%]".toRegex()).last()
            if (lastPart.contains(".")) return
        }

        // 3. 核心逻辑处理
        when {
            current == "0" -> {
                when {
                    str == "√" -> binding.tvMain.text = "√"
                    str == "-" -> binding.tvMain.text = "-"
                    str == "." -> binding.tvMain.text = "0."
                    // 【核心修改】：允许阶乘 ! 和 百分号 % 追加在 0 后面
                    str == "!" || str == "%" -> binding.tvMain.text = "0$str"
                    // 拦截加、乘、除等二元运算符
                    str.matches(isBinaryOp) -> return
                    else -> binding.tvMain.text = str
                }
            }

            // 处理减号
            str == "-" -> {
                if (lastChar == "(" || lastChar == "√" || lastChar == "×" || lastChar == "÷") {
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

            val result = CalculatorEngine.evaluate(expression)

            // 更新 UI
            binding.tvSecondary.text = ""
            binding.tvMain.text = CalcUtils.formatResult(result)
        } catch (e: Exception) {
            // 如果计算出错（比如除以0，或者括号不匹配）
            binding.tvMain.text = "Error"
            binding.tvSecondary.text = ""
        }
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
            val result = CalculatorEngine.evaluate(calcExpression)
            binding.tvSecondary.text = CalcUtils.formatResult(result)

        } catch (e: Exception) {
            // 遇到括号未闭合等情况，保持预览区为空
            binding.tvSecondary.text = "Error"
        }
    }
}