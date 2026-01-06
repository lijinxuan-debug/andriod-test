package com.example.unitexml

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.unitexml.databinding.ActivityMain3Binding

class MainActivity3 : AppCompatActivity() {

    private lateinit var binding : ActivityMain3Binding

    private var inputValue1 : Double? = 0.0
    private var inputValue2 : Double? = null
    private var currentOperator : Operator? = null
    private var result : Double? = null
    private var equation : StringBuilder = StringBuilder().append(ZERO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMain3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        setNightModeIndicator()
    }

    private fun setListeners() {
        for (button in getNumericButtons()) {
            button.setOnClickListener { onNumberClicked(button.text.toString()) }
        }
        with(binding) {
            buttonZero.setOnClickListener { onZeroClicked() }
            buttonDoubleZero.setOnClickListener { onDoubleZeroClicked() }
            buttonDecimalPoint.setOnClickListener { onDecimalPointClicked() }
            buttonAddition.setOnClickListener { onOperatorClicked(Operator.ADDITION) }
            buttonSubtraction.setOnClickListener { onOperatorClicked(Operator.DIVISION) }
            buttonMultiplication.setOnClickListener { onOperatorClicked(Operator.MULTIPLICATION) }
            buttonDivision.setOnClickListener { onOperatorClicked(Operator.DIVISION) }
            buttonEquals.setOnClickListener { onEqualsClicked() }
            buttonAllClear.setOnClickListener { onAllClearClicked() }
            buttonPlusMinus.setOnClickListener { onPlusMinusClicked() }
            buttonPercentage.setOnClickListener { onPercentageClicked() }
            imageNightMode.setOnClickListener { toggleNightMode() }
        }
    }

    // image_fc884e.jpg: 切换深色/浅色模式
    private fun toggleNightMode() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            // 如果是夜间模式，切换为白天
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            // 否则切换为夜间
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        recreate() // 重新创建Activity以应用主题
    }

    // 根据当前模式设置图标 (太阳/月亮)
    private fun setNightModeIndicator() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.imageNightMode.setImageResource(R.drawable.ic_sun)
        } else {
            binding.imageNightMode.setImageResource(R.drawable.ic_moon)
        }
    }

    // image_fc80a8.jpg: 百分比按键逻辑
    private fun onPercentageClicked() {
        if (inputValue2 == null) {
            // 场景1：只输入了第一个数时点百分比 (如 50 -> 0.5)
            val percentage = getInputValue1() / 100
            inputValue1 = percentage
            equation.clear().append(percentage)
            updateInputOnDisplay()
        } else {
            // 场景2：输入了两个数和运算符时点百分比 (如 100 + 10% -> 100 + 10)
            val percentageOfValue1 = (getInputValue1() * getInputValue2()) / 100
            val percentageOfValue2 = getInputValue2() / 100

            result = when (requireNotNull(currentOperator)) {
                Operator.ADDITION -> getInputValue1() + percentageOfValue1
                Operator.SUBTRACTION -> getInputValue1() - percentageOfValue1
                Operator.MULTIPLICATION -> getInputValue1() * percentageOfValue2
                Operator.DIVISION -> getInputValue1() / percentageOfValue2
            }

            equation.clear().append(ZERO)
            updateResultOnDisplay(isPercentage = true)

            // 计算完成后重置状态，准备下一次计算
            inputValue1 = result
            result = null
            inputValue2 = null
            currentOperator = null
        }
    }

    private fun onPlusMinusClicked() {
        if (equation.startsWith(MINUS)) {
            equation.deleteCharAt(0)
        } else {
            equation.insert(0,MINUS)
        }
        setInput()
        updateInputOnDisplay()
    }

    private fun onAllClearClicked() {
        inputValue1 = 0.0
        inputValue2 = null
        currentOperator = null
        result = null
        equation.clear().append(ZERO)
        clearDisplay()
    }

    private fun onOperatorClicked(operator: Operator) {
        onEqualsClicked()
        currentOperator = operator
    }

    private fun getNumericButtons() = with(binding) {
        arrayOf(
            buttonOne,
            buttonTwo,
            buttonThree,
            buttonFour,
            buttonFive,
            buttonSix,
            buttonSeven,
            buttonEight,
            buttonNine
        )
    }

    private fun onEqualsClicked() {
        if (inputValue2 != null) {
            result = calculate()
            equation.clear().append(ZERO)
            updateResultOnDisplay()
            inputValue1 = result
            result = null
            inputValue2 = null
            currentOperator = null
        } else {
            equation.clear().append(ZERO)
        }
    }

    private fun calculate(): Double {
        return when (requireNotNull(currentOperator)) {
            Operator.ADDITION -> getInputValue1() + getInputValue2()
            Operator.SUBTRACTION -> getInputValue1() - getInputValue2()
            Operator.MULTIPLICATION -> getInputValue1() * getInputValue2()
            Operator.DIVISION -> getInputValue1() / getInputValue2()
        }
    }

    private fun onDecimalPointClicked() {
        if (equation.contains(DECIMAL_POINT)) return
        equation.append(DECIMAL_POINT)
        setInput()
        updateInputOnDisplay()
    }

    private fun onZeroClicked() {
        if (equation.startsWith(ZERO)) return
        onNumberClicked(ZERO)
    }

    private fun onDoubleZeroClicked() {
        if (equation.startsWith(ZERO)) return
        onNumberClicked(DOUBLE_ZERO)
    }

    private fun clearDisplay() {
        with (binding) {
            textInput.text = getFormattedDisplayValue(getInputValue1())
            textEquation.text = null
        }
    }

    private fun onNumberClicked(numberText: String) {
        if (equation.startsWith(ZERO)) {
            equation.deleteCharAt(0)
        } else if (equation.startsWith("$MINUS$ZERO")) {
            equation.deleteCharAt(1)
        }

        equation.append(numberText)
        setInput()
        updateInputOnDisplay()
    }

    private fun setInput() {
        if (currentOperator == null) {
            inputValue1 = equation.toString().toDouble()
        } else {
            inputValue2 = equation.toString().toDouble()
        }
    }

    private fun updateInputOnDisplay() {
        if (result == null) {
            binding.textEquation.text = null
        }
        binding.textInput.text = equation
    }

    private fun updateResultOnDisplay(isPercentage: Boolean = false) {
        binding.textInput.text = getFormattedDisplayValue(result)
        var input2Text = getFormattedDisplayValue(result)
        if (isPercentage) input2Text = "$input2Text${getString(R.string.percentage)}"
        binding.textEquation.text = String.format(
            "%s %s %s",
            getFormattedDisplayValue(getInputValue1()),
            getOperatorSymbol(),
            input2Text
        )
    }

    private fun getInputValue1() = inputValue1 ?: 0.0
    private fun getInputValue2() = inputValue2 ?: 0.0

    private fun getOperatorSymbol(): String {
        return when (requireNotNull(currentOperator) {"运算符不可以为空"}) {
            Operator.ADDITION -> getString(R.string.addition)
            Operator.SUBTRACTION -> getString(R.string.subtraction)
            Operator.MULTIPLICATION -> getString(R.string.multiplication)
            Operator.DIVISION -> getString(R.string.division)
        }
    }

    // 避免出现0.0的情况
    private fun getFormattedDisplayValue(value: Double?): String? {
        val originalValue = value ?: return null
        return if (originalValue % 1 == 0.0) {
            originalValue.toInt().toString()
        } else {
            originalValue.toString()
        }
    }

    enum class Operator {
        ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION
    }

    private companion object {
        const val DECIMAL_POINT = "."
        const val ZERO = "0"
        const val DOUBLE_ZERO = "00"
        const val MINUS = "-"
    }
}