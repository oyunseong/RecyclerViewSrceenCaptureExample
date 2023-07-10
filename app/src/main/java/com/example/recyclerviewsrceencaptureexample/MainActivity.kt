package com.example.recyclerviewsrceencaptureexample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recyclerviewsrceencaptureexample.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var startPos = -1
    var endPos = -1
    var isActivationCapture = false
    var isSelectedFirstPosition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val items: List<Int> = (1..300).map { it }
        val item: ArrayList<User> = arrayListOf()
        for (i in 0..100) {
            item.add(User("유저${i}", i))
        }

        // TextView만 캡쳐
        binding.text1.setOnClickListenerWithPermissionCheck {
            binding.text1.fullScreenCapture()
        }

        binding.button1.setOnClickListenerWithPermissionCheck {
            if (!isActivationCapture) {
                this.showToast("범위 선택 시작!")
                isActivationCapture = true
            }

        }

        binding.button2.setOnClickListenerWithPermissionCheck {
            try {
                if (!isActivationCapture) {
                    binding.recyclerView.captureMyRecyclerView(
                        itemBackgroundColor = this.getColor(R.color.yellow),
                        startPos = startPos,
                        endPos = endPos
                    )
                }
            } catch (e: Exception) {
                this.showToast("캡쳐 실패")
                e.printStackTrace()
            }

        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = UserAdapter(item) {
            // 권한 확인
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {
                if (isActivationCapture && !isSelectedFirstPosition) {
                    startPos = it
                    isSelectedFirstPosition = true
                    this.showToast("시작 : ${startPos}")

                } else if (isActivationCapture) {
                    endPos = it
                    isSelectedFirstPosition = false
                    isActivationCapture = false
                    this.showToast("종료 :${endPos}, 범위 선택 종료!")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    Toast.makeText(
                        this,
                        "권한 획득 성공",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 권한 획득 이후 캡쳐
                    binding.recyclerView.captureMyRecyclerView(
                        itemBackgroundColor = this.getColor(R.color.yellow),
                        startPos = 0,
                        endPos = 10
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this,
                    "저장소 권한이 거부되어 캡쳐를 할 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun View.setOnClickListenerWithPermissionCheck(action: () -> Unit) {
        setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {
                action.invoke()
            }
        }
    }



    companion object {
        const val REQUEST_CODE_STORAGE_PERMISSION = 1
    }
}

