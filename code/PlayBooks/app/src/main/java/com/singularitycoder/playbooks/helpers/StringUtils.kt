package com.singularitycoder.playbooks.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


fun String.trimNewLines(): String = this.replace(oldValue = System.getProperty("line.separator") ?: "\n", newValue = " ")

// Works on Windows, Linux and Mac
// https://stackoverflow.com/questions/11048973/replace-new-line-return-with-space-using-regex
// https://javarevisited.blogspot.com/2014/04/how-to-replace-line-breaks-new-lines-windows-mac-linux.html
fun String.trimNewLinesUniversally(): String = this.replace(regex = Regex(pattern = "[\\t\\n\\r]+"), replacement = " ")

fun String.trimIndentsAndNewLines(): String = this.trimIndent().trimNewLinesUniversally()

fun String?.isNullOrBlankOrNaOrNullString(): Boolean {
    return this.isNullOrBlank() || "null" == this.toLowCase().trim() || "na" == this.toLowCase().trim()
}

fun String.toLowCase(): String = this.lowercase(Locale.getDefault())

fun String.toUpCase(): String = this.uppercase(Locale.getDefault())

fun String.capFirstChar(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.substringBeforeLastIgnoreCase(delimiter: String, missingDelimiterValue: String = this): String {
    val index = toLowCase().lastIndexOf(delimiter.toLowCase())
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

fun String.substringAfterLastIgnoreCase(delimiter: String, missingDelimiterValue: String? = null): String? {
    val index = toLowCase().lastIndexOf(delimiter.toLowCase())
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length, length)
}

fun String.toYoutubeThumbnailUrl(): String {
    val imageUrl = "https://img.youtube.com/vi/$this/0.jpg"
    println("Image url: $imageUrl")
    return imageUrl
}

// https://stackoverflow.com/questions/19945411/how-can-i-parse-a-local-json-file-from-assets-folder-into-a-listview
suspend fun Context.loadJsonStringFrom(@RawRes rawResource: Int): String? {
    return suspendCoroutine<String?> {
        try {
//        val inputStream: InputStream = assets.open("yourfilename.json")
            val inputStream: InputStream = resources.openRawResource(rawResource)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charset.forName("UTF-8"))
            it.resume(jsonString)
        } catch (_: IOException) {
            it.resume(null)
        }
    }
}

// https://stackoverflow.com/questions/21544973/convert-jsonobject-to-map
fun JSONObject.toMap(): Map<String, Any> = try {
    val map: MutableMap<String, Any> = HashMap()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        var value = this[key]
        if (value is JSONArray) {
            value = value.toList()
        } else if (value is JSONObject) {
            value = value.toMap()
        }
        map[key] = value
    }
    map
} catch (_: Exception) {
    emptyMap<String, Any>()
}

// https://stackoverflow.com/questions/21544973/convert-jsonobject-to-map
fun JSONArray.toList(): List<Any> = try {
    val list: MutableList<Any> = ArrayList()
    for (i in 0 until this.length()) {
        var value = this[i]
        if (value is JSONArray) {
            value = value.toList()
        } else if (value is JSONObject) {
            value = value.toMap()
        }
        list.add(value)
    }
    list
} catch (_: Exception) {
    emptyList<Any>()
}

// https://stackoverflow.com/questions/44870961/how-to-map-a-json-string-to-kotlin-map
fun JSONObject.toMap2(): Map<String, Any?> =
    keys().asSequence().associateWith { key -> toValue(get(key)) }

// https://stackoverflow.com/questions/44870961/how-to-map-a-json-string-to-kotlin-map
fun JSONArray.toList2(): List<Any?> =
    (0 until length()).map { index -> toValue(get(index)) }

// https://stackoverflow.com/questions/44870961/how-to-map-a-json-string-to-kotlin-map
private fun toValue(element: Any) = when (element) {
    JSONObject.NULL -> null
    is JSONObject -> element.toMap()
    is JSONArray -> element.toList()
    else -> element
}

fun getAssetsResourcePath(directory: String, resourceNameWithExtension: String): String {
    return "file:///android_asset/$directory/$resourceNameWithExtension"
}

fun getRawResourcePath(resourceNameWithExtension: String): String {
    return "file:///android_res/raw/$resourceNameWithExtension"
}

fun inputStreamToString(
    connection: HttpURLConnection,
    inputStream: InputStream
): String {
    var line: String? = ""
    val stringBuilder = StringBuilder()
    val inputStreamReader = InputStreamReader(inputStream)
    val bufferedReader = BufferedReader(inputStreamReader)
    try {
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            bufferedReader.close()
            connection.disconnect()
        } catch (ignored: Exception) {
        }
    }
    return stringBuilder.toString()
}

// Bitmap to Binary - Bard -> https://qiita.com/date62noka3/items/42f971fb0ee1be2970e8
// use the compress() method of the Bitmap object to write the bitmap to the output stream.
fun Bitmap?.toByteArray(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    this?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}

// https://stackoverflow.com/questions/4989182/converting-java-bitmap-to-byte-array
fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray? {
    val byteBuffer: ByteBuffer = ByteBuffer.allocate(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(byteBuffer)
    byteBuffer.rewind()
    return byteBuffer.array()
}

fun Context.getBitmapFrom(@DrawableRes image: Int): Bitmap {
    return BitmapFactory.decodeResource(resources, image)
}

// Bard -> https://qiita.com/date62noka3/items/42f971fb0ee1be2970e8
fun Context.getBitmapFromUri(uri: Uri?): Bitmap? {
    uri ?: return null
    val parcelFileDescriptor: ParcelFileDescriptor = this.contentResolver.openFileDescriptor(uri, "r") ?: return null
    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
    val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
    parcelFileDescriptor.close()
    return image
}

/**
 * Here are some other ways to convert a string to a byte array in Kotlin:
 * You can use the encodeToByteArray() method of the String class. This method takes an optional character set parameter, which defaults to UTF-8.
 * You can use the getBytes() method of the String class. This method takes an optional character set parameter, which defaults to the platform's default character set.
 * You can use the toByteArray() method of the CharSequence interface. This method takes an optional character set parameter, which defaults to UTF-8.
 * */
fun ByteArray?.toBitmap(): Bitmap? {
    this ?: return null
    return BitmapFactory.decodeByteArray(
        /* data = */ this,
        /* offset = */ 0,
        /* length = */ this.size
    )
}

// https://stackoverflow.com/questions/4989182/converting-java-bitmap-to-byte-array
fun encodeBitmapToBase64String(bitmap: Bitmap?): String? {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap?.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
}

// https://stackoverflow.com/questions/4989182/converting-java-bitmap-to-byte-array
fun decodeBase64StringToBitmap(string: String?): Bitmap? {
    string ?: return null
    val decodedByte: ByteArray = android.util.Base64.decode(string, 0)
    return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
}

fun String.toMaxChar(count: Long): String {
    return if (this.length >= count) {
        this.substring(0, 1000)
    } else {
        this
    }
}

// https://mkyong.com/java/java-convert-string-to-xml/
fun convertXmlToString(doc: Document): String? {
    val domSource = DOMSource(doc)
    val writer = StringWriter()
    val result = StreamResult(writer)
    val tf: TransformerFactory = TransformerFactory.newInstance()
    var transformer: Transformer? = null
    try {
        transformer = tf.newTransformer()
        transformer.transform(domSource, result)
    } catch (e: TransformerException) {
        throw RuntimeException(e)
    }
    return writer.toString()
}

// https://mkyong.com/java/java-convert-string-to-xml/
fun convertStringToXml(xmlString: String): Document? {
    val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    return try {
        // optional, but recommended - process XML securely, avoid attacks like XML External Entities (XXE)
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        val builder: DocumentBuilder = dbf.newDocumentBuilder()
        builder.parse(InputSource(StringReader(xmlString)))
    } catch (_: Exception) {
        null
    }
}

fun String?.trimCdata(): String {
    return this?.replace(oldValue = "<![CDATA[", newValue = "")?.replace(oldValue = "]]>", newValue = "") ?: ""
}

fun String?.resolveAmpersand(): String {
    return this?.replace(oldValue = "&amp;", newValue = "&") ?: ""
}

fun String?.trimEscapeChars(): String {
    var string = this ?: ""
    val escapeCharList = listOf("\r", "\n", "\\", "\t", "\b")
    listOf(escapeCharList, listOf("r/")).flatten().forEach {
        string = string.replace(oldValue = it, newValue = "")
    }
    return string
}