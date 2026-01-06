package com.example.unitexml

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        val button = findViewById<ImageButton>(R.id.reback)
        findViewById<Button>(R.id.phone).setOnClickListener(this)
        findViewById<Button>(R.id.message).setOnClickListener(this)
        findViewById<Button>(R.id.self).setOnClickListener(this)
        button.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.reback -> {
                // 销毁界面
                finish()
            }
            R.id.phone -> {
                // 拨打电话
                val int = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:16638354298")
                }
                startActivity(int)
            }
            R.id.message -> {
                val int = Intent(Intent.ACTION_SENDTO).apply {
                    // 指的是用户
                    data = Uri.parse("smsto:hello world")
                    // 发送的内容
                    putExtra("sms_body","你好世界")
                }
                startActivity(int)
            }
            R.id.self -> {
                val int = Intent("android.intent.action.MYAPP").apply {
                    data = Uri.parse("myapp://user_profile")
                }
                startActivity(int)
            }
        }
    }
}