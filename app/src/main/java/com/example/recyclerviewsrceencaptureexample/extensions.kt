package com.example.recyclerviewsrceencaptureexample


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * 리사이클러뷰 범위 캡쳐
 * @param itemBackgroundColor : 아이템의 백그라운드 color
 * @param startPos : 캡처 시작 포지션
 * @param endPos   : 캡처 종료 포지션
 */
fun RecyclerView.captureMyRecyclerView(
    @ColorInt itemBackgroundColor: Int,
    startPos: Int,
    endPos: Int,
) {
    val adapter = this.adapter

    // 시작 위치와 끝 위치 스왑
    var startPosition = startPos
    var endPosition = endPos

    if (startPosition > endPosition) {
        val tmp = endPosition
        endPosition = startPosition
        startPosition = tmp
    }

    val itemSize = (endPosition - startPosition) + 1     // 캡쳐할 아이템 수
    var height = 0  // RecyclerView의 높이
    val paint = Paint()
    var itemHeight = 0 // 캡쳐할 아이템 높이의 합
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxMemory / 8
    val bitmapCache = LruCache<String, Bitmap>(cacheSize)

    /**
     * 아이템 개수만큼 리사이클러뷰의 높이 추가
     * */
    for (i in startPosition .. endPosition) {
        val holder = adapter?.createViewHolder(this, adapter.getItemViewType(i))    // 뷰홀더 생성
        holder?.let {
            adapter.onBindViewHolder(holder, i)
            holder.itemView.measure(
                View.MeasureSpec.makeMeasureSpec(this.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            holder.itemView.layout(
                0,
                0,
                holder.itemView.measuredWidth,
                holder.itemView.measuredHeight
            )
            holder.itemView.isDrawingCacheEnabled = true    // 캐시 활성화
            holder.itemView.buildDrawingCache()             // 캐시 빌드 ???

            if (itemBackgroundColor != 0) {
                // 아이템 백그라운드 색상 지정
                holder.itemView.setBackgroundColor(itemBackgroundColor)
            }
            val drawingCache = holder.itemView.drawingCache // 캐시 획득
            if (drawingCache != null) {
                bitmapCache.put(i.toString(), drawingCache) // 캐시에 비트맵 추가
            }
            height += holder.itemView.measuredHeight // 아이템 높이를 누적하여 RecyclerView의 높이 계산 (ex. item이 10개면 bitmap 높이는 item height * 10)
        }
    }

    // Create bigBitmap with calculated height
    val bigBitmap =
        Bitmap.createBitmap(this.measuredWidth, height, Bitmap.Config.ARGB_8888)    // 캡쳐할 비트맵 생성

    val bigCanvas = Canvas(bigBitmap)
    bigCanvas.drawColor(Color.WHITE)    // 부모 뷰 백그라운드 색상 설정

    // 캡쳐한 비트맵 그리기
    for (i in startPosition..endPosition) {
        val bitmap = bitmapCache.get(i.toString())  //캡쳐한 비트맵 가져오기
        bigCanvas.drawBitmap(bitmap, 0f, itemHeight.toFloat(), paint) // 캔버스에 캡쳐한 비트맵 그리기
        itemHeight += bitmap?.height ?: 0   // 아이템별 높이 계산
        bitmap?.recycle()   // 캡쳐한 비트맵 해제
    }

    val filename = "Img_${System.currentTimeMillis()}.jpeg" // 파일명
    val mimeType = "image/jpeg"                             // 이미지 MIME 타입 설정

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            outputStream?.use { stream ->
                bigBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                Toast.makeText(
                    context,
                    "캡쳐가 저장되었습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    } else {
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(directory, filename)

        val outputStream = FileOutputStream(file)
        outputStream.use { stream ->
            bigBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            Toast.makeText(
                context,
                "캡쳐가 저장되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}


/**
 * 전체 캡쳐 기능
 * @return Uri 반환
 */
fun View.fullScreenCapture(): Uri? {
    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    val bgDrawable = this.background
    if (bgDrawable != null) bgDrawable.draw(canvas) else canvas.drawColor(Color.WHITE)
    this.draw(canvas)
    val filename = "Img_${System.currentTimeMillis()}.jpeg"
    val mimeType = "image/jpeg"

    // 디바이스가 Android Q 이상인지 확인합니다.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // 이미지 정보를 저장하기 위해 ContentValues를 생성합니다.
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        // ContentResolver를 가져옵니다.
        val contentResolver = context.contentResolver

        // MediaStore에 이미지 정보를 삽입하고 컨텐츠 URI를 가져옵니다.
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            // 컨텐츠 URI에 대한 OutputStream을 엽니다.
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)

            outputStream?.use { stream ->
                // 비트맵을 압축하여 OutputStream에 저장합니다.
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                Toast.makeText(
                    context,
                    "캡쳐 성공",//R.string.capture_success_txt,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return uri
    } else {
        // Android Q 이하 버전에서는 외부 저장소에 직접 파일을 저장합니다.
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(directory, filename)

        val outputStream = FileOutputStream(file)
        outputStream.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            Toast.makeText(
                context,
                "캡쳐 성공",//R.string.capture_success_txt,
                Toast.LENGTH_SHORT
            ).show()
        }
        return Uri.fromFile(file)
    }
}


/**
 * 전체 화면 캡처 기능
 * @param backgroundUrl: 캡처 대상 뷰에 설정할 배경 이미지의 URL입니다.
 * @param onComplete: 화면 캡처가 완료된 후 호출되는 콜백 함수입니다. 캡처된 이미지의 URI를 제공합니다.
 */
fun View.captureScreen(
    backgroundUrl: String? = null,
    onComplete: (Uri?) -> Unit = {}
) {
    if (backgroundUrl.isNullOrEmpty()) {
        captureScreenWithBackgroundOrDefaultWhite(onComplete)
    } else {
        captureScreenWithGlideBackground(backgroundUrl, onComplete)
    }
}

/**
 * 기존 백그라운드를 사용하여 전체 화면을 캡쳐합니다.
 * 기존 백그라운드 이미지가 없는 경우, 배경 색상을 흰색으로 설정하여 캡쳐합니다.
 */
private fun View.captureScreenWithBackgroundOrDefaultWhite(onComplete: (Uri?) -> Unit) {
    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    val bgDrawable = this.background
    if (bgDrawable != null) bgDrawable.draw(canvas) else canvas.drawColor(Color.WHITE)
    this.draw(canvas)
    val filename = "Img_${System.currentTimeMillis()}.jpeg"
    val mimeType = "image/jpeg"

    // 디바이스가 Android Q 이상인지 확인합니다.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // 이미지 정보를 저장하기 위해 ContentValues를 생성합니다.
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        // ContentResolver를 가져옵니다.
        val contentResolver = context.contentResolver

        // MediaStore에 이미지 정보를 삽입하고 컨텐츠 URI를 가져옵니다.
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            // 컨텐츠 URI에 대한 OutputStream을 엽니다.
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)

            outputStream?.use { stream ->
                // 비트맵을 압축하여 OutputStream에 저장합니다.
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
        }
        onComplete(uri)
    } else {
        // Android Q 이하 버전에서는 외부 저장소에 직접 파일을 저장합니다.
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(directory, filename)

        val outputStream = FileOutputStream(file)
        outputStream.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
        onComplete(Uri.fromFile(file))
    }
}

/**
 * Glide를 사용하여 배경 이미지를 설정한 상태에서 전체 화면을 캡처합니다.
 */
private fun View.captureScreenWithGlideBackground(
    background: String? = null,
    onComplete: (Uri?) -> Unit
) {
    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    val filename = "Img_${System.currentTimeMillis()}.jpeg"
    val mimeType = "image/jpeg"

    Glide.with(this.context)
        .load(background)
        .centerCrop()
        .into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) {
                this@captureScreenWithGlideBackground.background = resource
                resource.draw(canvas)
                this@captureScreenWithGlideBackground.draw(canvas)

                // 디바이스가 Android Q 이상인지 확인합니다.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 이미지 정보를 저장하기 위해 ContentValues를 생성합니다.
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES
                        )
                    }

                    // ContentResolver를 가져옵니다.
                    val contentResolver = context.contentResolver

                    // MediaStore에 이미지 정보를 삽입하고 컨텐츠 URI를 가져옵니다.
                    val contentUri =
                        contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        )

                    contentUri?.let {
                        // 컨텐츠 URI에 대한 OutputStream을 엽니다.
                        val outputStream: OutputStream? = contentResolver.openOutputStream(it)

                        outputStream?.use { stream ->
                            // 비트맵을 압축하여 OutputStream에 저장합니다.
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        }
                    }
                    onComplete.invoke(contentUri)
                } else {
                    // Android Q 이하 버전에서는 외부 저장소에 직접 파일을 저장합니다.
                    val directory =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val file = File(directory, filename)

                    val outputStream = FileOutputStream(file)
                    outputStream.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                    onComplete.invoke(Uri.fromFile(file))
                }

                // 캡처할 때 세팅한 background 제거
                this@captureScreenWithGlideBackground.background = ColorDrawable(Color.TRANSPARENT)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                //  onLoad() 메서드가 호출되기 전에 실행되는 메소드입니다.
                //  이 메소드는 이미지 로딩 작업이 취소되거나 이미지가 제거될 때 호출됩니다.
                Log.e("ViewExt", "Image loading canceled or removed.")
            }

            // Glide가 이미지 load를 실패했을 경우 호출됩니다.
            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                Log.e("ViewExt", "Image loading failed.")
                return
            }
        })
}